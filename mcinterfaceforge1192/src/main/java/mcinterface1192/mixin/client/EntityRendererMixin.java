package mcinterface1192.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightSystem;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    /**
     * Raises an entity's block light at its light probe by the dynamic lights while a shader pack is active.
     */
    @Inject(method = "getBlockLightLevel", at = @At("RETURN"), cancellable = true)
    private void inject_getBlockLightLevel(Entity entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int level = DynamicLightSystem.blockLightAt(pos.getX(), pos.getY(), pos.getZ());
        if (level > cir.getReturnValueI()) {
            cir.setReturnValue(level);
        }
    }
}
