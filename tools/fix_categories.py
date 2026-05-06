"""Execute category reclassification for 7 misplaced ingredients."""
import json

JSON_PATH = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\raw_ingredients.json"

MOVES = {
    "cornstarch":                  "grain_starch",   # pantry_staple -> grain_starch
    "all_purpose_flour":           "grain_starch",   # pantry_staple -> grain_starch
    "odong_noodles":               "grain_starch",   # pantry_staple -> grain_starch
    "sardines_tomato_sauce_canned":"protein",         # pantry_staple -> protein
    "lemon_juice":                 "seasoning",       # pantry_staple -> seasoning
    "lime_juice":                  "seasoning",       # pantry_staple -> seasoning
    "black_beans":                 "pantry_staple",   # seasoning -> pantry_staple
}

with open(JSON_PATH, "r", encoding="utf-8") as f:
    data = json.load(f)

for item in data:
    key = item["ingredient_key"]
    if key in MOVES:
        old = item["category"]
        item["category"] = MOVES[key]
        print(f"  {key}: {old} -> {MOVES[key]}")

with open(JSON_PATH, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

print(f"\nDone! Moved {len(MOVES)} ingredients.")
