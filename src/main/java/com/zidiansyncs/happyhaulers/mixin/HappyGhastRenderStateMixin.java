package com.zidiansyncs.happyhaulers.mixin;

import com.zidiansyncs.happyhaulers.util.mixin.IEnhancedHappyGhastMixin;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HappyGhastRenderState.class)
public class HappyGhastRenderStateMixin implements IEnhancedHappyGhastMixin {
    @Unique private String ehg$spawnBiome = "minecraft:plains";
    @Unique private boolean ehg$hasRpgName = false;
    @Unique private boolean ehg$isBeingRidden = false;

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
