"""Fix cooked_weight_g and per_serving_weight_g for egg_omelette and egg_scrambled."""
import json

PATH = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\dish_recipes.json"

with open(PATH, "r", encoding="utf-8") as f:
    dishes = json.load(f)

for d in dishes:
    if d["dish_label"] in ("egg_omelette", "egg_scrambled"):
        old_cooked = d["cooked_weight_g"]
        old_psw = d["per_serving_weight_g"]
        correct = round(d["total_raw_weight_g"] * d["dish_yield_factor"], 1)
        
        d["cooked_weight_g"] = correct
        d["per_serving_weight_g"] = correct
        
        print(f"{d['name_en']} ({d['dish_label']}):")
        print(f"  cooked_weight_g:      {old_cooked} -> {correct}")
        print(f"  per_serving_weight_g: {old_psw} -> {correct}")
        print(f"  (raw={d['total_raw_weight_g']} x yield={d['dish_yield_factor']} = {correct})")
        print()

with open(PATH, "w", encoding="utf-8") as f:
    json.dump(dishes, f, indent=2, ensure_ascii=False)

print("Done. dish_recipes.json updated.")
