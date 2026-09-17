package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONTextureOverlay;
import minecrafttransportsimulator.rendering.DurationDelayClock;

/**
 * Animation switchbox specialized for texture-space overlays.  Only translation and visibility
 * affect overlays; inhibitor and activator entries continue to be handled by the base switchbox.
 */
public class TextureOverlaySwitchbox extends AnimationSwitchbox {
    private final boolean blendedAnimations;
    private float visibilityAlpha = 1.0F;

    public TextureOverlaySwitchbox(AEntityD_Definable<?> entity, JSONTextureOverlay overlayDefinition) {
        super(entity, overlayDefinition.animations, null);
        this.blendedAnimations = overlayDefinition.blendedAnimations;
    }

    @Override
    public void runRotation(DurationDelayClock clock, float partialTicks) {
        //Rotation is intentionally unsupported for texture overlays.
    }

    @Override
    public void runScaling(DurationDelayClock clock, float partialTicks) {
        //Scaling is intentionally unsupported for texture overlays.
    }

    @Override
    protected boolean continueAfterFailedVisibility() {
        //A blended overlay still needs later translations evaluated while its alpha is zero.
        return blendedAnimations;
    }

    @Override
    protected void onRunStart() {
        //If an inhibitor skips visibility this frame, use the same default-visible state that
        //resetTransforms gives to skipped translations rather than reusing last frame's alpha.
        lastVisibilityClock = null;
        lastVisibilityValue = 1.0D;
        visibilityAlpha = 1.0F;
    }

    @Override
    protected void onVisibilityEvaluated(DurationDelayClock clock, double visibilityValue) {
        if (blendedAnimations) {
            double minimum = clock.animation.clampMin;
            double maximum = clock.animation.clampMax;
            float animationAlpha;
            if (maximum <= minimum) {
                //Equal clamps are a useful hard boolean threshold for an otherwise blended layer.
                animationAlpha = visibilityValue >= maximum ? 1.0F : 0.0F;
            } else if (visibilityValue <= minimum) {
                animationAlpha = 0.0F;
            } else if (visibilityValue >= maximum) {
                animationAlpha = 1.0F;
            } else {
                animationAlpha = (float) ((visibilityValue - minimum) / (maximum - minimum));
            }
            //Visibility entries use AND semantics; a later passing entry cannot undo an earlier fade.
            visibilityAlpha = Math.min(visibilityAlpha, animationAlpha);
        }
    }

    public float getVisibilityAlpha() {
        return visibilityAlpha;
    }
}
