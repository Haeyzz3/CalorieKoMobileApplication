import json
dr = json.load(open(r'app\src\main\assets\dish_recipes.json', 'r', encoding='utf-8'))
for d in dr:
    print(f"{d['dish_label']:25s} srv={d['servings']:>2} method={d['cooking_method']:25s} yield={d['dish_yield_factor']} raw={d['total_raw_weight_g']:>8} per_srv={d['per_serving_weight_g']:>8}")
