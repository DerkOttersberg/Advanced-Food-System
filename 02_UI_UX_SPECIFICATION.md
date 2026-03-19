# BUFF HUD UI/UX DESIGN SPECIFICATION

## OVERVIEW

The player-facing UI consists of a single HUD element: a 3-slot buff display at the bottom-left of the screen. This shows what temporary buffs are currently active, how long they last, and their progress.

---

## BUFF DISPLAY HUD - DETAILED SPECIFICATIONS

### Position & Layout

**Screen Position**
- Corner: Bottom-left
- X offset: 10 pixels from left edge
- Y offset: 70 pixels from bottom edge
- Rendered on top of most UI elements
- Z-layer: Above gameplay HUD but below tooltips/menus

**Physical Dimensions**
- Each buff slot: 150px wide × 20px tall
- Slot spacing: 5 pixels vertical gap between slots
- Total width: 150 pixels
- Total height with 3 slots: 70 pixels (20+5+20+5+20)
- No horizontal scrolling/wrapping

**Scaling**
- Default scale: 1.0x (normal size)
- Configurable range: 0.5x to 2.0x
- Scales from center point
- Applies to entire HUD element

### Visual Appearance

**Buff Slot Structure**
Each visible buff appears in a slot with:

```
┌──────────────────────────────────────────┐
│ ICON  NAME                    TIME        │
│ ════════════════════════════════════      │  ← Progress Bar
└──────────────────────────────────────────┘
```

**Background**
- Color: #333333 (dark gray)
- Opacity: 100% (fully opaque)
- Style: Solid rectangle fill
- Border: 1px gray line (#666666)

**Icon/Emoji (Left Side)**
- Position: 5px from left edge, centered vertically
- Size: 12-14 pixels
- Examples:
  - "⛏️" for mining_speed
  - "🚶" for walk_speed
  - "⬆️" for jump_height
  - "⚔️" for attack_speed
  - "🛡️" for damage_reduction
  - "❤️" for regeneration
  - "🍖" for saturation_boost
  - "🪨" for knockback_resistance

**Buff Name Text**
- Font: Minecraft default font
- Size: 10 pixels height
- Color: #FFFFFF (white)
- Position: 20px from left (after icon), 6px from top
- Format: "Icon + Space + Name"
- Examples: "⛏️ Mining Speed", "🚶 Walk Speed"

**Timer Text (Right Side)**
- Font: Minecraft default font
- Size: 10 pixels height
- Color: #00FF00 (bright green)
- Position: 5px from right edge, 6px from top
- Format: "MM:SS" (minutes:seconds)
- Examples: "20:00", "01:23", "00:05"
- Right-aligned
- Always shows 5 characters (zero-padded)

**Progress Bar**
- Position: 5px from left, 17px from top (bottom of slot)
- Width: 140 pixels (150 - 5 - 5 left/right margin)
- Height: 2 pixels
- Color: #00FF00 (bright green)
- Background: #444444 (filled area shows time remaining)
- Fills from left to right
- Bar width = (timeRemaining / totalDuration) × 140

**Example Rendered Slot**
```
┌─────────────────────────────────────────┐
│ ⛏️ Mining Speed               [12:34]    │
│ ████████████░░░░░░░░░░░░░░░░░░░░░░░   │
└─────────────────────────────────────────┘
```

### Multi-Slot Layout

**When 1 Buff Active**
```
Position: Y = screen.height - 70

┌─────────────────────────────────────────┐
│ ⛏️ Mining Speed               [20:00]    │
│ ████████████░░░░░░░░░░░░░░░░░░░░░░░   │
└─────────────────────────────────────────┘
```

**When 2 Buffs Active**
```
┌─────────────────────────────────────────┐
│ ⛏️ Mining Speed               [20:00]    │
│ ████████████░░░░░░░░░░░░░░░░░░░░░░░   │
└─────────────────────────────────────────┘
    (5px gap)
┌─────────────────────────────────────────┐
│ 🚶 Walk Speed                 [15:32]    │
│ ███████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
└─────────────────────────────────────────┘
```

**When 3 Buffs Active (Maximum Display)**
```
┌─────────────────────────────────────────┐
│ ⛏️ Mining Speed               [20:00]    │
│ ████████████░░░░░░░░░░░░░░░░░░░░░░░   │
└─────────────────────────────────────────┘
    (5px gap)
┌─────────────────────────────────────────┐
│ 🚶 Walk Speed                 [15:32]    │
│ ███████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
└─────────────────────────────────────────┘
    (5px gap)
┌─────────────────────────────────────────┐
│ ⬆️ Jump Height                [10:15]    │
│ ██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │
└─────────────────────────────────────────┘
```

**When 4+ Buffs Active (Show Top 3)**
- Display only the 3 oldest active buffs
- Newest buffs are hidden (off-screen below)
- When oldest expires, next buff moves up into view
- No scrolling animation (instant update when buff expires)

**Slot Update Order**
- Top slot: Oldest active buff (will expire first)
- Middle slot: Second oldest buff
- Bottom slot: Newest visible buff
- When top buff expires, all buffs shift up
- New buff drops in at bottom when applied

### Timer Formatting

**Time Display Format: MM:SS**
- MM = minutes (00-99)
- SS = seconds (00-59)
- Examples:
  - 1200 ticks = 60 seconds = "01:00"
  - 600 ticks = 30 seconds = "00:30"
  - 1 tick = <1 second = "00:00"
  - 3600 ticks = 180 seconds = "03:00"
  - 72000 ticks = 3600 seconds = "60:00"

**Calculation**
```
totalSeconds = durationTicks / 20
minutes = totalSeconds / 60
seconds = totalSeconds % 60
display = String.format("%02d:%02d", minutes, seconds)
```

**Special Cases**
- Buff expires: Timer shows "00:00" for last tick, then disappears
- No rounding: Floor division (3.9 seconds still shows "00:03")

---

## NOTIFICATIONS & FEEDBACK

### Buff Applied Notification

**Action Bar Message**
- Displays above hotbar (Minecraft action bar)
- Position: Center of screen, above hotbar
- Format: "§6+" + buff name
- Example: "§6+Mining Speed"
- Color: #FFD700 (gold/yellow)
- Font: Minecraft default
- Duration: 2 seconds on screen (40 ticks)
- One message per buff applied

**Message Examples**
- "§6+Mining Speed"
- "§6+Walk Speed"
- "§6+Damage Reduction"
- "§6+Regeneration"

**When Message Shows**
- Player eats food with buff
- Buff added to their NBT
- Message displays immediately
- Multiple foods eaten quickly = multiple messages (stacking in action bar)

### Combination Activated Notification

**Optional Combination Bonus Notification**
- Action bar message when combo triggers
- Format: "§6✨ COMBO UNLOCKED: " + combo name
- Example: "§6✨ COMBO UNLOCKED: Warrior Mode"
- Color: #FFD700 (gold/yellow) with sparkle emoji
- Duration: 3 seconds on screen (60 ticks)
- Only shows once per combo activation

**Notification Requirements**
- Only shows if combination actually triggers (all requirements met)
- Shows even if player isn't looking at HUD
- Shows even if HUD is disabled
- Can be disabled via config

### Optional Buff Expiration Notification

**When Buff Expires**
- Optional action bar message
- Format: "§7-" + buff name (gray color)
- Example: "§7-Mining Speed"
- Duration: 1.5 seconds
- Can be disabled via config
- Shows for each buff that expires

---

## HUD CUSTOMIZATION & TOGGLES

### Visibility Toggle

**Keybind**
- Default: No keybind (feature optional)
- Configurable: Can assign any key
- Toggles HUD on/off without disabling mod
- Default state: On (HUD visible)

**Config Option**
- Setting: "hud.enabled" (true/false)
- When false: HUD never renders, features disabled
- Useful for players who don't want visual indicator

### Position Selection

**Available Positions**
1. bottom_left (default)
2. bottom_right
3. top_left
4. top_right

**Config Example**
```json
"hud": {
  "position": "bottom_left"
}
```

**Position Coordinates**
- bottom_left: X = 10, Y = screen.height - 70
- bottom_right: X = screen.width - 160, Y = screen.height - 70
- top_left: X = 10, Y = 10
- top_right: X = screen.width - 160, Y = 10

### Scale/Size Adjustment

**Scale Range**: 0.5x to 2.0x
- 0.5x = half size (tiny HUD)
- 1.0x = normal size (default)
- 1.5x = 50% larger
- 2.0x = double size (large HUD)

**Config Example**
```json
"hud": {
  "scale": 1.0
}
```

**Scaling Effect**
- Applies to entire HUD element (slots, text, icons, bars)
- Scales from position anchor point
- Affects readability and space usage

### Max Buffs Displayed

**Configurable Range**: 1-5 buffs shown
- Default: 3 buffs
- Can be increased to show 4-5 buffs (taller HUD)
- Can be decreased to show 1-2 buffs (compact HUD)

**Config Example**
```json
"hud": {
  "maxBuffsShown": 3
}
```

**Effect**
- If set to 2: Only show 2 most recent buffs
- If set to 5: Show 5 oldest buffs (if available)
- Doesn't affect buff storage (just changes display)

### Text Color Customization

**Optional: Custom Colors (Advanced)**
- If supported: Can customize text colors
- Config values in hex format (#RRGGBB)
- Example colors:
  - Text: #FFFFFF (white)
  - Timer: #00FF00 (green)
  - Background: #333333 (dark gray)

---

## RENDERING BEHAVIOR

### Update Frequency
- HUD updates every client frame (FPS-dependent)
- Timer updates every tick (20 updates per second)
- No unnecessary re-renders

### Z-Layer / Draw Order
- Rendered AFTER: Gameplay HUD, hotbar, health bar
- Rendered BEFORE: Tooltips, menus, debug info
- Positioned so it doesn't block important info

### Screen Size Adaptation
- Works on any screen resolution
- Position adjusts based on screen size
- Text size stays constant (doesn't scale with resolution)
- Scale setting applies on top of resolution

### Performance
- Rendering cost: <2 milliseconds per frame
- No impact if HUD disabled
- Simple rectangle + text rendering (very efficient)

### Compatibility
- Doesn't conflict with other HUDs
- Doesn't hide player crosshair
- Doesn't affect other mods' GUI elements
- Works with F1 (first-person view, renders behind HUD)
- Works with F5 (third-person view, renders in corner)

---

## PLAYER EXPERIENCE FLOW

### When Player Spawns
1. Player joins world
2. Max health set to 7 hearts
3. No buffs active
4. HUD shows (if enabled) but no slots
5. Hunger bar visible (vanilla)

### When Player Eats Food With Buff
1. Player clicks to eat food
2. Eating animation plays (vanilla)
3. Hunger restores (vanilla)
4. Saturation increases (vanilla)
5. Buff added to NBT
6. Action bar shows: "§6+Mining Speed"
7. HUD slot appears at bottom-left
8. Buff effects apply immediately
9. Timer counts down

### As Buff Remains Active
1. HUD slot remains visible
2. Timer counts down MM:SS
3. Progress bar empties as time passes
4. Effects apply every tick
5. If another buff added, new slot appears above/below
6. UI updates in real-time

### When Buff Expires
1. Timer reaches "00:00"
2. HUD slot disappears
3. Other buffs shift position (no gaps)
4. Effects stop applying
5. Optional message: "§7-Mining Speed"
6. Buff removed from NBT

### With Multiple Buffs + Combinations
1. Eat steak: mining_speed buff applies
2. Eat carrot: walk_speed buff applies
3. System detects both buffs active
4. Combination triggers: Warrior Mode buff added
5. Action bar: "§6✨ COMBO UNLOCKED: Warrior Mode"
6. HUD now shows 3 buffs: mining_speed, walk_speed, warrior_boost
7. Buffs expire independently (oldest first)
8. Combination effect applies for full duration

---

## EDGE CASES & VISUAL HANDLING

**What if buff timer becomes negative?**
- Remove buff immediately
- Don't show negative timer (would look broken)
- Update HUD instantly

**What if player gets same buff twice quickly?**
- Add as separate buff entry
- HUD shows same buff name twice (or with number suffix: "Mining Speed (2)")
- Timers count down independently
- One expires, other remains visible

**What if HUD goes off-screen?**
- Position logic prevents this
- Positions calculated to fit on-screen
- Scale adjusted if needed
- Warning in logs if position invalid

**What if screen resized?**
- Position recalculated every frame
- HUD repositions automatically
- Works with fullscreen and windowed mode

**What if player plays on ultrawide monitor?**
- HUD stays in corner (still visible)
- Horizontal positioning doesn't change
- Works fine on any aspect ratio

**What if player has vision issues?**
- Can increase scale to 2.0x (much larger)
- Can change text colors (if supported)
- Font size stays readable at all scales
- High contrast (white text on dark background)

---

## TECHNICAL RENDERING DETAILS

### What NOT to Render
- Don't show buff effects in vanilla UI (top-right potion effect area)
- Don't render buff icons in 3D world
- Don't overlay buff effects on blocks/entities
- Don't block player vision

### What TO Render
- 2D HUD elements only (flat UI, no 3D)
- Client-side only (server doesn't care)
- On top of gameplay screen
- Updated frequently for smooth timers

### Rendering Pipeline
1. Read active buffs from player NBT
2. Filter to show only top N buffs (configured)
3. Sort by age (oldest first)
4. Calculate positions on screen
5. Render each slot background
6. Render slot contents (icon, name, timer, bar)
7. Update next frame

---

## VISUAL STYLE GUIDELINES

**Design Aesthetic**
- Minimalist (not cluttered)
- Clean (simple shapes, no shadows)
- Functional (information-focused, not decorative)
- Consistent with Minecraft UI

**Color Scheme**
- Dark background (#333333) for contrast
- Bright text (#FFFFFF) for readability
- Green accents (#00FF00) for progress/active status
- Gray borders (#666666) for definition

**Typography**
- Minecraft default font throughout
- Consistent sizing
- No fancy fonts or effects
- Clear and legible at all scales

**Layout Principles**
- Organized vertically (stacked slots)
- Consistent spacing
- Clear visual hierarchy
- No unnecessary decoration

---

## SUMMARY

The UI is:
- **Simple**: Single HUD element, one job (show buffs)
- **Clear**: Easy to read timer and buff name
- **Non-intrusive**: Positioned in corner, doesn't block gameplay
- **Customizable**: Position, scale, visibility all configurable
- **Responsive**: Updates smoothly as buffs change
- **Accessible**: Large text, high contrast, scalable

The UX is:
- **Intuitive**: Players understand buffs instantly
- **Informative**: Shows exactly how long buff lasts
- **Responsive**: Immediate feedback when buff applied/expired
- **Non-blocking**: Doesn't require user interaction to dismiss
- **Discoverable**: Players can find it without tutorial
