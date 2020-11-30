package minecrafttransportsimulator.vehicles.parts;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartGun;
import minecrafttransportsimulator.rendering.components.IVehiclePartFXProvider;
import minecrafttransportsimulator.rendering.instances.ParticleBullet;
import minecrafttransportsimulator.rendering.instances.ParticleMissile;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public class PartGun extends APart implements IVehiclePartFXProvider{
	//Stored variables used to determine bullet firing behavior.
	public int bulletsFired;
	public int bulletsLeft;
	public int bulletsReloading;
	public int gunNumber;
	public int currentMuzzle;
	public Point3d currentOrientation;
	public Point3d prevOrientation;
	private ItemPart loadedBullet;
	
	//These variables are used during firing and will be reset on entity loading.
	public boolean firing;
	public boolean active;
	public int cooldownTimeRemaining;
	public int reloadTimeRemaining;
	public int windupTimeCurrent;
	public int windupRotation;
	private IWrapperEntity lastController;
	private long lastTimeFired;
	private long timeToFire;
	private final double anglePerTickSpeed;
	public final List<Integer> bulletsHitOnServer = new ArrayList<Integer>();
		
	public PartGun(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, IWrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, item, data, parentPart);
		this.bulletsFired = data.getInteger("shotsFired");
		this.bulletsLeft = data.getInteger("bulletsLeft");
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
		this.anglePerTickSpeed = (50/definition.gun.diameter + 1/definition.gun.length);
	}
	
	@Override
	public boolean interact(IWrapperPlayer player){
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
		prevOrientation.setTo(currentOrientation);
		
		//Get the current controller for this gun.
		IWrapperEntity controller = getCurrentController();
		
		//Set the active state.
		//We flag ourselves as inactive if there are no controllers or the seat isn't set to us.
		//We aren't making sentry turrets here.... yet.
		//If this gun type can only have one selected at a time, check that this has the selected index.
		PartSeat controllerSeat = (PartSeat) vehicle.getPartAtLocation(vehicle.locationRiderMap.inverse().get(controller));
		active = controller != null && getItem().equals(controllerSeat.activeGun) && (!definition.gun.fireSolo || vehicle.guns.get(getItem()).get(controllerSeat.gunIndex).equals(this));
		
		
		//Adjust aim to face direction controller is facing.
		//Aim speed depends on gun size, with smaller and shorter guns moving quicker.
		//Pitch and yaw only depend on where the player is looking, and where the gun is pointed.
		//This allows for guns to be mounted anywhere on a vehicle and at any angle.
		if(active){
			//If the controller isn't a player, but is a NPC, make them look at the nearest hostile mob.
			//We also get a flag to see if the gun is currently pointed to the hostile mob.
			//If not, then we don't fire the gun, as that'd waste ammo.
			boolean lockedOn = true;
			if(!(controller instanceof IWrapperPlayer)){
				IWrapperEntity hostile = vehicle.world.getNearestHostile(controller, 48);
				if(hostile != null){
					//Need to aim for the middle of the mob, not their base (feet).
					Point3d hostilePosition = hostile.getPosition().add(0D, hostile.getEyeHeight()/2D, 0D);
					//Make the gunner account for bullet delay and movement of the hostile.
					//This makes them track better when the target is moving.
					double ticksToTarget = hostilePosition.distanceTo(worldPos)/definition.gun.muzzleVelocity/20D/10D;
					hostilePosition.add(hostile.getVelocity().copy().multiply(ticksToTarget));
					double yawHostile = Math.toDegrees(Math.atan2(hostilePosition.x - worldPos.x, hostilePosition.z - worldPos.z));
					double pitchHostile = -Math.toDegrees(Math.atan2(hostilePosition.y - worldPos.y, Math.hypot(hostilePosition.x - worldPos.x, hostilePosition.z - worldPos.z)));
					controller.setYaw(yawHostile);
					controller.setHeadYaw(yawHostile);
					controller.setPitch(pitchHostile);
					firing = true;
				}else{
					firing = false;
				}
			}
			
			//Aadjust yaw.  We need to normalize the delta here as yaw can go past -180 to 180.
			double deltaYaw = controller.getHeadYaw() - (vehicle.angles.y + totalRotation.y + currentOrientation.y);
			while(deltaYaw > 180){
				deltaYaw -= 360;
			}
			while(deltaYaw < -180){
				deltaYaw += 360;
			}
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
			if(definition.gun.minYaw == -180  && definition.gun.maxYaw == 180){
				if(currentOrientation.y > 180 ){
					currentOrientation.y -= 360;
					prevOrientation.y -= 360;
				}else if(currentOrientation.y < -180){
					currentOrientation.y += 360;
					prevOrientation.y += 360;
				}
			}else{
				if(currentOrientation.y > definition.gun.maxYaw){
					currentOrientation.y = definition.gun.maxYaw;
				}
				if(currentOrientation.y < definition.gun.minYaw){
					currentOrientation.y = definition.gun.minYaw;
				}
			}
			
			//Adjust pitch.
			//For pitch, we need to find the relative angle of the player to the vehicle's 0-pitch plane.
			//When the player rotates their head, they don't do so relative to the pitch of the vehicle the gun is on, 
			//so a yaw change can result in a pitch change.
			double vehiclePitchContribution = (vehicle.angles.x + totalRotation.x)*Math.cos(Math.toRadians(totalRotation.y + currentOrientation.y));
			double vehicleRollContribution = -(vehicle.angles.z + totalRotation.z)*Math.sin(Math.toRadians(totalRotation.y + currentOrientation.y));
			double deltaPitch = controller.getPitch() - (vehiclePitchContribution + vehicleRollContribution + currentOrientation.x);
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
			if(currentOrientation.x < -definition.gun.maxPitch){
				currentOrientation.x = -definition.gun.maxPitch;
			}
			if(currentOrientation.x > -definition.gun.minPitch){
				currentOrientation.x = -definition.gun.minPitch;
			}
			
			//If we told the gun to fire becase we saw an entity, but we can't hit it due to the gun clamp don't fire.
			//This keeps NPCs from wasting ammo.
			if(!(controller instanceof IWrapperPlayer)){
				if(!lockedOn || currentOrientation.y == definition.gun.maxYaw || currentOrientation.y == definition.gun.minYaw || currentOrientation.x == -definition.gun.minPitch || currentOrientation.x == -definition.gun.maxPitch){
					firing = false;
				}
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
			if(windupTimeCurrent == 0 && vehicle.world.isClient()){
				MasterLoader.audioInterface.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_winding", true));
			}
			++windupTimeCurrent;
		}else if(!firing && windupTimeCurrent > 0){
			--windupTimeCurrent;
		}
		windupRotation += windupTimeCurrent;
		
		//If we are reloading, decrement the reloading timer.
		//If we are done reloading, add the new bullets.
		if(reloadTimeRemaining > 0){
			--reloadTimeRemaining;
		}else if(bulletsReloading != 0){
			bulletsLeft += bulletsReloading;
			bulletsReloading = 0;
		}
		
		//If this gun is being told to fire, and we have bullets and are wound up, fire.
		//Don't spawn bullets on the server, as they will cause lots of lag and network traffic.
		//Instead, spawn them on the clients, and then send back hit data to the server.
		//This is backwards from what usually happens, and can possibly be hacked, but it's FAR
		//easier on MC to leave clients to handle lots of bullets than the server and network systems.
		//We still need to run the gun code on the server, however, as we need to mess with inventory.
		if(firing && windupTimeCurrent == definition.gun.windupTime && bulletsLeft > 0 && cooldownTimeRemaining == 0){
			//First update gun number so we know if we need to apply a cam offset.
			//Get the gun number based on how many guns the vehicle has.
			gunNumber = 1;
			for(PartGun gun : vehicle.guns.get(getItem())){
				if(gun.equals(this)){
					break;
				}else{
					++gunNumber;
				}
			}
			
			//We would fire a bullet here, but that's for the SFXSystem to handle, not the update loop.
			//Make sure to add-on an offset to our firing point to allow for multi-gun units.
			long millisecondCamOffset = definition.gun.fireSolo ? 0 : (long) (definition.gun.fireDelay*(1000D/20D)*(gunNumber - 1D)/vehicle.guns.get(getItem()).size());
			cooldownTimeRemaining = definition.gun.fireDelay;
			timeToFire = System.currentTimeMillis() + millisecondCamOffset;
			lastController = controller;
			if(!vehicle.world.isClient()){
				//Only remove bullets from the server.  We remove them from the client when they spawn.
				--bulletsLeft;
				++bulletsFired;
				if(bulletsLeft == 0){
					loadedBullet = null;
				}
			}
		}
		
		//If we can accept bullets, and aren't currently loading any, re-load ourselves from any vehicle inventories.
		//This only works if the gun is set to auto-reload.
		//While the reload method checks for reload time, we check here to save on code processing.
		//No sense in looking for bullets if we can't load them anyways.
		if(!vehicle.world.isClient() && definition.gun.autoReload && bulletsLeft < definition.gun.capacity && bulletsReloading == 0){
			//Iterate through all the inventory slots in crates to try to find matching ammo.
			for(APart part : vehicle.parts){
				if(part instanceof PartInteractable){
					IWrapperInventory inventory = ((PartInteractable) part).inventory;
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
			//If we are on the server, we need to set the bullet as null to allow for new bullets to be loaded.
			//If we are on the client, we need to leave this here, as the client might still be processing bullet firing.
			//If we set it null during that time, the bullet would be fired and not know what it was.
			if(!vehicle.world.isClient()){
				loadedBullet = null;
			}
		}
	}
	
	/**
	 * Attempts to reload the gun with the passed-in part.  Returns true if the part is a bullet
	 * and was loaded, false if not.  Responsible for packet callbacks and playing sounds.
	 */
	public boolean tryToReload(ItemPart part){
		if(part.definition.bullet != null){
			//Only fill bullets if we match the bullet already in the gun, or if our diameter matches, or if we got a signal on the client.
			//Also don't fill bullets if we are currently reloading bullets.
			if((bulletsReloading == 0 && (loadedBullet == null ? part.definition.bullet.diameter == definition.gun.diameter : loadedBullet.equals(part))) || vehicle.world.isClient()){
				//Make sure we don't over-fill the gun.
				if(part.definition.bullet.quantity + bulletsLeft <= definition.gun.capacity || vehicle.world.isClient()){
					loadedBullet = part;
					bulletsReloading = part.definition.bullet.quantity;
					reloadTimeRemaining = definition.gun.reloadTime;
					if(vehicle.world.isClient()){
						MasterLoader.audioInterface.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_reloading"));
					}else{
						MasterLoader.networkInterface.sendToAllClients(new PacketVehiclePartGun(this, loadedBullet));
					}
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Helper method to get the current controller of this gun.
	 */
	public IWrapperEntity getCurrentController(){
		//Check our parent part, if we have one.
		if(parentPart instanceof PartSeat){
			return vehicle.locationRiderMap.get(parentPart.placementOffset);
		}
		
		//Check any child parts.
		for(APart childPart : childParts){
			if(childPart instanceof PartSeat){
				return vehicle.locationRiderMap.get(childPart.placementOffset);
			}
		}
		
		//Not parent or child.  Get main vehicle controller.
		//Check all controllers in case there's multiple controller seats.
		for(APart vehiclePart : vehicle.parts){
			if(vehiclePart instanceof PartSeat){
				if(vehiclePart.vehicleDefinition.isController){
					IWrapperEntity controller = vehicle.locationRiderMap.get(vehiclePart.placementOffset);
					if(controller != null){
						return controller;
					}
				}
			}
		}
		
		//No controller found.
		return null;
	}
	
	public Point3d getFiringPosition() {
		//If muzzle count is the same as capacity, use the muzzles in order
		//Otherwise, iterate through the available muzzles
		if (definition.gun.muzzlePositions != null) {
			currentMuzzle = definition.gun.muzzlePositions.size() == definition.gun.capacity ? definition.gun.capacity - this.bulletsLeft : this.bulletsFired % definition.gun.muzzlePositions.size();
			return definition.gun.muzzlePositions.get(currentMuzzle).copy();
		}
		
		//If no muzzlePositions are defined, no offset will be used
		//This will also be returned if there was an issue finding the muzzle
		return new Point3d(0D, 0D, 0D);
	}
	
	@Override
	public IWrapperNBT getData(){
		IWrapperNBT data = super.getData();
		data.setInteger("shotsFired", bulletsFired);
		data.setInteger("bulletsLeft", bulletsLeft);
		data.setPoint3d("currentOrientation", currentOrientation);
		if(loadedBullet != null){
			data.setString("loadedBulletPack", loadedBullet.definition.packID);
			data.setString("loadedBulletName", loadedBullet.definition.systemName);
		}
		return data;
	}
	
	@Override
	public float getWidth(){
		return 0.75F;
	}

	@Override
	public float getHeight(){
		return 0.75F;
	}

	@Override
	public Point3d getActionRotation(float partialTicks){
		Point3d rotation = currentOrientation.copy().subtract(prevOrientation).multiply((double)partialTicks).add(currentOrientation);
		//Don't return pitch if we are a turret.
		if(definition.gun.isTurret){
			rotation.x = 0;
		}
		return rotation;
	}
	
	//--------------------START OF GUN SOUND METHODS--------------------	
	@Override
	public void updateProviderSound(SoundInstance sound){
		super.updateProviderSound(sound);
		//Adjust winding sound pitch to match winding value and stop looping if we aren't winding.
		if(sound.soundName.endsWith("_winding")){
			if(windupTimeCurrent == 0){
				sound.stop();
			}else{
				float windupPercent = windupTimeCurrent/(float)definition.gun.windupTime;
				sound.pitch = (float) 0.25F + 0.75F*windupPercent;
				sound.volume = 0.25F + 0.75F*windupPercent;
			}
		}
	}
	
	@Override
	public void startSounds(){
		if(windupTimeCurrent > 0){
			MasterLoader.audioInterface.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_winding", true));
		}
	}
		
	@Override
	public void spawnParticles(){
		if(timeToFire != lastTimeFired && System.currentTimeMillis() >= timeToFire && bulletsLeft > 0){
			//Fire a bullet by spawning it with the appropriate muzzle velocity and angle.
			//Angle is based on the current gun orientation, plus a slight fudge-factor.
			//This is based on the barrel length and shell size.
			Point3d vehicleFactoredAngles = vehicle.angles.copy().add((Math.random() - 0.5F)*(10*definition.gun.diameter/(definition.gun.length*1000)), (Math.random() - 0.5F)*(10*definition.gun.diameter/(definition.gun.length*1000)), 0D);
			
			//Set initial velocity to the vehicle's velocity, plus the gun muzzle velocity at the specified orientation.
			Point3d bulletDirection = new Point3d(0D, 0D, 1D).rotateFine(currentOrientation).rotateFine(totalRotation).rotateFine(vehicleFactoredAngles);
			Point3d bulletVelocity = vehicle.motion.copy().multiply(vehicle.SPEED_FACTOR).add(bulletDirection.multiply(definition.gun.muzzleVelocity/20D/10D));
			
			//Get the bullet's initial position, adjusted for barrel length and gun orientation.
			//Then move the bullet to the appropriate firing position.
			Point3d bulletPosition = new Point3d(0D, 0D, definition.gun.length).rotateFine(currentOrientation).rotateFine(totalRotation).rotateFine(vehicleFactoredAngles).add(worldPos);
			bulletPosition.add(getFiringPosition().rotateFine(getActionRotation(0)).rotateFine(totalRotation).rotateFine(vehicleFactoredAngles));

			//Add the bullet as a particle.
			//If the bullet is a missile, give it a target.
			if (loadedBullet.definition.bullet.turnFactor > 0) {
				//First find the block the controller is looking at, if possible
				double maxDistance = 2000D;
				Point3d lineOfSight = lastController.getLineOfSight((float) maxDistance);
				Point3i blockTarget = this.vehicle.world.getBlockHit(lastController.getPosition().add(0D, lastController.getEyeHeight(), 0D), lineOfSight);
				
				//Try to find the closest entity between the controller and the block
				//If no block was found, set target position to maxDistance in the direction of the line of sight
				if(blockTarget != null) {
					maxDistance = lastController.getPosition().distanceTo(blockTarget);
				}
				else {
					blockTarget = new Point3i(lastController.getPosition().add(0D, lastController.getEyeHeight(), 0D).add(lineOfSight));
				}
				IWrapperEntity entityTarget = this.vehicle.world.getEntityLookingAt(lastController, (float) maxDistance);
				
				//Fire a missile with the found entity as its target, if valid
				//Otherwise, fall back to the block target
				if(entityTarget != null) {
					MasterLoader.renderInterface.spawnParticle(new ParticleMissile(bulletPosition, bulletVelocity, bulletDirection, loadedBullet, this, lastController, entityTarget));
				}
				else {
					MasterLoader.renderInterface.spawnParticle(new ParticleMissile(bulletPosition, bulletVelocity, bulletDirection, loadedBullet, this, lastController, blockTarget));
				}
			}
			else {
				MasterLoader.renderInterface.spawnParticle(new ParticleBullet(bulletPosition, bulletVelocity, bulletDirection, loadedBullet, this, lastController));
			}
			MasterLoader.audioInterface.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_firing"));
			lastTimeFired = timeToFire;
			
			//Remove a bullet from the count and add shots fired.
			--bulletsLeft;
			++bulletsFired;
		}
	}
}
