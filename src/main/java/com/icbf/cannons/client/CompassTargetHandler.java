package com.icbf.cannons.client;

import com.icbf.cannons.IcbfCannons;
import com.icbf.cannons.network.CompassTargetPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler for compass targeting
 * Tracks right-click state and sends packets to server
 */
@Mod.EventBusSubscriber(modid = IcbfCannons.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CompassTargetHandler {
    private static boolean lastTickUseKeyDown = false;
    private static boolean isTargeting = false;
    private static long lastFireTime = 0;
    private static final long COOLDOWN_MS = 7000; // 7 second cooldown
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null) {
            isTargeting = false;
            return;
        }
        
        // Check if player is holding compass
        boolean holdingCompass = player.getMainHandItem().is(Items.COMPASS) || 
                                 player.getOffhandItem().is(Items.COMPASS);
        boolean useKeyDown = mc.options.keyUse.isDown();
        
        if (holdingCompass) {
            if (useKeyDown && !lastTickUseKeyDown) {
                // Check cooldown before starting targeting
                long currentTime = System.currentTimeMillis();
                long timeSinceLastFire = currentTime - lastFireTime;
                
                if (timeSinceLastFire < COOLDOWN_MS) {
                    // Still on cooldown - show message
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Reloading Cap'n..."),
                        true
                    );
                    return;
                }
                
                // Start targeting
                isTargeting = true;
                CompassCameraHandler.setTargeting(true);
                sendTargetPacket(false);
            } else if (useKeyDown && isTargeting) {
                // Continue targeting (update beacon)
                sendTargetPacket(false);
            } else if (!useKeyDown && lastTickUseKeyDown && isTargeting) {
                // Released - fire!
                sendTargetPacket(true);
                isTargeting = false;
                CompassCameraHandler.setTargeting(false);
                lastFireTime = System.currentTimeMillis(); // Start cooldown
            }
        } else {
            if (isTargeting) {
                isTargeting = false;
                CompassCameraHandler.setTargeting(false);
            }
        }
        
        lastTickUseKeyDown = useKeyDown;
    }
    
    /**
     * Cancel all right-click interactions while holding compass and targeting
     * Prevents opening VS helm GUI or other block interactions
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide().isClient() && isTargeting) {
            Player player = event.getEntity();
            boolean holdingCompass = player.getMainHandItem().is(Items.COMPASS) || 
                                     player.getOffhandItem().is(Items.COMPASS);
            if (holdingCompass) {
                // Cancel the interaction - compass targeting only
                event.setCanceled(true);
                event.setUseBlock(Event.Result.DENY);
                event.setUseItem(Event.Result.DENY);
            }
        }
    }
    
    /**
     * Cancel entity interactions while targeting
     */
    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getSide().isClient() && isTargeting) {
            Player player = event.getEntity();
            boolean holdingCompass = player.getMainHandItem().is(Items.COMPASS) || 
                                     player.getOffhandItem().is(Items.COMPASS);
            if (holdingCompass) {
                event.setCanceled(true);
            }
        }
    }
    
    private static void sendTargetPacket(boolean released) {
        IcbfCannons.NETWORK.sendToServer(new CompassTargetPacket(released));
    }
}
