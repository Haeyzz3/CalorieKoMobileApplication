"""List all ingredients grouped by category to audit misplacements."""
import json

with open(r"c:\Users\dcjon\AndroidStudioProjects\CalorieKoMobileApplication\app\src\main\assets\raw_ingredients.json", "r", encoding="utf-8") as f:
    data = json.load(f)

from collections import defaultdict
groups = defaultdict(list)
for item in data:
    groups[item["category"]].append({
        "key": item["ingredient_key"],
        "display": item["display_name"],
        "sub_cat": item["sub_category"],
    })

CATEGORY_LABELS = {
    "pantry_staple": "Pantry Staples",
    "grain_starch": "Grains & Starches",
    "seasoning": "Seasonings & Sauces",
    "produce": "Produce",
    "protein": "Protein",
    "store_bought": "Store-Bought",
}

for cat in ["pantry_staple", "grain_starch", "seasoning"]:
    items = groups[cat]
    label = CATEGORY_LABELS.get(cat, cat)
    print(f"=== {label} ({cat}) — {len(items)} items ===")
    for it in sorted(items, key=lambda x: x["sub_cat"]):
        print(f"  {it['key']:35s} | {it['display']:40s} | sub: {it['sub_cat']}")
    print()

# Also show produce and protein for reference
for cat in ["produce", "protein"]:
    items = groups[cat]
    label = CATEGORY_LABELS.get(cat, cat)
    print(f"=== {label} ({cat}) — {len(items)} items ===")
    for it in sorted(items, key=lambda x: x["sub_cat"]):
        print(f"  {it['key']:35s} | {it['display']:40s} | sub: {it['sub_cat']}")
    print()
