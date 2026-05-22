"""Validate dish nutrient totals against raw ingredient contributions."""

from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parents[1]
ASSETS_DIR = BASE_DIR / "app" / "src" / "main" / "assets"
RAW_INGREDIENTS_PATH = ASSETS_DIR / "raw_ingredients.json"
RECIPE_INGREDIENTS_PATH = ASSETS_DIR / "recipe_ingredients.json"
DISH_RECIPES_PATH = ASSETS_DIR / "dish_recipes.json"

NUTRIENT_KEYS = [
    "calories",
    "protein",
    "carbs",
    "fat",
    "fiber",
    "sugar",
    "sodium",
    "potassium",
    "vitamin_a",
    "vitamin_c",
    "calcium",
    "iron",
]

REQUIRED_DISH_FIELDS = [
    "dish_label",
    "name_en",
    "name_ph",
    "category",
    "cooking_method",
    "servings",
    "total_raw_weight_g",
    "dish_yield_factor",
    "cooked_weight_g",
    "per_serving_weight_g",
    "total_nutrients_raw",
    "per_serving_nutrients",
    "ingredient_count",
]

SPOT_CHECK_LABELS = [
    "egg_boiled",
    "egg_sunny",
    "egg_ampalaya",
    "pinakbet",
    "chopseuy",
    "kwekwek",
]


def load_json(path: Path):
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def round2(value: float) -> float:
    return round(value + 0.0, 2)


def main() -> None:
    raw_ingredients = load_json(RAW_INGREDIENTS_PATH)
    recipe_ingredients = load_json(RECIPE_INGREDIENTS_PATH)
    dish_recipes = load_json(DISH_RECIPES_PATH)

    nutrient_lookup = {
        item["ingredient_key"]: item.get("nutrients_per_100g", {})
        for item in raw_ingredients
    }

    recipe_rows_by_dish: dict[str, list[dict]] = defaultdict(list)
    for row in recipe_ingredients:
        recipe_rows_by_dish[row["dish_label"]].append(row)

    errors: list[str] = []
    recomputed_count = 0
    preserved_labels: list[str] = []

    missing_ingredients = sorted(
        {
            row["ingredient_key"]
            for row in recipe_ingredients
            if row["ingredient_key"] not in nutrient_lookup
        }
    )
    for key in missing_ingredients:
        errors.append(f"recipe ingredient missing from raw_ingredients.json: {key}")

    dish_labels = set()
    for dish in dish_recipes:
        label = dish.get("dish_label", "UNKNOWN")
        if label in dish_labels:
            errors.append(f"duplicate dish label: {label}")
        dish_labels.add(label)

        for field in REQUIRED_DISH_FIELDS:
            if field not in dish:
                errors.append(f"{label} missing field: {field}")

        total_nutrients = dish.get("total_nutrients_raw", {})
        per_serving_nutrients = dish.get("per_serving_nutrients", {})
        for key in NUTRIENT_KEYS:
            if key not in total_nutrients:
                errors.append(f"{label} missing total nutrient: {key}")
            if key not in per_serving_nutrients:
                errors.append(f"{label} missing per-serving nutrient: {key}")

        recipe_rows = recipe_rows_by_dish.get(label)
        if not recipe_rows:
            preserved_labels.append(label)
            continue

        recomputed_count += 1
        expected_totals = {key: 0.0 for key in NUTRIENT_KEYS}
        for row in recipe_rows:
            nutrients = nutrient_lookup[row["ingredient_key"]]
            grams = float(row.get("raw_weight_grams", 0.0) or 0.0)
            factor = grams / 100.0
            for key in NUTRIENT_KEYS:
                expected_totals[key] += factor * float(nutrients.get(key, 0.0) or 0.0)

        servings = max(int(dish.get("servings", 1) or 1), 1)
        for key in NUTRIENT_KEYS:
            expected_total = round2(expected_totals[key])
            actual_total = round2(float(total_nutrients.get(key, 0.0) or 0.0))
            if abs(expected_total - actual_total) > 0.01:
                errors.append(
                    f"{label} total {key}: actual {actual_total}, expected {expected_total}"
                )

            expected_per_serving = round2(expected_total / servings)
            actual_per_serving = round2(float(per_serving_nutrients.get(key, 0.0) or 0.0))
            if abs(expected_per_serving - actual_per_serving) > 0.01:
                errors.append(
                    f"{label} per-serving {key}: "
                    f"actual {actual_per_serving}, expected {expected_per_serving}"
                )

    recipe_labels = set(recipe_rows_by_dish)
    for label in sorted(recipe_labels - dish_labels):
        errors.append(f"recipe label missing from dish_recipes.json: {label}")

    if errors:
        print("Dish recipe validation failed:")
        for error in errors:
            print(f"ERROR: {error}")
        raise SystemExit(1)

    print(f"Dish recipes valid: {len(dish_recipes)} dishes")
    print(f"Recomputed dishes with recipe rows: {recomputed_count}")
    print(f"Preserved dishes without recipe rows: {len(preserved_labels)}")
    if preserved_labels:
        print("Preserved labels: " + ", ".join(preserved_labels))

    print("\nVitamin A spot checks (per serving):")
    dishes_by_label = {dish["dish_label"]: dish for dish in dish_recipes}
    for label in SPOT_CHECK_LABELS:
        dish = dishes_by_label.get(label)
        if dish:
            value = dish["per_serving_nutrients"]["vitamin_a"]
            print(f"  {label}: {value}")


if __name__ == "__main__":
    main()
