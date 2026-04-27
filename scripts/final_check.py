import json

dr = json.load(open(r'app\src\main\assets\dish_recipes.json', 'r', encoding='utf-8'))
print(f"{'Dish':30s} {'Srv':>3s} {'Cal/srv':>8s} {'Pro':>6s} {'Carb':>6s} {'Fat':>6s} {'Cooked':>8s}")
print("-" * 75)
zeros = []
for d in sorted(dr, key=lambda x: x['dish_label']):
    n = d['per_serving_nutrients']
    print(f"{d['dish_label']:30s} {d['servings']:>3} {n['calories']:>8.1f} {n['protein']:>6.1f} {n['carbs']:>6.1f} {n['fat']:>6.1f} {d['cooked_weight_g']:>7.1f}g")
    if n['calories'] == 0:
        zeros.append(d['dish_label'])
print(f"\nTotal: {len(dr)} dishes")
if zeros:
    print(f"WARNING: {len(zeros)} dishes with 0 calories: {zeros}")
else:
    print("ALL dishes have non-zero calories.")
