package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * This class is the base for all parts and should be extended for any entity-compatible parts.
 * Use {@link AEntityF_Multipart#addPart(APart, boolean)} to add parts
 * and {@link AEntityF_Multipart#removePart(APart, Iterator)} to remove them.
 * You may extend {@link AEntityF_Multipart} to get more functionality with those systems.
 * If you need to keep extra data ensure it is packed into whatever NBT is returned in item form.
 * This NBT will be fed into the constructor when creating this part, so expect it and ONLY look for it there.
 *
 * @author don_bruce
 */
public abstract class APart extends AEntityF_Multipart<JSONPart> {

    //JSON properties.
    public JSONPartDefinition placementDefinition;
    public final int placementSlot;

    //Instance properties.
    /**
     * The entity this part has been placed on,  can be a vehicle or a part.
     */
    public final AEntityF_Multipart<?> entityOn;
    /**
     * The top-most entity for this part.  May be the {@link #entityOn} if the part is only one level deep.
     */
    public final AEntityF_Multipart<?> masterEntity;
    /**
     * The vehicle this part has been placed on.  Identical to {@link #masterEntity}, just saves a cast.
     * Will be null, however, if this part isn't on a vehicle (say if it's on a decor).
     */
    public final EntityVehicleF_Physics vehicleOn;
    /**
     * The part this part is on, or null if it's on a base entity.
     */
    public final APart partOn;
    /**
     * All linked parts for this part.  Updated whenever the part set changes.
     */
    public final List<APart> linkedParts = new ArrayList<>();

    public boolean isInvisible = false;
    public boolean isActive = true;
    public boolean isInteractable = true;
    public boolean isPermanent = false;
    public boolean isMoveable;
    public final boolean turnsWithSteer;
    public final boolean isSpare;
    public final boolean isMirrored;
    /**
     * The local offset from this part, to the master entity.  This may not be the offset from the part to the entity it is
     * on if the entity is a part itself.
     */
    public final Point3D localOffset;
    public final RotationMatrix localOrientation;
    public final RotationMatrix zeroReferenceOrientation;
    public final RotationMatrix prevZeroReferenceOrientation;
    public final Point3D externalAnglesRotated = new Point3D();
    private AnimationSwitchbox placementMovementSwitchbox;
    private AnimationSwitchbox internalMovementSwitchbox;
    private static boolean checkingLinkedParts;

    public APart(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, IWrapperNBT data) {
        super(entityOn.world, placingPlayer, data);
        this.entityOn = entityOn;
        AEntityF_Multipart<?> parentEntity = entityOn;
        while (parentEntity instanceof APart) {
            parentEntity = ((APart) parentEntity).entityOn;
        }
        this.masterEntity = parentEntity;
        this.vehicleOn = parentEntity instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) parentEntity : null;
        this.partOn = entityOn instanceof APart ? (APart) entityOn : null;
        this.placementDefinition = placementDefinition;
        this.placementSlot = entityOn.definition.parts.indexOf(placementDefinition);

        this.localOffset = placementDefinition.pos.copy();
        this.localOrientation = new RotationMatrix();
        this.zeroReferenceOrientation = new RotationMatrix();
        this.prevZeroReferenceOrientation = new RotationMatrix();

        this.turnsWithSteer = placementDefinition.turnsWithSteer || (partOn != null && partOn.turnsWithSteer);
        this.isSpare = placementDefinition.isSpare || (partOn != null && partOn.isSpare);
        this.isMirrored = placementDefinition.isMirrored || (partOn != null && partOn.isMirrored);

        //Set initial position, rotation, and scale.  This ensures part doesn't "warp" the first tick.
        //Note that this isn't exact, as we can't calculate the exact locals until after the first tick
        //when we initialize all of our animations.
        position.set(localOffset).add(entityOn.position);
        prevPosition.set(position);
        orientation.set(entityOn.orientation);
        prevOrientation.set(orientation);
        scale.set(entityOn.scale);
        if (placementDefinition.partScale != null) {
            scale.multiply(placementDefinition.partScale);
        }
        prevScale.set(scale);
    }

    @Override
    protected void initializeAnimations() {
        super.initializeAnimations();
        isMoveable = false;
        if (placementDefinition.animations != null || placementDefinition.applyAfter != null) {
            List<JSONAnimationDefinition> animations = new ArrayList<>();
            if (placementDefinition.animations != null) {
                animations.addAll(placementDefinition.animations);
            }
            placementMovementSwitchbox = new AnimationSwitchbox(entityOn, animations, placementDefinition.applyAfter);
            isMoveable = true;
        }
        if (definition.generic.movementAnimations != null) {
            internalMovementSwitchbox = new AnimationSwitchbox(this, definition.generic.movementAnimations, null);
            isMoveable = true;
        }
    }

    @Override
    public void update() {
        super.update();
        isInvisible = false;

        //Update active state.
        isActive = partOn != null ? partOn.isActive : true;
        if (isActive) {
            isActive = checkConditions(placementDefinition.activeConditions, 0);
        }
        if (isActive) {
            isActive = checkConditions(definition.generic.activeConditions, 0);
        }
        if (!isActive && rider != null) {
            //Kick out rider from inactive seat.
            removeRider();
        }

        //Set initial offsets.
        motion.set(entityOn.motion);
        position.set(entityOn.position);
        orientation.set(entityOn.orientation);
        localOffset.set(placementDefinition.pos);
        
        //Update interactable state.
        isInteractable = (partOn == null || partOn.isInteractable) && entityOn.checkConditions(placementDefinition.interactableConditions, 0);

        //Update permanent-ness
        isPermanent = placementDefinition.isPermanent || (placementDefinition.lockingConditions != null && entityOn.checkConditions(placementDefinition.lockingConditions, 0));

        //Update zero-reference.
        prevZeroReferenceOrientation.set(zeroReferenceOrientation);
        if (partOn != null) {
            zeroReferenceOrientation.set(partOn.zeroReferenceOrientation);
        } else {
            zeroReferenceOrientation.set(entityOn.orientation);
        }
        if (placementDefinition.rot != null) {
            zeroReferenceOrientation.multiply(placementDefinition.rot);
        }

        //Init orientation.
        localOrientation.setToZero();

        //First apply part slot animation translation and rotation.
        //This will rotate us to our proper slot position.
        if (placementMovementSwitchbox != null) {
            isInvisible = !placementMovementSwitchbox.runSwitchbox(0, false);
            //Offset needs to move according to full transform.
            //This is because these coords are from what we are on.
            //Orientation just needs to update according to new rotation.
            localOffset.transform(placementMovementSwitchbox.netMatrix);
            localOrientation.multiply(placementMovementSwitchbox.rotation);
            externalAnglesRotated.set(placementMovementSwitchbox.rotation.convertToAngles());
        } else {
            externalAnglesRotated.set(0, 0, 0);
        }

        //Now rotate us to face the slot's requested orientation.
        if (placementDefinition.rot != null) {
            localOrientation.multiply(placementDefinition.rot);
        }

        //Now scale.  Needs to happen after placement operations to scale their transforms properly.
        scale.set(entityOn.scale);
        localOffset.multiply(scale);
        if (placementDefinition.partScale != null) {
            scale.multiply(placementDefinition.partScale);
        }

        //Finally, apply our internal translation and rotation.
        if (internalMovementSwitchbox != null) {
            isInvisible = !internalMovementSwitchbox.runSwitchbox(0, false) || isInvisible;
            //Offset here, to apply to locals, needs to be multiplied by scale and local orientation.
            //If we don't do this, then we won't calculate the locals right.
            localOffset.add(internalMovementSwitchbox.translation.multiply(scale).rotate(localOrientation));
            localOrientation.multiply(internalMovementSwitchbox.rotation);
        }

        //Set global position to reflect new local position.
        Point3D localPositionDelta = new Point3D().set(localOffset).rotate(orientation);
        position.add(localPositionDelta);
        orientation.multiply(localOrientation);

        //Adjust localOffset to align with actual local offset.  This happens if we are a sub-part.
        if (partOn != null) {
            localOffset.reOrigin(partOn.localOrientation).add(partOn.localOffset);
        }

        //Update bounding box, as scale changes width/height.
        boundingBox.widthRadius = getWidth() / 2D * scale.x;
        boundingBox.heightRadius = getHeight() / 2D * scale.y;
        boundingBox.depthRadius = getWidth() / 2D * scale.z;
    }

    @Override
    public boolean shouldAutomaticallyUpdate() {
        //Parts are always updated by their parent, not the main update calls.
        return false;
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return super.requiresDeltaUpdates() || entityOn.requiresDeltaUpdates() || isMoveable;
    }

    @Override
    protected void updateCollisionBoxes() {
        //Add collision if we aren't a fake part.
        if (!isFake()) {
            super.updateCollisionBoxes();
            interactionBoxes.add(boundingBox);
        }
    }

    @Override
    protected void updateEncompassingBox() {
        super.updateEncompassingBox();

        //Don't add our interaction boxes to the box list if we aren't active and on the client.
        //Servers need all of these since we might be active for some players and not others.
        if (world.isClient() && !canBeClicked()) {
            allInteractionBoxes.removeAll(interactionBoxes);
            return;
        }

        //If we are holding a screwdriver or wrench, run these checks to remove hitboxes if needed. This can only be done on the client.
        if (world.isClient()) {
	    	boolean isHoldingWrench = InterfaceManager.clientInterface.getClientPlayer().isHoldingItemType(ItemComponentType.WRENCH);
	    	boolean isHoldingScrewdriver = InterfaceManager.clientInterface.getClientPlayer().isHoldingItemType(ItemComponentType.SCREWDRIVER);
	
	        if (isHoldingWrench || isHoldingScrewdriver) {
	            //If we are holding a wrench and the part requires a screwdriver, remove interaction boxes so they don't get in the way and vice versa.
	            if ((isHoldingWrench && definition.generic.mustBeRemovedByScrewdriver) || (isHoldingScrewdriver && !definition.generic.mustBeRemovedByScrewdriver)) {
	                allInteractionBoxes.removeAll(interactionBoxes);
	                return;
	            }
	            //If we are holding a wrench or screwdriver, and the part has children, don't add the interaction boxes.  We can't wrench those parts.
	            //The only exceptions are parts that have permanent-default parts on them. or if they specifically don't block subpart removal.  These can be removed.
	            //Again, this only applies on clients for that client player.
	        	for (APart childPart : parts) {
	                if (!childPart.isPermanent && !childPart.placementDefinition.allowParentRemoval) {
	                    allInteractionBoxes.removeAll(interactionBoxes);
	                    return;
	                }
	            }
	        }
        }
    }

    @Override
    public void attack(Damage damage) {
        //Check if we can be removed by this attack.
        if (!isPermanent && definition.generic.canBeRemovedByHand && damage.isHand) {
            //Attacked a removable part, remove us to the player's inventory.
            //If the inventory can't fit us, don't remove us.
            IWrapperPlayer player = (IWrapperPlayer) damage.entityResponsible;
            if (masterEntity.locked) {
                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_VEHICLE_LOCKED));
            } else {
                if (player.getInventory().addStack(getItem().getNewStack(save(InterfaceManager.coreInterface.getNewNBTWrapper())))) {
                    entityOn.removePart(this, null);
                }
            }
        } else {
            //Not a removable part, or is an actual attack.
            super.attack(damage);
            if (outOfHealth && definition.generic.destroyable) {
                if (ConfigSystem.settings.damage.explosions.value) {
                    world.spawnExplosion(position, 1F, true);
                } else {
                    world.spawnExplosion(position, 0F, false);
                }
                destroy(damage.box);
            }
        }
    }

    @Override
    public void setVariable(String variable, double value) {
        if (variable.startsWith("parent_")) {
            entityOn.setVariable(variable.substring("parent_".length()), value);
        } else {
            super.setVariable(variable, value);
        }
    }

    @Override
    public void updatePartList() {
        super.updatePartList();
        linkedParts.clear();
        addLinkedPartsToList(linkedParts, APart.class);
    }

    @Override
    public PlayerOwnerState getOwnerState(IWrapperPlayer player) {
        return entityOn.getOwnerState(player);
    }

    @Override
    public double getMass() {
        return definition.generic.mass;
    }

    @Override
    public boolean shouldSavePosition() {
        return false;
    }

    @Override
    public boolean canBeClicked() {
        return isInteractable;
    }

    /**
     * Updates the tone of the part to its appropriate type.
     * If the part can't match the tone of this vehicle, then it is not modified.
     */
    public void updateTone(boolean recursive) {
        if (placementDefinition.toneIndex != 0) {
            if (entityOn.subDefinition.partTones != null && entityOn.subDefinition.partTones.size() >= placementDefinition.toneIndex) {
                String partTone = entityOn.subDefinition.partTones.get(placementDefinition.toneIndex - 1);
                for (JSONSubDefinition subDefinition : definition.definitions) {
                    if (subDefinition.subName.equals(partTone)) {
                        updateSubDefinition(partTone);
                        return;
                    }
                }
            }
        }

        if (recursive && !parts.isEmpty()) {
            for (APart part : parts) {
                part.updateTone(true);
            }
        }
    }

    /**
     * Adds all linked parts to the passed-in list.  This method is semi-recursive.  If a part is
     * in a linked slot, and it matches the class, then that part is added and the next slot is checked.
     * If the part doesn't match, then all child parts of that part are checked to see if they match.
     * This is done irrespective of the slot match on the sub-part, but will respect the part class.
     * This is done because wheels and other parts will frequently be attached to other parts in specific
     * slots, such as custom axles or gun mounting hard-points.  This method will also check in reverse, in
     * that if a part is linked to the slot of this part, then it will act as if this part had a linking to
     * the slot of the other part, provided the class matches the passed-in class.  Note that for all cases,
     * the JSON values are 1-indexed, whereas the map is 0-indexed.
     */
    public <PartClass extends APart> void addLinkedPartsToList(List<PartClass> partList, Class<PartClass> partClass) {
        //Check for parts we are linked to.
        if (placementDefinition.linkedParts != null) {
            for (int partIndex : placementDefinition.linkedParts) {
                APart partAtIndex = entityOn.partsInSlots.get(partIndex - 1);
                if (partClass.isInstance(partAtIndex)) {
                    partList.add(partClass.cast(partAtIndex));
                }
                if (partAtIndex != null) {
                    for (APart part : partAtIndex.allParts) {
                        if (partClass.isInstance(part)) {
                            partList.add(partClass.cast(part));
                        }
                    }
                }
            }
        }

        //If we are defined to always link to anything, get our master entity and all its parts and link them.
        if (definition.generic.forceAllLinks) {
            for (APart part : masterEntity.allParts) {
                if (part != this && partClass.isInstance(part) && !partList.contains(part)) {
                    partList.add(partClass.cast(part));
                }
            }
        }

        //Now check for parts linked to us.
        for (APart part : entityOn.parts) {
            if (part != this) {
                if (part.placementDefinition.linkedParts != null) {
                    for (int partIndex : part.placementDefinition.linkedParts) {
                        if (partIndex - 1 == this.placementSlot) {
                            if (partClass.isInstance(part)) {
                                //Part class matches, add it as linked.
                                partList.add(partClass.cast(part));
                            } else {
                                //Index matches, but not class, add all sub-parts that match (probably a generic part).
                                for (APart part2 : part.allParts) {
                                    if (partClass.isInstance(part2)) {
                                        partList.add(partClass.cast(part2));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //If any part is defined to always link to anything, and it could link to us, link it.
        for (APart part : masterEntity.allParts) {
            if (part != this && part.definition.generic.forceAllLinks && partClass.isInstance(part) && !partList.contains(part)) {
                partList.add(partClass.cast(part));
            }
        }

        //Add all parts our parent is linked to, if we have one that's a part.
        //This allows parts multiple levels deep to query their parent.
        //We know that our parent will have their lists correct at this point as
        //they update them first, then update us.
        if (partOn != null) {
            for (APart part : partOn.linkedParts) {
                if (partClass.isInstance(part)) {
                    partList.add(partClass.cast(part));
                }
            }
        }
    }

    /**
     * Returns true if this part is in liquid.
     */
    public boolean isInLiquid() {
        return world.isBlockLiquid(position);
    }

    /**
     * Checks if this part can be removed with a wrench.  If so, then null is returned.
     * If not, a {@link LanguageEntry} is returned with the message of why it cannot be.
     */
    public LanguageEntry checkForRemoval() {
        return null;
    }

    /**
     * This is called during part save/load calls.  Fakes parts are
     * added to entities, but they aren't saved with the NBT.  Rather,
     * they should be re-created in the constructor of the part that added
     * them in the first place.
     */
    public boolean isFake() {
        return false;
    }

    public double getWidth() {
        return definition.generic.width != 0 ? definition.generic.width : 0.75F;
    }

    public double getHeight() {
        return definition.generic.height != 0 ? definition.generic.height : 0.75F;
    }

    //--------------------START OF SOUND AND ANIMATION CODE--------------------
    @Override
    public float getLightProvided() {
        return entityOn.getLightProvided();
    }

    @Override
    public boolean shouldRenderBeams() {
        return entityOn.shouldRenderBeams();
    }

    @Override
    public String getTexture() {
        if (definition.generic.useVehicleTexture) {
            if (vehicleOn != null) {
                return vehicleOn.getTexture();
            } else if (definition.generic.benchTexture != null) {
                return PackResourceLoader.getPackResource(definition, ResourceType.PNG, definition.generic.benchTexture);
            } else {
                return null;
            }
        } else {
            return super.getTexture();
        }
    }

    @Override
    public boolean renderTextLit() {
        return entityOn.renderTextLit();
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        //If the variable is prefixed with "parent_", then we need to get our parent's value.
        if (variable.startsWith("parent_")) {
            return entityOn.getRawVariableValue(variable.substring("parent_".length()), partialTicks);
        } else if (definition.parts != null) {
            //Check sub-parts for the part with the specified index.
            int partNumber = getVariableNumber(variable);
            if (partNumber != -1) {
                return getSpecificPartAnimation(variable, partNumber, partialTicks);
            }
        }

        //Check for generic part variables.
        switch (variable) {
            case ("part_present"):
                return 1;
            case ("part_ismirrored"):
                return isMirrored ? 1 : 0;
            case ("part_isspare"):
                return isSpare ? 1 : 0;
            case ("part_onvehicle"):
                return vehicleOn != null ? 1 : 0;
        }

        //No variables, check super variables before doing generic forwarding.
        //We need this here for position-specific values, as some
        //super variables care about position, so we can't forward those.
        double value = super.getRawVariableValue(variable, partialTicks);
        if (!Double.isNaN(value)) {
            return value;
        }

        //If we are down here, we must have not found a part variable.
        //First check all linked parts in case we want one of theirs.
        if (!linkedParts.isEmpty() && !checkingLinkedParts) {
            checkingLinkedParts = true;
            for (APart part : linkedParts) {
                value = part.getRawVariableValue(variable, partialTicks);
                if (!Double.isNaN(value)) {
                    checkingLinkedParts = false;
                    return value;
                }
            }
            checkingLinkedParts = false;
        }

        //Not a linked part variable.
        //Try to get the parent variable, and return whatever we get, NaN or otherwise.
        return entityOn.getRawVariableValue(variable, partialTicks);
    }

    @Override
    public int getWorldLightValue() {
        //Use master for lighting consistency.
        return masterEntity.worldLightValue;
    }

    @Override
    public boolean disableRendering(float partialTicks) {
        return super.disableRendering(partialTicks) || isFake() || isInvisible;
    }

    @Override
    public void renderBoundingBoxes(TransformationMatrix transform) {
        if (canBeClicked()) {
            super.renderBoundingBoxes(transform);
        }
    }
}
