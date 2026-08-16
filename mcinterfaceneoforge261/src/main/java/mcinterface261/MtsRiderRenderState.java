package mcinterface261;

import java.util.IdentityHashMap;

import com.mojang.blaze3d.vertex.PoseStack;

import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Per-entity rider matrix store for MTS render injection.
 *
 * MC 26.1 runs extract for ALL entities before submit for ANY entity,
 * so a single static flag is stomped by each non-riding entity's extractPre
 * before the player's submit runs. Keying on the render-state identity
 * isolates each entity's matrix so ordering doesn't matter.
 *
 * Lives outside the mixin package so mixin classes can import it freely.
 */
public final class MtsRiderRenderState {
    private static final IdentityHashMap<LivingEntityRenderState, TransformationMatrix> PENDING =
        new IdentityHashMap<>();

    public static boolean pushed = false;

    /** Called at extractPre HEAD — clears any stale matrix for this specific state. */
    public static void clear(LivingEntityRenderState state) {
        PENDING.remove(state);
    }

    /**
     * Called at extractPost TAIL (riding entity) — retrieves or creates a
     * TransformationMatrix for this state and returns it ready to be filled.
     */
    public static TransformationMatrix put(LivingEntityRenderState state) {
        TransformationMatrix m = PENDING.get(state);
        if (m == null) {
            m = new TransformationMatrix();
            PENDING.put(state, m);
        }
        m.resetTransforms();
        return m;
    }

    /** Called at submitHead — returns the matrix for this state, or null if not riding. */
    public static TransformationMatrix get(LivingEntityRenderState state) {
        return PENDING.get(state);
    }

    /** Pushes a new pose scope and multiplies the given matrix into it. Sets pushed=true. */
    public static void applyTo(PoseStack poseStack, TransformationMatrix m) {
        poseStack.pushPose();
        poseStack.last().pose().mul(InterfaceRender.convertMatrix4f(m));
        pushed = true;
    }

    private MtsRiderRenderState() {}
}
