package com.calorieko.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.remote.api.AutoSyncManager
import com.calorieko.app.data.remote.firestore.FirestoreAutoSyncManager
import com.calorieko.app.data.remote.firestore.FirestoreEntityType
import com.calorieko.app.data.remote.firestore.FirestorePayloadSerializer

class MealPlanRepository(
    private val db: AppDatabase,
    private val appContext: Context
) {
    private val mealPlanDao = db.mealPlanDao()
    private val outboxDao = db.firestoreOutboxDao()

    suspend fun upsertMeal(uid: String, meal: PlannedMealEntity) {
        if (uid.isBlank()) {
            mealPlanDao.insertMeal(meal)
            return
        }
        val now = System.currentTimeMillis()
        db.withTransaction {
            mealPlanDao.insertMeal(meal)
            outboxDao.insert(upsertOperation(uid, meal, now))
        }
        triggerSync(uid)
    }

    suspend fun removeDish(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String, dishLabel: String) {
        if (uid.isBlank()) {
            mealPlanDao.removeDish(dayIndex, weekStartDate, mealSlot, dishLabel)
            return
        }
        val now = System.currentTimeMillis()
        db.withTransaction {
            val meal = PlannedMealEntity(
                dayIndex = dayIndex,
                dishLabel = dishLabel,
                weekStartDate = weekStartDate,
                mealSlot = mealSlot
            )
            outboxDao.insert(
                FirestorePayloadSerializer.deleteDocument(
                    uid = uid,
                    entityType = FirestoreEntityType.PLANNED_MEAL,
                    entityKey = FirestorePayloadSerializer.plannedMealDocumentId(meal),
                    remotePath = FirestorePayloadSerializer.plannedMealPath(uid, meal),
                    now = now
                )
            )
            mealPlanDao.removeDish(dayIndex, weekStartDate, mealSlot, dishLabel)
        }
        triggerSync(uid)
    }

    suspend fun clearSlot(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String) {
        if (uid.isBlank()) {
            mealPlanDao.clearSlot(dayIndex, weekStartDate, mealSlot)
            return
        }
        val now = System.currentTimeMillis()
        db.withTransaction {
            outboxDao.insert(
                plannedMealClearOperation(
                    uid = uid,
                    collectionKey = "plannedMeals:$weekStartDate:$dayIndex:$mealSlot",
                    filters = mapOf(
                        "weekStartDate" to weekStartDate,
                        "dayIndex" to dayIndex,
                        "mealSlot" to mealSlot
                    ),
                    now = now
                )
            )
            mealPlanDao.clearSlot(dayIndex, weekStartDate, mealSlot)
        }
        triggerSync(uid)
    }

    suspend fun clearDay(uid: String, dayIndex: Int, weekStartDate: String) {
        if (uid.isBlank()) {
            mealPlanDao.clearDay(dayIndex, weekStartDate)
            return
        }
        val now = System.currentTimeMillis()
        db.withTransaction {
            outboxDao.insert(
                plannedMealClearOperation(
                    uid = uid,
                    collectionKey = "plannedMeals:$weekStartDate:$dayIndex",
                    filters = mapOf(
                        "weekStartDate" to weekStartDate,
                        "dayIndex" to dayIndex
                    ),
                    now = now
                )
            )
            mealPlanDao.clearDay(dayIndex, weekStartDate)
        }
        triggerSync(uid)
    }

    suspend fun clearWeek(uid: String, weekStartDate: String) {
        if (uid.isBlank()) {
            mealPlanDao.clearWeek(weekStartDate)
            return
        }
        val now = System.currentTimeMillis()
        db.withTransaction {
            outboxDao.insert(
                plannedMealClearOperation(
                    uid = uid,
                    collectionKey = "plannedMeals:$weekStartDate",
                    filters = mapOf("weekStartDate" to weekStartDate),
                    now = now
                )
            )
            mealPlanDao.clearWeek(weekStartDate)
        }
        triggerSync(uid)
    }

    suspend fun clearWeekDays(uid: String, weekStartDate: String, dayIndices: List<Int>) {
        if (dayIndices.isEmpty()) return
        if (uid.isBlank()) {
            mealPlanDao.clearWeekDays(weekStartDate, dayIndices)
            return
        }
        val now = System.currentTimeMillis()
        db.withTransaction {
            outboxDao.insertAll(
                dayIndices.map { dayIndex ->
                    plannedMealClearOperation(
                        uid = uid,
                        collectionKey = "plannedMeals:$weekStartDate:$dayIndex",
                        filters = mapOf(
                            "weekStartDate" to weekStartDate,
                            "dayIndex" to dayIndex
                        ),
                        now = now
                    )
                }
            )
            mealPlanDao.clearWeekDays(weekStartDate, dayIndices)
        }
        triggerSync(uid)
    }

    suspend fun replaceWeek(uid: String, weekStartDate: String, meals: List<PlannedMealEntity>) {
        if (uid.isBlank()) {
            mealPlanDao.replaceWeek(weekStartDate, meals)
            return
        }
        val now = System.currentTimeMillis()
        db.withTransaction {
            val operations = buildList {
                add(
                    plannedMealClearOperation(
                        uid = uid,
                        collectionKey = "plannedMeals:$weekStartDate",
                        filters = mapOf("weekStartDate" to weekStartDate),
                        now = now
                    )
                )
                addAll(meals.map { upsertOperation(uid, it, now) })
            }
            outboxDao.insertAll(operations)
            mealPlanDao.replaceWeek(weekStartDate, meals)
        }
        triggerSync(uid)
    }

    suspend fun replaceSlot(
        uid: String,
        dayIndex: Int,
        weekStartDate: String,
        mealSlot: String,
        meals: List<PlannedMealEntity>
    ) {
        if (uid.isBlank()) {
            mealPlanDao.replaceSlot(dayIndex, weekStartDate, mealSlot, meals)
            return
        }
        val now = System.currentTimeMillis()
        db.withTransaction {
            val operations = buildList {
                add(
                    plannedMealClearOperation(
                        uid = uid,
                        collectionKey = "plannedMeals:$weekStartDate:$dayIndex:$mealSlot",
                        filters = mapOf(
                            "weekStartDate" to weekStartDate,
                            "dayIndex" to dayIndex,
                            "mealSlot" to mealSlot
                        ),
                        now = now
                    )
                )
                addAll(meals.map { upsertOperation(uid, it, now) })
            }
            outboxDao.insertAll(operations)
            mealPlanDao.replaceSlot(dayIndex, weekStartDate, mealSlot, meals)
        }
        triggerSync(uid)
    }

    suspend fun insertMeals(uid: String, meals: List<PlannedMealEntity>) {
        if (meals.isEmpty()) return
        if (uid.isBlank()) {
            mealPlanDao.insertMeals(meals)
            return
        }
        val now = System.currentTimeMillis()
        db.withTransaction {
            mealPlanDao.insertMeals(meals)
            outboxDao.insertAll(meals.map { upsertOperation(uid, it, now) })
        }
        triggerSync(uid)
    }

    private fun upsertOperation(uid: String, meal: PlannedMealEntity, now: Long) =
        FirestorePayloadSerializer.upsert(
            uid = uid,
            entityType = FirestoreEntityType.PLANNED_MEAL,
            entityKey = FirestorePayloadSerializer.plannedMealDocumentId(meal),
            remotePath = FirestorePayloadSerializer.plannedMealPath(uid, meal),
            payload = FirestorePayloadSerializer.plannedMealPayload(meal),
            now = now
        )

    private fun plannedMealClearOperation(
        uid: String,
        collectionKey: String,
        filters: Map<String, Any?>,
        now: Long
    ) =
        FirestorePayloadSerializer.clearCollection(
            uid = uid,
            collectionKey = collectionKey,
            remotePath = FirestorePayloadSerializer.plannedMealCollectionPath(uid),
            filters = filters,
            now = now
        )

    private fun triggerSync(uid: String) {
        if (uid.isBlank()) return
        FirestoreAutoSyncManager.triggerSync(appContext, uid)
        AutoSyncManager.triggerSync(appContext, uid)
    }
}
