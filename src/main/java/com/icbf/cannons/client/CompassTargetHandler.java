package com.icbf.cannons.client;

import com.icbf.cannons.IcbfCannons;
import com.icbf.cannons.network.CompassTargetPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
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
            }
        } else {
            if (isTargeting) {
                isTargeting = false;
                CompassCameraHandler.setTargeting(false);
            }
        }
        
        lastTickUseKeyDown = useKeyDown;
    }
    
    private static void sendTargetPacket(boolean released) {
        IcbfCannons.NETWORK.sendToServer(new CompassTargetPacket(released));
    }
}
