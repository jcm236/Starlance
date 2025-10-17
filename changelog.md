### Additions:
- Magnet boots can now be toggle with a keybind (defaults to `X`)
- Magnet boots work in the curios's boots slot
- Added `ship_landing_mode` config option:
    - `PLAYER_MENU`: Will always use the player menu for landing location. Ships cannot land without a player nearby. (Behaviour from previous versions)
    - `HISTORY`: The saved launch position (for that planet) will be used. If no launch has been saved, the origin will be used.
    - `AUTO_HISTORY`: Ships will use `PLAYER_MENU` mode if a player is nearby, otherwise they will use `HISTORY` mode.
- Added `ship_landing_accuracy` config option. When a ship is landing on a planet, the ship will be teleported randomly within the stated range in chunks.
- Added `ship_first_landing_spawn_range` config option. Defines how far away the ship should spawn from the origin when first landing on a planet.
- Added LOD warning when the `lodDetail` Valkyrien Skies setting is too low.
- Added `supressess_press_y_hint` config option to suppress the "Press Y To Open GUI" Cosmic Horizons message.

### Changes:
- Starlance now only supports forge 47.4.0 and above

### Fixes:
- Fixed magnet boots not working at block edge
- Fixed the render optimisation mixins using the server config instead of client one.
- Fixed ships having the incorrect orientation when entering planet from the sides.

### API:
- Added `PreTravelEvent` to adjust new position and new orientation when a ship cluster is teleporting.
- Added `PreShipTravelEvent` to adjust ship's velocity and omega when teleporting.