package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.systems.ConfigSystem;

public class PartPropeller extends APart{	
	public double angularPosition;
	public double angularVelocity;
	public double damageAmount;
	public int currentPitch;
	
	private final PartEngine connectedEngine;
	private final Point3d propellerForce = new Point3d();
	
	public static final int MIN_DYNAMIC_PITCH = 45;
	
	public PartPropeller(AEntityE_Multipart<?> entityOn, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placementDefinition, data, parentPart);
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
		if(damage.entityResponsible instanceof WrapperPlayer && ((WrapperPlayer) damage.entityResponsible).getHeldStack().isEmpty()){
			if(!entityOn.equals(damage.entityResponsible.getEntityRiding())){
				connectedEngine.handStartEngine();
				InterfacePacket.sendToAllClients(new PacketPartEngine(connectedEngine, Signal.HS_ON));
			}
			return;
		}
		this.damageAmount += damage.amount;
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Maybe we aren't connected to an engine?  Not sure how this could happen, but it could.
			if(connectedEngine == null){
				isValid = false;
				return false;
			}
			//If we are a dynamic-pitch propeller, adjust ourselves to the speed of the engine.
			if(definition.propeller.isDynamicPitch && vehicleOn != null){
				if(vehicleOn.reverseThrust && currentPitch > -MIN_DYNAMIC_PITCH){
					--currentPitch;
				}else if(!vehicleOn.reverseThrust && currentPitch < MIN_DYNAMIC_PITCH){
					++currentPitch;
				}else if(connectedEngine.rpm < PartEngine.getSafeRPM(connectedEngine.definition.engine)*0.60 && currentPitch > MIN_DYNAMIC_PITCH){
					--currentPitch;
				}else if(connectedEngine.rpm > PartEngine.getSafeRPM(connectedEngine.definition.engine)*0.85 && currentPitch < definition.propeller.pitch){
					++currentPitch;
				}
			}
			
			//Adjust angular position and velocity.
			if(connectedEngine.propellerGearboxRatio != 0){
				if(connectedEngine.currentGear > 0){
					angularVelocity = (float) (connectedEngine.rpm/connectedEngine.propellerGearboxRatio/60F/20F);
				}else{
					angularVelocity = (float) (-connectedEngine.rpm/connectedEngine.propellerGearboxRatio/60F/20F);
				}
			}else if(angularVelocity > .01){
				angularVelocity -= 0.01;
			}else if(angularVelocity < -.01){
				angularVelocity += 0.01;
			}else{
				angularVelocity = 0;
			}
			angularPosition += angularVelocity;
			
			//Damage propeller or entities if required.
			if(!world.isClient()){
				if(connectedEngine.rpm >= 100){
					//Expand the bounding box bounds, and send off the attack.
					boundingBox.widthRadius += 0.2;
					boundingBox.heightRadius += 0.2;
					boundingBox.depthRadius += 0.2;
					Damage propellerDamage = new Damage("propellor", ConfigSystem.configObject.damage.propellerDamageFactor.value*connectedEngine.rpm*connectedEngine.propellerGearboxRatio/500F, boundingBox, this, vehicleOn != null ? vehicleOn.getController() : null);
					world.attackEntities(propellerDamage, null);
					boundingBox.widthRadius -= 0.2;
					boundingBox.heightRadius -= 0.2;
					boundingBox.depthRadius -= 0.2;
					
					//If the propeller is colliding with blocks, damage it.
					if(!boundingBox.collidingBlockPositions.isEmpty()){
						++damageAmount;
						
					}
					
					//If the propeller is over-speeding, damage it enough to break it.
					if(20*angularVelocity*Math.PI*definition.propeller.diameter*0.0254 > 340.29){
						damageAmount += definition.propeller.startingHealth;
					}
				}
				
				//If we are too damaged, remove ourselves.
				if(damageAmount > definition.propeller.startingHealth && !world.isClient()){
					if(ConfigSystem.configObject.damage.explosions.value){
						world.spawnExplosion(position, 1F, true);
					}else{
						world.spawnExplosion(position, 0F, false);
					}
					isValid = false;
				}
			}
			return true;
		}else{
			return false;
		}
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
	public Point3d getRenderingRotation(float partialTicks, boolean animationValue){
		if(definition.propeller.isRotor && vehicleOn != null){
			Point3d rotations = new Point3d(vehicleOn.elevatorAngle*10D/EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, vehicleOn.aileronAngle*10D/EntityVehicleF_Physics.MAX_AILERON_ANGLE, (angularPosition + angularVelocity*partialTicks)*360D);
			if(vehicleOn.isVTOL){
				rotations.add(vehicleOn.elevatorTrim*20D/EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, vehicleOn.aileronTrim*20D/EntityVehicleF_Physics.MAX_AILERON_ANGLE, 0);
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
			Point3d propellerThrustAxis = new Point3d(0D, 0D, 1D).rotateCoarse(localAngles.copy().add(entityOn.angles));
			if(connectedEngine.currentGear < 0){
				propellerThrustAxis.multiply(-1);
			}
			double currentLinearVelocity = 20D*entityOn.motion.dotProduct(propellerThrustAxis);
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
				thrust *= airDensity/25D*1.5D;

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
					Point3d propellerActionRotation = getRenderingRotation(0, false);
					propellerActionRotation.z = 0;
					propellerThrustVector.rotateCoarse(propellerActionRotation); 
				}
				propellerForce.add(propellerThrustVector.rotateCoarse(localAngles));
			}
		}
		return propellerForce;
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setDouble("damageAmount", damageAmount);
	}
}
