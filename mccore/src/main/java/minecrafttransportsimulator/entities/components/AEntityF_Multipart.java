package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.EntityBullet.HitType;
import minecrafttransportsimulator.entities.instances.EntityPlacedPart;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.jsondefs.AJSONPartProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitCollision;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitEntity;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitGeneric;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketPartChange_Add;
import minecrafttransportsimulator.packets.instances.PacketPartChange_Remove;
import minecrafttransportsimulator.packets.instances.PacketPartChange_Transfer;
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
     * List of damage boxes, plus all part boxes included.
     **/
    public final List<BoundingBox> allDamageCollisionBoxes = new ArrayList<>();

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
    private static final float PART_SLOT_HITBOX_WIDTH = 0.75F;
    private static final float PART_SLOT_HITBOX_HEIGHT = 2.25F;
    private static final Point3D PART_TRANSFER_GROWTH = new Point3D(16, 16, 16);

    private APart partToPlace;
    private EntityPlacedPart placedPart;
    private int placeTimer;

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

        if (partToPlace != null && --placeTimer == 0) {
            JSONPartDefinition otherPartDef = placedPart.definition.parts.get(0);
            partToPlace.linkToEntity(placedPart, otherPartDef);
            placedPart.addPart(partToPlace, false);
            InterfaceManager.packetInterface.sendToAllClients(new PacketPartChange_Transfer(partToPlace, placedPart, otherPartDef));
            partToPlace = null;
            placedPart = null;
        }

        //Populate active part slot list and update box positions.
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

            //Update part slot box positions.
            if (requiresDeltaUpdates()) {
                world.beginProfiling("PartSlotPositions", false);
                activePartSlotBoxes.forEach((box, partDef) -> {
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
        }

        //Check for part slot variable changes and do logic.
        if (!world.isClient() && definition.parts != null) {
            for (int i = 0; i < definition.parts.size(); ++i) {
                JSONPartDefinition partDef = definition.parts.get(i);
                if (partDef.transferVariable != null) {
                    if (isVariableActive(partDef.transferVariable)) {
                        transferPart(partDef);
                        toggleVariable(partDef.transferVariable);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableToggle(this, partDef.transferVariable));
                    }
                }
            }
        }
        world.endProfiling();
    }

    private void transferPart(JSONPartDefinition partDef) {
        int ourSlotIndex = definition.parts.indexOf(partDef);
        APart currentPart = partsInSlots.get(ourSlotIndex);
        AEntityF_Multipart<?> masterEntity = this instanceof APart ? ((APart) this).masterEntity : this;

        if (currentPart == null) {
            //False->True change, try to grab a part.
            Point3D partAnchor = partDef.pos.copy().rotate(orientation).add(position);
            for (APart partToTransfer : world.getEntitiesExtendingType(APart.class)) {
                if (partToTransfer.definition.generic.canBePlacedOnGround && partToTransfer.masterEntity != masterEntity && partToTransfer.position.isDistanceToCloserThan(partAnchor, 2) && ((AItemPart) partToTransfer.getStack().getItem()).isPartValidForPackDef(partDef, this.subDefinition, true)) {
                    partToTransfer.entityOn.removePart(partToTransfer, false, null);
                    partToTransfer.linkToEntity(this, partDef);
                    addPart(partToTransfer, false);
                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartChange_Transfer(partToTransfer, this, partDef));
                    return;
                }
            }
        } else {
            //True-False change, place part in nearby slot or drop.
            //Double-check the part can be dropped, in case someone manually put a part in the slot.
            if (currentPart.definition.generic.canBePlacedOnGround) {
                Point3D partAnchor = new Point3D();
                AItemPart currentPartItem = (AItemPart) currentPart.getStack().getItem();
                for (AEntityF_Multipart<?> entity : world.getEntitiesExtendingType(AEntityF_Multipart.class)) {
                    //This keeps us from checking things really far away for no reason.
                    if (entity.encompassingBox.isPointInside(currentPart.position, PART_TRANSFER_GROWTH)) {
                        AEntityF_Multipart<?> otherMasterEntity = entity instanceof APart ? ((APart) entity).masterEntity : entity;
                        if(otherMasterEntity != masterEntity && entity.definition.parts != null) {
                            for (JSONPartDefinition otherPartDef : entity.definition.parts) {
                                partAnchor.set(otherPartDef.pos).rotate(entity.orientation).add(entity.position);
                                if (partAnchor.isDistanceToCloserThan(currentPart.position, 2) && currentPartItem.isPartValidForPackDef(otherPartDef, entity.subDefinition, true)) {
                                    removePart(currentPart, false, null);
                                    currentPart.linkToEntity(entity, otherPartDef);
                                    entity.addPart(currentPart, false);
                                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartChange_Transfer(currentPart, entity, otherPartDef));
                                    return;
                                }
                            }
                        }
                    }
                }

                //Remove the part from ourselves.
                removePart(currentPart, false, null);

                //Align part to world grid.
                currentPart.position.x = Math.floor(currentPart.position.x) + 0.5;
                currentPart.position.y = Math.floor(currentPart.position.y);
                currentPart.position.z = Math.floor(currentPart.position.z) + 0.5;
                currentPart.orientation.setToAngles(new Point3D(0, Math.round((currentPart.orientation.convertToAngles().y + 360) / 90) * 90 % 360, 0));

                //Create new placed part entity, align to part, add part, and spawn.
                IWrapperNBT placerData = InterfaceManager.coreInterface.getNewNBTWrapper();
                EntityPlacedPart entity = new EntityPlacedPart(world, null, placerData);
                entity.addPartsPostAddition(null, placerData);

                entity.position.set(currentPart.position);
                entity.prevPosition.set(entity.position);
                entity.orientation.set(currentPart.orientation);
                entity.prevOrientation.set(entity.orientation);
                entity.world.spawnEntity(entity);

                //TODO we would normally transfer the part here, but we can't do that since MC does jank.  Remove with EOBeta completion.  This forces the placed part to spawn before we try and add the part.
                partToPlace = currentPart;
                placedPart = entity;
                placeTimer = 20;
            }
        }
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
        if (damage.box != null) {
            APart hitPart = getPartWithBox(damage.box);
            if (hitPart != null && hitPart.isValid) {
                hitPart.attack(damage);
                return;
            }
        }
        super.attack(damage);
    }

    /**
     * Called to get all hitboxes hit by the passed-in projectile.  No processing is done at this time, this only lets
     * the bullet know if it WILL hit this entity.  Returns null if no boxes collided, not an empty collection.
     */
    public Collection<BoundingBox> getHitBoxes(Point3D pathStart, Point3D pathEnd, BoundingBox movementBounds) {
        if (encompassingBox.intersects(movementBounds)) {
            //Get all collision boxes and check if we hit any of them.
            //Sort them by distance for later.
            TreeMap<Double, BoundingBox> hitBoxes = new TreeMap<>();
            for (BoundingBox box : allDamageCollisionBoxes) {
                if (!allPartSlotBoxes.containsKey(box)) {
                    Point3D delta = box.getIntersectionPoint(pathStart, pathEnd);
                    if (delta != null) {
                        hitBoxes.put(delta.distanceTo(pathStart), box);
                    }
                }
            }
            if (!hitBoxes.isEmpty()) {
                return hitBoxes.values();
            }
        }
        return null;
    }

    /**
     * Called when the entity is attacked by a projectile.  Returns a {@link EntityBullet.HitType} if the projectile hit something
     * and should be removed from the world.  Null if it can keep going.  Note that returning false does
     * NOT imply no damage was applied: some entities/parts allow for projectiles to damage and pass through them.
     * Also note that unlike {@link #attack(Damage)}, this method functions both on client and servers, though you must only
     * call it on a single client in a group or on the server.  Calling it on every client will result in duplicate attacks.
     */
    public EntityBullet.HitType attackProjectile(Damage damage, Collection<BoundingBox> hitBoxes) {
            //Check all boxes for armor and see if we penetrated them.
        for (BoundingBox hitBox : hitBoxes) {
            APart hitPart = getPartWithBox(hitBox);
            AEntityF_Multipart<?> hitEntity = hitPart != null ? hitPart : this;
            EntityBullet bullet = damage.damgeSource instanceof PartGun ? ((PartGun) damage.damgeSource).currentBullet : null;

            //First check if we need to reduce health of the hitbox.
            boolean hitOperationalHitbox = false;
            if (hitBox.groupDef != null && hitBox.groupDef.health != 0 && !damage.isWater) {
                String variableName = "collision_" + (hitEntity.definition.collisionGroups.indexOf(hitBox.groupDef) + 1) + "_damage";
                double currentDamage = hitEntity.getVariable(variableName);
                if (bullet != null) {
                    bullet.displayDebugMessage("HIT HEALTH BOX.  BOX CURRENT DAMAGE: " + currentDamage + " OF " + hitBox.groupDef.health + "  ATTACKED FOR: " + damage.amount);
                }

                //This is a server-only action that does NOT cause us to stop processing.
                //Send off packet to damage the health hitbox (or damage directly on server) and continue as if we didn't hit anything.
                if (world.isClient()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitCollision(hitEntity, hitBox, damage.amount));
                } else {
                    hitEntity.damageCollisionBox(hitBox, damage.amount);
                }
                hitOperationalHitbox = true;
            }

            //Check armor pen and see if we hit too much and need to stop processing.
            if (hitBox.definition != null && (hitBox.definition.armorThickness != 0 || hitBox.definition.heatArmorThickness != 0)) {
                hitOperationalHitbox = true;
                if (bullet != null) {
                    double armorThickness = hitBox.definition != null ? (bullet.definition.bullet.isHeat && hitBox.definition.heatArmorThickness != 0 ? hitBox.definition.heatArmorThickness : hitBox.definition.armorThickness) : 0;
                    double penetrationPotential = bullet.definition.bullet.isHeat ? bullet.definition.bullet.armorPenetration : (bullet.definition.bullet.armorPenetration * bullet.velocity / bullet.initialVelocity);
                    bullet.armorPenetrated += armorThickness;
                    bullet.displayDebugMessage("HIT ARMOR OF: " + (int) armorThickness);

                    if (bullet.armorPenetrated > penetrationPotential) {
                        //Bullet hit too much armor.
                        if (world.isClient()) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitGeneric(bullet.gun, bullet.bulletNumber, hitBox.globalCenter, HitType.ARMOR));
                            bullet.waitingOnActionPacket = true;
                        } else {
                            EntityBullet.performGenericHitLogic(bullet.gun, bullet.bulletNumber, hitBox.globalCenter, HitType.ARMOR);
                        }
                        bullet.displayDebugMessage("HIT TOO MUCH ARMOR.  MAX PEN: " + (int) penetrationPotential);
                        return EntityBullet.HitType.ARMOR;
                    }
                } else {
                    //Not a bullet, but hit armor, 100% stopping power with no damage.
                    return EntityBullet.HitType.ARMOR;
                }
            }

            //Don't apply damage if we already damaged a health box.
            if (!hitOperationalHitbox) {
                if (bullet != null) {
                    //Didn't hit health or armor, must have hit something we can damage.
                    //Need to re-create damage object to reference this hitbox.
                    //Remove bullet if we are applying damage to a core group, or a part that forwards damage.
                    damage = new Damage(bullet.gun, hitBox, damage.amount);
                    boolean applyDamage = ((hitBox.groupDef != null && (hitBox.groupDef.health == 0 || damage.isWater)) || hitPart != null);
                    boolean removeAfterDamage = applyDamage && (hitPart == null || hitPart.definition.generic.forwardsDamageMultiplier > 0);

                    bullet.displayDebugMessage("HIT ENTITY BOX FOR DAMAGE: " + (int) damage.amount + " DAMAGE WAS AT " + (int) hitEntity.damageAmount);
                    if (world.isClient()) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitEntity(bullet.gun, hitEntity, damage));
                        if (removeAfterDamage) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitGeneric(bullet.gun, bullet.bulletNumber, damage.box.globalCenter, HitType.VEHICLE));
                            bullet.waitingOnActionPacket = true;
                            return EntityBullet.HitType.VEHICLE;
                        }
                    } else {
                        EntityBullet.performEntityHitLogic(hitEntity, damage);
                        if (removeAfterDamage) {
                            EntityBullet.performGenericHitLogic(bullet.gun, bullet.bulletNumber, damage.box.globalCenter, HitType.VEHICLE);
                            return EntityBullet.HitType.VEHICLE;
                        }
                    }
                } else {
                    //Not a bullet, just attack directly.
                    damage = new Damage(damage.amount, damage.box, damage.damgeSource, damage.entityResponsible, damage.language);
                    hitEntity.attack(damage);
                    return EntityBullet.HitType.VEHICLE;
                }
            }
        }
        return null;
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
    public IWrapperItemStack getStack() {
        //Add data to the stack we return.  We need to remember the parts we have on us, even in item form.
        IWrapperItemStack stack = super.getStack();
        stack.setData(save(InterfaceManager.coreInterface.getNewNBTWrapper()));
        return stack;
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
    public Collection<BoundingBox> getDamageBoxes() {
        return allDamageCollisionBoxes;
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
                //Part was removed during updates, remove from the part listing.
                removePart(part, true, iterator);
            } else {
                part.doPostUpdateLogic();
            }
        }
        world.endProfiling();
        super.doPostUpdateLogic();
    }

    @Override
    public void damageCollisionBox(BoundingBox box, double damageAmount) {
        APart part = getPartWithBox(box);
        if (part != null) {
            part.damageCollisionBox(box, damageAmount);
        } else {
            super.damageCollisionBox(box, damageAmount);
        }
    }

    @Override
    protected void updateCollisionBoxes() {
        super.updateCollisionBoxes();
        //Only add active slots on clients.
        //Different clients may have different boxes active, but the server will always have them all.
        if (world.isClient()) {
            interactionBoxes.addAll(activePartSlotBoxes.keySet());
            damageCollisionBoxes.addAll(activePartSlotBoxes.keySet());
        }
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
     * Helper method to get the index of the passed-in variable.  Indexes are defined by
     * variable names ending in _xx, where xx is a number.  The defined number is assumed
     * to be 1-indexed, but the returned number will be 0-indexed.  If the variable doesn't
     * define a number, then -1 is returned.
     */
    public static int getVariableNumber(String variable) {
        if (variable.matches("^.*_\\d+$")) {
            return Integer.parseInt(variable.substring(variable.lastIndexOf('_') + 1)) - 1;
        } else {
            return -1;
        }
    }

    /**
     * Returns true if this entity can be clicked.  Normally true, but can be false to block
     * click-able hitboxes from showing up.  This function should only be called on CLIENTs.
     */
    public boolean canBeClicked() {
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
                        addPartFromStack(PackParser.getItem(partData.getString("packID"), partData.getString("systemName"), partData.getString("subName")).getNewStack(partData), placingPlayer, i, true);
                    }
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("Could not load part from NBT.  Did you un-install a pack?");
                    e.printStackTrace();
                }

                //Add default parts.  We need to do this after we actually create this part so its slots are valid.
                //We also need to know if it is a new part or not, since that allows non-permanent default parts to be added.
                JSONPartDefinition partDef = definition.parts.get(i);
                if (newEntity) {
                    //Add constants. This is also done in initializeAnimations, but repeating it here ensures 
                    //the value will be set before spawning in any conditional parts.
                    if (definition.constantValues != null) {
                        variables.putAll(definition.constantValues);
                    }
                    if (partDef.conditionalDefaultParts != null) {
                        for (Entry<String, String> conditionalDef : partDef.conditionalDefaultParts.entrySet()) {
                            if (getCleanRawVariableValue(conditionalDef.getKey(), 0) > 0) {
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
     * if the item-based part can go to this entity (if it's not bypassed).  If so, it is constructed and added,
     * and a packet is sent to all clients to inform them of this change.
     * This method returns the part if it was added, null if it wasn't.
     */
    public APart addPartFromStack(IWrapperItemStack stack, IWrapperPlayer playerAdding, int slotIndex, boolean bypassSlotChecks) {
        JSONPartDefinition newPartDef = definition.parts.get(slotIndex);
        AItemPart partItem = (AItemPart) stack.getItem();
        if (partsInSlots.get(slotIndex) == null && (bypassSlotChecks || partItem.isPartValidForPackDef(newPartDef, subDefinition, !newPartDef.bypassSlotMinMax))) {
            //Part is not already present, and is valid, add it.
            IWrapperNBT partData = stack.getData();
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
     * and lists or maps that may have changed from adding the part.  Sending a packet here
     * will create the part on clients, which you will always want to do unless you are transferring a part.
     */
    public void addPart(APart part, boolean sendPacket) {
        parts.add(part);
        if (!part.isFake()) {
            partsInSlots.set(part.placementSlot, part);

            //Recalculate slots.
            recalculatePartSlots();

            //If we are on the server, and need to notify clients, do so.
            if (sendPacket && !world.isClient()) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketPartChange_Add(this, part));
            }
        }

        //Add the part to the world, if it doesn't exist already.
        if (part.ticksExisted == 0) {
            world.addEntity(part);
        }

        //Let parts know a change was made.
        part.masterEntity.updateAllpartList();
        part.masterEntity.updatePartList();
    }

    /**
     * Removes the passed-in part from the entity.  Calls the part's {@link APart#remove()} method to
     * let the part handle removal code.  Iterator is optional, but if you're in any code block that
     * is iterating over the parts list, and you don't pass that iterator in, you'll get a CME.
     */
    public void removePart(APart part, boolean removeFromWorld, Iterator<APart> iterator) {
        if (parts.contains(part)) {
            if (removeFromWorld) {
                //Call the part's removal code for it to process.
                part.remove();
            }

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
            }

            //If we are on the server, notify all clients of this change.
            if (!world.isClient()) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketPartChange_Remove(part, removeFromWorld));
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

        //Clear camera list in prep for new entries from other areas.
        cameras.clear();
        cameraEntities.clear();
    }

    /**
     * Gets the part that has the passed-in bounding box.
     * Useful if we interacted with a box on this multipart and need
     * to know exactly what it went to.
     */
    public APart getPartWithBox(BoundingBox box) {
        for (APart part : parts) {
            if (part.allDamageCollisionBoxes.contains(box)) {
                if (part.damageCollisionBoxes.contains(box)) {
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
                    APart addedPart = addPartFromStack(PackParser.getItem(partPackID, partSystemName).getNewStack(null), playerAdding, partSlot, false);
	                if (addedPart != null) {
	                    //Set the default tone for the part, if it requests one and we can provide one.
	                    addedPart.updateTone(false);
	                }
	            } catch (NullPointerException e) {
	            	playerAdding.sendPacket(new PacketPlayerChatMessage(playerAdding, JSONConfigLanguage.SYSTEM_DEBUG, "Attempted to add defaultPart: " + partPackID + ":" + partSystemName + " to: " + providingDef.packID + ":" + providingDef.systemName + " but that part doesn't exist in the pack item registry."));
	            }
	        } catch (IndexOutOfBoundsException e) {
	        	playerAdding.sendPacket(new PacketPlayerChatMessage(playerAdding, JSONConfigLanguage.SYSTEM_DEBUG, "Could not parse defaultPart definition: " + partName + ".  Format should be \"packId:partName\""));
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
        allDamageCollisionBoxes.clear();
        allDamageCollisionBoxes.addAll(damageCollisionBoxes);
        allPartSlotBoxes.clear();
        allPartSlotBoxes.putAll(partSlotBoxes);

        //Add all part boxes.
        for (APart part : parts) {
            allEntityCollisionBoxes.addAll(part.allEntityCollisionBoxes);
            allBlockCollisionBoxes.addAll(part.allBlockCollisionBoxes);
            allBulletCollisionBoxes.addAll(part.allBulletCollisionBoxes);
            allInteractionBoxes.addAll(part.allInteractionBoxes);
            allDamageCollisionBoxes.addAll(part.allDamageCollisionBoxes);
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
