package minecrafttransportsimulator.entities.parts;

import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSDamageSources.DamageSourcePropellor;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.packets.control.EnginePacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityPropeller extends EntityMultipartChild{
	public byte type;
	public int numberBlades;
	public int pitch;
	public int diameter;
	
	public float angularPosition;
	public float angularVelocity;
	public float health;
	
	public String engineUUID;
	public EntityEngineAircraft engine;
	
	public EntityPropeller(World world){
		super(world);
	}
	
	public EntityPropeller(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, 0.8F, 1.0F, propertyCode);		
	}
	
	@Override
	public void setNBTFromStack(ItemStack stack){
		NBTTagCompound stackNBT = stack.getTagCompound();
		type = stackNBT.getByte("type");
		numberBlades = stackNBT.getInteger("numberBlades");
		pitch = stackNBT.getInteger("pitch");
		diameter = stackNBT.getInteger("diameter");
		health = stackNBT.getFloat("health");
	}
	
	@Override
	public ItemStack getItemStack(){
		ItemStack propellerStack = new ItemStack(MTSRegistry.propeller, 1, type);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setByte("type", type);
		tag.setInteger("numberBlades", numberBlades);
		tag.setInteger("pitch", pitch);
		tag.setInteger("diameter", diameter);
		tag.setFloat("health", health);
		propellerStack.setTagCompound(tag);
		return propellerStack;
	}
	
	@Override
	protected boolean attackChild(DamageSource source, float damage){
		if(parent != null){
			if(source.getEntity() instanceof EntityPlayer){					
				if(((EntityPlayer) source.getEntity()).getHeldItemMainhand() == null){
					if(!(source.getEntity().getRidingEntity() instanceof EntitySeat)){
						for(EntityMultipartChild child : parent.getChildren()){
							if(child instanceof EntityEngineAircraft){
								if(this.equals(((EntityEngineAircraft) child).propeller)){
									((EntityMultipartVehicle) parent).handleEngineSignal(engine, (byte) 4);
									MTS.MTSNet.sendToAll(new EnginePacket(parent.getEntityId(), engine.getEntityId(), (byte) 4));
								}
							}
						}
					}
					return true;
				}
			}
		}
		damagePropeller(damage);
		return true;
	}
	
	@Override
	public void setDead(){
		super.setDead();
		if(engine != null){
			((EntityEngineAircraft) engine).propeller = null;
		}
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		if(!linked){return;}
		if(engine == null){
			engine = (EntityEngineAircraft) getEntityByUUID(worldObj, engineUUID);
			if(engine != null){
				engine.propeller = this;
			}else{
				return;
			}
		}
		
		if(worldObj.isRemote){
			angularVelocity = (float) (360*engine.RPM/60F/20F/150F);
			angularPosition += angularVelocity;
		}else{
			if(engine.RPM >= 100){
				List<EntityLivingBase> collidedEntites = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().expand(0.2F, 0.2F, 0.2F));
				if(!collidedEntites.isEmpty()){
					Entity attacker = null;
					for(EntityMultipartChild child : parent.getChildren()){
						if(child instanceof EntitySeat){
							EntitySeat seat = (EntitySeat) child;
							if(seat.isController){
								if(seat.getPassenger() != null){
									attacker = seat.getPassenger();
									break;
								}
							}
						}
						
					}
					for(int i=0; i < collidedEntites.size(); ++i){
						if(!(collidedEntites.get(i).getRidingEntity() instanceof EntitySeat)){
							collidedEntites.get(i).attackEntityFrom(new DamageSourcePropellor(attacker), (float) (ConfigSystem.getDoubleConfig("PropellerDamageFactor")*engine.RPM/500F));
						}
					}
				}
				if(this.isChildOffsetBoxCollidingWithBlocks(this.getEntityBoundingBox().expand(0.1F, 0.1F, 0.1F))){
					damagePropeller(1);
					
				}
				if(engine.RPM/60*Math.PI*diameter*0.0254 > 340.29){
					damagePropeller(9999);
				}
			}
		}
	}
	
	private void damagePropeller(float damage){
		health -= damage;
		if(health <= 0){
			this.parent.removeChild(UUID, true);
			return;
		}
	}
	
	public double getThrustForce(){
		if(parent != null && engine != null){
			return ((EntityPlane) parent).airDensity*Math.PI*Math.pow(0.0254*diameter/2D, 2)*(Math.pow(engine.RPM*0.0254*pitch/60D, 2)-(engine.RPM*0.0254*pitch/60D)*((EntityPlane) parent).velocity*20)*Math.pow(diameter/2D/pitch + numberBlades/1000D, 1.5)/400D;
		}else{
			return 0;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.type=tagCompound.getByte("type");
		this.numberBlades=tagCompound.getInteger("numberBlades");
		this.pitch=tagCompound.getInteger("pitch");
		this.diameter=tagCompound.getInteger("diameter");
		this.health=tagCompound.getFloat("health");
		this.engineUUID=tagCompound.getString("engineUUID");
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setByte("type", this.type);
		tagCompound.setInteger("numberBlades", this.numberBlades);
		tagCompound.setInteger("pitch", this.pitch);
		tagCompound.setInteger("diameter", this.diameter);
		tagCompound.setFloat("health", this.health);
		tagCompound.setString("engineUUID", this.engineUUID);
		return tagCompound;
	}
}
