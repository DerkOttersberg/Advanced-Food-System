# Advanced Food System - Big README

## Project Idea
Advanced Food System turns food into a long-term build system instead of a one-time hunger refill.
Players combine foods to build temporary archetypes (speed, mining, tank, fighter, XP) while balancing upside and downside.

Core design goals:
- Keep vanilla combat readable and server-authoritative.
- Make food choices meaningful in survival progression.
- Keep buffs small per item, stronger only when stacked through combos.
- Keep multiplayer sync reliable with low admin overhead.

Companion project:
- This mod consumes Seamless-API as a dependency for third-party food and combo registrations.
- API details are documented in ../Seamless-API/BIG_README.md.

## Feature Summary
- Slot-based food buff model with timed expiration.
- Per-food buff and debuff definitions from JSON config.
- Dynamic combo detection from active food sources.
- Capstone combo support and final-heart unlock policy.
- Health cap scaling from active foods and combo state.
- Server-authoritative damage pipeline with reduction and frailty penalties.
- In-game configuration screens for gameplay and effect strength tuning.
- HUD with active food slots, timers, bars, and combo context.
- Milk clears active buff state.
- Multiplayer-safe network sync for client HUD state.

## Gameplay Model
### 1) Food -> Buff Instances
When food is consumed, configured buff entries create BuffInstance records with:
- id (buff/debuff id)
- remaining ticks and total ticks
- magnitude
- health bonus hearts
- source key

### 2) Tick Lifecycle
Each server tick:
- Buff durations decrement.
- Expired entries are removed and events are posted.
- Active combo set is recomputed.
- Aggregated magnitudes are applied to attributes/effects.
- Buff state is synced to clients.

### 3) Health Rules
- Base max hearts and max-with-food are configurable.
- Food bonuses are source-based and merged per source.
- A selected capstone can unlock the final extra heart.
- Current balancing uses +1 heart per food source.

### 4) Damage Rules
Incoming damage is transformed with:
- damage_reduction (downward multiplier, capped)
- frailty (upward multiplier, capped)

So powerful builds can still be risky when frailty is present.

## Combo Catalog
All combo definitions live in gameplay/ComboEffectRegistry.java.

### Speed Path
- combo_speed_stride (pair): beetroot + potato
- combo_speed_current (pair): beetroot + salmon
- combo_speedster_raw (capstone): beetroot + salmon + potato
- combo_speedster_cooked (capstone): beetroot + cooked_salmon + baked_potato

### Mining Path
- combo_miner_focus (pair): dried_kelp + cookie
- combo_miner_study (pair): glow_berries + cookie
- combo_quarry_engine (capstone): dried_kelp + glow_berries + cookie

### Tank Path
- combo_guarded_plate (pair): cooked_porkchop + cooked_mutton
- combo_guarded_blessing (pair): golden_apple + cooked_mutton
- combo_bulwark_core (capstone): cooked_porkchop + cooked_mutton + golden_apple

### Fighting Path
- combo_duelist_line (pair): cooked_chicken + cooked_rabbit
- combo_duelist_heart (pair): apple + cooked_chicken
- combo_skirmisher (capstone): cooked_chicken + cooked_rabbit + apple

### XP Path
- combo_scholar_path (pair): cod + beetroot
- combo_scholar_path_cooked (pair): cooked_cod + beetroot
- combo_archivist (capstone): cod + cooked_cod + glow_berries

### Balanced Path
- combo_balanced_trail (pair): carrot + melon_slice
- combo_balanced_hearth (pair): bread + pumpkin_pie
- combo_balanced_tide (pair): tropical_fish + sweet_berries
- combo_balanced_harmony (capstone): apple + bread + carrot

Final-heart unlock is intentionally selective (not every capstone grants it).

## Code Architecture (Class Guide)

### Entry and Wiring
- AdvancedFoodSystemMod: mod bootstrap, event registration, config lifecycle, API merge hook.

### Config Layer
- config/AfsConfig: common gameplay config spec.
- config/AfsClientConfig: client/HUD config spec.
- config/ConfigManager: JSON load/create/sanitize, defaults, migration, API food merge, effect strengths.
- config/ModConfigData: runtime config model.
- config/FoodBuffEntry: per-food config schema.
- config/ComboEntry: legacy/static combo config schema placeholder.

### Data Layer
- data/BuffInstance: active buff record + NBT serialization.
- data/BuffStorage: per-player buff persistence and slot operations.
- data/BuffMath: aggregate helpers for buff totals.

### Gameplay Layer
- gameplay/BuffTicker: core tick loop, expiration, combo trigger, continuous effects, health scaling.
- gameplay/ComboEffectRegistry: built-in combo requirements/effects + API combo merge.
- gameplay/AttributeController: max-health and attribute modifier application.
- gameplay/BuffNames: display names/icons for HUD and tooltips.

### Event Layer
- events/CommonEvents: food consume pipeline, buff apply/remove hooks, tooltip injection, damage transform, milk clear.
- events/ClientEvents: client-side camera suppression for silent health-cap changes.

### Client UI Layer
- client/AfsConfigScreen: in-game config root screen.
- client/AfsEffectStrengthScreen: paged effect-strength tuning screen.
- client/BuffHudRenderer: slot HUD rendering, timers, combo icon, hover logic.
- client/ClientBuffState: client sync state cache and suppression timers.
- client/ComboTooltipData: tooltip DTO for combo display.

### Commands and Networking
- commands/AdvFoodCommand: admin/debug command surface.
- network/NetworkHandler: payload registration and sync utilities.
- network/BuffSyncPayload: server->client buff sync payload.

## Multiplayer and Persistence
- Buff state persists via player persistent data.
- Server is the single source of truth.
- Client HUD reflects synchronized payload data only.
- Death and milk clear paths post removal events and resync.

## Build and Run
From Advanced-Food-System root:
- Windows: gradlew.bat clean build

If developing with local API changes:
1) Build/publish Seamless-API first.
2) Build Advanced-Food-System against mavenLocal artifact.

## Extension Story
- Third-party mods register foods (and now combos) through Seamless-API.
- This mod merges API registrations after load-complete.
- Existing built-in combos are not overridden by external combos.

## Notes for Contributors
- Keep changes server-authoritative first, then sync to client.
- Prefer small buff numbers and explicit drawbacks for capstones.
- Add new buff ids to display naming/icon maps and effect strength defaults.
- Preserve compatibility with existing JSON configs where possible.
