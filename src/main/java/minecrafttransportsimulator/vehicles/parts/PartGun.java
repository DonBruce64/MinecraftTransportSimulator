package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.baseclasses.Gun;
import minecrafttransportsimulator.baseclasses.IGunProvider;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperInventory;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.rendering.components.IParticleProvider;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public class PartGun extends APart implements IParticleProvider, IGunProvider{
	
	public final Gun internalGun;
		
	public PartGun(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, WrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, item, data, parentPart);
		//Set min/max yaw/pitch angles based on our definition and the vehicle definition.
		//If the vehicle's min/max yaw is -180 to 180, set it to that.  Otherwise, get the max bounds.
		//Yaw/Pitch set to 0 is ignored as it's assumed to be un-defined.
		final double minYawAngle;
		final double maxYawAngle;
		final double minPitchAngle;
		final double maxPitchAngle;
		if(vehicleDefinition.minYaw == -180 && vehicleDefinition.maxYaw == 180){
			minYawAngle = -180;
			maxYawAngle = 180;
		}else{
			if(definition.gun.minYaw != 0){
				minYawAngle = vehicleDefinition.minYaw != 0 ? Math.max(definition.gun.minYaw, vehicleDefinition.minYaw) : definition.gun.minYaw;
			}else{
				minYawAngle =  vehicleDefinition.minYaw;
			}
			if(definition.gun.maxYaw != 0){
				maxYawAngle = vehicleDefinition.maxYaw != 0 ? Math.min(definition.gun.maxYaw, vehicleDefinition.maxYaw) : definition.gun.maxYaw;
			}else{
				maxYawAngle =  vehicleDefinition.maxYaw;
			}
		}
		if(definition.gun.minPitch != 0){
			minPitchAngle = vehicleDefinition.minPitch != 0 ? -Math.max(definition.gun.minPitch, vehicleDefinition.minPitch) : -definition.gun.minPitch;
		}else{
			minPitchAngle = -vehicleDefinition.minPitch;
		}	
		if(definition.gun.maxPitch != 0){
			maxPitchAngle = vehicleDefinition.maxPitch != 0 ? -Math.min(definition.gun.maxPitch, vehicleDefinition.maxPitch) : -definition.gun.maxPitch;
		}else{
			maxPitchAngle = -vehicleDefinition.maxPitch;
		}
		
		//Create a new Gun object.
		this.internalGun = new Gun(this, definition, minYawAngle, maxYawAngle, minPitchAngle, maxPitchAngle, data);
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		//Check to see if we have any bullets in our hands.
		//If so, try to re-load this gun with them.
		AItemBase heldItem = player.getHeldItem();
		if(heldItem instanceof ItemPart){
			if(internalGun.tryToReload((ItemPart) heldItem) && !player.isCreative()){
				player.getInventory().removeItem(heldItem, null);
			}
		}
		return true;
    }
	
	@Override
	public void update(){
		super.update();
		internalGun.update();
	}
	
	@Override
	public WrapperNBT getData(){
		WrapperNBT data = super.getData();
		internalGun.save(data);
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
	public Point3d getProviderRotation(){
		return vehicle.angles.copy().add(totalRotation);
	}
	
	@Override
	public void reloadGunBullets(){
		if(definition.gun.autoReload){
			//Iterate through all the inventory slots in crates to try to find matching ammo.
			for(APart part : vehicle.parts){
				if(part instanceof PartInteractable){
					WrapperInventory inventory = ((PartInteractable) part).inventory;
					if(inventory != null && part.definition.interactable.feedsVehicles){
						for(byte i=0; i<inventory.getSize(); ++i){
							AItemBase item = inventory.getItemInSlot(i);
							if(item instanceof ItemPart){
								if(internalGun.tryToReload((ItemPart) item)){
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

	@Override
	public WrapperEntity getController(){
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
					WrapperEntity controller = vehicle.locationRiderMap.get(vehiclePart.placementOffset);
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
	public boolean isGunActive(WrapperEntity controller){
		//We flag ourselves as inactive if there are no controllers or the seat isn't set to us.
		//We aren't making sentry turrets here.... yet.
		//If this gun type can only have one selected at a time, check that this has the selected index.
		PartSeat controllerSeat = (PartSeat) vehicle.getPartAtLocation(vehicle.locationRiderMap.inverse().get(controller));
		return controller != null && getItem().equals(controllerSeat.activeGun) && (!definition.gun.fireSolo || vehicle.guns.get(getItem()).get(controllerSeat.gunIndex).equals(this));
	}
	
	@Override
	public double getDesiredYaw(WrapperEntity controller){
		return controller.getHeadYaw() - (vehicle.angles.y + totalRotation.y);
	}
	
	@Override
	public double getDesiredPitch(WrapperEntity controller){
		double vehiclePitchContribution = (vehicle.angles.x + totalRotation.x)*Math.cos(Math.toRadians(totalRotation.y + internalGun.currentOrientation.y));
		double vehicleRollContribution = -(vehicle.angles.z + totalRotation.z)*Math.sin(Math.toRadians(totalRotation.y + internalGun.currentOrientation.y));
		return controller.getPitch() - (vehiclePitchContribution + vehicleRollContribution);
	}

	@Override
	public int getGunNumber(){
		int gunNumber = 1;
		for(PartGun vehicleGun : vehicle.guns.get(getItem())){
			if(vehicleGun.equals(this)){
				break;
			}else{
				++gunNumber;
			}
		}
		return gunNumber;
	}
	
	@Override
	public int getTotalGuns(){
		return vehicle.guns.get(getItem()).size();
	}
	
	@Override
	public void updateProviderSound(SoundInstance sound){
		super.updateProviderSound(sound);
		internalGun.updateProviderSound(sound);
	}
	
	@Override
	public void startSounds(){
		super.startSounds();
		internalGun.startSounds();
	}
	
	@Override
	public void spawnParticles(){
		internalGun.spawnParticles();
	}
	
	//--------------------START OF GUN SOUND METHODS--------------------
	//FIXME remove when gun code is good.
	/*
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
			bulletPosition.add(getFiringPosition().rotateFine(totalRotation).rotateFine(vehicleFactoredAngles));

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
				WrapperEntity entityTarget = this.vehicle.world.getEntityLookingAt(lastController, (float) maxDistance);
				
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
			MasterLoader.audioInterface.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_firing"));
			if(definition.gun.JSONParticleObjects != null) {
				spawnEffectParticles();
			}
			lastTimeFired = timeToFire;
			
			//Remove a bullet from the count and add shots fired.
			--bulletsLeft;
			++bulletsFired;
		}
	}
	
	//Rather than spawn a bullet particle, this method spawns
	//a particle related to some special effect, such as gun smoke.
	private void spawnEffectParticles() {
		for(JSONParticleObject JSONParticleObject : definition.gun.JSONParticleObjects) {
			//Set initial velocity to the be opposite the direction of motion in the magnitude of the defined velocity.
			//Add a little variation to this.
			Point3d particleVelocity = JSONParticleObject.velocityVector.copy().multiply(1/20D/10D).rotateFine(currentOrientation).rotateFine(totalRotation).rotateFine(vehicle.angles.copy());
			
			//Get the particle's initial position.
			Point3d particlePosition = worldPos.copy();
			if(JSONParticleObject.pos != null) {
				particlePosition.add(JSONParticleObject.pos.copy().rotateFine(currentOrientation).rotateFine(totalRotation).rotateFine(vehicle.angles.copy()));
			}

			//Spawn the appropriate type and amount of particles.
			//Change default values from 0 to 1.
			if(JSONParticleObject.quantity == 0) JSONParticleObject.quantity = 1;
			if(JSONParticleObject.scale == 0f && JSONParticleObject.toScale == 0f) JSONParticleObject.scale = 1f;
			AParticle currentParticle;
			switch(JSONParticleObject.type) {
				case "smoke": {
					if(JSONParticleObject.transparency == 0f && JSONParticleObject.toTransparency == 0F) JSONParticleObject.transparency = 1f;
					for(int i=0; i<JSONParticleObject.quantity; i++) {
						currentParticle = new ParticleSuspendedSmoke(vehicle.world, particlePosition, particleVelocity.copy(), JSONParticleObject);
						InterfaceRender.spawnParticle(currentParticle);
					}
					break;
				}
				case "flame": {
					for(int i=0; i<JSONParticleObject.quantity; i++) {
						currentParticle = new ParticleFlame(vehicle.world, particlePosition, particleVelocity.copy().add(new Point3d(0.04*Math.random(), 0.04*Math.random(), 0.04*Math.random())), JSONParticleObject.scale);
						currentParticle.deltaScale = (JSONParticleObject.toScale - currentParticle.scale) / (currentParticle.maxAge - currentParticle.age);
						InterfaceRender.spawnParticle(currentParticle);
					}
					break;
				}
			}
		}
	}*/
}
