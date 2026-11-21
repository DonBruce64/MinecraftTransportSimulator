package mcinterface1165;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.particles.ParticleObject;
import minecrafttransportsimulator.rendering.RenderableData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Vector3d;

/** Lets Minecraft own the lifetime and rendering of pack-defined particles. */
public class BuilderParticle extends Particle {
    private static final MatrixStack PARTICLE_STACK = new MatrixStack();
    private static final FloatBuffer PARTICLE_VIEW_MATRIX = BufferUtils.createFloatBuffer(16);
    private static final List<RenderableData> TRANSLUCENT_RENDERS = new ArrayList<>();
    private static boolean drawingTranslucent;
    private static boolean hasParticles;
    private static final Map<RenderType, BufferBuilder> PARTICLE_BUILDERS = new LinkedHashMap<>();
    private static final IRenderTypeBuffer.Impl PARTICLE_BUFFER = IRenderTypeBuffer.immediateWithBuffers(PARTICLE_BUILDERS, new BufferBuilder(256));
    private static final IRenderTypeBuffer PARTICLE_SOURCE = type -> {
        // Translucent material changes must flush the shared buffer to preserve ordering.
        if (!drawingTranslucent) {
            PARTICLE_BUILDERS.computeIfAbsent(type, key -> new BufferBuilder(256));
        }
        return PARTICLE_BUFFER.getBuffer(type);
    };
    /** Starts each native particle pass, including the separate passes used by shader mods. */
    public static void beginRendering() {
        TRANSLUCENT_RENDERS.clear();
        drawingTranslucent = false;
        hasParticles = false;
    }

    /** Flushes after the native sheet, while its camera and shader pass are still active. */
    public static void finishRendering() {
        if (!hasParticles) {
            return;
        }
        MatrixStack previousStack = InterfaceRender.matrixStack;
        IRenderTypeBuffer previousBuffer = InterfaceRender.renderBuffer;
        boolean previousParticles = InterfaceRender.renderingParticles;
        InterfaceRender.matrixStack = PARTICLE_STACK;
        InterfaceRender.renderBuffer = PARTICLE_SOURCE;
        InterfaceRender.renderingParticles = true;
        try {
            // Opaque particles must establish depth before smoke and other blended particles.
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            PARTICLE_BUFFER.endBatch();
            if (!TRANSLUCENT_RENDERS.isEmpty()) {
                // Minecraft has already applied the view matrix for this particle pass.
                PARTICLE_VIEW_MATRIX.clear();
                GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, PARTICLE_VIEW_MATRIX);
                TRANSLUCENT_RENDERS.sort(Comparator.comparingDouble(BuilderParticle::getDepth));
                drawingTranslucent = true;
                for (RenderableData data : TRANSLUCENT_RENDERS) {
                    data.render();
                }
                PARTICLE_BUFFER.endBatch();
            }
        } finally {
            beginRendering();
            InterfaceRender.matrixStack = previousStack;
            InterfaceRender.renderBuffer = previousBuffer;
            InterfaceRender.renderingParticles = previousParticles;
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            // RenderType cleanup binds the main target; later particles still need this target.
            if (Minecraft.useShaderTransparency()) {
                Minecraft.getInstance().levelRenderer.getParticlesTarget().bindWrite(false);
            }
        }
    }

    static boolean queueTranslucent(RenderableData data) {
        if (!drawingTranslucent && (data.isTranslucent || data.enableBrightBlending)) {
            TRANSLUCENT_RENDERS.add(data);
            return true;
        }
        return false;
    }

    private static double getDepth(RenderableData data) {
        // The prepared transform includes interpolation and attachment offsets.
        // View-space Z is negative in front of the camera, so farther sorts first.
        return PARTICLE_VIEW_MATRIX.get(2) * data.transform.m03 + PARTICLE_VIEW_MATRIX.get(6) * data.transform.m13 + PARTICLE_VIEW_MATRIX.get(10) * data.transform.m23 + PARTICLE_VIEW_MATRIX.get(14);
    }

    private final ParticleObject particle;
    private final TransformationMatrix transform = new TransformationMatrix();

    public BuilderParticle(ParticleObject particle) {
        super((ClientWorld) ((WrapperWorld) particle.world).world, particle.position.x, particle.position.y, particle.position.z);
        this.particle = particle;
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().level != level) {
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
        setPos(particle.position.x, particle.position.y, particle.position.z);
    }

    @Override
    public void remove() {
        super.remove();
        particle.remove();
    }

    @Override
    public IParticleRenderType getRenderType() {
        // Shader mods select this vanilla sheet for the translucent particle pass.
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public boolean shouldCull() {
        // A trail or an animated model may extend well beyond the particle's origin.
        return false;
    }

    @Override
    public void render(IVertexBuilder buffer, ActiveRenderInfo camera, float partialTicks) {
        if (!particle.isValid || Minecraft.getInstance().level != level) {
            return;
        }
        hasParticles = true;
        Vector3d cameraPosition = camera.getPosition();
        transform.resetTransforms().setTranslation(particle.position.x - cameraPosition.x, particle.position.y - cameraPosition.y, particle.position.z - cameraPosition.z);
        MatrixStack previousStack = InterfaceRender.matrixStack;
        IRenderTypeBuffer previousBuffer = InterfaceRender.renderBuffer;
        boolean previousParticles = InterfaceRender.renderingParticles;
        try {
            // Minecraft has already applied the view matrix for this particle pass.
            InterfaceRender.matrixStack = PARTICLE_STACK;
            InterfaceRender.renderBuffer = PARTICLE_SOURCE;
            InterfaceRender.renderingParticles = true;
            particle.render(transform, partialTicks);
        } finally {
            InterfaceRender.matrixStack = previousStack;
            InterfaceRender.renderBuffer = previousBuffer;
            InterfaceRender.renderingParticles = previousParticles;
        }
    }
}
