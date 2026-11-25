package mcinterface1192;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;

import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.particles.ParticleObject;
import minecrafttransportsimulator.rendering.RenderableData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

/** Lets Minecraft own particle ticking, rendering, and lifetime limits. */
public class WrapperParticle extends Particle {
    private static final ParticleRenderer RENDER_TYPE = new ParticleRenderer();

    private static final class ParticleRenderer {
        private final List<RenderableData> translucentRenders = new ArrayList<>();
        private final Matrix4f viewMatrix = new Matrix4f();
        private final Vector4f renderPosition = new Vector4f();
        private boolean drawingTranslucent;
        private boolean active;
        private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        private final MultiBufferSource.BufferSource buffers = MultiBufferSource.immediateWithBuffers(builders, new BufferBuilder(256));
        private final MultiBufferSource particleBuffers = renderType -> {
            //Only opaque particles may be regrouped by material.  The shared translucent
            //buffer flushes on material changes to preserve back-to-front ordering.
            if (!drawingTranslucent) {
                builders.computeIfAbsent(renderType, key -> new BufferBuilder(256));
            }
            return buffers.getBuffer(renderType);
        };
        private final PoseStack particleMatrixStack = new PoseStack();
        private PoseStack previousMatrixStack;
        private MultiBufferSource previousRenderBuffer;
        private boolean previouslyRenderingParticles;

        private void begin() {
            if (active) {
                return;
            }
            active = true;
            translucentRenders.clear();
            drawingTranslucent = false;
            previousMatrixStack = InterfaceRender.matrixStack;
            previousRenderBuffer = InterfaceRender.renderBuffer;
            previouslyRenderingParticles = InterfaceRender.renderingParticles;
            //Minecraft already applies the camera view through RenderSystem's model-view matrix.
            InterfaceRender.matrixStack = particleMatrixStack;
            InterfaceRender.renderBuffer = particleBuffers;
            InterfaceRender.renderingParticles = true;
        }

        private void end() {
            if (!active) {
                return;
            }
            try {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                //Solid particles must fill the depth buffer before translucent particles blend over them.
                buffers.endBatch();
                viewMatrix.load(RenderSystem.getModelViewMatrix());
                translucentRenders.sort(Comparator.comparingDouble(this::getDepth));
                drawingTranslucent = true;
                for (RenderableData data : translucentRenders) {
                    data.render();
                }
                buffers.endBatch();
            } finally {
                translucentRenders.clear();
                drawingTranslucent = false;
                active = false;
                InterfaceRender.matrixStack = previousMatrixStack;
                InterfaceRender.renderBuffer = previousRenderBuffer;
                InterfaceRender.renderingParticles = previouslyRenderingParticles;
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
                //RenderType cleanup binds the main target; later native particles still need this target.
                if (Minecraft.useShaderTransparency()) {
                    Minecraft.getInstance().levelRenderer.getParticlesTarget().bindWrite(false);
                }
            }
        }

        private double getDepth(RenderableData data) {
            //The prepared transform includes interpolation and attachment offsets.
            //View-space Z is negative in front of the camera, so farther sorts first.
            renderPosition.set((float) data.transform.m03, (float) data.transform.m13, (float) data.transform.m23, 1.0F);
            renderPosition.transform(viewMatrix);
            return renderPosition.z();
        }
    }

    /** Flush after Minecraft's translucent sheet, while the native particle pass is still active. */
    public static void finishRender() {
        RENDER_TYPE.end();
    }

    static boolean queueTranslucent(RenderableData data) {
        if (!RENDER_TYPE.drawingTranslucent && (data.isTranslucent || data.enableBrightBlending)) {
            RENDER_TYPE.translucentRenders.add(data);
            return true;
        }
        return false;
    }

    private final ParticleObject particle;
    private final TransformationMatrix transform = new TransformationMatrix();

    public WrapperParticle(ParticleObject particle) {
        super((ClientLevel) ((WrapperWorld) particle.world).world, particle.position.x, particle.position.y, particle.position.z);
        this.particle = particle;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().level != level || !particle.isValid) {
            remove();
            return;
        }
        particle.update();
        if (!particle.isValid) {
            remove();
            return;
        }
        xo = particle.prevPosition.x;
        yo = particle.prevPosition.y;
        zo = particle.prevPosition.z;
        x = particle.position.x;
        y = particle.position.y;
        z = particle.position.z;
        age = (int) particle.ticksExisted;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (particle.isValid && Minecraft.getInstance().level == level) {
            RENDER_TYPE.begin();
            Vec3 cameraPosition = camera.getPosition();
            transform.resetTransforms().setTranslation(particle.position.x - cameraPosition.x, particle.position.y - cameraPosition.y, particle.position.z - cameraPosition.z);
            particle.render(transform, partialTicks);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        //Shader mods recognize this sheet when splitting opaque and translucent passes.
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public boolean shouldCull() {
        //Pack models and trails can extend well beyond the native particle bounding box.
        return false;
    }

    @Override
    public void remove() {
        super.remove();
        particle.remove();
    }
}
