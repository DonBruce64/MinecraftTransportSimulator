package minecraftflightsimulator.entities;

import java.util.Iterator;
import java.util.List;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.other.MFSHelper;
import minecraftflightsimulator.other.RotationHelper;
import minecraftflightsimulator.packets.control.AileronPacket;
import minecraftflightsimulator.packets.control.ElevatorPacket;
import minecraftflightsimulator.packets.control.RudderPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 *Main base abstract class for planes.  To use it two things need to happen.
 *First, all plane variables need to be defined in {@link entitInit}
 *Next, all protected child UUID strings must be set in the constructor, otherwise errors will occur. 
 * @author don_bruce
 *
 */
public abstract class EntityPlane extends EntityParent{	
	//visible plane variables
	public boolean hasFlaps = false;
	public byte aileronIncrement;
	public byte elevatorIncrement;
	public byte rudderIncrement;
	
	//note that angle variable should be divided by 10 to get actual angle.
	public int aileronAngle;
	public int elevatorAngle;
	public int rudderAngle;
	public int flapAngle;
	public int aileronCooldown;
	public int elevatorCooldown;
	public int rudderCooldown;
	public float criticalAoA;
	
	//Defined plane properties
	protected long fuelCapcacity;
	protected float mass;//kg
	protected float centerOfGravity;//m forward from center of lift
	protected float momentRoll;//kg*m^2
	protected float momentPitch;//kg*m^2
	protected float momentYaw;//kg*m^2
	protected float wingspan;//m
	protected float wingArea;//m^2
	protected float wingEfficiency;//unit-less
	protected float tailDistance;//m away from center of lift
	protected float rudderArea;//m^2
	protected float elevatorArea;//m^2
	protected float maxLiftCoeff;//unit-less
	protected float angleOfIncidence;//degrees
	protected float defaultElevatorAngle;//degrees	
	protected float initialDragCoeff;//unit-less
	protected float dragAtCriticalAoA;//unit-less
	protected float dragCoeffOffset;//unit-less

	//internal plane variables
	private byte groundedWheels;
	private byte collidedCores;
	private float currentMass;
	private float thirdWheelDistance;
	private float motionRoll;
	private float motionPitch;
	private float motionYaw;
	private double currentWingArea;
	private double wingAoA;
	private double aileronAoA;
	private double elevatorAoA;
	private double rudderAoA;
	private double dragCoeff;
	private double wingLiftCoeff;
	private double aileronLiftCoeff;
	private double elevatorLiftCoeff;
	private double rudderLiftCoeff;
	private double thrust;
	private double brakeForce;//kg*m/ticks^2
	private double thrustForce;//kg*m/ticks^2
	private double dragForce;//kg*m/ticks^2
	private double wingForce;//kg*m/ticks^2
	private double aileronForce;//kg*m/ticks^2
	private double elevatorForce;//kg*m/ticks^2
	private double rudderForce;//kg*m/ticks^2
	private double gravitationalForce;//kg*m/ticks^2
	private double thrustTorque;//kg*m^2/ticks^2
	private double aileronTorque;//kg*m^2/ticks^2
	private double elevatorTorque;//kg*m^2/ticks^2
	private double rudderTorque;//kg*m^2/ticks^2
	private double gravitationalTorque;//kg*m^2/ticks^2
	private double xCollisionDepth;
	private double yCollisionDepth;
	private double zCollisionDepth;
	private double prevYawXChildOffset;
	private double prevYawZChildOffset;
	private double yawChildXOffset;
	private double yawChildZOffset;
	private double originalMotionYaw;
	private double prevPitchChildOffset;
	private double pitchChildOffset;
	private double originalMotionPitch;
	private double prevRollChildOffset;
	private double rollChildOffset;
	private double originalMotionRoll;
	
	private List collidingBoxes;
	private AxisAlignedBB newChildBox;
	private AxisAlignedBB collidingBox;
	private Iterator<EntityChild> childIterator;
	private Iterator<EntityPropeller> propellerIterator;
	private EntityChild child;
	private EntityPropeller propeller;
	private Vec3 offset;
	
	public EntityPlane(World world){
		super(world);
	}
	
	public EntityPlane(World world, float posX, float posY, float posZ, float rotation, int textureCode, boolean hasFlaps){
		super(world, posX, posY, posZ, rotation);
		this.textureOptions=(byte) textureCode;
		this.hasFlaps=hasFlaps;
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();	
		if(!linked){return;}
		refreshChildStatuses();
		getBasicProperties();
		getForcesAndMotions();
		performGroundOperations();
		adjustXZMovement();
		adjustYawMovement();
		adjustPitchMovement();
		adjustRollMovement();
		adjustYMovement();
		movePlane();
		if(!worldObj.isRemote){
			//Movement for children on client side is done in tick handler.
			moveChildren();
			dampenControlSurfaces();
		}
	}
		
	private Vec3 getWingVec(){
		return RotationHelper.getRotatedY(rotationPitch, rotationYaw, rotationRoll);
	}
	
	private double getLiftCoeff(double angleOfAttack, double maxLiftCoeff, double criticalAngleOfAttack){
		if(Math.abs(angleOfAttack) <= criticalAngleOfAttack*1.25){
			return maxLiftCoeff*Math.sin(Math.PI/2*angleOfAttack/criticalAngleOfAttack);
		}else if(Math.abs(angleOfAttack) <= criticalAngleOfAttack*1.5){
			if(angleOfAttack > 0){
				return maxLiftCoeff*(0.4 + 1/(angleOfAttack - criticalAngleOfAttack));
			}else{
				return maxLiftCoeff*(-0.4 + 1/(angleOfAttack + criticalAngleOfAttack));
			}
		}else{
			return maxLiftCoeff*Math.sin(Math.PI/6*angleOfAttack/criticalAngleOfAttack);
		}
	}
	
	private void refreshChildStatuses(){
		groundedWheels = 0;
		collidedCores = 0;
		childIterator = getChildIterator();
		while(childIterator.hasNext()){
			child = childIterator.next();
			if(child instanceof EntityWheel){
				if(!child.isDead){
					child.onGround = !child.worldObj.getCollidingBoundingBoxes(child, child.boundingBox.copy().offset(0, -0.05, 0)).isEmpty();
					if(child.onGround){
						if(child.offsetX > 0){
							groundedWheels += 2;
						}else if(child.offsetX < 0){
							groundedWheels += 4;
						}else{
							groundedWheels += 1;
							thirdWheelDistance = child.offsetZ;
						}
					}
				}
			}else if(child instanceof EntityCore){
				if(!child.worldObj.getCollidingBoundingBoxes(child, child.boundingBox.copy().expand(0.2, 0.2, 0.2)).isEmpty()){
					++collidedCores;
				}
			}
		}
	}
	
	private void getBasicProperties(){
		currentMass = (float) (mass + fuel/50);
		childIterator = this.getChildIterator();
		while(childIterator.hasNext()){
			child = childIterator.next();
			if(child.riddenByEntity != null){
				currentMass += 100;
				if(child.riddenByEntity instanceof EntityPlayer){
					currentMass += MFSHelper.calculateInventoryWeight(((EntityPlayer) child.riddenByEntity).inventory);
				}
			}else if(child instanceof EntityPlaneChest){
				currentMass += MFSHelper.calculateInventoryWeight((EntityPlaneChest) child);
			}else if(child instanceof EntityPropeller){
				currentMass += 50F*child.propertyCode%10;
			}
		}
		
		currentWingArea = wingArea + wingArea*flapAngle/250F;
		bearingVec = getLookVec();
		wingVec = getWingVec();
		sideVec = bearingVec.crossProduct(wingVec);
		velocityVec.xCoord = motionX;
		velocityVec.yCoord = motionY;
		velocityVec.zCoord = motionZ;
		velocity = velocityVec.dotProduct(bearingVec);
		velocityVec = velocityVec.normalize();
		
		trackAngle = Math.toDegrees(Math.atan2(velocityVec.dotProduct(wingVec), velocityVec.dotProduct(bearingVec)));
		wingAoA = angleOfIncidence - trackAngle;
		aileronAoA = aileronAngle/10F;
		elevatorAoA = defaultElevatorAngle - trackAngle - elevatorAngle/10F;
		rudderAoA = rudderAngle/10F + Math.toDegrees(Math.atan2(velocityVec.dotProduct(sideVec), velocityVec.dotProduct(bearingVec)));
		
		dragCoeff = dragCoeffOffset*Math.pow(angleOfIncidence - trackAngle, 2) + initialDragCoeff;
		wingLiftCoeff = getLiftCoeff(wingAoA, maxLiftCoeff + flapAngle/350F, criticalAoA);
		aileronLiftCoeff = getLiftCoeff(aileronAoA, maxLiftCoeff, criticalAoA);
		elevatorLiftCoeff = getLiftCoeff(elevatorAoA, maxLiftCoeff, criticalAoA);
		rudderLiftCoeff = getLiftCoeff(rudderAoA, maxLiftCoeff, criticalAoA);
	}
	
	private void getForcesAndMotions(){
		thrustForce = 0;
		propellerIterator = getPropellerIterator();
		while(propellerIterator.hasNext()){
			propeller = propellerIterator.next();
			if(!propeller.isDead){
				thrust = propeller.getThrustForce();
				thrustForce += thrust;
				thrustTorque += thrust*propeller.offsetX;
			}
		}	
		
		dragForce = 0.5F*airDensity*velocity*velocity*currentWingArea*(dragCoeff + wingLiftCoeff*wingLiftCoeff/(Math.PI*wingspan*wingspan/currentWingArea*wingEfficiency));		
		brakeForce = (brakeOn || parkingBrakeOn) ? ((groundedWheels & 1) + (groundedWheels & 2)/2 + (groundedWheels & 4)/4)*4 + collidedCores*2: collidedCores*2;
		wingForce = 0.5F*airDensity*velocity*velocity*currentWingArea*wingLiftCoeff;
		aileronForce = 0.5F*airDensity*velocity*velocity*wingArea/5*aileronLiftCoeff;
		elevatorForce = 0.5F*airDensity*velocity*velocity*elevatorArea*elevatorLiftCoeff;			
		rudderForce = 0.5F*airDensity*velocity*velocity*rudderArea*rudderLiftCoeff;
		gravitationalForce = currentMass*(9.8/400);
					
		aileronTorque = 2*aileronForce*wingspan*0.3;
		elevatorTorque = elevatorForce*tailDistance;
		rudderTorque = rudderForce*tailDistance;
		gravitationalTorque = gravitationalForce*centerOfGravity;
		
		if(brakeForce > 0){
			if(motionX > 0){
				motionX = Math.max(motionX + (bearingVec.xCoord*thrustForce - velocityVec.xCoord*(dragForce + brakeForce) + wingVec.xCoord*(wingForce + elevatorForce))/currentMass, 0);
			}else if(motionX < 0){
				motionX = Math.min(motionX + (bearingVec.xCoord*thrustForce - velocityVec.xCoord*(dragForce + brakeForce) + wingVec.xCoord*(wingForce + elevatorForce))/currentMass, 0);
			}
			if(motionZ > 0){
				motionZ = Math.max(motionZ + (bearingVec.zCoord*thrustForce - velocityVec.zCoord*(dragForce + brakeForce) + wingVec.zCoord*(wingForce + elevatorForce))/currentMass, 0);	
			}else if(motionZ < 0){
				motionZ = Math.min(motionZ + (bearingVec.zCoord*thrustForce - velocityVec.zCoord*(dragForce + brakeForce) + wingVec.zCoord*(wingForce + elevatorForce))/currentMass, 0);
			}
		}else{
			motionX += (bearingVec.xCoord*thrustForce - velocityVec.xCoord*dragForce + wingVec.xCoord*(wingForce + elevatorForce))/currentMass;
			motionZ += (bearingVec.zCoord*thrustForce - velocityVec.zCoord*dragForce + wingVec.zCoord*(wingForce + elevatorForce))/currentMass;
		}
		motionY += (bearingVec.yCoord*thrustForce - velocityVec.yCoord*dragForce + wingVec.yCoord*(wingForce + elevatorForce) - gravitationalForce)/currentMass;
		motionRoll = (float) (180/Math.PI*((1-bearingVec.yCoord)*aileronTorque)/momentRoll);
		motionPitch = (float) (180/Math.PI*((1-Math.abs(sideVec.yCoord))*elevatorTorque - sideVec.yCoord*(thrustTorque + rudderTorque) + wingVec.yCoord*gravitationalTorque)/momentPitch);
		motionYaw = (float) (180/Math.PI*(bearingVec.yCoord*aileronTorque - wingVec.yCoord*(thrustTorque - rudderTorque) + sideVec.yCoord*elevatorTorque)/momentYaw);
	}
	
	private void performGroundOperations(){
		if(groundedWheels >= 6 && Math.abs(rotationRoll) < 1){
			rotationRoll = motionRoll = 0;
			adjustYMovement2();
		}
		if(groundedWheels == 3 || groundedWheels == 5){
			if(thirdWheelDistance > 0){
				if(motionPitch > 0){
					motionPitch = 0;
					if(Math.abs(rotationPitch) < 2){
						rotationPitch = 0;
					}
					adjustYMovement2();
				}
			}
		}
		if(groundedWheels == 7){
			if(motionY<0){motionY=0;}
			if(thirdWheelDistance > 0){
				if(motionPitch > 0){
					motionPitch = 0;
					if(Math.abs(rotationPitch) < 2){
						rotationPitch = 0;
					}
					adjustYMovement2();
				}
			}
			motionYaw += 7*velocityVec.dotProduct(sideVec) + rudderAngle/(350*(0.5 + velocity*velocity));
			double groundSpeed = Math.hypot(motionX, motionZ);
			bearingVec = getLookVec();
			motionX = bearingVec.xCoord * groundSpeed;
			motionZ = bearingVec.zCoord * groundSpeed;
		}
	}
	
	private void adjustXZMovement(){
		childIterator = this.getChildIterator();
		while(childIterator.hasNext()){
			xCollisionDepth = 0;
			zCollisionDepth = 0;
			child = childIterator.next();
			newChildBox = child.boundingBox.copy().offset(motionX*MFS.planeSpeedFactor, 0, 0);
			collidingBoxes = child.worldObj.getCollidingBoundingBoxes(child, newChildBox);			
			for(int i=0; i < collidingBoxes.size(); ++i){
				collidingBox = (AxisAlignedBB) collidingBoxes.get(i);
				if(newChildBox.maxX > collidingBox.minX && newChildBox.maxX < collidingBox.maxX){
					xCollisionDepth = Math.min(collidingBox.minX - newChildBox.maxX, xCollisionDepth);
				}else if(newChildBox.minX < collidingBox.maxX && newChildBox.minX > collidingBox.minX){
					xCollisionDepth = Math.max(collidingBox.maxX - newChildBox.minX, xCollisionDepth);
				}
			}
			
			newChildBox = child.boundingBox.copy().offset(0, 0, motionZ*MFS.planeSpeedFactor);
			collidingBoxes = child.worldObj.getCollidingBoundingBoxes(child, newChildBox);	
			for(int i=0; i < collidingBoxes.size(); ++i){
				collidingBox = (AxisAlignedBB) collidingBoxes.get(i);
				if(newChildBox.maxZ > collidingBox.minZ && newChildBox.maxZ < collidingBox.maxZ){
					zCollisionDepth = Math.min(collidingBox.minZ - newChildBox.maxZ, zCollisionDepth);
				}else if(newChildBox.minZ < collidingBox.maxZ && newChildBox.minZ > collidingBox.minZ){
					zCollisionDepth = Math.max(collidingBox.maxZ - newChildBox.minZ, zCollisionDepth);
				}
			}
			
			if(xCollisionDepth != 0 || zCollisionDepth != 0){
				if(Math.abs(xCollisionDepth) > 0.3 || Math.abs(zCollisionDepth) > 0.3){
					if(!worldObj.isRemote){
						if(child instanceof EntityCore){
							this.explodeAtPosition(child.posX, child.posY, child.posZ);
							return;
						}else{
							worldObj.playSoundAtEntity(this, "minecraft:random.break", 2, 1);
							child.setDead();
							removeChild(child.UUID, childIterator);
							this.sendDataToClient();
						}
					}else{
						this.requestDataFromServer();
					}
					continue;
				}
				if(motionX > 0){
					motionX = Math.max(motionX + xCollisionDepth/MFS.planeSpeedFactor, 0);
				}else{
					motionX = Math.min(motionX + xCollisionDepth/MFS.planeSpeedFactor, 0);
				}
				if(motionZ > 0){
					motionZ = Math.max(motionZ + zCollisionDepth/MFS.planeSpeedFactor, 0);
				}else{
					motionZ = Math.min(motionZ + zCollisionDepth/MFS.planeSpeedFactor, 0);
				}
			}
		}
	}
	
	private void adjustYawMovement(){
		prevYawXChildOffset = 0;
		prevYawZChildOffset = 0;
		originalMotionYaw = motionYaw;
		do{
			yawChildXOffset = 0;
			yawChildZOffset = 0;
			childIterator = this.getChildIterator();
			while(childIterator.hasNext()){
				child = childIterator.next();
				offset = RotationHelper.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch, rotationYaw + motionYaw, rotationRoll);
				newChildBox = child.boundingBox.copy().offset(posX + offset.xCoord - child.posX + motionX*MFS.planeSpeedFactor, 0, posZ + offset.zCoord - child.posZ + motionZ*MFS.planeSpeedFactor);
				child.isCollidedHorizontally = !child.worldObj.getCollidingBoundingBoxes(child, newChildBox).isEmpty();
				if(child.isCollidedHorizontally){
					if(yawChildXOffset==0){
						yawChildXOffset = child.offsetX;
					}else if(Math.signum(yawChildXOffset)!=Math.signum(child.offsetX)){
						motionYaw = 0;
						return;				
					}
					if(yawChildZOffset==0){
						yawChildZOffset = child.offsetZ;
					}else if(Math.signum(yawChildZOffset)!=Math.signum(child.offsetZ)){ 
						motionYaw = 0;
						return;				
					}
				}
			}
			//if there's a blockage, take care of it.
			if(yawChildXOffset!=0 || yawChildZOffset!=0){
				if(Math.signum(yawChildXOffset)!=Math.signum(prevYawXChildOffset)){
					if(prevYawXChildOffset!=0){ 
						motionYaw = 0;
						return;
					}
				}
				if(Math.signum(yawChildZOffset)!=Math.signum(prevYawZChildOffset)){
					if(prevYawZChildOffset!=0){
						motionYaw = 0;
						return;
					}
				}				
				if(Math.signum(motionYaw)!=Math.signum(originalMotionYaw)){
					motionYaw = 0;
					return;
				}
				motionYaw -= motionYaw > 0 ? 0.1 : -0.1;
				prevYawXChildOffset = yawChildXOffset;
				prevYawZChildOffset = yawChildZOffset;
				if((Math.signum(originalMotionYaw)!=Math.signum(motionRoll)) || Math.abs(motionYaw) > 5){
					motionYaw = 0;
					return;
				}
			}
		}while(yawChildXOffset!=0 || yawChildZOffset!=0);
	}

	private void adjustPitchMovement(){
		prevPitchChildOffset = 0;
		originalMotionPitch = motionPitch;
		do{
			pitchChildOffset = 0;
			childIterator = this.getChildIterator();
			while(childIterator.hasNext()){
				child = childIterator.next();
				offset = RotationHelper.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll);
				newChildBox = child.boundingBox.copy().offset(posX + offset.xCoord - child.posX + motionX*MFS.planeSpeedFactor, posY + offset.yCoord - child.posY + motionY*MFS.planeSpeedFactor, posZ + offset.zCoord - child.posZ + motionZ*MFS.planeSpeedFactor).contract(0.001, 0.001, 0.001);
				child.isCollidedHorizontally = !child.worldObj.getCollidingBoundingBoxes(child, newChildBox).isEmpty();
				if(child.isCollidedHorizontally && child.offsetZ != 0){
					if(pitchChildOffset==0){
						pitchChildOffset = child.offsetZ;
					}else if(Math.signum(pitchChildOffset)!=Math.signum(child.offsetZ)){
						motionPitch = 0;
						return;	
					}
				}
				
			}
			//if there's a blockage, take care of it.
			if(pitchChildOffset!=0){
				if(Math.signum(pitchChildOffset)!=Math.signum(prevPitchChildOffset)){
					if(prevPitchChildOffset!=0){
						motionPitch = 0;
						return;							
					}
				}
				motionPitch += pitchChildOffset > 0 ? -0.1 : 0.1;
				prevPitchChildOffset = pitchChildOffset;
				if((Math.abs(motionPitch)>=5 && Math.signum(originalMotionPitch)!=Math.signum(motionPitch)) || Math.abs(motionPitch) > 5){
					motionPitch = 0;
					return;	
				}
			}
		}while(pitchChildOffset!=0);
	}
	
	private void adjustRollMovement(){
		prevRollChildOffset = 0;
		originalMotionRoll = motionRoll;
		do{
			rollChildOffset = 0;
			childIterator = this.getChildIterator();
			while(childIterator.hasNext()){
				child = childIterator.next();
				offset = RotationHelper.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
				newChildBox = child.boundingBox.copy().offset(posX + offset.xCoord - child.posX + motionX*MFS.planeSpeedFactor, posY + offset.yCoord - child.posY + motionY*MFS.planeSpeedFactor, posZ + offset.zCoord - child.posZ + motionZ*MFS.planeSpeedFactor).contract(0.001, 0.001, 0.001);
				child.isCollidedHorizontally = !child.worldObj.getCollidingBoundingBoxes(child, newChildBox).isEmpty();
				if(child.isCollidedHorizontally){
					if(rollChildOffset==0){
						rollChildOffset = child.offsetX;
					}else if(Math.signum(rollChildOffset)!=Math.signum(child.offsetX)){
						motionRoll = 0;
						return;	
					}
				}
			}
			//if there's a blockage, take care of it.
			if(rollChildOffset!=0){
				if(Math.signum(rollChildOffset)!=Math.signum(prevRollChildOffset)){
					if(prevRollChildOffset!=0){
						motionRoll = 0;
						return;
					}
				}
				motionRoll += rollChildOffset > 0 ? 0.1 : -0.1;
				prevRollChildOffset = rollChildOffset;
				if((Math.abs(motionRoll)>=5 && Math.signum(originalMotionRoll)!=Math.signum(motionRoll)) || Math.abs(motionRoll) > 5){
					motionRoll = 0;
					return;
				}
			}
		}while(rollChildOffset!=0);
	}
	
	private void adjustYMovement(){
		childIterator = this.getChildIterator();
		while(childIterator.hasNext()){
			yCollisionDepth = 0;
			child = childIterator.next();
			offset = RotationHelper.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
			newChildBox = child.boundingBox.copy().offset(posX + offset.xCoord - child.posX + motionX*MFS.planeSpeedFactor, posY + offset.yCoord - child.posY + motionY*MFS.planeSpeedFactor, posZ + offset.zCoord - child.posZ + motionZ*MFS.planeSpeedFactor);
			collidingBoxes = child.worldObj.getCollidingBoundingBoxes(child, newChildBox);
			for(int i=0; i < collidingBoxes.size(); ++i){
				collidingBox = (AxisAlignedBB) collidingBoxes.get(i);
				if(newChildBox.maxY > collidingBox.minY && newChildBox.maxY < collidingBox.maxY){
					yCollisionDepth = Math.min(collidingBox.minY - newChildBox.maxY, yCollisionDepth);
				}else if(newChildBox.minY < collidingBox.maxY && newChildBox.minY > collidingBox.minY){
					yCollisionDepth =  Math.max(collidingBox.maxY - newChildBox.minY, yCollisionDepth);
				}
			}
			if(yCollisionDepth != 0){
				if(Math.abs(yCollisionDepth) > 0.3){
					if(!worldObj.isRemote){
						if(child instanceof EntityCore){
							this.explodeAtPosition(child.posX, child.posY, child.posZ);
							return;
						}else{
							child.setDead();
							worldObj.playSoundAtEntity(this, "minecraft:random.break", 2, 1);
							removeChild(child.UUID, childIterator);
							this.sendDataToClient();
						}
					}else{
						this.requestDataFromServer();
					}
					continue;
				}
				motionY += yCollisionDepth/MFS.planeSpeedFactor;
			}
		}
	}
	
	private void adjustYMovement2(){
		childIterator = this.getChildIterator();
		while(childIterator.hasNext()){
			yCollisionDepth = 0;
			child = childIterator.next();
			newChildBox = child.boundingBox.copy().offset(0, motionY*MFS.planeSpeedFactor, 0);
			collidingBoxes = child.worldObj.getCollidingBoundingBoxes(child, newChildBox);
			for(int i=0; i < collidingBoxes.size(); ++i){
				collidingBox = (AxisAlignedBB) collidingBoxes.get(i);
				if(newChildBox.maxY > collidingBox.minY && newChildBox.maxY < collidingBox.maxY){
					yCollisionDepth = Math.min(collidingBox.minY - newChildBox.maxY, yCollisionDepth);
				}else if(newChildBox.minY < collidingBox.maxY && newChildBox.minY > collidingBox.minY){
					yCollisionDepth =  Math.max(collidingBox.maxY - newChildBox.minY, yCollisionDepth);
				}
			}
			if(yCollisionDepth > 0){
				if(yCollisionDepth < 0.3){
					motionY += yCollisionDepth/MFS.planeSpeedFactor;
				}
			}
		}
	}
		
	private void movePlane(){
		prevRotationRoll = rotationRoll;
		prevRotationPitch = rotationPitch;
		prevRotationYaw = rotationYaw;
		rotationRoll += motionRoll;
		rotationPitch += motionPitch;
		rotationYaw += motionYaw;
		setPosition(posX + motionX*MFS.planeSpeedFactor, posY + motionY*MFS.planeSpeedFactor, posZ + motionZ*MFS.planeSpeedFactor);
	}

	private void dampenControlSurfaces(){
		/*
		if(aileronCooldown==0){
			if(aileronAngle > 0){
				aileronAngle -= aileronIncrement/2;
				MFS.MFSNet.sendToAll(new AileronPacket(this.getEntityId(), (byte) (-aileronIncrement/2)));
			}else if(aileronAngle < 0){
				aileronAngle += aileronIncrement/2;
				MFS.MFSNet.sendToAll(new AileronPacket(this.getEntityId(), (byte) (aileronIncrement/2)));
			}
		}else{
			--aileronCooldown;
		}
		if(elevatorCooldown==0){
			if(elevatorAngle > 0){
				elevatorAngle -= elevatorIncrement/2;
				MFS.MFSNet.sendToAll(new ElevatorPacket(this.getEntityId(), (byte) (-elevatorIncrement/2)));
			}else if(elevatorAngle < 0){
				elevatorAngle += elevatorIncrement/2;
				MFS.MFSNet.sendToAll(new ElevatorPacket(this.getEntityId(), (byte) (elevatorIncrement/2)));
			}
		}else{
			--elevatorCooldown;
		}
		if(rudderCooldown==0){
			if(rudderAngle > 0){
				rudderAngle -= rudderIncrement/2;
				MFS.MFSNet.sendToAll(new RudderPacket(this.getEntityId(), (byte) (-rudderIncrement/2)));
			}else if(rudderAngle < 0){
				rudderAngle += rudderIncrement/2;
				MFS.MFSNet.sendToAll(new RudderPacket(this.getEntityId(), (byte) (rudderIncrement/2)));
			}
		}else{
			--rudderCooldown;
		}
		*/
	}
	
	public double[] getDebugForces(){
		return new double[]{this.thrustForce, this.dragForce, this.wingForce, this.gravitationalForce};
	}
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.hasFlaps=tagCompound.getBoolean("hasFlaps");
		this.aileronAngle=tagCompound.getInteger("aileronAngle");
		this.elevatorAngle=tagCompound.getInteger("elevatorAngle");
		this.rudderAngle=tagCompound.getInteger("rudderAngle");
		this.flapAngle=tagCompound.getInteger("flapAngle");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("hasFlaps", this.hasFlaps);
		tagCompound.setInteger("aileronAngle", this.aileronAngle);
		tagCompound.setInteger("elevatorAngle", this.elevatorAngle);
		tagCompound.setInteger("rudderAngle", this.rudderAngle);
		tagCompound.setInteger("flapAngle", this.flapAngle);
	}
}