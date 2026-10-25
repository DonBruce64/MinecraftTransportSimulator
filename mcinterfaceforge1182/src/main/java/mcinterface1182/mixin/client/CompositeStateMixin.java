package mcinterface1182.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/** A render type's texture and transparency shards (package-private and private in MC). */
@Mixin(RenderType.CompositeState.class)
public interface CompositeStateMixin {
    @Accessor("textureState")
    public RenderStateShard.EmptyTextureStateShard get_textureState();

    @Accessor("transparencyState")
    public RenderStateShard.TransparencyStateShard get_transparencyState();
}
