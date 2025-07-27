package com.zidiansyncs.happyhaulers.mixin;

import com.zidiansyncs.happyhaulers.util.mixin.IEnhancedHappyGhastMixin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.HappyGhast;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Mixin target: Vanilla HappyGhast class
// This allows us to add Enhanced Happy Ghast functionality to existing vanilla entities
@Mixin(HappyGhast.class)
public class EnhancedHappyGhastMixin implements IEnhancedHappyGhastMixin {

    // Data storage for Enhanced Happy Ghast features
    // These variables store the custom data for each ghast entity
    @Unique private String ehg$spawnBiome = "minecraft:plains";        // Which biome this ghast spawned in (determines texture)
    @Unique private boolean ehg$hasRpgName = false;                     // True if named "rpg" (gets special texture)
    @Unique private boolean ehg$isBeingRidden = false;                  // True if a player is riding this ghast
    @Unique private boolean ehg$biomeDetected = false;                  // True when biome detection is finished

    // Initialize data when a new Happy Ghast is created
    // This runs once when the entity is first spawned
    @Inject(method = "<init>", at = @At("TAIL"))
    private void ehg$testInit(EntityType<? extends HappyGhast> entityType, Level level, CallbackInfo ci) {
        // Set starting values for our custom data
        ehg$spawnBiome = "minecraft:plains";        // Start with plains biome as default
        ehg$biomeDetected = false;                  // Mark that we haven't detected the biome yet
    }

    // PERFORMANCE OPTIMIZED: Main update method that runs every game tick (20 times per second)
    // This handles biome detection and RPG name checking with minimal overhead
    @Inject(method = "tick", at = @At("HEAD"))
    private void ehg$testTick(CallbackInfo ci) {
        HappyGhast ghast = (HappyGhast)(Object)this;

        // OPTIMIZATION: Only run biome detection if not already detected and within first 10 ticks
        if (!ehg$biomeDetected && ghast.tickCount < 10) {
            // BOTH SIDES: Detect what biome this ghast spawned in
            // Client needs this for texture rendering, server for consistency
            net.minecraft.core.BlockPos spawnPos = ghast.blockPosition();

            if (!ghast.level().isClientSide) {
                // SERVER SIDE: Use ServerLevel for more reliable detection
                if (ghast.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biomeKey =
                        serverLevel.getBiome(spawnPos).unwrapKey().orElse(null);
                    if (biomeKey != null) {
                        String detectedBiome = biomeKey.location().toString();
                        ehg$spawnBiome = detectedBiome;
                        ehg$biomeDetected = true;
                    } else {
                        ehg$spawnBiome = "minecraft:plains";
                        ehg$biomeDetected = true;
                    }
                }
            } else {
                // CLIENT SIDE: Use client level for texture rendering
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biomeKey =
                    ghast.level().getBiome(spawnPos).unwrapKey().orElse(null);
                if (biomeKey != null) {
                    String detectedBiome = biomeKey.location().toString();
                    ehg$spawnBiome = detectedBiome;
                    ehg$biomeDetected = true;
                } else {
                    ehg$spawnBiome = "minecraft:plains";
                    ehg$biomeDetected = true;
                }
            }
        }

        // OPTIMIZATION: Safety check with early exit if already detected
        if (!ehg$biomeDetected && ghast.tickCount > 20) {
            ehg$spawnBiome = "minecraft:plains";
            ehg$biomeDetected = true;
        }

        // OPTIMIZATION: RPG NAME DETECTION - Reduced frequency to every 60 ticks (3 seconds)
        // This reduces CPU usage while still being responsive enough for gameplay
        if (ghast.tickCount % 60 == 0) {
            if (ghast.hasCustomName()) {
                String nameString = ghast.getCustomName().getString();
                // Check if the name is "rpg" (ignoring uppercase/lowercase)
                boolean newRpgStatus = nameString.toLowerCase().equals("rpg");
                // Update our stored RPG status if it changed
                if (newRpgStatus != ehg$hasRpgName) {
                    ehg$hasRpgName = newRpgStatus;
                }
            } else if (ehg$hasRpgName) {
                // The ghast had an RPG name but now has no name - clear the RPG status
                ehg$hasRpgName = false;
            }
        }
    }

    // Detect RPG names immediately when a ghast is named
    // This runs whenever someone uses a name tag on the ghast
    @Inject(method = "setCustomName*", at = @At("TAIL"))
    private void ehg$testSetCustomName(net.minecraft.network.chat.Component name, CallbackInfo ci) {
        // Check if the new name is "rpg" for special texture
        if (name != null) {
            String nameString = name.getString();
            // Check if it's "rpg" (ignoring uppercase/lowercase)
            ehg$hasRpgName = nameString.toLowerCase().equals("rpg");
            // Log when we detect an RPG name (for debugging)
            if (ehg$hasRpgName) {
                System.out.println("Enhanced Happy Ghast RPG name detected: " + nameString);
            }
        } else {
            // No name was set - clear the RPG status
            ehg$hasRpgName = false;
        }
    }

    // INTERFACE METHODS: Allow other parts of the mod to access our custom data
    // These methods let the renderer get the biome and RPG status for texture selection

    // Get the biome this ghast spawned in (like "minecraft:forest")
    @Override
    public String ehg$getSpawnBiome() {
        return ehg$spawnBiome;
    }

    // Set the biome this ghast spawned in
    @Override
    public void ehg$setSpawnBiome(String biome) {
        ehg$spawnBiome = biome != null ? biome : "minecraft:plains";
    }

    // Check if this ghast is named "rpg" for special texture
    @Override
    public boolean ehg$hasRpgName() {
        return ehg$hasRpgName;
    }

    // Set whether this ghast has an RPG name
    @Override
    public void ehg$setHasRpgName(boolean hasRpgName) {
        ehg$hasRpgName = hasRpgName;
    }

    // Check if a player is riding this ghast (for future features)
    @Override
    public boolean ehg$isBeingRidden() {
        return ehg$isBeingRidden;
    }

    // Set whether a player is riding this ghast
    @Override
    public void ehg$setBeingRidden(boolean beingRidden) {
        ehg$isBeingRidden = beingRidden;
    }


}
