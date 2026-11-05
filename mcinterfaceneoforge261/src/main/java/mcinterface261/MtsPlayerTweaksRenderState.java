package mcinterface261;

import java.util.IdentityHashMap;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public final class MtsPlayerTweaksRenderState {
    private static final IdentityHashMap<HumanoidRenderState, PlayerTweaks> PENDING = new IdentityHashMap<>();

    public static void clear(HumanoidRenderState state) {
        PENDING.remove(state);
    }

    public static PlayerTweaks put(HumanoidRenderState state) {
        PlayerTweaks tweaks = PENDING.get(state);
        if (tweaks == null) {
            tweaks = new PlayerTweaks();
            PENDING.put(state, tweaks);
        }
        tweaks.reset();
        return tweaks;
    }

    public static PlayerTweaks get(HumanoidRenderState state) {
        return PENDING.get(state);
    }

    public static final class PlayerTweaks {
        public boolean setRightArm;
        public boolean setLeftArm;
        public boolean setRightLeg;
        public boolean setLeftLeg;
        public float rightArmXRot;
        public float rightArmYRot;
        public float rightArmZRot;
        public float leftArmXRot;
        public float leftArmYRot;
        public float leftArmZRot;
        public float rightLegXRot;
        public float rightLegYRot;
        public float rightLegZRot;
        public float leftLegXRot;
        public float leftLegYRot;
        public float leftLegZRot;

        private void reset() {
            setRightArm = false;
            setLeftArm = false;
            setRightLeg = false;
            setLeftLeg = false;
        }
    }

    private MtsPlayerTweaksRenderState() {}
}
