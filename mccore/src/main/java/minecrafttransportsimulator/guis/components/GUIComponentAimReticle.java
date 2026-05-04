package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.MouseFlightController;

/**
 * GUI component that renders a small circle on-screen showing where the mouse
 * aim direction is pointing, relative to the camera view.  Used by the War
 * Thunder-style mouse flight controller.
 * <p>
 * Uses {@link minecrafttransportsimulator.mcinterface.IInterfaceClient#projectToScreen}
 * for accurate screen positioning that accounts for FOV, aspect ratio, and
 * the MTS-adjusted camera orientation.
 *
 * @author don_bruce
 */
public class GUIComponentAimReticle extends AGUIComponent {

    private static final int CIRCLE_SEGMENTS = 24;
    private static final int CIRCLE_RADIUS = 10;
    private static final float CIRCLE_THICKNESS = 0.5F;
    private static final float CIRCLE_INNER_RADIUS = Math.max(0.0F, CIRCLE_RADIUS - CIRCLE_THICKNESS / 2.0F);
    private static final float CIRCLE_OUTER_RADIUS = CIRCLE_RADIUS + CIRCLE_THICKNESS / 2.0F;
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_SEGMENT = 6;

    /** Distance along the aim vector to place the virtual world point for projection. */
    private static final double AIM_PROJECTION_DISTANCE = 100.0;

    private final RenderableData circleRenderable;
    private int screenWidth;
    private int screenHeight;

    /** Scratch point for computing the world-space aim target. */
    private static final Point3D aimWorldPoint = new Point3D();

    public GUIComponentAimReticle(int screenWidth, int screenHeight) {
        super(0, 0, screenWidth, screenHeight);
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        // Build a thin ring from triangles so thickness is controlled by geometry, not GL line state.
        RenderableVertices circleVertices = new RenderableVertices("AIM_RETICLE", FloatBuffer.allocate(CIRCLE_SEGMENTS * VERTICES_PER_SEGMENT * FLOATS_PER_VERTEX), false);
        FloatBuffer buf = circleVertices.vertices;
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double angle1 = (2 * Math.PI * i) / CIRCLE_SEGMENTS;
            double angle2 = (2 * Math.PI * (i + 1)) / CIRCLE_SEGMENTS;
            addRingVertex(buf, CIRCLE_OUTER_RADIUS, angle1);
            addRingVertex(buf, CIRCLE_OUTER_RADIUS, angle2);
            addRingVertex(buf, CIRCLE_INNER_RADIUS, angle1);
            addRingVertex(buf, CIRCLE_INNER_RADIUS, angle1);
            addRingVertex(buf, CIRCLE_OUTER_RADIUS, angle2);
            addRingVertex(buf, CIRCLE_INNER_RADIUS, angle2);
        }
        buf.flip();

        circleRenderable = new RenderableData(circleVertices);
        circleRenderable.setColor(ColorRGB.WHITE);
        circleRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
    }

    private static void addRingVertex(FloatBuffer buf, float radius, double angle) {
        buf.put(0);
        buf.put(0);
        buf.put(1);
        buf.put(0);
        buf.put(0);
        buf.put((float) (radius * Math.cos(angle)));
        buf.put((float) (radius * Math.sin(angle)));
        buf.put(0);
    }

    /**
     * Update the screen dimensions (called when screen resizes).
     */
    public void updateScreenSize(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /** Scratch point for the interpolated aim-forward direction. */
    private static final Point3D interpAimForward = new Point3D();

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (!MouseFlightController.isMouseFlightActive || blendingEnabled) {
            return;
        }

        // Compute a world-space point along the interpolated aim direction.
        // Interpolate between previous and current tick aim angles to eliminate 20-TPS jitter.
        MouseFlightController.getInterpolatedAimForward(interpAimForward, partialTicks);
        Point3D camPos = InterfaceManager.clientInterface.getCameraPosition();
        aimWorldPoint.set(interpAimForward)
                .scale(AIM_PROJECTION_DISTANCE)
                .add(camPos);

        // Project the world point onto the screen using the engine's camera state.
        Point3D screen = InterfaceManager.clientInterface.projectToScreen(aimWorldPoint, screenWidth, screenHeight);
        if (screen == null) {
            return;
        }

        double screenX = screen.x;
        double screenY = screen.y;

        // Clamp to screen bounds.
        screenX = Math.max(CIRCLE_OUTER_RADIUS, Math.min(screenWidth - CIRCLE_OUTER_RADIUS, screenX));
        screenY = Math.max(CIRCLE_OUTER_RADIUS, Math.min(screenHeight - CIRCLE_OUTER_RADIUS, screenY));

        // Position uses inverted Y (OpenGL convention in the GUI system).
        circleRenderable.transform.resetTransforms();
        circleRenderable.transform.setTranslation(screenX, -screenY, 300);
        circleRenderable.render();
    }
}
