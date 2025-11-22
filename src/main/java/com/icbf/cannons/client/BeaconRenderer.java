package com.icbf.cannons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders a red beacon beam at the compass target location
 */
@Mod.EventBusSubscriber(modid = "icbfcannons", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BeaconRenderer {
    private static BlockPos beaconPos = null;
    private static long lastUpdateTime = 0;
    private static final long BEACON_TIMEOUT = 100; // ms
    
    private static final ResourceLocation BEACON_BEAM = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/beacon_beam.png");
    
    public static void setBeaconPosition(BlockPos pos) {
        beaconPos = pos;
        lastUpdateTime = System.currentTimeMillis();
    }
    
    public static void clearBeacon() {
        beaconPos = null;
    }
    
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        
        // Check timeout
        if (beaconPos != null && System.currentTimeMillis() - lastUpdateTime > BEACON_TIMEOUT) {
            beaconPos = null;
            return;
        }
        
        if (beaconPos == null) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        
        poseStack.pushPose();
        
        // Translate to beacon position - centered on the block
        double renderX = beaconPos.getX() + 0.5 - event.getCamera().getPosition().x;
        double renderY = beaconPos.getY() + 1.0 - event.getCamera().getPosition().y; // Start from top of block
        double renderZ = beaconPos.getZ() + 0.5 - event.getCamera().getPosition().z;
        
        poseStack.translate(renderX, renderY, renderZ);
        
        // Render beacon beam
        renderBeaconBeam(poseStack, bufferSource, event.getPartialTick());
        
        poseStack.popPose();
    }
    
    private static void renderBeaconBeam(PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
        // Red color for targeting beam
        float[] color = new float[]{1.0f, 0.0f, 0.0f, 1.0f}; // Red with default opacity
        
        long gameTime = Minecraft.getInstance().level.getGameTime();
        int height = 256; // Beam extends upward to build limit
        int yOffset = -2; // Start rendering 2 blocks down (passes through target into block below)
        
        // Use Minecraft's beacon beam rendering
        net.minecraft.client.renderer.blockentity.BeaconRenderer.renderBeaconBeam(
            poseStack,
            bufferSource,
            BEACON_BEAM,
            partialTick,
            1.0f, // heightScale
            gameTime,
            yOffset, // Start 2 blocks down to pass through target block
            height,
            color,
            0.2f, // beamRadius (inner) - very thin core beam
            1.0f  // glowRadius (outer) - subtle glow
        );
    }
}
