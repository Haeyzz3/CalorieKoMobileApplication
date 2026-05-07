"""Add portion_original text to all 8 simple dishes in recipe_ingredients.json."""
import json

PATH = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\recipe_ingredients.json"

PORTIONS = {
    ("rice_well_milled", "rice_bigas"): "1/3 cup",
    ("rice_well_milled", "water"): "1/2 cup",
    ("egg_boiled", "chicken_egg"): "1 pc",
    ("egg_boiled", "water"): "enough to cover",
    ("egg_sunny", "chicken_egg"): "1 pc",
    ("egg_sunny", "cooking_oil"): "1 tsp",
    ("egg_omelette", "chicken_egg"): "1 pc",
    ("egg_omelette", "cooking_oil"): "1 tsp",
    ("egg_scrambled", "chicken_egg"): "1 pc",
    ("egg_scrambled", "cooking_oil"): "1 tsp",
    ("milkfish_fried", "bangus_fish"): "1 pc",
    ("milkfish_fried", "cooking_oil"): "1 tbsp",
    ("milkfish_fried", "salt_iodized"): "pinch",
    ("mackerel_fried", "mackerel_fish"): "1 pc",
    ("mackerel_fried", "cooking_oil"): "1 tbsp",
    ("mackerel_fried", "salt_iodized"): "pinch",
    ("galunggong_fried", "galunggong_fish"): "2 pcs",
    ("galunggong_fried", "cooking_oil"): "2 tsps",
    ("galunggong_fried", "salt_iodized"): "pinch",
}

with open(PATH, "r", encoding="utf-8") as f:
    data = json.load(f)

updated = 0
for ri in data:
    key = (ri["dish_label"], ri["ingredient_key"])
    if key in PORTIONS:
        old = ri["portion_original"]
        ri["portion_original"] = PORTIONS[key]
        print(f"  {key[0]:25s} / {key[1]:25s} : '{old}' -> '{PORTIONS[key]}'")
        updated += 1

with open(PATH, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

print(f"\nUpdated {updated} portion texts.")
