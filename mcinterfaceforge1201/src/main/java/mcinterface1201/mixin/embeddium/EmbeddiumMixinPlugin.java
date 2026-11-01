package mcinterface1201.mixin.embeddium;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies the {@code mts-embeddium.mixins.json} mixins only when Embeddium is installed.  The check looks the class
 * file up as a resource, so it loads nothing of Embeddium's.
 */
public final class EmbeddiumMixinPlugin implements IMixinConfigPlugin {
    private static final String MARKER = "me/jellysquid/mods/sodium/client/model/light/data/LightDataAccess.class";
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
        ClassLoader own = EmbeddiumMixinPlugin.class.getClassLoader();
        return context != null && context.getResource(MARKER) != null || own != null && own.getResource(MARKER) != null;
    }
}
