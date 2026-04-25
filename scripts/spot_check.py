import json
data = json.load(open(r'app\src\main\assets\raw_ingredients.json', 'r', encoding='utf-8'))
checks = ['salt_iodized','cornstarch','pechay','soy_sauce','vinegar_cane','brown_rice',
          'kangkong_leaves','ampalaya','patis','tomato_sauce','rice_bigas','pork_liempo',
          'bangus_fish','chicken_thigh','chicken_drumstick','lato_seaweed','malunggay_leaves']
print(f"Total ingredients: {len(data)}")
print(f"{'KEY':30s} {'FDC':>7} {'CAL':>6} {'PRO':>6} {'CARB':>6} {'FAT':>6} {'SOD':>8}")
print("-" * 80)
for i in data:
    if i['ingredient_key'] in checks:
        n = i['nutrients_per_100g']
        print(f"{i['ingredient_key']:30s} {i['fdc_id'] or 0:>7} {n['calories']:>6} {n['protein']:>6} {n['carbs']:>6} {n['fat']:>6} {n['sodium']:>8}")
