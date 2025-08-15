package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BlockHitResult;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONMuzzle;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.LockOnType;
import minecrafttransportsimulator.jsondefs.JSONPart.TargetType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Basic gun class.  This class is responsible for representing a gun in the world.  This gun
 * can be placed on anything and modeled by anything as the code is only for controlling the firing
 * of the gun.  This means this class only stores the internal state of the gun, such as the number
 * of bullets, cooldown time remaining, who is controlling it, etc.  It does NOT set these states, as
 * these are done externally.
 * <br><br>
 * However, since this gun object is responsible for firing bullets, it does need to have spatial awareness.
 * Because of this, the gun contains a position and orientation offset that may be set to "move" the gun in
 * the world.  This should not be confused with the gun's internal orientation, which is set based on commands
 * given to the gun and may change.
 *
 * @author don_bruce
 */
public class PartGun extends APart {
    //Variables based on the specific gun's properties.
    private final double minYaw;
    private final double maxYaw;
    private final double defaultYaw;
    private final double yawSpeed;

    private final double minPitch;
    private final double maxPitch;
    private final double defaultPitch;
    private final double pitchSpeed;

    private final boolean resetPosition;
    private final boolean isHandHeld;
    private final IWrapperPlayer holdingPlayer;

    //Variables that can be modified
    private final ComputedVariable muzzleVelocityVar;
    private final ComputedVariable fireDelayVar;
    private final ComputedVariable bulletSpreadFactorVar;
    private final ComputedVariable knockbackVar;
    private final ComputedVariable autoReloadVar;
    private final ComputedVariable blockReloadingVar;
    private final ComputedVariable isSemiAutoVar;
    private final ComputedVariable canLockTargetsVar;
    private final ComputedVariable firingRequestedVar;
    private final ComputedVariable ableToFireVar;
    public final ComputedVariable twoHandedVar;

    private final List<PartInteractable> connectedCrates = new ArrayList<>();

    //Stored variables used to determine bullet firing behavior.
    public ItemBullet lastLoadedBullet;
    public final List<ItemBullet> firedBullets = new ArrayList<>();
    private int bulletsFired;
    private int loadedBulletCount;
    private int reloadingBulletCount;
    private int currentMuzzleGroupIndex;
    private final RotationMatrix internalOrientation;
    private final RotationMatrix prevInternalOrientation;
    private final List<ItemBullet> loadedBullets = new ArrayList<>();
    private final List<Integer> loadedBulletCounts = new ArrayList<>();
    private final List<ItemBullet> reloadingBullets = new ArrayList<>();
    private final List<Integer> reloadingBulletCounts = new ArrayList<>();
    private final Random randomGenerator;

    //These variables are used during firing and will be reset on loading.
    public GunState state;
    public boolean bulletsPresentOnServer;
    public boolean firedThisTick;
    public boolean firedSinceRequested;
    public boolean isReloading;
    public boolean playerHoldingTrigger;
    public boolean playerPressedTrigger;
    public boolean isHandHeldGunAimed;
    public boolean isHandHeldGunEquipped;
    public boolean isHandHeldGunReloadRequested;
    public boolean isRunningInCoaxialMode;
    private int camOffset;
    private int cooldownTimeRemaining;
    private int reloadDelayRemaining;
    private int reloadStartTimeRemaining;
    private int reloadMainTimeRemaining;
    private int reloadEndTimeRemaining;
    private int windupTimeCurrent;
    private int windupRotation;
    private long lastMillisecondFired;
    public IWrapperEntity lastController;
    private PartSeat lastControllerSeat;
    private Point3D controllerRelativeLookVector = new Point3D();
    public IWrapperEntity entityTarget;
    public PartEngine engineTarget;
    public Point3D targetPosition;
    public EntityBullet currentBullet;
    public final Set<EntityBullet> activeManualBullets = new HashSet<>();
    private final Point3D bulletPosition = new Point3D();
    private final Point3D bulletVelocity = new Point3D();
    private final RotationMatrix bulletOrientation = new RotationMatrix();
    private final Point3D bulletPositionRender = new Point3D();
    private final Point3D bulletVelocityRender = new Point3D();
    private final RotationMatrix bulletOrientationRender = new RotationMatrix();
    private final List<PartSeat> seatsControllingGun = new ArrayList<>();

    //Temp helper variables for calculations
    private final Point3D targetVector = new Point3D();
    private final Point3D targetAngles = new Point3D();
    private final Point3D controllerAngles = new Point3D();
    private final RotationMatrix firingSpreadRotation = new RotationMatrix();
    private final RotationMatrix pitchMuzzleRotation = new RotationMatrix();
    private final RotationMatrix yawMuzzleRotation = new RotationMatrix();
    private final Point3D normalizedConeVector = new Point3D();
    private final Point3D normalizedEntityVector = new Point3D();

    //Global data.
    private static final int RAYTRACE_DISTANCE = 750;
    private static final double DEFAULT_CONE_ANGLE = 2.0;

    public PartGun(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, ItemPartGun item, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, item, data);

        //Set min/max yaw/pitch angles based on our definition and the entity definition.
        //If the entity definition min/max yaw is -180 to 180, set it to that.  Otherwise, get the max bounds.
        //Yaw/Pitch set to 0 is ignored as it's assumed to be un-defined.
        if (placementDefinition.minYaw == -180 && placementDefinition.maxYaw == 180) {
            this.minYaw = -180;
            this.maxYaw = 180;
        } else {
            if (definition.gun.minYaw != 0) {
                this.minYaw = placementDefinition.minYaw != 0 ? Math.max(definition.gun.minYaw, placementDefinition.minYaw) : definition.gun.minYaw;
            } else {
                this.minYaw = placementDefinition.minYaw;
            }
            if (definition.gun.maxYaw != 0) {
                this.maxYaw = placementDefinition.maxYaw != 0 ? Math.min(definition.gun.maxYaw, placementDefinition.maxYaw) : definition.gun.maxYaw;
            } else {
                this.maxYaw = placementDefinition.maxYaw;
            }
        }
        if (placementDefinition.defaultYaw != 0 && placementDefinition.defaultYaw >= minYaw && placementDefinition.defaultYaw <= maxYaw) {
            this.defaultYaw = placementDefinition.defaultYaw;
        } else {
            this.defaultYaw = definition.gun.defaultYaw;
        }
        if (definition.gun.yawSpeed != 0 && placementDefinition.yawSpeed != 0) {
            this.yawSpeed = definition.gun.yawSpeed < placementDefinition.yawSpeed ? definition.gun.yawSpeed : placementDefinition.yawSpeed;
        } else if (definition.gun.yawSpeed != 0) {
            this.yawSpeed = definition.gun.yawSpeed;
        } else {
            this.yawSpeed = placementDefinition.yawSpeed;
        }

        //Swap min and max pitch.  In JSON, negative values are down and positive up.
        //But for us, positive is down and negative is up.
        if (definition.gun.minPitch != 0) {
            this.minPitch = placementDefinition.maxPitch != 0 ? Math.max(-definition.gun.maxPitch, -placementDefinition.maxPitch) : -definition.gun.maxPitch;
        } else {
            this.minPitch = -placementDefinition.maxPitch;
        }
        if (definition.gun.minPitch != 0) {
            this.maxPitch = placementDefinition.minPitch != 0 ? Math.min(-definition.gun.minPitch, -placementDefinition.minPitch) : -definition.gun.minPitch;
        } else {
            this.maxPitch = -placementDefinition.minPitch;
        }
        if (placementDefinition.defaultPitch != 0 && -placementDefinition.defaultPitch >= minPitch && -placementDefinition.defaultPitch <= maxPitch) {
            this.defaultPitch = -placementDefinition.defaultPitch;
        } else {
            this.defaultPitch = -definition.gun.defaultPitch;
        }
        if (definition.gun.pitchSpeed != 0 && placementDefinition.pitchSpeed != 0) {
            this.pitchSpeed = definition.gun.pitchSpeed < placementDefinition.pitchSpeed ? definition.gun.pitchSpeed : placementDefinition.pitchSpeed;
        } else if (definition.gun.pitchSpeed != 0) {
            this.pitchSpeed = definition.gun.pitchSpeed;
        } else {
            this.pitchSpeed = placementDefinition.pitchSpeed;
        }
        
        this.resetPosition = definition.gun.resetPosition || placementDefinition.resetPosition;
        this.isHandHeld = entityOn instanceof EntityPlayerGun;
        this.holdingPlayer = isHandHeld ? ((EntityPlayerGun) entityOn).player : null;

        //Load saved data.
        if (data != null) {
            this.state = GunState.values()[data.getInteger("state")];
            this.bulletsFired = data.getInteger("bulletsFired");
            this.currentMuzzleGroupIndex = data.getInteger("currentMuzzleGroupIndex");
            this.internalOrientation = new RotationMatrix();
            if (!isHandHeld) {
                internalOrientation.setToAngles(data.getPoint3d("internalAngles"));
            }
            for(int i=0; i<data.getInteger("loadedBulletsSize"); ++i) {
                IWrapperNBT bulletData = data.getData("loadedBullet" + i);
                loadedBullets.add(bulletData.getPackItem());
                int count = bulletData.getInteger("count");
                loadedBulletCounts.add(count);
                loadedBulletCount += count;
            }
            for (int i = 0; i < data.getInteger("reloadingBulletsSize"); ++i) {
                IWrapperNBT bulletData = data.getData("reloadingBullet" + i);
                reloadingBullets.add(bulletData.getPackItem());
                int count = bulletData.getInteger("count");
                reloadingBulletCounts.add(count);
                reloadingBulletCount += count;
            }
            reloadStartTimeRemaining = data.getInteger("reloadStartTimeRemaining");
            reloadMainTimeRemaining = data.getInteger("reloadMainTimeRemaining");
            reloadEndTimeRemaining = data.getInteger("reloadEndTimeRemaining");
            isReloading = reloadStartTimeRemaining != 0 || reloadMainTimeRemaining != 0 || reloadEndTimeRemaining != 0;

            if (!loadedBullets.isEmpty()) {
                lastLoadedBullet = loadedBullets.get(0);
                if (world.isClient()) {
                    bulletsPresentOnServer = true;
                }
            }
            if (data.getBoolean("savedSeed")) {
                long randomSeed = (((long) data.getInteger("randomSeedPart1")) << 32) | (data.getInteger("randomSeedPart2") & 0xffffffffL);
                randomGenerator = new Random(randomSeed);
            } else {
                randomGenerator = new Random();
            }
        } else {
            this.state = GunState.INACTIVE;
            this.internalOrientation = new RotationMatrix();
            randomGenerator = new Random();
            if (definition.gun.preloadedBullet != null) {
                String[] splitName = definition.gun.preloadedBullet.split(":");
                ItemBullet loadedBullet = PackParser.getItem(splitName[0], splitName[1]);
                if (loadedBullet != null) {
                    loadedBullets.add(loadedBullet);
                    this.lastLoadedBullet = loadedBullet;
                    loadedBulletCounts.add(loadedBullet.definition.bullet.quantity);
                    loadedBulletCount = loadedBullet.definition.bullet.quantity;
                } else {
                    InterfaceManager.coreInterface.logError("Tried to load preloaded bullet " + definition.gun.preloadedBullet + " into gun " + definition + " but couldn't because the bullet doesn't exist.  Report this to the pack author!");
                }
            }
        }
        this.prevInternalOrientation = new RotationMatrix().set(internalOrientation);

        addVariable(this.muzzleVelocityVar = new ComputedVariable(this, "muzzleVelocity"));
        addVariable(this.fireDelayVar = new ComputedVariable(this, "fireDelay"));
        addVariable(this.bulletSpreadFactorVar = new ComputedVariable(this, "bulletSpreadFactor"));
        addVariable(this.knockbackVar = new ComputedVariable(this, "knockback"));
        addVariable(this.autoReloadVar = new ComputedVariable(this, "autoReload"));
        addVariable(this.blockReloadingVar = new ComputedVariable(this, "blockReloading"));
        addVariable(this.isSemiAutoVar = new ComputedVariable(this, "isSemiAuto"));
        addVariable(this.canLockTargetsVar = new ComputedVariable(this, "canLockTargets"));
        addVariable(this.ableToFireVar = new ComputedVariable(this, "ableToFire"));
        addVariable(this.firingRequestedVar = new ComputedVariable(this, "firingRequested"));
        addVariable(this.twoHandedVar = new ComputedVariable(this, "isTwoHanded"));
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        //Check to see if we have any bullets in our hands.
        //If so, try to re-load this gun with them.
        if (reloadDelayRemaining == 0) {
            IWrapperItemStack heldStack = player.getHeldStack();
            if (tryToReload(heldStack, false) && !player.isCreative()) {
                player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
            }
        }
        return true;
    }

    @Override
    public void update() {
        //Set gun state and do updates.
        firedThisTick = false;
        isRunningInCoaxialMode = false;
        prevInternalOrientation.set(internalOrientation);
        if (currentBullet != null && !currentBullet.isValid) {
            currentBullet = null;
        }
        if (isActive && !isSpare) {
            //Check if we have a controller.
            //We aren't making sentry turrets here.... yet.
            IWrapperEntity controller = getGunController();
            if (controller != null) {
                lastController = controller;
                if (isHandHeld) {
                    state = state.promote(GunState.CONTROLLED);
                } else {
                    //If this gun type can only have one selected at a time, check that this has the selected index.
                    lastControllerSeat = (PartSeat) lastController.getEntityRiding();
                    if (cachedItem == lastControllerSeat.activeGunItem && (!definition.gun.fireSolo || lastControllerSeat.gunGroups.get(cachedItem).get(lastControllerSeat.gunIndex) == this)) {
                        state = state.promote(GunState.CONTROLLED);
                    } else {
                        state = state.demote(GunState.ACTIVE);
                        controller = null;
                        entityTarget = null;
                        engineTarget = null;
                    }
                }
            }
            if (controller == null) {
                //If we aren't being controlled, check if we have any coaxial guns.
                //If we do, and they have a controller, then we use that as our controller.
                //This allows them to control this gun without being the actual controller for firing.
                if (!parts.isEmpty()) {
                    for (APart part : parts) {
                        if (part instanceof PartGun && part.placementDefinition.isCoAxial) {
                            controller = ((PartGun) part).getGunController();
                            if (controller != null) {
                                //Check if the coaxial is controlled or not.
                                lastController = controller;
                                lastControllerSeat = (PartSeat) lastController.getEntityRiding();
                                if (part.cachedItem == lastControllerSeat.activeGunItem && (!definition.gun.fireSolo || lastControllerSeat.gunGroups.get(part.cachedItem).get(lastControllerSeat.gunIndex) == part)) {
                                    state = state.promote(GunState.CONTROLLED);
                                    isRunningInCoaxialMode = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                //Conversely, if we are a coaxial gun, and our parent is being controlled, we need to be controlled by it. 
                if (placementDefinition.isCoAxial && entityOn instanceof PartGun) {
                    controller = ((PartGun) entityOn).getGunController();
                    if (controller != null) {
                        //Check if the coaxial is controlled or not.
                        lastController = controller;
                        lastControllerSeat = (PartSeat) lastController.getEntityRiding();
                        if (entityOn.cachedItem == lastControllerSeat.activeGunItem && (!definition.gun.fireSolo || lastControllerSeat.gunGroups.get(entityOn.cachedItem).get(lastControllerSeat.gunIndex) == entityOn)) {
                            state = state.promote(GunState.CONTROLLED);
                            isRunningInCoaxialMode = true;
                        }
                    }
                }

                if (controller == null) {
                    state = state.demote(GunState.ACTIVE);
                    //If we are hand-held, we need to die since we aren't a valid gun.
                    if (isHandHeld) {
                        remove();
                        return;
                    }
                }
            }

            //Adjust yaw and pitch to the direction of the controller.
            if (state.isAtLeast(GunState.CONTROLLED)) {
                handleControl(controller);
                if (isRunningInCoaxialMode) {
                    state = state.demote(GunState.ACTIVE);
                    controller = null;
                    entityTarget = null;
                    engineTarget = null;
                }
            }

            //If we can accept bullets, and aren't currently loading any, re-load ourselves from any inventories.
            //While the reload method checks for reload time, we check here to save on code processing.
            //No sense in looking for bullets if we can't load them anyways.
            if (!world.isClient()) {
                //Don't process reload stuff when at the end of the reloading phase if we are clipless, wait for the next phase.
                //Clipped guns we can't reload if we're already reloading.
                if (!blockReloadingVar.isActive && reloadDelayRemaining == 0 && (!isReloading || (definition.gun.isClipless && reloadMainTimeRemaining != 0))) {

                    if (isHandHeld) {
                        if ((loadedBulletCount == 0 && playerPressedTrigger) || isHandHeldGunReloadRequested || autoReloadVar.isActive) {
                            //Check the player's inventory for bullets.
                            IWrapperInventory inventory = ((IWrapperPlayer) lastController).getInventory();
                            for (int i = 0; i < inventory.getSize(); ++i) {
                                if (tryToReload(inventory.getStack(i), isHandHeldGunReloadRequested && !definition.gun.isClipless)) {
                                    //Bullet is right type, and we can fit it.  Remove from player's inventory and add to the gun.
                                    inventory.removeFromSlot(i, 1);
                                    break;
                                }
                            }
                        }
                    } else {
                        if (autoReloadVar.isActive) {
                            //Iterate through all the inventory slots in crates to try to find matching ammo.
                            for (PartInteractable crate : connectedCrates) {
                                if (crate.isActive) {
                                    EntityInventoryContainer inventory = crate.inventory;
                                    for (int i = 0; i < inventory.getSize(); ++i) {
                                        if (tryToReload(inventory.getStack(i), false)) {
                                            //Bullet is right type, and we can fit it.  Remove from crate and add to the gun.
                                            //Return here to ensure we don't set the loadedBullet to blank since we found bullets.
                                            if (!ConfigSystem.settings.general.devMode.value) {
                                                inventory.removeFromSlot(i, 1);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //Set final gun active state and variables, and fire if those line up with conditions.
            //Note that this code runs concurrently on the client and server.  This prevents the need for packets for bullet
            //spawning and ensures that they spawn every tick on quick-firing guns.  Hits are registered on both sides, but
            //hit processing is only done on the server; clients just de-spawn the bullet and wait for packets.
            //Because of this, there is no linking between client and server bullets, and therefore they do not handle NBT or UUIDs.
            if (state.isAtLeast(GunState.FIRING_REQUESTED)) {
                if (ableToFireVar.isActive) {
                    //Get current group and use it to determine firing offset.
                    //Don't calculate this if we already did on a prior firing command.
                    if (camOffset <= 0) {
                        if (!definition.gun.fireSolo && lastControllerSeat != null) {
                            List<PartGun> gunGroup = lastControllerSeat.gunGroups.get(cachedItem);
                            int thisGunIndex = gunGroup.indexOf(this);
                            if (lastControllerSeat.gunGroupIndex == thisGunIndex) {
                                if (gunGroup.size() > 1) {
                                    camOffset = ((int) fireDelayVar.currentValue) / gunGroup.size();
                                } else {
                                    camOffset = 0;
                                }
                            } else {
                                //Wait for our turn.
                                camOffset = -1;
                            }
                        }
                    } else {
                        --camOffset;
                    }

                    //If we have bullets, try and fire them.
                    //The only exception is if the server is the controller, in that case fire until we are told to stop.
                    //This can happen if the gun fires quickly and bullet counts get de-synced with on/off states.
                    boolean cycledGun = false;
                    boolean serverIsPrimaryController = lastLoadedBullet != null && (lastLoadedBullet.definition.bullet.isLongRange || !(lastController instanceof IWrapperPlayer));
                    if (loadedBulletCount > 0 || (world.isClient() && serverIsPrimaryController && bulletsPresentOnServer)) {
                        state = state.promote(GunState.FIRING_CURRENTLY);

                        //If we are in our cam, fire the bullets.
                        if (camOffset == 0) {
                            for (JSONMuzzle muzzle : definition.gun.muzzleGroups.get(currentMuzzleGroupIndex).muzzles) {
                                //Spawn a bullet for each pellet.
                                for (int i = 0; i < (lastLoadedBullet.definition.bullet.pellets > 0 ? lastLoadedBullet.definition.bullet.pellets : 1); i++) {
                                    //Need to increment the bullet count, even if we're not actually firing the bullet, so we sync state.
                                    //Note that this takes into account pellets too since they are each their own entity.
                                    ++bulletsFired;
                                    if (world.isClient() || serverIsPrimaryController) {
                                        //Get the bullet's state.
                                        setBulletSpawn(bulletPosition, bulletVelocity, bulletOrientation, muzzle, true);

                                        //Add the bullet to the world.
                                        //If the bullet is a missile, give it a target.
                                        EntityBullet newBullet;
                                        if (lastLoadedBullet.definition.bullet.turnRate > 0) {
                                            if (entityTarget != null) {
                                                newBullet = new EntityBullet(bulletPosition, bulletVelocity, bulletOrientation, this, bulletsFired, entityTarget);
                                            } else if (engineTarget != null) {
                                                newBullet = new EntityBullet(bulletPosition, bulletVelocity, bulletOrientation, this, bulletsFired, engineTarget);
                                            } else if (definition.gun.lockOnType == LockOnType.MANUAL) {
                                                newBullet = new EntityBullet(bulletPosition, bulletVelocity, bulletOrientation, this, bulletsFired, targetPosition);
                                                activeManualBullets.add(newBullet);
                                            } else {
                                                //No entity found, just fire missile off in direction facing.
                                                newBullet = new EntityBullet(bulletPosition, bulletVelocity, bulletOrientation, this, bulletsFired);
                                            }
                                        } else {
                                            newBullet = new EntityBullet(bulletPosition, bulletVelocity, bulletOrientation, this, bulletsFired);
                                        }
                                        world.addEntity(newBullet);
                                    }
                                }

                                //Now do knockback, if it exists.
                                if (isHandHeld) {
                                    if (!world.isClient()) {
                                        performGunKnockback();
                                        InterfaceManager.packetInterface.sendToAllClients(new PacketPartGun(this, PacketPartGun.Request.KNOCKBACK));
                                    } else if (InterfaceManager.clientInterface.getClientPlayer().equals(lastController)) {
                                        InterfaceManager.packetInterface.sendToServer(new PacketPartGun(this, PacketPartGun.Request.KNOCKBACK));
                                    }
                                }

                                //Decrement bullets, but check to make sure we still have some.
                                //We might have a partial volley with only some muzzles firing in this group, so if we did, break out early.
                                //Also add a bullet to the fired list, since the last loaded could be changed if we change ammo.
                                firedBullets.add(lastLoadedBullet);
                                if (loadedBulletCount > 0) {
                                    --loadedBulletCount;
                                    int count = loadedBulletCounts.get(0);
                                    if (--count == 0) {
                                        //Need to set next bullet in the queue, if we have one.
                                        loadedBullets.remove(0);
                                        loadedBulletCounts.remove(0);
                                        if (!loadedBullets.isEmpty()) {
                                            lastLoadedBullet = loadedBullets.get(0);
                                        } else {
                                            if (!world.isClient()) {
                                                InterfaceManager.packetInterface.sendToAllClients(new PacketPartGun(this, PacketPartGun.Request.BULLETS_OUT));
                                            }
                                            break;
                                        }
                                    } else {
                                        loadedBulletCounts.set(0, count);
                                    }
                                }
                            }

                            //Update states.
                            cooldownTimeRemaining = (int) fireDelayVar.currentValue;
                            firedThisTick = true;
                            firedSinceRequested = true;
                            cycledGun = true;
                            lastMillisecondFired = System.currentTimeMillis();
                            if (definition.gun.muzzleGroups.size() == ++currentMuzzleGroupIndex) {
                                currentMuzzleGroupIndex = 0;
                            }
                        }
                    } else if (camOffset == 0) {
                        //Got to end of cam with no bullets, cycle gun.
                        cycledGun = true;
                    }
                    if (cycledGun) {
                        if (lastControllerSeat != null) {
                            List<PartGun> gunGroup = lastControllerSeat.gunGroups.get(cachedItem);
                            int currentIndex = gunGroup.indexOf(this);
                            if (currentIndex + 1 < gunGroup.size()) {
                                lastControllerSeat.gunGroupIndex = currentIndex + 1;
                            } else {
                                lastControllerSeat.gunGroupIndex = 0;
                            }
                        }
                    }
                } else {
                    state = state.demote(GunState.FIRING_REQUESTED);
                }
            } else {
                firedSinceRequested = false;
            }

            //Set or decrement reloadDelay.
            if (state.isAtLeast(GunState.FIRING_CURRENTLY)) {
                reloadDelayRemaining = definition.gun.reloadDelay;
            } else if (reloadDelayRemaining > 0) {
                --reloadDelayRemaining;
            }
        } else {
            //Inactive gun, set as such and set to default position if we have one.
            state = GunState.INACTIVE;
            entityTarget = null;
            engineTarget = null;
            if (resetPosition) {
                handleMovement(defaultYaw - internalOrientation.angles.y, defaultPitch - internalOrientation.angles.x);
            }
        }

        //If we are reloading, decrement the reloading timer.
        //If we are done reloading, add the new bullets.
        //This comes after the reloading block as we need a 0/1 state-change for the various animations,
        //so at some point the reload time needs to hit 0.
        //This is something we do regardless of active status, since even inactive guns can have bullets put into them.
        if (isReloading) {
            if (reloadStartTimeRemaining > 0) {
                --reloadStartTimeRemaining;
            } else if (reloadMainTimeRemaining > 0) {
                --reloadMainTimeRemaining;
                if (reloadMainTimeRemaining == 0) {
                    ItemBullet bulletToLoad = reloadingBullets.remove(0);
                    int countToLoad = reloadingBulletCounts.remove(0);
                    loadedBullets.add(bulletToLoad);
                    loadedBulletCounts.add(countToLoad);
                    if (loadedBullets.size() == 1) {
                        //Just got our first bullet, set variable.
                        lastLoadedBullet = bulletToLoad;
                    }
                    loadedBulletCount += countToLoad;
                    reloadingBulletCount -= countToLoad;
                    firedBullets.clear();//Clear fired bullets in case we haven't used them for any animations, don't want an overflow list.
                    if (!world.isClient()) {
                        InterfaceManager.packetInterface.sendToAllClients(new PacketPartGun(this, PacketPartGun.Request.BULLETS_PRESENT));
                    }
                    if (reloadingBullets.isEmpty() && reloadEndTimeRemaining == 0) {
                        //No winddown, and no bullets left to reload, reloading ends here.
                        isReloading = false;
                    }
                }
            } else if (!reloadingBullets.isEmpty()) {
                //Go around again, we just needed to wait 1 tick for systems to see a 0->1 state change.
                reloadMainTimeRemaining = definition.gun.reloadTime;
            } else if (--reloadEndTimeRemaining == 0) {
                isReloading = false;
            }
        }

        //Increment or decrement windup.
        //This is done outside the main active area as windup can wind-down on deactivated guns.
        if (state.isAtLeast(GunState.FIRING_REQUESTED)) {
            if (windupTimeCurrent < definition.gun.windupTime) {
                ++windupTimeCurrent;
            }
        } else if (windupTimeCurrent > 0) {
            if (definition.gun.windsDownInstantly) {
                windupTimeCurrent = 0;
            } else {
                --windupTimeCurrent;
            }
        }
        windupRotation += windupTimeCurrent;

        //Decrement cooldown, if we have it.
        if (cooldownTimeRemaining > 0) {
            --cooldownTimeRemaining;
        }

        //Reset flags.
        isHandHeldGunReloadRequested = false;
        playerPressedTrigger = false;

        //Now run super.  This needed to wait for the gun states to ensure proper states.
        super.update();

        //If we have a controller seat on us, adjust the player's facing to account for our movement.
        //If we don't, we'll just rotate forever.
        if (lastControllerSeat != null && parts.contains(lastControllerSeat)) {
            orientation.convertToAngles();
            lastControllerSeat.riderRelativeOrientation.angles.y -= (orientation.angles.y - prevOrientation.angles.y);
            lastControllerSeat.riderRelativeOrientation.angles.x -= (orientation.angles.x - prevOrientation.angles.x);
        }
    }

    @Override
    public void updatePartList() {
        super.updatePartList();

        seatsControllingGun.clear();
        addLinkedPartsToList(seatsControllingGun, PartSeat.class);
        for (APart part : parts) {
            if (part instanceof PartSeat) {
                seatsControllingGun.add((PartSeat) part);
            }
        }

        connectedCrates.clear();
        for (APart part : parts) {
            if (part instanceof PartInteractable) {
                connectedCrates.add((PartInteractable) part);
            }
        }
        addLinkedPartsToList(connectedCrates, PartInteractable.class);
        connectedCrates.removeIf(crate -> crate.definition.interactable.interactionType != InteractableComponentType.CRATE || !crate.definition.interactable.feedsVehicles);
    }

    @Override
    public void setVariableDefaults() {
        super.setVariableDefaults();
        muzzleVelocityVar.setTo(definition.gun.muzzleVelocity, false);
        fireDelayVar.setTo(definition.gun.fireDelay, false);
        bulletSpreadFactorVar.setTo(definition.gun.bulletSpreadFactor, false);
        knockbackVar.setTo(definition.gun.knockback, false);
        autoReloadVar.setTo(definition.gun.autoReload ? 1 : 0, false);
        blockReloadingVar.setTo(definition.gun.blockReloading ? 1 : 0, false);
        isSemiAutoVar.setTo(definition.gun.isSemiAuto ? 1 : 0, false);
        canLockTargetsVar.setTo(definition.gun.canLockTargets ? 1 : 0, false);
        twoHandedVar.setTo(definition.gun.isTwoHanded ? 1 : 0, false);
        ableToFireVar.setTo(windupTimeCurrent == definition.gun.windupTime && cooldownTimeRemaining == 0 && !isReloading && (!isSemiAutoVar.isActive || !firedSinceRequested) ? 1 : 0, false);
        firingRequestedVar.setTo(playerHoldingTrigger ? 1 : 0, false);
    }

    @Override
    public void updateVariableModifiers() {
        //Yaw and pitch are special-case here since they're part of a struct VS a single variable.
        if (definition.variableModifiers != null) {
            for (JSONVariableModifier modifier : definition.variableModifiers) {
                switch (modifier.variable) {
                    case "gun_yaw":
                        internalOrientation.angles.y = adjustVariable(modifier, internalOrientation.angles.y);
                        break;
                    case "gun_pitch":
                        internalOrientation.angles.x = adjustVariable(modifier, internalOrientation.angles.x);
                        break;
                    default:
                    	ComputedVariable variable = getOrCreateVariable(modifier.variable);
                    	variable.setTo(adjustVariable(modifier, variable.currentValue), false);
                        break;
                }
            }
        }
    }

    /**
     * Helper method to calculate yaw/pitch movement.  Takes controller
     * look vector into account, as well as gun position.  Does not take
     * gun clamping into account as that's done in {@link #handleMovement(double, double)}
     */
    private void handleControl(IWrapperEntity controller) {
        //If the controller isn't a player, but is a NPC, make them look at the nearest hostile mob.
        //We also get a flag to see if the gun is currently pointed to the hostile mob.
        //If not, then we don't fire the gun, as that'd waste ammo.
        //Need to aim for the middle of the mob, not their base (feet).
        //Also make the gunner account for bullet delay and movement of the hostile.
        //This makes them track better when the target is moving.
        //We only do this
        if (!(controller instanceof IWrapperPlayer)) {
            //Get new target if we don't have one, or if we've gone 1 second and we have a closer target by 5 blocks.
            boolean checkForCloser = entityTarget != null && ticksExisted % 20 == 0;
            if (entityTarget == null || checkForCloser) {
                for (IWrapperEntity entity : world.getEntitiesHostile(controller, 48)) {
                    if (validateTarget(entity)) {
                        if (entityTarget != null) {
                            double distanceToBeat = position.distanceTo(entityTarget.getPosition());
                            if (checkForCloser) {
                                distanceToBeat += 5;
                            }
                            if (position.distanceTo(entity.getPosition()) > distanceToBeat) {
                                continue;
                            }
                        }
                        entityTarget = entity;
                    }
                }
            }

            //If we have a target, validate it and try to hit it.
            if (entityTarget != null) {
                if (validateTarget(entityTarget)) {
                    controllerAngles.set(targetVector).getAngles(true);
                    controller.setYaw(controllerAngles.y);
                    controller.setPitch(controllerAngles.x);

                    //Only fire if we're within 1 movement increment of the target.
                    if (Math.abs(targetAngles.y - internalOrientation.angles.y) < yawSpeed && Math.abs(targetAngles.x - internalOrientation.angles.x) < pitchSpeed) {
                        state = state.promote(GunState.FIRING_REQUESTED);
                    } else {
                        state = state.demote(GunState.CONTROLLED);
                    }
                } else {
                    entityTarget = null;
                    state = state.demote(GunState.CONTROLLED);
                }
            } else {
                state = state.demote(GunState.CONTROLLED);
            }
        } else {
            //Player-controlled gun.
            //Check for a target for this gun if we have a lock-on missile.
            //Only do this once every 1/2 second.
            //First, check if the loaded bullet is guided
            if (canLockTargetsVar.isActive || (lastLoadedBullet != null && lastLoadedBullet.definition.bullet.turnRate > 0)) {
                //We are the type of bullet to get a target, figure out if we need one, or we don't do auto-targeting.
                //If we do auto-target, we need to create a vector to look though.
                Point3D startPoint = null;
                Point3D searchVector = null;
                double coneAngle = 0;
                switch (definition.gun.lockOnType) {
                    case DEFAULT: {
                        //Default gets target based on controller eyes and where they are looking.
                        //Need to get their eye position though, not their main position, for accurate targeting.
                        //Also, don't use gun max distance here, since that's only for boresight.
                        startPoint = controller.getEyePosition();
                        searchVector = controller.getLineOfSight(RAYTRACE_DISTANCE);
                        coneAngle = DEFAULT_CONE_ANGLE;
                        break;
                    }
                    case BORESIGHT: {
                        //Boresight gets target based on gun position and barrel orientation, rather than player.
                        startPoint = position;
                        searchVector = new Point3D(0, 0, definition.gun.lockRange).rotate(orientation);
                        coneAngle = definition.gun.lockMaxAngle;
                        break;
                    }
                    case RADAR: {
                        //Set target here immediately.
                        break;
                    }
                    case MANUAL: {
                        //Don't do anything, we do this further on down in the code for all manual bullets.
                        //All we really need to do is make sure the target var is ready for operation.
                        if (targetPosition == null) {
                            targetPosition = new Point3D();
                        }
                        break;
                    }
                }

                //If we are the type of gun that needs to lock-on, try to do so now.
                //First set targets to null to clear any existing targets.
                engineTarget = null;
                entityTarget = null;

                //If we have a start point, it means we're a cone-based target system and need to find a target.
                if (startPoint != null) {
                    //First check for hard targets, since those are more dangerous.
                    if (definition.gun.targetType == TargetType.ALL || definition.gun.targetType == TargetType.HARD || definition.gun.targetType == TargetType.AIRCRAFT || definition.gun.targetType == TargetType.GROUND) {
                        normalizedConeVector.set(searchVector).normalize();
                        EntityVehicleF_Physics vehicleTarget = null;
                        double smallestDistance = searchVector.length();
                        for (EntityVehicleF_Physics vehicle : world.getEntitiesOfType(EntityVehicleF_Physics.class)) {
                            //Make sure we don't lock-on to our own vehicle.  Also, ensure if we want aircraft, or ground, we only get those.
                            if (vehicle != vehicleOn && (definition.gun.targetType != TargetType.AIRCRAFT || vehicle.definition.motorized.isAircraft) && (definition.gun.targetType != TargetType.GROUND || !vehicle.definition.motorized.isAircraft)) {
                                targetVector.set(vehicle.position).subtract(startPoint);
                                if (world.getBlockHit(startPoint, targetVector) == null) {
                                    double entityDistance = vehicle.position.distanceTo(startPoint);
                                    if (entityDistance < smallestDistance) {
                                        //Potential match by distance, check if the entity is inside the cone.
                                        normalizedEntityVector.set(vehicle.position).subtract(startPoint).normalize();
                                        double targetAngle = Math.abs(Math.toDegrees(Math.acos(normalizedConeVector.dotProduct(normalizedEntityVector, false))));
                                        if (targetAngle < coneAngle) {
                                            smallestDistance = entityDistance;
                                            vehicleTarget = vehicle;
                                        }
                                    }
                                }
                            }
                        }

                        //If we found a vehicle, get the engine to target.
                        if (vehicleTarget != null && !vehicleTarget.outOfHealth) {
                            for (APart part : vehicleTarget.parts) {
                                if (part instanceof PartEngine) {
                                    engineTarget = (PartEngine) part;
                                    break;
                                }
                            }
                        }
                    }

                    //If we didn't find a hard vehicle target, try and get a soft one.
                    if (engineTarget == null && definition.gun.targetType == TargetType.ALL || definition.gun.targetType == TargetType.SOFT) {
                        normalizedConeVector.set(searchVector).normalize();
                        double smallestDistance = searchVector.length();
                        BoundingBox searchBox = new BoundingBox(position, smallestDistance, smallestDistance, smallestDistance);
                        for (IWrapperEntity entity : world.getEntitiesWithin(searchBox)) {
                            if (entity.isValid() && entity != controller) {
                                targetVector.set(entity.getPosition()).subtract(startPoint);
                                if (world.getBlockHit(startPoint, targetVector) == null) {
                                    double entityDistance = entity.getPosition().distanceTo(startPoint);
                                    if (entityDistance < smallestDistance) {
                                        //Potential match by distance, check if the entity is inside the cone.
                                        normalizedEntityVector.set(entity.getPosition()).subtract(startPoint).normalize();
                                        double targetAngle = Math.abs(Math.toDegrees(Math.acos(normalizedConeVector.dotProduct(normalizedEntityVector, false))));
                                        if (targetAngle < coneAngle) {
                                            smallestDistance = entityDistance;
                                            entityTarget = entity;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //If we have any manual bullets, do tracking for them.
            if (!activeManualBullets.isEmpty()) {
                Point3D laserStart = controller.getEyePosition().copy();
                BlockHitResult laserHit = world.getBlockHit(laserStart, controller.getLineOfSight(2048));
                if (laserHit != null) {
                    targetPosition.set(laserHit.hitPosition);
                } else {
                    targetPosition.set(laserStart).add(controller.getLineOfSight(1024));
                }
            }

            //If we are holding the trigger, request to fire.
            if (firingRequestedVar.isActive) {
                state = state.promote(GunState.FIRING_REQUESTED);
            } else {
                state = state.demote(GunState.CONTROLLED);
            }
        }

        //Get the delta between our orientation and the player's orientation.
        if (lastControllerSeat != null) {
            controllerRelativeLookVector.computeVectorAngles(controller.getOrientation(), zeroReferenceOrientation);
            handleMovement(controllerRelativeLookVector.y - internalOrientation.angles.y, controllerRelativeLookVector.x - internalOrientation.angles.x);
            //If the seat is a part on us, or the seat has animations linked to us, adjust player rotations.
            //This is required to ensure this gun doesn't rotate forever.
            if (!lastControllerSeat.externalAnglesRotated.isZero() && lastControllerSeat.placementDefinition.animations != null) {
                boolean updateYaw = false;
                boolean updatePitch = false;
                for (JSONAnimationDefinition def : lastControllerSeat.placementDefinition.animations) {
                    if (def.variable.contains("gun_yaw")) {
                        updateYaw = true;
                    } else if (def.variable.contains("gun_pitch")) {
                        updatePitch = true;
                    }
                }
                if (updateYaw) {
                    lastControllerSeat.riderRelativeOrientation.angles.y -= (internalOrientation.angles.y - prevInternalOrientation.angles.y);

                }
                if (updatePitch) {
                    lastControllerSeat.riderRelativeOrientation.angles.x -= (internalOrientation.angles.x - prevInternalOrientation.angles.x);
                }
            }
        }
    }

    /**
     * Helper method to validate a target as possible for this gun.
     * Checks entity position relative to the gun, and if the entity
     * is behind any blocks.  Returns true if the target is valid.
     * Also sets {@link #targetVector} and {@link #targetAngles}
     */
    private boolean validateTarget(IWrapperEntity target) {
        if (target.isValid()) {
            //Get vector from gun center to target.
            //Target we aim for the middle, as it's more accurate.
            //We also take into account tracking for bullet speed.
            targetVector.set(target.getPosition());
            targetVector.y += target.getEyeHeight() / 2D;
            targetVector.subtract(position);

            //Transform vector to gun's coordinate system.
            //Get the angles the gun has to rotate to match the target.
            //If the are outside the gun's clamps, this isn't a valid target.
            targetAngles.set(targetVector).reOrigin(zeroReferenceOrientation).getAngles(true);

            //Check yaw, if we need to.
            if (minYaw != -180 || maxYaw != 180) {
                if (targetAngles.y < minYaw || targetAngles.y > maxYaw) {
                    return false;
                }
            }

            //Check pitch.
            if (targetAngles.x < minPitch || targetAngles.x > maxPitch) {
                return false;
            }

            //Check block raytracing.
            return world.getBlockHit(position, targetVector) == null;
        }
        return false;
    }

    /**
     * Helper method to do yaw/pitch movement.
     * Returns true if the movement was impeded by a clamp.
     * Only call this ONCE per update loop as it sets prev values.
     */
    private void handleMovement(double deltaYaw, double deltaPitch) {
        if (deltaYaw != 0 || deltaPitch != 0) {
            if (deltaYaw != 0) {
                //Adjust yaw.  We need to normalize the delta here as yaw can go past -180 to 180.
                if (deltaYaw < -180)
                    deltaYaw += 360;
                if (deltaYaw > 180)
                    deltaYaw -= 360;
                if (deltaYaw < 0) {
                    if (deltaYaw < -yawSpeed) {
                        deltaYaw = -yawSpeed;
                    }
                    internalOrientation.angles.y += deltaYaw;
                } else if (deltaYaw > 0) {
                    if (deltaYaw > yawSpeed) {
                        deltaYaw = yawSpeed;
                    }
                    internalOrientation.angles.y += deltaYaw;
                }

                //Apply yaw clamps.
                //If yaw is from -180 to 180, we are a gun that can spin around on its mount.
                //We need to do special logic for this type of gun.
                if (minYaw == -180 && maxYaw == 180) {
                    if (internalOrientation.angles.y > 180) {
                        internalOrientation.angles.y -= 360;
                        prevInternalOrientation.angles.y -= 360;
                    } else if (internalOrientation.angles.y < -180) {
                        internalOrientation.angles.y += 360;
                        prevInternalOrientation.angles.y += 360;
                    }
                } else {
                    if (internalOrientation.angles.y > maxYaw) {
                        internalOrientation.angles.y = maxYaw;
                    }
                    if (internalOrientation.angles.y < minYaw) {
                        internalOrientation.angles.y = minYaw;
                    }
                }
            }

            if (deltaPitch != 0) {
                //Adjust pitch.
                if (deltaPitch < 0) {
                    if (deltaPitch < -pitchSpeed) {
                        deltaPitch = -pitchSpeed;
                    }
                    internalOrientation.angles.x += deltaPitch;
                } else if (deltaPitch > 0) {
                    if (deltaPitch > pitchSpeed) {
                        deltaPitch = pitchSpeed;
                    }
                    internalOrientation.angles.x += deltaPitch;
                }

                //Apply pitch clamps.
                if (internalOrientation.angles.x > maxPitch) {
                    internalOrientation.angles.x = maxPitch;
                }
                if (internalOrientation.angles.x < minPitch) {
                    internalOrientation.angles.x = minPitch;
                }
            }
        }
    }

    /**
     * Attempts to reload the gun with the passed-in item.  Returns true if the item is a bullet
     * and was loaded, false if not.  Provider methods are then called for packet callbacks.
     */
    public boolean tryToReload(IWrapperItemStack stack, boolean swapIfFull) {
        //Check to make sure this is a bullet stack.
        AItemBase item = stack.getItem();
        if(item instanceof ItemBullet) {
            ItemBullet bulletItem = (ItemBullet) item;
            //Check to make sure this is a bullet, the gun isn't reloading and can accept reloads.
            if (bulletItem.definition.bullet != null) {
                //Check bullet parameters match.
                if (bulletItem.definition.bullet.diameter == definition.gun.diameter && bulletItem.definition.bullet.caseLength >= definition.gun.minCaseLength && bulletItem.definition.bullet.caseLength <= definition.gun.maxCaseLength) {
                    //If we are hand held with a partial capacity in us, and use clips, swap the clip out.
                    if (isHandHeld && loadedBulletCount > 0 && !definition.gun.isClipless) {
                        //First remove old clip.
                        IWrapperNBT data = InterfaceManager.coreInterface.getNewNBTWrapper();
                        data.setInteger(ItemBullet.BULLET_QTY_KEY, loadedBulletCount);
                        IWrapperItemStack bulletStack = loadedBullets.get(0).getNewStack(data);
                        clearBullets();
                        if (holdingPlayer.getInventory().addStack(bulletStack)) {
                            InterfaceManager.packetInterface.sendToAllClients(new PacketPartGun(this, PacketPartGun.Request.CLEAR_ONCLIENT));
                        } else {
                            //Can't add the clip to the player's inventory.
                            return false;
                        }
                    }

                    //Get quantity, which might be partial if we're using hand-held clips.
                    final int bulletQty;
                    if(isHandHeld) {
                        IWrapperNBT data = stack.getData();
                        if(data != null) {
                            bulletQty = data.getInteger(ItemBullet.BULLET_QTY_KEY);
                        }else {
                            bulletQty = bulletItem.definition.bullet.quantity;
                        }
                    }else {
                        bulletQty = bulletItem.definition.bullet.quantity;
                    }
                    
                    if (bulletQty + loadedBulletCount + reloadingBulletCount <= definition.gun.capacity) {
                        //Able to load, do so now (above check shouldn't matter for hand-helds since those always have capacity, but common code is common).
                        setReloadVars(bulletItem, bulletQty);
                        InterfaceManager.packetInterface.sendToAllClients(new PacketPartGun(this, bulletItem, bulletQty));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Sets reloading times and variables for a reload.
     */
    public void setReloadVars(ItemBullet bulletItem, int bulletQty) {
        if (reloadingBullets.isEmpty()) {
            //Set reload windup time to reload time since this is first reload of the type.
            reloadStartTimeRemaining = definition.gun.reloadStartTime;
        }
        reloadingBullets.add(bulletItem);
        reloadingBulletCounts.add(bulletQty);
        reloadingBulletCount += bulletQty;
        reloadMainTimeRemaining = definition.gun.reloadTime;
        reloadEndTimeRemaining = definition.gun.reloadEndTime;
        isReloading = true;
    }

    /**
     * Clears out the bullets in the gun by setting the appropriate variables.
     */
    public void clearBullets() {
        loadedBullets.clear();
        loadedBulletCounts.clear();
        loadedBulletCount = 0;
    }

    /**
     * Returns the text that represents the bullet loaded in this gun.
     */
    public String getBulletText() {
        if (loadedBulletCount > 0) {
            return lastLoadedBullet.getItemName() + "X" + loadedBulletCounts.get(0);
        } else {
            return "EMPTY";
        }
    }

    /**
     * Returns the controller for the gun.
     * The returned value may be a player riding the entity that this gun is on,
     * or perhaps a player in a seat that's on this gun.  May also be the player
     * hodling this gun if the gun is hand-held.
     */
    public IWrapperEntity getGunController() {
        //If the master entity we are on is destroyed, don't allow anything to control us.
        if (masterEntity.outOfHealth) {
            return null;
        }

        //Check if the entity we are on is a player-holding entity.
        if (isHandHeld) {
            return holdingPlayer;
        }

        //Check if our parent entity is a seat and has a rider.
        if (entityOn instanceof PartSeat && entityOn.rider != null) {
            return entityOn.rider;
        }

        //Check any child seats.  These take priority over global seats.
        for (APart part : parts) {
            if (part instanceof PartSeat && part.rider != null) {
                return part.rider;
            }
        }

        //Check any linked seats.
        //This also includes seats on us, and the seat we are on (if we are on one).
        for (PartSeat seat : seatsControllingGun) {
            if (seat.rider != null) {
                return seat.rider;
            }
        }
        return null;
    }

    /**
     * Common method to do knocback for guns.  Is either called directly on the server when the gun is fired, or via packet sent by the master-client.
     */
    public void performGunKnockback() {
        if (knockbackVar.currentValue != 0) {
            holdingPlayer.applyMotion(new Point3D(0, 0, 1).rotate(orientation).scale(-knockbackVar.currentValue));
        }
    }

    /**
     * Helper method to set the position and velocity of a bullet's spawn.
     * This is based on the passed-in muzzle, and the parameters of that muzzle.
     * Used in both spawning the bullet, and in rendering where the muzzle position is.
     */
    public void setBulletSpawn(Point3D bulletPosition, Point3D bulletVelocity, RotationMatrix bulletOrientation, JSONMuzzle muzzle, boolean addSpread) {
        //Set velocity.
        if (muzzleVelocityVar.currentValue != 0) {
            bulletVelocity.set(0, 0, muzzleVelocityVar.currentValue / 20D);
            //Randomize the spread for normal bullet and pellets
            if (addSpread) {
                if (lastLoadedBullet == null) {
                    if (bulletSpreadFactorVar.isActive) {
                        firingSpreadRotation.angles.set((randomGenerator.nextFloat() - 0.5F) * bulletSpreadFactorVar.currentValue, (randomGenerator.nextFloat() - 0.5F) * bulletSpreadFactorVar.currentValue, 0D);
                        bulletVelocity.rotate(firingSpreadRotation);
                    }
                } else {
                    if (bulletSpreadFactorVar.isActive || lastLoadedBullet.definition.bullet.pelletSpreadFactor > 0) {
                        firingSpreadRotation.angles.set((randomGenerator.nextFloat() - 0.5F) * (bulletSpreadFactorVar.currentValue + lastLoadedBullet.definition.bullet.pelletSpreadFactor), (randomGenerator.nextFloat() - 0.5F) * (bulletSpreadFactorVar.currentValue + lastLoadedBullet.definition.bullet.pelletSpreadFactor), 0D);
                        bulletVelocity.rotate(firingSpreadRotation);
                    }
                }
            }

            //Now that velocity is set, rotate it to match the gun's orientation.
            //For this, we get the reference orientation, and our internal orientation.
            if (muzzle.rot != null) {
                bulletVelocity.rotate(muzzle.rot);
            }
            bulletVelocity.rotate(internalOrientation).rotate(zeroReferenceOrientation);
        } else {
            bulletVelocity.set(0, 0, 0);
        }

        //Add gun velocity to bullet to ensure we spawn with the offset.
        if (!definition.gun.disableInheritedMotion) {
            if (vehicleOn != null) {
                bulletVelocity.addScaled(motion, vehicleOn.speedFactor);
            } else {
                bulletVelocity.add(motion);
            }
        }

        //Set position.
        bulletPosition.set(muzzle.pos);
        if (muzzle.center != null) {
            pitchMuzzleRotation.setToZero().rotateX(internalOrientation.angles.x);
            yawMuzzleRotation.setToZero().rotateY(internalOrientation.angles.y);
            bulletPosition.subtract(muzzle.center).rotate(pitchMuzzleRotation).add(muzzle.center).rotate(yawMuzzleRotation);
        } else {
            bulletPosition.rotate(internalOrientation);
        }
        bulletPosition.rotate(zeroReferenceOrientation).add(position);

        //Set orientation.
        bulletOrientation.set(zeroReferenceOrientation).multiply(internalOrientation);
        if (muzzle.rot != null && !definition.gun.disableMuzzleOrientation) {
            bulletOrientation.multiply(muzzle.rot);
        }
    }

    public double getLockedOnDirection(){
        double direction = 0;
        Point3D referencePos = vehicleOn != null ? vehicleOn.position : position;
        if (engineTarget != null) {
            direction = Math.toDegrees(Math.atan2(-engineTarget.vehicleOn.position.z + referencePos.z, -engineTarget.vehicleOn.position.x + referencePos.x)) + 90 + orientation.angles.y;
        } else if (entityTarget != null) {
            direction = Math.toDegrees(Math.atan2(-entityTarget.getPosition().z + referencePos.z, -entityTarget.getPosition().x + referencePos.x)) + 90 + orientation.angles.y;
        }
        while (direction < -180)
            direction += 360;
        while (direction > 180)
            direction -= 360;
        return direction;
    }

    public double getLockedOnAngle(){
        double angle = 0;
        Point3D referencePos = vehicleOn != null ? vehicleOn.position : position;
        if (engineTarget != null) {
            angle = -Math.toDegrees(Math.atan2(-engineTarget.position.y + referencePos.y,Math.hypot(-engineTarget.position.z + referencePos.z,-engineTarget.position.x + referencePos.x))) + orientation.angles.x;
        } else if (entityTarget != null) {
            angle = -Math.toDegrees(Math.atan2(-entityTarget.getPosition().y + referencePos.y,Math.hypot(-entityTarget.getPosition().z + referencePos.z,-entityTarget.getPosition().x + referencePos.x))) + orientation.angles.x;
        }
        while (angle < -180)
            angle += 360;
        while (angle > 180)
            angle -= 360;
        return angle;
    }

    public Point3D getLockedOnLeadPoint(){
        Point3D leadPoint = new Point3D();
        double ticksToTarget = 0;
        if (engineTarget != null) {
            ticksToTarget = engineTarget.vehicleOn.position.distanceTo(position) / (muzzleVelocityVar.currentValue / 20D);
            leadPoint.set(engineTarget.vehicleOn.position).addScaled(engineTarget.vehicleOn.motion, (engineTarget.vehicleOn.speedFactor) * ticksToTarget);
        } else if (entityTarget != null) {
            ticksToTarget = entityTarget.getPosition().distanceTo(position) / (knockbackVar.currentValue / 20D);
            leadPoint.set(entityTarget.getPosition()).addScaled(entityTarget.getVelocity(), ticksToTarget);
        }
        return leadPoint;
    }

    public double getLeadPointDirection(){
        double direction = 0;
        if (engineTarget != null || entityTarget != null) {
            direction = Math.toDegrees(Math.atan2(-getLockedOnLeadPoint().z + position.z, -getLockedOnLeadPoint().x + position.x)) + 90 + orientation.angles.y;
        }
        while (direction < -180)
            direction += 360;
        while (direction > 180)
            direction -= 360;
        return direction;
    }

    public double getLeadAngleY() {
        double angle = 0;
        Point3D referencePos = vehicleOn != null ? vehicleOn.position : position;
        if(engineTarget != null) {
            angle = (-Math.toDegrees(Math.atan2(-getLockedOnLeadPoint().y + position.y, Math.hypot(-getLockedOnLeadPoint().z + position.z, -getLockedOnLeadPoint().x + position.x))) + orientation.angles.x) - (-Math.toDegrees(Math.atan2(-engineTarget.position.y + referencePos.y, Math.hypot(-engineTarget.position.z + referencePos.z, -engineTarget.position.x + referencePos.x))) + orientation.angles.x);
        } else if (entityTarget != null) {
            angle = (-Math.toDegrees(Math.atan2(-getLockedOnLeadPoint().y + position.y, Math.hypot(-getLockedOnLeadPoint().z + position.z, -getLockedOnLeadPoint().x + position.x))) + orientation.angles.x) - (-Math.toDegrees(Math.atan2(-entityTarget.getPosition().y + referencePos.y, Math.hypot(-entityTarget.getPosition().z + referencePos.z, -entityTarget.getPosition().x + referencePos.x))) + orientation.angles.x);
        }
        return angle;
    }

    @Override
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        switch (variable) {
            case ("gun_inhand"):
                return new ComputedVariable(this, variable, partialTicks -> isHandHeld ? 1 : 0, false);
            case ("gun_inhand_sneaking"):
                return new ComputedVariable(this, variable, partialTicks -> isHandHeld && holdingPlayer.isSneaking() ? 1 : 0, false);
            case ("gun_inhand_aiming"):
                return new ComputedVariable(this, variable, partialTicks -> isHandHeldGunAimed ? 1 : 0, false);
            case ("gun_inhand_equipped"):
                return new ComputedVariable(this, variable, partialTicks -> isHandHeldGunEquipped && isValid ? 1 : 0, false);
            case ("gun_controller_firstperson"):
                return new ComputedVariable(this, variable, partialTicks -> InterfaceManager.clientInterface.getClientPlayer().equals(lastController) && InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON ? 1 : 0, false);
            case ("gun_active"):
                return new ComputedVariable(this, variable, partialTicks -> state.isAtLeast(GunState.CONTROLLED) ? 1 : 0, false);
            case ("gun_firing"):
                return new ComputedVariable(this, variable, partialTicks -> state.isAtLeast(GunState.FIRING_REQUESTED) ? 1 : 0, false);
            case ("gun_fired"):
                return new ComputedVariable(this, variable, partialTicks -> firedThisTick ? 1 : 0, false);
            case ("gun_muzzleflash"):
                return new ComputedVariable(this, variable, partialTicks -> firedThisTick && lastMillisecondFired + 25 < System.currentTimeMillis() ? 1 : 0, true);
            case ("gun_lockedon"):
                return new ComputedVariable(this, variable, partialTicks -> entityTarget != null || engineTarget != null ? 1 : 0, false);
            case ("gun_lockedon_x"):
                return new ComputedVariable(this, variable, partialTicks -> entityTarget != null ? entityTarget.getPosition().x : (engineTarget != null ? engineTarget.position.x : 0), false);
            case ("gun_lockedon_y"):
                return new ComputedVariable(this, variable, partialTicks -> entityTarget != null ? entityTarget.getPosition().y : (engineTarget != null ? engineTarget.position.y : 0), false);
            case ("gun_lockedon_z"):
                return new ComputedVariable(this, variable, partialTicks -> entityTarget != null ? entityTarget.getPosition().z : (engineTarget != null ? engineTarget.position.z : 0), false);
            case ("gun_lockedon_direction"):
                return new ComputedVariable(this, variable, partialTicks -> entityTarget != null ? getLockedOnDirection() : (engineTarget != null ? getLockedOnDirection() : 0), false);
            case ("gun_lockedon_angle"):
                return new ComputedVariable(this, variable, partialTicks -> entityTarget != null ? getLockedOnAngle() : (engineTarget != null ? getLockedOnAngle() : 0), false);
            case ("gun_lockedon_leadpoint_direction"):
                return new ComputedVariable(this, variable, partialTicks -> entityTarget != null ? getLeadPointDirection() : (engineTarget != null ? getLeadPointDirection() : 0), false);
            case ("gun_lockedon_leadpoint_angle"):
                return new ComputedVariable(this, variable, partialTicks -> entityTarget != null ? (-Math.toDegrees(Math.atan2(-getLockedOnLeadPoint().y + position.y, Math.hypot(-getLockedOnLeadPoint().z + position.z, -getLockedOnLeadPoint().x + position.x))) + orientation.angles.x) : (engineTarget != null ? (-Math.toDegrees(Math.atan2(-getLockedOnLeadPoint().y + position.y, Math.hypot(-getLockedOnLeadPoint().z + position.z, -getLockedOnLeadPoint().x + position.x))) + orientation.angles.x) : 0), false);
            case ("gun_lockedon_distance"):
                return new ComputedVariable(this, variable, partialTicks -> entityTarget != null ? entityTarget.getPosition().distanceTo(position) : (engineTarget != null ? engineTarget.position.distanceTo(position) : 0), false);
            case ("gun_lockedon_leadangle_x"):
                return new ComputedVariable(this, variable, partialTicks -> entityTarget != null ? (getLeadPointDirection() - getLockedOnDirection()) : (engineTarget != null ? (getLeadPointDirection() - getLockedOnDirection()) : 0), false);
            case ("gun_lockedon_leadangle_y"):
                return new ComputedVariable(this, variable, partialTicks -> entityTarget != null ? getLeadAngleY() : (engineTarget != null ? getLeadAngleY() : 0), false);
            case ("gun_pitch"):
                return new ComputedVariable(this, variable, partialTicks -> partialTicks != 0 ? prevInternalOrientation.angles.x + (internalOrientation.angles.x - prevInternalOrientation.angles.x) * partialTicks : internalOrientation.angles.x, true);
            case ("gun_yaw"):
                return new ComputedVariable(this, variable, partialTicks -> partialTicks != 0 ? prevInternalOrientation.angles.y + (internalOrientation.angles.y - prevInternalOrientation.angles.y) * partialTicks : internalOrientation.angles.y, true);
            case ("gun_pitching"):
                return new ComputedVariable(this, variable, partialTicks -> Math.abs(prevInternalOrientation.angles.x - internalOrientation.angles.x) > 0.01 ? 1 : 0, false);
            case ("gun_yawing"):
                return new ComputedVariable(this, variable, partialTicks -> Math.abs(prevInternalOrientation.angles.y - internalOrientation.angles.y) > 0.01 ? 1 : 0, false);
            case ("gun_cooldown"):
                return new ComputedVariable(this, variable, partialTicks -> cooldownTimeRemaining > 0 ? 1 : 0, false);
            case ("gun_windup_time"):
                return new ComputedVariable(this, variable, partialTicks -> windupTimeCurrent, false);
            case ("gun_windup_rotation"):
                return new ComputedVariable(this, variable, partialTicks -> windupRotation, false);
            case ("gun_windup_complete"):
                return new ComputedVariable(this, variable, partialTicks -> windupTimeCurrent == definition.gun.windupTime ? 1 : 0, false);
            case ("gun_reload"):
                return new ComputedVariable(this, variable, partialTicks -> isReloading ? 1 : 0, false);
            case ("gun_reload_windup"):
                return new ComputedVariable(this, variable, partialTicks -> isReloading && reloadStartTimeRemaining > 0 ? 1 : 0, false);
            case ("gun_reload_main"):
                return new ComputedVariable(this, variable, partialTicks -> isReloading && reloadStartTimeRemaining == 0 && reloadMainTimeRemaining > 0 ? 1 : 0, false);
            case ("gun_reload_winddown"):
                return new ComputedVariable(this, variable, partialTicks -> isReloading && reloadStartTimeRemaining == 0 && reloadMainTimeRemaining == 0 && reloadEndTimeRemaining > 0 ? 1 : 0, false);
            case ("gun_ammo_count"):
                return new ComputedVariable(this, variable, partialTicks -> loadedBulletCount, false);
            case ("gun_ammo_count_reloading"):
                return new ComputedVariable(this, variable, partialTicks -> reloadingBulletCount, false);
            case ("gun_ammo_percent"):
                return new ComputedVariable(this, variable, partialTicks -> loadedBulletCount / definition.gun.capacity, false);
            case ("gun_active_muzzlegroup"):
                return new ComputedVariable(this, variable, partialTicks -> currentMuzzleGroupIndex + 1, false);
            case ("gun_bullet_present"):
                return new ComputedVariable(this, variable, partialTicks -> currentBullet != null ? 1 : 0, false);
            case ("gun_bullet_x"):
                return new ComputedVariable(this, variable, partialTicks -> currentBullet != null ? currentBullet.getRelativePos(1, partialTicks) : 0, true);
            case ("gun_bullet_y"):
                return new ComputedVariable(this, variable, partialTicks -> currentBullet != null ? currentBullet.getRelativePos(2, partialTicks) : 0, true);
            case ("gun_bullet_z"):
                return new ComputedVariable(this, variable, partialTicks -> currentBullet != null ? currentBullet.getRelativePos(3, partialTicks) : 0, true);
            case ("gun_bullet_yaw"):
                return new ComputedVariable(this, variable, partialTicks -> currentBullet != null ? currentBullet.orientation.angles.y - orientation.angles.y : 0, false);
            case ("gun_bullet_pitch"):
                return new ComputedVariable(this, variable, partialTicks -> currentBullet != null ? currentBullet.orientation.angles.x - orientation.angles.x : 0, false);
            default:
                if (variable.startsWith("gun_ammo_") && variable.endsWith("_loaded")) {
                    final int index = Integer.parseInt(variable.substring("gun_ammo_".length(), "gun_ammo_".length() + 1));
                    return new ComputedVariable(this, variable, partialTicks -> loadedBulletCount >= index ? 1 : 0, false);
                } else {
                    return super.createComputedVariable(variable, createDefaultIfNotPresent);
                }
        }
    }

    @Override
    public String getRawTextVariableValue(JSONText textDef, float partialTicks) {
        if (textDef.variableName.equals("gun_lockedon_name")) {
            return entityTarget != null ? entityTarget.getName() : (engineTarget != null ? engineTarget.masterEntity.toString() : "");
        }

        return super.getRawTextVariableValue(textDef, partialTicks);
    }

    @Override
    public void renderBoundingBoxes(TransformationMatrix transform) {
        if (entityOn.isVariableListTrue(placementDefinition.interactableVariables)) {
            super.renderBoundingBoxes(transform);
            //Draw the gun muzzle bounding boxes.
            for (JSONMuzzle muzzle : definition.gun.muzzleGroups.get(currentMuzzleGroupIndex).muzzles) {
                setBulletSpawn(bulletPositionRender, bulletVelocityRender, bulletOrientationRender, muzzle, false);
                new BoundingBox(bulletPositionRender, 0.25, 0.25, 0.25).renderWireframe(this, transform, null, ColorRGB.BLUE);
            }
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setInteger("state", (byte) state.ordinal());
        data.setInteger("bulletsFired", bulletsFired);
        data.setInteger("currentMuzzleGroupIndex", currentMuzzleGroupIndex);
        data.setPoint3d("internalAngles", internalOrientation.angles);
        for (int i = 0; i < loadedBullets.size(); ++i) {
            IWrapperNBT bulletData = InterfaceManager.coreInterface.getNewNBTWrapper();
            bulletData.setPackItem(loadedBullets.get(i).definition, "");
            bulletData.setInteger("count", loadedBulletCounts.get(i));
            data.setData("loadedBullet" + i, bulletData);
        }
        data.setInteger("loadedBulletsSize", loadedBullets.size());
        for (int i = 0; i < reloadingBullets.size(); ++i) {
            IWrapperNBT bulletData = InterfaceManager.coreInterface.getNewNBTWrapper();
            bulletData.setPackItem(reloadingBullets.get(i).definition, "");
            bulletData.setInteger("count", reloadingBulletCounts.get(i));
            data.setData("reloadingBullet" + i, bulletData);
        }
        data.setInteger("reloadingBulletsSize", reloadingBullets.size());
        data.setInteger("reloadStartTimeRemaining", reloadStartTimeRemaining);
        data.setInteger("reloadMainTimeRemaining", reloadMainTimeRemaining);
        data.setInteger("reloadEndTimeRemaining", reloadEndTimeRemaining);
        long randomSeed = randomGenerator.nextLong();
        data.setBoolean("savedSeed", true);
        data.setInteger("randomSeedPart1", (int) (randomSeed >> 32));
        data.setInteger("randomSeedPart2", (int) randomSeed);
        return data;
    }

    public enum GunState {
        INACTIVE,
        ACTIVE,
        CONTROLLED,
        FIRING_REQUESTED,
        FIRING_CURRENTLY;

        public GunState promote(GunState newState) {
            return newState.ordinal() > this.ordinal() ? newState : this;
        }

        public GunState demote(GunState newState) {
            return newState.ordinal() < this.ordinal() ? newState : this;
        }

        public boolean isAtLeast(GunState testState) {
            return this.ordinal() >= testState.ordinal();
        }
    }
}
