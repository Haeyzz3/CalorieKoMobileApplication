"""
Fix all 8 simple dishes:
1. Add raw_weight_grams to recipe_ingredients.json
2. Recompute dish_recipes.json from raw ingredients
"""
import json

BASE = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets"
RECIPE_PATH = f"{BASE}/recipe_ingredients.json"
DISH_PATH = f"{BASE}/dish_recipes.json"
RAW_PATH = f"{BASE}/raw_ingredients.json"

# Load data
with open(RAW_PATH, "r", encoding="utf-8") as f:
    raw_map = {item["ingredient_key"]: item for item in json.load(f)}

with open(RECIPE_PATH, "r", encoding="utf-8") as f:
    recipe_ingredients = json.load(f)

with open(DISH_PATH, "r", encoding="utf-8") as f:
    dishes = json.load(f)

NKEYS = ["calories", "protein", "carbs", "fat", "fiber", "sugar",
         "sodium", "potassium", "vitamin_a", "vitamin_c", "calcium", "iron"]

# Define the correct weights for each simple dish
WEIGHTS = {
    "rice_well_milled": {
        "rice_bigas": 65.0,   # 1/3 cup dry -> ~1 cup cooked
        "water": 130.0,       # 1:2 rice-to-water ratio
    },
    "egg_boiled": {
        "chicken_egg": 50.0,  # 1 medium egg
        "water": 200.0,       # enough to cover
    },
    "egg_sunny": {
        "chicken_egg": 50.0,  # 1 medium egg
        "cooking_oil": 5.0,   # ~1 tsp for frying
    },
    "egg_omelette": {
        "chicken_egg": 50.0,  # 1 medium egg
        "cooking_oil": 5.0,   # ~1 tsp for frying
    },
    "egg_scrambled": {
        "chicken_egg": 50.0,  # 1 medium egg
        "cooking_oil": 5.0,   # ~1 tsp for frying
    },
    "milkfish_fried": {
        "bangus_fish": 150.0,   # 1 piece
        "cooking_oil": 14.0,    # ~1 tbsp for pan-frying
        "salt_iodized": 1.0,    # light seasoning
    },
    "mackerel_fried": {
        "mackerel_fish": 150.0, # 1 piece
        "cooking_oil": 14.0,    # ~1 tbsp for pan-frying
        "salt_iodized": 1.0,    # light seasoning
    },
    "galunggong_fried": {
        "galunggong_fish": 160.0, # 2 pieces x 80g
        "cooking_oil": 10.0,     # absorbed oil
        "salt_iodized": 1.0,     # light seasoning
    },
}

# Yield factors by cooking method
YIELD_FACTORS = {
    "boiled": 0.9,
    "pan_fried": 0.8,
}

# Special yield for rice (water absorption, not evaporation)
RICE_YIELD = 2.0  # 65g dry rice -> ~130g cooked (without excess water)

# Step 1: Update raw_weight_grams in recipe_ingredients.json
print("=== STEP 1: Updating recipe_ingredients.json ===")
for ri in recipe_ingredients:
    label = ri["dish_label"]
    if label in WEIGHTS:
        key = ri["ingredient_key"]
        if key in WEIGHTS[label]:
            old_wt = ri["raw_weight_grams"]
            ri["raw_weight_grams"] = WEIGHTS[label][key]
            print(f"  {label} / {key}: {old_wt}g -> {WEIGHTS[label][key]}g")

with open(RECIPE_PATH, "w", encoding="utf-8") as f:
    json.dump(recipe_ingredients, f, indent=2, ensure_ascii=False)
print(f"  Saved recipe_ingredients.json\n")

# Step 2: Recompute dish_recipes.json
print("=== STEP 2: Recomputing dish_recipes.json ===")
for dish in dishes:
    label = dish["dish_label"]
    if label not in WEIGHTS:
        continue
    
    weights = WEIGHTS[label]
    
    # Calculate total raw weight (exclude water for rice since it's absorbed/drained)
    if label == "rice_well_milled":
        total_raw = weights["rice_bigas"]  # Only count the rice grain
        # Rice expands: 65g dry -> ~195g cooked (3x expansion)
        cooked_weight = round(weights["rice_bigas"] * 3.0, 1)  # ~195g cooked rice
    elif label == "egg_boiled":
        total_raw = weights["chicken_egg"]  # Only count the egg
        cooked_weight = round(weights["chicken_egg"] * 1.0, 1)  # Egg weight stays same
    else:
        total_raw = sum(weights.values())
        method = dish.get("cooking_method", "pan_fried")
        yf = YIELD_FACTORS.get(method, 0.85)
        cooked_weight = round(total_raw * yf, 1)
    
    # Sum nutrients from raw ingredients
    total_nutrients = {k: 0.0 for k in NKEYS}
    for ing_key, grams in weights.items():
        if ing_key == "water":
            continue  # Water adds no nutrients
        raw = raw_map.get(ing_key)
        if not raw:
            print(f"  WARNING: {ing_key} not found in raw_ingredients.json")
            continue
        n = raw["nutrients_per_100g"]
        for k in NKEYS:
            total_nutrients[k] += n.get(k, 0) * grams / 100.0
    
    total_nutrients = {k: round(v, 2) for k, v in total_nutrients.items()}
    
    servings = dish["servings"]  # Should be 1 for all simple dishes
    per_serving = {k: round(v / servings, 2) for k, v in total_nutrients.items()}
    per_serving_weight = round(cooked_weight / servings, 1)
    
    # Update dish
    dish["total_raw_weight_g"] = round(total_raw, 1)
    dish["cooked_weight_g"] = cooked_weight
    dish["per_serving_weight_g"] = per_serving_weight
    dish["total_nutrients_raw"] = total_nutrients
    dish["per_serving_nutrients"] = per_serving
    
    old_cal = dish.get("per_serving_nutrients", {}).get("calories", 0)
    print(f"\n  {label} ({dish.get('name_en', '')}):")
    print(f"    Raw weight: {total_raw}g  Cooked: {cooked_weight}g  Servings: {servings}")
    print(f"    Cal={per_serving['calories']:.1f}  Pro={per_serving['protein']:.1f}g  Carb={per_serving['carbs']:.1f}g  Fat={per_serving['fat']:.1f}g  Na={per_serving['sodium']:.1f}mg")

with open(DISH_PATH, "w", encoding="utf-8") as f:
    json.dump(dishes, f, indent=2, ensure_ascii=False)
print(f"\n  Saved dish_recipes.json")
print("\nDone!")
