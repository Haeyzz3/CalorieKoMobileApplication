package com.calorieko.app.data.remote.firestore

object FirestoreSyncOperation {
    const val UPSERT_DOCUMENT = "UPSERT_DOCUMENT"
    const val DELETE_DOCUMENT = "DELETE_DOCUMENT"
    const val DELETE_MEAL_LOG_RECURSIVE = "DELETE_MEAL_LOG_RECURSIVE"
    const val CLEAR_COLLECTION = "CLEAR_COLLECTION"
}

object FirestoreEntityType {
    const val USER_PROFILE = "USER_PROFILE"
    const val WEIGHT_LOG = "WEIGHT_LOG"
    const val ACTIVITY_LOG = "ACTIVITY_LOG"
    const val MEAL_LOG = "MEAL_LOG"
    const val DAILY_NUTRITION_SUMMARY = "DAILY_NUTRITION_SUMMARY"
    const val PANTRY_ITEM = "PANTRY_ITEM"
    const val PLANNED_MEAL = "PLANNED_MEAL"
    const val COLLECTION = "COLLECTION"
}
