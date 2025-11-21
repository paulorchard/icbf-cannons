// Residual file left from earlier refactor. Removed content to avoid duplicate classes.
package com.icbf.cannons;

import com.icbf.cannons.network.SpyglassTargetPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(IcbfCannons.MODID)
public class IcbfCannons
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "icbfcannons";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Network channel for client-server communication
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    // Create a Deferred Register to hold Blocks which will all be registered under the "IcbfCannons" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "IcbfCannons" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "IcbfCannons" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Remove duplicate - use ModItems.ICBF_CANNON instead
    // public static final RegistryObject<Item> ICBF_CANNON = ITEMS.register("icbf_cannon", () -> new Item(new Item.Properties()));

    // Creates the IslandCraft creative tab. Uses the vanilla spyglass as the icon
    public static final RegistryObject<CreativeModeTab> ISLANDCRAFT_TAB = CREATIVE_MODE_TABS.register("islandcraft", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .title(java.util.Objects.requireNonNull(net.minecraft.network.chat.Component.translatable("itemGroup.icbfcannons.islandcraft")))
            .icon(() -> Items.SPYGLASS.getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Place the vanilla spyglass first
                output.accept(Items.SPYGLASS);
                // Add the cannon item so it appears in IslandCraft
                output.accept(ModItems.ICBF_CANNON.get());
            }).build());

    public IcbfCannons(FMLJavaModLoadingContext context) {
        var modEventBus = context.getModEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        
        // Register network packets
        int packetId = 0;
        NETWORK.registerMessage(packetId++, SpyglassTargetPacket.class,
            SpyglassTargetPacket::encode,
            SpyglassTargetPacket::decode,
            SpyglassTargetPacket::handle);

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Task scheduler for delayed cannon firing
    private static class ScheduledTask {
        int delay;
        Runnable action;

        ScheduledTask(int delay, Runnable action) {
            this.delay = delay;
            this.action = action;
        }
    }

    private static final List<ScheduledTask> delayedTasks = new ArrayList<>();

    // Track whether the spyglass tick handler is currently registered
    private boolean spyglassTickListenerActive = false;
    private boolean lastTickAttackKeyDown = false; // Track attack key state to detect press (not hold)
    
    // Client-side tick handler for spyglass left-click detection (only registered when scoped)
    private final Object spyglassTickHandler = new Object() {
        @SubscribeEvent
        public void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
                return; // Only run at end of tick
            }
            
            @SuppressWarnings("resource")
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.world.entity.player.Player player = mc.player;
            
            // Safety check - unregister if player no longer exists or stopped using spyglass
            if (player == null || !player.isUsingItem() || !player.getMainHandItem().is(Items.SPYGLASS)) {
                unregisterSpyglassTickListener();
                return;
            }
            
            // Check if attack key is currently pressed
            boolean attackKeyDown = mc.options.keyAttack.isDown();
            
            // Detect rising edge (key just pressed, not held)
            if (attackKeyDown && !lastTickAttackKeyDown) {
                // Send packet to server to process targeting
                NETWORK.send(net.minecraftforge.network.PacketDistributor.SERVER.noArg(), new SpyglassTargetPacket());
            }
            
            // Update state for next tick
            lastTickAttackKeyDown = attackKeyDown;
        }
    };

    @SubscribeEvent
    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            if (delayedTasks.isEmpty()) return;
            
            java.util.Iterator<ScheduledTask> iterator = delayedTasks.iterator();
            while (iterator.hasNext()) {
                ScheduledTask task = iterator.next();
                task.delay--;
                if (task.delay <= 0) {
                    task.action.run();
                    iterator.remove();
                }
            }
        }
    }

    // Helper methods for dynamic event registration
    private void registerSpyglassTickListener() {
        if (!spyglassTickListenerActive) {
            MinecraftForge.EVENT_BUS.register(spyglassTickHandler);
            spyglassTickListenerActive = true;
        }
    }
    
    private void unregisterSpyglassTickListener() {
        if (spyglassTickListenerActive) {
            MinecraftForge.EVENT_BUS.unregister(spyglassTickHandler);
            spyglassTickListenerActive = false;
            lastTickAttackKeyDown = false; // Reset state when unregistering
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        delayedTasks.clear(); // Clear any pending tasks from previous session
        unregisterSpyglassTickListener(); // Cleanup tick listener on server restart
    }
    
    // Detect when player starts using spyglass (right-click to scope)
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        // Only run on client side
        if (!event.getLevel().isClientSide) {
            return;
        }
        
        // Check if player is starting to use spyglass
        if (event.getItemStack().is(Items.SPYGLASS)) {
            // Register the tick listener to detect left-clicks while scoped
            registerSpyglassTickListener();
        }
    }
    
    // Detect when player stops using any item (releases right-click or switches items)
    @SubscribeEvent
    public void onStopUsingItem(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Stop event) {
        // Only run on client side and only for players
        if (!(event.getEntity() instanceof Player) || !event.getEntity().level().isClientSide) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // If they were using spyglass, unregister the tick listener
        if (event.getItem().is(Items.SPYGLASS)) {
            unregisterSpyglassTickListener();
        }
    }
    
    // Handle left-click when NOT scoped (LeftClickEmpty for air clicks)
    @SubscribeEvent
    public void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        
        // Check if player is holding spyglass in main hand
        if (!player.getMainHandItem().is(Items.SPYGLASS)) {
            return;
        }
        
        // LeftClickEmpty is client-side only, send packet to server
        NETWORK.send(PacketDistributor.SERVER.noArg(), new SpyglassTargetPacket());
    }
    
    // Also handle left-click on blocks (when clicking visible blocks)
    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        
        // Check if player is holding spyglass in main hand
        if (!player.getMainHandItem().is(Items.SPYGLASS)) {
            return;
        }
        
        // Cancel block breaking when holding spyglass
        event.setCanceled(true);
        
        // Process targeting
        handleSpyglassTargeting(player, event.getLevel());
    }
    
    // Common targeting logic for spyglass
    public static void handleSpyglassTargeting(Player player, Level level) {
        // Only process on server side
        if (level.isClientSide) {
            return;
        }
        
        // Raycast from player's eyes to find target block (up to 200 blocks)
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        BlockPos targetPos = null;
        
        // Step along look vector in 1-block increments (low accuracy, fast)
        for (double distance = 0; distance <= 200; distance += 1.0) {
            Vec3 checkPos = eyePos.add(lookVec.scale(distance));
            BlockPos blockPos = BlockPos.containing(checkPos);
            BlockState state = level.getBlockState(blockPos);
            
            // Stop at ANY non-air block (water, glass, partial blocks, etc.)
            if (!state.isAir()) {
                targetPos = blockPos;
                break;
            }
        }
        
        // If no target found (looking at sky/air)
        if (targetPos == null) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("No target Cap'n"),
                true
            );
            return;
        }
        
        Vec3 targetVec = Vec3.atCenterOf(targetPos);
        
        // Search for cannon block entities in a reasonable radius (much more efficient)
        ServerLevel serverLevel = (ServerLevel) level;
        int searchRadiusXZ = 20;  // 20 blocks horizontal around player
        int searchRadiusY = 4;    // 4 blocks vertical (up and down)
        
        // Use chunk-based search for efficiency - only check loaded chunks
        int chunkRadius = (searchRadiusXZ / 16) + 1;
        int playerChunkX = player.blockPosition().getX() >> 4;
        int playerChunkZ = player.blockPosition().getZ() >> 4;
        int playerY = player.blockPosition().getY();
        
        // Collect all cannons that can fire at the target
        List<BlockPos> firableCannons = new ArrayList<>();
        int totalCannonsFound = 0;
        
        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                // Only check if chunk is loaded
                if (!serverLevel.hasChunk(cx, cz)) {
                    continue;
                }
                
                // Iterate through block entities in this chunk
                for (BlockEntity be : serverLevel.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof ModBlockEntities.CannonBlockEntity) {
                        BlockPos cannonPos = be.getBlockPos();
                        BlockState state = level.getBlockState(cannonPos);
                        
                        // Calculate horizontal and vertical distances separately
                        int dx = cannonPos.getX() - player.blockPosition().getX();
                        int dz = cannonPos.getZ() - player.blockPosition().getZ();
                        int dy = cannonPos.getY() - playerY;
                        int distXZSq = dx * dx + dz * dz;
                        
                        // Check XZ distance (20 blocks) and Y distance (4 blocks)
                        if (distXZSq > searchRadiusXZ * searchRadiusXZ || Math.abs(dy) > searchRadiusY) {
                            continue;
                        }
                        
                        totalCannonsFound++;
                        
                        // Check if this cannon can fire at the target (cone of fire check)
                        if (canFireAtTarget(level, cannonPos, state, targetVec)) {
                            firableCannons.add(cannonPos);
                        }
                    }
                }
            }
        }
        
        // Check if any cannons found in range
        if (totalCannonsFound == 0) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("No cannons in range Cap'n"),
                true
            );
            return;
        }
        
        // Check if any cannons can actually fire at target
        if (firableCannons.isEmpty()) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("No target Cap'n"),
                true
            );
            return;
        }
        
        // Fire all cannons with staggered delays (very tight - 50-100ms between each)
        for (int i = 0; i < firableCannons.size(); i++) {
            final BlockPos cannonPos = firableCannons.get(i);
            final BlockState cannonState = level.getBlockState(cannonPos);
            
            // Random delay between 1-2 ticks (0.05-0.1 seconds at 20 TPS)
            int delayTicks = 1 + level.random.nextInt(2);
            int totalDelay = i + delayTicks; // Base stagger of 1 tick per cannon plus random
            
            // Schedule the cannon to fire after delay using our custom scheduler
            delayedTasks.add(new ScheduledTask(totalDelay, () -> 
                tryFireCannonAtTarget(level, cannonPos, cannonState, player, targetVec)
            ));
        }
        
        // Notify player
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("FIRE! (" + firableCannons.size() + ")"),
            true
        );
    }
    
    // Check if a cannon can fire at the target (without actually firing)
    private static boolean canFireAtTarget(Level level, BlockPos cannonPos, BlockState cannonState, Vec3 targetVec) {
        Direction facing = cannonState.getValue(ModBlocks.CannonControllerBlock.FACING);
        
        // Calculate cannon firing position (pos5 - front top)
        BlockPos middle = cannonPos;
        BlockPos front = middle.relative(facing);
        BlockPos frontTop = front.above();
        Vec3 cannonVec = Vec3.atCenterOf(frontTop);
        
        // Get cannon facing direction
        Vec3 cannonDir = Vec3.atLowerCornerOf(facing.getNormal());
        
        // Check 1: Is target in front of cannon?
        Vec3 toTarget = targetVec.subtract(cannonVec).normalize();
        double dotProduct = cannonDir.dot(toTarget);
        if (dotProduct < 0.5) { // Must be roughly in front (within ~60 degrees)
            return false;
        }
        
        // Check 2: Is target within range?
        double distance = cannonVec.distanceTo(targetVec);
        if (distance < 2 || distance > 200) {
            return false;
        }
        
        // Check 3: Is target within cone of fire?
        double angle = Math.acos(dotProduct);
        double maxAngle = Math.toRadians(14); // ~14 degrees = 50 blocks at 200 distance
        if (angle > maxAngle) {
            return false;
        }
        
        return true;
    }
    
    // Try to fire a specific cannon at a target position
    private static boolean tryFireCannonAtTarget(Level level, BlockPos cannonPos, BlockState cannonState, Player player, Vec3 targetVec) {
        Direction facing = cannonState.getValue(ModBlocks.CannonControllerBlock.FACING);
        
        // Calculate cannon firing position (pos5 - front top)
        BlockPos middle = cannonPos;
        BlockPos front = middle.relative(facing);
        BlockPos frontTop = front.above();
        Vec3 cannonVec = Vec3.atCenterOf(frontTop);
        
        // Get cannon facing direction
        Vec3 cannonDir = Vec3.atLowerCornerOf(facing.getNormal());
        
        // Check 1: Is target in front of cannon?
        Vec3 toTarget = targetVec.subtract(cannonVec).normalize();
        double dotProduct = cannonDir.dot(toTarget);
        if (dotProduct < 0.5) { // Must be roughly in front (within ~60 degrees)
            return false;
        }
        
        // Check 2: Is target within range?
        double distance = cannonVec.distanceTo(targetVec);
        if (distance < 2 || distance > 200) {
            return false;
        }
        
        // Check 3: Is target within cone of fire?
        // Calculate angle between cannon direction and target direction
        double angle = Math.acos(dotProduct);
        double maxAngle = Math.toRadians(14); // ~14 degrees = 50 blocks at 200 distance
        if (angle > maxAngle) {
            return false;
        }
        
        // All checks passed - fire!
        Vec3 spawnPos = new Vec3(
            frontTop.getX() + 0.5 + cannonDir.x * 1.5,
            frontTop.getY() + 0.5 + cannonDir.y * 1.5,
            frontTop.getZ() + 0.5 + cannonDir.z * 1.5
        );
        
        // Calculate direction to target
        Vec3 fireDirection = targetVec.subtract(cannonVec).normalize();
        double speed = 0.5;
        
        LargeFireball fireball = new LargeFireball(
            level,
            player,
            fireDirection.x * speed,
            fireDirection.y * speed,
            fireDirection.z * speed,
            0  // explosion power - 0 means no damage to blocks
        );
        
        fireball.setPos(spawnPos);
        level.addFreshEntity(fireball);
        
        return true;
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    @SubscribeEvent
    public void addCreative(BuildCreativeModeTabContentsEvent event) {
        // check event API in your Forge version; this is an example
        if (event.getTabKey() != null && event.getTabKey().location().getPath().equals("islandcraft")) {
            event.accept(ModItems.ICBF_CANNON.get());
        }
    }
}

// New ModBlocks class to register cannon blocks with multiblock destruction
class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, IcbfCannons.MODID);

    public static final RegistryObject<Block> CANNON_CONTROLLER = BLOCKS.register("cannon_controller",
            () -> new CannonControllerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> CANNON_PART = BLOCKS.register("cannon_part",
            () -> new CannonPartBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));
    
    public static final RegistryObject<Block> CANNON_BARRIER = BLOCKS.register("cannon_barrier",
            () -> new CannonBarrierBlock(BlockBehaviour.Properties.copy(Blocks.BARRIER).noCollission().noOcclusion()));

    // Custom block for cannon controller - destroys all parts when broken
    public static class CannonControllerBlock extends Block implements EntityBlock {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        
        public CannonControllerBlock(BlockBehaviour.Properties props) {
            super(props);
            this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
        }
        
        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }
        
        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }
        
        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ModBlockEntities.CannonBlockEntity(pos, state);
        }
        
        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            // BlockEntity is automatically created when block is placed
        }
        
        @Override
        public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
            // Prevent blocks from being placed next to the controller (pos1)
            return false;
        }
        
        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!level.isClientSide) {
                // Fire a fireball from the front of the cannon
                Direction facing = state.getValue(FACING);
                
                // Calculate front position (pos5 - front top of cannon)
                BlockPos middle = pos;  // Controller is at middle
                BlockPos front = middle.relative(facing);
                BlockPos frontTop = front.above();  // pos5
                
                // Spawn fireball OUTSIDE the model - 1.5 blocks past the front face
                Vec3 direction = Vec3.atLowerCornerOf(facing.getNormal());
                Vec3 spawnPos = new Vec3(
                    frontTop.getX() + 0.5 + direction.x * 1.5,
                    frontTop.getY() + 0.5 + direction.y * 1.5,
                    frontTop.getZ() + 0.5 + direction.z * 1.5
                );
                
                // Set fireball velocity
                double speed = 0.5;
                
                LargeFireball fireball = new LargeFireball(
                    level,
                    player,
                    direction.x * speed,
                    direction.y * speed,
                    direction.z * speed,
                    0  // explosion power - 0 means no damage to blocks
                );
                
                fireball.setPos(spawnPos);
                level.addFreshEntity(fireball);
                
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Cannon fired!"),
                    true
                );
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        
        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
            if (!state.is(newState.getBlock()) && !level.isClientSide) {
                destroyMultiblock(level, pos, state.getValue(FACING));
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
        
        private void destroyMultiblock(Level level, BlockPos controllerPos, Direction facing) {
            // Destroy the 3x1x2 multiblock structure based on facing direction
            // Controller is at middle position, structure extends backward and forward
            BlockPos back = controllerPos.relative(facing.getOpposite());    // Back of cannon
            BlockPos middle = controllerPos;                                  // Middle (controller)
            BlockPos front = controllerPos.relative(facing);                  // Front of cannon
            
            List<BlockPos> positions = new ArrayList<>();
            positions.add(back);                    // [0] back bottom
            positions.add(middle);                  // [1] middle bottom (controller)
            positions.add(front);                   // [2] front bottom
            positions.add(back.above());            // [3] back top
            positions.add(middle.above());          // [4] middle top
            positions.add(front.above());           // [5] front top
            
            for (BlockPos targetPos : positions) {
                level.destroyBlock(targetPos, false);
            }
            
            // Destroy left and right barrier blocks (silently, without triggering their onRemove)
            Direction leftDir = facing.getCounterClockWise();
            Direction rightDir = facing.getClockWise();
            
            BlockPos leftBarrier = controllerPos.relative(leftDir);
            if (level.getBlockState(leftBarrier).is(CANNON_BARRIER.get())) {
                level.setBlock(leftBarrier, Blocks.AIR.defaultBlockState(), 3); // Silent removal
            }
            
            BlockPos rightBarrier = controllerPos.relative(rightDir);
            if (level.getBlockState(rightBarrier).is(CANNON_BARRIER.get())) {
                level.setBlock(rightBarrier, Blocks.AIR.defaultBlockState(), 3); // Silent removal
            }
        }
    }
    
    // Custom block for cannon parts - destroys entire multiblock when broken
    public static class CannonPartBlock extends Block {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        
        public CannonPartBlock(BlockBehaviour.Properties props) {
            super(props);
            this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
        }
        
        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }
        
        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }
        
        @Override
        public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
            // Check if this is pos0 (back) by checking if there's a controller in front
            Direction facing = state.getValue(FACING);
            BlockPos nextPos = context.getClickedPos().relative(facing);
            BlockState nextState = context.getLevel().getBlockState(nextPos);
            
            // If there's a controller in the facing direction, this is pos0 - prevent replacement
            if (nextState.is(CANNON_CONTROLLER.get())) {
                return false;
            }
            
            return super.canBeReplaced(state, context);
        }
        
        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
            if (!state.is(newState.getBlock()) && !level.isClientSide) {
                findAndDestroyController(level, pos, state.getValue(FACING));
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
        
        private void findAndDestroyController(Level level, BlockPos partPos, Direction facing) {
            // Search for the controller - it's always at the middle position
            // Part could be at: back, front, or any of the top positions
            List<BlockPos> possibleControllers = new ArrayList<>();
            
            // Check current and adjacent positions in the facing direction
            possibleControllers.add(partPos);                               // if this part IS the controller somehow
            possibleControllers.add(partPos.relative(facing));              // if this is back, controller is forward
            possibleControllers.add(partPos.relative(facing.getOpposite())); // if this is front, controller is backward
            possibleControllers.add(partPos.below());                       // if this is top middle
            possibleControllers.add(partPos.below().relative(facing));      // if this is top back
            possibleControllers.add(partPos.below().relative(facing.getOpposite())); // if this is top front
            
            for (BlockPos checkPos : possibleControllers) {
                BlockState checkState = level.getBlockState(checkPos);
                if (checkState.is(CANNON_CONTROLLER.get()) && checkState.getValue(CannonControllerBlock.FACING) == facing) {
                    level.destroyBlock(checkPos, true);
                    return;
                }
            }
        }
    }
    
    // Invisible barrier block to prevent placement next to pos1
    public static class CannonBarrierBlock extends Block {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        
        public CannonBarrierBlock(BlockBehaviour.Properties props) {
            super(props);
            this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
        }
        
        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }
        
        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }
        
        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
            if (!state.is(newState.getBlock()) && !level.isClientSide) {
                // If barrier is destroyed by player, find and destroy the cannon controller
                Direction facing = state.getValue(FACING);
                
                // Check left and right for the controller
                Direction leftDir = facing.getCounterClockWise();
                Direction rightDir = facing.getClockWise();
                
                BlockPos leftPos = pos.relative(leftDir);
                BlockState leftState = level.getBlockState(leftPos);
                if (leftState.is(CANNON_CONTROLLER.get())) {
                    level.destroyBlock(leftPos, true);
                    super.onRemove(state, level, pos, newState, isMoving);
                    return;
                }
                
                BlockPos rightPos = pos.relative(rightDir);
                BlockState rightState = level.getBlockState(rightPos);
                if (rightState.is(CANNON_CONTROLLER.get())) {
                    level.destroyBlock(rightPos, true);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}

// BlockEntity registry and implementation
class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, IcbfCannons.MODID);

    public static final RegistryObject<BlockEntityType<CannonBlockEntity>> CANNON_BE = BLOCK_ENTITIES.register(
            "cannon_block_entity",
            () -> BlockEntityType.Builder.of(
                    CannonBlockEntity::new,
                    ModBlocks.CANNON_CONTROLLER.get()
            ).build(null)
    );

    // Cannon BlockEntity - stores multiblock data
    public static class CannonBlockEntity extends BlockEntity {

        public CannonBlockEntity(BlockPos pos, BlockState state) {
            super(CANNON_BE.get(), pos, state);
        }

        // Save data to NBT
        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            // Future: Add any data you need to save here
        }

        // Load data from NBT
        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            // Future: Add any data you need to load here
        }
    }
}

// New ModItems class to register cannon item
class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, IcbfCannons.MODID);

    public static final RegistryObject<Item> ICBF_CANNON = ITEMS.register("icbf_cannon",
            () -> new CannonItem(new Item.Properties()));

    // CannonItem: handles right-click placement of the multi-block structure
    public static class CannonItem extends Item {
        public CannonItem(Properties properties) { super(properties); }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            if (level.isClientSide) return InteractionResult.sidedSuccess(true);

            Player player = context.getPlayer();
            if (player == null) return InteractionResult.PASS;

            Direction face = context.getClickedFace();
            BlockPos clickedPos = context.getClickedPos().relative(face);
            
            // Determine facing direction from player's look direction
            Direction facing = player.getDirection();
            
            // Calculate positions based on facing direction
            // Anchor is now pos0 (back of cannon) instead of pos1 (middle)
            List<BlockPos> positions = getCannonPositions(clickedPos, facing);

            // Check all positions are clear
            for (BlockPos p : positions) {
                BlockState s = level.getBlockState(p);
                if (!(s.isAir() || s.canBeReplaced())) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("This cannon needs more room"), true);
                    return InteractionResult.FAIL;
                }
                if (!level.getEntities(null, net.minecraft.world.phys.AABB.unitCubeFromLowerCorner(new Vec3(p.getX(), p.getY(), p.getZ()))).isEmpty()) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("This cannon needs more room"), true);
                    return InteractionResult.FAIL;
                }
            }
            
            // Check that blocks adjacent to pos0 are clear (for AO prevention)
            BlockPos pos0 = positions.get(0);  // back bottom
            BlockPos pos1 = positions.get(1);  // middle bottom (controller)
            
            // Check all 4 horizontal directions around pos0
            Direction[] horizontalDirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            for (Direction dir : horizontalDirs) {
                if (dir == facing || dir == facing.getOpposite()) continue; // Skip facing direction
                
                // Check pos0 adjacent blocks
                BlockPos pos0Adjacent = pos0.relative(dir);
                if (!level.getBlockState(pos0Adjacent).isAir()) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("This cannon needs more room"), true);
                    return InteractionResult.FAIL;
                }
            }
            
            // Check left and right of pos1 are clear (we'll place barriers there)
            Direction leftDir = facing.getCounterClockWise();
            Direction rightDir = facing.getClockWise();
            
            BlockPos leftBarrierPos = pos1.relative(leftDir);
            BlockState leftState = level.getBlockState(leftBarrierPos);
            if (!(leftState.isAir() || leftState.canBeReplaced())) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("This cannon needs more room"), true);
                return InteractionResult.FAIL;
            }
            
            BlockPos rightBarrierPos = pos1.relative(rightDir);
            BlockState rightState = level.getBlockState(rightBarrierPos);
            if (!(rightState.isAir() || rightState.canBeReplaced())) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("This cannon needs more room"), true);
                return InteractionResult.FAIL;
            }

            ServerLevel slevel = (ServerLevel) level;
            Block controller = ModBlocks.CANNON_CONTROLLER.get();
            Block part = ModBlocks.CANNON_PART.get();

            // Place controller at position 1 (middle block where model origin is)
            slevel.setBlock(positions.get(1), controller.defaultBlockState().setValue(ModBlocks.CannonControllerBlock.FACING, facing), 3);
            
            // Place parts at all other positions
            for (int i = 0; i < positions.size(); i++) {
                if (i != 1) {  // Skip controller position
                    slevel.setBlock(positions.get(i), part.defaultBlockState().setValue(ModBlocks.CannonPartBlock.FACING, facing), 3);
                }
            }
            
            // Place invisible barrier blocks to left and right of pos1 (controller)
            BlockPos controllerPos = positions.get(1);
            BlockPos leftBarrier = controllerPos.relative(leftDir);
            slevel.setBlock(leftBarrier, ModBlocks.CANNON_BARRIER.get().defaultBlockState().setValue(ModBlocks.CannonBarrierBlock.FACING, facing), 3);
            
            BlockPos rightBarrier = controllerPos.relative(rightDir);
            slevel.setBlock(rightBarrier, ModBlocks.CANNON_BARRIER.get().defaultBlockState().setValue(ModBlocks.CannonBarrierBlock.FACING, facing), 3);

            if (!player.isCreative()) {
                context.getItemInHand().shrink(1);
            }

            return InteractionResult.sidedSuccess(false);
        }

        /**
         * Calculate cannon block positions based on anchor (pos0 - back of cannon) and facing direction
         * The multiblock is 3 blocks long in the facing direction, 1 block wide, 2 blocks tall
         * 
         * @param anchor The clicked position (becomes pos0 - back of cannon)
         * @param facing The direction the cannon faces (player's direction)
         * @return List of 6 BlockPos: [0]=back-bottom, [1]=middle-bottom(controller), [2]=front-bottom,
         *                              [3]=back-top, [4]=middle-top, [5]=front-top
         */
        private static List<BlockPos> getCannonPositions(BlockPos anchor, Direction facing) {
            List<BlockPos> positions = new ArrayList<>();
            
            // anchor is now pos0 (back of cannon)
            // We need to extend forward in the facing direction
            BlockPos back = anchor;                                    // [0] Back bottom (anchor)
            BlockPos middle = back.relative(facing);                   // [1] Middle bottom (controller) - model origin
            BlockPos front = middle.relative(facing);                  // [2] Front bottom
            
            positions.add(back);                                       // [0] Back bottom
            positions.add(middle);                                     // [1] Middle bottom (controller)
            positions.add(front);                                      // [2] Front bottom
            positions.add(back.above());                               // [3] Back top
            positions.add(middle.above());                             // [4] Middle top
            positions.add(front.above());                              // [5] Front top
            
            return positions;
        }
    }
}
