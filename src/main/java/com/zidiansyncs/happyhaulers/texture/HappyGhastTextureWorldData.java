package com.zidiansyncs.happyhaulers.texture;

import com.zidiansyncs.happyhaulers.HappyHaulers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;

import java.io.File;
import java.io.IOException;

/**
 * File-based persistence handler for Happy Ghast texture variants.
 * This ensures texture variant state persists across world saves/loads,
 * dimension travel, and server restarts.
 * 
 * Based on the CustomLeashWorldData pattern for consistency.
 */
public class HappyGhastTextureWorldData {

    private static final String DATA_FILE_NAME = HappyHaulers.MODID + "_happy_ghast_textures.dat";

    /**
     * Get the data file for a level
     */
    private static File getDataFile(ServerLevel level) {
        File worldDir = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        return new File(worldDir, DATA_FILE_NAME);
    }

    /**
     * Save texture variant data to file
     */
    public static void saveTextureData(ServerLevel level) {
        try {
            CompoundTag textureData = HappyGhastTextureManager.saveToNBT();
            File dataFile = getDataFile(level);

            // Create parent directories if they don't exist
            dataFile.getParentFile().mkdirs();

            NbtIo.writeCompressed(textureData, dataFile.toPath());
            System.out.println("HappyHaulers: Saved Happy Ghast texture data to file: " + dataFile.getAbsolutePath());
            System.out.println("HappyHaulers: Saved " + HappyGhastTextureManager.getAllTextureVariants().size() + " texture variants");
        } catch (IOException e) {
            System.out.println("HappyHaulers: Error saving texture data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load texture variant data from file
     */
    public static void loadTextureData(ServerLevel level) {
        try {
            File dataFile = getDataFile(level);
            System.out.println("HappyHaulers: Looking for texture data file: " + dataFile.getAbsolutePath());

            if (dataFile.exists()) {
                System.out.println("HappyHaulers: Found texture data file, loading...");
                CompoundTag textureData = NbtIo.readCompressed(dataFile.toPath(), NbtAccounter.unlimitedHeap());
                if (textureData != null && !textureData.isEmpty()) {
                    HappyGhastTextureManager.loadFromNBT(textureData, level);
                    System.out.println("HappyHaulers: Loaded Happy Ghast texture data from file: " + dataFile.getAbsolutePath());
                    System.out.println("HappyHaulers: Loaded " + HappyGhastTextureManager.getAllTextureVariants().size() + " texture variants");
                } else {
                    System.out.println("HappyHaulers: Empty texture data file found");
                }
            } else {
                System.out.println("HappyHaulers: No Happy Ghast texture data file found at: " + dataFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("HappyHaulers: Error loading texture data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called when a new texture variant is created - save data immediately
     */
    public static void onTextureVariantCreated(ServerLevel level) {
        saveTextureData(level);
    }

    /**
     * Called when a texture variant is updated - save data immediately
     */
    public static void onTextureVariantUpdated(ServerLevel level) {
        saveTextureData(level);
    }

    /**
     * Called when a texture variant is removed - save data immediately
     */
    public static void onTextureVariantRemoved(ServerLevel level) {
        saveTextureData(level);
    }
}
