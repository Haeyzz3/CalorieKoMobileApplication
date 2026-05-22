"""
Ingredient Data Verification Script
Queries the USDA FoodData Central API to verify all FDC IDs and nutrient values
in raw_ingredients.json against the actual USDA database.
"""

import json
import time
import urllib.request
import urllib.error
import os
import csv
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")
sys.stderr.reconfigure(encoding="utf-8", errors="replace")

TOOLS_DIR = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "tools"))
if TOOLS_DIR not in sys.path:
    sys.path.insert(0, TOOLS_DIR)

from usda_nutrient_schema import (
    NUTRIENT_KEYS,
    NUTRIENT_UNITS,
    extract_nutrients_from_detail,
)

API_KEY = "NxgG3iwRfp4HpfmejYVeRAHmJqlBTYeyKyMCHSVc"
BASE_URL = "https://api.nal.usda.gov/fdc/v1"

def fetch_food(fdc_id):
    """Fetch food details from USDA API."""
    url = f"{BASE_URL}/food/{fdc_id}?api_key={API_KEY}"
    try:
        req = urllib.request.Request(url)
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        return {"error": f"HTTP {e.code}"}
    except Exception as e:
        return {"error": str(e)}

def extract_nutrients(food_data):
    """Extract per-100g nutrients from USDA API response."""
    return extract_nutrients_from_detail(food_data)

def extract_portions(food_data):
    """Extract portion descriptions from USDA API response."""
    portions = []
    for p in food_data.get("foodPortions", []):
        desc = p.get("portionDescription") or p.get("modifier") or p.get("measureUnit", {}).get("name", "")
        grams = p.get("gramWeight", 0)
        if desc and grams:
            portions.append({"description": desc, "grams": grams})
    return portions

def compare_values(current, usda, tolerance=0.01):
    """Compare current vs USDA values, flag significant differences."""
    issues = []
    for key in NUTRIENT_KEYS:
        cur = current.get(key, 0)
        usd = usda.get(key, 0)

        if abs(cur - usd) <= tolerance:
            continue

        unit = NUTRIENT_UNITS.get(key, "")
        unit_suffix = f" {unit}" if unit else ""
        issues.append(f"  {key}: app={cur}{unit_suffix}, USDA={usd}{unit_suffix}")
    
    return issues

def main():
    # Load current data
    json_path = os.path.join(os.path.dirname(__file__), "..", 
                             "app", "src", "main", "assets", "raw_ingredients.json")
    json_path = os.path.normpath(json_path)
    
    with open(json_path, "r", encoding="utf-8") as f:
        ingredients = json.load(f)
    
    # Filter to browsable only (exclude store_bought)
    browsable = [i for i in ingredients if i.get("category") != "store_bought"]
    
    print(f"Verifying {len(browsable)} ingredients against USDA API...")
    print("=" * 80)
    
    results = []
    
    for idx, item in enumerate(browsable):
        key = item["ingredient_key"]
        display = item["display_name"]
        fdc_id = item.get("fdc_id")
        current_nutrients = item.get("nutrients_per_100g", {})
        
        print(f"\n[{idx+1}/{len(browsable)}] {key} (FDC: {fdc_id})")
        
        if not fdc_id:
            print(f"  âš  No FDC ID â€” skipping API check")
            results.append({
                "key": key,
                "display": display,
                "fdc_id": fdc_id,
                "usda_desc": "NO FDC ID",
                "status": "NO_FDC",
                "issues": ["No FDC ID assigned"],
                "usda_nutrients": {},
                "current_nutrients": current_nutrients,
            })
            continue
        
        # Rate limit: 1 request per 0.5 seconds
        time.sleep(0.5)
        
        food_data = fetch_food(fdc_id)
        
        if "error" in food_data:
            print(f"  âŒ API Error: {food_data['error']}")
            results.append({
                "key": key,
                "display": display,
                "fdc_id": fdc_id,
                "usda_desc": f"ERROR: {food_data['error']}",
                "status": "API_ERROR",
                "issues": [f"API returned: {food_data['error']}"],
                "usda_nutrients": {},
                "current_nutrients": current_nutrients,
            })
            continue
        
        usda_desc = food_data.get("description", "Unknown")
        usda_nutrients = extract_nutrients(food_data)
        usda_portions = extract_portions(food_data)
        
        print(f"  USDA: \"{usda_desc}\"")
        
        # Compare values
        issues = compare_values(current_nutrients, usda_nutrients)
        
        if issues:
            status = "MISMATCH"
            print(f"  âš  VALUE MISMATCHES:")
            for issue in issues:
                print(f"    {issue}")
        else:
            status = "OK"
            print(f"  âœ… Values match USDA data")
        
        results.append({
            "key": key,
            "display": display,
            "fdc_id": fdc_id,
            "usda_desc": usda_desc,
            "status": status,
            "issues": issues,
            "usda_nutrients": usda_nutrients,
            "current_nutrients": current_nutrients,
            "usda_portions": usda_portions,
        })
    
    # Summary
    print("\n" + "=" * 80)
    print("SUMMARY")
    print("=" * 80)
    
    ok = [r for r in results if r["status"] == "OK"]
    mismatch = [r for r in results if r["status"] == "MISMATCH"]
    no_fdc = [r for r in results if r["status"] == "NO_FDC"]
    errors = [r for r in results if r["status"] == "API_ERROR"]
    
    print(f"  âœ… OK: {len(ok)}")
    print(f"  âš  Mismatch: {len(mismatch)}")
    print(f"  ðŸŸ¡ No FDC ID: {len(no_fdc)}")
    print(f"  âŒ API Error: {len(errors)}")
    
    if mismatch:
        print(f"\nMISMATCHED INGREDIENTS:")
        for r in mismatch:
            print(f"  {r['key']}: \"{r['display']}\" â†’ USDA: \"{r['usda_desc']}\"")
            for issue in r["issues"]:
                print(f"    {issue}")
    
    # Save full results to JSON
    output_path = os.path.join(os.path.dirname(__file__), "verification_results.json")
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"\nFull results saved to: {output_path}")

if __name__ == "__main__":
    main()
