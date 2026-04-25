import json, urllib.request, urllib.parse
url = 'https://api.nal.usda.gov/fdc/v1/foods/search?' + urllib.parse.urlencode({
    'api_key': 'NxgG3iwRfp4HpfmejYVeRAHmJqlBTYeyKyMCHSVc',
    'query': 'bitter melon gourd raw leafy pod',
    'dataType': 'SR Legacy',
    'pageSize': 10
})
resp = urllib.request.urlopen(url)
data = json.loads(resp.read())
for f in data.get('foods', []):
    cals = [n for n in f['foodNutrients'] if n['nutrientId'] == 1008]
    pros = [n for n in f['foodNutrients'] if n['nutrientId'] == 1003]
    carbs = [n for n in f['foodNutrients'] if n['nutrientId'] == 1005]
    cal = cals[0]['value'] if cals else '?'
    pro = pros[0]['value'] if pros else '?'
    carb = carbs[0]['value'] if carbs else '?'
    print(f"{f['fdcId']} | {f['description']} | cal={cal} pro={pro} carb={carb}")
