package minecrafttransportsimulator.vehicles.parts;

import mcinterface.InterfaceAudio;
import mcinterface.InterfaceNetwork;
import mcinterface.InterfaceRender;
import mcinterface.WrapperInventory;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.parts.ItemPartBullet;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartGun;
import minecrafttransportsimulator.rendering.components.IVehiclePartFXProvider;
import minecrafttransportsimulator.rendering.instances.ParticleBullet;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.item.ItemStack;

public class PartGun extends APart implements IVehiclePartFXProvider{	
	//Stored variables used to determine bullet firing behavior.
	public int shotsFired;
	public int bulletsLeft;
	public double currentPitch;
	public double currentYaw;
	public double prevPitch;
	public double prevYaw;
	public ItemPartBullet loadedBullet;
	
	//These variables are used during firing and will be reset on entity loading.
	public boolean firing;
	public boolean reloading;
	public int temp;
	public int cooldownTimeRemaining;
	public int reloadTimeRemaining;
	private byte gunNumber;
	private long lastTimeFired;
	private long timeToFire;
	
	private final double anglePerTickSpeed;
		
	public PartGun(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, WrapperNBT data){
		super(vehicle, packVehicleDef, definition, data);
		this.shotsFired = data.getInteger("shotsFired");
		this.bulletsLeft = data.getInteger("bulletsLeft");
		this.currentPitch = data.getDouble("currentPitch");
		this.currentYaw = data.getDouble("currentYaw");
		String loadedBulletsPack = data.getString("loadedBulletsPack");
		String loadedBulletName = data.getString("loadedBulletName");
		if(!loadedBulletsPack.isEmpty()){
			this.loadedBullet = (ItemPartBullet) MTSRegistry.packItemMap.get(loadedBulletsPack).get(loadedBulletName);
		}
		this.anglePerTickSpeed = (50/definition.gun.diameter + 1/definition.gun.length);
		
		//Get the gun number based on how many guns the vehicle has.
		gunNumber = 1;
		for(APart part : vehicle.parts){
			if(part instanceof PartGun){
				++gunNumber;
			}
		}
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		//Only reload bullets if we aren't currently reloading.
		if(!reloading){
			//Check to see if we have any bullets in our hands.
			//If so, and they go to this gun, reload the gun and send a packet to update other guns.
			ItemStack heldStack = player.getHeldStack();
			if(heldStack != null && heldStack.getItem() instanceof ItemPartBullet){
				ItemPartBullet bulletItem = (ItemPartBullet) heldStack.getItem();
				//Only fill bullets if we match the bullet already in the gun, or if our diameter matches.
				if((loadedBullet == null && bulletItem.definition.bullet.diameter == definition.gun.diameter) || loadedBullet.equals(bulletItem)){
					//Make sure we don't over-fill the gun.
					if(bulletItem.definition.bullet.quantity <= definition.gun.capacity + bulletsLeft){
						player.removeItem(heldStack, 1);
						loadedBullet = bulletItem;
						bulletsLeft += bulletItem.definition.bullet.quantity;
						reloadTimeRemaining = definition.gun.reloadTime;
						reloading = true;
						InterfaceNetwork.sendToClientsTracking(new PacketVehiclePartGun(this, bulletItem.definition.packID, bulletItem.definition.systemName), this.vehicle);
					}
				}
			}
		}
		return true;
    }
	
	@Override
	public void attack(Damage damage){
		//Add shots fired when damaged.  If the damage is explosive, add more damage and jam the gun.
		if(!damage.isExplosion){
			shotsFired += (int) (damage.amount*2F);
			//If the source is flammable, add temp to the gun.
			if(damage.isFire){
				temp += damage.amount;
			}
		}else{
			shotsFired += (int) (damage.amount*10F);
			reloadTimeRemaining = definition.gun.reloadTime;
			reloading = true;
		}
	}
	
	@Override
	public void update(){
		super.update();
		prevPitch = currentPitch;
		prevYaw = currentYaw;
		
		//Decrement cooldown time, if we have any.
		if(cooldownTimeRemaining > 0){
			--cooldownTimeRemaining;
		} 
		
		if(firing){
			//Before we do any logic, check to make sure the player is still seated in the gunner seat.
			//It it quite possible they could dismount with their hands on the trigger, so we need to be sure we check.
			//Otherwise, guns could be set to fire and the player could just run away...
			WrapperPlayer playerController = (WrapperPlayer) vehicle.locationsToRiders.get(placementOffset);
			if(playerController != null){
				//If we are out of bullets, and we can automatically reload, and are not doing so, start the reload sequence.
				if(bulletsLeft == 0 && definition.gun.autoReload && !reloading){
					//Iterate through all the inventory slots in crates to try to find matching ammo.
					for(APart part : vehicle.parts){
						if(part instanceof PartInteractable){
							WrapperInventory inventory = ((PartInteractable) part).inventory;
							if(inventory != null && part.definition.interactable.feedsVehicles){
								for(byte i=0; i<inventory.getSize(); ++i){
									ItemStack stack = inventory.getStackInSlot(i);
									if(stack != null && stack.getItem() instanceof ItemPartBullet){
										ItemPartBullet bullet = (ItemPartBullet) stack.getItem();
										//Only reload the same bulletType to ensure we don't mis-match ammo.
										if(loadedBullet == null || loadedBullet.equals(bullet)){
											//Also check to see if we have enough space for this bullet.
											if(bullet.definition.bullet.quantity + bulletsLeft <= definition.gun.capacity){
												//Bullet is right type, and we can fit it.  Remove from crate and add to the gun.
												//Return here to ensure we don't set the loadedBullet to blank since we found bullets.
												reloadTimeRemaining = definition.gun.reloadTime;
												reloading = true;
												inventory.decrement(i);
												loadedBullet = bullet;
												bulletsLeft = bullet.definition.bullet.quantity;
												InterfaceNetwork.sendToClientsTracking(new PacketVehiclePartGun(this, definition.packID, definition.systemName), this.vehicle);
												return;
											}
										}
									}
								}
							}
						}
					}
					loadedBullet = null;
				}
				
				//If we are reloading, decrement the countdown timer.
				//If we hit 0, complete the reload.  This will allow the gun to fire again.
				//This timer is also used for jams, which are essentially reloads without getting more bullets.
				if(reloading){
					--reloadTimeRemaining;
					if(reloadTimeRemaining <= 0){
						reloading = false;
					}
				}
				
				//If this gun is being told to fire, and we have bullets and are not reloading, fire.
				//Don't spawn bullets on the server, as they will cause lots of lag and network traffic.
				//Instead, spawn them on the clients, and then send back hit data to the server.
				//This is backwards from what usually happens, and can possibly be hacked, but it's FAR
				//easier on MC to leave clients to handle lots of bullets than the server and network systems.
				//We still need to run the gun code on the server, however, as we need to mess with inventory.
				if(firing && bulletsLeft > 0 && !reloading && cooldownTimeRemaining == 0){
					//We would fire a bullet here, but that's for the SFXSystem to handle, not the update loop.
					//Make sure to add-on an offset to our firing point to allow for multi-gun units.
					//We also add 1 tick here, as it's only subsequent ticks we check for gun firing.
					long millisecondCamOffset = (long) (definition.gun.fireDelay*(1000D/20D)*(gunNumber - 1D)/vehicle.totalGuns);
					cooldownTimeRemaining = definition.gun.fireDelay;
					timeToFire = System.currentTimeMillis() + millisecondCamOffset;
					--bulletsLeft;
				}
				
				//Adjust aim to face direction controller is facing.
				//Aim speed depends on gun size, with smaller and shorter guns moving quicker.
				//Pitch and yaw are relative to the vehicle, so use those.
				//When we do yaw, make sure to normalize the player's yaw from -180/180.
				double deltaPitch = playerController.getPitch() - vehicle.angles.x;
				double playerYaw = ((playerController.getYaw() - vehicle.angles.y)%360 + 360)%360;
				if(playerYaw >= 180){
					playerYaw = -(360 - playerYaw);
				}
				double deltaYaw = playerYaw -(currentYaw + placementRotation.y);
				
				//I know this is weird, but the pitch is bigger when it's pointing the ground and smaller when it's pointing the sky.
				//At least this won't be confusing on the pack creator's end in this way. -Bunting_chj
				if(deltaPitch < currentPitch && currentPitch > -definition.gun.maxPitch){
					currentPitch -= Math.min(anglePerTickSpeed, currentPitch - deltaPitch);
				}else if(deltaPitch > currentPitch && currentPitch < -definition.gun.minPitch){
					currentPitch += Math.min(anglePerTickSpeed, deltaPitch - currentPitch);
				}
				
				//Apply yaw deltas to the gun.
				//If yaw is from -180 to 180, we are a gun that can spin around on its mount.
				//We need to do special logic for this type of gun.
				if(definition.gun.minYaw == -180  && definition.gun.maxYaw == 180){
					//Adjust our deltaYaw if its quicker to rotate the other direction.
					if(deltaYaw > 180){
						deltaYaw -= 360;
					}else if(deltaYaw < -180){
						deltaYaw += 360;
					}
					
					//Apply rotations.  We don't check min-max here as we don't have a min or max.
					if(deltaYaw > 0){
						currentYaw += Math.min(anglePerTickSpeed, deltaYaw);
					}else{
						currentYaw += Math.max(-anglePerTickSpeed, deltaYaw);
					}
					
					//If we are over our yaw bounds, re-clamp.
					//Also adjust the prev values so we don't get spazzing for rendering.
					if(currentYaw > 180 ){
						currentYaw -= 360;
						prevYaw -= 360;
					}else if(currentYaw < -180){
						currentYaw += 360;
						prevYaw += 360;
					}
				}else{
					if(deltaYaw > 0){
						if(currentYaw < definition.gun.maxYaw){
							currentYaw += Math.min(anglePerTickSpeed, deltaYaw);
						}
					}else{
						if(currentYaw > definition.gun.minYaw){
							currentYaw += Math.max(-anglePerTickSpeed, deltaYaw);
						}
					}
				}
			}else{
				firing = false;
			}
		}
	}
	
	@Override
	public WrapperNBT getData(){
		WrapperNBT data = super.getData();
		data.setInteger("shotsFired", this.shotsFired);
		data.setInteger("bulletsLeft", this.bulletsLeft);
		data.setDouble("currentPitch", this.currentPitch);
		data.setDouble("currentYaw", this.currentYaw);
		if(loadedBullet != null){
			data.setString("loadedBulletPack", this.loadedBullet.definition.packID);
			data.setString("loadedBulletName", this.loadedBullet.definition.systemName);
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
		//Don't return pitch if we are a turret.
		if(definition.gun.isTurret){
			return new Point3d(0, prevYaw + (currentYaw - prevYaw)*partialTicks, 0);
		}else{
			return new Point3d(prevPitch + (currentPitch - prevPitch)*partialTicks, prevYaw + (currentYaw - prevYaw)*partialTicks, 0);
		}
	}
		
	@Override
	public void spawnParticles(){
		if(timeToFire != lastTimeFired && System.currentTimeMillis() >= timeToFire){
			//Fire a bullet by spawning it with the appropriate muzzle velocity and angle.
			//Angle is based on rotation of the vehicle, gun, and gun mount.
			//Set the trajectory of the bullet.
			//Add a slight fudge-factor to the bullet's trajectory depending on the barrel length and shell size.
			Point3d gunAngle = new Point3d(0D, 0D, 0D);
			gunAngle.y = (float) (vehicle.angles.y + placementRotation.y + currentYaw + (Math.random() - 0.5F)*(10*definition.gun.diameter/(definition.gun.length*1000)));
			gunAngle.x = (float) (vehicle.angles.x + placementRotation.x + currentPitch + (Math.random() - 0.5F)*(10*definition.gun.diameter/(definition.gun.length*1000)));
			Point3d bulletOrientation = new Point3d(0D, 0D, 1D).rotateFine(gunAngle);
			
			//Set initial velocity to the vehicle's velocity, plus the gun muzzle velocity at the specified orientation.
			Point3d bulletVelocity = vehicle.motion.copy().multiply(vehicle.SPEED_FACTOR).add(bulletOrientation.copy().multiply(definition.gun.muzzleVelocity/20D/10D));
			
			//Get the bullet's initial position.  This is based off the gun orientation and barrel length.
			bulletOrientation.multiply((double) definition.gun.length).add(worldPos);
	        
			//Add the bullet as a particle.
			InterfaceRender.spawnParticle(new ParticleBullet(vehicle.world, bulletOrientation, bulletVelocity, loadedBullet, (WrapperPlayer) vehicle.locationsToRiders.get(placementOffset), vehicle));
			InterfaceAudio.playQuickSound(new SoundInstance(this, definition.packID + ":" + definition.systemName + "_firing"));
			lastTimeFired = timeToFire;
		}
	}
}
