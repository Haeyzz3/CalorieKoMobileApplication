"""Add 7 new ingredients to raw_ingredients.json with USDA API data."""
import json, time, urllib.request, os

API_KEY = "NxgG3iwRfp4HpfmejYVeRAHmJqlBTYeyKyMCHSVc"
BASE_URL = "https://api.nal.usda.gov/fdc/v1"

NUTRIENT_MAP = {
    1008: "calories", 1003: "protein", 1005: "carbs", 1004: "fat",
    1079: "fiber", 2000: "sugar", 1093: "sodium", 1092: "potassium",
    1104: "vitamin_a", 1162: "vitamin_c", 1087: "calcium", 1089: "iron",
}

NEW_INGREDIENTS = [
    {
        "ingredient_key": "oregano_leaves",
        "display_name": "Oregano Leaves (Dried)",
        "category": "seasoning",
        "sub_category": "herb",
        "fdc_id": 171328,  # Spices, oregano, dried
    },
    {
        "ingredient_key": "tanglad",
        "display_name": "Tanglad (Lemongrass)",
        "category": "produce",
        "sub_category": "herb",
        "fdc_id": 168573,  # Lemon grass (citronella), raw
    },
    {
        "ingredient_key": "spring_onion",
        "display_name": "Spring Onion (Scallion)",
        "category": "produce",
        "sub_category": "allium",
        "fdc_id": 170005,  # Onions, spring or scallions, raw
    },
    {
        "ingredient_key": "pork_belly",
        "display_name": "Pork Belly (raw)",
        "category": "protein",
        "sub_category": "pork",
        "fdc_id": 167812,  # Pork, fresh, belly, raw
    },
    {
        "ingredient_key": "laurel_leaves",
        "display_name": "Laurel (Bay) Leaves",
        "category": "seasoning",
        "sub_category": "herb",
        "fdc_id": 170917,  # Spices, bay leaf
    },
    {
        "ingredient_key": "black_beans",
        "display_name": "Black Beans (Canned)",
        "category": "seasoning",
        "sub_category": "legume",
        "fdc_id": 175188,  # Beans, black turtle, mature seeds, canned
    },
    {
        "ingredient_key": "alugbati",
        "display_name": "Alugbati (Malabar Spinach)",
        "category": "produce",
        "sub_category": "leafy_green",
        "fdc_id": 170474,  # Vinespinach (basella), raw
    },
]

def fetch_food(fdc_id):
    url = f"{BASE_URL}/food/{fdc_id}?api_key={API_KEY}"
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode())

def extract_nutrients(food_data):
    nutrients = {}
    for fn in food_data.get("foodNutrients", []):
        nid = fn.get("nutrient", {}).get("id", 0)
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

    existing_keys = {i["ingredient_key"] for i in ingredients}

    for item in NEW_INGREDIENTS:
        key = item["ingredient_key"]
        if key in existing_keys:
            print(f"[SKIP] {key} already exists")
            continue

        fdc_id = item["fdc_id"]
        print(f"[ADD] {key} (FDC {fdc_id})...", end=" ")
        time.sleep(0.5)

        food_data = fetch_food(fdc_id)
        usda_desc = food_data.get("description", "Unknown")
        nutrients = extract_nutrients(food_data)
        portions = extract_portions(food_data)

        new_item = {
            "ingredient_key": key,
            "display_name": item["display_name"],
            "category": item["category"],
            "sub_category": item["sub_category"],
            "fdc_id": fdc_id,
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
            "portions": portions if portions else [],
        }
        ingredients.append(new_item)
        print(f"OK - \"{usda_desc}\" ({nutrients.get('calories', '?')} kcal)")

    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(ingredients, f, indent=2, ensure_ascii=False)

    print(f"\nTotal ingredients now: {len(ingredients)}")
    print("Done!")

if __name__ == "__main__":
    main()
