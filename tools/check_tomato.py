"""Check tomato and rice_bigas entries."""
import json, csv
from collections import defaultdict

with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\raw_ingredients.json", "r", encoding="utf-8") as f:
    data = json.load(f)

print("=== Tomato entries in JSON ===")
for item in data:
    if "tomato" in item["ingredient_key"] and "sauce" not in item["ingredient_key"]:
        n = item["nutrients_per_100g"]
        print(f"  Key: {item['ingredient_key']}")
        print(f"  Display: {item['display_name']}")
        print(f"  FDC ID: {item.get('fdc_id', '?')}")
        print(f"  Nutrients: Cal={n['calories']} P={n['protein']} C={n['carbs']} F={n['fat']} Na={n['sodium']}")
        print()

print("=== rice_bigas in JSON ===")
for item in data:
    if item["ingredient_key"] == "rice_bigas":
        print(f"  Key: {item['ingredient_key']}")
        print(f"  Display: {item['display_name']}")
        print(f"  Category: {item['category']}")
        print(f"  Sub-category: {item['sub_category']}")
        print()

print("=== Tomato usage in CSV ===")
csv_path = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\dish_ingredients.csv"
usage = defaultdict(list)
with open(csv_path, "r", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        key = row["ingredient_name"].strip().lower()
        if "tomato" in key and "sauce" not in key:
            usage[key].append(row["ml_label"].strip())

for k, dishes in sorted(usage.items()):
    print(f"  {k} -> {len(set(dishes))} dishes: {sorted(set(dishes))}")
