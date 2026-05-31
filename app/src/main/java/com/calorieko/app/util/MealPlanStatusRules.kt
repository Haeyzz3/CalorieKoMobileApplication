package com.calorieko.app.util

import com.calorieko.app.data.model.PlannedMealEntity

enum class MealPlanSlotStatus { LOGGED, SKIPPED, MISSED, PLANNED }

object MealPlanStatusRules {
    fun normalizeSlotName(slot: String): String =
        slot.lowercase().trimEnd('s')

    fun normalizeDishName(name: String): String =
        name.lowercase().replace("_", " ").trim()

    fun deriveSlotStatus(
        dishes: List<PlannedMealEntity>,
        loggedSet: Set<Pair<String, String>>,
        mealSlot: String,
        isPast: Boolean
    ): MealPlanSlotStatus {
        val explicitStatuses = dishes.mapNotNull { meal ->
            when (meal.status) {
                "logged" -> MealPlanSlotStatus.LOGGED
                "skipped" -> MealPlanSlotStatus.SKIPPED
                "missed" -> MealPlanSlotStatus.MISSED
                else -> null
            }
        }

        if (explicitStatuses.isNotEmpty()) {
            return explicitStatuses.minByOrNull { it.ordinal } ?: MealPlanSlotStatus.PLANNED
        }

        val normalizedSlot = normalizeSlotName(mealSlot)
        val matchCount = dishes.count { meal ->
            (normalizedSlot to normalizeDishName(meal.dishLabel)) in loggedSet
        }

        return when {
            matchCount > 0 -> MealPlanSlotStatus.LOGGED
            isPast -> MealPlanSlotStatus.MISSED
            else -> MealPlanSlotStatus.PLANNED
        }
    }
}
