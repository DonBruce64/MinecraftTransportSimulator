package minecrafttransportsimulator.multipart.parts;

import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.DamageSources.DamageSourcePropellor;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;
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
	public float damage;
	public short currentPitch;
	
	private byte linkingTicks = 0;
	private final PartEngineAircraft connectedEngine;
	
	public PartPropeller(EntityMultipartD_Moving multipart, Vec3d offset, boolean isController, boolean turnsWithSteer, String partName, NBTTagCompound dataTag){
		super(multipart, offset, isController, turnsWithSteer, partName, dataTag);
		this.damage = dataTag.getFloat("damage");
		this.currentPitch = pack.propeller.pitch;
		//Due to JSON setup, it will be impossible for players to add propellers without engines and
		//have them be saved to multiparts.  Because of this, we can check for engines here as they MUST be present.
		//This engine must also have the propeller as a sub-part, so we can just find which engine has us as a sub-part and link to that.
		for(APart part : multipart.getMultipartParts()){
			for(PackPart subPart : part.pack.subParts){
				if(part.offset.addVector(subPart.pos[0], subPart.pos[1], subPart.pos[2]).equals(this.offset)){
					this.connectedEngine = ((PartEngineAircraft) part);
					connectedEngine.propeller = this;
					return;
				}
			}
		}
		connectedEngine = null;
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
		//If we are a dynamic-pitch propeller, adjust ourselves to the speed of the engine.
		if(pack.propeller.isDynamicPitch){
			if(((EntityMultipartF_Plane) multipart).propellersReversed && currentPitch > -45){
				--currentPitch;
			}else if(!((EntityMultipartF_Plane) multipart).propellersReversed && currentPitch < 45){
				++currentPitch;
			}else if(connectedEngine.RPM < connectedEngine.pack.engine.maxRPM*0.87 && currentPitch > 45){
				--currentPitch;
			}else if(connectedEngine.RPM > connectedEngine.pack.engine.maxRPM*0.93 && currentPitch < pack.propeller.pitch){
				++currentPitch;
			}
		}
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
				if(this.isPartCollidingWithBlocks(Vec3d.ZERO)){
					damagePropeller(1);
					
				}
				if(connectedEngine.RPM/60*Math.PI*pack.propeller.diameter*0.0254 > 340.29){
					damagePropeller(9999);
				}
			}
		}
	}
	
	@Override
	public void removePart(){
		super.removePart();
		if(connectedEngine != null){
			connectedEngine.propeller = null;
		}
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = new NBTTagCompound();		
		dataTag.setFloat("damage", this.damage);
		return dataTag;
	}
	
	@Override
	public float getWidth(){
		return pack.propeller.diameter*0.0254F/2F;
	}

	@Override
	public float getHeight(){
		return pack.propeller.diameter*0.0254F;
	}

	@Override
	public Vec3d getRotation(float partialTicks){
		return new Vec3d(0, 0, this.angularPosition + this.angularVelocity*partialTicks);
	}
	
	private void damagePropeller(float damage){
		this.damage += damage;
		if(this.damage > pack.propeller.startingHealth && !multipart.worldObj.isRemote){
			multipart.removePart(this, true);
		}
	}
	
	public double getThrustForce(){
		if(currentPitch != 0){
			//Get what the pitch velocity of the propeller would be at the current velocity.
			double currentPitchVelocity = multipart.velocity*20D;
			//Get the effective pitch velocity of the propeller at the current RPM.
			double effectivePitchVelocity = 0.0254D*currentPitch*connectedEngine.RPM/60D;
			//Multiply by a factor to get the true effective pitch velocity.  This is slightly higher than ideal.
			effectivePitchVelocity *= (1D*currentPitch/pack.propeller.diameter + 0.2D)/(1D*currentPitch/pack.propeller.diameter);
			if(connectedEngine != null && effectivePitchVelocity != 0){
				//Get the angle of attack of the propeller.
				double angleOfAttack = Math.abs(effectivePitchVelocity - currentPitchVelocity);
				//Now return the thrust equation.  If the angle of attack is greater than 25, sap power off the propeller for stalling.
				return multipart.airDensity*Math.PI*Math.pow(0.0254*pack.propeller.diameter/2D, 2)*
						(effectivePitchVelocity*effectivePitchVelocity - effectivePitchVelocity*currentPitchVelocity)*
						Math.pow(pack.propeller.diameter/2D/Math.abs(effectivePitchVelocity) + pack.propeller.numberBlades/1000D, 1.5)/400D
						*(angleOfAttack > 35 ? 35/angleOfAttack : 1.0D)*Math.signum(effectivePitchVelocity);
			}
		}
		return 0;
	}
	
	public void dropAsItem(){
		multipart.removePart(this, false);
		ItemStack propellerStack = new ItemStack(this.getItemForPart());
		propellerStack.setTagCompound(this.getPartNBTTag());
		multipart.worldObj.spawnEntityInWorld(new EntityItem(multipart.worldObj, partPos.xCoord, partPos.yCoord, partPos.zCoord, propellerStack));
	}
}
