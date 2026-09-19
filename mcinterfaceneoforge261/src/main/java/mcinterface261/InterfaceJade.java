package mcinterface261;

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
        //hideTarget was removed in this version of Jade; entity hiding not currently supported.
    }
}
