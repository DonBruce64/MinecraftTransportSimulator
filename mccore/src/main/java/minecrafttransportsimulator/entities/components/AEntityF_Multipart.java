package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.jsondefs.AJSONPartProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartChange;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * Base class for multipart entities.  These entities hold other, part-based entities.  These part
 * entities may be added or removed from this entity based on the implementation, but assurances
 * are made with how they are stored and how they are accessed.
 *
 * @author don_bruce
 */
public abstract class AEntityF_Multipart<JSONDefinition extends AJSONPartProvider> extends AEntityE_Interactable<JSONDefinition> {

    /**
     * This list contains all parts this entity has.  Do NOT directly modify this list.  Instead,
     * call {@link #addPart}, {@link #addPartFromItem}, or {@link #removePart} to ensure all sub-classed
     * operations are performed.  Note that if you are iterating over this list when you call one of those
     * methods, and you don't pass the method an iterator instance, you will get a CME!.
     */
    public final List<APart> parts = new ArrayList<>();

    /**
     * Like {@link #parts}, except contains all parts on parts as well, recursively to the lowest part.
     */
    public final List<APart> allParts = new ArrayList<>();

    /**
     * Identical to {@link #parts}, except this list has null elements for empty slots.  Designed
     * for obtaining the part in a specific slot rather than iterative operations.
     */
    public final List<APart> partsInSlots = new ArrayList<>();

    /**
     * List of block collision boxes, with all part block collision boxes included.
     **/
    public final List<BoundingBox> allBlockCollisionBoxes = new ArrayList<>();

    /**
     * List of entity collision boxes, with all part collision boxes included.
     **/
    public final List<BoundingBox> allEntityCollisionBoxes = new ArrayList<>();

    /**
     * List of interaction boxes, plus all part boxes included.
     **/
    public final List<BoundingBox> allInteractionBoxes = new ArrayList<>();

    /**
     * List of bullet boxes, plus all part boxes included.
     **/
    public final List<BoundingBox> allBulletCollisionBoxes = new ArrayList<>();

    /**
     * Map of part slot boxes.  Key is the box, value is the definition for that slot.
     **/
    public final Map<BoundingBox, JSONPartDefinition> partSlotBoxes = new HashMap<>();
    private final Map<JSONPartDefinition, AnimationSwitchbox> partSlotSwitchboxes = new HashMap<>();

    /**
     * Map of active part slot boxes.  Boxes in here will also be in {@link #partSlotBoxes}.
     **/
    public final Map<BoundingBox, JSONPartDefinition> activePartSlotBoxes = new HashMap<>();

    /**
     * Map of part slot boxes, plus all part boxes included.
     **/
    public final Map<BoundingBox, JSONPartDefinition> allPartSlotBoxes = new HashMap<>();

    //Constants
    private final float PART_SLOT_HITBOX_WIDTH = 0.75F;
    private final float PART_SLOT_HITBOX_HEIGHT = 2.25F;

    public AEntityF_Multipart(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, placingPlayer, data);
    }

    @Override
    protected void initializeAnimations() {
        super.initializeAnimations();
        //Check for removed slots.
        if (definition.parts != null) {
            //Check for removed slots.
            if(partsInSlots.size() > definition.parts.size()) {
                List<APart> removedParts = new ArrayList<APart>();
                while(partsInSlots.size() > definition.parts.size()) {
                    APart partRemoved = partsInSlots.remove(partsInSlots.size() - 1);
                    if (partRemoved != null) {
                        removedParts.add(partRemoved);
                    }
                }
                removedParts.forEach(part -> {
                    //Need to manually remove, since indexes won't exist.
                    parts.remove(part);
                    part.remove();
                });
            }

            //Check for added slots.
            while (partsInSlots.size() < definition.parts.size()) {
                partsInSlots.add(null);
            }

            //Update existing slots.
            parts.forEach(part -> {
                if (!part.isFake()) {
                    part.placementDefinition = definition.parts.get(part.placementSlot);
                    part.animationsInitialized = false;
                }
            });
            if (definition.parts != null) {
                partSlotSwitchboxes.clear();
                for (JSONPartDefinition partDef : definition.parts) {
                    if (partDef.animations != null || partDef.applyAfter != null) {
                        List<JSONAnimationDefinition> animations = new ArrayList<>();
                        if (partDef.animations != null) {
                            animations.addAll(partDef.animations);
                        }
                        partSlotSwitchboxes.put(partDef, new AnimationSwitchbox(this, animations, partDef.applyAfter));
                    }
                }
            }
            recalculatePartSlots();
        }
    }

    @Override
    public void update() {
        //Need to do this before updating as these require knowledge of prior states.
        //If we call super, then it will overwrite the prior state.
        //We update both our variables and our part variables here.
        updateVariableModifiers();
        for (APart part : parts) {
            part.updateVariableModifiers();
        }

        //Now call super and do the updates.
        super.update();
        world.beginProfiling("EntityF_Level", true);

        //Populate active part slot list.
        //Only do this on clients; servers reference the main list to handle clicks.
        //Boxes added on clients depend on what the player is holding.
        //We add these before part boxes so the player can click them before clicking a part.
        if (world.isClient() && !partSlotBoxes.isEmpty()) {
            world.beginProfiling("PartSlotActives", false);
            activePartSlotBoxes.clear();
            if (canBeClicked()) {
                IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
                AItemBase heldItem = player.getHeldItem();
                if (heldItem instanceof AItemPart) {
                    for (Entry<BoundingBox, JSONPartDefinition> partSlotBoxEntry : partSlotBoxes.entrySet()) {
                        AItemPart heldPart = (AItemPart) heldItem;
                        //Does the part held match this packPart?
                        if (heldPart.isPartValidForPackDef(partSlotBoxEntry.getValue(), subDefinition, false)) {
                            //Are there any doors blocking us from clicking this part?
                            if (isVariableListTrue(partSlotBoxEntry.getValue().interactableVariables)) {
                                //Part matches.  Add the box.  Set the box bounds to the generic box, or the
                                //special bounds of the generic part if we're holding one.
                                BoundingBox box = partSlotBoxEntry.getKey();
                                box.widthRadius = (heldPart.definition.generic.width != 0 ? heldPart.definition.generic.width / 2D : PART_SLOT_HITBOX_WIDTH / 2D) * scale.x;
                                box.heightRadius = (heldPart.definition.generic.height != 0 ? heldPart.definition.generic.height / 2D : PART_SLOT_HITBOX_HEIGHT / 2D) * scale.y;
                                box.depthRadius = (heldPart.definition.generic.width != 0 ? heldPart.definition.generic.width / 2D : PART_SLOT_HITBOX_WIDTH / 2D) * scale.z;
                                activePartSlotBoxes.put(partSlotBoxEntry.getKey(), partSlotBoxEntry.getValue());
                                forceCollisionUpdateThisTick = true;
                                if(this instanceof APart) {
                                    ((APart) this).masterEntity.forceCollisionUpdateThisTick = true;
                                }
                            }
                        }
                    }
                } else if (heldItem instanceof ItemItem && ((ItemItem) heldItem).definition.item.type == ItemComponentType.SCANNER) {
                    //Don't check held parts, just check if we can actually place anything in a slot.
                    for (Entry<BoundingBox, JSONPartDefinition> partSlotBoxEntry : partSlotBoxes.entrySet()) {
                        if (isVariableListTrue(partSlotBoxEntry.getValue().interactableVariables)) {
                            activePartSlotBoxes.put(partSlotBoxEntry.getKey(), partSlotBoxEntry.getValue());
                            forceCollisionUpdateThisTick = true;
                        }
                    }
                }
            }
        }

        if (requiresDeltaUpdates()) {
            //Update part slot box positions.
            world.beginProfiling("PartSlotPositions", true);
            partSlotBoxes.forEach((box, partDef) -> {
                AnimationSwitchbox switchBox = partSlotSwitchboxes.get(partDef);
                if (switchBox != null) {
                    if (switchBox.runSwitchbox(0, false)) {
                        box.globalCenter.set(box.localCenter).transform(switchBox.netMatrix);
                        box.updateToEntity(this, box.globalCenter);
                    }
                } else {
                    box.updateToEntity(this, null);
                }
            });
        }

        world.endProfiling();
        world.endProfiling();
    }

    @Override
    public double getMass() {
        //Return our mass, plus our parts.
        double currentMass = super.getMass();
        for (APart part : parts) {
            currentMass += part.getMass();
        }
        return currentMass;
    }

    @Override
    public void attack(Damage damage) {
        //If the bounding box attacked corresponds to a part, forward the attack to that part for calculation.
        //Otherwise, we allow ourselves to be attacked.
        APart hitPart = getPartWithBox(damage.box);
        if (hitPart != null) {
            if (hitPart.isValid) {
                hitPart.attack(damage);
            }
        } else {
            super.attack(damage);
        }
    }

    @Override
    public void remove() {
        super.remove();
        //Call all the part removal methods to ensure they save their states properly.
        for (APart part : parts) {
            part.remove();
        }
    }

    @Override
    public void updateText(LinkedHashMap<String, String> textLines) {
        super.updateText(textLines);
        allParts.forEach(part -> {
            for (Entry<JSONText, String> textEntry : part.text.entrySet()) {
                textEntry.setValue(textLines.get(textEntry.getKey().fieldName));
            }
        });
    }

    @Override
    public Collection<BoundingBox> getCollisionBoxes() {
        return allEntityCollisionBoxes;
    }

    @Override
    public Collection<BoundingBox> getInteractionBoxes() {
        return allInteractionBoxes;
    }

    @Override
    public boolean canCollideWith(AEntityB_Existing entityToCollide) {
        return !(entityToCollide instanceof APart);
    }

    @Override
    public void doPostUpdateLogic() {
        //Update parts prior to doing our post-updates.
        //This is required for trailers, as they may attached to parts.
        //This also ensures that during our post-update loop, all parts are post-updated.
        world.beginProfiling("PartUpdates_" + parts.size(), true);
        Iterator<APart> iterator = parts.iterator();
        while (iterator.hasNext()) {
            APart part = iterator.next();
            part.update();
            if (!part.isValid) {
                removePart(part, iterator);
            } else {
                part.doPostUpdateLogic();
            }
        }
        world.endProfiling();
        super.doPostUpdateLogic();
    }

    @Override
    protected void updateCollisionBoxes() {
        super.updateCollisionBoxes();
        //Add part slot boxes to interaction boxes since we can interact with those.
        interactionBoxes.addAll(activePartSlotBoxes.keySet());
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        //If we have a variable with a suffix, we need to get that part first and pass
        //it into this method rather than trying to run through the code now.
        int partNumber = getVariableNumber(variable);
        if (partNumber != -1) {
            return getSpecificPartAnimation(variable, partNumber, partialTicks);
        } else {
            return super.getRawVariableValue(variable, partialTicks);
        }
    }

    @Override
    public void toggleVariable(String variable) {
        int partNumber = getVariableNumber(variable);
        if (partNumber != -1) {
            APart foundPart = getSpecificPart(variable, partNumber);
            if (foundPart != null) {
                variable = variable.substring(0, variable.lastIndexOf("_"));
                foundPart.toggleVariable(variable);
            }
        } else {
            super.toggleVariable(variable);
        }
    }

    @Override
    public void setVariable(String variable, double value) {
        int partNumber = getVariableNumber(variable);
        if (partNumber != -1) {
            APart foundPart = getSpecificPart(variable, partNumber);
            if (foundPart != null) {
                foundPart.setVariable(variable.substring(0, variable.lastIndexOf("_")), value);
            }
        } else {
            super.setVariable(variable, value);
        }
    }

    /**
     * Returns true if this entity can be clicked.  Normally true, but can be false to block
     * click-able hitboxes from showing up.
     */
    public boolean canBeClicked() {
        return true;
    }

    /**
     * Returns true if any linked variables are blocking the player from
     * accessing the passed-in part slot.
     */
    public boolean isVariableListTrue(List<List<String>> list) {
        if (list != null) {
	        for (List<String> variableList : list) {
	            boolean listIsTrue = false;
	            for (String variableName : variableList) {
	                if (variableName.startsWith("!")) {
	                    double value = getRawVariableValue(variableName.substring(1), 0);
	                    if (value == 0 || Double.isNaN(value)) {
	                        //Inverted variable value is 0, therefore list is true.
	                        listIsTrue = true;
	                        break;
	                    }
	                } else {
	                    double value = getRawVariableValue(variableName, 0);
	                    if (!Double.isNaN(value) && value > 0) {
	                        //Normal variable value is non-zero 0, therefore list is true.
	                        listIsTrue = true;
	                        break;
	                    }
	                }
	            }
	            if (!listIsTrue) {
	                //List doesn't have any true variables, therefore the value is false.
	                return false;
	            }
	        }
	        //No false lists were found for this collection, therefore the list is true.
        }  //No lists found for this entry, therefore no variables are false.

        return true;
    }

    /**
     * Called to add parts from NBT.  This cannot be done during construction, as this method adds sub-parts
     * defined in this multipart's definition.  If this was done in the constructor, and those sub parts
     * depended on some property that was present in the extended constructor of this multipart, then the
     * sub-parts wouldn't have all the info they needed.  As such, this method should be called only after
     * this multipart exists in the world.  And, if it is a part, it has been added to the multipart it is a part of.
     */
    public void addPartsPostAddition(IWrapperPlayer placingPlayer, IWrapperNBT data) {
        //Init part lookup list and add parts.
        if (definition.parts != null) {
            //Need to init slots first, just in case we reference them on sub-part linking logic.
            for (int i = 0; i < definition.parts.size(); ++i) {
                partsInSlots.add(null);
            }

            boolean newEntity = data.getString("uniqueUUID").isEmpty();
            for (int i = 0; i < definition.parts.size(); ++i) {
                //Use a try-catch for parts in case they've changed since this entity was last placed.
                //Don't want crashes due to pack updates.
                try {
                    IWrapperNBT partData = data.getData("part_" + i);
                    if (partData != null) {
                        AItemPart partItem = PackParser.getItem(partData.getString("packID"), partData.getString("systemName"), partData.getString("subName"));

                        //TODO remove this a few versions down the line.
                        int partSlot = i;
                        Point3D partOffset = partData.getPoint3d("offset");
                        if (!partOffset.isZero()) {
                            for (int j = 0; j < definition.parts.size(); ++j) {
                                JSONPartDefinition partDef = definition.parts.get(j);
                                if (partDef.pos.equals(partOffset)) {
                                    partSlot = j;
                                    break;
                                }
                            }
                        }
                        //End todo

                        addPartFromItem(partItem, placingPlayer, partData, partSlot);
                    }
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("Could not load part from NBT.  Did you un-install a pack?");
                    e.printStackTrace();
                }

                //Add default parts.  We need to do this after we actually create this part so its slots are valid.
                //We also need to know if it is a new part or not, since that allows non-permanent default parts to be added.
                JSONPartDefinition partDef = definition.parts.get(i);
                if (newEntity) {
                    if (partDef.conditionalDefaultParts != null) {
                        //Add constants. This is also done in initializeAnimations, but repeating it here ensures 
                    	//the value will be set before spawning in any conditional parts.
                        if (definition.rendering != null && definition.rendering.constants != null) {
                            for (String variable : definition.rendering.constants) {
                                variables.put(variable, 1D);
                            }
                        }
                        for (Entry<String, String> conditionalDef : partDef.conditionalDefaultParts.entrySet()) {
                            if (getRawVariableValue(conditionalDef.getKey(), 0) > 0) {
                                addDefaultPart(conditionalDef.getValue(), i, placingPlayer, definition);
                                break;
                            }
                        }
                    }
                    if (partDef.defaultPart != null) {
                        addDefaultPart(partDef.defaultPart, i, placingPlayer, definition);
                    }
                }
            }
        }

        //Create the initial boxes and slots.
        recalculatePartSlots();
    }

    /**
     * Adds the passed-part to this entity.  This method will check at the passed-in point
     * if the item-based part can go to this entity.  If so, it is constructed and added,
     * and a packet is sent to all clients to inform them of this change.  Returns true
     * if all operations completed, false if the part was not able to be added.
     * If the part is being added during construction, set doingConstruction to true to
     * prevent calling the lists, maps, and other systems that aren't set up yet.
     * This method returns the part if it was added, null if it wasn't.
     */
    public APart addPartFromItem(AItemPart partItem, IWrapperPlayer playerAdding, IWrapperNBT partData, int slotIndex) {
        JSONPartDefinition newPartDef = definition.parts.get(slotIndex);
        if (partsInSlots.get(slotIndex) == null && (newPartDef.bypassSlotChecks || partItem.isPartValidForPackDef(newPartDef, subDefinition, true))) {
            //Part is not already present, and is valid, add it.
            partItem.populateDefaultData(partData);
            APart partToAdd = partItem.createPart(this, playerAdding, newPartDef, partData);
            addPart(partToAdd, true);
            partToAdd.addPartsPostAddition(playerAdding, partData);
            return partToAdd;
        } else {
            return null;
        }
    }

    /**
     * Adds the passed-in part to the entity.  Also is responsible for modifying
     * and lists or maps that may have changed from adding the part.
     */
    public void addPart(APart part, boolean sendPacket) {
        parts.add(part);
        if (!part.isFake()) {
            partsInSlots.set(part.placementSlot, part);

            //Recalculate slots.
            recalculatePartSlots();

            //If we are on the server, and need to notify clients, do so.
            if (sendPacket && !world.isClient()) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketPartChange(this, part));
            }
        }

        //Add the part to the world.
        world.addEntity(part);

        //Let parts know a change was made.
        part.masterEntity.updateAllpartList();
        part.masterEntity.updatePartList();
    }

    /**
     * Removes the passed-in part from the entity.  Calls the part's {@link APart#remove()} method to
     * let the part handle removal code.  Iterator is optional, but if you're in any code block that
     * is iterating over the parts list, and you don't pass that iterator in, you'll get a CME.
     */
    public void removePart(APart part, Iterator<APart> iterator) {
        if (parts.contains(part)) {
            //Call the part's removal code for it to process.
            part.remove();

            //Remove part from main list of parts.
            if (iterator != null) {
                iterator.remove();
            } else {
                parts.remove(part);
            }
            if (!part.isFake()) {
                partsInSlots.set(definition.parts.indexOf(part.placementDefinition), null);

                //Recalculate slots.
                recalculatePartSlots();

                //If we are on the server, notify all clients of this change.
                if (!world.isClient()) {
                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartChange(part));
                }
            }
        }

        //Let parts know a change was made.
        part.masterEntity.updateAllpartList();
        part.masterEntity.updatePartList();
    }

    /**
     * Called whenever a part is added or removed from the entity this part is on.
     * At the time of call, the part that was added will already be added, and the part
     * that was removed will already be removed.
     */
    protected void updateAllpartList() {
        allParts.clear();
        parts.forEach(part -> {
            part.updateAllpartList();
            allParts.add(part);
            allParts.addAll(part.allParts);
        });
    }

    /**
     * Called after all allPart lists associated with this entity have been updated.
     * This includes both parent and child lists.  Operations that reference the allpart
     * list should occur here, not in {@link #updateAllpartList()}.
     */
    public void updatePartList() {
        parts.forEach(APart::updatePartList);
    }

    /**
     * Gets the part that has the passed-in bounding box.
     * Useful if we interacted with a box on this multipart and need
     * to know exactly what it went to.
     */
    public APart getPartWithBox(BoundingBox box) {
        for (APart part : parts) {
            if (part.allInteractionBoxes.contains(box) || part.allBulletCollisionBoxes.contains(box)) {
                if (part.interactionBoxes.contains(box) || part.bulletCollisionBoxes.contains(box)) {
                    return part;
                } else {
                    return part.getPartWithBox(box);
                }
            }
        }
        return null;
    }

    /**
     * Helper method to add the default part for the passed-in part def.
     * This method adds all default parts for the passed-in part entry.
     * The entry can either be on the main entity, or a part on this entity.
     * This method should only be called when the entity or part with the
     * passed-in definition is placed on this entity, not when it's being loaded from saved data.
     */
    public void addDefaultPart(String partName, int partSlot, IWrapperPlayer playerAdding, AJSONPartProvider providingDef) {
    	//Don't even try if the partName is an empty string
    	if (!partName.isEmpty()) {
	        try {
	            String partPackID = partName.substring(0, partName.indexOf(':'));
	            String partSystemName = partName.substring(partName.indexOf(':') + 1);
	            try {
	                APart addedPart = addPartFromItem(PackParser.getItem(partPackID, partSystemName), playerAdding, InterfaceManager.coreInterface.getNewNBTWrapper(), partSlot);
	                if (addedPart != null) {
	                    //Set the default tone for the part, if it requests one and we can provide one.
	                    addedPart.updateTone(false);
	                }
	            } catch (NullPointerException e) {
	            	playerAdding.sendPacket(new PacketPlayerChatMessage(playerAdding, "Attempted to add defaultPart: " + partPackID + ":" + partSystemName + " to: " + providingDef.packID + ":" + providingDef.systemName + " but that part doesn't exist in the pack item registry."));
	            }
	        } catch (IndexOutOfBoundsException e) {
	        	playerAdding.sendPacket(new PacketPlayerChatMessage(playerAdding, "Could not parse defaultPart definition: " + partName + ".  Format should be \"packId:partName\""));
	        }
    	}
    }

    /**
     * Call to re-create the list of all valid part slot boxes.
     * This should be called after part addition or part removal.
     * Also must be called at construction time to create the initial slot set.
     */
    private void recalculatePartSlots() {
        partSlotBoxes.clear();
        //Need to clear this since if we have no part slots we won't run the code to align this.
        activePartSlotBoxes.clear();
        for (int i = 0; i < partsInSlots.size(); ++i) {
            if (partsInSlots.get(i) == null) {
                JSONPartDefinition partDef = definition.parts.get(i);
                BoundingBox newSlotBox = new BoundingBox(partDef.pos, partDef.pos.copy().rotate(orientation).add(position), PART_SLOT_HITBOX_WIDTH / 2D, PART_SLOT_HITBOX_HEIGHT / 2D, PART_SLOT_HITBOX_WIDTH / 2D, false);
                partSlotBoxes.put(newSlotBox, partDef);
            }
        }
        forceCollisionUpdateThisTick = true;
        if (this instanceof APart) {
            ((APart) this).masterEntity.forceCollisionUpdateThisTick = true;
        }
    }

    @Override
    protected void updateEncompassingBox() {
        super.updateEncompassingBox();
        //Set active collision box, door box, and interaction box lists to current boxes.
        allEntityCollisionBoxes.clear();
        allEntityCollisionBoxes.addAll(entityCollisionBoxes);
        allBlockCollisionBoxes.clear();
        allBlockCollisionBoxes.addAll(blockCollisionBoxes);
        allInteractionBoxes.clear();
        allInteractionBoxes.addAll(interactionBoxes);
        allBulletCollisionBoxes.clear();
        allBulletCollisionBoxes.addAll(bulletCollisionBoxes);
        allPartSlotBoxes.clear();
        allPartSlotBoxes.putAll(partSlotBoxes);

        //Only add active slots on clients, but all slots on servers.
        //Different clients may have different boxes active, but the server will always have them all.
        if (world.isClient()) {
            allInteractionBoxes.addAll(activePartSlotBoxes.keySet());
        } else {
            allInteractionBoxes.addAll(partSlotBoxes.keySet());
        }

        //Add all part boxes.
        for (APart part : parts) {
            allEntityCollisionBoxes.addAll(part.allEntityCollisionBoxes);
            allBlockCollisionBoxes.addAll(part.allBlockCollisionBoxes);
            allBulletCollisionBoxes.addAll(part.allBulletCollisionBoxes);
            allInteractionBoxes.addAll(part.allInteractionBoxes);
            allPartSlotBoxes.putAll(part.allPartSlotBoxes);
        }

        //Update encompassing bounding box to reflect all bounding boxes of all parts.
        if (!parts.isEmpty()) {
            for (APart part : parts) {
                if (!part.isFake()) {
                    encompassingBox.widthRadius = (float) Math.max(encompassingBox.widthRadius, Math.abs(part.encompassingBox.globalCenter.x - position.x) + part.encompassingBox.widthRadius);
                    encompassingBox.heightRadius = (float) Math.max(encompassingBox.heightRadius, Math.abs(part.encompassingBox.globalCenter.y - position.y) + part.encompassingBox.heightRadius);
                    encompassingBox.depthRadius = (float) Math.max(encompassingBox.depthRadius, Math.abs(part.encompassingBox.globalCenter.z - position.z) + part.encompassingBox.depthRadius);
                }
            }
        }

        //Also check active part slots, but only on the client.
        //Servers will just get packets to the box, but clients need to raytrace the slots.
        if (world.isClient() && !activePartSlotBoxes.isEmpty()) {
            for (BoundingBox box : activePartSlotBoxes.keySet()) {
                encompassingBox.widthRadius = (float) Math.max(encompassingBox.widthRadius, Math.abs(box.globalCenter.x - position.x) + box.widthRadius);
                encompassingBox.heightRadius = (float) Math.max(encompassingBox.heightRadius, Math.abs(box.globalCenter.y - position.y) + box.heightRadius);
                encompassingBox.depthRadius = (float) Math.max(encompassingBox.depthRadius, Math.abs(box.globalCenter.z - position.z) + box.depthRadius);
            }
        }
        encompassingBox.updateToEntity(this, null);
    }

    /**
     * Helper method to return the part at the specific index for the passed-in variable.
     * Returns null if the part doesn't exist.
     */
    public APart getSpecificPart(String variable, int partNumber) {
        //Iterate through our parts to find the index of the pack def for the part we want.
        String partType = variable.substring(0, variable.indexOf("_"));
        if (partType.equals("part")) {
            //Shortcut as we can just get the part for the slot.
            //Check index just in case someone screwed up a JSON.
            return partNumber < partsInSlots.size() ? partsInSlots.get(partNumber) : null;
        } else if (definition.parts != null) {
            for (int i = 0; i < definition.parts.size(); ++i) {
                JSONPartDefinition partDef = definition.parts.get(i);
                for (String defPartType : partDef.types) {
                    if (defPartType.startsWith(partType)) {
                        if (partNumber == 0) {
                            return partsInSlots.get(i);
                        } else {
                            --partNumber;
                        }
                        break;
                    }
                }
            }
        }

        //No valid sub-part definitions found.  This is an error, but not one we should crash for.  Return null.
        return null;
    }

    /**
     * Helper method to return the value of an animation for a specific part, as
     * determined by the index of that part.
     */
    public double getSpecificPartAnimation(String variable, int partNumber, float partialTicks) {
        APart foundPart = getSpecificPart(variable, partNumber);
        if (foundPart != null) {
            return foundPart.getRawVariableValue(variable.substring(0, variable.lastIndexOf("_")), partialTicks);
        } else {
            return 0;
        }
    }

    @Override
    public void renderBoundingBoxes(TransformationMatrix transform) {
        super.renderBoundingBoxes(transform);
        encompassingBox.renderWireframe(this, transform, null, ColorRGB.WHITE);
    }

    @Override
    protected void renderHolographicBoxes(TransformationMatrix transform) {
        if (!activePartSlotBoxes.isEmpty()) {
            //If we are holding a part or scanner, render the valid slots.
            world.beginProfiling("PartHoloboxes", true);
            IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
            AItemBase heldItem = player.getHeldItem();
            AItemPart heldPart = heldItem instanceof AItemPart ? (AItemPart) heldItem : null;
            boolean holdingScanner = player.isHoldingItemType(ItemComponentType.SCANNER);
            if (heldPart != null || holdingScanner) {
                if (holdingScanner) {
                    for (BoundingBox box : activePartSlotBoxes.keySet()) {
                        Point3D boxCenterDelta = box.globalCenter.copy().subtract(position);
                        box.renderHolographic(transform, boxCenterDelta, ColorRGB.BLUE);
                    }
                } else {
                    for (Entry<BoundingBox, JSONPartDefinition> partSlotEntry : activePartSlotBoxes.entrySet()) {
                        boolean isHoldingCorrectTypePart = false;
                        boolean isHoldingCorrectParamPart = false;

                        if (heldPart.isPartValidForPackDef(partSlotEntry.getValue(), subDefinition, false)) {
                            isHoldingCorrectTypePart = true;
                            if (heldPart.isPartValidForPackDef(partSlotEntry.getValue(), subDefinition, true)) {
                                isHoldingCorrectParamPart = true;
                            }
                        }

                        if (isHoldingCorrectTypePart) {
                            BoundingBox box = partSlotEntry.getKey();
                            Point3D boxCenterDelta = box.globalCenter.copy().subtract(position);
                            box.renderHolographic(transform, boxCenterDelta, isHoldingCorrectParamPart ? ColorRGB.GREEN : ColorRGB.RED);
                        }
                    }
                }
            }
            world.endProfiling();
        }
    }

    /**
     * Helper method used to get the controlling entity for this entity.
     * Is normally the player, but may be a NPC if one is in the seat.
     */
    public IWrapperEntity getController() {
        for (APart part : parts) {
            if (part.rider != null && part.placementDefinition.isController) {
                return part.rider;
            }
        }
        return null;
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        for (APart part : parts) {
            //Don't save the part if it's not valid or a fake part.
            if (part.isValid && !part.isFake()) {
                IWrapperNBT partData = part.save(InterfaceManager.coreInterface.getNewNBTWrapper());
                //We need to set some extra data here for the part to allow this entity to know where it went.
                //This only gets set here during saving/loading, and is NOT returned in the item that comes from the part.
                data.setData("part_" + part.placementSlot, partData);
            }
        }
        return data;
    }
}
