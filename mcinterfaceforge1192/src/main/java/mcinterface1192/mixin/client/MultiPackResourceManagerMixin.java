package mcinterface1192.mixin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcinterface1192.InterfaceEventsModelLoader;
import mcinterface1192.InterfaceSound;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.LanguageSystem;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

@Mixin(MultiPackResourceManager.class)
public abstract class MultiPackResourceManagerMixin {
    @Shadow
    private Map<String, FallbackResourceManager> namespacedManagers;
    @Shadow
    @Mutable
    private List<PackResources> packs;
    private static boolean populatedLanguages;

    /**
     * Need this to add our packs to the fallback pack location to properly load.
     */
    @Inject(method = "<init>(Lnet/minecraft/server/packs/PackType;Ljava/util/List;)V", at = @At(value = "TAIL"))
    public void inject_init(PackType pType, List<PackResources> pPackResources, CallbackInfo ci) {
        List<PackResources> packs2 = new ArrayList<>();
        packs2.addAll(packs);
        packs2.add(InterfaceEventsModelLoader.packPack);
        packs = packs2;
        namespacedManagers.computeIfAbsent(InterfaceManager.coreModID, k -> new FallbackResourceManager(pType, InterfaceManager.coreModID)).push(InterfaceEventsModelLoader.packPack);
        PackParser.getAllPackIDs().forEach(packID -> namespacedManagers.computeIfAbsent(packID, k -> new FallbackResourceManager(pType, packID)).push(InterfaceEventsModelLoader.packPack));

        //Need to do this here since languages happen on pack loading vs on boot.
        //Keep checking until we get more than one: MC starts with only en_us on boot.
        if (!populatedLanguages && InterfaceManager.clientInterface != null) {
            if (InterfaceManager.clientInterface.getAllLanguages().size() > 1) {
                LanguageSystem.populateNames();
                populatedLanguages = true;
            }
        }
    }

    /**
     * Kill off any sounds and models.  Their cached indexes will get fouled here if we don't.
     */
    @Inject(method = "close", at = @At(value = "TAIL"))
    public void inject_close(CallbackInfo ci) {
        //Need to check if we have a client interface.  We might not have one yet if we're just loading.
        if (InterfaceManager.clientInterface != null) {
            //Stop all sounds, since sound slots will have changed.
            InterfaceSound.stopAllSounds();

            //Clear all model caches, since OpenGL indexes will have changed.
            AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
            if (world != null) {
                for (AEntityD_Definable<?> entity : world.getEntitiesExtendingType(AEntityD_Definable.class)) {
                    entity.resetModelsAndAnimations();
                }
            }
        }
    }
}
