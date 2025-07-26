package com.zidiansyncs.happyhaulers.mixin;

import com.zidiansyncs.happyhaulers.util.mixin.IEnhancedHappyGhastMixin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.HappyGhast;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HappyGhast.class)
public class EnhancedHappyGhastMixin implements IEnhancedHappyGhastMixin {

    @Unique private String ehg$spawnBiome = "minecraft:plains";
    @Unique private boolean ehg$hasRpgName = false;
    @Unique private boolean ehg$isBeingRidden = false;
    @Unique private boolean ehg$biomeDetected = false;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ehg$testInit(EntityType<? extends HappyGhast> entityType, Level level, CallbackInfo ci) {
        // Initialize with plains as default, will be updated in tick
        ehg$spawnBiome = "minecraft:plains";
        ehg$biomeDetected = false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ehg$testTick(CallbackInfo ci) {
        HappyGhast ghast = (HappyGhast)(Object)this;

        // Simple approach: Detect biome on both client and server
        if (ghast.tickCount < 20 && !ehg$biomeDetected) {
            net.minecraft.core.BlockPos spawnPos = ghast.blockPosition();

            // Try server-side detection first (most reliable)
            if (!ghast.level().isClientSide && ghast.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biomeKey =
                    serverLevel.getBiome(spawnPos).unwrapKey().orElse(null);
                if (biomeKey != null) {
                    ehg$spawnBiome = biomeKey.location().toString();
                    ehg$biomeDetected = true;
                }
            }
            // Fallback: Client-side just uses forest for testing
            else if (ghast.level().isClientSide) {
                // For now, just assume forest on client side for testing
                ehg$spawnBiome = "minecraft:forest";
                ehg$biomeDetected = true;
            }
        }



        // Check for name changes every 20 ticks (1 second)
        if (ghast.tickCount % 20 == 0) {
            if (ghast.hasCustomName()) {
                String nameString = ghast.getCustomName().getString().toLowerCase();
                boolean newRpgStatus = nameString.equals("rpg");
                if (newRpgStatus != ehg$hasRpgName) {
                    ehg$hasRpgName = newRpgStatus;
                }
            } else if (ehg$hasRpgName) {
                ehg$hasRpgName = false;
            }
        }
    }

    @Inject(method = "setCustomName*", at = @At("TAIL"))
    private void ehg$testSetCustomName(net.minecraft.network.chat.Component name, CallbackInfo ci) {
        // Check for RPG name
        if (name != null) {
            String nameString = name.getString().toLowerCase();
            ehg$hasRpgName = nameString.equals("rpg");
            // Debug logging
            System.out.println("TEST: Enhanced Happy Ghast named: '" + nameString + "' -> RPG: " + ehg$hasRpgName);
        } else {
            ehg$hasRpgName = false;
            System.out.println("TEST: Enhanced Happy Ghast name cleared -> RPG: false");
        }
    }

    @Override
    public String ehg$getSpawnBiome() {
        return ehg$spawnBiome;
    }

    @Override
    public void ehg$setSpawnBiome(String biome) {
        ehg$spawnBiome = biome != null ? biome : "minecraft:plains";
    }

    @Override
    public boolean ehg$hasRpgName() {
        return ehg$hasRpgName;
    }

    @Override
    public void ehg$setHasRpgName(boolean hasRpgName) {
        ehg$hasRpgName = hasRpgName;
    }

    @Override
    public boolean ehg$isBeingRidden() {
        return ehg$isBeingRidden;
    }

    @Override
    public void ehg$setBeingRidden(boolean beingRidden) {
        ehg$isBeingRidden = beingRidden;
    }
}
