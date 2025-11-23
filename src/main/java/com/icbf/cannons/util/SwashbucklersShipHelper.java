package com.icbf.cannons.util;

import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for detecting and interacting with Swashbucklers mod ships.
 * Uses reflection to avoid compile-time dependency on Swashbucklers.
 */
public class SwashbucklersShipHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwashbucklersShipHelper.class);
    private static final String SWASHBUCKLERS_PACKAGE = "hal.studios.hpm.entity";
    
    // Ship type enum for easy identification
    public enum ShipType {
        RAFT(0, "RaftEntity"),
        SWASHBUCKLER(1, "SwashbucklerEntity"),
        SWASHBUCKLER_UPGRADED(1, "SwashbucklerupgradedEntity"),
        CUTTER(2, "CutterEntity"),
        CUTTER_MILITARISED(2, "CuttermilitarisedEntity"),
        CUTTER_PIRATE(2, "CutterPirateEntity"),
        CORVETTE_STEAMSHIP(3, "CorvetteSteamshipEntity");
        
        private final int cannonsPerSide;
        private final String className;
        
        ShipType(int cannonsPerSide, String className) {
            this.cannonsPerSide = cannonsPerSide;
            this.className = className;
        }
        
        public int getCannonsPerSide() {
            return cannonsPerSide;
        }
        
        public String getClassName() {
            return className;
        }
    }
    
    // Cached ship entity classes
    private static final Map<ShipType, Class<?>> shipClasses = new HashMap<>();
    private static boolean initialized = false;
    private static boolean swashbucklersAvailable = false;
    
    /**
     * Initialize reflection - called during mod setup
     */
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        
        LOGGER.info("Initializing Swashbucklers ship detection...");
        
        // Try to load each ship class
        int loaded = 0;
        for (ShipType type : ShipType.values()) {
            try {
                String fullClassName = SWASHBUCKLERS_PACKAGE + "." + type.getClassName();
                Class<?> shipClass = Class.forName(fullClassName);
                shipClasses.put(type, shipClass);
                loaded++;
                LOGGER.debug("Loaded ship class: {}", type.getClassName());
            } catch (ClassNotFoundException e) {
                LOGGER.debug("Ship class not found: {} (Swashbucklers may not be installed)", type.getClassName());
            }
        }
        
        swashbucklersAvailable = loaded > 0;
        
        if (swashbucklersAvailable) {
            LOGGER.info("Swashbucklers compatibility: ENABLED ({} ship types detected)", loaded);
        } else {
            LOGGER.info("Swashbucklers compatibility: DISABLED (mod not found)");
        }
    }
    
    /**
     * Check if Swashbucklers mod is available
     */
    public static boolean isSwashbucklersAvailable() {
        if (!initialized) initialize();
        return swashbucklersAvailable;
    }
    
    /**
     * Check if an entity is a Swashbucklers ship
     */
    public static boolean isSwashbucklersShip(Entity entity) {
        if (!isSwashbucklersAvailable() || entity == null) {
            return false;
        }
        
        Class<?> entityClass = entity.getClass();
        for (Class<?> shipClass : shipClasses.values()) {
            if (shipClass.isAssignableFrom(entityClass)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the ship type of an entity, or null if not a ship
     */
    public static ShipType getShipType(Entity entity) {
        if (!isSwashbucklersAvailable() || entity == null) {
            return null;
        }
        
        Class<?> entityClass = entity.getClass();
        for (Map.Entry<ShipType, Class<?>> entry : shipClasses.entrySet()) {
            if (entry.getValue().isAssignableFrom(entityClass)) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * Get the number of cannons per side for a ship entity
     */
    public static int getCannonsPerSide(Entity entity) {
        ShipType type = getShipType(entity);
        return type != null ? type.getCannonsPerSide() : 0;
    }
    
    /**
     * Get compatibility status message
     */
    public static String getCompatStatus() {
        if (!initialized) initialize();
        
        if (swashbucklersAvailable) {
            return "Swashbucklers ship cannons available (" + shipClasses.size() + " ship types)";
        } else {
            return "Swashbucklers not detected (ship cannons disabled)";
        }
    }
}
