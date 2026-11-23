package mcinterface1122;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import minecrafttransportsimulator.mcinterface.IInterfaceDynamicLightingGL;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightBody;
import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightShadowAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerCreeperCharge;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerEnderDragonDeath;
import net.minecraft.client.renderer.entity.layers.LayerEnderDragonEyes;
import net.minecraft.client.renderer.entity.layers.LayerEndermanEyes;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerSlimeGel;
import net.minecraft.client.renderer.entity.layers.LayerSpiderEyes;
import net.minecraft.client.renderer.entity.layers.LayerWitherAura;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.ITickableTextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * 1.12.2's GL calls for the shared dynamic light renderer (mccore {@code DynamicLightRenderer}): LWJGL 2 raw GL on the
 * game's own (compatibility) context, through {@link GlStateManager} wherever MC caches the state.  Needs GL 3.2 (GLSL
 * 1.50, framebuffers, RGBA16F), or GL 2.1 with the extensions {@link IInterfaceDynamicLightingGL#legacyContext} names (macOS gives
 * 1.12.2 that one); {@link #supported} says which.  On GL 2.1 the framebuffer calls are ARB_framebuffer_object's
 * (LWJGL 2 loads GL 3.0's only on a 3.0 context and throws on them otherwise), and there are no vertex arrays: every
 * draw goes through attribute 0 on vertex array 0.
 * <p>
 * MC 1.12.2 is fixed function and keeps no state of its own around a pass like this one, so the first call of a pass
 * snapshots every piece of GL state it is going to touch ({@link #ensureSaved}) and {@link #restore} puts it all back.
 * IV's cached models are display lists; {@link #drawBody} calls them with the shadow program bound and their texture on
 * unit 0, which reads their {@code glVertex} as vertex attribute 0 ({@code Position}) and the texture coordinates the
 * list also records as attribute {@link #UV_ATTRIBUTE} ({@code UV0}).  A living entity is drawn by its own renderer in
 * GL feedback mode, which hands back its transformed polygons and texture coordinates instead of drawing them, with
 * the texture and alpha cut of each marked in the stream ({@link MarkingTextures}, {@link MarkedLayer}), so a face
 * shadows only where its texture shows.  Render thread only.
 */
final class InterfaceDynamicLightingGL implements IInterfaceDynamicLightingGL {
    static final InterfaceDynamicLightingGL INSTANCE = new InterfaceDynamicLightingGL();

    /**
     * {@code lamp_body}'s {@code UV0}: a generic attribute no driver aliases to a fixed function one (NVIDIA's 2 is
     * {@code gl_Normal}, 8 {@code gl_MultiTexCoord0}), so IV's display lists can record it beside their own calls.
     */
    static final int UV_ATTRIBUTE = 6;
    /** A living body's corner on the GPU: 3 position floats, then 2 texture coordinates. */
    private static final int CORNER_BYTES = 20;
    /** Floats a vertex in GL_3D_COLOR_TEXTURE feedback: x y z, RGBA, s t r q. */
    private static final int FEEDBACK_VERTEX = 11;
    /**
     * Pass-through tokens in the feedback stream: a GL texture id (never negative) sets the texture of the polygons
     * after it; these set how they cast.  Polygons after {@link #MARK_BLENDED} blend by alpha
     * ({@link DynamicLightBody#CUT_BLENDED}), after {@link #MARK_OPAQUE} they are cutout again and shown, after
     * {@link #MARK_HIDDEN} they cast nothing until the next texture is bound.
     */
    private static final int MARK_BLENDED = -1;
    private static final int MARK_OPAQUE = -2;
    private static final int MARK_HIDDEN = -3;
    /** The additive overlay of enchanted armor (a second pass of the same model): it hides nothing. */
    private static final ResourceLocation ENCHANTED_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    /** The texture units mccore's passes bind (0..4: unit 4 is the block shapes). */
    private static final int UNITS = 5;
    private static Boolean supported;
    private static boolean legacy;

    private int emptyArray;
    /** GL 2.1: three corners for the fullscreen triangle, which draws nothing without an enabled array. */
    private int triangleBuffer;
    private ByteBuffer byteStaging;
    private FloatBuffer floatStaging;
    private FloatBuffer uniformStaging = BufferUtils.createFloatBuffer(16 * 64);
    private final FloatBuffer matrixStaging = BufferUtils.createFloatBuffer(16);
    private final IntBuffer ints = BufferUtils.createIntBuffer(16);
    private final ByteBuffer bytes = BufferUtils.createByteBuffer(16);
    /**
     * Raw polygons of a living body, GL_3D_COLOR_TEXTURE feedback: token, vertex count, then {@link #FEEDBACK_VERTEX}
     * floats per vertex; pass-through tokens carry the texture bound next.
     */
    private FloatBuffer feedback = BufferUtils.createFloatBuffer(1 << 18);
    private int maxViewport;
    private MarkingTextures markingTextures;

    /** A copy of the main target's depth, as a texture ({@link #mainDepthTexture}). */
    private int depthFbo;
    private int depthTexture;
    private int depthWidth;
    private int depthHeight;
    private boolean depthStencil;

    //GL state at the start of a pass, put back by restore().
    private boolean saved;
    private int savedActiveUnit;
    private final int[] savedTextures = new int[UNITS];
    private int savedProgram;
    private int savedVertexArray;
    private int savedArrayBuffer;
    private int savedElementBuffer;
    private int savedFramebuffer;
    private final int[] savedViewport = new int[4];
    private boolean savedScissor;
    private final boolean[] savedColorMask = new boolean[4];
    private boolean savedDepthTest;
    private boolean savedDepthMask;
    private int savedDepthFunc;
    private boolean savedBlend;
    private final int[] savedBlendFunc = new int[4];
    private boolean savedAlphaTest;
    private int savedAlphaFunc;
    private float savedAlphaRef;
    private boolean savedCull;

    private InterfaceDynamicLightingGL() {
    }

    /**
     * Whether this context can run the shared passes: GL 3.2 (core features in a compatibility context), or GL 2.1 with
     * the extensions of {@link IInterfaceDynamicLightingGL#legacyContext}.  Without either mccore lights the world in its fallback
     * mode.
     */
    static boolean supported() {
        if (supported == null) {
            ContextCapabilities caps = null;
            try {
                caps = GLContext.getCapabilities();
            } catch (Throwable e) {
                //No context to ask; stays unsupported.
            }
            boolean modern = caps != null && caps.OpenGL32;
            legacy = !modern && caps != null && caps.OpenGL21 && caps.GL_EXT_gpu_shader4 && caps.GL_ARB_texture_float && caps.GL_ARB_texture_rg && caps.GL_ARB_framebuffer_object;
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

    //GL 3.0's framebuffer calls, or ARB_framebuffer_object's (same names and values) on GL 2.1.
    private static int glGenFramebuffers() {
        return legacy ? ARBFramebufferObject.glGenFramebuffers() : GL30.glGenFramebuffers();
    }

    private static void glBindFramebuffer(int target, int framebuffer) {
        if (legacy) {
            ARBFramebufferObject.glBindFramebuffer(target, framebuffer);
        } else {
            GL30.glBindFramebuffer(target, framebuffer);
        }
    }

    private static void glFramebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
        if (legacy) {
            ARBFramebufferObject.glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
        } else {
            GL30.glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
        }
    }

    private static int glCheckFramebufferStatus(int target) {
        return legacy ? ARBFramebufferObject.glCheckFramebufferStatus(target) : GL30.glCheckFramebufferStatus(target);
    }

    private static void glDeleteFramebuffers(int framebuffer) {
        if (legacy) {
            ARBFramebufferObject.glDeleteFramebuffers(framebuffer);
        } else {
            GL30.glDeleteFramebuffers(framebuffer);
        }
    }

    private static void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        if (legacy) {
            ARBFramebufferObject.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        } else {
            GL30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        }
    }

    /** Snapshots the GL state a pass changes, once per pass; {@link #restore} ends the pass. */
    private void ensureSaved() {
        if (saved) {
            return;
        }
        saved = true;
        savedActiveUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        for (int unit = 0; unit < UNITS; unit++) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + unit);
            savedTextures[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        savedProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        savedVertexArray = legacy ? 0 : GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        savedArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        savedElementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        savedFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        ints.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, ints);
        for (int i = 0; i < 4; i++) {
            savedViewport[i] = ints.get(i);
        }
        savedScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        bytes.clear();
        GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, bytes);
        for (int i = 0; i < 4; i++) {
            savedColorMask[i] = bytes.get(i) != 0;
        }
        savedDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        savedBlendFunc[0] = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        savedBlendFunc[1] = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        savedBlendFunc[2] = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        savedBlendFunc[3] = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        savedAlphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        savedAlphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        savedAlphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        savedCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        //Fixed function state that would act on a shader's output: the alpha test would drop the shadow depth, which is written with alpha 0.
        GlStateManager.disableAlpha();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        unpack(0);
    }

    @Override
    public void restore() {
        if (!saved) {
            return;
        }
        saved = false;
        OpenGlHelper.glUseProgram(savedProgram);
        if (!legacy) {
            GL30.glBindVertexArray(savedVertexArray);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedArrayBuffer);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, savedElementBuffer);
        glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFramebuffer);
        GlStateManager.viewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
        if (savedScissor) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
        GlStateManager.colorMask(savedColorMask[0], savedColorMask[1], savedColorMask[2], savedColorMask[3]);
        if (savedDepthTest) {
            GlStateManager.enableDepth();
        } else {
            GlStateManager.disableDepth();
        }
        GlStateManager.depthMask(savedDepthMask);
        GlStateManager.depthFunc(savedDepthFunc);
        if (savedBlend) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
        }
        GlStateManager.tryBlendFuncSeparate(savedBlendFunc[0], savedBlendFunc[1], savedBlendFunc[2], savedBlendFunc[3]);
        GlStateManager.alphaFunc(savedAlphaFunc, savedAlphaRef);
        if (savedAlphaTest) {
            GlStateManager.enableAlpha();
        } else {
            GlStateManager.disableAlpha();
        }
        if (savedCull) {
            GlStateManager.enableCull();
        } else {
            GlStateManager.disableCull();
        }
        for (int unit = UNITS - 1; unit >= 0; unit--) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + unit);
            GlStateManager.bindTexture(savedTextures[unit]);
        }
        GlStateManager.setActiveTexture(savedActiveUnit);
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
        GL20.glBindAttribLocation(program, UV_ATTRIBUTE, "UV0");
        GL20.glLinkProgram(program);
        GL20.glDetachShader(program, vertex);
        GL20.glDetachShader(program, fragment);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            InterfaceManager.coreInterface.logError("Could not link dynamic light shader " + name + ": " + GL20.glGetProgramInfoLog(program, 8192));
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
            InterfaceManager.coreInterface.logError("Could not compile dynamic light shader " + name + ": " + GL20.glGetShaderInfoLog(shader, 8192));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    @Override
    public void useProgram(int program) {
        ensureSaved();
        OpenGlHelper.glUseProgram(program);
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
        if (uniformStaging.capacity() < size * count) {
            uniformStaging = BufferUtils.createFloatBuffer(size * count);
        }
        uniformStaging.clear();
        uniformStaging.put(values, 0, size * count).flip();
        if (size == 4) {
            GL20.glUniform4(location, uniformStaging);
        } else if (size == 2) {
            GL20.glUniform2(location, uniformStaging);
        } else {
            GL20.glUniform1(location, uniformStaging);
        }
    }

    @Override
    public void setUniformMatrix(int location, float[] matrix) {
        matrixStaging.clear();
        matrixStaging.put(matrix, 0, 16).flip();
        GL20.glUniformMatrix4(location, false, matrixStaging);
    }

    /** A texture nearest- or linear-filtered and edge-clamped, bound on unit 0, with storage for {@code width x height}. */
    private int newTexture(int internalFormat, int width, int height, int format, int type, int filter) {
        ensureSaved();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        int texture = GlStateManager.generateTexture();
        GlStateManager.bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, (ByteBuffer) null);
        return texture;
    }

    @Override
    public int createDataTexture(int width, int height, boolean floats) {
        if (floats) {
            return newTexture(GL30.GL_RGBA32F, width, height, GL11.GL_RGBA, GL11.GL_FLOAT, GL11.GL_NEAREST);
        }
        int texture = newTexture(GL30.GL_R8, width, height, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, GL11.GL_NEAREST);
        //R8 contents start zero.
        ByteBuffer zero = BufferUtils.createByteBuffer(width * height);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, zero);
        return texture;
    }

    @Override
    public void uploadBytes(int texture, byte[] data, int stride, int x, int y, int width, int height) {
        ensureSaved();
        if (byteStaging == null || byteStaging.capacity() < width * height) {
            byteStaging = BufferUtils.createByteBuffer(width * height);
        }
        //The region's rows packed one after another, so the upload needs no row length.
        byteStaging.clear();
        for (int row = 0; row < height; row++) {
            byteStaging.put(data, (y + row) * stride + x, width);
        }
        byteStaging.flip();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.bindTexture(texture);
        unpack(1);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, width, height, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, byteStaging);
        unpack(0);
    }

    @Override
    public void uploadFloats(int texture, float[] data, int width, int rows) {
        ensureSaved();
        int floats = width * rows * 4;
        if (floatStaging == null || floatStaging.capacity() < floats) {
            floatStaging = BufferUtils.createFloatBuffer(floats);
        }
        floatStaging.clear();
        floatStaging.put(data, 0, floats).flip();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.bindTexture(texture);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, rows, GL11.GL_RGBA, GL11.GL_FLOAT, floatStaging);
    }

    /** Tight rows, byte aligned when {@code alignment} is 1: MC's texture uploads leave their own settings behind. */
    private static void unpack(int alignment) {
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment == 0 ? 4 : alignment);
    }

    /** Binds {@code texture} on unit {@code unit} through MC's texture cache, so its own binds stay right. */
    @Override
    public void bindTexture(int unit, int texture) {
        ensureSaved();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + unit);
        GlStateManager.bindTexture(texture);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    @Override
    public Object createTarget(int width, int height, boolean depth, boolean floatColor, boolean linear) {
        Target target = new Target();
        target.width = width;
        target.height = height;
        target.color = floatColor ? newTexture(GL30.GL_RGBA16F, width, height, GL11.GL_RGBA, GL11.GL_FLOAT, linear ? GL11.GL_LINEAR : GL11.GL_NEAREST) : newTexture(GL11.GL_RGBA8, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, linear ? GL11.GL_LINEAR : GL11.GL_NEAREST);
        if (depth) {
            target.depth = newTexture(GL14.GL_DEPTH_COMPONENT24, width, height, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, GL11.GL_NEAREST);
        }
        target.fbo = glGenFramebuffers();
        glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.fbo);
        glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, target.color, 0);
        if (depth) {
            glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, target.depth, 0);
        }
        int status = glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            InterfaceManager.coreInterface.logError("Dynamic light target " + width + "x" + height + " is incomplete: " + status);
        }
        return target;
    }

    @Override
    public void deleteTarget(Object target) {
        ensureSaved();
        Target t = (Target) target;
        glDeleteFramebuffers(t.fbo);
        GlStateManager.deleteTexture(t.color);
        if (t.depth != 0) {
            GlStateManager.deleteTexture(t.depth);
        }
    }

    @Override
    public void bindTarget(Object target, boolean viewport) {
        ensureSaved();
        if (target == null) {
            Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(viewport);
        } else {
            Target t = (Target) target;
            glBindFramebuffer(GL30.GL_FRAMEBUFFER, t.fbo);
            if (viewport) {
                GlStateManager.viewport(0, 0, t.width, t.height);
            }
        }
    }

    @Override
    public void clearTarget(Object target) {
        bindTarget(target, false);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.depthMask(true);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.clearColor(0F, 0F, 0F, 0F);
        GlStateManager.clearDepth(1.0);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public int colorTexture(Object target) {
        return ((Target) target).color;
    }

    @Override
    public int depthTexture(Object target) {
        return ((Target) target).depth;
    }

    @Override
    public int mainWidth() {
        return Minecraft.getMinecraft().getFramebuffer().framebufferWidth;
    }

    @Override
    public int mainHeight() {
        return Minecraft.getMinecraft().getFramebuffer().framebufferHeight;
    }

    /**
     * The main target's depth is a renderbuffer (no texture to sample), so it is blitted, once a call, into a depth
     * texture of the same format: a depth blit needs identical formats, and Forge may give the main target a stencil.
     */
    @Override
    public int mainDepthTexture() {
        ensureSaved();
        Framebuffer main = Minecraft.getMinecraft().getFramebuffer();
        int width = main.framebufferWidth;
        int height = main.framebufferHeight;
        boolean stencil = main.isStencilEnabled();
        if (depthFbo == 0 || depthWidth != width || depthHeight != height || depthStencil != stencil) {
            if (depthFbo != 0) {
                glDeleteFramebuffers(depthFbo);
                GlStateManager.deleteTexture(depthTexture);
            }
            depthWidth = width;
            depthHeight = height;
            depthStencil = stencil;
            if (stencil) {
                depthTexture = newTexture(GL30.GL_DEPTH24_STENCIL8, width, height, GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8, GL11.GL_NEAREST);
            } else {
                depthTexture = newTexture(GL14.GL_DEPTH_COMPONENT24, width, height, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, GL11.GL_NEAREST);
            }
            depthFbo = glGenFramebuffers();
            glBindFramebuffer(GL30.GL_FRAMEBUFFER, depthFbo);
            glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, stencil ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, 0);
            GL11.glDrawBuffer(GL11.GL_NONE);
            GL11.glReadBuffer(GL11.GL_NONE);
            int status = glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                InterfaceManager.coreInterface.logError("Dynamic light main depth copy " + width + "x" + height + " is incomplete: " + status);
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.framebufferObject);
        glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, depthFbo);
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindFramebuffer(false);
        return depthTexture;
    }

    @Override
    public void viewport(int x, int y, int width, int height) {
        GlStateManager.viewport(x, y, width, height);
    }

    @Override
    public void clearDepth(int x, int y, int width, int height) {
        ensureSaved();
        //glClear obeys the write masks and the scissor: whatever the last draw left must not keep it from clearing.
        GlStateManager.depthMask(true);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, width, height);
        GlStateManager.clearDepth(1.0);
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void drawFullscreen(int mode) {
        ensureSaved();
        if (mode == DRAW_DEPTH || mode == DRAW_DEPTH_NEARER) {
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(mode == DRAW_DEPTH ? GL11.GL_ALWAYS : GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            GlStateManager.colorMask(false, false, false, false);
            drawTriangle();
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            return;
        }
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        if (mode == DRAW_REPLACE) {
            GlStateManager.disableBlend();
        } else {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(mode == DRAW_SURFACE ? GlStateManager.SourceFactor.DST_COLOR : GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        }
        drawTriangle();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
    }

    /** One triangle over the viewport from {@code gl_VertexID} ({@code fullscreen.vsh}): no vertex buffer but on GL 2.1. */
    private void drawTriangle() {
        if (legacy) {
            if (triangleBuffer == 0) {
                triangleBuffer = GL15.glGenBuffers();
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, triangleBuffer);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(9), GL15.GL_STATIC_DRAW);
            }
            drawArray(triangleBuffer, GL11.GL_TRIANGLES, 0, 3);
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
        if (Minecraft.getMinecraft().world == null) {
            return;
        }
        java.util.List<Entity> entities = Minecraft.getMinecraft().world.loadedEntityList;
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            //Every entity casts (mobs, players, minecarts, boats, item frames, dropped items) except IV's builders (the
            //forwarder's renderer draws the whole IV scene) and a rider of an IV seat, who sits in the vehicle, inside
            //the reach of its own cabin lamps; other riders cast.
            if (!(entity instanceof ABuilderEntityBase) && !(entity.getRidingEntity() instanceof BuilderEntityLinkedSeat) && !entity.isInvisible()) {
                //The box's centre, not the position: a hanging entity's position is its block's.
                AxisAlignedBB box = entity.getEntityBoundingBox();
                double w = box.maxX - box.minX;
                double h = box.maxY - box.minY;
                double d = box.maxZ - box.minZ;
                double behind = 1 - partialTicks;
                DynamicLightShadowAtlas.addCandidate(entity, (box.minX + box.maxX) * 0.5 - (entity.posX - entity.lastTickPosX) * behind, (box.minY + box.maxY) * 0.5 - (entity.posY - entity.lastTickPosY) * behind, (box.minZ + box.maxZ) * 0.5 - (entity.posZ - entity.lastTickPosZ) * behind, (float) Math.sqrt(w * w * 0.25 + h * h * 0.25 + d * d * 0.25));
            }
        }
    }

    /**
     * Draws the entity with its own renderer in GL feedback mode: nothing is rasterized, and the polygons it would
     * have drawn come back transformed, here by an orthographic view over the box round it (so they can be turned back
     * into camera-relative positions) with a viewport as large as the context allows for precision.  Their texture
     * coordinates come back too, and every texture the renderer binds through MC's texture manager is marked in the
     * stream ({@link MarkingTextures}), so each polygon shadows only where its texture shows.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void captureLiving(DynamicLightBody body, double cameraX, double cameraY, double cameraZ, float partialTicks) {
        Entity entity = (Entity) body.entity;
        Render<Entity> renderer = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(entity);
        if (renderer == null) {
            return;
        }
        ensureSaved();
        if (maxViewport == 0) {
            ints.clear();
            GL11.glGetInteger(GL11.GL_MAX_VIEWPORT_DIMS, ints);
            maxViewport = Math.max(1024, Math.min(8192, Math.min(ints.get(0), ints.get(1))));
        }
        //The box round the entity, camera-relative; held items and wings reach past the sphere the candidate came with.
        double centreX = body.centreX - cameraX;
        double centreY = body.centreY - cameraY;
        double centreZ = body.centreZ - cameraZ;
        float half = body.radius * 1.5F + 1F;
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - cameraX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - cameraY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - cameraZ;
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        //Rendering outside the normal pass sets lightmap coordinates and shifts GL state; both are put back.
        float brightnessX = OpenGlHelper.lastBrightnessX;
        float brightnessY = OpenGlHelper.lastBrightnessY;
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        ints.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, ints);
        int vx = ints.get(0), vy = ints.get(1), vw = ints.get(2), vh = ints.get(3);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        OpenGlHelper.glUseProgram(0);
        //The renderer draws through the Tessellator's client-side arrays, which read from the bound array buffer when one
        //is (a body drawn just before leaves its own, and its vertex array): the driver then reads past that buffer and
        //crashes.  restore() puts both back at the end of the pass.
        if (!legacy) {
            GL30.glBindVertexArray(0);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GlStateManager.disableCull();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(-half, half, -half, half, -half, half);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.translate(-centreX, -centreY, -centreZ);
        GlStateManager.viewport(0, 0, maxViewport, maxViewport);
        //Renderers bind through Minecraft's texture manager, which RenderManager shares: both see the marking one.
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager manager = mc.getRenderManager();
        TextureManager textures = mc.renderEngine;
        TextureManager managerTextures = manager.renderEngine;
        if (markingTextures == null) {
            markingTextures = new MarkingTextures(mc.getResourceManager());
        }
        markingTextures.textures = textures;
        try {
            int written = -1;
            for (int attempt = 0; attempt < 4 && written < 0; attempt++) {
                feedback.clear();
                GL11.glFeedbackBuffer(GL11.GL_3D_COLOR_TEXTURE, feedback);
                GL11.glRenderMode(GL11.GL_FEEDBACK);
                List<LayerRenderer<?>> layers = itemLayers(renderer);
                List<LayerRenderer<?>> all = layers != null ? new ArrayList<>(layers) : null;
                if (layers != null) {
                    layers.clear();
                    for (LayerRenderer<?> layer : all) {
                        if (layer instanceof LayerSlimeGel) {
                            layers.add(new MarkedLayer(layer));
                        } else if (!skippedLayer(layer)) {
                            layers.add(layer);
                        }
                    }
                }
                mc.renderEngine = markingTextures;
                manager.renderEngine = markingTextures;
                try {
                    renderer.doRender(entity, x, y, z, yaw, partialTicks);
                } finally {
                    mc.renderEngine = textures;
                    manager.renderEngine = managerTextures;
                    written = GL11.glRenderMode(GL11.GL_RENDER);
                    if (layers != null) {
                        //Back in their order: layers draw in list order.
                        layers.clear();
                        layers.addAll(all);
                    }
                }
                if (written < 0) {
                    //Overflowed: more room.
                    feedback = BufferUtils.createFloatBuffer(Math.min(1 << 22, feedback.capacity() * 4));
                }
            }
            body.clearLiving();
            if (written > 0) {
                readPolygons(body, written, centreX, centreY, centreZ, half);
            }
        } finally {
            GlStateManager.viewport(vx, vy, vw, vh);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
            if (cull) {
                GlStateManager.enableCull();
            }
            //Entity renderers switch the alpha test on; it would drop the zero-alpha output of every pass after this.
            GlStateManager.disableAlpha();
            OpenGlHelper.glUseProgram(program);
            OpenGlHelper.lastBrightnessX = brightnessX;
            OpenGlHelper.lastBrightnessY = brightnessY;
        }
    }

    /** The renderer's layer list (SRG name of {@code RenderLivingBase.layerRenderers}), or null when it is not a living renderer. */
    @SuppressWarnings("unchecked")
    private static List<LayerRenderer<?>> itemLayers(Render<?> renderer) {
        if (!(renderer instanceof RenderLivingBase)) {
            return null;
        }
        try {
            return (List<LayerRenderer<?>>) ObfuscationReflectionHelper.getPrivateValue(RenderLivingBase.class, (RenderLivingBase<?>) renderer, "field_177097_h");
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Layers left out of a living shadow: held and worn items, which draw through the Tessellator's client arrays that
     * crash some drivers in feedback mode, and the additively blended ones (glowing eyes, a charged creeper's swirl,
     * the wither's aura, the dragon's death rays), which hide nothing.  The one alpha blended layer, a
     * slime's outer jelly, is kept and marked ({@link MarkedLayer}); enchantment glint is dropped by its texture
     * ({@link MarkingTextures}).  Nothing else in 1.12.2 blends by alpha: players, mobs, armor, wool and saddles are
     * alpha tested.
     */
    private static boolean skippedLayer(LayerRenderer<?> layer) {
        return layer instanceof LayerHeldItem || layer instanceof LayerCustomHead || layer instanceof LayerSpiderEyes || layer instanceof LayerEndermanEyes || layer instanceof LayerEnderDragonEyes || layer instanceof LayerCreeperCharge || layer instanceof LayerWitherAura || layer instanceof LayerEnderDragonDeath;
    }

    /**
     * Feedback token values, GL_3D_COLOR_TEXTURE layout: a polygon is {@code 0x0703, n, n x (x, y, z, r, g, b, a, s, t,
     * r, q)} in window coordinates; a pass-through token carries the GL texture of the polygons after it.
     */
    private void readPolygons(DynamicLightBody body, int written, double centreX, double centreY, double centreZ, float half) {
        float scale = 2F * half / maxViewport;
        //No texture until the renderer binds one: such faces cast whole.
        int texture = 0;
        float cut = DynamicLightBody.CUT_CUTOUT;
        boolean hidden = false;
        int i = 0;
        while (i < written) {
            int token = (int) feedback.get(i++);
            if (token == GL11.GL_POLYGON_TOKEN) {
                int n = (int) feedback.get(i++);
                if (hidden) {
                    //Additive: casts nothing.
                } else if (n == 4) {
                    for (int k = 0; k < 4; k++) {
                        corner(body, true, i + k * FEEDBACK_VERTEX, texture, cut, centreX, centreY, centreZ, half, scale);
                    }
                } else {
                    for (int k = 1; k + 1 < n; k++) {
                        corner(body, false, i, texture, cut, centreX, centreY, centreZ, half, scale);
                        corner(body, false, i + k * FEEDBACK_VERTEX, texture, cut, centreX, centreY, centreZ, half, scale);
                        corner(body, false, i + (k + 1) * FEEDBACK_VERTEX, texture, cut, centreX, centreY, centreZ, half, scale);
                    }
                }
                i += n * FEEDBACK_VERTEX;
            } else if (token == GL11.GL_POINT_TOKEN) {
                i += FEEDBACK_VERTEX;
            } else if (token == GL11.GL_LINE_TOKEN || token == GL11.GL_LINE_RESET_TOKEN) {
                i += 2 * FEEDBACK_VERTEX;
            } else if (token == GL11.GL_PASS_THROUGH_TOKEN) {
                int mark = (int) feedback.get(i++);
                if (mark >= 0) {
                    //A texture ends the overlay that hid the polygons before it.
                    texture = mark;
                    hidden = false;
                } else if (mark == MARK_BLENDED) {
                    cut = DynamicLightBody.CUT_BLENDED;
                } else if (mark == MARK_OPAQUE) {
                    cut = DynamicLightBody.CUT_CUTOUT;
                    hidden = false;
                } else if (mark == MARK_HIDDEN) {
                    hidden = true;
                }
            } else {
                //Anything else (bitmaps, pixels) has no geometry; stop rather than misread.
                break;
            }
        }
    }

    /**
     * The feedback vertex at {@code at}, back to camera-relative world (window x y -> ortho eye x y, depth -> eye z),
     * with its texture coordinates (unit 0's, after the texture matrix) on GL {@code texture}, whose texels under alpha
     * {@code cut} cast nothing.
     */
    private void corner(DynamicLightBody body, boolean quad, int at, int texture, float cut, double centreX, double centreY, double centreZ, float half, float scale) {
        float x = (float) (feedback.get(at) * scale - half + centreX);
        float y = (float) (feedback.get(at + 1) * scale - half + centreY);
        float z = (float) (-(feedback.get(at + 2) * 2F - 1F) * half + centreZ);
        float q = feedback.get(at + 10);
        float u = q != 0 ? feedback.get(at + 7) / q : feedback.get(at + 7);
        float v = q != 0 ? feedback.get(at + 8) / q : feedback.get(at + 8);
        if (quad) {
            body.addQuadCorner(x, y, z, u, v, texture, cut);
        } else {
            body.addTriangleCorner(x, y, z, u, v, texture, cut);
        }
    }

    @Override
    public void beginBodies() {
        ensureSaved();
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.colorMask(false, false, false, false);
    }

    @Override
    public void drawBody(DynamicLightBody body, int cutLocation) {
        if (body.buffer != null) {
            //IV's cached model: a display list of glVertex calls, which the shadow program takes as attribute 0, and
            //of their texture coordinates as attribute UV_ATTRIBUTE (InterfaceRender records both).
            if (!legacy) {
                GL30.glBindVertexArray(0);
            }
            bindTexture(0, body.texture);
            setCut(cutLocation, body.cut);
            GL11.glCallList((Integer) body.buffer);
            return;
        }
        Corners gpu = (Corners) body.gpu;
        if (gpu == null) {
            body.gpu = gpu = new Corners();
            gpu.positions = GL15.glGenBuffers();
            if (!legacy) {
                gpu.array = GL30.glGenVertexArrays();
                GL30.glBindVertexArray(gpu.array);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, gpu.positions);
                pointCorners();
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
                gpu.staging = BufferUtils.createFloatBuffer(floats + floats / 2);
            }
            gpu.staging.clear();
            putCorners(gpu.staging, body.quadPositions, body.quadUVs, quadCorners);
            putCorners(gpu.staging, body.trianglePositions, body.triangleUVs, triangleCorners);
            gpu.staging.flip();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, gpu.positions);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, gpu.staging, GL15.GL_DYNAMIC_DRAW);
        }
        //Quads are legal in this compatibility context.  A draw per run of one texture and cut.
        for (int run = 0; run < body.runs(true); run++) {
            int first = body.runStart(true, run);
            int count = body.runEnd(true, run) - first;
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

    /** Sets {@code lamp_body}'s {@code alphaCut} (the program in use), if it has one. */
    private static void setCut(int location, float cut) {
        if (location >= 0) {
            GL20.glUniform1f(location, cut);
        }
    }

    /** {@code corners} corners into {@code staging}: 3 position floats and 2 texture coordinates each. */
    private static void putCorners(FloatBuffer staging, float[] positions, float[] uvs, int corners) {
        for (int c = 0; c < corners; c++) {
            staging.put(positions, 3 * c, 3).put(uvs, 2 * c, 2);
        }
    }

    /** {@code lamp_body}'s Position (location 0) and UV0 ({@link #UV_ATTRIBUTE}) over the bound buffer of corners. */
    private static void pointCorners() {
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, CORNER_BYTES, 0L);
        GL20.glEnableVertexAttribArray(UV_ATTRIBUTE);
        GL20.glVertexAttribPointer(UV_ATTRIBUTE, 2, GL11.GL_FLOAT, false, CORNER_BYTES, 12L);
    }

    /** Draws {@code count} of a living body's corners from {@code first}: through its vertex array, or on GL 2.1 vertex array 0. */
    private static void drawCorners(Corners gpu, int mode, int first, int count) {
        if (!legacy) {
            GL11.glDrawArrays(mode, first, count);
            return;
        }
        //MC's own client-side arrays live on vertex array 0, so the attributes are on only for the draw.
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, gpu.positions);
        pointCorners();
        GL11.glDrawArrays(mode, first, count);
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(UV_ATTRIBUTE);
    }

    /**
     * GL 2.1: draws {@code count} corners from {@code first} with attribute 0 over {@code buffer}'s packed positions on
     * vertex array 0, then turns the attribute off again, since MC's own client-side arrays live there.
     */
    private static void drawArray(int buffer, int mode, int first, int count) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 12, 0L);
        GL11.glDrawArrays(mode, first, count);
        GL20.glDisableVertexAttribArray(0);
    }

    @Override
    public void endBodies() {
        if (!legacy) {
            GL30.glBindVertexArray(0);
        }
        GlStateManager.colorMask(true, true, true, true);
    }

    /** A target: framebuffer object, colour texture and (when asked for) depth texture. */
    private static final class Target {
        int fbo;
        int color;
        int depth;
        int width;
        int height;
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

    /**
     * A layer that blends by alpha, marked in the feedback stream so its polygons cast at
     * {@link DynamicLightBody#CUT_BLENDED}: a slime's outer jelly (alpha 0.7) casts nothing.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final class MarkedLayer implements LayerRenderer {
        private final LayerRenderer layer;

        MarkedLayer(LayerRenderer layer) {
            this.layer = layer;
        }

        @Override
        public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            GL11.glPassThrough(MARK_BLENDED);
            layer.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
            GL11.glPassThrough(MARK_OPAQUE);
        }

        @Override
        public boolean shouldCombineTextures() {
            return layer.shouldCombineTextures();
        }
    }

    /**
     * MC's texture manager for the length of a living capture: it binds through the real one ({@link #textures}) and
     * puts each bound texture's GL id into the feedback stream as a pass-through token, the only state feedback keeps
     * in order with the polygons (the enchantment glint's texture as {@link #MARK_HIDDEN}: it overlays the same model).  A renderer that binds with {@link GlStateManager#bindTexture} directly is not seen,
     * so its faces keep the texture marked before them.
     */
    private static final class MarkingTextures extends TextureManager {
        TextureManager textures;

        MarkingTextures(IResourceManager resources) {
            super(resources);
        }

        @Override
        public void bindTexture(ResourceLocation location) {
            textures.bindTexture(location);
            if (ENCHANTED_GLINT.equals(location)) {
                GL11.glPassThrough(MARK_HIDDEN);
                return;
            }
            ITextureObject texture = textures.getTexture(location);
            GL11.glPassThrough(texture != null ? texture.getGlTextureId() : 0);
        }

        @Override
        public boolean loadTickableTexture(ResourceLocation location, ITickableTextureObject texture) {
            return textures.loadTickableTexture(location, texture);
        }

        @Override
        public boolean loadTexture(ResourceLocation location, ITextureObject texture) {
            return textures.loadTexture(location, texture);
        }

        @Override
        public ITextureObject getTexture(ResourceLocation location) {
            return textures.getTexture(location);
        }

        @Override
        public ResourceLocation getDynamicTextureLocation(String name, DynamicTexture texture) {
            return textures.getDynamicTextureLocation(name, texture);
        }

        @Override
        public void tick() {
            textures.tick();
        }

        @Override
        public void deleteTexture(ResourceLocation location) {
            textures.deleteTexture(location);
        }

        @Override
        public void onResourceManagerReload(IResourceManager resources) {
            textures.onResourceManagerReload(resources);
        }
    }
}
