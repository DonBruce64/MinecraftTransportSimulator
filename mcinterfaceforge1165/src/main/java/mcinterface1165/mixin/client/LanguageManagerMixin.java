package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import minecrafttransportsimulator.systems.LanguageSystem;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mixin(LanguageManager.class)
public abstract class LanguageManagerMixin {

    /**
     * Need this to allow us to populate the language names at the right time.  If we call in the Forge events, this happens too soon
     * and the languages arne't populated yet.
     */
    @Inject(method = "onResourceManagerReload", at = @At(value = "TAIL"))
    public void inject_onResourceManagerReload(IResourceManager pResourceManager, CallbackInfo ci) {
        if (FMLEnvironment.dist.isClient()) {
            LanguageSystem.populateNames();
        }
    }
}
