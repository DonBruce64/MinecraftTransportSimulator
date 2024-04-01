package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.instances.ItemPartEffector;
import minecrafttransportsimulator.jsondefs.JSONPart.EffectorComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartEffector;

public class PartEffector extends APart {

    private final List<IWrapperItemStack> drops = new ArrayList<>();
    private final Map<IWrapperEntity, IWrapperItemStack> entityItems = new HashMap<>();

    //Variables used for drills.
    public int blocksBroken;
    public boolean activatedThisTick;
    private final Point3D flooredCenter = new Point3D();
    private final Map<BoundingBox, Point3D> boxLastPositionsFloored = new HashMap<>();
    private final Map<BoundingBox, Integer> boxTimeSpentAtPosition = new HashMap<>();
    private final Set<Point3D> blockFlooredPositionsBrokeThisTick = new HashSet<>();

    //Variables used for placers.
    private int placerDelay;

    public PartEffector(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, ItemPartEffector item, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, item, data);
        if (data != null) {
            this.blocksBroken = data.getInteger("blocksBroken");
        }
    }

    @Override
    public void update() {
        super.update();
        //If we are active, do effector things.  Only do these on the server, clients get packets.
        activatedThisTick = false;
        if (isActive && !world.isClient() && !outOfHealth) {
            drops.clear();
            entityItems.clear();
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
                                        activatedThisTick = true;
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case HARVESTER: {
                        //Harvest drops, and add to inventories.
                        List<IWrapperItemStack> drops = world.harvestBlock(box.globalCenter);
                        if (!drops.isEmpty()) {
                            drops.addAll(world.harvestBlock(box.globalCenter));
                            activatedThisTick = true;
                        }
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
                                        activatedThisTick = true;
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case PLOW: {
                        if (world.plowBlock(box.globalCenter)) {
                            activatedThisTick = true;
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
                        if (world.removeSnow(box.globalCenter)) {
                            activatedThisTick = true;
                        }
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
                                            remove();
                                        } else {
                                            InterfaceManager.packetInterface.sendToAllClients(new PacketPartEffector(this, true));
                                        }
                                        activatedThisTick = true;
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
                    case PLACER: {
                        if (placerDelay == definition.effector.placerDelay) {
                            if (world.isAir(box.globalCenter)) {
                                //Search all inventories for blocks  and try to place them.
                                for (APart part : linkedParts) {
                                    if (part instanceof PartInteractable && part.definition.interactable.interactionType == InteractableComponentType.CRATE && part.isActive && part.definition.interactable.feedsVehicles) {
                                        EntityInventoryContainer inventory = ((PartInteractable) part).inventory;
                                        for (int i = 0; i < inventory.getSize(); ++i) {
                                            IWrapperItemStack stack = inventory.getStack(i);
                                            if (world.placeBlock(box.globalCenter, stack)) {
                                                inventory.removeFromSlot(i, 1);
                                                activatedThisTick = true;
                                                break;
                                            }
                                        }
                                    }
                                    if(activatedThisTick) {
                                        break;
                                    }
                                }
                            }
                            placerDelay = 0;
                        } else {
                            ++placerDelay;
                        }
                        break;
                    }
                    case COLLECTOR: {
                        //Populate item list for later.
                        world.populateItemStackEntities(entityItems, box);
                        drops.addAll(entityItems.values());
                        break;
                    }
                    case DROPPER: {
                        if (placerDelay == definition.effector.placerDelay) {
                            world.populateItemStackEntities(entityItems, box);
                            if(entityItems.isEmpty()) {
                              //Place the first item found.
                                for (APart part : linkedParts) {
                                    if (part instanceof PartInteractable && part.definition.interactable.interactionType == InteractableComponentType.CRATE && part.isActive && part.definition.interactable.feedsVehicles) {
                                        EntityInventoryContainer inventory = ((PartInteractable) part).inventory;
                                        for (int i = 0; i < inventory.getSize(); ++i) {
                                            IWrapperItemStack stack = inventory.getStack(i);
                                            if (!stack.isEmpty()) {
                                                IWrapperItemStack stackToDrop = stack.split(1);
                                                world.spawnItemStack(stackToDrop, box.globalCenter);
                                                activatedThisTick = true;
                                                break;
                                            }
                                        }
                                    }
                                    if(activatedThisTick) {
                                        break;
                                    }
                                }
                            }
                            placerDelay = 0;
                        } else {
                            ++placerDelay;
                        }
                        break;
                    }
                    case SPRAYER: {
                        //Just spray block below.
                        --box.globalCenter.y;
                        world.hydrateBlock(box.globalCenter);
                        ++box.globalCenter.y;
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
                                //For collectors, we can only add the whole stack, not a partial stack.
                                //Therefore, we need to simulate the addition first to make sure things fit.
                                if (definition.effector.type == EffectorComponentType.COLLECTOR) {
                                    if (((PartInteractable) part).inventory.addStack(dropStack, dropStack.getSize(), false)) {
                                        activatedThisTick = true;
                                        for(Entry<IWrapperEntity, IWrapperItemStack> entry : entityItems.entrySet()) {
                                            if(entry.getValue() == dropStack) {
                                                world.removeItemStackEntity(entry.getKey());
                                            }
                                        }
                                    }
                                }
                                //Do actual addition.
                                if (((PartInteractable) part).inventory.addStack(dropStack)) {
                                    iterator.remove();
                                    break;
                                }
                            }
                        }
                    }

                    //Check our drops.  If we couldn't add any of them to any inventory, drop them on the ground instead.
                    //Don't do this for collectors, since those items are actually entities that weren't collected.
                    if (definition.effector.type != EffectorComponentType.COLLECTOR) {
                        for (IWrapperItemStack dropStack : drops) {
                            world.spawnItemStack(dropStack, position);
                        }
                    }
                    drops.clear();
                }
            }
            if (activatedThisTick) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketPartEffector(this, false));
            }
        }
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        switch (variable) {
            case ("effector_active"):
                return isActive ? 1 : 0;
            case ("effector_operated"):
                return activatedThisTick ? 1 : 0;
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
