package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.EntityInteractResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.ControlSystem;

/**Progress for held part and vehicle actions, independent of the part-slot marker UI.*/
public class GUIComponentTimedActionProgress extends AGUIComponent {
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_QUAD = 6;
    private static final int PROGRESS_SEGMENTS = 32;
    private static final double PROGRESS_INNER_RADIUS = 5.0D;
    private static final double PROGRESS_OUTER_RADIUS = 7.0D;
    private static final double PROGRESS_BASE_Z = 350.0D;
    private static final double INTERACTION_REACH = 3.5D;

    private final RenderableVertices progressVertices = createProgressRingVertices("TIMED_ACTION_PROGRESS", 0.0F);
    private final RenderableData trackRenderable = createProgressRenderable(createProgressRingVertices("TIMED_ACTION_TRACK", 1.0F), ColorRGB.DARK_GRAY, 0.75F);
    private final RenderableData progressRenderable = createProgressRenderable(progressVertices, ColorRGB.GREEN, 1.0F);
    private final Point3D markerCenter = new Point3D();

    public GUIComponentTimedActionProgress() {
        super(0, 0, 0, 0);
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (!blendingEnabled) {
            return;
        }
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (player == null || player.getWorld() == null || player.isSpectator()) {
            return;
        }

        float progress;
        boolean removing;
        if (ControlSystem.getVehiclePackingMarkerPosition(markerCenter)) {
            progress = ControlSystem.getVehiclePackingProgress(partialTicks);
            removing = true;
        } else if (ControlSystem.getVehicleDeploymentMarkerPosition(markerCenter)) {
            progress = ControlSystem.getVehicleDeploymentProgress(partialTicks);
            removing = false;
        } else if (ControlSystem.isPartInstallationInProgress() || ControlSystem.isPartRemovalInProgress()) {
            Point3D start = player.getEyePosition();
            EntityInteractResult hit = player.getWorld().getMultipartEntityIntersect(start, player.getLineOfSight(INTERACTION_REACH).add(start));
            if (hit == null || !(hit.entity instanceof AEntityF_Multipart)) {
                return;
            }
            AEntityF_Multipart<?> multipart = (AEntityF_Multipart<?>) hit.entity;
            removing = ControlSystem.isPartRemovalTarget(multipart);
            if (!removing && !ControlSystem.isPartInstallationTarget(multipart, hit.box)) {
                return;
            }
            progress = removing ? ControlSystem.getPartRemovalProgress(partialTicks) : ControlSystem.getPartInstallationProgress(partialTicks);
            markerCenter.set(hit.box.globalCenter);
            if (hit.entity.requiresDeltaUpdates()) {
                double partialTickOffset = 1.0D - partialTicks;
                markerCenter.x -= (hit.entity.position.x - hit.entity.prevPosition.x) * partialTickOffset;
                markerCenter.y -= (hit.entity.position.y - hit.entity.prevPosition.y) * partialTickOffset;
                markerCenter.z -= (hit.entity.position.z - hit.entity.prevPosition.z) * partialTickOffset;
            }
        } else {
            return;
        }

        Point3D projected = InterfaceManager.clientInterface.projectToScreen(markerCenter, gui.screenWidth, gui.screenHeight);
        if (projected != null) {
            double screenX = projected.x;
            double screenY = projected.y;
            double z = PROGRESS_BASE_Z - projected.z;
            renderRing(trackRenderable, screenX, screenY, z);
            if (progress > 0.0F) {
                setProgressRingVertices(progressVertices.vertices, progress);
                progressRenderable.setColor(removing ? ColorRGB.RED : ColorRGB.GREEN);
                renderRing(progressRenderable, screenX, screenY, z);
            }
        }
    }

    private static RenderableData createProgressRenderable(RenderableVertices vertices, ColorRGB color, float alpha) {
        RenderableData renderable = new RenderableData(vertices, "mts:textures/rendering/light.png");
        renderable.setColor(color);
        renderable.setAlpha(alpha);
        renderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        renderable.setTransucentOverride();
        return renderable;
    }

    private static void renderRing(RenderableData renderable, double x, double y, double z) {
        renderable.transform.resetTransforms();
        renderable.transform.setTranslation(x, -y, z);
        renderable.render();
    }

    private static RenderableVertices createProgressRingVertices(String name, float progress) {
        RenderableVertices vertices = new RenderableVertices(name, FloatBuffer.allocate(PROGRESS_SEGMENTS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX), false);
        setProgressRingVertices(vertices.vertices, progress);
        return vertices;
    }

    private static void setProgressRingVertices(FloatBuffer vertices, float progress) {
        vertices.clear();
        double completedSegments = Math.max(0.0D, Math.min(1.0D, progress)) * PROGRESS_SEGMENTS;
        int fullSegments = (int) completedSegments;
        for (int segmentIndex = 0; segmentIndex < fullSegments; ++segmentIndex) {
            addProgressRingSegment(vertices, segmentIndex, segmentIndex + 1.0D);
        }

        double partialSegment = completedSegments - fullSegments;
        if (partialSegment > 0.0D && fullSegments < PROGRESS_SEGMENTS) {
            addProgressRingSegment(vertices, fullSegments, fullSegments + partialSegment);
        }
        vertices.flip();
    }

    private static void addProgressRingSegment(FloatBuffer vertices, double segmentStart, double segmentEnd) {
        double startAngle = Math.PI / 2.0D - 2.0D * Math.PI * segmentStart / PROGRESS_SEGMENTS;
        double endAngle = Math.PI / 2.0D - 2.0D * Math.PI * segmentEnd / PROGRESS_SEGMENTS;
        float outerStartX = (float) (PROGRESS_OUTER_RADIUS * Math.cos(startAngle));
        float outerStartY = (float) (PROGRESS_OUTER_RADIUS * Math.sin(startAngle));
        float innerStartX = (float) (PROGRESS_INNER_RADIUS * Math.cos(startAngle));
        float innerStartY = (float) (PROGRESS_INNER_RADIUS * Math.sin(startAngle));
        float outerEndX = (float) (PROGRESS_OUTER_RADIUS * Math.cos(endAngle));
        float outerEndY = (float) (PROGRESS_OUTER_RADIUS * Math.sin(endAngle));
        float innerEndX = (float) (PROGRESS_INNER_RADIUS * Math.cos(endAngle));
        float innerEndY = (float) (PROGRESS_INNER_RADIUS * Math.sin(endAngle));

        addVertex(vertices, outerStartX, outerStartY);
        addVertex(vertices, innerStartX, innerStartY);
        addVertex(vertices, innerEndX, innerEndY);
        addVertex(vertices, outerStartX, outerStartY);
        addVertex(vertices, innerEndX, innerEndY);
        addVertex(vertices, outerEndX, outerEndY);
    }

    private static void addVertex(FloatBuffer vertices, float x, float y) {
        vertices.put(0.0F);
        vertices.put(0.0F);
        vertices.put(1.0F);
        vertices.put(0.0F);
        vertices.put(0.0F);
        vertices.put(x);
        vertices.put(y);
        vertices.put(0.0F);
    }
}
