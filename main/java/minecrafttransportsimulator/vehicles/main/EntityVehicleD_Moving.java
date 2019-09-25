package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.VehicleAxisAlignedBB;
import minecrafttransportsimulator.packets.vehicles.PacketVehicleDeltas;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevicePontoon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**At the final basic vehicle level we add in the functionality for state-based movement.
 * Here is where the functions for moving permissions, such as collision detection
 * routines and ground device effects come in.  We also add functionality to keep
 * servers and clients from de-syncing, and a few basic variables that will be common for all vehicles.
 * At this point we now have a basic vehicle that can be manipulated for movement in the world.  
 * 
 * @author don_bruce
 */
public abstract class EntityVehicleD_Moving extends EntityVehicleC_Colliding{
	public boolean brakeOn;
	public boolean parkingBrakeOn;
	public float motionRoll;
	public float motionPitch;
	public float motionYaw;
	public double velocity;
	
	private double clientDeltaX;
	private double clientDeltaY;
	private double clientDeltaZ;
	private float clientDeltaYaw;
	private float clientDeltaPitch;
	private float clientDeltaRoll;
	private double serverDeltaX;
	private double serverDeltaY;
	private double serverDeltaZ;
	private float serverDeltaYaw;
	private float serverDeltaPitch;
	private float serverDeltaRoll;
	
	/**List of ground devices on the ground.  Populated after each movement to be used in turning/braking calculations.*/
	public final List<APartGroundDevice> groundedGroundDevices = new ArrayList<APartGroundDevice>();
	
	//Lists used for ground device calculations.  These are only used for convenience and not in actual movement math.
	private final List<APartGroundDevice> frontLeftGroundDevices = new ArrayList<APartGroundDevice>();
	private final List<APartGroundDevice> frontRightGroundDevices = new ArrayList<APartGroundDevice>();
	private final List<APartGroundDevice> rearLeftGroundDevices = new ArrayList<APartGroundDevice>();
	private final List<APartGroundDevice> rearRightGroundDevices = new ArrayList<APartGroundDevice>();
	
	//AxisAlignedBBs used for ground device calculations.  These are based off of the lists.
	private VehicleAxisAlignedBB frontLeftGroundDeviceBox;
	private VehicleAxisAlignedBB frontRightGroundDeviceBox;
	private VehicleAxisAlignedBB rearLeftGroundDeviceBox;
	private VehicleAxisAlignedBB rearRightGroundDeviceBox;
	
	//Lists used to save which AABBs have collided with the ground device AABBs above.
	private List<AxisAlignedBB> frontLeftCollidingBoxes;
	private List<AxisAlignedBB> frontRightCollidingBoxes;
	private List<AxisAlignedBB> rearLeftCollidingBoxes;
	private List<AxisAlignedBB> rearRightCollidingBoxes;
	
	//Values of collision depths between the ground device AABBs and the collision box lists above.
	private double frontLeftCollisionDepth;
	private double frontRightCollisionDepth;
	private double rearLeftCollisionDepth;
	private double rearRightCollisionDepth;
	
	//Booleans used to save ground device collision state.  Easier than using list.isEmpty().
	private boolean frontLeftGroundDeviceCollided;
	private boolean frontRightGroundDeviceCollided;
	private boolean rearLeftGroundDeviceCollided;
	private boolean rearRightGroundDeviceCollided;
	private boolean frontLeftGroundDeviceGrounded;
	private boolean frontRightGroundDeviceGrounded;
	private boolean rearLeftGroundDeviceGrounded;
	private boolean rearRightGroundDeviceGrounded;
	
	
	public final double clingSpeed = ConfigSystem.getDoubleConfig("ClingSpeed");
	public static final double maxRotationInRadPerTick = 0.0174533D*2D;
	
	public EntityVehicleD_Moving(World world){
		super(world);
	}
	
	public EntityVehicleD_Moving(World world, float posX, float posY, float posZ, float playerRotation, String vehicleName){
		super(world, posX, posY, posZ, playerRotation, vehicleName);
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(pack != null){
			//Populate the ground device lists for use in the methods here.
			//We need to get which ground devices are in which quadrant,
			//as well as which ground devices are on the ground.
			//This needs to be done before movement calculations so we can do checks during them.
			groundedGroundDevices.clear();
			frontLeftGroundDevices.clear();
			frontRightGroundDevices.clear();
			rearLeftGroundDevices.clear();
			rearRightGroundDevices.clear();
			for(APart part : this.getVehicleParts()){
				if(part instanceof APartGroundDevice){
					//X-offsets of 0 are both left and right as they are center wheels.
					//This ensures we don't roll when center wheels collide.
					if(part.offset.z > 0){
						if(part.offset.x >= 0){
							frontLeftGroundDevices.add((APartGroundDevice) part);
						}
						if(part.offset.x <= 0){
							frontRightGroundDevices.add((APartGroundDevice) part);
						}
					}else{
						if(part.offset.x >= 0){
							rearLeftGroundDevices.add((APartGroundDevice) part);
						}
						if(part.offset.x <= 0){
							rearRightGroundDevices.add((APartGroundDevice) part);
						}
					}
					if(((APartGroundDevice) part).isOnGround()){
						groundedGroundDevices.add((APartGroundDevice) part);
					}
				}
			}
			
			//Now do update calculations and logic.
			getForcesAndMotions();
			performGroundOperations();
			moveVehicle();
			if(!world.isRemote){
				dampenControlSurfaces();
			}
			
			//Finally, update parts.
			for(APart part : this.getVehicleParts()){
				part.updatePart();
			}
		}
	}
	
	/**
	 * Helper function to calculate ground device points.
	 */
	private VehicleAxisAlignedBB getAABBForGroundDeviceSet(List<APartGroundDevice> groundDeviceList){
		if(groundDeviceList.isEmpty()){
			return null;
		}
		
		boolean deviceFloats = false;
		float heights = 0;
		float widths = 0;
		double xCoords = 0;
		double yCoords = 0;
		double zCoords = 0;
		for(APartGroundDevice groundDevice : groundDeviceList){
			if(groundDevice instanceof PartGroundDevicePontoon){
				deviceFloats = true;
			}
			heights += groundDevice.getHeight();
			widths += groundDevice.getWidth();
			xCoords += groundDevice.offset.x;
			yCoords += groundDevice.offset.y;
			zCoords += groundDevice.offset.z;
		}
		
		heights /= groundDeviceList.size();
		widths /= groundDeviceList.size();
		xCoords /= groundDeviceList.size();
		yCoords /= groundDeviceList.size();
		zCoords /= groundDeviceList.size();
		
		Vec3d boxRelativePosition = new Vec3d(xCoords, yCoords, zCoords);
		Vec3d offset = RotationSystem.getRotatedPoint(boxRelativePosition, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
		return new VehicleAxisAlignedBB(this.getPositionVector().add(offset).addVector(motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor), boxRelativePosition, widths, heights, false, deviceFloats); 
	}
	
	/**
	 * Helper function to update the ground device AABBs, AABB collision lists, collision flags, and collision depths.
	 * Only update the collisions if we have any ground devices.
	 * If we are missing any, just set everything to not colliding.
	 * Collision updates follow this format:
	 * 1 Get the current AABBs for the ground devices based on the expected movement.
	 * 2 Get the collision boxes that collide with those AABBs.
	 * 3 Set the collision flags.
	 * 4 Set the collision depths.
	 */
	private void updateGroundDeviceCollisions(){
		if(!frontLeftGroundDevices.isEmpty()){
			frontLeftGroundDeviceBox = getAABBForGroundDeviceSet(frontLeftGroundDevices);
			frontLeftCollidingBoxes = getAABBCollisions(frontLeftGroundDeviceBox, null);
			frontLeftGroundDeviceCollided = !frontLeftCollidingBoxes.isEmpty();
			frontLeftGroundDeviceGrounded = frontLeftGroundDeviceCollided ? false : !this.getAABBCollisions(frontLeftGroundDeviceBox.offset(0, APartGroundDevice.groundDetectionOffset.y, 0), null).isEmpty();
			frontLeftCollisionDepth = frontLeftGroundDeviceCollided ? getCollisionDepthForGroundDeviceCollisions(frontLeftGroundDeviceBox, frontLeftCollidingBoxes) : 0;
		}else{
			frontLeftGroundDeviceBox = null;
			frontLeftGroundDeviceCollided = false;
			frontLeftGroundDeviceGrounded = false;
			frontLeftCollisionDepth = 0;
		}
		
		if(!frontRightGroundDevices.isEmpty()){
			frontRightGroundDeviceBox = getAABBForGroundDeviceSet(frontRightGroundDevices);
			frontRightCollidingBoxes = getAABBCollisions(frontRightGroundDeviceBox, null);
			frontRightGroundDeviceCollided = !frontRightCollidingBoxes.isEmpty();
			frontRightGroundDeviceGrounded = frontRightGroundDeviceCollided ? false : !this.getAABBCollisions(frontRightGroundDeviceBox.offset(0, APartGroundDevice.groundDetectionOffset.y, 0), null).isEmpty();
			frontRightCollisionDepth = frontRightGroundDeviceCollided ? getCollisionDepthForGroundDeviceCollisions(frontRightGroundDeviceBox, frontRightCollidingBoxes) : 0;
		}else{
			frontRightGroundDeviceBox = null;
			frontRightGroundDeviceCollided = false;
			frontRightGroundDeviceGrounded = false;
			frontRightCollisionDepth = 0;
		}
		
		if(!rearLeftGroundDevices.isEmpty()){
			rearLeftGroundDeviceBox = getAABBForGroundDeviceSet(rearLeftGroundDevices);
			rearLeftCollidingBoxes = getAABBCollisions(rearLeftGroundDeviceBox, null);
			rearLeftGroundDeviceCollided = !rearLeftCollidingBoxes.isEmpty();
			rearLeftGroundDeviceGrounded = rearLeftGroundDeviceCollided ? false : !this.getAABBCollisions(rearLeftGroundDeviceBox.offset(0, APartGroundDevice.groundDetectionOffset.y, 0), null).isEmpty();
			rearLeftCollisionDepth = rearLeftGroundDeviceCollided ? getCollisionDepthForGroundDeviceCollisions(rearLeftGroundDeviceBox, rearLeftCollidingBoxes) : 0;
		}else{
			rearLeftGroundDeviceBox = null;
			rearLeftGroundDeviceCollided = false;
			rearLeftGroundDeviceGrounded = false;
			rearLeftCollisionDepth = 0;
		}
		
		if(!rearRightGroundDevices.isEmpty()){
			rearRightGroundDeviceBox = getAABBForGroundDeviceSet(rearRightGroundDevices);
			rearRightCollidingBoxes = getAABBCollisions(rearRightGroundDeviceBox, null);
			rearRightGroundDeviceCollided = !rearRightCollidingBoxes.isEmpty();
			rearRightGroundDeviceGrounded = rearRightGroundDeviceCollided ? false : !this.getAABBCollisions(rearRightGroundDeviceBox.offset(0, APartGroundDevice.groundDetectionOffset.y, 0), null).isEmpty();
			rearRightCollisionDepth = rearRightGroundDeviceCollided ? getCollisionDepthForGroundDeviceCollisions(rearRightGroundDeviceBox, rearRightCollidingBoxes) : 0;
		}else{
			rearRightGroundDeviceBox = null;
			rearRightGroundDeviceCollided = false;
			rearRightGroundDeviceGrounded = false;
			rearRightCollisionDepth = 0;
		}
	}
	
	/**
	 * Helper function for calculating the collision depth between a box and the boxes that collide with it.
	 * This function is used for ground device collisions only, and makes some assumptions that are incorrect
	 * for a general-purpose function.
	 */
	private double getCollisionDepthForGroundDeviceCollisions(VehicleAxisAlignedBB groundDeviceBox, List<AxisAlignedBB> boxList){
		double collisionDepth = 0;
		for(AxisAlignedBB box : boxList){
			if(groundDeviceBox.minY < box.maxY){
				collisionDepth = Math.max(collisionDepth, box.maxY - groundDeviceBox.minY);
			}
		}
		//Don't return small collisions.  These can be due to floating-point errors in calculations.		
		if(Math.abs(collisionDepth) < 0.0001){
			return 0;
		}else{
			return collisionDepth;
		}
	}
	
	/**
	 *  This needs to be called before checking pitch and roll of ground devices.  It is responsible for ensuring that
	 *  the Y position of the vehicle is in such a place that the checks can run.  If motionY is negative, and opposite
	 *  ground devices are collided it will add motionY to a level that the least-collided ground device will no longer collide.
	 *  If motionY is positive, then it will add motionY to prevent either the front or rear ground devices from colliding, but only
	 *  if motionPitch is non-zero.  This is for vehicles that rotate when flying and need to pivot on their ground devices.
	 *  Finally, if motionY is negative, and only the rear ground devices are collided, but motionPitch is also negative, it will
	 *  add motionY to get those out of the ground.  This is also for flying vehicles, in that they normally pitch down (positive)
	 *  due to gravity, but if they are trying to pitch up we need to allow them to do so.
	 *  
	 *  In all cases, this function will return the amount of motionY boost that was applied.  This value should be removed
	 *  from motionY after the vehicle is moved, as it's not a force and will cause the physics system to behave badly if
	 *  it is left in.  The only exception is if we had motionY that we shouldn't have applied in the first place, such as
	 *  if motionY was moving us into the ground.  In this case, we need to keep this as a force as it's force the ground is
	 *  applying to the vehicle to keep it from moving into the ground (normal force).
	 */
	private double correctMotionYMovement(){
		if(motionY < 0){
			if(motionPitch >= -0.1){
				if((frontLeftGroundDeviceCollided && rearRightGroundDeviceCollided) || (frontRightGroundDeviceCollided && rearLeftGroundDeviceCollided)){
					double collisionDepth = Math.max(Math.min(frontLeftCollisionDepth, rearRightCollisionDepth), Math.min(frontRightCollisionDepth, rearLeftCollisionDepth));
					double motionToNotCollide = collisionDepth/speedFactor;
					motionY += motionToNotCollide;
					//Check if motionY is close to 0.  If so, we should make it 0 as we are
					//just sitting on the ground and shouldn't have any motionY to begin with.
					if(Math.abs(motionY) < 0.0001){
						motionY = 0;
					}
					updateGroundDeviceCollisions();
					return motionY > 0 ? motionY : 0;
				}
			}else{
				if(rearLeftGroundDeviceCollided || rearRightGroundDeviceCollided){
					double collisionDepth = Math.max(rearLeftCollisionDepth, rearRightCollisionDepth);
					double motionToNotCollide = collisionDepth/speedFactor;
					motionY += motionToNotCollide;
					updateGroundDeviceCollisions();
					return motionY;
				}
			}
		}else if(motionY > 0){
			if(motionPitch > 0 && (frontLeftGroundDeviceCollided || frontRightGroundDeviceCollided)){
				double collisionDepth = Math.max(frontLeftCollisionDepth, frontRightCollisionDepth);
				double motionToNotCollide = collisionDepth/speedFactor;
				motionY += motionToNotCollide;
				updateGroundDeviceCollisions();
				return motionY;
			}else if(motionPitch < 0 && (rearLeftGroundDeviceCollided || rearRightGroundDeviceCollided)){
				double collisionDepth = Math.max(rearLeftCollisionDepth, rearRightCollisionDepth);
				double motionToNotCollide = collisionDepth/speedFactor;
				motionY += motionToNotCollide;
				updateGroundDeviceCollisions();
				return motionY;
			}
		}
		return 0;
	}
	
	/**
	 *  If a collision box collided, we need to restrict our proposed movement.
	 *  Do this by removing motions that cause collisions.
	 *  If the motion has a value of 0, skip it as it couldn't have caused the collision.
	 *  Note that even though motionY may have been adjusted for ground device operation prior to this call,
	 *  we shouldn't have an issue with the change as this logic takes priority over that logic to ensure 
	 *  no collision box collides with another block, even if it requires all the ground devices to be collided.
	 */
	private void correctCollidingMovement(){
		motionPitch = 0;
		motionYaw = 0;
		motionRoll = 0;
		
		//First check the X-axis.
		if(motionX != 0){
			for(VehicleAxisAlignedBB box : this.getCurrentCollisionBoxes()){
				float collisionDepth = getCollisionForAxis(box, true, false, false);
				if(collisionDepth == -1){
					return;
				}else{
					if(this.motionX > 0){
						this.motionX = Math.max(motionX - collisionDepth/speedFactor, 0);
					}else if(this.motionX < 0){
						this.motionX = Math.min(motionX + collisionDepth/speedFactor, 0);
					}
				}
			}
		}
		
		//Do the same for the Z-axis
		if(motionZ != 0){
			for(VehicleAxisAlignedBB box : this.getCurrentCollisionBoxes()){
				float collisionDepth = getCollisionForAxis(box, false, false, true);
				if(collisionDepth == -1){
					return;
				}else{
					if(this.motionZ > 0){
						this.motionZ = Math.max(motionZ - collisionDepth/speedFactor, 0);
					}else if(this.motionZ < 0){
						this.motionZ = Math.min(motionZ + collisionDepth/speedFactor, 0);
					}
				}
			}
		}
		
		//Now that the XZ motion has been limited based on collision we can move in the Y.
		if(motionY != 0){
			for(VehicleAxisAlignedBB box : this.getCurrentCollisionBoxes()){
				float collisionDepth = getCollisionForAxis(box, false, true, false);
				if(collisionDepth == -1){
					return;
				}else if(collisionDepth != 0){
					if(this.motionY > 0){
						this.motionY = Math.max(motionY - collisionDepth/speedFactor, 0);
					}else if(this.motionY < 0){
						this.motionY = Math.min(motionY + collisionDepth/speedFactor, 0);
					}
				}
			}
		}
		
		//Check the yaw.
		if(motionYaw != 0){
			for(VehicleAxisAlignedBB box : this.getCurrentCollisionBoxes()){
				while(motionYaw != 0){
					Vec3d offset = RotationSystem.getRotatedPoint(box.rel, rotationPitch, rotationYaw + motionYaw, rotationRoll);
					//Raise this box ever so slightly because Floating Point errors are a PITA.
					VehicleAxisAlignedBB offsetBox = box.getBoxWithOrigin(this.getPositionVector().add(offset).addVector(motionX*speedFactor, motionY*speedFactor + 0.1, motionZ*speedFactor));
					if(getAABBCollisions(offsetBox, null).isEmpty()){
						break;
					}
					if(this.motionYaw > 0){
						this.motionYaw = Math.max(motionYaw - 0.1F, 0);
					}else{
						this.motionYaw = Math.min(motionYaw + 0.1F, 0);
					}
				}
			}
		}

		//Now do pitch.
		//Make sure to take into account yaw as it's already been checked.
		if(motionPitch != 0){
			for(VehicleAxisAlignedBB box : this.getCurrentCollisionBoxes()){
				while(motionPitch != 0){
					Vec3d offset = RotationSystem.getRotatedPoint(box.rel, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll);
					VehicleAxisAlignedBB offsetBox = box.getBoxWithOrigin(this.getPositionVector().add(offset).addVector(motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor));
					if(getAABBCollisions(offsetBox, null).isEmpty()){
						break;
					}
					if(this.motionPitch > 0){
						this.motionPitch = Math.max(motionPitch - 0.1F, 0);
					}else{
						this.motionPitch = Math.min(motionPitch + 0.1F, 0);
					}
				}
			}
		}
		
		//And lastly the roll.
		if(motionRoll != 0){
			for(VehicleAxisAlignedBB box : this.getCurrentCollisionBoxes()){
				while(motionRoll != 0){
					Vec3d offset = RotationSystem.getRotatedPoint(box.rel, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
					VehicleAxisAlignedBB offsetBox = box.getBoxWithOrigin(this.getPositionVector().add(offset).addVector(motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor));
					if(getAABBCollisions(offsetBox, null).isEmpty()){
						break;
					}
					if(this.motionRoll > 0){
						this.motionRoll = Math.max(motionRoll - 0.1F, 0);
					}else{
						this.motionRoll = Math.min(motionRoll + 0.1F, 0);
					}
				}
			}
		}
		
		//As a final precaution, take a bit of extra movement off of the motions.
		//This is done to prevent vehicles from moving into blocks when they shouldn't
		//due to floating-point errors.
		if(motionX > 0){
			motionX -= 0.002F;
		}
		if(motionX < 0){
			motionX += 0.002F;
		}
		if(motionZ > 0){
			motionZ -= 0.002F;
		}
		if(motionZ < 0){
			motionZ += 0.002F;
		}
	}

	/**
	 *  Called to adjust the pitch of the vehicle to handle collided ground devices.
	 *  This adds to motionPitch to the vehicle, as well as returns a value to compensate for the motionPitch added.
	 *  This value should be added to motionY when applying vehicle motion, but should not be left in as it's
	 *  not a force and will cause the physics system to mis-behave.
	 *  
	 *  This system works by checking which ground devices are collided (if any) and rotating the vehicle accordingly.
	 *  Note that the yCoord for the boxes is their center, so we need to take half the height to get the
	 *  collision point at the ground for all ground devices.  We limit rotation in all cases to
	 *  2 degrees per tick, which is 40 degrees a second.  Plenty fast, and prevents vehicles from
	 *  instantly pitching up on steep slopes.  This isn't Big Rigs-Over the Road Racing...
	 */
	private double correctPitchMovement(){
		//If we only have front ground devices collided, we need to pitch up.
		//If we only have rear ground devices collided, we need to pitch down.
		//In either case, we will have to rotate the vehicle and move it in the Y-direction.
		//This is to ensure it follows the ground after pitching rather than pitching in the air.
		double ptichRotationBoost = 0;
		if((frontLeftCollisionDepth > 0 || frontRightCollisionDepth > 0) && rearLeftCollisionDepth == 0 && rearRightCollisionDepth == 0){				
			//First, we get the front point that has collided.
			//This is needed no matter if the rears are grounded or not.
			double frontY;
			double frontZ;
			double collisionDepth;
			if(frontLeftCollisionDepth > frontRightCollisionDepth){
				frontY = frontLeftGroundDeviceBox.rel.y - frontLeftGroundDeviceBox.height/2D;
				frontZ = frontLeftGroundDeviceBox.rel.z;
				collisionDepth = frontLeftCollisionDepth;
			}else{
				frontY = frontRightGroundDeviceBox.rel.y - frontRightGroundDeviceBox.height/2D;
				frontZ = frontRightGroundDeviceBox.rel.z;
				collisionDepth = frontRightCollisionDepth;
			}
			
			if(rearLeftGroundDeviceGrounded || rearRightGroundDeviceGrounded){
				//Get the farthest-back grounded rear point for the greatest angle.
				double rearY;
				double rearZ;
				if(rearLeftGroundDeviceGrounded && rearRightGroundDeviceGrounded){
					if(rearLeftGroundDeviceBox.rel.z < rearRightGroundDeviceBox.rel.z){
						rearY = rearLeftGroundDeviceBox.rel.y - rearLeftGroundDeviceBox.height/2D;
						rearZ = rearLeftGroundDeviceBox.rel.z;
					}else{
						rearY = rearRightGroundDeviceBox.rel.y - rearRightGroundDeviceBox.height/2D;
						rearZ = rearRightGroundDeviceBox.rel.z;
					}
				}else if(rearLeftGroundDeviceGrounded){
					rearY = rearLeftGroundDeviceBox.rel.y - rearLeftGroundDeviceBox.height/2D;
					rearZ = rearLeftGroundDeviceBox.rel.z;
				}else{
					rearY = rearRightGroundDeviceBox.rel.y - rearRightGroundDeviceBox.height/2D;
					rearZ = rearRightGroundDeviceBox.rel.z;
				}

				//Finally, get the distance between the two points and the angle needed to get out of the collision.
				//After that, calculate how much we will need to offset the vehicle to keep the rear in the same place.
				double distance = Math.hypot(frontY - rearY, frontZ - rearZ);
				double angle = -Math.min(Math.asin(collisionDepth/distance), maxRotationInRadPerTick);
				motionPitch += Math.toDegrees(angle);
				ptichRotationBoost = -Math.sin(angle)*Math.hypot(rearY, rearZ);
			}else{
				//In this case, we are just trying to get to a point where we have a grounded ground device.
				//This will allow us to rotate about it and level the vehicle.
				//We just rotate as much as we can here, and if we have negative motionY we need to set it to 0
				//after this cycle.  This is because we don't want to go down any further than we are until we can
				//do calcs using the grounded ground device.  Use the collision variable we use for the motionY
				//as it won't be used at this point as one of the sets of ground devices are free.  If it was used
				//because we were in the ground then one set would have to be grounded.
				double angle = -maxRotationInRadPerTick;
				motionPitch += Math.toDegrees(angle);
				if(motionY < 0){
					ptichRotationBoost = Math.sin(angle)*Math.hypot(frontY, frontZ) + motionY*speedFactor;
					motionY = 0;
				}else{
					ptichRotationBoost = Math.sin(angle)*Math.hypot(frontY, frontZ);
				}
			}
		}else if((rearLeftCollisionDepth > 0 || rearRightCollisionDepth > 0) && frontLeftCollisionDepth == 0 && frontRightCollisionDepth == 0){				
			//First, we get the rear point that has collided.
			//This is needed no matter if the fronts are grounded or not.
			double rearY;
			double rearZ;
			double collisionDepth;
			if(rearLeftCollisionDepth > rearRightCollisionDepth){
				rearY = rearLeftGroundDeviceBox.rel.y - rearLeftGroundDeviceBox.height/2D;
				rearZ = rearLeftGroundDeviceBox.rel.z;
				collisionDepth = rearLeftCollisionDepth;
			}else{
				rearY = rearRightGroundDeviceBox.rel.y - rearRightGroundDeviceBox.height/2D;
				rearZ = rearRightGroundDeviceBox.rel.z;
				collisionDepth = rearRightCollisionDepth;
			}
			
			if(frontLeftGroundDeviceGrounded || frontRightGroundDeviceGrounded){
				//Get the farthest-forward grounded front point for the greatest angle.
				double frontY;
				double frontZ;
				if(frontLeftGroundDeviceGrounded && frontRightGroundDeviceGrounded){
					if(frontLeftGroundDeviceBox.rel.z > frontRightGroundDeviceBox.rel.z){
						frontY = frontLeftGroundDeviceBox.rel.y - frontLeftGroundDeviceBox.height/2D;
						frontZ = frontLeftGroundDeviceBox.rel.z;
					}else{
						frontY = frontRightGroundDeviceBox.rel.y - frontRightGroundDeviceBox.height/2D;
						frontZ = frontRightGroundDeviceBox.rel.z;
					}
				}else if(frontLeftGroundDeviceGrounded){
					frontY = frontLeftGroundDeviceBox.rel.y - frontLeftGroundDeviceBox.height/2D;
					frontZ = frontLeftGroundDeviceBox.rel.z;
				}else{
					frontY = frontRightGroundDeviceBox.rel.y - frontRightGroundDeviceBox.height/2D;
					frontZ = frontRightGroundDeviceBox.rel.z;
				}

				//Finally, get the distance between the two points and the angle needed to get out of the collision.
				//After that, calculate how much we will need to offset the vehicle to keep the front in the same place.
				double distance = Math.hypot(frontY - rearY, frontZ - rearZ);
				double angle = Math.min(Math.asin(collisionDepth/distance), maxRotationInRadPerTick);
				motionPitch += Math.toDegrees(angle);
				ptichRotationBoost = Math.sin(angle)*Math.hypot(frontY, frontZ);
			}else{
				//In this case, we are just trying to get to a point where we have a grounded ground device.
				//This will allow us to rotate about it and level the vehicle.
				//We just rotate as much as we can here, and if we have negative motionY we need to set it to 0
				//after this cycle.  This is because we don't want to go down any further than we are until we can
				//do calcs using the grounded ground device.  Use the collision variable we use for the motionY
				//as it won't be used at this point as one of the sets of ground devices are free.  If it was used
				//because we were in the ground then one set would have to be grounded.
				double angle = maxRotationInRadPerTick;
				motionPitch += Math.toDegrees(angle);
				if(motionY < 0){
					ptichRotationBoost = -Math.sin(angle)*Math.hypot(rearY, rearZ) + motionY*speedFactor;
					motionY = 0;
				}else{
					ptichRotationBoost = -Math.sin(angle)*Math.hypot(rearY, rearZ);
				}
			}
		}
		return ptichRotationBoost;
	}

	/**
	 *  Called to adjust the roll of the vehicle to handle collided ground devices.
	 *  Same as correctPitchMovment, just for roll using the X axis rather than Z.
	 */
	private double correctRollMovement(){
		//Negative rolls left, postive rolls right.
		//If we only have left ground devices collided, we need to roll left.
		//If we only have right ground devices collided, we need to roll right.
		//In either case, we will have to rotate the vehicle and move it in the Y-direction.
		//This is to ensure it follows the ground after rolling rather than rolling in the air.
		double rollRotationBoost = 0;
		if((frontRightCollisionDepth > 0 || rearRightCollisionDepth > 0) && frontLeftCollisionDepth == 0 && rearLeftCollisionDepth == 0){				
			//Negative "pitch" added here, so we need to have an operation that adds roll to the left.
			//This is if we have the right side collided.
			//Swap front with right and rear with left.
			//First, we get the right point that has collided.
			//This is needed no matter if the lefts are grounded or not.
			double rightY;
			double rightX;
			double collisionDepth;
			if(frontRightCollisionDepth > rearRightCollisionDepth){
				rightY = frontRightGroundDeviceBox.rel.y - frontRightGroundDeviceBox.height/2D;
				rightX = frontRightGroundDeviceBox.rel.x;
				collisionDepth = frontRightCollisionDepth;
			}else{
				rightY = rearRightGroundDeviceBox.rel.y - rearRightGroundDeviceBox.height/2D;
				rightX = rearRightGroundDeviceBox.rel.x;
				collisionDepth = rearRightCollisionDepth;
			}
			
			if(frontLeftGroundDeviceGrounded || rearLeftGroundDeviceGrounded){
				//Get the farthest-left grounded left point for the greatest angle.
				double leftY;
				double leftX;
				if(frontLeftGroundDeviceGrounded && rearLeftGroundDeviceGrounded){
					if(frontLeftGroundDeviceBox.rel.x < rearLeftGroundDeviceBox.rel.x){
						leftY = frontLeftGroundDeviceBox.rel.y - frontLeftGroundDeviceBox.height/2D;
						leftX = frontLeftGroundDeviceBox.rel.x;
					}else{
						leftY = rearLeftGroundDeviceBox.rel.y - rearLeftGroundDeviceBox.height/2D;
						leftX = rearLeftGroundDeviceBox.rel.x;
					}
				}else if(frontLeftGroundDeviceGrounded){
					leftY = frontLeftGroundDeviceBox.rel.y - frontLeftGroundDeviceBox.height/2D;
					leftX = frontLeftGroundDeviceBox.rel.x;
				}else{
					leftY = rearLeftGroundDeviceBox.rel.y - rearLeftGroundDeviceBox.height/2D;
					leftX = rearLeftGroundDeviceBox.rel.x;
				}

				//Finally, get the distance between the two points and the angle needed to get out of the collision.
				//After that, calculate how much we will need to offset the vehicle to keep the rear in the same place.
				double distance = Math.hypot(rightY - leftY, rightX - leftX);
				double angle = -Math.min(Math.asin(collisionDepth/distance), maxRotationInRadPerTick);
				motionRoll += Math.toDegrees(angle);
				rollRotationBoost = -Math.sin(angle)*Math.hypot(leftY, leftX);
			}else{
				//In this case, we are just trying to get to a point where we have a grounded ground device.
				//This will allow us to rotate about it and level the vehicle.
				//We just rotate as much as we can here, and if we have negative motionY we need to set it to 0
				//after this cycle.  This is because we don't want to go down any further than we are until we can
				//do calcs using the grounded ground device.  Use the collision variable we use for the motionY
				//as it won't be used at this point as one of the sets of ground devices are free.  If it was used
				//because we were in the ground then one set would have to be grounded.
				double angle = -maxRotationInRadPerTick;
				motionRoll+= Math.toDegrees(angle);
				if(motionY < 0){
					rollRotationBoost = Math.sin(angle)*Math.hypot(rightY, rightX) + motionY*speedFactor;
					motionY = 0;
				}else{
					rollRotationBoost = Math.sin(angle)*Math.hypot(rightY, rightX);
				}
			}
		}else if((frontLeftCollisionDepth > 0 || rearLeftCollisionDepth > 0) && frontRightCollisionDepth == 0 && rearRightCollisionDepth == 0){				
			//First, we get the left point that has collided.
			//This is needed no matter if the rights are grounded or not.
			double leftY;
			double leftX;
			double collisionDepth;
			if(frontLeftCollisionDepth > rearLeftCollisionDepth){
				leftY = frontLeftGroundDeviceBox.rel.y - frontLeftGroundDeviceBox.height/2D;
				leftX = frontLeftGroundDeviceBox.rel.x;
				collisionDepth = frontLeftCollisionDepth;
			}else{
				leftY = rearLeftGroundDeviceBox.rel.y - rearLeftGroundDeviceBox.height/2D;
				leftX = rearLeftGroundDeviceBox.rel.x;
				collisionDepth = rearLeftCollisionDepth;
			}
			
			if(frontRightGroundDeviceGrounded || rearRightGroundDeviceGrounded){
				//Get the farthest-right grounded front point for the greatest angle.
				double rightY;
				double rightX;
				if(frontRightGroundDeviceGrounded && rearRightGroundDeviceGrounded){
					if(frontRightGroundDeviceBox.rel.x > rearRightGroundDeviceBox.rel.x){
						rightY = frontRightGroundDeviceBox.rel.y - frontRightGroundDeviceBox.height/2D;
						rightX = frontRightGroundDeviceBox.rel.x;
					}else{
						rightY = rearRightGroundDeviceBox.rel.y - rearRightGroundDeviceBox.height/2D;
						rightX = rearRightGroundDeviceBox.rel.x;
					}
				}else if(frontRightGroundDeviceGrounded){
					rightY = frontRightGroundDeviceBox.rel.y - frontRightGroundDeviceBox.height/2D;
					rightX = frontRightGroundDeviceBox.rel.x;
				}else{
					rightY = rearRightGroundDeviceBox.rel.y - rearRightGroundDeviceBox.height/2D;
					rightX = rearRightGroundDeviceBox.rel.x;
				}

				//Finally, get the distance between the two points and the angle needed to get out of the collision.
				//After that, calculate how much we will need to offset the vehicle to keep the right in the same place.
				double distance = Math.hypot(rightY - leftY, rightX - leftX);
				double angle = Math.min(Math.asin(collisionDepth/distance), maxRotationInRadPerTick);
				motionRoll += Math.toDegrees(angle);
				rollRotationBoost = Math.sin(angle)*Math.hypot(rightY, rightX);
			}else{
				//In this case, we are just trying to get to a point where we have a grounded ground device.
				//This will allow us to rotate about it and level the vehicle.
				//We just rotate as much as we can here, and if we have negative motionY we need to set it to 0
				//after this cycle.  This is because we don't want to go down any further than we are until we can
				//do calcs using the grounded ground device.  Use the collision variable we use for the motionY
				//as it won't be used at this point as one of the sets of ground devices are free.  If it was used
				//because we were in the ground then one set would have to be grounded.
				double angle = maxRotationInRadPerTick;
				motionRoll += Math.toDegrees(angle);
				if(motionY < 0){
					rollRotationBoost = -Math.sin(angle)*Math.hypot(leftY, leftX) + motionY*speedFactor;
					motionY = 0;
				}else{
					rollRotationBoost = -Math.sin(angle)*Math.hypot(leftY, leftX);
				}
			}
		}
		return rollRotationBoost;
	}

	
	/**
	 * Call this when moving vehicle to ensure they move correctly.
	 * Failure to do this will result in things going badly!
	 */
	private void moveVehicle(){
		//First populate the variables for ground and collided states for the groundDevices.
		updateGroundDeviceCollisions();
		
		//Now check to make sure our ground devices are in the right spot relative to the ground.
		double groundCollisionBoost = correctMotionYMovement();
		
		//After checking the ground devices to ensure we aren't shoving ourselves into the ground, we try to move the vehicle.
		//If the vehicle can move without a collision box colliding with something, then we can move to the re-positioning of the vehicle.
		//That is done through trig functions.  If we hit something, however, we need to inhibit the movement so we don't do that.
		boolean collisionBoxCollided = false;
		for(VehicleAxisAlignedBB box : this.getCurrentCollisionBoxes()){
			Vec3d offset = RotationSystem.getRotatedPoint(box.rel, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
			VehicleAxisAlignedBB offsetBox = box.getBoxWithOrigin(this.getPositionVector().add(offset).addVector(motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor));
			List<AxisAlignedBB> collisionBoxes = getAABBCollisions(offsetBox, null);
			if(!collisionBoxes.isEmpty()){
				collisionBoxCollided = true;
				break;
			}
		}
		
		//Handle collision box collisions, if we have any.
		//This is done after the ground device logic, as that removes any excess motionY caused by gravity.
		//The gravity could cause more collisions here that really don't need to happen, and we don't want
		//to check those as the checks for collision box collision are far more intensive than the ones for ground devices.
		//If we have collisions, handle those rather than do ground device operations, otherwise do ground device operations as normal.
		double groundRotationBoost = 0;
		if(collisionBoxCollided){
			correctCollidingMovement();
		}else{
			//If we have pitch corrections boost on those.  Otherwise boost on roll corrections.
			//We don't want to do both pitch and roll the same tick as they could conflict with each other.
			double pitchRotationBoost = correctPitchMovement();
			if(pitchRotationBoost != 0){
				groundRotationBoost = pitchRotationBoost;
				motionY += groundRotationBoost/speedFactor;
			}else{
				groundRotationBoost = correctRollMovement();
				motionY += groundRotationBoost/speedFactor;
			}
		}

		//Now that that the movement has been checked, move the vehicle.
		prevRotationRoll = rotationRoll;
		if(!world.isRemote){
			if(motionX != 0 || motionY != 0 || motionZ != 0 || motionPitch != 0 || motionYaw != 0 || motionRoll != 0){
				rotationYaw += motionYaw;
				rotationPitch += motionPitch;
				rotationRoll += motionRoll;
				setPosition(posX + motionX*speedFactor, posY + motionY*speedFactor, posZ + motionZ*speedFactor);
				addToServerDeltas(motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor, motionYaw, motionPitch, motionRoll);
				MTS.MTSNet.sendToAll(new PacketVehicleDeltas(this, motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor, motionYaw, motionPitch, motionRoll));
			}
		}else{
			//Make sure the server is sending delta packets and NBT is initialized before we try to do delta correction.
			if(!(serverDeltaX == 0 && serverDeltaY == 0 && serverDeltaZ == 0)){
				//Check to make sure the delta is non-zero before trying to do complex math to calculate it.
				//Saves a bit of CPU power due to division and multiplication operations, and prevents constant
				//movement due to floating-point errors.
				final double deltaX;
				if(serverDeltaX - clientDeltaX != 0){
					deltaX = motionX*speedFactor + (serverDeltaX - clientDeltaX)/25D*Math.abs(serverDeltaX - clientDeltaX);
				}else{
					deltaX = 0;
				}
				
				final double deltaY; 
				if(serverDeltaY - clientDeltaY !=  0){
					deltaY = motionY*speedFactor + (serverDeltaY - clientDeltaY)/25D*Math.abs(serverDeltaY - clientDeltaY);
				}else{
					deltaY = 0;
				}
				
				final double deltaZ; 
				if(serverDeltaZ - clientDeltaZ !=  0){
					deltaZ = motionZ*speedFactor + (serverDeltaZ - clientDeltaZ)/25D*Math.abs(serverDeltaZ - clientDeltaZ);
				}else{
					deltaZ = 0;
				}
				
				final float deltaYaw; 
				if(serverDeltaYaw - clientDeltaYaw != 0){
					deltaYaw = motionYaw + (serverDeltaYaw - clientDeltaYaw)/25F*Math.abs(serverDeltaYaw - clientDeltaYaw);
				}else{
					deltaYaw = 0;
				}
				
				final float deltaPitch; 
				if(serverDeltaPitch - clientDeltaPitch != 0){
					deltaPitch = motionPitch + (serverDeltaPitch - clientDeltaPitch)/25F*Math.abs(serverDeltaPitch - clientDeltaPitch);
				}else{
					deltaPitch = 0;
				}
				
				final float deltaRoll; 
				if(serverDeltaRoll - clientDeltaRoll != 0){
					deltaRoll = motionRoll + (serverDeltaRoll - clientDeltaRoll)/25F*Math.abs(serverDeltaRoll - clientDeltaRoll);
				}else{
					deltaRoll = 0;
				}

				setPosition(posX + deltaX, posY + deltaY, posZ + deltaZ);
				rotationYaw += deltaYaw;
				rotationPitch += deltaPitch;
				rotationRoll += deltaRoll;
				addToClientDeltas(deltaX, deltaY, deltaZ, deltaYaw, deltaPitch, deltaRoll);
			}else{
				rotationYaw += motionYaw;
				rotationPitch += motionPitch;
				rotationRoll += motionRoll;
				setPosition(posX + motionX*speedFactor, posY + motionY*speedFactor, posZ + motionZ*speedFactor);
			}
		}
		
		//After all movement is done, try and move players on hitboxes.
		//Note that we need to interpolate the delta here based on actual movement, so don't use motionX!
		if(this.velocity != 0){
			for(EntityPlayer player : world.playerEntities){
				for(VehicleAxisAlignedBB box : this.getCurrentCollisionBoxes()){
					//Add a slight yOffset to every box to "grab" players standing on collision points.
					if(box.offset(this.posX - this.prevPosX, this.posY - this.prevPosY + 0.1F, this.posZ - this.prevPosZ).intersects(player.getEntityBoundingBox())){
						//Player has collided with this vehicle.  Adjust movement to allow them to ride on it.
						//If we are going too fast, the player should slip off the collision box if it's not an interior box.
						if(Math.abs(this.velocity) <= clingSpeed || box.isInterior){
							player.setPosition(player.posX + (this.posX - this.prevPosX), player.posY + (this.posY - this.prevPosY), player.posZ + (this.posZ - this.prevPosZ));
						}else if(Math.abs(this.velocity) < 2F*clingSpeed){
							double slip = (2F*clingSpeed - Math.abs(this.velocity))*4D;
							player.setPosition(player.posX + (this.posX - this.prevPosX)*slip, player.posY + (this.posY - this.prevPosY)*slip, player.posZ + (this.posZ - this.prevPosZ)*slip);
						}
						break;
					}
				}
			}
		}
		
		//Before we end this tick we need to remove any motions added for ground devices.  These motions are required 
		//only for the updating of the vehicle position due to rotation operations and should not be considered forces.
		//Leaving them in will cause the physics system to think a force was applied, which will make it behave badly!
		//We need to strip away any positive motionY we gave the vehicle to get it out of the ground if it
		//collided on its ground devices, as well as any motionY we added when doing rotation adjustments.
		motionY -= (groundCollisionBoost + groundRotationBoost/speedFactor);
	}
	
	
	/**
	 * Returns factor for braking.
	 * Depends on number of grounded core collision sections and braking ground devices.
	 */
	protected float getBrakingForceFactor(){
		float brakingFactor = 0;
		//First get the ground device braking contributions.
		for(APartGroundDevice groundDevice : this.groundedGroundDevices){
			float addedFactor = 0;
			if(brakeOn || parkingBrakeOn){
				addedFactor = groundDevice.getMotiveFriction();
			}
			if(addedFactor != 0){
				brakingFactor += Math.max(addedFactor - groundDevice.getFrictionLoss(), 0);
			}
		}
		
		//Now get any contributions from the colliding collision bits.
		for(VehicleAxisAlignedBB box : this.getCurrentCollisionBoxes()){
			if(!world.getCollisionBoxes(null, box.offset(0, -0.05F, 0)).isEmpty()){
				//0.6 is default slipperiness for blocks.  Anything extra should reduce friction, anything less should increase it.
				BlockPos pos = new BlockPos(box.pos.addVector(0, -1, 0));
				float frictionLoss = 0.6F - world.getBlockState(pos).getBlock().getSlipperiness(world.getBlockState(pos), world, pos, null);
				brakingFactor += Math.max(2.0 - frictionLoss, 0);
			}
		}
		return brakingFactor;
	}
	
	/**
	 * Returns factor for skidding based on lateral friction and velocity.
	 * If the value is non-zero, it indicates that yaw should be restricted
	 * due to ground devices being in contact with the ground.
	 * Note that this should be called prior to turning code as it will interpret
	 * the yaw change as a skid and will attempt to prevent it!
	 */
	protected float getSkiddingFactor(){
		float skiddingFactor = 0;
		for(APartGroundDevice groundDevice : this.groundedGroundDevices){
			skiddingFactor += groundDevice.getLateralFriction() - groundDevice.getFrictionLoss();
		}
		return skiddingFactor > 0 ? skiddingFactor : 0;
	}
	
	/**
	 * Returns factor for turning based on lateral friction, velocity, and wheel distance.
	 * Sign of returned value indicates which direction entity should yaw.
	 * A 0 value indicates no yaw change.
	 */
	protected float getTurningFactor(){
		float turningForce = 0;
		float steeringAngle = this.getSteerAngle();
		if(steeringAngle != 0){
			float turningFactor = 0;
			float turningDistance = 0;
			for(APartGroundDevice groundDevice : this.groundedGroundDevices){
				float frictionLoss = groundDevice.getFrictionLoss();
				//Do we have enough friction to change yaw?
				if(groundDevice.turnsWithSteer && groundDevice.getLateralFriction() - frictionLoss > 0){
					turningFactor += groundDevice.getLateralFriction() - frictionLoss;
					turningDistance = (float) Math.max(turningDistance, Math.abs(groundDevice.offset.z));
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
				if(Math.abs(velocity*speedFactor/0.35F) - turningFactor/3F > 0){
					steeringAngle *= Math.pow(0.25F, (Math.abs(velocity*(0.75F + speedFactor/0.35F/4F)) - turningFactor/3F));
				}
				//Adjust turn force to steer angle based on turning factor.
				turningForce = -(float) (steeringAngle*velocity/2F);
				//Correct for speedFactor changes.
				turningForce *= speedFactor/0.35F;
				//Now add the sign to this force.
				turningForce *= Math.signum(this.getSteerAngle());
			}
		}
		return turningForce;
	}
	
	protected void reAdjustGroundSpeed(double groundSpeed){
		Vec3d groundVec = new Vec3d(headingVec.x, 0, headingVec.z).normalize();
		motionX = groundVec.x * groundSpeed;
		motionZ = groundVec.z * groundSpeed;
	}
	
	private void addToClientDeltas(double dX, double dY, double dZ, float dYaw, float dPitch, float dRoll){
		this.clientDeltaX += dX;
		this.clientDeltaY += dY;
		this.clientDeltaZ += dZ;
		this.clientDeltaYaw += dYaw;
		this.clientDeltaPitch += dPitch;
		this.clientDeltaRoll += dRoll;
	}
	
	public void addToServerDeltas(double dX, double dY, double dZ, float dYaw, float dPitch, float dRoll){
		this.serverDeltaX += dX;
		this.serverDeltaY += dY;
		this.serverDeltaZ += dZ;
		this.serverDeltaYaw += dYaw;
		this.serverDeltaPitch += dPitch;
		this.serverDeltaRoll += dRoll;
	}
	
	/**
	 * Method block for force and motion calculations.
	 */
	protected abstract void getForcesAndMotions();
	
	/**
	 * Method block for ground operations.
	 * Must come AFTER force calculations as it depends on motions.
	 */
	protected abstract void performGroundOperations();
	
	/**
	 * Method block for dampening control surfaces.
	 * Used to move control surfaces back to neutral position.
	 */
	protected abstract void dampenControlSurfaces();
	
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.parkingBrakeOn=tagCompound.getBoolean("parkingBrakeOn");
		this.brakeOn=tagCompound.getBoolean("brakeOn");
		
		this.serverDeltaX=tagCompound.getDouble("serverDeltaX");
		this.serverDeltaY=tagCompound.getDouble("serverDeltaY");
		this.serverDeltaZ=tagCompound.getDouble("serverDeltaZ");
		this.serverDeltaYaw=tagCompound.getFloat("serverDeltaYaw");
		this.serverDeltaPitch=tagCompound.getFloat("serverDeltaPitch");
		this.serverDeltaRoll=tagCompound.getFloat("serverDeltaRoll");
		
		if(world.isRemote){
			this.clientDeltaX = this.serverDeltaX;
			this.clientDeltaY = this.serverDeltaY;
			this.clientDeltaZ = this.serverDeltaZ;
			this.clientDeltaYaw = this.serverDeltaYaw;
			this.clientDeltaPitch = this.serverDeltaPitch;
			this.clientDeltaRoll = this.serverDeltaRoll;
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("brakeOn", this.brakeOn);
		tagCompound.setBoolean("parkingBrakeOn", this.parkingBrakeOn);
		
		tagCompound.setDouble("serverDeltaX", this.serverDeltaX);
		tagCompound.setDouble("serverDeltaY", this.serverDeltaY);
		tagCompound.setDouble("serverDeltaZ", this.serverDeltaZ);
		tagCompound.setFloat("serverDeltaYaw", this.serverDeltaYaw);
		tagCompound.setFloat("serverDeltaPitch", this.serverDeltaPitch);
		tagCompound.setFloat("serverDeltaRoll", this.serverDeltaRoll);
		return tagCompound;
	}
}
