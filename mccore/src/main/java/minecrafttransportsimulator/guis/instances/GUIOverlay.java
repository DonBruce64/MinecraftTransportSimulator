package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFluidLoader;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
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

        addComponent(mouseoverLabel = new GUIComponentLabel(screenWidth / 2, screenHeight / 2, ColorRGB.WHITE, "", TextAlignment.LEFT_ALIGNED, 1.0F));
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
        //Set mouseover label text.
        mouseoverLabel.text = "";
        scannerItem.stack = null;
        tooltipText.clear();

        AEntityB_Existing mousedOverEntity = InterfaceManager.clientInterface.getMousedOverEntity();
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (player.isHoldingItemType(ItemComponentType.SCANNER)) {
            if (mousedOverEntity instanceof AEntityF_Multipart) {
                AEntityF_Multipart<?> multipart = (AEntityF_Multipart<?>) mousedOverEntity;
                Point3D playerEyesStartVector = player.getPosition().add(0, player.getEyeHeight(), 0);
                Point3D playerEyesEndVector = playerEyesStartVector.copy().add(player.getLineOfSight(10));

                BoundingBox mousedOverBox = null;
                JSONPartDefinition packVehicleDef = null;
                for (Entry<BoundingBox, JSONPartDefinition> boxEntry : multipart.activePartSlotBoxes.entrySet()) {
                    BoundingBox box = boxEntry.getKey();
                    if (box.getIntersectionPoint(playerEyesStartVector, playerEyesEndVector) != null) {
                        if (mousedOverBox == null || (box.globalCenter.distanceTo(playerEyesStartVector) < mousedOverBox.globalCenter.distanceTo(playerEyesStartVector))) {
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
        } else {
            if (mousedOverEntity instanceof PartInteractable) {
                EntityFluidTank tank = ((PartInteractable) mousedOverEntity).tank;
                if (tank != null) {
                    mouseoverLabel.text = tank.getFluid().isEmpty() ? "EMPTY" : tank.getFluid().toUpperCase() + " : " + tank.getFluidLevel() + "/" + tank.getMaxLevel();
                }
            } else if (mousedOverEntity instanceof TileEntityFluidLoader) {
                EntityFluidTank tank = ((TileEntityFluidLoader) mousedOverEntity).getTank();
                if (tank != null) {
                    mouseoverLabel.text = tank.getFluid().isEmpty() ? "EMPTY" : tank.getFluid().toUpperCase() + " : " + tank.getFluidLevel() + "/" + tank.getMaxLevel();
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
