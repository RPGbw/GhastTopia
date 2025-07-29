package com.zidiansyncs.ghasttopia.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.HappyGhast;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.zidiansyncs.ghasttopia.util.mixin.IEnhancedHappyGhastMixin;
import com.zidiansyncs.ghasttopia.mixin.EnhancedHappyGhastMixin;

import java.util.List;

/**
 * Debug command to test mushroom ghast transformations
 * Usage: /mushroom_transform
 */
public class MushroomTransformCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mushroom_transform")
            .requires(source -> source.hasPermission(2)) // Requires OP level 2
            .executes(MushroomTransformCommand::execute));
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Vec3 position = source.getPosition();
        
        // Find Happy Ghasts within 10 blocks
        AABB searchArea = new AABB(position.add(-10, -10, -10), position.add(10, 10, 10));
        List<Entity> entities = source.getLevel().getEntities(null, searchArea);
        
        int transformedCount = 0;
        
        for (Entity entity : entities) {
            if (entity instanceof HappyGhast ghast) {
                IEnhancedHappyGhastMixin mixin = (IEnhancedHappyGhastMixin) ghast;
                
                // Check if it's a red mushroom ghast
                if (mixin.ehg$isMushroomVariant() && "red".equals(mixin.ehg$getMushroomType())) {
                    // Try to call the debug transformation method
                    try {
                        // Use reflection to call the debug method
                        java.lang.reflect.Method debugMethod = ghast.getClass().getDeclaredMethod("ehg$debugTransformToBrown");
                        debugMethod.setAccessible(true);
                        debugMethod.invoke(ghast);
                        
                        transformedCount++;
                        source.sendSuccess(() -> Component.literal("Transformed red mushroom ghast " + ghast.getUUID() + " to brown"), false);
                    } catch (Exception e) {
                        source.sendFailure(Component.literal("Failed to transform ghast: " + e.getMessage()));
                    }
                } else {
                    source.sendSuccess(() -> Component.literal("Found Happy Ghast " + ghast.getUUID() + 
                        " - isMushroomVariant: " + mixin.ehg$isMushroomVariant() + 
                        ", mushroomType: " + mixin.ehg$getMushroomType()), false);
                }
            }
        }
        
        final int finalTransformedCount = transformedCount; // Make variable final for lambda

        if (finalTransformedCount == 0) {
            source.sendSuccess(() -> Component.literal("No red mushroom ghasts found within 10 blocks"), false);
        } else {
            source.sendSuccess(() -> Component.literal("Transformed " + finalTransformedCount + " red mushroom ghasts to brown"), false);
        }

        return finalTransformedCount;
    }
}
