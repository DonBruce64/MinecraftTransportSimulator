package minecrafttransportsimulator.vehicles.parts;

import mcinterface.InterfaceNetwork;
import mcinterface.WrapperNBT;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine.Signal;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public class PartPropeller extends APart{	
	public double angularPosition;
	public double angularVelocity;
	public double damage;
	public short currentPitch;
	
	private final PartEngine connectedEngine;
	
	public static final int MIN_DYNAMIC_PITCH = 45;
	
	public PartPropeller(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, WrapperNBT data){
		super(vehicle, packVehicleDef, definition, data);
		this.damage = data.getDouble("damage");
		this.currentPitch = definition.propeller.pitch;
		this.connectedEngine = (PartEngine) parentPart;
	}
	
	@Override
	public void attack(Damage damage){
		if(damage.attacker != null){
			if(damage.attacker.getHeldStack().isEmpty()){
				if(!vehicle.equals(damage.attacker.getEntityRiding())){
					connectedEngine.handStartEngine();
					InterfaceNetwork.sendToClientsTracking(new PacketVehiclePartEngine(connectedEngine, Signal.HS_ON), vehicle);
				}
				return;
			}
		}
		this.damage += damage.amount;
	}
	
	@Override
	public void update(){
		super.update();
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
		if(!vehicle.world.isClient()){
			if(connectedEngine.rpm >= 100){
				//Expand the bounding box bounds, and send off the attack.
				boundingBox.widthRadius += 0.2;
				boundingBox.heightRadius += 0.2;
				boundingBox.depthRadius += 0.2;
				Damage propellerDamage = new Damage("propellor", ConfigSystem.configObject.damage.propellerDamageFactor.value*connectedEngine.rpm*propellerGearboxRatio/500F, boundingBox, vehicle.getController());
				vehicle.world.attackEntities(propellerDamage, vehicle);
				boundingBox.widthRadius -= 0.2;
				boundingBox.heightRadius -= 0.2;
				boundingBox.depthRadius -= 0.2;
				
				//If the propeller is colliding with blocks, damage it.
				if(!boundingBox.collidingBlocks.isEmpty()){
					++damage;
					
				}
				
				//If the propeller is over-speeding, damage it enough to break it.
				if(20*angularVelocity*Math.PI*definition.propeller.diameter*0.0254 > 340.29){
					damage += 9999;
				}
			}
			
			//If we are too damaged, remove ourselves.
			if(damage > definition.propeller.startingHealth && !vehicle.world.isClient()){
				isValid = false;
			}
		}
	}
	
	@Override
	public WrapperNBT getData(){
		WrapperNBT data = super.getData();		
		data.setDouble("damage", damage);
		return data;
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
}
