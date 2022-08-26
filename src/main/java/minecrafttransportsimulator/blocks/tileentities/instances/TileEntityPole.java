package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest.EntityGUIType;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleCollisionUpdate;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Pole tile entity.  Remembers what components we have attached and the state of the components.
 * This tile entity does not tick, as states can be determined without ticks or are controlled
 * from other tickable TEs.
 *
 * @author don_bruce
 */
public class TileEntityPole extends ATileEntityBase<JSONPoleComponent> {
    public final Map<Axis, ATileEntityPole_Component> components = new HashMap<>();

    private float maxTotalLightLevel;

    public TileEntityPole(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, position, placingPlayer, data);

        //Load components back in.
        for (Axis axis : Axis.values()) {
            IWrapperNBT componentData = data.getData(axis.name());
            if (componentData != null) {
                ATileEntityPole_Component newComponent = PoleComponentType.createComponent(this, placingPlayer, axis, componentData);
                changeComponent(axis, newComponent);
            } else if (axis.equals(Axis.NONE)) {
                //Add our core component to the NONE axis.
                //This is done for ease of rendering and lookup routines.
                changeComponent(axis, PoleComponentType.createComponent(this, placingPlayer, axis, data));
            }
        }
    }

    @Override
    public void update() {
        super.update();
        //Forward update call to components.
        for (ATileEntityPole_Component component : components.values()) {
            component.update();
        }

        //If this is the first tick, update collision on ourselves and others.
        if (ticksExisted == 1) {
            updateCollision(false);
            for (Axis axis : Axis.values()) {
                if (!axis.equals(Axis.NONE)) {
                    ATileEntityBase<?> tile = world.getTileEntity(axis.getOffsetPoint(position));
                    if (tile instanceof TileEntityPole) {
                        ((TileEntityPole) tile).updateCollision(false);
                    }
                }
            }
        }
    }

    @Override
    public void remove() {
        super.remove();
        //Remove components as these all come with the main pole.
        for (ATileEntityPole_Component component : components.values()) {
            component.remove();
        }
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        //Fire a packet to interact with this pole.  Will either add, remove, or allow editing of the pole.
        //Only fire packet if player is holding a pole component that's not an actual pole, a wrench,
        //or is clicking a sign with text.
        Axis axis = Axis.getFromRotation(player.getYaw(), definition.pole.allowsDiagonals).getOpposite();
        IWrapperItemStack heldStack = player.getHeldStack();
        AItemBase heldItem = heldStack.getItem();
        ATileEntityPole_Component clickedComponent = components.get(axis);
        if (!ConfigSystem.settings.general.opSignEditingOnly.value || player.isOP()) {
            if (player.isHoldingItemType(ItemComponentType.WRENCH)) {
                //Holding a wrench, try to remove the component.
                //Need to check if it will fit in the player's inventory.
                if (components.containsKey(axis)) {
                    ATileEntityPole_Component component = components.get(axis);
                    if (player.isCreative() || player.getInventory().addStack(component.getItem().getNewStack(component.save(InterfaceManager.coreInterface.getNewNBTWrapper())))) {
                        changeComponent(axis, null);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityPoleChange(this, player, axis, null));
                    }
                    return true;
                }
            } else if (clickedComponent instanceof TileEntityPole_Sign && clickedComponent.definition.rendering != null && clickedComponent.definition.rendering.textObjects != null) {
                //Player clicked a sign with text.  Open the GUI to edit it.
                player.sendPacket(new PacketEntityGUIRequest(clickedComponent, player, EntityGUIType.TEXT_EDITOR));
                return true;
            } else if (heldItem instanceof ItemPoleComponent && !((ItemPoleComponent) heldItem).definition.pole.type.equals(PoleComponentType.CORE) && !components.containsKey(axis)) {
                //Player is holding component that could be added.  Try and do so.
                ItemPoleComponent componentItem = (ItemPoleComponent) heldItem;
                IWrapperNBT stackData = heldStack.getData();
                componentItem.populateDefaultData(stackData);
                ATileEntityPole_Component newComponent = PoleComponentType.createComponent(this, player, axis, stackData);
                changeComponent(axis, newComponent);
                if (!player.isCreative()) {
                    player.getInventory().removeStack(player.getHeldStack(), 1);
                }
                InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityPoleChange(this, player, axis, newComponent.save(InterfaceManager.coreInterface.getNewNBTWrapper())));
                return true;
            }
        }
        return false;
    }

    @Override
    public int getRotationIncrement() {
        return 360;
    }

    @Override
    public float getLightProvided() {
        return maxTotalLightLevel;
    }

    @Override
    public void addDropsToList(List<IWrapperItemStack> drops) {
        for (Axis axis : Axis.values()) {
            ATileEntityPole_Component component = components.get(axis);
            if (component != null) {
                drops.add(component.getItem().getNewStack(component.save(InterfaceManager.coreInterface.getNewNBTWrapper())));
            }
        }
    }

    /**
     * Helper method to add/remove components to this pole.  Ensures all states are maintained
     * for bounding boxes and component position.  To remove a component, pass-in null for the axis.
     */
    public void changeComponent(Axis newAxis, ATileEntityPole_Component newComponent) {
        //Update component map.
        if (newComponent != null) {
            components.put(newAxis, newComponent);
            newComponent.orientation.set(orientation);
            if (newAxis.equals(Axis.NONE)) {
                newComponent.position.set(position);
            } else {
                //Update orientation to account for axis rotation.
                newComponent.orientation.multiply(newAxis.rotation);

                //Adjust position to new orientation.
                newComponent.position.set(0, 0, definition.pole.radius + 0.001).rotate(newComponent.orientation).add(position);
            }
            world.addEntity(newComponent);
        } else if (components.containsKey(newAxis)) {
            components.remove(newAxis).remove();
        }

        //Update lighting state.
        maxTotalLightLevel = 0;
        for (ATileEntityPole_Component component : components.values()) {
            maxTotalLightLevel = Math.max(maxTotalLightLevel, component.getLightProvided());
        }
    }

    @Override
    public void onNeighborChanged(Point3D otherPosition) {
        updateCollision(true);
    }

    public void updateCollision(boolean sendToClient) {
        boundingBox.widthRadius = definition.pole.radius;
        boundingBox.heightRadius = definition.pole.radius;
        boundingBox.depthRadius = definition.pole.radius;
        boundingBox.globalCenter.set(position);
        boundingBox.globalCenter.add(0, 0.5, 0);
        for (Axis axis : Axis.values()) {
            if (axis.blockBased) {
                if (world.getBlock(axis.getOffsetPoint(position)) instanceof BlockPole || world.isBlockSolid(axis.getOffsetPoint(position), axis.getOpposite()) || components.containsKey(axis)) {
                    switch (axis) {
                        case NORTH: {
                            if (boundingBox.depthRadius == definition.pole.radius) {
                                boundingBox.depthRadius = (0.5 + definition.pole.radius) / 2D;
                                boundingBox.globalCenter.z = position.z - (0.5 - definition.pole.radius) / 2D;
                            } else {
                                boundingBox.depthRadius = 0.5;
                                boundingBox.globalCenter.z = position.z;
                            }
                            break;
                        }
                        case SOUTH: {
                            if (boundingBox.depthRadius == definition.pole.radius) {
                                boundingBox.depthRadius = (0.5 + definition.pole.radius) / 2D;
                                boundingBox.globalCenter.z = position.z + (0.5 - definition.pole.radius) / 2D;
                            } else {
                                boundingBox.depthRadius = 0.5;
                                boundingBox.globalCenter.z = position.z;
                            }
                            break;
                        }
                        case EAST: {
                            if (boundingBox.widthRadius == definition.pole.radius) {
                                boundingBox.widthRadius = (0.5 + definition.pole.radius) / 2D;
                                boundingBox.globalCenter.x = position.x + (0.5 - definition.pole.radius) / 2D;
                            } else {
                                boundingBox.widthRadius = 0.5;
                                boundingBox.globalCenter.x = position.x;
                            }
                            break;
                        }
                        case WEST: {
                            if (boundingBox.widthRadius == definition.pole.radius) {
                                boundingBox.widthRadius = (0.5 + definition.pole.radius) / 2D;
                                boundingBox.globalCenter.x = position.x - (0.5 - definition.pole.radius) / 2D;
                            } else {
                                boundingBox.widthRadius = 0.5;
                                boundingBox.globalCenter.x = position.x;
                            }
                            break;
                        }
                        case UP: {
                            if (boundingBox.heightRadius == definition.pole.radius) {
                                boundingBox.heightRadius = (0.5 + definition.pole.radius) / 2D;
                                boundingBox.globalCenter.y = position.y + 1.0 - boundingBox.heightRadius;
                            } else {
                                boundingBox.heightRadius = 0.5;
                                boundingBox.globalCenter.y = position.y + 0.5;
                            }
                            break;
                        }
                        case DOWN: {
                            if (boundingBox.heightRadius == definition.pole.radius) {
                                boundingBox.heightRadius = (0.5 + definition.pole.radius) / 2D;
                                boundingBox.globalCenter.y = position.y + boundingBox.heightRadius;
                            } else {
                                boundingBox.heightRadius = 0.5;
                                boundingBox.globalCenter.y = position.y + 0.5;
                            }
                            break;
                        }
                        default:
                            break;
                    }
                }
            }
        }

        //Send packet to clients to update them.
        if (sendToClient && !world.isClient()) {
            InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityPoleCollisionUpdate(this));
        }
    }

    @Override
    public boolean disableRendering(float partialTicks) {
        //We don't render poles themselves.  Just their components.
        return true;
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        //Save all components.
        for (Entry<Axis, ATileEntityPole_Component> connectedObjectEntry : components.entrySet()) {
            data.setData(connectedObjectEntry.getKey().name(), connectedObjectEntry.getValue().save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        }
        return data;
    }
}
