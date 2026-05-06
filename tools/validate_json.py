"""Final validation of raw_ingredients.json integrity."""
import json

with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\raw_ingredients.json", "r", encoding="utf-8") as f:
    data = json.load(f)

required = ["ingredient_key", "display_name", "category", "sub_category", "data_source", "nutrients_per_100g"]
nutrient_fields = ["calories", "protein", "carbs", "fat", "fiber", "sugar", "sodium", "potassium", "vitamin_a", "vitamin_c", "calcium", "iron"]
removed = {"pork_belly", "milkfish", "tomato_red", "bihon_noodles", "canton_noodles", "brown_rice"}
errors = []

for item in data:
    key = item.get("ingredient_key", "UNKNOWN")
    for field in required:
        if field not in item:
            errors.append(f"{key} missing field: {field}")
    n = item.get("nutrients_per_100g", {})
    for nf in nutrient_fields:
        if nf not in n:
            errors.append(f"{key} missing nutrient: {nf}")
    if key in removed:
        errors.append(f"REMOVED key still present: {key}")

if errors:
    for e in errors:
        print(f"ERROR: {e}")
else:
    print(f"All {len(data)} ingredients valid. No errors found.")
