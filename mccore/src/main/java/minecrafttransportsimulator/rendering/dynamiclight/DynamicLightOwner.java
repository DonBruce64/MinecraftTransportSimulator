package minecrafttransportsimulator.rendering.dynamiclight;

import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.jsondefs.JSONLight.JSONLightBlendableComponent;
import minecrafttransportsimulator.rendering.RenderableModelObject;

/**
 * The dynamic light components of one light object of one entity, as {@link RenderableModelObject}
 * last drew it: those marked {@code isDynamicLight} or given a {@code lightPreset}
 * ({@link JSONLightBlendableComponent#isDynamic()}).  The solid pass hands in the object's transform and each component's brightness and
 * color; {@link DynamicLightSystem} turns that into world-space lamps once a frame.  An object its own
 * visibility animations hide goes dark ({@link #hide}).  An owner its entity did not draw this frame runs the
 * object's animations itself ({@link RenderableModelObject#runVisibility}), so its transform, and whether it shows,
 * are this frame's, and takes its brightness from the entity's light values.  Render thread only.
 */
public final class DynamicLightOwner {
    public final int id;
    public final RenderableModelObject modelObject;
    public final AEntityD_Definable<?> entity;
    public final JSONLight lightDef;
    public final Lamp[] lamps;

    /** The object's transform relative to {@code entity.position}, as last drawn. */
    final TransformationMatrix objectTransform = new TransformationMatrix();
    /** The object's animation matrix (its switchbox), to rebuild {@link #objectTransform} when not drawn. */
    final TransformationMatrix animationTransform = new TransformationMatrix();
    long renderedFrame = -1;
    boolean removed;

    private static final RotationMatrix interpolatedOrientation = new RotationMatrix();
    private static final Point3D scratch = new Point3D();

    DynamicLightOwner(int id, RenderableModelObject modelObject, AEntityD_Definable<?> entity, JSONLight lightDef, List<JSONLightBlendableComponent> components) {
        this.id = id;
        this.modelObject = modelObject;
        this.entity = entity;
        this.lightDef = lightDef;
        this.lamps = new Lamp[components.size()];
        for (int i = 0; i < lamps.length; ++i) {
            lamps[i] = new Lamp(this, components.get(i), ((long) id << 16) | i);
        }
    }

    /**
     * Records this frame's drawn state: {@code transform} (relative to the entity position) and {@code animation}
     * (null for none), and per lamp its brightness and color.
     */
    public void update(TransformationMatrix transform, TransformationMatrix animation) {
        objectTransform.set(transform);
        if (animation != null) {
            animationTransform.set(animation);
        } else {
            animationTransform.resetTransforms();
        }
        renderedFrame = DynamicLightSystem.getFrameNumber();
    }

    /** This frame its object is hidden by its own visibility animations: every lamp is dark. */
    public void hide() {
        renderedFrame = DynamicLightSystem.getFrameNumber();
        for (Lamp lamp : lamps) {
            lamp.brightness = 0;
        }
    }

    /**
     * Rebuilds the transform and brightness of an owner its entity did not draw this frame, running the object's
     * animations as a draw would: a hidden object gives no light, a fading one ({@code blendedAnimations}) less.
     */
    void refresh(float partialTicks) {
        //A part its slot's animations hide (or a fake one) is not drawn even on screen.
        if (entity instanceof APart && (((APart) entity).isInvisible || ((APart) entity).isFake())) {
            hide();
            return;
        }
        float shown = modelObject.runVisibility(partialTicks);
        TransformationMatrix animation = modelObject.getAnimationMatrix();
        if (animation != null) {
            animationTransform.set(animation);
        }
        entity.getInterpolatedOrientation(interpolatedOrientation, partialTicks);
        objectTransform.resetTransforms();
        if (entity.requiresDeltaUpdates()) {
            scratch.set(entity.prevPosition).interpolate(entity.position, partialTicks).subtract(entity.position);
            objectTransform.setTranslation(scratch);
        }
        objectTransform.applyRotation(interpolatedOrientation);
        scratch.set(entity.scale).subtract(entity.prevScale).scale(partialTicks).add(entity.prevScale);
        objectTransform.applyScaling(scratch);
        objectTransform.multiply(animationTransform);
        entity.updateLightBrightness(partialTicks);
        float lightLevel = shown * RenderableModelObject.getLightLevel(entity, lightDef);
        for (Lamp lamp : lamps) {
            lamp.setLighting(lightLevel);
        }
    }

    /** Places every lamp in the world from {@link #objectTransform}. */
    void place() {
        TransformationMatrix m = objectTransform;
        for (Lamp lamp : lamps) {
            Point3D p = lamp.component.pos;
            lamp.x = entity.position.x + m.m00 * p.x + m.m01 * p.y + m.m02 * p.z + m.m03;
            lamp.y = entity.position.y + m.m10 * p.x + m.m11 * p.y + m.m12 * p.z + m.m13;
            lamp.z = entity.position.z + m.m20 * p.x + m.m21 * p.y + m.m22 * p.z + m.m23;
            Point3D a = lamp.component.axis;
            double dx = m.m00 * a.x + m.m01 * a.y + m.m02 * a.z;
            double dy = m.m10 * a.x + m.m11 * a.y + m.m12 * a.z;
            double dz = m.m20 * a.x + m.m21 * a.y + m.m22 * a.z;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length > 1e-6) {
                lamp.dx = (float) (dx / length);
                lamp.dy = (float) (dy / length);
                lamp.dz = (float) (dz / length);
            } else {
                lamp.dx = 0;
                lamp.dy = 0;
                lamp.dz = 1;
            }
        }
    }

    /** One dynamic light component of the owner, and what it shows this frame. */
    public static final class Lamp extends DynamicLightSource {
        public final DynamicLightOwner owner;
        public final JSONLightBlendableComponent component;
        private final DynamicLightAim.Beam beam;

        Lamp(DynamicLightOwner owner, JSONLightBlendableComponent component, long key) {
            this.owner = owner;
            this.component = component;
            this.key = key;
            DynamicLightAim.Preset preset = DynamicLightAim.Preset.of(component.lightPreset);
            this.beam = DynamicLightAim.authored(component, preset);
            this.priority = preset != null && preset.isHeadlight() ? DynamicLightAim.HEADLIGHT_PRIORITY : 1F;
            setBeam(beam);
        }

        /**
         * Sets this lamp's brightness and color from the entity's current light values: the component's own color,
         * else the light's.  The light's brightness animations drive it, so it blinks and switches as the pack defines.
         */
        public void setLighting(float lightLevel) {
            AEntityD_Definable<?> entity = owner.entity;
            Float componentBrightness = entity.lightComponentBrightnessValues.get(component);
            brightness = lightLevel * (componentBrightness != null ? componentBrightness : 1) * beam.intensity;
            ColorRGB componentColor = entity.lightComponentColorValues.get(component);
            if (componentColor == null) {
                componentColor = entity.lightColorValues.get(owner.lightDef);
            }
            if (componentColor != null) {
                color.setTo(componentColor);
            }
        }
    }
}
