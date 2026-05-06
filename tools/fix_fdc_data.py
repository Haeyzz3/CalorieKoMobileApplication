"""Fix incorrect FDC data for corn_oil, sea_salt, and vinegar_coconut in raw_ingredients.json."""
import json
import urllib.request
import time

API_KEY = "NxgG3iwRfp4HpfmejYVeRAHmJqlBTYeyKyMCHSVc"
API_BASE = "https://api.nal.usda.gov/fdc/v1"

NUTRIENT_MAP = {
    1008: "calories", 1003: "protein", 1005: "carbs", 1004: "fat",
    1079: "fiber", 2000: "sugar", 1093: "sodium", 1092: "potassium",
    1106: "vitamin_a", 1162: "vitamin_c", 1087: "calcium", 1089: "iron",
}

JSON_PATH = r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\raw_ingredients.json"

def fetch_nutrients(fdc_id):
    url = f"{API_BASE}/food/{fdc_id}?api_key={API_KEY}"
    req = urllib.request.Request(url)
    req.add_header("Accept", "application/json")
    with urllib.request.urlopen(req, timeout=15) as resp:
        data = json.loads(resp.read().decode())
    nutrients = {name: 0.0 for name in NUTRIENT_MAP.values()}
    for fn in data.get("foodNutrients", []):
        nid = fn.get("nutrient", {}).get("id") or fn.get("nutrientId")
        if nid in NUTRIENT_MAP:
            amount = fn.get("amount", 0.0) or 0.0
            nutrients[NUTRIENT_MAP[nid]] = round(amount, 2)
    portions = []
    for pm in data.get("foodPortions", []):
        desc = pm.get("portionDescription") or pm.get("measureUnit", {}).get("name", "")
        grams = pm.get("gramWeight", 0.0)
        if desc and grams and grams > 0:
            portions.append({"description": desc, "grams": round(grams, 1)})
    return nutrients, portions[:3]

with open(JSON_PATH, "r", encoding="utf-8") as f:
    data = json.load(f)

# Fixes needed:
fixes = {
    "corn_oil": {
        "fdc_id": 171029,
        "fetch": True,
    },
    "sea_salt": {
        # No separate sea salt in USDA. Use same profile as table salt (FDC 173468)
        # but with manual data since sea salt ≈ table salt nutritionally
        "fdc_id": 173468,
        "manual_nutrients": {
            "calories": 0.0, "protein": 0.0, "carbs": 0.0, "fat": 0.0,
            "fiber": 0.0, "sugar": 0.0, "sodium": 38758.0, "potassium": 8.0,
            "vitamin_a": 0.0, "vitamin_c": 0.0, "calcium": 24.0, "iron": 0.33
        },
        "manual_portions": [{"description": "tsp", "grams": 6.0}, {"description": "tbsp", "grams": 18.0}],
    },
    "vinegar_coconut": {
        # No coconut vinegar in USDA SR Legacy. Use distilled vinegar (172237) as proxy
        # Coconut vinegar is nutritionally very similar to other vinegars (~18-22 kcal/100g)
        "fdc_id": 172237,
        "fetch": True,
        "display_name": "Coconut Vinegar (Sukang Tuba)",
        "data_source": "USDA_FDC_PROXY",  # Mark as proxy
    },
    "vinegar_apple_cider": {
        # 173469 was actually correct — let's re-fetch to confirm
        "fdc_id": 173469,
        "fetch": True,
    },
}

for item in data:
    key = item["ingredient_key"]
    if key in fixes:
        fix = fixes[key]
        
        if fix.get("fetch"):
            print(f"Fetching corrected data for {key} (FDC {fix['fdc_id']})...")
            nutrients, portions = fetch_nutrients(fix["fdc_id"])
            item["nutrients_per_100g"] = nutrients
            item["portions"] = portions
            item["fdc_id"] = fix["fdc_id"]
            if "data_source" in fix:
                item["data_source"] = fix["data_source"]
            if "display_name" in fix:
                item["display_name"] = fix["display_name"]
            print(f"  Updated: Cal={nutrients['calories']} P={nutrients['protein']} F={nutrients['fat']} C={nutrients['carbs']}")
            time.sleep(0.5)
        else:
            # Manual fix
            item["fdc_id"] = fix["fdc_id"]
            item["nutrients_per_100g"] = fix["manual_nutrients"]
            item["portions"] = fix["manual_portions"]
            print(f"  Manually fixed {key}: Cal={fix['manual_nutrients']['calories']} Na={fix['manual_nutrients']['sodium']}")

with open(JSON_PATH, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

print("\nDone! All corrections applied.")
