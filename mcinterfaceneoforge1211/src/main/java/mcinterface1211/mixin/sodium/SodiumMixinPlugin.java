package mcinterface1211.mixin.sodium;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies the {@code mts-sodium.mixins.json} mixins only when Sodium is installed.  The check looks the class
 * file up as a resource, so it loads nothing of Sodium's.
 */
public final class SodiumMixinPlugin implements IMixinConfigPlugin {
    private static final String MARKER = "net/caffeinemc/mods/sodium/client/model/light/data/LightDataAccess.class";
    private static final boolean EMBEDDIUM_PRESENT = isPresent();

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return EMBEDDIUM_PRESENT;
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
        ClassLoader own = SodiumMixinPlugin.class.getClassLoader();
        return context != null && context.getResource(MARKER) != null || own != null && own.getResource(MARKER) != null;
    }
}
