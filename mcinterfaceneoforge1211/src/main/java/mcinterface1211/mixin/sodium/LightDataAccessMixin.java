package mcinterface1211.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightSystem;

/**
 * Sodium bakes chunk light from its own cached per-block word, not from vanilla's
 * {@code LevelRenderer.getLightColor}: its block-light nibble is raised by the dynamic lights while a shader pack is
 * active, as {@code LevelRendererMixin} does for vanilla meshes.  An opaque full block keeps Sodium's zero.
 * Applied only when Sodium is present ({@link SodiumMixinPlugin}).
 */
@Mixin(value = LightDataAccess.class, remap = false)
public abstract class LightDataAccessMixin {
    @Inject(method = "compute", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void inject_compute(int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
        int word = cir.getReturnValueI();
        if (LightDataAccess.unpackFO(word)) {
            return;
        }
        int level = DynamicLightSystem.blockLightAt(x, y, z);
        if (level > LightDataAccess.unpackBL(word)) {
            cir.setReturnValue((word & ~LightDataAccess.packBL(15)) | LightDataAccess.packBL(level));
        }
    }
}
