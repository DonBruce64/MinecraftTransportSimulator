package minecrafttransportsimulator.rendering;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject.JSONOutline;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;

/** The activation and fade state of an outline on one model object. */
final class ModelOutline {
    private final JSONOutline definition;
    private final AnimationSwitchbox activation;
    private final float maxOpacity;
    private RenderableData renderable;
    private double alpha;
    private double lastSampleTime = Double.NaN;
    private boolean wasActive;

    ModelOutline(AEntityD_Definable<?> entity, JSONOutline definition) {
        this.definition = definition;
        this.maxOpacity = (float) Math.min(definition.opacity, 1.0);
        this.activation = definition.activeAnimations == null || definition.activeAnimations.isEmpty()
                ? null : new AnimationSwitchbox(entity, definition.activeAnimations, null);
    }

    boolean prepare(AEntityD_Definable<?> entity, RenderableVertices source, float partialTicks, float objectAlpha) {
        boolean active = activation == null || activation.runSwitchbox(partialTicks, Double.isNaN(lastSampleTime));
        float opacity = updateAlpha(active, entity.ticksExisted + (double) partialTicks) * maxOpacity * objectAlpha;
        if (opacity <= 0) {
            return false;
        }
        if (renderable == null) {
            RenderableVertices vertices = source.getOutline(definition.outlineScale);
            if (vertices.vertices.limit() == 0) {
                return false;
            }
            //The light texture is solid white.  Use the blended pass even at full opacity so solid
            //geometry has already populated the depth buffer when the outline is drawn.
            renderable = new RenderableData(vertices, "mts:textures/rendering/light.png");
            renderable.setColor(definition.outlineColor != null ? definition.outlineColor : ColorRGB.WHITE);
            renderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
            renderable.setTransucentOverride();
        }
        renderable.setAlpha(opacity);
        return true;
    }

    private float updateAlpha(boolean active, double time) {
        if (time < lastSampleTime) {
            return (float) alpha;
        }
        int fadeTime = active ? definition.fadeInTime : definition.fadeOutTime;
        if (fadeTime <= 0) {
            alpha = active ? 1 : 0;
        } else if (time > lastSampleTime) {
            //Advance the previous fade up to this sample.  If activation reversed, the new fade
            //starts from this alpha on the next sample, including when a frame is skipped.
            int previousFadeTime = wasActive ? definition.fadeInTime : definition.fadeOutTime;
            if (previousFadeTime > 0) {
                double elapsed = (time - lastSampleTime) / previousFadeTime;
                alpha = Math.max(0, Math.min(1, alpha + (wasActive ? elapsed : -elapsed)));
            }
        }
        lastSampleTime = time;
        wasActive = active;
        return (float) alpha;
    }

    void render(TransformationMatrix transform) {
        renderable.transform.set(transform);
        renderable.render();
    }

    void destroy() {
        if (renderable != null) {
            renderable.destroy();
        }
    }
}
