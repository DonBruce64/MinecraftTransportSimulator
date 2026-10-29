package mcinterface1192.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.RenderType;

/** A render type's state shards, for the texture and transparency its faces cast dynamic light shadows with. */
@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
public interface CompositeRenderTypeMixin {
    @Invoker("state")
    public RenderType.CompositeState invoke_state();
}
