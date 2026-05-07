import json

with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\dish_recipes.json", "r", encoding="utf-8") as f:
    dishes = json.load(f)

for d in dishes:
    if d["dish_label"] in ("egg_omelette", "egg_scrambled"):
        print(f"\n=== {d['name_en']} ({d['dish_label']}) ===")
        print(f"  total_raw_weight_g:   {d['total_raw_weight_g']}")
        print(f"  dish_yield_factor:    {d['dish_yield_factor']}")
        print(f"  cooked_weight_g:      {d['cooked_weight_g']}")
        print(f"  per_serving_weight_g: {d['per_serving_weight_g']}")
        print(f"  servings:             {d['servings']}")
        print(f"  Expected cooked:      {round(d['total_raw_weight_g'] * d['dish_yield_factor'], 1)}")
        psn = d["per_serving_nutrients"]
        print(f"  Per-serving cal:      {psn['calories']}")
        print(f"  Per-serving protein:  {psn['protein']}")
