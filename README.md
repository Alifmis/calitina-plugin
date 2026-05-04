# Calitina Plugin

Minecraft Folia plugin for 1.21.11 that adds a single-use reinforced pickaxe for breaking reinforced deepslate.

## Features

- Adds a custom item named "Reinforced Pickaxe" (`reinforced_pickaxe`).
- Uses a netherite pickaxe model.
- Breaks reinforced deepslate after 1 second and drops a reinforced deepslate item.
- Single-use (consumed after one successful break).
- Enchantments are blocked on this item.
- Creative category set to "Tools and Utilities".

## Usage

- Obtain the item in creative mode (Tools and Utilities tab) or via admin tools that can create items with custom NBT.
- Left-click reinforced deepslate with the reinforced pickaxe to start the 1-second break.

## Build

```powershell
./gradlew build
```

The built jar is located at `build/libs/`.

