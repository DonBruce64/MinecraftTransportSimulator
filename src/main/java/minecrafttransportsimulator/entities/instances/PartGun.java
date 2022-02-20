package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.jsondefs.JSONMuzzle;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Basic gun class class.  This class is responsible for representing a gun in the world.  This gun
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
public class PartGun extends APart{
	//Variables based on the specific gun's properties.
	private final double minYaw;
	private final double maxYaw;
	private final double minPitch;
	private final double maxPitch;
	private final double defaultYaw;
	private final double defaultPitch;
	private final long millisecondFiringDelay;
	private final AItemPart gunItem;
	
	//Stored variables used to determine bullet firing behavior.
	protected int bulletsFired;
	private int bulletsLeft;
	private int bulletsReloading;
	private int bulletsRemovedThisRequest;
	//TODO make this private when rendering goes in-class.
	public int currentMuzzleGroupIndex;
	private final RotationMatrix internalOrientation;
	private final RotationMatrix prevInternalOrientation;
	protected ItemBullet loadedBullet;
	
	//These variables are used during firing and will be reset on loading.
	public GunState state;
	public boolean firedThisRequest;
	public boolean firedThisCheck;
	public boolean playerHoldingTrigger;
	public boolean isHandHeldGunAimed;
	private int ticksFiring;
	private int reloadTimeRemaining;
	private long millisecondLastTimeFired;
	private int windupTimeCurrent;
	private int windupRotation;
	public WrapperEntity lastController;
	private WrapperEntity entityTarget;
	private PartEngine engineTarget;
	private long millisecondCamOffset;
	private long lastTimeFired;
	private final Point3D bulletPosition = new Point3D();
	private final Point3D bulletVelocity = new Point3D();
	private final RotationMatrix bulletOrientation = new RotationMatrix();
	
	//Temp helper variables for calculations
	private final Point3D targetVector = new Point3D();
	private final Point3D targetAngles = new Point3D();
	private final RotationMatrix firingSpreadRotation = new RotationMatrix();
	
	//Global data.
	public final List<Integer> bulletsHitOnServer = new ArrayList<Integer>();
	public static final int RAYTRACE_DISTANCE = 750;
		
	public PartGun(AEntityF_Multipart<?> entityOn, WrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placingPlayer, placementDefinition, data, parentPart);
		
		//Set min/max yaw/pitch angles based on our definition and the entity definition.
		//If the entity definition min/max yaw is -180 to 180, set it to that.  Otherwise, get the max bounds.
		//Yaw/Pitch set to 0 is ignored as it's assumed to be un-defined.
		if(placementDefinition.minYaw == -180 && placementDefinition.maxYaw == 180){
			this.minYaw = -180;
			this.maxYaw = 180;
		}else{
			if(definition.gun.minYaw != 0){
				this.minYaw = placementDefinition.minYaw != 0 ? Math.max(definition.gun.minYaw, placementDefinition.minYaw) : definition.gun.minYaw;
			}else{
				this.minYaw =  placementDefinition.minYaw;
			}
			if(definition.gun.maxYaw != 0){
				this.maxYaw = placementDefinition.maxYaw != 0 ? Math.min(definition.gun.maxYaw, placementDefinition.maxYaw) : definition.gun.maxYaw;
			}else{
				this.maxYaw =  placementDefinition.maxYaw;
			}
		}
		//Swap min and max pitch.  In JSON, negative values are down and positive up.
		//But for us, positive is down and negative is up.
		if(definition.gun.minPitch != 0){
			this.minPitch = placementDefinition.maxPitch != 0 ? -Math.max(definition.gun.maxPitch, placementDefinition.maxPitch) : -definition.gun.maxPitch;
		}else{
			this.minPitch = -placementDefinition.maxPitch;
		}	
		if(definition.gun.minPitch != 0){
			this.maxPitch = placementDefinition.minPitch != 0 ? -Math.min(definition.gun.minPitch, placementDefinition.minPitch) : -definition.gun.minPitch;
		}else{
			this.maxPitch = -placementDefinition.minPitch;
		}
		if(placementDefinition.defaultYaw != 0 && placementDefinition.defaultYaw >= minYaw && placementDefinition.defaultYaw <= maxYaw){
			this.defaultYaw = placementDefinition.defaultYaw;
		}else{
			this.defaultYaw = definition.gun.defaultYaw;
		}
		if(placementDefinition.defaultPitch != 0 && -placementDefinition.defaultPitch >= minPitch && -placementDefinition.defaultPitch <= maxPitch){
			this.defaultPitch = -placementDefinition.defaultPitch;
		}else{
			this.defaultPitch = -definition.gun.defaultPitch;
		}
		this.millisecondFiringDelay = (long) (definition.gun.fireDelay*50);
		this.gunItem = getItem();
		
		//Load saved data.
		this.state = GunState.values()[data.getInteger("state")];
		this.bulletsFired = data.getInteger("shotsFired");
		this.bulletsLeft = data.getInteger("bulletsLeft");
		this.bulletsReloading = data.getInteger("bulletsReloading");
		this.currentMuzzleGroupIndex = data.getInteger("currentMuzzleGroupIndex");
		this.internalOrientation = new RotationMatrix().setToAngles(data.getPoint3d("internalAngles"));
		this.prevInternalOrientation = new RotationMatrix().set(internalOrientation);
		String loadedBulletPack = data.getString("loadedBulletPack");
		String loadedBulletName = data.getString("loadedBulletName");
		if(!loadedBulletPack.isEmpty()){
			this.loadedBullet = PackParserSystem.getItem(loadedBulletPack, loadedBulletName);
		}
		//If we didn't load the bullet due to pack changes, set the current bullet count to 0.
		//This prevents pack changes from locking guns.
		if(loadedBullet == null){
			bulletsLeft = 0;
		}
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		//Check to see if we have any bullets in our hands.
		//If so, try to re-load this gun with them.
		AItemBase heldItem = player.getHeldItem();
		if(heldItem instanceof ItemBullet){
			if(tryToReload((ItemBullet) heldItem) && !player.isCreative()){
				player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
			}
		}
		return true;
    }
	
	@Override
	public void update(){
		//Set gun state and do updates.
		prevInternalOrientation.set(internalOrientation);
		if(isActive && !placementDefinition.isSpare){
			//Check if we have a controller.
			//We aren't making sentry turrets here.... yet.
			WrapperEntity controller = getGunController();
			if(controller != null){
				lastController = controller;
				if(entityOn instanceof EntityPlayerGun){
					state = state.promote(GunState.CONTROLLED);
				}else{
					//If this gun type can only have one selected at a time, check that this has the selected index.
					PartSeat controllerSeat = entityOn.getSeatForRider(controller);
					if(gunItem.equals(controllerSeat.activeGun) && (!definition.gun.fireSolo || entityOn.partsByItem.get(gunItem).get(controllerSeat.gunIndex).equals(this))){
						state = state.promote(GunState.CONTROLLED);
					}else{
						state = state.demote(GunState.ACTIVE);
						controller = null;
						entityTarget = null;
						engineTarget = null;
					}
				}
			}
			if(controller == null){
				//If we aren't being controlled, check if we have any coaxial guns.
				//If we do, and they have a controller, then we use that as our controller.
				//This allows them to control this gun without being the actual controller for firing.
				if(!childParts.isEmpty()){
					for(APart part : childParts){
						if(part instanceof PartGun && part.placementDefinition.isCoAxial){
							controller = ((PartGun) part).getGunController();
							if(controller != null){
								state = state.promote(GunState.CONTROLLED);
								break;
							}
						}
					}
				}
				if(controller == null){
					state = state.demote(GunState.ACTIVE);
				}
			}
			
			//Adjust yaw and pitch to the direction of the controller.
			if(state.isAtLeast(GunState.CONTROLLED)){
				handleControl(controller);
			}
			
			//Set final gun active state and variables.
			boolean ableToFire = windupTimeCurrent == definition.gun.windupTime && bulletsLeft > 0 && (!definition.gun.isSemiAuto || !firedThisRequest);
			if(ableToFire && state.isAtLeast(GunState.FIRING_REQUESTED)){
				//Set firing to true if we aren't firing, and we've waited long enough since the last firing command.
				//If we don't wait, we can bypass the cooldown by toggling the trigger.
				long timeSinceFiring = System.currentTimeMillis() - lastTimeFired;
				if(!state.isAtLeast(GunState.FIRING_CURRENTLY) && timeSinceFiring >= millisecondFiringDelay){
					List<APart> allGuns = entityOn.partsByItem.get(gunItem);
					//Check if we have a primary gun.  If so, we may need to adjust cams to resume the firing sequence.
					int sequenceIndex = allGuns.indexOf(this);
					APart lastPrimaryPart = entityOn.lastPrimaryPart.get(gunItem);
					if(lastPrimaryPart != null){
						sequenceIndex = sequenceIndex - 1 - allGuns.indexOf(lastPrimaryPart);
						if(sequenceIndex < 0){
							sequenceIndex += allGuns.size();
						}
					}

					state = state.promote(GunState.FIRING_CURRENTLY);
					millisecondCamOffset = definition.gun.fireSolo ? 0 : millisecondFiringDelay*sequenceIndex/allGuns.size();
					lastTimeFired = System.currentTimeMillis() + millisecondCamOffset;
					//For clients, we offset the time fired back one cycle, so we can spawn the first bullet.
					//This will be set current in the particle spawning logic.
					if(world.isClient()){
						lastTimeFired -= millisecondFiringDelay;
					}
				}
			}else if(!ableToFire){
				state = state.demote(GunState.FIRING_REQUESTED);
				ticksFiring = 0;
				if(!state.isAtLeast(GunState.FIRING_REQUESTED)){
					firedThisRequest = false;
				}
			}
			
			//If we are on the server, check if we are firing and can remove a bullet this tick.
			//This uses a total time of firing to account for partial tick firing rates on clients.
			if(!world.isClient()){
				if(state.isAtLeast(GunState.FIRING_CURRENTLY)){
					int bulletsToRemove = definition.gun.isSemiAuto ? 1 : (int) ((++ticksFiring + definition.gun.fireDelay - millisecondCamOffset/50)/definition.gun.fireDelay - bulletsRemovedThisRequest);
					if(bulletsToRemove > 0){
						//Need to take muzzle count into account.
						bulletsToRemove *= definition.gun.muzzleGroups.get(currentMuzzleGroupIndex).muzzles.size();
						firedThisRequest = true;
						if(!ConfigSystem.configObject.clientControls.devMode.value)bulletsLeft -= bulletsToRemove;
						bulletsRemovedThisRequest += bulletsToRemove;
						bulletsFired += bulletsToRemove;
						entityOn.lastPrimaryPart.put(gunItem, this);
						millisecondLastTimeFired = System.currentTimeMillis();
						if(definition.gun.muzzleGroups.size() == ++currentMuzzleGroupIndex){
							currentMuzzleGroupIndex = 0;
						}
						if(bulletsLeft <= 0){
							bulletsLeft = 0;
							loadedBullet = null;
						}
					}
				}else{
					bulletsRemovedThisRequest = 0;
				}
			}
			
			//If we can accept bullets, and aren't currently loading any, re-load ourselves from any inventories.
			//While the reload method checks for reload time, we check here to save on code processing.
			//No sense in looking for bullets if we can't load them anyways.
			if(!world.isClient() && bulletsLeft < definition.gun.capacity && bulletsReloading == 0){
				if(entityOn instanceof EntityPlayerGun){
					if(definition.gun.autoReload || bulletsLeft == 0){
						//Check the player's inventory for bullets.
						WrapperInventory inventory = ((WrapperPlayer) lastController).getInventory();
						for(int i=0; i<inventory.getSize(); ++i){
							WrapperItemStack stack = inventory.getStack(i);
							AItemBase item = stack.getItem();
							if(item instanceof ItemBullet){
								if(tryToReload((ItemBullet) item)){
									//Bullet is right type, and we can fit it.  Remove from player's inventory and add to the gun.
									if(!ConfigSystem.configObject.clientControls.devMode.value)inventory.removeFromSlot(i, 1);
									break;
								}
							}
						}
					}
				}else{
					if(definition.gun.autoReload){
						//Iterate through all the inventory slots in crates to try to find matching ammo.
						for(APart part : entityOn.parts){
							if(part instanceof PartInteractable && part.definition.interactable.interactionType.equals(InteractableComponentType.CRATE) && part.isActive && part.definition.interactable.feedsVehicles){
								EntityInventoryContainer inventory = ((PartInteractable) part).inventory;
								for(int i=0; i<inventory.getSize(); ++i){
									WrapperItemStack stack = inventory.getStack(i);
									AItemBase item = stack.getItem();
									if(item instanceof ItemBullet){
										if(tryToReload((ItemBullet) item)){
											//Bullet is right type, and we can fit it.  Remove from crate and add to the gun.
											//Return here to ensure we don't set the loadedBullet to blank since we found bullets.
											if(!ConfigSystem.configObject.clientControls.devMode.value)inventory.removeFromSlot(i, 1);
											break;
										}
									}
								}
							}
						}
					}
				}
			}
			
			//If we are reloading, decrement the reloading timer.
			//If we are done reloading, add the new bullets.
			//This comes after the reloading block as we need a 0/1 state-change for the various animations,
			//so at some point the reload time needs to hit 0.
			if(reloadTimeRemaining > 0){
				--reloadTimeRemaining;
			}else if(bulletsReloading != 0){
				bulletsLeft += bulletsReloading;
				bulletsReloading = 0;
			}
		}else{
			//Inactive gun, set as such and set to default position if we have one.
			state = GunState.INACTIVE;
			entityTarget = null;
			engineTarget = null;
			if(definition.gun.resetPosition){
				handleMovement(defaultYaw - internalOrientation.angles.y, defaultPitch - internalOrientation.angles.x);
			}
		}
		
		
		//Increment or decrement windup.
		//This is done outside the main active area as windup can wind-down on deactivated guns.
		if(state.isAtLeast(GunState.FIRING_REQUESTED)){
			if(windupTimeCurrent < definition.gun.windupTime){
				++windupTimeCurrent;
			}
		}else if(windupTimeCurrent > 0){
			--windupTimeCurrent;
		}
		windupRotation += windupTimeCurrent;
		
		//Reset fire command bit if we aren't firing.
		if(!state.isAtLeast(GunState.FIRING_REQUESTED)){
			firedThisRequest = false;
		}
		
		//Now run super.  Firing check reset needs to happen after this call as the firing gets
		//done by the rendering system for particle spawning and will be set true here on call.
		super.update();
		firedThisCheck = false;
	}
	
	/**
	 * Helper method to calculate yaw/pitch movement.  Takes controller
	 * look vector into account, as well as gun position.  Does not take
	 * gun clamping into account as that's done in {@link #handleMovement(double, double)} 
	 */
	private void handleControl(WrapperEntity controller){
		//If the controller isn't a player, but is a NPC, make them look at the nearest hostile mob.
		//We also get a flag to see if the gun is currently pointed to the hostile mob.
		//If not, then we don't fire the gun, as that'd waste ammo.
		//Need to aim for the middle of the mob, not their base (feet).
		//Also make the gunner account for bullet delay and movement of the hostile.
		//This makes them track better when the target is moving.
		//We only do this 
		if(!(controller instanceof WrapperPlayer)){
			//Get new target if we don't have one, or if we've gone 1 second and we have a closer target by 5 blocks.
			boolean checkForCloser = entityTarget != null && ticksExisted%20 == 0;
			if(entityTarget == null || checkForCloser){
				for(WrapperEntity entity : world.getEntitiesHostile(controller, 48)){
					if(validateTarget(entity)){
						if(entityTarget != null){
							double distanceToBeat = position.distanceTo(entityTarget.getPosition());
							if(checkForCloser){
								distanceToBeat += 5;
							}
							if(position.distanceTo(entity.getPosition()) > distanceToBeat){
								continue;
							}
						}
						entityTarget = entity;
					}
				}
			}
			
			//If we have a target, validate it and try to hit it.
			if(entityTarget != null){
				if(validateTarget(entityTarget)){
					controller.setYaw(targetAngles.y);
					controller.setPitch(targetAngles.x);
					//Only fire if we're within 1 movement increment of the target.
					if(Math.abs(targetAngles.y - internalOrientation.angles.y) < definition.gun.yawSpeed && Math.abs(targetAngles.x - internalOrientation.angles.x) < definition.gun.pitchSpeed){
						state = state.promote(GunState.FIRING_REQUESTED);
					}else{
						state = state.demote(GunState.CONTROLLED);
					}
				}else{
					entityTarget = null;
					state = state.demote(GunState.CONTROLLED);
				}
			}else{
				state = state.demote(GunState.CONTROLLED);
			}
		}else{
			//Player-controlled gun.
			//If we are on a client, check for a target for this gun if we have a lock-on missile.
			//Only do this once every 1/2 second.
			if(world.isClient() && loadedBullet != null && loadedBullet.definition.bullet.turnRate > 0){
				//Try to find the entity the controller is looking at.
				entityTarget = world.getEntityLookingAt(controller, RAYTRACE_DISTANCE, true);
				if(entityTarget == null){
					engineTarget = null;
					EntityVehicleF_Physics vehicleTargeted = world.getRaytraced(EntityVehicleF_Physics.class, controller.getPosition(), controller.getPosition().copy().add(controller.getLineOfSight(RAYTRACE_DISTANCE)), true, vehicleOn);
					if(vehicleTargeted != null){
						for(APart part : vehicleTargeted.parts){
							if(part instanceof PartEngine){
								engineTarget = (PartEngine) part;
								break;
							}
						}
					}
				}
			}
			
			//If we are holding the trigger, request to fire.
			if(playerHoldingTrigger){
				state = state.promote(GunState.FIRING_REQUESTED);
			}else{
				state = state.demote(GunState.CONTROLLED);
			}
		}
	
		//Get the delta between our orientation and the player's orientation.
		if(!(entityOn instanceof EntityPlayerGun)){
			handleMovement(controller.getYaw() - internalOrientation.angles.y, controller.getPitch() - internalOrientation.angles.x);
		}
	}
	
	/**
	 * Helper method to validate a target as possible for this gun.
	 * Checks entity position relative to the gun, and if the entity
	 * is behind any blocks.  Returns true if the target is valid.
	 * Also sets {@link #targetVector} and {@link #targetAngles}
	 */
	private boolean validateTarget(WrapperEntity target){
		if(target.isValid()){
			//Get vector from eyes of controller to target.
			//Target we aim for the middle, as it's more accurate.
			//We also take into account tracking for bullet speed.
			targetVector.set(target.getPosition());
			targetVector.y += target.getEyeHeight()/2D;
			double ticksToTarget = target.getPosition().distanceTo(position)/definition.gun.muzzleVelocity/20D/10D;
			targetVector.add(target.getVelocity().scale(ticksToTarget)).subtract(position);
			
			//Transform vector to gun's coordinate system.
			//Get the angles the gun has to rotate to match the target.
			//If the are outside the gun's clamps, this isn't a valid target.
			targetAngles.set(targetVector).reOrigin(zeroReferenceOrientation).getAngles(true);
			
			//Check yaw, if we need to.
			if(minYaw != -180 || maxYaw != 180){
				if(targetAngles.y < minYaw || targetAngles.y > maxYaw){
					return false;
				}
			}
			
			//Check pitch.
			if(targetAngles.x < minPitch || targetAngles.x > maxPitch){
				return false;
			}
			
			//Check block raytracing.
			if(world.getBlockHit(position, targetVector) == null){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Helper method to do yaw/pitch movement.
	 * Returns true if the movement was impeded by a clamp.
	 * Only call this ONCE per update loop as it sets prev values.
	 */
	private void handleMovement(double deltaYaw, double deltaPitch){
		if(deltaYaw != 0 || deltaPitch != 0){
			if(deltaYaw != 0){
				//Adjust yaw.  We need to normalize the delta here as yaw can go past -180 to 180.
				if(deltaYaw < -180)deltaYaw += 360;
				if(deltaYaw > 180)deltaYaw -= 360;
				if(deltaYaw < 0){
					if(deltaYaw < -definition.gun.yawSpeed){
						deltaYaw = -definition.gun.yawSpeed;
					}
					internalOrientation.angles.y += deltaYaw; 
				}else if(deltaYaw > 0){
					if(deltaYaw > definition.gun.yawSpeed){
						deltaYaw = definition.gun.yawSpeed;
					}
					internalOrientation.angles.y += deltaYaw;
				}
				
				//Apply yaw clamps.
				//If yaw is from -180 to 180, we are a gun that can spin around on its mount.
				//We need to do special logic for this type of gun.
				if(minYaw == -180  && maxYaw == 180){
					if(internalOrientation.angles.y > 180 ){
						internalOrientation.angles.y -= 360;
						prevInternalOrientation.angles.y -= 360;
					}else if(internalOrientation.angles.y < -180){
						internalOrientation.angles.y += 360;
						prevInternalOrientation.angles.y += 360;
					}
				}else{
					if(internalOrientation.angles.y > maxYaw){
						internalOrientation.angles.y = maxYaw;
					}
					if(internalOrientation.angles.y < minYaw){
						internalOrientation.angles.y = minYaw;
					}
				}
			}
			
			if(deltaPitch != 0){
				//Adjust pitch.
				if(deltaPitch < 0){
					if(deltaPitch < -definition.gun.pitchSpeed){
						deltaPitch = -definition.gun.pitchSpeed;
					}
					internalOrientation.angles.x += deltaPitch; 
				}else if(deltaPitch > 0){
					if(deltaPitch > definition.gun.pitchSpeed){
						deltaPitch = definition.gun.pitchSpeed;
					}
					internalOrientation.angles.x += deltaPitch;
				}
				
				//Apply pitch clamps.
				if(internalOrientation.angles.x > maxPitch){
					internalOrientation.angles.x = maxPitch;
				}
				if(internalOrientation.angles.x < minPitch){
					internalOrientation.angles.x = minPitch;
				}
			}
		}
	}
	
	/**
	 * Attempts to reload the gun with the passed-in item.  Returns true if the item is a bullet
	 * and was loaded, false if not.  Provider methods are then called for packet callbacks.
	 */
	public boolean tryToReload(ItemBullet item){
		//Only fill bullets if we match the bullet already in the gun, or if our diameter matches, or if we got a signal on the client.
		//Also don't fill bullets if we are currently reloading bullets.
		if(item.definition.bullet != null){
			boolean isNewBulletValid = item.definition.bullet.diameter == definition.gun.diameter && item.definition.bullet.caseLength >= definition.gun.minCaseLength && item.definition.bullet.caseLength <= definition.gun.maxCaseLength; 
			if((bulletsReloading == 0 && (loadedBullet == null ? isNewBulletValid : loadedBullet.equals(item))) || world.isClient()){
				//Make sure we don't over-fill the gun.
				if(item.definition.bullet.quantity + bulletsLeft <= definition.gun.capacity || world.isClient()){
					loadedBullet = item;
					bulletsReloading = item.definition.bullet.quantity;
					reloadTimeRemaining = definition.gun.reloadTime;
					if(!world.isClient()){
						InterfacePacket.sendToAllClients(new PacketPartGun(this, loadedBullet));
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 *  Returns the controller for the gun.
	 *  The returned value may be a player riding the entity that this gun is on,
	 *  or perhaps a player in a seat that's on this gun.  May also be the player
	 *  hodling this gun if the gun is hand-held.
	 */
	public WrapperEntity getGunController(){
		//If the entity we are on is destroyed, don't allow anything to control us.
		if(entityOn.damageAmount == entityOn.definition.general.health){
			return null;
		}
		
		//Check if the entity we are on is a player-holding entity.
		if(entityOn instanceof EntityPlayerGun){
			return ((EntityPlayerGun) entityOn).player;
		}
		
		//Check our parent part, if we have one.
		if(parentPart instanceof PartSeat){
			return entityOn.locationRiderMap.get(parentPart.placementOffset);
		}
		
		//Check any child parts.
		for(APart childPart : childParts){
			if(childPart instanceof PartSeat){
				return entityOn.locationRiderMap.get(childPart.placementOffset);
			}
		}
		
		//Not parent or child.  Get main vehicle controller if we have one.
		return entityOn.getController();
	}
	
	/**
	 * Helper method to set the position and velocity of a bullet's spawn.
	 * This is based on the passed-in muzzle, and the parameters of that muzzle.
	 * Used in both spawning the bullet, and in rendering where the muzzle position is.
	 */
	public void setBulletSpawn(Point3D bulletPosition, Point3D bulletVelocity, RotationMatrix bulletOrientation, JSONMuzzle muzzle){		
		//Set velocity.
		if(definition.gun.muzzleVelocity != 0){
			bulletVelocity.set(0, 0, definition.gun.muzzleVelocity/20D/10D);
			if(definition.gun.bulletSpreadFactor > 0){
				firingSpreadRotation.angles.set((Math.random() - 0.5F)*definition.gun.bulletSpreadFactor, (Math.random() - 0.5F)*definition.gun.bulletSpreadFactor, 0D);
				bulletVelocity.rotate(firingSpreadRotation);
			}
			
			//Now that velocity is set, rotate it to match the gun's orientation.
			//For this, we get the reference orientation, and our internal orientation.
			if(muzzle.rot != null){
				bulletVelocity.rotate(muzzle.rot);
			}
			bulletVelocity.rotate(internalOrientation).rotate(zeroReferenceOrientation);
		}else{
			bulletVelocity.set(0, 0, 0);
		}
		
		//Add gun velocity to bullet to ensure we spawn with the offset.
		bulletVelocity.addScaled(motion, EntityVehicleF_Physics.SPEED_FACTOR);

		//Set position.
		bulletPosition.set(muzzle.pos).rotate(internalOrientation).rotate(zeroReferenceOrientation).add(position);
		
		//Set orientation.
		bulletOrientation.set(zeroReferenceOrientation).multiply(internalOrientation);
		if(muzzle.rot != null && !definition.gun.disableMuzzleOrientation){
			bulletOrientation.multiply(muzzle.rot);
		}
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("gun_inhand"): return entityOn instanceof EntityPlayerGun ? 1 : 0;	
			case("gun_active"): return state.isAtLeast(GunState.CONTROLLED) ? 1 : 0;
			case("gun_firing"): return state.isAtLeast(GunState.FIRING_REQUESTED) ? 1 : 0;
			case("gun_fired"): return firedThisCheck ? 1 : 0;
			case("gun_lockedon"): return entityTarget != null || engineTarget != null ? 1 : 0;
			case("gun_lockedon_x"): return entityTarget != null ? entityTarget.getPosition().x : (engineTarget != null ? engineTarget.position.x : 0);
			case("gun_lockedon_y"): return entityTarget != null ? entityTarget.getPosition().y : (engineTarget != null ? engineTarget.position.y : 0);
			case("gun_lockedon_z"): return entityTarget != null ? entityTarget.getPosition().z : (engineTarget != null ? engineTarget.position.z : 0);
			case("gun_pitch"): return prevInternalOrientation.angles.x + (internalOrientation.angles.x - prevInternalOrientation.angles.x)*partialTicks;
			case("gun_yaw"): return prevInternalOrientation.angles.y + (internalOrientation.angles.y - prevInternalOrientation.angles.y)*partialTicks;
			case("gun_pitching"): return prevInternalOrientation.angles.x != internalOrientation.angles.x ? 1 : 0;
			case("gun_yawing"): return prevInternalOrientation.angles.y != internalOrientation.angles.y ? 1 : 0;
			case("gun_cooldown"): return millisecondLastTimeFired + millisecondFiringDelay > System.currentTimeMillis() ? 1 : 0;
			case("gun_windup_time"): return windupTimeCurrent;
			case("gun_windup_rotation"): return windupRotation;
			case("gun_windup_complete"): return windupTimeCurrent == definition.gun.windupTime ? 1 : 0;
			case("gun_reload"): return reloadTimeRemaining > 0 ? 1 : 0;
			case("gun_ammo_count"): return bulletsLeft;
			case("gun_ammo_percent"): return bulletsLeft/definition.gun.capacity;
			case("gun_active_muzzlegroup"): return currentMuzzleGroupIndex + 1;
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}
	
	@Override
	public String getRawTextVariableValue(JSONText textDef, float partialTicks){
		if(textDef.variableName.equals("gun_lockedon_name")){
			 return entityTarget != null ? entityTarget.getName() : (engineTarget != null ? engineTarget.entityOn.getItem().getItemName() : "");
		}
		
		return super.getRawTextVariableValue(textDef, partialTicks);
	}
	
	@Override
	public void spawnParticles(float partialTicks){
		super.spawnParticles(partialTicks);
		//If this gun is being told to fire, and we are currently active and able to fire, do so.
		//We don't spawn bullets on the server, as they will cause lots of lag and network traffic.
		//Instead, we spawn them on the clients, and then send back hit data to the server.
		//This is backwards from what usually happens, and can possibly be hacked, but it's FAR
		//easier on MC to leave clients to handle lots of bullets than the server and network systems.
		//We still need to run the gun code on the server, however, as we need to mess with inventory.
		long timeSinceFiring = System.currentTimeMillis() - lastTimeFired;
		if(state.isAtLeast(GunState.FIRING_CURRENTLY) && bulletsLeft > 0 && (!definition.gun.isSemiAuto || !firedThisRequest) && timeSinceFiring >= millisecondFiringDelay){			
			for(JSONMuzzle muzzle : definition.gun.muzzleGroups.get(currentMuzzleGroupIndex).muzzles){
				//Get the bullet's state.
				setBulletSpawn(bulletPosition, bulletVelocity, bulletOrientation, muzzle);
				
				//Add the bullet as a particle.
				//If the bullet is a missile, give it a target.
				EntityBullet newBullet;
				if(loadedBullet.definition.bullet.turnRate > 0){
					if(entityTarget != null){
						newBullet = new EntityBullet(bulletPosition, bulletVelocity, bulletOrientation, this, entityTarget);
					}else if(engineTarget != null){
						newBullet = new EntityBullet(bulletPosition, bulletVelocity, bulletOrientation, this, engineTarget);
					}else{
						//No entity found, just fire missile off in direction facing.
						newBullet = new EntityBullet(bulletPosition, bulletVelocity, bulletOrientation, this);
					}
				}else{
					newBullet = new EntityBullet(bulletPosition, bulletVelocity, bulletOrientation, this);
				}
				world.addEntity(newBullet);
				
				//Decrement bullets, but check to make sure we still have some.
				//We might have a partial volley.
				if(!ConfigSystem.configObject.clientControls.devMode.value)--bulletsLeft;
				++bulletsFired;
				millisecondLastTimeFired = System.currentTimeMillis();
				if(bulletsLeft == 0){
					break;
				}
			}
			
			//Update states.
			entityOn.lastPrimaryPart.put(gunItem, this);
			lastTimeFired += millisecondFiringDelay;
			firedThisRequest = true;
			firedThisCheck = true;
			if(definition.gun.muzzleGroups.size() == ++currentMuzzleGroupIndex){
				currentMuzzleGroupIndex = 0;
			}
		}else{
			//Only keep variable on for one tick, or one frame, depending on the firing rate.
			//This ensures we don't start tons of sounds.
			if(millisecondFiringDelay < 50){
				firedThisCheck = false;
			}
		}
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setInteger("state", (byte) state.ordinal());
		data.setInteger("shotsFired", bulletsFired);
		data.setInteger("bulletsLeft", bulletsLeft);
		data.setInteger("bulletsReloading", bulletsReloading);
		data.setInteger("currentMuzzleGroupIndex", currentMuzzleGroupIndex);
		data.setPoint3d("internalAngles", internalOrientation.angles);
		if(loadedBullet != null){
			data.setString("loadedBulletPack", loadedBullet.definition.packID);
			data.setString("loadedBulletName", loadedBullet.definition.systemName);
		}
		return data;
	}
	
	public static enum GunState{
		INACTIVE,
		ACTIVE,
		CONTROLLED,
		FIRING_REQUESTED,
		FIRING_CURRENTLY;
		
		public GunState promote(GunState newState){
			return newState.ordinal() > this.ordinal() ? newState : this;
		}
		
		public GunState demote(GunState newState){
			return newState.ordinal() < this.ordinal() ? newState : this;
		}
		
		public boolean isAtLeast(GunState testState){
			return this.ordinal() >= testState.ordinal();
		}
	}
}
