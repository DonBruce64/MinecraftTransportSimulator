package minecrafttransportsimulator.guis.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.BoundingBoxHitResult;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.EntityInteractResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.sound.SoundInstance;

/**
 * A compact compatible-part preview anchored to the slot being scanned.
 */
public class GUIComponentPartSlotScanner extends AGUIComponent {
    private static final int ICON_SIZE = 20;
    private static final int PADDING = 5;
    private static final int ICON_GAP = 6;
    private static final int MARKER_GAP = 10;
    private static final int SCREEN_MARGIN = 4;
    private static final int MAX_NAME_WIDTH = 160;
    private static final int MIN_NAME_WIDTH = 80;
    private static final float NAME_SCALE = 0.85F;
    private static final float ID_SCALE = 0.75F;
    private static final int NAME_ID_GAP = 2;
    private static final int BORDER_SEGMENTS = 8;
    private static final ColorRGB ID_COLOR = new ColorRGB(0x555555);
    private static final double PREVIEW_Z = 380.0D;
    private static final String SOLID_TEXTURE = "mts:textures/rendering/light.png";

    private final GUIComponentItem previewItem = new GUIComponentItem(0, 0, ICON_SIZE / 16.0F);
    private final RenderableVertices topBorderVertices = RenderableVertices.createSprite(1, null, null);
    private final RenderableVertices bottomBorderVertices = RenderableVertices.createSprite(1, null, null);
    private final RenderableVertices backgroundVertices = RenderableVertices.createSprite(3, null, null);
    private final RenderableData topBorderRenderable = createRenderable(topBorderVertices, new ColorRGB(0x5000FF), 80.0F / 255.0F);
    private final RenderableData bottomBorderRenderable = createRenderable(bottomBorderVertices, new ColorRGB(0x28007F), 80.0F / 255.0F);
    private final RenderableData backgroundRenderable = createRenderable(backgroundVertices, new ColorRGB(0x100010), 240.0F / 255.0F);
    private final List<RenderableData> sideBorderRenderables = new ArrayList<>();
    private final List<AItemPart> validParts = new ArrayList<>();
    private final Point3D markerPosition = new Point3D();
    private final Point3D namePosition = new Point3D();
    private AEntityF_Multipart<?> targetEntity;
    private BoundingBox targetBox;
    private JSONPartDefinition targetDefinition;
    private JSONSubDefinition targetSubDefinition;
    private AItemPart currentPart;
    private String currentPartID;
    private String targetLanguage;
    private float longestTextWidth;

    public GUIComponentPartSlotScanner() {
        super(0, 0, 0, 0);
        //Match vanilla's tooltip border gradient without depending on a version-specific GUI texture.
        for (int segment = 0; segment < BORDER_SEGMENTS; ++segment) {
            float progress = (segment + 0.5F) / BORDER_SEGMENTS;
            ColorRGB color = new ColorRGB(Math.round(80.0F - 40.0F * progress), 0, Math.round(255.0F - 128.0F * progress));
            sideBorderRenderables.add(createRenderable(RenderableVertices.createSprite(2, null, null), color, 80.0F / 255.0F));
        }
    }

    public void updateTarget(IWrapperPlayer player, EntityInteractResult interactResult, Point3D startPosition, Point3D endPosition) {
        if (player.isSpectator() || !player.isHoldingItemType(ItemComponentType.SCANNER)
                || interactResult == null || !(interactResult.entity instanceof AEntityF_Multipart)) {
            clearTarget();
            return;
        }

        AEntityF_Multipart<?> multipart = (AEntityF_Multipart<?>) interactResult.entity;
        if (!multipart.isValid || multipart instanceof EntityPlayerGun
                || multipart instanceof APart && ((APart) multipart).masterEntity instanceof EntityPlayerGun) {
            clearTarget();
            return;
        }

        BoundingBox closestBox = null;
        JSONPartDefinition closestDefinition = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entry<BoundingBox, JSONPartDefinition> slotEntry : multipart.activeClientPartSlotBoxes.entrySet()) {
            BoundingBox box = slotEntry.getKey();
            if (slotEntry.getValue().isPermanent || GUIComponentPartSlotMarkers.getDistanceSquaredToBox(startPosition, box) >= GUIComponentPartSlotMarkers.SCANNER_REACH_SQUARED) {
                continue;
            }
            BoundingBoxHitResult hit = box.getIntersection(startPosition, endPosition);
            if (hit != null) {
                double distance = hit.position.distanceTo(startPosition);
                if (distance < closestDistance) {
                    closestBox = box;
                    closestDefinition = slotEntry.getValue();
                    closestDistance = distance;
                }
            }
        }
        if (closestBox == null) {
            clearTarget();
            //An occupied slot still has a marker, even though it has no compatible-part preview.
            if (multipart instanceof APart && !((APart) multipart).placementDefinition.isPermanent
                    && interactResult.box == multipart.boundingBox
                    && GUIComponentPartSlotMarkers.getDistanceSquaredToBox(startPosition, multipart.boundingBox) < GUIComponentPartSlotMarkers.SCANNER_REACH_SQUARED) {
                targetEntity = multipart;
                targetBox = multipart.boundingBox;
            }
            return;
        }

        String language = InterfaceManager.clientInterface.getLanguageName();
        if (multipart != targetEntity || closestBox != targetBox || closestDefinition != targetDefinition
                || multipart.subDefinition != targetSubDefinition || !language.equals(targetLanguage)) {
            targetEntity = multipart;
            targetBox = closestBox;
            targetDefinition = closestDefinition;
            targetSubDefinition = multipart.subDefinition;
            targetLanguage = language;
            validParts.clear();
            longestTextWidth = 0.0F;
            for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
                if (packItem instanceof AItemPart) {
                    AItemPart part = (AItemPart) packItem;
                    if (part.isPartValidForPackDef(targetDefinition, multipart.subDefinition, true)) {
                        validParts.add(part);
                        longestTextWidth = Math.max(longestTextWidth, RenderText.getStringWidth(part.getItemName(), null) * NAME_SCALE);
                        longestTextWidth = Math.max(longestTextWidth, RenderText.getStringWidth(InterfaceManager.coreModID + ":" + part.getRegistrationName(), null) * ID_SCALE);
                    }
                }
            }
        }

        if (validParts.isEmpty()) {
            currentPart = null;
            currentPartID = null;
            previewItem.stack = null;
            return;
        }

        int cycle = player.isSneaking() ? 30 : 15;
        AItemPart selectedPart = validParts.get((int) ((multipart.ticksExisted / cycle) % validParts.size()));
        if (selectedPart != currentPart) {
            currentPart = selectedPart;
            previewItem.stack = selectedPart.getNewStack(null);
            currentPartID = InterfaceManager.coreInterface.getStackItemName(previewItem.stack);
            //Beep once when the displayed variant changes, rather than once per render frame.
            if (multipart.ticksExisted % cycle == 0) {
                InterfaceManager.soundInterface.playQuickSound(new SoundInstance(multipart, InterfaceManager.coreModID + ":scanner_beep"));
            }
        }
    }

    public boolean isTarget(AEntityF_Multipart<?> multipart, BoundingBox box) {
        return currentPart != null && isHoveredSlot(multipart, box);
    }

    public boolean isHoveredSlot(AEntityF_Multipart<?> multipart, BoundingBox box) {
        return targetEntity == multipart && targetBox == box;
    }

    private void clearTarget() {
        targetEntity = null;
        targetBox = null;
        targetDefinition = null;
        targetSubDefinition = null;
        targetLanguage = null;
        currentPart = null;
        currentPartID = null;
        previewItem.stack = null;
        validParts.clear();
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (!blendingEnabled || currentPart == null || targetEntity == null || !targetEntity.isValid
                || gui.screenWidth <= 2 * SCREEN_MARGIN + 2 * PADDING + ICON_SIZE + ICON_GAP
                || gui.screenHeight < 2 * SCREEN_MARGIN + 2 * PADDING + ICON_SIZE) {
            return;
        }

        markerPosition.set(targetBox.globalCenter);
        if (targetEntity.requiresDeltaUpdates()) {
            double partialTickOffset = 1.0D - partialTicks;
            markerPosition.x -= (targetEntity.position.x - targetEntity.prevPosition.x) * partialTickOffset;
            markerPosition.y -= (targetEntity.position.y - targetEntity.prevPosition.y) * partialTickOffset;
            markerPosition.z -= (targetEntity.position.z - targetEntity.prevPosition.z) * partialTickOffset;
        }
        Point3D projectedPosition = InterfaceManager.clientInterface.projectToScreen(markerPosition, gui.screenWidth, gui.screenHeight);
        if (projectedPosition == null) {
            return;
        }
        double screenX = projectedPosition.x;
        double screenY = projectedPosition.y;

        //Reserve the same width for every compatible variant so cycling cannot move the preview.
        double rightSpace = gui.screenWidth - screenX - MARKER_GAP - SCREEN_MARGIN;
        double leftSpace = screenX - MARKER_GAP - SCREEN_MARGIN;
        boolean renderOnRight = rightSpace >= 2 * PADDING + ICON_SIZE + ICON_GAP + MIN_NAME_WIDTH;
        boolean renderBeside = Math.max(rightSpace, leftSpace) >= 2 * PADDING + ICON_SIZE + ICON_GAP + MIN_NAME_WIDTH;
        int availableNameWidth = (renderBeside ? (int) (renderOnRight ? rightSpace : leftSpace) : gui.screenWidth - 2 * SCREEN_MARGIN) - 2 * PADDING - ICON_SIZE - ICON_GAP;
        int nameWidth = (int) Math.min(Math.min(MAX_NAME_WIDTH, availableNameWidth), Math.max(40.0F, Math.ceil(longestTextWidth)));
        int previewWidth = 2 * PADDING + ICON_SIZE + ICON_GAP + nameWidth;
        String[] nameLines = splitName(currentPart.getItemName(), nameWidth);
        double textHeight = RenderText.getHeight(nameLines.length) * NAME_SCALE + NAME_ID_GAP + RenderText.getHeight(1) * ID_SCALE;
        int previewHeight = 2 * PADDING + (int) Math.ceil(Math.max(ICON_SIZE, textHeight));
        if (gui.screenHeight < 2 * SCREEN_MARGIN + previewHeight) {
            return;
        }
        double verticalSpace = Math.max(gui.screenHeight - screenY - SCREEN_MARGIN, screenY - SCREEN_MARGIN);
        int verticalGap = (int) Math.max(9.0D, Math.min(MARKER_GAP, verticalSpace - previewHeight));
        boolean renderBelow = screenY + verticalGap + previewHeight + SCREEN_MARGIN <= gui.screenHeight;
        if (!renderBeside && !renderBelow && screenY - verticalGap - previewHeight < SCREEN_MARGIN) {
            return;
        }
        double previewX = renderBeside ? (renderOnRight ? screenX + MARKER_GAP : screenX - MARKER_GAP - previewWidth) : screenX - previewWidth / 2.0D;
        previewX = Math.max(SCREEN_MARGIN, Math.min(gui.screenWidth - SCREEN_MARGIN - previewWidth, previewX));
        double previewY = renderBeside ? screenY - previewHeight / 2.0D : (renderBelow ? screenY + verticalGap : screenY - verticalGap - previewHeight);
        previewY = Math.max(SCREEN_MARGIN, Math.min(gui.screenHeight - SCREEN_MARGIN - previewHeight, previewY));

        renderTooltipBackground(previewX, previewY, previewWidth, previewHeight);

        previewItem.position.set(previewX + PADDING, -(previewY + (previewHeight - ICON_SIZE) / 2.0D), PREVIEW_Z + 100.0D);
        previewItem.render(gui, mouseX, mouseY, true, false, blendingEnabled, partialTicks);
        double nameY = previewY + (previewHeight - textHeight) / 2.0D;
        for (String nameLine : nameLines) {
            namePosition.set(previewX + PADDING + ICON_SIZE + ICON_GAP, -nameY, PREVIEW_Z + 2.0D);
            RenderText.drawText(nameLine, null, namePosition, ColorRGB.WHITE, TextAlignment.LEFT_ALIGNED, NAME_SCALE, true, nameWidth, true, gui.worldLightValue);
            nameY += RenderText.getHeight(1) * NAME_SCALE;
        }
        namePosition.set(previewX + PADDING + ICON_SIZE + ICON_GAP, -(nameY + NAME_ID_GAP), PREVIEW_Z + 2.0D);
        RenderText.drawText(currentPartID, null, namePosition, ID_COLOR, TextAlignment.LEFT_ALIGNED, ID_SCALE, true, nameWidth, true, gui.worldLightValue);
    }

    private static String[] splitName(String name, int width) {
        if (RenderText.getStringWidth(name, null) * NAME_SCALE <= width) {
            return new String[]{name};
        }
        int splitIndex = -1;
        for (int spaceIndex = name.indexOf(' '); spaceIndex >= 0; spaceIndex = name.indexOf(' ', spaceIndex + 1)) {
            if (splitIndex >= 0 && RenderText.getStringWidth(name.substring(0, spaceIndex), null) * NAME_SCALE > width) {
                break;
            }
            splitIndex = spaceIndex;
        }
        return splitIndex > 0 ? new String[]{name.substring(0, splitIndex), name.substring(splitIndex + 1)} : new String[]{name};
    }

    private void renderTooltipBackground(double x, double y, int width, int height) {
        backgroundVertices.setSpriteProperties(0, 1, 0, width - 2, 1, 0, 0, 1, 1);
        backgroundVertices.setSpriteProperties(1, 0, -1, width, height - 2, 0, 0, 1, 1);
        backgroundVertices.setSpriteProperties(2, 1, -(height - 1), width - 2, 1, 0, 0, 1, 1);
        renderTooltipSection(backgroundRenderable, x, y, PREVIEW_Z);

        topBorderVertices.setSpriteProperties(0, 1, -1, width - 2, 1, 0, 0, 1, 1);
        bottomBorderVertices.setSpriteProperties(0, 1, -(height - 2), width - 2, 1, 0, 0, 1, 1);
        renderTooltipSection(topBorderRenderable, x, y, PREVIEW_Z + 1.0D);
        renderTooltipSection(bottomBorderRenderable, x, y, PREVIEW_Z + 1.0D);
        for (int segment = 0; segment < BORDER_SEGMENTS; ++segment) {
            RenderableData sideBorder = sideBorderRenderables.get(segment);
            int segmentTop = 2 + (height - 4) * segment / BORDER_SEGMENTS;
            int segmentBottom = 2 + (height - 4) * (segment + 1) / BORDER_SEGMENTS;
            sideBorder.vertexObject.setSpriteProperties(0, 1, -segmentTop, 1, segmentBottom - segmentTop, 0, 0, 1, 1);
            sideBorder.vertexObject.setSpriteProperties(1, width - 2, -segmentTop, 1, segmentBottom - segmentTop, 0, 0, 1, 1);
            renderTooltipSection(sideBorder, x, y, PREVIEW_Z + 1.0D);
        }
    }

    private static void renderTooltipSection(RenderableData renderable, double x, double y, double z) {
        renderable.transform.resetTransforms();
        renderable.transform.setTranslation(x, -y, z);
        renderable.render();
    }

    private static RenderableData createRenderable(RenderableVertices vertices, ColorRGB color, float alpha) {
        RenderableData renderable = new RenderableData(vertices);
        if (!vertices.isLines) {
            renderable.setTexture(SOLID_TEXTURE);
        }
        renderable.setColor(color);
        renderable.setAlpha(alpha);
        renderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        renderable.setTransucentOverride();
        renderable.setDepthWriting(true);
        return renderable;
    }
}
