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
	public Point3d getPositionRotation(float partialTicks){
		Point3d rotation = super.getPositionRotation(partialTicks);
		if(!definition.gun.yawIsInternal){
			rotation.add(0, internalGun.prevOrientation.y + (internalGun.currentOrientation.y - internalGun.prevOrientation.y)*partialTicks, 0);
		}
		if(!definition.gun.pitchIsInternal){
			rotation.add(internalGun.prevOrientation.x + (internalGun.currentOrientation.x - internalGun.prevOrientation.x)*partialTicks, 0, 0);
		}
		return rotation;
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
	public void orientToProvider(Point3d point){
		point.rotateFine(totalRotation).rotateFine(vehicle.angles);
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
		if(controller != null){
			PartSeat controllerSeat = (PartSeat) vehicle.getPartAtLocation(vehicle.locationRiderMap.inverse().get(controller));
			return !vehicleDefinition.isSpare && controller != null && getItem().equals(controllerSeat.activeGun) && (!definition.gun.fireSolo || vehicle.guns.get(getItem()).get(controllerSeat.gunIndex).equals(this));
		}else{
			return false;
		}
	}
	
	@Override
	public double getDesiredYaw(WrapperEntity controller){
		//Get the actual angle this gun is as.  This needs to remove all part-based animations we applied to this gun.
		//This is because if the gun moves based on those animations, we shouldn't take them into account.
		double partYawContribution = definition.gun.yawIsInternal ? totalRotation.y : totalRotation.y - internalGun.prevOrientation.y;
		return controller.getYaw() - (vehicle.angles.y + partYawContribution);
	}
	
	@Override
	public double getDesiredPitch(WrapperEntity controller){
		//For pitch, we need to find the relative angle of the player to the vehicle's 0-pitch plane.
		//When the player rotates their head, they don't do so relative to the pitch of the vehicle the gun is on, 
		//so a yaw change can result in a pitch change.
		double partYawContribution = definition.gun.yawIsInternal ? totalRotation.y : totalRotation.y - internalGun.prevOrientation.y;
		double partPitchContribution = definition.gun.pitchIsInternal ? totalRotation.x : totalRotation.x - internalGun.prevOrientation.x;
		double vehiclePitchContribution = (vehicle.angles.x + partPitchContribution)*Math.cos(Math.toRadians(partYawContribution));
		double vehicleRollContribution = (vehicle.angles.z + totalRotation.z)*Math.sin(Math.toRadians(partYawContribution));
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
}
