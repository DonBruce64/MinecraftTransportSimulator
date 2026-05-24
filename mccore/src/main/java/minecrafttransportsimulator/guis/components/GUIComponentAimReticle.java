package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.MouseFlightController;

/**
 * GUI component that renders a small circle on-screen showing where the mouse
 * aim direction is pointing, relative to the camera view.  Used by the War
 * Thunder-style mouse flight controller.
 * <p>
 * Projects a distant point along the mouse-flight aim vector so camera zoom and
 * free-look modes use the same camera projection path as other dynamic overlays.
 *
 * @author don_bruce
 */
public class GUIComponentAimReticle extends AGUIComponent {

    private static final int CIRCLE_SEGMENTS = 24;
    private static final int CIRCLE_RADIUS = 10;
    private static final float CIRCLE_THICKNESS = 0.5F;
    private static final float CIRCLE_INNER_RADIUS = Math.max(0.0F, CIRCLE_RADIUS - CIRCLE_THICKNESS / 2.0F);
    private static final float CIRCLE_OUTER_RADIUS = CIRCLE_RADIUS + CIRCLE_THICKNESS / 2.0F;
    private static final int AIM_RETICLE_Z = 600;
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_SEGMENT = 6;
    private static final double AIM_PROJECTION_DISTANCE = 512.0D;

    private final RenderableData circleRenderable;
    private int screenWidth;
    private int screenHeight;

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
        circleRenderable.setTransucentOverride();
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

    private static final Point3D aimForward = new Point3D();
    private static final Point3D cameraLocalAim = new Point3D();
    private static final Point3D projectedAimPoint = new Point3D();
    private static final RotationMatrix cameraOrientation = new RotationMatrix();
    private static final RotationMatrix projectionOrientation = new RotationMatrix();

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        boolean mouseFlightActive = MouseFlightController.isMouseFlightActive;
        if (!mouseFlightActive && !shouldRenderArcadeAircraftReticle()) {
            return;
        }
        if (!blendingEnabled) {
            return;
        }

        CameraMode cameraMode = InterfaceManager.clientInterface.getCameraMode();
        boolean freecamThirdPerson = ConfigSystem.client.renderingSettings.freecam_3P.value && cameraMode.thirdPerson;
        PartSeat controllingSeat = getControlledAircraftSeat();
        if (mouseFlightActive) {
            MouseFlightController.getInterpolatedAimForward(aimForward, partialTicks);
        } else {
            aimForward.set(0, 0, 1);
        }

        Point3D projectedScreenPoint = InterfaceManager.clientInterface.projectToScreen(projectedAimPoint.set(InterfaceManager.clientInterface.getCameraPosition()).addScaled(aimForward, AIM_PROJECTION_DISTANCE), screenWidth, screenHeight);
        double screenX;
        double screenY;
        if (projectedScreenPoint != null) {
            screenX = projectedScreenPoint.x;
            screenY = projectedScreenPoint.y;
        } else {
            if (freecamThirdPerson && controllingSeat != null) {
                controllingSeat.getRiderInterpolatedOrientation(cameraOrientation, partialTicks);
            } else if (mouseFlightActive) {
                MouseFlightController.getInterpolatedCameraOrientation(cameraOrientation, partialTicks);
            } else {
                cameraOrientation.setToZero();
            }

            projectionOrientation.set(cameraOrientation);
            if (cameraMode == CameraMode.THIRD_PERSON_INVERTED) {
                projectionOrientation.convertToAngles();
                projectionOrientation.angles.set(-projectionOrientation.angles.x, projectionOrientation.angles.y - 180, -projectionOrientation.angles.z);
                projectionOrientation.updateToAngles();
            }
            cameraLocalAim.set(aimForward);
            projectionOrientation.reOrigin(cameraLocalAim);

            double fov = Math.max(1.0D, Math.min(179.0D, InterfaceManager.clientInterface.getFOV()));
            double tanHalfFOV = Math.tan(Math.toRadians(fov) / 2.0D);
            double aspect = screenHeight > 0 ? (double) screenWidth / screenHeight : 1.0D;
            double depth = Math.max(0.001D, cameraLocalAim.z);
            double ndcX = -cameraLocalAim.x / (depth * tanHalfFOV * aspect);
            double ndcY = cameraLocalAim.y / (depth * tanHalfFOV);
            screenX = (ndcX + 1.0D) / 2.0D * screenWidth;
            screenY = (1.0D - ndcY) / 2.0D * screenHeight;
        }

        // Clamp to screen bounds.
        screenX = Math.max(CIRCLE_OUTER_RADIUS, Math.min(screenWidth - CIRCLE_OUTER_RADIUS, screenX));
        screenY = Math.max(CIRCLE_OUTER_RADIUS, Math.min(screenHeight - CIRCLE_OUTER_RADIUS, screenY));

        // Position uses inverted Y (OpenGL convention in the GUI system).
        circleRenderable.transform.resetTransforms();
        circleRenderable.transform.setTranslation(screenX, -screenY, AIM_RETICLE_Z);
        circleRenderable.render();
    }

    private static boolean shouldRenderArcadeAircraftReticle() {
        if (!ConfigSystem.client.controlSettings.arcadeMode.value) {
            return false;
        }
        return getControlledAircraftSeat() != null;
    }

    private static PartSeat getControlledAircraftSeat() {
        AEntityB_Existing ridingEntity = InterfaceManager.clientInterface.getClientPlayer().getEntityRiding();
        if (ridingEntity instanceof PartSeat) {
            PartSeat seat = (PartSeat) ridingEntity;
            if (seat.placementDefinition.isController
                    && seat.vehicleOn != null
                    && seat.vehicleOn.definition.motorized.isAircraft
                    && !seat.vehicleOn.definition.motorized.isBlimp) {
                return seat;
            }
        }
        return null;
    }
}
