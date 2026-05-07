"""
Audit the 8 simple dishes that have no portion text.
Traces how their nutrition was computed and checks accuracy.
"""
import json, csv
from collections import defaultdict

BASE = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets"

# Load all data
with open(f"{BASE}/raw_ingredients.json", "r", encoding="utf-8") as f:
    raw_map = {item["ingredient_key"]: item for item in json.load(f)}

with open(f"{BASE}/recipe_ingredients.json", "r", encoding="utf-8") as f:
    recipe_data = json.load(f)

with open(f"{BASE}/dish_recipes.json", "r", encoding="utf-8") as f:
    dish_map = {d["dish_label"]: d for d in json.load(f)}

# Load the original CSV values
csv_values = {}
with open(f"{BASE}/dish_labels_and_values.csv", "r", encoding="utf-8", errors="replace") as f:
    reader = csv.DictReader(f)
    for row in reader:
        label = row.get("ml_label", "").strip()
        if label:
            csv_values[label] = row

# Group recipe ingredients
dish_ingredients = defaultdict(list)
for ri in recipe_data:
    dish_ingredients[ri["dish_label"]].append(ri)

SIMPLE_DISHES = [
    "rice_well_milled",
    "egg_boiled",
    "egg_omelette",
    "egg_scrambled",
    "egg_sunny",
    "mackerel_fried",
    "milkfish_fried",
    "galunggong_fried",
]

for label in SIMPLE_DISHES:
    dish = dish_map.get(label, {})
    ingredients = dish_ingredients.get(label, [])
    csv_row = csv_values.get(label, {})
    
    print(f"{'='*80}")
    print(f"DISH: {dish.get('name_en', label)} ({label})")
    print(f"{'='*80}")
    print(f"  Servings: {dish.get('servings', '?')}  |  Cooking: {dish.get('cooking_method', '?')}")
    print(f"  Total Raw Weight: {dish.get('total_raw_weight_g', '?')}g")
    print(f"  Cooked Weight:    {dish.get('cooked_weight_g', '?')}g")
    print(f"  Per-Serving Wt:   {dish.get('per_serving_weight_g', '?')}g")
    
    # Show ingredients and their raw weights
    print(f"\n  INGREDIENTS ({len(ingredients)}):")
    all_zero = True
    for ri in ingredients:
        wt = ri.get("raw_weight_grams", 0)
        if wt > 0:
            all_zero = False
        raw = raw_map.get(ri["ingredient_key"], {})
        dn = raw.get("display_name", ri["ingredient_key"])
        print(f"    {ri['ingredient_key']:30s} | {dn:30s} | wt={wt:6.1f}g | type={ri.get('ingredient_type','?'):8s} | portion='{ri.get('portion_original','')}'")
    
    if all_zero:
        print(f"\n  >> ALL WEIGHTS ARE 0 -- nutrition comes from CSV fallback, NOT raw ingredient calc")
    
    # Current per-serving values (from dish_recipes.json)
    ps = dish.get("per_serving_nutrients", {})
    print(f"\n  CURRENT Per-Serving (dish_recipes.json):")
    print(f"    Cal={ps.get('calories', 0):>7.1f}  Pro={ps.get('protein', 0):>6.1f}g  Carb={ps.get('carbs', 0):>6.1f}g  Fat={ps.get('fat', 0):>6.1f}g  Na={ps.get('sodium', 0):>8.1f}mg")
    
    # Original CSV values
    csv_cal = float(csv_row.get("calories_kcal", 0) or 0)
    csv_pro = float(csv_row.get("protein_g", 0) or 0)
    csv_carb = float(csv_row.get("carbs_g", 0) or 0)
    csv_fat = float(csv_row.get("fat_g", 0) or 0)
    csv_na = float(csv_row.get("sodium_mg", 0) or 0)
    print(f"\n  ORIGINAL CSV Values (dish_labels_and_values.csv):")
    print(f"    Cal={csv_cal:>7.1f}  Pro={csv_pro:>6.1f}g  Carb={csv_carb:>6.1f}g  Fat={csv_fat:>6.1f}g  Na={csv_na:>8.1f}mg")
    
    # What we COULD calculate from raw ingredients (if we had proper weights)
    if not all_zero:
        total_cal = 0
        total_pro = 0
        for ri in ingredients:
            wt = ri.get("raw_weight_grams", 0)
            raw = raw_map.get(ri["ingredient_key"], {})
            n = raw.get("nutrients_per_100g", {})
            f = wt / 100.0
            total_cal += n.get("calories", 0) * f
            total_pro += n.get("protein", 0) * f
        print(f"\n  RAW INGREDIENT CALC: Cal={total_cal:.1f}  Pro={total_pro:.1f}g")
    
    # Compare: are current values same as CSV?
    if abs(ps.get("calories", 0) - csv_cal) < 1:
        print(f"\n  STATUS: Current values MATCH the CSV (using CSV fallback path)")
    else:
        print(f"\n  STATUS: Current values DIFFER from CSV")
        print(f"    Diff: Cal={ps.get('calories',0)-csv_cal:+.1f}  Pro={ps.get('protein',0)-csv_pro:+.1f}g")
    
    # Check what the raw ingredient data WOULD give for typical serving sizes
    print(f"\n  WHAT RAW CALC WOULD GIVE (with proper weights):")
    for ri in ingredients:
        raw = raw_map.get(ri["ingredient_key"], {})
        n = raw.get("nutrients_per_100g", {})
        if ri["ingredient_key"] in ["chicken_egg"]:
            wt = 50  # 1 medium egg
            print(f"    {ri['ingredient_key']}: 1 egg = ~50g -> Cal={n.get('calories',0)*0.5:.1f} Pro={n.get('protein',0)*0.5:.1f}g Fat={n.get('fat',0)*0.5:.1f}g")
        elif ri["ingredient_key"] in ["rice_bigas"]:
            wt = 65  # 1/3 cup dry rice -> ~1 cup cooked
            print(f"    {ri['ingredient_key']}: 1/3 cup dry (~65g) -> Cal={n.get('calories',0)*0.65:.1f} Pro={n.get('protein',0)*0.65:.1f}g")
        elif ri["ingredient_key"] in ["cooking_oil"]:
            wt = 14  # 1 tbsp oil
            print(f"    {ri['ingredient_key']}: 1 tbsp (~14g) -> Cal={n.get('calories',0)*0.14:.1f} Fat={n.get('fat',0)*0.14:.1f}g")
        elif "fish" in ri["ingredient_key"] or "bangus" in ri["ingredient_key"] or "mackerel" in ri["ingredient_key"] or "galunggong" in ri["ingredient_key"]:
            wt = 150  # typical fish serving
            print(f"    {ri['ingredient_key']}: 1 fish (~150g) -> Cal={n.get('calories',0)*1.5:.1f} Pro={n.get('protein',0)*1.5:.1f}g Fat={n.get('fat',0)*1.5:.1f}g")
        elif ri["ingredient_key"] == "water":
            print(f"    {ri['ingredient_key']}: variable (0 cal)")
        elif ri["ingredient_key"] == "salt_iodized":
            print(f"    {ri['ingredient_key']}: pinch (~1g) -> Na={n.get('sodium',0)*0.01:.1f}mg")
    
    print()
