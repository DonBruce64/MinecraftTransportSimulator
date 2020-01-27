package minecrafttransportsimulator.vehicles.parts;

import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.DamageSources.DamageSourcePropellor;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal.PacketEngineTypes;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleG_Blimp;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;

public class PartPropeller extends APart<EntityVehicleF_Air>{	
	public float angularPosition;
	public float angularVelocity;
	public float damage;
	public short currentPitch;
	
	private final PartEngineAircraft connectedEngine;
	
	public PartPropeller(EntityVehicleF_Air vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
		this.damage = dataTag.getFloat("damage");
		this.currentPitch = definition.propeller.pitch;
		this.connectedEngine = (PartEngineAircraft) parentPart;
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){
		if(source.getTrueSource() instanceof EntityPlayer){
			EntityPlayer player = (EntityPlayer) source.getTrueSource();
			if(player.getHeldItemMainhand().isEmpty()){
				if(!vehicle.equals(player.getRidingEntity())){
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
		//But don't do this for blimps, as they reverse their engines rather than adjust their propellers.
		if(definition.propeller.isDynamicPitch && !(vehicle instanceof EntityVehicleG_Blimp)){
			if(vehicle.reverseThrust && currentPitch > -45){
				--currentPitch;
			}else if(!vehicle.reverseThrust && currentPitch < 45){
				++currentPitch;
			}else if(connectedEngine.RPM < connectedEngine.definition.engine.maxRPM*0.80 && currentPitch > 45){
				--currentPitch;
			}else if(connectedEngine.RPM > connectedEngine.definition.engine.maxRPM*0.85 && currentPitch < definition.propeller.pitch){
				++currentPitch;
			}
		}
		if(vehicle.world.isRemote){
			angularVelocity = (float) (360*connectedEngine.RPM*connectedEngine.definition.engine.gearRatios[0]/60F/20F);
			angularPosition += angularVelocity;
		}else{
			if(connectedEngine.RPM >= 100){
				List<EntityLivingBase> collidedEntites = vehicle.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getAABBWithOffset(Vec3d.ZERO).expand(0.2F, 0.2F, 0.2F));
				if(!collidedEntites.isEmpty()){
					Entity attacker = null;
					for(Entity passenger : vehicle.getPassengers()){
						if(vehicle.getSeatForRider(passenger).isController){
							attacker = passenger;
							break;
						}
					}
					for(int i=0; i < collidedEntites.size(); ++i){
						if(!vehicle.equals(collidedEntites.get(i).getRidingEntity())){
							collidedEntites.get(i).attackEntityFrom(new DamageSourcePropellor(attacker), (float) (ConfigSystem.configObject.damage.propellerDamageFactor.value*connectedEngine.RPM*connectedEngine.definition.engine.gearRatios[0]/500F));
						}
					}
				}
				if(this.isPartCollidingWithBlocks(Vec3d.ZERO)){
					damagePropeller(1);
					
				}
				if(connectedEngine.RPM*connectedEngine.definition.engine.gearRatios[0]/60*Math.PI*definition.propeller.diameter*0.0254 > 340.29){
					damagePropeller(9999);
				}
			}
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
		return definition.propeller.diameter*0.0254F/2F;
	}

	@Override
	public float getHeight(){
		return definition.propeller.diameter*0.0254F;
	}

	@Override
	public Vec3d getActionRotation(float partialTicks){
		//If we are on an engine that can reverse, adjust our direction.
		//Getting smooth changes here is a PITA, and I ain't gonna do it myself.
		if(vehicle instanceof EntityVehicleG_Blimp && vehicle.reversePercent != 0){
			return vehicle.reversePercent != 20 ? Vec3d.ZERO : new Vec3d(0, 0, -(this.angularPosition + this.angularVelocity*partialTicks));
		}else{
			return new Vec3d(0, 0, this.angularPosition + this.angularVelocity*partialTicks);
		}
	}
	
	private void damagePropeller(float damage){
		this.damage += damage;
		if(this.damage > definition.propeller.startingHealth && !vehicle.world.isRemote){
			vehicle.removePart(this, true);
		}
	}
}
