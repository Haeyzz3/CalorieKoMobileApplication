import json

labels = set(l.strip() for l in open(r'app\src\main\assets\labels.txt', 'r') if l.strip() and l.strip() != 'negative')
dishes = set(d['dish_label'] for d in json.load(open(r'app\src\main\assets\dish_recipes.json', 'r', encoding='utf-8')))
csv_labels = set()
with open(r'app\src\main\assets\dish_labels_and_values.csv', 'r', encoding='utf-8', errors='replace') as f:
    for line in f:
        ml = line.split(',')[0].strip()
        if ml and ml != 'ml_label':
            csv_labels.add(ml)

print(f"labels.txt: {len(labels)} dishes")
print(f"dish_recipes.json: {len(dishes)} dishes")
print(f"dish_labels_and_values.csv: {len(csv_labels)} dishes")
print()
print(f"In labels but not recipes: {sorted(labels - dishes) or 'NONE'}")
print(f"In recipes but not labels: {sorted(dishes - labels) or 'NONE'}")
print(f"In labels but not CSV: {sorted(labels - csv_labels) or 'NONE'}")
print(f"In CSV but not labels: {sorted(csv_labels - labels) or 'NONE'}")
