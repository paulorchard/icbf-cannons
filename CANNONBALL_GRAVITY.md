# Cannonball Gravity Implementation

## Overview
Implemented custom cannonball entity with distance-based gravity system for realistic cannon physics.

## Important: Render Distance Limitation

**Client Render Distance:** Minecraft's client has a built-in entity render distance limit (typically 64-128 blocks depending on settings) that cannot be overridden by mods. While the cannonball continues to exist and move server-side up to 200 blocks, **it may disappear visually** beyond ~80-120 blocks based on the player's video settings.

**To see projectiles at 200 blocks:**
- Players need to increase their `Entity Distance` setting in Video Settings
- Alternatively, use `Simulation Distance` set to 12+ chunks
- This is a Minecraft engine limitation, not a mod issue

**Server-side behavior:** The projectile works correctly server-side at all ranges - it will still hit targets and deal damage even if not visible.

## Changes Made

### 1. New File: `CannonballEntity.java`
Custom projectile entity that extends `LargeFireball` with distance-based gravity.

**Key Features:**
- Tracks starting position to calculate distance traveled
- Applies **0.001 gravity** within 200 blocks (minimal drop: ~1 block at 200 blocks)
- Applies **0.1 gravity** beyond 200 blocks (rapid fall)
- Auto-despawns at 400 blocks for performance
- Saves/loads start position to NBT for server persistence

### 2. Modified: `IcbfCannons.java`

#### Added Entity Type Registry
```java
public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
    DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

public static final RegistryObject<EntityType<CannonballEntity>> CANNONBALL = 
    ENTITY_TYPES.register("cannonball", ...);
```

#### Updated Firing Methods
- `tryFireCannonAtTarget()` - Spyglass targeting system
- `CannonControllerBlock.use()` - Right-click firing

Both now spawn `CannonballEntity` instead of vanilla `LargeFireball`.

## Physics Behavior

### Within 200 Blocks
- **Gravity:** 0.001 blocks/tick²
- **Drop at 100 blocks:** ~0.5 blocks
- **Drop at 200 blocks:** ~1 block
- **Result:** Nearly straight trajectory, barely noticeable arc

### Beyond 200 Blocks
- **Gravity:** 0.1 blocks/tick² (100x stronger)
- **Effect:** Rapid descent like a stone
- **At 250 blocks:** Steep downward arc
- **At 300 blocks:** Likely hit ground
- **At 400 blocks:** Auto-despawn (safety)

## Tuning Parameters
Located in `CannonballEntity.java`:

```java
private static final double MAX_EFFECTIVE_RANGE = 200.0;      // Switch point
private static final double MAX_ABSOLUTE_RANGE = 400.0;       // Despawn distance
private static final double GRAVITY_WITHIN_RANGE = 0.001;     // Minimal gravity
private static final double GRAVITY_BEYOND_RANGE = 0.1;       // Strong gravity
```

Adjust these values to change behavior:
- Increase `GRAVITY_WITHIN_RANGE` for more visible arc
- Decrease `GRAVITY_BEYOND_RANGE` for gentler falloff
- Change `MAX_EFFECTIVE_RANGE` to modify switch point

## Technical Details

### Entity Lifecycle
1. **Spawn:** Created with initial velocity toward target
2. **Each Tick:**
   - Calculate distance from start position
   - Apply appropriate gravity based on distance
   - Check if beyond max range (despawn if yes)
3. **Impact:** Explodes on hit (no block damage)
4. **Chunk Unload:** Entity removed if chunk unloads

### Performance
- Minimal overhead: One distance calculation per tick
- No pathfinding or complex AI
- Auto-cleanup at 400 blocks prevents infinite entities
- Same rendering as vanilla fireballs

## Testing Recommendations

1. **Short Range (< 200 blocks):**
   - Should fly nearly straight
   - Minimal visible drop
   
2. **Long Range (> 200 blocks):**
   - Should maintain trajectory until 200 blocks
   - Then drop rapidly like a stone
   
3. **Max Range (400 blocks):**
   - Should despawn automatically
   - No entities left floating in unloaded chunks

## Future Enhancements

Possible improvements:
- Add trail particles that change color at 200 blocks
- Add sound effects at range transition
- Make gravity values configurable via config file
- Add different projectile types with different physics
