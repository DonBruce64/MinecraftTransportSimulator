package mcinterface1192;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class InterfaceJade implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.hideTarget(BuilderEntityRenderForwarder.E_TYPE4.get());
    }
}
