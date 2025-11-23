# Valkyrien Skies Optional Compatibility

## Implementation: Option 2 - Soft Dependency with Reflection

This mod implements **optional** Valkyrien Skies compatibility using reflection, allowing it to work both with and without VS installed.

## How It Works

### Without Valkyrien Skies
- Mod works normally with vanilla Minecraft raycasting
- Compass targeting uses standard `Level.clip()` method
- Range: 200 blocks in all vehicles (boats, minecarts, etc.)

### With Valkyrien Skies
- Automatically detected at runtime via `ModList.get().isLoaded("valkyrienskies")`
- Uses VS's `clipIncludeShips()` method via reflection
- **Ship-aware raycasting**: Correctly raycasts from ships into the world
- Coordinates automatically transformed from ship-space to world-space
- **No dependency required** - works if VS is present or absent

## Technical Details

### Files Modified/Created

1. **VSCompatHelper.java** (`util` package)
   - Reflection-based wrapper for VS raycast
   - Static initialization checks for VS presence
   - Falls back to vanilla raycast if VS unavailable or incompatible
   - Zero compile-time dependencies on VS

2. **CompassTargetPacket.java**
   - Updated to use `VSCompatHelper.performRaycast()` instead of direct vanilla raycast
   - Single line change for full VS compatibility

3. **IcbfCannons.java**
   - Logs VS compatibility status on startup:
     - "Valkyrien Skies: Not installed"
     - "Valkyrien Skies: Active (ship-aware raycast enabled)"
     - "Valkyrien Skies: Installed but incompatible version (using vanilla raycast)"

4. **mods.toml**
   - Added optional dependency declaration:
     ```toml
     [[dependencies.icbf_cannons]]
         modId="valkyrienskies"
         mandatory=false
         versionRange="[2.0,)"
         ordering="AFTER"
         side="BOTH"
     ```

5. **build.gradle**
   - No VS compile-time dependencies (pure reflection approach)
   - Added VS Maven repository (commented for reference)

## Benefits

✅ **Works standalone** - No crashes if VS isn't installed  
✅ **Full compatibility** - Proper ship-to-world raycasting when VS present  
✅ **Zero dependencies** - No VS JARs needed to compile  
✅ **Version resilient** - Graceful degradation if VS API changes  
✅ **Performance** - Minimal reflection overhead (method cached on startup)

## Testing

### Without VS:
1. Install mod in modpack without Valkyrien Skies
2. Check logs for: `Compass Targeting: Valkyrien Skies: Not installed`
3. Compass targeting should work normally

### With VS:
1. Install mod with Valkyrien Skies 2.x
2. Check logs for: `Compass Targeting: Valkyrien Skies: Active (ship-aware raycast enabled)`
3. Build a ship with VS Eureka
4. Use compass targeting while on ship
5. Raycast should properly target world blocks (not limited to ship bounding box)

## Logging

On mod initialization, console will show:
```
[icbfcannons] Compass Targeting: Valkyrien Skies: Active (ship-aware raycast enabled)
```

Or if VS not installed:
```
[icbfcannons] Compass Targeting: Valkyrien Skies: Not installed
```

## Maintenance

If VS API changes in future versions:
- VSCompatHelper will catch `NoSuchMethodException`
- Logs warning: "Valkyrien Skies detected but clipIncludeShips method signature changed"
- Automatically falls back to vanilla raycast
- No crashes or errors

## Performance Impact

- **Startup**: One-time reflection lookup (~1ms)
- **Runtime**: Cached method reference - negligible overhead
- **Memory**: ~200 bytes for cached Method object

## Comparison to Alternatives

| Approach | Pros | Cons |
|----------|------|------|
| **Option 1: Required Dependency** | Clean API usage | Users must install VS |
| **Option 2: Reflection (Used)** | Works with/without VS | Small reflection overhead |
| **Option 3: Manual Transform** | No dependencies | Complex, error-prone |
| **Option 4: Mixins** | Most powerful | Fragile, breaks easily |

This implementation chose **Option 2** for the best balance of compatibility, maintainability, and user experience.
