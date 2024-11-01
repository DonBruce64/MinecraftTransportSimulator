package mcinterface1182.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.Camera;

@Mixin(Camera.class)
public interface CameraMixin {
    @Invoker("setPosition")
    public void invoke_setPosition(double pX, double pY, double pZ);
}
