package minecrafttransportsimulator.rendering.dynamiclight;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.jsondefs.JSONLight.JSONLightBlendableComponent;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderableModelObject;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Runs the dynamic lights (light components marked {@code isDynamicLight} or given a {@code lightPreset}, on models
 * through {@link DynamicLightOwner} and on particles through {@link #submitLight}) once a frame: collects every owner's lit lamps, culls and ranks them for the light pass, fills the beams and coronas, keeps the block grid
 * current, and hands the frame to {@link DynamicLightRenderer}.  Under a shader pack the
 * deferred pass cannot draw (the pack replaces the pipeline it reads), nor on a context it cannot run on, so the lamps raise block light
 * through {@link DynamicLightField} instead, and {@link RenderableModelObject} draws a simple cone and
 * flare.  Everything is client-side and derived from synced
 * variables.  Render thread only, except {@link #blockLightAt}, which mesher threads call.
 */
public final class DynamicLightSystem {
    /** What dynamic lights do this frame. */
    public enum Mode {
        /** Dynamic components draw the regular beam and flare. */
        OFF,
        /** The deferred light pass lights the world: no shader pack. */
        DEFERRED,
        /** A shader pack is active, or the deferred pass cannot run on this context: block light, cone mesh and flare billboard. */
        FALLBACK
    }

    /** Merge distance (blocks) of point lights before scoring. */
    private static final float MERGE_DISTANCE = 1.5F;
    /** Budget and corona rank of particle lights: below every lamp, so particles never push out headlights. */
    public static final float PARTICLE_PRIORITY = 0.25F;
    /** At most this many particle lights a frame; the budget merges a burst's point lights. */
    private static final int PARTICLE_MAX = 256;
    /**
     * Share of a light's intensity taken away in full daylight at its lamp: a lamp in a cave lights it however bright
     * the day is where the player stands.
     */
    private static final float DAYLIGHT_DIM = 1.0F;
    /** The visible beam's length as a share of the light's reach, and its cap, blocks, unless lightBeamLength sets it. */
    private static final float BEAM_LENGTH = 0.45F;
    private static final float BEAM_MAX = 24F;
    /**
     * How far ahead of a spot lamp's point along its axis its lens face is, for the corona's depth test.  Legacy packs
     * put a lamp's point up to 30 cm inside the body behind its lens.
     */
    private static final float CORONA_LENS = 0.35F;
    /** The furthest a spot's corona is depth-tested toward the camera, blocks: its lens seen near edge-on. */
    private static final float CORONA_LENS_MAX = 0.7F;
    /** A point light has no lens: its corona is depth-tested this share of its radius toward the camera, the bulb's face. */
    private static final float CORONA_BULB = 0.5F;
    private static final float CORONA_BULB_MIN = 0.05F;
    /** The visible beam's density at night in clear air. */
    private static final float BEAM_DENSITY = 0.06F;
    /** How much rain thickens the air the beam shows in. */
    private static final float BEAM_RAIN = 1.5F;
    /** Time a frame may spend reading block sections into the grid. */
    private static final long VOXEL_BUDGET_NANOS = 700_000L;

    private static final Map<RenderableModelObject, DynamicLightOwner> owners = new LinkedHashMap<>();
    private static final DynamicLightFrame frame = new DynamicLightFrame();
    private static final DynamicLightBudget budget = new DynamicLightBudget();
    private static final List<DynamicLightEmitter> submitted = new ArrayList<>();
    /** This frame's particle lights, one each; keyed by their index, so a steady emitter keeps its keys. */
    private static final List<DynamicLightSource> particleLights = new ArrayList<>();
    private static final List<DynamicLightSource> particlePool = new ArrayList<>();
    /** Beams of particle light components: one per component. */
    private static final Map<JSONLightBlendableComponent, DynamicLightAim.Beam> particleBeams = new IdentityHashMap<>();
    private static final DynamicLightVoxelWindow voxels = new DynamicLightVoxelWindow();
    private static final DynamicLightField field = new DynamicLightField();
    private static final int[] section = new int[3];
    private static final Point3D daylightProbe = new Point3D();
    /** This frame's camera frustum: coronas off screen take no place in the list. */
    private static DynamicLightFrustum cameraFrustum = DynamicLightFrustum.ACCEPT_ALL;

    private static int nextOwnerId;
    private static long frameNumber;
    private static Mode mode = Mode.OFF;
    /** Whether a shader pack was active at the last {@link #renderFrame}. */
    private static boolean shaderPack;
    private static AWrapperWorld lastWorld;
    /** Whether the field's lamps are in the world's meshes, read by mesher threads. */
    private static volatile boolean fieldActive;

    private static boolean irisResolved;
    private static Object irisApi;
    private static Method irisInUse;
    private static boolean optifineResolved;
    private static Field optifineLoaded;

    private DynamicLightSystem() {
    }

    /** Returns the owner of {@code object}'s dynamic components, creating it on first use. */
    public static DynamicLightOwner getOwner(RenderableModelObject object, AEntityD_Definable<?> entity, JSONLight lightDef, List<JSONLightBlendableComponent> components) {
        DynamicLightOwner owner = owners.get(object);
        if (owner == null) {
            owner = new DynamicLightOwner(++nextOwnerId & 0xFFFFFF, object, entity, lightDef, components);
            owners.put(object, owner);
        }
        return owner;
    }

    /** Forgets {@code object}'s owner: its model object was destroyed. */
    public static void removeOwner(RenderableModelObject object) {
        DynamicLightOwner owner = owners.remove(object);
        if (owner != null) {
            owner.removed = true;
        }
    }

    /** This frame's mode, decided by the last {@link #renderFrame}. */
    public static Mode getMode() {
        return mode;
    }

    /**
     * Whether components with {@code hideWhenDynLightActive} are hidden this frame: dynamic lights draw, or a
     * shader pack is active (even with dynamic lights off).
     */
    public static boolean hidesStandIns() {
        return mode != Mode.OFF || shaderPack;
    }

    /** Counts frames; an owner drawn in the current frame carries this number. */
    public static long getFrameNumber() {
        return frameNumber;
    }

    /** The last frame built, for the shader pack fallback drawing in the blended pass. */
    public static DynamicLightFrame getFrame() {
        return frame;
    }

    /**
     * Builds and draws this frame's dynamic lights.  Call once per frame from the world render, after the
     * solid entity pass and before IV's blended pass, also while dynamic lights are off (it then only resets).
     *
     * @param frustum the camera frustum
     * @param left    the camera's left vector (unit, world space)
     * @param up      the camera's up vector (unit, world space)
     * @param view    the world render's camera rotation (16 floats, column-major)
     * @param projection the world render's projection (16 floats, column-major)
     */
    public static void renderFrame(float partialTicks, DynamicLightFrustum frustum, double cameraX, double cameraY, double cameraZ, float[] left, float[] up, float[] view, float[] projection) {
        AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
        try {
            if (world != lastWorld) {
                lastWorld = world;
                owners.clear();
                voxels.reset();
                releaseField();
            }
            shaderPack = world != null && isShaderPackActive();
            Mode newMode = world == null ? Mode.OFF : computeMode();
            if (newMode != Mode.FALLBACK) {
                releaseField();
            }
            mode = newMode;
            frame.clear(Math.min(DynamicLightFrame.CAPACITY, Math.max(0, ConfigSystem.client.renderingSettings.DynL_MaxLights.value)));
            if (newMode == Mode.OFF) {
                return;
            }
            frame.cameraX = cameraX;
            frame.cameraY = cameraY;
            frame.cameraZ = cameraZ;
            frame.partialTicks = partialTicks;
            System.arraycopy(left, 0, frame.cameraLeft, 0, 3);
            System.arraycopy(up, 0, frame.cameraUp, 0, 3);
            frame.shadows = ConfigSystem.client.renderingSettings.DynL_shadows.value;
            updateVoxels(world, cameraX, cameraY, cameraZ);

            frame.rain = InterfaceManager.renderingInterface.getRainLevel(world, partialTicks);
            cameraFrustum = frustum;

            submitted.clear();
            field.beginFrame();
            for (Iterator<DynamicLightOwner> iterator = owners.values().iterator(); iterator.hasNext();) {
                DynamicLightOwner owner = iterator.next();
                if (!owner.entity.isValid || owner.entity.world != world) {
                    owner.removed = true;
                    iterator.remove();
                    continue;
                }
                if (owner.renderedFrame != frameNumber) {
                    owner.refresh(partialTicks);
                }
                owner.place();
                for (DynamicLightOwner.Lamp lamp : owner.lamps) {
                    lamp.showsCone = false;
                    lamp.showsFlare = false;
                    addLamp(world, lamp, newMode);
                }
            }
            for (DynamicLightSource light : particleLights) {
                addLamp(world, light, newMode);
            }
            field.endFrame();

            if (newMode == Mode.FALLBACK) {
                fieldActive = true;
                int count = field.dirtyBoxes(System.currentTimeMillis() / 50);
                if (count > 0) {
                    InterfaceManager.renderingInterface.markLightSectionsDirty(field.boxBuffer(), count);
                }
            } else {
                int cap = Math.min(DynamicLightFrame.CAPACITY, Math.max(0, ConfigSystem.client.renderingSettings.DynL_MaxLights.value));
                budget.select(submitted, cameraX, cameraY, cameraZ, frustum, cap, MERGE_DISTANCE, frame.lights);
                //The beams of the lights kept, so a beam never stands in the air without its light on the ground.
                for (DynamicLightEmitter e : frame.lights) {
                    if (e.beamLength > 0) {
                        frame.beams.add(e.x, e.y, e.z, e.dx, e.dy, e.dz, e.beamTan, e.beamLength, e.red, e.green, e.blue, e.beamDensity);
                    }
                }
                frame.pack();
                DynamicLightRenderer.beginFrame(view, projection, frustum);
                DynamicLightRenderer.render(frame, InterfaceManager.renderingInterface.getDynamicLightGL());
            }
        } finally {
            particleLights.clear();
            ++frameNumber;
        }
    }

    /**
     * Adds one light for this frame from something with no model object (a particle's {@code light}): the component's
     * cone, reach, corona and intensity over its preset, at world {@code x, y, z} aimed along the unit {@code dx, dy, dz}
     * (zero: a point light), in {@code color} at {@code brightness} (times the component's intensity).  Call it while
     * the solid pass draws, before {@link #renderFrame}.  Each call is its own light: the budget then merges a burst's point lights within {@link #MERGE_DISTANCE} into one (added intensity,
     * no cap).  At most {@link #PARTICLE_MAX} a frame.
     */
    public static void submitLight(JSONLightBlendableComponent component, double x, double y, double z, float dx, float dy, float dz, ColorRGB color, float brightness, float priority) {
        if (mode == Mode.OFF || !(brightness > 0) || particleLights.size() >= PARTICLE_MAX) {
            return;
        }
        DynamicLightAim.Beam beam = particleBeams.computeIfAbsent(component, c -> DynamicLightAim.authored(c, DynamicLightAim.Preset.of(c.lightPreset)));
        DynamicLightSource source = obtainParticleSource();
        source.key = (1L << 61) | particleLights.size();
        source.setBeam(beam);
        if (dx * dx + dy * dy + dz * dz > 1e-6F && beam.angle > 0) {
            source.dx = dx;
            source.dy = dy;
            source.dz = dz;
        } else {
            source.angle = 0;
            source.dx = 0;
            source.dy = 0;
            source.dz = 1;
        }
        source.priority = priority;
        source.x = x;
        source.y = y;
        source.z = z;
        source.color.setTo(color);
        source.brightness = brightness * beam.intensity;
        particleLights.add(source);
    }

    /**
     * The light pass's lights at a world point, dimmed by daylight at each lamp, into {@code rgb} (overwritten, 0 and up): what a
     * volume the pass cannot see on the depth buffer (a smoke puff) takes from them.  The same falloff and cone as light_pass.fsh, without surface shading or
     * shadows.  Reads the latest selection, so at most a frame old; zero with no lights selected (dynamic lights off,
     * or a shader pack, whose block light already lights the puff).  Render thread only.
     */
    public static void lightAt(double x, double y, double z, float[] rgb) {
        rgb[0] = 0;
        rgb[1] = 0;
        rgb[2] = 0;
        if (mode != Mode.DEFERRED) {
            return;
        }
        for (DynamicLightEmitter e : frame.lights) {
            double d = e.distanceTo(x, y, z);
            if (d >= e.radius) {
                continue;
            }
            float f = 1F - (float) (d / e.radius);
            float k = f * f * e.intensity;
            if (e.spot) {
                float cos = (float) (((x - e.x) * e.dx + (y - e.y) * e.dy + (z - e.z) * e.dz) / Math.max(d, 0.001));
                k *= DynamicLightAim.cone(e.cosOuter, e.cosInner, cos);
            }
            if (k > 0) {
                rgb[0] += e.red * k;
                rgb[1] += e.green * k;
                rgb[2] += e.blue * k;
            }
        }
    }

    private static DynamicLightSource obtainParticleSource() {
        int used = particleLights.size();
        if (used == particlePool.size()) {
            particlePool.add(new DynamicLightSource());
        }
        return particlePool.get(used);
    }

    /**
     * One light's emitter, corona and beam for this frame.  The daylight at the lamp dims its light in the light pass
     * (not the block light under a shader pack), thins its beam's air and its corona.
     */
    private static void addLamp(AWrapperWorld world, DynamicLightSource lamp, Mode mode) {
        float intensity = lamp.brightness;
        if (!(intensity > 0)) {
            return;
        }
        daylightProbe.set(lamp.x, lamp.y, lamp.z);
        float daylight = Math.max(0F, Math.min(1F, InterfaceManager.renderingInterface.getDaylight(world, daylightProbe)));
        float dim = Math.max(0F, 1F - DAYLIGHT_DIM * daylight);
        float red = lamp.color.red;
        float green = lamp.color.green;
        float blue = lamp.color.blue;

        DynamicLightEmitter emitter = lamp.emitter;
        emitter.key = lamp.key;
        emitter.x = lamp.x;
        emitter.y = lamp.y;
        emitter.z = lamp.z;
        emitter.dx = lamp.dx;
        emitter.dy = lamp.dy;
        emitter.dz = lamp.dz;
        emitter.spot = lamp.angle > 0;
        emitter.angle = lamp.angle;
        emitter.soft = lamp.soft;
        emitter.cosOuter = DynamicLightAim.cosOuter(lamp.angle);
        emitter.cosInner = DynamicLightAim.cosInner(lamp.angle, lamp.soft);
        emitter.red = red;
        emitter.green = green;
        emitter.blue = blue;
        emitter.intensity = mode == Mode.DEFERRED ? intensity * dim : intensity;
        emitter.beamLength = 0;
        emitter.radius = lamp.reach;
        emitter.priority = lamp.priority;
        emitter.clearance = lamp.clearance;
        if (emitter.radius > 0 && emitter.intensity > 0) {
            submitted.add(emitter);
            if (mode == Mode.FALLBACK) {
                field.submit(lamp.key, emitter);
            }
        }

        //Corona of the lamp: drawn by the light pass where the depth shows its lens, or as a billboard under a pack.
        float flareRadius = lamp.coronaRadius;
        float flareBrightness = Math.min(1F, intensity / 2F);
        if (flareRadius > 0 && lamp.bloom > 0 && ConfigSystem.client.renderingSettings.renderFlares.value && (mode != Mode.DEFERRED || cameraFrustum.isSphereVisible(lamp.x, lamp.y, lamp.z, flareRadius))) {
            double tx = frame.cameraX - lamp.x;
            double ty = frame.cameraY - lamp.y;
            double tz = frame.cameraZ - lamp.z;
            float distance = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            //No corona with the camera inside it.
            if (distance > flareRadius) {
                float cosToViewer = (float) ((lamp.dx * tx + lamp.dy * ty + lamp.dz * tz) / distance);
                float visibility = DynamicLightAim.flareVisibility(lamp.angle, lamp.soft, cosToViewer);
                float strength = visibility * flareBrightness;
                if (mode == Mode.DEFERRED) {
                    //Tested on the line from the lamp to the camera, so on screen at the lamp itself: a spot where that
                    //line crosses its lens face, so its own lens never hides it while the hull in front of a lamp seen
                    //from the back does; a point light at its bulb face.  A point off that line (ahead along the axis)
                    //lies in the clear past the hull when a lamp is seen from behind and above.
                    float lens;
                    if (emitter.spot) {
                        lens = cosToViewer > 0 ? Math.min(CORONA_LENS / cosToViewer, CORONA_LENS_MAX) : 0;
                    } else {
                        lens = Math.max(CORONA_BULB_MIN, flareRadius * CORONA_BULB);
                    }
                    float pull = Math.min(distance * 0.5F, lens) / distance;
                    double lx = lamp.x + tx * pull;
                    double ly = lamp.y + ty * pull;
                    double lz = lamp.z + tz * pull;
                    frame.coronas.add(lamp.x, lamp.y, lamp.z, lx, ly, lz, red, green, blue, strength, lamp.bloom, lamp.priority, dim);
                } else {
                    //Grows with distance as a lamp's glare does, so a far lamp is not a dot, until its bloom's reach.
                    lamp.showsFlare = strength > 0;
                    float bloomSize = (float) Math.sqrt(lamp.bloom);
                    lamp.flareSize = flareRadius * bloomSize * (0.8F + 0.2F * visibility) * (1F + Math.min(distance, DynamicLightAim.bloomGrowth(lamp.bloom)) / 24F);
                    lamp.flareStrength = Math.min(1F, strength * bloomSize);
                }
            }
        }

        //The beam seen in the air: lightBeamLength of the reach, else 0.45 of it up to 24 blocks.
        if (emitter.spot && ConfigSystem.client.renderingSettings.renderBeams.value) {
            //Dark air at the lamp shows the beam, rain thickens it.
            float density = BEAM_DENSITY * flareBrightness * (1F - daylight) * (1F + BEAM_RAIN * frame.rain);
            if (density > 0.002F) {
                float length = lamp.beamShare >= 0 ? emitter.radius * lamp.beamShare : Math.min(BEAM_MAX, emitter.radius * BEAM_LENGTH);
                float tan = DynamicLightAim.beamTan(lamp.angle, lamp.soft);
                if (!(length > 0)) {
                    //A negative lightBeamLength: no beam in the air.
                } else if (mode == Mode.DEFERRED) {
                    //Drawn if the budget keeps the light.
                    emitter.beamLength = length;
                    emitter.beamTan = tan;
                    emitter.beamDensity = density;
                } else {
                    lamp.showsCone = true;
                    lamp.coneLength = length;
                    lamp.coneTan = tan;
                    lamp.coneDensity = density;
                }
            }
        }
    }

    /**
     * Recentres the block grid on the camera, reads this frame's share of sections, and hands the renderer every
     * section that changed: read, or emptied as it entered the window (until read, it must be empty on the GPU too).
     */
    private static void updateVoxels(AWrapperWorld world, double cameraX, double cameraY, double cameraZ) {
        voxels.recentre((int) Math.floor(cameraX), (int) Math.floor(cameraY), (int) Math.floor(cameraZ));
        frame.voxels = voxels;
        long end = System.nanoTime() + VOXEL_BUDGET_NANOS;
        while (voxels.poll(section)) {
            if (!InterfaceManager.renderingInterface.readDynamicLightSection(world, section[0], section[1], section[2], voxels)) {
                voxels.fillSection(section[0], section[1], section[2], false);
            }
            if (System.nanoTime() > end) {
                break;
            }
        }
        boolean changed = false;
        while (voxels.pollChanged(section)) {
            frame.addVoxelSection(section[0], section[1], section[2]);
            changed = true;
        }
        if (changed) {
            ++frame.voxelVersion;
        }
    }

    private static Mode computeMode() {
        if (!ConfigSystem.client.renderingSettings.dynamicLights.value) {
            return Mode.OFF;
        }
        if (shaderPack) {
            return ConfigSystem.client.renderingSettings.DynL_ShaderFB.value ? Mode.FALLBACK : Mode.OFF;
        }
        //A context the passes cannot run on (no GL 3.2 nor the GL 2.1 extensions), or whose shaders did not build,
        //still lights the world the way a shader pack frame does.
        if (InterfaceManager.renderingInterface.getDynamicLightGL() == null || DynamicLightRenderer.hasFailed()) {
            return Mode.FALLBACK;
        }
        return Mode.DEFERRED;
    }

    /** Takes the field's lamps out of the world's meshes. */
    private static void releaseField() {
        if (fieldActive) {
            fieldActive = false;
            int count = field.releaseAll();
            if (count > 0) {
                InterfaceManager.renderingInterface.markLightSectionsDirty(field.boxBuffer(), count);
            }
        }
    }

    /**
     * Block light 0..15 the lamps add at a block's center while a shader pack is active, none past a solid
     * block; 0 otherwise.  Any thread.
     */
    public static int blockLightAt(int x, int y, int z) {
        if (!fieldActive || field.isEmpty()) {
            return 0;
        }
        return field.levelAt(x + 0.5, y + 0.5, z + 0.5, voxels);
    }

    /** A block changed on the client: its grid section is read again. */
    public static void markBlock(int x, int y, int z) {
        voxels.markBlock(x, y, z);
    }

    /** A chunk arrived on the client: its grid sections are read again. */
    public static void markColumn(int chunkX, int chunkZ) {
        voxels.markColumn(chunkX, chunkZ);
    }

    /**
     * Whether a shader pack draws the world right now: Iris or Oculus through {@code IrisApi}, or OptiFine
     * through {@code Shaders.shaderPackLoaded}.  Asked every frame, since a pack can be switched live.
     * Reflection only, resolved once; an absent or failing mod counts as no pack.
     */
    public static boolean isShaderPackActive() {
        if (!irisResolved) {
            irisResolved = true;
            for (String name : new String[] { "net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi" }) {
                try {
                    Class<?> api = findModClass(name);
                    irisInUse = api.getMethod("isShaderPackInUse");
                    irisApi = api.getMethod("getInstance").invoke(null);
                    break;
                } catch (ClassNotFoundException absent) {
                    irisInUse = null;
                } catch (Throwable t) {
                    irisInUse = null;
                    InterfaceManager.coreInterface.logError("Iris shader-pack probe failed; treating as no shader pack: " + t);
                    break;
                }
            }
        }
        if (irisInUse != null) {
            try {
                if ((Boolean) irisInUse.invoke(irisApi)) {
                    return true;
                }
            } catch (Throwable t) {
                irisInUse = null;
                InterfaceManager.coreInterface.logError("Iris shader-pack probe failed; treating as no shader pack: " + t);
            }
        }
        if (!optifineResolved) {
            optifineResolved = true;
            try {
                optifineLoaded = findModClass("net.optifine.shaders.Shaders").getField("shaderPackLoaded");
            } catch (Throwable absent) {
                optifineLoaded = null;
            }
        }
        if (optifineLoaded != null) {
            try {
                return optifineLoaded.getBoolean(null);
            } catch (Throwable t) {
                optifineLoaded = null;
            }
        }
        return false;
    }

    /**
     * Loads another mod's class.  Tries the thread's context loader first: where mccore sits in a library layer (a
     * dev run) its own loader cannot see mod classes, while the render thread's context loader is the game's.
     */
    private static Class<?> findModClass(String name) throws ClassNotFoundException {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            try {
                return Class.forName(name, false, context);
            } catch (ClassNotFoundException notThere) {
                //Fall through to mccore's own loader.
            }
        }
        return Class.forName(name);
    }
}
