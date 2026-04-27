"""Patch the 3 simple dishes with computed nutrient values in dish_recipes.json."""
import json

PATH = r'app\src\main\assets\dish_recipes.json'
RAW_PATH = r'app\src\main\assets\raw_ingredients.json'

raw = json.load(open(RAW_PATH, 'r', encoding='utf-8'))
n_by_key = {i["ingredient_key"]: i["nutrients_per_100g"] for i in raw}

NKEYS = ["calories", "protein", "carbs", "fat", "fiber", "sugar",
         "sodium", "potassium", "vitamin_a", "vitamin_c", "calcium", "iron"]

def compute_nutrients(ingredients_with_grams):
    """Sum nutrients for a list of (ingredient_key, grams) tuples."""
    total = {k: 0.0 for k in NKEYS}
    for key, grams in ingredients_with_grams:
        n = n_by_key.get(key, {})
        for k in NKEYS:
            total[k] += n.get(k, 0) * grams / 100.0
    return {k: round(v, 2) for k, v in total.items()}

# Define the 3 dishes
PATCHES = {
    "egg_omelette": {
        "ingredients": [("chicken_egg", 50.0), ("cooking_oil", 5.0)],
        "total_raw_weight_g": 55.0,
        "dish_yield_factor": 0.92,
        "servings": 1,
        "cooking_method": "pan_fried",
    },
    "egg_scrambled": {
        "ingredients": [("chicken_egg", 50.0), ("cooking_oil", 5.0)],
        "total_raw_weight_g": 55.0,
        "dish_yield_factor": 0.92,
        "servings": 1,
        "cooking_method": "pan_fried",
    },
    "galunggong_fried": {
        "ingredients": [("galunggong_fish", 160.0), ("cooking_oil", 10.0), ("salt_iodized", 1.0)],
        "total_raw_weight_g": 171.0,
        "dish_yield_factor": 0.8,
        "servings": 1,
        "cooking_method": "pan_fried",
    },
}

dishes = json.load(open(PATH, 'r', encoding='utf-8'))

for d in dishes:
    label = d["dish_label"]
    if label not in PATCHES:
        continue

    patch = PATCHES[label]
    total_nutrients = compute_nutrients(patch["ingredients"])
    servings = patch["servings"]
    per_serving = {k: round(v / servings, 2) for k, v in total_nutrients.items()}

    raw_w = patch["total_raw_weight_g"]
    yf = patch["dish_yield_factor"]
    cooked_w = round(raw_w * yf, 1)
    per_srv_w = round(cooked_w / servings, 1)

    d["total_raw_weight_g"] = raw_w
    d["dish_yield_factor"] = yf
    d["cooked_weight_g"] = cooked_w
    d["per_serving_weight_g"] = per_srv_w
    d["cooking_method"] = patch["cooking_method"]
    d["servings"] = servings
    d["total_nutrients_raw"] = total_nutrients
    d["per_serving_nutrients"] = per_serving

    print(f"[PATCHED] {label}: cal={per_serving['calories']} pro={per_serving['protein']} "
          f"carb={per_serving['carbs']} fat={per_serving['fat']} "
          f"raw={raw_w}g cooked={cooked_w}g")

with open(PATH, 'w', encoding='utf-8') as f:
    json.dump(dishes, f, indent=2, ensure_ascii=False)

print("\nDone! dish_recipes.json patched.")
