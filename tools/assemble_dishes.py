"""
CalorieKo - Dish Recipe Assembly Script  [DEPRECATED]
=====================================================
DEPRECATED: This script is superseded by scripts/generate_dish_jsons.py,
    which is the canonical pipeline for generating both recipe_ingredients.json
    and dish_recipes.json.

    This file is kept for reference only. Do not use it to regenerate data.

Original description:
CalorieKo — Dish Recipe Assembly Script
=========================================
Phase 1, Step 4: Creates dish_recipes.json by combining recipe_ingredients.json
and raw_ingredients.json to compute per-dish and per-serving nutritional values.

Usage:
    python assemble_dishes.py

Input:
    ../app/src/main/assets/raw_ingredients.json
    ../app/src/main/assets/recipe_ingredients.json
    ../app/src/main/assets/dish_labels_and_values.csv  (for name_en, name_ph, category)

Output:
    ../app/src/main/assets/dish_recipes.json
"""

import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

import csv
import json
import os

# ──────────────────────────────────────────────
# DISH METADATA (from dish_labels_and_values.csv)
# ──────────────────────────────────────────────

DISH_METADATA = {
    "tokneneng_salad": {
        "name_en": "Tokneneng with Salad",
        "name_ph": "Tokneneng na may Ensalada",
        "category": "Street Food",
        "cooking_method": "deep_fried",
        "servings": 5,
    },
    "kinilaw_tuna": {
        "name_en": "Tuna Ceviche",
        "name_ph": "Kinilaw na Tuna",
        "category": "Fish",
        "cooking_method": "raw_cured",
        "servings": 6,
    },
    "egg_ampalaya": {
        "name_en": "Ampalaya with Egg",
        "name_ph": "Ginisang Ampalaya na may Itlog",
        "category": "Vegetable Dish",
        "cooking_method": "sauteed",
        "servings": 6,
    },
    "sinigang_pork": {
        "name_en": "Pork Sinigang",
        "name_ph": "Sinigang na Baboy",
        "category": "Soup",
        "cooking_method": "simmered",
        "servings": 10,
    },
    "menudo": {
        "name_en": "Menudo",
        "name_ph": "Menudo",
        "category": "Main Dish",
        "cooking_method": "stewed",
        "servings": 10,
    },
    "udong": {
        "name_en": "Udong",
        "name_ph": "Udong",
        "category": "Noodles",
        "cooking_method": "simmered",
        "servings": 6,
    },
    "pesang_bangus": {
        "name_en": "Pesang Bangus",
        "name_ph": "Pesang Bangus",
        "category": "Soup",
        "cooking_method": "simmered",
        "servings": 6,
    },
    "galunggong_grilled": {
        "name_en": "Grilled Galunggong",
        "name_ph": "Inihaw na Galunggong",
        "category": "Fish",
        "cooking_method": "grilled",
        "servings": 5,
    },
    "tilapya_fried": {
        "name_en": "Fried Tilapia",
        "name_ph": "Pritong Tilapya",
        "category": "Fish",
        "cooking_method": "pan_fried",
        "servings": 5,
    },
    "pinakbet": {
        "name_en": "Pinakbet",
        "name_ph": "Pinakbet",
        "category": "Vegetable Dish",
        "cooking_method": "sauteed",
        "servings": 8,
    },
    "chopseuy": {
        "name_en": "Chopsuey",
        "name_ph": "Chopsuey",
        "category": "Vegetable Dish",
        "cooking_method": "stir_fried",
        "servings": 8,
    },
    "chicken_tinola": {
        "name_en": "Chicken Tinola",
        "name_ph": "Tinolang Manok",
        "category": "Soup",
        "cooking_method": "simmered",
        "servings": 8,
    },
    "sinuglaw_pork": {
        "name_en": "Sinuglaw Pork",
        "name_ph": "Sinuglaw na Baboy",
        "category": "Main Dish",
        "cooking_method": "grilled_and_cured",
        "servings": 6,
    },
    # ── Single-serving simple dishes ──
    "milkfish_fried": {
        "name_en": "Fried Milkfish",
        "name_ph": "Pritong Bangus",
        "category": "Fish",
        "cooking_method": "pan_fried",
        "servings": 1,
    },
    "mackerel_fried": {
        "name_en": "Fried Mackerel",
        "name_ph": "Pritong Alumahan",
        "category": "Fish",
        "cooking_method": "pan_fried",
        "servings": 1,
    },
    "rice_well_milled": {
        "name_en": "Steamed White Rice",
        "name_ph": "Kanin na Puti",
        "category": "Rice",
        "cooking_method": "boiled",
        "servings": 1,
    },
    "egg_sunny": {
        "name_en": "Sunny Side Up Egg",
        "name_ph": "Itlog na Sunny Side Up",
        "category": "Egg",
        "cooking_method": "pan_fried",
        "servings": 1,
    },
    "egg_fried": {
        "name_en": "Fried Egg",
        "name_ph": "Pritong Itlog",
        "category": "Egg",
        "cooking_method": "pan_fried",
        "servings": 1,
    },
    "egg_boiled": {
        "name_en": "Boiled Egg",
        "name_ph": "Nilagang Itlog",
        "category": "Egg",
        "cooking_method": "boiled",
        "servings": 1,
    },
    # ── Store-bought (already cooked) ──
    "chicken_wing": {
        "name_en": "Rotisserie Chicken - Wing",
        "name_ph": "Lechon Manok - Pakpak",
        "category": "Main Dish",
        "cooking_method": "store_bought_roasted",
        "servings": 1,
    },
    "chicken_thigh": {
        "name_en": "Rotisserie Chicken - Thigh",
        "name_ph": "Lechon Manok - Hita",
        "category": "Main Dish",
        "cooking_method": "store_bought_roasted",
        "servings": 1,
    },
    "chicken_drumstick": {
        "name_en": "Rotisserie Chicken - Drumstick",
        "name_ph": "Lechon Manok - Binti",
        "category": "Main Dish",
        "cooking_method": "store_bought_roasted",
        "servings": 1,
    },
    "chicken_breast": {
        "name_en": "Rotisserie Chicken - Breast",
        "name_ph": "Lechon Manok - Dibdib",
        "category": "Main Dish",
        "cooking_method": "store_bought_roasted",
        "servings": 1,
    },
}

# ──────────────────────────────────────────────
# YIELD FACTORS BY COOKING METHOD
# ──────────────────────────────────────────────
# Sources: USDA Table of Cooking Yields, food science literature
#
# Yield factor = (cooked weight) / (raw weight)
# Applied at the dish level to adjust total raw grams after cooking.

YIELD_FACTORS = {
    # Soups: water-based, moderate evaporation (~10-20% loss)
    "sinigang_pork":        0.85,   # Simmered soup, meat shrinks but broth retains volume
    "pesang_bangus":        0.88,   # Light clear soup, minimal evaporation
    "chicken_tinola":       0.85,   # Simmered soup with leafy greens

    # Sauteed/stir-fried: vegetable moisture loss, some oil absorption
    "egg_ampalaya":         0.80,   # Sauteed, eggs retain moisture
    "pinakbet":             0.75,   # Sauteed veggies, bagoong is dense
    "chopseuy":             0.78,   # Stir-fried, quick cook retains more

    # Stewed: meat shrinkage + sauce reduction
    "menudo":               0.75,   # Stewed with tomato sauce

    # Noodle soup
    "udong":                0.90,   # Noodles absorb water, broth-based

    # Deep-fried: moisture loss offset by oil absorption
    "tokneneng_salad":      0.90,   # Deep-fried eggs + raw salad component

    # Raw/ceviche: minimal change
    "kinilaw_tuna":         0.95,   # Raw/cured, minor liquid drainage
    "sinuglaw_pork":        0.85,   # Grilled pork (shrinks ~30%) + raw components

    # Grilled fish: significant moisture/fat loss
    "galunggong_grilled":   0.65,   # Whole grilled fish, 35% moisture/fat loss

    # Pan-fried fish/egg: moderate moisture loss
    "tilapya_fried":        0.80,   # Pan-fried whole fish
    "milkfish_fried":       0.80,   # Pan-fried milkfish
    "mackerel_fried":       0.80,   # Pan-fried mackerel
    "egg_sunny":            0.92,   # Sunny side up, minimal loss
    "egg_fried":            0.90,   # Fried egg, slightly more crisp

    # Boiled
    "egg_boiled":           1.00,   # Boiled egg portion uses edible egg weight only
    "rice_well_milled":     2.81,   # Rice ABSORBS water: USDA raw (365 kcal/100g) ÷ cooked (130 kcal/100g) = 2.81× weight gain

    # Store-bought (already cooked, weighed as-is)
    "chicken_wing":         1.00,
    "chicken_thigh":        1.00,
    "chicken_drumstick":    1.00,
    "chicken_breast":       1.00,
}

# ──────────────────────────────────────────────
# OIL ABSORPTION FACTORS
# ──────────────────────────────────────────────
# What fraction of the total cooking_oil is actually absorbed by the food.
# Sources: Food science literature, USDA cooking methods research
#
# Deep frying: large oil bath, only ~8-12% absorbed
# Pan frying: moderate oil, ~10-15% absorbed
# Sauteing: small amount of oil, ~100% absorbed (it's all in the dish)
# Stir frying: small amount of oil, ~100% absorbed

OIL_ABSORPTION_RATE = {
    # Deep-fried: large oil volume, most stays in the fryer
    "tokneneng_salad":      0.10,   # 10% of 2.5 cups oil absorbed

    # Pan-fried with significant oil
    "tilapya_fried":        0.15,   # 15% of 1 cup oil absorbed
    "milkfish_fried":       0.15,   # Pan-fried fish
    "mackerel_fried":       0.15,   # Pan-fried fish

    # Pan-fried eggs: very small oil amount, ~100% absorbed
    "egg_sunny":            1.00,   # 5g oil, all absorbed
    "egg_fried":            1.00,   # 5g oil, all absorbed

    # Sauteed/stir-fried: small oil, all absorbed into dish
    "egg_ampalaya":         1.00,   # 2 tbsps oil
    "pinakbet":             1.00,   # 3 tbsps oil
    "chopseuy":             1.00,   # 3 tbsps oil
    "chicken_tinola":       1.00,   # 3 tbsps oil
    "sinuglaw_pork":        1.00,   # 1 tbsp oil

    # Simmered/stewed: oil is part of the broth
    "sinigang_pork":        1.00,   # 1/2 cup oil for sauteing step
    "menudo":               1.00,   # 1/3 cup oil for sauteing step
    "udong":                1.00,   # 1/3 cup oil
    "pesang_bangus":        1.00,   # 1/8 cup oil
}

# Nutrient keys we track
NUTRIENT_KEYS = [
    "calories", "protein", "carbs", "fat", "fiber", "sugar",
    "sodium", "potassium", "vitamin_a", "vitamin_c", "calcium", "iron"
]


def load_json(path: str) -> list:
    """Load a JSON file and return its contents."""
    with open(path, 'r', encoding='utf-8') as f:
        return json.load(f)


def build_ingredient_lookup(raw_ingredients: list) -> dict:
    """Build a lookup dict: ingredient_key -> nutrients_per_100g."""
    lookup = {}
    for ing in raw_ingredients:
        key = ing["ingredient_key"]
        lookup[key] = ing["nutrients_per_100g"]
    return lookup


def compute_dish_nutrients(dish_label: str, recipe_rows: list, nutrient_lookup: dict) -> dict:
    """
    Compute total raw nutrients for a dish by summing each ingredient's contribution.

    Formula per ingredient:
        nutrient_amount = (raw_weight_grams / 100) * nutrients_per_100g[nutrient]

    For cooking_oil, applies the oil absorption rate to only count the fraction
    of oil that the food actually absorbs during cooking.
    """
    totals = {k: 0.0 for k in NUTRIENT_KEYS}
    oil_absorption = OIL_ABSORPTION_RATE.get(dish_label, 1.00)

    for row in recipe_rows:
        ing_key = row["ingredient_key"]
        grams = row["raw_weight_grams"]

        if ing_key not in nutrient_lookup:
            print(f"  [WARN] {dish_label}: ingredient '{ing_key}' not found in raw_ingredients.json")
            continue

        # Apply oil absorption factor for cooking_oil
        if ing_key == "cooking_oil" and oil_absorption < 1.0:
            effective_grams = grams * oil_absorption
        else:
            effective_grams = grams

        nutrients = nutrient_lookup[ing_key]
        factor = effective_grams / 100.0

        for k in NUTRIENT_KEYS:
            totals[k] += factor * nutrients.get(k, 0.0)

    # Round all values
    return {k: round(v, 2) for k, v in totals.items()}


def assemble_dishes(raw_ingredients: list, recipe_ingredients: list) -> list:
    """Assemble the final dish_recipes.json."""
    nutrient_lookup = build_ingredient_lookup(raw_ingredients)

    # Group recipe ingredients by dish
    dishes_ingredients = {}
    for row in recipe_ingredients:
        dish = row["dish_label"]
        if dish not in dishes_ingredients:
            dishes_ingredients[dish] = []
        dishes_ingredients[dish].append(row)

    results = []

    for dish_label, rows in sorted(dishes_ingredients.items()):
        meta = DISH_METADATA.get(dish_label)
        if not meta:
            print(f"  [WARN] No metadata for dish '{dish_label}' -- skipping")
            continue

        yield_factor = YIELD_FACTORS.get(dish_label, 0.85)
        servings = meta["servings"]

        # Compute total raw weight
        total_raw_g = sum(r["raw_weight_grams"] for r in rows)

        # Compute total raw nutrients (sum of all ingredients)
        total_nutrients = compute_dish_nutrients(dish_label, rows, nutrient_lookup)

        # Per-serving nutrients = total / servings
        per_serving = {k: round(v / servings, 2) for k, v in total_nutrients.items()}

        # Cooked weight estimate
        cooked_weight_g = round(total_raw_g * yield_factor, 1)
        per_serving_weight_g = round(cooked_weight_g / servings, 1)

        entry = {
            "dish_label": dish_label,
            "name_en": meta["name_en"],
            "name_ph": meta["name_ph"],
            "category": meta["category"],
            "cooking_method": meta["cooking_method"],
            "servings": servings,
            "total_raw_weight_g": round(total_raw_g, 1),
            "dish_yield_factor": yield_factor,
            "cooked_weight_g": cooked_weight_g,
            "per_serving_weight_g": per_serving_weight_g,
            "total_nutrients_raw": total_nutrients,
            "per_serving_nutrients": per_serving,
            "ingredient_count": len(rows),
        }
        results.append(entry)

    return results


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = os.path.join(script_dir, "..", "app", "src", "main", "assets")
    base_dir = os.path.normpath(base_dir)

    raw_path = os.path.join(base_dir, "raw_ingredients.json")
    recipe_path = os.path.join(base_dir, "recipe_ingredients.json")
    output_path = os.path.join(base_dir, "dish_recipes.json")

    print("=" * 60)
    print("CalorieKo -- Dish Recipe Assembly")
    print("=" * 60)
    print(f"Raw ingredients: {raw_path}")
    print(f"Recipe ingredients: {recipe_path}")
    print(f"Output: {output_path}")
    print()

    raw_ingredients = load_json(raw_path)
    recipe_ingredients = load_json(recipe_path)

    print(f"Loaded {len(raw_ingredients)} raw ingredients")
    print(f"Loaded {len(recipe_ingredients)} recipe ingredient entries")
    print()

    results = assemble_dishes(raw_ingredients, recipe_ingredients)

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    # Summary table
    print(f"\n{'Dish':<26s} {'Serv':>4s} {'YF':>5s} {'Raw(g)':>8s} {'Cook(g)':>8s} {'Cal/srv':>8s} {'Pro/srv':>8s}")
    print("-" * 72)
    for d in results:
        print(
            f"{d['dish_label']:<26s} "
            f"{d['servings']:>4d} "
            f"{d['dish_yield_factor']:>5.2f} "
            f"{d['total_raw_weight_g']:>8.1f} "
            f"{d['cooked_weight_g']:>8.1f} "
            f"{d['per_serving_nutrients']['calories']:>8.1f} "
            f"{d['per_serving_nutrients']['protein']:>8.1f}"
        )

    print(f"\nTotal dishes: {len(results)}")
    print(f"Output: {output_path}")


if __name__ == "__main__":
    main()
