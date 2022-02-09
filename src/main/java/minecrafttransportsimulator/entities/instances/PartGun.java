package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Matrix4dPlus;
import minecrafttransportsimulator.baseclasses.Point3dPlus;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.jsondefs.JSONMuzzle;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
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
	private final Point3dPlus internalAngles;
	private final Matrix4dPlus internalOrientation;
	private final Matrix4dPlus prevInternalOrientation;
	protected ItemBullet loadedBullet;
	
	//These variables are used during firing and will be reset on loading.
	public GunState state;
	public boolean firedThisRequest;
	public boolean firedThisCheck;
	public boolean playerHoldingTrigger;
	public boolean isHandHeldGunAimed;
	public int ticksFiring;
	public int reloadTimeRemaining;
	public int windupTimeCurrent;
	public int windupRotation;
	public int currentMuzzle;
	public final Point3dPlus lastMuzzlePosition;
	public WrapperEntity lastController;
	private WrapperEntity entityTarget;
	private long millisecondCamOffset;
	private long lastTimeFired;
	private final Matrix4dPlus zeroReferenceOrientation;
	
	//Temp helper variables for calculations
	private final Point3dPlus firingSpreadAngles = new Point3dPlus();
	private final Matrix4dPlus firingSpreadMatrix = new Matrix4dPlus();
	
	//Global data.
	public final List<Integer> bulletsHitOnServer = new ArrayList<Integer>();
		
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
		this.internalAngles = data.getPoint3d("internalAngles");
		this.internalOrientation = new Matrix4dPlus();
		internalOrientation.setToAngles(internalAngles);
		this.prevInternalOrientation = new Matrix4dPlus(internalOrientation);
		this.zeroReferenceOrientation = new Matrix4dPlus();
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
		
		//Create muzzle boxes.
		
		this.lastMuzzlePosition = new Point3dPlus();
		
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
	public boolean update(){
		if(super.update()){
			//Set gun state and do updates.
			//First get our zero-reference.
			if(parentPart != null && placementDefinition.isSubPart){
				zeroReferenceOrientation.set(parentPart.orientation);
			}else{
				zeroReferenceOrientation.set(entityOn.orientation);
			}
			zeroReferenceOrientation.mul(placementDefinition.rot);
			
			//Now set our state.
			firedThisCheck = false;
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
						}
					}
				}
				if(controller == null){
					//If we aren't being controller, check if we have any coaxial guns.
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
							bulletsLeft -= bulletsToRemove;
							bulletsRemovedThisRequest += bulletsToRemove;
							bulletsFired += bulletsToRemove;
							entityOn.lastPrimaryPart.put(gunItem, this);
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
										inventory.removeFromSlot(i, 1);
										return true;
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
												inventory.removeFromSlot(i, 1);
												return true;
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
				if(definition.gun.resetPosition){
					handleMovement(defaultYaw, defaultPitch);
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
			return true;
		}else{
			return false;
		}
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
			//FIXME fix auto-aiming.
			if(entityTarget != null){
				if(entityTarget.isValid()){
					//Check if target is still in bounds to hit.  If not, we need to find another.
					//This could be a valid entity, but might not be.
					//First check if we hit one of the stops on the gun, if so, it means the target is out of gun area.
					//If the gun is still good on the stops, make sure there's no blocks in the way.
					Point3dPlus positionDelta = entityTarget.getPosition().add(0D, entityTarget.getEyeHeight()/2D, 0D).subtract(position);
					if(((minYaw != -180 || maxYaw != 180) && (internalAngles.y == minYaw || internalAngles.y == maxYaw)) || internalAngles.x == minPitch || internalAngles.x == maxPitch || world.getBlockHit(position, positionDelta) != null){
						entityTarget = null;
					}					
				}else{
					entityTarget = null;
				}
			}
			if(entityTarget == null || !entityTarget.isValid()){
				//Target is null or dead, get another one.
				for(WrapperEntity entity : world.getEntitiesHostile(controller, 48)){
					Point3dPlus positionDelta = entity.getPosition().add(0D, entity.getEyeHeight()/2D, 0D).subtract(position);
					/*targetAngles.set(positionDelta);
					targetAngles.getAngles(true).subtract(angles);
					if(targetAngles.y < -180)targetAngles.y += 360;
					if(targetAngles.y > 180)targetAngles.y -= 360;
					//Check the distance between this target and our current one, if we have one.
					//No sense in doing other checks if the testing target is further.
					if(entityTarget == null || position.distanceTo(entityTarget.getPosition()) > position.distanceTo(entity.getPosition())){
						//Check if bounds are good.  If so, set target.
						if(((minYaw == -180 && maxYaw == 180) || (targetAngles.y >= minYaw && targetAngles.y <= maxYaw)) && targetAngles.x >= minPitch && targetAngles.x <= maxPitch && world.getBlockHit(position, positionDelta) == null){
							entityTarget = entity;
						}
					}*/
				}
			}
			
			if(entityTarget != null){
				//Have target in proper bounds, try to fire.
				//Make the gunner account for bullet delay and movement of the hostile.
				//This makes them track better when the target is moving.
				/*double ticksToTarget = entityTarget.getPosition().distanceTo(position)/definition.gun.muzzleVelocity/20D/10D;
				targetAngles = entityTarget.getPosition().add(0D, entityTarget.getEyeHeight()/2D, 0D).add(entityTarget.getVelocity().multiply(ticksToTarget)).subtract(position).getAngles(true);
				controller.setYaw(targetAngles.y);
				controller.setPitch(targetAngles.x);*/
				state = state.promote(GunState.FIRING_REQUESTED);
			}else{
				state = state.demote(GunState.CONTROLLED);
			}
		}else{
			//Player-controlled gun.
			//If we are on a client, check for a target for this gun if we have a lock-on missile.
			//Only do this once every 1/2 second.
			if(world.isClient() && loadedBullet != null && loadedBullet.definition.bullet.turnFactor > 0){
				//Try to find the entity the controller is looking at.
				entityTarget = world.getEntityLookingAt(controller, 750);
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
			handleMovement(controller.getYaw() - internalAngles.y, controller.getPitch() - internalAngles.x);
		}
	}
	
	/**
	 * Helper method to do yaw/pitch movement.
	 * Returns true if the movement was impeded by a clamp.
	 * Only call this ONCE per update loop as it sets prev values.
	 */
	private void handleMovement(double deltaYaw, double deltaPitch){
		//Set prev orientation now that we don't need it for the gun delta calculations.
		prevInternalOrientation.set(internalOrientation);
		prevInternalOrientation.lastAnglesSet.set(internalOrientation.lastAnglesSet);
		
		//Adjust yaw.  We need to normalize the delta here as yaw can go past -180 to 180.
		if(deltaYaw < -180)deltaYaw += 360;
		if(deltaYaw > 180)deltaYaw -= 360;
		if(deltaYaw < 0){
			if(deltaYaw < -definition.gun.yawSpeed){
				deltaYaw = -definition.gun.yawSpeed;
			}
			internalAngles.y += deltaYaw; 
		}else if(deltaYaw > 0){
			if(deltaYaw > definition.gun.yawSpeed){
				deltaYaw = definition.gun.yawSpeed;
			}
			internalAngles.y += deltaYaw;
		}
		
		//Apply yaw clamps.
		//If yaw is from -180 to 180, we are a gun that can spin around on its mount.
		//We need to do special logic for this type of gun.
		if(minYaw == -180  && maxYaw == 180){
			if(internalAngles.y > 180 ){
				internalAngles.y -= 360;
				prevInternalOrientation.lastAnglesSet.y -= 360;
			}else if(internalAngles.y < -180){
				internalAngles.y += 360;
				prevInternalOrientation.lastAnglesSet.y += 360;
			}
		}else{
			if(internalAngles.y > maxYaw){
				internalAngles.y = maxYaw;
			}
			if(internalAngles.y < minYaw){
				internalAngles.y = minYaw;
			}
		}
		
		//Adjust pitch.
		if(deltaPitch < 0){
			if(deltaPitch < -definition.gun.pitchSpeed){
				deltaPitch = -definition.gun.pitchSpeed;
			}
			internalAngles.x += deltaPitch; 
		}else if(deltaPitch > 0){
			if(deltaPitch > definition.gun.pitchSpeed){
				deltaPitch = definition.gun.pitchSpeed;
			}
			internalAngles.x += deltaPitch;
		}
		
		//Apply pitch clamps.
		if(internalAngles.x > maxPitch){
			internalAngles.x = maxPitch;
		}
		if(internalAngles.x < minPitch){
			internalAngles.x = minPitch;
		}
		
		//Update internal orientation.
		internalOrientation.setToAngles(internalAngles);
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
	public void setBulletSpawn(Point3dPlus bulletPosition, Point3dPlus bulletVelocity, JSONMuzzle muzzle){
		//Set velocity.
		bulletVelocity.set(0, 0, definition.gun.muzzleVelocity/20D/10D);
		if(definition.gun.bulletSpreadFactor > 0){
			firingSpreadAngles.set((Math.random() - 0.5F)*definition.gun.bulletSpreadFactor, (Math.random() - 0.5F)*definition.gun.bulletSpreadFactor, 0D);
			firingSpreadMatrix.setToAngles(firingSpreadAngles);
			firingSpreadMatrix.transform(bulletVelocity);
		}
		
		//Now that velocity is set, rotate it to match the gun's orientation.
		//For this, we get the reference orientation, and our internal orientation.
		muzzle.rot.transform(bulletVelocity);
		internalOrientation.transform(bulletVelocity);
		zeroReferenceOrientation.transform(bulletVelocity);
		
		//Add gun velocity to bullet to ensure we spawn with the offset.
		bulletVelocity.scaleAdd(EntityVehicleF_Physics.SPEED_FACTOR, motion, bulletVelocity);

		//Now set position.
		bulletPosition.set(muzzle.pos);
		internalOrientation.transform(bulletPosition);
		zeroReferenceOrientation.transform(bulletPosition);
		bulletPosition.add(position);
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("gun_inhand"): return entityOn instanceof EntityPlayerGun ? 1 : 0;	
			case("gun_active"): return state.isAtLeast(GunState.CONTROLLED) ? 1 : 0;
			case("gun_firing"): return state.isAtLeast(GunState.FIRING_CURRENTLY) ? 1 : 0;
			case("gun_fired"): return firedThisCheck ? 1 : 0;
			case("gun_lockedon"): return entityTarget != null ? 1 : 0;
			case("gun_pitch"): return prevInternalOrientation.lastAnglesSet.x + (internalOrientation.lastAnglesSet.x - prevInternalOrientation.lastAnglesSet.x)*partialTicks;
			case("gun_yaw"): return prevInternalOrientation.lastAnglesSet.y + (internalOrientation.lastAnglesSet.y - prevInternalOrientation.lastAnglesSet.y)*partialTicks;
			case("gun_pitching"): return prevInternalOrientation.lastAnglesSet.x != internalOrientation.lastAnglesSet.x ? 1 : 0;
			case("gun_yawing"): return prevInternalOrientation.lastAnglesSet.y != internalOrientation.lastAnglesSet.y ? 1 : 0;
			case("gun_cooldown"): return state.isAtLeast(GunState.FIRING_CURRENTLY) && lastTimeFired != 0 ? (System.currentTimeMillis() - lastTimeFired)/50D : 0;
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
			Point3dPlus bulletPosition = new Point3dPlus();
			Point3dPlus bulletVelocity = new Point3dPlus();
			
			for(JSONMuzzle muzzle : definition.gun.muzzleGroups.get(currentMuzzleGroupIndex).muzzles){
				//Get the bullet's state.
				setBulletSpawn(bulletPosition, bulletVelocity, muzzle);
				
				//Add the bullet as a particle.
				//If the bullet is a missile, give it a target.
				EntityBullet newBullet;
				if(loadedBullet.definition.bullet.turnFactor > 0){
					if(entityTarget != null){
						newBullet = new EntityBullet(bulletPosition, bulletVelocity, this, entityTarget);
					}else{
						//No entity found, try blocks.
						Point3dPlus lineOfSight = lastController.getLineOfSight(2000F);
						Point3dPlus blockTarget = world.getBlockHit(lastController.getPosition().add(0D, lastController.getEyeHeight(), 0D), lineOfSight);
						if(blockTarget != null){
							newBullet = new EntityBullet(bulletPosition, bulletVelocity, this, blockTarget);
						}else{
							//No block found, just fire missile off in direction facing.
							newBullet = new EntityBullet(bulletPosition, bulletVelocity, this);
						}
					}
				}else{
					newBullet = new EntityBullet(bulletPosition, bulletVelocity, this);
				}
				world.addEntity(newBullet);
				
				//Decrement bullets, but check to make sure we still have some.
				//We might have a partial volley.
				--bulletsLeft;
				++bulletsFired;
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
		data.setPoint3d("internalAngles", internalAngles);
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
