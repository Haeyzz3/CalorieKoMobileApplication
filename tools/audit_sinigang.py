"""Audit sinigang_pork ingredient breakdown with raw weights."""
import json
from collections import defaultdict

with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\raw_ingredients.json", "r", encoding="utf-8") as f:
    raw_map = {item["ingredient_key"]: item for item in json.load(f)}

with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\recipe_ingredients.json", "r", encoding="utf-8") as f:
    recipe_data = json.load(f)

print("=== SINIGANG_PORK Ingredient Breakdown ===")
print(f"{'Key':30s} | {'Display':30s} | {'Raw(g)':>8s} | {'Cal':>8s} | {'Pro(g)':>8s} | {'Carb(g)':>8s} | {'Fat(g)':>8s} | {'Na(mg)':>8s}")
print(f"{'-'*30}-+-{'-'*30}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}")

totals = {"cal": 0, "pro": 0, "carb": 0, "fat": 0, "na": 0}
total_wt = 0

for ri in recipe_data:
    if ri["dish_label"] != "sinigang_pork":
        continue
    key = ri["ingredient_key"]
    wt = ri.get("raw_weight_grams", 0)
    total_wt += wt
    raw = raw_map.get(key)
    if not raw:
        print(f"{key:30s} | {'NOT FOUND':30s} | {wt:8.1f} |")
        continue
    n = raw["nutrients_per_100g"]
    f = wt / 100.0
    cal = n["calories"] * f
    pro = n["protein"] * f
    carb = n["carbs"] * f
    fat = n["fat"] * f
    na = n["sodium"] * f
    totals["cal"] += cal
    totals["pro"] += pro
    totals["carb"] += carb
    totals["fat"] += fat
    totals["na"] += na
    print(f"{key:30s} | {raw['display_name']:30s} | {wt:8.1f} | {cal:8.1f} | {pro:8.2f} | {carb:8.2f} | {fat:8.2f} | {na:8.1f}")

print(f"{'-'*30}-+-{'-'*30}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}-+-{'-'*8}")
print(f"{'TOTAL':30s} | {'':30s} | {total_wt:8.1f} | {totals['cal']:8.1f} | {totals['pro']:8.2f} | {totals['carb']:8.2f} | {totals['fat']:8.2f} | {totals['na']:8.1f}")
print(f"\nPer serving (/ 10): Cal={totals['cal']/10:.1f}  Pro={totals['pro']/10:.1f}g  Carb={totals['carb']/10:.1f}g  Fat={totals['fat']/10:.1f}g  Na={totals['na']/10:.1f}mg")

# Flag the big contributors
print("\n=== TOP CONTRIBUTORS ===")
print(f"  Cooking Oil:   1/2 cup = how many grams?")
print(f"  Pork Liempo:   4 cups lean & sliced = how many grams?")

# Also check the FDC IDs for key ingredients
for key in ["cooking_oil", "pork_liempo", "kangkong_leaves", "salt_iodized", "sinigang_mix"]:
    raw = raw_map.get(key)
    if raw:
        n = raw["nutrients_per_100g"]
        print(f"\n  {key} (FDC {raw.get('fdc_id', '?')}):")
        print(f"    Per 100g: Cal={n['calories']} Pro={n['protein']} Fat={n['fat']} Na={n['sodium']}")
