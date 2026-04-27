import json, urllib.request, urllib.parse
url = 'https://api.nal.usda.gov/fdc/v1/foods/search?' + urllib.parse.urlencode({
    'api_key': 'NxgG3iwRfp4HpfmejYVeRAHmJqlBTYeyKyMCHSVc',
    'query': 'beans black mature seeds canned',
    'dataType': 'SR Legacy', 'pageSize': 5
})
data = json.loads(urllib.request.urlopen(url).read())
NID = {1008:"cal",1003:"pro",1005:"carb",1004:"fat",1093:"sod"}
for f in data.get('foods', []):
    ns = {NID[n["nutrientId"]]: n["value"] for n in f["foodNutrients"] if n["nutrientId"] in NID}
    print(f"[{f['fdcId']}] {f['description']}")
    print(f"  cal={ns.get('cal','?')} pro={ns.get('pro','?')} carb={ns.get('carb','?')} fat={ns.get('fat','?')} sod={ns.get('sod','?')}")
