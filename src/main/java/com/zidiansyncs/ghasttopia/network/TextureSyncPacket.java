package com.zidiansyncs.ghasttopia.network;

import com.zidiansyncs.ghasttopia.GhastTopia;
import com.zidiansyncs.ghasttopia.texture.HappyGhastTextureManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Packet for synchronizing individual texture variants from server to client
 */
public record TextureSyncPacket(
    UUID ghastId,
    HappyGhastTextureManager.HappyGhastTextureVariant variant
) implements CustomPacketPayload {
    
    public static final Type<TextureSyncPacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(GhastTopia.MODID, "texture_sync"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, TextureSyncPacket> STREAM_CODEC = 
        StreamCodec.composite(
            // UUID codec
            StreamCodec.of(
                (buf, uuid) -> {
                    buf.writeLong(uuid.getMostSignificantBits());
                    buf.writeLong(uuid.getLeastSignificantBits());
                },
                buf -> new UUID(buf.readLong(), buf.readLong())
            ),
            TextureSyncPacket::ghastId,
            
            // Texture variant codec
            StreamCodec.of(
                (buf, variant) -> {
                    // Write UUID
                    buf.writeLong(variant.ghastId.getMostSignificantBits());
                    buf.writeLong(variant.ghastId.getLeastSignificantBits());
                    
                    // Write strings
                    buf.writeUtf(variant.spawnBiome != null ? variant.spawnBiome : "minecraft:plains");
                    buf.writeUtf(variant.mushroomType != null ? variant.mushroomType : "red");
                    
                    // Write booleans
                    buf.writeBoolean(variant.hasRpgName);
                    buf.writeBoolean(variant.hasExcelsiesName);
                    buf.writeBoolean(variant.isMushroomVariant);
                    buf.writeBoolean(variant.isLocked);
                },
                buf -> {
                    // Read UUID
                    UUID ghastId = new UUID(buf.readLong(), buf.readLong());
                    
                    // Read strings
                    String spawnBiome = buf.readUtf();
                    String mushroomType = buf.readUtf();
                    
                    // Read booleans
                    boolean hasRpgName = buf.readBoolean();
                    boolean hasExcelsiesName = buf.readBoolean();
                    boolean isMushroomVariant = buf.readBoolean();
                    boolean isLocked = buf.readBoolean();
                    
                    // Create variant with all parameters
                    var variant = new HappyGhastTextureManager.HappyGhastTextureVariant(
                        ghastId, spawnBiome, hasRpgName, hasExcelsiesName,
                        isMushroomVariant, mushroomType, "minecraft:overworld"
                    );
                    
                    return variant;
                }
            ),
            TextureSyncPacket::variant,
            TextureSyncPacket::new
        );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
