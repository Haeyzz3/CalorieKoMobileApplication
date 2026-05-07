"""
Deep audit of per-serving nutrition for linatan and sinigang_pork.
Traces the full calculation pipeline: raw ingredients → summation → per-serving.
"""
import json
from collections import defaultdict

# Load all data sources
with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\raw_ingredients.json", "r", encoding="utf-8") as f:
    raw_data = json.load(f)
raw_map = {item["ingredient_key"]: item for item in raw_data}

with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\recipe_ingredients.json", "r", encoding="utf-8") as f:
    recipe_data = json.load(f)

with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\dish_recipes.json", "r", encoding="utf-8") as f:
    dish_data = json.load(f)
dish_map = {d["dish_label"]: d for d in dish_data}

# Group recipe ingredients by dish
dish_ingredients = defaultdict(list)
for ri in recipe_data:
    dish_ingredients[ri["dish_label"]].append(ri)


def audit_dish(dish_label):
    dish = dish_map.get(dish_label)
    if not dish:
        print(f"  ERROR: dish {dish_label} not found in dish_recipes.json")
        return

    print(f"{'='*80}")
    print(f"DISH: {dish.get('name_en', '?')} ({dish_label})")
    print(f"{'='*80}")
    print(f"  Servings:           {dish.get('servings', '?')}")
    print(f"  Cooked Weight (g):  {dish.get('cooked_weight_g', '?')}")
    print(f"  Per-Serving Wt (g): {dish.get('per_serving_weight_g', '?')}")
    print(f"  Yield Factor:       {dish.get('yield_factor', '?')}")
    print()

    # Pre-computed per-serving values from dish_recipes.json
    ps = dish.get("per_serving", {})
    print("  PRE-COMPUTED Per-Serving (from dish_recipes.json):")
    print(f"    Calories: {ps.get('calories', '?')}")
    print(f"    Protein:  {ps.get('protein', '?')}g")
    print(f"    Carbs:    {ps.get('carbs', '?')}g")
    print(f"    Fat:      {ps.get('fat', '?')}g")
    print(f"    Sodium:   {ps.get('sodium', '?')}mg")
    print(f"    Fiber:    {ps.get('fiber', '?')}g")
    print()

    # Calculate from raw ingredients
    ingredients = dish_ingredients.get(dish_label, [])
    if not ingredients:
        print("  WARNING: No recipe ingredients found!")
        return

    print(f"  INGREDIENT BREAKDOWN ({len(ingredients)} ingredients):")
    print(f"  {'Key':30s} | {'Display':30s} | {'Raw(g)':>8s} | {'Cal':>8s} | {'Pro(g)':>8s} | {'Carb(g)':>8s} | {'Fat(g)':>8s} | {'Na(mg)':>8s}")
    print(f"  {'-'*30}-+-{'-'*30}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}")

    totals = {"calories": 0, "protein": 0, "carbs": 0, "fat": 0, "sodium": 0, "fiber": 0}
    total_raw_weight = 0

    for ri in ingredients:
        key = ri["ingredient_key"]
        raw_wt = ri.get("raw_weight_grams", 0)
        total_raw_weight += raw_wt

        raw = raw_map.get(key)
        if not raw:
            print(f"  {key:30s} | {'NOT IN RAW DB':30s} | {raw_wt:8.1f} | {'?':>8s} | {'?':>8s} | {'?':>8s} | {'?':>8s} | {'?':>8s}")
            continue

        n = raw["nutrients_per_100g"]
        factor = raw_wt / 100.0

        cal = n["calories"] * factor
        pro = n["protein"] * factor
        carb = n["carbs"] * factor
        fat = n["fat"] * factor
        na = n["sodium"] * factor
        fib = n["fiber"] * factor

        totals["calories"] += cal
        totals["protein"] += pro
        totals["carbs"] += carb
        totals["fat"] += fat
        totals["sodium"] += na
        totals["fiber"] += fib

        print(f"  {key:30s} | {raw['display_name']:30s} | {raw_wt:8.1f} | {cal:8.1f} | {pro:8.2f} | {carb:8.2f} | {fat:8.2f} | {na:8.1f}")

    print(f"  {'-'*30}-+-{'-'*30}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}")
    print(f"  {'TOTAL (full batch)':30s} | {'':30s} | {total_raw_weight:8.1f} | {totals['calories']:8.1f} | {totals['protein']:8.2f} | {totals['carbs']:8.2f} | {totals['fat']:8.2f} | {totals['sodium']:8.1f}")

    servings = dish.get("servings", 1)
    print(f"\n  CALCULATED Per-Serving (÷ {servings} servings):")
    print(f"    Calories: {totals['calories']/servings:.1f}")
    print(f"    Protein:  {totals['protein']/servings:.2f}g")
    print(f"    Carbs:    {totals['carbs']/servings:.2f}g")
    print(f"    Fat:      {totals['fat']/servings:.2f}g")
    print(f"    Sodium:   {totals['sodium']/servings:.1f}mg")
    print(f"    Fiber:    {totals['fiber']/servings:.2f}g")

    # Compare with pre-computed
    print(f"\n  COMPARISON (Pre-computed vs Calculated):")
    fields = [("calories", "Calories"), ("protein", "Protein"), ("carbs", "Carbs"),
              ("fat", "Fat"), ("sodium", "Sodium"), ("fiber", "Fiber")]
    for fkey, flabel in fields:
        pre = ps.get(fkey, 0)
        calc = totals[fkey] / servings
        diff = calc - pre if pre else 0
        pct = (diff / pre * 100) if pre else 0
        flag = " WARN" if abs(pct) > 10 else " OK"
        print(f"    {flabel:12s}: pre={pre:8.1f}  calc={calc:8.1f}  diff={diff:+8.1f} ({pct:+6.1f}%){flag}")

    print()


# Audit both dishes
audit_dish("linatan")
audit_dish("sinigang_pork")
