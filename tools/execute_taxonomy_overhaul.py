"""
Executes the Substitution Taxonomy Overhaul plan on raw_ingredients.json.

Changes:
1. Remove 3 duplicate entries (pork_belly, milkfish, tomato_red)
2. Remove 3 obsolete substitute-only entries (bihon_noodles, canton_noodles, brown_rice)
3. Reclassify rice_bigas category to grain_starch
4. Rename display names (onion_bombay -> Yellow Onion, water -> Purified Water)
5. Split allium sub_category into onion/garlic/scallion
6. Set is_substitutable=false for locked sub_categories
7. Set is_substitutable=true for water (was false)
8. Add new ingredients (salts, vinegars, water types, shallot, palm_oil, corn_oil) via USDA API
"""
import json
import urllib.request
import time
import os

API_KEY = "NxgG3iwRfp4HpfmejYVeRAHmJqlBTYeyKyMCHSVc"
API_BASE = "https://api.nal.usda.gov/fdc/v1"

JSON_PATH = os.path.normpath(os.path.join(
    os.path.dirname(__file__), "..",
    "app", "src", "main", "assets", "raw_ingredients.json"
))

# Nutrient IDs we need
NUTRIENT_MAP = {
    1008: "calories", 1003: "protein", 1005: "carbs", 1004: "fat",
    1079: "fiber", 2000: "sugar", 1093: "sodium", 1092: "potassium",
    1106: "vitamin_a", 1162: "vitamin_c", 1087: "calcium", 1089: "iron",
}

# Keys to remove entirely
KEYS_TO_REMOVE = {
    "pork_belly", "milkfish", "tomato_red",       # duplicates
    "bihon_noodles", "canton_noodles", "brown_rice" # obsolete substitute-only
}

# Sub-categories to lock (is_substitutable = false)
LOCK_SUB_CATEGORIES = {
    "fish_freshwater", "fish_saltwater", "fish_smoked",      # fish
    "leafy_green", "gourd", "legume_veg", "nightshade",      # produce
    "root", "fruit", "seaweed", "cruciferous",               # produce
    "garlic", "scallion",                                     # split from allium
    "noodle", "grain", "flour", "starch",                    # pantry
    "fermented", "herb", "spice", "canned_good",             # seasonings
    "seasoning_mix", "legume", "dried_fruit",                # seasonings
    "egg",                                                    # protein singleton
}

# Sub-categories that should be substitutable
UNLOCK_SUB_CATEGORIES = {
    "water", "onion", "oil", "citrus_juice", "acid",
    "sweetener", "salt", "poultry", "pork",
}

# Allium split mapping: ingredient_key -> new sub_category
ALLIUM_SPLIT = {
    "onion_red": "onion",
    "onion_white": "onion",
    "onion_bombay": "onion",
    "garlic": "garlic",
    "spring_onion": "scallion",
}

# Display name renames
DISPLAY_RENAMES = {
    "onion_bombay": "Yellow Onion",
    "water": "Purified Water",
}

# New ingredients to fetch from USDA and add
NEW_INGREDIENTS = [
    # (key, display_name, category, sub_category, fdc_id)
    ("palm_oil",             "Palm Oil",                      "pantry_staple", "oil",         171015),
    ("corn_oil",             "Corn Oil",                      "pantry_staple", "oil",         170375),
    ("kosher_salt",          "Kosher Salt",                   "seasoning",     "salt",        173468),
    ("sea_salt",             "Sea Salt",                      "seasoning",     "salt",        173470),
    ("vinegar_coconut",      "Coconut Vinegar (Sukang Tuba)", "seasoning",     "acid",        171868),
    ("vinegar_apple_cider",  "Apple Cider Vinegar",           "seasoning",     "acid",        173469),
    ("shallot",              "Shallot",                       "produce",       "onion",       170400),
]

# Water types — added manually (all 0-calorie, no USDA fetch needed)
WATER_TYPES = [
    {
        "ingredient_key": "distilled_water",
        "display_name": "Distilled Water",
        "category": "pantry_staple",
        "sub_category": "water",
        "fdc_id": 0,
        "data_source": "MANUAL",
        "is_substitutable": True,
        "nutrients_per_100g": {
            "calories": 0.0, "protein": 0.0, "carbs": 0.0, "fat": 0.0,
            "fiber": 0.0, "sugar": 0.0, "sodium": 0.0, "potassium": 0.0,
            "vitamin_a": 0.0, "vitamin_c": 0.0, "calcium": 0.0, "iron": 0.0
        },
        "portions": []
    },
    {
        "ingredient_key": "mineral_water",
        "display_name": "Mineral Water",
        "category": "pantry_staple",
        "sub_category": "water",
        "fdc_id": 0,
        "data_source": "MANUAL",
        "is_substitutable": True,
        "nutrients_per_100g": {
            "calories": 0.0, "protein": 0.0, "carbs": 0.0, "fat": 0.0,
            "fiber": 0.0, "sugar": 0.0, "sodium": 4.0, "potassium": 1.0,
            "vitamin_a": 0.0, "vitamin_c": 0.0, "calcium": 10.0, "iron": 0.0
        },
        "portions": []
    },
]


def fetch_food(fdc_id: int) -> dict:
    url = f"{API_BASE}/food/{fdc_id}?api_key={API_KEY}"
    req = urllib.request.Request(url)
    req.add_header("Accept", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode())
    except Exception as e:
        print(f"  ERROR fetching FDC {fdc_id}: {e}")
        return None


def extract_nutrients(food_data: dict) -> dict:
    nutrients = {name: 0.0 for name in NUTRIENT_MAP.values()}
    for fn in food_data.get("foodNutrients", []):
        nid = fn.get("nutrient", {}).get("id") or fn.get("nutrientId")
        if nid in NUTRIENT_MAP:
            amount = fn.get("amount", 0.0)
            if amount is None:
                amount = 0.0
            nutrients[NUTRIENT_MAP[nid]] = round(amount, 2)
    return nutrients


def extract_portions(food_data: dict) -> list:
    portions = []
    for pm in food_data.get("foodPortions", []):
        desc = pm.get("portionDescription") or pm.get("measureUnit", {}).get("name", "")
        grams = pm.get("gramWeight", 0.0)
        if desc and grams and grams > 0:
            portions.append({"description": desc, "grams": round(grams, 1)})
    return portions[:3]


def main():
    print(f"Loading {JSON_PATH}...")
    with open(JSON_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
    print(f"  Loaded {len(data)} ingredients")

    existing_keys = {item["ingredient_key"] for item in data}

    # --- Step 1: Remove duplicates and obsolete entries ---
    before_count = len(data)
    data = [item for item in data if item["ingredient_key"] not in KEYS_TO_REMOVE]
    removed = before_count - len(data)
    print(f"\n[1] Removed {removed} entries: {KEYS_TO_REMOVE}")

    # --- Step 2: Reclassify rice_bigas ---
    for item in data:
        if item["ingredient_key"] == "rice_bigas":
            item["category"] = "grain_starch"
            print(f"\n[2] Reclassified rice_bigas: pantry_staple -> grain_starch")

    # --- Step 3: Rename display names ---
    for item in data:
        if item["ingredient_key"] in DISPLAY_RENAMES:
            old = item["display_name"]
            item["display_name"] = DISPLAY_RENAMES[item["ingredient_key"]]
            print(f"\n[3] Renamed {item['ingredient_key']}: '{old}' -> '{item['display_name']}'")

    # --- Step 4: Split allium sub_category ---
    for item in data:
        if item["ingredient_key"] in ALLIUM_SPLIT:
            old_sc = item["sub_category"]
            item["sub_category"] = ALLIUM_SPLIT[item["ingredient_key"]]
            print(f"[4] Split allium: {item['ingredient_key']} -> sub_category='{item['sub_category']}'")

    # --- Step 5: Set is_substitutable flags ---
    locked = 0
    unlocked = 0
    for item in data:
        sc = item.get("sub_category", "")
        if sc in LOCK_SUB_CATEGORIES:
            item["is_substitutable"] = False
            locked += 1
        elif sc in UNLOCK_SUB_CATEGORIES:
            item["is_substitutable"] = True
            unlocked += 1
        # Keep existing flags for anything not in either set
    print(f"\n[5] Locked {locked} ingredients, unlocked {unlocked} ingredients")

    # --- Step 6: Add new USDA-backed ingredients ---
    added = 0
    for key, display, category, sub_cat, fdc_id in NEW_INGREDIENTS:
        if key in existing_keys:
            print(f"  SKIP {key} (already exists)")
            continue

        print(f"\n[6] Fetching {key} (FDC {fdc_id})...")
        food_data = fetch_food(fdc_id)
        if not food_data:
            print(f"  FAILED to fetch {key} — skipping")
            continue

        nutrients = extract_nutrients(food_data)
        portions = extract_portions(food_data)

        entry = {
            "ingredient_key": key,
            "display_name": display,
            "category": category,
            "sub_category": sub_cat,
            "fdc_id": fdc_id,
            "data_source": "USDA_FDC",
            "is_substitutable": True,
            "nutrients_per_100g": nutrients,
            "portions": portions
        }
        data.append(entry)
        existing_keys.add(key)
        added += 1
        print(f"    Added: {key} | {nutrients['calories']} kcal | P:{nutrients['protein']}g F:{nutrients['fat']}g C:{nutrients['carbs']}g")
        time.sleep(0.5)

    # --- Step 7: Add water types (manual, no API) ---
    for wt in WATER_TYPES:
        if wt["ingredient_key"] not in existing_keys:
            data.append(wt)
            existing_keys.add(wt["ingredient_key"])
            print(f"\n[7] Added water type: {wt['ingredient_key']} ({wt['display_name']})")
            added += 1

    # --- Save ---
    with open(JSON_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    print(f"\n{'='*60}")
    print(f"Done! Final count: {len(data)} ingredients (was {before_count})")
    print(f"  Removed: {removed}")
    print(f"  Added: {added}")
    print(f"  Net change: {added - removed:+d}")


if __name__ == "__main__":
    main()
