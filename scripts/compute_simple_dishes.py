import json

dr = json.load(open(r'app\src\main\assets\dish_recipes.json', 'r', encoding='utf-8'))
ri = json.load(open(r'app\src\main\assets\raw_ingredients.json', 'r', encoding='utf-8'))
nutrients_by_key = {i["ingredient_key"]: i["nutrients_per_100g"] for i in ri}

# Show the reference dishes
for d in dr:
    if d['dish_label'] in ('egg_sunny', 'galunggong_grilled', 'egg_omelette', 'egg_scrambled', 'galunggong_fried'):
        print(f"\n=== {d['dish_label']} ===")
        print(f"  servings={d['servings']} method={d['cooking_method']}")
        print(f"  total_raw_weight={d['total_raw_weight_g']}g yield={d['dish_yield_factor']}")
        print(f"  cooked_weight={d['cooked_weight_g']}g per_serving={d['per_serving_weight_g']}g")
        n = d['per_serving_nutrients']
        print(f"  cal={n['calories']} pro={n['protein']} carb={n['carbs']} fat={n['fat']}")

print("\n\n=== RAW INGREDIENT VALUES (per 100g) ===")
for key in ['chicken_egg', 'cooking_oil', 'galunggong_fish', 'salt_iodized']:
    n = nutrients_by_key[key]
    print(f"  {key:25s} cal={n['calories']:>6} pro={n['protein']:>5} carb={n['carbs']:>5} fat={n['fat']:>5}")

# Compute egg_omelette/egg_scrambled following egg_sunny pattern
# egg_sunny: 1 egg (50g) + cooking_oil (5g for frying)
print("\n\n=== COMPUTED: egg_omelette / egg_scrambled (following egg_sunny) ===")
egg_w = 50.0  # 1 medium egg
oil_w = 5.0   # small amount of oil for frying
total_raw = egg_w + oil_w
egg_n = nutrients_by_key['chicken_egg']
oil_n = nutrients_by_key['cooking_oil']
total = {}
for k in egg_n:
    total[k] = round(egg_n[k] * egg_w / 100 + oil_n[k] * oil_w / 100, 2)
print(f"  raw_weight: {total_raw}g (egg={egg_w}g + oil={oil_w}g)")
print(f"  yield=0.92 → cooked={total_raw * 0.92}g")
print(f"  cal={total['calories']} pro={total['protein']} carb={total['carbs']} fat={total['fat']}")
print(f"  sodium={total['sodium']} iron={total['iron']}")

# Compute galunggong_fried following galunggong_grilled pattern
print("\n=== COMPUTED: galunggong_fried (following galunggong_grilled) ===")
# galunggong_grilled: 10 pcs × 80g each = 800g fish, divided by 5 servings
# For fried: 1 serving = ~1-2 pieces of galunggong
# galunggong_grilled per_serving_weight = 104.8g (cooked)
# Let's use: 2 pcs (160g raw fish) + 10g oil + 1g salt per serving raw
fish_w = 160.0   # 2 pieces raw
oil_w_f = 10.0   # absorbed oil
salt_w = 1.0
total_raw_f = fish_w + oil_w_f + salt_w
fish_n = nutrients_by_key['galunggong_fish']
salt_n = nutrients_by_key['salt_iodized']
total_f = {}
for k in fish_n:
    total_f[k] = round(fish_n[k] * fish_w / 100 + oil_n[k] * oil_w_f / 100 + salt_n.get(k, 0) * salt_w / 100, 2)
print(f"  raw_weight: {total_raw_f}g (fish={fish_w}g + oil={oil_w_f}g + salt={salt_w}g)")
print(f"  yield=0.8 → cooked={total_raw_f * 0.8}g")
print(f"  cal={total_f['calories']} pro={total_f['protein']} carb={total_f['carbs']} fat={total_f['fat']}")
print(f"  sodium={total_f['sodium']} iron={total_f['iron']}")
