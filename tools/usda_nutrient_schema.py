"""Canonical USDA nutrient extraction for CalorieKo ingredients.

All values are per 100 g. Vitamin A is always RAE in micrograms, sourced from
USDA nutrient 1106. Nutrient 1104 is the older international-unit Vitamin A
value and must not be mapped to
CalorieKo's ``vitamin_a`` field.
"""

from __future__ import annotations

from collections import OrderedDict
from typing import Any


NUTRIENT_KEYS = [
    "calories",
    "protein",
    "carbs",
    "fat",
    "fiber",
    "sugar",
    "sodium",
    "potassium",
    "vitamin_a",
    "vitamin_c",
    "calcium",
    "iron",
]

NUTRIENT_UNITS = {
    "calories": "kcal",
    "protein": "g",
    "carbs": "g",
    "fat": "g",
    "fiber": "g",
    "sugar": "g",
    "sodium": "mg",
    "potassium": "mg",
    "vitamin_a": "µg RAE",
    "vitamin_c": "mg",
    "calcium": "mg",
    "iron": "mg",
}

# Ordered by preferred source ID. Fallbacks are only used when preferred IDs
# are absent from a USDA response.
NUTRIENT_ID_CANDIDATES = OrderedDict(
    [
        ("calories", (1008, 2047, 2048)),
        ("protein", (1003,)),
        ("carbs", (1005,)),
        ("fat", (1004,)),
        ("fiber", (1079,)),
        ("sugar", (2000, 1063)),
        ("sodium", (1093,)),
        ("potassium", (1092,)),
        ("vitamin_a", (1106,)),
        ("vitamin_c", (1162,)),
        ("calcium", (1087,)),
        ("iron", (1089,)),
    ]
)

CANONICAL_NUTRIENT_ID_MAP = {
    ids[0]: key for key, ids in NUTRIENT_ID_CANDIDATES.items()
}

DISALLOWED_NUTRIENT_IDS = {
    1104: "Vitamin A, international units",
}


def empty_nutrients() -> dict[str, float]:
    """Return all tracked nutrients initialized to zero."""
    return {key: 0.0 for key in NUTRIENT_KEYS}


def _coerce_id(value: Any) -> int | None:
    if value is None:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


def _coerce_float(value: Any) -> float | None:
    if value is None:
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def _nutrient_id(food_nutrient: dict[str, Any]) -> int | None:
    nutrient = food_nutrient.get("nutrient") or {}
    return _coerce_id(nutrient.get("id") or food_nutrient.get("nutrientId"))


def _nutrient_value(food_nutrient: dict[str, Any], value_keys: tuple[str, ...]) -> float | None:
    for key in value_keys:
        value = _coerce_float(food_nutrient.get(key))
        if value is not None:
            return value
    return None


def _lookup_from_food_nutrients(
    food_nutrients: list[dict[str, Any]],
    value_keys: tuple[str, ...],
) -> dict[int, float]:
    lookup: dict[int, float] = {}
    for food_nutrient in food_nutrients:
        nutrient_id = _nutrient_id(food_nutrient)
        value = _nutrient_value(food_nutrient, value_keys)
        if nutrient_id is not None and value is not None:
            lookup[nutrient_id] = value
    return lookup


def _extract_from_lookup(lookup: dict[int, float]) -> dict[str, float]:
    nutrients = empty_nutrients()
    for key, candidate_ids in NUTRIENT_ID_CANDIDATES.items():
        for nutrient_id in candidate_ids:
            if nutrient_id in lookup:
                nutrients[key] = round(lookup[nutrient_id], 2)
                break
    return nutrients


def extract_nutrients_from_detail(food_data: dict[str, Any]) -> dict[str, float]:
    """Extract nutrients from the /food/{fdcId} detail API response."""
    lookup = _lookup_from_food_nutrients(
        food_data.get("foodNutrients", []),
        ("amount", "value"),
    )
    return _extract_from_lookup(lookup)


def extract_nutrients_from_search(food_item: dict[str, Any]) -> dict[str, float]:
    """Extract nutrients from a /foods/search result item."""
    lookup = _lookup_from_food_nutrients(
        food_item.get("foodNutrients", []),
        ("value", "amount"),
    )
    return _extract_from_lookup(lookup)


def extract_nutrients(food_data: dict[str, Any]) -> dict[str, float]:
    """Backward-compatible alias for detail API responses."""
    return extract_nutrients_from_detail(food_data)
