"""
CalorieKo — Gram Conversion Script  [DEPRECATED]
====================================================
⚠️  DEPRECATED: This script is superseded by scripts/generate_dish_jsons.py,
    which is the canonical pipeline for generating both recipe_ingredients.json
    and dish_recipes.json. The ingredient-specific lookup tables from this file
    have been merged into generate_dish_jsons.py.

    This file is kept for reference only. Do not use it to regenerate data.

Original description:
    Phase 1, Step 3: Converts all portion quantities from dish_ingredients.csv
    to raw_weight_grams using USDA portion data and standard conversions.

Usage:
    python convert_portions.py  (deprecated — use scripts/generate_dish_jsons.py)

Input:
    ../app/src/main/assets/dish_ingredients.csv
    ../app/src/main/assets/raw_ingredients.json

Output:
    ../app/src/main/assets/recipe_ingredients.json
"""

import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

import csv
import json
import os
import re
from fractions import Fraction

# ──────────────────────────────────────────────
# STANDARD VOLUME-TO-GRAM DEFAULTS (fallbacks)
# ──────────────────────────────────────────────
# Used when a specific ingredient doesn't have USDA portion data for that unit.
# Based on water density (1 cup = 236.6ml) with category-based density adjustments.

VOLUME_DEFAULTS_GRAMS = {
    # Standard cooking measures
    "cup": 236.6,      # 1 US cup in ml ~ grams for water-like liquids
    "tbsp": 14.8,      # 1 US tablespoon
    "tsp": 4.9,        # 1 US teaspoon
}

# Category-specific cup weights (grams per cup) for better accuracy
CUP_WEIGHT_BY_INGREDIENT = {
    # Proteins (cubed/sliced meat per cup)
    "tuna_fish": 150.0,
    "tinapa_fish": 140.0,
    "pork_liempo": 225.0,
    "chicken_breast": 140.0,

    # Produce - leafy (loosely packed)
    "kangkong_leaves": 30.0,      # Very light leafy green
    "malunggay_leaves": 25.0,     # Very light leaves
    "kamote_tops_green": 30.0,    # Similar to kangkong
    "pechay": 70.0,               # Bok choy, sliced
    "pansit-pansitan": 45.0,      # Light herb

    # Produce - chunky/cubed vegetables
    "tomato": 180.0,
    "tomato_red": 180.0,
    "onion_red": 160.0,
    "onion_white": 160.0,
    "onion_bombay": 160.0,
    "ginger": 120.0,
    "cucumber": 133.0,
    "ampalaya": 120.0,            # Bitter gourd, sliced
    "gabi": 140.0,                # Taro, cubed
    "sitaw": 100.0,               # Yard-long beans, cut
    "upo": 120.0,                 # Bottle gourd, sliced
    "sayote": 135.0,              # Chayote, sliced
    "squash": 140.0,              # Kalabasa, cubed
    "okra": 100.0,                # Okra, sliced
    "eggplant": 82.0,             # Eggplant, cubed
    "cauliflower": 100.0,         # Cauliflower, florets
    "carrot": 128.0,              # Carrots, chopped
    "baguio_beans": 100.0,        # Green beans, sliced
    "bell_pepper_red": 149.0,     # Red pepper, chopped
    "papaya_green": 140.0,        # Green papaya, cubed
    "mango_unripe": 165.0,        # Mango, sliced
    "potato": 150.0,              # Potato, cubed
    "green_peas": 145.0,          # Frozen peas
    "radish": 116.0,              # Radish, sliced
    "lato_seaweed": 80.0,         # Sea grapes, loose
    "guso_seaweed": 80.0,         # Seaweed, loose

    # Liquids
    "cooking_oil": 218.0,         # Oil is lighter than water
    "vinegar_cane": 239.0,
    "vinegar_white": 239.0,
    "calamansi_juice": 244.0,
    "water": 236.6,
    "soy_sauce": 255.0,           # Slightly denser

    # Seasonings
    "alamang_bagoong": 240.0,     # Dense paste
    "garlic": 136.0,              # Chopped garlic per cup

    # Pantry
    "cornstarch": 128.0,
    "all_purpose_flour": 125.0,
    "sugar_brown": 220.0,
    "sugar_white": 200.0,
    "tomato_sauce": 245.0,
    "raisins": 145.0,
}

# TBSP weights for specific ingredients
TBSP_WEIGHT_BY_INGREDIENT = {
    "garlic": 8.5,        # ~3 cloves chopped
    "onion_bombay": 10.0,
    "onion_red": 10.0,
    "cooking_oil": 13.6,
    "patis": 18.0,        # Fish sauce
    "soy_sauce": 16.0,
    "vinegar_cane": 15.0,
    "vinegar_white": 15.0,
    "calamansi_juice": 15.0,
    "sugar_brown": 13.8,
    "sugar_white": 12.5,
    "cornstarch": 8.0,
    "all_purpose_flour": 7.8,
    "salt_iodized": 18.0,
    "water": 14.8,
}

# TSP weights for specific ingredients
TSP_WEIGHT_BY_INGREDIENT = {
    "salt_iodized": 6.0,
    "black_pepper": 2.3,
    "sugar_brown": 4.6,
    "sugar_white": 4.2,
    "thyme": 1.4,
    "food_coloring_orange": 2.0,  # Approximate
    "cornstarch": 2.7,
}

# Default weight per piece for counted items
PIECE_WEIGHT = {
    "chicken_egg": 50.0,          # 1 large egg
    "galunggong_fish": 80.0,      # 1 small whole galunggong (~80g)
    "tilapya_fish": 200.0,        # 1 small whole tilapia (~200g)
    "bangus_fish": 150.0,         # 1 small bangus piece
}

# ──────────────────────────────────────────────
# DEFAULT PORTIONS FOR EMPTY-QUANTITY DISHES
# ──────────────────────────────────────────────
# Lines 141-158 of CSV have no portion_quantity.
# These are "per serving" defaults for single-ingredient simple dishes.

EMPTY_PORTION_DEFAULTS = {
    # milkfish_fried — 1 serving of fried milkfish
    ("milkfish_fried", "milkfish"): 150.0,
    ("milkfish_fried", "cooking_oil"): 30.0,
    ("milkfish_fried", "salt_iodized"): 3.0,

    # mackerel_fried — 1 serving of fried mackerel
    ("mackerel_fried", "mackerel_fish"): 150.0,
    ("mackerel_fried", "cooking_oil"): 30.0,
    ("mackerel_fried", "salt_iodized"): 3.0,

    # rice_well_milled — 1 cup of raw rice (makes ~2 cups cooked)
    ("rice_well_milled", "rice_bigas"): 185.0,
    ("rice_well_milled", "water"): 370.0,

    # egg_sunny — 1 sunny side up egg
    ("egg_sunny", "chicken_egg"): 50.0,
    ("egg_sunny", "cooking_oil"): 5.0,

    # egg_fried — 1 fried egg
    ("egg_fried", "chicken_egg"): 50.0,
    ("egg_fried", "cooking_oil"): 5.0,

    # egg_boiled — 1 boiled egg
    ("egg_boiled", "chicken_egg"): 50.0,
    ("egg_boiled", "water"): 0.0,

    # store-bought lechon manok parts — typical serving weight
    ("chicken_wing", "store_bought_lechon_manok_wing"): 90.0,
    ("chicken_thigh", "store_bought_lechon_manok_thigh"): 150.0,
    ("chicken_drumstick", "store_bought_lechon_manok_drumstick"): 130.0,
    ("chicken_breast", "store_bought_lechon_manok_breast"): 200.0,
}


def parse_fraction(text: str) -> float:
    """Parse a string like '1/2', '2 1/2', '1 3/4', '3/4' into a float."""
    text = text.strip()
    if not text:
        return 0.0

    # Handle compound fractions like "2 1/2" or "1 3/4"
    parts = text.split()
    total = 0.0
    for part in parts:
        if '/' in part:
            try:
                total += float(Fraction(part))
            except (ValueError, ZeroDivisionError):
                pass
        else:
            try:
                total += float(part)
            except ValueError:
                pass
    return total


def parse_portion_quantity(portion_str: str, ingredient_key: str) -> float:
    """
    Parse a portion_quantity string from dish_ingredients.csv and return grams.

    Handles formats like:
    - "5 pcs"
    - "2 tbsps"
    - "1/3 cup"
    - "2 1/2 cups"
    - "1/2 cup + 1/8 cup"
    - "1 pack, 44 gram"
    - "8 packs, 10 gram per pack"
    - "6 cans, 155 gram per can"
    - "5 slices, 90 gram per slice"
    - "3/4 kilo or 5 pcs small sized"
    - "1 kilo or 5 pcs small size"
    - "1/4 tbsp"
    - "1 1/2 tbsps"
    """
    if not portion_str or not portion_str.strip():
        return 0.0

    portion = portion_str.strip().lower()

    # ── EXPLICIT GRAM PATTERNS ──

    # "X packs, Y gram per pack" or "X cans, Y gram per can"
    m = re.match(r'(\d+)\s+(?:packs?|cans?|slices?)[,\s]+(\d+)\s*gram\s+per\s+(?:pack|can|slice)', portion)
    if m:
        count = int(m.group(1))
        gram_per = float(m.group(2))
        return count * gram_per

    # "1 pack, 44 gram" or "1 pack, 50 gram"
    m = re.match(r'(\d+)\s+packs?[,\s]+(\d+)\s*gram', portion)
    if m:
        count = int(m.group(1))
        grams = float(m.group(2))
        return count * grams

    # "3/4 kilo or ..." — take the kilo value
    m = re.match(r'([\d\s/]+)\s*kilo', portion)
    if m:
        kilo_val = parse_fraction(m.group(1))
        return kilo_val * 1000.0

    # ── COMPOUND PORTIONS (e.g., "1/2 cup + 1/8 cup") ──
    if '+' in portion:
        parts = portion.split('+')
        total = 0.0
        for part in parts:
            total += parse_portion_quantity(part.strip(), ingredient_key)
        return total

    # ── PIECE-COUNTED ITEMS ──
    m = re.match(r'([\d\s/]+)\s*(?:pcs?|pieces?)\b', portion)
    if m:
        count = parse_fraction(m.group(1))
        piece_wt = PIECE_WEIGHT.get(ingredient_key, 50.0)  # default 50g per piece
        return count * piece_wt

    # ── VOLUME UNITS: CUPS ──
    m = re.match(r'([\d\s/]+)\s*cups?\b', portion)
    if m:
        qty = parse_fraction(m.group(1))
        cup_wt = CUP_WEIGHT_BY_INGREDIENT.get(ingredient_key, VOLUME_DEFAULTS_GRAMS["cup"])
        return qty * cup_wt

    # ── VOLUME UNITS: TABLESPOONS ──
    m = re.match(r'([\d\s/]+)\s*tbsps?\b', portion)
    if m:
        qty = parse_fraction(m.group(1))
        tbsp_wt = TBSP_WEIGHT_BY_INGREDIENT.get(ingredient_key, VOLUME_DEFAULTS_GRAMS["tbsp"])
        return qty * tbsp_wt

    # ── VOLUME UNITS: TEASPOONS ──
    m = re.match(r'([\d\s/]+)\s*tsps?\b', portion)
    if m:
        qty = parse_fraction(m.group(1))
        tsp_wt = TSP_WEIGHT_BY_INGREDIENT.get(ingredient_key, VOLUME_DEFAULTS_GRAMS["tsp"])
        return qty * tsp_wt

    # ── FALLBACK: try to parse as a plain number (grams) ──
    try:
        return float(portion)
    except ValueError:
        print(f"  [WARN] Could not parse portion: '{portion_str}' for {ingredient_key}")
        return 0.0


def load_csv(csv_path: str) -> list:
    """Load dish_ingredients.csv and return list of dicts."""
    rows = []
    with open(csv_path, 'r', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)
    return rows


def convert_all(csv_rows: list) -> list:
    """Convert all CSV rows to recipe_ingredients entries with gram weights."""
    results = []
    warnings = []

    for i, row in enumerate(csv_rows):
        dish = row['ml_label'].strip()
        ingredient = row['ingredient_name'].strip()
        ing_type = row['ingredient_type'].strip()
        ing_category = row['ingredient_category'].strip()
        portion_str = row['portion_quantity'].strip() if row['portion_quantity'] else ''
        prep = row['preparation_method'].strip() if row['preparation_method'] else ''
        step = int(row['step'].strip()) if row['step'].strip() else 1

        # ── Handle mixed_vegetables decomposition ──
        if ingredient == 'mixed_vegetables':
            # Split "5 cups" into 4 equal parts
            total_cups = parse_fraction(re.match(r'([\d\s/]+)', portion_str).group(1)) if portion_str else 5.0
            per_veg_cups = total_cups / 4.0
            for veg_key, veg_cup_wt in [
                ('eggplant', CUP_WEIGHT_BY_INGREDIENT['eggplant']),
                ('okra', CUP_WEIGHT_BY_INGREDIENT['okra']),
                ('squash', CUP_WEIGHT_BY_INGREDIENT['squash']),
                ('sitaw', CUP_WEIGHT_BY_INGREDIENT['sitaw']),
            ]:
                grams = round(per_veg_cups * veg_cup_wt, 1)
                results.append({
                    "dish_label": dish,
                    "ingredient_key": veg_key,
                    "ingredient_type": ing_type,
                    "ingredient_category": "produce",
                    "raw_weight_grams": grams,
                    "portion_original": f"{per_veg_cups} cups (from mixed_vegetables)",
                    "preparation_method": prep,
                    "step": step,
                })
            continue

        # ── Handle empty portions (lines 141-158) ──
        if not portion_str:
            default_key = (dish, ingredient)
            if default_key in EMPTY_PORTION_DEFAULTS:
                grams = EMPTY_PORTION_DEFAULTS[default_key]
                results.append({
                    "dish_label": dish,
                    "ingredient_key": ingredient,
                    "ingredient_type": ing_type,
                    "ingredient_category": ing_category,
                    "raw_weight_grams": grams,
                    "portion_original": f"(default: {grams}g per serving)",
                    "preparation_method": prep,
                    "step": step,
                })
            else:
                warnings.append(f"No default for ({dish}, {ingredient}) — skipping")
            continue

        # ── Check if gram info is in preparation_method ──
        # CSV stores: portion="8 packs", prep="10 gram per pack"
        # Or: portion="1 pack", prep="44 gram"
        combined = portion_str
        if prep and 'gram' in prep.lower() and re.search(r'(?:packs?|cans?|slices?)\b', portion_str.lower()):
            combined = f"{portion_str}, {prep}"

        # ── Handle "2 packs" without gram info (known products) ──
        if re.match(r'^\d+\s+packs?$', combined.strip().lower()):
            pack_defaults = {
                "tomato_sauce": 250.0,   # ~250g per standard pack
            }
            m_count = re.match(r'(\d+)', combined)
            if m_count and ingredient in pack_defaults:
                combined = f"{m_count.group(1)} packs, {int(pack_defaults[ingredient])} gram per pack"

        # ── Standard conversion ──
        grams = parse_portion_quantity(combined, ingredient)
        grams = round(grams, 1)

        results.append({
            "dish_label": dish,
            "ingredient_key": ingredient,
            "ingredient_type": ing_type,
            "ingredient_category": ing_category,
            "raw_weight_grams": grams,
            "portion_original": portion_str,
            "preparation_method": prep,
            "step": step,
        })

    if warnings:
        print("\nWarnings:")
        for w in warnings:
            print(f"  - {w}")

    return results


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = os.path.join(script_dir, "..", "app", "src", "main", "assets")
    base_dir = os.path.normpath(base_dir)

    csv_path = os.path.join(base_dir, "dish_ingredients.csv")
    output_path = os.path.join(base_dir, "recipe_ingredients.json")

    print("=" * 60)
    print("CalorieKo -- Portion-to-Gram Conversion")
    print("=" * 60)
    print(f"Input:  {csv_path}")
    print(f"Output: {output_path}")
    print()

    # Load CSV
    csv_rows = load_csv(csv_path)
    print(f"Loaded {len(csv_rows)} rows from CSV")

    # Convert
    results = convert_all(csv_rows)
    print(f"Produced {len(results)} recipe ingredient entries")

    # Write output
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    # Summary by dish
    dishes = {}
    for r in results:
        d = r['dish_label']
        if d not in dishes:
            dishes[d] = {'count': 0, 'total_grams': 0.0}
        dishes[d]['count'] += 1
        dishes[d]['total_grams'] += r['raw_weight_grams']

    print(f"\n{'Dish':<30s} {'Ingredients':>12s} {'Total Raw (g)':>14s}")
    print("-" * 58)
    for dish, info in sorted(dishes.items()):
        print(f"{dish:<30s} {info['count']:>12d} {info['total_grams']:>14.1f}")

    # Flag any zero-gram entries
    zeros = [r for r in results if r['raw_weight_grams'] == 0.0
             and r['ingredient_key'] not in ('water', 'food_coloring_orange')]
    if zeros:
        print(f"\n[!] {len(zeros)} entries have 0g weight (review needed):")
        for z in zeros:
            print(f"    {z['dish_label']}/{z['ingredient_key']}: '{z['portion_original']}'")

    print(f"\nDone! Output: {output_path}")


if __name__ == "__main__":
    main()
