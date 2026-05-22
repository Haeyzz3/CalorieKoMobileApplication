"""Final validation of raw_ingredients.json integrity."""
import json
import os
import re

BASE_DIR = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication"
RAW_INGREDIENTS_PATH = os.path.join(BASE_DIR, "app", "src", "main", "assets", "raw_ingredients.json")

with open(RAW_INGREDIENTS_PATH, "r", encoding="utf-8") as f:
    data = json.load(f)

required = ["ingredient_key", "display_name", "category", "sub_category", "data_source", "nutrients_per_100g"]
nutrient_fields = ["calories", "protein", "carbs", "fat", "fiber", "sugar", "sodium", "potassium", "vitamin_a", "vitamin_c", "calcium", "iron"]
removed = {"pork_belly", "milkfish", "tomato_red", "bihon_noodles", "canton_noodles", "brown_rice"}
errors = []

for item in data:
    key = item.get("ingredient_key", "UNKNOWN")
    for field in required:
        if field not in item:
            errors.append(f"{key} missing field: {field}")
    n = item.get("nutrients_per_100g", {})
    for nf in nutrient_fields:
        if nf not in n:
            errors.append(f"{key} missing nutrient: {nf}")
    if key in removed:
        errors.append(f"REMOVED key still present: {key}")

by_key = {item.get("ingredient_key"): item for item in data}

sentinels = {
    "chicken_egg": {
        "fdc_id": 171287,
        "vitamin_a": 160.0,
        "bad_values": {540.0},
    },
    "carrot": {
        "fdc_id": 170393,
        "vitamin_a": 835.0,
        "bad_values": {16706.0},
    },
    "tomato": {
        "fdc_id": 170457,
        "vitamin_a": 42.0,
        "bad_values": {833.0},
    },
    "green_peas": {
        "fdc_id": 170419,
        "vitamin_a": 38.0,
        "bad_values": {765.0},
    },
}

for key, expected in sentinels.items():
    item = by_key.get(key)
    if not item:
        errors.append(f"Sentinel missing: {key}")
        continue
    nutrients = item.get("nutrients_per_100g", {})
    actual = nutrients.get("vitamin_a")
    if item.get("fdc_id") == expected["fdc_id"] and actual != expected["vitamin_a"]:
        errors.append(f"{key} vitamin_a={actual}; expected {expected['vitamin_a']} µg RAE")
    if actual in expected["bad_values"]:
        errors.append(f"{key} vitamin_a={actual}; appears to be an obsolete international-unit value")

pipeline_files = [
    os.path.join(BASE_DIR, "tools", "fetch_usda_data.py"),
    os.path.join(BASE_DIR, "tools", "add_substitutes.py"),
    os.path.join(BASE_DIR, "tools", "execute_taxonomy_overhaul.py"),
    os.path.join(BASE_DIR, "tools", "fix_ingredient_fdc.py"),
    os.path.join(BASE_DIR, "tools", "fix_fdc_data.py"),
    os.path.join(BASE_DIR, "tools", "verify_fdc_ids.py"),
    os.path.join(BASE_DIR, "tools", "search_usda.py"),
    os.path.join(BASE_DIR, "scripts", "add_new_ingredients.py"),
    os.path.join(BASE_DIR, "scripts", "generate_corrected_json.py"),
    os.path.join(BASE_DIR, "scripts", "search_correct_fdc.py"),
    os.path.join(BASE_DIR, "scripts", "verify_ingredients.py"),
]

obsolete_id = "110" + "4"
vitamin_a_key = "vitamin_" + "a"
obsolete_mapping_pattern = re.compile(
    rf"{obsolete_id}\s*:\s*[\"']{vitamin_a_key}[\"']|[\"']{vitamin_a_key}[\"']\s*:\s*{obsolete_id}"
)
for path in pipeline_files:
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        text = f.read()
    if obsolete_mapping_pattern.search(text):
        errors.append(f"Obsolete Vitamin A mapping found in {path}")

if errors:
    for e in errors:
        print(f"ERROR: {e}")
else:
    print(f"All {len(data)} ingredients valid. No errors found.")
