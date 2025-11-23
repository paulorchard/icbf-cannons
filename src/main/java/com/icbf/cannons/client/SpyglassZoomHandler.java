package com.icbf.cannons.client;

import com.icbf.cannons.IcbfCannons;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forces first-person camera when using spyglass while mounted on a vehicle/ship
 */
@Mod.EventBusSubscriber(modid = IcbfCannons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SpyglassZoomHandler {
    
    private static boolean wasZooming = false;
    private static CameraType previousCamera = null;
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null) {
            wasZooming = false;
            previousCamera = null;
            return;
        }
        
        // Check if player is using spyglass
        boolean isZooming = player.isUsingItem() && player.getMainHandItem().is(Items.SPYGLASS);
        
        // Check if player is mounted on anything (ship, horse, minecart, etc.)
        boolean isMounted = player.getVehicle() != null;
        
        // Starting to zoom while mounted
        if (isZooming && isMounted && !wasZooming) {
            // Save current camera type
            previousCamera = mc.options.getCameraType();
            
            // Force first-person
            if (previousCamera != CameraType.FIRST_PERSON) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
        }
        
        // Stopped zooming or dismounted - restore camera
        if (wasZooming && (!isZooming || !isMounted)) {
            if (previousCamera != null && previousCamera != CameraType.FIRST_PERSON) {
                mc.options.setCameraType(previousCamera);
            }
            previousCamera = null;
        }
        
        wasZooming = isZooming && isMounted;
    }
}
