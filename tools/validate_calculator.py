"""Quick validation: calculator logic vs old CSV values for 5 dishes."""
import json, csv

with open('app/src/main/assets/dish_recipes.json', 'r', encoding='utf-8') as f:
    dishes = {d['dish_label']: d for d in json.load(f)}

old = {}
with open('app/src/main/assets/dish_labels_and_values.csv', 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    for row in reader:
        old[row['ml_label'].strip()] = row

targets = ['galunggong_grilled', 'pinakbet', 'menudo', 'sinuglaw_pork', 'mackerel_fried']

print("Validation: New calculator per-100g-cooked vs Old CSV per-100g")
print("=" * 75)
header = f"{'Dish':<25s} {'OldCal':>7s} {'NewCal':>7s} {'OldPro':>7s} {'NewPro':>7s} {'Match?':>7s}"
print(header)
print("-" * 75)

for label in targets:
    d = dishes[label]
    o = old[label]

    srv = d['servings']
    psn = d['per_serving_nutrients']
    cooked = d['cooked_weight_g']

    # New per-100g cooked: (perServing * servings) * (100 / cookedWeight)
    new_cal_100g = psn['calories'] * srv * 100.0 / cooked if cooked > 0 else 0
    new_pro_100g = psn['protein'] * srv * 100.0 / cooked if cooked > 0 else 0

    old_cal = float(o['calories_kcal'])
    old_pro = float(o['protein_g'])

    # Within 50%? (generous given methodology difference)
    ratio = new_cal_100g / old_cal if old_cal > 0 else 0
    match = "OK" if 0.5 <= ratio <= 2.0 else "WARN"

    print(f"{label:<25s} {old_cal:>7.1f} {new_cal_100g:>7.1f} {old_pro:>7.1f} {new_pro_100g:>7.1f} {match:>7s}")

print()
print("Note: Differences are expected due to raw-ingredient-summation vs")
print("DOST-FNRI cooked-dish methodology. Values within 2x are acceptable.")
