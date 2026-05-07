"""
Audit all dishes for potential water/cooking-medium weight issues.
Checks:
1. Dishes where water is a large % of total raw weight (cooking medium, not consumed)
2. Yield factor sanity vs cooking method
3. Compares total_raw_weight_g against sum of ingredient weights
"""
import json
from collections import defaultdict

BASE = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets"

with open(f"{BASE}/dish_recipes.json", "r", encoding="utf-8") as f:
    dishes = {d["dish_label"]: d for d in json.load(f)}

with open(f"{BASE}/recipe_ingredients.json", "r", encoding="utf-8") as f:
    recipe_data = json.load(f)

# Group ingredients by dish
dish_ingredients = defaultdict(list)
for ri in recipe_data:
    dish_ingredients[ri["dish_label"]].append(ri)

# Expected yield factors by cooking method
EXPECTED_YIELDS = {
    "boiled": (0.85, 1.0),       # Boiled dishes lose some water, but eggs don't
    "simmered": (0.8, 1.0),      # Stews/soups keep the liquid
    "pan_fried": (0.75, 0.92),   # Frying loses moisture
    "deep_fried": (0.7, 0.95),   # Deep frying
    "grilled": (0.7, 0.85),      # Grilling loses moisture
    "raw_cured": (0.9, 1.0),     # Raw/cured minimal change
    "stir_fried": (0.75, 0.9),   # Stir frying
    "braised": (0.8, 1.0),       # Braising
}

print("=" * 100)
print("DISH WEIGHT AUDIT")
print("=" * 100)

issues_found = []

for label in sorted(dishes.keys()):
    dish = dishes[label]
    ings = dish_ingredients.get(label, [])
    
    # Calculate ingredient weight totals
    total_ing_weight = sum(ri["raw_weight_grams"] for ri in ings)
    water_weight = sum(ri["raw_weight_grams"] for ri in ings if ri["ingredient_key"] == "water")
    non_water_weight = total_ing_weight - water_weight
    water_pct = (water_weight / total_ing_weight * 100) if total_ing_weight > 0 else 0
    
    dish_raw_wt = dish["total_raw_weight_g"]
    dish_yield = dish["dish_yield_factor"]
    dish_cooked = dish["cooked_weight_g"]
    method = dish["cooking_method"]
    servings = dish["servings"]
    
    # Check 1: Does total_raw_weight_g match sum of ingredient weights?
    weight_mismatch = abs(dish_raw_wt - total_ing_weight) > 1.0 if total_ing_weight > 0 else False
    
    # Check 2: Is water a large % of total weight? (potential cooking medium issue)
    water_issue = water_weight > 0 and water_pct > 30
    
    # Check 3: Yield factor sanity
    expected = EXPECTED_YIELDS.get(method, (0.7, 1.0))
    yield_issue = dish_yield < expected[0] or dish_yield > expected[1]
    
    # Check 4: Does cooked weight = raw weight * yield?
    expected_cooked = round(dish_raw_wt * dish_yield, 1)
    cooked_mismatch = abs(dish_cooked - expected_cooked) > 1.0
    
    # Check 5: Per serving weight sanity
    expected_per_srv = round(dish_cooked / servings, 1) if servings > 0 else 0
    srv_wt_mismatch = abs(dish["per_serving_weight_g"] - expected_per_srv) > 1.0
    
    has_issue = weight_mismatch or water_issue or yield_issue or cooked_mismatch or srv_wt_mismatch
    
    if has_issue:
        issues_found.append(label)
        print(f"\n{'[!]' if has_issue else '[OK]'} {dish['name_en']} ({label})")
        print(f"   Method: {method}  |  Servings: {servings}  |  Yield: {dish_yield}")
        print(f"   Dish raw_wt: {dish_raw_wt}g  |  Sum of ingredients: {total_ing_weight:.1f}g")
        print(f"   Water: {water_weight:.1f}g ({water_pct:.0f}% of total)  |  Non-water: {non_water_weight:.1f}g")
        print(f"   Cooked: {dish_cooked}g (expected: {expected_cooked}g)  |  Per-serving: {dish['per_serving_weight_g']}g")
        
        if weight_mismatch:
            print(f"   [X] WEIGHT MISMATCH: dish says {dish_raw_wt}g, ingredients sum to {total_ing_weight:.1f}g")
        if water_issue:
            print(f"   [!] WATER CHECK: {water_pct:.0f}% of weight is water - is it consumed or just cooking medium?")
        if yield_issue:
            print(f"   [X] YIELD FACTOR: {dish_yield} is outside expected range {expected} for '{method}'")
        if cooked_mismatch:
            print(f"   [X] COOKED WEIGHT: {dish_cooked}g != raw({dish_raw_wt}) × yield({dish_yield}) = {expected_cooked}g")
        if srv_wt_mismatch:
            print(f"   [X] PER-SERVING: {dish['per_serving_weight_g']}g != cooked({dish_cooked}) / servings({servings}) = {expected_per_srv}g")

# Also check dishes with all-zero weights (Lechon Manok)
print(f"\n{'='*100}")
print("DISHES WITH ALL-ZERO INGREDIENT WEIGHTS (expected: only Lechon Manok)")
print(f"{'='*100}")
for label in sorted(dishes.keys()):
    ings = dish_ingredients.get(label, [])
    if all(ri["raw_weight_grams"] <= 0 for ri in ings):
        dish = dishes[label]
        print(f"  {label:30s} | {dish['name_en']:30s} | raw={dish['total_raw_weight_g']}g | cal={dish['per_serving_nutrients']['calories']:.0f}")

print(f"\n{'='*100}")
print(f"SUMMARY: {len(issues_found)} dishes flagged out of {len(dishes)} total")
if issues_found:
    print(f"Flagged: {', '.join(issues_found)}")
else:
    print("No issues found! All dishes look clean.")
print(f"{'='*100}")
