package com.calorieko.app.ml

/**
 * Maps the TFLite model's snake_case labels to the English dish names (`nameEn`)
 * stored in the Room `FOOD_TABLE`. This allows us to look up nutritional data
 * for each AI-detected dish via [com.calorieko.app.data.local.FoodDao.getFoodByName].
 *
 * The special label `"negative"` maps to `null`, indicating an unsupported image.
 */
object DishLabelMapper {

    private val labelToNameEn = mapOf(
        "chicken_breast"     to "Lechon Manok – Breast",
        "chicken_drumstick"  to "Lechon Manok – Drumstick",
        "chicken_thigh"      to "Lechon Manok – Thigh",
        "chicken_wings"      to "Lechon Manok – Wings",
        "chicken_tinola"     to "Chicken Tinola",
        "chopseuy"           to "Chopsuey",
        "egg_ampalaya"       to "Ampalaya with Egg",
        "egg_boiled"         to "Boiled Egg",
        "egg_fried"          to "Fried Egg",
        "egg_sunny"          to "Sunny Side Up Egg",
        "galunggong_grilled" to "Grilled Galunggong",
        "kinilaw_tuna"       to "Kinilaw na Tuna",
        "mackerel_fried"     to "Fried Mackerel",
        "menudo"             to "Menudo",
        "milkfish_fried"     to "Fried Milkfish",
        "pesang_bangus"      to "Pesang Bangus",
        "pinakbet"           to "Pinakbet",
        "rice_well_milled"   to "Well-Milled Rice",
        "sinigang_pork"      to "Pork Sinigang",
        "sinuglaw_pork"      to "Sinuglaw Pork",
        "tilapya_fried"      to "Fried Tilapia",
        "tinapa_ginisa"      to "Ginisang Tinapa",
        "tokneneng_salad"    to "Tokneneng with Salad",
        "udong"              to "Udong"
    )

    /**
     * Convert a model label to the corresponding `FoodItem.nameEn`.
     *
     * @return The English dish name, or `null` if the label is `"negative"` or unknown.
     */
    fun toFoodName(label: String): String? = labelToNameEn[label]

    /** @return `true` when the label represents a supported, recognizable dish. */
    fun isSupported(label: String): Boolean = label != "negative" && label in labelToNameEn
}
