package mcinterface1211;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Native particle lifecycle with shared buffers for pack textures and models. */
@EventBusSubscriber(modid = InterfaceLoader.MODID, value = Dist.CLIENT)
public final class WrapperParticle extends Particle {
    private static final PoseStack particleMatrices = new PoseStack();
    private static final TransformationMatrix particleTransform = new TransformationMatrix();
    private static ParticleBuffers particleBuffers;
    private final ParticleObject particle;

    public WrapperParticle(ClientLevel level, ParticleObject particle) {
        super(level, particle.position.x, particle.position.y, particle.position.z);
        this.particle = particle;
        hasPhysics = false;
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().level != level) {
            remove();
            return;
        }
        particle.update();
        xo = particle.prevPosition.x;
        yo = particle.prevPosition.y;
        zo = particle.prevPosition.z;
        x = particle.position.x;
        y = particle.position.y;
        z = particle.position.z;
        if (!particle.isValid) {
            remove();
        }
    }

    @Override
    public void remove() {
        super.remove();
        particle.remove();
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (!particle.isValid || Minecraft.getInstance().level != level) {
            return;
        }
        if (particleBuffers == null) {
            particleBuffers = new ParticleBuffers();
        }
        particleBuffers.hasParticles = true;
        PoseStack previousMatrices = InterfaceRender.matrixStack;
        MultiBufferSource previousBuffer = InterfaceRender.renderBuffer;
        boolean previousParticles = InterfaceRender.renderingParticles;
        InterfaceRender.matrixStack = particleMatrices;
        InterfaceRender.renderBuffer = particleBuffers;
        InterfaceRender.renderingParticles = true;
        try {
            Vec3 cameraPosition = camera.getPosition();
            particleTransform.resetTransforms();
            particleTransform.setTranslation(particle.position.x - cameraPosition.x, particle.position.y - cameraPosition.y, particle.position.z - cameraPosition.z);
            particle.render(particleTransform, partialTicks);
        } finally {
            InterfaceRender.matrixStack = previousMatrices;
            InterfaceRender.renderBuffer = previousBuffer;
            InterfaceRender.renderingParticles = previousParticles;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        // Pack models and trail segments can extend well beyond the collision box.
        return AABB.INFINITE;
    }

    static boolean queueTranslucent(RenderableData data) {
        if (!particleBuffers.drawingTranslucent && (data.isTranslucent || data.enableBrightBlending)) {
            particleBuffers.translucentRenders.add(data);
            return true;
        }
        return false;
    }

    /** Starts each native pass, including passes moved by shader packs. */
    public static void beginRendering() {
        if (particleBuffers != null) {
            particleBuffers.translucentRenders.clear();
            particleBuffers.drawingTranslucent = false;
            particleBuffers.hasParticles = false;
        }
    }

    /** Flushes before ParticleEngine leaves the active shader pass and lightmap. */
    public static void finishRendering() {
        if (particleBuffers != null && particleBuffers.hasParticles) {
            try {
                particleBuffers.finish();
            } finally {
                particleBuffers.hasParticles = false;
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
                if (Minecraft.useShaderTransparency()) {
                    Minecraft.getInstance().levelRenderer.getParticlesTarget().bindWrite(false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide() && particleBuffers != null) {
            particleBuffers.close();
            particleBuffers = null;
        }
    }

    private static final class ParticleBuffers extends MultiBufferSource.BufferSource {
        private final List<RenderableData> translucentRenders = new ArrayList<>();
        private final Matrix4f viewMatrix = new Matrix4f();
        private boolean drawingTranslucent;
        private boolean hasParticles;

        private ParticleBuffers() {
            super(new ByteBufferBuilder(4096), new LinkedHashMap<>());
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            // Only opaque particles may be regrouped by material.  Translucent material
            // changes must flush the shared buffer to preserve back-to-front ordering.
            if (!drawingTranslucent) {
                fixedBuffers.computeIfAbsent(type, key -> new ByteBufferBuilder(4096));
            }
            return super.getBuffer(type);
        }

        private void finish() {
            PoseStack previousMatrices = InterfaceRender.matrixStack;
            MultiBufferSource previousBuffer = InterfaceRender.renderBuffer;
            boolean previousParticles = InterfaceRender.renderingParticles;
            InterfaceRender.matrixStack = particleMatrices;
            InterfaceRender.renderBuffer = this;
            InterfaceRender.renderingParticles = true;
            try {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                // Draw depth-writing models before smoke and other translucent particles.
                endBatch();
                viewMatrix.set(RenderSystem.getModelViewMatrix());
                translucentRenders.sort(Comparator.comparingDouble(this::getDepth));
                drawingTranslucent = true;
                for (RenderableData data : translucentRenders) {
                    data.render();
                }
                endBatch();
            } finally {
                translucentRenders.clear();
                drawingTranslucent = false;
                InterfaceRender.matrixStack = previousMatrices;
                InterfaceRender.renderBuffer = previousBuffer;
                InterfaceRender.renderingParticles = previousParticles;
            }
        }

        private double getDepth(RenderableData data) {
            // The prepared transform includes interpolation and attachment offsets.
            // View-space Z is negative in front of the camera, so farther sorts first.
            return viewMatrix.m02() * data.transform.m03 + viewMatrix.m12() * data.transform.m13 + viewMatrix.m22() * data.transform.m23 + viewMatrix.m32();
        }

        private void close() {
            translucentRenders.clear();
            fixedBuffers.values().forEach(ByteBufferBuilder::close);
            fixedBuffers.clear();
            sharedBuffer.close();
        }
    }
}
