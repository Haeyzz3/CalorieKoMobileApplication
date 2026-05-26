package com.calorieko.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.local.DishRecipeDao
import com.calorieko.app.data.local.MealPlanDao
import com.calorieko.app.data.local.PantryDao
import com.calorieko.app.data.local.DishMatchInfo
import com.calorieko.app.data.local.RawIngredientDao
import com.calorieko.app.data.local.RecipeNutritionCalculator
import com.calorieko.app.data.local.IngredientNutritionBreakdown
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.local.UserDao
import com.calorieko.app.data.repository.MealPlanRepository
import com.calorieko.app.data.model.NutritionResult
import com.calorieko.app.data.model.PantryItem
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.model.RawIngredientEntity
import com.calorieko.app.data.remote.FirestoreSyncRepository
import com.calorieko.app.data.remote.api.AutoSyncManager
import com.calorieko.app.data.repository.NutritionalValuesRepository
import com.calorieko.app.util.RecipeCustomizationRules
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Rich ingredient info for display in the Recipe Detail bottom sheet.
 *
 * [name] is the authoritative display name from RAW_INGREDIENTS_TABLE (e.g., "Soybean Oil").
 * [ingredientKey] is the raw key used for substitution lookups and pantry matching (e.g., "cooking_oil").
 */
data class IngredientInfo(
    val name: String,
    val ingredientKey: String = "",  // Raw key from DISH_INGREDIENTS_TABLE
    val type: String,           // "core" or "optional"
    val category: String,       // "protein", "produce", "seasoning", "pantry_staple"
    val portionQuantity: String, // e.g. "5 cups", "" if not specified
    val preparationMethod: String, // e.g. "sliced", "" if not specified
    val step: Int,               // 1-based step number
    val replacementIngredientKey: String? = null,
    val replacementName: String? = null,
    val isRemoved: Boolean = false
)

/**
 * Result of the core-aware recipe matching engine.
 */
data class DishResult(
    val dishLabel: String,
    val dishName: String,
    val dishNamePh: String = "",
    val dishNameEn: String = "",
    val ingredients: List<String>,
    val ingredientDetails: List<IngredientInfo> = emptyList(),
    val missingCoreIngredients: List<String>,
    val missingOptionalIngredients: List<String>,
    val coreMatchedCount: Int = 0,
    val coreTotalCount: Int = 0,
    // Core energy
    val calories: Int = 0,
    // Macros
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    // Minerals & vitamins
    val sodium: Int = 0,
    val potassium: Float = 0f,
    val vitaminA: Float = 0f,
    val vitaminC: Float = 0f,
    val calcium: Float = 0f,
    val iron: Float = 0f,
    // Source attribution
    val dataSource: String = "USDA_FDC",
    // FNRI serving size description (e.g., "1 1/2 cups")
    val servingSizeDescription: String = "",
    // Per-serving weight in grams (cooked_weight_g / servings)
    val perServingWeightG: Float = 0f,
    // Original serving count from DishRecipeEntity (baseline for scaling)
    val originalServings: Int = 1,
    val appliedSubstitutions: Map<String, String> = emptyMap(),
    val appliedScaledServings: Int = 0,
    val appliedTweaks: Map<String, Float> = emptyMap(),
    val appliedTweakedPerServingWeightG: Float = 0f,
    // Weighted readiness score: (core_matched*3 + optional_matched*1) / (core_total*3 + optional_total*1)
    // Used for classification and sorting in the "What Can I Cook?" section.
    val readinessScore: Float = 0f,
    // Number of dish ingredients matched via pantry substitutes (not direct matches).
    // Used for the "🔄 with subs" visual indicator on recipe cards.
    val substituteMatchCount: Int = 0
)

/**
 * Week metadata for the scrubber pills in the Meal Plan Calendar.
 */
data class WeekInfo(
    val weekStartDate: String,      // ISO date, e.g., "2026-04-13"
    val label: String,              // "Apr 13 – 19"
    val mealCount: Int,             // for density dots
    val isCurrentWeek: Boolean,     // true if this is today's week
    val isPast: Boolean,            // true if entire week is before current week
    val isBeyondHorizon: Boolean    // true if beyond 8-week planning cap
)


sealed interface PantryUiEvent {
    data class Snackbar(val message: String) : PantryUiEvent
}

class PantryViewModel(
    private val auth: FirebaseAuth,
    private val pantryDao: PantryDao,
    private val mealPlanDao: MealPlanDao,
    private val dishRecipeDao: DishRecipeDao,
    private val rawIngredientDao: RawIngredientDao,
    private val calculator: RecipeNutritionCalculator,
    private val firestoreSyncRepo: FirestoreSyncRepository,
    private val userDao: UserDao,
    private val nutritionalValuesRepo: NutritionalValuesRepository,
    private val mealLogDao: MealLogDao,
    private val mealPlanRepository: MealPlanRepository,
    private val appContext: Context
) : ViewModel() {

    private val uid: String get() = auth.currentUser?.uid ?: ""

    // --- Factory ---
    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            pantryDao: PantryDao,
            mealPlanDao: MealPlanDao,
            dishRecipeDao: DishRecipeDao,
            rawIngredientDao: RawIngredientDao,
            calculator: RecipeNutritionCalculator,
            firestoreSyncRepo: FirestoreSyncRepository,
            userDao: UserDao,
            nutritionalValuesRepo: NutritionalValuesRepository,
            mealLogDao: MealLogDao,
            mealPlanRepository: MealPlanRepository,
            appContext: Context
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(PantryViewModel::class.java)) {
                    return PantryViewModel(auth, pantryDao, mealPlanDao, dishRecipeDao, rawIngredientDao, calculator, firestoreSyncRepo, userDao, nutritionalValuesRepo, mealLogDao, mealPlanRepository, appContext) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        /** Sentinel value in the substitution map indicating an ingredient was removed. */
        const val REMOVED_INGREDIENT = "__REMOVED__"
    }

    // --- Search ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- Pantry items (reactive from Room) ---
    val pantryItems: StateFlow<List<String>> = pantryDao.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- All unique ingredients for autocomplete ---
    private val _allIngredients = MutableStateFlow<List<String>>(emptyList())

    // --- Autocomplete suggestions (filtered) ---
    private val _autocompleteSuggestions = MutableStateFlow<List<String>>(emptyList())
    val autocompleteSuggestions: StateFlow<List<String>> = _autocompleteSuggestions.asStateFlow()

    // --- Recipe matching results ---
    private val _readyToCookDishes = MutableStateFlow<List<DishResult>>(emptyList())
    val readyToCookDishes: StateFlow<List<DishResult>> = _readyToCookDishes.asStateFlow()

    private val _almostReadyDishes = MutableStateFlow<List<DishResult>>(emptyList())
    val almostReadyDishes: StateFlow<List<DishResult>> = _almostReadyDishes.asStateFlow()

    // --- Store-bought dishes (always available, no ingredients needed) ---
    private val _storeBoughtDishes = MutableStateFlow<List<DishResult>>(emptyList())
    val storeBoughtDishes: StateFlow<List<DishResult>> = _storeBoughtDishes.asStateFlow()

    // --- Meal Plan ---
    private val _currentWeekStart = MutableStateFlow(getWeekStartDate())
    val currentWeekStart: StateFlow<String> = _currentWeekStart.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val plannedMeals: StateFlow<List<PlannedMealEntity>> = _currentWeekStart
        .flatMapLatest { weekStart -> mealPlanDao.getMealsForWeek(uid, weekStart) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _weeklyCalories = MutableStateFlow(0)
    val weeklyCalories: StateFlow<Int> = _weeklyCalories.asStateFlow()

    private val _avgDailySodium = MutableStateFlow(0)
    val avgDailySodium: StateFlow<Int> = _avgDailySodium.asStateFlow()

    // --- Month Navigation & Week Scrubber ---
    private val _displayedMonth = MutableStateFlow(YearMonth.now())
    val displayedMonth: StateFlow<YearMonth> = _displayedMonth.asStateFlow()

    private val _weeksInMonth = MutableStateFlow<List<WeekInfo>>(emptyList())
    val weeksInMonth: StateFlow<List<WeekInfo>> = _weeksInMonth.asStateFlow()

    // --- Day Dates for Grid Column Headers ---
    private val _weekDayDates = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val weekDayDates: StateFlow<List<Pair<String, Int>>> = _weekDayDates.asStateFlow()

    // --- Today Indicator (null if today is not in the displayed week) ---
    private val _todayColumnIndex = MutableStateFlow<Int?>(null)
    val todayColumnIndex: StateFlow<Int?> = _todayColumnIndex.asStateFlow()

    private val _uiEvents = MutableSharedFlow<PantryUiEvent>(extraBufferCapacity = 1)
    val uiEvents: SharedFlow<PantryUiEvent> = _uiEvents.asSharedFlow()

    // --- Meal Plan Completion Status ---
    enum class CellCompletionStatus { LOGGED, SKIPPED, MISSED, PLANNED }

    /** Logged dish references for the displayed week (reactive from MealLogDao). */
    private val _weekLoggedDishes = MutableStateFlow<Set<Pair<String, String>>>(emptySet())

    /**
     * Per-cell completion status map.
     * Key: "dayIndex_mealSlot" (e.g., "0_Breakfast")
     * Value: CellCompletionStatus
     */
    private val _cellCompletionStatus = MutableStateFlow<Map<String, CellCompletionStatus>>(emptyMap())
    val cellCompletionStatus: StateFlow<Map<String, CellCompletionStatus>> = _cellCompletionStatus.asStateFlow()

    /** Weekly adherence string: "X/Y" (meals logged / meals planned). */
    private val _weeklyAdherence = MutableStateFlow("")
    val weeklyAdherence: StateFlow<String> = _weeklyAdherence.asStateFlow()

    /** Job for observing logged dishes — cancelled and restarted on week changes. */
    private var weekLogObservationJob: Job? = null

    // --- Cache for dish nutritional data ---
    private val _dishNutritionCache = mutableMapOf<String, DishNutritionInfo>()

    private data class DishNutritionInfo(
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fats: Int,
        val fiber: Float,
        val sugar: Float,
        val sodium: Int,
        val potassium: Float,
        val vitaminA: Float,
        val vitaminC: Float,
        val calcium: Float,
        val iron: Float
    )

    // --- User's actual daily calorie target and sodium limit ---
    private val _userCalorieTarget = MutableStateFlow(2000)
    val userCalorieTarget: StateFlow<Int> = _userCalorieTarget.asStateFlow()

    private val _userSodiumLimit = MutableStateFlow(2000)
    val userSodiumLimit: StateFlow<Int> = _userSodiumLimit.asStateFlow()

    // --- Pantry items grouped by category for UI ---
    private val _pantryItemsByCategory = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val pantryItemsByCategory: StateFlow<Map<String, List<String>>> = _pantryItemsByCategory.asStateFlow()

    // --- All browsable ingredients for the Ingredient Browser ---
    private val _allBrowsableIngredients = MutableStateFlow<List<RawIngredientEntity>>(emptyList())
    val allBrowsableIngredients: StateFlow<List<RawIngredientEntity>> = _allBrowsableIngredients.asStateFlow()

    // --- Display name cache: ingredient_key → display_name from RAW_INGREDIENTS_TABLE ---
    // Populated once at init; used by formatIngredientName() for authoritative naming.
    private val _displayNameCache = mutableMapOf<String, String>()

    private data class DishDisplayNames(
        val namePh: String,
        val nameEn: String,
        val servingSizeDescription: String = "",
        val perServingWeightG: Float = 0f,
        val servings: Int = 1
    ) {
        val primaryName: String
            get() = namePh.ifBlank { nameEn }
    }

    private val _dishDisplayNameCache = mutableMapOf<String, DishDisplayNames>()

    init {
        // Backfill uid on pre-v30 rows (migration sets uid = '')
        viewModelScope.launch(Dispatchers.IO) {
            if (uid.isNotBlank()) {
                mealPlanDao.backfillUid(uid)
            }
        }

        // Load all unique ingredients for autocomplete
        // Merge recipe ingredients + raw ingredient keys to cover all browsable items
        viewModelScope.launch(Dispatchers.IO) {
            val recipeIngredients = pantryDao.getAllUniqueIngredients()
            val rawKeys = rawIngredientDao.getAllBrowsable().map { it.ingredientKey }
            _allIngredients.value = (recipeIngredients + rawKeys).distinct().sorted()
        }

        // Load all browsable ingredients for the Ingredient Browser (excluding store_bought)
        // Also populate the display name cache for formatIngredientName()
        viewModelScope.launch(Dispatchers.IO) {
            val browsable = rawIngredientDao.getAllBrowsable()
            _allBrowsableIngredients.value = browsable
            // Build cache from ALL raw ingredients (including store_bought) for full coverage
            val allRaw = rawIngredientDao.getAllRawIngredients()
            allRaw.forEach { _displayNameCache[it.ingredientKey] = it.displayName }
        }

        viewModelScope.launch(Dispatchers.IO) {
            dishRecipeDao.getAllDishRecipes().forEach { recipe ->
                _dishDisplayNameCache[recipe.dishLabel] = DishDisplayNames(
                    namePh = recipe.namePh,
                    nameEn = recipe.nameEn,
                    servingSizeDescription = recipe.servingSizeDescription,
                    perServingWeightG = recipe.perServingWeightG,
                    servings = recipe.servings
                )
            }
        }

        // Load user's actual nutritional targets
        viewModelScope.launch(Dispatchers.IO) {
            loadUserNutritionalTargets()
        }

        // React to pantry changes → recompute recipe matches + category grouping
        viewModelScope.launch {
            pantryItems.collect { items ->
                withContext(Dispatchers.IO) {
                    recomputeRecipeMatches(items)
                    recomputePantryCategories(items)
                }
            }
        }

        // React to planned meals changes → recompute weekly stats + refresh scrubber dots
        viewModelScope.launch {
            plannedMeals.collect { meals ->
                withContext(Dispatchers.IO) {
                    recomputeWeeklyStats(meals)
                    recomputeWeekScrubberData()
                }
            }
        }

        // React to current week changes → update day dates + today indicator + observe meal logs
        viewModelScope.launch {
            _currentWeekStart.collect { weekStart ->
                _weekDayDates.value = computeWeekDayDates(weekStart)
                _todayColumnIndex.value = computeTodayColumnIndex(weekStart)
                observeLoggedDishesForWeek(weekStart)
            }
        }

        // React to displayed month changes → recompute week scrubber pills
        viewModelScope.launch {
            _displayedMonth.collect {
                withContext(Dispatchers.IO) { recomputeWeekScrubberData() }
            }
        }

        // React to planned meals + logged dishes → derive cell completion status + adherence
        viewModelScope.launch {
            combine(plannedMeals, _weekLoggedDishes) { planned, loggedSet ->
                planned to loggedSet
            }.collect { (planned, loggedSet) ->
                _cellCompletionStatus.value = deriveCellCompletionStatus(planned, loggedSet)
                // Compute adherence
                val totalPlanned = planned.size
                val totalLogged = planned.count { meal ->
                    (normalizeSlotName(meal.mealSlot) to normalizeDishName(meal.dishLabel)) in loggedSet
                }
                _weeklyAdherence.value = if (totalPlanned > 0) "$totalLogged/$totalPlanned" else ""
            }
        }
    }

    /**
     * Loads the user's actual calorie target and sodium limit from their profile
     * via the Mifflin-St Jeor calculation in NutritionalValuesRepository.
     */
    private suspend fun loadUserNutritionalTargets() {
        val currentUid = uid
        if (currentUid.isEmpty()) return

        val profile = userDao.getUserProfile(currentUid) ?: return
        val targets = nutritionalValuesRepo.getTargetsForUser(profile)

        _userCalorieTarget.value = targets.targetCalories
        _userSodiumLimit.value = targets.targetSodium
    }

    // ============================================================
    // Pantry Actions
    // ============================================================

    fun addIngredient(name: String) {
        val trimmed = name.trim().lowercase()
        if (trimmed.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            pantryDao.insertItem(PantryItem(ingredientName = trimmed))
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.syncPantryItem(uid, trimmed) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
        }
    }

    fun removeIngredient(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            pantryDao.deleteItem(name)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.deletePantryItem(uid, name) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
        }
    }

    /**
     * Applies batch pantry changes from the Ingredient Browser.
     * Computes the diff between current pantry and the selected set,
     * then adds new items and removes deselected items in batch.
     *
     * @param selectedKeys The full set of ingredient_keys the user has checked
     */
    fun batchUpdatePantry(selectedKeys: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentPantry = pantryItems.value.toSet()

            val toAdd = selectedKeys - currentPantry
            val toRemove = currentPantry - selectedKeys

            // Batch insert new items
            if (toAdd.isNotEmpty()) {
                pantryDao.insertAll(toAdd.map { PantryItem(ingredientName = it) })
            }

            // Batch remove deselected items
            if (toRemove.isNotEmpty()) {
                pantryDao.deleteItems(toRemove.toList())
            }

            // Single Firestore sync pass for all changes
            if (uid.isNotEmpty() && (toAdd.isNotEmpty() || toRemove.isNotEmpty())) {
                withTimeoutOrNull(5_000L) {
                    try {
                        toAdd.forEach { firestoreSyncRepo.syncPantryItem(uid, it) }
                        toRemove.forEach { firestoreSyncRepo.deletePantryItem(uid, it) }
                    } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
        }
    }

    // ============================================================
    // Search & Autocomplete
    // ============================================================

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _autocompleteSuggestions.value = emptyList()
            return
        }
        val lowerQuery = query.lowercase()
        val currentPantry = pantryItems.value
        _autocompleteSuggestions.value = _allIngredients.value.filter { ingredient ->
            val matchesKey = ingredient.contains(lowerQuery)
            // Also match against authoritative display name (e.g., "Soybean Oil")
            val displayName = _displayNameCache[ingredient]?.lowercase() ?: ""
            val matchesDisplayName = displayName.contains(lowerQuery)
            (matchesKey || matchesDisplayName) && ingredient !in currentPantry
        }.take(10)
    }

    // ============================================================
    // Core-Aware Recipe Matching Engine
    // ============================================================

    /** Minimum weighted readiness score for a dish to appear in "Almost Ready". */
    private val readinessThreshold = 0.40f

    /**
     * Computes a weighted readiness score for a dish.
     *
     * Core ingredients are weighted 3× heavier than optional ingredients,
     * reflecting their greater importance to the dish's identity.
     *
     * @return A score in [0.0, 1.0]. Returns 0 if the dish has no ingredients.
     */
    private fun computeReadinessScore(info: DishMatchInfo): Float {
        val optionalTotal = info.total_ingredients - info.core_total
        val optionalMatched = info.matched_count - info.core_matched
        val denominator = info.core_total * 3f + optionalTotal * 1f
        if (denominator == 0f) return 0f
        return (info.core_matched * 3f + optionalMatched * 1f) / denominator
    }

    /**
     * Builds a substitute-expanded virtual pantry.
     *
     * For each pantry item that is [is_substitutable], finds all other ingredients
     * in the same [sub_category] and adds them to the virtual pantry. This allows
     * the recipe matching SQL to treat substitute availability as ingredient presence.
     *
     * @return Pair of (expandedPantryList, substituteOnlyKeys) where substituteOnlyKeys
     *         contains keys that exist ONLY via expansion (not in the real pantry).
     */
    private suspend fun buildExpandedPantry(
        pantryItems: List<String>
    ): Pair<List<String>, Set<String>> {
        val subCategoryInfo = rawIngredientDao.getSubCategoryInfo(pantryItems)
        val substitutableSubCategories = subCategoryInfo
            .filter { it.is_substitutable }
            .map { it.sub_category }
            .distinct()

        if (substitutableSubCategories.isEmpty()) {
            return pantryItems to emptySet()
        }

        val expansionKeys = rawIngredientDao.getKeysInSubCategories(substitutableSubCategories)
        val originalSet = pantryItems.toSet()
        val substituteOnlyKeys = expansionKeys.toSet() - originalSet
        val expandedPantry = (pantryItems + substituteOnlyKeys).distinct()

        return expandedPantry to substituteOnlyKeys
    }

    /**
     * Recomputes recipe matches using a weighted readiness score algorithm.
     *
     * Uses a substitute-expanded virtual pantry so that valid substitutes
     * in the user's inventory count toward ingredient matching.
     *
     * Classification:
     * - "Ready to Cook" = all core ingredients present (optional may be missing)
     * - "Almost Ready" = readiness score >= [readinessThreshold]
     * - Hidden = readiness score below threshold
     *
     * Sorting: by readinessScore (descending), with fewest missing optionals as tiebreaker
     */
    private suspend fun recomputeRecipeMatches(pantryItems: List<String>) {
        // Store-bought dishes are always available regardless of pantry state
        _storeBoughtDishes.value = buildStoreBoughtDishResults()

        if (pantryItems.isEmpty()) {
            _readyToCookDishes.value = emptyList()
            _almostReadyDishes.value = emptyList()
            return
        }

        // Build substitute-expanded virtual pantry
        val (expandedPantry, substituteOnlyKeys) = buildExpandedPantry(pantryItems)

        val matchInfoList = pantryDao.getDishMatchCounts(expandedPantry)

        val ready = mutableListOf<DishResult>()
        val almostReady = mutableListOf<DishResult>()

        for (info in matchInfoList) {
            val allIngredients = pantryDao.getIngredientsForDish(info.dish_label)
            val details = pantryDao.getIngredientDetailsForDish(info.dish_label)
            val missingWithType = if (info.core_matched < info.core_total || info.matched_count < info.total_ingredients) {
                pantryDao.getMissingIngredients(info.dish_label, expandedPantry)
            } else {
                emptyList()
            }

            val missingCore = missingWithType.filter { it.ingredient_type == "core" }.map { it.ingredient_name }
            val missingOptional = missingWithType.filter { it.ingredient_type == "optional" }.map { it.ingredient_name }

            // Collect all ingredient keys (details + missing) for batch display name resolution
            val allMissingKeys = missingWithType.map { it.ingredient_name }.distinct()

            val nutrition = getDishNutrition(info.dish_label)

            // Resolve authoritative display names from RAW_INGREDIENTS_TABLE
            val allKeys = (details.map { it.ingredient_name } + allMissingKeys + allIngredients).distinct()
            val displayNameMap = resolveDisplayNames(allKeys)

            val ingredientInfoList = details.map { detail ->
                IngredientInfo(
                    name = displayNameMap[detail.ingredient_name] ?: detail.ingredient_name,
                    ingredientKey = detail.ingredient_name,
                    type = detail.ingredient_type,
                    category = detail.ingredient_category,
                    portionQuantity = detail.portion_quantity,
                    preparationMethod = detail.preparation_method,
                    step = detail.step
                )
            }

            // Map ingredient lists to display names for the recipe card
            val resolvedIngredients = allIngredients.map { displayNameMap[it] ?: it }
            val resolvedMissingCore = missingCore.map { displayNameMap[it] ?: it }
            val resolvedMissingOptional = missingOptional.map { displayNameMap[it] ?: it }
            val dishDisplayNames = getDishDisplayNames(info.dish_label)

            val result = DishResult(
                dishLabel = info.dish_label,
                dishName = dishDisplayNames.primaryName,
                dishNamePh = dishDisplayNames.namePh,
                dishNameEn = dishDisplayNames.nameEn,
                ingredients = resolvedIngredients,
                ingredientDetails = ingredientInfoList,
                missingCoreIngredients = resolvedMissingCore,
                missingOptionalIngredients = resolvedMissingOptional,
                coreMatchedCount = info.core_matched,
                coreTotalCount = info.core_total,
                calories = nutrition.calories,
                protein = nutrition.protein,
                carbs = nutrition.carbs,
                fats = nutrition.fats,
                fiber = nutrition.fiber,
                sugar = nutrition.sugar,
                sodium = nutrition.sodium,
                potassium = nutrition.potassium,
                vitaminA = nutrition.vitaminA,
                vitaminC = nutrition.vitaminC,
                calcium = nutrition.calcium,
                iron = nutrition.iron,
                servingSizeDescription = dishDisplayNames.servingSizeDescription,
                perServingWeightG = dishDisplayNames.perServingWeightG,
                originalServings = dishDisplayNames.servings
            )

            val score = computeReadinessScore(info)

            // Count how many dish ingredients are matched ONLY via substitutes
            val subMatchCount = allIngredients.distinct().count { it in substituteOnlyKeys }

            val scoredResult = result.copy(
                readinessScore = score,
                substituteMatchCount = subMatchCount
            )

            if (info.core_matched >= info.core_total) {
                // All core ingredients present → Ready to Cook
                ready.add(scoredResult)
            } else if (score >= readinessThreshold) {
                // Score meets threshold → Almost Ready
                almostReady.add(scoredResult)
            }
            // else: score below threshold → Hidden (not added to either list)
        }

        // Ready to Cook: sort by readiness score descending (breaks tie on optional completeness),
        // then by fewest missing optional ingredients as a secondary key
        _readyToCookDishes.value = ready.sortedWith(
            compareByDescending<DishResult> { it.readinessScore }
                .thenBy { it.missingOptionalIngredients.size }
        )

        // Almost Ready: sort by readiness score descending
        _almostReadyDishes.value = almostReady.sortedByDescending { it.readinessScore }
    }

    /**
     * Batch-resolves ingredient keys to their authoritative display names
     * from RAW_INGREDIENTS_TABLE. Falls back to the raw key if not found.
     *
     * @return Map of ingredient_key → display_name
     */
    private suspend fun resolveDisplayNames(keys: List<String>): Map<String, String> {
        if (keys.isEmpty()) return emptyMap()
        val results = rawIngredientDao.getDisplayNamesForKeys(keys)
        return results.associate { it.ingredient_key to it.display_name }
    }

    /**
     * Builds DishResult objects for store-bought dishes (ingredient_count = 0).
     * These items are always "ready" since they require no cooking/ingredients.
     */
    private suspend fun buildStoreBoughtDishResults(): List<DishResult> {
        return dishRecipeDao.getStoreBoughtDishes().map { recipe ->
            val dishDisplayNames = getDishDisplayNames(recipe.dishLabel)
            val nutrition = getDishNutrition(recipe.dishLabel)
            DishResult(
                dishLabel = recipe.dishLabel,
                dishName = dishDisplayNames.primaryName,
                dishNamePh = dishDisplayNames.namePh,
                dishNameEn = dishDisplayNames.nameEn,
                ingredients = emptyList(),
                ingredientDetails = emptyList(),
                missingCoreIngredients = emptyList(),
                missingOptionalIngredients = emptyList(),
                coreMatchedCount = 0,
                coreTotalCount = 0,
                calories = nutrition.calories,
                protein = nutrition.protein,
                carbs = nutrition.carbs,
                fats = nutrition.fats,
                fiber = nutrition.fiber,
                sugar = nutrition.sugar,
                sodium = nutrition.sodium,
                potassium = nutrition.potassium,
                vitaminA = nutrition.vitaminA,
                vitaminC = nutrition.vitaminC,
                calcium = nutrition.calcium,
                iron = nutrition.iron,
                servingSizeDescription = dishDisplayNames.servingSizeDescription,
                perServingWeightG = dishDisplayNames.perServingWeightG,
                originalServings = dishDisplayNames.servings
            )
        }
    }

    /**
     * Groups pantry items by their ingredient category from the raw ingredients table.
     * Uses RAW_INGREDIENTS_TABLE as the authoritative source for category data,
     * ensuring all 78 browsable ingredients are correctly categorized.
     * Items not found in the table are placed in "pantry_staple" by default.
     */
    private suspend fun recomputePantryCategories(items: List<String>) {
        if (items.isEmpty()) {
            _pantryItemsByCategory.value = emptyMap()
            return
        }

        val categoryMappings = rawIngredientDao.getCategoriesForKeys(items)
        val categoryMap = categoryMappings.associate { it.ingredient_key to it.category }

        val grouped = items.groupBy { ingredient ->
            categoryMap[ingredient] ?: "pantry_staple"
        }

        _pantryItemsByCategory.value = grouped
    }

    // ============================================================
    // Meal Plan Actions
    // ============================================================

    fun addMealToPlan(
        dayIndex: Int,
        dishLabel: String,
        mealSlot: String,
        weekStartDate: String = _currentWeekStart.value,
        substitutions: Map<String, String> = emptyMap(),
        scaledServings: Int = 0,
        tweaks: Map<String, Float> = emptyMap()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!isValidCopyTarget(weekStartDate, dayIndex) || mealSlot.isBlank()) {
                _uiEvents.emit(PantryUiEvent.Snackbar("Choose today or a future date."))
                return@launch
            }

            val sanitizedSubstitutions = RecipeCustomizationRules.sanitizeSubstitutions(substitutions)
            val substitutionsJson = if (sanitizedSubstitutions.isNotEmpty()) {
                org.json.JSONObject(sanitizedSubstitutions as Map<*, *>).toString()
            } else ""

            val tweaksJson = if (tweaks.isNotEmpty()) {
                org.json.JSONObject(tweaks.mapValues { it.value.toDouble() } as Map<*, *>).toString()
            } else ""

            val meal = PlannedMealEntity(
                uid = uid,
                dayIndex = dayIndex,
                dishLabel = dishLabel,
                weekStartDate = weekStartDate,
                mealSlot = mealSlot,
                substitutionsJson = substitutionsJson,
                scaledServings = scaledServings,
                tweaksJson = tweaksJson
            )
            mealPlanDao.insertMeal(meal)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.syncPlannedMeal(uid, meal) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
        }
    }

    fun removeDishFromSlot(dayIndex: Int, mealSlot: String, dishLabel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val week = _currentWeekStart.value
            if (!isDayEditableForWeek(dayIndex, week)) {
                _uiEvents.emit(PantryUiEvent.Snackbar("Past planned meals can only be viewed."))
                return@launch
            }

            mealPlanDao.removeDish(uid, dayIndex, week, mealSlot, dishLabel)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.deletePlannedMeal(uid, dayIndex, week, mealSlot, dishLabel) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
            recomputeWeekScrubberData()
        }
    }

    fun clearMealSlot(dayIndex: Int, mealSlot: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val week = _currentWeekStart.value
            if (!isDayEditableForWeek(dayIndex, week)) {
                _uiEvents.emit(PantryUiEvent.Snackbar("Past planned meals can only be viewed."))
                return@launch
            }

            mealPlanDao.clearSlot(uid, dayIndex, week, mealSlot)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.deletePlannedMealSlot(uid, dayIndex, week, mealSlot) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
            recomputeWeekScrubberData()
            _uiEvents.emit(PantryUiEvent.Snackbar("Cleared $mealSlot."))
        }
    }

    fun clearMealDay(dayIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val week = _currentWeekStart.value
            if (!isDayEditableForWeek(dayIndex, week)) {
                _uiEvents.emit(PantryUiEvent.Snackbar("Past planned meals can only be viewed."))
                return@launch
            }

            mealPlanDao.clearDay(uid, dayIndex, week)
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.clearDayPlannedMeals(uid, dayIndex, week) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
            recomputeWeekScrubberData()
            _uiEvents.emit(PantryUiEvent.Snackbar("Cleared planned meals for ${formatDateLabel(week, dayIndex)}."))
        }
    }

    fun clearMealWeek() {
        viewModelScope.launch(Dispatchers.IO) {
            val week = _currentWeekStart.value
            val weekMeals = mealPlanDao.getMealsForWeekOneShot(uid, week)
            val editableDayIndices = (0..6).filter { isDayEditableForWeek(it, week) }
            val clearableDayIndices = weekMeals
                .filter { it.dayIndex in editableDayIndices }
                .map { it.dayIndex }
                .distinct()

            if (clearableDayIndices.isEmpty()) {
                _uiEvents.emit(PantryUiEvent.Snackbar("Past planned meals can only be viewed."))
                return@launch
            }

            val clearedCount = weekMeals.count { it.dayIndex in clearableDayIndices }
            val clearedWholeWeek = editableDayIndices.size == 7
            if (clearedWholeWeek) {
                mealPlanDao.clearWeek(uid, week)
            } else {
                mealPlanDao.clearWeekDays(uid, week, editableDayIndices)
            }

            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try {
                        if (clearedWholeWeek) {
                            firestoreSyncRepo.clearWeekPlannedMeals(uid, week)
                        } else {
                            editableDayIndices.forEach { dayIndex ->
                                firestoreSyncRepo.clearDayPlannedMeals(uid, dayIndex, week)
                            }
                        }
                    } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
            recomputeWeekScrubberData()
            _uiEvents.emit(PantryUiEvent.Snackbar("Cleared $clearedCount planned dish${if (clearedCount == 1) "" else "es"}."))
        }
    }

    fun clearAllPantryItems() {
        viewModelScope.launch(Dispatchers.IO) {
            pantryDao.clearAllItems()
            if (uid.isNotEmpty()) {
                withTimeoutOrNull(5_000L) {
                    try { firestoreSyncRepo.clearPantryItems(uid) } catch (_: Exception) {}
                }
                AutoSyncManager.triggerSync(appContext, uid)
            }
        }
    }

    // ============================================================
    // Week & Month Navigation
    // ============================================================

    /**
     * Selects a week by its start date (tapping a scrubber pill).
     * Also updates the displayed month if the selected week is in a different month.
     */
    fun selectWeek(weekStartDate: String) {
        _currentWeekStart.value = weekStartDate
        val weekDate = LocalDate.parse(weekStartDate)
        val weekMonth = YearMonth.from(weekDate)
        if (weekMonth != _displayedMonth.value) {
            _displayedMonth.value = weekMonth
        }
    }

    /**
     * Navigates to the previous or next month in the scrubber.
     * No limits — users can browse any month (past or future) to view/plan meals.
     */
    fun navigateMonth(offset: Int) {
        _displayedMonth.value = _displayedMonth.value.plusMonths(offset.toLong())
    }

    /**
     * Snaps back to the current week and current month.
     */
    fun navigateToToday() {
        _currentWeekStart.value = getWeekStartDate()
        _displayedMonth.value = YearMonth.now()
    }

    /**
     * Jumps to the first month of the given year (or current month if same year).
     * Used by the year-picker dropdown in the month header.
     * No limits — users can view any year.
     */
    fun navigateToYear(year: Int) {
        val currentMonth = YearMonth.now()
        val targetMonth = if (year == currentMonth.year) {
            currentMonth // Same year — stay on current month
        } else {
            YearMonth.of(year, 1) // Different year — jump to January
        }
        _displayedMonth.value = targetMonth
    }

    /**
     * Returns whether a specific day in the displayed week is editable.
     * A day is editable if it is today or in the future.
     */
    fun isDayEditable(dayIndex: Int): Boolean {
        val weekStart = LocalDate.parse(_currentWeekStart.value)
        val dayDate = weekStart.plusDays(dayIndex.toLong())
        return !dayDate.isBefore(LocalDate.now())
    }

    fun copyCurrentWeekToNextReplacing() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sourceWeek = _currentWeekStart.value
                val sourceDate = LocalDate.parse(sourceWeek)
                val targetWeekDate = sourceDate.plusWeeks(1)
                val targetWeek = targetWeekDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                if (targetWeek < getWeekStartDate()) {
                    _uiEvents.emit(PantryUiEvent.Snackbar("Choose today or a future date."))
                    return@launch
                }

                if (targetWeek > getMaxPlanningWeekStart()) {
                    _uiEvents.emit(PantryUiEvent.Snackbar("Next week is outside your planning range."))
                    return@launch
                }

                val sourceMeals = mealPlanDao.getMealsForWeekOneShot(uid, sourceWeek)
                if (sourceMeals.isEmpty()) {
                    _uiEvents.emit(PantryUiEvent.Snackbar("No meals to copy this week."))
                    return@launch
                }

                val copiedMeals = sourceMeals.map { it.copy(weekStartDate = targetWeek) }
                mealPlanDao.replaceWeek(uid, targetWeek, copiedMeals)

                if (uid.isNotEmpty()) {
                    withTimeoutOrNull(5_000L) {
                        try {
                            firestoreSyncRepo.clearWeekPlannedMeals(uid, targetWeek)
                            firestoreSyncRepo.syncPlannedMealsBatch(uid, copiedMeals)
                        } catch (_: Exception) {}
                    }
                    AutoSyncManager.triggerSync(appContext, uid)
                }

                recomputeWeekScrubberData()
                _uiEvents.emit(PantryUiEvent.Snackbar("Copied week to ${formatWeekRange(targetWeek)}."))
            } catch (_: Exception) {
                _uiEvents.emit(PantryUiEvent.Snackbar("Could not copy meal plan. Please try again."))
            }
        }
    }

    fun copyMealSlot(
        sourceWeekStart: String,
        sourceDayIndex: Int,
        sourceMealSlot: String,
        targetWeekStart: String,
        targetDayIndex: Int,
        targetMealSlot: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!isValidCopyTarget(targetWeekStart, targetDayIndex) || targetMealSlot.isBlank()) {
                    _uiEvents.emit(PantryUiEvent.Snackbar("Choose today or a future date."))
                    return@launch
                }

                val sourceMeals = mealPlanDao.getMealsForWeekOneShot(uid, sourceWeekStart)
                    .filter { it.dayIndex == sourceDayIndex && it.mealSlot == sourceMealSlot }

                if (sourceMeals.isEmpty()) {
                    _uiEvents.emit(PantryUiEvent.Snackbar("No dishes found in this meal."))
                    return@launch
                }

                val copiedMeals = sourceMeals.map {
                    it.copy(
                        weekStartDate = targetWeekStart,
                        dayIndex = targetDayIndex,
                        mealSlot = targetMealSlot
                    )
                }

                mealPlanDao.replaceSlot(uid, targetDayIndex, targetWeekStart, targetMealSlot, copiedMeals)

                if (uid.isNotEmpty()) {
                    withTimeoutOrNull(5_000L) {
                        try {
                            firestoreSyncRepo.deletePlannedMealSlot(uid, targetDayIndex, targetWeekStart, targetMealSlot)
                            firestoreSyncRepo.syncPlannedMealsBatch(uid, copiedMeals)
                        } catch (_: Exception) {}
                    }
                    AutoSyncManager.triggerSync(appContext, uid)
                }

                recomputeWeekScrubberData()
                val targetDateLabel = formatDateLabel(targetWeekStart, targetDayIndex)
                _uiEvents.emit(PantryUiEvent.Snackbar("Copied $sourceMealSlot to $targetDateLabel $targetMealSlot."))
            } catch (_: Exception) {
                _uiEvents.emit(PantryUiEvent.Snackbar("Could not copy meal plan. Please try again."))
            }
        }
    }

    fun copySingleDish(
        sourceMeal: PlannedMealEntity,
        targetWeekStart: String,
        targetDayIndex: Int,
        targetMealSlot: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!isValidCopyTarget(targetWeekStart, targetDayIndex) || targetMealSlot.isBlank()) {
                    _uiEvents.emit(PantryUiEvent.Snackbar("Choose today or a future date."))
                    return@launch
                }

                val copiedMeal = sourceMeal.copy(
                    weekStartDate = targetWeekStart,
                    dayIndex = targetDayIndex,
                    mealSlot = targetMealSlot
                )

                mealPlanDao.insertMeal(copiedMeal)

                if (uid.isNotEmpty()) {
                    withTimeoutOrNull(5_000L) {
                        try { firestoreSyncRepo.syncPlannedMeal(uid, copiedMeal) } catch (_: Exception) {}
                    }
                    AutoSyncManager.triggerSync(appContext, uid)
                }

                recomputeWeekScrubberData()
                val targetDateLabel = formatDateLabel(targetWeekStart, targetDayIndex)
                _uiEvents.emit(PantryUiEvent.Snackbar("Copied ${formatIngredientName(sourceMeal.dishLabel)} to $targetDateLabel $targetMealSlot."))
            } catch (_: Exception) {
                _uiEvents.emit(PantryUiEvent.Snackbar("Could not copy meal plan. Please try again."))
            }
        }
    }

    // ============================================================
    // Public Helpers for Recipe Card "Add to Meal Plan" Dialog
    // ============================================================

    /**
     * One-shot fetch of meals for a specific week.
     * Used by the Recipe Card dialog for accurate duplicate detection.
     */
    suspend fun getPlannedMealsForWeekSnapshot(weekStartDate: String): List<PlannedMealEntity> {
        return mealPlanDao.getMealsForWeekOneShot(uid, weekStartDate)
    }

    /**
     * Returns whether a specific day in a given week is editable (today or future).
     * Used by the Recipe Card dialog which may target a different week.
     */
    fun isDayEditableForWeek(dayIndex: Int, weekStartDate: String): Boolean {
        val weekStart = LocalDate.parse(weekStartDate)
        val dayDate = weekStart.plusDays(dayIndex.toLong())
        return !dayDate.isBefore(LocalDate.now())
    }

    /**
     * Computes day-of-month values for a given week start date.
     * Public wrapper for use by the Recipe Card dialog.
     */
    fun computeWeekDayDatesPublic(weekStart: String): List<Pair<String, Int>> {
        return computeWeekDayDates(weekStart)
    }

    /**
     * Returns the furthest Monday users can plan ahead to.
     * Public wrapper for use by the Recipe Card dialog's week navigator.
     */
    fun getMaxPlanningWeekStartPublic(): String = getMaxPlanningWeekStart()

    /**
     * Returns the Monday of the current (real-world) week.
     * Public wrapper for use by the Recipe Card dialog's week navigator boundary.
     */
    fun getCurrentWeekStartDate(): String = getWeekStartDate()

    // ============================================================
    // Planned Dish Detail Lookups (for Meal Plan Calendar)
    // ============================================================

    /**
     * Compact nutrition summary for inline display in the Meal Detail Dialog.
     */
    data class CompactDishNutrition(
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fats: Int
    )

    /**
     * Returns compact nutrition info for a dish.
     * Used for inline display in the Meal Detail Dialog without loading full ingredient details.
     * When [substitutionsJson] is non-empty, nutrition is recalculated with the substitutions.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun getCompactNutrition(
        dishLabel: String,
        substitutionsJson: String = "",
        scaledServings: Int = 0,
        tweaksJson: String = ""
    ): CompactDishNutrition {
        val subs = parseSubstitutionsJson(substitutionsJson)
        val tweaks = parseTweaksJson(tweaksJson)
        return if (tweaks.isNotEmpty()) {
            val r = calculator.calculateWithTweaks(dishLabel, tweaks, subs).first
            CompactDishNutrition(r.calories.toInt(), r.protein.toInt(), r.carbs.toInt(), r.fat.toInt())
        } else if (subs.isNotEmpty()) {
            val r = calculator.calculateWithSubstitution(dishLabel, subs)
            CompactDishNutrition(r.calories.toInt(), r.protein.toInt(), r.carbs.toInt(), r.fat.toInt())
        } else {
            val n = getDishNutrition(dishLabel)
            CompactDishNutrition(n.calories, n.protein, n.carbs, n.fats)
        }
    }

    /**
     * Constructs a full DishResult for a given dish label.
     * Used to view planned dish details from the Meal Plan Calendar.
     * Returns null if the dish doesn't exist in the recipe database.
     *
     * When [substitutionsJson] is non-empty, ingredient names are swapped
     * to reflect the substituted versions and nutrition is recalculated
     * via the RecipeNutritionCalculator.
     */
    suspend fun getDishResultByLabel(
        dishLabel: String,
        substitutionsJson: String = "",
        scaledServings: Int = 0,
        tweaksJson: String = ""
    ): DishResult? {
        val subs = parseSubstitutionsJson(substitutionsJson)
        val tweaks = parseTweaksJson(tweaksJson)
        val allIngredients = pantryDao.getIngredientsForDish(dishLabel)

        // For dishes with no ingredients (store-bought), build from DISH_RECIPES_TABLE directly
        if (allIngredients.isEmpty()) {
            val recipe = dishRecipeDao.getByDishLabel(dishLabel) ?: return null
            val dishDisplayNames = getDishDisplayNames(dishLabel)
            val nutrition = getDishNutrition(dishLabel)
            return DishResult(
                dishLabel = dishLabel,
                dishName = dishDisplayNames.primaryName,
                dishNamePh = dishDisplayNames.namePh,
                dishNameEn = dishDisplayNames.nameEn,
                ingredients = emptyList(),
                ingredientDetails = emptyList(),
                missingCoreIngredients = emptyList(),
                missingOptionalIngredients = emptyList(),
                coreMatchedCount = 0,
                coreTotalCount = recipe.ingredientCount,
                calories = nutrition.calories,
                protein = nutrition.protein,
                carbs = nutrition.carbs,
                fats = nutrition.fats,
                fiber = nutrition.fiber,
                sugar = nutrition.sugar,
                sodium = nutrition.sodium,
                potassium = nutrition.potassium,
                vitaminA = nutrition.vitaminA,
                vitaminC = nutrition.vitaminC,
                calcium = nutrition.calcium,
                iron = nutrition.iron,
                servingSizeDescription = dishDisplayNames.servingSizeDescription,
                originalServings = dishDisplayNames.servings,
                appliedSubstitutions = subs,
                appliedScaledServings = scaledServings,
                appliedTweaks = tweaks
            )
        }

        val details = pantryDao.getIngredientDetailsForDish(dishLabel)

        val ingredientInfoList = run {
            // Resolve display names for original and substituted ingredient keys.
            val allKeys = details.flatMap { detail ->
                val mapped = subs[detail.ingredient_name]
                listOfNotNull(
                    detail.ingredient_name,
                    mapped?.takeUnless { it == REMOVED_INGREDIENT }
                )
            }.distinct()
            val displayNameMap = resolveDisplayNames(allKeys)

            details.map { detail ->
                val mapped = subs[detail.ingredient_name] ?: detail.ingredient_name
                val isRemoved = mapped == REMOVED_INGREDIENT
                val replacementKey = mapped.takeUnless { isRemoved || it == detail.ingredient_name }
                IngredientInfo(
                    name = displayNameMap[detail.ingredient_name] ?: detail.ingredient_name,
                    ingredientKey = detail.ingredient_name,
                    type = detail.ingredient_type,
                    category = detail.ingredient_category,
                    portionQuantity = detail.portion_quantity,
                    preparationMethod = detail.preparation_method,
                    step = detail.step,
                    replacementIngredientKey = replacementKey,
                    replacementName = replacementKey?.let { displayNameMap[it] ?: it },
                    isRemoved = isRemoved
                )
            }
        }

        val finalIngredients = ingredientInfoList
            .filterNot { it.isRemoved }
            .map { it.replacementName ?: it.name }

        val tweakedResult = if (tweaks.isNotEmpty()) {
            calculator.calculateWithTweaks(dishLabel, tweaks, subs)
        } else {
            null
        }

        // Use recalculated nutrition when substitutions or tweaks are present
        val nutrition = if (tweakedResult != null) {
            tweakedResult.first.toDishNutritionInfo()
        } else if (subs.isNotEmpty()) {
            calculator.calculateWithSubstitution(dishLabel, subs).toDishNutritionInfo()
        } else {
            getDishNutrition(dishLabel)
        }

        val dishDisplayNames = getDishDisplayNames(dishLabel)

        // Get actual ingredient count from recipe entity for accurate UI rendering
        val recipeEntity = dishRecipeDao.getByDishLabel(dishLabel)
        val actualIngredientCount = recipeEntity?.ingredientCount ?: details.size
        val appliedTweakedPerServingWeight = if (tweakedResult != null) {
            calculateTweakedPerServingWeight(dishLabel, tweakedResult.second)
        } else {
            0f
        }

        val result = DishResult(
            dishLabel = dishLabel,
            dishName = dishDisplayNames.primaryName,
            dishNamePh = dishDisplayNames.namePh,
            dishNameEn = dishDisplayNames.nameEn,
            ingredients = finalIngredients,
            ingredientDetails = ingredientInfoList,
            missingCoreIngredients = emptyList(),
            missingOptionalIngredients = emptyList(),
            coreMatchedCount = actualIngredientCount,
            coreTotalCount = actualIngredientCount,
            calories = nutrition.calories,
            protein = nutrition.protein,
            carbs = nutrition.carbs,
            fats = nutrition.fats,
            fiber = nutrition.fiber,
            sugar = nutrition.sugar,
            sodium = nutrition.sodium,
            potassium = nutrition.potassium,
            vitaminA = nutrition.vitaminA,
            vitaminC = nutrition.vitaminC,
            calcium = nutrition.calcium,
            iron = nutrition.iron,
            servingSizeDescription = dishDisplayNames.servingSizeDescription,
            originalServings = dishDisplayNames.servings,
            perServingWeightG = dishDisplayNames.perServingWeightG,
            appliedSubstitutions = subs,
            appliedScaledServings = scaledServings,
            appliedTweaks = tweaks,
            appliedTweakedPerServingWeightG = appliedTweakedPerServingWeight
        )

        return result
    }

    /**
     * Parses a substitutions JSON string into a Map<String, String>.
     * Returns empty map if the string is blank or malformed.
     */
    private fun parseSubstitutionsJson(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        return try {
            val obj = org.json.JSONObject(json)
            val map = mutableMapOf<String, String>()
            obj.keys().forEach { key -> map[key] = obj.getString(key) }
            RecipeCustomizationRules.sanitizeSubstitutions(map)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Parses a tweaks JSON string into a Map<String, Float>.
     * Returns empty map if the string is blank or malformed.
     */
    private fun parseTweaksJson(json: String): Map<String, Float> {
        if (json.isBlank()) return emptyMap()
        return try {
            val obj = org.json.JSONObject(json)
            val map = mutableMapOf<String, Float>()
            obj.keys().forEach { key -> map[key] = obj.getDouble(key).toFloat() }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // ============================================================
    // Weekly Stats
    // ============================================================

    private suspend fun recomputeWeeklyStats(meals: List<PlannedMealEntity>) {
        if (meals.isEmpty()) {
            _weeklyCalories.value = 0
            _avgDailySodium.value = 0
            return
        }

        var totalCalories = 0
        var totalSodium = 0

        for (meal in meals) {
            val substitutions = parseSubstitutionsJson(meal.substitutionsJson)
            val tweaks = parseTweaksJson(meal.tweaksJson)
            if (tweaks.isNotEmpty()) {
                val nutrition = calculator.calculateWithTweaks(meal.dishLabel, tweaks, substitutions).first
                totalCalories += nutrition.calories.toInt()
                totalSodium += nutrition.sodium.toInt()
            } else if (substitutions.isNotEmpty()) {
                val nutrition = calculator.calculateWithSubstitution(meal.dishLabel, substitutions)
                totalCalories += nutrition.calories.toInt()
                totalSodium += nutrition.sodium.toInt()
            } else {
                val nutrition = getDishNutrition(meal.dishLabel)
                totalCalories += nutrition.calories
                totalSodium += nutrition.sodium
            }
        }

        _weeklyCalories.value = totalCalories
        val daysWithMeals = meals.map { it.dayIndex }.distinct().size
        _avgDailySodium.value = if (daysWithMeals > 0) totalSodium / daysWithMeals else 0
    }

    // ============================================================
    // Meal Plan Completion Status Derivation
    // ============================================================

    /**
     * Observes logged dish names for the given week from MealLogDao.
     * Cancels any previous observation job and starts a new one.
     * Populates [_weekLoggedDishes] with normalized (slot, dishName) pairs.
     */
    private fun observeLoggedDishesForWeek(weekStart: String) {
        weekLogObservationJob?.cancel()
        val currentUid = uid
        if (currentUid.isEmpty()) {
            _weekLoggedDishes.value = emptySet()
            return
        }

        val weekStartDate = LocalDate.parse(weekStart)
        val startEpoch = weekStartDate.atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val endEpoch = weekStartDate.plusDays(7).atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        weekLogObservationJob = viewModelScope.launch {
            mealLogDao.observeLoggedDishNames(currentUid, startEpoch, endEpoch).collect { refs ->
                _weekLoggedDishes.value = refs.map {
                    normalizeSlotName(it.mealType) to normalizeDishName(it.dishName)
                }.toSet()
            }
        }
    }

    /**
     * Derives per-cell completion status.
     * Priority: persisted schema status (Phase 2) > read-time derivation (Phase 1 fallback).
     */
    private fun deriveCellCompletionStatus(
        planned: List<PlannedMealEntity>,
        loggedSet: Set<Pair<String, String>>
    ): Map<String, CellCompletionStatus> {
        val result = mutableMapOf<String, CellCompletionStatus>()
        val grouped = planned.groupBy { "${it.dayIndex}_${it.mealSlot}" }

        for ((cellKey, dishes) in grouped) {
            val dayIndex = cellKey.substringBefore("_").toInt()
            val slot = cellKey.substringAfter("_")

            // Phase 2: Check for explicit persisted statuses first
            val explicitStatuses = dishes.mapNotNull { meal ->
                when (meal.status) {
                    "logged" -> CellCompletionStatus.LOGGED
                    "skipped" -> CellCompletionStatus.SKIPPED
                    "missed" -> CellCompletionStatus.MISSED
                    else -> null  // "planned" → fall through to derivation
                }
            }

            if (explicitStatuses.isNotEmpty()) {
                // Use highest-priority explicit status: LOGGED > SKIPPED > MISSED
                result[cellKey] = explicitStatuses.minByOrNull { it.ordinal }
                    ?: CellCompletionStatus.PLANNED
            } else {
                // Phase 1 fallback: read-time derivation for pre-migration data
                val normalizedSlot = normalizeSlotName(slot)
                val matchCount = dishes.count { meal ->
                    (normalizedSlot to normalizeDishName(meal.dishLabel)) in loggedSet
                }
                val isPast = !isDayEditable(dayIndex)
                result[cellKey] = when {
                    matchCount >= dishes.size -> CellCompletionStatus.LOGGED
                    matchCount > 0 -> CellCompletionStatus.LOGGED
                    isPast -> CellCompletionStatus.MISSED
                    else -> CellCompletionStatus.PLANNED
                }
            }
        }
        return result
    }

    /** Skips an entire meal slot (sets status = "skipped" for all dishes in the slot). */
    fun skipSlot(dayIndex: Int, mealSlot: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mealPlanRepository.skipSlot(uid, dayIndex, _currentWeekStart.value, mealSlot)
            }
        }
    }

    /** Reverts a skipped meal slot back to "planned" status. */
    fun unskipSlot(dayIndex: Int, mealSlot: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mealPlanRepository.unskipSlot(uid, dayIndex, _currentWeekStart.value, mealSlot)
            }
        }
    }

    /** Normalizes slot names: "Snacks" → "snack", "Breakfast" → "breakfast" */
    private fun normalizeSlotName(slot: String): String =
        slot.lowercase().trimEnd('s')

    /** Normalizes dish names: "chicken_adobo" ↔ "Chicken Adobo" → "chicken adobo" */
    private fun normalizeDishName(name: String): String =
        name.lowercase().replace("_", " ").trim()

    // ============================================================
    // Helpers
    // ============================================================

    private suspend fun getDishNutrition(dishLabel: String): DishNutritionInfo {
        _dishNutritionCache[dishLabel]?.let { return it }
        val recipe = dishRecipeDao.getByDishLabel(dishLabel)
        val info = if (recipe != null) {
            DishNutritionInfo(
                calories = recipe.calPerServing.toInt(),
                protein = recipe.proteinPerServing.toInt(),
                carbs = recipe.carbsPerServing.toInt(),
                fats = recipe.fatPerServing.toInt(),
                fiber = recipe.fiberPerServing,
                sugar = recipe.sugarPerServing,
                sodium = recipe.sodiumPerServing.toInt(),
                potassium = recipe.potassiumPerServing,
                vitaminA = recipe.vitaminAPerServing,
                vitaminC = recipe.vitaminCPerServing,
                calcium = recipe.calciumPerServing,
                iron = recipe.ironPerServing
            )
        } else {
            DishNutritionInfo(0, 0, 0, 0, 0f, 0f, 0, 0f, 0f, 0f, 0f, 0f)
        }
        _dishNutritionCache[dishLabel] = info
        return info
    }

    private suspend fun getDishDisplayNames(dishLabel: String): DishDisplayNames {
        _dishDisplayNameCache[dishLabel]?.let { return it }
        val recipe = dishRecipeDao.getByDishLabel(dishLabel)
        val names = if (recipe != null) {
            DishDisplayNames(
                namePh = recipe.namePh,
                nameEn = recipe.nameEn,
                servingSizeDescription = recipe.servingSizeDescription,
                perServingWeightG = recipe.perServingWeightG,
                servings = recipe.servings
            )
        } else {
            val fallback = formatDishName(dishLabel)
            DishDisplayNames(namePh = fallback, nameEn = "")
        }
        _dishDisplayNameCache[dishLabel] = names
        return names
    }

    private fun formatDishName(label: String): String {
        return label.split("_").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun isValidCopyTarget(weekStartDate: String, dayIndex: Int): Boolean {
        if (dayIndex !in 0..6) return false
        val targetDate = LocalDate.parse(weekStartDate).plusDays(dayIndex.toLong())
        return !targetDate.isBefore(LocalDate.now()) && weekStartDate <= getMaxPlanningWeekStart()
    }

    private fun formatDateLabel(weekStartDate: String, dayIndex: Int): String {
        val targetDate = LocalDate.parse(weekStartDate).plusDays(dayIndex.toLong())
        return targetDate.format(DateTimeFormatter.ofPattern("MMM d"))
    }

    private fun formatWeekRange(weekStartDate: String): String {
        val start = LocalDate.parse(weekStartDate)
        val end = start.plusDays(6)
        val startFormatter = DateTimeFormatter.ofPattern("MMM d")
        return if (start.month == end.month) {
            "${start.format(startFormatter)}-${end.dayOfMonth}"
        } else {
            "${start.format(startFormatter)}-${end.format(startFormatter)}"
        }
    }

    private fun getWeekStartDate(): String {
        return LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun getMaxPlanningWeekStart(): String {
        return LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(8)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun computeWeekDayDates(weekStart: String): List<Pair<String, Int>> {
        val startDate = LocalDate.parse(weekStart)
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return dayNames.mapIndexed { index, name ->
            name to startDate.plusDays(index.toLong()).dayOfMonth
        }
    }

    private fun computeTodayColumnIndex(weekStart: String): Int? {
        val startDate = LocalDate.parse(weekStart)
        val today = LocalDate.now()
        val daysBetween = ChronoUnit.DAYS.between(startDate, today).toInt()
        return if (daysBetween in 0..6) daysBetween else null
    }

    private fun computeWeeksInMonth(yearMonth: YearMonth): List<String> {
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()
        val firstMonday = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStarts = mutableListOf<String>()
        var current = firstMonday
        while (!current.isAfter(lastDayOfMonth)) {
            weekStarts.add(current.format(DateTimeFormatter.ISO_LOCAL_DATE))
            current = current.plusWeeks(1)
        }
        return weekStarts
    }

    private suspend fun recomputeWeekScrubberData() {
        val month = _displayedMonth.value
        val weekStartDates = computeWeeksInMonth(month)
        val currentWeek = getWeekStartDate()
        val maxWeek = getMaxPlanningWeekStart()
        val mealCounts = if (weekStartDates.isNotEmpty()) {
            mealPlanDao.getMealCountsForWeeks(uid, weekStartDates)
                .associate { it.weekStartDate to it.count }
        } else emptyMap()
        val labelFormatter = DateTimeFormatter.ofPattern("MMM d")
        _weeksInMonth.value = weekStartDates.map { weekStart ->
            val start = LocalDate.parse(weekStart)
            val end = start.plusDays(6)
            val label = "${start.format(labelFormatter)} \u2013 ${end.dayOfMonth}"
            WeekInfo(
                weekStartDate = weekStart,
                label = label,
                mealCount = mealCounts[weekStart] ?: 0,
                isCurrentWeek = weekStart == currentWeek,
                isPast = weekStart < currentWeek,
                isBeyondHorizon = weekStart > maxWeek
            )
        }
    }

    fun formatIngredientName(name: String): String {
        _displayNameCache[name]?.let { return it }
        return name.split("_").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    // ============================================================
    // Recipe Serving Scaling
    // ============================================================

    private val _scaledServings = MutableStateFlow<Map<String, Int>>(emptyMap())
    val scaledServings: StateFlow<Map<String, Int>> = _scaledServings.asStateFlow()

    fun setTargetServings(dishLabel: String, target: Int) {
        val current = _scaledServings.value.toMutableMap()
        current[dishLabel] = target.coerceAtLeast(1)
        _scaledServings.value = current
    }

    fun resetTargetServings(dishLabel: String) {
        val current = _scaledServings.value.toMutableMap()
        current.remove(dishLabel)
        _scaledServings.value = current
    }

    // ============================================================
    // Ingredient Substitution
    // ============================================================

    private val _activeSubstitutions = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val activeSubstitutions: StateFlow<Map<String, Map<String, String>>> = _activeSubstitutions.asStateFlow()

    private val _substitutedNutrition = MutableStateFlow<Map<String, NutritionResult>>(emptyMap())
    val substitutedNutrition: StateFlow<Map<String, NutritionResult>> = _substitutedNutrition.asStateFlow()

    suspend fun getSubstitutesForIngredient(ingredientKey: String): List<RawIngredientEntity> {
        val ingredient = rawIngredientDao.getByKey(ingredientKey) ?: return emptyList()
        return rawIngredientDao.getSubstituteCandidates(ingredient.subCategory, ingredientKey)
    }

    /**
     * Returns per-ingredient nutrition breakdown for a dish.
     * Each entry shows how many calories/protein/fat/carbs that ingredient contributes.
     */
    suspend fun getIngredientBreakdown(
        dishLabel: String,
        substitutions: Map<String, String> = emptyMap()
    ): Map<String, IngredientNutritionBreakdown> {
        return calculator.getIngredientBreakdown(
            dishLabel,
            RecipeCustomizationRules.sanitizeSubstitutions(substitutions)
        )
    }

    /**
     * Applies a substitution: replaces [originalKey] with [newKey] in the dish recipe
     * and recalculates nutrition using the dynamic path.
     */
    fun applySubstitution(dishLabel: String, originalKey: String, newKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (newKey == REMOVED_INGREDIENT && RecipeCustomizationRules.isProtectedBaseIngredient(originalKey)) {
                _uiEvents.emit(PantryUiEvent.Snackbar("This base ingredient is required for nutrition calculations."))
                return@launch
            }

            // Update the substitution map
            val currentSubs = _activeSubstitutions.value.toMutableMap()
            val dishSubs = (currentSubs[dishLabel] ?: emptyMap()).toMutableMap()
            dishSubs[originalKey] = newKey
            val sanitizedDishSubs = RecipeCustomizationRules.sanitizeSubstitutions(dishSubs)
            if (sanitizedDishSubs.isEmpty()) {
                currentSubs.remove(dishLabel)
            } else {
                currentSubs[dishLabel] = sanitizedDishSubs
            }
            _activeSubstitutions.value = currentSubs

            // Recalculate with substitutions
            val result = calculator.calculateWithSubstitution(dishLabel, sanitizedDishSubs)
            val currentNutrition = _substitutedNutrition.value.toMutableMap()
            if (sanitizedDishSubs.isEmpty()) {
                currentNutrition.remove(dishLabel)
            } else {
                currentNutrition[dishLabel] = result
            }
            _substitutedNutrition.value = currentNutrition
        }
    }

    /**
     * Removes a specific substitution for an ingredient in a dish.
     * If no more substitutions remain, removes the dish entry entirely.
     */
    fun removeSubstitution(dishLabel: String, originalKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSubs = _activeSubstitutions.value.toMutableMap()
            val dishSubs = (currentSubs[dishLabel] ?: emptyMap()).toMutableMap()
            dishSubs.remove(originalKey)
            val sanitizedDishSubs = RecipeCustomizationRules.sanitizeSubstitutions(dishSubs)

            if (sanitizedDishSubs.isEmpty()) {
                currentSubs.remove(dishLabel)
                val currentNutrition = _substitutedNutrition.value.toMutableMap()
                currentNutrition.remove(dishLabel)
                _substitutedNutrition.value = currentNutrition
            } else {
                currentSubs[dishLabel] = sanitizedDishSubs
                val result = calculator.calculateWithSubstitution(dishLabel, sanitizedDishSubs)
                val currentNutrition = _substitutedNutrition.value.toMutableMap()
                currentNutrition[dishLabel] = result
                _substitutedNutrition.value = currentNutrition
            }

            _activeSubstitutions.value = currentSubs
        }
    }

    /**
     * Clears all substitutions for a dish, resetting nutrition to the original values.
     */
    fun clearSubstitutions(dishLabel: String) {
        val currentSubs = _activeSubstitutions.value.toMutableMap()
        currentSubs.remove(dishLabel)
        _activeSubstitutions.value = currentSubs

        val currentNutrition = _substitutedNutrition.value.toMutableMap()
        currentNutrition.remove(dishLabel)
        _substitutedNutrition.value = currentNutrition
    }

    /**
     * Removes an optional ingredient from the dish by marking it as [REMOVED_INGREDIENT]
     * in the substitution map and recalculating nutrition.
     * Undo is handled by [removeSubstitution] — same as any other substitution.
     */
    fun removeIngredient(dishLabel: String, ingredientKey: String) {
        if (RecipeCustomizationRules.isProtectedBaseIngredient(ingredientKey)) return
        applySubstitution(dishLabel, ingredientKey, REMOVED_INGREDIENT)
    }

    // ============================================================
    // Individual Ingredient Tweaking
    // ============================================================

    /** Per-ingredient multipliers: dishLabel → (ingredientKey → multiplier). */
    private val _ingredientTweaks = MutableStateFlow<Map<String, Map<String, Float>>>(emptyMap())
    val ingredientTweaks: StateFlow<Map<String, Map<String, Float>>> = _ingredientTweaks.asStateFlow()

    /** Tweaked per-serving nutrition (replaces base/sub nutrition when tweaks are active). */
    private val _tweakedNutrition = MutableStateFlow<Map<String, NutritionResult>>(emptyMap())
    val tweakedNutrition: StateFlow<Map<String, NutritionResult>> = _tweakedNutrition.asStateFlow()

    /** Estimated per-serving weight in grams when tweaks alter the raw weight total. */
    private val _tweakedPerServingWeight = MutableStateFlow<Map<String, Float>>(emptyMap())
    val tweakedPerServingWeight: StateFlow<Map<String, Float>> = _tweakedPerServingWeight.asStateFlow()

    /** Debounce job for tweak recalculations — cancelled and restarted on each slider move. */
    private var tweakRecalcJob: Job? = null

    /**
     * Sets the multiplier for a specific ingredient in a dish.
     *
     * The tweak map update is immediate (slider stays responsive), but the
     * expensive nutrition recalculation is debounced by 150ms to avoid
     * Room query thrashing during rapid slider adjustments.
     *
     * @param dishLabel the dish being customized
     * @param ingredientKey the ingredient being tweaked
     * @param multiplier the tweak multiplier (1.0 = original, 2.0 = double, etc.)
     */
    fun setIngredientTweak(dishLabel: String, ingredientKey: String, multiplier: Float) {
        // Update the tweak map immediately so the UI slider stays responsive
        val current = _ingredientTweaks.value.toMutableMap()
        val dishTweaks = (current[dishLabel] ?: emptyMap()).toMutableMap()

        if (multiplier == 1f) dishTweaks.remove(ingredientKey)
        else dishTweaks[ingredientKey] = multiplier

        if (dishTweaks.isEmpty()) current.remove(dishLabel)
        else current[dishLabel] = dishTweaks

        _ingredientTweaks.value = current

        // Debounce the expensive recalculation
        tweakRecalcJob?.cancel()
        tweakRecalcJob = viewModelScope.launch(Dispatchers.IO) {
            delay(150L)
            recalculateTweakedNutrition(dishLabel)
        }
    }

    /**
     * Resets a single ingredient tweak back to 1× (original).
     */
    fun resetIngredientTweak(dishLabel: String, ingredientKey: String) {
        setIngredientTweak(dishLabel, ingredientKey, 1f)
    }

    /**
     * Clears all ingredient tweaks for a dish, resetting to original recipe.
     */
    fun clearIngredientTweaks(dishLabel: String) {
        val current = _ingredientTweaks.value.toMutableMap()
        current.remove(dishLabel)
        _ingredientTweaks.value = current

        val currentNutrition = _tweakedNutrition.value.toMutableMap()
        currentNutrition.remove(dishLabel)
        _tweakedNutrition.value = currentNutrition

        val currentWeights = _tweakedPerServingWeight.value.toMutableMap()
        currentWeights.remove(dishLabel)
        _tweakedPerServingWeight.value = currentWeights
    }

    /**
     * Runs the full recalculation for a tweaked dish.
     * Called after the debounce delay elapses.
     */
    private suspend fun recalculateTweakedNutrition(dishLabel: String) {
        val tweaks = _ingredientTweaks.value[dishLabel] ?: emptyMap()
        val subs = RecipeCustomizationRules.sanitizeSubstitutions(
            _activeSubstitutions.value[dishLabel] ?: emptyMap()
        )

        if (tweaks.isEmpty()) {
            // No tweaks left — clear the tweaked state
            val currentNutrition = _tweakedNutrition.value.toMutableMap()
            currentNutrition.remove(dishLabel)
            _tweakedNutrition.value = currentNutrition

            val currentWeights = _tweakedPerServingWeight.value.toMutableMap()
            currentWeights.remove(dishLabel)
            _tweakedPerServingWeight.value = currentWeights
            return
        }

        val (perServingNutrition, newTotalRawWeightG) =
            calculator.calculateWithTweaks(dishLabel, tweaks, subs)

        // Emit tweaked nutrition
        val currentNutrition = _tweakedNutrition.value.toMutableMap()
        currentNutrition[dishLabel] = perServingNutrition
        _tweakedNutrition.value = currentNutrition

        // Estimate new per-serving weight using the yield factor
        val dish = dishRecipeDao.getByDishLabel(dishLabel) ?: return
        val yieldFactor = if (dish.dishYieldFactor > 0f) dish.dishYieldFactor else 1f
        val newCookedWeight = newTotalRawWeightG * yieldFactor
        val newPerServingWeight = newCookedWeight / dish.servings.coerceAtLeast(1)

        val currentWeights = _tweakedPerServingWeight.value.toMutableMap()
        currentWeights[dishLabel] = newPerServingWeight
        _tweakedPerServingWeight.value = currentWeights
    }

    private suspend fun calculateTweakedPerServingWeight(dishLabel: String, totalRawWeightG: Float): Float {
        val dish = dishRecipeDao.getByDishLabel(dishLabel) ?: return 0f
        val yieldFactor = if (dish.dishYieldFactor > 0f) dish.dishYieldFactor else 1f
        return (totalRawWeightG * yieldFactor) / dish.servings.coerceAtLeast(1)
    }

    private fun NutritionResult.toDishNutritionInfo(): DishNutritionInfo =
        DishNutritionInfo(
            calories = calories.toInt(),
            protein = protein.toInt(),
            carbs = carbs.toInt(),
            fats = fat.toInt(),
            fiber = fiber,
            sugar = sugar,
            sodium = sodium.toInt(),
            potassium = potassium,
            vitaminA = vitaminA,
            vitaminC = vitaminC,
            calcium = calcium,
            iron = iron
        )
}
