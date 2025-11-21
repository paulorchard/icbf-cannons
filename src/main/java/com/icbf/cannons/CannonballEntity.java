package com.icbf.cannons;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nonnull;

/**
 * Custom cannonball projectile with distance-based gravity.
 * Applies minimal gravity (0.001) within 200 blocks for a nearly straight trajectory,
 * then applies strong gravity (0.1) beyond 200 blocks for rapid fall.
 * Auto-despawns at 400 blocks for performance.
 */
public class CannonballEntity extends LargeFireball {
    private Vec3 startPos;
    private static final double MAX_EFFECTIVE_RANGE = 200.0;
    private static final double MAX_ABSOLUTE_RANGE = 400.0;
    private static final double GRAVITY_WITHIN_RANGE = 0.001;  // Minimal drop: ~1 block at 200 blocks
    private static final double GRAVITY_BEYOND_RANGE = 0.1;    // Rapid fall beyond range
    
    // Required constructor for entity registration
    public CannonballEntity(EntityType<? extends LargeFireball> type, Level level) {
        super(type, level);
    }
    
    // Constructor for spawning with initial velocity
    public CannonballEntity(Level level, LivingEntity shooter, double velocityX, double velocityY, double velocityZ) {
        super(level, shooter, velocityX, velocityY, velocityZ, 0); // 0 = no block damage
        this.startPos = this.position();
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (this.startPos != null) {
            double distanceTraveled = this.position().distanceTo(this.startPos);
            
            // Despawn if traveled too far (safety mechanism)
            if (distanceTraveled > MAX_ABSOLUTE_RANGE) {
                this.discard();
                return;
            }
            
            // Apply distance-based gravity
            Vec3 motion = this.getDeltaMovement();
            
            if (distanceTraveled < MAX_EFFECTIVE_RANGE) {
                // Slight drop: minimal gravity for nearly straight flight
                this.setDeltaMovement(motion.x, motion.y - GRAVITY_WITHIN_RANGE, motion.z);
            } else {
                // Rapid fall: strong gravity beyond effective range
                this.setDeltaMovement(motion.x, motion.y - GRAVITY_BEYOND_RANGE, motion.z);
            }
        }
    }
    
    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (startPos != null) {
            tag.putDouble("StartX", startPos.x);
            tag.putDouble("StartY", startPos.y);
            tag.putDouble("StartZ", startPos.z);
        }
    }
    
    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("StartX")) {
            this.startPos = new Vec3(
                tag.getDouble("StartX"),
                tag.getDouble("StartY"),
                tag.getDouble("StartZ")
            );
        }
    }
}
