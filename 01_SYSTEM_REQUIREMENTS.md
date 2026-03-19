# ADVANCED FOOD SYSTEM MOD - REQUIREMENTS

## CORE SYSTEM OVERVIEW

### What This Mod Does
- Reduces maximum player health from 20 HP (10 hearts) to 14 HP (7 hearts)
- Adds food-based buff system that grants temporary stat boosts
- Provides visual 3-slot buff display UI at bottom-left of screen
- Supports buff combinations that trigger from eating specific foods together
- All buff effects are custom (no vanilla potion effects)

### Design Philosophy
- Survival becomes critical: Players must eat to survive with reduced health
- Food is strategic: Different foods provide different temporary bonuses
- Multiple systems work together: Vanilla hunger + custom buffs = integrated food gameplay
- Easy to expand: JSON configuration allows unlimited foods and buffs without code changes

---

## SYSTEM COMPONENTS

### 1. HEALTH SYSTEM

**Base Health Reduction**
- Player maximum health: 14 HP (7 hearts) instead of vanilla 20 HP (10 hearts)
- Applies to all players, all game modes
- Applied when player joins server/world
- This is permanent base health, not affected by difficulty

**Food-Based Health Bonus**
- Full saturation (≥9.5) grants +8 HP bonus health
- Total max health with full saturation: 22 HP (11 hearts)
- Health bonus removed when saturation drops below 2
- Bonus scales proportionally with saturation level (not all-or-nothing)
- When saturation drops, excess health is removed (can't have more health than max)

**Interaction with Vanilla Systems**
- Hunger bar works normally (saturation works normally)
- Healing potions work as-is
- Health regeneration works as-is
- Damage from all sources works normally
- Absorption hearts (from other mods) stack on top

---

### 2. BUFF SYSTEM (CUSTOM EFFECTS - NO POTION EFFECTS)

**Buff Structure**
Each buff has:
- Unique buff ID (string identifier, e.g., "mining_speed")
- Duration in ticks (0-432000 range)
- Magnitude/Power value (0.0-10.0 float)
- Creation timestamp
- Source food name

**Buff Storage**
- Stored in player persistent NBT data (survives logout/login)
- Key name: "active_buffs"
- Stored as CompoundTag list
- Maximum 20 active buffs per player (excess are ignored)
- Each buff entry contains: id, duration (ticks), magnitude, source, created_timestamp

**Buff Duration System**
- Measured in ticks (20 ticks = 1 second)
- Decreases by 1 tick every server tick
- Buff expires when duration ≤ 0
- Duration range: 300-1800 ticks (15-90 seconds) per food item
- Display format: MM:SS (minutes:seconds)

**How Buffs Are Applied**
1. Player eats food
2. Vanilla hunger system processes normally (hunger restores, saturation +2)
3. Mod detects food type via item registry name
4. Looks up buff in food configuration
5. Adds buff to player NBT with duration and magnitude
6. Sends action bar notification to player
7. Buff immediately starts applying its effects

**How Buffs Expire**
1. Server ticks down buff duration every tick
2. When duration reaches 0, buff removed from list
3. UI stops showing buff
4. Player receives optional notification
5. Effects cease immediately

**Buff Stacking**
- Same buff type can stack (e.g., eat 2 steaks = 2 mining_speed buffs)
- Maximum 2 of same buff ID at once (additional eating doesn't add more)
- When checking buff strength, add magnitudes together (additive stacking)
- Older buff expires first (FIFO)

---

### 3. BUFF EFFECTS (CUSTOM SYSTEM - NO VANILLA POTION EFFECTS)

**What Are Custom Effects**
- NOT vanilla Minecraft potion effects
- Completely custom system managed by mod
- Player only sees effects if they have the mod installed
- Hidden from vanilla UI (no effect icons in top-right)
- Effects are applied via stat modifications, not potion effects

**How Custom Effects Work**
- Each buff type has a corresponding effect handler
- Effect handler modifies player stats/attributes directly
- Effects reapply every tick (1 second duration, refreshed)
- No visual potion effect icons - only HUD display shows active buffs

**Supported Buff Types**

| Buff ID | What It Does | How It Works | Magnitude |
|---------|-------------|-------------|-----------|
| mining_speed | Player breaks blocks faster | Increase block breaking speed attribute | 0.05-0.2 |
| walk_speed | Player moves faster | Increase movement speed attribute | 0.05-0.15 |
| jump_height | Player jumps higher | Increase jump boost attribute | 0.3-0.8 |
| attack_speed | Player attacks faster | Increase attack speed attribute | 0.1-0.3 |
| damage_reduction | Player takes less damage | Reduce incoming damage by % | 0.05-0.2 |
| regeneration | Player heals slowly | Restore health each tick | 0.2-0.5 |
| saturation_boost | Food fills faster | Increase saturation gain | 0.1-0.3 |
| knockback_resistance | Player knocked back less | Reduce knockback effect | 0.2-0.5 |

**Magnitude System**
- Magnitude is a decimal number 0.0-10.0
- Represents "strength" of buff
- 0.1 = 10% boost, 0.2 = 20% boost, etc.
- Examples:
  - mining_speed 0.1 = 10% faster mining
  - walk_speed 0.05 = 5% faster walking
  - damage_reduction 0.1 = 10% less damage taken

**Effect Application Logic**
- Buffs are applied every server tick
- Each tick: get all active buffs, for each buff, apply its effect
- Effects stack if same buff from multiple sources (add magnitudes)
- Effects immediately stop when buff expires (buff removed = effect removed)

---

### 4. FOOD SYSTEM

**How Foods Work**
- Any vanilla food or modded food can have a buff
- Identified by item registry name (e.g., "minecraft:cooked_beef")
- One food can grant multiple buffs simultaneously
- Different foods can grant same buff type (steak and cooked salmon both grant mining_speed)

**Supported Foods (Base)**
- Steak (cooked_beef)
- Cooked Chicken (cooked_chicken)
- Cooked Pork (cooked_porkchop)
- Salmon (cooked_salmon)
- Carrot
- Apple
- Bread
- And 20+ more vanilla foods

**Food Configuration**
Each food entry has:
- Food name (registry name)
- List of buff IDs it grants
- Duration in seconds (converted to ticks: seconds × 20)
- Magnitude for each buff

**Food Eating Flow**
```
Player eats → Vanilla hunger restores → Buff added → UI shows buff → Effect applies
```

---

### 5. BUFF COMBINATION SYSTEM

**What Are Combinations**
- When player has multiple specific buffs active at same time, trigger bonus buff
- Bonus buff is stronger than individual buffs
- Creates strategy: players discover which foods to combine for better results

**How Combinations Work**
1. Every 5 server ticks (4 times per second), scan player's active buffs
2. Check if player has all required buffs for a combination
3. If yes, apply combination buff
4. Combination buff lasts for set duration (usually 300 ticks / 15 seconds)
5. Only 1 combination triggers per check cycle

**Example Combinations**

| Combination | Requires | Gives | Duration | Magnitude |
|-------------|----------|-------|----------|-----------|
| Warrior Mode | mining_speed + walk_speed | warrior_boost | 15 sec | 1.5x |
| Acrobat Mode | jump_height + walk_speed | acrobat_boost | 15 sec | 1.2x |
| Guardian Mode | regeneration + damage_reduction | guardian_boost | 20 sec | 1.4x |
| Scholar Mode | saturation_boost + regeneration | scholar_boost | 15 sec | 1.3x |

**Combination Stacking**
- If player already has combination buff active, new combo doesn't trigger
- Only trigger new combinations after current one expires
- Prevents spam/overflow of combination buffs

**Discovering Combinations**
- No crafting recipe system
- Players discover by eating foods together
- System provides feedback: action bar message when combo triggers
- Optional wiki/guide for players

---

### 6. USER INTERFACE

**Buff Display HUD (3-Slot Bottom-Left)**

**Position**
- Bottom-left corner of screen
- X: 10 pixels from left edge
- Y: 70 pixels from bottom edge
- Can be toggled on/off via keybind or config

**Layout**
- Vertical stack of 3 slots
- Each slot: 150 pixels wide × 20 pixels tall
- 5 pixel spacing between slots
- Total height: ~70 pixels (3 slots + spacing)
- Renders above most UI elements but below tooltips

**Each Buff Slot Shows**
- **Buff Icon**: Small icon/emoji representing buff type
- **Buff Name**: Text name of buff (e.g., "⛏️ Mining Speed")
- **Time Remaining**: Timer in MM:SS format (e.g., "12:34")
- **Progress Bar**: Horizontal bar showing time remaining (filled = time left)

**Visual Design**
- **Background**: Dark gray box (0xFF333333)
- **Text Color**: White (0xFFFFFF)
- **Timer Color**: Green (0xFF00FF00)
- **Progress Bar Color**: Green (0xFF00FF00)
- **Border**: Light gray outline (0xFF666666)
- **Font**: Default Minecraft font

**Buff Display Order**
- Newest buffs appear at top
- Oldest buffs appear at bottom
- If >3 buffs active, show top 3 (oldest 3)
- When buff expires, update immediately

**Buff Slot Styling**
```
┌─────────────────────────────────────┐
│ ⛏️ Mining Speed        [12:34]       │
│ ████████████░░░░░░░░░░░░░░░░░░     │
└─────────────────────────────────────┘

Colors:
Background: #333333 (dark gray)
Text: #FFFFFF (white)
Timer: #00FF00 (bright green)
Bar: #00FF00 (bright green)
```

**HUD Customization (Optional)**
- Toggle HUD visibility via keybind
- Move HUD position (top-left, top-right, bottom-left, bottom-right)
- Scale HUD size (0.5x to 2.0x)
- Change number of buffs displayed (1-5)
- Change update frequency (optional)

**Player Notifications**

**Action Bar Message (When Buff Applied)**
- Text appears above hotbar (action bar)
- Format: "§6+" + buff name
- Example: "§6+Mining Speed"
- Color: Gold/yellow
- Duration: 2 seconds on screen

**Optional: Combination Activation Message**
- Format: "§6✨ COMBO UNLOCKED: " + combo name
- Example: "§6✨ COMBO UNLOCKED: Warrior Mode"
- Color: Gold/yellow with sparkle emoji
- Duration: 3 seconds on screen

**Optional Visual Effects (Particles)**
- Small particle effects when buff applied (can be disabled)
- Particles at feet of player
- Color matches buff type
- No performance impact if disabled

---

## DATA STORAGE

**NBT Storage Structure**
```
Player Persistent Data:
{
  active_buffs: [
    {
      id: "mining_speed",
      time: 1200,          (ticks remaining)
      mag: 0.1,            (magnitude/power)
      source: "steak",     (what food gave this buff)
      created: 1704067200  (timestamp)
    },
    {
      id: "walk_speed",
      time: 900,
      mag: 0.05,
      source: "carrot",
      created: 1704067201
    }
  ]
}
```

**Server-to-Client Sync**
- NBT automatically syncs from server to client
- Client reads NBT to display HUD
- Updates every tick (20 times per second)
- Minimal bandwidth usage

**Data Persistence**
- Stored in player.dat file (Minecraft standard)
- Survives server restarts
- Survives world reload
- Survives player logout/login
- Data validated on load (invalid entries removed)

---

## CONFIGURATION SYSTEM

**Configuration Files (JSON Format)**

**File 1: food_buffs.json**
Defines which foods give which buffs
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
  }
}
```

**File 2: buff_combinations.json**
Defines buff combinations and their effects
```json
{
  "warrior_mode": {
    "requires": ["mining_speed", "walk_speed"],
    "buffId": "warrior_boost",
    "durationSeconds": 15,
    "magnitude": 1.5
  }
}
```

**File 3: mod_config.json**
General mod settings
```json
{
  "system": {
    "maxHearts": 7,
    "maxHeartsWithFood": 11,
    "enableBuffHud": true,
    "enableParticles": true
  },
  "hud": {
    "position": "bottom_left",
    "scale": 1.0,
    "maxBuffsShown": 3
  }
}
```

**Configuration Behavior**
- Auto-generates default files if missing
- Loads on server startup
- Can be edited while server running (reload via command)
- Invalid entries logged as warnings, uses defaults
- Supports unlimited custom buffs/foods (add to JSON, no code changes)

---

## TECHNICAL REQUIREMENTS

**Game Version**
- Minecraft: 1.21.11 (requested target)
- NeoForge loader: 21.1.11

**Performance Targets**
- Server tick cost: <1 millisecond per player
- HUD render cost: <2 milliseconds per frame
- Memory per player: <1 kilobyte

**Compatibility**
- Works with vanilla Minecraft
- Works with other mods (doesn't override core systems)
- Works on servers (single and multiplayer)
- Works on clients (single player)

**NBT System**
- Uses standard Minecraft NBT (no custom binary formats)
- CompoundTag for player data
- ListTag for buff array
- Auto-saves with player data

**Event Hooks Needed**
- LivingEntityUseItemEvent (food eating)
- ServerTickEvent (buff updates)
- ClientRenderGuiEvent (HUD rendering)
- PlayerEvent.Clone (respawn handling)

---

## EXPANSION POSSIBILITIES

**Can Be Added Later Without Breaking Changes**

**Custom Buff Effects**
- Add new buff types to system (regeneration, knockback_resistance, etc.)
- Just add new case in effect handler
- Configure via JSON

**Advanced Combinations**
- 3-buff combinations (more rare, stronger bonuses)
- Time-limited combinations (only active at night, etc.)
- Conditional combinations (only in specific biomes)

**Buff UI Enhancements**
- Custom buff icons (textures instead of emojis)
- Buff tooltips (hover to see description)
- Buff sorting options
- Keybind to collapse/expand HUD

**Server Features**
- Admin command to give player buff
- Admin command to remove buff
- Buff whitelist/blacklist per world
- Per-player buff caps

**Mod Integrations**
- JEI/REI integration (show food buffs in tooltip)
- Config API support for other mods to register buffs
- Server-side synced food buff registry

---

## WHAT THIS MOD DOES NOT DO

- ✗ Does NOT use vanilla potion effects
- ✗ Does NOT change crafting recipes
- ✗ Does NOT add new items
- ✗ Does NOT add new blocks
- ✗ Does NOT change mob behavior
- ✗ Does NOT affect boss fights (except player health change)
- ✗ Does NOT require special items to eat
- ✗ Does NOT prevent vanilla food from working
- ✗ Does NOT show buff effects in vanilla UI
- ✗ Does NOT add GUI menus/screens

---

## EDGE CASES & SOLUTIONS

**What if player dies?**
- Player respawns with buffs cleared (like vanilla effects)
- Health reset to 7 hearts base
- Optional: Preserve buffs on respawn (configurable)

**What if saturation drops fast?**
- Health bonus removed smoothly (not abruptly)
- Current health doesn't exceed new max
- No damage to player

**What if player eats same food multiple times?**
- New buff instance added to list (stacks)
- Up to 2 max of same buff type (3rd eat doesn't add)

**What if buff expires while player in menu?**
- HUD doesn't update until screen closed
- Buff still expires server-side (just not visible until menu closed)
- Next screen open shows correct state

**What if server crashes mid-tick?**
- NBT saved to disk by Minecraft on close
- All buffs persist on server restart
- No data loss

**What if combination triggers multiple times?**
- Only 1 combination per check cycle (happens every 5 ticks)
- If combo already active, new trigger waits for expiry
- Prevents overflow

**What if player in creative mode?**
- Health modification doesn't apply
- Buffs still track in NBT (but don't affect anything)
- HUD still shows (optional to disable)

**What if player on multiplayer server?**
- Each player's buffs independent
- Buffs don't affect other players
- NBT syncs per-player
- No server lag from buff system

---

## SUMMARY

This mod creates a strategic food system by:
1. Reducing base health (making food critical)
2. Adding food-based temporary stat boosts
3. Providing clear visual feedback (3-slot HUD)
4. Supporting buff combinations (discovery/strategy)
5. Using completely custom buff effects (no vanilla potion effects)

The system is:
- Simple to understand (eat food → get buff → buff expires)
- Deep to master (discover food combinations)
- Easy to extend (JSON configuration)
- Performant (minimal server cost)
- Reliable (NBT persistence)
