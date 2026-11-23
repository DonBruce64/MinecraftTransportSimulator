package minecrafttransportsimulator.rendering;

import minecrafttransportsimulator.rendering.dynamiclight.DynamicLightSystem;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Decides who draws IV's solid scene ({@code doRenderCall(false, ...)}) each frame: the render forwarder entity's
 * renderer, during the entity loop, or the world renderer mixin at the late point of the frame.  The rule lives
 * here once so every version agrees; the adapter only names the Minecraft types (which entity is the viewer).
 * <p>
 * The forwarder of the viewer (the camera entity when it is a player, as when spectating or in a replay, else the
 * local player) draws the solid pass unless {@code forceRenderLastSolid} is on, in which case the mixin does it late.
 * When no forwarder follows the viewer at all, the mixin draws it late too ({@link #needsLateSolid}).
 * <p>
 * A shader pack's shadow pass ({@link DynamicLightSystem#isShaderShadowPass}) is the exception: the mixin's draw does
 * not run there, only the entity loop reaches IV's scene, so the forwarder always draws it, whatever
 * {@code forceRenderLastSolid} says.  That draw does not count as the frame's solid pass: the shadow pass comes
 * before the main one in the frame and must not stop the late fallback of the main pass.
 */
public class ScenePass {
    private static boolean drewSolid;

    /** Call at the start of the world render: no forwarder has drawn the solid pass yet. */
    public static void startFrame() {
        drewSolid = false;
    }

    /**
     * Asked by the forwarder's renderer.
     *
     * @param followsViewer whether this forwarder follows the viewer
     * @return whether it draws the scene now; if so, a draw outside a shadow pass is recorded as the frame's solid pass
     */
    public static boolean forwarderDraws(boolean followsViewer) {
        if (!followsViewer) {
            return false;
        }
        boolean shadowPass = DynamicLightSystem.isShaderShadowPass();
        if (ConfigSystem.settings.general.forceRenderLastSolid.value && !shadowPass) {
            return false;
        }
        if (!shadowPass) {
            drewSolid = true;
        }
        return true;
    }

    /** Whether the mixin must draw the solid pass itself at the late point: no forwarder drew it this frame. */
    public static boolean needsLateSolid() {
        return !drewSolid;
    }
}
