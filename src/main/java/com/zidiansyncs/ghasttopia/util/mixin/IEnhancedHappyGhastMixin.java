package com.zidiansyncs.ghasttopia.util.mixin;

import java.util.UUID;

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

    // Entity identification methods
    // Used to identify specific ghast entities for persistent texture variants
    UUID ehg$getGhastId();                         // Returns the UUID of this ghast entity
    void ehg$setGhastId(UUID ghastId);             // Sets the UUID of this ghast entity

    // Mushroom variant methods
    // Used to track mushroom ghast variants and lightning transformations
    boolean ehg$isMushroomVariant();               // Returns true if this is a mushroom ghast (red or brown)
    void ehg$setMushroomVariant(boolean isMushroomVariant); // Sets the mushroom variant status
    String ehg$getMushroomType();                  // Returns "red" or "brown" mushroom type
    void ehg$setMushroomType(String mushroomType); // Sets the mushroom type ("red" or "brown")
}
