package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1165.InterfaceEventsModelLoader;
import mcinterface1165.InterfaceSound;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.resources.SimpleReloadableResourceManager;

@Mixin(SimpleReloadableResourceManager.class)
public abstract class SimpleReloadableResourceManagerMixin {

    /**
     * Need this to inject our resources into the resource queries.
     * Every time the list is cleared, re-add ourselves to it.
     * We also kill off any sounds, since those shouldn't be playing at resource pack load time.
     * They can be playing if the soundsystem is reloaded during pack reloading.
     */
    @Inject(method = "clear", at = @At(value = "TAIL"))
    public void inject_clear(CallbackInfo ci) {
        SimpleReloadableResourceManager manager = (SimpleReloadableResourceManager) ((Object) this);
        InterfaceEventsModelLoader.packPacks.forEach(pack -> manager.add(pack));

        //Stop all sounds, since sound slots will have changed.
        InterfaceSound.stopAllSounds();

        //Clear all model caches, since OpenGL indexes will have changed.
        AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
        if (world != null) {
            for (AEntityA_Base entity : world.allEntities) {
                if (entity instanceof AEntityD_Definable) {
                    ((AEntityD_Definable<?>) entity).animationsInitialized = false;
                    AEntityD_Definable.clearObjectCaches(((AEntityD_Definable<?>) entity).definition);
                }
            }
        }
    }
}
