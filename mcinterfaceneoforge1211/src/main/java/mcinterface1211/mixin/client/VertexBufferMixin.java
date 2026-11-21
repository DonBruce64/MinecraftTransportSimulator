package mcinterface1211.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexBuffer;

import mcinterface1211.BrightnessBlending;

@Mixin(VertexBuffer.class)
public abstract class VertexBufferMixin {
    //Wrap the buffered draw after shader setup, including Iris's own OpenGL draw hook.
    @WrapMethod(method = "draw()V")
    private void mts$drawWithBrightnessBlending(Operation<Void> original) {
        BrightnessBlending.draw(original::call);
    }
}
