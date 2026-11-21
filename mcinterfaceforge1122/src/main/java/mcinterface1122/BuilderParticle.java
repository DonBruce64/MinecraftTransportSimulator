package mcinterface1122;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.particles.ParticleObject;
import minecrafttransportsimulator.rendering.RenderableData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Lets Minecraft own the lifetime and rendering of pack-defined particles. */
@EventBusSubscriber(Side.CLIENT)
public class BuilderParticle extends Particle {
    private static final Field particleLayers = ObfuscationReflectionHelper.findField(ParticleManager.class, "field_78876_b");
    private static final FloatBuffer viewMatrix = ByteBuffer.allocateDirect(16 * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final List<RenderableData> translucentRenders = new ArrayList<>();
    private static BuilderParticle lastParticle;
    private static boolean drawingTranslucent;
    private final ParticleObject particle;
    private final TransformationMatrix transform = new TransformationMatrix();

    public BuilderParticle(ParticleObject particle) {
        super(((WrapperWorld) particle.world).world, particle.position.x, particle.position.y, particle.position.z);
        this.particle = particle;
    }

    @Override
    public void onUpdate() {
        if (Minecraft.getMinecraft().world != world) {
            setExpired();
            return;
        }
        particle.update();
        if (!particle.isValid) {
            setExpired();
            return;
        }
        prevPosX = particle.prevPosition.x;
        prevPosY = particle.prevPosition.y;
        prevPosZ = particle.prevPosition.z;
        setPosition(particle.position.x, particle.position.y, particle.position.z);
    }

    @Override
    public void setExpired() {
        super.setExpired();
        particle.remove();
    }

    @Override
    public int getFXLayer() {
        // Layer 3 gives custom particles ownership of their render state and vertices.
        return 3;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity camera, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        if (lastParticle == null) {
            beginBatch();
        }
        float previousLightX = OpenGlHelper.lastBrightnessX;
        float previousLightY = OpenGlHelper.lastBrightnessY;
        boolean previousParticles = InterfaceRender.renderingParticles;
        try {
            InterfaceRender.renderingParticles = true;
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 1.0F / 255.0F);
            if (particle.isValid && Minecraft.getMinecraft().world == world) {
                double cameraX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partialTicks;
                double cameraY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partialTicks;
                double cameraZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partialTicks;
                transform.resetTransforms().setTranslation(particle.position.x - cameraX, particle.position.y - cameraY, particle.position.z - cameraZ);
                particle.render(transform, partialTicks);
            }
        } catch (RuntimeException | Error exception) {
            clearBatch();
            throw exception;
        } finally {
            try {
                // Even an expired last wrapper must finish the particles queued before it.
                if (this == lastParticle) {
                    finishBatch();
                }
            } finally {
                InterfaceRender.renderingParticles = previousParticles;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
                // Restore the same state expected after Minecraft's other layer-3 particles.
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.depthMask(true);
                GlStateManager.disableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
                GlStateManager.enableLighting();
            }
        }
    }

    static boolean queueTranslucent(RenderableData data) {
        if (!drawingTranslucent && (data.isTranslucent || data.enableBrightBlending)) {
            translucentRenders.add(data);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void beginBatch() {
        try {
            // Layer 3 has no end callback.  Find its last MTS wrapper without changing
            // Minecraft's queues, tick order, particle limits, or eviction order.
            ArrayDeque<Particle>[][] layers = (ArrayDeque<Particle>[][]) particleLayers.get(Minecraft.getMinecraft().effectRenderer);
            for (ArrayDeque<Particle> layer : layers[3]) {
                for (Particle particle : layer) {
                    if (particle instanceof BuilderParticle) {
                        lastParticle = (BuilderParticle) particle;
                    }
                }
            }
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not access Minecraft's particle render queues.", exception);
        }
    }

    private static void finishBatch() {
        try {
            // Opaque particles have already written depth in their native callbacks.
            // Sort all translucent textures together using their prepared transforms.
            viewMatrix.clear();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, viewMatrix);
            translucentRenders.sort(Comparator.comparingDouble(BuilderParticle::getDepth));
            drawingTranslucent = true;
            for (RenderableData data : translucentRenders) {
                data.render();
            }
        } finally {
            clearBatch();
        }
    }

    private static double getDepth(RenderableData data) {
        // View-space Z includes the actual camera offset and is negative in front.
        return viewMatrix.get(2) * data.transform.m03 + viewMatrix.get(6) * data.transform.m13 + viewMatrix.get(10) * data.transform.m23 + viewMatrix.get(14);
    }

    private static void clearBatch() {
        translucentRenders.clear();
        lastParticle = null;
        drawingTranslucent = false;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // Discard a batch left by an interrupted render pass.
            clearBatch();
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            clearBatch();
        }
    }
}
