"""Verify and fix incorrect FDC IDs for corn_oil, sea_salt, vinegar entries."""
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

def fetch_and_print(fdc_id, label):
    url = f"{API_BASE}/food/{fdc_id}?api_key={API_KEY}"
    req = urllib.request.Request(url)
    req.add_header("Accept", "application/json")
    with urllib.request.urlopen(req, timeout=15) as resp:
        data = json.loads(resp.read().decode())
    
    desc = data.get("description", "?")
    nutrients = {name: 0.0 for name in NUTRIENT_MAP.values()}
    for fn in data.get("foodNutrients", []):
        nid = fn.get("nutrient", {}).get("id") or fn.get("nutrientId")
        if nid in NUTRIENT_MAP:
            amount = fn.get("amount", 0.0) or 0.0
            nutrients[NUTRIENT_MAP[nid]] = round(amount, 2)
    
    print(f"FDC {fdc_id} ({label}):")
    print(f"  Description: {desc}")
    print(f"  Cal={nutrients['calories']} P={nutrients['protein']} C={nutrients['carbs']} F={nutrients['fat']} Na={nutrients['sodium']}")
    print()
    time.sleep(0.5)

# Test candidate IDs
candidates = [
    (171029, "Corn Oil (SR Legacy)"),
    (172336, "Canola Oil (existing)"),
    (171413, "Olive Oil (existing)"),
    (171867, "Vinegar, cider"),
    (1002048, "Vinegar, cider (Foundation)"),
    (171866, "Vinegar, distilled"),
    (171868, "Vinegar, coconut? (current ID)"),
    (170457, "Salt, table (current iodized)"),
    (173468, "Kosher salt? (current ID)"),
    (173470, "Sea salt? (current ID)"),
]

for fdc_id, label in candidates:
    try:
        fetch_and_print(fdc_id, label)
    except Exception as e:
        print(f"FDC {fdc_id} ({label}): ERROR - {e}\n")
