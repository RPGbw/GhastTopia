package com.zidiansyncs.ghasttopia.network;

import com.zidiansyncs.ghasttopia.GhastTopia;
import com.zidiansyncs.ghasttopia.texture.HappyGhastTextureManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.UUID;

/**
 * Network handler for GhastTopia mod
 * Handles client-server synchronization of texture variants
 */
public class NetworkHandler {
    
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(GhastTopia.MODID);
        
        // Register texture sync packet
        registrar.playToClient(
            TextureSyncPacket.TYPE,
            TextureSyncPacket.STREAM_CODEC,
            (packet, context) -> {
                // Handle on client side
                context.enqueueWork(() -> {
                    HappyGhastTextureManager.syncToClient(packet.ghastId(), packet.variant());
                    GhastTopia.LOGGER.info("Client received texture sync for ghast: {} -> {}", 
                        packet.ghastId(), packet.variant().getEffectiveVariant());
                });
            }
        );
        
        // Register bulk texture sync packet
        registrar.playToClient(
            BulkTextureSyncPacket.TYPE,
            BulkTextureSyncPacket.STREAM_CODEC,
            (packet, context) -> {
                // Handle on client side
                context.enqueueWork(() -> {
                    for (var entry : packet.variants().entrySet()) {
                        HappyGhastTextureManager.syncToClient(entry.getKey(), entry.getValue());
                    }
                    GhastTopia.LOGGER.info("Client received bulk texture sync for {} ghasts", 
                        packet.variants().size());
                });
            }
        );
    }
    
    /**
     * Send texture variant to specific player
     */
    public static void sendTextureSyncToPlayer(ServerPlayer player, UUID ghastId, 
                                             HappyGhastTextureManager.HappyGhastTextureVariant variant) {
        PacketDistributor.sendToPlayer(player, new TextureSyncPacket(ghastId, variant));
    }
    
    /**
     * Send texture variant to all players
     */
    public static void sendTextureSyncToAll(UUID ghastId, 
                                          HappyGhastTextureManager.HappyGhastTextureVariant variant) {
        PacketDistributor.sendToAllPlayers(new TextureSyncPacket(ghastId, variant));
    }
    
    /**
     * Send all texture variants to specific player (for login sync)
     */
    public static void sendBulkTextureSyncToPlayer(ServerPlayer player) {
        var variantCollection = HappyGhastTextureManager.getAllTextureVariants();
        if (!variantCollection.isEmpty()) {
            // Convert Collection to Map
            var variants = new java.util.HashMap<UUID, HappyGhastTextureManager.HappyGhastTextureVariant>();
            for (var variant : variantCollection) {
                variants.put(variant.ghastId, variant);
            }
            PacketDistributor.sendToPlayer(player, new BulkTextureSyncPacket(variants));
        }
    }

    /**
     * Send all texture variants to all players
     */
    public static void sendBulkTextureSyncToAll() {
        var variantCollection = HappyGhastTextureManager.getAllTextureVariants();
        if (!variantCollection.isEmpty()) {
            // Convert Collection to Map
            var variants = new java.util.HashMap<UUID, HappyGhastTextureManager.HappyGhastTextureVariant>();
            for (var variant : variantCollection) {
                variants.put(variant.ghastId, variant);
            }
            PacketDistributor.sendToAllPlayers(new BulkTextureSyncPacket(variants));
        }
    }
}
