# Advanced Food System Mod - Requirements & Specifications

## Overview

This folder contains the complete requirements and specifications for the Advanced Food System Mod for Minecraft NeoForge.

**Three Documents:**
1. **01_SYSTEM_REQUIREMENTS.md** - What the mod does and how it works
2. **02_UI_UX_SPECIFICATION.md** - How the UI looks and behaves
3. **03_CONFIGURATION_GUIDE.md** - How to configure and customize

---

## Quick Summary

### What It Does
- Reduces max health to 7 hearts (makes food critical for survival)
- Adds food-based buff system with custom effects (no potion effects)
- Shows 3-slot buff display HUD (bottom-left corner)
- Supports buff combinations (eat 2 foods together = bonus buff)

### Key Features
- **Custom Effect System**: No vanilla potion effects, fully custom-built
- **NBT Persistence**: Buffs survive logout/login
- **JSON Configuration**: Fully customizable via JSON files
- **Combination System**: Eating specific foods together grants bonus buffs
- **Visual Feedback**: Clear HUD display with timers and progress bars

### Design Approach
- Single food event triggers both vanilla hunger AND custom buffs
- Two systems work together (not separate)
- Easy to expand (add foods/combos via JSON)
- Balanced and performant

---

## Document Guide

### 01_SYSTEM_REQUIREMENTS.md
**Read this to understand:**
- What the mod does
- Health system (7 hearts base, 11 max with food)
- Buff system structure
- Custom effect system (not vanilla potion effects)
- How food works
- Buff combinations
- Data storage (NBT)
- Edge cases

**Sections:**
- Core System Overview
- System Components (Health, Buffs, Effects, Food, Combinations)
- Data Storage
- Configuration System
- Technical Requirements
- What It Does/Doesn't Do
- Edge Cases

---

### 02_UI_UX_SPECIFICATION.md
**Read this to understand:**
- How the buff HUD looks
- HUD position and layout
- Visual appearance (colors, fonts, icons)
- Timer formatting
- Notifications and feedback
- HUD customization options
- Rendering behavior

**Sections:**
- Position & Layout
- Visual Appearance
- Multi-Slot Layout
- Timer Formatting
- Notifications & Feedback
- HUD Customization & Toggles
- Rendering Behavior
- Player Experience Flow
- Visual Style Guidelines

---

### 03_CONFIGURATION_GUIDE.md
**Read this to understand:**
- How to configure the mod
- JSON file locations
- What each setting does
- Default configurations
- How to add new foods
- How to create combinations
- Examples of custom setups

**Sections:**
- File Locations
- mod_config.json (System & HUD settings)
- food_buffs.json (Foods and buffs)
- buff_combinations.json (Combo rules)
- Validation & Error Handling
- Example Configurations
- Advanced Customization

---

## Key Design Decisions

### Why Custom Effects (Not Potion Effects)?
- **Control**: Mod has complete control over how buffs work
- **Flexibility**: Can create any effect imaginable
- **Clean UI**: Buffs don't clutter vanilla potion effect area
- **Combinations**: Easier to detect and trigger combinations

### Why Reduce Health to 7 Hearts?
- **Food Importance**: Makes food survival-critical
- **Challenge**: Adds difficulty to survival mode
- **Strategy**: Players must plan food/eating
- **Integration**: Food bonus comes from full saturation (makes sense)

### Why 3-Slot HUD?
- **Not Overwhelming**: 3 buffs is enough info, not too much
- **Readable**: Fits in corner without blocking vision
- **Manageable**: If >3 buffs, oldest shown first (auto-management)

### Why Combinations?
- **Discovery**: Players find foods work together
- **Strategy**: Rewards planning and preparation
- **Fun**: Creates memorable moments ("I unlocked Warrior Mode!")
- **Depth**: Adds progression/mastery feeling

---

## Technical Approach

### No Potion Effects
- Buffs managed completely separately
- Track in NBT, apply via stat modifications
- No icons in vanilla potion effect area
- Custom HUD displays buffs instead

### NBT Storage
- Buffs stored in player persistent data
- Survives logout/login
- Standard Minecraft save format
- Auto-syncs to client

### JSON Configuration
- Three config files: mod_config, food_buffs, buff_combinations
- Auto-generate defaults if missing
- Reloadable without restart
- Unlimited foods/buffs/combinations

---

## Implementation Notes

### What to Build First
1. Health system (reduce max HP)
2. Buff data structures (NBT storage)
3. Food eat event (detect eating)
4. Buff ticker (duration countdown)
5. HUD renderer (display buffs)
6. Buff combinations (trigger bonuses)
7. Configuration system (JSON loading)

### Performance Requirements
- Server tick cost: <1ms per player
- HUD render cost: <2ms per frame
- Memory: <1KB per player

### Mod Dependencies
- NeoForge (required)
- No other mod dependencies

---

## Configuration Files

### mod_config.json
Controls: Health values, HUD position/scale, notifications

### food_buffs.json
Defines: Which foods give which buffs and for how long

### buff_combinations.json
Defines: Which buff combinations trigger which bonus buffs

---

## Customization Examples

### Change Max Health
Edit mod_config.json:
```json
"maxHearts": 6,
"maxHeartsWithFood": 10
```

### Add New Food
Edit food_buffs.json:
```json
"minecraft:golden_carrot": {
  "buffs": ["regeneration", "damage_reduction"],
  "durationSeconds": 90,
  "magnitude": 0.2
}
```

### Create New Combo
Edit buff_combinations.json:
```json
"speed_demon": {
  "requires": ["walk_speed", "attack_speed"],
  "buffId": "speed_boost",
  "durationSeconds": 15,
  "magnitude": 1.8
}
```

---

## Testing Checklist

- [ ] Max health is 7 hearts
- [ ] Full saturation grants 4 hearts bonus
- [ ] Eating food adds buff to NBT
- [ ] Buff timer counts down
- [ ] Buff expires and disappears
- [ ] HUD renders at correct position
- [ ] HUD shows 3 slots max
- [ ] Timer displays MM:SS format
- [ ] Progress bar fills correctly
- [ ] Combinations trigger correctly
- [ ] Multiple foods stacking works
- [ ] Config files load properly
- [ ] NBT persists after logout

---

## Questions?

Refer to the specific document:
- **"How does the health system work?"** → 01_SYSTEM_REQUIREMENTS.md
- **"What does the HUD look like?"** → 02_UI_UX_SPECIFICATION.md
- **"How do I configure buffs?"** → 03_CONFIGURATION_GUIDE.md

---

## Summary

You have complete specifications for:
- ✓ What the mod does
- ✓ How it works
- ✓ How it looks
- ✓ How it behaves
- ✓ How to configure it
- ✓ How to customize it

No code examples (you requested clean requirements only).
No phases or roadmap (just the requirements).
No success metrics (just implementation details).

Ready to build! 🚀
