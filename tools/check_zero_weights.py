"""Verify that NO simple dishes have all-zero weights anymore."""
import json

with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\recipe_ingredients.json", "r", encoding="utf-8") as f:
    recipe_data = json.load(f)

from collections import defaultdict
dish_ingredients = defaultdict(list)
for ri in recipe_data:
    dish_ingredients[ri["dish_label"]].append(ri)

# Check which dishes still have all-zero weights
print("Dishes with ALL zero weights:")
found = False
for label, ingredients in dish_ingredients.items():
    if all(ri["raw_weight_grams"] <= 0 for ri in ingredients):
        print(f"  {label} ({len(ingredients)} ingredients)")
        found = True

if not found:
    print("  NONE! All dishes now have raw weights.")

# Also check Lechon Manok dishes
print("\nLechon Manok dishes (store-bought, expected to have 0 weights):")
for label in ["chicken_wing", "chicken_thigh", "chicken_drumstick", "chicken_breast"]:
    ings = dish_ingredients.get(label, [])
    zero = all(ri["raw_weight_grams"] <= 0 for ri in ings)
    print(f"  {label}: {len(ings)} ingredients, all_zero={zero}")
