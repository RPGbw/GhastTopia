package com.zidiansyncs.ghasttopia.texture;

import com.zidiansyncs.ghasttopia.network.NetworkHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.HappyGhast;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages persistent texture variants for Happy Ghast entities.
 * This ensures texture variants are permanently tied to spawn biome/conditions
 * and never change regardless of dimension travel or world reloads.
 * 
 * Based on the CustomLeashManager persistence pattern for consistency.
 */
public class HappyGhastTextureManager {
    
    // Server-side texture variant data storage
    private static final Map<UUID, HappyGhastTextureVariant> activeTextureVariants = new ConcurrentHashMap<>();

    // Client-side texture variant data for rendering
    private static final Map<UUID, HappyGhastTextureVariant> clientTextureVariants = new ConcurrentHashMap<>();

    /**
     * Represents a persistent texture variant for a Happy Ghast entity
     */
    public static class HappyGhastTextureVariant {
        public final UUID ghastId;
        public final String spawnBiome;
        public final boolean hasRpgName;
        public final boolean hasExcelsiesName;
        public final boolean isMushroomVariant;
        public final String mushroomType;
        public final long createdTime;
        public final String levelId;
        public final boolean isLocked; // Once locked, variant never changes

        // Constructor for new texture variants
        public HappyGhastTextureVariant(UUID ghastId, String spawnBiome, boolean hasRpgName,
                                      boolean hasExcelsiesName, boolean isMushroomVariant,
                                      String mushroomType, String levelId) {
            this.ghastId = ghastId;
            this.spawnBiome = spawnBiome != null ? spawnBiome : "minecraft:plains";
            this.hasRpgName = hasRpgName;
            this.hasExcelsiesName = hasExcelsiesName;
            this.isMushroomVariant = isMushroomVariant;
            this.mushroomType = mushroomType != null ? mushroomType : "red";
            this.createdTime = System.currentTimeMillis();
            this.levelId = levelId;
            this.isLocked = true; // Always lock variants when created
        }

        // NBT constructor for loading from saved data
        public HappyGhastTextureVariant(CompoundTag nbt) {
            // Load UUID from string representation
            String ghastIdStr = nbt.getString("GhastId").orElse("");
            this.ghastId = ghastIdStr.isEmpty() ? UUID.randomUUID() : UUID.fromString(ghastIdStr);

            String loadedBiome = nbt.getString("SpawnBiome").orElse("minecraft:plains");
            this.spawnBiome = loadedBiome.isEmpty() ? "minecraft:plains" : loadedBiome;

            this.hasRpgName = nbt.getBoolean("HasRpgName").orElse(false);
            this.hasExcelsiesName = nbt.getBoolean("HasExcelsiesName").orElse(false);
            this.isMushroomVariant = nbt.getBoolean("IsMushroomVariant").orElse(false);

            String loadedMushroomType = nbt.getString("MushroomType").orElse("red");
            this.mushroomType = loadedMushroomType.isEmpty() ? "red" : loadedMushroomType;

            this.createdTime = nbt.getLong("CreatedTime").orElse(System.currentTimeMillis());

            String levelIdStr = nbt.getString("LevelId").orElse("");
            this.levelId = levelIdStr.isEmpty() ? "minecraft:overworld" : levelIdStr;

            this.isLocked = nbt.getBoolean("IsLocked").orElse(true);
        }

        /**
         * Serialize this texture variant to NBT for saving
         */
        public CompoundTag toNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("GhastId", this.ghastId.toString());
            nbt.putString("SpawnBiome", this.spawnBiome);
            nbt.putBoolean("HasRpgName", this.hasRpgName);
            nbt.putBoolean("HasExcelsiesName", this.hasExcelsiesName);
            nbt.putBoolean("IsMushroomVariant", this.isMushroomVariant);
            nbt.putString("MushroomType", this.mushroomType);
            nbt.putLong("CreatedTime", this.createdTime);
            nbt.putString("LevelId", this.levelId);
            nbt.putBoolean("IsLocked", this.isLocked);
            return nbt;
        }

        /**
         * Get the effective texture variant considering priority:
         * 1. RPG name (highest priority)
         * 2. Excelsies name
         * 3. Brown mushroom variant (lightning-transformed)
         * 4. Spawn biome
         */
        public String getEffectiveVariant() {
            if (hasRpgName) {
                return "rpg";
            }
            if (hasExcelsiesName) {
                return "excelsies";
            }
            if (isMushroomVariant && "brown".equals(mushroomType)) {
                return "brown_mushroom";
            }
            return spawnBiome;
        }
    }
    
    /**
     * Register a texture variant for a Happy Ghast entity
     * This locks the texture variant permanently to prevent changes
     * ONLY REGISTERS OVERWORLD VARIANTS - prevents Nether/End variants
     */
    public static void registerTextureVariant(HappyGhast ghast, String spawnBiome,
                                            boolean hasRpgName, boolean hasExcelsiesName,
                                            boolean isMushroomVariant, String mushroomType) {
        UUID ghastId = ghast.getUUID();
        String levelId = ghast.level().dimension().location().toString();

        // REGISTRATION RULES:
        // - Overworld: Always allow registration (biome-based or special names)
        // - Nether: Allow registration for new spawns (default texture) and special names
        // - End: Allow registration for new spawns (end texture) and special names
        // - Other dimensions: Only allow special names
        if (!ghast.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD) &&
            !ghast.level().dimension().equals(net.minecraft.world.level.Level.NETHER) &&
            !ghast.level().dimension().equals(net.minecraft.world.level.Level.END)) {
            if (hasRpgName || hasExcelsiesName) {
                // Allow special names in any dimension
            } else {
                // Block registration in other custom dimensions for regular ghasts
                return;
            }
        }

        // Check if variant already exists (avoid overwriting locked variants)
        if (activeTextureVariants.containsKey(ghastId)) {
            HappyGhastTextureVariant existing = activeTextureVariants.get(ghastId);
            if (existing.isLocked) {
                System.out.println("GhastTopia: Texture variant already locked for ghast " + ghastId +
                                 " - keeping existing variant: " + existing.getEffectiveVariant());
                return;
            }
        }

        // Create new locked texture variant (Overworld only)
        HappyGhastTextureVariant variant = new HappyGhastTextureVariant(
            ghastId, spawnBiome, hasRpgName, hasExcelsiesName, isMushroomVariant, mushroomType, levelId);

        activeTextureVariants.put(ghastId, variant);

        // Sync to all clients for rendering (SERVER-SIDE)
        syncToAllClients(ghastId, variant);

        // Mark world data as dirty for persistence
        if (ghast.level() instanceof ServerLevel serverLevel) {
            HappyGhastTextureWorldData.onTextureVariantCreated(serverLevel);
        }

        // Texture variant successfully registered and locked
    }

    /**
     * Update special name status for an existing texture variant
     * This only updates name-based variants, biome remains locked
     */
    public static void updateSpecialNameStatus(UUID ghastId, boolean hasRpgName, boolean hasExcelsiesName, ServerLevel level) {
        HappyGhastTextureVariant existing = activeTextureVariants.get(ghastId);
        if (existing == null) {
            System.out.println("GhastTopia: No texture variant found for ghast " + ghastId + " - cannot update names");
            return;
        }

        // Only update if name status actually changed
        if (existing.hasRpgName != hasRpgName || existing.hasExcelsiesName != hasExcelsiesName) {
            // Create updated variant with new name status but same biome and mushroom data
            HappyGhastTextureVariant updated = new HappyGhastTextureVariant(
                ghastId, existing.spawnBiome, hasRpgName, hasExcelsiesName,
                existing.isMushroomVariant, existing.mushroomType, existing.levelId);
            
            activeTextureVariants.put(ghastId, updated);
            syncToAllClients(ghastId, updated);
            
            // Mark world data as dirty for persistence
            HappyGhastTextureWorldData.onTextureVariantUpdated(level);
            
            System.out.println("GhastTopia: Updated special names for ghast " + ghastId +
                             " - new variant: " + updated.getEffectiveVariant());
        }
    }

    /**
     * Update mushroom transformation for an existing texture variant
     * This preserves all existing data (biome, names) while updating mushroom type
     * Used when red mushroom ghasts are struck by lightning and transform to brown
     */
    public static void updateMushroomTransformation(UUID ghastId, String newMushroomType, ServerLevel level) {
        HappyGhastTextureVariant existing = activeTextureVariants.get(ghastId);
        if (existing == null) {
            System.out.println("GhastTopia: No texture variant found for ghast " + ghastId + " - cannot update mushroom transformation");
            return;
        }

        // Only update if mushroom type actually changed
        if (!existing.mushroomType.equals(newMushroomType)) {
            // Create updated variant with new mushroom type but preserve ALL other data
            // This maintains compatibility with existing texture saving and custom name systems
            HappyGhastTextureVariant updated = new HappyGhastTextureVariant(
                ghastId, existing.spawnBiome, existing.hasRpgName, existing.hasExcelsiesName,
                existing.isMushroomVariant, newMushroomType, existing.levelId);

            activeTextureVariants.put(ghastId, updated);
            syncToAllClients(ghastId, updated);

            // Mark world data as dirty for persistence
            HappyGhastTextureWorldData.onTextureVariantUpdated(level);

            System.out.println("GhastTopia: Updated mushroom transformation for ghast " + ghastId +
                             " from " + existing.mushroomType + " to " + newMushroomType +
                             " - new variant: " + updated.getEffectiveVariant());
        }
    }

    /**
     * Create a special transformation variant for mushroom ghasts
     * This is used when a mushroom ghast is transformed but doesn't have an existing locked variant
     * Preserves the existing registration system while handling transformations
     */
    public static void createTransformationVariant(HappyGhast ghast, String spawnBiome,
                                                  boolean hasRpgName, boolean hasExcelsiesName,
                                                  boolean isMushroomVariant, String mushroomType) {
        // Use the existing registration system but ensure it's marked as a transformation
        registerTextureVariant(ghast, spawnBiome, hasRpgName, hasExcelsiesName, isMushroomVariant, mushroomType);

        System.out.println("GhastTopia: Created transformation variant for ghast " + ghast.getUUID() +
                         " - mushroom type: " + mushroomType + " (TRANSFORMATION)");
    }

    /**
     * Debug method to check if a ghast is a mushroom variant
     * Useful for testing and debugging mushroom transformations
     */
    public static boolean isMushroomVariant(UUID ghastId) {
        HappyGhastTextureVariant variant = activeTextureVariants.get(ghastId);
        if (variant != null) {
            return variant.isMushroomVariant;
        }

        // Check client-side variants as fallback
        variant = clientTextureVariants.get(ghastId);
        return variant != null && variant.isMushroomVariant;
    }

    /**
     * Debug method to get mushroom type for a ghast
     * Returns "red", "brown", or null if not a mushroom variant
     */
    public static String getMushroomType(UUID ghastId) {
        HappyGhastTextureVariant variant = activeTextureVariants.get(ghastId);
        if (variant != null && variant.isMushroomVariant) {
            return variant.mushroomType;
        }

        // Check client-side variants as fallback
        variant = clientTextureVariants.get(ghastId);
        if (variant != null && variant.isMushroomVariant) {
            return variant.mushroomType;
        }

        return null; // Not a mushroom variant
    }

    /**
     * Debug method to list all mushroom variants currently tracked
     * Useful for testing and debugging the mushroom transformation system
     */
    public static void debugListMushroomVariants() {
        System.out.println("GhastTopia: === MUSHROOM VARIANT DEBUG ===");
        int mushroomCount = 0;
        int redCount = 0;
        int brownCount = 0;

        for (HappyGhastTextureVariant variant : activeTextureVariants.values()) {
            if (variant.isMushroomVariant) {
                mushroomCount++;
                if ("red".equals(variant.mushroomType)) {
                    redCount++;
                } else if ("brown".equals(variant.mushroomType)) {
                    brownCount++;
                }

                System.out.println("HappyHaulers: Mushroom Ghast " + variant.ghastId +
                                 " - Type: " + variant.mushroomType +
                                 " - Biome: " + variant.spawnBiome +
                                 " - Effective: " + variant.getEffectiveVariant());
            }
        }

        System.out.println("HappyHaulers: Total mushroom variants: " + mushroomCount +
                         " (Red: " + redCount + ", Brown: " + brownCount + ")");
        System.out.println("HappyHaulers: === END MUSHROOM DEBUG ===");
    }
    
    /**
     * Get texture variant for a Happy Ghast entity
     */
    public static HappyGhastTextureVariant getTextureVariant(UUID ghastId) {
        return activeTextureVariants.get(ghastId);
    }

    /**
     * Check if a Happy Ghast has a registered texture variant
     */
    public static boolean hasTextureVariant(UUID ghastId) {
        return activeTextureVariants.containsKey(ghastId);
    }
    
    /**
     * Remove texture variant for a Happy Ghast entity
     */
    public static void removeTextureVariant(UUID ghastId) {
        HappyGhastTextureVariant removed = activeTextureVariants.remove(ghastId);
        if (removed != null) {
            // Remove from client side too
            clientTextureVariants.remove(ghastId);
            System.out.println("HappyHaulers: Removed texture variant for ghast: " + ghastId);
        }
    }

    /**
     * Remove texture variant with world data persistence
     */
    public static void removeTextureVariant(UUID ghastId, ServerLevel level) {
        HappyGhastTextureVariant removed = activeTextureVariants.remove(ghastId);
        if (removed != null) {
            // Remove from client side too
            clientTextureVariants.remove(ghastId);

            // Mark world data as dirty for persistence
            HappyGhastTextureWorldData.onTextureVariantRemoved(level);

            System.out.println("HappyHaulers: Removed texture variant for ghast: " + ghastId);
        }
    }
    
    /**
     * Get all active texture variants
     */
    public static Collection<HappyGhastTextureVariant> getAllTextureVariants() {
        return new ArrayList<>(activeTextureVariants.values());
    }
    
    /**
     * Clean up invalid texture variants (ghasts that no longer exist)
     * VERY CONSERVATIVE: Only removes variants after 30 minutes of absence
     */
    public static void cleanupInvalidVariants(ServerLevel level) {
        Iterator<Map.Entry<UUID, HappyGhastTextureVariant>> iterator = activeTextureVariants.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, HappyGhastTextureVariant> entry = iterator.next();
            UUID ghastId = entry.getKey();
            HappyGhastTextureVariant variant = entry.getValue();

            // CONSERVATIVE: Check if entity still exists across ALL dimensions
            boolean entityFound = false;
            try {
                // Check current level first
                var entity = level.getEntity(ghastId);
                if (entity != null && entity.isAlive() && !entity.isRemoved()) {
                    entityFound = true;
                } else {
                    // Check all other dimensions too (entity might have traveled)
                    for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
                        var entityInLevel = serverLevel.getEntity(ghastId);
                        if (entityInLevel != null && entityInLevel.isAlive() && !entityInLevel.isRemoved()) {
                            entityFound = true;
                            break;
                        }
                    }
                }

                if (!entityFound) {
                    // Entity not found in any dimension - remove variant after LONG delay
                    long timeSinceCreation = System.currentTimeMillis() - variant.createdTime;
                    if (timeSinceCreation > 1800000) { // 30 minutes (very conservative)
                        System.out.println("HappyHaulers: Ghast " + ghastId + " missing for 30+ minutes across all dimensions - removing texture variant");
                        iterator.remove();
                        clientTextureVariants.remove(ghastId);
                        HappyGhastTextureWorldData.onTextureVariantRemoved(level);
                    } else {
                        System.out.println("HappyHaulers: Ghast " + ghastId + " missing but within 30min grace period - preserving texture variant");
                    }
                }
            } catch (Exception e) {
                System.out.println("HappyHaulers: Error checking ghast " + ghastId + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Sync texture variant to client for rendering (CLIENT-SIDE ONLY)
     * This method is called when receiving network packets from server
     */
    public static void syncToClient(UUID ghastId, HappyGhastTextureVariant variant) {
        clientTextureVariants.put(ghastId, variant);
        System.out.println("GhastTopia: CLIENT - Received texture sync for ghast " + ghastId +
                         " -> " + variant.getEffectiveVariant());
    }

    /**
     * Sync texture variant to all clients (SERVER-SIDE ONLY)
     * This method sends network packets to all connected players
     */
    public static void syncToAllClients(UUID ghastId, HappyGhastTextureVariant variant) {
        // Only run on server side
        if (variant != null) {
            NetworkHandler.sendTextureSyncToAll(ghastId, variant);
            System.out.println("GhastTopia: SERVER - Sent texture sync to all clients for ghast " + ghastId +
                             " -> " + variant.getEffectiveVariant());
        }
    }

    /**
     * Sync texture variant to specific player (SERVER-SIDE ONLY)
     */
    public static void syncToPlayer(ServerPlayer player, UUID ghastId, HappyGhastTextureVariant variant) {
        if (variant != null) {
            NetworkHandler.sendTextureSyncToPlayer(player, ghastId, variant);
            System.out.println("GhastTopia: SERVER - Sent texture sync to player " + player.getName().getString() +
                             " for ghast " + ghastId + " -> " + variant.getEffectiveVariant());
        }
    }

    /**
     * Force immediate synchronization of a specific texture variant to all clients
     * Used for critical situations like dimension travel
     */
    public static void forceSyncToAllClients(UUID ghastId) {
        HappyGhastTextureVariant variant = activeTextureVariants.get(ghastId);
        if (variant != null && variant.isLocked) {
            // Send to all clients via network
            NetworkHandler.sendTextureSyncToAll(ghastId, variant);
            System.out.println("GhastTopia: SERVER - Force-synced texture variant to all clients for ghast " + ghastId +
                             " -> " + variant.getEffectiveVariant());
        } else {
            System.out.println("GhastTopia: SERVER - Cannot force-sync ghast " + ghastId +
                             " - variant not found or not locked");
        }
    }

    /**
     * Force synchronization of all texture variants to all clients
     * Used when dimensions change or client needs to be refreshed
     */
    public static void syncAllToClients() {
        if (!activeTextureVariants.isEmpty()) {
            NetworkHandler.sendBulkTextureSyncToAll();
            System.out.println("GhastTopia: SERVER - Sent bulk texture sync to all clients (" +
                             activeTextureVariants.size() + " variants)");
        }
    }

    /**
     * Sync all texture variants to specific player (used when player joins)
     */
    public static void syncAllToPlayer(ServerPlayer player) {
        if (!activeTextureVariants.isEmpty()) {
            NetworkHandler.sendBulkTextureSyncToPlayer(player);
            System.out.println("GhastTopia: SERVER - Sent bulk texture sync to player " +
                             player.getName().getString() + " (" + activeTextureVariants.size() + " variants)");
        }
    }

    /**
     * Force-load texture variant for a specific ghast (aggressive loading for dimension travel)
     * This ensures texture variants are immediately available when entities are loaded
     */
    public static boolean forceLoadTextureVariant(UUID ghastId) {
        // Check if already loaded
        if (activeTextureVariants.containsKey(ghastId)) {
            HappyGhastTextureVariant variant = activeTextureVariants.get(ghastId);
            // Ensure client sync
            syncToClient(ghastId, variant);
            System.out.println("HappyHaulers: Force-loaded existing texture variant for ghast " + ghastId +
                             " - variant: " + variant.getEffectiveVariant());
            return true;
        }

        // Check client-side variants
        if (clientTextureVariants.containsKey(ghastId)) {
            HappyGhastTextureVariant variant = clientTextureVariants.get(ghastId);
            // Copy to server-side if not already there
            activeTextureVariants.put(ghastId, variant);
            System.out.println("HappyHaulers: Force-loaded texture variant from client for ghast " + ghastId +
                             " - variant: " + variant.getEffectiveVariant());
            return true;
        }

        return false; // No variant found
    }

    /**
     * Get client-side texture variants for rendering
     */
    public static Map<UUID, HappyGhastTextureVariant> getClientTextureVariants() {
        return new HashMap<>(clientTextureVariants);
    }
    
    /**
     * Clear all texture variant data (for cleanup)
     */
    public static void clearAll() {
        activeTextureVariants.clear();
        clientTextureVariants.clear();
    }

    /**
     * Save all texture variant data to NBT for world persistence
     */
    public static CompoundTag saveToNBT() {
        CompoundTag nbt = new CompoundTag();
        ListTag variantList = new ListTag();

        System.out.println("HappyHaulers: saveToNBT called - current texture variants: " + activeTextureVariants.size() + " ghasts");

        // Save all texture variants
        for (HappyGhastTextureVariant variant : activeTextureVariants.values()) {
            variantList.add(variant.toNBT());
            System.out.println("HappyHaulers: Saving texture variant - Ghast: " + variant.ghastId + 
                             ", Variant: " + variant.getEffectiveVariant());
        }

        nbt.put("TextureVariants", variantList);
        nbt.putLong("SaveTime", System.currentTimeMillis());

        System.out.println("HappyHaulers: Saved " + variantList.size() + " texture variants to NBT");
        return nbt;
    }

    // Track if data has been loaded to prevent multiple loads
    private static boolean dataLoaded = false;

    /**
     * Load texture variant data from NBT and restore variants
     */
    public static void loadFromNBT(CompoundTag nbt, ServerLevel level) {
        System.out.println("HappyHaulers: loadFromNBT called for level: " + level.dimension().location());

        if (!nbt.contains("TextureVariants")) {
            System.out.println("HappyHaulers: No texture variants found in NBT data");
            return;
        }

        // Only clear existing data on first load
        if (!dataLoaded) {
            activeTextureVariants.clear();
            clientTextureVariants.clear();
            System.out.println("HappyHaulers: Cleared existing texture variant data (first load)");
            dataLoaded = true;
        } else {
            System.out.println("HappyHaulers: Data already loaded, adding to existing variants");
        }

        ListTag variantList = nbt.getList("TextureVariants").orElse(new ListTag());
        System.out.println("HappyHaulers: Found " + variantList.size() + " texture variants in NBT");

        int loadedCount = 0;
        int skippedCount = 0;

        for (int i = 0; i < variantList.size(); i++) {
            CompoundTag variantNBT = variantList.getCompound(i).orElse(new CompoundTag());
            try {
                HappyGhastTextureVariant variant = new HappyGhastTextureVariant(variantNBT);

                // Check if this variant already exists (avoid duplicates)
                if (activeTextureVariants.containsKey(variant.ghastId)) {
                    System.out.println("HappyHaulers: Skipping duplicate texture variant for ghast: " + variant.ghastId);
                    skippedCount++;
                    continue;
                }

                // Add to active variants
                activeTextureVariants.put(variant.ghastId, variant);

                // Sync to client
                syncToClient(variant.ghastId, variant);

                loadedCount++;
                System.out.println("HappyHaulers: Loaded texture variant - Ghast: " + variant.ghastId + 
                                 " in " + variant.levelId + ", Variant: " + variant.getEffectiveVariant());
            } catch (Exception e) {
                System.out.println("HappyHaulers: Error loading texture variant: " + e.getMessage());
                e.printStackTrace();
                skippedCount++;
            }
        }

        System.out.println("HappyHaulers: Loaded " + loadedCount + " texture variants from NBT (skipped " + skippedCount + ")");
    }

    /**
     * Reset the data loaded flag - used when server stops/starts
     */
    public static void resetDataLoadedFlag() {
        dataLoaded = false;
        System.out.println("HappyHaulers: Reset texture variant data loaded flag");
    }
}
