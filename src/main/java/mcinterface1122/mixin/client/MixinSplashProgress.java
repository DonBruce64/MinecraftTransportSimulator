package mcinterface1122.mixin.client;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.SplashProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.*;

@Mixin(targets = "net/minecraftforge/fml/client/SplashProgress$2")
public class MixinSplashProgress {
    private static final SplashProgress.Texture TEXTURE = new SplashProgress.Texture(new ResourceLocation("mts:textures/splash/splashscreen.png"), null);

    @Inject(method = "run", at = @At(value = "TAIL", target = "org/lwjgl/opengl/GL11.glEnable(I)V"), remap = false)
    public void runMixin(CallbackInfo ci) {
        TEXTURE.bind();
        glBegin(GL_QUADS);
        TEXTURE.texCoord(0, 0, 0);
        glVertex2f(320, 240);
        TEXTURE.texCoord(0, 0, 1);
        glVertex2f(320, 240);
        TEXTURE.texCoord(0, 1, 1);
        glVertex2f(320, 240);
        TEXTURE.texCoord(0, 1, 0);
        glVertex2f(320, 240);
        glEnd();
    }
}
