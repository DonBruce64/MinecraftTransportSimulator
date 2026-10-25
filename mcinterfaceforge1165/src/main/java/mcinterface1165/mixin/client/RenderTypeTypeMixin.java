package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.RenderType;

/** A render type's state, for the texture and transparency its faces cast dynamic light shadows with. */
@Mixin(targets = "net.minecraft.client.renderer.RenderType$Type")
public interface RenderTypeTypeMixin {
    @Accessor("state")
    public RenderType.State get_state();
}
