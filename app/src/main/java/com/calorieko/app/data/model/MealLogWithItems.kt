package com.calorieko.app.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation object that bundles a [MealLogEntity] with its
 * child [MealLogItemEntity] list.
 *
 * Used by @Transaction queries in MealLogDao.
 */
data class MealLogWithItems(
    @Embedded val mealLog: MealLogEntity,

    @Relation(
        parentColumn = "meal_log_id",
        entityColumn = "meal_log_id"
    )
    val items: List<MealLogItemEntity>
)
