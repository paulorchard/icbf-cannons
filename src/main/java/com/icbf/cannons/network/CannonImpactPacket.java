package com.icbf.cannons.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client when a cannonball impacts.
 * Provides visual feedback to the shooter even at long range.
 */
public class CannonImpactPacket {
    private final double x;
    private final double y;
    private final double z;

    public CannonImpactPacket(Vec3 impactPos) {
        this.x = impactPos.x;
        this.y = impactPos.y;
        this.z = impactPos.z;
    }

    public CannonImpactPacket(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(CannonImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
    }

    public static CannonImpactPacket decode(FriendlyByteBuf buffer) {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        return new CannonImpactPacket(x, y, z);
    }

    public static void handle(CannonImpactPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // CLIENT-SIDE: Spawn visual effects at impact location
            Minecraft mc = Minecraft.getInstance();
            Level level = mc.level;
            
            if (level != null) {
                Vec3 impactPos = new Vec3(packet.x, packet.y, packet.z);
                Vec3 playerPos = mc.player != null ? mc.player.position() : Vec3.ZERO;
                double distance = playerPos.distanceTo(impactPos);
                
                // Spawn marker particles visible only to this player (client-side)
                // These particles render even if the explosion is far away
                for (int i = 0; i < 30; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 3;
                    double offsetY = (level.random.nextDouble() - 0.5) * 3;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 3;
                    
                    level.addParticle(
                        ParticleTypes.EXPLOSION,
                        packet.x + offsetX,
                        packet.y + offsetY,
                        packet.z + offsetZ,
                        0, 0.05, 0
                    );
                }
                
                // Add smoke column for visibility
                for (int i = 0; i < 50; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 2;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 2;
                    double offsetY = level.random.nextDouble() * 5;
                    
                    level.addParticle(
                        ParticleTypes.LARGE_SMOKE,
                        packet.x + offsetX,
                        packet.y + offsetY,
                        packet.z + offsetZ,
                        0, 0.1, 0
                    );
                }
                
                // Play distance-scaled sound
                float volume = Math.max(0.5F, Math.min(4.0F, (float) (100.0 / Math.max(distance, 10.0))));
                float pitch = 0.7F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F;
                
                level.playLocalSound(
                    packet.x, packet.y, packet.z,
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS,
                    volume,
                    pitch,
                    false
                );
            }
        });
        context.setPacketHandled(true);
    }
}
