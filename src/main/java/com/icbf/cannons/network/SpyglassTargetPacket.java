package com.icbf.cannons.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SpyglassTargetPacket {
    
    public SpyglassTargetPacket() {
        // Empty packet - just signals that the player left-clicked with spyglass
    }

    public static void encode(SpyglassTargetPacket packet, FriendlyByteBuf buffer) {
        // No data to encode
    }

    public static SpyglassTargetPacket decode(FriendlyByteBuf buffer) {
        return new SpyglassTargetPacket();
    }

    public static void handle(SpyglassTargetPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Call the targeting logic on the server thread
                com.icbf.cannons.IcbfCannons.handleSpyglassTargeting(player, player.level());
            }
        });
        context.setPacketHandled(true);
    }
}
