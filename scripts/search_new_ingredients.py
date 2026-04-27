"""Search USDA for 7 new ingredients needed by new dishes."""
import json, time, urllib.request, urllib.parse

API_KEY = "NxgG3iwRfp4HpfmejYVeRAHmJqlBTYeyKyMCHSVc"
BASE_URL = "https://api.nal.usda.gov/fdc/v1"

SEARCHES = [
    ("oregano_leaves", "spices oregano dried"),
    ("tanglad", "lemongrass raw"),
    ("spring_onion", "onions spring or scallions raw"),
    ("pork_belly", "pork belly raw"),
    ("laurel_leaves", "spices bay leaf"),
    ("black_beans", "beans black canned drained"),
    ("alugbati", "spinach malabar basella raw"),
]

def search(query, data_type="SR Legacy"):
    params = urllib.parse.urlencode({
        'api_key': API_KEY, 'query': query,
        'dataType': data_type, 'pageSize': 5
    })
    url = f"{BASE_URL}/foods/search?{params}"
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode())

NID = {1008:"cal",1003:"pro",1005:"carb",1004:"fat",1093:"sod"}

for key, query in SEARCHES:
    print(f"\n=== {key} (search: \"{query}\") ===")
    time.sleep(0.6)
    data = search(query)
    for f in data.get("foods", [])[:5]:
        ns = {NID[n["nutrientId"]]: n["value"] for n in f["foodNutrients"] if n["nutrientId"] in NID}
        print(f"  [{f['fdcId']}] {f['description']}")
        print(f"    cal={ns.get('cal','?')} pro={ns.get('pro','?')} carb={ns.get('carb','?')} fat={ns.get('fat','?')} sod={ns.get('sod','?')}")
