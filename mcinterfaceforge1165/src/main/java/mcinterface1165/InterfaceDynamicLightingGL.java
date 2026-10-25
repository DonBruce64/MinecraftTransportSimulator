package mcinterface1165;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import mcinterface1165.mixin.client.RenderTypeStateMixin;
import mcinterface1165.mixin.client.RenderTypeTypeMixin;
import mcinterface1165.mixin.client.TextureStateMixin;
import minecrafttransportsimulator.mcinterface.IInterfaceDynamicLightingGL;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightBody;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightShadowAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/**
 * 1.16.5's GL calls for the shared dynamic light renderer (mccore {@code DynamicLightRenderer}): LWJGL 3 raw GL,
 * through {@link GlStateManager}/{@link RenderSystem} wherever MC caches the state.  1.16.5 draws with the fixed
 * function pipeline in a compatibility context, so every pass here runs with a program of ours, a vertex array of
 * ours (MC's client-side arrays live on vertex array 0 and stay untouched) and alpha test off
 * ({@link InterfaceRender#renderDynamicLightFrame} handles that one around the frame).  Targets are
 * {@link Framebuffer}s; captured bodies are IV's cached {@link InterfaceRender.BufferData} (a
 * {@link net.minecraft.client.renderer.vertex.VertexBuffer} of quads); living bodies are drawn by their own
 * {@link EntityRenderer}.  A body's faces keep the texture and alpha cut of their render type
 * ({@link #shadowTexture}, {@link #shadowCut}), so they shadow only where it shows.  Needs GL 3.2, or GL 2.1 with the extensions {@link IInterfaceDynamicLightingGL#legacyContext} names
 * ({@link #supported}): macOS gives 1.16.5 that one, and there every draw goes through attributes 0 and 2 on vertex
 * array 0 instead of a vertex array of ours.  Render thread only.
 */
final class InterfaceDynamicLightingGL implements IInterfaceDynamicLightingGL {
    /** Where UV0 sits in an IV buffer's {@code NEW_ENTITY} vertex, after the position and the colour. */
    private static final int UV_OFFSET = DefaultVertexFormats.NEW_ENTITY.getOffset(DefaultVertexFormats.NEW_ENTITY.getElements().indexOf(DefaultVertexFormats.ELEMENT_UV0));
    static final InterfaceDynamicLightingGL INSTANCE = new InterfaceDynamicLightingGL();
    private static Boolean supported;
    private static boolean legacy;

    private int emptyArray;
    private ByteBuffer byteStaging;
    private FloatBuffer floatStaging;
    /** The one vertex array captured IV bodies are drawn through: attributes 0 and 2 over whichever buffer is bound. */
    private int bodyArray;
    /** GL 2.1: three corners for the fullscreen triangle, which draws nothing without an enabled array. */
    private int triangleBuffer;

    private InterfaceDynamicLightingGL() {
    }

    /**
     * Whether this context can run the shared passes: GL 3.2 (core features in a compatibility context), or GL 2.1 with
     * the extensions of {@link IInterfaceDynamicLightingGL#legacyContext} (macOS gives 1.16.5 a legacy GL 2.1 context).  LWJGL 3
     * crashes the JVM on a call the context lacks, so without either no pass may run at all; mccore then lights the
     * world in its fallback mode.
     */
    static boolean supported() {
        if (supported == null) {
            GLCapabilities caps = null;
            try {
                caps = GL.getCapabilities();
            } catch (Throwable e) {
                //No context to ask; stays unsupported.
            }
            boolean modern = caps != null && caps.OpenGL32;
            legacy = !modern && caps != null && caps.OpenGL21 && caps.GL_EXT_gpu_shader4 && caps.GL_ARB_texture_float && caps.GL_ARB_texture_rg && (caps.OpenGL30 || caps.GL_ARB_framebuffer_object);
            if (!modern && !legacy) {
                InterfaceManager.coreInterface.logError("Dynamic lights draw in their simple mode: this context has neither OpenGL 3.2 nor OpenGL 2.1 with EXT_gpu_shader4, ARB_texture_float, ARB_texture_rg and ARB_framebuffer_object.");
            }
            supported = modern || legacy;
        }
        return supported;
    }

    @Override
    public boolean legacyContext() {
        return legacy;
    }

    @Override
    public int createProgram(String vertexSource, String fragmentSource, String name) {
        int vertex = compile(GL20.GL_VERTEX_SHADER, vertexSource, name);
        int fragment = compile(GL20.GL_FRAGMENT_SHADER, fragmentSource, name);
        if (vertex == 0 || fragment == 0) {
            return 0;
        }
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glBindAttribLocation(program, 0, "Position");
        //lamp_body's texture coordinates, on location 2, where the shared shaders read UV0.  1.16.5 feeds its
        //own buffers through fixed-function arrays, so drawBody points this attribute itself.
        GL20.glBindAttribLocation(program, 2, "UV0");
        GL20.glLinkProgram(program);
        GL20.glDetachShader(program, vertex);
        GL20.glDetachShader(program, fragment);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            InterfaceManager.coreInterface.logError("Could not link dynamic light shader " + name + ": " + GL20.glGetProgramInfoLog(program));
            GL20.glDeleteProgram(program);
            return 0;
        }
        return program;
    }

    private static int compile(int type, String source, String name) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            InterfaceManager.coreInterface.logError("Could not compile dynamic light shader " + name + ": " + GL20.glGetShaderInfoLog(shader));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    @Override
    public void useProgram(int program) {
        GlStateManager._glUseProgram(program);
    }

    @Override
    public int uniformLocation(int program, String name) {
        return GL20.glGetUniformLocation(program, name);
    }

    @Override
    public void setUniform(int location, int value) {
        GL20.glUniform1i(location, value);
    }

    @Override
    public void setUniform(int location, float value) {
        GL20.glUniform1f(location, value);
    }

    @Override
    public void setUniform(int location, float x, float y) {
        GL20.glUniform2f(location, x, y);
    }

    @Override
    public void setUniform(int location, float x, float y, float z) {
        GL20.glUniform3f(location, x, y, z);
    }

    @Override
    public void setUniform(int location, float x, float y, float z, float w) {
        GL20.glUniform4f(location, x, y, z, w);
    }

    @Override
    public void setUniformInts(int location, int x, int y, int z) {
        GL20.glUniform3i(location, x, y, z);
    }

    @Override
    public void setUniformArray(int location, float[] values, int size, int count) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(size * count);
            buffer.put(values, 0, size * count).flip();
            if (size == 4) {
                GL20.glUniform4fv(location, buffer);
            } else if (size == 2) {
                GL20.glUniform2fv(location, buffer);
            } else {
                GL20.glUniform1fv(location, buffer);
            }
        }
    }

    @Override
    public void setUniformMatrix(int location, float[] matrix) {
        GL20.glUniformMatrix4fv(location, false, matrix);
    }

    @Override
    public int createDataTexture(int width, int height, boolean floats) {
        int texture = GlStateManager._genTexture();
        GlStateManager._bindTexture(texture);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
        unpack(0);
        if (floats) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, width, height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) null);
        } else {
            ByteBuffer zero = MemoryUtil.memCalloc(width * height);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, width, height, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, zero);
            MemoryUtil.memFree(zero);
        }
        GlStateManager._bindTexture(0);
        return texture;
    }

    @Override
    public void uploadBytes(int texture, byte[] data, int stride, int x, int y, int width, int height) {
        if (byteStaging == null || byteStaging.capacity() < data.length) {
            if (byteStaging != null) {
                MemoryUtil.memFree(byteStaging);
            }
            byteStaging = MemoryUtil.memAlloc(data.length);
        }
        //The region's rows at their own offsets, so the upload reads them through the row length.
        for (int row = 0; row < height; row++) {
            int at = (y + row) * stride + x;
            byteStaging.position(at);
            byteStaging.put(data, at, width);
        }
        byteStaging.clear();
        GlStateManager._bindTexture(texture);
        unpack(stride);
        GlStateManager._texSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, width, height, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, MemoryUtil.memAddress(byteStaging) + (long) y * stride + x);
        unpack(0);
        GlStateManager._bindTexture(0);
    }

    @Override
    public void uploadFloats(int texture, float[] data, int width, int rows) {
        int floats = width * rows * 4;
        if (floatStaging == null || floatStaging.capacity() < floats) {
            if (floatStaging != null) {
                MemoryUtil.memFree(floatStaging);
            }
            floatStaging = MemoryUtil.memAllocFloat(floats);
        }
        floatStaging.clear();
        floatStaging.put(data, 0, floats).flip();
        GlStateManager._bindTexture(texture);
        unpack(0);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, rows, GL11.GL_RGBA, GL11.GL_FLOAT, floatStaging);
        GlStateManager._bindTexture(0);
    }

    /** Rows {@code rowLength} wide (0 = tight): NativeImage uploads leave other settings behind. */
    private static void unpack(int rowLength) {
        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, rowLength == 0 ? 4 : 1);
    }

    /** Binds {@code texture} on unit {@code unit} through MC's texture cache, so its own binds stay right. */
    @Override
    public void bindTexture(int unit, int texture) {
        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + unit);
        GlStateManager._bindTexture(texture);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
    }

    @Override
    public Object createTarget(int width, int height, boolean depth, boolean floatColor, boolean linear) {
        Framebuffer made = floatColor ? new FloatTarget(width, height, depth) : new Framebuffer(width, height, depth, Minecraft.ON_OSX);
        if (linear) {
            made.setFilterMode(GL11.GL_LINEAR);
        }
        return made;
    }

    @Override
    public void deleteTarget(Object target) {
        ((Framebuffer) target).destroyBuffers();
    }

    @Override
    public void bindTarget(Object target, boolean viewport) {
        (target == null ? Minecraft.getInstance().getMainRenderTarget() : (Framebuffer) target).bindWrite(viewport);
    }

    @Override
    public void clearTarget(Object target) {
        Framebuffer t = (Framebuffer) target;
        t.setClearColor(0F, 0F, 0F, 0F);
        t.clear(Minecraft.ON_OSX);
    }

    @Override
    public int colorTexture(Object target) {
        return ((Framebuffer) target).getColorTextureId();
    }

    @Override
    public int depthTexture(Object target) {
        return ((Framebuffer) target).getDepthTextureId();
    }

    @Override
    public int mainWidth() {
        return Minecraft.getInstance().getMainRenderTarget().width;
    }

    @Override
    public int mainHeight() {
        return Minecraft.getInstance().getMainRenderTarget().height;
    }

    @Override
    public int mainDepthTexture() {
        //1.16.5's Framebuffer keeps its depth in a texture (also with Forge's combined depth-stencil format).
        return Minecraft.getInstance().getMainRenderTarget().getDepthTextureId();
    }

    @Override
    public void viewport(int x, int y, int width, int height) {
        RenderSystem.viewport(x, y, width, height);
    }

    @Override
    public void clearDepth(int x, int y, int width, int height) {
        //glClear obeys the write masks and the scissor: whatever the last draw left must not keep it from clearing.
        RenderSystem.depthMask(true);
        RenderSystem.enableScissor(x, y, width, height);
        RenderSystem.clearDepth(1.0);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.disableScissor();
    }

    @Override
    public void drawFullscreen(int mode) {
        if (mode == DRAW_DEPTH || mode == DRAW_DEPTH_NEARER) {
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(mode == DRAW_DEPTH ? GL11.GL_ALWAYS : GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(false, false, false, false);
            drawTriangle();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        if (mode == DRAW_REPLACE) {
            RenderSystem.disableBlend();
        } else {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(mode == DRAW_SURFACE ? GlStateManager.SourceFactor.DST_COLOR : GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        }
        drawTriangle();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    /** One triangle over the viewport from {@code gl_VertexID} ({@code fullscreen.vsh}): no vertex buffer but on GL 2.1. */
    private void drawTriangle() {
        if (legacy) {
            if (triangleBuffer == 0) {
                triangleBuffer = GlStateManager._glGenBuffers();
                GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, triangleBuffer);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, new float[9], GL15.GL_STATIC_DRAW);
            }
            drawArray(triangleBuffer, 12, -1, GL11.GL_TRIANGLES, 0, 3);
            return;
        }
        if (emptyArray == 0) {
            emptyArray = GL30.glGenVertexArrays();
        }
        GL30.glBindVertexArray(emptyArray);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    @Override
    public void listLivingBodies(float partialTicks) {
        for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            //Every entity casts (mobs, players, minecarts, boats, item frames, dropped items) except IV's builders (the
            //forwarder's renderer draws the whole IV scene) and a rider of an IV seat, who sits in the vehicle, inside
            //the reach of its own cabin lamps; other riders cast.
            if (!(entity instanceof ABuilderEntityBase) && !(entity.getVehicle() instanceof BuilderEntityLinkedSeat) && !entity.isInvisible()) {
                float h = entity.getBbHeight();
                float w = entity.getBbWidth();
                DynamicLightShadowAtlas.addCandidate(entity, MathHelper.lerp(partialTicks, entity.xOld, entity.getX()), MathHelper.lerp(partialTicks, entity.yOld, entity.getY()) + h * 0.5, MathHelper.lerp(partialTicks, entity.zOld, entity.getZ()), (float) Math.sqrt(w * w * 0.25F + h * h * 0.25F));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void captureLiving(DynamicLightBody body, double cameraX, double cameraY, double cameraZ, float partialTicks) {
        Entity entity = (Entity) body.entity;
        double x = MathHelper.lerp(partialTicks, entity.xOld, entity.getX());
        double y = MathHelper.lerp(partialTicks, entity.yOld, entity.getY());
        double z = MathHelper.lerp(partialTicks, entity.zOld, entity.getZ());
        EntityRenderer<Entity> renderer = (EntityRenderer<Entity>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        MatrixStack poses = new MatrixStack();
        poses.translate(x - cameraX, y - cameraY, z - cameraZ);
        Map<RenderType, IVertexBuilder> views = new IdentityHashMap<>();
        renderer.render(entity, MathHelper.lerp(partialTicks, entity.yRotO, entity.yRot), partialTicks, poses, type -> views.computeIfAbsent(type, t -> {
            int texture = shadowTexture(t);
            if (texture < 0) {
                return new Ignore();
            }
            switch (t.mode()) {
                case GL11.GL_QUADS:
                    return new View(body, true, texture, shadowCut(t));
                case GL11.GL_TRIANGLES:
                    return new View(body, false, texture, shadowCut(t));
                default:
                    return new Ignore();
            }
        }), LightTexture.pack(15, 15));
    }

    /**
     * The GL texture whose alpha cuts {@code type}'s faces out of a shadow, as its alpha test discards them: 0 for a
     * type without a texture (or one not loaded yet, which casts whole until its first draw loads it), -1 for a type
     * whose faces hide nothing: blended other than by alpha (glowing eyes, a charged creeper's swirl, enchantment glint).
     * A type that blends by alpha (entityTranslucent: players, a slime's jelly) casts where its texture is opaque enough
     * ({@link #shadowCut}).
     */
    static int shadowTexture(RenderType type) {
        if (!(type instanceof RenderTypeTypeMixin)) {
            return 0;
        }
        RenderTypeStateMixin state = (RenderTypeStateMixin) (Object) ((RenderTypeTypeMixin) type).get_state();
        if (!States.opaque(state.get_transparencyState()) && !States.alphaBlended(state.get_transparencyState())) {
            return -1;
        }
        Optional<ResourceLocation> texture = ((TextureStateMixin) state.get_textureState()).invoke_texture();
        if (!texture.isPresent()) {
            return 0;
        }
        //getTexture does not load a texture: one never bound yet reads 0, so it casts whole until first drawn.
        Texture loaded = Minecraft.getInstance().getTextureManager().getTexture(texture.get());
        return loaded != null ? loaded.getId() : 0;
    }

    /**
     * The alpha below which {@code type}'s texture casts nothing: {@link DynamicLightBody#CUT_BLENDED} for a type that
     * blends by alpha, so a slime's outer jelly (alpha 0.7) casts nothing while a player's skin and an item sprite
     * (alpha 0 or 1) cast exactly, else the cutout types' {@link DynamicLightBody#CUT_CUTOUT}.
     */
    static float shadowCut(RenderType type) {
        if (type instanceof RenderTypeTypeMixin && States.alphaBlended(((RenderTypeStateMixin) (Object) ((RenderTypeTypeMixin) type).get_state()).get_transparencyState())) {
            return DynamicLightBody.CUT_BLENDED;
        }
        return DynamicLightBody.CUT_CUTOUT;
    }

    /** Only here to reach MC's protected transparency states. */
    private static final class States extends RenderState {
        private States() {
            super(null, null, null);
            throw new IllegalStateException("This class must not be instantiated.");
        }

        static boolean opaque(RenderState.TransparencyState transparency) {
            return transparency == NO_TRANSPARENCY;
        }

        static boolean alphaBlended(RenderState.TransparencyState transparency) {
            return transparency == TRANSLUCENT_TRANSPARENCY;
        }
    }

    @Override
    public void beginBodies() {
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.colorMask(false, false, false, false);
    }

    @Override
    public void drawBody(DynamicLightBody body, int cutLocation) {
        if (body.buffer != null) {
            //IV's cached buffer: quads in MC's NEW_ENTITY format, whose position is the first three floats of each vertex
            //and UV0 two floats at UV_OFFSET.  Its own vertex array, since MC's vertex state is client-side arrays on
            //vertex array 0.
            InterfaceRender.BufferData data = (InterfaceRender.BufferData) body.buffer;
            bindTexture(0, body.texture);
            setCut(cutLocation, body.cut);
            if (legacy) {
                data.buffer.bind();
                drawArray(0, data.stride, UV_OFFSET, GL11.GL_QUADS, 0, data.vertexCount);
                return;
            }
            if (bodyArray == 0) {
                bodyArray = GL30.glGenVertexArrays();
            }
            GL30.glBindVertexArray(bodyArray);
            data.buffer.bind();
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, data.stride, 0L);
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, data.stride, UV_OFFSET);
            GL11.glDrawArrays(GL11.GL_QUADS, 0, data.vertexCount);
            return;
        }
        Corners gpu = (Corners) body.gpu;
        if (gpu == null) {
            body.gpu = gpu = new Corners();
            gpu.positions = GlStateManager._glGenBuffers();
            if (!legacy) {
                gpu.array = GL30.glGenVertexArrays();
                GL30.glBindVertexArray(gpu.array);
                GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, gpu.positions);
                //lamp_body's Position (location 0) and UV0 (location 2), interleaved.
                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 20, 0L);
                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, 20, 12L);
            }
        } else if (!legacy) {
            GL30.glBindVertexArray(gpu.array);
        }
        int quadCorners = body.quadCorners();
        int triangleCorners = body.triangleCorners();
        if (!body.uploaded) {
            body.uploaded = true;
            int floats = (quadCorners + triangleCorners) * 5;
            if (gpu.staging == null || gpu.staging.capacity() < floats) {
                if (gpu.staging != null) {
                    MemoryUtil.memFree(gpu.staging);
                }
                gpu.staging = MemoryUtil.memAllocFloat(floats + floats / 2);
            }
            gpu.staging.clear();
            putCorners(gpu.staging, body.quadPositions, body.quadUVs, quadCorners);
            putCorners(gpu.staging, body.trianglePositions, body.triangleUVs, triangleCorners);
            gpu.staging.flip();
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, gpu.positions);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, gpu.staging, GL15.GL_DYNAMIC_DRAW);
        }
        //A draw per run of one texture and cut, bound on unit 0: quads as quads (the compatibility context has them),
        //then the triangles after them.
        for (int run = 0; run < body.runs(true); run++) {
            int first = body.runStart(true, run) / 4 * 4;
            int count = body.runEnd(true, run) / 4 * 4 - first;
            if (count > 0) {
                bindTexture(0, body.runTexture(true, run));
                setCut(cutLocation, body.runCut(true, run));
                drawCorners(gpu, GL11.GL_QUADS, first, count);
            }
        }
        for (int run = 0; run < body.runs(false); run++) {
            int first = body.runStart(false, run);
            int count = body.runEnd(false, run) - first;
            if (count > 0) {
                bindTexture(0, body.runTexture(false, run));
                setCut(cutLocation, body.runCut(false, run));
                drawCorners(gpu, GL11.GL_TRIANGLES, quadCorners + first, count);
            }
        }
    }

    /** The alpha cut {@code lamp_body} discards under (its {@code alphaCut} uniform); -1: the program has none. */
    private static void setCut(int location, float cut) {
        if (location >= 0) {
            GL20.glUniform1f(location, cut);
        }
    }

    /** {@code count} of a living body's corners from {@code first}: through its vertex array, or on GL 2.1 its buffer. */
    private void drawCorners(Corners gpu, int mode, int first, int count) {
        if (legacy) {
            drawArray(gpu.positions, 20, 12, mode, first, count);
        } else {
            GL11.glDrawArrays(mode, first, count);
        }
    }

    /** {@code corners} corners into {@code staging}: 3 position floats and 2 texture coordinates each. */
    private static void putCorners(FloatBuffer staging, float[] positions, float[] uvs, int corners) {
        for (int c = 0; c < corners; c++) {
            staging.put(positions, 3 * c, 3).put(uvs, 2 * c, 2);
        }
    }

    /**
     * GL 2.1: draws {@code count} corners from {@code first} with attribute 0 (three floats at the start of each
     * {@code stride}-byte vertex) and, unless {@code uvOffset} is negative, attribute 2 (two floats at {@code uvOffset})
     * over {@code buffer} (0: the one bound) on vertex array 0, then turns them off again, since MC's own client-side
     * arrays live there.
     */
    private static void drawArray(int buffer, int stride, int uvOffset, int mode, int first, int count) {
        if (buffer != 0) {
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        }
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0L);
        if (uvOffset >= 0) {
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, stride, uvOffset);
        }
        GL11.glDrawArrays(mode, first, count);
        GL20.glDisableVertexAttribArray(0);
        if (uvOffset >= 0) {
            GL20.glDisableVertexAttribArray(2);
        }
    }

    @Override
    public void endBodies() {
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableCull();
    }

    @Override
    public void restore() {
        GlStateManager._glUseProgram(0);
        //Back to vertex array 0, where MC's client-side vertex state lives, and no buffer bound.
        if (!legacy) {
            GL30.glBindVertexArray(0);
        }
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        //Units 0 to 4: DynamicLightRenderer's samplers.
        for (int unit = 4; unit >= 0; unit--) {
            bindTexture(unit, 0);
        }
        RenderSystem.disableScissor();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    /**
     * A living body's corners on the GPU: a vertex array (none on GL 2.1) over one buffer of positions and texture
     * coordinates, quads then triangles.
     */
    private static final class Corners {
        int array;
        int positions;
        FloatBuffer staging;
    }

    /** A colour target of 16-bit floats: the light runs past 1 near a lamp. */
    private static final class FloatTarget extends Framebuffer {
        FloatTarget(int width, int height, boolean depth) {
            super(width, height, depth, Minecraft.ON_OSX);
        }

        @Override
        public void createBuffers(int width, int height, boolean onOsx) {
            super.createBuffers(width, height, onOsx);
            GlStateManager._bindTexture(getColorTextureId());
            GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, this.width, this.height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (IntBuffer) null);
            GlStateManager._bindTexture(0);
        }
    }

    /** Takes nothing. */
    private static class Ignore implements IVertexBuilder {
        @Override
        public IVertexBuilder vertex(double x, double y, double z) {
            return this;
        }

        @Override
        public void endVertex() {
        }

        @Override
        public IVertexBuilder color(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public IVertexBuilder uv(float u, float v) {
            return this;
        }

        @Override
        public IVertexBuilder overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public IVertexBuilder uv2(int u, int v) {
            return this;
        }

        @Override
        public IVertexBuilder normal(float x, float y, float z) {
            return this;
        }
    }

    /**
     * One render type's way into a body's quads or triangles, with that type's texture: a corner of its own until it
     * ends, so two fed as one still add whole corners.
     */
    private static final class View extends Ignore {
        private final DynamicLightBody body;
        private final boolean quads;
        private final int texture;
        private final float cut;
        private float x;
        private float y;
        private float z;
        private float u;
        private float v;

        View(DynamicLightBody body, boolean quads, int texture, float cut) {
            this.body = body;
            this.quads = quads;
            this.texture = texture;
            this.cut = cut;
        }

        @Override
        public IVertexBuilder vertex(double x, double y, double z) {
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
            return this;
        }

        @Override
        public IVertexBuilder uv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public void endVertex() {
            add(x, y, z, u, v);
        }

        @Override
        public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float nx, float ny, float nz) {
            add(x, y, z, u, v);
        }

        private void add(float x, float y, float z, float u, float v) {
            if (quads) {
                body.addQuadCorner(x, y, z, u, v, texture, cut);
            } else {
                body.addTriangleCorner(x, y, z, u, v, texture, cut);
            }
        }
    }
}
