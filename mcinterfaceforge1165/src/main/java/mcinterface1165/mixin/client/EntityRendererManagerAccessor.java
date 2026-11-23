package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.entity.EntityRendererManager;

@Mixin(EntityRendererManager.class)
public interface EntityRendererManagerAccessor {
    @Accessor("shouldRenderShadow")
    boolean get_shouldRenderShadow();
}
