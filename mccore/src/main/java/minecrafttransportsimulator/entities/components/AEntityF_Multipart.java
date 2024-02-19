package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.BoundingBoxHitResult;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.EntityBullet.HitType;
import minecrafttransportsimulator.entities.instances.EntityPlacedPart;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.jsondefs.AJSONPartProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitCollision;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitEntity;
import minecrafttransportsimulator.packets.instances.PacketEntityBulletHitGeneric;
import minecrafttransportsimulator.packets.instances.PacketPartChange_Add;
import minecrafttransportsimulator.packets.instances.PacketPartChange_Remove;
import minecrafttransportsimulator.packets.instances.PacketPartChange_Transfer;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.LanguageSystem;

/**
 * Base class for multipart entities.  These entities hold other, part-based entities.  These part
 * entities may be added or removed from this entity based on the implementation, but assurances
 * are made with how they are stored and how they are accessed.
 *
 * @author don_bruce
 */
public abstract class AEntityF_Multipart<JSONDefinition extends AJSONPartProvider> extends AEntityE_Interactable<JSONDefinition> {

    /**
     * List of collision boxes, with all part collision boxes included.
     **/
    public final Set<BoundingBox> allCollisionBoxes = new HashSet<>();

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
     * Map of part slot boxes.  Key is the box, value is the definition for that slot.
     * Note that this contains all POSSIBLE boxes.  Boxes may not be active and in
     * {@link #allCollisionBoxes} if their conditions aren't right for clicking.
     **/
    public final Map<BoundingBox, JSONPartDefinition> partSlotBoxes = new HashMap<>();
    public final Map<BoundingBox, JSONPartDefinition> activeClientPartSlotBoxes = new HashMap<>();
    private final Map<JSONPartDefinition, AnimationSwitchbox> partSlotSwitchboxes = new HashMap<>();

    //Constants
    private static final float PART_SLOT_NORMAL_HITBOX_WIDTH = 0.5F;
    private static final float PART_SLOT_NORMAL_HITBOX_HEIGHT = 0.5F;
    private static final float PART_SLOT_LARGE_HITBOX_WIDTH = 0.75F;
    private static final float PART_SLOT_LARGE_HITBOX_HEIGHT = 2.25F;
    private static final Point3D PART_TRANSFER_GROWTH = new Point3D(16, 16, 16);
    private static final Set<CollisionType> partSlotBoxCollisionTypes = new HashSet<>(Arrays.asList(CollisionType.CLICK));

    private APart partToPlace;
    private EntityPlacedPart placedPart;
    private int placeTimer;

    public AEntityF_Multipart(AWrapperWorld world, IWrapperPlayer placingPlayer, AItemSubTyped<JSONDefinition> item, IWrapperNBT data) {
        super(world, placingPlayer, item, data);
        if (data == null) {
            //Add constants. This is also done in initializeAnimations, but repeating it here ensures 
            //the value will be set before any subsequent logic occurs.
            if (definition.constantValues != null) {
                definition.constantValues.forEach((constantKey, constantValue) -> this.getVariable(constantKey).setTo(constantValue, false));
            }
        }

        //Init part slots.
        if (definition.parts != null) {
            while (partsInSlots.size() < definition.parts.size()) {
                partsInSlots.add(null);
            }
        }
    }

    @Override
    public void initializeAnimations() {
        super.initializeAnimations();
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
            recalculatePartSlots();
        }

        //Update the part list, this will re-create any linkings on us.
        updatePartList();
    }

    @Override
    public void update() {
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

        //Check for part slot variable changes and do logic.
        if (!world.isClient() && definition.parts != null) {
            for (int i = 0; i < definition.parts.size(); ++i) {
                JSONPartDefinition partDef = definition.parts.get(i);
                if (partDef.transferVariable != null) {
                    ComputedVariable variable = getVariable(partDef.transferVariable);
                    if (variable.isActive) {
                        transferPart(partDef);
                        variable.toggle(true);
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
                if (partToTransfer.definition.generic.canBePlacedOnGround && partToTransfer.masterEntity != masterEntity && partToTransfer.position.isDistanceToCloserThan(partAnchor, 2) && ((AItemPart) partToTransfer.cachedItem).isPartValidForPackDef(partDef, this.subDefinition, true)) {
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
                AItemPart currentPartItem = (AItemPart) currentPart.cachedItem;
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

                //Need to defer the  adding of this part to give time for the main entity to spawn and transfer to the clients.
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
    public Collection<BoundingBoxHitResult> getHitBoxes(Point3D pathStart, Point3D pathEnd, BoundingBox movementBounds, boolean isBullet) {
        if (encompassingBox.intersects(movementBounds)) {
            //Get all collision boxes and check if we hit any of them.
            //Sort them by distance for later.
            TreeMap<Double, BoundingBoxHitResult> hitBoxes = new TreeMap<>();
            for (BoundingBox box : allCollisionBoxes) {
                if (box.collisionTypes.contains(CollisionType.ATTACK) || (isBullet && box.collisionTypes.contains(CollisionType.BULLET))) {
                    BoundingBoxHitResult hitResult = box.getIntersection(pathStart, pathEnd);
                    if (hitResult != null) {
                        double boxDistance = hitResult.position.distanceTo(pathStart);
                        boolean addBox = true;
                        if (box.groupDef != null) {
                            //Don't add boxes within the same group.
                            Iterator<Entry<Double, BoundingBoxHitResult>> iterator = hitBoxes.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Entry<Double, BoundingBoxHitResult> entry = iterator.next();
                                BoundingBoxHitResult otherHitEntry = entry.getValue();
                                if (otherHitEntry.box.groupDef == box.groupDef) {
                                    if (entry.getKey() > boxDistance) {
                                        iterator.remove();
                                    } else {
                                        addBox = false;
                                    }
                                    break;
                                }
                            }
                        }
                        if (addBox) {
                            hitBoxes.put(boxDistance, hitResult);
                        }
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
    public EntityBullet.HitType attackProjectile(Damage damage, EntityBullet bullet, Collection<BoundingBoxHitResult> hitBoxes) {
        //Check all boxes for armor and see if we penetrated them.
        for (BoundingBoxHitResult hitEntry : hitBoxes) {
            APart hitPart = getPartWithBox(hitEntry.box);
            AEntityF_Multipart<?> hitEntity = hitPart != null ? hitPart : this;

            //First check if we need to reduce health of the hitbox.
            boolean hitOperationalHitbox = false;
            if (hitEntry.box.groupDef != null && hitEntry.box.groupDef.health != 0 && !damage.isWater) {
                if (bullet != null) {
                    double currentDamage = hitEntity.getVariable("collision_" + (hitEntity.definition.collisionGroups.indexOf(hitEntry.box.groupDef) + 1) + "_damage").currentValue;
                    bullet.displayDebugMessage("HIT HEALTH BOX.  BOX CURRENT DAMAGE: " + currentDamage + " OF " + hitEntry.box.groupDef.health + "  ATTACKED FOR: " + damage.amount);
                }

                //This is a server-only action that does NOT cause us to stop processing.
                //Send off packet to damage the health hitbox (or damage directly on server) and continue as if we didn't hit anything.
                if (world.isClient()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitCollision(hitEntity, hitEntry.box, damage.amount));
                } else {
                    hitEntity.damageCollisionBox(hitEntry.box, damage.amount);
                }
                hitOperationalHitbox = true;
            }

            //Check armor pen and see if we hit too much and need to stop processing.
            if (hitEntry.box.groupDef != null && (hitEntry.box.groupDef.armorThickness != 0 || hitEntry.box.groupDef.heatArmorThickness != 0)) {
                hitOperationalHitbox = true;
                if (bullet != null) {
                    double armorThickness = hitEntry.box.definition != null ? (bullet.definition.bullet.isHeat && hitEntry.box.groupDef.heatArmorThickness != 0 ? hitEntry.box.groupDef.heatArmorThickness : hitEntry.box.groupDef.armorThickness) : 0;
                    double penetrationPotential = bullet.definition.bullet.isHeat ? bullet.definition.bullet.armorPenetration : (bullet.definition.bullet.armorPenetration * bullet.velocity / bullet.initialVelocity);
                    bullet.armorPenetrated += armorThickness;
                    bullet.displayDebugMessage("HIT ARMOR OF: " + (int) armorThickness);

                    if (bullet.armorPenetrated > penetrationPotential) {
                        //Bullet hit too much armor.
                        if (world.isClient()) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitGeneric(bullet.gun, bullet.bulletNumber, hitEntry.position, hitEntry.side, HitType.ARMOR));
                            bullet.waitingOnActionPacket = true;
                        } else {
                            EntityBullet.performGenericHitLogic(bullet.gun, bullet.bulletNumber, hitEntry.position, hitEntry.side, HitType.ARMOR);
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
                    damage = new Damage(bullet.gun, hitEntry.box, damage.amount);
                    boolean applyDamage = ((hitEntry.box.groupDef != null && (hitEntry.box.groupDef.health == 0 || damage.isWater)) || hitPart != null);
                    boolean removeAfterDamage = applyDamage && (hitPart == null || hitPart.definition.generic.forwardsDamageMultiplier > 0);

                    bullet.displayDebugMessage("HIT ENTITY BOX FOR DAMAGE: " + (int) damage.amount + " DAMAGE WAS AT " + (int) hitEntity.damageVar.currentValue);
                    if (world.isClient()) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitEntity(bullet.gun, hitEntity, damage));
                        if (removeAfterDamage) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityBulletHitGeneric(bullet.gun, bullet.bulletNumber, hitEntry.position, hitEntry.side, HitType.VEHICLE));
                            bullet.waitingOnActionPacket = true;
                            return EntityBullet.HitType.VEHICLE;
                        }
                    } else {
                        EntityBullet.performEntityHitLogic(hitEntity, damage);
                        if (removeAfterDamage) {
                            EntityBullet.performGenericHitLogic(bullet.gun, bullet.bulletNumber, hitEntry.position, hitEntry.side, HitType.VEHICLE);
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
        //Just don't add default data or our UUID, that needs to be fresh.
        IWrapperItemStack stack = super.getStack();
        IWrapperNBT stackData = save(InterfaceManager.coreInterface.getNewNBTWrapper());
        stackData.deleteAllUUIDTags();

        IWrapperNBT freshData = InterfaceManager.coreInterface.getNewNBTWrapper();
        freshData.setPackItem(definition, subDefinition.subName);
        freshData.getAllNames().forEach(name -> stackData.deleteEntry(name));

        if (!stackData.getAllNames().isEmpty()) {
            stack.setData(stackData);
        }
        return stack;
    }

    @Override
    public void updateText(LinkedHashMap<String, String> textLines) {
        super.updateText(textLines);
        parts.forEach(part -> part.updateText(textLines));
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
    public ComputedVariable createComputedVariable(String variable) {
        if (ComputedVariable.isNumberedVariable(variable)) {
            //Iterate through our parts to find the index of the pack def for the part we want.
            String partType = variable.substring(0, variable.indexOf("_"));
            int partNumber = ComputedVariable.getVariableNumber(variable);
            String partVariable = variable.substring(0, variable.lastIndexOf("_"));
            if (partType.equals("part")) {
                //Shortcut as we can just get the part for the slot.
                //Check index just in case someone screwed up a JSON.
                APart partFound = partNumber < partsInSlots.size() ? partsInSlots.get(partNumber) : null;
                if (partFound != null) {
                    return partFound.createComputedVariable(partVariable);
                }
            } else if (definition.parts != null) {
                for (int i = 0; i < definition.parts.size(); ++i) {
                    JSONPartDefinition partDef = definition.parts.get(i);
                    for (String partDefType : partDef.types) {
                        if (partDefType.startsWith(partType)) {
                            if (partNumber == 0) {
                                APart partFound = partsInSlots.get(i);
                                if (partFound != null) {
                                    return partFound.createComputedVariable(partVariable);
                                } else {
                                    return ZERO_VARIABLE;
                                }
                            } else {
                                --partNumber;
                            }
                            break;
                        }
                    }
                }
            }

            //Couldn't find the part, set to 0.
            return ZERO_VARIABLE;
        } else {
            return super.createComputedVariable(variable);
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
        if (definition.parts != null) {
            for (int i = 0; i < definition.parts.size(); ++i) {
                if (data != null) {
                    //Use a try-catch for parts in case they've changed since this entity was last placed.
                    //Don't want crashes due to pack updates.
                    try {
                        IWrapperNBT partData = data.getData("part_" + i);
                        if (partData != null) {
                            addPartFromStack(PackParser.getItem(partData.getString("packID"), partData.getString("systemName"), partData.getString("subName")).getNewStack(partData), placingPlayer, i, true, false);
                        }
                    } catch (Exception e) {
                        InterfaceManager.coreInterface.logError("Could not load part from NBT.  Did you un-install a pack?");
                        e.printStackTrace();
                    }
                    //Add constants. This is also done in initializeAnimations, but repeating it here ensures 
                    //the value will be set before spawning in any conditional parts.
                    if (definition.constantValues != null) {
                        definition.constantValues.forEach((constantKey, constantValue) -> getVariable(constantKey).setTo(constantValue, false));
                    }
                } else {
                    //Add default parts.  We need to do this after we actually create this part so its slots are valid.
                    //We also need to know if it is a new part or not, since that allows non-permanent default parts to be added.
                    JSONPartDefinition partDef = definition.parts.get(i);
                    if (partDef.conditionalDefaultParts != null) {
                        for (Entry<String, String> conditionalDef : partDef.conditionalDefaultParts.entrySet()) {
                            if (getVariable(conditionalDef.getKey()).isActive) {
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
    public APart addPartFromStack(IWrapperItemStack stack, IWrapperPlayer playerAdding, int slotIndex, boolean bypassSlotChecks, boolean alignTone) {
        JSONPartDefinition newPartDef = definition.parts.get(slotIndex);
        AItemPart partItem = (AItemPart) stack.getItem();
        if (partsInSlots.get(slotIndex) == null && (bypassSlotChecks || partItem.isPartValidForPackDef(newPartDef, subDefinition, !newPartDef.bypassSlotMinMax))) {
            //Part is not already present, and is valid, add it.
            IWrapperNBT partData = stack.getData();
            APart partToAdd = partItem.createPart(this, playerAdding, newPartDef, partData);
            if (alignTone) {
                partToAdd.updateTone(false);
            }
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
        //If the part is a seat, add it at the end of the list, since other parts might affect its movement.
        //If the part isn't a seat, add to the start so we assure its before any seats.
        //If we don't, update order with seated riders might get fouled.
        if (part instanceof PartSeat) {
            parts.add(part);
        } else {
            parts.add(0, part);
        }
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
    protected final void updateAllpartList() {
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
        //Clear camera list in prep for new entries from other areas.
        cameras.clear();
        cameraEntities.clear();

        parts.forEach(APart::updatePartList);
        //Clear computed variables, since our parts changed.
        resetVariables();
    }

    /**
     * Gets the part that has the passed-in bounding box.
     * Useful if we interacted with a box on this multipart and need
     * to know exactly what it went to.
     */
    public APart getPartWithBox(BoundingBox box) {
        for (APart part : parts) {
            if (part.allCollisionBoxes.contains(box)) {
                if (part.collisionBoxes.contains(box)) {
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
                    addPartFromStack(PackParser.getItem(partPackID, partSystemName).getNewStack(null), playerAdding, partSlot, false, true);
	            } catch (NullPointerException e) {
	            	playerAdding.sendPacket(new PacketPlayerChatMessage(playerAdding, LanguageSystem.SYSTEM_DEBUG, "Attempted to add defaultPart: " + partPackID + ":" + partSystemName + " to: " + providingDef.packID + ":" + providingDef.systemName + " but that part doesn't exist in the pack item registry."));
	            }
	        } catch (IndexOutOfBoundsException e) {
	        	playerAdding.sendPacket(new PacketPlayerChatMessage(playerAdding, LanguageSystem.SYSTEM_DEBUG, "Could not parse defaultPart definition: " + partName + ".  Format should be \"packId:partName\""));
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
        if (world.isClient()) {
            activeClientPartSlotBoxes.clear();
        }

        //Check if we made childLimit.  If so, bail on adding slot boxes.
        AEntityF_Multipart<?> testEntity = this;
        int level = 0;
        do {
            if (testEntity instanceof APart) {
                ++level;
                APart part = (APart) testEntity;
                if (part.placementDefinition.maxPartLevels != 0 && part.placementDefinition.maxPartLevels <= level) {
                    return;
                }
                testEntity = part.entityOn;
            } else {
                testEntity = null;
            }
        } while (testEntity != null);

        //Good to add slot boxes.
        for (int i = 0; i < partsInSlots.size(); ++i) {
            if (partsInSlots.get(i) == null) {
                JSONPartDefinition partDef = definition.parts.get(i);
                boolean isLarge = false;
                for (String type : partDef.types) {
                    if (type.startsWith("engine_") || type.startsWith("ground_") || type.startsWith("propeller")) {
                        isLarge = true;
                        break;
                    }
                }
                BoundingBox newSlotBox;
                if (partDef.slotWidth != 0) {
                    newSlotBox = new BoundingBox(partDef.pos, partDef.pos.copy().rotate(orientation).add(position), partDef.slotWidth / 2D, partDef.slotHeight / 2D, partDef.slotWidth / 2D, false, partSlotBoxCollisionTypes);
                } else if (isLarge) {
                    newSlotBox = new BoundingBox(partDef.pos, partDef.pos.copy().rotate(orientation).add(position), PART_SLOT_LARGE_HITBOX_WIDTH / 2D, PART_SLOT_LARGE_HITBOX_HEIGHT / 2D, PART_SLOT_LARGE_HITBOX_WIDTH / 2D, false, partSlotBoxCollisionTypes);
                } else {
                    newSlotBox = new BoundingBox(partDef.pos, partDef.pos.copy().rotate(orientation).add(position), PART_SLOT_NORMAL_HITBOX_WIDTH / 2D, PART_SLOT_NORMAL_HITBOX_HEIGHT / 2D, PART_SLOT_NORMAL_HITBOX_WIDTH / 2D, false, partSlotBoxCollisionTypes);
                }
                partSlotBoxes.put(newSlotBox, partDef);
            }
        }
        //Add all slot boxes, clients will remove as applicable, servers will never remove.
        collisionBoxes.addAll(partSlotBoxes.keySet());
    }

    @Override
    protected void updateCollisionBoxes(boolean requiresDeltaUpdates) {
        super.updateCollisionBoxes(requiresDeltaUpdates);
        if (world.isClient()) {
            if (!partSlotBoxes.isEmpty()) {
                world.beginProfiling("PartSlotActives", false);
                activeClientPartSlotBoxes.clear();
                if (canBeClicked()) {
                    IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
                    AItemBase heldItem = player.getHeldItem();
                    boolean holdingScanner = heldItem instanceof ItemItem && ((ItemItem) heldItem).definition.item.type == ItemComponentType.SCANNER;
                    if (holdingScanner || heldItem instanceof AItemPart) {
                        for (Entry<BoundingBox, JSONPartDefinition> partSlotBoxEntry : partSlotBoxes.entrySet()) {
                            BoundingBox box = partSlotBoxEntry.getKey();
                            JSONPartDefinition slotDef = partSlotBoxEntry.getValue();
                            boolean activeSlotFound = false;
                            if (holdingScanner) {
                                //Don't check held parts, just check if we can actually place anything in a slot.
                                if (isVariableListTrue(partSlotBoxEntry.getValue().interactableVariables)) {
                                    activeSlotFound = true;
                                }
                            } else {
                                AItemPart heldPart = (AItemPart) heldItem;
                                if (heldPart.isPartValidForPackDef(slotDef, subDefinition, false) && isVariableListTrue(slotDef.interactableVariables)) {
                                    //Part matches.  Add the box.  Set the box bounds to the special bounds of the generic part if we're holding one, but only if we don't have a holo box bounds defined.
                                    if (slotDef.slotWidth == 0 && heldPart.definition.generic.width != 0 && heldPart.definition.generic.height != 0) {
                                        box.widthRadius = heldPart.definition.generic.width / 2D;
                                        box.heightRadius = heldPart.definition.generic.height / 2D;
                                        box.depthRadius = heldPart.definition.generic.width / 2D;
                                    }
                                    activeSlotFound = true;
                                }
                            }
                            if (activeSlotFound) {
                                collisionBoxes.add(box);
                                activeClientPartSlotBoxes.put(box, slotDef);
                                if (requiresDeltaUpdates) {
                                    AnimationSwitchbox switchBox = partSlotSwitchboxes.get(slotDef);
                                    if (switchBox != null) {
                                        if (switchBox.runSwitchbox(0, false)) {
                                            box.globalCenter.set(box.localCenter).transform(switchBox.netMatrix);
                                            box.updateToEntity(this, box.globalCenter);
                                        }
                                    } else {
                                        box.updateToEntity(this, null);
                                    }
                                }
                            } else {
                                collisionBoxes.remove(box);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void updateEncompassingBox() {
        super.updateEncompassingBox();
        //If we are on the server, now add slot boxes.
        //If did this before in the update method, the encompassing box would use the slot boxes.
        //Since the server doesn't update slot position, this leads to issues.
        if (!world.isClient()) {
            collisionBoxes.addAll(partSlotBoxes.keySet());
        }

        //Populate all box list.
        allCollisionBoxes.clear();
        allCollisionBoxes.addAll(collisionBoxes);
        for (APart part : parts) {
            allCollisionBoxes.addAll(part.allCollisionBoxes);
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
        encompassingBox.updateToEntity(this, null);
    }

    @Override
    public void renderBoundingBoxes(TransformationMatrix transform) {
        super.renderBoundingBoxes(transform);
        if (System.currentTimeMillis() % 1000 > 500) {
            encompassingBox.renderWireframe(this, transform, null, ColorRGB.WHITE);
        }
    }

    @Override
    protected void renderHolographicBoxes(TransformationMatrix transform) {
        if (!activeClientPartSlotBoxes.isEmpty()) {
            //If we are holding a part or scanner, render the valid slots.
            world.beginProfiling("PartHoloboxes", true);
            IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
            AItemBase heldItem = player.getHeldItem();
            AItemPart heldPart = heldItem instanceof AItemPart ? (AItemPart) heldItem : null;
            boolean holdingScanner = player.isHoldingItemType(ItemComponentType.SCANNER);
            if (heldPart != null || holdingScanner) {
                if (holdingScanner) {
                    for (Entry<BoundingBox, JSONPartDefinition> slotEntry : activeClientPartSlotBoxes.entrySet()) {
                        BoundingBox box = slotEntry.getKey();
                        JSONPartDefinition slotDef = slotEntry.getValue();
                        Point3D boxCenterDelta = box.globalCenter.copy().subtract(position);
                        boolean isImportant = false;
                        for (String slotType : slotDef.types) {
                            if (slotType.startsWith("ground") || slotType.startsWith("engine") || slotType.startsWith("propeller") || slotType.startsWith("seat")) {
                                isImportant = true;
                                break;
                            }
                        }
                        box.renderHolographic(transform, boxCenterDelta, isImportant ? ColorRGB.YELLOW : ColorRGB.DARK_GRAY);
                    }
                } else {
                    for (Entry<BoundingBox, JSONPartDefinition> partSlotEntry : activeClientPartSlotBoxes.entrySet()) {
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
        data.setBoolean("spawnedDefaultParts", true);
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
