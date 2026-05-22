"""Verify and fix incorrect FDC IDs for corn_oil, sea_salt, vinegar entries."""
import json
import urllib.request
import time

from usda_nutrient_schema import empty_nutrients, extract_nutrients_from_detail

API_KEY = "NxgG3iwRfp4HpfmejYVeRAHmJqlBTYeyKyMCHSVc"
API_BASE = "https://api.nal.usda.gov/fdc/v1"

def fetch_and_print(fdc_id, label):
    url = f"{API_BASE}/food/{fdc_id}?api_key={API_KEY}"
    req = urllib.request.Request(url)
    req.add_header("Accept", "application/json")
    with urllib.request.urlopen(req, timeout=15) as resp:
        data = json.loads(resp.read().decode())
    
    desc = data.get("description", "?")
    nutrients = extract_nutrients_from_detail(data) if data else empty_nutrients()
    
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
