package mcinterface1201;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
        private boolean drawingTranslucent;
        private boolean active;
        private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        private final MultiBufferSource.BufferSource buffers = MultiBufferSource.immediateWithBuffers(builders, new BufferBuilder(256));
        private final MultiBufferSource particleBuffers = renderType -> {
            //Only opaque particles may be regrouped by material.
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
                viewMatrix.set(RenderSystem.getModelViewMatrix());
                translucentRenders.sort(Comparator.comparingDouble(this::getDepth));
                drawingTranslucent = true;
                for (RenderableData data : translucentRenders) {
                    data.render();
                }
                //Shared buffers flush on material changes, preserving the sorted order.
                buffers.endBatch();
            } finally {
                translucentRenders.clear();
                drawingTranslucent = false;
                active = false;
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
                InterfaceRender.matrixStack = previousMatrixStack;
                InterfaceRender.renderBuffer = previousRenderBuffer;
                InterfaceRender.renderingParticles = previouslyRenderingParticles;
                //RenderType cleanup binds the main target; later native particles still need this target.
                if (Minecraft.useShaderTransparency()) {
                    Minecraft.getInstance().levelRenderer.getParticlesTarget().bindWrite(false);
                }
            }
        }

        private double getDepth(RenderableData data) {
            //Prepared transforms include interpolation and attachment offsets; farther Z is more negative.
            return viewMatrix.m02() * data.transform.m03 + viewMatrix.m12() * data.transform.m13 + viewMatrix.m22() * data.transform.m23 + viewMatrix.m32();
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
