# Features
- Added `THRUSTER_FLAME_IMPACT` config option to disable thrusters setting fire and pushing entities 

# Bug fixes
- Fixed thruster particles getting stuck active when ship goes out of loading distance and comes back
- Fixed a crash from thrusters if the game was paused (in single player) or a tick took too long on server
- Fixed thruster flame having a 0.5 block offset in some directions
- Fixed thruster data sometimes not saving
- Fixed minecart contraptions and CBC cannons crashing the game when entering space or a planet
- Fixed Ship gravity sometimes being removed by other mods, leading to normal gravity in space

# Tweaks
- Increased planet teleport collision range
- Ships will now freeze when touching a planet to avoid passing through them
- Ships will now have correct orientation when entering from different faces of a planet