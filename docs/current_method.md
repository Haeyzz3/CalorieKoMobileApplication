### The Formula

```
score = (core_matched × 3 + optional_matched × 1) / (core_total × 3 + optional_total × 1)
```

Core ingredients are weighted **3×** heavier than optional ones. This means a single core match contributes as much to the score as 3 optional matches. The denominator is the "perfect score" — what you'd get if you had _everything_.

### Concrete Walkthroughs

Let me pick 4 dishes that illustrate different scenarios:

---

#### 🥚 **Boiled Egg** (simplest dish — 1 Core, 1 Optional)

||Core|Optional|
|---|---|---|
|Total|1 (`chicken_egg`)|1 (`water`)|

**Scenario: User has only `water` (no `chicken_egg`)**

- `core_matched = 0`, `optional_matched = 1`
- `score = (0×3 + 1×1) / (1×3 + 1×1) = 1/4 = 0.25`
- **Result: Hidden** (below 0.40 threshold)
- ✅ This is correct — having only water shouldn't suggest "Boiled Egg"

**Scenario: User has `chicken_egg` (no water)**

- `core_matched = 1`, `optional_matched = 0`
- `score = (1×3 + 0×1) / (1×3 + 1×1) = 3/4 = 0.75`
- **Result: Ready to Cook** (all core matched)
- ✅ Same as current behavior

---

#### 🍲 **Kinilaw na Tuna** (2 Core, 8 Optional — your exact problem scenario)

||Core|Optional|
|---|---|---|
|Total|2 (`tuna_fish`, `guso_seaweed`)|8 (vinegar, onion, tomato, ginger, calamansi, salt, pepper, water)|

**Scenario: User has all 8 Optional but 0 Core**

- `core_matched = 0`, `optional_matched = 8`
- `score = (0×3 + 8×1) / (2×3 + 8×1) = 8/14 = 0.571`
- **Result: Almost Ready** ✅ (above 0.40 — **rescued!**)
- Under the current system this dish is **completely hidden**

**Scenario: User has 3 Optional, 0 Core**

- `score = (0×3 + 3×1) / (2×3 + 8×1) = 3/14 = 0.214`
- **Result: Hidden** ✅ (below 0.40 — user isn't close enough)

**Scenario: User has 1 Core + 0 Optional**

- `score = (1×3 + 0×1) / 14 = 3/14 = 0.214`
- Hmm — this would be **Hidden** under the threshold. But under the current system, 1 core match puts this in "Almost Ready". This is a **behavioral change** we need to decide on. See open question below.

---

#### 🍖 **Menudo** (5 Core, 7 Optional — complex dish)

||Core|Optional|
|---|---|---|
|Total|5 (`pork_liempo`, `tomato_sauce`, `potato`, `bell_pepper_red`, `green_peas`)|7 (oil, garlic, onion, salt, pepper, water, sugar, raisins)|

**Scenario: User has all 7 Optional, 0 Core**

- `score = (0×3 + 7×1) / (5×3 + 7×1) = 7/22 = 0.318`
- **Result: Hidden** (below 0.40)
- ✅ Makes sense — missing all 5 core ingredients is a big shopping trip even with all seasonings

**Scenario: User has all 7 Optional + 2 Core**

- `score = (2×3 + 7×1) / 22 = 13/22 = 0.591`
- **Result: Almost Ready** ✅

---

#### 🐟 **Sinabawang Bangus** (3 Core, 5 Optional)

||Core|Optional|
|---|---|---|
|Total|3 (`sayote`, `bangus_fish`, `pechay`)|5 (oil, ginger, garlic, onion, water, salt)|

**Scenario: User has all 5 Optional, 0 Core**

- `score = (0×3 + 5×1) / (3×3 + 5×1) = 5/14 = 0.357`
- **Result: Hidden** (just below 0.40)

**Scenario: User has all 5 Optional + 1 Core**

- `score = (1×3 + 5×1) / 14 = 8/14 = 0.571`
- **Result: Almost Ready** ✅

---

### Classification Summary

|Condition|Category|
|---|---|
|`core_matched == core_total`|**Ready to Cook** _(unchanged)_|
|`score >= 0.40` and `core_matched < core_total`|**Almost Ready**|
|`score < 0.40`|**Hidden**|

Current Behavior

| Behavior                      | Current                                                                     |
| ----------------------------- | --------------------------------------------------------------------------- |
| 0 core, many optional matched | ✅ Shown if score ≥ 0.40                                                     |
| 1 core matched, 0 optional    | ⚠️ Depends on score (could be hidden for dishes with many core ingredients) |
| Sort order in "Ready to Cook" | Fixed — secondary sort by score or missing-optional count                   |
| Sort order in "Almost Ready"  | Score-based (considers both core + optional)                                |
