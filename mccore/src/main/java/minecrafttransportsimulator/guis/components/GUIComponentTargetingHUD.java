package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.jsondefs.JSONMuzzle;
import minecrafttransportsimulator.jsondefs.JSONPart.LockOnType;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.LanguageSystem;

/**
 * Renders the targeting information for the currently-selected guided weapon.  The large ring is
 * the weapon's angular acquisition cone, projected around the actual muzzle direction rather than
 * around the middle of the player's screen.  The selected target gets an animated set of corner
 * brackets and a TRACKING/LOCKED state label.
 *
 * <p>This component deliberately consumes the target selected by {@link PartGun}; it does not run
 * a second target search.  This keeps the HUD in agreement with the target that will actually be
 * passed to a fired missile.
 */
public class GUIComponentTargetingHUD extends AGUIComponent {
    private static final int CIRCLE_SEGMENTS = 64;
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_QUAD = 6;
    private static final int BRACKET_QUADS = 8;
    private static final int VALID_TARGET_QUADS = 4;
    private static final float RING_THICKNESS = 0.375F;
    private static final float BRACKET_THICKNESS = 1.25F;
    private static final float VALID_TARGET_THICKNESS = 0.6F;
    private static final float VALID_TARGET_PADDING = 2.0F;
    private static final float VALID_TARGET_SCALE = 0.5F;
    private static final float BRACKET_MINIMUM_SIZE = 12.0F;
    private static final float BRACKET_CORNER_LENGTH = 8.0F;
    private static final float TRACKING_START_PADDING = 28.0F;
    private static final float LOCKED_PADDING = 3.0F;
    private static final int ACQUISITION_ANIMATION_TICKS = 10;
    private static final int TARGETING_Z = 610;
    private static final double RING_SMOOTHING_SPEED = 24.0D;
    private static final long RING_SMOOTHING_RESET_NANOS = 250000000L;

    private final RenderableData ringRenderable;
    private final RenderableData validTargetRenderable;
    private final RenderableData bracketRenderable;
    private final FloatBuffer ringVertices;
    private final FloatBuffer validTargetVertices;
    private final FloatBuffer bracketVertices;

    private final Point3D muzzlePosition = new Point3D();
    private final Point3D muzzleVelocity = new Point3D();
    private final RotationMatrix muzzleOrientation = new RotationMatrix();
    private final Point3D coneAxisPoint = new Point3D();
    private final Point3D adjustedTargetCenter = new Point3D();
    private final Point3D targetCorner = new Point3D();
    private final float[] projectedBounds = new float[4];

    private PartGun activeGun;
    private PartGun targetGun;
    private PartGun trackedTargetGun;
    private Object trackedTarget;
    private long acquisitionStartTick;
    private double coneAngle;
    private double coneRange;
    private boolean targetOnScreen;
    private boolean targetLocked;
    private float targetMinX;
    private float targetMinY;
    private float targetMaxX;
    private float targetMaxY;
    private boolean smoothedRingCenterValid;
    private double smoothedRingCenterX;
    private double smoothedRingCenterY;
    private long lastRingRenderNanos;

    public GUIComponentTargetingHUD(int screenWidth, int screenHeight) {
        super(0, 0, screenWidth, screenHeight);
        text = "";

        RenderableVertices ringVertexObject = new RenderableVertices("TARGETING_RING", FloatBuffer.allocate(CIRCLE_SEGMENTS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX), false);
        ringVertices = ringVertexObject.vertices;
        ringVertices.limit(0);
        ringRenderable = new RenderableData(ringVertexObject);
        ringRenderable.setColor(ColorRGB.WHITE);
        ringRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        ringRenderable.setTransucentOverride();

        RenderableVertices validTargetVertexObject = new RenderableVertices("VALID_TARGET_BOX", FloatBuffer.allocate(VALID_TARGET_QUADS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX), false);
        validTargetVertices = validTargetVertexObject.vertices;
        validTargetVertices.limit(0);
        validTargetRenderable = new RenderableData(validTargetVertexObject);
        validTargetRenderable.setColor(ColorRGB.WHITE);
        validTargetRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        validTargetRenderable.setTransucentOverride();

        RenderableVertices targetVertices = new RenderableVertices("TARGETING_BRACKETS", FloatBuffer.allocate(BRACKET_QUADS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX), false);
        bracketVertices = targetVertices.vertices;
        bracketVertices.limit(0);
        bracketRenderable = new RenderableData(targetVertices);
        bracketRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        bracketRenderable.setTransucentOverride();
    }

    /**
     * Selects the gun and target represented by this component.  The first gun supplies the cone,
     * while the first gun in the group with a target supplies the target marker.
     */
    public void setGunGroup(List<PartGun> gunGroup) {
        PartGun selectedGun = gunGroup != null && !gunGroup.isEmpty() ? gunGroup.get(0) : null;
        if (activeGun != selectedGun) {
            smoothedRingCenterValid = false;
            lastRingRenderNanos = 0;
        }
        activeGun = selectedGun;
        targetGun = null;
        coneAngle = 0;
        coneRange = 0;

        if (activeGun != null) {
            boolean guidedBullet = activeGun.lastLoadedBullet != null && activeGun.lastLoadedBullet.definition.bullet.turnRate > 0;
            if (activeGun.definition.gun.lockOnType == LockOnType.MANUAL) {
                //Manual guidance has no separate gun-side cone.  Use an explicitly-configured
                //missile seeker cone when present so packs can still expose its steering envelope.
                if (guidedBullet) {
                    coneAngle = activeGun.lastLoadedBullet.definition.bullet.seekerMaxAngle;
                    coneRange = activeGun.lastLoadedBullet.definition.bullet.seekerRange;
                }
            } else if (activeGun.canCurrentlyLockTargets() || guidedBullet) {
                coneAngle = activeGun.definition.gun.lockMaxAngle;
                coneRange = activeGun.definition.gun.lockRange;
                if ((coneAngle <= 0 || coneRange <= 0) && guidedBullet) {
                    coneAngle = activeGun.lastLoadedBullet.definition.bullet.seekerMaxAngle;
                    coneRange = activeGun.lastLoadedBullet.definition.bullet.seekerRange;
                }
            }

            for (PartGun gun : gunGroup) {
                if (gun.engineTarget != null || gun.entityTarget != null) {
                    targetGun = gun;
                    break;
                }
            }
        }

        Object currentTarget = null;
        if (targetGun != null) {
            currentTarget = targetGun.engineTarget != null ? targetGun.engineTarget.vehicleOn : targetGun.entityTarget;
        }
        if (currentTarget != trackedTarget || targetGun != trackedTargetGun) {
            trackedTarget = currentTarget;
            trackedTargetGun = targetGun;
            acquisitionStartTick = targetGun != null ? targetGun.ticksExisted : 0;
        }

        visible = activeGun != null && ((coneAngle > 0 && coneRange > 0) || trackedTarget != null);
        if (!visible) {
            targetOnScreen = false;
            text = "";
        }
    }

    private static void addRingVertex(FloatBuffer vertices, float radius, double angle) {
        addVertex(vertices, (float) (radius * Math.cos(angle)), (float) (radius * Math.sin(angle)));
    }

    private void setRingGeometry(float radius) {
        float innerRadius = Math.max(0, radius - RING_THICKNESS);
        ringVertices.clear();
        for (int segment = 0; segment < CIRCLE_SEGMENTS; ++segment) {
            double angle1 = Math.PI * 2D * segment / CIRCLE_SEGMENTS;
            double angle2 = Math.PI * 2D * (segment + 1) / CIRCLE_SEGMENTS;
            addRingVertex(ringVertices, radius, angle1);
            addRingVertex(ringVertices, radius, angle2);
            addRingVertex(ringVertices, innerRadius, angle1);
            addRingVertex(ringVertices, innerRadius, angle1);
            addRingVertex(ringVertices, radius, angle2);
            addRingVertex(ringVertices, innerRadius, angle2);
        }
        ringVertices.flip();
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
        return TARGETING_Z;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (activeGun == null) {
            return;
        }

        updateTargetProjection(gui, partialTicks);
        if (!blendingEnabled) {
            return;
        }

        renderAcquisitionRing(gui, partialTicks);
        renderValidTargets(gui, partialTicks);
        if (targetOnScreen) {
            bracketRenderable.setColor(targetLocked ? ColorRGB.RED : ColorRGB.WHITE);
            bracketRenderable.transform.resetTransforms();
            bracketRenderable.transform.setTranslation(0, 0, TARGETING_Z + 5);
            bracketRenderable.render();
        }
    }

    private void renderAcquisitionRing(AGUIBase gui, float partialTicks) {
        if (coneAngle <= 0 || coneRange <= 0) {
            return;
        }

        JSONMuzzle muzzle = activeGun.getActiveMuzzle();
        if (muzzle == null) {
            return;
        }
        activeGun.setBulletSpawn(muzzlePosition, muzzleVelocity, muzzleOrientation, muzzle, false);
        applyVehicleInterpolation(muzzlePosition, activeGun.vehicleOn, partialTicks);
        coneAxisPoint.set(0, 0, coneRange).rotate(muzzleOrientation).add(muzzlePosition);
        Point3D projectedCenter = InterfaceManager.clientInterface.projectToScreen(coneAxisPoint, gui.screenWidth, gui.screenHeight);
        if (projectedCenter == null) {
            smoothedRingCenterValid = false;
            lastRingRenderNanos = 0;
            return;
        }
        double centerX = projectedCenter.x;
        double centerY = projectedCenter.y;

        //Muzzle transforms update on game ticks.  Move the displayed centre toward each new
        //sample every render frame so the ring remains fluid at high client frame rates.
        long renderNanos = System.nanoTime();
        long elapsedNanos = renderNanos - lastRingRenderNanos;
        if (!smoothedRingCenterValid || lastRingRenderNanos == 0 || elapsedNanos <= 0 || elapsedNanos > RING_SMOOTHING_RESET_NANOS) {
            smoothedRingCenterX = centerX;
            smoothedRingCenterY = centerY;
            smoothedRingCenterValid = true;
        } else {
            double elapsedSeconds = elapsedNanos / 1000000000.0D;
            double blend = 1.0D - Math.exp(-RING_SMOOTHING_SPEED * elapsedSeconds);
            smoothedRingCenterX += (centerX - smoothedRingCenterX) * blend;
            smoothedRingCenterY += (centerY - smoothedRingCenterY) * blend;
        }
        lastRingRenderNanos = renderNanos;

        //The interface FOV uses the vertical screen axis, matching this conversion.  The ring is
        //kept circular in screen space so it remains readable when its centre approaches an edge.
        double safeAngle = Math.min(89.0D, coneAngle);
        double halfFOV = Math.toRadians(Math.max(1.0D, Math.min(179.0D, InterfaceManager.clientInterface.getFOV()))) / 2D;
        double radius = gui.screenHeight / 2D * Math.tan(Math.toRadians(safeAngle)) / Math.tan(halfFOV);
        if (!Double.isFinite(radius) || radius < 1.0D) {
            return;
        }

        setRingGeometry((float) radius);
        ringRenderable.transform.resetTransforms();
        ringRenderable.transform.setTranslation(smoothedRingCenterX, -smoothedRingCenterY, TARGETING_Z);
        ringRenderable.render();
    }

    private void renderValidTargets(AGUIBase gui, float partialTicks) {
        for (EntityVehicleF_Physics vehicle : activeGun.validVehicleTargets) {
            if (vehicle.isValid && vehicle != trackedTarget && projectBounds(vehicle.boundingBox, vehicle, gui, partialTicks)) {
                renderValidTargetBox();
            }
        }
        for (IWrapperEntity entity : activeGun.validEntityTargets) {
            if (entity.isValid() && !entity.equals(trackedTarget) && projectBounds(entity.getBounds(), null, gui, partialTicks)) {
                renderValidTargetBox();
            }
        }
    }

    private void renderValidTargetBox() {
        float originalMinX = projectedBounds[0] - VALID_TARGET_PADDING;
        float originalMinY = projectedBounds[1] - VALID_TARGET_PADDING;
        float originalMaxX = projectedBounds[2] + VALID_TARGET_PADDING;
        float originalMaxY = projectedBounds[3] + VALID_TARGET_PADDING;
        float centerX = (originalMinX + originalMaxX) / 2F;
        float centerY = (originalMinY + originalMaxY) / 2F;
        float halfWidth = (originalMaxX - originalMinX) * VALID_TARGET_SCALE / 2F;
        float halfHeight = (originalMaxY - originalMinY) * VALID_TARGET_SCALE / 2F;
        float minX = centerX - halfWidth;
        float minY = centerY - halfHeight;
        float maxX = centerX + halfWidth;
        float maxY = centerY + halfHeight;
        float halfThickness = VALID_TARGET_THICKNESS / 2F;
        validTargetVertices.clear();
        addScreenQuad(validTargetVertices, minX, minY - halfThickness, maxX, minY + halfThickness);
        addScreenQuad(validTargetVertices, minX, maxY - halfThickness, maxX, maxY + halfThickness);
        addScreenQuad(validTargetVertices, minX - halfThickness, minY, minX + halfThickness, maxY);
        addScreenQuad(validTargetVertices, maxX - halfThickness, minY, maxX + halfThickness, maxY);
        validTargetVertices.flip();
        validTargetRenderable.transform.resetTransforms();
        validTargetRenderable.transform.setTranslation(0, 0, TARGETING_Z + 2);
        validTargetRenderable.render();
    }

    private void updateTargetProjection(AGUIBase gui, float partialTicks) {
        targetOnScreen = false;
        text = "";
        if (targetGun == null || trackedTarget == null) {
            return;
        }

        BoundingBox bounds;
        AEntityB_Existing movingTarget = null;
        if (targetGun.engineTarget != null) {
            PartEngine engine = targetGun.engineTarget;
            if (!engine.isValid || engine.vehicleOn == null) {
                return;
            }
            movingTarget = engine.vehicleOn;
            bounds = movingTarget.boundingBox;
        } else {
            IWrapperEntity entity = targetGun.entityTarget;
            if (entity == null || !entity.isValid()) {
                return;
            }
            bounds = entity.getBounds();
        }

        if (!projectBounds(bounds, movingTarget, gui, partialTicks)) {
            return;
        }

        double minX = projectedBounds[0];
        double minY = projectedBounds[1];
        double maxX = projectedBounds[2];
        double maxY = projectedBounds[3];

        double acquisitionProgress = Math.min(1.0D, Math.max(0.0D, (targetGun.ticksExisted - acquisitionStartTick + partialTicks) / ACQUISITION_ANIMATION_TICKS));
        targetLocked = acquisitionProgress >= 1.0D;
        double padding = LOCKED_PADDING + (TRACKING_START_PADDING - LOCKED_PADDING) * (1.0D - acquisitionProgress);
        targetMinX = (float) (minX - padding);
        targetMinY = (float) (minY - padding);
        targetMaxX = (float) (maxX + padding);
        targetMaxY = (float) (maxY + padding);
        setBracketGeometry();

        text = targetLocked ? LanguageSystem.GUI_TARGET_LOCKED.getCurrentValue() : LanguageSystem.GUI_TARGET_TRACKING.getCurrentValue();
        textPosition.set(targetMaxX + 5, -targetMinY - 2, TARGETING_Z + 20);
        targetOnScreen = true;
    }

    private boolean projectBounds(BoundingBox bounds, AEntityB_Existing movingTarget, AGUIBase gui, float partialTicks) {
        adjustedTargetCenter.set(bounds.globalCenter);
        applyVehicleInterpolation(adjustedTargetCenter, movingTarget, partialTicks);
        Point3D projectedCenter = InterfaceManager.clientInterface.projectToScreen(adjustedTargetCenter, gui.screenWidth, gui.screenHeight);
        if (projectedCenter == null) {
            return false;
        }

        double minX = projectedCenter.x;
        double minY = projectedCenter.y;
        double maxX = projectedCenter.x;
        double maxY = projectedCenter.y;
        for (int xSign = -1; xSign <= 1; xSign += 2) {
            for (int ySign = -1; ySign <= 1; ySign += 2) {
                for (int zSign = -1; zSign <= 1; zSign += 2) {
                    targetCorner.set(adjustedTargetCenter).add(xSign * bounds.widthRadius, ySign * bounds.heightRadius, zSign * bounds.depthRadius);
                    Point3D projectedCorner = InterfaceManager.clientInterface.projectToScreen(targetCorner, gui.screenWidth, gui.screenHeight);
                    if (projectedCorner != null) {
                        minX = Math.min(minX, projectedCorner.x);
                        minY = Math.min(minY, projectedCorner.y);
                        maxX = Math.max(maxX, projectedCorner.x);
                        maxY = Math.max(maxY, projectedCorner.y);
                    }
                }
            }
        }

        if (maxX - minX < BRACKET_MINIMUM_SIZE) {
            double extension = (BRACKET_MINIMUM_SIZE - (maxX - minX)) / 2D;
            minX -= extension;
            maxX += extension;
        }
        if (maxY - minY < BRACKET_MINIMUM_SIZE) {
            double extension = (BRACKET_MINIMUM_SIZE - (maxY - minY)) / 2D;
            minY -= extension;
            maxY += extension;
        }
        projectedBounds[0] = (float) minX;
        projectedBounds[1] = (float) minY;
        projectedBounds[2] = (float) maxX;
        projectedBounds[3] = (float) maxY;
        return true;
    }

    private void setBracketGeometry() {
        float cornerLength = Math.min(BRACKET_CORNER_LENGTH, Math.min((targetMaxX - targetMinX) / 3F, (targetMaxY - targetMinY) / 3F));
        float halfThickness = BRACKET_THICKNESS / 2F;
        bracketVertices.clear();

        addScreenQuad(bracketVertices, targetMinX, targetMinY - halfThickness, targetMinX + cornerLength, targetMinY + halfThickness);
        addScreenQuad(bracketVertices, targetMaxX - cornerLength, targetMinY - halfThickness, targetMaxX, targetMinY + halfThickness);
        addScreenQuad(bracketVertices, targetMinX, targetMaxY - halfThickness, targetMinX + cornerLength, targetMaxY + halfThickness);
        addScreenQuad(bracketVertices, targetMaxX - cornerLength, targetMaxY - halfThickness, targetMaxX, targetMaxY + halfThickness);

        addScreenQuad(bracketVertices, targetMinX - halfThickness, targetMinY, targetMinX + halfThickness, targetMinY + cornerLength);
        addScreenQuad(bracketVertices, targetMaxX - halfThickness, targetMinY, targetMaxX + halfThickness, targetMinY + cornerLength);
        addScreenQuad(bracketVertices, targetMinX - halfThickness, targetMaxY - cornerLength, targetMinX + halfThickness, targetMaxY);
        addScreenQuad(bracketVertices, targetMaxX - halfThickness, targetMaxY - cornerLength, targetMaxX + halfThickness, targetMaxY);
        bracketVertices.flip();
    }

    private static void addScreenQuad(FloatBuffer vertices, float minX, float minY, float maxX, float maxY) {
        //Screen Y grows down while GUI vertex Y grows up.
        addQuad(vertices, minX, -maxY, maxX, -minY);
    }

    private static void addQuad(FloatBuffer vertices, float minX, float minY, float maxX, float maxY) {
        addVertex(vertices, minX, minY);
        addVertex(vertices, maxX, minY);
        addVertex(vertices, maxX, maxY);
        addVertex(vertices, minX, minY);
        addVertex(vertices, maxX, maxY);
        addVertex(vertices, minX, maxY);
    }

    private static void applyVehicleInterpolation(Point3D point, AEntityB_Existing vehicle, float partialTicks) {
        if (vehicle != null) {
            double priorTickOffset = 1.0D - partialTicks;
            point.x -= (vehicle.position.x - vehicle.prevPosition.x) * priorTickOffset;
            point.y -= (vehicle.position.y - vehicle.prevPosition.y) * priorTickOffset;
            point.z -= (vehicle.position.z - vehicle.prevPosition.z) * priorTickOffset;
        }
    }

    @Override
    public void renderText(boolean renderTextLit, int worldLightValue) {
        if (targetOnScreen && !text.isEmpty()) {
            RenderText.drawText(text, null, textPosition, targetLocked ? ColorRGB.RED : ColorRGB.WHITE, TextAlignment.LEFT_ALIGNED, 0.75F, false, 0, true, worldLightValue);
        }
    }
}
