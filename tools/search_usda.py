"""Search USDA for correct FDC IDs by description."""
import json
import urllib.request
import urllib.parse
import time

API_KEY = "NxgG3iwRfp4HpfmejYVeRAHmJqlBTYeyKyMCHSVc"
API_BASE = "https://api.nal.usda.gov/fdc/v1"

NUTRIENT_MAP = {
    1008: "calories", 1003: "protein", 1005: "carbs", 1004: "fat",
    1093: "sodium",
}

def search_food(query, data_type="SR Legacy"):
    url = f"{API_BASE}/foods/search?api_key={API_KEY}"
    body = json.dumps({
        "query": query,
        "dataType": [data_type],
        "pageSize": 5
    }).encode()
    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode())

searches = [
    ("vinegar cider", "SR Legacy"),
    ("vinegar distilled", "SR Legacy"),
    ("salt sea", "SR Legacy"),
    ("salt table", "SR Legacy"),
    ("oil corn", "SR Legacy"),
    ("vinegar", "SR Legacy"),
]

for query, dt in searches:
    print(f"=== Search: '{query}' ({dt}) ===")
    try:
        result = search_food(query, dt)
        foods = result.get("foods", [])
        for food in foods[:5]:
            fdc_id = food.get("fdcId")
            desc = food.get("description", "?")
            # Extract key nutrients
            nutrients = {}
            for fn in food.get("foodNutrients", []):
                nid = fn.get("nutrientId")
                if nid in NUTRIENT_MAP:
                    nutrients[NUTRIENT_MAP[nid]] = fn.get("value", 0)
            cal = nutrients.get("calories", "?")
            na = nutrients.get("sodium", "?")
            print(f"  FDC {fdc_id}: {desc} | Cal={cal} Na={na}")
        if not foods:
            print("  No results")
    except Exception as e:
        print(f"  ERROR: {e}")
    print()
    time.sleep(0.5)
