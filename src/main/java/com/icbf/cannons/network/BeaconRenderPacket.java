package com.icbf.cannons.network;

import com.icbf.cannons.client.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to render/clear beacon
 */
public class BeaconRenderPacket {
    private final BlockPos pos;
    private final boolean show;
    
    public BeaconRenderPacket(BlockPos pos, boolean show) {
        this.pos = pos;
        this.show = show;
    }
    
    public static void encode(BeaconRenderPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeBoolean(packet.show);
    }
    
    public static BeaconRenderPacket decode(FriendlyByteBuf buffer) {
        return new BeaconRenderPacket(buffer.readBlockPos(), buffer.readBoolean());
    }
    
    public static void handle(BeaconRenderPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Execute on client side only
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (packet.show) {
                    BeaconRenderer.setBeaconPosition(packet.pos);
                } else {
                    BeaconRenderer.clearBeacon();
                }
            });
        });
        context.setPacketHandled(true);
    }
}
