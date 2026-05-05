"""
Generate recipe_ingredients.json and dish_recipes.json from the CSV files.
- Parses dish_ingredients.csv for ingredient lists
- Parses dish_labels_and_values.csv for dish metadata
- Converts portion quantities to gram weights
- Computes per-serving nutrients from raw_ingredients.json
"""

import json, csv, re, os, math
from fractions import Fraction

# ─── Paths ───
BASE = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets"))
INGREDIENTS_CSV = os.path.join(BASE, "dish_ingredients.csv")
VALUES_CSV = os.path.join(BASE, "dish_labels_and_values.csv")
RAW_JSON = os.path.join(BASE, "raw_ingredients.json")
RECIPE_OUT = os.path.join(BASE, "recipe_ingredients.json")
DISH_OUT = os.path.join(BASE, "dish_recipes.json")

# ─── Volume-to-grams conversion (standard US measures) ───
# Base unit: 1 cup = 240 mL for liquids
VOLUME_ML = {
    "cup": 240, "cups": 240,
    "tbsp": 15, "tbsps": 15, "tablespoon": 15, "tablespoons": 15,
    "tsp": 15/3, "tsps": 15/3, "teaspoon": 15/3, "teaspoons": 15/3,
}

# Density approximations (g per mL) for common ingredient categories
DENSITY = {
    # Liquids
    "water": 1.0, "cooking_oil": 0.92, "olive_oil": 0.92, "coconut_oil": 0.92,
    "canola_oil": 0.92, "vinegar_cane": 1.0, "vinegar_white": 1.0,
    "soy_sauce": 1.08, "patis": 1.1, "calamansi_juice": 1.0,
    "lemon_juice": 1.0, "lime_juice": 1.0, "tomato_sauce": 1.1,
    # Powders/granules
    "salt_iodized": 1.2, "sugar_white": 0.85, "sugar_brown": 0.83,
    "black_pepper": 0.45, "cornstarch": 0.54, "all_purpose_flour": 0.52,
    "food_coloring_orange": 0.5, "sinigang_mix": 0.7,
    "alamang_bagoong": 1.1, "thyme": 0.35, "oregano_leaves": 0.35,
    "laurel_leaves": 0.2, "black_beans": 1.0,
    # Chopped/diced produce (g per mL, loose packed)
    "onion_red": 0.67, "onion_white": 0.67, "onion_bombay": 0.67,
    "garlic": 0.73, "tomato": 0.75, "tomato_red": 0.75,
    "ginger": 0.67, "cucumber": 0.54, "ampalaya": 0.52,
    "malunggay_leaves": 0.21, "gabi": 0.63, "sitaw": 0.42,
    "kangkong_leaves": 0.21, "upo": 0.52, "kamote_tops_green": 0.21,
    "sayote": 0.58, "pechay": 0.29, "squash": 0.58,
    "okra": 0.42, "eggplant": 0.42, "cauliflower": 0.42,
    "carrot": 0.54, "baguio_beans": 0.42, "bell_pepper_red": 0.58,
    "papaya_green": 0.58, "mango_unripe": 0.67, "radish": 0.5,
    "potato": 0.63, "green_peas": 0.63, "raisins": 0.67,
    "lato_seaweed": 0.33, "guso_seaweed": 0.33,
    "pansit-pansitan": 0.25, "spring_onion": 0.42,
    "alugbati": 0.21, "tanglad": 0.3,
    # Proteins (cubed/sliced, g per mL)
    "chicken_egg": 1.0, "tuna_fish": 0.75, "tinapa_fish": 0.58,
    "pork_liempo": 0.75, "bangus_fish": 0.75, "galunggong_fish": 0.75,
    "tilapya_fish": 0.75, "chicken_breast": 0.67, "pork_tenderloin": 0.67,
    "milkfish": 0.75, "mackerel_fish": 0.75,
    "chicken_thigh": 0.67, "chicken_drumstick": 0.67,
    "pork_shoulder": 0.75, "ground_pork": 0.73, "pork_belly": 0.75,
    # Dry noodles/grains
    "rice_bigas": 0.75, "brown_rice": 0.75,
    "odong_noodles": 0.5, "bihon_noodles": 0.5, "canton_noodles": 0.5,
    "sardines_tomato_sauce_canned": 1.0,
}

# Piece weights (grams per piece)
PIECE_WEIGHT = {
    "chicken_egg": 50, "galunggong_fish": 80, "tilapya_fish": 200,
    "bangus_fish": 150, "milkfish": 150, "mackerel_fish": 150,
    "oregano_leaves": 0.5, "laurel_leaves": 0.6, "tanglad": 25,
}

# ─── Cooking yield factors by method ───
YIELD_FACTORS = {
    "deep_fried": 0.9, "pan_fried": 0.8, "stir_fried": 0.78,
    "sauteed": 0.8, "simmered": 0.85, "boiled": 0.9,
    "grilled": 0.65, "stewed": 0.75, "raw_cured": 0.95,
    "grilled_and_cured": 0.85, "store_bought_roasted": 1.0,
    "braised": 0.75, "steamed": 0.9,
}

# ─── Cooking methods per dish ───
DISH_COOKING_METHODS = {
    # Existing dishes
    "kwekwek": "deep_fried",
    "kinilaw_tuna": "raw_cured",
    "tinapa_ginisa": "sauteed",
    "egg_ampalaya": "sauteed",
    "sinigang_pork": "simmered",
    "menudo": "stewed",
    "udong": "simmered",
    "sinabawang_bangus": "simmered",
    "galunggong_grilled": "grilled",
    "tilapia_fried": "pan_fried",
    "pinakbet": "sauteed",
    "chopseuy": "stir_fried",
    "chicken_tinola": "simmered",
    "sinuglaw_pork": "grilled_and_cured",
    "milkfish_fried": "pan_fried",
    "mackerel_fried": "pan_fried",
    "rice_well_milled": "boiled",
    "egg_sunny": "pan_fried",
    "egg_boiled": "boiled",
    "chicken_wing": "store_bought_roasted",
    "chicken_thigh": "store_bought_roasted",
    "chicken_drumstick": "store_bought_roasted",
    "chicken_breast": "store_bought_roasted",
    # New dishes
    "galunggong_fried": "pan_fried",
    "egg_omelette": "pan_fried",
    "egg_scrambled": "pan_fried",
    "linatan": "simmered",
    "humba_pork": "braised",
    "lawuy": "simmered",
}

# ─── Servings per dish ───
DISH_SERVINGS = {
    # Existing dishes (from original dish_recipes.json)
    "kwekwek": 5,
    "kinilaw_tuna": 6,
    "tinapa_ginisa": 6,
    "egg_ampalaya": 6,
    "sinigang_pork": 10,
    "menudo": 10,
    "udong": 6,
    "sinabawang_bangus": 6,
    "galunggong_grilled": 5,
    "tilapia_fried": 5,
    "pinakbet": 8,
    "chopseuy": 8,
    "chicken_tinola": 8,
    "sinuglaw_pork": 6,
    "milkfish_fried": 1,
    "mackerel_fried": 1,
    "rice_well_milled": 1,
    "egg_sunny": 1,
    "egg_boiled": 1,
    "chicken_wing": 1,
    "chicken_thigh": 1,
    "chicken_drumstick": 1,
    "chicken_breast": 1,
    # New dishes
    "galunggong_fried": 1,
    "egg_omelette": 1,
    "egg_scrambled": 1,
    "linatan": 6,
    "humba_pork": 6,
    "lawuy": 6,
}


def parse_quantity(qty_str):
    """Parse a portion quantity string like '2 1/2 cups' into (number, unit)."""
    if not qty_str or not qty_str.strip():
        return None, None
    
    s = qty_str.strip().lower()
    
    # Handle special cases
    if "kilo" in s or "gram" in s:
        # "3/4 kilo" or "10 gram per pack" etc
        m = re.match(r'([\d\s/]+)\s*kilo', s)
        if m:
            return eval_fraction(m.group(1).strip()) * 1000, "g"
        m = re.match(r'(\d+)\s*(gram|g)', s)
        if m:
            return float(m.group(1)), "g"
        return None, None
    
    # Handle "X packs, Y gram per pack"
    m = re.match(r'(\d+)\s*packs?\s*,?\s*(\d+)\s*gram', s)
    if m:
        return float(m.group(1)) * float(m.group(2)), "g"
    
    # Handle "X cans, Y gram per can"
    m = re.match(r'(\d+)\s*cans?\s*,?\s*(\d+)\s*gram', s)
    if m:
        return float(m.group(1)) * float(m.group(2)), "g"
    
    # Handle "X pcs" or "X pc"
    m = re.match(r'([\d\s/]+)\s*(?:pcs?|pieces?|stalk)', s)
    if m:
        return eval_fraction(m.group(1).strip()), "pcs"
    
    # Handle "X pack" (single pack with gram info)
    m = re.match(r'(\d+)\s*pack\s*,?\s*(\d+)\s*gram', s)
    if m:
        return float(m.group(1)) * float(m.group(2)), "g"
    m = re.match(r'(\d+)\s*pack', s)
    if m:
        return float(m.group(1)), "pack"
    
    # Handle combined like "1/2 cup + 1/8 cup"
    if '+' in s:
        total = 0
        unit = None
        for part in s.split('+'):
            n, u = parse_quantity(part.strip())
            if n is not None:
                total += n
                if u:
                    unit = u
        return total, unit
    
    # Standard: "2 1/2 cups", "1/3 cup", "3 tbsps"
    m = re.match(r'([\d\s/]+)\s*(cups?|tbsps?|tsps?|tablespoons?|teaspoons?)', s)
    if m:
        return eval_fraction(m.group(1).strip()), m.group(2)
    
    return None, None


def eval_fraction(s):
    """Evaluate a string like '2 1/2' or '1/3' to a float."""
    s = s.strip()
    parts = s.split()
    total = 0
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


def portion_to_grams(ingredient_key, qty_str):
    """Convert a portion quantity to grams."""
    num, unit = parse_quantity(qty_str)
    
    if num is None:
        return 0.0  # No portion info (simple dishes)
    
    if unit == "g":
        return num
    
    if unit == "pcs":
        pw = PIECE_WEIGHT.get(ingredient_key, 50)  # default 50g per piece
        return num * pw
    
    if unit == "pack":
        # Guess: sinigang mix = 44g, tomato sauce pack = 250g
        if ingredient_key == "sinigang_mix":
            return num * 44
        elif ingredient_key == "tomato_sauce":
            return num * 250
        elif ingredient_key == "raisins":
            return num * 50
        elif ingredient_key == "odong_noodles":
            return num * 10
        return num * 100  # default pack size
    
    # Volume units
    if unit in VOLUME_ML:
        ml = num * VOLUME_ML[unit]
        density = DENSITY.get(ingredient_key, 0.6)  # default density
        return ml * density
    
    return 0.0


def main():
    # Load raw ingredients for nutrient lookup
    with open(RAW_JSON, "r", encoding="utf-8") as f:
        raw_ingredients = json.load(f)
    nutrients_by_key = {i["ingredient_key"]: i["nutrients_per_100g"] for i in raw_ingredients}
    
    # Load dish metadata from values CSV
    dish_meta = {}
    with open(VALUES_CSV, "r", encoding="utf-8", errors="replace") as f:
        reader = csv.DictReader(f)
        for row in reader:
            label = row.get("ml_label", "").strip()
            if label:
                dish_meta[label] = {
                    "name_en": row.get("name_en", ""),
                    "name_ph": row.get("name_ph", ""),
                    "category": row.get("category", ""),
                    "data_source": row.get("data_source", "USDA_FDC"),
                }
    
    # Load dish ingredients CSV
    csv_rows = []
    with open(INGREDIENTS_CSV, "r", encoding="utf-8", errors="replace") as f:
        reader = csv.DictReader(f)
        for row in reader:
            label = row.get("ml_label", "").strip()
            if label:
                csv_rows.append(row)
    
    # Group by dish label
    dishes = {}
    for row in csv_rows:
        label = row["ml_label"].strip()
        if label not in dishes:
            dishes[label] = []
        dishes[label].append(row)
    
    # ─── Generate recipe_ingredients.json ───
    recipe_ingredients = []
    for label, rows in dishes.items():
        for row in rows:
            ing_key = row["ingredient_name"].strip()
            portion = row.get("portion_quantity", "").strip()
            prep = row.get("preparation_method", "").strip()
            step = int(row.get("step", 1))
            ing_type = row.get("ingredient_type", "core").strip()
            ing_cat = row.get("ingredient_category", "").strip()
            
            grams = portion_to_grams(ing_key, portion)
            
            recipe_ingredients.append({
                "dish_label": label,
                "ingredient_key": ing_key,
                "ingredient_type": ing_type,
                "ingredient_category": ing_cat,
                "raw_weight_grams": round(grams, 1),
                "portion_original": portion,
                "preparation_method": prep,
                "step": step,
            })
    
    # ─── Generate dish_recipes.json ───
    # Load existing dish_recipes.json for preserving serving/yield data of old dishes
    existing_recipes = {}
    try:
        with open(DISH_OUT, "r", encoding="utf-8") as f:
            for d in json.load(f):
                existing_recipes[d["dish_label"]] = d
    except:
        pass
    
    dish_recipes = []
    for label, rows in dishes.items():
        meta = dish_meta.get(label, {})
        
        # Use existing recipe data if available (for servings, yield, etc.)
        existing = existing_recipes.get(label, {})
        
        # Determine servings — prioritize our map, then existing data
        servings = DISH_SERVINGS.get(label, existing.get("servings", 1))
        
        # Determine cooking method — prioritize our map, then existing data
        cooking_method = DISH_COOKING_METHODS.get(label, existing.get("cooking_method", "sauteed"))
        
        # Determine yield factor from cooking method
        yield_factor = YIELD_FACTORS.get(cooking_method, 0.85)
        
        # Calculate total raw weight and nutrients
        total_weight = 0.0
        total_nutrients = {k: 0.0 for k in ["calories", "protein", "carbs", "fat", "fiber", "sugar",
                                              "sodium", "potassium", "vitamin_a", "vitamin_c", "calcium", "iron"]}
        
        ingredient_count = 0
        seen_ingredients = set()
        
        for row in rows:
            ing_key = row["ingredient_name"].strip()
            portion = row.get("portion_quantity", "").strip()
            grams = portion_to_grams(ing_key, portion)
            
            if ing_key not in seen_ingredients:
                ingredient_count += 1
                seen_ingredients.add(ing_key)
            
            if grams > 0 and ing_key in nutrients_by_key:
                total_weight += grams
                n = nutrients_by_key[ing_key]
                for nkey in total_nutrients:
                    total_nutrients[nkey] += n.get(nkey, 0) * grams / 100.0
            elif grams > 0:
                total_weight += grams  # Still add weight even if no nutrient data
        
        # Use fallback for simple dishes with no portion info
        if total_weight == 0:
            # Try existing dish_recipes.json first
            if existing and existing.get("total_raw_weight_g", 0) > 0:
                total_weight = existing["total_raw_weight_g"]
                if existing.get("total_nutrients_raw"):
                    total_nutrients = dict(existing["total_nutrients_raw"])
            else:
                # Fall back to values CSV data (for new simple dishes)
                csv_meta = dish_meta.get(label, {})
                csv_vals = {}
                # Re-read the CSV to get nutritional values
                with open(VALUES_CSV, "r", encoding="utf-8", errors="replace") as vf:
                    vreader = csv.DictReader(vf)
                    for vrow in vreader:
                        if vrow.get("ml_label", "").strip() == label:
                            csv_vals = vrow
                            break
                if csv_vals:
                    cal = float(csv_vals.get("calories_kcal", 0) or 0)
                    if cal > 0:
                        total_nutrients = {
                            "calories": cal,
                            "protein": float(csv_vals.get("protein_g", 0) or 0),
                            "carbs": float(csv_vals.get("carbs_g", 0) or 0),
                            "fat": float(csv_vals.get("fat_g", 0) or 0),
                            "fiber": float(csv_vals.get("fiber_g", 0) or 0),
                            "sugar": float(csv_vals.get("sugar_g", 0) or 0),
                            "sodium": float(csv_vals.get("sodium_mg", 0) or 0),
                            "potassium": float(csv_vals.get("potassium_mg", 0) or 0),
                            "vitamin_a": float(csv_vals.get("vitamin_a_mcg", 0) or 0),
                            "vitamin_c": float(csv_vals.get("vitamin_c_mg", 0) or 0),
                            "calcium": float(csv_vals.get("calcium_mg", 0) or 0),
                            "iron": float(csv_vals.get("iron_mg", 0) or 0),
                        }
                        # Default weights for simple dishes
                        if label == "egg_boiled":
                            total_weight = 50.0  # edible boiled egg portion
                            yield_factor = 1.0
                        elif "egg" in label:
                            total_weight = 55.0  # 1 egg + oil
                        elif "chicken" in label:
                            total_weight = 150.0  # typical portion
                        elif "galunggong" in label or "mackerel" in label or "milkfish" in label:
                            total_weight = 183.0  # typical fish
                        else:
                            total_weight = 200.0  # default
        
        cooked_weight = round(total_weight * yield_factor, 1)
        per_serving_weight = round(cooked_weight / servings, 1) if servings > 0 else cooked_weight
        
        # Compute per-serving nutrients
        per_serving_nutrients = {}
        for nkey, val in total_nutrients.items():
            per_serving_nutrients[nkey] = round(val / servings, 2) if servings > 0 else round(val, 2)
        
        # Round total nutrients
        total_nutrients_rounded = {k: round(v, 2) for k, v in total_nutrients.items()}
        
        dish_recipes.append({
            "dish_label": label,
            "name_en": meta.get("name_en", label.replace("_", " ").title()),
            "name_ph": meta.get("name_ph", ""),
            "category": meta.get("category", ""),
            "cooking_method": cooking_method,
            "servings": servings,
            "total_raw_weight_g": round(total_weight, 1),
            "dish_yield_factor": yield_factor,
            "cooked_weight_g": cooked_weight,
            "per_serving_weight_g": per_serving_weight,
            "total_nutrients_raw": total_nutrients_rounded,
            "per_serving_nutrients": per_serving_nutrients,
            "ingredient_count": ingredient_count,
        })
    
    # Write outputs
    with open(RECIPE_OUT, "w", encoding="utf-8") as f:
        json.dump(recipe_ingredients, f, indent=2, ensure_ascii=False)
    print(f"recipe_ingredients.json: {len(recipe_ingredients)} entries for {len(dishes)} dishes")
    
    with open(DISH_OUT, "w", encoding="utf-8") as f:
        json.dump(dish_recipes, f, indent=2, ensure_ascii=False)
    print(f"dish_recipes.json: {len(dish_recipes)} dishes")
    
    # Print summary
    print("\n=== DISH SUMMARY ===")
    for d in dish_recipes:
        n = d["per_serving_nutrients"]
        print(f"  {d['dish_label']:25s} srv={d['servings']:>2} cal/srv={n['calories']:>7.1f} pro={n['protein']:>6.1f} carb={n['carbs']:>6.1f} fat={n['fat']:>6.1f}")


if __name__ == "__main__":
    main()
