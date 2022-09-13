package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartEffector;

public class PartEffector extends APart {

    private final List<IWrapperItemStack> drops = new ArrayList<>();

    //Variables used for drills.
    public int blocksBroken;
    private final Point3D flooredCenter = new Point3D();
    private final Map<BoundingBox, Point3D> boxLastPositionsFloored = new HashMap<>();
    private final Map<BoundingBox, Integer> boxTimeSpentAtPosition = new HashMap<>();
    private final Set<Point3D> blockFlooredPositionsBrokeThisTick = new HashSet<>();

    public PartEffector(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, data);
        this.blocksBroken = data.getInteger("blocksBroken");
    }

    @Override
    public void update() {
        super.update();
        //If we are active, do effector things.
        if (isActive && !world.isClient() && damageAmount < definition.general.health) {
            drops.clear();
            blockFlooredPositionsBrokeThisTick.clear();
            for (BoundingBox box : entityCollisionBoxes) {
                switch (definition.effector.type) {
                    case FERTILIZER: {
                        //Search all inventories for fertilizer and try to use it.
                        for (APart part : linkedParts) {
                            if (part instanceof PartInteractable && part.definition.interactable.interactionType.equals(InteractableComponentType.CRATE) && part.isActive && part.definition.interactable.feedsVehicles) {
                                EntityInventoryContainer inventory = ((PartInteractable) part).inventory;
                                for (int i = 0; i < inventory.getSize(); ++i) {
                                    IWrapperItemStack stack = inventory.getStack(i);
                                    if (world.fertilizeBlock(box.globalCenter, stack)) {
                                        inventory.removeFromSlot(i, 1);
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case HARVESTER: {
                        //Harvest drops, and add to inventories.
                        drops.addAll(world.harvestBlock(box.globalCenter));
                        break;
                    }
                    case PLANTER: {
                        //Search all inventories for seeds and try to plant them.
                        for (APart part : linkedParts) {
                            if (part instanceof PartInteractable && part.definition.interactable.interactionType.equals(InteractableComponentType.CRATE) && part.isActive && part.definition.interactable.feedsVehicles) {
                                EntityInventoryContainer inventory = ((PartInteractable) part).inventory;
                                for (int i = 0; i < inventory.getSize(); ++i) {
                                    IWrapperItemStack stack = inventory.getStack(i);
                                    if (world.plantBlock(box.globalCenter, stack)) {
                                        inventory.removeFromSlot(i, 1);
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case PLOW: {
                        if (world.plowBlock(box.globalCenter)) {
                            //Harvest blocks on top of this block in case they need to be dropped.
                            List<IWrapperItemStack> harvestedDrops = world.harvestBlock(box.globalCenter);
                            if (!harvestedDrops.isEmpty()) {
                                for (IWrapperItemStack stack : harvestedDrops) {
                                    if (stack.getSize() > 0) {
                                        world.spawnItemStack(stack, position);
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case SNOWPLOW: {
                        world.removeSnow(box.globalCenter);
                        break;
                    }
                    case DRILL: {
                        if (!world.isAir(box.globalCenter)) {
                            float blockHardness = world.getBlockHardness(box.globalCenter);
                            if (blockHardness <= definition.effector.drillHardness) {
                                if (!boxLastPositionsFloored.containsKey(box)) {
                                    boxLastPositionsFloored.put(box, new Point3D());
                                    boxTimeSpentAtPosition.put(box, 0);
                                }

                                flooredCenter.set(Math.floor(box.globalCenter.x), Math.floor(box.globalCenter.y), Math.floor(box.globalCenter.z));
                                if (boxLastPositionsFloored.get(box).equals(flooredCenter) && !blockFlooredPositionsBrokeThisTick.contains(flooredCenter)) {
                                    int timeSpentBreaking = boxTimeSpentAtPosition.get(box);
                                    if (timeSpentBreaking >= definition.effector.drillSpeed * blockHardness / definition.effector.drillHardness) {
                                        drops.addAll(world.getBlockDrops(flooredCenter));
                                        world.destroyBlock(flooredCenter, false);
                                        boxTimeSpentAtPosition.put(box, 0);
                                        blockFlooredPositionsBrokeThisTick.add(flooredCenter.copy());
                                        if (++blocksBroken == definition.effector.drillDurability) {
                                            this.isValid = false;
                                        } else {
                                            InterfaceManager.packetInterface.sendToAllClients(new PacketPartEffector(this));
                                        }
                                    } else {
                                        boxTimeSpentAtPosition.put(box, timeSpentBreaking + 1);
                                    }
                                    break;
                                }
                                boxLastPositionsFloored.put(box, flooredCenter.copy());
                            }
                        }
                        boxTimeSpentAtPosition.put(box, 0);
                        break;
                    }
                }

                //Handle any drops we got from our effector.
                if (!drops.isEmpty()) {
                    Iterator<IWrapperItemStack> iterator = drops.iterator();
                    while (iterator.hasNext()) {
                        IWrapperItemStack dropStack = iterator.next();
                        for (APart part : linkedParts) {
                            if (part instanceof PartInteractable && part.isActive && part.definition.interactable.interactionType.equals(InteractableComponentType.CRATE)) {
                                if (((PartInteractable) part).inventory.addStack(dropStack)) {
                                    iterator.remove();
                                    break;
                                }
                            }
                        }
                    }

                    //Check our drops.  If we couldn't add any of them to any inventory, drop them on the ground instead.
                    for (IWrapperItemStack dropStack : drops) {
                        world.spawnItemStack(dropStack, position);
                    }
                    drops.clear();
                }
            }
        }
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        switch (variable) {
            case ("effector_active"):
                return isActive ? 1 : 0;
            case ("effector_drill_broken"):
                return blocksBroken;
            case ("effector_drill_max"):
                return definition.effector.drillDurability;
            case ("effector_drill_percentage"):
                return blocksBroken / (double) definition.effector.drillDurability;
        }

        return super.getRawVariableValue(variable, partialTicks);
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setInteger("blocksBroken", blocksBroken);
        return data;
    }
}
