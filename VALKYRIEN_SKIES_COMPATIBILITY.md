# Valkyrien Skies Compatibility - Compass Targeting System

## Overview
This branch adds **optional** Valkyrien Skies Eureka Ships compatibility. The compass-based targeting system only activates when players are on a Valkyrien Skies ship.

## Important Notes

⚠️ **Valkyrien Skies 2 is OPTIONAL** - The mod works with or without it installed!

- **With VS installed**: Compass targeting system works on ships
- **Without VS installed**: Mod works normally, compass targeting is automatically disabled
- Download Valkyrien Skies: https://www.curseforge.com/minecraft/mc-mods/valkyrien-skies
- All non-VS features remain fully functional regardless of VS installation

### Safety Features
- Automatic VS detection using `ModList.get().isLoaded("valkyrienskies")`
- Graceful fallback if VS classes fail to load
- No crashes or errors when VS is not present

## Features

### Compass Targeting
While mounted on a Valkyrien Skies ship, players can use a compass to target and attack distant locations:

1. **Hold a compass** in either hand
2. **Right-click and hold** to activate targeting mode
3. A **red beacon beam** appears at the raycast target location
4. The beacon updates in real-time as you move your crosshair
5. **Release right-click** to spawn a TNT explosion 1 block above the target

### Technical Details

#### Raycast Behavior
- Maximum range: **200 blocks**
- Blocks water: **Yes** (raycast stops at water)
- Blocks glass: **Yes** (raycast stops at solid blocks including glass)
- Uses `ClipContext.Block.COLLIDER` for block collision
- Uses `ClipContext.Fluid.ANY` for fluid collision

#### TNT Explosion
- Spawns **PrimedTnt** entity with 0 fuse (instant explosion)
- Position: **1 block above** the targeted block
- Triggered by: **Releasing right-click**

## Implementation Files

### New Files Created

1. **ShipUtil.java** (`src/main/java/com/icbf/cannons/vs/ShipUtil.java`)
   - Utility class for Valkyrien Skies ship detection
   - Methods:
     - `isPlayerOnShip(Player)` - Check if player is on a ship
     - `getShipEntityIsOn(Entity)` - Get the ship an entity is on

2. **CompassTargetPacket.java** (`src/main/java/com/icbf/cannons/network/CompassTargetPacket.java`)
   - Network packet for compass targeting
   - Handles raycast calculation on server
   - Triggers TNT explosion when released

3. **BeaconRenderPacket.java** (`src/main/java/com/icbf/cannons/network/BeaconRenderPacket.java`)
   - Network packet to show/hide beacon on client
   - Sent from server to client with target position

4. **BeaconRenderer.java** (`src/main/java/com/icbf/cannons/client/BeaconRenderer.java`)
   - Client-side renderer for red beacon beam
   - Uses vanilla beacon beam texture
   - Renders at target position with red color

5. **CompassHandler.java** (`src/main/java/com/icbf/cannons/client/CompassHandler.java`)
   - Client-side event handler for compass usage
   - Detects right-click press/hold/release
   - Sends packets to server

6. **Compilation Stubs** (`src/main/java/org/valkyrienskies/...`)
   - `Ship.java` - Interface stub for compilation
   - `VSGameUtilsKt.java` - Utility class stub for compilation
   - **Note**: These are replaced by actual VS implementations at runtime

### Modified Files

1. **build.gradle**
   - Added Valkyrien Skies Maven repository
   - Added VS2 and VSCore dependencies

2. **gradle.properties**
   - Added version variables for Valkyrien Skies

3. **IcbfCannons.java**
   - Registered new network packets (CompassTargetPacket, BeaconRenderPacket)

## Dependencies

- **Valkyrien Skies 2**: 2.5.0-beta.3
- **VSCore**: 2.5.0-beta.3
- **Minecraft**: 1.20.1
- **Forge**: 47.4.10

## Building

1. Sync Gradle to download Valkyrien Skies dependencies:
   ```powershell
   .\gradlew --refresh-dependencies
   ```

2. Build the mod:
   ```powershell
   .\gradlew build
   ```

## Installation & Testing

### For Players

1. Install Forge 1.20.1-47.4.10 or newer
2. **Install Valkyrien Skies 2** from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/valkyrien-skies) or [Modrinth](https://modrinth.com/mod/valkyrien-skies)
3. Install Eureka (or another VS ship addon)
4. Place the ICBF Cannons mod JAR in your mods folder
5. Launch Minecraft

### Testing the Features

To test the compass targeting system:

1. Build or board a Valkyrien Skies ship
2. Hold a compass in either hand
3. Right-click and hold while aiming at a distant location
4. Observe the red beacon beam appear at the target
5. Release right-click to trigger an explosion

### Without Valkyrien Skies

If Valkyrien Skies is not installed:
- The mod will still load and function normally
- Compass targeting will be disabled (requires being on a ship)
- All other cannon features work as expected

## Notes

- The compass targeting **only works when on a ship**
- The beacon auto-hides after 100ms if not updated
- The raycast respects collision with water and glass blocks
- TNT explosion uses vanilla explosion mechanics

## Future Enhancements

Potential improvements:
- Configurable explosion power
- Different projectile types
- Cooldown between shots
- Visual effects for targeting
- Sound effects
- Permission system for multiplayer
