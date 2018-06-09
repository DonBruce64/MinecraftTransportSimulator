package minecrafttransportsimulator.multipart.parts;

import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.DamageSources.DamageSourcePropellor;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal.PacketEngineTypes;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;

public class PartPropeller extends APart{	
	public float angularPosition;
	public float angularVelocity;
	public float health;
	
	private byte linkingTicks = 0;
	private final PartEngineAircraft connectedEngine;
	private final Vec3d collisionBoundingOffset = new Vec3d(0.1, 0.1, 0.1);
	
	public PartPropeller(EntityMultipartD_Moving multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, String partName, NBTTagCompound dataTag){
		super(multipart, offset, isController, turnsWithSteer, partName, dataTag);
		this.health = dataTag.getFloat("health");
		if(health == 0){
			this.health = pack.propeller.startingHealth;
		}
		//Due to linking timings, it will be impossible for players to add propellers without engines and
		//have them be saved to multiparts.  Because of this, we can check for engines here as they MUST
		//be present or this propeller is invalid and should be dropped as an item.
		for(APart part : multipart.getMultipartParts()){
			if(part instanceof PartEngineAircraft){
				if(part.offset.xCoord == this.offset.xCoord || part.offset.yCoord == this.offset.yCoord || part.offset.zCoord == this.offset.zCoord){
					//If we align with any axis for any engine we must be linked to that engine.
					//Do a quick distance check to make sure, however, and then link.
					if(part.offset.distanceTo(this.offset) < 2){
						this.connectedEngine = ((PartEngineAircraft) part);
						connectedEngine.propeller = this;
						return;
					}
				}
			}
		}
		connectedEngine = null;
		if(!multipart.worldObj.isRemote){
			this.dropAsItem();
		}
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){
		if(source.getEntity() instanceof EntityPlayer){
			EntityPlayer player = (EntityPlayer) source.getEntity();
			if(player.getHeldItemMainhand() == null){
				if(!multipart.equals(player.getRidingEntity())){
					connectedEngine.handStartEngine();
					MTS.MTSNet.sendToAll(new PacketPartEngineSignal(connectedEngine, PacketEngineTypes.HS_ON));
				}
				return;
			}
		}
		damagePropeller(damage);
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		if(multipart.worldObj.isRemote){
			angularVelocity = (float) (360*connectedEngine.RPM/60F/20F);
			angularPosition += angularVelocity;
		}else{
			if(connectedEngine.RPM >= 100){
				List<EntityLivingBase> collidedEntites = multipart.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.getAABBWithOffset(Vec3d.ZERO).expand(0.2F, 0.2F, 0.2F));
				if(!collidedEntites.isEmpty()){
					Entity attacker = null;
					for(Entity passenger : multipart.getPassengers()){
						if(multipart.getSeatForRider(passenger).isController){
							attacker = passenger;
							break;
						}
					}
					for(int i=0; i < collidedEntites.size(); ++i){
						if(!multipart.equals(collidedEntites.get(i).getRidingEntity())){
							collidedEntites.get(i).attackEntityFrom(new DamageSourcePropellor(attacker), (float) (ConfigSystem.getDoubleConfig("PropellerDamageFactor")*connectedEngine.RPM/500F));
						}
					}
				}
				if(this.isPartCollidingWithBlocks(collisionBoundingOffset)){
					damagePropeller(1);
					
				}
				if(connectedEngine.RPM/60*Math.PI*pack.propeller.diameter*0.0254 > 340.29){
					damagePropeller(9999);
				}
			}
		}
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = new NBTTagCompound();		
		dataTag.setFloat("health", health);
		return dataTag;
	}
	
	@Override
	public float getWidth(){
		return pack.propeller.diameter*0.0254F/2F;
	}

	@Override
	public float getHeight(){
		return pack.propeller.diameter*0.0254F/2F;
	}

	@Override
	public Vec3d getRotation(float partialTicks){
		return new Vec3d(0, 0, this.angularPosition + this.angularVelocity*partialTicks);
	}
	
	private void damagePropeller(float damage){
		health -= damage;
		if(health <= 0 && !multipart.worldObj.isRemote){
			multipart.removePart(this, true);
		}
	}
	
	public double getThrustForce(){
		if(connectedEngine != null){
			//TODO
			//Need to align this to radius at 75% blade with pitch to get new
			//force equation that uses pitch to take into account flow of air.
			//Current model only works for small-diameter propellers.
			return multipart.airDensity*Math.PI*Math.pow(0.0254*pack.propeller.diameter/2D, 2)*(Math.pow(connectedEngine.RPM*0.0254*pack.propeller.pitch/60D, 2)-(connectedEngine.RPM*0.0254*pack.propeller.pitch/60D)*multipart.velocity*20)*Math.pow(pack.propeller.diameter/2D/pack.propeller.pitch + pack.propeller.numberBlades/1000D, 1.5)/400D;
		}else{
			return 0;
		}
	}
	
	public void dropAsItem(){
		multipart.removePart(this, false);
		ItemStack propellerStack = new ItemStack(this.getItemForPart());
		propellerStack.setTagCompound(this.getPartNBTTag());
		multipart.worldObj.spawnEntityInWorld(new EntityItem(multipart.worldObj, partPos.xCoord, partPos.yCoord, partPos.zCoord, propellerStack));
	}
}
