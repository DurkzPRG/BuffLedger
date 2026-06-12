# BuffLedger

HUD overlay for active status effects on Hytale. Vanilla food, potions, and debuffs show up with remaining time. The base game icons do not expose timers in one place.

**Mod id:** `durkz:BuffLedger`  
**Requires:** Hytale server `>=0.5.3`

## Commands

| Command | Description |
|---------|-------------|
| `/buffs on` | Re-enable auto-tracking (if you used `/buffs off`) |
| `/buffs off` | Stop tracking and hide the overlay |
| `/buffs status` | Tracking state, HUD visibility, effect count |
| `/buffs list` | Print active effects in chat |

## Install

1. Copy `BuffLedger-0.1.0.jar` into your world's `mods/` folder
2. Join the world. Tracking starts automatically (unless you opted out before)
3. Eat food or drink a potion. The HUD appears while timed effects are active

## Quick verify

1. Join a world. No HUD until you gain a timed effect
2. Eat food or drink a potion with a timed buff
3. Overlay should list effect name and countdown (`+Regen 1:23`, `-Poison 8s`)
4. When the last effect ends, the HUD should disappear

## Optional mod compatibility

BuffLedger reads the vanilla `EffectControllerComponent`. No hard dependency on other mods.

| Mod | Required? | What works | Known limitations |
|-----|-----------|------------|-------------------|
| **[MultipleHUD](https://www.curseforge.com/hytale/mods/multiplehud)** | No | Extra custom HUD slot when you run several HUD mods | Stub integration in 0.1.0; native `addCustomHud` is used if MultipleHUD is absent |
| **[Not Enough Potions](https://www.curseforge.com/hytale/mods/not-enough-potions)** (NEP) | No | Most timed potions and morphs appear with readable labels | **Morph potions:** `CleanseMorph` clears effects first; HUD timers can **stall or freeze** until the new effect registers. Some dragon morphs may fail to apply independently of BuffLedger |
| **[BuffStacks](https://www.curseforge.com/hytale/mods/buffstacks)** | No | Same effect pipeline; stack count when multiple slots share one effect ID | BuffStacks does not track morph effects; duplicate vanilla icons are from BuffStacks, not BuffLedger |
| **MoreFood / SNIP3 FoodPack** | No | Food buffs detected automatically; human-readable tier labels | Asset packs only; no extra integration code |

## Known issues (0.1.0)

- Morph potion transitions (NEP `CleanseMorph`) can pause HUD countdowns while effects are cleared and re-applied

## Build

```bat
.\gradlew.bat build
```

Output: `build/libs/BuffLedger-0.1.0.jar`

Deploy to local Hytale install:

```bat
.\gradlew.bat deployLocal
```

Copies the JAR to `%AppData%\Roaming\Hytale\UserData\Mods`.

## License

MIT. Copyright (c) 2026 DurkzPRG
