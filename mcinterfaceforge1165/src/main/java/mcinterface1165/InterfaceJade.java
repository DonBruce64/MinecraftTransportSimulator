package mcinterface1165;

import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.entity.Entity;

@WailaPlugin
public class InterfaceJade implements IWailaPlugin, IEntityComponentProvider {
    @Override
    public void register(IRegistrar registrar) {
        registrar.registerOverrideEntityProvider(this, BuilderEntityRenderForwarder.class);
    }

    @Override
    public Entity getOverride(IEntityAccessor accessor, IPluginConfig config) {
        return null;
    }
}
