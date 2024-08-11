package mcinterface1165.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

@SuppressWarnings("unused")
public class MixinConnector implements IMixinConnector {
    @Override
    public void connect() {
        Mixins.addConfiguration("mts.mixins.json");
    }
}
