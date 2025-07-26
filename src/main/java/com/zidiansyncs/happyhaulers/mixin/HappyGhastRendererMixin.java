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

@Mixin(HappyGhastRenderer.class)
public abstract class HappyGhastRendererMixin {
    @Shadow @Final private static ResourceLocation GHAST_BABY_LOCATION;
    @Shadow @Final private static ResourceLocation GHAST_LOCATION;

    // Default Enhanced Happy Ghast texture
    @Unique private static final ResourceLocation DEFAULT_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/happy_ghast.png");
    
    // Special RPG texture (highest priority)
    @Unique private static final ResourceLocation RPG_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/rpg_texture.png");
    
    // Biome-specific texture mappings
    @Unique private static final Map<String, ResourceLocation> BIOME_TEXTURES = new HashMap<>();
    
    static {
        // Initialize biome texture mappings
        BIOME_TEXTURES.put("minecraft:desert", 
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/desert_ghast.png"));
        BIOME_TEXTURES.put("minecraft:forest", 
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/forest_ghast.png"));
        BIOME_TEXTURES.put("minecraft:ocean", 
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/ocean_ghast.png"));
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
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/snowy_ghast.png"));
        BIOME_TEXTURES.put("minecraft:ice_spikes", 
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/ice_ghast.png"));
        BIOME_TEXTURES.put("minecraft:mushroom_fields", 
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/mushroom_ghast.png"));
        BIOME_TEXTURES.put("minecraft:nether_wastes", 
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/nether_ghast.png"));
        BIOME_TEXTURES.put("minecraft:crimson_forest", 
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/crimson_ghast.png"));
        BIOME_TEXTURES.put("minecraft:warped_forest", 
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/warped_ghast.png"));
        BIOME_TEXTURES.put("minecraft:soul_sand_valley", 
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/soul_ghast.png"));
        BIOME_TEXTURES.put("minecraft:basalt_deltas", 
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/basalt_ghast.png"));
        BIOME_TEXTURES.put("minecraft:the_end", 
            ResourceLocation.fromNamespaceAndPath(HappyHaulers.MODID, "textures/entity/ghast/end_ghast.png"));
    }

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/HappyGhastRenderState;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void ehg$getTextureLocation(HappyGhastRenderState happyGhastRenderState, CallbackInfoReturnable<ResourceLocation> cir) {
        IEnhancedHappyGhastMixin renderStateMixin = (IEnhancedHappyGhastMixin) happyGhastRenderState;

        // Always check for RPG name first (highest priority)
        if (renderStateMixin.ehg$hasRpgName()) {
            cir.setReturnValue(RPG_TEXTURE);
            return;
        }

        // Check if this has biome data for biome variants
        String spawnBiome = renderStateMixin.ehg$getSpawnBiome();
        if (spawnBiome != null && !spawnBiome.isEmpty() && !spawnBiome.equals("minecraft:plains")) {
            ResourceLocation selectedTexture = ehg$determineTexture(spawnBiome, false);
            cir.setReturnValue(selectedTexture);
            return;
        }

        // Fall back to vanilla behavior for regular Happy Ghasts
        cir.setReturnValue(happyGhastRenderState.isBaby ? GHAST_BABY_LOCATION : GHAST_LOCATION);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/animal/HappyGhast;Lnet/minecraft/client/renderer/entity/state/HappyGhastRenderState;F)V", at = @At("TAIL"))
    private void ehg$extractRenderState(HappyGhast happyGhast, HappyGhastRenderState happyGhastRenderState, float f, CallbackInfo ci) {
        IEnhancedHappyGhastMixin entityMixin = (IEnhancedHappyGhastMixin) happyGhast;
        IEnhancedHappyGhastMixin renderStateMixin = (IEnhancedHappyGhastMixin) happyGhastRenderState;

        // Transfer data from entity to render state
        String biome = entityMixin.ehg$getSpawnBiome();
        boolean hasRpg = entityMixin.ehg$hasRpgName();
        boolean isRidden = entityMixin.ehg$isBeingRidden();

        renderStateMixin.ehg$setSpawnBiome(biome);
        renderStateMixin.ehg$setHasRpgName(hasRpg);
        renderStateMixin.ehg$setBeingRidden(isRidden);

        // Data transfer complete - no logging needed in production
    }
    
    // Determine which texture to use based on name and biome
    @Unique
    private ResourceLocation ehg$determineTexture(String spawnBiome, boolean hasRpgName) {
        // Priority 1: RPG name takes precedence over everything
        if (hasRpgName) {
            return RPG_TEXTURE;
        }
        
        // Priority 2: Biome-specific texture if available
        ResourceLocation biomeTexture = BIOME_TEXTURES.get(spawnBiome);
        if (biomeTexture != null) {
            return biomeTexture;
        }
        
        // Priority 3: Default Enhanced Happy Ghast texture
        return DEFAULT_TEXTURE;
    }
}
