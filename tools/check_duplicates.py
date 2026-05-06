"""Check which duplicate/critical keys are used in dish recipes."""
import csv
from collections import defaultdict

csv_path = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\dish_ingredients.csv"
usage = defaultdict(list)
with open(csv_path, "r", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        key = row["ingredient_name"].strip().lower()
        dish = row["ml_label"].strip().lower()
        usage[key].append(dish)

targets = [
    "pork_liempo", "pork_belly", "bangus_fish", "milkfish",
    "onion_bombay", "onion_red", "onion_white", "garlic", "spring_onion",
    "water", "olive_oil", "coconut_oil", "canola_oil",
    "chicken_thigh", "chicken_drumstick", "pork_shoulder", "ground_pork",
    "bihon_noodles", "canton_noodles", "brown_rice", "tuyo_fish",
    "lemon_juice", "lime_juice"
]
print("Recipe usage of key ingredients:")
print("-" * 70)
for k in targets:
    dishes = usage.get(k, [])
    unique = sorted(set(dishes))
    tag = "IN RECIPES" if unique else "NOT IN RECIPES"
    print(f"  {k:25s} [{tag}] {len(unique)} dishes: {unique}")
