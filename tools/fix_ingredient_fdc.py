"""
Fix duplicate/missing FDC IDs in raw_ingredients.json.
Updates 7 ingredients with correct FDC IDs and pulls fresh nutrients from USDA API.

Changes:
  onion_red:        170000 -> 790577  (Red onion, Foundation)
  onion_white:      170000 -> 1104962 (White onion, Foundation)
  onion_bombay:     170000 -> 790646  (Yellow onion, Foundation)
  salt_iodized:     173468 -> 746775  (Iodized salt, Foundation)
  vinegar_cane:     172237 -> 173469  (Cider vinegar, SR Legacy)
  vinegar_coconut:  172237 -> 173469  (Cider vinegar, SR Legacy)
  calamansi_juice:  168156 -> 167747  (Lemon juice, SR Legacy)
  pansit-pansitan:  null   -> 170068  (Watercress, SR Legacy)
"""
import json
import time
import urllib.request
import os
import copy

API_KEY = "NxgG3iwRfp4HpfmejYVeRAHmJqlBTYeyKyMCHSVc"
BASE_URL = "https://api.nal.usda.gov/fdc/v1"

NUTRIENT_MAP = {
    1008: "calories", 1003: "protein", 1005: "carbs", 1004: "fat",
    1079: "fiber", 2000: "sugar", 1093: "sodium", 1092: "potassium",
    1106: "vitamin_a", 1162: "vitamin_c", 1087: "calcium", 1089: "iron",
}

# Ingredients to update: ingredient_key -> (new_fdc_id, data_source_label)
UPDATES = {
    "onion_red":        (790577,  "USDA_FOUNDATION"),
    "onion_white":      (1104962, "USDA_FOUNDATION"),
    "onion_bombay":     (790646,  "USDA_FOUNDATION"),
    "salt_iodized":     (746775,  "USDA_FOUNDATION"),
    "vinegar_cane":     (173469,  "USDA_SR_LEGACY"),
    "vinegar_coconut":  (173469,  "USDA_SR_LEGACY"),
    "calamansi_juice":  (167747,  "USDA_SR_LEGACY"),
    "pansit-pansitan":  (170068,  "USDA_SR_LEGACY"),
}


def fetch_food(fdc_id):
    url = f"{BASE_URL}/food/{fdc_id}?api_key={API_KEY}"
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode())


def extract_nutrients(food_data):
    result = {v: 0.0 for v in NUTRIENT_MAP.values()}
    for fn in food_data.get("foodNutrients", []):
        nid = fn.get("nutrient", {}).get("id") or fn.get("nutrientId", 0)
        if nid in NUTRIENT_MAP:
            val = fn.get("amount", 0) or fn.get("value", 0) or 0
            result[NUTRIENT_MAP[nid]] = round(float(val), 2)
    return result


def main():
    json_path = os.path.normpath(os.path.join(
        os.path.dirname(__file__), "..",
        "app", "src", "main", "assets", "raw_ingredients.json"
    ))

    with open(json_path, "r", encoding="utf-8") as f:
        ingredients = json.load(f)

    # Create backup
    backup_path = json_path + ".pre_fdc_fix.backup"
    with open(backup_path, "w", encoding="utf-8") as f:
        json.dump(ingredients, f, indent=2, ensure_ascii=False)
    print(f"Backup saved to: {backup_path}\n")

    # Fetch nutrients for each new FDC ID (deduplicate API calls)
    fdc_cache = {}
    unique_ids = set(fdc_id for fdc_id, _ in UPDATES.values())
    for fdc_id in sorted(unique_ids):
        print(f"Fetching FDC {fdc_id}...", end=" ")
        time.sleep(0.5)
        try:
            food_data = fetch_food(fdc_id)
            desc = food_data.get("description", "Unknown")
            nutrients = extract_nutrients(food_data)
            fdc_cache[fdc_id] = (nutrients, desc)
            print(f"OK - '{desc}' (cal={nutrients['calories']})")
        except Exception as e:
            print(f"ERROR: {e}")
            return

    print(f"\n{'='*100}")
    print("APPLYING UPDATES")
    print(f"{'='*100}")

    for ing in ingredients:
        key = ing["ingredient_key"]
        if key not in UPDATES:
            continue

        new_fdc_id, data_source = UPDATES[key]
        old_fdc = ing.get("fdc_id")
        old_nutrients = copy.deepcopy(ing.get("nutrients_per_100g", {}))
        new_nutrients, desc = fdc_cache[new_fdc_id]

        # Apply updates
        ing["fdc_id"] = new_fdc_id
        ing["data_source"] = data_source
        ing["nutrients_per_100g"] = {
            "calories": new_nutrients["calories"],
            "protein": new_nutrients["protein"],
            "carbs": new_nutrients["carbs"],
            "fat": new_nutrients["fat"],
            "fiber": new_nutrients["fiber"],
            "sugar": new_nutrients["sugar"],
            "sodium": new_nutrients["sodium"],
            "potassium": new_nutrients["potassium"],
            "vitamin_a": new_nutrients["vitamin_a"],
            "vitamin_c": new_nutrients["vitamin_c"],
            "calcium": new_nutrients["calcium"],
            "iron": new_nutrients["iron"],
        }

        # Print change summary
        print(f"\n  {ing['display_name']} ({key}):")
        print(f"    FDC: {old_fdc} -> {new_fdc_id} ('{desc}')")
        print(f"    Source: {data_source}")
        
        # Show nutrient changes
        for nkey in ["calories", "protein", "carbs", "fat", "sodium", "vitamin_c", "calcium", "iron"]:
            old_val = old_nutrients.get(nkey, 0)
            new_val = new_nutrients.get(nkey, 0)
            if old_val != new_val:
                print(f"    {nkey:>12}: {old_val:>8} -> {new_val:>8}")

    # Write updated file
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(ingredients, f, indent=2, ensure_ascii=False)

    print(f"\n{'='*100}")
    print(f"Updated {len(UPDATES)} ingredients in {json_path}")
    print(f"{'='*100}")


if __name__ == "__main__":
    main()
