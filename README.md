# Calitina Plugin

Minecraft Folia plugin for 1.21.11 that adds a few features to the server

## Features

- Adds a custom item named "Reinforced Pickaxe" (`reinforced_pickaxe`).
- Uses a netherite pickaxe model.
- Breaks reinforced deepslate after 1 second and drops a reinforced deepslate fragment item.
- Reinforced deepslate fragments thrown on an active beacon have a 50% chance per item to convert into a reinforced ingot.
- Single-use (consumed after one successful break).
- Enchantments are blocked on this item.

## Usage

- Use /reinforcedpickaxe to obtain the reinforced pickaxe.
- Left-click reinforced deepslate with the reinforced pickaxe to start the 1-second break.
- Breaking reinforced deepslate drops one reinforced deepslate fragment.
- Toss fragments onto an active beacon to attempt conversion into reinforced ingots.

## Build

```powershell
gradle build
```

The built jar is located at `build/libs/`.
