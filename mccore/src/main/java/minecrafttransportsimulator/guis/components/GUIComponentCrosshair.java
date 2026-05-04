package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;

/**
 * GUI component that renders a weapon aiming crosshair (+) at a dynamically-computed screen position.
 * The crosshair is drawn as four rectangular arms forming a + shape with a small gap at the centre.
 *
 * <p>Usage each frame (from {@link AGUIBase#setStates()}):
 * <ol>
 *   <li>Set {@link #pendingImpactPoint} to the world-space ballistic impact point.</li>
 *   <li>Set {@link #vehicleRef} to the top-level vehicle for partial-tick interpolation.</li>
 *   <li>Set {@link #visible} = {@code true}.</li>
 * </ol>
 *
 * <p>Screen projection is deferred to {@link #render}, where {@code partialTicks} is available.
 * A partial-tick vehicle position correction is applied before projecting to eliminate the
 * discrete per-tick jump that arises when the impact point (tick-time) is projected from a
 * camera that is already at its interpolated position.
 *
 * @author don_bruce
 */
public class GUIComponentCrosshair extends AGUIComponent {
    /** Half-length of each crosshair arm, in screen pixels. */
    private static final int ARM_LENGTH = 5;
    /** Gap between the crosshair centre and the start of each arm, in screen pixels. */
    private static final int CENTER_GAP = 2;
    /** Thickness of each crosshair arm, in screen pixels. */
    private static final float CROSSHAIR_THICKNESS = 0.7F;
    /** Z-layer for the crosshair within the GUI stack. */
    private static final int CROSSHAIR_Z = 50;

    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_ARM = 6;
    private static final int ARM_COUNT = 4;

    /**
     * World-space ballistic impact point, set by
     * {@link minecrafttransportsimulator.guis.instances.GUIOverlay} each frame in
     * {@code setStates()}.  {@code null} means "do not draw".
     */
    public Point3D pendingImpactPoint = null;

    /**
     * Top-level vehicle the gun is mounted on, used for partial-tick position interpolation to
     * smooth per-tick jitter.  May be null for a hand-held player gun.
     */
    public AEntityB_Existing vehicleRef = null;

    /**
     * When {@code true}, applies world-space exponential-moving-average smoothing to the projected
     * impact point each render frame.  This hides the discrete tick-rate barrel direction updates
     * that otherwise cause a jerky crosshair on fast-turning aircraft.
     */
    public boolean applySmoothing = false;

    // EMA state: running smoothed world-space position.
    private final Point3D smoothedImpact = new Point3D();
    private boolean smoothedImpactValid = false;

    // Smoothing speed constant: higher = less lag but more jitter.
    private static final double SMOOTH_K = 30.0;

    // Scratch point reused every render to avoid allocation on the hot path.
    private final Point3D adjustedImpact = new Point3D();

    public GUIComponentCrosshair(int x, int y) {
        super(x, y, ARM_LENGTH * 2, ARM_LENGTH * 2);
        RenderableVertices crosshairVertices = new RenderableVertices("CROSSHAIR", FloatBuffer.allocate(ARM_COUNT * VERTICES_PER_ARM * FLOATS_PER_VERTEX), false);
        setCrosshairGeometry(crosshairVertices.vertices);
        renderable = new RenderableData(crosshairVertices, null);
        renderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        renderable.setColor(ColorRGB.WHITE);
        renderable.setTransucentOverride();
    }

    /**
     * Moves the crosshair centre to the given screen-pixel coordinates (origin = top-left).
     * Called internally from {@link #render} after projection.
     */
    private void updateScreenPosition(int screenX, int screenY) {
        // In the GUI coordinate system used by AGUIComponent, y is stored negative (top = 0).
        position.set(screenX, -screenY, CROSSHAIR_Z);
        renderable.transform.setTranslation(position);
    }

    /** Writes the four rectangular arms in component-local coordinates (centred at 0,0,0). */
    private static void setCrosshairGeometry(FloatBuffer vertices) {
        float halfThickness = CROSSHAIR_THICKNESS / 2.0F;

        addQuad(vertices, CENTER_GAP, -halfThickness, ARM_LENGTH, halfThickness);
        addQuad(vertices, -ARM_LENGTH, -halfThickness, -CENTER_GAP, halfThickness);
        addQuad(vertices, -halfThickness, CENTER_GAP, halfThickness, ARM_LENGTH);
        addQuad(vertices, -halfThickness, -ARM_LENGTH, halfThickness, -CENTER_GAP);
        vertices.flip();
    }

    private static void addQuad(FloatBuffer vertices, float minX, float minY, float maxX, float maxY) {
        addVertex(vertices, minX, minY);
        addVertex(vertices, maxX, minY);
        addVertex(vertices, maxX, maxY);
        addVertex(vertices, minX, minY);
        addVertex(vertices, maxX, maxY);
        addVertex(vertices, minX, maxY);
    }

    private static void addVertex(FloatBuffer vertices, float x, float y) {
        vertices.put(0);
        vertices.put(0);
        vertices.put(1);
        vertices.put(0);
        vertices.put(0);
        vertices.put(x);
        vertices.put(y);
        vertices.put(0);
    }

    @Override
    public int getZOffset() {
        return CROSSHAIR_Z;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        // Only render on the blended (translucent) pass so the crosshair is drawn on top of everything.
        if (!blendingEnabled || pendingImpactPoint == null) {
            // Reset smoothing state so the crosshair snaps to position when it reappears.
            smoothedImpactValid = false;
            return;
        }

        // Apply partial-tick vehicle position correction to eliminate tick-boundary jitter.
        // The impact point is computed at tick time (t=1).  The camera is at partial-tick t,
        // so we shift the impact point back by (1-t) * vehicleVelocity to keep them consistent.
        adjustedImpact.set(pendingImpactPoint);
        if (vehicleRef != null) {
            double dt = 1.0 - partialTicks;
            adjustedImpact.x -= (vehicleRef.position.x - vehicleRef.prevPosition.x) * dt;
            adjustedImpact.y -= (vehicleRef.position.y - vehicleRef.prevPosition.y) * dt;
            adjustedImpact.z -= (vehicleRef.position.z - vehicleRef.prevPosition.z) * dt;
        }

        // Apply world-space EMA smoothing for aircraft to hide per-tick barrel direction jumps.
        // alpha = 1 - exp(-k * dt); at 60 FPS with k=30 this gives ~90% convergence in ~5 frames.
        if (applySmoothing) {
            if (!smoothedImpactValid) {
                smoothedImpact.set(adjustedImpact);
                smoothedImpactValid = true;
            } else {
                double alpha = 1.0 - Math.exp(-SMOOTH_K / 60.0);
                smoothedImpact.x += (adjustedImpact.x - smoothedImpact.x) * alpha;
                smoothedImpact.y += (adjustedImpact.y - smoothedImpact.y) * alpha;
                smoothedImpact.z += (adjustedImpact.z - smoothedImpact.z) * alpha;
            }
            adjustedImpact.set(smoothedImpact);
        }

        // Project world position onto the screen; skip if behind the camera.
        Point3D screen = InterfaceManager.clientInterface.projectToScreen(adjustedImpact, gui.screenWidth, gui.screenHeight);
        if (screen == null) return;

        updateScreenPosition((int) screen.x, (int) screen.y);
        renderable.render();
    }

    /**
     * Convenience accessor: returns the crosshair's current screen-pixel X position.
     */
    public int getScreenX() {
        return (int) position.x;
    }

    /**
     * Convenience accessor: returns the crosshair's current screen-pixel Y position (top = 0).
     */
    public int getScreenY() {
        return (int) -position.y;
    }
}
