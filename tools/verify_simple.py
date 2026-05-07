"""Final verification of all 8 simple dishes."""
import json

with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\dish_recipes.json", "r", encoding="utf-8") as f:
    dishes = {d["dish_label"]: d for d in json.load(f)}

simple = ["rice_well_milled", "egg_boiled", "egg_sunny", "egg_omelette",
          "egg_scrambled", "milkfish_fried", "mackerel_fried", "galunggong_fried"]

for label in simple:
    d = dishes[label]
    n = d["per_serving_nutrients"]
    print(f"  {d['name_en']:25s} | Cal={n['calories']:>6.1f} | Pro={n['protein']:>5.1f}g | Carb={n['carbs']:>5.1f}g | Fat={n['fat']:>5.1f}g | Na={n['sodium']:>6.1f}mg | RawWt={d['total_raw_weight_g']:.0f}g -> CookedWt={d['cooked_weight_g']:.0f}g")
