package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.NetworkSystem;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine.Signal;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public class PartPropeller extends APart{	
	public double angularPosition;
	public double angularVelocity;
	public double damageAmount;
	public short currentPitch;
	
	private final PartEngine connectedEngine;
	private final Point3d propellerForce = new Point3d(0D, 0D, 0D);
	
	public static final int MIN_DYNAMIC_PITCH = 45;
	
	public PartPropeller(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, ItemPart item, WrapperNBT data, APart parentPart){
		super(vehicle, packVehicleDef, item, data, parentPart);
		this.damageAmount = data.getDouble("damageAmount");
		this.currentPitch = definition.propeller.pitch;
		this.connectedEngine = (PartEngine) parentPart;
		if(definition.propeller.isRotor){
			//Rotors need different collision box bounds as they are pointed upwards.
			boundingBox.widthRadius = getWidth()/2D;
			boundingBox.heightRadius = 0.25D;
			boundingBox.depthRadius = getWidth()/2D;
			
		}
	}
	
	@Override
	public void attack(Damage damage){
		if(damage.attacker != null){
			if(damage.attacker instanceof WrapperPlayer && ((WrapperPlayer) damage.attacker).getHeldItem() == null){
				if(!vehicle.equals(damage.attacker.getEntityRiding())){
					connectedEngine.handStartEngine();
					NetworkSystem.sendToAllClients(new PacketVehiclePartEngine(connectedEngine, Signal.HS_ON));
				}
				return;
			}
		}
		this.damageAmount += damage.amount;
	}
	
	@Override
	public void update(){
		super.update();
		//Maybe we aren't connected to an engine?  Not sure how this could happen, but it could.
		if(connectedEngine == null){
			isValid = false;
			return;
		}
		//If we are a dynamic-pitch propeller, adjust ourselves to the speed of the engine.
		if(definition.propeller.isDynamicPitch){
			if(vehicle.reverseThrust && currentPitch > -MIN_DYNAMIC_PITCH){
				--currentPitch;
			}else if(!vehicle.reverseThrust && currentPitch < MIN_DYNAMIC_PITCH){
				++currentPitch;
			}else if(connectedEngine.rpm < PartEngine.getSafeRPMFromMax(connectedEngine.definition.engine.maxRPM)*0.60 && currentPitch > MIN_DYNAMIC_PITCH){
				--currentPitch;
			}else if(connectedEngine.rpm > PartEngine.getSafeRPMFromMax(connectedEngine.definition.engine.maxRPM)*0.85 && currentPitch < definition.propeller.pitch){
				++currentPitch;
			}
		}
		
		//Adjust angular position and velocity.
		if(connectedEngine.propellerGearboxRatio != 0){
			angularVelocity = (float) (connectedEngine.rpm/connectedEngine.propellerGearboxRatio/60F/20F);
		}else if(angularVelocity > .01){
			angularVelocity -= 0.01;
		}else if(angularVelocity < -.01){
			angularVelocity += 0.01;
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
				Damage propellerDamage = new Damage("propellor", ConfigSystem.configObject.damage.propellerDamageFactor.value*connectedEngine.rpm*connectedEngine.propellerGearboxRatio/500F, boundingBox, vehicle.getController());
				vehicle.world.attackEntities(propellerDamage, vehicle.wrapper, null);
				boundingBox.widthRadius -= 0.2;
				boundingBox.heightRadius -= 0.2;
				boundingBox.depthRadius -= 0.2;
				
				//If the propeller is colliding with blocks, damage it.
				if(!boundingBox.collidingBlocks.isEmpty()){
					++damageAmount;
					
				}
				
				//If the propeller is over-speeding, damage it enough to break it.
				if(20*angularVelocity*Math.PI*definition.propeller.diameter*0.0254 > 340.29){
					damageAmount += definition.propeller.startingHealth;
				}
			}
			
			//If we are too damaged, remove ourselves.
			if(damageAmount > definition.propeller.startingHealth && !vehicle.world.isClient()){
				if(ConfigSystem.configObject.damage.explosions.value){
					vehicle.world.spawnExplosion(vehicle, worldPos, 1F, true);
				}else{
					vehicle.world.spawnExplosion(vehicle, worldPos, 0F, false);
				}
				isValid = false;
			}
		}
	}
	
	@Override
	public WrapperNBT getData(){
		WrapperNBT data = super.getData();		
		data.setDouble("damageAmount", damageAmount);
		return data;
	}
	
	@Override
	public float getWidth(){
		return definition.propeller.diameter*0.0254F;
	}

	@Override
	public float getHeight(){
		return definition.propeller.diameter*0.0254F;
	}

	@Override
	public Point3d getRenderingRotation(float partialTicks){
		if(definition.propeller.isRotor){
			Point3d rotations = new Point3d(vehicle.elevatorAngle*10D/EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, vehicle.aileronAngle*10D/EntityVehicleF_Physics.MAX_AILERON_ANGLE, (angularPosition + angularVelocity*partialTicks)*360D);
			if(vehicle.isVTOL){
				rotations.add(vehicle.elevatorTrim*20D/EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, vehicle.aileronTrim*20D/EntityVehicleF_Physics.MAX_AILERON_ANGLE, 0);
			}
			return rotations;
		}else{
			return new Point3d(0, 0, (angularPosition + angularVelocity*partialTicks)*360D);
		}
	}
	
	public Point3d getForceOutput(){
		propellerForce.set(0D, 0D, 0D);
		if(connectedEngine != null && connectedEngine.state.running){
			//Get the current linear velocity of the propeller, based on our axial velocity.
			//This is is meters per second.
			Point3d propellerThrustAxis = new Point3d(0D, 0D, 1D).rotateCoarse(totalRotation.copy().add(vehicle.angles));
			double currentLinearVelocity = 20D*vehicle.motion.dotProduct(propellerThrustAxis);
			//Get the desired linear velocity of the propeller, based on the current RPM and pitch.
			//We add to the desired linear velocity by a small factor.  This is because the actual cruising speed of aircraft
			//is based off of engine max RPM equating exactly to ideal linear speed of the propeller.  I'm sure there are nuances
			//here, like perhaps the propeller manufactures reporting the prop pitch to match cruise, but for physics, that don't work,
			//because the propeller never reaches that speed during cruise due to drag.  So we add a small addition here to compensate.
			double desiredLinearVelocity = 0.0254D*(currentPitch + 20)*20D*angularVelocity;
			//Not sure why, but this follows given the fact cruising speed of aircraft is a bit
			if(desiredLinearVelocity != 0){
				//Thrust produced by the propeller is the difference between the desired linear velocity and the current linear velocity.
				//This gets the magnitude of the initial thrust force.
				double thrust = (desiredLinearVelocity - currentLinearVelocity);
				//Multiply the thrust difference by the area of the propeller.  This accounts for the force-area defined by it.
				thrust *= Math.PI*Math.pow(0.0254*definition.propeller.diameter/2D, 2);
				//Finally, multiply by the air density, and a constant.  Less dense air causes less thrust force.
				thrust *= vehicle.airDensity/25D*1.5D;

				//Get the angle of attack of the propeller.
				//Note pitch velocity is in linear in meters per second, 
				//This means we need to convert it to meters per revolution before we can move on.
				//This gets the angle as a ratio of forward pitch to propeller circumference.
				//If the angle of attack is greater than 25 degrees (or a ratio of 0.4663), sap power off the propeller for stalling.
				double angleOfAttack = ((desiredLinearVelocity - currentLinearVelocity)/(connectedEngine.rpm/connectedEngine.propellerGearboxRatio/60D))/(definition.propeller.diameter*Math.PI*0.0254D);
				if(Math.abs(angleOfAttack) > 0.4663D){
					thrust *= 0.4663D/Math.abs(angleOfAttack);
				}
				
				//If the propeller is in the water, increase thrust.
				if(isInLiquid()){
					thrust *= 50;
				}
				
				//Add propeller force to total engine force as a vector.
				//Depends on propeller orientation, as upward propellers provide upwards thrust.
				Point3d propellerThrustVector = new Point3d(0D, 0D, thrust);
				if(definition.propeller.isRotor){
					//Get the X and Y coords of the action rotation for thrust vectoring on rotors.
					Point3d propellerActionRotation = getRenderingRotation(0);
					propellerActionRotation.z = 0;
					propellerThrustVector.rotateCoarse(propellerActionRotation); 
				}
				propellerForce.add(propellerThrustVector.rotateCoarse(totalRotation));
			}
		}
		return propellerForce;
	}
}
