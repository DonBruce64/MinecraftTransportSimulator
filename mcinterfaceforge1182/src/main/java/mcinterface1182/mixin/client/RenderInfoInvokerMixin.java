package mcinterface1182.mixin.client;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface RenderInfoInvokerMixin {
    @Invoker("setPosition")
    public void invoke_setPosition(double pX, double pY, double pZ);
}
