package minecrafttransportsimulator.guis.components;

import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.LanguageSystem;

/**
 * Screen-space markers for part slots.  Markers use a fixed small screen-space size, while
 * interaction reach continues to follow the slot's actual world-space hitbox.
 */
public class GUIComponentPartSlotMarkers extends AGUIComponent {
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_QUAD = 6;
    private static final double INTERACTION_REACH = 3.5D;
    private static final double INTERACTION_REACH_SQUARED = INTERACTION_REACH * INTERACTION_REACH;
    private static final double MARKER_RADIUS = 6.0D;
    private static final String MARKER_TEXTURE = "mts:textures/guis/part_marker.png";
    private static final String UNAVAILABLE_MARKER_TEXTURE = "mts:textures/guis/part_marker_unavailable.png";
    private static final String REMOVE_MARKER_TEXTURE = "mts:textures/guis/part_marker_remove.png";
    private static final double MARKER_BASE_Z = 350.0D;
    private static final double LABEL_GAP = 3.0D;

    private static final EnumMap<PartTypeCategory, String> TYPE_MARKER_TEXTURES = new EnumMap<>(PartTypeCategory.class);

    static {
        TYPE_MARKER_TEXTURES.put(PartTypeCategory.ENGINE, "mts:textures/guis/part_marker_engine.png");
        TYPE_MARKER_TEXTURES.put(PartTypeCategory.GROUND, "mts:textures/guis/part_marker_wheel.png");
        TYPE_MARKER_TEXTURES.put(PartTypeCategory.PROPELLER, "mts:textures/guis/part_marker_propeller.png");
        TYPE_MARKER_TEXTURES.put(PartTypeCategory.SEAT, "mts:textures/guis/part_marker_seat.png");
        TYPE_MARKER_TEXTURES.put(PartTypeCategory.GUN, "mts:textures/guis/part_marker_gun.png");
        TYPE_MARKER_TEXTURES.put(PartTypeCategory.INTERACTABLE, "mts:textures/guis/part_marker_interactable.png");
        TYPE_MARKER_TEXTURES.put(PartTypeCategory.EFFECTOR, "mts:textures/guis/part_marker_effector.png");
        TYPE_MARKER_TEXTURES.put(PartTypeCategory.UNKNOWN, "mts:textures/guis/part_marker_unknown.png");
    }

    private final RenderableData defaultMarkerRenderable;
    private final RenderableData unavailableMarkerRenderable;
    private final RenderableData removeMarkerRenderable;
    private final RenderableData lockedIconRenderable;
    private final EnumMap<PartTypeCategory, RenderableData> typeMarkerRenderables = new EnumMap<>(PartTypeCategory.class);
    private final Point3D markerCenter = new Point3D();
    private final Point3D labelPosition = new Point3D();

    public GUIComponentPartSlotMarkers() {
        super(0, 0, 0, 0);

        defaultMarkerRenderable = createTexturedMarkerRenderable(MARKER_TEXTURE);
        unavailableMarkerRenderable = createTexturedMarkerRenderable(UNAVAILABLE_MARKER_TEXTURE);
        removeMarkerRenderable = createTexturedMarkerRenderable(REMOVE_MARKER_TEXTURE);
        lockedIconRenderable = createUnlitRenderable(createLockedIconVertices(), ColorRGB.BLACK);
        for (Entry<PartTypeCategory, String> typeMarkerTexture : TYPE_MARKER_TEXTURES.entrySet()) {
            typeMarkerRenderables.put(typeMarkerTexture.getKey(), createTexturedMarkerRenderable(typeMarkerTexture.getValue()));
        }
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (!blendingEnabled) {
            return;
        }

        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (player == null || player.getWorld() == null) {
            return;
        }

        AItemBase heldItem = player.getHeldItem();
        AItemPart heldPart = heldItem instanceof AItemPart ? (AItemPart) heldItem : null;
        boolean holdingScanner = player.isHoldingItemType(ItemComponentType.SCANNER);
        boolean holdingWrench = player.isHoldingItemType(ItemComponentType.WRENCH);
        if (heldPart == null && !holdingScanner && !holdingWrench) {
            return;
        }

        Point3D eyePosition = player.getEyePosition();
        for (AEntityF_Multipart<?> multipart : player.getWorld().getEntitiesExtendingType(AEntityF_Multipart.class)) {
            renderMultipartMarkers(gui, multipart, eyePosition, heldPart, holdingScanner, holdingWrench, player, partialTicks);
        }
    }

    private void renderMultipartMarkers(AGUIBase gui, AEntityF_Multipart<?> multipart, Point3D eyePosition, AItemPart heldPart, boolean holdingScanner, boolean holdingWrench, IWrapperPlayer player, float partialTicks) {
        if (!multipart.isValid || multipart.definition.parts == null) {
            return;
        }

        //Empty slots retain their placement hitboxes in this map even when they are unavailable.
        if (!holdingWrench) {
            for (Entry<BoundingBox, JSONPartDefinition> slotEntry : multipart.partSlotBoxes.entrySet()) {
                JSONPartDefinition slotDefinition = slotEntry.getValue();
                if (!holdingScanner && !heldPart.isPartValidForPackDef(slotDefinition, multipart.subDefinition, false)) {
                    continue;
                }

                PartSlotMarkerState state;
                if (isLocked(multipart)) {
                    state = PartSlotMarkerState.LOCKED;
                } else if (!multipart.canBeClicked()
                        || !multipart.isVariableListTrue(slotDefinition.interactableVariables)
                        || (!holdingScanner && !heldPart.isPartValidForPackDef(slotDefinition, multipart.subDefinition, true))) {
                    state = PartSlotMarkerState.UNAVAILABLE;
                } else {
                    state = PartSlotMarkerState.AVAILABLE;
                }

                String rawType = heldPart != null ? heldPart.definition.generic.type : getFirstSlotType(slotDefinition);
                renderMarker(gui, multipart, slotEntry.getKey(), eyePosition, rawType, state, partialTicks);
            }
        }

        //Occupied slots do not have entries in partSlotBoxes, so source them from partsInSlots.
        int slotCount = Math.min(multipart.definition.parts.size(), multipart.partsInSlots.size());
        for (int slotIndex = 0; slotIndex < slotCount; ++slotIndex) {
            APart installedPart = multipart.partsInSlots.get(slotIndex);
            if (installedPart == null || !installedPart.isValid) {
                continue;
            }

            JSONPartDefinition slotDefinition = multipart.definition.parts.get(slotIndex);
            PartSlotMarkerState state;
            if (holdingWrench) {
                if (!canRemoveWithWrench(installedPart, player)) {
                    continue;
                }
                state = PartSlotMarkerState.REMOVE;
            } else {
                if (!holdingScanner && !heldPart.isPartValidForPackDef(slotDefinition, multipart.subDefinition, false)) {
                    continue;
                }
                state = PartSlotMarkerState.INSTALLED;
            }

            renderMarker(gui, installedPart, installedPart.boundingBox, eyePosition, installedPart.definition.generic.type, state, partialTicks);
        }
    }

    private void renderMarker(AGUIBase gui, AEntityF_Multipart<?> markerEntity, BoundingBox markerBox, Point3D eyePosition, String rawType, PartSlotMarkerState state, float partialTicks) {
        if (!isWithinReach(eyePosition, markerBox)) {
            return;
        }

        markerCenter.set(markerBox.globalCenter);
        if (markerEntity.requiresDeltaUpdates()) {
            double partialTickOffset = 1.0D - partialTicks;
            markerCenter.x -= (markerEntity.position.x - markerEntity.prevPosition.x) * partialTickOffset;
            markerCenter.y -= (markerEntity.position.y - markerEntity.prevPosition.y) * partialTickOffset;
            markerCenter.z -= (markerEntity.position.z - markerEntity.prevPosition.z) * partialTickOffset;
        }

        Point3D projectedCenter = InterfaceManager.clientInterface.projectToScreen(markerCenter, gui.screenWidth, gui.screenHeight);
        if (projectedCenter == null) {
            return;
        }

        //projectToScreen returns a shared mutable point, so retain its values locally.
        double screenX = projectedCenter.x;
        double screenY = projectedCenter.y;
        double screenDepth = projectedCenter.z;
        double markerRadius = MARKER_RADIUS;
        double markerZ = MARKER_BASE_Z - screenDepth;
        PartTypeCategory typeCategory = getPartTypeCategory(rawType);

        renderMarkerBackground(screenX, screenY, markerRadius, markerZ, state, typeCategory);
        if (state == PartSlotMarkerState.LOCKED) {
            renderStatusIcon(screenX, screenY, markerRadius, markerZ, state);
        }
        renderTypeLabel(gui, screenX, screenY, markerRadius, markerZ, typeCategory, rawType);
    }

    private void renderMarkerBackground(double screenX, double screenY, double radius, double z, PartSlotMarkerState state, PartTypeCategory typeCategory) {
        RenderableData markerRenderable;
        switch (state) {
            case AVAILABLE:
                markerRenderable = typeMarkerRenderables.get(typeCategory);
                if (markerRenderable == null) {
                    markerRenderable = defaultMarkerRenderable;
                }
                break;
            case UNAVAILABLE:
            case INSTALLED:
                markerRenderable = unavailableMarkerRenderable;
                break;
            case REMOVE:
                markerRenderable = removeMarkerRenderable;
                break;
            default:
                markerRenderable = defaultMarkerRenderable;
                break;
        }
        markerRenderable.transform.resetTransforms();
        markerRenderable.transform.setTranslation(screenX, -screenY, z);
        markerRenderable.transform.applyScaling(radius * 2.0D, radius * 2.0D, 1.0D);
        markerRenderable.render();
    }

    private void renderStatusIcon(double screenX, double screenY, double radius, double z, PartSlotMarkerState state) {
        RenderableData iconRenderable;
        switch (state) {
            case LOCKED:
                iconRenderable = lockedIconRenderable;
                break;
            default:
                return;
        }

        double iconScale = radius * 0.72D;
        iconRenderable.transform.resetTransforms();
        iconRenderable.transform.setTranslation(screenX, -screenY, z);
        iconRenderable.transform.applyScaling(iconScale, iconScale, 1.0D);
        iconRenderable.render();
    }

    private void renderTypeLabel(AGUIBase gui, double screenX, double screenY, double radius, double z, PartTypeCategory typeCategory, String rawType) {
        int wrapWidth = (int) Math.max(48.0D, radius * 4.0D);
        labelPosition.set(screenX, -(screenY + radius + LABEL_GAP), z);
        RenderText.drawText(getLocalizedTypeName(typeCategory, rawType), null, labelPosition, ColorRGB.WHITE, TextAlignment.CENTERED, 0.75F, true, wrapWidth, true, gui.worldLightValue);
    }

    private static boolean isWithinReach(Point3D eyePosition, BoundingBox box) {
        double xDistance = Math.max(Math.abs(eyePosition.x - box.globalCenter.x) - box.widthRadius, 0.0D);
        double yDistance = Math.max(Math.abs(eyePosition.y - box.globalCenter.y) - box.heightRadius, 0.0D);
        double zDistance = Math.max(Math.abs(eyePosition.z - box.globalCenter.z) - box.depthRadius, 0.0D);
        return xDistance * xDistance + yDistance * yDistance + zDistance * zDistance <= INTERACTION_REACH_SQUARED;
    }

    private static boolean isLocked(AEntityF_Multipart<?> multipart) {
        EntityVehicleF_Physics vehicle = multipart instanceof EntityVehicleF_Physics
                ? (EntityVehicleF_Physics) multipart
                : multipart instanceof APart ? ((APart) multipart).vehicleOn : null;
        return vehicle != null && vehicle.lockedVar.isActive;
    }

    private static boolean canRemoveWithWrench(APart part, IWrapperPlayer player) {
        return !player.isSneaking()
                && part.isValid
                && !part.isFake()
                && !part.isPermanent
                && part.canBeClicked()
                && !isLocked(part)
                && part.checkForRemoval(player) == null;
    }

    private static String getFirstSlotType(JSONPartDefinition slotDefinition) {
        return slotDefinition.types != null && !slotDefinition.types.isEmpty() ? slotDefinition.types.get(0) : null;
    }

    private static PartTypeCategory getPartTypeCategory(String rawType) {
        if (rawType == null || rawType.isEmpty()) {
            return PartTypeCategory.UNKNOWN;
        }
        String lowerType = rawType.toLowerCase(Locale.ROOT);
        int separatorIndex = lowerType.indexOf('_');
        String prefix = separatorIndex >= 0 ? lowerType.substring(0, separatorIndex) : lowerType;
        try {
            return PartTypeCategory.valueOf(prefix.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return PartTypeCategory.UNKNOWN;
        }
    }

    private static String getLocalizedTypeName(PartTypeCategory typeCategory, String rawType) {
        switch (typeCategory) {
            case GENERIC:
                return LanguageSystem.GUI_PART_SLOT_GENERIC.getCurrentValue();
            case ENGINE:
                return LanguageSystem.GUI_PART_SLOT_ENGINE.getCurrentValue();
            case GROUND:
                return LanguageSystem.GUI_PART_SLOT_GROUND.getCurrentValue();
            case PROPELLER:
                return LanguageSystem.GUI_PART_SLOT_PROPELLER.getCurrentValue();
            case SEAT:
                return LanguageSystem.GUI_PART_SLOT_SEAT.getCurrentValue();
            case GUN:
                return LanguageSystem.GUI_PART_SLOT_GUN.getCurrentValue();
            case INTERACTABLE:
                return LanguageSystem.GUI_PART_SLOT_INTERACTABLE.getCurrentValue();
            case EFFECTOR:
                return LanguageSystem.GUI_PART_SLOT_EFFECTOR.getCurrentValue();
            default:
                return rawType == null || rawType.isEmpty() ? "?" : rawType.replace('_', ' ');
        }
    }

    private static RenderableData createTexturedMarkerRenderable(String texture) {
        RenderableData renderable = createUnlitRenderable(RenderableVertices.createSprite(1, null, null), ColorRGB.WHITE);
        renderable.setTexture(texture);
        return renderable;
    }

    private static RenderableData createUnlitRenderable(RenderableVertices vertices, ColorRGB color) {
        RenderableData renderable = new RenderableData(vertices);
        renderable.setColor(color);
        renderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        renderable.setTransucentOverride();
        renderable.setDepthWriting(true);
        return renderable;
    }

    private static RenderableVertices createLockedIconVertices() {
        RenderableVertices vertices = new RenderableVertices("PART_SLOT_LOCKED", FloatBuffer.allocate(4 * VERTICES_PER_QUAD * FLOATS_PER_VERTEX), false);
        addQuad(vertices.vertices, -0.40F, -0.45F, 0.40F, 0.12F);
        addLineQuad(vertices.vertices, -0.28F, 0.08F, -0.28F, 0.38F, 0.11F);
        addLineQuad(vertices.vertices, -0.28F, 0.38F, 0.28F, 0.38F, 0.11F);
        addLineQuad(vertices.vertices, 0.28F, 0.38F, 0.28F, 0.08F, 0.11F);
        vertices.vertices.flip();
        return vertices;
    }

    private static void addLineQuad(FloatBuffer vertices, float startX, float startY, float endX, float endY, float thickness) {
        float xDelta = endX - startX;
        float yDelta = endY - startY;
        float length = (float) Math.sqrt(xDelta * xDelta + yDelta * yDelta);
        float xOffset = -yDelta / length * thickness / 2.0F;
        float yOffset = xDelta / length * thickness / 2.0F;

        addVertex(vertices, startX - xOffset, startY - yOffset);
        addVertex(vertices, endX - xOffset, endY - yOffset);
        addVertex(vertices, endX + xOffset, endY + yOffset);
        addVertex(vertices, startX - xOffset, startY - yOffset);
        addVertex(vertices, endX + xOffset, endY + yOffset);
        addVertex(vertices, startX + xOffset, startY + yOffset);
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
        vertices.put(0.0F);
        vertices.put(0.0F);
        vertices.put(1.0F);
        vertices.put(0.0F);
        vertices.put(0.0F);
        vertices.put(x);
        vertices.put(y);
        vertices.put(0.0F);
    }

    private enum PartSlotMarkerState {
        AVAILABLE,
        INSTALLED,
        LOCKED,
        UNAVAILABLE,
        REMOVE
    }

    private enum PartTypeCategory {
        GENERIC,
        ENGINE,
        GROUND,
        PROPELLER,
        SEAT,
        GUN,
        INTERACTABLE,
        EFFECTOR,
        UNKNOWN
    }
}
