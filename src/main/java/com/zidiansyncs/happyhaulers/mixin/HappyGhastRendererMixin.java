package com.zidiansyncs.happyhaulers.mixin;


import com.zidiansyncs.happyhaulers.HappyHaulers;
import com.zidiansyncs.happyhaulers.util.mixin.IEnhancedHappyGhastMixin;
import net.minecraft.client.renderer.entity.HappyGhastRenderer;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.HappyGhast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

// ===== HAPPY GHAST TEXTURE SYSTEM OVERVIEW =====
//
// TEXTURE STORAGE LOCATION:
// All textures are stored in: src/main/resources/assets/happyhaulers/textures/entity/ghast/
//
// TEXTURE TYPES:
// 1. Special Name Textures (Highest Priority):
//    - rpg_texture.png (for ghasts named "rpg")
//    - excelsies_texture.png (for ghasts named "excelsies")
//
// 2. Biome Variant Textures (Medium Priority):
//    - desert_ghast.png, forest_ghast.png, ocean_ghast.png, etc.
//    - Each biome gets its own unique texture
//
// 3. Default Texture (Lowest Priority):
//    - happy_ghast.png (fallback when no other texture applies)
//
// TEXTURE SELECTION PRIORITY:
// 1. RPG name ("rpg") → rpg_texture.png
// 2. Excelsies name ("excelsies") → excelsies_texture.png
// 3. Biome variant → [biome]_ghast.png
// 4. Default → happy_ghast.png
//
// PERSISTENCE:
// - Biome variants are saved using entity tags (ehg_biome_[biomename])
// - Special names are detected dynamically from entity custom names
// - All data persists across world loads and dimension travel
//
// Mixin target: Vanilla HappyGhastRenderer class
// This allows us to intercept texture selection without creating a custom renderer
@Mixin(HappyGhastRenderer.class)
public abstract class HappyGhastRendererMixin {
    // Shadow vanilla texture locations for fallback behavior
    @Shadow @Final private static ResourceLocation GHAST_BABY_LOCATION;
    @Shadow @Final private static ResourceLocation GHAST_LOCATION;

    // TEXTURE STORAGE EXPLANATION:
    // All textures are stored in: src/main/resources/assets/happyhaulers/textures/entity/ghast/
    // These ResourceLocation objects point to .png files in that directory
    // The mod ID "happyhaulers" tells Minecraft to look in our mod's assets folder

    // Default Enhanced Happy Ghast texture - used when no biome variant exists
    // File location: src/main/resources/assets/happyhaulers/textures/entity/ghast/happy_ghast.png
    @Unique private static final ResourceLocation DEFAULT_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/happy_ghast.png");

    // Special RPG texture - highest priority, used when entity is named "rpg" or "RPG"
    // File location: src/main/resources/assets/happyhaulers/textures/entity/ghast/rpg_texture.png
    @Unique private static final ResourceLocation RPG_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/rpg_texture.png");

    // Special Excelsies texture - highest priority, used when entity is named "excelsies"
    // File location: src/main/resources/assets/happyhaulers/textures/entity/ghast/excelsies_texture.png
    @Unique private static final ResourceLocation EXCELSIES_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/excelsies_texture.png");
    
    // BIOME TEXTURE STORAGE EXPLANATION:
    // All biome-specific textures are stored in the same directory as above
    // Each biome gets its own unique texture file with descriptive names
    // The HashMap maps Minecraft biome IDs to their corresponding texture files

    // PERFORMANCE OPTIMIZED: Biome-specific texture mappings with pre-computed ResourceLocations
    // Using HashMap for O(1) lookup performance with large numbers of ghasts
    @Unique private static final Map<String, ResourceLocation> BIOME_TEXTURES = new HashMap<>(16, 0.75f);

    static {
        // Initialize biome texture mappings - all ResourceLocations pre-computed at class load time
        // Each entry maps a Minecraft biome ID to its corresponding texture file

        // Desert biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/desert_ghast.png
        BIOME_TEXTURES.put("minecraft:desert",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/desert_ghast.png"));

        // Forest biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/forest_ghast.png
        BIOME_TEXTURES.put("minecraft:forest",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/forest_ghast.png"));

        // Ocean biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/ocean_ghast.png
        BIOME_TEXTURES.put("minecraft:ocean",
                ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/ocean_ghast.png"));

        // Warm Ocean biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/coral_ghast.png
        BIOME_TEXTURES.put("minecraft:warm_ocean",
                ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/coral_ghast.png"));

        // Taiga biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/taiga_ghast.png
        BIOME_TEXTURES.put("minecraft:taiga",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/taiga_ghast.png"));

        // Swamp biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/swamp_ghast.png
        BIOME_TEXTURES.put("minecraft:swamp",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/swamp_ghast.png"));

        // Jungle biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/jungle_ghast.png
        BIOME_TEXTURES.put("minecraft:jungle",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/jungle_ghast.png"));

        // Savanna biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/savanna_ghast.png
        BIOME_TEXTURES.put("minecraft:savanna",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/savanna_ghast.png"));

        // Badlands biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/badlands_ghast.png
        BIOME_TEXTURES.put("minecraft:badlands",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/badlands_ghast.png"));

        // Snowy Plains biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/ice_ghast.png
        BIOME_TEXTURES.put("minecraft:snowy_plains",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/ice_ghast.png"));

        // Ice Spikes biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/ice_ghast.png
        // Note: Ice Spikes uses the same texture as Snowy Plains (ice_ghast.png)
        BIOME_TEXTURES.put("minecraft:ice_spikes",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/ice_ghast.png"));

        // Cherry Grove biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/cherry_ghast.png
        BIOME_TEXTURES.put("minecraft:cherry_grove",
                ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/cherry_ghast.png"));

        // Lush Caves biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/lush_ghast.png
        BIOME_TEXTURES.put("minecraft:lush_caves",
                ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/lush_ghast.png"));

        // Mushroom Fields biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/mushroom_ghast.png
        BIOME_TEXTURES.put("minecraft:mushroom_fields",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/mushroom_ghast.png"));

        // The End biome texture - File: src/main/resources/assets/happyhaulers/textures/entity/ghast/end_ghast.png
        BIOME_TEXTURES.put("minecraft:the_end",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/end_ghast.png"));
    }

    // Main texture selection method - intercepts vanilla getTextureLocation to provide custom textures
    // This method implements the priority system: RPG name > Excelsies name > Biome variant > Default texture
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/HappyGhastRenderState;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void ehg$getTextureLocation(HappyGhastRenderState happyGhastRenderState, CallbackInfoReturnable<ResourceLocation> cir) {
        IEnhancedHappyGhastMixin renderStateMixin = (IEnhancedHappyGhastMixin) happyGhastRenderState;

        // Extract Enhanced Happy Ghast data from render state
        String spawnBiome = renderStateMixin.ehg$getSpawnBiome();
        boolean hasRpg = renderStateMixin.ehg$hasRpgName();
        boolean hasExcelsies = renderStateMixin.ehg$hasExcelsiesName();

        // Priority 1: RPG name takes precedence over everything else
        // When entity is named "rpg" or "RPG", always use the special RPG texture
        if (hasRpg) {
            cir.setReturnValue(RPG_TEXTURE);
            return;
        }

        // Priority 2: Excelsies name takes precedence over biome variants
        // When entity is named "excelsies", always use the special Excelsies texture
        if (hasExcelsies) {
            cir.setReturnValue(EXCELSIES_TEXTURE);
            return;
        }

        // Priority 3: Biome-specific texture variants
        // Use biome-specific texture if available and if not the default plains biome
        if (spawnBiome != null && !spawnBiome.isEmpty() && !spawnBiome.equals("minecraft:plains")) {
            ResourceLocation selectedTexture = ehg$determineTexture(spawnBiome, false, false);
            cir.setReturnValue(selectedTexture);
            return;
        }

        // Priority 4: Fall back to vanilla behavior for regular Happy Ghasts
        // This maintains compatibility with vanilla Happy Ghasts that don't have enhanced features
        cir.setReturnValue(happyGhastRenderState.isBaby ? GHAST_BABY_LOCATION : GHAST_LOCATION);
    }

    // PERFORMANCE OPTIMIZED: Data transfer method with change detection
    // Only updates render state when data actually changes to reduce overhead
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/animal/HappyGhast;Lnet/minecraft/client/renderer/entity/state/HappyGhastRenderState;F)V", at = @At("TAIL"))
    private void ehg$extractRenderState(HappyGhast happyGhast, HappyGhastRenderState happyGhastRenderState, float f, CallbackInfo ci) {
        IEnhancedHappyGhastMixin entityMixin = (IEnhancedHappyGhastMixin) happyGhast;
        IEnhancedHappyGhastMixin renderStateMixin = (IEnhancedHappyGhastMixin) happyGhastRenderState;

        // OPTIMIZATION: Only update render state if data has changed
        String entityBiome = entityMixin.ehg$getSpawnBiome();
        boolean entityHasRpg = entityMixin.ehg$hasRpgName();
        boolean entityHasExcelsies = entityMixin.ehg$hasExcelsiesName();
        boolean entityIsRidden = entityMixin.ehg$isBeingRidden();

        String renderBiome = renderStateMixin.ehg$getSpawnBiome();
        boolean renderHasRpg = renderStateMixin.ehg$hasRpgName();
        boolean renderHasExcelsies = renderStateMixin.ehg$hasExcelsiesName();
        boolean renderIsRidden = renderStateMixin.ehg$isBeingRidden();

        // Only update if values have changed
        if (!entityBiome.equals(renderBiome)) {
            renderStateMixin.ehg$setSpawnBiome(entityBiome);
        }
        if (entityHasRpg != renderHasRpg) {
            renderStateMixin.ehg$setHasRpgName(entityHasRpg);
        }
        if (entityHasExcelsies != renderHasExcelsies) {
            renderStateMixin.ehg$setHasExcelsiesName(entityHasExcelsies);
        }
        if (entityIsRidden != renderIsRidden) {
            renderStateMixin.ehg$setBeingRidden(entityIsRidden);
        }
    }

    // TEXTURE SELECTION SYSTEM EXPLANATION:
    // This method implements a priority-based texture selection system
    // Higher priority textures override lower priority ones
    // The system ensures consistent texture selection across all ghasts

    // PERFORMANCE OPTIMIZED: Texture determination helper method
    // Uses O(1) HashMap lookup for fast biome texture resolution with large entity counts
    @Unique
    private ResourceLocation ehg$determineTexture(String spawnBiome, boolean hasRpgName, boolean hasExcelsiesName) {
        // PRIORITY 1: RPG name takes precedence over everything
        // If entity is named "rpg", always use the special RPG texture
        // Texture file: src/main/resources/assets/happyhaulers/textures/entity/ghast/rpg_texture.png
        if (hasRpgName) {
            return RPG_TEXTURE;
        }

        // PRIORITY 2: Excelsies name takes precedence over biome variants
        // If entity is named "excelsies", always use the special Excelsies texture
        // Texture file: src/main/resources/assets/happyhaulers/textures/entity/ghast/excelsies_texture.png
        if (hasExcelsiesName) {
            return EXCELSIES_TEXTURE;
        }

        // PRIORITY 3: Biome-specific texture if available in our mapping
        // O(1) HashMap lookup for optimal performance with many ghasts
        // Looks up the spawn biome (e.g., "minecraft:forest") in BIOME_TEXTURES map
        // Returns the corresponding texture file (e.g., forest_ghast.png)
        ResourceLocation biomeTexture = BIOME_TEXTURES.get(spawnBiome);
        if (biomeTexture != null) {
            return biomeTexture;
        }

        // PRIORITY 4: Default Enhanced Happy Ghast texture as fallback
        // Used when no biome variant exists or biome detection failed
        // Texture file: src/main/resources/assets/happyhaulers/textures/entity/ghast/happy_ghast.png
        return DEFAULT_TEXTURE;
    }
}
