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

// Mixin target: Vanilla HappyGhastRenderer class
// This allows us to intercept texture selection without creating a custom renderer
@Mixin(HappyGhastRenderer.class)
public abstract class HappyGhastRendererMixin {
    // Shadow vanilla texture locations for fallback behavior
    @Shadow @Final private static ResourceLocation GHAST_BABY_LOCATION;
    @Shadow @Final private static ResourceLocation GHAST_LOCATION;

    // Default Enhanced Happy Ghast texture - used when no biome variant exists
    @Unique private static final ResourceLocation DEFAULT_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/happy_ghast.png");

    // Special RPG texture - highest priority, used when entity is named "rpg" or "RPG"
    @Unique private static final ResourceLocation RPG_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/rpg_texture.png");
    
    // PERFORMANCE OPTIMIZED: Biome-specific texture mappings with pre-computed ResourceLocations
    // Using HashMap for O(1) lookup performance with large numbers of ghasts
    @Unique private static final Map<String, ResourceLocation> BIOME_TEXTURES = new HashMap<>(16, 0.75f);

    static {
        // Initialize biome texture mappings - all ResourceLocations pre-computed at class load time
        BIOME_TEXTURES.put("minecraft:desert",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/desert_ghast.png"));
        BIOME_TEXTURES.put("minecraft:forest",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/forest_ghast.png"));
        BIOME_TEXTURES.put("minecraft:ocean",
                ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/ocean_ghast.png"));
        BIOME_TEXTURES.put("minecraft:warm_ocean",
                ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/coral_ghast.png"));
        BIOME_TEXTURES.put("minecraft:taiga",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/taiga_ghast.png"));
        BIOME_TEXTURES.put("minecraft:swamp",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/swamp_ghast.png"));
        BIOME_TEXTURES.put("minecraft:jungle",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/jungle_ghast.png"));
        BIOME_TEXTURES.put("minecraft:savanna",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/savanna_ghast.png"));
        BIOME_TEXTURES.put("minecraft:badlands",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/badlands_ghast.png"));
        BIOME_TEXTURES.put("minecraft:snowy_plains",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/ice_ghast.png"));
        BIOME_TEXTURES.put("minecraft:ice_spikes",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/ice_ghast.png"));
        BIOME_TEXTURES.put("minecraft:cherry_grove",
                ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/cherry_ghast.png"));
        BIOME_TEXTURES.put("minecraft:lush_caves",
                ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/lush_ghast.png"));
        BIOME_TEXTURES.put("minecraft:mushroom_fields",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/mushroom_ghast.png"));
        BIOME_TEXTURES.put("minecraft:the_end",
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/end_ghast.png"));
    }

    // Main texture selection method - intercepts vanilla getTextureLocation to provide custom textures
    // This method implements the priority system: RPG name > Biome variant > Default texture
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/HappyGhastRenderState;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void ehg$getTextureLocation(HappyGhastRenderState happyGhastRenderState, CallbackInfoReturnable<ResourceLocation> cir) {
        IEnhancedHappyGhastMixin renderStateMixin = (IEnhancedHappyGhastMixin) happyGhastRenderState;

        // Extract Enhanced Happy Ghast data from render state
        String spawnBiome = renderStateMixin.ehg$getSpawnBiome();
        boolean hasRpg = renderStateMixin.ehg$hasRpgName();

        // Priority 1: RPG name takes precedence over everything else
        // When entity is named "rpg" or "RPG", always use the special RPG texture
        if (hasRpg) {
            cir.setReturnValue(RPG_TEXTURE);
            return;
        }

        // Priority 2: Biome-specific texture variants
        // Use biome-specific texture if available and if not the default plains biome
        if (spawnBiome != null && !spawnBiome.isEmpty() && !spawnBiome.equals("minecraft:plains")) {
            ResourceLocation selectedTexture = ehg$determineTexture(spawnBiome, false);
            cir.setReturnValue(selectedTexture);
            return;
        }

        // Priority 3: Fall back to vanilla behavior for regular Happy Ghasts
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
        boolean entityIsRidden = entityMixin.ehg$isBeingRidden();

        String renderBiome = renderStateMixin.ehg$getSpawnBiome();
        boolean renderHasRpg = renderStateMixin.ehg$hasRpgName();
        boolean renderIsRidden = renderStateMixin.ehg$isBeingRidden();

        // Only update if values have changed
        if (!entityBiome.equals(renderBiome)) {
            renderStateMixin.ehg$setSpawnBiome(entityBiome);
        }
        if (entityHasRpg != renderHasRpg) {
            renderStateMixin.ehg$setHasRpgName(entityHasRpg);
        }
        if (entityIsRidden != renderIsRidden) {
            renderStateMixin.ehg$setBeingRidden(entityIsRidden);
        }
    }

    // PERFORMANCE OPTIMIZED: Texture determination helper method
    // Uses O(1) HashMap lookup for fast biome texture resolution with large entity counts
    @Unique
    private ResourceLocation ehg$determineTexture(String spawnBiome, boolean hasRpgName) {
        // Priority 1: RPG name takes precedence over everything
        // If entity is named "rpg", always use the special RPG texture
        if (hasRpgName) {
            return RPG_TEXTURE;
        }

        // Priority 2: Biome-specific texture if available in our mapping
        // O(1) HashMap lookup for optimal performance with many ghasts
        ResourceLocation biomeTexture = BIOME_TEXTURES.get(spawnBiome);
        if (biomeTexture != null) {
            return biomeTexture;
        }

        // Priority 3: Default Enhanced Happy Ghast texture as fallback
        // Used when no biome variant exists or biome detection failed
        return DEFAULT_TEXTURE;
    }
}
