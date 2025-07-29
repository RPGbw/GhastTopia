package com.zidiansyncs.ghasttopia.network;

import com.zidiansyncs.ghasttopia.GhastTopia;
import com.zidiansyncs.ghasttopia.texture.HappyGhastTextureManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Packet for synchronizing multiple texture variants from server to client
 */
public record BulkTextureSyncPacket(
    Map<UUID, HappyGhastTextureManager.HappyGhastTextureVariant> variants
) implements CustomPacketPayload {
    
    public static final Type<BulkTextureSyncPacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(GhastTopia.MODID, "bulk_texture_sync"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, BulkTextureSyncPacket> STREAM_CODEC = 
        StreamCodec.of(
            (buf, packet) -> {
                buf.writeInt(packet.variants.size());
                for (var variant : packet.variants.values()) {
                    buf.writeLong(variant.ghastId.getMostSignificantBits());
                    buf.writeLong(variant.ghastId.getLeastSignificantBits());
                    buf.writeUtf(variant.spawnBiome != null ? variant.spawnBiome : "minecraft:plains");
                    buf.writeUtf(variant.mushroomType != null ? variant.mushroomType : "red");
                    buf.writeBoolean(variant.hasRpgName);
                    buf.writeBoolean(variant.hasExcelsiesName);
                    buf.writeBoolean(variant.isMushroomVariant);
                    buf.writeBoolean(variant.isLocked);
                }
            },
            buf -> {
                int count = buf.readInt();
                Map<UUID, HappyGhastTextureManager.HappyGhastTextureVariant> variants = new HashMap<>();
                for (int i = 0; i < count; i++) {
                    UUID ghastId = new UUID(buf.readLong(), buf.readLong());
                    String spawnBiome = buf.readUtf();
                    String mushroomType = buf.readUtf();
                    boolean hasRpgName = buf.readBoolean();
                    boolean hasExcelsiesName = buf.readBoolean();
                    boolean isMushroomVariant = buf.readBoolean();
                    boolean isLocked = buf.readBoolean();
                    
                    var variant = new HappyGhastTextureManager.HappyGhastTextureVariant(
                        ghastId, spawnBiome, hasRpgName, hasExcelsiesName,
                        isMushroomVariant, mushroomType, "minecraft:overworld"
                    );
                    variants.put(ghastId, variant);
                }
                return new BulkTextureSyncPacket(variants);
            }
        );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
