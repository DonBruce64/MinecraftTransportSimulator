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
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.instances.ItemPartEffector;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONPart.EffectorComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackMaterialComponent;

public class PartEffector extends APart {

    private final List<PartInteractable> linkedPullableCrates = new ArrayList<>();
    private final List<PartInteractable> linkedPushableCrates = new ArrayList<>();
    private final List<IWrapperItemStack> drops = new ArrayList<>();
    private final Map<IWrapperEntity, IWrapperItemStack> entityItems = new HashMap<>();

    //Variables used for all effectors.
    private final ComputedVariable operatedThisTickVar;
    private final Damage OPERATION_DAMAGE = new Damage(1.0D, boundingBox, null, null, null);

    //Variables used for drills.
    private final Point3D flooredCenter = new Point3D();
    private final Map<BoundingBox, Point3D> boxLastPositionsFloored = new HashMap<>();
    private final Map<BoundingBox, Integer> boxTimeSpentAtPosition = new HashMap<>();
    private final Set<Point3D> blockFlooredPositionsBrokeThisTick = new HashSet<>();

    //Variables used for placers.
    private int operationDelay;

    //Variables used for crafters.
    private final List<PackMaterialComponent> inputMaterials = new ArrayList<>();
    private final List<PackMaterialComponent> outputMaterials = new ArrayList<>();

    public PartEffector(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, ItemPartEffector item, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, item, data);
        if (definition.effector.type == EffectorComponentType.CRAFTER) {
            definition.effector.crafterInputs.forEach(input -> {
                PackMaterialComponent material = new PackMaterialComponent(input);
                if (material.possibleItems.isEmpty()) {
                    InterfaceManager.coreInterface.logError("ERROR: Crafter of type " + definition + " is set with a material input of " + input + " but that's not a valid item.  Contact your modpack, or pack author as this input is being skipped!");
                } else {
                    inputMaterials.add(material);
                }
            });
            definition.effector.crafterOutputs.forEach(output -> {
                PackMaterialComponent material = new PackMaterialComponent(output);
                if (material.possibleItems.isEmpty()) {
                    InterfaceManager.coreInterface.logError("ERROR: Crafter of type " + definition + " is set with a material output of " + output + " but that's not a valid item.  Contact your modpack, or pack author as this output is being skipped!");
                } else {
                    outputMaterials.add(material);
                }
            });
        }
        addVariable(this.operatedThisTickVar = new ComputedVariable(this, "effector_operated"));
    }

    @Override
    public void update() {
        super.update();
        //If we are active, do effector things.  Only do these on the server, clients get packets.
        boolean operatedThisTick = false;
        if (operationDelay < definition.effector.operationDelay) {
            ++operationDelay;
        } else {
            operationDelay = 0;
        }
        if (operatedThisTickVar.isActive) {
            operatedThisTickVar.toggle(false);
        }
        if (!world.isClient() && isActiveVar.isActive && !outOfHealth && operationDelay == definition.effector.operationDelay) {
            drops.clear();
            entityItems.clear();
            blockFlooredPositionsBrokeThisTick.clear();
            for (BoundingBox box : collisionBoxes) {
                if (box.collisionTypes.contains(CollisionType.EFFECTOR)) {
                    switch (definition.effector.type) {
                        case FERTILIZER: {
                            //Search all inventories for fertilizer and try to use it.
                            for (PartInteractable crate : linkedPullableCrates) {
                                if (crate.isActiveVar.isActive) {
                                    for (int i = 0; i < crate.inventory.getSize(); ++i) {
                                        IWrapperItemStack stack = crate.inventory.getStack(i);
                                        if (world.fertilizeBlock(box.globalCenter, stack)) {
                                            crate.inventory.removeFromSlot(i, 1);
                                            operatedThisTick = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        case HARVESTER: {
                            //Harvest drops, and add to inventories.
                            if (world.harvestBlock(box.globalCenter, drops)) {
                                operatedThisTick = true;
                            }
                            break;
                        }
                        case PLANTER: {
                            //Search all inventories for seeds and try to plant them.
                            for (PartInteractable crate : linkedPullableCrates) {
                                if (crate.isActiveVar.isActive) {
                                    for (int i = 0; i < crate.inventory.getSize(); ++i) {
                                        IWrapperItemStack stack = crate.inventory.getStack(i);
                                        if (world.plantBlock(box.globalCenter, stack)) {
                                            crate.inventory.removeFromSlot(i, 1);
                                            operatedThisTick = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        case PLOW: {
                            if (world.plowBlock(box.globalCenter)) {
                                operatedThisTick = true;
                                //Harvest blocks on top of this block in case they need to be dropped.
                                List<IWrapperItemStack> harvestedDrops = new ArrayList<>();
                                world.harvestBlock(box.globalCenter, harvestedDrops);
                                if (!harvestedDrops.isEmpty()) {
                                    for (IWrapperItemStack stack : harvestedDrops) {
                                        if (stack.getSize() > 0) {
                                            world.spawnItemStack(stack, position, null);
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        case SNOWPLOW: {
                            if (world.removeSnow(box.globalCenter)) {
                                operatedThisTick = true;
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
                                            operatedThisTick = true;
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
                            if (world.isAir(box.globalCenter)) {
                                //Search all inventories for blocks  and try to place them.
                                for (PartInteractable crate : linkedPullableCrates) {
                                    if (crate.isActiveVar.isActive) {
                                        for (int i = 0; i < crate.inventory.getSize(); ++i) {
                                            IWrapperItemStack stack = crate.inventory.getStack(i);
                                            if (world.placeBlock(box.globalCenter, stack)) {
                                                crate.inventory.removeFromSlot(i, 1);
                                                operatedThisTick = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (operatedThisTick) {
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                        case COLLECTOR: {
                            //Populate item list for later.
                            world.populateItemStackEntities(entityItems, box);
                            if (!entityItems.isEmpty()) {
                                drops.addAll(entityItems.values());
                                operatedThisTick = true;
                            }
                            break;
                        }
                        case DROPPER: {
                            //Place the first item found.
                            boolean placedItem = false;
                            for (PartInteractable crate : linkedPullableCrates) {
                                if (crate.isActiveVar.isActive) {
                                    for (int i = 0; i < crate.inventory.getSize(); ++i) {
                                        IWrapperItemStack stack = crate.inventory.getStack(i);
                                        if (!stack.isEmpty()) {
                                            IWrapperItemStack stackToDrop = stack.copy();
                                            stackToDrop.add(-stackToDrop.getSize() + 1);
                                            crate.inventory.removeFromSlot(i, 1);
                                            world.spawnItemStack(stackToDrop, box.globalCenter, new Point3D(Math.random() * 0.2D - 0.1D, -0.2D, Math.random() * 0.2D - 0.1D).rotate(orientation));
                                            operatedThisTick = true;
                                            placedItem = true;
                                            break;
                                        }
                                    }
                                }
                                if (placedItem) {
                                    break;
                                }
                            }
                            break;
                        }
                        case SPRAYER: {
                            //Just spray block below.
                            --box.globalCenter.y;
                            if (world.hydrateBlock(box.globalCenter)) {
                                operatedThisTick = true;
                            }
                            ++box.globalCenter.y;
                            break;
                        }
                        case CRAFTER: {
                            if (!outputMaterials.isEmpty()) {
                                for (PartInteractable crate : linkedPullableCrates) {
                                    if (crate.isActiveVar.isActive && crate.inventory.hasMaterials(inputMaterials)) {
                                        crate.inventory.removeMaterials(inputMaterials);
                                        outputMaterials.forEach(material -> drops.add(material.possibleItems.get(0).copy()));
                                        operatedThisTick = true;
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                    }

                    //Handle any drops we got from our effector.
                    if (!drops.isEmpty()) {
                        Iterator<IWrapperItemStack> iterator = drops.iterator();
                        while (iterator.hasNext()) {
                            IWrapperItemStack dropStack = iterator.next();
                            for (PartInteractable crate : linkedPushableCrates) {
                                if (crate.isActiveVar.isActive) {
                                    //For collectors, we can only add the whole stack, not a partial stack.
                                    //Therefore, we need to simulate the addition first to make sure things fit.
                                    if (definition.effector.type == EffectorComponentType.COLLECTOR) {
                                        if (crate.inventory.addStack(dropStack, dropStack.getSize(), false)) {
                                            operatedThisTick = true;
                                            for (Entry<IWrapperEntity, IWrapperItemStack> entry : entityItems.entrySet()) {
                                                if (entry.getValue() == dropStack) {
                                                    world.removeItemStackEntity(entry.getKey());
                                                }
                                            }
                                        }
                                    }
                                    //Do actual addition.
                                    if (crate.inventory.addStack(dropStack)) {
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
                                world.spawnItemStack(dropStack, position, null);
                            }
                        }
                        drops.clear();
                    }
                }
            }
            if (operatedThisTick) {
                attack(OPERATION_DAMAGE);
                operatedThisTickVar.setActive(true, true);
            }
        }
    }

    @Override
    public void updatePartList() {
        super.updatePartList();

        //Update linked effector linkables list.
        linkedPullableCrates.clear();
        linkedParts.forEach(part -> {
            if (part instanceof PartInteractable && part.definition.interactable.interactionType == InteractableComponentType.CRATE) {
                linkedPushableCrates.add((PartInteractable) part);
                if(part.definition.interactable.feedsVehicles) {
                    linkedPullableCrates.add((PartInteractable) part);
                }
            }
        });
        if (entityOn instanceof PartInteractable) {
            PartInteractable interactable = (PartInteractable) entityOn;
            if (interactable.definition.interactable.interactionType == InteractableComponentType.CRATE) {
                linkedPushableCrates.add(interactable);
                if (interactable.definition.interactable.feedsVehicles) {
                    linkedPullableCrates.add(interactable);
                }
            }
        }
    }
}
