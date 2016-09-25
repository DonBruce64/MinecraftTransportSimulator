package minecraftflightsimulator.entities.parts;

import java.util.List;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityBase;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityFlyable;
import minecraftflightsimulator.packets.control.EnginePacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.world.World;

public class EntityPropeller extends EntityChild{
	private EntityFlyable flyer;
	public int health;
	public int numberBlades;
	public int pitch;
	public int diameter;
	public float angularPosition;
	public float angularVelocity;
	public double engineRPM;
	
	public EntityPropeller(World world){
		super(world);
	}
	
	public EntityPropeller(World world, EntityFlyable flyer, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, flyer, parentUUID, offsetX, offsetY, offsetZ, 0.8F, 1.0F, propertyCode);
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
		flyer = (EntityFlyable) this.parent;
				
		if(worldObj.isRemote){
			angularVelocity = (float) (0.8F*Math.atan(engineRPM/250F));
			angularPosition = (angularVelocity+angularPosition)%6.283185312F;
		}else{
			if(engineRPM >= 100){
				List collidedEntites = worldObj.getEntitiesWithinAABBExcludingEntity(this, this.getBoundingBox());
				for(int i=0; i < collidedEntites.size(); ++i){
					Entity collidedEntity = (Entity) collidedEntites.get(i);
					if(!(collidedEntity instanceof EntityBase)){
						collidedEntity.attackEntityFrom(new EntityDamageSourcePropellor("propellor", this), (float) (MFS.propellerDamageFactor*engineRPM/500F));
					}
				}
				if(isPartCollided(getBoundingBox().expand(0.1, 0.1, 0.1))){
					if(--health<0){
						MFS.proxy.playSound(this, "random.break", 2, 1);
						this.setDead();
					}
				}
				if(engineRPM/60*Math.PI*diameter*0.0254 > 340.29){
					MFS.proxy.playSound(this, "random.break", 2, 1);
					this.setDead();
				}
			}
		}
	}
	
	public double getThrustForce(){
		if(flyer!=null){
			return flyer.airDensity*Math.PI*Math.pow(0.0254*diameter/2D, 2)*(Math.pow(engineRPM*0.0254*pitch/60D, 2)-(engineRPM*0.0254*pitch/60D)*flyer.velocity*20)*Math.pow(diameter/2D/pitch + numberBlades/1000D, 1.5)/400D;
		}else{
			return 0;
		}
	}
	
	@Override
	public boolean performAttackAction(DamageSource source, float damage){
		if(!worldObj.isRemote){
			if(flyer != null){
				if(source.getEntity() instanceof EntityPlayer){
					int engineID = flyer.getEngineIdOfHitPropeller(this.UUID);
					if(engineID != 0){
						if(flyer.setEngineState((byte) 0, engineID)){
							MFS.MFSNet.sendToAll(new EnginePacket(this.parent.getEntityId(), (byte) 0, engineID));
						}
					}
				}
			}
		}
		return true;
	}
	
	public class EntityDamageSourcePropellor extends EntityDamageSource{
		public EntityDamageSourcePropellor(String name, Entity transmitter){
			super(name, transmitter);
			this.damageType="propellor";
		}
	};
	
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
