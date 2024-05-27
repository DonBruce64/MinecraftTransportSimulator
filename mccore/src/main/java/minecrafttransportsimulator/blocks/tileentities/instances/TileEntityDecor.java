package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONDecor.DecorComponentType;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityInteractGUI;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;

/**
 * Decor tile entity.  Contains the definition so we know how
 * to render this in the TESR call, as well as if we need to do
 * crafting operations if we are a crafting decor type.
 *
 * @author don_bruce
 */
public class TileEntityDecor extends ATileEntityBase<JSONDecor> {

    public static final String CLICKED_VARIABLE = "clicked";
    public static final String ACTIVATED_VARIABLE = "activated";
    private float lightLevel;
    public boolean craftedItem;

    public TileEntityDecor(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, ItemDecor item, IWrapperNBT data) {
        super(world, position, placingPlayer, item, data);
        //If we are on a slab, we go down to match it.
        if (world.isBlockBelowBottomSlab(position)) {
            this.position.y -= 0.5D;
            boundingBox.globalCenter.set(this.position);
        }
        //Set our bounding box based on our rotation and parameters.
        boundingBox.heightRadius = definition.decor.height / 2D;
        this.boundingBox.globalCenter.y += boundingBox.heightRadius;
        if (Math.abs(new Point3D(0, 0, 1).rotate(orientation).z) == 1) {
            boundingBox.widthRadius = definition.decor.width / 2D;
            boundingBox.depthRadius = definition.decor.depth / 2D;
        } else {
            boundingBox.widthRadius = definition.decor.depth / 2D;
            boundingBox.depthRadius = definition.decor.width / 2D;
        }
    }

    @Override
    public void update() {
        //Need to do this before updating as these require knowledge of prior states.
        //If we call super, then it will overwrite the prior state.
        //We update both our variables and our part variables here.
        updateVariableModifiers();

        super.update();
        //Reset clicked state and crafted item.
        setVariable(CLICKED_VARIABLE, 0);
    }

    @Override
    public boolean updateRider() {
        if (super.updateRider()) {
            position.y += definition.decor.sittingOffset;
            rider.setPosition(position, false);
            position.y -= definition.decor.sittingOffset;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        if (player.isHoldingItemType(ItemComponentType.PAINT_GUN)) {
            return false;
        } else if (player.isHoldingItemType(ItemComponentType.WRENCH)) {
            if (player.isSneaking() && !text.isEmpty()) {
                player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.TEXT_EDITOR));
                playersInteracting.add(player);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityInteractGUI(this, player, true));
            }
        } else if (definition.decor.type == DecorComponentType.SEAT) {
            setRider(player, true);
        } else {
            if (definition.decor.crafting != null) {
                player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.PART_BENCH));
                playersInteracting.add(player);
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityInteractGUI(this, player, true));
            }
            setVariable(CLICKED_VARIABLE, 1);
            toggleVariable(ACTIVATED_VARIABLE);
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableSet(this, CLICKED_VARIABLE, 1));
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableToggle(this, ACTIVATED_VARIABLE));
        }
        return true;
    }

    @Override
    public int getRotationIncrement() {
        return 90;
    }

    @Override
    public float getLightProvided() {
        return lightLevel;
    }

    @Override
    public void updateVariableModifiers() {
        lightLevel = definition.decor.lightLevel;

        //Adjust current variables to modifiers, if any exist.
        if (definition.variableModifiers != null) {
            for (JSONVariableModifier modifier : definition.variableModifiers) {
                switch (modifier.variable) {
                    case "lightLevel":
                        lightLevel = adjustVariable(modifier, lightLevel);
                        break;
                    default:
                        setVariable(modifier.variable, adjustVariable(modifier, (float) getVariable(modifier.variable)));
                        break;
                }
            }
        }
    }
}
