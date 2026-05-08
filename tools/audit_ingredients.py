"""
Comprehensive audit of raw_ingredients.json for data quality issues:
1. Ingredients with no FDC ID but having nutrients
2. Duplicate FDC IDs across different ingredients  
3. Ingredients with all-zero nutrients
4. Suspicious nutrient patterns
"""
import json
from collections import defaultdict, Counter

PATH = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\raw_ingredients.json"

with open(PATH, "r", encoding="utf-8") as f:
    ingredients = json.load(f)

print(f"Total ingredients: {len(ingredients)}")
print("=" * 110)

# --- Check 1: No FDC ID but has nutrients ---
print("\n[CHECK 1] Ingredients with NO FDC ID (fdc_id = 0 or missing) but having nutrients")
print("-" * 110)
no_fdc = []
for ing in ingredients:
    fdc = ing.get("fdc_id", 0)
    nutrients = ing.get("nutrients_per_100g", {})
    has_nutrients = any(v > 0 for v in nutrients.values()) if nutrients else False
    if (fdc == 0 or fdc is None or str(fdc).strip() == "") and has_nutrients:
        no_fdc.append(ing)
        cal = nutrients.get("calories", 0)
        prot = nutrients.get("protein", 0)
        print(f"  {ing['ingredient_key']:30s} | {ing['display_name']:30s} | cal={cal:>6.1f} | prot={prot:>5.1f} | fdc_id={fdc}")

if not no_fdc:
    print("  None found.")

# --- Check 2: Duplicate FDC IDs ---
print(f"\n[CHECK 2] Duplicate FDC IDs (same FDC ID used by multiple ingredients)")
print("-" * 110)
fdc_map = defaultdict(list)
for ing in ingredients:
    fdc = ing.get("fdc_id", 0)
    if fdc and fdc > 0:
        fdc_map[fdc].append(ing)

dup_count = 0
for fdc_id, items in sorted(fdc_map.items()):
    if len(items) > 1:
        dup_count += 1
        names = [f"{i['ingredient_key']} ({i['display_name']})" for i in items]
        # Check if nutrients actually differ
        nutrient_sets = []
        for i in items:
            n = i.get("nutrients_per_100g", {})
            nutrient_sets.append(tuple(sorted(n.items())))
        all_same = len(set(nutrient_sets)) == 1
        
        print(f"\n  FDC ID {fdc_id} used by {len(items)} ingredients {'[IDENTICAL nutrients]' if all_same else '[DIFFERENT nutrients - CHECK!]'}:")
        for i in items:
            n = i.get("nutrients_per_100g", {})
            print(f"    - {i['ingredient_key']:30s} | {i['display_name']:25s} | cal={n.get('calories',0):>7.1f} | prot={n.get('protein',0):>5.1f} | sodium={n.get('sodium',0):>7.1f}")

if dup_count == 0:
    print("  None found.")
else:
    print(f"\n  Total: {dup_count} duplicate FDC ID groups")

# --- Check 3: Ingredients with ALL zero nutrients ---
print(f"\n[CHECK 3] Ingredients with ALL-ZERO nutrients (potential data gaps)")
print("-" * 110)
zero_count = 0
for ing in ingredients:
    nutrients = ing.get("nutrients_per_100g", {})
    if not nutrients or all(v == 0 for v in nutrients.values()):
        zero_count += 1
        fdc = ing.get("fdc_id", 0)
        print(f"  {ing['ingredient_key']:30s} | {ing['display_name']:30s} | fdc_id={fdc}")
if zero_count == 0:
    print("  None found.")

# --- Check 4: Water ingredient with non-zero calories ---
print(f"\n[CHECK 4] Water/non-nutritive ingredients with unexpected calories")
print("-" * 110)
water_keys = [ing for ing in ingredients if "water" in ing["ingredient_key"].lower()]
for ing in water_keys:
    n = ing.get("nutrients_per_100g", {})
    cal = n.get("calories", 0)
    if cal > 0:
        print(f"  {ing['ingredient_key']:30s} | cal={cal} (should be 0!)")
    else:
        print(f"  {ing['ingredient_key']:30s} | cal={cal} (OK)")

# --- Check 5: Store-bought items ---
print(f"\n[CHECK 5] Store-bought items (should they have FDC IDs?)")
print("-" * 110)
for ing in ingredients:
    if ing.get("category") == "store_bought" or "store_bought" in ing.get("ingredient_key", ""):
        fdc = ing.get("fdc_id", 0)
        n = ing.get("nutrients_per_100g", {})
        cal = n.get("calories", 0)
        print(f"  {ing['ingredient_key']:30s} | fdc_id={fdc:>7} | cal={cal:>7.1f}")

# --- Summary ---
print(f"\n{'=' * 110}")
print("SUMMARY")
print(f"{'=' * 110}")
print(f"  Total ingredients:           {len(ingredients)}")
print(f"  No FDC ID + has nutrients:   {len(no_fdc)}")
print(f"  Duplicate FDC ID groups:     {dup_count}")
print(f"  All-zero nutrient items:     {zero_count}")
