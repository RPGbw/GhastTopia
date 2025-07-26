package com.zidiansyncs.happyhaulers.util.mixin;

// Interface for sharing data between Enhanced Happy Ghast mixins
// Based on FrozenHappyGhast's IHappyGhastMixin pattern
public interface IEnhancedHappyGhastMixin {
    
    // Biome variant data
    String ehg$getSpawnBiome();
    void ehg$setSpawnBiome(String biome);
    
    // RPG name detection
    boolean ehg$hasRpgName();
    void ehg$setHasRpgName(boolean hasRpgName);
    
    // Rideable state
    boolean ehg$isBeingRidden();
    void ehg$setBeingRidden(boolean beingRidden);
}
