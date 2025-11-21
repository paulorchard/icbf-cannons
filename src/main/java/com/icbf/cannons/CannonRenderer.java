package com.icbf.cannons;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraft.world.level.LightLayer;

import java.util.List;

public class CannonRenderer implements BlockEntityRenderer<ModBlockEntities.CannonBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public CannonRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(ModBlockEntities.CannonBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof ModBlocks.CannonControllerBlock)) {
            return;
        }

        // Get the base model
        BakedModel model = blockRenderer.getBlockModel(state);
        Direction facing = state.getValue(ModBlocks.CannonControllerBlock.FACING);
        
        poseStack.pushPose();
        
        // Apply rotation manually since ENTITYBLOCK_ANIMATED doesn't use blockstate variants
        poseStack.translate(0.5, 0.0, 0.5);
        
        switch (facing) {
            case NORTH:
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                break;
            case EAST:
                poseStack.mulPose(Axis.YP.rotationDegrees(270));
                break;
            case WEST:
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                break;
            case SOUTH:
            default:
                // No rotation for south (0 degrees)
                break;
        }
        
        poseStack.translate(-0.5, 0.0, -0.5);
        
        // Render with per-face lighting
        renderModelWithPerFaceLighting(blockEntity, poseStack, bufferSource, state, model, combinedOverlay, facing);
        
        poseStack.popPose();
    }
    
    /**
     * Render the model with per-face intelligent lighting
     * Each face samples light from the appropriate multiblock position
     */
    private void renderModelWithPerFaceLighting(ModBlockEntities.CannonBlockEntity blockEntity, 
                                                 PoseStack poseStack, MultiBufferSource bufferSource,
                                                 BlockState state, BakedModel model, int overlay, Direction cannonFacing) {
        
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
        RandomSource random = RandomSource.create();
        
        // Render each direction's quads with appropriate lighting
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            List<BakedQuad> quads = model.getQuads(state, direction, random, ModelData.EMPTY, RenderType.solid());
            
            if (!quads.isEmpty()) {
                // Calculate light for this face direction
                int faceLight = getLightForFace(blockEntity, direction, cannonFacing);
                
                // Render all quads for this direction with the calculated light
                for (BakedQuad quad : quads) {
                    consumer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, faceLight, overlay);
                }
            }
        }
        
        // Also render non-culled quads (null direction)
        random.setSeed(42L);
        List<BakedQuad> unculledQuads = model.getQuads(state, null, random, ModelData.EMPTY, RenderType.solid());
        for (BakedQuad quad : unculledQuads) {
            // For unculled quads, use smart lighting based on quad's own direction
            Direction quadDir = quad.getDirection();
            int light = getLightForFace(blockEntity, quadDir, cannonFacing);
            consumer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, light, overlay);
        }
    }
    
    /**
     * Get intelligent light value for a specific face direction
     * Samples light from the multiblock position that corresponds to that face
     */
    private int getLightForFace(ModBlockEntities.CannonBlockEntity blockEntity, Direction faceDir, Direction cannonFacing) {
        BlockAndTintGetter level = blockEntity.getLevel();
        if (level == null) {
            return LightTexture.FULL_BRIGHT;
        }
        
        BlockPos controllerPos = blockEntity.getBlockPos();
        
        // Map face direction to appropriate multiblock sampling position
        // This ensures each face gets light from the correct part of the 3x1x2 structure
        BlockPos samplePos = getSamplePositionForFace(controllerPos, faceDir, cannonFacing);
        
        // Sample light from that position
        int blockLight = level.getBrightness(LightLayer.BLOCK, samplePos);
        int skyLight = level.getBrightness(LightLayer.SKY, samplePos);
        
        return LightTexture.pack(blockLight, skyLight);
    }
    
    /**
     * Determine which multiblock position to sample light from based on face direction
     * This is the KEY to solving the AO issue - each face samples from its own area
     */
    private BlockPos getSamplePositionForFace(BlockPos controllerPos, Direction faceDir, Direction cannonFacing) {
        if (faceDir == null) {
            return controllerPos; // Default to controller for null faces
        }
        
        // For BOTTOM faces - sample from the position BELOW to avoid ground darkening
        if (faceDir == Direction.DOWN) {
            return controllerPos.above(); // Sample from TOP positions instead!
        }
        
        // For TOP faces - sample from above positions
        if (faceDir == Direction.UP) {
            return controllerPos.above();
        }
        
        // For NORTH faces (back of cannon) - sample from back positions
        if (faceDir == Direction.NORTH) {
            return controllerPos.north().above(); // Sample from back-top for better lighting
        }
        
        // For SOUTH faces (front of cannon) - sample from front positions
        if (faceDir == Direction.SOUTH) {
            return controllerPos.south().above(); // Sample from front-top for better lighting
        }
        
        // For EAST/WEST faces - sample from controller or shifted position
        if (faceDir == Direction.EAST || faceDir == Direction.WEST) {
            return controllerPos.above(); // Sample from middle-top for side faces
        }
        
        return controllerPos;
    }
}
