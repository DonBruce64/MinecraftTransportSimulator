package minecraftflightsimulator.entities;

import java.util.List;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.other.EntityDamageSourcePropellor;
import minecraftflightsimulator.packets.control.EnginePacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntityPropeller extends EntityChild{
	public int health;
	public int numberBlades;
	public int pitch;
	public int diameter;
	public float angularPosition;
	public float angularVelocity;
	public double engineRPM;
	
	public EntityPropeller(World world){
		super(world);
		this.setSize(0.8F, 1.0F);
	}
	
	public EntityPropeller(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
		if(propertyCode%10==1){
			this.health = 500;
		}else if(propertyCode%10==2){
			this.health = 1000;
		}else{
			this.health = 100;
		}
		this.numberBlades=propertyCode%100/10;
		this.pitch = 55+3*(propertyCode%1000/100);
		this.diameter = 70+5*(propertyCode/1000);
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		if(worldObj.isRemote){
			angularVelocity = (float) (0.8F*Math.atan(engineRPM/250F));
			angularPosition = (angularVelocity+angularPosition)%6.283185312F;
		}else{
			if(engineRPM >= 100){
				List collidedEntites = worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox);
				for(int i=0; i < collidedEntites.size(); ++i){
					Entity collidedEntity = (Entity) collidedEntites.get(i);
					if(!(collidedEntity instanceof EntityBase)){
						collidedEntity.attackEntityFrom(new EntityDamageSourcePropellor("propellor", this), (float) (engineRPM/500F));
					}
				}
				if(!worldObj.getCollidingBoundingBoxes(this, this.boundingBox.expand(0.2, 0.2, 0.2)).isEmpty()){
					if(--health<0){
						worldObj.playSoundAtEntity(this, "minecraft:random.break", 2, 1);
						this.setDead();
					}
				}
				if(engineRPM/60*Math.PI*diameter*0.0254 > 340.29){
					worldObj.playSoundAtEntity(this, "minecraft:random.break", 2, 1);
					this.setDead();
				}
			}
		}
	}
	
	public double getThrustForce(){
		if(parent!=null){
			return parent.airDensity*Math.PI*Math.pow(0.0254*diameter, 2)/4D*(Math.pow(engineRPM*0.0254*pitch/60D, 2)-(engineRPM*0.0254*pitch/60D)*parent.velocity*20)*Math.pow(diameter/2.25/pitch, 1.5)/400D;
		}else{
			return 0;
		}
	}
	
	@Override
	public boolean hitByEntity(Entity entity){
		if(!worldObj.isRemote){
			if(parent != null){
				if(entity instanceof EntityPlayer){
					MFS.MFSNet.sendToServer(new EnginePacket(parent.getEntityId(), parent.getEngineOfHitPropeller(this.UUID)));
				}
			}
		}
		return true;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.numberBlades=tagCompound.getInteger("numberBlades");
		this.health=tagCompound.getInteger("health");
		this.pitch=tagCompound.getInteger("pitch");
		this.diameter=tagCompound.getInteger("diameter");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setInteger("numberBlades", this.numberBlades);
		tagCompound.setInteger("health", this.health);
		tagCompound.setInteger("pitch", this.pitch);
		tagCompound.setInteger("diameter", this.diameter);
	}
}
