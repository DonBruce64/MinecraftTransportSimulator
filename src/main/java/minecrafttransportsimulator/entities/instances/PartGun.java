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
	
	//Stored variables used to determine bullet firing behavior.
	public int bulletsFired;
	public int bulletsLeft;
	public int bulletsReloading;
	public int currentMuzzle;
	public final Point3d currentOrientation;
	public final Point3d prevOrientation;
	public ItemBullet loadedBullet;
	
	//These variables are used during firing and will be reset on loading.
	public boolean firingEnabled;
	public boolean firedThisCommand;
	public boolean firedThisTick;
	public boolean gunEnabled;
	public int cooldownTimeRemaining;
	public int reloadTimeRemaining;
	public int windupTimeCurrent;
	public int windupRotation;
	public WrapperEntity lastController;
	private long lastTimeFired;
	private long timeToFire;
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
		
		//Load saved data.
		this.firingEnabled = data.getBoolean("firingEnabled");
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
			//Get the current controller and item reference for this gun.
			WrapperEntity controller = getController();
			ItemPartGun gunItem = getItem();
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
			
			//We flag ourselves as inactive if there are no controllers and the seat isn't set to us if we're not hand-held.
			//We aren't making sentry turrets here.... yet.
			//If this gun type can only have one selected at a time, check that this has the selected index.
			if(controller != null){
				if(playerHolding != null){
					gunEnabled = true;
				}else{
					PartSeat controllerSeat = (PartSeat) entityOn.getPartAtLocation(entityOn.locationRiderMap.inverse().get(controller));
					gunEnabled = !placementDefinition.isSpare && controller != null && controllerSeat != null && getItem().equals(controllerSeat.activeGun) && (!definition.gun.fireSolo || entityOn.partsByItem.get(gunItem).get(controllerSeat.gunIndex).equals(this));
				}
			}else{
				gunEnabled = false;
			}
			
			//Adjust aim to face direction controller is facing.
			//Aim speed depends on gun size, with smaller and shorter guns moving quicker.
			//Pitch and yaw only depend on where the controller is looking, and where the gun is pointed.
			//This allows for guns to be mounted anywhere on a vehicle and at any angle.
			if(isActive && (gunEnabled || definition.gun.resetPosition)){
				boolean lockedOn = gunEnabled;
				//If the controller isn't a player, but is a NPC, make them look at the nearest hostile mob.
				//We also get a flag to see if the gun is currently pointed to the hostile mob.
				//If not, then we don't fire the gun, as that'd waste ammo.
				if(gunEnabled && !(controller instanceof WrapperPlayer)){
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
						firingEnabled = true;
					}else{
						firingEnabled = false;
					}
				}
				
				//Get the target yaw and pitch, then set the orientation.
				//We need to wait here before setting the prev to the current in case 
				//our provider needs to know the prev rotation for its calculations.
				double targetYaw;
				double targetPitch;
				if(playerHolding != null){
					targetYaw = 0;
					targetPitch = 0;
				}else if(gunEnabled){
					//Get the actual angle this gun is as.  This needs to remove all part-based animations we applied to this gun.
					//This is because if the gun moves based on those animations, we shouldn't take them into account.
					//For pitch, we need to find the relative angle of the player to the entity's 0-pitch plane.
					//When the player rotates their head, they don't do so relative to the pitch of the entity the gun is on, 
					//so a yaw change can result in a pitch change.
					double partYawContribution = definition.gun.yawIsInternal ? localAngles.y : localAngles.y - prevOrientation.y;
					double partPitchContribution = definition.gun.pitchIsInternal ? localAngles.x : localAngles.x - prevOrientation.x;
					double entityPitchContribution = (entityOn.angles.x + partPitchContribution)*Math.cos(Math.toRadians(partYawContribution));
					double entityRollContribution = (entityOn.angles.z + localAngles.z)*Math.sin(Math.toRadians(partYawContribution));
					targetYaw = controller.getYaw() - (entityOn.angles.y + partYawContribution);
					targetPitch = controller.getPitch() - (entityPitchContribution + entityRollContribution);
				}else{
					targetYaw = defaultYaw;
					targetPitch = defaultPitch;
				}
				prevOrientation.setTo(currentOrientation);
				
				//Adjust yaw.  We need to normalize the delta here as yaw can go past -180 to 180.
				double deltaYaw = -currentOrientation.getClampedYDelta(targetYaw);
				if(deltaYaw < 0){
					if(deltaYaw < -definition.gun.yawSpeed){
						deltaYaw = -definition.gun.yawSpeed;
						lockedOn = false;
					}
					currentOrientation.y += deltaYaw; 
				}else if(deltaYaw > 0){
					if(deltaYaw > definition.gun.yawSpeed){
						deltaYaw = definition.gun.yawSpeed;
						lockedOn = false;
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
					}
					if(currentOrientation.y < minYaw){
						currentOrientation.y = minYaw;
					}
				}
				
				//Adjust pitch.
				double deltaPitch = targetPitch - currentOrientation.x;
				if(deltaPitch < 0){
					if(deltaPitch < -definition.gun.pitchSpeed){
						deltaPitch = -definition.gun.pitchSpeed;
						lockedOn = false;
					}
					currentOrientation.x += deltaPitch; 
				}else if(deltaPitch > 0){
					if(deltaPitch > definition.gun.pitchSpeed){
						deltaPitch = definition.gun.pitchSpeed;
						lockedOn = false;
					}
					currentOrientation.x += deltaPitch;
				}
				//Apply pitch clamps.
				if(currentOrientation.x < maxPitch){
					currentOrientation.x = maxPitch;
				}
				if(currentOrientation.x > minPitch){
					currentOrientation.x = minPitch;
				}
				
				//If we told the gun to fire because we saw an entity, but we can't hit it due to the gun clamp don't fire.
				//This keeps NPCs from wasting ammo.
				if(!(controller instanceof WrapperPlayer)){
					if(!lockedOn || currentOrientation.y == minYaw || currentOrientation.y == maxYaw || currentOrientation.x == minPitch || currentOrientation.x == maxPitch){
						firingEnabled = false;
					}
				}
			}else{
				firingEnabled = false;
			}
			
			//Increment or decrement windup.
			if(isActive && firingEnabled && windupTimeCurrent < definition.gun.windupTime){
				++windupTimeCurrent;
			}else if(!firingEnabled && windupTimeCurrent > 0){
				--windupTimeCurrent;
			}
			windupRotation += windupTimeCurrent;
			
			//If this gun is being told to fire, and we have bullets and are wound up, fire.
			//Don't spawn bullets on the server, as they will cause lots of lag and network traffic.
			//Instead, spawn them on the clients, and then send back hit data to the server.
			//This is backwards from what usually happens, and can possibly be hacked, but it's FAR
			//easier on MC to leave clients to handle lots of bullets than the server and network systems.
			//We still need to run the gun code on the server, however, as we need to mess with inventory.
			firedThisTick = false;
			if(firingEnabled && windupTimeCurrent == definition.gun.windupTime && bulletsLeft > 0 && cooldownTimeRemaining == 0 && (!definition.gun.isSemiAuto || !firedThisCommand)){
				//First update gun number so we know if we need to apply a cam offset.
				//We would fire a bullet here, but that's for the SFXSystem to handle, not the update loop.
				//Make sure to add-on an offset to our firing point to allow for multi-gun units.
				List<APart> allGuns = entityOn.partsByItem.get(gunItem);
				long millisecondCamOffset = definition.gun.fireSolo ? 0 : (long) (definition.gun.fireDelay*(1000D/20D)*(allGuns.indexOf(this))/allGuns.size());
				cooldownTimeRemaining = definition.gun.fireDelay;
				timeToFire = System.currentTimeMillis() + millisecondCamOffset;
				lastController = controller;
				firedThisCommand = true;
				firedThisTick = true;
				if(!world.isClient()){
					//Only remove bullets from the server.  We remove them from the client when they spawn.
					--bulletsLeft;
					++bulletsFired;
					if(bulletsLeft == 0){
						loadedBullet = null;
					}
				}
			}
			
			//Reset fire command bit if we aren't firing.
			if(!firingEnabled){
				firedThisCommand = false;
			}
			
			//If we can accept bullets, and aren't currently loading any, re-load ourselves from any inventories.
			//While the reload method checks for reload time, we check here to save on code processing.
			//No sense in looking for bullets if we can't load them anyways.
			if(isActive && !world.isClient() && bulletsLeft < definition.gun.capacity && bulletsReloading == 0){
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
			
			if(isActive){
				//If we are reloading, decrement the reloading timer.
				//If we are done reloading, add the new bullets.
				//This comes after the reloading block as we need a 0/1 state-change for the various animations.
				if(reloadTimeRemaining > 0){
					--reloadTimeRemaining;
				}else if(bulletsReloading != 0){
					bulletsLeft += bulletsReloading;
					bulletsReloading = 0;
				}
			}
			
			//Decrement cooldown time blocking gun from firing, if we have any.
			if(cooldownTimeRemaining > 0){
				--cooldownTimeRemaining;
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
		//Check for an instance of a gun_muzzle_# variable, since these requires additional parsing
		if(variable.startsWith("gun_muzzle_")){
			//Get the rest of the variable after gun_muzzle_
			String muzzleVariable = variable.substring("gun_muzzle_".length());
			//Parse one or more digits, then take off one because we are zero-indexed
			int muzzleNumber = Integer.parseInt(muzzleVariable.substring(0, muzzleVariable.indexOf('_'))) - 1;
			switch(muzzleVariable.substring(muzzleVariable.indexOf('_') + 1)) {
				case("firing"): return (muzzleNumber == currentMuzzle ? 1 : 0) * cooldownTimeRemaining/(double)definition.gun.fireDelay;
			}
		}
		switch(variable){
			case("gun_inhand"): return entityOn instanceof EntityPlayerGun ? 1 : 0;	
			case("gun_active"): return gunEnabled ? 1 : 0;
			case("gun_firing"): return firingEnabled ? 1 : 0;
			case("gun_fired"): return firedThisTick ? 1 : 0;
			case("gun_pitch"): return prevOrientation.x + (currentOrientation.x - prevOrientation.x)*partialTicks;
			case("gun_yaw"): return prevOrientation.y + (currentOrientation.y - prevOrientation.y)*partialTicks;
			case("gun_pitching"): return prevOrientation.x != currentOrientation.x ? 1 : 0;
			case("gun_yawing"): return prevOrientation.y != currentOrientation.y ? 1 : 0;
			case("gun_cooldown"): return cooldownTimeRemaining > 0 ? 1 : 0;
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
		
		//Spawn bullets here.  We do this to allow for the cam offsets.
		if(timeToFire != lastTimeFired && System.currentTimeMillis() >= timeToFire && bulletsLeft > 0){
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
			Point3d bulletPosition = getFiringOrigin();
			bulletPosition.rotateFine(localAngles).rotateFine(entityOn.angles);
			bulletPosition.add(position);

			//Add the bullet as a particle.
			//If the bullet is a missile, give it a target.
			if(loadedBullet.definition.bullet.turnFactor > 0){
				//Try to find the entity the controller is looking at.
				WrapperEntity entityTarget = world.getEntityLookingAt(lastController, 2000F);
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
			
			//Set last firing time to current time.
			lastTimeFired = timeToFire;
			
			//Remove a bullet from the count and add shots fired.
			--bulletsLeft;
			++bulletsFired;
		}
	}
	
	/**
	 * Helper method for getting the firing origin of the gun.
	 */
	public Point3d getFiringOrigin(){
		Point3d firingOrigin = new Point3d(0, 0, definition.gun.length);
		//If muzzle count is the same as capacity, use the muzzles in order
		//Otherwise, iterate through the available muzzles.
		if(definition.gun.muzzlePositions != null) {
			if(bulletsLeft > 0){
				currentMuzzle = definition.gun.muzzlePositions.size() == definition.gun.capacity ? definition.gun.capacity - bulletsLeft : bulletsFired%definition.gun.muzzlePositions.size(); 
			}else{
				currentMuzzle = 0;
			}
			firingOrigin.add(definition.gun.muzzlePositions.get(currentMuzzle));
			
		}
		return firingOrigin;
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setBoolean("firingEnabled", firingEnabled);
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
}
