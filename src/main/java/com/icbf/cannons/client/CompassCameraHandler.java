package com.icbf.cannons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles camera zoom when using compass targeting
 * Only zooms out if camera is closer than the target distance
 */
@Mod.EventBusSubscriber(modid = "icbfcannons", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CompassCameraHandler {
    
    // Configurable zoom distance - adjust this to change how far the camera pulls back
    public static double TARGET_ZOOM_DISTANCE = 10.0; // Default: 10 blocks
    
    private static boolean isTargeting = false;
    private static double savedCameraDistance = 4.0;
    private static boolean hasModifiedCamera = false;
    
    public static void setTargeting(boolean targeting) {
        Minecraft mc = Minecraft.getInstance();
        
        if (targeting && !isTargeting) {
            // Starting to target - save current camera distance
            if (mc.options.getCameraType().isFirstPerson()) {
                // Don't modify first person view
                isTargeting = targeting;
                return;
            }
            
            // Get current third person distance
            savedCameraDistance = 4.0; // Default Minecraft distance
            
            // Only zoom if current distance is less than target
            if (savedCameraDistance < TARGET_ZOOM_DISTANCE) {
                // We'll handle the zoom via matrix transforms in render event
                hasModifiedCamera = true;
            } else {
                hasModifiedCamera = false;
            }
        } else if (!targeting && isTargeting) {
            // Stopped targeting - restore camera
            hasModifiedCamera = false;
        }
        
        isTargeting = targeting;
    }
    
    public static boolean isTargeting() {
        return isTargeting;
    }
    
    public static double getZoomMultiplier() {
        if (!isTargeting || !hasModifiedCamera) {
            return 1.0;
        }
        return TARGET_ZOOM_DISTANCE / savedCameraDistance;
    }
}
