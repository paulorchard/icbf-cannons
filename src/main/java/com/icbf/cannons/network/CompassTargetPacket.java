package com.icbf.cannons.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server when player uses compass targeting
 */
public class CompassTargetPacket {
    private final boolean released; // true when right-click is released
    
    public CompassTargetPacket(boolean released) {
        this.released = released;
    }
    
    public static void encode(CompassTargetPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.released);
    }
    
    public static CompassTargetPacket decode(FriendlyByteBuf buffer) {
        return new CompassTargetPacket(buffer.readBoolean());
    }
    
    public static void handle(CompassTargetPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            // Perform raycast
            BlockHitResult hitResult = performRaycast(player);
            
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos targetPos = hitResult.getBlockPos();
                
                if (packet.released) {
                    // Calculate distance for delay
                    double distance = player.position().distanceTo(
                        new net.minecraft.world.phys.Vec3(
                            targetPos.getX() + 0.5,
                            targetPos.getY() + 0.5,
                            targetPos.getZ() + 0.5
                        )
                    );
                    
                    // 10ms per block delay
                    long delayMs = (long) (distance * 10);
                    
                    // Schedule delayed explosion
                    scheduleDelayedExplosion(player, targetPos, delayMs);
                } else {
                    // Update beacon location (send to client for rendering)
                    sendBeaconUpdate(player, targetPos);
                }
            }
        });
        context.setPacketHandled(true);
    }
    
    private static BlockHitResult performRaycast(ServerPlayer player) {
        double reach = 200.0; // Maximum raycast distance
        
        ClipContext context = new ClipContext(
            player.getEyePosition(),
            player.getEyePosition().add(player.getLookAngle().scale(reach)),
            ClipContext.Block.COLLIDER, // Stop at solid blocks
            ClipContext.Fluid.ANY, // Stop at water
            player
        );
        
        // Use vanilla raycast
        return player.level().clip(context);
    }
    
    private static void scheduleDelayedExplosion(ServerPlayer player, BlockPos targetPos, long delayMs) {
        // Schedule the explosion on the server thread
        if (player.getServer() == null) return;
        
        player.getServer().execute(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            
            // Execute on main server thread after delay
            if (player.getServer() != null) {
                player.getServer().execute(() -> {
                    spawnTNTExplosion(player, targetPos);
                });
            }
        });
    }
    
    private static void spawnTNTExplosion(ServerPlayer player, BlockPos targetPos) {
        BlockPos explosionPos = targetPos.above();
        
        // Play explosion sound for hit confirmation
        player.level().playSound(
            null, // null = broadcast to all nearby players
            explosionPos,
            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
            net.minecraft.sounds.SoundSource.PLAYERS,
            1.0f, // volume
            1.0f  // pitch
        );
        
        // Send explosion particles to the player for hit confirmation (no actual damage)
        if (player.level() instanceof ServerLevel serverLevel) {
            // Large explosion particle visible at range
            serverLevel.sendParticles(
                player, // Only send to the targeting player
                ParticleTypes.EXPLOSION_EMITTER,
                true, // Force render at distance
                explosionPos.getX() + 0.5,
                explosionPos.getY() + 0.5,
                explosionPos.getZ() + 0.5,
                5, // particle count
                0.5, 0.5, 0.5, // spread
                0.0 // speed
            );
        }
        
        // Clear beacon on client
        sendBeaconClear(player);
    }
    
    private static void sendBeaconUpdate(ServerPlayer player, BlockPos targetPos) {
        // Send packet to client to render beacon
        com.icbf.cannons.IcbfCannons.NETWORK.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new BeaconRenderPacket(targetPos, true)
        );
    }
    
    private static void sendBeaconClear(ServerPlayer player) {
        // Send packet to client to clear beacon
        com.icbf.cannons.IcbfCannons.NETWORK.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new BeaconRenderPacket(BlockPos.ZERO, false)
        );
    }
}
