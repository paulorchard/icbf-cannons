package com.icbf.cannons.client;

import com.icbf.cannons.IcbfCannons;
import com.icbf.cannons.network.ShipCannonFirePacket;
import com.icbf.cannons.util.SwashbucklersShipHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for ship cannon targeting with spyglass
 */
@Mod.EventBusSubscriber(modid = IcbfCannons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SpyglassShipTargeting {
    
    private static boolean wasUsingSpyglass = false;
    private static long lastFireTime = 0;
    private static final long COOLDOWN_MS = 7000; // 7 seconds
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null || mc.level == null) {
            wasUsingSpyglass = false;
            return;
        }
        
        // Check if player is mounted on a ship
        Entity vehicle = player.getVehicle();
        if (vehicle == null || !SwashbucklersShipHelper.isSwashbucklersShip(vehicle)) {
            wasUsingSpyglass = false;
            return;
        }
        
        // Check if player is using spyglass
        boolean usingSpyglass = player.isUsingItem() && player.getMainHandItem().is(Items.SPYGLASS);
        
        // Detect when spyglass use ends (released)
        if (wasUsingSpyglass && !usingSpyglass) {
            // Check cooldown
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFireTime < COOLDOWN_MS) {
                long remainingSeconds = (COOLDOWN_MS - (currentTime - lastFireTime)) / 1000;
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Ship Cannons Reloading! (" + remainingSeconds + "s)"),
                    true
                );
                wasUsingSpyglass = false;
                return;
            }
            
            // Perform raycast to find target
            Vec3 targetPos = performRaycast(player);
            
            if (targetPos != null) {
                // Send packet to server to fire ship cannons
                IcbfCannons.NETWORK.sendToServer(new ShipCannonFirePacket(targetPos));
                lastFireTime = currentTime;
            } else {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("No target Cap'n"),
                    true
                );
            }
        }
        
        wasUsingSpyglass = usingSpyglass;
    }
    
    /**
     * Perform raycast to find target position (same as regular cannons)
     */
    private static Vec3 performRaycast(Player player) {
        double reach = 200.0;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(reach));
        
        // Block raycast
        ClipContext context = new ClipContext(
            eyePos,
            endPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.ANY,
            player
        );
        
        BlockHitResult blockHit = player.level().clip(context);
        
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getLocation();
        }
        
        // TODO: Add entity raycast if needed
        
        return null;
    }
}
