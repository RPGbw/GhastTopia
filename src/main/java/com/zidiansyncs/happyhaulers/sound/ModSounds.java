package com.zidiansyncs.happyhaulers.sound;

import com.zidiansyncs.happyhaulers.HappyHaulers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.Holder;

/**
 * Registry for all custom sound events in the Happy Haulers mod
 */
public class ModSounds {
    
    // Create the deferred register for sound events
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
        DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, HappyHaulers.MODID);

    // Cloudstride Music Disc Sound Event - links to assets/happyhaulers/sounds/cloudstride.ogg
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DISC_CLOUDSTRIDE = SOUND_EVENTS.register(
        "music_disc_cloudstride", // Name must match the entry in sounds.json
        () -> SoundEvent.createVariableRangeEvent( // Variable range allows music to be heard from far away
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "music_disc_cloudstride")
        )
    );

    /**
     * Register all sound events with the event bus
     */
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
        HappyHaulers.LOGGER.info("Registering Happy Haulers sound events");
    }
}
