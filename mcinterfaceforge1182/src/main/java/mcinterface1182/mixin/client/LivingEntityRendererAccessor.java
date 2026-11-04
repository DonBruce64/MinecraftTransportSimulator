package mcinterface1182.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;

/** Exposes a living entity renderer's layer list (held items, armor, cape, etc.) so it can be swapped temporarily. */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {
    @Accessor("layers")
    public List<?> get_layers();

    @Mutable
    @Accessor("layers")
    public void set_layers(List<?> layers);
}
