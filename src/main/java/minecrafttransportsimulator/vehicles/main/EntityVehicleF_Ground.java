package minecrafttransportsimulator.vehicles.main;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.wrappers.WrapperNetwork;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;


public abstract class EntityVehicleF_Ground extends EntityVehicleE_Powered{	
	//Note that angle variable should be divided by 10 to get actual angle.
	public final short MAX_STEERING_ANGLE = 450;
	public final short STEERING_DAMPEN_RATE = 20;
	public short steeringAngle;
	public byte steeringCooldown; 
	
	//Internal variables
	private double forwardForce;//kg*m/ticks^2
	private double dragForce;//kg*m/ticks^2
	private double gravitationalForce;//kg*m/ticks^2
	
	//Variables used for towed logic.
	private double deltaYaw;
	private Point3d hookupOffset;
	private Point3d hookupPos;
	private Point3d hitchOffset;
	private Point3d hitchOffset2;
	private Point3d hitchPos;
	private Point3d hitchPos2;
	private Point3d xzPlaneDelta;
	private Point3d xzPlaneHeading;
	
	public boolean cruiseControl;
	public double cruiseControlSpeed;
	public EntityVehicleF_Ground towedVehicle;
	public EntityVehicleF_Ground towedByVehicle;
	public byte towingAngle;
	
	public EntityVehicleF_Ground(World world){
		super(world);
	}
	
	public EntityVehicleF_Ground(World world, float posX, float posY, float posZ, float rotation, JSONVehicle definition){
		super(world, posX, posY, posZ, rotation, definition);
	}
	
	@Override
	protected float getCurrentMass(){
		if(towedVehicle != null){
			return super.getCurrentMass() + towedVehicle.getCurrentMass();
		}else{
			return super.getCurrentMass();
		}
	}
	
	@Override
	protected void getBasicProperties(){
		if(towedByVehicle != null){
			if(towingAngle < 30){
				++towingAngle;
			}
		}else{
			if(towingAngle > 0){
				--towingAngle;
			}
		}
	}
	
	@Override
	protected void getForcesAndMotions(){
		forwardForce = 0;
		for(PartEngine engine : engines.values()){
			forwardForce += engine.getForceOutput().z;
		}
		dragForce = 0.5F*airDensity*velocity*velocity*5.0F*getDragCoefficient();
		gravitationalForce = currentMass*(9.8/400);
		
		//If we are linked, move us with the other vehicle.
		//Otherwise, do our own logic for movement.
		if(towedByVehicle != null){
			//Check to make sure vehicle isn't dead for some reason.
			if(towedByVehicle.isDead){
				towedByVehicle = null;
			}else{
				//Update our lights to match our towers if we are a trailer.
				if(definition.motorized.isTrailer){
					lightsOn.clear();
					lightsOn.addAll(towedByVehicle.lightsOn);
				}
				
				//Turn off the parking brake, but turn on the brake if we are a trailer and our tower is braking.
				parkingBrakeOn = false;
				brakeOn = definition.motorized.isTrailer && towedByVehicle.brakeOn;
				
				//We use a second hitchPos here to allow us to calculate the yaw angle we need to apply.
				//If we don't, the vehicle has no clue of the orientation of the towed vehicle hitch and gets all jittery.
				//This is because when the hitch and the hookup are at the same point, the dot product returns floating-point errors.
				hookupOffset = new Point3d(definition.motorized.hookupPos[0], definition.motorized.hookupPos[1], definition.motorized.hookupPos[2]);
				hookupPos = RotationSystem.getRotatedPoint(hookupOffset, rotationPitch, rotationYaw, rotationRoll).add(currentPosition);
				hitchOffset = new Point3d(towedByVehicle.definition.motorized.hitchPos[0], towedByVehicle.definition.motorized.hitchPos[1], towedByVehicle.definition.motorized.hitchPos[2]);
				hitchOffset2 = new Point3d(towedByVehicle.definition.motorized.hitchPos[0], towedByVehicle.definition.motorized.hitchPos[1], towedByVehicle.definition.motorized.hitchPos[2] + 0.5);
				hitchPos = RotationSystem.getRotatedPoint(hitchOffset, towedByVehicle.rotationPitch, towedByVehicle.rotationYaw, towedByVehicle.rotationRoll).add(towedByVehicle.currentPosition);
				hitchPos2 = RotationSystem.getRotatedPoint(hitchOffset2, towedByVehicle.rotationPitch, towedByVehicle.rotationYaw, towedByVehicle.rotationRoll).add(towedByVehicle.currentPosition);
				
				xzPlaneDelta = new Point3d(hitchPos2.x - hookupPos.x, 0, hitchPos2.z - hookupPos.z).normalize();
				xzPlaneHeading = new Point3d(currentHeading.x, 0, currentHeading.z).normalize();
				deltaYaw = Math.toDegrees(Math.acos(Math.min(Math.abs(xzPlaneDelta.dotProduct(xzPlaneHeading)), 1)));
				if(xzPlaneDelta.crossProduct(xzPlaneHeading).y < 0){
					deltaYaw *= -1;
				}
				
				//Don't apply yaw if we aren't moving. Apply Yaw in proportion to trailer length
				motionYaw = velocity > 0 ? (float) (deltaYaw/(2*Math.abs(definition.motorized.hookupPos[2]))) : 0;
				//If we are in the air, pitch up to get our devices on the ground. Pitching speed is determined by elevation difference of rear wheels.
				motionPitch = groundedGroundDevices.isEmpty() ? -(float)(Math.min(Math.max(Math.abs((((towedByVehicle.rearLeftGroundDeviceBox.currentBox.minY + towedByVehicle.rearRightGroundDeviceBox.currentBox.minY)/2) - ((rearLeftGroundDeviceBox.currentBox.minY + rearRightGroundDeviceBox.currentBox.minY)/2)) * 2),0.1),1.0)) : 0;
				//Match our tower's roll.
				motionRoll = (towedByVehicle.rotationRoll - rotationRoll)/10;
				
				//Now set the motions.
				motionX = hitchPos.x - hookupPos.x;
				motionY = hitchPos.y - hookupPos.y;
				motionZ = hitchPos.z - hookupPos.z;
				return;
			}
		}else{
			//Make sure the parking brake is on if this is a disconnected trailer
			if(definition.motorized.isTrailer){
				parkingBrakeOn = true;
			}

		}
		
		motionX += (currentHeading.x*forwardForce - currentVelocity.x*dragForce)/currentMass;
		motionZ += (currentHeading.z*forwardForce - currentVelocity.z*dragForce)/currentMass;
		motionY += (currentHeading.y*forwardForce - currentVelocity.y*dragForce - gravitationalForce)/currentMass;
		
		motionYaw = 0;
		motionPitch = 0;
		motionRoll = 0;
	}
	
	@Override
	protected void dampenControlSurfaces(){
		if(cruiseControl){
			if(velocity < cruiseControlSpeed){
				if(throttle < 100){
					WrapperNetwork.sendToClientsTracking(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 1, (byte) 0), this);
					++throttle;
				}
			}else if(velocity > cruiseControlSpeed){
				if(throttle > 0){
					WrapperNetwork.sendToClientsTracking(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.THROTTLE, (short) -1, (byte) 0), this);
					--throttle;
				}
			}
		}
		
		if(steeringCooldown==0){
			if(steeringAngle != 0){
				if(steeringAngle < STEERING_DAMPEN_RATE && steeringAngle > -STEERING_DAMPEN_RATE){
					WrapperNetwork.sendToClientsTracking(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.STEERING, (short) -steeringAngle, (byte) 0), this);
					steeringAngle = 0;
				}else{
					WrapperNetwork.sendToClientsTracking(new PacketVehicleControlAnalog(this, PacketVehicleControlAnalog.Controls.STEERING, steeringAngle < 0 ? STEERING_DAMPEN_RATE : -STEERING_DAMPEN_RATE, (byte) 0), this);
					steeringAngle += steeringAngle < 0 ? STEERING_DAMPEN_RATE : -STEERING_DAMPEN_RATE;
				}
			}
		}else{
			--steeringCooldown;
		}
	}
	
	@Override
	public float getSteerAngle(){
		return -steeringAngle/10F;
	}
	
	protected abstract float getDragCoefficient();
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.steeringAngle=tagCompound.getShort("steeringAngle");
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setShort("steeringAngle", this.steeringAngle);
		return tagCompound;
	}
}
