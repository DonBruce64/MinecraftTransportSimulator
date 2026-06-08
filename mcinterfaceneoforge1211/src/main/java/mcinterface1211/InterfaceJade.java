package mcinterface1211;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade integration to hide IV's internal render-forwarding entity.
 */
@WailaPlugin
public class InterfaceJade implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        ResourceLocation renderForwarderId = ResourceLocation.fromNamespaceAndPath(InterfaceLoader.MODID, "builder_rendering");
        BuiltInRegistries.ENTITY_TYPE.getOptional(renderForwarderId).ifPresent(registration::hideTarget);
    }
}
