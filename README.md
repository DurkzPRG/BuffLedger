# BuffLedger

HUD overlay for active status effects on Hytale. Vanilla food, potions, and debuffs show up with remaining time — the base game icons do not expose timers in one place.

**Mod id:** `durkz:BuffLedger`  
**Requires:** Hytale server `>=0.5.3`

## Commands

| Command | Description |
|---------|-------------|
| `/buffs on` | Show the overlay |
| `/buffs off` | Hide it |
| `/buffs status` | Current toggle state |

## Install

1. Copy `BuffLedger-0.1.0.jar` into your world's `mods/` folder
2. Join the world and run `/buffs on`
3. Apply a food buff or potion — lines should update every second

Optional: [MultipleHUD](https://www.curseforge.com/hytale/mods/multiplehud) if you run several HUD mods (LeanCore, TaleStatistics, etc.). Without it, BuffLedger uses the native `addCustomHud` slot.

## Quick verify

1. `/buffs on` — top-right overlay appears (may read `No active effects`)
2. Eat food or drink a potion with a timed buff
3. Overlay should list effect name and countdown (`+Regen 1:23`, `-Poison 8s`)

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
