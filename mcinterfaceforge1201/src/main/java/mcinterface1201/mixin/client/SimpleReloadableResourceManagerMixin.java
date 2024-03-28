package mcinterface1201.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1201.InterfaceEventsModelLoader;
import net.minecraft.resources.SimpleReloadableResourceManager;

@Mixin(SimpleReloadableResourceManager.class)
public abstract class SimpleReloadableResourceManagerMixin {

    /**
     * Need this to inject our resources into the resource queries.
     * Every time the list is cleared, re-add ourselves to it.
     */
    @Inject(method = "clear", at = @At(value = "TAIL"))
    public void inject_clear(CallbackInfo ci) {
        SimpleReloadableResourceManager manager = (SimpleReloadableResourceManager) ((Object) this);
        InterfaceEventsModelLoader.packPacks.forEach(pack -> manager.add(pack));
    }
}
