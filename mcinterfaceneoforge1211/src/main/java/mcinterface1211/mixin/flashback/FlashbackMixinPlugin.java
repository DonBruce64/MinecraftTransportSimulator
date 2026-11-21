package mcinterface1211.mixin.flashback;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class FlashbackMixinPlugin implements IMixinConfigPlugin {
    private static final String MARKER = "com/moulberry/flashback/record/Recorder.class";
    private static final boolean FLASHBACK_PRESENT = isPresent();

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return FLASHBACK_PRESENT;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    private static boolean isPresent() {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        ClassLoader own = FlashbackMixinPlugin.class.getClassLoader();
        return context != null && context.getResource(MARKER) != null || own != null && own.getResource(MARKER) != null;
    }
}
