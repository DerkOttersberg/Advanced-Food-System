# Advanced Food System - Complete Feature README

## Overview
Advanced Food System is a NeoForge mod for Minecraft 1.21 that turns food into a long-duration build system.

Core philosophy:
- Food is a loadout, not just hunger refill.
- Players build around offensive, defensive, mobility, sustain, and utility lanes.
- Harmful foods are viable high-risk options.
- The system is multiplayer-safe and server-authoritative.

## Core Gameplay Model

### 1) Food Buff Slots
- Players can have up to 3 active food sources at once.
- Buffs are tracked server-side and persisted in player NBT.
- Duplicate exact source entries are prevented.
- One food source can carry multiple linked effects (positive plus minimal debuff for harmful foods).

### 2) Health Model
- Base max hearts are configurable.
- Food grants per-source heart bonus.
- Capstone combo logic unlocks the final heart (10-heart path).
- Health cap is enforced through transient max-health modifiers.

### 3) Duration Model
- Default matrix uses 1200 seconds (20 minutes) for food effects.
- Harmful-food debuffs are also 1200 seconds (20 minutes), minimal magnitude.
- Duration and strength are further tunable with global and per-effect multipliers.

## Full Food Matrix
- Implemented in defaults and documented in:
  - FOOD_MATRIX_README.md
- Includes all 40 edible foods in target scope.
- Soups/stews are enforced as +1 heart only and neutral for combo tags.

## Effect Lanes and Tags

### Lane Tags
- O = Offense
- D = Defense
- S = Sustain
- M = Mobility
- U = Utility
- R = Risk
- N = Neutral

### Implemented Effect IDs
Positive:
- mining_speed
- walk_speed
- attack_speed
- attack_damage
- damage_reduction
- regeneration
- hunger_efficiency
- saturation_boost
- knockback_resistance
- xp_gain
- heart_bonus (neutral marker for soups/stews)

Minimal long debuffs:
- frailty
- fatigue
- queasy
- appetite_leak

## Combo System

### Pair Intersections
- Intersections are computed from active lane tags.
- Multiple pair combos can be active simultaneously.
- Implemented pair combos include:
  - Predator, Bastion, Renewal, Windstep, Scholar
  - Vanguard, Reaver, Duelist, Hunter
  - Warden, Sentinel, Bulwark Sage
  - Ranger, Steward, Nomad
  - Bloodrush, Last Stand, Dark Renewal, Frenzy Step, Gambler, Cursed Chain

### Triple Capstones
- Triple lane intersections activate capstone combos.
- Implemented capstones include:
  - Bruiser Prime, Skirmish Tank, War Scholar
  - Blood Dancer, Reaping Sage, Raider
  - Juggernaut, Iron Sustainer, Expedition Guard, Endless Nomad

### Risk Tax
- If an R-tag food is part of the active build, capstone combo magnitude is reduced to 90%.
- Harmful-food debuffs remain active while risk combos are active.

## Harmful Food Design
- Harmful foods receive meaningful positive effects for build viability.
- They also apply minimal persistent debuffs for 20 minutes.
- This creates intentional high-risk archetypes without dominance.

## Hunger Efficiency Debug

### Admin Command
- /advfood debughunger on
- /advfood debughunger off

### Behavior
When enabled, once per second action-bar debug output shows:
- food level
- saturation
- hunger efficiency total
- appetite leak total
- net hunger efficiency

Use this to tune balance in survival and multiplayer testing sessions.

## Networking and Multiplayer
- Buff state is server-authoritative.
- Active buff state is synced to clients with packet payloads.
- NBT persistence survives logout/login.
- Clone/death events clear and broadcast removal lifecycles.
- Continuous effects and attributes are recomputed server-side every tick.

## API Integration (Seamless-API)
The mod integrates with Seamless-API for external mod compatibility.

Implemented integration points:
- SatiationAPI registration merge into local food map
- BuffApplyingEvent pre-application hook
- BuffAppliedEvent post-application hook
- BuffRemovedEvent expiry/death notifications
- BuffModifiers magnitude/health/filter hooks

This allows third-party mods to register foods and influence calculations safely.

## Config System

### Files
- config/advancedfoodsystem/food_buffs.json
- config/advancedfoodsystem/buff_combinations.json
- config/advancedfoodsystem/effect_strengths.json

### Runtime behavior
- Files auto-generate if missing.
- Values are sanitized/clamped.
- Effect strength multipliers are discoverable and editable.
- Reload command applies updated config state.

## UI and UX
- In-game config screen for core settings and HUD options.
- Effect strengths screen with page controls.
- Tooltip rendering shows:
  - heart bonus
  - positive effects and effective strengths
  - debuffs and effective strengths
  - duration

## Commands
- /advfood reload
- /advfood debughunger on
- /advfood debughunger off

## Implementation Notes

### Slot counting
- Slot cap uses distinct base food sources.
- Source suffixes are used internally for linked effects (positive/debuff instances from one food).

### Combo contribution model
- Combo buffs are synthetic entries (combo_*).
- Combo effects are expanded through ComboEffectRegistry into real effect channels.

### Damage pipeline
- damage_reduction is capped and applied.
- frailty increases incoming damage (capped) and stacks with reduction math.

## Build and Run

### Build
- gradlew.bat build

### Notes
- This README reflects current implemented feature set.
- No GitHub push is required or performed for local implementation.

## File Index (Key Implementation Files)
- src/main/java/com/derko/advancedfoodsystem/config/ConfigManager.java
- src/main/java/com/derko/advancedfoodsystem/config/FoodBuffEntry.java
- src/main/java/com/derko/advancedfoodsystem/events/CommonEvents.java
- src/main/java/com/derko/advancedfoodsystem/data/BuffStorage.java
- src/main/java/com/derko/advancedfoodsystem/data/BuffMath.java
- src/main/java/com/derko/advancedfoodsystem/gameplay/BuffTicker.java
- src/main/java/com/derko/advancedfoodsystem/gameplay/AttributeController.java
- src/main/java/com/derko/advancedfoodsystem/gameplay/BuffNames.java
- src/main/java/com/derko/advancedfoodsystem/gameplay/ComboEffectRegistry.java
- src/main/java/com/derko/advancedfoodsystem/commands/AdvFoodCommand.java

## Summary
Advanced Food System now includes:
- complete 40-food matrix baseline
- persistent minimal long debuffs for harmful foods
- all pair/triple intersection combo architecture
- risk-tax capstone balancing
- hunger-efficiency debug mode
- multiplayer-safe server-authoritative processing
- comprehensive matrix and feature documentation
