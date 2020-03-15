package minecrafttransportsimulator.vehicles.main;

import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngineBoat;
import net.minecraft.world.World;


public final class EntityVehicleG_Boat extends EntityVehicleF_Ground{	

	public EntityVehicleG_Boat(World world){
		super(world);
	}
	
	public EntityVehicleG_Boat(World world, float posX, float posY, float posZ, float rotation, JSONVehicle definition){
		super(world, posX, posY, posZ, rotation, definition);
	}
	
	@Override
	protected float getDragCoefficient(){
		return 2.0F;
	}
	
	@Override
	protected float getBrakingForceFactor(){
		//Need to manually add breaking factor here for anchors, even though we don't collide with anything.
		return brakeOn || parkingBrakeOn ? super.getBrakingForceFactor() + 2.0F : super.getBrakingForceFactor();
	}
	
	@Override
	protected float getSkiddingFactor(){
		float skiddingFactor = 0;
		for(VehicleAxisAlignedBB box : collisionBoxes){
			if(box.collidesWithLiquids){
				skiddingFactor += 0.125F; 
			}
		}
		return skiddingFactor > 0 ? skiddingFactor : 0;
	}
	
	@Override
	protected float getTurningFactor(){
		//Copied from the D-class, but made to work with boats using collision boxes rather than ground devices.
		//We need to do this as boats don't have ground devices to make them turn.
		float turningForce = 0;
		float steeringAngle = this.getSteerAngle();
		if(steeringAngle != 0){
			float turningFactor = 0;
			float turningDistance = 0;
			for(APart<? extends EntityVehicleA_Base> part : this.getVehicleParts()){
				//Adjust yaw if we have an engine in the water.
				if(part instanceof PartEngineBoat){
					turningFactor += 1.0F;
					turningDistance = (float) Math.max(turningDistance, Math.abs(part.offset.z));
				}
			}
			if(turningFactor > 0){
				//Now that we know we can turn, we can attempt to change the track.
				steeringAngle = Math.abs(steeringAngle);
				if(turningFactor < 1){
					steeringAngle *= turningFactor;
				}
				//Adjust steering angle to be aligned with distance of the turning part from the center of the vehicle.
				steeringAngle /= turningDistance;
				//Another thing that can affect the steering angle is speed.
				//More speed makes for less wheel turn to prevent crazy circles.
				if(Math.abs(velocity*SPEED_FACTOR/0.35F) - turningFactor/3F > 0){
					steeringAngle *= Math.pow(0.25F, (Math.abs(velocity*(0.75F + SPEED_FACTOR/0.35F/4F)) - turningFactor/3F));
				}
				//Adjust turn force to steer angle based on turning factor.
				turningForce = -(float) (steeringAngle*velocity/2F);
				//Correct for speedFactor changes.
				turningForce *= SPEED_FACTOR/0.35F;
				//Now add the sign to this force.
				turningForce *= Math.signum(this.getSteerAngle());
			}
		}
		return turningForce;
	}
}