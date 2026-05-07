import json
with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\recipe_ingredients.json","r",encoding="utf-8") as f:
    data = json.load(f)
simple = ["rice_well_milled","egg_boiled","egg_sunny","egg_omelette","egg_scrambled","milkfish_fried","mackerel_fried","galunggong_fried"]
for ri in data:
    if ri["dish_label"] in simple:
        print(f"  {ri['dish_label']:25s} | {ri['ingredient_key']:25s} | wt={ri['raw_weight_grams']:>6.1f}g | portion=\"{ri['portion_original']}\"")
