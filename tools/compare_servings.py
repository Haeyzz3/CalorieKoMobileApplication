"""Compare current serving counts with FNRI actuals and show impact on per-serving nutrition."""
import json

with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\dish_recipes.json", "r", encoding="utf-8") as f:
    dishes = {d["dish_label"]: d for d in json.load(f)}

# FNRI actual data provided by user
FNRI = {
    "kinilaw_tuna":       {"servings": 5, "size_desc": "1 cup"},
    "sinuglaw_pork":      {"servings": 5, "size_desc": "1 cup salad + 1/4 cup pork"},
    "kwekwek":            {"servings": 5, "size_desc": "1 pc egg + 1/3 cup salad"},
    "egg_ampalaya":       {"servings": 5, "size_desc": "1 1/4 cups"},
    "sinigang_pork":      {"servings": 10, "size_desc": "1 1/2 cups"},
    "menudo":             {"servings": 10, "size_desc": "3/4 cup"},
    "udong":              {"servings": 5, "size_desc": "1 1/3 cups"},
    "sinabawang_bangus":  {"servings": 5, "size_desc": "1 pc fish + 3/4 cup veg + 1/4 cup soup"},
    "galunggong_grilled": {"servings": 5, "size_desc": "2 pieces"},
    "tilapia_fried":      {"servings": 5, "size_desc": "1 piece"},
    "pinakbet":           {"servings": 5, "size_desc": "1 cup"},
    "chopseuy":           {"servings": 5, "size_desc": "3 matchbox chicken + 1 1/4 cups veg"},
    "chicken_tinola":     {"servings": 5, "size_desc": "3 matchbox chicken + 1 cup veg"},
    "lawuy":              {"servings": 5, "size_desc": "1 cup veg + 1 cup soup"},
    "humba_pork":         {"servings": 5, "size_desc": "1/2 cup pork + 3 tbsps sauce"},
    "linatan":            {"servings": 5, "size_desc": "1/3 cup meat + 3/4 cup veg"},
}

print(f"{'Dish':25s} | {'Curr':>4s} | {'FNRI':>4s} | {'Match':>5s} | {'Curr Cal/srv':>12s} | {'FNRI Cal/srv':>12s} | {'Diff':>8s} | Serving Size")
print(f"{'-'*25}-+-{'-'*4}-+-{'-'*4}-+-{'-'*5}-+-{'-'*12}-+-{'-'*12}-+-{'-'*8}-+-{'-'*40}")

for label, fnri in sorted(FNRI.items()):
    dish = dishes.get(label)
    if not dish:
        print(f"{label:25s} | NOT FOUND")
        continue
    
    curr_srv = dish["servings"]
    fnri_srv = fnri["servings"]
    match = "OK" if curr_srv == fnri_srv else "WRONG"
    
    # Current per-serving cal
    curr_cal = dish.get("per_serving_nutrients", {}).get("calories", 0)
    curr_na = dish.get("per_serving_nutrients", {}).get("sodium", 0)
    
    # What it SHOULD be with FNRI servings
    total_cal = dish.get("total_nutrients_raw", {}).get("calories", 0)
    total_na = dish.get("total_nutrients_raw", {}).get("sodium", 0)
    fnri_cal = total_cal / fnri_srv if fnri_srv > 0 else 0
    fnri_na = total_na / fnri_srv if fnri_srv > 0 else 0
    
    cal_diff = fnri_cal - curr_cal
    
    print(f"{label:25s} | {curr_srv:4d} | {fnri_srv:4d} | {match:>5s} | {curr_cal:12.1f} | {fnri_cal:12.1f} | {cal_diff:+8.1f} | {fnri['size_desc']}")

print(f"\n{'='*120}")
print("\nDISHES WITH WRONG SERVING COUNT:")
for label, fnri in sorted(FNRI.items()):
    dish = dishes.get(label)
    if dish and dish["servings"] != fnri["servings"]:
        total = dish.get("total_nutrients_raw", {})
        curr = dish["servings"]
        correct = fnri["servings"]
        print(f"\n  {label}:")
        print(f"    Servings: {curr} -> {correct}")
        print(f"    Cal/srv:  {total.get('calories',0)/curr:.1f} -> {total.get('calories',0)/correct:.1f}")
        print(f"    Pro/srv:  {total.get('protein',0)/curr:.1f}g -> {total.get('protein',0)/correct:.1f}g")
        print(f"    Fat/srv:  {total.get('fat',0)/curr:.1f}g -> {total.get('fat',0)/correct:.1f}g")
        print(f"    Na/srv:   {total.get('sodium',0)/curr:.1f}mg -> {total.get('sodium',0)/correct:.1f}mg")
