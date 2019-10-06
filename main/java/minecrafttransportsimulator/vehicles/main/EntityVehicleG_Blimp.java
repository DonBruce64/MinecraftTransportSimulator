package minecrafttransportsimulator.vehicles.main;

import net.minecraft.world.World;

public final class EntityVehicleG_Blimp extends EntityVehicleF_Air{
	//Internal plane variables
	private double dragCoeff;
	private double wingLiftCoeff;
	
	private double dragForce;//kg*m/ticks^2
	private double rudderForce;//kg*m/ticks^2
	private double rudderTorque;//kg*m^2/ticks^2
	
	private double ballastForce;
			
	public EntityVehicleG_Blimp(World world){
		super(world);
	}
	
	public EntityVehicleG_Blimp(World world, float posX, float posY, float posZ, float rotation, String name){
		super(world, posX, posY, posZ, rotation, name);
	}
	
	@Override
	protected void getBasicProperties(){
		super.getBasicProperties();
		//Blimps are turned with rudders, not ailerons.  This puts the keys at an odd location.  To compensate, 
		//we set the rudder to the aileron if the aileron is greater or less than the rudder.  That way no matter 
		//which key is pressed, they both activate the rudder for turning.
		if((aileronAngle < 0 && aileronAngle < rudderAngle) || (aileronAngle > 0 && aileronAngle > rudderAngle)){
			rudderAngle = aileronAngle;
			rudderCooldown = aileronCooldown;
		}
		dragCoeff = 0.0004F*Math.pow(trackAngle, 2) + 0.04F;
	}
	
	@Override
	protected void getForcesAndMotions(){
		super.getForcesAndMotions();
		dragForce = 0.5F*airDensity*velocity*velocity*pack.blimp.crossSectionalArea*dragCoeff;		
		rudderForce = 0.5F*airDensity*velocity*velocity*pack.blimp.rudderArea*rudderLiftCoeff;
		rudderTorque = rudderForce*pack.blimp.tailDistance;
		
		//Ballast gets less effective at applying positive lift at higher altitudes.
		//This prevents blimps from ascending into space.
		//Also take into account motionY, as we should provide less force if we are already going in the same direction.
		if(elevatorAngle < 0){
			ballastForce = 5*airDensity*pack.blimp.ballastVolume*-elevatorAngle;
		}else{
			ballastForce = 5*1.225*pack.blimp.ballastVolume*-elevatorAngle;
		}		
		if(motionY*ballastForce > 0){
			ballastForce /= Math.pow(1 + Math.abs(motionY), 2);
		}
		
		//If we don't have any thrustForce, start slowing down.
		//We can't use brakes because those don't exist on blimps!
		if(thrustForce == 0){
			motionX *= 0.95;
			motionZ *= 0.95;
		}
				
		//Note that only the ballast, thrust, and drag affect movement.  Gravity and control surfaces do not.
		//MotionY is directly based on ballast, as that's the only thing that can affect it.
		motionX += (headingVec.x*thrustForce - velocityVec.x*dragForce + verticalVec.x*ballastForce)/currentMass;
		motionZ += (headingVec.z*thrustForce - velocityVec.z*dragForce + verticalVec.z*ballastForce)/currentMass;
		motionY = ballastForce/currentMass;
		
		//Roll and pitch are applied only if we aren't level.
		//This only happens if we fall out of the sky and land on the ground and tilt.
		if(rotationRoll > 0){
			motionRoll = -Math.min(0.5F, rotationRoll);
		}else if(rotationRoll < 0){
			motionRoll = -Math.max(-0.5F, rotationRoll);
		}else{
			motionRoll = 0;
		}
		if(rotationPitch > 0){
			motionPitch = -Math.min(0.5F, rotationPitch);
		}else if(rotationPitch < 0){
			motionPitch = -Math.max(-0.5F, rotationPitch);
		}else{
			motionPitch = 0;
		}
		motionYaw = (float) (180/Math.PI*(-verticalVec.y*(-thrustTorque - rudderTorque))/momentYaw);
	}
}