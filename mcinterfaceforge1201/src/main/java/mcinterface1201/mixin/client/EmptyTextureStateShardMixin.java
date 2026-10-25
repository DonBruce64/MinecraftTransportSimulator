package mcinterface1201.mixin.client;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;

/** The texture a render type's texture shard binds (protected in MC), empty for none or several. */
@Mixin(RenderStateShard.EmptyTextureStateShard.class)
public interface EmptyTextureStateShardMixin {
    @Invoker("cutoutTexture")
    public Optional<ResourceLocation> invoke_cutoutTexture();
}
