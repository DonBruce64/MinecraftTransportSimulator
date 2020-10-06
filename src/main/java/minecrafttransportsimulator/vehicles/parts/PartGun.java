package minecrafttransportsimulator.vehicles.parts;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
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
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public class PartGun extends APart implements IVehiclePartFXProvider{	
	//Stored variables used to determine bullet firing behavior.
	public int bulletsFired;
	public int bulletsLeft;
	public int gunNumber;
	public Point3d currentOrientation;
	public Point3d prevOrientation;
	private ItemPart loadedBullet;
	
	//These variables are used during firing and will be reset on entity loading.
	public boolean firing;
	public int cooldownTimeRemaining;
	public int reloadTimeRemaining;
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
	public void attack(Damage damage){
		//If we are hit, jam the gun.
		if(damage.isExplosion){
			reloadTimeRemaining = definition.gun.reloadTime;
		}
	}
	
	@Override
	public void update(){
		super.update();
		prevOrientation.setTo(currentOrientation);
		
		//Get the current controller for this gun.
		IWrapperEntity controller = getCurrentController();
		
		//Adjust aim to face direction controller is facing.
		//Aim speed depends on gun size, with smaller and shorter guns moving quicker.
		//Pitch and yaw only depend on where the player is looking, and where the gun is pointed.
		//This allows for guns to be mounted anywhere on a vehicle and at any angle.
		//If the controller is null, and we are firing, set us to not do so.
		//We ain't coding sentrys here.... yet.
		if(controller != null){
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
		
		//If we are reloading, decrement the reloading timer.
		//This timer is also used for jams, which are essentially reloads without getting more bullets.
		//In either case, we can't fire until the reload is done.
		if(reloadTimeRemaining > 0){
			--reloadTimeRemaining;
			return;
		}
		
		//If this gun is being told to fire, and we have bullets, and are not reloading, fire.
		//Don't spawn bullets on the server, as they will cause lots of lag and network traffic.
		//Instead, spawn them on the clients, and then send back hit data to the server.
		//This is backwards from what usually happens, and can possibly be hacked, but it's FAR
		//easier on MC to leave clients to handle lots of bullets than the server and network systems.
		//We still need to run the gun code on the server, however, as we need to mess with inventory.
		if(firing && bulletsLeft > 0 && reloadTimeRemaining == 0 && cooldownTimeRemaining == 0){
			//First update gun number so we know if we need to apply a cam offset.
			//Get the gun number based on how many guns the vehicle has.
			gunNumber = 1;
			for(APart part : vehicle.parts){
				if(part instanceof PartGun){
					if(part.equals(this)){
						break;
					}else{
						++gunNumber;
					}
				}
			}
			
			//We would fire a bullet here, but that's for the SFXSystem to handle, not the update loop.
			//Make sure to add-on an offset to our firing point to allow for multi-gun units.
			long millisecondCamOffset = (long) (definition.gun.fireDelay*(1000D/20D)*(gunNumber - 1D)/vehicle.totalGuns);
			cooldownTimeRemaining = definition.gun.fireDelay;
			timeToFire = System.currentTimeMillis() + millisecondCamOffset;
			lastController = controller;
			if(!vehicle.world.isClient()){
				//Only remove bullets from the server.  We remove them from the client when they spawn.
				--bulletsLeft;
				++bulletsFired;
			}
		}
		
		//If we are out of bullets, and aren't cooling down from firing, re-load ourselves from any vehicle inventories.
		//This only works if the gun is set to auto-reload.
		//While the reload method checks for reload time, we check here to save on code processing.
		//No sense in looking for bullets if we can't load them anyways.
		if(bulletsLeft == 0 && definition.gun.autoReload && reloadTimeRemaining == 0){
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
			//IF we set it null during that time, the bullet would be fired and not know what it was.
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
			if(reloadTimeRemaining == 0){
				//Only fill bullets if we match the bullet already in the gun, or if our diameter matches.
				if((loadedBullet == null && part.definition.bullet.diameter == definition.gun.diameter) || loadedBullet.equals(part)){
					//Make sure we don't over-fill the gun.
					if(part.definition.bullet.quantity + bulletsLeft <= definition.gun.capacity){
						loadedBullet = part;
						bulletsLeft += part.definition.bullet.quantity;
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
		for(APart vehiclePart : vehicle.parts){
			if(vehiclePart instanceof PartSeat){
				if(vehiclePart.vehicleDefinition.isController){
					return vehicle.locationRiderMap.get(vehiclePart.placementOffset);
				}
			}
		}
		
		//No controller found.
		return null;
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
		
	@Override
	public void spawnParticles(){
		if(timeToFire != lastTimeFired && System.currentTimeMillis() >= timeToFire && bulletsLeft > 0){
			//Fire a bullet by spawning it with the appropriate muzzle velocity and angle.
			//Angle is based on the current gun orientation, plus a slight fudge-factor.
			//This is based on the barrel length and shell size.
			Point3d vehicleFactoredAngles = vehicle.angles.copy().add((Math.random() - 0.5F)*(10*definition.gun.diameter/(definition.gun.length*1000)), (Math.random() - 0.5F)*(10*definition.gun.diameter/(definition.gun.length*1000)), 0D);
			
			//Set initial velocity to the vehicle's velocity, plus the gun muzzle velocity at the specified orientation.
			Point3d bulletVelocity = vehicle.motion.copy().multiply(vehicle.SPEED_FACTOR).add(new Point3d(0D, 0D, definition.gun.muzzleVelocity/20D/10D).rotateFine(currentOrientation).rotateFine(totalRotation).rotateFine(vehicleFactoredAngles));
			
			//Get the bullet's initial position.  This is based off the gun orientation and barrel length.
			Point3d bulletPosition = new Point3d(0D, 0D, definition.gun.length).rotateFine(currentOrientation).rotateFine(totalRotation).rotateFine(vehicleFactoredAngles).add(worldPos);
	        
			//Add the bullet as a particle.
			MasterLoader.renderInterface.spawnParticle(new ParticleBullet(bulletPosition, bulletVelocity, loadedBullet, this, lastController));
			MasterLoader.audioInterface.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_firing"));
			lastTimeFired = timeToFire;
			
			//Remove a bullet from the count and add shots fired..
			--bulletsLeft;
			++bulletsFired;
		}
	}
}
