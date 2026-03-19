# Full Food Matrix Backup

## 0) Global Rules
- Base food effect duration: 1200s (20m)
- Harmful-food debuff duration: 1200s (20m)
- 3 active food slots
- Max 10 hearts only via capstone combo path
- Soups/stews contribute heart only, no combo tags

Tag legend:
- O = Offense
- D = Defense
- S = Sustain
- M = Mobility
- U = Utility
- R = Risk (harmful-food overlay tag)
- N = Neutral (soups/stews)

Effect legend:
- AS attack_speed
- AD attack_damage
- DR damage_reduction
- REG regeneration
- MS walk_speed
- MIN mining_speed
- KB knockback_resistance
- XP xp_gain
- HUNG hunger_efficiency
- SAT saturation_boost

Minimal long debuffs (all 20m):
- frailty: +0.04 incoming damage
- fatigue: -0.04 attack speed
- queasy: -0.03 movement speed
- appetite_leak: -0.06 hunger efficiency

## 1) Full Food Matrix (All 40)

| Food | Tags | Heart | Positive effect (20m) | Debuff (20m) |
|---|---|---:|---|---|
| Apple | S | +1 | REG +0.10 | none |
| Mushroom Stew | N | +1 | heart bonus only | none |
| Bread | U | +1 | HUNG +0.18 | none |
| Raw Porkchop | O,R | +1 | AD +0.07 | frailty |
| Cooked Porkchop | D | +1 | DR +0.12 | none |
| Golden Apple | S,D | +1 | REG +0.18, DR +0.06 | none |
| Enchanted Golden Apple | S,D,U | +1 | REG +0.22, DR +0.10, XP +0.08 | none |
| Raw Cod | U | +1 | XP +0.08 | none |
| Raw Salmon | S,U | +1 | REG +0.06, XP +0.06 | none |
| Tropical Fish | M,U | +1 | MS +0.05, XP +0.10 | none |
| Pufferfish | D,U,R | +1 | DR +0.14, XP +0.12 | queasy |
| Cooked Cod | U | +1 | XP +0.14, HUNG +0.06 | none |
| Cooked Salmon | S | +1 | REG +0.14 | none |
| Cookie | U | +1 | XP +0.16, MIN +0.08 | none |
| Melon Slice | M,D | +1 | KB +0.10, MS +0.06 | none |
| Raw Beef | O,R | +1 | AD +0.08 | fatigue |
| Steak | O,U | +1 | AD +0.08, MIN +0.14 | none |
| Raw Chicken | O,R | +1 | AS +0.10 | appetite_leak |
| Cooked Chicken | O | +1 | AS +0.16 | none |
| Rotten Flesh | O,U,R | +1 | AD +0.18, HUNG +0.08 | frailty |
| Spider Eye | O,U,R | +1 | AD +0.08, XP +0.18 | fatigue |
| Carrot | M | +1 | MS +0.14 | none |
| Potato | U | +1 | HUNG +0.10 | none |
| Baked Potato | U,S | +1 | HUNG +0.14, SAT +0.06 | none |
| Poisonous Potato | M,U,R | +1 | MS +0.16, XP +0.10 | frailty |
| Golden Carrot | S,D,U | +1 | REG +0.16, DR +0.08, XP +0.10 | none |
| Pumpkin Pie | U,S | +1 | HUNG +0.10, SAT +0.08 | none |
| Raw Rabbit | M,O,R | +1 | MS +0.07, AD +0.05 | appetite_leak |
| Cooked Rabbit | M,O | +1 | MS +0.12, AD +0.10 | none |
| Rabbit Stew | N | +1 | heart bonus only | none |
| Raw Mutton | O,R | +1 | AD +0.06 | frailty |
| Cooked Mutton | D | +1 | DR +0.10 | none |
| Chorus Fruit | M | +1 | MS +0.18, KB +0.08 | none |
| Beetroot | U | +1 | XP +0.08 | none |
| Beetroot Soup | N | +1 | heart bonus only | none |
| Dried Kelp | U | +1 | HUNG +0.12, MIN +0.06 | none |
| Suspicious Stew | N | +1 | heart bonus only | none |
| Sweet Berries | M,U | +1 | MS +0.06, XP +0.10 | none |
| Glow Berries | U | +1 | XP +0.14, MIN +0.08 | none |
| Cake | U,S | +1 | SAT +0.10, HUNG +0.08 | none |

## 2) Pair Intersection Matrix (All intersections)

| Intersect | Combo buff name | Combo effect |
|---|---|---|
| O + O | Predator | AS +0.05, AD +0.05 |
| D + D | Bastion | DR +0.05, KB +0.08 |
| S + S | Renewal | REG +0.08 |
| M + M | Windstep | MS +0.10 |
| U + U | Scholar | XP +0.12 |
| O + D | Vanguard | AD +0.03, DR +0.03 |
| O + S | Reaver | AS +0.04, REG +0.05 |
| O + M | Duelist | AS +0.04, MS +0.06 |
| O + U | Hunter | AD +0.04, XP +0.08 |
| D + S | Warden | DR +0.04, REG +0.04 |
| D + M | Sentinel | DR +0.03, MS +0.04 |
| D + U | Bulwark Sage | DR +0.03, XP +0.08 |
| S + M | Ranger | REG +0.04, MS +0.05 |
| S + U | Steward | REG +0.04, HUNG +0.08 |
| M + U | Nomad | MS +0.05, XP +0.08 |
| R + O | Bloodrush | AD +0.06 (risk debuff remains) |
| R + D | Last Stand | DR +0.05 (risk debuff remains) |
| R + S | Dark Renewal | REG +0.05 (risk debuff remains) |
| R + M | Frenzy Step | MS +0.06 (risk debuff remains) |
| R + U | Gambler | XP +0.10 (risk debuff remains) |
| R + R | Cursed Chain | AD +0.05, XP +0.08, plus extra frailty +0.02 |
| N + anything | No combo | none |

## 3) Triple Intersection Matrix (All 3-lane capstones)

| Triple intersection | Capstone | Capstone effect |
|---|---|---|
| O + D + S | Bruiser Prime | AD +0.05, DR +0.05, REG +0.05 |
| O + D + M | Skirmish Tank | AS +0.04, DR +0.04, MS +0.06 |
| O + D + U | War Scholar | AD +0.05, DR +0.04, XP +0.10 |
| O + S + M | Blood Dancer | AS +0.05, REG +0.05, MS +0.06 |
| O + S + U | Reaping Sage | AD +0.05, REG +0.05, XP +0.10 |
| O + M + U | Raider | AS +0.04, MS +0.07, XP +0.10 |
| D + S + M | Juggernaut | DR +0.06, REG +0.05, KB +0.08 |
| D + S + U | Iron Sustainer | DR +0.05, REG +0.05, HUNG +0.10 |
| D + M + U | Expedition Guard | DR +0.04, MS +0.05, XP +0.10 |
| S + M + U | Endless Nomad | REG +0.05, MS +0.05, HUNG +0.10 |

Capstone heart rule:
- Any valid capstone active plus heart threshold met unlocks the 10th heart.
- If any R-tag food is in the 3 slots, capstone effect strength is multiplied by 0.90.

## 4) Harmful Food Combo Intent
- Harmful foods are not just punishments.
- They offer above-average offensive/utility scaling.
- They always carry a minimal, persistent 20m drawback.
- Risk builds can hit strong pair/triple combos, but:
  - debuff remains
  - capstone reduced by 10% if R-tag included
  - R+R stack adds extra frailty

This keeps them viable, never mandatory, never dominant.

## 5) Soup/Stew Enforcement
- Mushroom Stew, Beetroot Soup, Rabbit Stew, Suspicious Stew:
  - +1 heart only
  - no effect tag contribution
  - no pair/triple combo generation
  - still useful as neutral heart-slot fillers
