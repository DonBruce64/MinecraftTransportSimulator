package minecrafttransportsimulator.multipart.main;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MultipartAxisAlignedBB;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.multipart.parts.PartGroundDevice;
import minecrafttransportsimulator.packets.multipart.PacketMultipartDeltas;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**At the final basic multipart level we add in the functionality for state-based movement.
 * Here is where the functions for moving permissions, such as collision detection
 * routines and ground device effects come in.  We also add functionality to keep
 * servers and clients from de-syncing, and a few basic variables that will be commmon for all multiparts.
 * At this point we now have a basic multipart that can be manipulated for movement in the world.  
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartD_Moving extends EntityMultipartC_Colliding{
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
	public final List<PartGroundDevice> groundedGroundDevices = new ArrayList<PartGroundDevice>();
			
	public EntityMultipartD_Moving(World world){
		super(world);
	}
	
	public EntityMultipartD_Moving(World world, float posX, float posY, float posZ, float playerRotation, String multipartName){
		super(world, posX, posY, posZ, playerRotation, multipartName);
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(pack != null){
			getForcesAndMotions();
			performGroundOperations();
			moveMultipart();
			if(!worldObj.isRemote){
				dampenControlSurfaces();
			}
			
			//Populate the ground device list for use in the methods here.
			groundedGroundDevices.clear();
			for(APart part : this.getMultipartParts()){
				if(part instanceof PartGroundDevice){
					if(((PartGroundDevice) part).isOnGround()){
						groundedGroundDevices.add((PartGroundDevice) part);
					}
				}
			}
			
			//Finally, update parts.
			for(APart part : this.getMultipartParts()){
				part.updatePart();
			}
		}
	}

	/**
	 * Call this when moving multiparts to ensure they move correctly.
	 * Failure to do this will result in things going badly!
	 */
	private void moveMultipart(){
		//First check planned movement.
		boolean needCheck = false;
		boolean groundDeviceNeedsLifting = false;
		double originalMotionY = motionY;
		
		//First try to add the current motion and see if we need to check anything.
		for(MultipartAxisAlignedBB box : this.getCurrentCollisionBoxes()){
			Vec3d offset = RotationSystem.getRotatedPoint(box.rel, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
			MultipartAxisAlignedBB offsetBox = box.getBoxWithOrigin(this.getPositionVector().add(offset).addVector(motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor));
			if(!getAABBCollisions(offsetBox, groundDeviceCollisionBoxMap.get(box), null).isEmpty()){
				needCheck = true;
			}
		}
	
		//If anything was collided we need to adjust movement here.
		//Otherwise we can just let the multipart move like normal.
		if(needCheck){
			//The first thing we need to do is see the depth of the collision in the XZ plane.
			//If minor, we can stop movement or move up (if a ground device is collided).

			//First check the X-axis.
			for(MultipartAxisAlignedBB box : this.getCurrentCollisionBoxes()){
				float collisionDepth = getCollisionForAxis(box, true, false, false, groundDeviceCollisionBoxMap.get(box));
				if(collisionDepth < 0){
					if(collisionDepth != -2){
						if(collisionDepth == -3){
							groundDeviceNeedsLifting = true;
						}
						continue;
					}else{
						return;
					}
				}else{
					if(this.motionX > 0){
						this.motionX = Math.max(motionX - collisionDepth/speedFactor, 0);
					}else if(this.motionX < 0){
						this.motionX = Math.min(motionX + collisionDepth/speedFactor, 0);
					}
				}
			}
			
			//Do the same for the Z-axis
			for(MultipartAxisAlignedBB box : this.getCurrentCollisionBoxes()){
				float collisionDepth = getCollisionForAxis(box, false, false, true, groundDeviceCollisionBoxMap.get(box));
				if(collisionDepth < 0){
					if(collisionDepth != -2){
						if(collisionDepth == -3){
							groundDeviceNeedsLifting = true;
						}
						continue;
					}else{
						return;
					}
				}else{
					if(this.motionZ > 0){
						this.motionZ = Math.max(motionZ - collisionDepth/speedFactor, 0);
					}else if(this.motionZ < 0){
						this.motionZ = Math.min(motionZ + collisionDepth/speedFactor, 0);
					}
				}
			}
			
			//Now that the XZ motion has been limited based on collision we can move in the Y.
			for(MultipartAxisAlignedBB box : this.getCurrentCollisionBoxes()){
				float collisionDepth = getCollisionForAxis(box, false, true, false, groundDeviceCollisionBoxMap.get(box));
				if(collisionDepth < 0){
					if(collisionDepth != -2){
						continue;
					}else{
						return;
					}
				}else if(collisionDepth != 0){
					if(this.motionY > 0){
						this.motionY = Math.max(motionY - collisionDepth/speedFactor, 0);
					}else if(this.motionY < 0){
						this.motionY = Math.min(motionY + collisionDepth/speedFactor, 0);
					}
				}
			}			
			
			//Check the yaw.
			for(MultipartAxisAlignedBB box : this.getCurrentCollisionBoxes()){
				while(motionYaw != 0){
					Vec3d offset = RotationSystem.getRotatedPoint(box.rel, rotationPitch, rotationYaw + motionYaw, rotationRoll);
					//Raise this box ever so slightly because Floating Point errors are a PITA.
					MultipartAxisAlignedBB offsetBox = box.getBoxWithOrigin(this.getPositionVector().add(offset).addVector(motionX*speedFactor, motionY*speedFactor + 0.1, motionZ*speedFactor));
					if(getAABBCollisions(offsetBox, groundDeviceCollisionBoxMap.get(box), null).isEmpty()){
						break;
					}
					if(this.motionYaw > 0){
						this.motionYaw = Math.max(motionYaw - 0.1F, 0);
					}else{
						this.motionYaw = Math.min(motionYaw + 0.1F, 0);
					}
				}
			}

			//Now do pitch.
			//Make sure to take into account yaw as it's already been checked.
			//Note that pitch is special in that it can add a slight Y to multiparts if the multipart is
			//trying to pitch up and rear ground devices are blocking it.  This needed to allow vehicles to
			//rotate on their ground devices.
			for(MultipartAxisAlignedBB box : this.getCurrentCollisionBoxes()){
				while(motionPitch != 0){
					Vec3d offset = RotationSystem.getRotatedPoint(box.rel, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll);
					MultipartAxisAlignedBB offsetBox = box.getBoxWithOrigin(this.getPositionVector().add(offset).addVector(motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor));
					if(getAABBCollisions(offsetBox, groundDeviceCollisionBoxMap.get(box), null).isEmpty()){
						break;
					}else if((motionPitch < 0 && box.rel.zCoord <= 0) || (motionPitch > 0 && rotationPitch < -10 && box.rel.zCoord > 0 && originalMotionY > 0)){
						float yBoost = 0;
						for(AxisAlignedBB box2 : getAABBCollisions(offsetBox, groundDeviceCollisionBoxMap.get(box), null)){
							if(box2.maxY > offsetBox.minY + yBoost){
								yBoost += (box2.maxY - offsetBox.minY);
							}
						}
						//Clamp the boost relative to the speed of the vehicle.
						//Otherwise things get bouncy.
						yBoost = (float) Math.min(Math.min(this.velocity, Math.abs(motionPitch)), yBoost/speedFactor);
						motionY += yBoost;
						originalMotionY += yBoost;
						break;
					}
					if(this.motionPitch > 0){
						this.motionPitch = Math.max(motionPitch - 0.1F, 0);
					}else{
						this.motionPitch = Math.min(motionPitch + 0.1F, 0);
					}
				}
			}
			
			//And lastly the roll.
			for(MultipartAxisAlignedBB box : this.getCurrentCollisionBoxes()){
				while(motionRoll != 0){
					Vec3d offset = RotationSystem.getRotatedPoint(box.rel, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
					MultipartAxisAlignedBB offsetBox = box.getBoxWithOrigin(this.getPositionVector().add(offset).addVector(motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor));
					if(getAABBCollisions(offsetBox, groundDeviceCollisionBoxMap.get(box), null).isEmpty()){
						break;
					}
					if(this.motionRoll > 0){
						this.motionRoll = Math.max(motionRoll - 0.1F, 0);
					}else{
						this.motionRoll = Math.min(motionRoll + 0.1F, 0);
					}
				}
			}
			
			//Now everything has been checked and clamped.  If we had a colliding ground device
			//during the XZ movement that could move up to get out of the way increase the Y
			//of this multipart to do so.
			if(groundDeviceNeedsLifting && motionY <= 0.15F){
				motionY = 0.15F;
			}
			
			if(originalMotionY != motionY && originalMotionY < 0){
				//Even if we didn't collide any ground devices, we still may need to adjust pitch/roll.
				//In this case it's determined by what is on the ground and the 
				//original motionY.  If we are supposed to go down by gravity, and can't because the
				//multipart is crooked, we need to fix this.
				
				//To do this we check which boxes are colliding in which motion groups and add pitch/roll.
				//Do this until we are able to do the entire motionY or until opposite groups are stable.
				//Boxes with an offset of 0 in X or Z are ignored when calculating roll and pitch respectively.
				boolean needPitchUp = false;
				boolean needPitchDown = false;
				boolean needRollRight = false;
				boolean needRollLeft = false;
				boolean needPitchUpAnytime = false;
				boolean needPitchDownAnytime = false;
				boolean needRollRightAnytime = false;
				boolean needRollLeftAnytime = false;
				boolean continueLoop = true;
				int loopCount = 0;
				float modifiedPitch = this.motionPitch;
				float modifiedRoll = this.motionRoll;
				
				do{
					needPitchUp = false;
					needPitchDown = false;
					needRollRight = false;
					needRollLeft = false;
					for(MultipartAxisAlignedBB box : this.getCurrentCollisionBoxes()){
						Vec3d offset = RotationSystem.getRotatedPoint(box.rel, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
						MultipartAxisAlignedBB offsetBox = box.getBoxWithOrigin(this.getPositionVector().add(offset).addVector(motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor));
						if(!getAABBCollisions(offsetBox, groundDeviceCollisionBoxMap.get(box), null).isEmpty()){
							if(box.rel.zCoord > 0){
								needPitchUp = true;
								needPitchUpAnytime = true;
							}
							if(box.rel.zCoord <= 0 && motionPitch >= 0){
								needPitchDown = true;
								needPitchDownAnytime = true;
							}
							if(box.rel.xCoord > 0){
								needRollRight = true;
								needRollRightAnytime = true;
							}
							if(box.rel.xCoord < 0){
								needRollLeft = true;
								needRollLeftAnytime = true;
							}
						}
					}
					
					//At this point a combinations of pitch and roll may be needed.
					//If so, apply either or both.  If not, add back some Y.
					//Do NOT add pitch or roll if we were subtracting it before (or vice-versa).
					//That's stupid and leads to infinite loops.
					if(needPitchUp && !needPitchDownAnytime){
						this.motionPitch -= 0.1F;
					}
					if(needPitchDown && !needPitchUpAnytime){
						this.motionPitch += 0.1F;
					}
					if(needRollLeft && !needRollRightAnytime){
						this.motionRoll -= 0.1F;
					}
					if(needRollRight && !needRollLeftAnytime){
						this.motionRoll += 0.1F;
					}
					
					//If nothing is colliding see about adding some Y back.
					if(!needPitchUp && !needPitchDown && !needRollRight && !needRollLeft){
						motionY = Math.max(originalMotionY, motionY - 0.1F);
					}
					
					//Exit this loop when:
					//Nothing is colliding (and motionY is equal to originalMotionY if not grounded) or
					//the pitch axis is colliding on both ends and the roll axis is not colliding (on the ground with no wheels) or
					//the roll axis is colliding on both ends and the pitch is not colliding (hanging on wheels) or
					//both roll and pitch have gone as far as they can go (system is in balance).
					if(!needPitchUp && !needPitchDown && !needRollRight && !needRollLeft && (groundDeviceNeedsLifting ? true : motionY == originalMotionY)){
						continueLoop = false;
					}else if(needPitchUp && needPitchDown && !needRollRight && !needRollLeft){
						continueLoop = false;
					}else if(needRollRight && needRollLeft && !needPitchUp && !needPitchDown){
						continueLoop = false;
					}else if(needPitchUpAnytime && needPitchDownAnytime && needRollRightAnytime && needRollLeftAnytime){
						continueLoop = false;
						if(loopCount == 1){
							loopCount = 0;
						}
					}
					if(loopCount == 0 && !continueLoop){
						motionPitch = modifiedPitch;
						motionRoll = modifiedRoll;
					}
					++loopCount;
				}while(continueLoop && loopCount < 20);
			}
		}
		
		//Now that that the movement has been checked, move the multipart.
		prevRotationRoll = rotationRoll;
		
		//Make sure we don't try to move if we have a really small movement.
		//This prevents odd jittering when idle.
		if(Math.abs(motionX) < 0.001){
			motionX = 0;
		}
		if(Math.abs(motionY) < 0.001){
			motionY = 0;
		}
		if(Math.abs(motionZ) < 0.001){
			motionZ = 0;
		}
		if(Math.abs(motionYaw) < 0.001){
			motionYaw = 0;
		}
		if(Math.abs(motionPitch) < 0.001){
			motionPitch = 0;
		}
		if(Math.abs(motionRoll) < 0.001){
			motionRoll = 0;
		}
		if(motionX != 0 || motionY != 0 || motionZ != 0 || motionPitch != 0 || motionYaw != 0 || motionRoll != 0){
			if(!worldObj.isRemote){
				rotationYaw += motionYaw;
				rotationPitch += motionPitch;
				rotationRoll += motionRoll;
				setPosition(posX + motionX*speedFactor, posY + motionY*speedFactor, posZ + motionZ*speedFactor);
				addToServerDeltas(motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor, motionYaw, motionPitch, motionRoll);
				MTS.MTSNet.sendToAll(new PacketMultipartDeltas(this, motionX*speedFactor, motionY*speedFactor, motionZ*speedFactor, motionYaw, motionPitch, motionRoll));
			}else{
				//Make sure the server is sending delta packets and NBT is initialized before we try to do delta correction.
				if(!(serverDeltaX == 0 && serverDeltaY == 0 && serverDeltaZ == 0)){
					double deltaX = motionX*speedFactor + (serverDeltaX - clientDeltaX)/200F;
					double deltaY = motionY*speedFactor + (serverDeltaY - clientDeltaY)/200F;
					double deltaZ = motionZ*speedFactor + (serverDeltaZ - clientDeltaZ)/200F;
					float deltaYaw = motionYaw + (serverDeltaYaw - clientDeltaYaw)/200F;
					float deltaPitch = motionPitch + (serverDeltaPitch - clientDeltaPitch)/200F;
					float deltaRoll = motionRoll + (serverDeltaRoll - clientDeltaRoll)/200F;
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
		}
	}
	
	/**
	 * Returns factor for braking.
	 * Depends on number of grounded core collision sections and braking ground devices.
	 */
	protected float getBrakingForceFactor(){
		float brakingFactor = 0;
		//First get the ground device braking contributions.
		for(PartGroundDevice groundDevice : this.groundedGroundDevices){
			float addedFactor = 0;
			if(brakeOn || parkingBrakeOn){
				addedFactor = groundDevice.getMotiveFriction();
			}
			if(addedFactor != 0){
				brakingFactor += Math.max(addedFactor - groundDevice.getFrictionLoss(), 0);
			}
		}
		
		//Now get any contributions from the colliding collision bits.
		for(MultipartAxisAlignedBB box : this.getCurrentCollisionBoxes()){
			if(!groundDeviceCollisionBoxMap.containsKey(box)){
				if(!worldObj.getCollisionBoxes(box.offset(0, -0.05F, 0)).isEmpty()){
					//0.6 is default slipperiness for blocks.  Anything extra should reduce friction, anything less should increase it.
					BlockPos pos = new BlockPos(box.pos.addVector(0, -1, 0));
					float frictionLoss = 0.6F - worldObj.getBlockState(pos).getBlock().slipperiness;
					brakingFactor += Math.max(2.0 - frictionLoss, 0);
				}
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
		for(PartGroundDevice groundDevice : this.groundedGroundDevices){
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
			for(PartGroundDevice groundDevice : this.groundedGroundDevices){
				float frictionLoss = groundDevice.getFrictionLoss();
				//Do we have enough friction to change yaw?
				if(groundDevice.turnsWithSteer && groundDevice.getLateralFriction() - frictionLoss > 0){
					turningFactor += groundDevice.getLateralFriction() - frictionLoss;
					turningDistance = (float) Math.max(turningDistance, Math.abs(groundDevice.offset.zCoord));
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
				if(Math.abs(velocity) - turningFactor/3F > 0){
					steeringAngle *= Math.pow(0.25F, Math.abs(velocity) - turningFactor/3F);
				}
				//Adjust turn force to steer angle based on turning factor.
				turningForce = -(float) (steeringAngle*velocity/2F);
				//Now add the sign to this force.
				turningForce *= Math.signum(this.getSteerAngle());
			}
		}
		return turningForce;
	}
	
	protected void reAdjustGroundSpeed(double groundSpeed){
		Vec3d groundVec = new Vec3d(headingVec.xCoord, 0, headingVec.zCoord).normalize();
		motionX = groundVec.xCoord * groundSpeed;
		motionZ = groundVec.zCoord * groundSpeed;
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
		
		if(worldObj.isRemote){
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
