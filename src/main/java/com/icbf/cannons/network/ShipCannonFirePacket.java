package com.icbf.cannons.network;

import com.icbf.cannons.CannonballEntity;
import com.icbf.cannons.IcbfCannons;
import com.icbf.cannons.util.ShipCannonHelper;
import com.icbf.cannons.util.SwashbucklersShipHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Packet sent from client to server when player fires ship cannons with spyglass
 */
public class ShipCannonFirePacket {
    private final Vec3 targetPos;
    
    public ShipCannonFirePacket(Vec3 targetPos) {
        this.targetPos = targetPos;
    }
    
    public static void encode(ShipCannonFirePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.targetPos.x);
        buffer.writeDouble(packet.targetPos.y);
        buffer.writeDouble(packet.targetPos.z);
    }
    
    public static ShipCannonFirePacket decode(FriendlyByteBuf buffer) {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        return new ShipCannonFirePacket(new Vec3(x, y, z));
    }
    
    public static void handle(ShipCannonFirePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            // Validate player is mounted on a ship
            Entity vehicle = player.getVehicle();
            if (vehicle == null || !SwashbucklersShipHelper.isSwashbucklersShip(vehicle)) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Not mounted on a ship Cap'n"),
                    true
                );
                return;
            }
            
            // Get ship type and cannon count
            int cannonsPerSide = SwashbucklersShipHelper.getCannonsPerSide(vehicle);
            if (cannonsPerSide == 0) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("This ship has no cannons Cap'n"),
                    true
                );
                return;
            }
            
            // Calculate all cannon positions
            List<ShipCannonHelper.CannonPosition> allCannons = 
                ShipCannonHelper.calculateCannonPositions(vehicle, cannonsPerSide);
            
            // Filter cannons that can fire at the target (cone of fire check)
            List<ShipCannonHelper.CannonPosition> validCannons = 
                ShipCannonHelper.getValidCannons(allCannons, packet.targetPos);
            
            if (validCannons.isEmpty()) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("No cannons can target that position Cap'n"),
                    true
                );
                return;
            }
            
            // Fire all valid cannons with sequential delay
            fireShipCannons(player, vehicle, validCannons, packet.targetPos);
        });
        context.setPacketHandled(true);
    }
    
    /**
     * Fire cannons sequentially with visual effects
     */
    private static void fireShipCannons(ServerPlayer player, Entity ship, 
                                       List<ShipCannonHelper.CannonPosition> cannons, Vec3 targetPos) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        
        // Fire cannons sequentially with 100ms delay between each
        for (int i = 0; i < cannons.size(); i++) {
            final int index = i;
            final ShipCannonHelper.CannonPosition cannon = cannons.get(i);
            
            // Schedule delayed firing
            long delayMs = i * 100L; // 100ms between each cannon
            
            if (player.getServer() != null) {
                player.getServer().execute(() -> {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    
                    // Fire on main server thread
                    if (player.getServer() != null) {
                        player.getServer().execute(() -> {
                            fireCannonball(serverLevel, cannon, targetPos);
                        });
                    }
                });
            }
        }
        
        // Feedback message
        String side = cannons.get(0).isPort ? "Port" : "Starboard";
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(side + " cannons fired! (" + cannons.size() + " cannons)"),
            true
        );
    }
    
    /**
     * Fire a single cannonball from a cannon position
     */
    private static void fireCannonball(ServerLevel level, ShipCannonHelper.CannonPosition cannon, Vec3 targetPos) {
        // Calculate spawn position (slightly in front of cannon)
        Vec3 spawnPos = cannon.position.add(cannon.direction.scale(1.5));
        
        // Create muzzle flash effects
        level.sendParticles(
            ParticleTypes.LARGE_SMOKE,
            spawnPos.x, spawnPos.y, spawnPos.z,
            30, // Count
            cannon.direction.x * 2, cannon.direction.y * 2, cannon.direction.z * 2,
            0.3 // Speed
        );
        
        level.sendParticles(
            ParticleTypes.EXPLOSION,
            spawnPos.x, spawnPos.y, spawnPos.z,
            3,
            0.2, 0.2, 0.2,
            0.0
        );
        
        // Play cannon fire sound
        level.playSound(
            null,
            spawnPos.x, spawnPos.y, spawnPos.z,
            SoundEvents.GENERIC_EXPLODE,
            SoundSource.PLAYERS,
            2.0f, // Louder than normal
            0.8f  // Lower pitch for cannon sound
        );
        
        // Spawn cannonball entity
        CannonballEntity cannonball = new CannonballEntity(
            IcbfCannons.CANNONBALL.get(),
            level
        );
        
        cannonball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        
        // Calculate direction to target
        Vec3 fireDirection = targetPos.subtract(cannon.position).normalize();
        double speed = 1.0;
        cannonball.setDeltaMovement(
            fireDirection.x * speed,
            fireDirection.y * speed,
            fireDirection.z * speed
        );
        
        level.addFreshEntity(cannonball);
    }
}
