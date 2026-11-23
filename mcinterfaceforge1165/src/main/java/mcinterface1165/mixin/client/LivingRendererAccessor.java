package mcinterface1165.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.entity.LivingRenderer;

/** Exposes a living entity renderer's layer list (held items, armor, cape, etc.) so it can be swapped temporarily. */
@Mixin(LivingRenderer.class)
public interface LivingRendererAccessor {
    @Accessor("layers")
    public List<?> get_layers();

    @Mutable
    @Accessor("layers")
    public void set_layers(List<?> layers);
}
