# CottageTPAGUI

A Paper 26.2 plugin for The Cottage★ SMP. It adds a Java inventory GUI for SimpleTPA and native Bedrock forms for server options, tutorials, custom destinations, and player-to-player teleport requests.

## Player experience

### Java Edition

- `/tpamenu` opens the Cottage★ Teleport Menu.
- Teleport-to-player and invite-player actions show online player heads.
- The Java player picker excludes the person using the menu, sorts names alphabetically, and includes refresh and pagination.
- Add `/tpamenu` to FoPauseMenu to expose the GUI from the Java pause menu.

### Bedrock Edition

Requires Geyser and Floodgate to be installed and enabled.

- `/options` opens the native Bedrock Server Options form, matching the enabled player-facing actions in the Java FoPauseMenu configuration.
- `/serveroptions` is an alias for `/options`.
- A **Server Options★** compass is placed in the first empty hotbar slot when a Bedrock player joins. It is never placed over an existing item.
- Right-clicking the compass opens Server Options★.
- If the hotbar is full, players can still use `/options`.
- `/tpamenu` opens the Bedrock-native teleport menu.
- Bedrock teleport selection displays online player names as form buttons, with pagination, refresh, and a back action.
- Player Tutorials are ordered like the Java help actions: Teleport Help, Team Guide, Mob Guide, Villager Guide, Mining Guide, Happy Ghast Guide, Skin Guide, Dynamic Lights, Better Mending, and Easy Crafting.
- The Server Options form follows the same order as Java: travel and teleport actions first (Spawn, Wanderer's Way, Teleport Menu, Broken Holy Sword, Public Trading Hall, Jade Chamber), plugin shortcuts next (Team System, Team Ender Chest, Skin Support), then Player Tutorials, followed by community links.
- The three custom destinations use the same Overworld coordinates and rotations as the Java FoPauseMenu actions.
- Disabled or unavailable Java actions such as `/menu` and Store are not shown in the Bedrock menu.
- Discord and website links are shown in a form and sent to chat as clickable links. The Discord action intentionally uses the same website URL as the supplied Java configuration.

Bedrock forms use Floodgate's Cumulus API. The API is a compile-only dependency: this plugin does not bundle or replace Geyser, Floodgate, or Cumulus.

## Compatibility and design

- Built for Java 25 and Paper 26.2.
- Floodgate API is used to identify Bedrock players and send Cumulus forms.
- Floodgate is a soft dependency: Java inventory menus continue to work when Floodgate is not installed, but Bedrock forms and the compass are unavailable.
- The plugin executes existing SimpleTPA commands as the player who selected the target. It does not dispatch teleport requests as console or bypass SimpleTPA's permission checks.
- SimpleTPA remains responsible for cooldowns, request timeouts, incoming-request toggles, and request lifecycle.
- Team actions execute `/team gui` and `/team echest` as the player.
- The Bedrock spawn action teleports the player to the configured Cottage★ Overworld spawn coordinates: `-49.50 70.50 72.53`, yaw `-269.80`, pitch `-33.56`.
- The compass shortcut does not overwrite an item. If all nine hotbar slots are occupied, `/options` remains available.

## Build

Requires JDK 25 and Gradle. From this folder run:

```bash
gradle clean build
```

The plugin JAR is created at `build/libs/CottageTPAGUI-1.3.0.jar`.

A GitHub Actions workflow builds with JDK 25 and uploads the plugin JAR as an artifact.

## Install

1. Keep Paper 26.2 and Java 25 as the server runtime.
2. Make sure SimpleTPA is installed and enabled.
3. For Bedrock forms, make sure Geyser and Floodgate are installed and enabled. Use versions compatible with the Floodgate 2.2.5 API/Cumulus forms API used to compile this release.
4. Place `CottageTPAGUI-1.3.0.jar` in the server's `plugins/` folder.
5. Fully restart the Paper server; do not use `/reload`.
6. Test Java with `/tpamenu`.
7. Test Bedrock with `/options`, then check that the Server Options★ compass is added on join.

To add the Java teleport GUI to `plugins/FoPauseMenu/config.yml`, use an unused action ID and order:

```yaml
  cottage-tpa:
    enabled: true
    type: player-command
    command: "/tpamenu"
    label: "Teleport Menu"
    tooltip: "{white}Send a teleport request or invite someone to you."
    width: 98
    order: 4
    permission: ''
```

No changes to the SimpleTPA configuration are required.

## Permissions

| Permission | Default | Purpose |
|---|---:|---|
| `cottagetpagui.use` | true | Open the teleport GUI |
| `cottagetpagui.options` | true | Open Bedrock Server Options |
| `tpa.command.tpa` | Provided by SimpleTPA | Send a normal TPA request |
| `tpa.command.tpahere` | Provided by SimpleTPA | Send a TPA-here request |

The plugin checks SimpleTPA's permissions before presenting request actions and checks again when a player is selected.
