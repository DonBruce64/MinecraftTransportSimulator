package mcinterface1182;

import org.lwjgl.opengl.ARBDrawBuffersBlend;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GLCapabilities;

/** Applies IV's brightness blend to the scene color output for one draw. */
public final class BrightnessBlending {
    private static final int COLOR_OUTPUT = 0;
    private static boolean active;

    private BrightnessBlending() {
    }

    public static void setActive(boolean active) {
        BrightnessBlending.active = active;
    }

    /** Saves the shader pack's blend state and returns the action that restores it after the draw. */
    public static Runnable beginDraw() {
        if (!active) {
            return null;
        }

        GLCapabilities caps = GL.getCapabilities();
        boolean indexed = caps.OpenGL40 || caps.GL_ARB_draw_buffers_blend;
        if (!caps.OpenGL30 || GL11.glGetInteger(GL20.GL_DRAW_BUFFER0) == GL11.GL_NONE || (!indexed && hasMultipleOutputs())) {
            return null;
        }

        boolean enabled = GL30.glIsEnabledi(GL11.GL_BLEND, COLOR_OUTPUT);
        int srcRGB = getBlendParameter(GL14.GL_BLEND_SRC_RGB, indexed);
        int dstRGB = getBlendParameter(GL14.GL_BLEND_DST_RGB, indexed);
        int equationRGB = getBlendParameter(GL20.GL_BLEND_EQUATION_RGB, indexed);
        if (enabled && srcRGB == GL11.GL_DST_COLOR && dstRGB == GL11.GL_SRC_ALPHA && equationRGB == GL14.GL_FUNC_ADD) {
            return null;
        }

        int srcAlpha = getBlendParameter(GL14.GL_BLEND_SRC_ALPHA, indexed);
        int dstAlpha = getBlendParameter(GL14.GL_BLEND_DST_ALPHA, indexed);
        int equationAlpha = getBlendParameter(GL20.GL_BLEND_EQUATION_ALPHA, indexed);
        //The pack can lock RenderSystem blend calls.  Temporarily change GL without changing its state cache.
        setBlendFactors(caps, GL11.GL_DST_COLOR, GL11.GL_SRC_ALPHA, enabled ? srcAlpha : GL11.GL_ONE, enabled ? dstAlpha : GL11.GL_ZERO);
        setBlendEquations(caps, GL14.GL_FUNC_ADD, enabled ? equationAlpha : GL14.GL_FUNC_ADD);
        if (!enabled) {
            GL30.glEnablei(GL11.GL_BLEND, COLOR_OUTPUT);
        }
        return () -> {
            if (!enabled) {
                GL30.glDisablei(GL11.GL_BLEND, COLOR_OUTPUT);
            }
            setBlendFactors(caps, srcRGB, dstRGB, srcAlpha, dstAlpha);
            setBlendEquations(caps, equationRGB, equationAlpha);
        };
    }

    private static boolean hasMultipleOutputs() {
        int maxOutputs = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
        for (int output = 1; output < maxOutputs; ++output) {
            if (GL11.glGetInteger(GL20.GL_DRAW_BUFFER0 + output) != GL11.GL_NONE) {
                return true;
            }
        }
        return false;
    }

    private static int getBlendParameter(int parameter, boolean indexed) {
        return indexed ? GL30.glGetIntegeri(parameter, COLOR_OUTPUT) : GL11.glGetInteger(parameter);
    }

    private static void setBlendFactors(GLCapabilities caps, int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        if (caps.OpenGL40) {
            GL40.glBlendFuncSeparatei(COLOR_OUTPUT, srcRGB, dstRGB, srcAlpha, dstAlpha);
        } else if (caps.GL_ARB_draw_buffers_blend) {
            ARBDrawBuffersBlend.glBlendFuncSeparateiARB(COLOR_OUTPUT, srcRGB, dstRGB, srcAlpha, dstAlpha);
        } else {
            GL14.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
        }
    }

    private static void setBlendEquations(GLCapabilities caps, int equationRGB, int equationAlpha) {
        if (caps.OpenGL40) {
            GL40.glBlendEquationSeparatei(COLOR_OUTPUT, equationRGB, equationAlpha);
        } else if (caps.GL_ARB_draw_buffers_blend) {
            ARBDrawBuffersBlend.glBlendEquationSeparateiARB(COLOR_OUTPUT, equationRGB, equationAlpha);
        } else {
            GL20.glBlendEquationSeparate(equationRGB, equationAlpha);
        }
    }
}
