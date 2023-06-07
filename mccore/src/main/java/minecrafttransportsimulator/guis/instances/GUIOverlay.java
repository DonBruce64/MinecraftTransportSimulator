package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.EntityManager.EntityInteractResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.CameraSystem;

/**
 * A GUI that is used to render overlay components.  These components are independent of
 * any vehicle or entity the player is riding, and are always visible.
 *
 * @author don_bruce
 */
public class GUIOverlay extends AGUIBase {
    private GUIComponentLabel mouseoverLabel;
    private GUIComponentItem scannerItem;
    private final List<String> tooltipText = new ArrayList<>();

    @Override
    public void setupComponents() {
        super.setupComponents();

        addComponent(mouseoverLabel = new GUIComponentLabel(screenWidth / 2, screenHeight / 2 + 10, ColorRGB.WHITE, "", TextAlignment.CENTERED, 1.0F));
        addComponent(scannerItem = new GUIComponentItem(0, screenHeight / 4, 6.0F) {
            //Render the item stats as a tooltip, as it's easier to see.
            @Override
            public boolean isMouseInBounds(int mouseX, int mouseY) {
                return true;
            }

            @Override
            public void renderTooltip(AGUIBase gui, int mouseX, int mouseY) {
                super.renderTooltip(gui, scannerItem.constructedX, scannerItem.constructedY + 24 * 6);
            }

            @Override
            public List<String> getTooltipText() {
                return tooltipText;
            }
        });
    }

    @Override
    public void setStates() {
        super.setStates();
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        Point3D startPosition = player.getEyePosition();
        Point3D endPosition = player.getLineOfSight(10).add(startPosition);
        EntityInteractResult interactResult = player.getWorld().getMultipartEntityIntersect(startPosition, endPosition);

        mouseoverLabel.text = "";
        if (interactResult != null && interactResult.entity instanceof PartInteractable) {
            PartInteractable interactable = (PartInteractable) interactResult.entity;
            if (interactable.tank != null) {
                String fluidName = interactable.tank.getFluid();
                if(fluidName.isEmpty()) {
                    mouseoverLabel.text = String.format("%.1f/%.1fb", interactable.tank.getFluidLevel() / 1000F, interactable.tank.getMaxLevel() / 1000F);
                }else {
                    mouseoverLabel.text = String.format("%s: %.1f/%.1fb", InterfaceManager.clientInterface.getFluidName(fluidName), interactable.tank.getFluidLevel() / 1000F, interactable.tank.getMaxLevel() / 1000F);
                }
            }
        }

        scannerItem.stack = null;
        tooltipText.clear();
        if (player.isHoldingItemType(ItemComponentType.SCANNER)) {
            if (interactResult != null && interactResult.entity instanceof AEntityF_Multipart) {
                AEntityF_Multipart<?> multipart = (AEntityF_Multipart<?>) interactResult.entity;
                BoundingBox mousedOverBox = null;
                JSONPartDefinition packVehicleDef = null;
                for (Entry<BoundingBox, JSONPartDefinition> boxEntry : multipart.activePartSlotBoxes.entrySet()) {
                    BoundingBox box = boxEntry.getKey();
                    if (box.getIntersectionPoint(startPosition, endPosition) != null) {
                        if (mousedOverBox == null || (box.globalCenter.distanceTo(startPosition) < mousedOverBox.globalCenter.distanceTo(startPosition))) {
                            mousedOverBox = box;
                            packVehicleDef = boxEntry.getValue();
                        }
                    }
                }

                if (mousedOverBox != null) {
                    //Populate stacks.
                    List<AItemPart> validParts = new ArrayList<>();
                    for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
                        if (packItem instanceof AItemPart) {
                            AItemPart part = (AItemPart) packItem;
                            if (part.isPartValidForPackDef(packVehicleDef, multipart.subDefinition, true)) {
                                validParts.add(part);
                            }
                        }
                    }

                    //Get the slot info.
                    tooltipText.add("Types: " + packVehicleDef.types.toString());
                    tooltipText.add("Min/Max: " + packVehicleDef.minValue + "/" + packVehicleDef.maxValue);
                    if (packVehicleDef.customTypes != null) {
                        tooltipText.add("CustomTypes: " + packVehicleDef.customTypes);
                    } else {
                        tooltipText.add("CustomTypes: None");
                    }

                    //Get the stack to render..
                    if (!validParts.isEmpty()) {
                        //Get current part to render based on the cycle.
                        int cycle = player.isSneaking() ? 30 : 15;
                        AItemPart partToRender = validParts.get((int) ((multipart.ticksExisted / cycle) % validParts.size()));
                        tooltipText.add(partToRender.getItemName());
                        scannerItem.stack = partToRender.getNewStack(null);

                        //If we are on the start of the cycle, beep.
                        if (multipart.ticksExisted % cycle == 0) {
                            InterfaceManager.soundInterface.playQuickSound(new SoundInstance(multipart, InterfaceManager.coreModID + ":scanner_beep"));
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean renderBackground() {
        return CameraSystem.customCameraOverlay != null;
    }

    @Override
    protected boolean renderBackgroundFullTexture() {
        return true;
    }

    @Override
    public boolean capturesPlayer() {
        return false;
    }

    @Override
    public int getWidth() {
        return screenWidth;
    }

    @Override
    public int getHeight() {
        return screenHeight;
    }

    @Override
    public boolean renderFlushBottom() {
        return true;
    }

    @Override
    public boolean renderTranslucent() {
        return true;
    }

    @Override
    protected String getTexture() {
        return CameraSystem.customCameraOverlay;
    }
}
