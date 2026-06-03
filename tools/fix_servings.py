"""
Fix serving counts in dish_recipes.json to match FNRI Menu Guide.
Also recomputes per_serving_nutrients, per_serving_weight_g,
and adds serving_size_description field.
"""
import json

JSON_PATH = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\dish_recipes.json"

# FNRI actual data
FNRI = {
    "kinilaw_tuna":       {"servings": 5, "desc": "1 cup"},
    "sinuglaw_pork":      {"servings": 5, "desc": "1 cup salad + 1/4 cup pork"},
    "kwekwek":            {"servings": 5, "desc": "1 pc egg + 1/3 cup salad"},
    "egg_ampalaya":       {"servings": 5, "desc": "1 1/4 cups"},
    "sinigang_pork":      {"servings": 10, "desc": "1 1/2 cups"},
    "menudo":             {"servings": 10, "desc": "3/4 cup"},
    "udong":              {"servings": 5, "desc": "1 1/3 cups"},
    "sinabawang_bangus":  {"servings": 5, "desc": "1 pc fish + 3/4 cup vegetables + 1/4 cup soup"},
    "galunggong_grilled": {"servings": 5, "desc": "2 pieces"},
    "tilapia_fried":      {"servings": 5, "desc": "1 piece"},
    "pinakbet":           {"servings": 5, "desc": "1 cup"},
    "chopseuy":           {"servings": 5, "desc": "3 matchbox size chicken + 1 1/4 cups vegetables"},
    "chicken_tinola":     {"servings": 5, "desc": "3 matchbox size chicken + 1 cup vegetables"},
    "lawuy":              {"servings": 5, "desc": "1 cup vegetables + 1 cup soup"},
    "humba_pork":         {"servings": 5, "desc": "1/2 cup pork + 3 tbsps sauce"},
    "linatan":            {"servings": 5, "desc": "1/3 cup meat + 3/4 cup vegetables"},
}

with open(JSON_PATH, "r", encoding="utf-8") as f:
    dishes = json.load(f)

fixed = 0
for dish in dishes:
    label = dish["dish_label"]
    fnri = FNRI.get(label)
    
    if fnri:
        old_srv = dish["servings"]
        new_srv = fnri["servings"]
        
        # Add serving size description
        dish["serving_size_description"] = fnri["desc"]
        
        if old_srv != new_srv:
            dish["servings"] = new_srv
            
            # Recompute per_serving_weight_g
            dish["per_serving_weight_g"] = round(dish["cooked_weight_g"] / new_srv, 1)
            
            # Recompute per_serving_nutrients
            total = dish["total_nutrients_raw"]
            dish["per_serving_nutrients"] = {
                k: round(v / new_srv, 2) for k, v in total.items()
            }
            
            new_cal = dish["per_serving_nutrients"]["calories"]
            print(f"  FIXED {label}: {old_srv} -> {new_srv} servings (cal/srv: {total['calories']/old_srv:.1f} -> {new_cal:.1f})")
            fixed += 1
        else:
            print(f"  OK    {label}: {old_srv} servings (added serving_size_description)")
    else:
        # Simple dishes - add empty description
        dish["serving_size_description"] = ""

with open(JSON_PATH, "w", encoding="utf-8") as f:
    json.dump(dishes, f, indent=2, ensure_ascii=False)

print(f"\nDone! Fixed {fixed} serving counts, added descriptions to {len(FNRI)} dishes.")
