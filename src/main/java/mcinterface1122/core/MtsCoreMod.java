package mcinterface1122.core;

import mcinterface1122.InterfaceLoader;
import mcinterface1122.patches.PhosphorSlicePatch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("Immersive Vehicles")
public class MtsCoreMod implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        String[] transformers = new String[1];
        transformers[0] = PhosphorSlicePatch.class.getName();
        return transformers;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        MixinBootstrap.init();
        Mixins.addConfiguration(InterfaceLoader.MODID + ".mixins.json");
        //MixinEnvironment.getDefaultEnvironment().setObfuscationContext("searge");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
