package com.icbf.cannons.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for calculating ship cannon positions and validating targeting
 */
public class ShipCannonHelper {
    
    /**
     * Represents a single cannon position on a ship
     */
    public static class CannonPosition {
        public final Vec3 position;
        public final Vec3 direction;
        public final boolean isPort; // true = port (left), false = starboard (right)
        
        public CannonPosition(Vec3 position, Vec3 direction, boolean isPort) {
            this.position = position;
            this.direction = direction;
            this.isPort = isPort;
        }
    }
    
    /**
     * Calculate all cannon positions for a ship
     * @param ship The ship entity
     * @param cannonsPerSide Number of cannons on each side (port and starboard)
     * @return List of all cannon positions
     */
    public static List<CannonPosition> calculateCannonPositions(Entity ship, int cannonsPerSide) {
        List<CannonPosition> cannons = new ArrayList<>();
        
        if (cannonsPerSide <= 0) {
            return cannons; // No cannons (e.g., Raft)
        }
        
        // Get ship's bounding box and center
        AABB bounds = ship.getBoundingBox();
        Vec3 shipCenter = ship.position();
        
        // Calculate ship dimensions
        double shipLength = Math.max(bounds.maxZ - bounds.minZ, bounds.maxX - bounds.minX);
        double shipWidth = Math.min(bounds.maxZ - bounds.minZ, bounds.maxX - bounds.minX);
        
        // Ship's rotation (yaw)
        float shipYaw = ship.getYRot();
        double yawRad = Math.toRadians(shipYaw);
        
        // Calculate forward and right vectors based on ship rotation
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad));
        
        // Cannon height (slightly above ship center)
        double cannonHeight = shipCenter.y + 1.0;
        
        // Calculate spacing between cannons along the ship's length
        double spacing = cannonsPerSide > 1 ? shipLength * 0.6 / (cannonsPerSide - 1) : 0;
        double startOffset = cannonsPerSide > 1 ? -shipLength * 0.3 : 0; // Start at front third
        
        // Offset from centerline (half ship width + extra for clearance)
        double sideOffset = shipWidth * 0.6;
        
        // Generate cannon positions
        for (int i = 0; i < cannonsPerSide; i++) {
            // Position along ship's length
            double lengthOffset = startOffset + (i * spacing);
            
            // PORT SIDE (left when facing forward)
            Vec3 portBase = shipCenter.add(forward.scale(lengthOffset));
            Vec3 portPos = portBase.add(right.scale(-sideOffset)); // Left side
            portPos = new Vec3(portPos.x, cannonHeight, portPos.z);
            Vec3 portDir = right.scale(-1); // Fire to the left (port)
            cannons.add(new CannonPosition(portPos, portDir, true));
            
            // STARBOARD SIDE (right when facing forward)
            Vec3 starboardBase = shipCenter.add(forward.scale(lengthOffset));
            Vec3 starboardPos = starboardBase.add(right.scale(sideOffset)); // Right side
            starboardPos = new Vec3(starboardPos.x, cannonHeight, starboardPos.z);
            Vec3 starboardDir = right; // Fire to the right (starboard)
            cannons.add(new CannonPosition(starboardPos, starboardDir, false));
        }
        
        return cannons;
    }
    
    /**
     * Check if a cannon can fire at the target using the same logic as block cannons
     * @param cannonPos Position of the cannon
     * @param cannonDir Direction the cannon is facing
     * @param targetPos Target position
     * @return true if the cannon can fire at the target
     */
    public static boolean canFireAtTarget(Vec3 cannonPos, Vec3 cannonDir, Vec3 targetPos) {
        // Calculate vector to target
        Vec3 toTarget = targetPos.subtract(cannonPos).normalize();
        double dotProduct = cannonDir.dot(toTarget);
        
        // Check 1: Is target in front of cannon? (within ~60 degrees)
        if (dotProduct < 0.5) {
            return false;
        }
        
        // Check 2: Is target within range?
        double distance = cannonPos.distanceTo(targetPos);
        if (distance < 2 || distance > 200) {
            return false;
        }
        
        // Check 3: Is target within cone of fire? (14 degrees, same as block cannons)
        double angle = Math.acos(Math.min(1.0, Math.max(-1.0, dotProduct))); // Clamp for safety
        double maxAngle = Math.toRadians(14);
        if (angle > maxAngle) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Filter cannons that can actually fire at the target
     * @param cannons All cannon positions
     * @param targetPos Target position
     * @return List of cannons that can fire at the target
     */
    public static List<CannonPosition> getValidCannons(List<CannonPosition> cannons, Vec3 targetPos) {
        List<CannonPosition> validCannons = new ArrayList<>();
        
        for (CannonPosition cannon : cannons) {
            if (canFireAtTarget(cannon.position, cannon.direction, targetPos)) {
                validCannons.add(cannon);
            }
        }
        
        return validCannons;
    }
}
