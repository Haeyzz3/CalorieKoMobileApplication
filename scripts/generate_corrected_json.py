"""
Phase 2: Generate corrected raw_ingredients.json
Uses verified FDC IDs to pull exact nutrient values from the USDA API.
"""

import json
import time
import urllib.request
import os

API_KEY = "NxgG3iwRfp4HpfmejYVeRAHmJqlBTYeyKyMCHSVc"
BASE_URL = "https://api.nal.usda.gov/fdc/v1"

NUTRIENT_MAP = {
    1008: "calories", 1003: "protein", 1005: "carbs", 1004: "fat",
    1079: "fiber", 2000: "sugar", 1093: "sodium", 1092: "potassium",
    1104: "vitamin_a", 1162: "vitamin_c", 1087: "calcium", 1089: "iron",
}

# ===================================================================
# VERIFIED FDC ID MAP
# Key: ingredient_key -> (new_fdc_id, action)
# action: "update" = update FDC + re-pull nutrients
#         "keep"   = keep current FDC + re-pull nutrients (to normalize)
#         "remove" = remove this ingredient entirely
#         "manual" = keep manual values (no FDC ID)
# ===================================================================
VERIFIED_FDC = {
    # === CORRECT (30) — re-pull to normalize ===
    "chicken_egg":      (171287, "keep"),
    "tuna_fish":        (175159, "keep"),
    "chicken_breast":   (171077, "keep"),
    "pork_tenderloin":  (168249, "keep"),
    "onion_red":        (170000, "keep"),
    "onion_white":      (170000, "keep"),
    "onion_bombay":     (170000, "keep"),
    "garlic":           (169230, "keep"),
    "tomato":           (170457, "keep"),
    "tomato_red":       (170457, "keep"),
    "ginger":           (169231, "keep"),
    "cucumber":         (168409, "keep"),
    "upo":              (169232, "keep"),
    "okra":             (169260, "keep"),
    "eggplant":         (169228, "keep"),
    "cauliflower":      (169986, "keep"),
    "carrot":           (170393, "keep"),
    "bell_pepper_red":  (170108, "keep"),
    "radish":           (169276, "keep"),
    "potato":           (170026, "keep"),
    "black_pepper":     (170931, "keep"),
    "sugar_brown":      (168833, "keep"),
    "sugar_white":      (169655, "keep"),
    "all_purpose_flour":(169761, "keep"),
    "raisins":          (168165, "keep"),
    "rice_bigas":       (169756, "keep"),
    "olive_oil":        (171413, "keep"),
    "coconut_oil":      (171412, "keep"),
    "canola_oil":       (172336, "keep"),
    "lemon_juice":      (167747, "keep"),

    # === CORRECTED FDC IDs (36) ===
    "tinapa_fish":      (175116, "update"),   # Fish, herring, Atlantic, raw (smoked herring proxy)
    "pork_liempo":      (167812, "update"),   # Pork, fresh, belly, raw
    "bangus_fish":      (173675, "update"),   # Fish, milkfish, raw
    "galunggong_fish":  (175119, "update"),   # Fish, mackerel, Atlantic, raw (proxy per user)
    "milkfish":         (173675, "update"),   # Fish, milkfish, raw
    "mackerel_fish":    (175119, "update"),   # Fish, mackerel, Atlantic, raw
    "tilapya_fish":     (175176, "update"),   # Fish, tilapia, raw (was cooked)
    "chicken_thigh":    (173627, "update"),   # Chicken, thigh, meat only, raw
    "chicken_drumstick":(173614, "update"),   # Chicken, drumstick, meat only, raw
    "pork_shoulder":    (167843, "update"),   # Pork, shoulder, whole, lean and fat, raw
    "ground_pork":      (167902, "update"),   # Pork, fresh, ground, raw
    "lato_seaweed":     (169280, "update"),   # Seaweed, agar, raw
    "guso_seaweed":     (168456, "update"),   # Seaweed, irishmoss, raw
    "ampalaya":         (168393, "update"),   # Balsam-pear (bitter gourd), pods, raw
    "malunggay_leaves": (168416, "update"),   # Drumstick leaves (moringa), raw
    "gabi":             (169308, "update"),   # Taro, raw
    "kamote_tops_green":(169303, "update"),   # Sweet potato leaves, raw
    "sayote":           (170402, "update"),   # Chayote, fruit, raw
    "pechay":           (170390, "update"),   # Cabbage, chinese (pak-choi), raw
    "sitaw":            (169222, "update"),   # Yardlong bean, raw
    "baguio_beans":     (169961, "update"),   # Beans, snap, green, raw
    "kangkong_leaves":  (169301, "update"),   # Water convolvulus, raw
    "squash":           (169295, "update"),   # Squash, winter, butternut, raw
    "papaya_green":     (169926, "update"),   # Papayas, raw (proxy for green)
    "mango_unripe":     (169910, "update"),   # Mangos, raw (proxy for unripe)
    "salt_iodized":     (173468, "update"),   # Salt, table
    "vinegar_cane":     (172237, "update"),   # Vinegar, distilled
    "vinegar_white":    (172237, "update"),   # Vinegar, distilled
    "calamansi_juice":  (168156, "update"),   # Lime juice, raw
    "patis":            (174531, "update"),   # Sauce, fish, ready-to-serve
    "soy_sauce":        (174277, "update"),   # Soy sauce (shoyu)
    "thyme":            (170938, "update"),   # Spices, thyme, dried
    "cornstarch":       (169698, "update"),   # Cornstarch
    "cooking_oil":      (171411, "update"),   # Oil, soybean, salad or cooking
    "tomato_sauce":     (170054, "update"),   # Tomato products, canned, sauce
    "odong_noodles":    (168908, "update"),   # Noodles, japanese, somen, dry
    "sardines_tomato_sauce_canned": (175140, "update"),  # Sardine, Pacific, canned in tomato sauce
    "brown_rice":       (169703, "update"),   # Rice, brown, long-grain, raw
    "bihon_noodles":    (169742, "update"),   # Rice noodles, dry
    "canton_noodles":   (169731, "update"),   # Noodles, egg, dry, enriched
    "lime_juice":       (168156, "update"),   # Lime juice, raw
    "green_peas":       (170419, "keep"),     # Peas, green, raw (correct)
    "alamang_bagoong":  (391912, "keep"),     # Keep branded sautéed bagoong

    # === REMOVE ===
    "tuyo_fish":        (None, "remove"),     # User requested removal

    # === MANUAL (no API pull) ===
    "food_coloring_orange": (None, "manual"),
    "water":                (None, "manual"),
    "pansit-pansitan":      (None, "manual"),
    "sinigang_mix":         (None, "manual"),
}


def fetch_food(fdc_id):
    url = f"{BASE_URL}/food/{fdc_id}?api_key={API_KEY}"
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode())


def extract_nutrients(food_data):
    nutrients = {}
    for fn in food_data.get("foodNutrients", []):
        nutrient_obj = fn.get("nutrient", {})
        nid = nutrient_obj.get("id", 0)
        if nid in NUTRIENT_MAP:
            val = fn.get("amount", 0.0)
            nutrients[NUTRIENT_MAP[nid]] = round(val, 2) if val else 0.0
    for key in NUTRIENT_MAP.values():
        if key not in nutrients:
            nutrients[key] = 0.0
    return nutrients


def extract_portions(food_data):
    portions = []
    for p in food_data.get("foodPortions", []):
        desc = (p.get("portionDescription") or p.get("modifier") or
                p.get("measureUnit", {}).get("name", ""))
        grams = p.get("gramWeight", 0)
        if desc and grams:
            portions.append({"description": desc, "grams": round(grams, 1)})
    return portions


def main():
    json_path = os.path.normpath(os.path.join(
        os.path.dirname(__file__), "..",
        "app", "src", "main", "assets", "raw_ingredients.json"
    ))

    with open(json_path, "r", encoding="utf-8") as f:
        ingredients = json.load(f)

    # Build lookup by ingredient_key
    by_key = {i["ingredient_key"]: i for i in ingredients}

    corrected = []
    removed = []
    errors = []

    for item in ingredients:
        key = item["ingredient_key"]

        # Store-bought items: pass through unchanged
        if key not in VERIFIED_FDC:
            corrected.append(item)
            continue

        fdc_id, action = VERIFIED_FDC[key]

        if action == "remove":
            removed.append(key)
            print(f"[REMOVED] {key}")
            continue

        if action == "manual":
            # Keep existing values, no API call
            corrected.append(item)
            print(f"[MANUAL] {key} — keeping existing values")
            continue

        # "keep" or "update" — pull from API
        target_fdc = fdc_id
        print(f"[{'UPDATE' if action == 'update' else 'VERIFY'}] {key} — FDC {target_fdc}...", end=" ")

        time.sleep(0.5)
        try:
            food_data = fetch_food(target_fdc)
            usda_desc = food_data.get("description", "Unknown")
            nutrients = extract_nutrients(food_data)
            portions = extract_portions(food_data)

            # Build corrected item
            new_item = {
                "ingredient_key": item["ingredient_key"],
                "display_name": item["display_name"],
                "category": item["category"],
                "sub_category": item["sub_category"],
                "fdc_id": target_fdc,
                "data_source": "USDA_SR_LEGACY",
                "nutrients_per_100g": {
                    "calories": nutrients.get("calories", 0.0),
                    "protein": nutrients.get("protein", 0.0),
                    "carbs": nutrients.get("carbs", 0.0),
                    "fat": nutrients.get("fat", 0.0),
                    "fiber": nutrients.get("fiber", 0.0),
                    "sugar": nutrients.get("sugar", 0.0),
                    "sodium": nutrients.get("sodium", 0.0),
                    "potassium": nutrients.get("potassium", 0.0),
                    "vitamin_a": nutrients.get("vitamin_a", 0.0),
                    "vitamin_c": nutrients.get("vitamin_c", 0.0),
                    "calcium": nutrients.get("calcium", 0.0),
                    "iron": nutrients.get("iron", 0.0),
                },
                "portions": portions if portions else item.get("portions", []),
            }
            corrected.append(new_item)
            print(f"OK — \"{usda_desc}\" ({nutrients.get('calories', '?')} kcal)")

        except Exception as e:
            errors.append((key, str(e)))
            corrected.append(item)  # Keep original on error
            print(f"ERROR: {e}")

    # === Sanity checks ===
    print("\n" + "=" * 80)
    print("SANITY CHECKS")
    print("=" * 80)
    
    failures = 0
    for item in corrected:
        key = item["ingredient_key"]
        cat = item["category"]
        n = item["nutrients_per_100g"]
        cal = n.get("calories", 0)
        pro = n.get("protein", 0)
        carb = n.get("carbs", 0)
        fat = n.get("fat", 0)
        sod = n.get("sodium", 0)

        # Check 1: Calorie range by category
        if cat == "produce" and cal > 200:
            print(f"  WARN: {key} (produce) has {cal} kcal — seems high")
            failures += 1
        if cat == "protein" and (cal < 50 or cal > 600):
            print(f"  WARN: {key} (protein) has {cal} kcal — out of range")
            failures += 1
        if cat == "seasoning" and key == "salt_iodized" and cal != 0:
            print(f"  FAIL: salt has {cal} kcal — should be 0!")
            failures += 1

        # Check 2: Macronutrient calorie consistency
        if cal > 10:
            computed = (pro * 4) + (carb * 4) + (fat * 9)
            if computed > 0:
                ratio = cal / computed
                if ratio < 0.5 or ratio > 1.5:
                    print(f"  WARN: {key} cal={cal} but macro-computed={computed:.0f} (ratio={ratio:.2f})")
                    failures += 1

        # Check 3: Salt sodium
        if key == "salt_iodized" and sod < 30000:
            print(f"  FAIL: salt sodium={sod} — should be ~38,000!")
            failures += 1

    if failures == 0:
        print("  All sanity checks passed!")
    else:
        print(f"  {failures} warning(s) found — review above.")

    # === Write output ===
    output_path = json_path  # Overwrite original
    backup_path = json_path + ".backup"

    # Create backup
    with open(json_path, "r", encoding="utf-8") as f:
        backup_data = f.read()
    with open(backup_path, "w", encoding="utf-8") as f:
        f.write(backup_data)
    print(f"\nBackup saved to: {backup_path}")

    # Write corrected
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(corrected, f, indent=2, ensure_ascii=False)

    print(f"Corrected JSON written to: {output_path}")
    print(f"  Total ingredients: {len(corrected)} (was {len(ingredients)})")
    print(f"  Removed: {removed}")
    if errors:
        print(f"  Errors: {errors}")


if __name__ == "__main__":
    main()
