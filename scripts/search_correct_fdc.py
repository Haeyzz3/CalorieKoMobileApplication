"""
Search USDA FoodData Central for correct FDC IDs for each mismatched ingredient.
Uses proper English search terms to find the right USDA entry.
"""

import json
import time
import urllib.request
import urllib.parse
import os

API_KEY = "NxgG3iwRfp4HpfmejYVeRAHmJqlBTYeyKyMCHSVc"
BASE_URL = "https://api.nal.usda.gov/fdc/v1"

# Nutrient IDs we care about
NUTRIENT_IDS = {
    1008: "calories", 1003: "protein", 1005: "carbs", 1004: "fat",
    1079: "fiber", 2000: "sugar", 1093: "sodium", 1092: "potassium",
    1104: "vitamin_a", 1162: "vitamin_c", 1087: "calcium", 1089: "iron",
}

def search_foods(query, data_type="SR Legacy", page_size=5):
    """Search USDA FDC for foods matching query."""
    params = urllib.parse.urlencode({
        "api_key": API_KEY,
        "query": query,
        "dataType": data_type,
        "pageSize": page_size,
    })
    url = f"{BASE_URL}/foods/search?{params}"
    try:
        req = urllib.request.Request(url)
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode())
    except Exception as e:
        return {"error": str(e)}

def fetch_food(fdc_id):
    """Fetch full food details from USDA API."""
    url = f"{BASE_URL}/food/{fdc_id}?api_key={API_KEY}"
    try:
        req = urllib.request.Request(url)
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode())
    except Exception as e:
        return {"error": str(e)}

def extract_nutrients_from_search(food_item):
    """Extract nutrients from search result format."""
    nutrients = {}
    for fn in food_item.get("foodNutrients", []):
        nid = fn.get("nutrientId", 0)
        if nid in NUTRIENT_IDS:
            nutrients[NUTRIENT_IDS[nid]] = fn.get("value", 0.0)
    return nutrients

def extract_nutrients_from_detail(food_data):
    """Extract nutrients from detail API format."""
    nutrients = {}
    for fn in food_data.get("foodNutrients", []):
        nutrient_obj = fn.get("nutrient", {})
        nid = nutrient_obj.get("id", 0)
        if nid in NUTRIENT_IDS:
            nutrients[NUTRIENT_IDS[nid]] = fn.get("amount", 0.0)
    for key in NUTRIENT_IDS.values():
        if key not in nutrients:
            nutrients[key] = 0.0
    return nutrients

def extract_portions_from_detail(food_data):
    """Extract portions from detail API format."""
    portions = []
    for p in food_data.get("foodPortions", []):
        desc = p.get("portionDescription") or p.get("modifier") or p.get("measureUnit", {}).get("name", "")
        grams = p.get("gramWeight", 0)
        if desc and grams:
            portions.append({"description": desc, "grams": grams})
    return portions

# Each ingredient that needs fixing, with the proper English search term
# Format: (ingredient_key, search_query, notes)
INGREDIENTS_TO_FIX = [
    # === WRONG FDC ID (40) ===
    ("tinapa_fish", "fish smoked herring", "Filipino tinapa is usually smoked herring or milkfish"),
    ("pork_liempo", "pork belly raw", "Liempo = pork belly, not ground pork"),
    ("bangus_fish", "fish milkfish raw", "Bangus = milkfish (Chanos chanos)"),
    ("galunggong_fish", "fish scad raw", "Galunggong = round scad, a common Filipino fish"),
    ("milkfish", "fish milkfish raw", "Same as bangus"),
    ("mackerel_fish", "fish mackerel Atlantic raw", "Mackerel, not halibut"),
    ("lato_seaweed", "seaweed raw", "Lato = sea grapes (Caulerpa), fresh seaweed"),
    ("guso_seaweed", "seaweed raw", "Guso = Eucheuma seaweed, fresh"),
    ("ampalaya", "bittermelon raw", "Ampalaya = bitter melon/bitter gourd"),
    ("malunggay_leaves", "drumstick leaves raw moringa", "Malunggay = moringa oleifera leaves"),
    ("gabi", "taro raw", "Gabi = taro root"),
    ("kamote_tops_green", "sweet potato leaves raw", "Kamote tops = sweet potato leaves, NOT yam"),
    ("sayote", "chayote fruit raw", "Sayote = chayote squash"),
    ("pechay", "cabbage chinese bok choy raw", "Pechay = bok choy"),
    ("papaya_green", "papaya raw", "Green papaya - use ripe papaya as closest proxy"),
    ("mango_unripe", "mango raw", "Unripe mango - use ripe mango as closest proxy"),
    ("salt_iodized", "salt table", "Plain iodized table salt"),
    ("vinegar_cane", "vinegar distilled", "Cane vinegar - closest to distilled vinegar"),
    ("vinegar_white", "vinegar distilled", "White vinegar = distilled vinegar"),
    ("calamansi_juice", "lime juice raw", "Calamansi juice - closest proxy is lime juice"),
    ("patis", "fish sauce ready to serve", "Patis = Filipino fish sauce"),
    ("soy_sauce", "soy sauce made from soy", "Standard soy sauce"),
    ("thyme", "spices thyme dried", "Dried thyme herb"),
    ("cornstarch", "cornstarch", "Pure cornstarch"),
    ("cooking_oil", "oil soybean salad or cooking", "Soybean cooking oil"),
    ("tomato_sauce", "sauce tomato canned", "Canned tomato sauce"),
    ("odong_noodles", "noodles japanese somen wheat dry", "Odong/udon = thick wheat noodles"),
    ("sardines_tomato_sauce_canned", "sardines canned tomato sauce", "Sardines in tomato sauce, not oil"),
    ("chicken_thigh", "chicken thigh meat only raw", "Chicken thigh, raw"),
    ("chicken_drumstick", "chicken drumstick meat only raw", "Chicken drumstick, raw"),
    ("pork_shoulder", "pork shoulder raw", "Pork shoulder/butt, raw"),
    ("ground_pork", "pork ground raw", "Ground pork, raw"),
    ("bihon_noodles", "rice noodles dry", "Bihon = rice vermicelli noodles, dry"),
    ("canton_noodles", "noodles egg dry", "Canton = egg noodles, dry"),
    ("lime_juice", "lime juice raw", "Fresh lime juice"),
    ("tuyo_fish", "fish anchovy dried", "Tuyo = dried salted fish, similar to dried anchovy"),
    ("sitaw", "beans yard long raw", "Sitaw = yard-long beans"),
    ("baguio_beans", "beans snap green raw", "Baguio beans = green snap beans"),
    ("kangkong_leaves", "spinach water raw", "Kangkong = water spinach (Ipomoea aquatica)"),
    ("squash", "squash winter butternut raw", "Kalabasa = winter squash like butternut"),
    # === WRONG PREP STATE (4) ===
    ("tilapya_fish", "fish tilapia raw", "Need raw, not cooked"),
    ("brown_rice", "rice brown long grain raw", "Need raw, not cooked"),
    ("green_peas", "peas green frozen", "Labeled frozen but got raw - search for frozen"),
    ("alamang_bagoong", "shrimp paste", "Need standard reference, not branded sauteed"),
]

def main():
    results = []
    total = len(INGREDIENTS_TO_FIX)
    
    print(f"Searching USDA for correct FDC IDs for {total} ingredients...")
    print("=" * 90)
    
    for idx, (key, query, notes) in enumerate(INGREDIENTS_TO_FIX):
        print(f"\n[{idx+1}/{total}] {key}")
        print(f"  Search: \"{query}\"")
        print(f"  Notes: {notes}")
        
        time.sleep(0.6)  # Rate limit
        
        search_result = search_foods(query)
        
        if "error" in search_result:
            print(f"  ERROR: {search_result['error']}")
            results.append({
                "key": key, "query": query, "notes": notes,
                "candidates": [], "error": search_result["error"]
            })
            continue
        
        foods = search_result.get("foods", [])
        if not foods:
            # Try Foundation Foods
            print(f"  No SR Legacy results, trying Foundation Foods...")
            time.sleep(0.6)
            search_result = search_foods(query, data_type="Foundation")
            foods = search_result.get("foods", [])
        
        candidates = []
        for f in foods[:5]:
            desc = f.get("description", "")
            fdc_id = f.get("fdcId", 0)
            nutrients = extract_nutrients_from_search(f)
            cal = nutrients.get("calories", "?")
            pro = nutrients.get("protein", "?")
            carb = nutrients.get("carbs", "?")
            fat = nutrients.get("fat", "?")
            sod = nutrients.get("sodium", "?")
            
            print(f"  [{fdc_id}] {desc}")
            print(f"    cal={cal} pro={pro} carb={carb} fat={fat} sod={sod}")
            
            candidates.append({
                "fdc_id": fdc_id,
                "description": desc,
                "data_type": f.get("dataType", ""),
                "nutrients_preview": nutrients,
            })
        
        results.append({
            "key": key, "query": query, "notes": notes,
            "candidates": candidates,
        })
    
    # Save results
    output_path = os.path.join(os.path.dirname(__file__), "search_results.json")
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"\n\nResults saved to: {output_path}")

if __name__ == "__main__":
    main()
