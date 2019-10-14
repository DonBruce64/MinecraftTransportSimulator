package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.items.parts.ItemPartBullet;
import minecrafttransportsimulator.packets.parts.PacketPartClientGunReload;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.SFXSystem.FXPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class APartGun extends APart implements FXPart{	
	//Stored variables used to determine bullet firing behavior.
	public int shotsFired;
	public int bulletsLeft;
	public float currentPitch;
	public float currentYaw;
	public String loadedBullet;
	
	//These variables are used during firing and will be reset on entity loading.
	public boolean firing;
	public boolean reloading;
	public int playerControllerID = -1;
	public int temp;
	public int cooldownTimeRemaining;
	public int reloadTimeRemaining;
	private long lastTickFired;
	private long lastTickToFire;
	
	private final double anglePerTickSpeed;
		
	public APartGun(EntityVehicleE_Powered vehicle, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
		this.shotsFired = dataTag.getInteger("shotsFired");
		this.bulletsLeft = dataTag.getInteger("bulletsLeft");
		this.currentPitch = dataTag.getFloat("currentPitch");
		this.currentYaw = dataTag.getFloat("currentYaw");
		this.loadedBullet = dataTag.getString("loadedBullet");
		this.anglePerTickSpeed = (50/pack.gun.diameter + 1/pack.gun.length);
	}
	
	@Override
	public boolean interactPart(EntityPlayer player){
		//Only reload bullets if we aren't currently reloading.
		if(!reloading){
			//Check to see if we have any bullets in our hands.
			//If so, and they go to this gun, reload the gun and send a packet to update other guns.
			ItemStack heldStack = player.getHeldItemMainhand();
			if(heldStack != null && heldStack.getItem() instanceof ItemPartBullet){
				ItemPartBullet bulletItem = (ItemPartBullet) heldStack.getItem();
				//Only fill bullets if we match the bullet already in the gun, or if our diameter matches.
				if((loadedBullet.isEmpty() && bulletItem.bulletPackData.diameter == this.pack.gun.diameter) || loadedBullet.equals(bulletItem.partName)){
					//Make sure we don't over-fill the gun.
					if(bulletItem.bulletPackData.quantity <= this.pack.gun.capacity + this.bulletsLeft){
						if(!player.capabilities.isCreativeMode){
							player.inventory.clearMatchingItems(bulletItem, -1, 1, null);
						}
						this.loadedBullet = bulletItem.partName;
						this.bulletsLeft += bulletItem.bulletPackData.quantity;
						reloadTimeRemaining = pack.gun.reloadTime;
						reloading = true;
						MTS.MTSNet.sendToAll(new PacketPartClientGunReload(this, bulletItem.partName));
					}
				}
			}
		}
		return true;
    }
	
	@Override
	public void attackPart(DamageSource source, float damage){
		//Add shots fired when damaged.  If the damage is explosive, add more damage and jam the gun.
		if(!source.isExplosion()){
			shotsFired += (int) (damage*2F);
			//If the source is flammable, add temp to the gun.
			if(source.isFireDamage()){
				temp += damage;
			}
		}else{
			shotsFired += (int) (damage*10F);
			reloadTimeRemaining = pack.gun.reloadTime;
			reloading = true;
		}
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		//Before we do any logic, check to make sure the player is still seated in the gunner seat.
		//It it quite possible they could dismount with their hands on the trigger, so we need to be sure we check.
		//Otherwise, guns could be set to fire and the player could just run away...
		if(playerControllerID != -1){
			Entity playerController = vehicle.world.getEntityByID(playerControllerID);
			PartSeat seat = vehicle.getSeatForRider(playerController);
			if(seat != null && (this.parentPart == null ? seat.isController : this.parentPart.equals(seat))){
				//If we are out of bullets, and we can automatically reload, and are not doing so, start the reload sequence.
				if(bulletsLeft == 0 && pack.gun.autoReload && !reloading){
					//Iterate through all the inventory slots in crates to try to find matching ammo.
					for(APart part : vehicle.getVehicleParts()){
						if(part instanceof PartCrate){
							InventoryBasic crateInventory = ((PartCrate) part).crateInventory;
							for(byte i=0; i<crateInventory.getSizeInventory(); ++i){
								ItemStack stack = crateInventory.getStackInSlot(i);
								if(stack != null && stack.getItem() instanceof ItemPartBullet){
									ItemPartBullet bullet = (ItemPartBullet) stack.getItem();
									//Only reload the same bulletType to ensure we don't mis-match ammo.
									if(loadedBullet == null || loadedBullet.equals(bullet.partName)){
										PackPartObject bulletPack = PackParserSystem.getPartPack(bullet.partName);
										//Also check to see if we have enough space for this bullet.
										if(bulletPack.bullet.quantity + bulletsLeft <= pack.gun.capacity){
											//Bullet is right type, and we can fit it.  Remove from crate and add to the gun.
											//Return here to ensure we don't set the loadedBullet to blank since we found bullets.
											reloadTimeRemaining = pack.gun.reloadTime;
											reloading = true;
											crateInventory.decrStackSize(i, 1);
											this.loadedBullet = bullet.partName;
											this.bulletsLeft = bulletPack.bullet.quantity;
											MTS.MTSNet.sendToAll(new PacketPartClientGunReload(this, bullet.partName));
											return;
										}
									}
								}
							}
						}
					}
					loadedBullet = "";
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
				if(firing && bulletsLeft > 0 && !reloading){
					//If we aren't in a cooldown time, we can fire.
					if(cooldownTimeRemaining == 0){
						//We would fire a bullet here, but that's for the SFXSystem to handle, not the update loop.
						cooldownTimeRemaining = pack.gun.fireDelay;
						lastTickToFire = vehicle.world.getTotalWorldTime();
						--bulletsLeft;
					}
					if(cooldownTimeRemaining > 0){
						--cooldownTimeRemaining;
					}
				}
				
				//Adjust aim to face direction controller is facing.
				//Aim speed depends on gun size, with smaller and shorter guns moving quicker.
				//Pitch and yaw are relative to the vehicle, so use those.
				//When we do yaw, make sure we do calculations with positive values.
				//Both the vehicle and the player can have yaw greater than 360.
				double deltaPitch = playerController.rotationPitch - vehicle.rotationPitch;
				double deltaYaw = (vehicle.rotationYaw%360 - partRotation.y + 360)%360 - (playerController.rotationYaw%360 + 360)%360;
				if(deltaPitch < currentPitch && currentPitch > getMinPitch()){
					currentPitch -= Math.min(anglePerTickSpeed, currentPitch - deltaPitch);
				}else if(deltaPitch > currentPitch && currentPitch < getMaxPitch()){
					currentPitch += Math.min(anglePerTickSpeed, deltaPitch - currentPitch);
				}
				if(deltaYaw < currentYaw && currentYaw > getMinYaw()){
					currentYaw -= Math.min(anglePerTickSpeed, currentYaw - deltaYaw);
				}else if(deltaYaw > currentYaw && currentYaw < getMaxYaw()){
					currentYaw += Math.min(anglePerTickSpeed, deltaYaw - currentYaw);
				}
			}else{
				playerControllerID = -1;
			}
		}	
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = new NBTTagCompound();
		dataTag.setInteger("shotsFired", this.shotsFired);
		dataTag.setInteger("bulletsLeft", this.bulletsLeft);
		dataTag.setFloat("currentPitch", this.currentPitch);
		dataTag.setFloat("currentYaw", this.currentYaw);
		dataTag.setString("loadedBullet", this.loadedBullet);
		return dataTag;
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
	public Vec3d getActionRotation(float partialTicks){
		return new Vec3d(currentPitch, currentYaw, 0);
	}
	
	public abstract float getMinYaw();
	
	public abstract float getMaxYaw();
	
	public abstract float getMinPitch();
	
	public abstract float getMaxPitch();
		
	@Override
	@SideOnly(Side.CLIENT)
	public void spawnParticles(){
		if(lastTickToFire != lastTickFired){
			//Fire a bullet by spawning it with the appropriate muzzle velocity and angle.
			//Angle is based on rotation of the vehicle, gun, and gun mount.
			//Set the trajectory of the bullet.
			//Add a slight fudge-factor to the bullet's trajectory depending on the barrel length and shell size.
			float bulletYaw = (float) (vehicle.rotationYaw - partRotation.y - currentYaw + (Math.random() - 0.5F)*(10*pack.gun.diameter/(pack.gun.length*1000)));
			float bulletPitch = (float) (vehicle.rotationPitch + partRotation.x + currentPitch + (Math.random() - 0.5F)*(10*pack.gun.diameter/(pack.gun.length*1000)));
			
			//Set initial velocity to the gun muzzle velocity times the speedFactor.
			//We bring in the code for vectors here to make the velocity calculations easier.
			//Copied from Entity.getVectorForRotation()
			float f = MathHelper.cos(-bulletYaw * 0.017453292F - (float)Math.PI);
	        float f1 = MathHelper.sin(-bulletYaw * 0.017453292F - (float)Math.PI);
	        float f2 = -MathHelper.cos(-bulletPitch * 0.017453292F);
	        float f3 = MathHelper.sin(-bulletPitch * 0.017453292F);
	        Vec3d bulletOrientation = new Vec3d((double)(f1 * f2), (double)f3, (double)(f * f2));
			
			double bulletMotionX = bulletOrientation.x*pack.gun.muzzleVelocity/20D/10D;
			double bulletMotionY = bulletOrientation.y*pack.gun.muzzleVelocity/20D/10D;
			double bulletMotionZ = bulletOrientation.z*pack.gun.muzzleVelocity/20D/10D;
			
			//Now add the bullet as a particle.
			Minecraft.getMinecraft().effectRenderer.addEffect(new PartBullet(vehicle.world, partPos.x, partPos.y, partPos.z, bulletMotionX, bulletMotionY, bulletMotionZ, loadedBullet, playerControllerID, this.vehicle));
			MTS.proxy.playSound(partPos, partName + "_firing", 1, 1);
			lastTickFired = lastTickToFire;
		}
	}
}
