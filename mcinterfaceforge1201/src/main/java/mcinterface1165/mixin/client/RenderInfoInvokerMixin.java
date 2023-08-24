package mcinterface1165.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.ActiveRenderInfo;

@Mixin(ActiveRenderInfo.class)
public interface RenderInfoInvokerMixin {
    @Invoker("setPosition")
    public void invoke_setPosition(double pX, double pY, double pZ);
}
