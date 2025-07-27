package com.zidiansyncs.happyhaulers.util.mixin;

// Enhanced Happy Ghast Mixin Interface - Defines data sharing contract between mixins
// This interface allows communication between entity mixin, render state mixin, and renderer mixin
// Based on FrozenHappyGhast's IHappyGhastMixin pattern for cross-mixin data sharing
public interface IEnhancedHappyGhastMixin {

    // Biome variant data access methods
    // Used to store and retrieve the detected spawn biome for texture selection
    String ehg$getSpawnBiome();                    // Returns biome resource location (e.g., "minecraft:forest")
    void ehg$setSpawnBiome(String biome);          // Sets the spawn biome for this entity

    // RPG name detection methods
    // Used to track special "rpg" naming for highest priority texture override
    boolean ehg$hasRpgName();                      // Returns true if entity is named "rpg" or "RPG"
    void ehg$setHasRpgName(boolean hasRpgName);    // Sets the RPG name status

    // Rideable state methods
    // Used to track whether a player is currently riding this Enhanced Happy Ghast
    boolean ehg$isBeingRidden();                   // Returns true if player is riding this ghast
    void ehg$setBeingRidden(boolean beingRidden);  // Sets the rideable status
}
