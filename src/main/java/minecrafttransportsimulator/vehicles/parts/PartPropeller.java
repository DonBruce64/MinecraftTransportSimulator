package minecrafttransportsimulator.vehicles.parts;

import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.dataclasses.DamageSources.DamageSourcePropellor;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal.PacketEngineTypes;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;

public class PartPropeller extends APart{	
	public float angularPosition;
	public float angularVelocity;
	public float damage;
	public short currentPitch;
	
	private final PartEngine connectedEngine;
	
	public static final int MIN_DYNAMIC_PITCH = 45;
	
	public PartPropeller(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
		this.damage = dataTag.getFloat("damage");
		this.currentPitch = definition.propeller.pitch;
		this.connectedEngine = (PartEngine) parentPart;
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
		if(definition.propeller.isDynamicPitch){
			if(vehicle.reverseThrust && currentPitch > -MIN_DYNAMIC_PITCH){
				--currentPitch;
			}else if(!vehicle.reverseThrust && currentPitch < MIN_DYNAMIC_PITCH){
				++currentPitch;
			}else if(connectedEngine.rpm < (PartEngine.getSafeRPMFromMax(connectedEngine.definition.engine.maxRPM) - 200) && currentPitch > MIN_DYNAMIC_PITCH){
				--currentPitch;
			}else if(connectedEngine.rpm > (PartEngine.getSafeRPMFromMax(connectedEngine.definition.engine.maxRPM) - 150) && currentPitch < definition.propeller.pitch){
				++currentPitch;
			}
		}
		
		double propellerGearboxRatio = connectedEngine.definition.engine.propellerRatio != 0 ? connectedEngine.definition.engine.propellerRatio*Math.signum(connectedEngine.currentGear) : (connectedEngine.currentGear != 0 ? connectedEngine.definition.engine.gearRatios[connectedEngine.currentGear + connectedEngine.reverseGears] : 0);
		
		//Adjust angular position and velocity.
		if(propellerGearboxRatio != 0){
			angularVelocity = (float) (connectedEngine.rpm/propellerGearboxRatio/60F/20F);
		}else if(angularVelocity > 1){
			--angularVelocity;
		}else if(angularVelocity < -1){
			++angularVelocity;
		}else{
			angularVelocity = 0;
		}
		angularPosition += angularVelocity;
		
		//Damage propeller or entities if required.
		if(!vehicle.world.isRemote){
			if(connectedEngine.rpm >= 100){
				List<EntityLivingBase> collidedEntites = vehicle.world.getEntitiesWithinAABB(EntityLivingBase.class, boundingBox.expand(0.2F, 0.2F, 0.2F));
				if(!collidedEntites.isEmpty()){
					Entity attacker = null;
					for(Entity passenger : vehicle.getPassengers()){
						PartSeat seat = vehicle.getSeatForRider(passenger);
						if(seat != null && seat.vehicleDefinition.isController){
							attacker = passenger;
							break;
						}
					}
					for(int i=0; i < collidedEntites.size(); ++i){
						if(!vehicle.equals(collidedEntites.get(i).getRidingEntity())){
							collidedEntites.get(i).attackEntityFrom(new DamageSourcePropellor(attacker), (float) (ConfigSystem.configObject.damage.propellerDamageFactor.value*connectedEngine.rpm*propellerGearboxRatio/500F));
						}
					}
				}
				if(isPartColliding()){
					damagePropeller(1);
					
				}
				if(20*angularVelocity*Math.PI*definition.propeller.diameter*0.0254 > 340.29){
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
	public Point3d getActionRotation(float partialTicks){
		if(definition.propeller.isRotor){
			return new Point3d(-vehicle.elevatorAngle*10D/EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, vehicle.aileronAngle*10D/EntityVehicleF_Physics.MAX_AILERON_ANGLE, (angularPosition + angularVelocity*partialTicks)*360D);
		}else{
			return new Point3d(0, 0, (angularPosition + angularVelocity*partialTicks)*360D);
		}
	}
	
	private void damagePropeller(float damage){
		this.damage += damage;
		if(this.damage > definition.propeller.startingHealth && !vehicle.world.isRemote){
			vehicle.removePart(this, true);
		}
	}
}
