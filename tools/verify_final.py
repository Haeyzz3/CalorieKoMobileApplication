"""Final verification of all modified/added ingredients."""
import json

with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\raw_ingredients.json", "r", encoding="utf-8") as f:
    data = json.load(f)

targets = [
    "corn_oil", "palm_oil", "sea_salt", "kosher_salt",
    "vinegar_coconut", "vinegar_apple_cider", "shallot",
    "distilled_water", "mineral_water", "water",
    "onion_bombay", "rice_bigas", "cooking_oil", "olive_oil",
    "salt_iodized", "onion_red", "onion_white"
]

print(f"Total ingredients: {len(data)}")
print()
for item in data:
    if item["ingredient_key"] in targets:
        n = item["nutrients_per_100g"]
        sub = item.get("is_substitutable", "?")
        print(f"  {item['ingredient_key']:25s} | {item['display_name']:35s}")
        print(f"    cat={item['category']:15s} sub_cat={item['sub_category']:12s} substitutable={sub}")
        print(f"    Cal={n['calories']:6.1f}  P={n['protein']:5.2f}  F={n['fat']:6.2f}  C={n['carbs']:5.2f}  Na={n['sodium']:8.1f}")
        print()
