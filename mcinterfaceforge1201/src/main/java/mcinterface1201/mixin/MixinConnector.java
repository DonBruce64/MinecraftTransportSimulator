package mcinterface1201.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class MixinConnector implements IMixinConnector {
    @Override
    public void connect() {
        Mixins.addConfiguration("mts.mixins.json");
        //Optional: its plugin applies it only when Embeddium is installed.
        Mixins.addConfiguration("mts-embeddium.mixins.json");
    }
}
