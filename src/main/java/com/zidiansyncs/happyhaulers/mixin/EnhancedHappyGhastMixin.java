package com.zidiansyncs.happyhaulers.mixin;

import com.zidiansyncs.happyhaulers.texture.HappyGhastTextureManager;
import com.zidiansyncs.happyhaulers.util.mixin.IEnhancedHappyGhastMixin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.HappyGhast;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

// Mixin target: Vanilla HappyGhast class
// This allows us to add Enhanced Happy Ghast functionality to existing vanilla entities
@Mixin(HappyGhast.class)
public class EnhancedHappyGhastMixin implements IEnhancedHappyGhastMixin {

    // ===== DATA STORAGE FOR ENHANCED HAPPY GHAST FEATURES =====
    // These variables store the custom data for each ghast entity instance
    // All variables are marked @Unique to prevent conflicts with vanilla code

    @Unique private String ehg$spawnBiome = "minecraft:plains";        // Which biome this ghast spawned in (determines texture)
    @Unique private boolean ehg$hasRpgName = false;                     // True if named "rpg" (gets special RPG texture)
    @Unique private boolean ehg$hasExcelsiesName = false;               // True if named "excelsies" (gets special Excelsies texture)
    @Unique private boolean ehg$isBeingRidden = false;                  // True if a player is riding this ghast
    @Unique private boolean ehg$biomeDetected = false;                  // True when biome detection is finished (prevents re-detection)
    @Unique private boolean ehg$hasLockedVariant = false;               // True when this ghast has a locked texture variant (prevents biome detection)

    // Initialize data when a new Happy Ghast is created
    // This runs once when the entity is first spawned
    @Inject(method = "<init>", at = @At("TAIL"))
    private void ehg$testInit(EntityType<? extends HappyGhast> entityType, Level level, CallbackInfo ci) {
        // Set starting values for our custom data
        ehg$spawnBiome = "minecraft:plains";        // Start with plains biome as default
        ehg$biomeDetected = false;                  // Mark that we haven't detected the biome yet
        ehg$hasLockedVariant = false;               // Start with no locked variant

        HappyGhast ghast = (HappyGhast)(Object)this;

        // AGGRESSIVE: Try to force-load texture variant first (highest priority)
        if (HappyGhastTextureManager.forceLoadTextureVariant(ghast.getUUID())) {
            // Successfully loaded locked variant - apply it immediately
            HappyGhastTextureManager.HappyGhastTextureVariant variant =
                HappyGhastTextureManager.getTextureVariant(ghast.getUUID());
            if (variant != null && variant.isLocked) {
                ehg$spawnBiome = variant.spawnBiome;
                ehg$hasRpgName = variant.hasRpgName;
                ehg$hasExcelsiesName = variant.hasExcelsiesName;
                ehg$biomeDetected = true;
                ehg$hasLockedVariant = true;

                // CRITICAL: Ensure client-side sync immediately
                HappyGhastTextureManager.syncToClient(ghast.getUUID(), variant);

                // CRITICAL: Save to persistent data immediately to ensure survival
                ehg$saveBiomeToPersistentData();

                System.out.println("HappyHaulers: INIT - Force-loaded locked variant for ghast " + ghast.getUUID() +
                                 " in dimension " + level.dimension().location() +
                                 " - variant: " + variant.getEffectiveVariant() + " (LOCKED & SYNCED)");
                return; // Skip other loading methods
            }
        }

        // Try to load biome from persistent data (for backward compatibility)
        ehg$loadBiomeFromPersistentData();

        // Try to load from world data system (new persistence method)
        ehg$loadFromWorldData();
    }

    // ===== MAIN TICK METHOD - RUNS EVERY GAME TICK (20 TIMES PER SECOND) =====
    // This is the core method that handles biome detection and name checking
    // Performance optimized to minimize CPU usage with large numbers of ghasts
    @Inject(method = "tick", at = @At("HEAD"))
    private void ehg$testTick(CallbackInfo ci) {
        HappyGhast ghast = (HappyGhast)(Object)this;

        // LOCKED VARIANT OPTIMIZATION: Skip expensive biome detection for ghasts with locked variants
        // This prevents unnecessary processing and protects against texture changes during dimension travel
        if (ehg$hasLockedVariant) {
            // Periodic client sync to ensure texture variants are available for rendering
            // Reduced frequency (every 10 seconds) to prevent log spam while maintaining reliability
            if (ghast.tickCount % 200 == 0) { // Every 10 seconds
                HappyGhastTextureManager.forceSyncToClient(ghast.getUUID());
            }
            // Skip biome detection but continue to name detection below to allow dynamic name changes
        } else {

        // DIMENSION-BASED TEXTURE ASSIGNMENT
        // Handle different dimensions with appropriate texture assignment logic
        if (!ghast.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            // Check if we have an existing locked variant to restore (for dimension travelers)
            if (HappyGhastTextureManager.hasTextureVariant(ghast.getUUID())) {
                HappyGhastTextureManager.HappyGhastTextureVariant lockedVariant =
                    HappyGhastTextureManager.getTextureVariant(ghast.getUUID());
                if (lockedVariant != null && lockedVariant.isLocked) {
                    // Restore the existing locked variant (from previous dimension)
                    ehg$spawnBiome = lockedVariant.spawnBiome;
                    ehg$hasRpgName = lockedVariant.hasRpgName;
                    ehg$hasExcelsiesName = lockedVariant.hasExcelsiesName;
                    ehg$biomeDetected = true;
                    ehg$hasLockedVariant = true;
                    return;
                }
            }

            // NETHER SPAWN HANDLING: Assign default Happy Ghast texture for new Nether spawns
            if (ghast.level().dimension().equals(net.minecraft.world.level.Level.NETHER)) {
                ehg$spawnBiome = "minecraft:plains"; // Use plains key for default texture
                ehg$biomeDetected = true;
                // Continue to registration below
            }
            // END SPAWN HANDLING: Assign End Ghast texture for new End spawns
            else if (ghast.level().dimension().equals(net.minecraft.world.level.Level.END)) {
                ehg$spawnBiome = "minecraft:the_end"; // Use the_end key for End texture
                ehg$biomeDetected = true;
                // Continue to registration below
            } else {
                // Other custom dimensions - skip detection for now
                return;
            }
        }

        // OVERWORLD ONLY: Check if we already have a locked texture variant first (highest priority)
        if (!ehg$biomeDetected && ghast.tickCount < 10) {
            // First, try to load from world data system (prevents dimension travel texture reset)
            if (HappyGhastTextureManager.hasTextureVariant(ghast.getUUID())) {
                HappyGhastTextureManager.HappyGhastTextureVariant lockedVariant =
                    HappyGhastTextureManager.getTextureVariant(ghast.getUUID());
                if (lockedVariant != null && lockedVariant.isLocked) {
                    // Use the locked variant data - this prevents texture reset on dimension travel
                    ehg$spawnBiome = lockedVariant.spawnBiome;
                    ehg$hasRpgName = lockedVariant.hasRpgName;
                    ehg$hasExcelsiesName = lockedVariant.hasExcelsiesName;
                    ehg$biomeDetected = true;
                    ehg$hasLockedVariant = true; // Mark as having locked variant to prevent future detection

                    System.out.println("HappyHaulers: Restored locked texture variant for ghast " + ghast.getUUID() +
                                     " in Overworld - variant: " + lockedVariant.getEffectiveVariant() + " (LOCKED)");
                    return; // Skip biome detection since we have locked data
                }
            }

            // OVERWORLD ONLY: Detect what biome this ghast spawned in (only if no locked variant exists)
            // This code only runs in the Overworld to prevent creating variants in Nether/End
            // Client needs this for texture rendering, server for consistency
            net.minecraft.core.BlockPos spawnPos = ghast.blockPosition();

            if (!ghast.level().isClientSide) {
                // SERVER SIDE: Use ServerLevel for more reliable detection
                if (ghast.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biomeKey =
                        serverLevel.getBiome(spawnPos).unwrapKey().orElse(null);
                    if (biomeKey != null) {
                        String detectedBiome = biomeKey.location().toString();

                        // PRIORITY 1: Special names override ALL biome restrictions
                        if (ehg$hasRpgName || ehg$hasExcelsiesName) {
                            ehg$spawnBiome = detectedBiome; // Use any biome for special names
                            ehg$biomeDetected = true;
                        }
                        // PRIORITY 2: Regular biome validation for non-special ghasts
                        else if (ehg$isSupportedBiome(detectedBiome)) {
                            ehg$spawnBiome = detectedBiome;
                            ehg$biomeDetected = true;
                        } else {
                            ehg$spawnBiome = "minecraft:plains";
                            ehg$biomeDetected = true;
                        }
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

                    // PRIORITY 1: Special names override ALL biome restrictions
                    if (ehg$hasRpgName || ehg$hasExcelsiesName) {
                        ehg$spawnBiome = detectedBiome; // Use any biome for special names
                        ehg$biomeDetected = true;
                    }
                    // PRIORITY 2: Regular biome validation for non-special ghasts
                    else if (ehg$isSupportedBiome(detectedBiome)) {
                        ehg$spawnBiome = detectedBiome;
                        ehg$biomeDetected = true;
                    } else {
                        ehg$spawnBiome = "minecraft:plains";
                        ehg$biomeDetected = true;
                    }
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

        // PERSISTENCE: Save biome data using both persistent data and world data when first detected
        if (ehg$biomeDetected && ghast.tickCount == 15) {
            ehg$saveBiomeToPersistentData();

            // CRITICAL: Register ALL ghasts to prevent texture changes on reload/dimension travel
            // Supported biomes get their custom texture, unsupported biomes get locked plains texture
            ehg$registerWithWorldData();
        }
        } // End of biome detection else block

        // OPTIMIZATION: SPECIAL NAME DETECTION - Reduced frequency to every 60 ticks (3 seconds)
        // This runs for ALL ghasts (locked or not) to allow name changes after biome assignment
        if (ghast.tickCount % 60 == 0) {
            if (ghast.hasCustomName()) {
                String nameString = ghast.getCustomName().getString().toLowerCase();

                // Check for "rpg" name (ignoring uppercase/lowercase)
                boolean newRpgStatus = nameString.equals("rpg");
                if (newRpgStatus != ehg$hasRpgName) {
                    ehg$hasRpgName = newRpgStatus;
                    ehg$saveBiomeToPersistentData(); // Save when name status changes
                    ehg$updateWorldDataNames(); // Update world data system
                }

                // Check for "excelsies" name (ignoring uppercase/lowercase)
                boolean newExcelsiesStatus = nameString.equals("excelsies");
                if (newExcelsiesStatus != ehg$hasExcelsiesName) {
                    ehg$hasExcelsiesName = newExcelsiesStatus;
                    ehg$saveBiomeToPersistentData(); // Save when name status changes
                    ehg$updateWorldDataNames(); // Update world data system
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
                    ehg$updateWorldDataNames(); // Update world data system
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
            ehg$updateWorldDataNames();
        } else {
            // No name was set - clear both special name statuses
            ehg$hasRpgName = false;
            ehg$hasExcelsiesName = false;
            // Save the cleared name status
            ehg$saveBiomeToPersistentData();
            ehg$updateWorldDataNames();
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

    // Get the UUID of this ghast entity
    @Override
    public UUID ehg$getGhastId() {
        HappyGhast ghast = (HappyGhast)(Object)this;
        return ghast.getUUID();
    }

    // Set the UUID of this ghast entity (not used for entities, but required by interface)
    @Override
    public void ehg$setGhastId(UUID ghastId) {
        // Not applicable for entities - UUID is immutable
        // This method is primarily for render states
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
            persistentData.putBoolean("ehg_has_locked_variant", ehg$hasLockedVariant);
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
            ehg$hasLockedVariant = persistentData.getBoolean("ehg_has_locked_variant").orElse(false);
        }
    }

    // NEW WORLD DATA PERSISTENCE METHODS
    // These methods integrate with the HappyGhastTextureManager for persistent texture variants

    /**
     * Load texture variant from world data system (new persistence method)
     */
    private void ehg$loadFromWorldData() {
        HappyGhast ghast = (HappyGhast)(Object)this;

        // Check if we have a registered texture variant in the world data system
        // Try both server-side and client-side data
        HappyGhastTextureManager.HappyGhastTextureVariant variant = null;

        if (!ghast.level().isClientSide) {
            // Server-side: Check active texture variants
            variant = HappyGhastTextureManager.getTextureVariant(ghast.getUUID());
        } else {
            // Client-side: Check client texture variants for rendering
            variant = HappyGhastTextureManager.getClientTextureVariants().get(ghast.getUUID());
        }

        if (variant != null && variant.isLocked) {
            // Use the locked variant data - this overrides any other detection
            ehg$spawnBiome = variant.spawnBiome;
            ehg$hasRpgName = variant.hasRpgName;
            ehg$hasExcelsiesName = variant.hasExcelsiesName;
            ehg$biomeDetected = true; // Mark as detected since we have locked data
            ehg$hasLockedVariant = true; // Mark as having locked variant to prevent future detection

            System.out.println("HappyHaulers: Loaded locked texture variant for ghast " + ghast.getUUID() +
                             " on " + (ghast.level().isClientSide ? "client" : "server") +
                             " - variant: " + variant.getEffectiveVariant() + " (LOCKED)");
        }
    }

    /**
     * Register this ghast with the world data system when biome is first detected
     * REGISTERS IN OVERWORLD AND NETHER - allows default texture assignment for Nether spawns
     */
    private void ehg$registerWithWorldData() {
        HappyGhast ghast = (HappyGhast)(Object)this;

        // REGISTRATION RULES:
        // - Overworld: Always allow registration (biome-based or special names)
        // - Nether: Allow registration for new spawns (default texture) and special names
        // - End: Allow registration for new spawns (end texture) and special names
        // - Other dimensions: Only allow special names
        if (!ghast.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD) &&
            !ghast.level().dimension().equals(net.minecraft.world.level.Level.NETHER) &&
            !ghast.level().dimension().equals(net.minecraft.world.level.Level.END)) {
            if (ehg$hasRpgName || ehg$hasExcelsiesName) {
                // Allow special names in any dimension
            } else {
                // Skip registration in other custom dimensions for regular ghasts
                return;
            }
        }

        // Only register on server side and in Overworld
        if (!ghast.level().isClientSide && ghast.level() instanceof ServerLevel) {
            // Check if already registered to avoid overwriting locked variants
            if (!HappyGhastTextureManager.hasTextureVariant(ghast.getUUID())) {
                // Register the texture variant with the world data system
                HappyGhastTextureManager.registerTextureVariant(
                    ghast, ehg$spawnBiome, ehg$hasRpgName, ehg$hasExcelsiesName);

                // Mark this ghast as having a locked variant to prevent future biome detection
                ehg$hasLockedVariant = true;
            } else {
                // Already registered - ensure client sync for dimension travel
                HappyGhastTextureManager.HappyGhastTextureVariant existing =
                    HappyGhastTextureManager.getTextureVariant(ghast.getUUID());
                if (existing != null) {
                    // Mark this ghast as having a locked variant
                    ehg$hasLockedVariant = true;
                    ehg$spawnBiome = existing.spawnBiome;
                    ehg$hasRpgName = existing.hasRpgName;
                    ehg$hasExcelsiesName = existing.hasExcelsiesName;
                    ehg$biomeDetected = true;

                    HappyGhastTextureManager.syncToClient(ghast.getUUID(), existing);
                    System.out.println("HappyHaulers: Re-synced existing OVERWORLD texture variant for ghast " + ghast.getUUID() +
                                     " - variant: " + existing.getEffectiveVariant() + " (LOCKED)");
                }
            }
        }
    }

    /**
     * Update special name status in the world data system
     */
    private void ehg$updateWorldDataNames() {
        HappyGhast ghast = (HappyGhast)(Object)this;

        // Only update on server side
        if (!ghast.level().isClientSide && ghast.level() instanceof ServerLevel serverLevel) {
            HappyGhastTextureManager.updateSpecialNameStatus(
                ghast.getUUID(), ehg$hasRpgName, ehg$hasExcelsiesName, serverLevel);

            // CRITICAL: Force sync after name update
            HappyGhastTextureManager.forceSyncToClient(ghast.getUUID());

            System.out.println("HappyHaulers: Updated special names for ghast " + ghast.getUUID() +
                             " - RPG: " + ehg$hasRpgName + ", Excelsies: " + ehg$hasExcelsiesName +
                             " - locked variant: " + ehg$hasLockedVariant);
        }
    }

    /**
     * Check if a biome is supported for custom texture variants
     * Only major Overworld biomes are supported to prevent unwanted variants
     */
    @Unique
    private boolean ehg$isSupportedBiome(String biome) {
        // Major Overworld biomes that get custom textures
        return biome.equals("minecraft:desert") ||
               biome.equals("minecraft:forest") ||
               biome.equals("minecraft:ocean") ||
               biome.equals("minecraft:plains") ||
               biome.equals("minecraft:cold_ocean") ||
               biome.equals("minecraft:deep_cold_ocean") ||
               biome.equals("minecraft:deep_frozen_ocean") ||
               biome.equals("minecraft:frozen_ocean") ||
               biome.equals("minecraft:deep_ocean") ||
               biome.equals("minecraft:warm_ocean") ||
               biome.equals("minecraft:taiga") ||
               biome.equals("minecraft:swamp") ||
               biome.equals("minecraft:jungle") ||
               biome.equals("minecraft:lush_caves") ||
               biome.equals("minecraft:cherry_grove") ||
               biome.equals("minecraft:the_end") ||
               biome.equals("minecraft:snowy_plains");
    }

}
