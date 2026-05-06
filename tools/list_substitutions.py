"""Lists all ingredients grouped by sub_category to review substitution taxonomy."""
import json
from collections import defaultdict

json_path = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\raw_ingredients.json"

with open(json_path, "r", encoding="utf-8") as f:
    data = json.load(f)

# Group by sub_category
groups = defaultdict(list)
for item in data:
    sc = item.get("sub_category", "(none)")
    is_sub = item.get("is_substitutable", True)
    groups[sc].append({
        "key": item["ingredient_key"],
        "display": item["display_name"],
        "category": item["category"],
        "is_substitutable": is_sub
    })

# Also track which keys are used in dish recipes
import csv
recipe_keys = set()
csv_path = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\dish_ingredients.csv"
with open(csv_path, "r", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        recipe_keys.add(row["ingredient_name"].strip().lower())

print(f"Total ingredients in raw_ingredients.json: {len(data)}")
print(f"Total ingredients used in dish recipes (CSV): {len(recipe_keys)}")
print()

for sc in sorted(groups.keys()):
    items = groups[sc]
    substitutable = [it for it in items if it["is_substitutable"]]
    print(f"=== {sc} ({len(items)} total, {len(substitutable)} substitutable) ===")
    for it in items:
        sub_flag = " [NOT SUBSTITUTABLE]" if not it["is_substitutable"] else ""
        in_recipe = " [IN RECIPES]" if it["key"] in recipe_keys else " [SUBSTITUTE ONLY]"
        print(f"  {it['key']:30s} | {it['display']:40s} | {it['category']:15s}{sub_flag}{in_recipe}")
    print()
