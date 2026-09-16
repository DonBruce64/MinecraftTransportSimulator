package mcinterface262;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade integration to hide IV's internal render-forwarding entity.
 * Jade 26.2 removed the client-side hideTarget API, so nothing is registered for now.
 */
@WailaPlugin
public class InterfaceJade implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        //No-op until Jade offers an equivalent target-hiding API.
    }
}
