package com.icbf.cannons.vs;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

/**
 * Utility class for Valkyrien Skies integration
 * Gracefully handles cases where VS is not installed
 */
public class ShipUtil {
    
    private static final boolean VS_LOADED = ModList.get().isLoaded("valkyrienskies");
    
    /**
     * Check if Valkyrien Skies mod is loaded
     * @return true if VS is loaded, false otherwise
     */
    public static boolean isVSLoaded() {
        return VS_LOADED;
    }
    
    /**
     * Check if a player is currently mounted on a ship
     * @param player The player to check
     * @return true if the player is on a ship, false otherwise (or if VS not loaded)
     */
    public static boolean isPlayerOnShip(Player player) {
        if (player == null || !VS_LOADED) {
            return false;
        }
        
        try {
            // Check if player is directly mounted to a ship
            Ship mountedShip = VSGameUtilsKt.getShipMountedTo(player);
            if (mountedShip != null) {
                return true;
            }
            
            // Check if player's position is within a ship's bounding box
            Ship shipAtPosition = VSGameUtilsKt.getShipManagingPos(player.level(), player.position());
            return shipAtPosition != null;
        } catch (Throwable e) {
            // If VS classes fail to load, return false
            return false;
        }
    }
    
    /**
     * Get the ship that the entity is currently on
     * @param entity The entity to check
     * @return The ship the entity is on, or null if not on a ship (or if VS not loaded)
     */
    public static Ship getShipEntityIsOn(Entity entity) {
        if (entity == null || !VS_LOADED) {
            return null;
        }
        
        try {
            // First check if entity is mounted to a ship
            Ship mountedShip = VSGameUtilsKt.getShipMountedTo(entity);
            if (mountedShip != null) {
                return mountedShip;
            }
            
            // Then check if entity's position is within a ship
            return VSGameUtilsKt.getShipManagingPos(entity.level(), entity.position());
        } catch (Throwable e) {
            // If VS classes fail to load, return null
            return null;
        }
    }
}
