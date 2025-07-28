package com.zidiansyncs.happyhaulers.mixin;

import com.zidiansyncs.happyhaulers.util.mixin.IEnhancedHappyGhastMixin;
import net.minecraft.nbt.CompoundTag;
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
    @Unique private boolean ehg$hasExcelsiesName = false;               // True if named "excelsies" (gets special texture)
    @Unique private boolean ehg$isBeingRidden = false;                  // True if a player is riding this ghast
    @Unique private boolean ehg$biomeDetected = false;                  // True when biome detection is finished

    // Initialize data when a new Happy Ghast is created
    // This runs once when the entity is first spawned
    @Inject(method = "<init>", at = @At("TAIL"))
    private void ehg$testInit(EntityType<? extends HappyGhast> entityType, Level level, CallbackInfo ci) {
        // Set starting values for our custom data
        ehg$spawnBiome = "minecraft:plains";        // Start with plains biome as default
        ehg$biomeDetected = false;                  // Mark that we haven't detected the biome yet

        // Try to load biome from persistent data (for persistence)
        ehg$loadBiomeFromPersistentData();
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

        // PERSISTENCE: Save biome data using persistent data when first detected
        if (ehg$biomeDetected && ghast.tickCount == 15) {
            ehg$saveBiomeToPersistentData();
        }

        // OPTIMIZATION: SPECIAL NAME DETECTION - Reduced frequency to every 60 ticks (3 seconds)
        // This reduces CPU usage while still being responsive enough for gameplay
        if (ghast.tickCount % 60 == 0) {
            if (ghast.hasCustomName()) {
                String nameString = ghast.getCustomName().getString().toLowerCase();

                // Check for "rpg" name (ignoring uppercase/lowercase)
                boolean newRpgStatus = nameString.equals("rpg");
                if (newRpgStatus != ehg$hasRpgName) {
                    ehg$hasRpgName = newRpgStatus;
                    ehg$saveBiomeToPersistentData(); // Save when name status changes
                }

                // Check for "excelsies" name (ignoring uppercase/lowercase)
                boolean newExcelsiesStatus = nameString.equals("excelsies");
                if (newExcelsiesStatus != ehg$hasExcelsiesName) {
                    ehg$hasExcelsiesName = newExcelsiesStatus;
                    ehg$saveBiomeToPersistentData(); // Save when name status changes
                }
            } else {
                // The ghast has no name - clear both special name statuses
                boolean needsSave = false;
                if (ehg$hasRpgName) {
                    ehg$hasRpgName = false;
                    needsSave = true;
                }
                if (ehg$hasExcelsiesName) {
                    ehg$hasExcelsiesName = false;
                    needsSave = true;
                }
                if (needsSave) {
                    ehg$saveBiomeToPersistentData(); // Save when name status changes
                }
            }
        }
    }

    // Detect special names immediately when a ghast is named
    // This runs whenever someone uses a name tag on the ghast
    @Inject(method = "setCustomName*", at = @At("TAIL"))
    private void ehg$testSetCustomName(net.minecraft.network.chat.Component name, CallbackInfo ci) {
        // Check if the new name is a special name for texture override
        if (name != null) {
            String nameString = name.getString().toLowerCase();

            // Check for "rpg" name
            ehg$hasRpgName = nameString.equals("rpg");
            if (ehg$hasRpgName) {
                System.out.println("Enhanced Happy Ghast RPG name detected: " + name.getString());
            }

            // Check for "excelsies" name
            ehg$hasExcelsiesName = nameString.equals("excelsies");
            if (ehg$hasExcelsiesName) {
                System.out.println("Enhanced Happy Ghast Excelsies name detected: " + name.getString());
            }

            // Save the name status immediately
            ehg$saveBiomeToPersistentData();
        } else {
            // No name was set - clear both special name statuses
            ehg$hasRpgName = false;
            ehg$hasExcelsiesName = false;
            // Save the cleared name status
            ehg$saveBiomeToPersistentData();
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

    // Check if this ghast is named "excelsies" for special texture
    @Override
    public boolean ehg$hasExcelsiesName() {
        return ehg$hasExcelsiesName;
    }

    // Set whether this ghast has an Excelsies name
    @Override
    public void ehg$setHasExcelsiesName(boolean hasExcelsiesName) {
        ehg$hasExcelsiesName = hasExcelsiesName;
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

    // PROPER NBT PERSISTENCE: Use entity's persistent data capability
    // This approach uses the entity's built-in persistent data system
    // which properly persists across world loads, dimension travel, and server restarts

    private void ehg$saveBiomeToPersistentData() {
        HappyGhast ghast = (HappyGhast)(Object)this;

        // Save biome data to the entity's persistent data
        if (ehg$biomeDetected && !ehg$spawnBiome.equals("minecraft:plains")) {
            CompoundTag persistentData = ghast.getPersistentData();
            persistentData.putString("ehg_spawn_biome", ehg$spawnBiome);
            persistentData.putBoolean("ehg_biome_detected", ehg$biomeDetected);
            persistentData.putBoolean("ehg_has_rpg_name", ehg$hasRpgName);
            persistentData.putBoolean("ehg_has_excelsies_name", ehg$hasExcelsiesName);
            persistentData.putBoolean("ehg_is_being_ridden", ehg$isBeingRidden);
        }
    }

    private void ehg$loadBiomeFromPersistentData() {
        HappyGhast ghast = (HappyGhast)(Object)this;

        // Load biome data from the entity's persistent data
        CompoundTag persistentData = ghast.getPersistentData();
        if (persistentData.contains("ehg_spawn_biome")) {
            ehg$spawnBiome = persistentData.getString("ehg_spawn_biome").orElse("minecraft:plains");
            ehg$biomeDetected = persistentData.getBoolean("ehg_biome_detected").orElse(false);
            ehg$hasRpgName = persistentData.getBoolean("ehg_has_rpg_name").orElse(false);
            ehg$hasExcelsiesName = persistentData.getBoolean("ehg_has_excelsies_name").orElse(false);
            ehg$isBeingRidden = persistentData.getBoolean("ehg_is_being_ridden").orElse(false);
        }
    }

}
