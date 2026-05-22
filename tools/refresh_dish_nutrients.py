"""Refresh dish nutrient totals from raw ingredients.

This script intentionally updates only the derived nutrient fields in
``dish_recipes.json``:

* total_nutrients_raw
* per_serving_nutrients

It preserves recipe composition and all dish metadata/weight fields.
"""

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


def load_json(path: Path):
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def build_nutrient_lookup(raw_ingredients: list[dict]) -> dict[str, dict]:
    return {
        item["ingredient_key"]: item.get("nutrients_per_100g", {})
        for item in raw_ingredients
    }


def group_recipe_rows(recipe_ingredients: list[dict]) -> dict[str, list[dict]]:
    grouped: dict[str, list[dict]] = defaultdict(list)
    for row in recipe_ingredients:
        grouped[row["dish_label"]].append(row)
    return grouped


def validate_recipe_ingredient_keys(
    recipe_ingredients: list[dict],
    nutrient_lookup: dict[str, dict],
) -> None:
    missing = sorted(
        {
            row["ingredient_key"]
            for row in recipe_ingredients
            if row["ingredient_key"] not in nutrient_lookup
        }
    )
    if missing:
        missing_text = "\n".join(f"  - {key}" for key in missing)
        raise SystemExit(
            "Missing ingredient keys in raw_ingredients.json; no files written:\n"
            f"{missing_text}"
        )


def compute_totals(recipe_rows: list[dict], nutrient_lookup: dict[str, dict]) -> dict[str, float]:
    totals = {key: 0.0 for key in NUTRIENT_KEYS}

    for row in recipe_rows:
        nutrients = nutrient_lookup[row["ingredient_key"]]
        grams = float(row.get("raw_weight_grams", 0.0) or 0.0)
        factor = grams / 100.0

        for key in NUTRIENT_KEYS:
            totals[key] += factor * float(nutrients.get(key, 0.0) or 0.0)

    return {key: round(value, 2) for key, value in totals.items()}


def compute_per_serving(total_nutrients: dict[str, float], servings: int) -> dict[str, float]:
    divisor = max(int(servings), 1)
    return {key: round(value / divisor, 2) for key, value in total_nutrients.items()}


def main() -> None:
    raw_ingredients = load_json(RAW_INGREDIENTS_PATH)
    recipe_ingredients = load_json(RECIPE_INGREDIENTS_PATH)
    dish_recipes = load_json(DISH_RECIPES_PATH)

    nutrient_lookup = build_nutrient_lookup(raw_ingredients)
    validate_recipe_ingredient_keys(recipe_ingredients, nutrient_lookup)
    recipe_rows_by_dish = group_recipe_rows(recipe_ingredients)

    recomputed = 0
    preserved: list[str] = []
    vitamin_a_changes: list[tuple[str, float, float]] = []

    for dish in dish_recipes:
        label = dish["dish_label"]
        recipe_rows = recipe_rows_by_dish.get(label)
        if not recipe_rows:
            preserved.append(label)
            continue

        old_vitamin_a = float(dish["per_serving_nutrients"].get("vitamin_a", 0.0) or 0.0)
        total_nutrients = compute_totals(recipe_rows, nutrient_lookup)
        per_serving_nutrients = compute_per_serving(total_nutrients, dish.get("servings", 1))

        dish["total_nutrients_raw"] = total_nutrients
        dish["per_serving_nutrients"] = per_serving_nutrients

        new_vitamin_a = float(per_serving_nutrients.get("vitamin_a", 0.0) or 0.0)
        if old_vitamin_a != new_vitamin_a:
            vitamin_a_changes.append((label, old_vitamin_a, new_vitamin_a))
        recomputed += 1

    with DISH_RECIPES_PATH.open("w", encoding="utf-8") as f:
        json.dump(dish_recipes, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"Recomputed dishes: {recomputed}")
    print(f"Preserved dishes: {len(preserved)}")
    if preserved:
        print("Preserved labels: " + ", ".join(preserved))

    print("\nRepresentative Vitamin A changes (per serving):")
    report_labels = [
        "egg_boiled",
        "egg_sunny",
        "egg_ampalaya",
        "pinakbet",
        "chopseuy",
        "kwekwek",
    ]
    change_by_label = {label: (old, new) for label, old, new in vitamin_a_changes}
    for label in report_labels:
        if label in change_by_label:
            old, new = change_by_label[label]
            print(f"  {label}: {old:.2f} -> {new:.2f}")


if __name__ == "__main__":
    main()
