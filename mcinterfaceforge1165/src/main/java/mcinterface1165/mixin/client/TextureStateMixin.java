package mcinterface1165.mixin.client;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.util.ResourceLocation;

/** The texture a render type's texture state binds (protected in MC), empty for none. */
@Mixin(RenderState.TextureState.class)
public interface TextureStateMixin {
    @Invoker("texture")
    public Optional<ResourceLocation> invoke_texture();
}
