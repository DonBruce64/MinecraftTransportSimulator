package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
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
import minecrafttransportsimulator.rendering.components.RenderableObject;
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
	public int bulletsFired;
	public int bulletsLeft;
	public int bulletsReloading;
	public int bulletsRemovedThisRequest;
	public int currentMuzzleGroupIndex;
	public final Point3d currentGunOrientation;
	public final Point3d prevGunOrientation;
	public ItemBullet loadedBullet;
	
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
	public WrapperEntity lastController;
	private WrapperEntity entityTarget;
	private long millisecondCamOffset;
	private long lastTimeFired;
	public final List<Integer> bulletsHitOnServer = new ArrayList<Integer>();
	public final RenderableObject muzzleWireframe = new RenderableObject(new BoundingBox(new Point3d(), 0.25, 0.25, 0.25), ColorRGB.BLUE, false);
		
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
		this.currentGunOrientation = data.getPoint3d("currentOrientation");
		this.prevGunOrientation = currentGunOrientation.copy();
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
	public boolean update(){
		//Set gun state and do updates.
		firedThisCheck = false;
		if(isActive && !placementDefinition.isSpare){
			//Check if we have a controller.
			//We aren't making sentry turrets here.... yet.
			WrapperEntity controller = getController();
			if(controller != null){
				lastController = controller;
				if(entityOn instanceof EntityPlayerGun){
					state = state.promote(GunState.CONTROLLED);
				}else{
					//If this gun type can only have one selected at a time, check that this has the selected index.
					PartSeat controllerSeat = (PartSeat) entityOn.getPartAtLocation(entityOn.locationRiderMap.inverse().get(controller));
					if(controller != null && controllerSeat != null && gunItem.equals(controllerSeat.activeGun) && (!definition.gun.fireSolo || entityOn.partsByItem.get(gunItem).get(controllerSeat.gunIndex).equals(this))){
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
							controller = ((PartGun) part).getController();
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
											inventory.removeFromSlot(i, 1);
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
		
		//Now fire super updates.  We have to do these after ourselves so we have the right state.
		return super.update();
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
			Point3d targetAngles = new Point3d();
			if(entityTarget != null){
				if(entityTarget.isValid()){
					//Check if target is still in bounds to hit.  If not, we need to find another.
					//This could be a valid entity, but might not be.  Do raytracing to make sure we can see them.
					Point3d positionDelta = entityTarget.getPosition().add(0D, entityTarget.getEyeHeight()/2D, 0D).subtract(position);
					targetAngles.setTo(positionDelta).getAngles(true).subtract(angles);
					if(targetAngles.y < -180)targetAngles.y += 360;
					if(targetAngles.y > 180)targetAngles.y -= 360;
					if(((minYaw != -180 || maxYaw != 180) && (targetAngles.y < minYaw || targetAngles.y > maxYaw)) || targetAngles.x < minPitch || targetAngles.x > maxPitch || world.getBlockHit(position, positionDelta) != null){
						entityTarget = null;
					}
				}else{
					entityTarget = null;
				}
			}
			if(entityTarget == null || !entityTarget.isValid()){
				//Target is null or dead, get another one.
				for(WrapperEntity entity : world.getEntitiesHostile(controller, 48)){
					Point3d positionDelta = entity.getPosition().add(0D, entity.getEyeHeight()/2D, 0D).subtract(position);
					targetAngles.setTo(positionDelta).getAngles(true).subtract(angles);
					if(targetAngles.y < -180)targetAngles.y += 360;
					if(targetAngles.y > 180)targetAngles.y -= 360;
					//Check the distance between this target and our current one, if we have one.
					//No sense in doing other checks if the testing target is further.
					if(entityTarget == null || position.distanceTo(entityTarget.getPosition()) > position.distanceTo(entity.getPosition())){
						//Check if bounds are good.  If so, set target.
						if(((minYaw == -180 && maxYaw == 180) || (targetAngles.y >= minYaw && targetAngles.y <= maxYaw)) && targetAngles.x >= minPitch && targetAngles.x <= maxPitch && world.getBlockHit(position, positionDelta) == null){
							entityTarget = entity;
						}
					}
				}
			}
			
			if(entityTarget != null){
				//Have target in proper bounds, try to fire.
				//Make the gunner account for bullet delay and movement of the hostile.
				//This makes them track better when the target is moving.
				double ticksToTarget = entityTarget.getPosition().distanceTo(position)/definition.gun.muzzleVelocity/20D/10D;
				targetAngles = entityTarget.getPosition().add(0D, entityTarget.getEyeHeight()/2D, 0D).add(entityTarget.getVelocity().multiply(ticksToTarget)).subtract(position).getAngles(true);
				controller.setYaw(targetAngles.y);
				controller.setPitch(targetAngles.x);
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
	
		//Get the actual angle this gun is as.  This needs to remove all part-based animations we applied to this gun.
		//This is because if the gun moves based on those animations, we shouldn't take them into account.
		//For pitch, we need to find the relative angle of the player to the entity's 0-pitch plane.
		//When the player rotates their head, they don't do so relative to the pitch of the entity the gun is on, 
		//so a yaw change can result in a pitch change.
		if(!(entityOn instanceof EntityPlayerGun)){
			double partYawContribution = localAngles.y - prevGunOrientation.y;
			double partPitchContribution = definition.gun.pitchIsInternal ? localAngles.x : localAngles.x - prevGunOrientation.x;
			double entityPitchContribution = (entityOn.angles.x + partPitchContribution)*Math.cos(Math.toRadians(partYawContribution));
			double entityRollContribution = (entityOn.angles.z + localAngles.z)*Math.sin(Math.toRadians(partYawContribution));
			double targetYaw = controller.getYaw() - (entityOn.angles.y + partYawContribution);
			double targetPitch = controller.getPitch() - (entityPitchContribution + entityRollContribution);
			handleMovement(targetYaw, targetPitch);
		}
	}
	
	/**
	 * Helper method to do yaw/pitch movement.
	 * Returns true if the movement was impeded by a clamp.
	 * Only call this ONCE per update loop as it sets prev values.
	 */
	private void handleMovement(double targetYaw, double targetPitch){
		//Set prev orientation now that we don't need it for the gun delta calculations.
		prevGunOrientation.setTo(currentGunOrientation);
		
		//Adjust yaw.  We need to normalize the delta here as yaw can go past -180 to 180.
		double deltaYaw = -currentGunOrientation.getClampedYDelta(targetYaw);
		if(deltaYaw < 0){
			if(deltaYaw < -definition.gun.yawSpeed){
				deltaYaw = -definition.gun.yawSpeed;
			}
			currentGunOrientation.y += deltaYaw; 
		}else if(deltaYaw > 0){
			if(deltaYaw > definition.gun.yawSpeed){
				deltaYaw = definition.gun.yawSpeed;
			}
			currentGunOrientation.y += deltaYaw;
		}
		
		//Apply yaw clamps.
		//If yaw is from -180 to 180, we are a gun that can spin around on its mount.
		//We need to do special logic for this type of gun.
		if(minYaw == -180  && maxYaw == 180){
			if(currentGunOrientation.y > 180 ){
				currentGunOrientation.y -= 360;
				prevGunOrientation.y -= 360;
			}else if(currentGunOrientation.y < -180){
				currentGunOrientation.y += 360;
				prevGunOrientation.y += 360;
			}
		}else{
			if(currentGunOrientation.y > maxYaw){
				currentGunOrientation.y = maxYaw;
			}
			if(currentGunOrientation.y < minYaw){
				currentGunOrientation.y = minYaw;
			}
		}
		
		//Adjust pitch.
		double deltaPitch = targetPitch - currentGunOrientation.x;
		if(deltaPitch < 0){
			if(deltaPitch < -definition.gun.pitchSpeed){
				deltaPitch = -definition.gun.pitchSpeed;
			}
			currentGunOrientation.x += deltaPitch; 
		}else if(deltaPitch > 0){
			if(deltaPitch > definition.gun.pitchSpeed){
				deltaPitch = definition.gun.pitchSpeed;
			}
			currentGunOrientation.x += deltaPitch;
		}
		
		//Apply pitch clamps.
		if(currentGunOrientation.x > maxPitch){
			currentGunOrientation.x = maxPitch;
		}
		if(currentGunOrientation.x < minPitch){
			currentGunOrientation.x = minPitch;
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
	public WrapperEntity getController(){
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
		
		//Not parent or child.  Get main vehicle controller.
		//Check all controllers in case there's multiple controller seats.
		for(APart vehiclePart : entityOn.parts){
			if(vehiclePart instanceof PartSeat){
				if(vehiclePart.placementDefinition.isController){
					WrapperEntity controller = entityOn.locationRiderMap.get(vehiclePart.placementOffset);
					if(controller != null){
						return controller;
					}
				}
			}
		}
		
		//No controller found.
		return null;
	}
	
	/**
	 * Helper method to set the position and velocity of a bullet's spawn.
	 * This is based on the passed-in muzzle, and the parameters of that muzzle.
	 * Used in both spawning the bullet, and in rendering where the muzzle position is.
	 */
	public void setBulletSpawn(Point3d bulletPosition, Point3d bulletVelocity, JSONMuzzle muzzle){
		//Set initial velocity to gun's velocity, otherwise it starts at 0.
		if(definition.gun.muzzleVelocity > 0){
			bulletVelocity.set(0, 0, definition.gun.muzzleVelocity/20D/10D);
		}else{
			bulletVelocity.set(0, 0, 0);
		}
		
		//Velocity is based on the current gun orientation, plus a slight fudge-factor based on the spread factor.
		if(definition.gun.pitchIsInternal){
			bulletVelocity.rotateFine(new Point3d(currentGunOrientation.x, 0, 0));
		}
		bulletVelocity.rotateFine(muzzle.rot);
		if(definition.gun.bulletSpreadFactor > 0){
			bulletVelocity.rotateFine(new Point3d((Math.random() - 0.5F)*definition.gun.bulletSpreadFactor, (Math.random() - 0.5F)*definition.gun.bulletSpreadFactor, 0D));
		}
		bulletVelocity.rotateFine(localAngles).rotateFine(entityOn.angles);
		
		//Add gun velocity to bullet to ensure we spawn with the offset.
		bulletVelocity.addScaled(motion, EntityVehicleF_Physics.SPEED_FACTOR);
		
		//Position is based on JSON parameters and current orientation.
		if(definition.gun.pitchIsInternal){
			Point3d muzzleDelta = muzzle.pos.copy().subtract(muzzle.center);
			bulletPosition.setTo(muzzleDelta).rotateFine(new Point3d(currentGunOrientation.x, 0, 0)).subtract(muzzleDelta);
			bulletPosition.add(muzzle.pos);
		}else{
			bulletPosition.setTo(muzzle.pos);
		}
		bulletPosition.rotateFine(localAngles).rotateFine(entityOn.angles).add(position);
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("gun_inhand"): return entityOn instanceof EntityPlayerGun ? 1 : 0;	
			case("gun_active"): return state.isAtLeast(GunState.CONTROLLED) ? 1 : 0;
			case("gun_firing"): return state.isAtLeast(GunState.FIRING_CURRENTLY) ? 1 : 0;
			case("gun_fired"): return firedThisCheck ? 1 : 0;
			case("gun_lockedon"): return entityTarget != null ? 1 : 0;
			case("gun_pitch"): return prevGunOrientation.x + (currentGunOrientation.x - prevGunOrientation.x)*partialTicks;
			case("gun_yaw"): return prevGunOrientation.y + (currentGunOrientation.y - prevGunOrientation.y)*partialTicks;
			case("gun_pitching"): return prevGunOrientation.x != currentGunOrientation.x ? 1 : 0;
			case("gun_yawing"): return prevGunOrientation.y != currentGunOrientation.y ? 1 : 0;
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
			Point3d bulletPosition = new Point3d();
			Point3d bulletVelocity = new Point3d();
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
						Point3d lineOfSight = lastController.getLineOfSight(2000F);
						Point3d blockTarget = world.getBlockHit(lastController.getPosition().add(0D, lastController.getEyeHeight(), 0D), lineOfSight);
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
		data.setPoint3d("currentOrientation", currentGunOrientation);
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
