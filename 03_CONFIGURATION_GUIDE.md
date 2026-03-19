# CONFIGURATION & CUSTOMIZATION SPECIFICATION

## OVERVIEW

All mod features are configurable via JSON files. These files are stored in the mod's config directory and auto-generate with defaults if missing. No code changes are required to customize the mod.

---

## CONFIGURATION FILE LOCATIONS

**Directory Structure**
```
.minecraft/config/
└── advancedfoodsystem/
    ├── food_buffs.json              (Which foods give which buffs)
    ├── buff_combinations.json       (Buff combination rules)
    └── mod_config.json              (General settings)
```

**Creation**
- Files auto-generate on first load if missing
- If invalid JSON provided, defaults used (warning logged)
- Config reloadable via command (without restart)

---

## FILE 1: mod_config.json (General Settings)

**Purpose**: Control core mod behavior and HUD display

**Default File Structure**
```json
{
  "system": {
    "maxHearts": 7,
    "maxHeartsWithFood": 11,
    "enableBuffHud": true,
    "enableParticles": false,
    "enableSounds": false,
    "maxActiveBuffs": 20
  },
  "hud": {
    "position": "bottom_left",
    "scale": 1.0,
    "maxBuffsShown": 3,
    "offsetX": 10,
    "offsetY": -70,
    "renderFrequency": 1
  },
  "notifications": {
    "showBuffApplied": true,
    "showComboUnlocked": true,
    "showBuffExpired": false
  }
}
```

### System Settings

**maxHearts** (integer, range: 5-10)
- Base maximum health in half-hearts
- Default: 7 (= 3.5 hearts)
- Example: 10 = 5 hearts max
- Applied on player join

**maxHeartsWithFood** (integer, range: 10-20)
- Maximum health with full saturation
- Default: 11 (= 5.5 hearts max)
- Must be greater than maxHearts
- Example: 12 = 6 hearts max with food

**enableBuffHud** (true/false)
- Whether to render buff display HUD
- Default: true
- Set false to completely disable HUD rendering

**enableParticles** (true/false)
- Whether to show particle effects when buffs applied
- Default: false
- Minimal performance impact if enabled

**enableSounds** (true/false)
- Whether to play sound effects when buffs applied/expired
- Default: false
- Requires sound assets in mod

**maxActiveBuffs** (integer, range: 5-50)
- Maximum number of buffs player can have simultaneously
- Default: 20
- Excess buffs beyond this limit are ignored
- Prevents buff spam/overflow

### HUD Settings

**position** (string)
- Where HUD renders on screen
- Options: "bottom_left", "bottom_right", "top_left", "top_right"
- Default: "bottom_left"
- Invalid values fall back to default

**scale** (decimal, range: 0.5-2.0)
- Size of HUD element
- Default: 1.0 (normal size)
- 0.5 = half size (compact)
- 2.0 = double size (large)
- Applies to entire HUD (slots, text, bars)

**maxBuffsShown** (integer, range: 1-5)
- Number of buff slots displayed
- Default: 3
- If player has more buffs, oldest ones shown first
- Doesn't affect actual buff count (just display)

**offsetX** (integer)
- Horizontal pixel offset from position corner
- Default: 10 (10 pixels from left/right edge)
- Range: 0-100
- Positive = away from edge, Negative = toward edge

**offsetY** (integer)
- Vertical pixel offset from position corner
- Default: -70 (70 pixels from bottom edge)
- Negative = from bottom, Positive = from top
- Range: -1000 to 1000

**renderFrequency** (integer, range: 1-10)
- How often HUD updates (in frames)
- Default: 1 (every frame)
- 1 = 60 updates/second (if 60 FPS)
- 2 = 30 updates/second (skips every other frame)
- Optimization: higher number = less CPU, less smooth

### Notification Settings

**showBuffApplied** (true/false)
- Show action bar message when buff applied
- Default: true
- Message format: "§6+Buff Name"
- Format: "§6+Mining Speed"

**showComboUnlocked** (true/false)
- Show action bar message when combo triggered
- Default: true
- Message format: "§6✨ COMBO UNLOCKED: Combo Name"
- Example: "§6✨ COMBO UNLOCKED: Warrior Mode"

**showBuffExpired** (true/false)
- Show action bar message when buff expires
- Default: false
- Message format: "§7-Buff Name"
- Example: "§7-Mining Speed"

---

## FILE 2: food_buffs.json (Food → Buff Mappings)

**Purpose**: Define which foods grant which buffs

**File Structure**
```json
{
  "minecraft:cooked_beef": {
    "buffs": ["mining_speed"],
    "durationSeconds": 60,
    "magnitude": 0.1
  },
  "minecraft:carrot": {
    "buffs": ["walk_speed"],
    "durationSeconds": 45,
    "magnitude": 0.05
  },
  "minecraft:apple": {
    "buffs": ["jump_height"],
    "durationSeconds": 30,
    "magnitude": 0.5
  }
}
```

### Entry Structure

**Key**: Item registry name (string)
- Examples: "minecraft:cooked_beef", "farmersdelight:cooked_salmon"
- Format: "modid:itemname" (lowercase)
- Must be valid registry name or entry ignored

**Value**: Buff data object with:

**buffs** (array of strings)
- Array of buff IDs to apply
- Examples: ["mining_speed"], ["walk_speed", "saturation_boost"]
- Can have 1, 2, or more buffs per food
- If multiple: all buffs applied simultaneously

**durationSeconds** (integer, range: 15-300)
- How long buff lasts in seconds
- Default: 60 seconds
- Converted to ticks internally (multiply by 20)
- Example: 60 seconds = 1200 ticks

**magnitude** (decimal, range: 0.05-2.0)
- Strength of buff effect
- 0.1 = 10% boost
- 0.2 = 20% boost
- 1.0 = 100% boost (doubles attribute)
- Exact effect depends on buff type

### Default Foods (Pre-configured)

These come with default configuration:
```
minecraft:cooked_beef         → mining_speed (60s, 0.1)
minecraft:cooked_chicken      → walk_speed (45s, 0.05)
minecraft:cooked_porkchop     → attack_speed (45s, 0.1)
minecraft:cooked_salmon       → regeneration (30s, 0.2)
minecraft:carrot              → walk_speed (45s, 0.05)
minecraft:apple               → jump_height (30s, 0.5)
minecraft:bread               → saturation_boost (30s, 0.1)
minecraft:golden_carrot       → regeneration (60s, 0.3)
minecraft:melon_slice         → damage_reduction (30s, 0.05)
minecraft:poisonous_potato    → None (poison only)
minecraft:rotten_flesh        → None (no buff)
minecraft:spider_eye          → None (poison only)
```

### Adding New Foods

**Custom Modded Foods**
1. Find mod item registry name (e.g., "farmersdelight:cooked_salmon")
2. Add entry to food_buffs.json:
```json
{
  "farmersdelight:cooked_salmon": {
    "buffs": ["regeneration"],
    "durationSeconds": 60,
    "magnitude": 0.25
  }
}
```
3. Reload config (or restart server)
4. Eating that food now grants buff

**Creating New Food Buff Combos**
- Simply add food entry with different buff/duration/magnitude
- Example: Make apple give walk_speed instead:
```json
{
  "minecraft:apple": {
    "buffs": ["walk_speed"],
    "durationSeconds": 45,
    "magnitude": 0.15
  }
}
```

### Available Buff Types

| Buff ID | Effect | Magnitude Guide |
|---------|--------|-----------------|
| mining_speed | Faster block breaking | 0.05-0.2 |
| walk_speed | Faster movement | 0.05-0.15 |
| jump_height | Higher jumps | 0.3-1.0 |
| attack_speed | Faster attack cooldown | 0.1-0.3 |
| damage_reduction | Less damage taken | 0.05-0.2 |
| regeneration | Passive health recovery | 0.2-0.5 |
| saturation_boost | Food fills faster | 0.1-0.3 |
| knockback_resistance | Less knockback | 0.2-0.5 |

---

## FILE 3: buff_combinations.json (Buff Combo Rules)

**Purpose**: Define which buff combinations trigger bonus effects

**File Structure**
```json
{
  "warrior_mode": {
    "requires": ["mining_speed", "walk_speed"],
    "buffId": "warrior_boost",
    "durationSeconds": 15,
    "magnitude": 1.5
  },
  "acrobat_mode": {
    "requires": ["jump_height", "walk_speed"],
    "buffId": "acrobat_boost",
    "durationSeconds": 15,
    "magnitude": 1.2
  }
}
```

### Combination Entry Structure

**Key**: Combination identifier (string)
- Used internally, not shown to players
- Should be lowercase, no spaces
- Examples: "warrior_mode", "speed_demon"

**Value**: Combination data with:

**requires** (array of strings)
- Which buffs must be active simultaneously
- Order doesn't matter
- All buffs must be present to trigger combo
- Examples: ["mining_speed", "walk_speed"]

**buffId** (string)
- What buff is granted when combo triggers
- Must be valid buff ID
- Usually stronger version of individual buffs
- Example: "warrior_boost" (stronger than mining_speed alone)

**durationSeconds** (integer, range: 5-60)
- How long combination buff lasts
- Usually shorter than individual buffs (15-20 seconds)
- Does NOT depend on source buff durations

**magnitude** (decimal, range: 0.5-3.0)
- Strength of combination effect
- Usually 1.2x-1.5x stronger than individuals
- Example: individual 0.1 + 0.05 = 0.15, combo 1.5x stronger

### Default Combinations (Pre-configured)

```
Warrior Mode:
  Requires: mining_speed + walk_speed
  Grants: warrior_boost (1.5x multiplier, 15 sec)
  
Acrobat Mode:
  Requires: jump_height + walk_speed
  Grants: acrobat_boost (1.2x multiplier, 15 sec)
  
Guardian Mode:
  Requires: regeneration + damage_reduction
  Grants: guardian_boost (1.4x multiplier, 20 sec)
  
Scholar Mode:
  Requires: saturation_boost + regeneration
  Grants: scholar_boost (1.3x multiplier, 15 sec)
```

### Creating New Combinations

**Step 1: Identify Source Buffs**
- Decide which 2+ buffs should combine
- Example: attack_speed + walk_speed

**Step 2: Name the Combination**
- Choose a name for the combo
- Example: "berserker_mode"

**Step 3: Define Bonus Buff**
- Create a new buff effect
- Make it stronger than individuals
- Example: "berserker_boost" (2.0x multiplier)

**Step 4: Add to JSON**
```json
{
  "berserker_mode": {
    "requires": ["attack_speed", "walk_speed"],
    "buffId": "berserker_boost",
    "durationSeconds": 12,
    "magnitude": 2.0
  }
}
```

**Step 5: Test**
- Eat foods that give attack_speed and walk_speed together
- Should see combo notification
- Buff HUD shows new "berserker_boost" buff

### Combination Balancing

**Guidelines for Magnitude**
- Combination should be noticeably stronger (1.2x-1.5x base)
- Not overpowered (nothing >2.0x)
- Roughly balanced with other combos
- Example:
  - attack_speed (0.15) + walk_speed (0.1) normally = 0.25 total
  - Combo boost (2.0) = 0.5 total effectiveness (2x stronger)

**Duration Considerations**
- Shorter duration than source buffs (12-20 seconds)
- Rewards players for eating foods strategically
- Not too short (still enjoyable)
- Not too long (not overpowered)

### Advanced: 3+ Buff Combinations

**Possible but Not Implemented by Default**
- Would require 3+ source buffs
- Example:
```json
{
  "godmode": {
    "requires": ["mining_speed", "walk_speed", "jump_height"],
    "buffId": "godmode_boost",
    "durationSeconds": 10,
    "magnitude": 3.0
  }
}
```
- Harder to achieve (must eat 3 specific foods)
- More powerful reward
- Can be added manually to buff_combinations.json

---

## RELOAD COMMAND

**Config Reload (Server)**
```
/advfood reload
```
- Reloads all JSON config files
- No server restart required
- Takes effect immediately
- Already-applied buffs unaffected
- New foods/combos available after reload

---

## VALIDATION & ERROR HANDLING

**Invalid Configuration**
- If JSON malformed: Uses defaults, logs error
- If invalid buff ID: Ignored, logs warning
- If food registry name invalid: Ignored, logs warning
- If duration out of range: Clamped to valid range
- If magnitude out of range: Clamped to valid range

**Example Error Messages**
```
[ERROR] Invalid JSON in food_buffs.json: Unexpected character at line 5
[WARNING] Unknown buff ID "invalid_buff" in food_buffs.json - ignoring
[WARNING] Duration 500 seconds exceeds max 300 - clamping to 300
```

**Invalid Values**
- Missing required fields: Entry skipped
- Wrong data types: Entry skipped or converted
- Out of range values: Clamped to min/max

---

## EXAMPLE CONFIGURATIONS

### Minimal Setup
```json
{
  "system": {
    "maxHearts": 7,
    "enableBuffHud": true
  },
  "hud": {
    "position": "bottom_left",
    "scale": 1.0
  },
  "food_buffs": {
    "minecraft:cooked_beef": {
      "buffs": ["mining_speed"],
      "durationSeconds": 60,
      "magnitude": 0.1
    }
  },
  "buff_combinations": {}
}
```

### No Combos (Only Individual Buffs)
```json
{
  "buff_combinations": {}
}
```

### Hardcore Mode (Limited Resources)
```json
{
  "system": {
    "maxHearts": 5,
    "maxHeartsWithFood": 8
  },
  "hud": {
    "scale": 1.5
  }
}
```

### PvP Server (Faster Buff Decay)
```json
{
  "food_buffs": {
    "minecraft:cooked_beef": {
      "buffs": ["attack_speed"],
      "durationSeconds": 30,
      "magnitude": 0.2
    }
  }
}
```

### Exploration Server (Enhanced Travel Buffs)
```json
{
  "food_buffs": {
    "minecraft:carrot": {
      "buffs": ["walk_speed"],
      "durationSeconds": 120,
      "magnitude": 0.2
    },
    "minecraft:apple": {
      "buffs": ["jump_height"],
      "durationSeconds": 120,
      "magnitude": 1.0
    }
  }
}
```

---

## ADVANCED CUSTOMIZATION

### Disabling Specific Foods
- Remove entry from food_buffs.json
- Example: Remove apple buff:
```json
{
  "minecraft:apple": {
    "buffs": [],
    "durationSeconds": 0,
    "magnitude": 0
  }
}
```

### Nerfing/Buffing Specific Foods
- Adjust magnitude and duration
- Example: Make steak less powerful:
```json
{
  "minecraft:cooked_beef": {
    "buffs": ["mining_speed"],
    "durationSeconds": 30,
    "magnitude": 0.05
  }
}
```

### Multiple Buffs Per Food
- Add multiple buff IDs:
```json
{
  "minecraft:cooked_salmon": {
    "buffs": ["regeneration", "saturation_boost"],
    "durationSeconds": 60,
    "magnitude": 0.15
  }
}
```

### Disabling Combinations
- Remove from buff_combinations.json
- Or set empty combos:
```json
{
  "buff_combinations": {}
}
```

### Creating Custom Buff Effects
- Add new buff ID to food configuration
- Mod supports any buff type via custom effect handlers
- Examples: "custom_effect_1", "flying_speed", etc.

---

## SUMMARY

Configuration allows:
- ✓ Change max health values
- ✓ Add/remove foods and buffs
- ✓ Adjust buff duration and strength
- ✓ Create new combinations
- ✓ Customize HUD position/scale
- ✓ Toggle notifications/effects
- ✓ Limit maximum active buffs
- ✓ Customize every aspect without code changes

No code modifications needed to extend or customize the mod.
