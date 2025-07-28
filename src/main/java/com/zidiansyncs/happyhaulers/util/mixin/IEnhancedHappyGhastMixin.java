package com.zidiansyncs.happyhaulers.util.mixin;

public interface IEnhancedHappyGhastMixin {

    // Biome variant data access methods
    // Used to store and retrieve the detected spawn biome for texture selection
    String ehg$getSpawnBiome();                    // Returns biome resource location (e.g., "minecraft:forest")
    void ehg$setSpawnBiome(String biome);          // Sets the spawn biome for this entity

    // RPG name detection methods
    // Used to track special "rpg" naming for highest priority texture override
    boolean ehg$hasRpgName();                      // Returns true if entity is named "rpg" or "RPG"
    void ehg$setHasRpgName(boolean hasRpgName);    // Sets the RPG name status

    // Excelsies name detection methods
    // Used to track special "excelsies" naming for highest priority texture override
    boolean ehg$hasExcelsiesName();                // Returns true if entity is named "excelsies"
    void ehg$setHasExcelsiesName(boolean hasExcelsiesName); // Sets the Excelsies name status

    // Rideable state methods
    // Used to track whether a player is currently riding this Enhanced Happy Ghast
    boolean ehg$isBeingRidden();                   // Returns true if player is riding this ghast
    void ehg$setBeingRidden(boolean beingRidden);  // Sets the rideable status
}
