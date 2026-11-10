# CottageTPAGUI

A small Paper plugin that adds a Cottage★ inventory GUI on top of the existing SimpleTPA plugin.

## Player experience

- `/tpamenu` opens the Cottage★ Teleport Menu.
- **Teleport to Player** opens a paginated list of currently online players and sends `/tpa <player>` when a head is selected.
- **Invite Player** opens the same kind of list and sends `/tpahere <player>` when a head is selected.
- The player list excludes the person using the menu, sorts names alphabetically, displays player heads, and supports refresh and pagination.
- A selected target is checked again before the request is sent. If they have left, the list refreshes instead.
- `/ctpa` and `/cottagetpa` are aliases for `/tpamenu`.

## Compatibility and design

- Built for the Paper 26.2 API and Java 25.
- Uses only Bukkit/Paper inventory APIs; it does not depend on SimpleTPA's internal classes.
- Runs the existing SimpleTPA command as the player who clicked the target. It does not dispatch as console or bypass permissions.
- Checks SimpleTPA's documented player permissions: `tpa.command.tpa` and `tpa.command.tpahere`.
- SimpleTPA remains responsible for cooldowns, request timeouts, incoming-request toggles, and the actual teleport request lifecycle.
- Standard inventory interactions are used, which are compatible with normal Geyser inventory interaction. Player-head skin display depends on the player's skin being available to the server/client.

## Build

Requires JDK 25 and Gradle. From this folder run:

```bash
gradle build
```

The plugin JAR will be created at `build/libs/CottageTPAGUI-1.0.0.jar`.

A GitHub Actions workflow is included to build the JAR with JDK 25 and upload it as an artifact.

## Install

1. Make sure SimpleTPA is installed and enabled.
2. Place `CottageTPAGUI-1.0.0.jar` in the server's `plugins/` folder.
3. Fully restart the Paper server.
4. In game, run `/tpamenu`.
5. To add it to FoPauseMenu, add an action like this to `plugins/FoPauseMenu/config.yml` (choose an unused action ID and order):

```yaml
  cottage-tpa:
    enabled: true
    type: player-command
    command: "/tpamenu"
    label: "Teleport Menu"
    tooltip: "{white}Open The Cottage★ teleport menu."
    width: 98
    order: 9
    permission: ''
```

No changes to the SimpleTPA config are required.

## Permissions

| Permission | Default | Purpose |
|---|---:|---|
| `cottagetpagui.use` | true | Open the GUI |
| `tpa.command.tpa` | Provided by SimpleTPA | Send a normal TPA request |
| `tpa.command.tpahere` | Provided by SimpleTPA | Send a TPA-here request |

The GUI checks SimpleTPA's permission before showing each action and checks again when a player is selected.
