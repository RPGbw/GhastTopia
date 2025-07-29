package com.zidiansyncs.happyhaulers;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// Import our custom classes
import com.zidiansyncs.happyhaulers.sound.ModSounds;
import com.zidiansyncs.happyhaulers.command.MushroomTransformCommand;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(HappyHaulers.MODID)
public class HappyHaulers {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "happyhaulers";

    // Logger for debugging and info messages
    public static final Logger LOGGER = LogUtils.getLogger();

    // Registry for blocks (currently unused but ready for future blocks)
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);

    // Registry for items (holds our music disc item)
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    // Registry for creative mode tabs (organizes items in creative menu)
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Registry for jukebox songs (defines what music plays when disc is used)
    public static final DeferredRegister<JukeboxSong> JUKEBOX_SONGS = DeferredRegister.create(Registries.JUKEBOX_SONG, MODID);

    // Resource key that links our music disc item to its jukebox song data
    public static final ResourceKey<JukeboxSong> CLOUDSTRIDE_JUKEBOX_SONG_KEY = ResourceKey.create(Registries.JUKEBOX_SONG,
        ResourceLocation.fromNamespaceAndPath(MODID, "cloudstride"));

    // The jukebox song data - defines what happens when the disc plays in a jukebox
    public static final DeferredHolder<JukeboxSong, JukeboxSong> CLOUDSTRIDE_JUKEBOX_SONG = JUKEBOX_SONGS.register("cloudstride",
        () -> new JukeboxSong(
            ModSounds.MUSIC_DISC_CLOUDSTRIDE, // Which sound file to play
            Component.translatable("item.happyhaulers.music_disc_cloudstride.desc"), // Description text
            180.0F, // Song length in seconds (3 minutes)
            15)); // Redstone comparator output strength when playing

    // Our custom music disc item - the physical disc players can hold and use
    public static final DeferredItem<Item> MUSIC_DISC_CLOUDSTRIDE = ITEMS.register("music_disc_cloudstride", () ->
        new Item(new Item.Properties()
            .stacksTo(1) // Only one disc per stack
            .rarity(net.minecraft.world.item.Rarity.RARE) // Shows as rare (light blue) in tooltips
            .jukeboxPlayable(CLOUDSTRIDE_JUKEBOX_SONG_KEY) // Links to the song data above
            .setId(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "music_disc_cloudstride")))));

    // Creates a creative tab with the id "happyhaulers:happy_haulers_tab" for the example item, that is placed after the combat tab


    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public HappyHaulers(IEventBus modEventBus, ModContainer modContainer) {
        // Initialize Mixin configuration for Enhanced Happy Ghast texture variants
        System.setProperty("mixin.env.remapRefMap", "true");

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register all our deferred registries to the mod event bus
        BLOCKS.register(modEventBus); // Register blocks (currently unused)
        ITEMS.register(modEventBus); // Register items (our music disc)
        CREATIVE_MODE_TABS.register(modEventBus); // Register creative tabs
        JUKEBOX_SONGS.register(modEventBus); // Register jukebox songs (our music)
        ModSounds.register(modEventBus); // Register sound events (our music file)

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (happyhaulers) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

        // Add Cloudstride music disc to the Tools and Utilities tab (where music discs belong)
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MUSIC_DISC_CLOUDSTRIDE);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // Register debug commands
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MushroomTransformCommand.register(event.getDispatcher());
        LOGGER.info("Registered Happy Haulers debug commands");
    }
}
