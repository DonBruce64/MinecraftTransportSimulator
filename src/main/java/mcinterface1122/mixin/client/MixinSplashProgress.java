package mcinterface1122.mixin.client;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
import jdk.internal.org.objectweb.asm.Opcodes;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.*;

@Mixin(targets = "net/minecraftforge/fml/client/SplashProgress$2")
public class MixinSplashProgress {
    private static final ResourceLocation TEXTURE = new ResourceLocation("mts:textures/splash/splashscreen.png");

    @Inject(method = "run",
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraftforge/fml/client/SplashProgress;access$100()Lnet/minecraftforge/fml/client/SplashProgress$Texture;", opcode = Opcodes.GETSTATIC)),
            at = @At(value = "INVOKE", target = "net/minecraftforge/fml/client/SplashProgress$Texture.bind()V", ordinal = 0),
            remap = false)
    public void runMixin(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.getTextureManager().bindTexture(TEXTURE);
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2f(320, 240);
        glTexCoord2f(0, 1);
        glVertex2f(320, 240);
        glTexCoord2f(1, 1);
        glVertex2f(320, 240);
        glTexCoord2f(1, 0);
        glVertex2f(320, 240);
        glEnd();
    }
}
