package com.zidiansyncs.ghasttopia.event;

import com.zidiansyncs.ghasttopia.GhastTopia;
import com.zidiansyncs.ghasttopia.texture.HappyGhastTextureManager;
import com.zidiansyncs.ghasttopia.texture.HappyGhastTextureWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.HappyGhast;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server-side event handlers for Happy Ghast texture persistence system.
 * Manages loading/saving texture variant data and cleanup operations.
 */
@EventBusSubscriber(modid = GhastTopia.MODID)
public class ServerEventHandlers {

    private static int textureCleanupCounter = 0;
    private static final int TEXTURE_CLEANUP_INTERVAL = 6000; // 5 minutes (6000 ticks)
    
    // Track if we've loaded texture data yet to avoid multiple loads
    private static boolean hasLoadedTextureData = false;

    /**
     * Handle server starting - reset flags and prepare for data loading
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        System.out.println("GhastTopia: Server started - resetting texture data flags");

        // Reset flags so data can be loaded when levels load
        hasLoadedTextureData = false;
        HappyGhastTextureManager.resetDataLoadedFlag();

        System.out.println("GhastTopia: Ready to load texture data when levels load");
    }

    /**
     * Handle server stopping - save all texture data from memory
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        System.out.println("GhastTopia: Server stopping, saving Happy Ghast texture data from memory");

        // Save texture data once (not per dimension since it's global data)
        Iterable<ServerLevel> levels = event.getServer().getAllLevels();
        if (levels.iterator().hasNext()) {
            ServerLevel firstLevel = levels.iterator().next();
            System.out.println("GhastTopia: Saving texture data to file");
            HappyGhastTextureWorldData.saveTextureData(firstLevel);
        }

        // Reset flags for next server start
        hasLoadedTextureData = false;
        HappyGhastTextureManager.resetDataLoadedFlag();
    }

    /**
     * Handle world loading - restore texture data from saved world data
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        System.out.println("GhastTopia: LevelEvent.Load triggered for: " + event.getLevel().getClass().getSimpleName());

        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            System.out.println("GhastTopia: Not a ServerLevel, skipping texture data load");
            return;
        }

        // Only load data once when the first dimension loads
        if (!hasLoadedTextureData && serverLevel != null) {
            System.out.println("GhastTopia: " + serverLevel.dimension().location() + " loaded, restoring Happy Ghast texture data");
            HappyGhastTextureWorldData.loadTextureData(serverLevel);

            // Sync all loaded variants to all clients for rendering
            HappyGhastTextureManager.syncAllToClients();

            hasLoadedTextureData = true;
        } else if (serverLevel != null) {
            System.out.println("GhastTopia: " + serverLevel.dimension().location() + " loaded, but texture data already loaded");

            // Still sync to all clients in case of dimension changes
            HappyGhastTextureManager.syncAllToClients();
        }
    }

    /**
     * Handle world unloading - clear texture data from memory only
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        System.out.println("HappyHaulers: LevelEvent.Unload triggered for: " + event.getLevel().getClass().getSimpleName());

        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            System.out.println("HappyHaulers: Not a ServerLevel, skipping");
            return;
        }

        System.out.println("HappyHaulers: Level unloading: " + serverLevel.dimension().location());

        // Only clear memory when the overworld unloads (last to unload)
        if (serverLevel.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            System.out.println("HappyHaulers: Overworld unloading, clearing texture data from memory");
            HappyGhastTextureManager.clearAll();
            HappyGhastTextureManager.resetDataLoadedFlag();
            hasLoadedTextureData = false;
        }
    }

    /**
     * Handle Happy Ghast entities joining the level
     * This ensures existing ghasts get their texture variants restored
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof HappyGhast ghast && !event.getLevel().isClientSide) {
            // AGGRESSIVE: Force-load texture variant immediately when entity joins level
            boolean hasVariant = HappyGhastTextureManager.forceLoadTextureVariant(ghast.getUUID());

            if (hasVariant) {
                HappyGhastTextureManager.HappyGhastTextureVariant variant =
                    HappyGhastTextureManager.getTextureVariant(ghast.getUUID());

                // Ensure client-side sync when entity joins level (critical for dimension travel)
                if (variant != null && variant.isLocked) {
                    HappyGhastTextureManager.syncToAllClients(ghast.getUUID(), variant);

                    System.out.println("HappyHaulers: AGGRESSIVE RESTORE - Happy Ghast " + ghast.getUUID() +
                                     " joined level " + event.getLevel().dimension().location() +
                                     " with locked texture variant: " + variant.getEffectiveVariant() + " (LOCKED)");
                }
            } else {
                // No existing variant - check if this is in Overworld or not
                if (event.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
                    System.out.println("HappyHaulers: Happy Ghast " + ghast.getUUID() +
                                     " joined OVERWORLD with no existing texture variant - will detect on first tick");
                } else {
                    System.out.println("HappyHaulers: Happy Ghast " + ghast.getUUID() +
                                     " joined NON-OVERWORLD dimension " + event.getLevel().dimension().location() +
                                     " with no existing texture variant - NO DETECTION WILL OCCUR");
                }
            }
        }
    }

    /**
     * Handle Happy Ghast entities traveling to different dimensions
     * This ensures texture variants are preserved during dimension travel
     */
    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof HappyGhast ghast) {
            // AGGRESSIVE: Ensure texture variant is preserved when traveling to new dimension
            if (HappyGhastTextureManager.hasTextureVariant(ghast.getUUID())) {
                HappyGhastTextureManager.HappyGhastTextureVariant variant =
                    HappyGhastTextureManager.getTextureVariant(ghast.getUUID());

                if (variant != null && variant.isLocked) {
                    // CRITICAL: Multiple sync attempts to ensure texture is available in new dimension
                    HappyGhastTextureManager.forceSyncToAllClients(ghast.getUUID());

                    // Force sync all variants to ensure client has complete data
                    HappyGhastTextureManager.syncAllToClients();

                    System.out.println("HappyHaulers: DIMENSION TRAVEL - Happy Ghast " + ghast.getUUID() +
                                     " traveling from " + ghast.level().dimension().location() +
                                     " to " + event.getDimension().location() +
                                     " - AGGRESSIVELY preserving texture variant: " + variant.getEffectiveVariant() + " (LOCKED)");
                } else {
                    System.out.println("HappyHaulers: WARNING - Happy Ghast " + ghast.getUUID() +
                                     " traveling to " + event.getDimension().location() +
                                     " but variant is not locked or null!");
                }
            } else {
                System.out.println("HappyHaulers: WARNING - Happy Ghast " + ghast.getUUID() +
                                 " traveling to " + event.getDimension().location() +
                                 " but no texture variant found!");
            }
        }
    }

    /**
     * Handle Happy Ghast entities leaving the level
     * CONSERVATIVE: Only clean up texture variants for truly dead ghasts
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof HappyGhast ghast && !event.getLevel().isClientSide) {
            // CRITICAL: Be VERY conservative about removing texture variants
            // Only remove if the ghast is actually dead AND removed (not just dimension travel)
            if (ghast.isRemoved() && !ghast.isAlive() && ghast.getRemovalReason() != null) {
                // Additional check: only remove if it's a permanent removal reason
                if (ghast.getRemovalReason() == net.minecraft.world.entity.Entity.RemovalReason.KILLED ||
                    ghast.getRemovalReason() == net.minecraft.world.entity.Entity.RemovalReason.DISCARDED) {

                    System.out.println("HappyHaulers: Happy Ghast " + ghast.getUUID() +
                                     " permanently removed (" + ghast.getRemovalReason() + ") - cleaning up texture variant");
                    if (event.getLevel() instanceof ServerLevel serverLevel) {
                        HappyGhastTextureManager.removeTextureVariant(ghast.getUUID(), serverLevel);
                    }
                } else {
                    System.out.println("HappyHaulers: Happy Ghast " + ghast.getUUID() +
                                     " leaving level but not permanently removed (" + ghast.getRemovalReason() +
                                     ") - PRESERVING texture variant");
                }
            } else {
                System.out.println("HappyHaulers: Happy Ghast " + ghast.getUUID() +
                                 " leaving level (likely dimension travel) - PRESERVING texture variant");
            }
        }
    }

    /**
     * Periodic cleanup of invalid texture variants
     * Runs every 5 minutes to clean up variants for ghasts that no longer exist
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        textureCleanupCounter++;
        
        if (textureCleanupCounter >= TEXTURE_CLEANUP_INTERVAL) {
            textureCleanupCounter = 0;
            
            // Run cleanup on all server levels
            for (ServerLevel level : event.getServer().getAllLevels()) {
                HappyGhastTextureManager.cleanupInvalidVariants(level);
            }
        }
    }

    /**
     * Sync texture data when player joins server
     * This ensures new players see the correct textures for existing ghasts
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Send all texture variants to the joining player
            HappyGhastTextureManager.syncAllToPlayer(player);
            System.out.println("GhastTopia: Player " + player.getName().getString() +
                             " joined - synced texture data");
        }
    }
}
