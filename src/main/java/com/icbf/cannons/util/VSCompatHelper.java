package com.icbf.cannons.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * Helper class for optional Valkyrien Skies compatibility
 * Uses reflection to call VS raycast when available, falls back to vanilla otherwise
 */
public class VSCompatHelper {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String VS_MOD_ID = "valkyrienskies";
    private static final boolean VS_LOADED;
    private static Method clipIncludeShipsMethod = null;
    private static Method getShipManagingMethod = null;
    private static Method getIdMethod = null;
    
    static {
        VS_LOADED = ModList.get().isLoaded(VS_MOD_ID);
        
        if (VS_LOADED) {
            try {
                // Try to load the VS raycast utility class
                Class<?> raycastUtilsClass = Class.forName("org.valkyrienskies.mod.common.world.RaycastUtilsKt");
                Class<?> shipIdClass = Class.forName("org.valkyrienskies.core.api.ships.properties.ShipId");
                Class<?> vsGameUtilsClass = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
                Class<?> shipClass = Class.forName("org.valkyrienskies.core.api.ships.Ship");
                
                // Find the clipIncludeShips method
                // public static BlockHitResult clipIncludeShips(Level level, ClipContext context, ...)
                clipIncludeShipsMethod = raycastUtilsClass.getMethod(
                    "clipIncludeShips",
                    net.minecraft.world.level.Level.class,
                    ClipContext.class,
                    boolean.class,  // shouldTransformHitPos
                    shipIdClass,    // skipShip (nullable)
                    boolean.class   // skipWorld
                );
                
                // Find the getShipManagingPos method to detect what ship player is on
                // public static Ship getShipManagingPos(Level level, Vec3 pos)
                getShipManagingMethod = vsGameUtilsClass.getMethod(
                    "getShipManagingPos",
                    net.minecraft.world.level.Level.class,
                    net.minecraft.world.phys.Vec3.class
                );
                
                // Find the getId method from Ship interface
                getIdMethod = shipClass.getMethod("getId");
                
                LOGGER.info("Valkyrien Skies detected! Ship-aware raycasting enabled.");
            } catch (ClassNotFoundException e) {
                LOGGER.warn("Valkyrien Skies mod detected but RaycastUtils class not found. Using vanilla raycast.");
            } catch (NoSuchMethodException e) {
                LOGGER.warn("Valkyrien Skies detected but clipIncludeShips method signature changed. Using vanilla raycast.");
            } catch (Exception e) {
                LOGGER.error("Error initializing Valkyrien Skies compatibility: " + e.getMessage());
            }
        }
    }
    
    /**
     * Performs a raycast that works correctly both in world and on VS ships
     * Skips the ship the player is currently on to prevent hitting own ship
     * @param player The player performing the raycast
     * @param context The clip context for the raycast
     * @return BlockHitResult with proper world coordinates
     */
    public static BlockHitResult performRaycast(ServerPlayer player, ClipContext context) {
        if (VS_LOADED && clipIncludeShipsMethod != null) {
            try {
                // Get the ship the player is standing on (if any)
                Object playerShipId = null;
                if (getShipManagingMethod != null && getIdMethod != null) {
                    Object playerShip = getShipManagingMethod.invoke(null, player.level(), player.position());
                    if (playerShip != null) {
                        playerShipId = getIdMethod.invoke(playerShip);
                    }
                }
                
                // Call VS raycast: clipIncludeShips(level, context, shouldTransformHitPos=true, skipShip=playerShipId, skipWorld=false)
                Object result = clipIncludeShipsMethod.invoke(
                    null,  // static method
                    player.level(),
                    context,
                    true,        // shouldTransformHitPos - transform hit position to world space
                    playerShipId, // skipShip - skip the ship player is on (null if not on a ship)
                    false        // skipWorld - include world in raycast
                );
                
                if (result instanceof BlockHitResult) {
                    return (BlockHitResult) result;
                }
            } catch (Exception e) {
                LOGGER.error("Error calling Valkyrien Skies raycast, falling back to vanilla: " + e.getMessage());
            }
        }
        
        // Fallback to vanilla raycast
        return player.level().clip(context);
    }
    
    /**
     * Check if Valkyrien Skies is loaded and functional
     * @return true if VS raycast is available
     */
    public static boolean isVSActive() {
        return VS_LOADED && clipIncludeShipsMethod != null;
    }
    
    /**
     * Get debug info about VS compatibility status
     * @return Status string for logging
     */
    public static String getCompatStatus() {
        if (!VS_LOADED) {
            return "Valkyrien Skies: Not installed";
        } else if (clipIncludeShipsMethod != null) {
            return "Valkyrien Skies: Active (ship-aware raycast enabled)";
        } else {
            return "Valkyrien Skies: Installed but incompatible version (using vanilla raycast)";
        }
    }
}
