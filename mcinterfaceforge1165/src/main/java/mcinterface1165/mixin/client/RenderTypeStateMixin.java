package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;

/** A render type's texture and transparency states (private in MC). */
@Mixin(RenderType.State.class)
public interface RenderTypeStateMixin {
    @Accessor("textureState")
    public RenderState.TextureState get_textureState();

    @Accessor("transparencyState")
    public RenderState.TransparencyState get_transparencyState();
}
