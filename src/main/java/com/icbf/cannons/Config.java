package com.icbf.cannons;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = "icbfcannons", bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // Cannonball explosion settings
    private static final ForgeConfigSpec.DoubleValue EXPLOSION_POWER = BUILDER
            .comment("Explosion power for visual effects and knockback (0.0 - 10.0)")
            .defineInRange("cannonball.explosionPower", 1.0, 0.0, 10.0);

    private static final ForgeConfigSpec.DoubleValue BLOCK_BREAK_CHANCE = BUILDER
            .comment("Chance for each block to break from explosion (0.0 - 1.0, where 1.0 = 100%)")
            .defineInRange("cannonball.blockBreakChance", 0.25, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue ENTITY_DAMAGE_RADIUS = BUILDER
            .comment("Radius for entity damage in blocks")
            .defineInRange("cannonball.entityDamageRadius", 3.0, 0.0, 10.0);

    private static final ForgeConfigSpec.DoubleValue ENTITY_DAMAGE = BUILDER
            .comment("Damage dealt to entities (2.0 = 1 heart)")
            .defineInRange("cannonball.entityDamage", 10.0, 0.0, 100.0);

    private static final ForgeConfigSpec.BooleanValue FRIENDLY_FIRE = BUILDER
            .comment("Whether cannonballs can damage the shooter")
            .define("cannonball.friendlyFire", true);

    private static final ForgeConfigSpec.IntValue CANNON_COOLDOWN_TICKS = BUILDER
            .comment("Cooldown between cannon fires in ticks (20 ticks = 1 second, default 100 = 5 seconds)")
            .defineInRange("cannon.cooldownTicks", 100, 0, 6000);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    // Cannonball settings
    public static double explosionPower;
    public static double blockBreakChance;
    public static double entityDamageRadius;
    public static double entityDamage;
    public static boolean friendlyFire;

    // Cannon settings
    public static int cannonCooldownTicks;

    private static boolean validateItemName(final Object obj)
    {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)))
                .collect(Collectors.toSet());

        // Load cannonball settings
        explosionPower = EXPLOSION_POWER.get();
        blockBreakChance = BLOCK_BREAK_CHANCE.get();
        entityDamageRadius = ENTITY_DAMAGE_RADIUS.get();
        entityDamage = ENTITY_DAMAGE.get();
        friendlyFire = FRIENDLY_FIRE.get();

        // Load cannon settings
        cannonCooldownTicks = CANNON_COOLDOWN_TICKS.get();
    }
}
