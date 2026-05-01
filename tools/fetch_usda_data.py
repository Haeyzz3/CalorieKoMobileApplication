import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')
"""
CalorieKo — USDA FoodData Central Fetch Script
================================================
Phase 1, Step 2: Downloads nutrient + portion data for all mapped ingredients.

Usage:
    python fetch_usda_data.py --api-key YOUR_API_KEY_HERE

Output:
    ../app/src/main/assets/raw_ingredients.json
"""

import argparse
import json
import os
import sys
import time
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError
from urllib.parse import quote

# ──────────────────────────────────────────────
# INGREDIENT MAPPING (from approved Checkpoint 1)
# ──────────────────────────────────────────────

INGREDIENT_MAP = [
    # ── PROTEIN ──
    {"key": "chicken_egg", "fdc_id": 171287, "display_name": "Chicken Egg", "category": "protein", "sub_category": "egg"},
    {"key": "tuna_fish", "fdc_id": 175159, "display_name": "Tuna Fish (Yellowfin)", "category": "protein", "sub_category": "fish_saltwater"},
    {"key": "tinapa_fish", "fdc_id": 174183, "display_name": "Tinapa (Smoked Fish)", "category": "protein", "sub_category": "fish_smoked"},
    {"key": "pork_liempo", "fdc_id": 167902, "display_name": "Pork Liempo (Belly)", "category": "protein", "sub_category": "pork"},
    {"key": "bangus_fish", "fdc_id": 173693, "display_name": "Bangus (Milkfish)", "category": "protein", "sub_category": "fish_freshwater"},
    {"key": "galunggong_fish", "fdc_id": 174201, "display_name": "Galunggong (Round Scad)", "category": "protein", "sub_category": "fish_saltwater"},
    {"key": "tilapya_fish", "fdc_id": 175177, "display_name": "Tilapia", "category": "protein", "sub_category": "fish_freshwater"},
    {"key": "chicken_breast", "fdc_id": 171077, "display_name": "Chicken Breast", "category": "protein", "sub_category": "poultry"},
    {"key": "pork_tenderloin", "fdc_id": 168249, "display_name": "Pork Tenderloin", "category": "protein", "sub_category": "pork"},
    {"key": "milkfish", "fdc_id": 173693, "display_name": "Milkfish", "category": "protein", "sub_category": "fish_freshwater"},
    {"key": "mackerel_fish", "fdc_id": 174201, "display_name": "Mackerel Fish", "category": "protein", "sub_category": "fish_saltwater"},

    # ── PRODUCE ──
    {"key": "onion_red", "fdc_id": 170000, "display_name": "Red Onion", "category": "produce", "sub_category": "allium"},
    {"key": "onion_white", "fdc_id": 170000, "display_name": "White Onion", "category": "produce", "sub_category": "allium"},
    {"key": "onion_bombay", "fdc_id": 170000, "display_name": "Bombay Onion", "category": "produce", "sub_category": "allium"},
    {"key": "garlic", "fdc_id": 169230, "display_name": "Garlic", "category": "produce", "sub_category": "allium"},
    {"key": "tomato", "fdc_id": 170457, "display_name": "Tomato", "category": "produce", "sub_category": "nightshade"},
    {"key": "tomato_red", "fdc_id": 170457, "display_name": "Tomato (Red)", "category": "produce", "sub_category": "nightshade"},
    {"key": "ginger", "fdc_id": 169231, "display_name": "Ginger", "category": "produce", "sub_category": "root"},
    {"key": "cucumber", "fdc_id": 168409, "display_name": "Cucumber", "category": "produce", "sub_category": "gourd"},
    {"key": "lato_seaweed", "fdc_id": 170495, "display_name": "Lato (Sea Grapes)", "category": "produce", "sub_category": "seaweed"},
    {"key": "guso_seaweed", "fdc_id": 170495, "display_name": "Guso Seaweed", "category": "produce", "sub_category": "seaweed"},
    {"key": "ampalaya", "fdc_id": 169226, "display_name": "Ampalaya (Bitter Gourd)", "category": "produce", "sub_category": "gourd"},
    {"key": "malunggay_leaves", "fdc_id": 168456, "display_name": "Malunggay Leaves", "category": "produce", "sub_category": "leafy_green"},
    {"key": "gabi", "fdc_id": 170434, "display_name": "Gabi (Taro)", "category": "produce", "sub_category": "root"},
    {"key": "sitaw", "fdc_id": 170378, "display_name": "Sitaw (Yard-long Bean)", "category": "produce", "sub_category": "legume_veg"},
    {"key": "kangkong_leaves", "fdc_id": 169297, "display_name": "Kangkong (Water Spinach)", "category": "produce", "sub_category": "leafy_green"},
    {"key": "upo", "fdc_id": 169232, "display_name": "Upo (Bottle Gourd)", "category": "produce", "sub_category": "gourd"},
    {"key": "kamote_tops_green", "fdc_id": 170071, "display_name": "Kamote Tops", "category": "produce", "sub_category": "leafy_green"},
    {"key": "sayote", "fdc_id": 169343, "display_name": "Sayote (Chayote)", "category": "produce", "sub_category": "gourd"},
    {"key": "pechay", "fdc_id": 169406, "display_name": "Pechay (Bok Choy)", "category": "produce", "sub_category": "leafy_green"},
    {"key": "squash", "fdc_id": 170487, "display_name": "Squash (Kalabasa)", "category": "produce", "sub_category": "gourd"},
    {"key": "okra", "fdc_id": 169260, "display_name": "Okra", "category": "produce", "sub_category": "legume_veg"},
    {"key": "eggplant", "fdc_id": 169228, "display_name": "Eggplant (Talong)", "category": "produce", "sub_category": "nightshade"},
    {"key": "cauliflower", "fdc_id": 169986, "display_name": "Cauliflower", "category": "produce", "sub_category": "cruciferous"},
    {"key": "carrot", "fdc_id": 170393, "display_name": "Carrot", "category": "produce", "sub_category": "root"},
    {"key": "baguio_beans", "fdc_id": 170378, "display_name": "Baguio Beans", "category": "produce", "sub_category": "legume_veg"},
    {"key": "bell_pepper_red", "fdc_id": 170108, "display_name": "Red Bell Pepper", "category": "produce", "sub_category": "nightshade"},
    {"key": "papaya_green", "fdc_id": 169926, "display_name": "Green Papaya", "category": "produce", "sub_category": "fruit"},
    {"key": "mango_unripe", "fdc_id": 169910, "display_name": "Unripe Mango", "category": "produce", "sub_category": "fruit"},
    {"key": "pansit-pansitan", "fdc_id": None, "display_name": "Pansit-pansitan", "category": "produce", "sub_category": "leafy_green"},
    {"key": "radish", "fdc_id": 169276, "display_name": "Radish", "category": "produce", "sub_category": "root"},
    {"key": "potato", "fdc_id": 170026, "display_name": "Potato", "category": "produce", "sub_category": "root"},
    {"key": "green_peas", "fdc_id": 170419, "display_name": "Green Peas (Frozen)", "category": "produce", "sub_category": "legume_veg"},

    # ── SEASONING ──
    {"key": "salt_iodized", "fdc_id": 170723, "display_name": "Iodized Salt", "category": "seasoning", "sub_category": "salt"},
    {"key": "black_pepper", "fdc_id": 170931, "display_name": "Black Pepper", "category": "seasoning", "sub_category": "spice"},
    {"key": "vinegar_cane", "fdc_id": 173467, "display_name": "Cane Vinegar", "category": "seasoning", "sub_category": "acid"},
    {"key": "vinegar_white", "fdc_id": 173467, "display_name": "White Vinegar", "category": "seasoning", "sub_category": "acid"},
    {"key": "calamansi_juice", "fdc_id": 168155, "display_name": "Calamansi Juice", "category": "seasoning", "sub_category": "citrus_juice"},
    {"key": "sinigang_mix", "fdc_id": None, "display_name": "Sinigang Mix (Knorr)", "category": "seasoning", "sub_category": "seasoning_mix"},
    {"key": "patis", "fdc_id": 173741, "display_name": "Patis (Fish Sauce)", "category": "seasoning", "sub_category": "fermented"},
    {"key": "alamang_bagoong", "fdc_id": 391912, "display_name": "Alamang (Shrimp Paste)", "category": "seasoning", "sub_category": "fermented"},
    {"key": "soy_sauce", "fdc_id": 174714, "display_name": "Soy Sauce", "category": "seasoning", "sub_category": "fermented"},
    {"key": "thyme", "fdc_id": 170930, "display_name": "Thyme (Dried)", "category": "seasoning", "sub_category": "spice"},
    {"key": "sugar_brown", "fdc_id": 168833, "display_name": "Brown Sugar", "category": "seasoning", "sub_category": "sweetener"},
    {"key": "sugar_white", "fdc_id": 169655, "display_name": "White Sugar", "category": "seasoning", "sub_category": "sweetener"},

    # ── PANTRY STAPLE ──
    {"key": "cornstarch", "fdc_id": 170569, "display_name": "Cornstarch", "category": "pantry_staple", "sub_category": "starch"},
    {"key": "all_purpose_flour", "fdc_id": 169761, "display_name": "All-Purpose Flour", "category": "pantry_staple", "sub_category": "flour"},
    {"key": "food_coloring_orange", "fdc_id": None, "display_name": "Food Coloring (Orange)", "category": "pantry_staple", "sub_category": "additive"},
    {"key": "cooking_oil", "fdc_id": 171028, "display_name": "Soybean Oil", "category": "pantry_staple", "sub_category": "oil"},
    {"key": "water", "fdc_id": None, "display_name": "Water", "category": "pantry_staple", "sub_category": "water"},
    {"key": "tomato_sauce", "fdc_id": 170466, "display_name": "Tomato Sauce", "category": "pantry_staple", "sub_category": "canned_good"},
    {"key": "raisins", "fdc_id": 168165, "display_name": "Raisins", "category": "pantry_staple", "sub_category": "dried_fruit"},
    {"key": "odong_noodles", "fdc_id": 169741, "display_name": "Odong Noodles", "category": "pantry_staple", "sub_category": "noodle"},
    {"key": "sardines_tomato_sauce_canned", "fdc_id": 175139, "display_name": "Sardines in Tomato Sauce", "category": "pantry_staple", "sub_category": "canned_good"},
    {"key": "rice_bigas", "fdc_id": 169756, "display_name": "Raw White Rice Grain (Bigas)", "category": "pantry_staple", "sub_category": "grain"},

    # ── STORE-BOUGHT (cooked chicken) ──
    {"key": "store_bought_lechon_manok_wing", "fdc_id": 174608, "display_name": "Lechon Manok Wing", "category": "store_bought", "sub_category": "poultry_cooked"},
    {"key": "store_bought_lechon_manok_thigh", "fdc_id": 174605, "display_name": "Lechon Manok Thigh", "category": "store_bought", "sub_category": "poultry_cooked"},
    {"key": "store_bought_lechon_manok_drumstick", "fdc_id": 174603, "display_name": "Lechon Manok Drumstick", "category": "store_bought", "sub_category": "poultry_cooked"},
    {"key": "store_bought_lechon_manok_breast", "fdc_id": 174599, "display_name": "Lechon Manok Breast", "category": "store_bought", "sub_category": "poultry_cooked"},
]

# ──────────────────────────────────────────────
# MANUAL OVERRIDES (Tier 4 — no fdcId)
# ──────────────────────────────────────────────

MANUAL_OVERRIDES = {
    "water": {
        "data_source": "ZERO_NUTRIENT",
        "nutrients_per_100g": {
            "calories": 0.0, "protein": 0.0, "carbs": 0.0, "fat": 0.0,
            "fiber": 0.0, "sugar": 0.0, "sodium": 0.0, "potassium": 0.0,
            "vitamin_a": 0.0, "vitamin_c": 0.0, "calcium": 0.0, "iron": 0.0,
        },
        "portions": [],
    },
    "food_coloring_orange": {
        "data_source": "ZERO_NUTRIENT",
        "nutrients_per_100g": {
            "calories": 0.0, "protein": 0.0, "carbs": 0.0, "fat": 0.0,
            "fiber": 0.0, "sugar": 0.0, "sodium": 0.0, "potassium": 0.0,
            "vitamin_a": 0.0, "vitamin_c": 0.0, "calcium": 0.0, "iron": 0.0,
        },
        "portions": [],
    },
    "sinigang_mix": {
        "data_source": "KNORR_LABEL",
        "nutrients_per_100g": {
            "calories": 179.0, "protein": 2.0, "carbs": 35.0, "fat": 0.5,
            "fiber": 0.0, "sugar": 10.0, "sodium": 17000.0, "potassium": 0.0,
            "vitamin_a": 0.0, "vitamin_c": 0.0, "calcium": 0.0, "iron": 0.0,
        },
        "portions": [{"description": "1 pack (44g)", "grams": 44.0}],
    },
    "pansit-pansitan": {
        "data_source": "DOST_FNRI_LITERATURE",
        "nutrients_per_100g": {
            "calories": 30.0, "protein": 1.8, "carbs": 5.5, "fat": 0.4,
            "fiber": 1.5, "sugar": 0.0, "sodium": 15.0, "potassium": 350.0,
            "vitamin_a": 250.0, "vitamin_c": 28.0, "calcium": 184.0, "iron": 5.3,
        },
        "portions": [{"description": "1 cup, chopped", "grams": 45.0}],
    },
}

# ──────────────────────────────────────────────
# USDA NUTRIENT ID MAPPING
# ──────────────────────────────────────────────
# Maps our nutrient keys to USDA nutrient numbers
NUTRIENT_ID_MAP = {
    "calories": 1008,     # Energy (kcal)
    "protein": 1003,      # Protein (g)
    "carbs": 1005,        # Carbohydrate, by difference (g)
    "fat": 1004,          # Total lipid (fat) (g)
    "fiber": 1079,        # Fiber, total dietary (g)
    "sugar": 2000,        # Sugars, total including NLEA (g)
    "sodium": 1093,       # Sodium, Na (mg)
    "potassium": 1092,    # Potassium, K (mg)
    "vitamin_a": 1106,    # Vitamin A, RAE (mcg)
    "vitamin_c": 1162,    # Vitamin C (mg)
    "calcium": 1087,      # Calcium, Ca (mg)
    "iron": 1089,         # Iron, Fe (mg)
}

# Fallback for sugar — older entries use nutrient 1063
SUGAR_FALLBACK_ID = 1063  # Sugars, Total (NLEA) — older SR Legacy entries


def fetch_food_data(fdc_id: int, api_key: str, retries: int = 3) -> dict:
    """Fetch full food data from USDA FDC API for a single fdcId."""
    url = f"https://api.nal.usda.gov/fdc/v1/food/{fdc_id}?api_key={api_key}"
    for attempt in range(retries):
        try:
            req = Request(url, headers={"Accept": "application/json"})
            with urlopen(req, timeout=30) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except HTTPError as e:
            if e.code == 429:
                wait = 2 ** (attempt + 1)
                print(f"  ⏳ Rate limited. Waiting {wait}s...")
                time.sleep(wait)
            elif e.code == 404:
                print(f"  ❌ fdcId {fdc_id} not found (404)")
                return None
            else:
                print(f"  ❌ HTTP {e.code} for fdcId {fdc_id}")
                if attempt == retries - 1:
                    return None
                time.sleep(1)
        except URLError as e:
            print(f"  ❌ Network error: {e.reason}")
            if attempt == retries - 1:
                return None
            time.sleep(2)
    return None


def extract_nutrients(food_data: dict) -> dict:
    """Extract our target nutrients from USDA API response."""
    nutrients = {}
    food_nutrients = food_data.get("foodNutrients", [])

    # Build lookup: nutrient_id -> amount
    # USDA API uses nutrient.id (int like 1008) as the stable identifier
    nutrient_lookup = {}
    for fn in food_nutrients:
        nutrient = fn.get("nutrient", {})
        nutrient_id = nutrient.get("id")
        if nutrient_id is not None and "amount" in fn:
            amount = fn.get("amount")
            if amount is not None:
                nutrient_lookup[int(nutrient_id)] = float(amount)

    for key, usda_id in NUTRIENT_ID_MAP.items():
        if usda_id in nutrient_lookup:
            nutrients[key] = round(nutrient_lookup[usda_id], 2)
        elif key == "sugar" and SUGAR_FALLBACK_ID in nutrient_lookup:
            nutrients[key] = round(nutrient_lookup[SUGAR_FALLBACK_ID], 2)
        else:
            nutrients[key] = 0.0

    return nutrients


def extract_portions(food_data: dict) -> list:
    """Extract portion/measure data from USDA API response."""
    portions = []
    for fp in food_data.get("foodPortions", []):
        # Build a human-readable description from available fields
        modifier = fp.get("modifier", "") or ""
        measure_unit = fp.get("measureUnit", {})
        unit_name = measure_unit.get("name", "") if measure_unit else ""
        portion_desc = fp.get("portionDescription", "") or ""
        amount = fp.get("amount", 1.0)
        grams = fp.get("gramWeight", 0.0)

        # Prefer modifier (e.g., "large", "medium", "cup, chopped")
        # then portionDescription, then measureUnit name
        if modifier and modifier != "undetermined":
            desc = f"{amount} {modifier}" if amount and amount != 1.0 else modifier
        elif portion_desc:
            desc = portion_desc
        elif unit_name and unit_name != "undetermined":
            desc = f"{amount} {unit_name}" if amount and amount != 1.0 else unit_name
        else:
            # Fallback: just describe by gram weight
            desc = f"{grams}g serving"

        if grams and grams > 0:
            portions.append({
                "description": desc.strip(),
                "grams": round(float(grams), 1),
            })

    return portions


def process_all_ingredients(api_key: str) -> list:
    """Process all ingredients: fetch from API or use manual overrides."""
    results = []
    fetched_ids = {}  # Cache: fdcId -> (nutrients, portions) to avoid duplicate fetches

    total = len(INGREDIENT_MAP)
    for i, ing in enumerate(INGREDIENT_MAP):
        key = ing["key"]
        fdc_id = ing["fdc_id"]
        print(f"[{i+1}/{total}] {key}", end="")

        # Check manual override
        if key in MANUAL_OVERRIDES:
            override = MANUAL_OVERRIDES[key]
            entry = {
                "ingredient_key": key,
                "display_name": ing["display_name"],
                "category": ing["category"],
                "sub_category": ing["sub_category"],
                "fdc_id": None,
                "data_source": override["data_source"],
                "nutrients_per_100g": override["nutrients_per_100g"],
                "portions": override["portions"],
            }
            results.append(entry)
            print(f" -> Manual override ({override['data_source']})")
            continue

        # Check cache for duplicate fdcIds
        if fdc_id in fetched_ids:
            cached_nutrients, cached_portions = fetched_ids[fdc_id]
            entry = {
                "ingredient_key": key,
                "display_name": ing["display_name"],
                "category": ing["category"],
                "sub_category": ing["sub_category"],
                "fdc_id": fdc_id,
                "data_source": "USDA_SR_LEGACY",
                "nutrients_per_100g": cached_nutrients,
                "portions": cached_portions,
            }
            results.append(entry)
            print(f" -> Cached (same fdcId as previous)")
            continue

        # Fetch from API
        print(f" -> Fetching fdcId {fdc_id}...", end="")
        food_data = fetch_food_data(fdc_id, api_key)
        if food_data is None:
            print(f" FAILED [X]")
            # Insert placeholder
            entry = {
                "ingredient_key": key,
                "display_name": ing["display_name"],
                "category": ing["category"],
                "sub_category": ing["sub_category"],
                "fdc_id": fdc_id,
                "data_source": "FETCH_FAILED",
                "nutrients_per_100g": {k: 0.0 for k in NUTRIENT_ID_MAP},
                "portions": [],
            }
            results.append(entry)
            continue

        # Determine data source from response
        data_type = food_data.get("dataType", "Unknown")
        if data_type == "SR Legacy":
            source = "USDA_SR_LEGACY"
        elif data_type == "Foundation":
            source = "USDA_FOUNDATION"
        elif data_type == "Branded":
            source = "USDA_BRANDED"
        else:
            source = f"USDA_{data_type.upper().replace(' ', '_')}"

        nutrients = extract_nutrients(food_data)
        portions = extract_portions(food_data)

        # Cache
        fetched_ids[fdc_id] = (nutrients, portions)

        entry = {
            "ingredient_key": key,
            "display_name": ing["display_name"],
            "category": ing["category"],
            "sub_category": ing["sub_category"],
            "fdc_id": fdc_id,
            "data_source": source,
            "nutrients_per_100g": nutrients,
            "portions": portions,
        }
        results.append(entry)
        print(f" OK [{len(portions)} portions]")

        # Respect rate limit: ~1 req/sec (1000/hr max)
        time.sleep(1.2)

    return results


def main():
    parser = argparse.ArgumentParser(
        description="Fetch USDA nutrient data for CalorieKo ingredients"
    )
    parser.add_argument(
        "--api-key", required=True,
        help="Your USDA FoodData Central API key"
    )
    parser.add_argument(
        "--output", default=None,
        help="Output file path (default: ../app/src/main/assets/raw_ingredients.json)"
    )
    parser.add_argument(
        "--dry-run", action="store_true",
        help="Print ingredient list without fetching"
    )
    args = parser.parse_args()

    # Determine output path
    if args.output:
        output_path = args.output
    else:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        output_path = os.path.join(
            script_dir, "..", "app", "src", "main", "assets", "raw_ingredients.json"
        )

    output_path = os.path.normpath(output_path)

    if args.dry_run:
        print("=== DRY RUN — Ingredient List ===\n")
        unique_ids = set()
        for ing in INGREDIENT_MAP:
            fdc = ing["fdc_id"]
            is_dup = fdc in unique_ids if fdc else False
            unique_ids.add(fdc)
            manual = "MANUAL" if ing["key"] in MANUAL_OVERRIDES else ""
            dup = "DUP" if is_dup else ""
            tag = f" [{manual}{dup}]" if (manual or dup) else ""
            print(f"  {ing['key']:40s} fdcId={str(fdc):>8s}{tag}")
        api_count = len([i for i in unique_ids if i is not None])
        print(f"\nTotal ingredients: {len(INGREDIENT_MAP)}")
        print(f"Unique fdcIds to fetch: {api_count}")
        print(f"Manual overrides: {len(MANUAL_OVERRIDES)}")
        return

    print("=" * 60)
    print("CalorieKo — USDA FoodData Central Fetch")
    print("=" * 60)
    print(f"Total ingredients: {len(INGREDIENT_MAP)}")
    print(f"Manual overrides: {len(MANUAL_OVERRIDES)}")
    print(f"Output: {output_path}")
    print()

    results = process_all_ingredients(args.api_key)

    # Ensure output directory exists
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    # Summary
    succeeded = sum(1 for r in results if r["data_source"] != "FETCH_FAILED")
    failed = sum(1 for r in results if r["data_source"] == "FETCH_FAILED")
    print()
    print("=" * 60)
    print(f"✅ Done! {succeeded}/{len(results)} ingredients written.")
    if failed:
        print(f"❌ {failed} ingredients failed to fetch.")
        for r in results:
            if r["data_source"] == "FETCH_FAILED":
                print(f"   - {r['ingredient_key']} (fdcId: {r['fdc_id']})")
    print(f"📁 Output: {output_path}")


if __name__ == "__main__":
    main()
