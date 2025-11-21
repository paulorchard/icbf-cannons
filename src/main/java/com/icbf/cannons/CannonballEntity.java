package com.icbf.cannons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nonnull;
import java.util.List;

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
        super(level, shooter, velocityX, velocityY, velocityZ, 0); // We handle explosion manually
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

    @Override
    public boolean canCollideWith(@Nonnull Entity other) {
        // Ignore collisions with other cannonballs
        if (other instanceof CannonballEntity) {
            return false;
        }
        return super.canCollideWith(other);
    }

    @Override
    public boolean canBeCollidedWith() {
        // Cannonballs can be hit by blocks/entities, but not other cannonballs
        return true;
    }

    @Override
    public boolean isPushable() {
        // Prevent physics push from other entities (including explosions and other cannonballs)
        return false;
    }

    @Override
    public void push(@Nonnull Entity entity) {
        // Don't apply push forces from other entities
        if (entity instanceof CannonballEntity) {
            return; // Completely ignore cannonball-to-cannonball pushes
        }
        // Allow other entity pushes (though isPushable() should prevent most)
        super.push(entity);
    }

    @Override
    public boolean hurt(@Nonnull DamageSource source, float amount) {
        // Immune to explosions - prevents chain reactions and deflection
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void onHit(@Nonnull HitResult result) {
        super.onHit(result);
        
        if (!this.level().isClientSide) {
            Vec3 impactPos = this.position();
            
            // Create custom explosion with visual effects
            this.createCustomExplosion();
            
            // Send impact notification to shooter for long-range visual feedback
            if (this.getOwner() instanceof ServerPlayer shooter) {
                IcbfCannons.sendImpactPacketToPlayer(shooter, impactPos);
            }
            
            this.discard();
        }
    }

    private void createCustomExplosion() {
        Level level = this.level();
        Vec3 pos = this.position();
        
        // Get config values
        double explosionPower = Config.explosionPower;
        double blockBreakChance = Config.blockBreakChance;
        double entityDamageRadius = Config.entityDamageRadius;
        double entityDamage = Config.entityDamage;
        boolean friendlyFire = Config.friendlyFire;
        
        // Create visual/sound explosion effect (no block damage from vanilla explosion)
        level.explode(
            this,
            pos.x, pos.y, pos.z,
            (float) explosionPower,
            Level.ExplosionInteraction.NONE // No block breaking from vanilla explosion
        );
        
        // Enhanced particle effects for better visibility
        if (level instanceof ServerLevel serverLevel) {
            // Large explosion cloud (visible from far away)
            serverLevel.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y, pos.z,
                1, 0, 0, 0, 0
            );
            
            // Multiple smaller explosions for visual impact
            for (int i = 0; i < 8; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 2;
                double offsetY = (level.random.nextDouble() - 0.5) * 2;
                double offsetZ = (level.random.nextDouble() - 0.5) * 2;
                serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION,
                    pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                    3, 0.1, 0.1, 0.1, 0.05
                );
            }
            
            // Smoke plume
            serverLevel.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                pos.x, pos.y, pos.z,
                20, 0.5, 0.5, 0.5, 0.05
            );
            
            // Fire effects
            serverLevel.sendParticles(
                ParticleTypes.FLAME,
                pos.x, pos.y, pos.z,
                15, 0.3, 0.3, 0.3, 0.1
            );
        }
        
        // Play explosion sound (audible from distance)
        level.playSound(
            null,
            pos.x, pos.y, pos.z,
            SoundEvents.GENERIC_EXPLODE,
            SoundSource.BLOCKS,
            4.0F, // Volume
            (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F // Pitch
        );
        
        // Custom block destruction with chance-based breaking
        if (blockBreakChance > 0) {
            int radius = (int) Math.ceil(explosionPower * 2);
            BlockPos center = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z);
            
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos blockPos = center.offset(x, y, z);
                        double distance = Math.sqrt(x * x + y * y + z * z);
                        
                        // Only affect blocks within explosion radius
                        if (distance <= explosionPower * 2) {
                            // Random chance to break block (diminishing returns for volleys)
                            if (level.random.nextDouble() < blockBreakChance) {
                                // Only destroy non-air blocks
                                if (!level.getBlockState(blockPos).isAir()) {
                                    level.destroyBlock(blockPos, true);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Custom entity damage
        if (entityDamageRadius > 0 && entityDamage > 0) {
            AABB damageBounds = new AABB(
                pos.x - entityDamageRadius,
                pos.y - entityDamageRadius,
                pos.z - entityDamageRadius,
                pos.x + entityDamageRadius,
                pos.y + entityDamageRadius,
                pos.z + entityDamageRadius
            );
            
            List<Entity> entities = level.getEntities(this, damageBounds);
            
            for (Entity entity : entities) {
                // Skip cannonballs
                if (entity instanceof CannonballEntity) {
                    continue;
                }
                
                // Skip shooter if friendly fire is disabled
                if (!friendlyFire && entity == this.getOwner()) {
                    continue;
                }
                
                double distance = entity.position().distanceTo(pos);
                
                if (distance <= entityDamageRadius) {
                    // Damage falloff based on distance
                    double damageMultiplier = 1.0 - (distance / entityDamageRadius);
                    float finalDamage = (float) (entityDamage * damageMultiplier);
                    
                    entity.hurt(this.damageSources().explosion(this, this.getOwner()), finalDamage);
                }
            }
        }
    }
}
