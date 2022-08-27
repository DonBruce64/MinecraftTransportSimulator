package mcinterface1122.mixin.client;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.SplashProgress;
import net.minecraftforge.fml.common.FMLLog;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.Iterator;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV;

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
