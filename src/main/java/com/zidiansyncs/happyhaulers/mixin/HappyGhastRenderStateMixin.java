package com.zidiansyncs.happyhaulers.mixin;

// Enhanced Happy Ghast Render State Mixin - Stores Enhanced Happy Ghast data for rendering
// This mixin adds custom data fields to the vanilla HappyGhastRenderState
// The render state is used to pass data from entity to renderer during the rendering process

import com.zidiansyncs.happyhaulers.util.mixin.IEnhancedHappyGhastMixin;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

// Mixin target: Vanilla HappyGhastRenderState class
// This allows us to store Enhanced Happy Ghast data in the render state for texture selection
@Mixin(HappyGhastRenderState.class)
public class HappyGhastRenderStateMixin implements IEnhancedHappyGhastMixin {
    // Enhanced Happy Ghast render data - copied from entity during extractRenderState
    @Unique private String ehg$spawnBiome = "minecraft:plains";         // Spawn biome for texture selection
    @Unique private boolean ehg$hasRpgName = false;                     // RPG name status for special texture
    @Unique private boolean ehg$hasExcelsiesName = false;               // Excelsies name status for special texture
    @Unique private boolean ehg$isBeingRidden = false;                  // Rideable status for future features
    @Unique private UUID ehg$ghastId = null;                            // Ghast UUID for world data lookup
    @Unique private boolean ehg$isMushroomVariant = false;              // Mushroom variant status for texture selection
    @Unique private String ehg$mushroomType = "red";                    // Mushroom type ("red" or "brown") for texture selection

    // Interface implementation methods - provide access to Enhanced Happy Ghast render data
    // These methods are called by the renderer to access texture variant information

    // Biome data access - used by renderer to determine which texture variant to use
    @Override
    public String ehg$getSpawnBiome() {
        return ehg$spawnBiome;
    }

    @Override
    public void ehg$setSpawnBiome(String biome) {
        ehg$spawnBiome = biome != null ? biome : "minecraft:plains";
    }

    // RPG name status access - used by renderer to check for special RPG texture
    @Override
    public boolean ehg$hasRpgName() {
        return ehg$hasRpgName;
    }

    @Override
    public void ehg$setHasRpgName(boolean hasRpgName) {
        ehg$hasRpgName = hasRpgName;
    }

    // Excelsies name status access - used by renderer to check for special Excelsies texture
    @Override
    public boolean ehg$hasExcelsiesName() {
        return ehg$hasExcelsiesName;
    }

    @Override
    public void ehg$setHasExcelsiesName(boolean hasExcelsiesName) {
        ehg$hasExcelsiesName = hasExcelsiesName;
    }

    // Rideable status access - stored for potential future rendering features
    @Override
    public boolean ehg$isBeingRidden() {
        return ehg$isBeingRidden;
    }

    @Override
    public void ehg$setBeingRidden(boolean beingRidden) {
        ehg$isBeingRidden = beingRidden;
    }

    // Ghast ID access - used by renderer to look up world data texture variants
    @Override
    public UUID ehg$getGhastId() {
        return ehg$ghastId;
    }

    @Override
    public void ehg$setGhastId(UUID ghastId) {
        ehg$ghastId = ghastId;
    }

    // Mushroom variant access - used by renderer to determine mushroom texture variants
    @Override
    public boolean ehg$isMushroomVariant() {
        return ehg$isMushroomVariant;
    }

    @Override
    public void ehg$setMushroomVariant(boolean isMushroomVariant) {
        ehg$isMushroomVariant = isMushroomVariant;
    }

    @Override
    public String ehg$getMushroomType() {
        return ehg$mushroomType;
    }

    @Override
    public void ehg$setMushroomType(String mushroomType) {
        ehg$mushroomType = mushroomType != null ? mushroomType : "red";
    }
}
