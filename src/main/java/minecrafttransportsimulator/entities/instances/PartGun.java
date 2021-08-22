package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.BuilderItem;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

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
	
	//Stored variables used to determine bullet firing behavior.
	public int bulletsFired;
	public int bulletsLeft;
	public int bulletsReloading;
	public int bulletsRemovedThisRequest;
	public final Point3d currentOrientation;
	public final Point3d prevOrientation;
	public ItemBullet loadedBullet;
	
	//These variables are used during firing and will be reset on loading.
	public GunState state;
	public boolean firedThisRequest;
	public boolean firedThisCheck;
	public int ticksFiring;
	public int reloadTimeRemaining;
	public int windupTimeCurrent;
	public int windupRotation;
	public int currentMuzzle;
	public WrapperEntity lastController;
	private WrapperEntity entityTarget;
	private long millisecondCamOffset;
	private long lastTimeFired;
	private final Set<EntityBullet> activeBullets = new HashSet<EntityBullet>();
	public final List<Integer> bulletsHitOnServer = new ArrayList<Integer>();
		
	public PartGun(AEntityE_Multipart<?> entityOn, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placementDefinition, data, parentPart);
		
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
		if(definition.gun.minPitch != 0){
			this.minPitch = placementDefinition.minPitch != 0 ? -Math.max(definition.gun.minPitch, placementDefinition.minPitch) : -definition.gun.minPitch;
		}else{
			this.minPitch = -placementDefinition.minPitch;
		}	
		if(definition.gun.maxPitch != 0){
			this.maxPitch = placementDefinition.maxPitch != 0 ? -Math.min(definition.gun.maxPitch, placementDefinition.maxPitch) : -definition.gun.maxPitch;
		}else{
			this.maxPitch = -placementDefinition.maxPitch;
		}
		if(placementDefinition.defaultYaw != 0 && placementDefinition.defaultYaw >= minYaw && placementDefinition.defaultYaw <= maxYaw){
			this.defaultYaw = placementDefinition.defaultYaw;
		}else{
			this.defaultYaw = definition.gun.defaultYaw;
		}
		if(placementDefinition.defaultPitch != 0 && placementDefinition.defaultPitch >= minPitch && placementDefinition.defaultPitch <= maxPitch){
			this.defaultPitch = -placementDefinition.defaultPitch;
		}else{
			this.defaultPitch = -definition.gun.defaultPitch;
		}
		this.millisecondFiringDelay = (long) (definition.gun.fireDelay*50);
		
		//Load saved data.
		this.state = GunState.values()[data.getInteger("state")];
		this.bulletsFired = data.getInteger("shotsFired");
		this.bulletsLeft = data.getInteger("bulletsLeft");
		this.bulletsReloading = data.getInteger("bulletsReloading");
		this.currentOrientation = data.getPoint3d("currentOrientation");
		this.prevOrientation = currentOrientation.copy();
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
				player.getInventory().removeItem(heldItem, null);
			}
		}
		return true;
    }
	
	@Override
	public boolean update(){
		if(super.update()){
			//if(entityOn.definition.systemName.contains("p11") && world.isClient())System.out.println(bulletsLeft);
			//Set gun state and do updates.
			if(isActive){				
				//Get the player holding, if we are a hand-held.
				WrapperPlayer playerHolding;
				if(entityOn instanceof EntityPlayerGun){
					playerHolding = ((EntityPlayerGun) entityOn).player;
					if(playerHolding == null){
						//Can't just remove this as we'll get a CME here.
						//TODO perhaps make updates with an iterator?
						this.isValid = false;
						return false;
					}
				}else{
					playerHolding = null;
				}
				
				//Check if we have a controller.
				//We aren't making sentry turrets here.... yet.
				WrapperEntity controller = getController();
				if(controller != null){
					lastController = controller;
					if(playerHolding != null){
						state = state.promote(GunState.CONTROLLED);
					}else{
						//If this gun type can only have one selected at a time, check that this has the selected index.
						ItemPartGun gunItem = getItem();
						PartSeat controllerSeat = (PartSeat) entityOn.getPartAtLocation(entityOn.locationRiderMap.inverse().get(controller));
						if(!placementDefinition.isSpare && controller != null && controllerSeat != null && gunItem.equals(controllerSeat.activeGun) && (!definition.gun.fireSolo || entityOn.partsByItem.get(gunItem).get(controllerSeat.gunIndex).equals(this))){
							state = state.promote(GunState.CONTROLLED);	
						}else{
							state = state.demote(GunState.ACTIVE);
						}
					}
				}else{
					state = state.demote(GunState.ACTIVE);
				}
				
				//Adjust yaw and pitch to the direction of the controller, or the default if we need to do so.
				if(state.isAtLeast(GunState.CONTROLLED)){
					//If the controller isn't a player, but is a NPC, make them look at the nearest hostile mob.
					//We also get a flag to see if the gun is currently pointed to the hostile mob.
					//If not, then we don't fire the gun, as that'd waste ammo.
					if(!(controller instanceof WrapperPlayer)){
						WrapperEntity hostile = world.getNearestHostile(controller, 48);
						if(hostile != null){
							//Need to aim for the middle of the mob, not their base (feet).
							Point3d hostilePosition = hostile.getPosition().add(0D, hostile.getEyeHeight()/2D, 0D);
							//Make the gunner account for bullet delay and movement of the hostile.
							//This makes them track better when the target is moving.
							double ticksToTarget = hostilePosition.distanceTo(position)/definition.gun.muzzleVelocity/20D/10D;
							hostilePosition.add(hostile.getVelocity().copy().multiply(ticksToTarget));
							double yawHostile = Math.toDegrees(Math.atan2(hostilePosition.x - position.x, hostilePosition.z - position.z));
							double pitchHostile = -Math.toDegrees(Math.atan2(hostilePosition.y - position.y, Math.hypot(hostilePosition.x - position.x, hostilePosition.z - position.z)));
							controller.setYaw(yawHostile);
							controller.setHeadYaw(yawHostile);
							controller.setPitch(pitchHostile);
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
					if(playerHolding == null){
						double partYawContribution = definition.gun.yawIsInternal ? localAngles.y : localAngles.y - prevOrientation.y;
						double partPitchContribution = definition.gun.pitchIsInternal ? localAngles.x : localAngles.x - prevOrientation.x;
						double entityPitchContribution = (entityOn.angles.x + partPitchContribution)*Math.cos(Math.toRadians(partYawContribution));
						double entityRollContribution = (entityOn.angles.z + localAngles.z)*Math.sin(Math.toRadians(partYawContribution));
						double targetYaw = controller.getYaw() - (entityOn.angles.y + partYawContribution);
						double targetPitch = controller.getPitch() - (entityPitchContribution + entityRollContribution);
						boolean clampedMovement = handleMovement(targetYaw, targetPitch);
						
						//If we told the gun to fire because we saw an entity, but we can't hit it due to the gun clamp don't fire.
						//This keeps NPCs from wasting ammo.
						if(!(controller instanceof WrapperPlayer) && clampedMovement){
							state = state.demote(GunState.CONTROLLED);
						}
					}
				}
				
				//If we are on a client, check for a target for this gun if we have a lock-on missile.
				//Only do this once every 1/2 second.
				if(world.isClient() && loadedBullet != null && loadedBullet.definition.bullet.turnFactor > 0){
					//Try to find the entity the controller is looking at.
					entityTarget = world.getEntityLookingAt(controller, 750);
				}else{
					entityTarget = null;
				}
				
				//Set final gun active state and variables.
				boolean ableToFire = windupTimeCurrent == definition.gun.windupTime && bulletsLeft > 0 && (!definition.gun.isSemiAuto || !firedThisRequest);
				if(ableToFire && state.isAtLeast(GunState.FIRING_REQUESTED)){
					if(!state.isAtLeast(GunState.FIRING_CURRENTLY)){
						state = state.promote(GunState.FIRING_CURRENTLY);
						List<APart> allGuns = entityOn.partsByItem.get(getItem());
						millisecondCamOffset = definition.gun.fireSolo ? 0 : millisecondFiringDelay*allGuns.indexOf(this)/allGuns.size();
						lastTimeFired = System.currentTimeMillis() + millisecondCamOffset - millisecondFiringDelay;
					}
				}else if(!ableToFire){
					state = state.demote(GunState.FIRING_REQUESTED);
					firedThisRequest = false;
					ticksFiring = 0;
				}
				
				//If we are on the server, check if we are firing and can remove a bullet this tick.
				//This uses a total time of firing to account for partial tick firing rates on clients.
				if(!world.isClient() && state.isAtLeast(GunState.FIRING_CURRENTLY)){
					int bulletsToRemove = (int) (++ticksFiring/definition.gun.fireDelay - bulletsRemovedThisRequest); 
					bulletsLeft -= bulletsToRemove;
					bulletsRemovedThisRequest += bulletsToRemove;
					bulletsFired += bulletsToRemove;
					if(bulletsLeft <= 0){
						bulletsLeft = 0;
						loadedBullet = null;
					}
				}
				
				//If we can accept bullets, and aren't currently loading any, re-load ourselves from any inventories.
				//While the reload method checks for reload time, we check here to save on code processing.
				//No sense in looking for bullets if we can't load them anyways.
				if(!world.isClient() && bulletsLeft < definition.gun.capacity && bulletsReloading == 0){
					if(playerHolding != null){
						if(definition.gun.autoReload || bulletsLeft == 0){
							//Check the player's inventory for bullets.
							WrapperInventory inventory = playerHolding.getInventory();
							for(int i=0; i<inventory.getSize(); ++i){
								AItemBase item = inventory.getItemInSlot(i);
								if(item instanceof ItemBullet){
									if(tryToReload((ItemBullet) item)){
										//Bullet is right type, and we can fit it.  Remove from player's inventory and add to the gun.
										inventory.decrementSlot(i, 1);
										return true;
									}
								}
							}
						}
					}else{
						if(definition.gun.autoReload){
							//Iterate through all the inventory slots in crates to try to find matching ammo.
							for(APart part : entityOn.parts){
								if(part instanceof PartInteractable && part.definition.interactable.interactionType.equals(InteractableComponentType.CRATE) && part.definition.interactable.feedsVehicles){
									EntityInventoryContainer inventory = ((PartInteractable) part).inventory;
									for(int i=0; i<inventory.getSize(); ++i){
										ItemStack stack = inventory.getStack(i);
										Item item = stack.getItem();
										if(item instanceof BuilderItem && ((BuilderItem) item).item instanceof ItemBullet){
											if(tryToReload((ItemBullet) ((BuilderItem) item).item)){
												//Bullet is right type, and we can fit it.  Remove from crate and add to the gun.
												//Return here to ensure we don't set the loadedBullet to blank since we found bullets.
												inventory.removeItems(i, 1, true);
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
			
			//Update active bullets.
			Iterator<EntityBullet> iterator = activeBullets.iterator();
			while(iterator.hasNext()){
				EntityBullet bullet = iterator.next();
				bullet.update();
				if(!bullet.isValid){
					iterator.remove();
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Helper method to do yaw/pitch movement.
	 * Returns true if the movement was impeded by a clamp.
	 * Only call this ONCE per update loop as it sets prev value.s
	 */
	public boolean handleMovement(double targetPitch, double targetYaw){
		//Set prev orientation now that we don't need it for the gun delta calculations.
		//Also create variable to note if we hit a clamp in a yaw or pitch.
		prevOrientation.setTo(currentOrientation);
		boolean clampedMovement = false;
		
		//Adjust yaw.  We need to normalize the delta here as yaw can go past -180 to 180.
		double deltaYaw = -currentOrientation.getClampedYDelta(targetYaw);
		if(deltaYaw < 0){
			if(deltaYaw < -definition.gun.yawSpeed){
				deltaYaw = -definition.gun.yawSpeed;
				clampedMovement = true;
			}
			currentOrientation.y += deltaYaw; 
		}else if(deltaYaw > 0){
			if(deltaYaw > definition.gun.yawSpeed){
				deltaYaw = definition.gun.yawSpeed;
				clampedMovement = true;
			}
			currentOrientation.y += deltaYaw;
		}
		
		//Apply yaw clamps.
		//If yaw is from -180 to 180, we are a gun that can spin around on its mount.
		//We need to do special logic for this type of gun.
		if(minYaw == -180  && maxYaw == 180){
			if(currentOrientation.y > 180 ){
				currentOrientation.y -= 360;
				prevOrientation.y -= 360;
			}else if(currentOrientation.y < -180){
				currentOrientation.y += 360;
				prevOrientation.y += 360;
			}
		}else{
			if(currentOrientation.y > maxYaw){
				currentOrientation.y = maxYaw;
				clampedMovement = true;
			}
			if(currentOrientation.y < minYaw){
				currentOrientation.y = minYaw;
				clampedMovement = true;
			}
		}
		
		//Adjust pitch.
		double deltaPitch = targetPitch - currentOrientation.x;
		if(deltaPitch < 0){
			if(deltaPitch < -definition.gun.pitchSpeed){
				deltaPitch = -definition.gun.pitchSpeed;
				clampedMovement = true;
			}
			currentOrientation.x += deltaPitch; 
		}else if(deltaPitch > 0){
			if(deltaPitch > definition.gun.pitchSpeed){
				deltaPitch = definition.gun.pitchSpeed;
				clampedMovement = true;
			}
			currentOrientation.x += deltaPitch;
		}
		
		//Apply pitch clamps.
		if(currentOrientation.x < maxPitch){
			currentOrientation.x = maxPitch;
			clampedMovement = true;
		}
		if(currentOrientation.x > minPitch){
			currentOrientation.x = minPitch;
			clampedMovement = true;
		}
		return clampedMovement;
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
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("gun_inhand"): return entityOn instanceof EntityPlayerGun ? 1 : 0;	
			case("gun_active"): return state.isAtLeast(GunState.CONTROLLED) ? 1 : 0;
			case("gun_firing"): return state.isAtLeast(GunState.FIRING_CURRENTLY) ? 1 : 0;
			case("gun_fired"): return firedThisCheck ? 1 : 0;
			case("gun_lockedon"): return entityTarget != null ? 1 : 0;
			case("gun_pitch"): return prevOrientation.x + (currentOrientation.x - prevOrientation.x)*partialTicks;
			case("gun_yaw"): return prevOrientation.y + (currentOrientation.y - prevOrientation.y)*partialTicks;
			case("gun_pitching"): return prevOrientation.x != currentOrientation.x ? 1 : 0;
			case("gun_yawing"): return prevOrientation.y != currentOrientation.y ? 1 : 0;
			case("gun_cooldown"): return state.isAtLeast(GunState.FIRING_CURRENTLY) && lastTimeFired != 0 ? (System.currentTimeMillis() - lastTimeFired)/50D : 0;
			case("gun_windup_time"): return windupTimeCurrent;
			case("gun_windup_rotation"): return windupRotation;
			case("gun_windup_complete"): return windupTimeCurrent == definition.gun.windupTime ? 1 : 0;
			case("gun_reload"): return reloadTimeRemaining > 0 ? 1 : 0;
			case("gun_ammo_count"): return bulletsLeft;
			case("gun_ammo_percent"): return bulletsLeft/definition.gun.capacity;
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}

	/**
	 *  Returns the controller for the gun.
	 *  The returned value may or may not be the entity the gun is on.
	 */
	public WrapperEntity getController(){
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
			//Fire a bullet by spawning it with the appropriate muzzle velocity and angle.
			//Angle is based on the current gun orientation, plus a slight fudge-factor.
			//This is based on the barrel length and shell size.
			Point3d spreadAngle = new Point3d();
			if(definition.gun.bulletSpreadFactor > 0){
				spreadAngle.add((Math.random() - 0.5F)*definition.gun.bulletSpreadFactor, (Math.random() - 0.5F)*definition.gun.bulletSpreadFactor, 0D);
			}
			
			//Set the bullet's direction the the provider's orientation.
			Point3d bulletVelocity = new Point3d(0D, 0D, 1D).rotateFine(spreadAngle);
			bulletVelocity.rotateFine(localAngles).rotateFine(entityOn.angles);
			
			//If we have a gun with a muzzle velocity, set the bullet's velocity to that.  Otherwise set it to the vehicle's velocity.
			if(definition.gun.muzzleVelocity > 0){
				bulletVelocity.multiply(definition.gun.muzzleVelocity/20D/10D);
			}else{
				bulletVelocity.add(motion.copy().multiply(EntityVehicleF_Physics.SPEED_FACTOR));
			}
			
			//Get the bullet's initial position, adjusted for barrel length and gun orientation.
			//Then move the bullet to the appropriate firing position.
			Point3d bulletPosition = new Point3d(0, 0, definition.gun.length).rotateFine(localAngles).rotateFine(entityOn.angles).add(position);

			//Add the bullet as a particle.
			//If the bullet is a missile, give it a target.
			if(loadedBullet.definition.bullet.turnFactor > 0){
				if(entityTarget != null){
					activeBullets.add(new EntityBullet(bulletPosition, bulletVelocity, this, entityTarget));
				}else{
					//No entity found, try blocks.
					Point3d lineOfSight = lastController.getLineOfSight(2000F);
					Point3d blockTarget = world.getBlockHit(lastController.getPosition().add(0D, lastController.getEyeHeight(), 0D), lineOfSight);
					if(blockTarget != null){
						activeBullets.add(new EntityBullet(bulletPosition, bulletVelocity, this, blockTarget));
					}else{
						//No block found, just fire missile off in direction facing.
						activeBullets.add(new EntityBullet(bulletPosition, bulletVelocity, this));
					}
				}
			}else{
				activeBullets.add(new EntityBullet(bulletPosition, bulletVelocity, this));
			}
			
			//Update states.
			lastTimeFired += millisecondFiringDelay;
			firedThisRequest = true;
			firedThisCheck = true;
			--bulletsLeft;
			++bulletsFired;
		}else{
			//Only keep variable on for one tick, or one frame, depending on the firing rate.
			//This ensures we don't start tons of sounds.
			if(millisecondFiringDelay%50 != 0 || timeSinceFiring > 50){
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
		data.setPoint3d("currentOrientation", currentOrientation);
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
