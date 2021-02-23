package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONParticleObject;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.rendering.components.AParticle;
import minecrafttransportsimulator.rendering.components.InterfaceRender;
import minecrafttransportsimulator.rendering.instances.ParticleBullet;
import minecrafttransportsimulator.rendering.instances.ParticleFlame;
import minecrafttransportsimulator.rendering.instances.ParticleMissile;
import minecrafttransportsimulator.rendering.instances.ParticleSuspendedSmoke;
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
	private final double minYawAngle;
	private final double maxYawAngle;
	private final double minPitchAngle;
	private final double maxPitchAngle;
	
	//Stored variables used to determine bullet firing behavior.
	public int bulletsFired;
	public int bulletsLeft;
	public int bulletsReloading;
	public int currentMuzzle;
	public final Point3d currentOrientation;
	public final Point3d prevOrientation;
	private final Point3d internalOrientation;
	private ItemPart loadedBullet;
	
	//These variables are used during firing and will be reset on loading.
	public boolean firing;
	public boolean firedThisCommand;
	public boolean active;
	public int cooldownTimeRemaining;
	public int reloadTimeRemaining;
	public int windupTimeCurrent;
	public int windupRotation;
	private WrapperEntity lastController;
	private long lastTimeFired;
	private long timeToFire;
	private final double anglePerTickSpeed;
	public final List<Integer> bulletsHitOnServer = new ArrayList<Integer>();
		
	public PartGun(AEntityE_Multipart<?> entityOn, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placementDefinition, data, parentPart);
		
		//Set min/max yaw/pitch angles based on our definition and the entity definition.
		//If the entity definition min/max yaw is -180 to 180, set it to that.  Otherwise, get the max bounds.
		//Yaw/Pitch set to 0 is ignored as it's assumed to be un-defined.
		if(placementDefinition.minYaw == -180 && placementDefinition.maxYaw == 180){
			this.minYawAngle = -180;
			this.maxYawAngle = 180;
		}else{
			if(definition.gun.minYaw != 0){
				this.minYawAngle = placementDefinition.minYaw != 0 ? Math.max(definition.gun.minYaw, placementDefinition.minYaw) : definition.gun.minYaw;
			}else{
				this.minYawAngle =  placementDefinition.minYaw;
			}
			if(definition.gun.maxYaw != 0){
				this.maxYawAngle = placementDefinition.maxYaw != 0 ? Math.min(definition.gun.maxYaw, placementDefinition.maxYaw) : definition.gun.maxYaw;
			}else{
				this.maxYawAngle =  placementDefinition.maxYaw;
			}
		}
		if(definition.gun.minPitch != 0){
			this.minPitchAngle = placementDefinition.minPitch != 0 ? -Math.max(definition.gun.minPitch, placementDefinition.minPitch) : -definition.gun.minPitch;
		}else{
			this.minPitchAngle = -placementDefinition.minPitch;
		}	
		if(definition.gun.maxPitch != 0){
			this.maxPitchAngle = placementDefinition.maxPitch != 0 ? -Math.min(definition.gun.maxPitch, placementDefinition.maxPitch) : -definition.gun.maxPitch;
		}else{
			this.maxPitchAngle = -placementDefinition.maxPitch;
		}
		
		//Load saved data.
		this.firing = data.getBoolean("firing");
		this.bulletsFired = data.getInteger("shotsFired");
		this.bulletsLeft = data.getInteger("bulletsLeft");
		this.bulletsReloading = data.getInteger("bulletsReloading");
		this.currentOrientation = data.getPoint3d("currentOrientation");
		this.prevOrientation = currentOrientation.copy();
		this.internalOrientation = currentOrientation.copy();
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
		this.anglePerTickSpeed = definition.gun.travelSpeed != 0 ? definition.gun.travelSpeed : (50/definition.gun.diameter + 1/definition.gun.length);
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		//Check to see if we have any bullets in our hands.
		//If so, try to re-load this gun with them.
		AItemBase heldItem = player.getHeldItem();
		if(heldItem instanceof ItemPart){
			if(tryToReload((ItemPart) heldItem) && !player.isCreative()){
				player.getInventory().removeItem(heldItem, null);
			}
		}
		return true;
    }
	
	@Override
	public void update(){
		super.update();
		
		//Get the current controller and item reference for this gun.
		WrapperEntity controller = getController();
		ItemPart gunItem = getItem();
		WrapperPlayer playerHolding;
		if(entityOn instanceof EntityPlayerGun){
			playerHolding = ((EntityPlayerGun) entityOn).player;
			if(playerHolding == null){
				this.isValid = false;
				return;
			}
		}else{
			playerHolding = null;
		}
		
		//We flag ourselves as inactive if there are no controllers and the seat isn't set to us if we're not hand-held.
		//We aren't making sentry turrets here.... yet.
		//If this gun type can only have one selected at a time, check that this has the selected index.
		if(controller != null){
			if(playerHolding != null){
				active = true;
			}else{
				PartSeat controllerSeat = (PartSeat) entityOn.getPartAtLocation(entityOn.locationRiderMap.inverse().get(controller));
				active = !placementDefinition.isSpare && controller != null && controllerSeat != null && getItem().equals(controllerSeat.activeGun) && (!definition.gun.fireSolo || entityOn.partsByItem.get(gunItem).get(controllerSeat.gunIndex).equals(this));
			}
		}else{
			active = false;
		}
		
		//Adjust aim to face direction controller is facing.
		//Aim speed depends on gun size, with smaller and shorter guns moving quicker.
		//Pitch and yaw only depend on where the controller is looking, and where the gun is pointed.
		//This allows for guns to be mounted anywhere on a vehicle and at any angle.
		if(active || definition.gun.resetPosition){
			boolean lockedOn = active;
			//If the controller isn't a player, but is a NPC, make them look at the nearest hostile mob.
			//We also get a flag to see if the gun is currently pointed to the hostile mob.
			//If not, then we don't fire the gun, as that'd waste ammo.
			if(active && !(controller instanceof WrapperPlayer)){
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
					firing = true;
				}else{
					firing = false;
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
			}else if(active){
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
				targetYaw = definition.gun.defaultYaw;
				targetPitch = definition.gun.defaultPitch;
			}
			prevOrientation.setTo(currentOrientation);
			
			//Adjust yaw.  We need to normalize the delta here as yaw can go past -180 to 180.
			double deltaYaw = -currentOrientation.getClampedYDelta(targetYaw);
			if(deltaYaw < 0){
				if(deltaYaw < -anglePerTickSpeed){
					deltaYaw = -anglePerTickSpeed;
					lockedOn = false;
				}
				currentOrientation.y += deltaYaw; 
			}else if(deltaYaw > 0){
				if(deltaYaw > anglePerTickSpeed){
					deltaYaw = anglePerTickSpeed;
					lockedOn = false;
				}
				currentOrientation.y += deltaYaw;
			}
			//Apply yaw clamps.
			//If yaw is from -180 to 180, we are a gun that can spin around on its mount.
			//We need to do special logic for this type of gun.
			if(minYawAngle == -180  && maxYawAngle == 180){
				if(currentOrientation.y > 180 ){
					currentOrientation.y -= 360;
					prevOrientation.y -= 360;
				}else if(currentOrientation.y < -180){
					currentOrientation.y += 360;
					prevOrientation.y += 360;
				}
			}else{
				if(currentOrientation.y > maxYawAngle){
					currentOrientation.y = maxYawAngle;
				}
				if(currentOrientation.y < minYawAngle){
					currentOrientation.y = minYawAngle;
				}
			}
			
			//Adjust pitch.
			double deltaPitch = targetPitch - currentOrientation.x;
			if(deltaPitch < 0){
				if(deltaPitch < -anglePerTickSpeed){
					deltaPitch = -anglePerTickSpeed;
					lockedOn = false;
				}
				currentOrientation.x += deltaPitch; 
			}else if(deltaPitch > 0){
				if(deltaPitch > anglePerTickSpeed){
					deltaPitch = anglePerTickSpeed;
					lockedOn = false;
				}
				currentOrientation.x += deltaPitch;
			}
			//Apply pitch clamps.
			if(currentOrientation.x < maxPitchAngle){
				currentOrientation.x = maxPitchAngle;
			}
			if(currentOrientation.x > minPitchAngle){
				currentOrientation.x = minPitchAngle;
			}
			
			//If we told the gun to fire because we saw an entity, but we can't hit it due to the gun clamp don't fire.
			//This keeps NPCs from wasting ammo.
			if(!(controller instanceof WrapperPlayer)){
				if(!lockedOn || currentOrientation.y == minYawAngle || currentOrientation.y == maxYawAngle || currentOrientation.x == minPitchAngle || currentOrientation.x == maxPitchAngle){
					firing = false;
				}
			}
			
			//Set internal orientation for use in other functions as this has changed.
			internalOrientation.setTo(currentOrientation);
			if(!definition.gun.yawIsInternal){
				internalOrientation.y = 0;
			}
			if(!definition.gun.pitchIsInternal){
				internalOrientation.x = 0;
			}
		}else{
			firing = false;
		}
		
		//Decrement cooldown time blocking gun from firing, if we have any.
		if(cooldownTimeRemaining > 0){
			--cooldownTimeRemaining;
		}
		
		//Increment or decrement windup.
		if(firing && windupTimeCurrent < definition.gun.windupTime){
			++windupTimeCurrent;
		}else if(!firing && windupTimeCurrent > 0){
			--windupTimeCurrent;
		}
		windupRotation += windupTimeCurrent;
		
		//If this gun is being told to fire, and we have bullets and are wound up, fire.
		//Don't spawn bullets on the server, as they will cause lots of lag and network traffic.
		//Instead, spawn them on the clients, and then send back hit data to the server.
		//This is backwards from what usually happens, and can possibly be hacked, but it's FAR
		//easier on MC to leave clients to handle lots of bullets than the server and network systems.
		//We still need to run the gun code on the server, however, as we need to mess with inventory.
		if(firing && windupTimeCurrent == definition.gun.windupTime && bulletsLeft > 0 && cooldownTimeRemaining == 0 && (!definition.gun.isSemiAuto || !firedThisCommand)){
			//First update gun number so we know if we need to apply a cam offset.
			//We would fire a bullet here, but that's for the SFXSystem to handle, not the update loop.
			//Make sure to add-on an offset to our firing point to allow for multi-gun units.
			List<APart> allGuns = entityOn.partsByItem.get(gunItem);
			long millisecondCamOffset = definition.gun.fireSolo ? 0 : (long) (definition.gun.fireDelay*(1000D/20D)*(allGuns.indexOf(this))/allGuns.size());
			cooldownTimeRemaining = definition.gun.fireDelay;
			timeToFire = System.currentTimeMillis() + millisecondCamOffset;
			lastController = controller;
			firedThisCommand = true;
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
		if(!firing){
			firedThisCommand = false;
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
						if(item instanceof ItemPart){
							if(tryToReload((ItemPart) item)){
								//Bullet is right type, and we can fit it.  Remove from player's inventory and add to the gun.
								inventory.decrementSlot(i);
								return;
							}
						}
					}
				}
			}else{
				if(definition.gun.autoReload){
					//Iterate through all the inventory slots in crates to try to find matching ammo.
					for(APart part : entityOn.parts){
						if(part instanceof PartInteractable){
							WrapperInventory inventory = ((PartInteractable) part).inventory;
							if(inventory != null && part.definition.interactable.feedsVehicles){
								for(byte i=0; i<inventory.getSize(); ++i){
									AItemBase item = inventory.getItemInSlot(i);
									if(item instanceof ItemPart){
										if(tryToReload((ItemPart) item)){
											//Bullet is right type, and we can fit it.  Remove from crate and add to the gun.
											//Return here to ensure we don't set the loadedBullet to blank since we found bullets.
											inventory.decrementSlot(i);
											return;
										}
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
		//This comes after the reloading block as we need a 0/1 state-change for the various animations.
		if(reloadTimeRemaining > 0){
			--reloadTimeRemaining;
		}else if(bulletsReloading != 0){
			bulletsLeft += bulletsReloading;
			bulletsReloading = 0;
		}
	}
	
	/**
	 * Attempts to reload the gun with the passed-in item.  Returns true if the item is a bullet
	 * and was loaded, false if not.  Provider methods are then called for packet callbacks.
	 */
	public boolean tryToReload(ItemPart part){
		//Only fill bullets if we match the bullet already in the gun, or if our diameter matches, or if we got a signal on the client.
		//Also don't fill bullets if we are currently reloading bullets.
		if(part.definition.bullet != null){
			boolean isNewBulletValid = part.definition.bullet.diameter == definition.gun.diameter && part.definition.bullet.caseLength >= definition.gun.minCaseLength && part.definition.bullet.caseLength <= definition.gun.maxCaseLength; 
			if((bulletsReloading == 0 && (loadedBullet == null ? isNewBulletValid : loadedBullet.equals(part))) || world.isClient()){
				//Make sure we don't over-fill the gun.
				if(part.definition.bullet.quantity + bulletsLeft <= definition.gun.capacity || world.isClient()){
					loadedBullet = part;
					bulletsReloading = part.definition.bullet.quantity;
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
	public void updatePositionAndRotation(){
		super.updatePositionAndRotation();
		//Need to use prev orientation here, as otherwise we get into a feedback loop.
		if(!definition.gun.yawIsInternal){
			localAngles.add(0, prevOrientation.y, 0);
		}
		if(!definition.gun.pitchIsInternal){
			localAngles.add(prevOrientation.x, 0, 0);
		}
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
	public void spawnParticles(){
		if(timeToFire != lastTimeFired && System.currentTimeMillis() >= timeToFire && bulletsLeft > 0){
			//Fire a bullet by spawning it with the appropriate muzzle velocity and angle.
			//Angle is based on the current gun orientation, plus a slight fudge-factor.
			//This is based on the barrel length and shell size.
			Point3d spreadAngle = new Point3d((Math.random() - 0.5F)*(10*definition.gun.diameter/(definition.gun.length*1000)), (Math.random() - 0.5F)*(10*definition.gun.diameter/(definition.gun.length*1000)), 0D);
			spreadAngle.add(internalOrientation);
			
			//Set the bullet's direction the the provider's orientation.
			Point3d bulletDirection = new Point3d(0D, 0D, 1D).rotateFine(spreadAngle);
			bulletDirection.rotateFine(localAngles).rotateFine(entityOn.angles);
			
			//If we have a gun with a muzzle velocity, set the bullet's velocity to that.  Otherwise set it to the vehicle's velocity.
			Point3d bulletVelocity;
			if(definition.gun.muzzleVelocity > 0){
				bulletVelocity = bulletDirection.copy().multiply(definition.gun.muzzleVelocity/20D/10D);
			}else{
				bulletVelocity = motion.copy().multiply(EntityVehicleF_Physics.SPEED_FACTOR);
			}
			
			//Get the bullet's initial position, adjusted for barrel length and gun orientation.
			//Then move the bullet to the appropriate firing position.
			Point3d bulletPosition = getFiringOrigin();
			bulletPosition.rotateFine(localAngles).rotateFine(entityOn.angles);
			bulletPosition.add(position);

			//Add the bullet as a particle.
			//If the bullet is a missile, give it a target.
			if (loadedBullet.definition.bullet.turnFactor > 0){
				//First find the block the controller is looking at, if possible
				double maxDistance = 2000D;
				Point3d lineOfSight = lastController.getLineOfSight(maxDistance);
				Point3d blockTarget = world.getBlockHit(lastController.getPosition().add(0D, lastController.getEyeHeight(), 0D), lineOfSight);
				
				//Try to find the closest entity between the controller and the block
				//If no block was found, set target position to maxDistance in the direction of the line of sight
				if(blockTarget != null){
					maxDistance = lastController.getPosition().distanceTo(blockTarget);
				}else{
					blockTarget = lastController.getPosition().add(0D, lastController.getEyeHeight(), 0D).add(lineOfSight);
				}
				WrapperEntity entityTarget = world.getEntityLookingAt(lastController, (float) maxDistance);
				
				//Fire a missile with the found entity as its target, if valid
				//Otherwise, fall back to the block target
				if(entityTarget != null) {
					InterfaceRender.spawnParticle(new ParticleMissile(bulletPosition, bulletVelocity, bulletDirection, loadedBullet, this, lastController, entityTarget));
				}
				else {
					InterfaceRender.spawnParticle(new ParticleMissile(bulletPosition, bulletVelocity, bulletDirection, loadedBullet, this, lastController, blockTarget));
				}
			}
			else {
				InterfaceRender.spawnParticle(new ParticleBullet(bulletPosition, bulletVelocity, bulletDirection, loadedBullet, this, lastController));
			}
			
			//Do effects.
			if(definition.gun.particleObjects != null){
				spawnEffectParticles();
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
	private Point3d getFiringOrigin(){
		Point3d firingOrigin = new Point3d(0, 0, definition.gun.length).rotateFine(internalOrientation);
		//If muzzle count is the same as capacity, use the muzzles in order
		//Otherwise, iterate through the available muzzles.
		if(definition.gun.muzzlePositions != null) {
			currentMuzzle = definition.gun.muzzlePositions.size() == definition.gun.capacity ? definition.gun.capacity - bulletsLeft : bulletsFired%definition.gun.muzzlePositions.size();
			firingOrigin.add(definition.gun.muzzlePositions.get(currentMuzzle));
			
		}
		return firingOrigin;
	}
	
	/**
	 * Helper method for spawning particles.  This spawns the smoke and other particles.
	 * The actual bullet is spawned in {@link #spawnParticles()}.
	 */
	private void spawnEffectParticles(){
		for(JSONParticleObject gunParticle : definition.gun.particleObjects){
			//Set initial velocity to the be opposite the direction of motion in the magnitude of the defined velocity.
			//Add a little variation to this.
			Point3d particleVelocity = gunParticle.velocityVector.copy().multiply(1/20D/10D).rotateFine(internalOrientation);
			particleVelocity.rotateFine(localAngles).rotateFine(entityOn.angles);
			
			//Get the particle's initial position.
			Point3d particlePosition = position.copy();
			if(gunParticle.pos != null) {
				particlePosition.add(gunParticle.pos.copy().rotateFine(internalOrientation));
				particlePosition.rotateFine(localAngles).rotateFine(entityOn.angles);
			}

			//Spawn the appropriate type and amount of particles.
			//Change default values from 0 to 1.
			if(gunParticle.quantity == 0) gunParticle.quantity = 1;
			if(gunParticle.scale == 0f && gunParticle.toScale == 0f) gunParticle.scale = 1f;
			AParticle currentParticle;
			switch(gunParticle.type) {
				case SMOKE: {
					if(gunParticle.transparency == 0f && gunParticle.toTransparency == 0F) gunParticle.transparency = 1f;
					for(int i=0; i<gunParticle.quantity; i++) {
						currentParticle = new ParticleSuspendedSmoke(world, particlePosition, particleVelocity.copy(), gunParticle);
						InterfaceRender.spawnParticle(currentParticle);
					}
					break;
				}
				case FLAME: {
					for(int i=0; i<gunParticle.quantity; i++) {
						currentParticle = new ParticleFlame(world, particlePosition, particleVelocity.copy().add(new Point3d(0.04*Math.random(), 0.04*Math.random(), 0.04*Math.random())), gunParticle.scale);
						currentParticle.deltaScale = (gunParticle.toScale - currentParticle.scale) / (currentParticle.maxAge - currentParticle.age);
						InterfaceRender.spawnParticle(currentParticle);
					}
					break;
				}
			}
		}
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setBoolean("firing", firing);
		data.setInteger("shotsFired", bulletsFired);
		data.setInteger("bulletsLeft", bulletsLeft);
		data.setInteger("bulletsReloading", bulletsReloading);
		data.setPoint3d("currentOrientation", currentOrientation);
		if(loadedBullet != null){
			data.setString("loadedBulletPack", loadedBullet.definition.packID);
			data.setString("loadedBulletName", loadedBullet.definition.systemName);
		}
	}
}
