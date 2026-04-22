"""
Fetches USDA data for new substitute ingredients and adds them to raw_ingredients.json.
Usage: python tools/add_substitutes.py --api-key YOUR_API_KEY
"""
import json
import argparse
import urllib.request
import urllib.parse
import time
import os

API_BASE = "https://api.nal.usda.gov/fdc/v1"

# New ingredients to add. Format:
# (ingredient_key, display_name, category, sub_category, fdc_id)
# FDC IDs sourced from USDA FoodData Central (SR Legacy / Foundation)
NEW_INGREDIENTS = [
    # Oils (currently only cooking_oil/soybean in "oil" sub_category)
    ("olive_oil", "Olive Oil", "pantry_staple", "oil", 171413),
    ("coconut_oil", "Coconut Oil", "pantry_staple", "oil", 171412),
    ("canola_oil", "Canola Oil", "pantry_staple", "oil", 172336),

    # Poultry (currently only chicken_breast in "poultry" sub_category)
    ("chicken_thigh", "Chicken Thigh (raw)", "protein", "poultry", 171474),
    ("chicken_drumstick", "Chicken Drumstick (raw)", "protein", "poultry", 171466),

    # Pork (currently only pork_liempo + pork_tenderloin)
    ("pork_shoulder", "Pork Shoulder (raw)", "protein", "pork", 167820),
    ("ground_pork", "Ground Pork (raw)", "protein", "pork", 167904),

    # Noodles (currently only odong_noodles)
    ("bihon_noodles", "Bihon (Rice Noodles)", "grain_starch", "noodle", 168916),
    ("canton_noodles", "Canton (Egg Noodles)", "grain_starch", "noodle", 168919),

    # Grain (currently only rice_bigas)
    ("brown_rice", "Brown Rice (raw)", "grain_starch", "grain", 169704),

    # Citrus juice (currently only calamansi_juice)
    ("lemon_juice", "Lemon Juice (fresh)", "pantry_staple", "citrus_juice", 167747),
    ("lime_juice", "Lime Juice (fresh)", "pantry_staple", "citrus_juice", 167748),

    # Fish smoked (currently only tinapa_fish)
    ("tuyo_fish", "Tuyo (Dried Salted Fish)", "protein", "fish_smoked", 175154),
]

# Nutrient IDs we need from USDA
NUTRIENT_MAP = {
    1008: "calories",     # Energy (kcal)
    1003: "protein",      # Protein (g)
    1005: "carbs",        # Carbohydrate (g)
    1004: "fat",          # Total fat (g)
    1079: "fiber",        # Dietary fiber (g)
    2000: "sugar",        # Total sugars (g)
    1093: "sodium",       # Sodium (mg)
    1092: "potassium",    # Potassium (mg)
    1106: "vitamin_a",    # Vitamin A, RAE (µg)
    1162: "vitamin_c",    # Vitamin C (mg)
    1087: "calcium",      # Calcium (mg)
    1089: "iron",         # Iron (mg)
}


def fetch_food(fdc_id: int, api_key: str) -> dict:
    """Fetch a single food item from USDA FDC API."""
    url = f"{API_BASE}/food/{fdc_id}?api_key={api_key}"
    req = urllib.request.Request(url)
    req.add_header("Accept", "application/json")
    
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode())
    except Exception as e:
        print(f"  ERROR fetching FDC {fdc_id}: {e}")
        return None


def extract_nutrients(food_data: dict) -> dict:
    """Extract our 12 nutrients from USDA response."""
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
    """Extract portion/measure data from USDA response."""
    portions = []
    for pm in food_data.get("foodPortions", []):
        desc = pm.get("portionDescription") or pm.get("measureUnit", {}).get("name", "")
        grams = pm.get("gramWeight", 0.0)
        if desc and grams and grams > 0:
            portions.append({"description": desc, "grams": round(grams, 1)})
    return portions[:3]  # Keep top 3 portions


def main():
    parser = argparse.ArgumentParser(description="Add substitute ingredients to raw_ingredients.json")
    parser.add_argument("--api-key", required=True, help="USDA FDC API key")
    parser.add_argument("--dry-run", action="store_true", help="Print results without modifying file")
    args = parser.parse_args()

    # Load existing data
    json_path = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "raw_ingredients.json")
    json_path = os.path.normpath(json_path)
    
    with open(json_path, "r", encoding="utf-8") as f:
        existing = json.load(f)
    
    existing_keys = {item["ingredient_key"] for item in existing}
    print(f"Existing ingredients: {len(existing_keys)}")

    added = 0
    for key, display, category, sub_cat, fdc_id in NEW_INGREDIENTS:
        if key in existing_keys:
            print(f"  SKIP {key} (already exists)")
            continue

        print(f"  Fetching {key} (FDC {fdc_id})...")
        food_data = fetch_food(fdc_id, args.api_key)
        if not food_data:
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
            "nutrients_per_100g": nutrients,
            "portions": portions
        }

        if args.dry_run:
            print(f"    Would add: {key} | {nutrients['calories']} kcal | P:{nutrients['protein']}g F:{nutrients['fat']}g C:{nutrients['carbs']}g")
        else:
            existing.append(entry)
            added += 1
            print(f"    Added: {key} | {nutrients['calories']} kcal | P:{nutrients['protein']}g F:{nutrients['fat']}g C:{nutrients['carbs']}g")

        time.sleep(0.5)  # Rate limit

    if not args.dry_run and added > 0:
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(existing, f, indent=2, ensure_ascii=False)
        print(f"\nDone! Added {added} new ingredients to {json_path}")
    elif args.dry_run:
        print(f"\nDry run complete. Would add {len(NEW_INGREDIENTS) - sum(1 for k,_,_,_,_ in NEW_INGREDIENTS if k in existing_keys)} ingredients.")
    else:
        print("\nNo new ingredients to add.")


if __name__ == "__main__":
    main()
