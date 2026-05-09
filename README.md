# Calitina Plugin

Minecraft Folia plugin for 1.21.11 that adds a few features to the server

## Features

- Adds a custom item named "Reinforced Pickaxe" (`reinforced_pickaxe`).
- Uses a netherite pickaxe model.
- Breaks reinforced deepslate after 1 second and drops a reinforced deepslate fragment item.
- Reinforced deepslate fragments thrown on an active beacon have a 50% chance per item to convert into a reinforced ingot.
- Combining any armor with a reinforced ingot in an anvil makes the armor invisible when equipped and removes its glint.
- Single-use (consumed after one successful break).
- Enchantments are blocked on this item.
- Using an ominous trial key on an ominous vault has a configurable chance to grant a reinforced pickaxe.

## Usage

- Use /reinforcedpickaxe to obtain the reinforced pickaxe.
- Left-click reinforced deepslate with the reinforced pickaxe to start the 1-second break.
- Breaking reinforced deepslate drops one reinforced deepslate fragment.
- Toss fragments onto an active beacon to attempt conversion into reinforced ingots.
- Combine any armor with a reinforced ingot in an anvil to make the armor invisible.
- Use an ominous trial key on an ominous vault for a chance to receive a reinforced pickaxe.

## Configuration

`config.yml`

```yaml
reinforced-pickaxe:
  ominous-vault-chance: 0.10
```

## Build

```powershell
gradle build
```

The built jar is located at `build/libs/`.
