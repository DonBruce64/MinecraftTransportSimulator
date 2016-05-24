package minecraftflightsimulator.entities.core;

import java.util.List;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.parts.EntityPlaneChest;
import minecraftflightsimulator.entities.parts.EntityPropeller;
import minecraftflightsimulator.entities.parts.EntitySkid;
import minecraftflightsimulator.entities.parts.EntityWheel;
import minecraftflightsimulator.helpers.RotationHelper;
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
	public boolean hasFlaps;
	
	//note that angle variable should be divided by 10 to get actual angle.
	public short aileronAngle;
	public short elevatorAngle;
	public short rudderAngle;
	public short flapAngle;
	public short aileronCooldown;
	public short elevatorCooldown;
	public short rudderCooldown;
	
	//Defined plane properties
	protected int fuelCapcacity;//1 bucket is 100 units
	protected float emptyMass;//kg
	protected float momentRoll;//kg*m^2
	protected float momentPitch;//kg*m^2
	protected float momentYaw;//kg*m^2
	protected float wingspan;//m
	protected float wingArea;//m^2
	protected float tailDistance;//m away from center of lift
	protected float rudderArea;//m^2
	protected float elevatorArea;//m^2
	protected float defaultElevatorAngle;//degrees	
	protected float initialDragCoeff;//unit-less
	protected float dragAtCriticalAoA;//unit-less
	protected float dragCoeffOffset;//unit-less

	//internal plane variables
	private byte groundedWheels;
	private byte groundedCores;
	private float currentMass;
	private float addedMass;
	private float currentCOG;
	private float brakeDistance;
	private float motionRoll;
	private float motionPitch;
	private float motionYaw;
	private double currentWingArea;
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
	private double brakeTorque;//kg*m^2/ticks^2
	private double thrustTorque;//kg*m^2/ticks^2
	private double aileronTorque;//kg*m^2/ticks^2
	private double elevatorTorque;//kg*m^2/ticks^2
	private double rudderTorque;//kg*m^2/ticks^2
	private double gravitationalTorque;//kg*m^2/ticks^2
	
	private double xCollisionDepth;
	private double yCollisionDepth;
	private double zCollisionDepth;
	private double originalMotionYaw;
	private double originalMotionPitch;
	private double originalMotionRoll;
	private double yawChildXOffset;
	private double yawChildZOffset;
	private double pitchChildOffset;
	private double rollChildOffset;
	private double prevYawXChildOffset;
	private double prevYawZChildOffset;
	private double prevPitchChildOffset;
	private double prevRollChildOffset;
	
	private List collidingBoxes;
	private AxisAlignedBB newChildBox;
	private AxisAlignedBB collidingBox;
	private Vec3 offset;
	
	public EntityPlane(World world){
		super(world);
	}
	
	public EntityPlane(World world, float posX, float posY, float posZ, float rotation, int textureCode){
		super(world, posX, posY, posZ, rotation);
		this.textureOptions=(byte) textureCode;
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();	
		if(!linked){return;}
		refreshGroundedStatuses();
		getBasicProperties();
		getForcesAndMotions();
		performGroundOperations();
		adjustXZMovement();
		adjustYawMovement();
		adjustRollMovement();
		adjustVerticalMovement();
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
	
	private double getLiftCoeff(double angleOfAttack, double maxLiftCoeff){
		if(Math.abs(angleOfAttack) <= 15*1.25){
			return maxLiftCoeff*Math.sin(Math.PI/2*angleOfAttack/15);
		}else if(Math.abs(angleOfAttack) <= 15*1.5){
			if(angleOfAttack > 0){
				return maxLiftCoeff*(0.4 + 1/(angleOfAttack - 15));
			}else{
				return maxLiftCoeff*(-0.4 + 1/(angleOfAttack + 15));
			}
		}else{
			return maxLiftCoeff*Math.sin(Math.PI/6*angleOfAttack/15);
		}
	}
	
	private void refreshGroundedStatuses(){
		brakeDistance = groundedWheels = groundedCores = 0;
		for(EntityChild child : getChildren()){
			if(child instanceof EntityWheel || child instanceof EntitySkid){
				if(!child.isDead){
					if(child.isOnGround()){
						if(child.offsetX != 0){
							brakeDistance = -child.offsetY;
							groundedWheels += child.offsetX > 0 ? 2 : 4;
						}else{
							groundedWheels += 1;
						}
					}
				}
			}else if(child instanceof EntityCore){
				if(child.isOnGround()){
					++groundedCores;
				}
			}
		}
	}
	
	private void getBasicProperties(){		
		currentCOG = 1;
		currentMass = (float) (emptyMass + fuel/50);
		for(EntityChild child : getChildren()){
			addedMass = 0;
			if(child.riddenByEntity != null){
				if(child.riddenByEntity instanceof EntityPlayer){
					addedMass = 100 + calculateInventoryWeight(((EntityPlayer) child.riddenByEntity).inventory);
				}else{
					addedMass = 100;
				}
			}else if(child instanceof EntityPlaneChest){
				addedMass = calculateInventoryWeight((EntityPlaneChest) child);
			}else if(child instanceof EntityPropeller){
				addedMass = 50*(child.propertyCode%10);
			}
			currentCOG = (currentCOG*currentMass + child.offsetZ*addedMass)/(currentMass+addedMass);
			currentMass += addedMass;
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
		dragCoeff = dragCoeffOffset*Math.pow(trackAngle, 2) + initialDragCoeff;
		wingLiftCoeff = getLiftCoeff(-trackAngle, 2 + flapAngle/350F);
		aileronLiftCoeff = getLiftCoeff(aileronAngle/10F, 2);
		elevatorLiftCoeff = getLiftCoeff(defaultElevatorAngle - trackAngle - elevatorAngle/10F, 2);
		rudderLiftCoeff = getLiftCoeff(rudderAngle/10F + Math.toDegrees(Math.atan2(velocityVec.dotProduct(sideVec), velocityVec.dotProduct(bearingVec))), 2);
	}
	
	private void getForcesAndMotions(){
		thrustForce = thrustTorque = 0;
		for(EntityPropeller propeller : getPropellers()){
			if(!propeller.isDead){
				thrust = propeller.getThrustForce();
				thrustForce += thrust;
				thrustTorque += thrust*propeller.offsetX;
			}
		}
		
		dragForce = 0.5F*airDensity*velocity*velocity*currentWingArea*(dragCoeff + wingLiftCoeff*wingLiftCoeff/(Math.PI*wingspan*wingspan/currentWingArea*0.8));		
		brakeForce = (brakeOn || parkingBrakeOn) ? ((groundedWheels & 1) + (groundedWheels & 2)/2 + (groundedWheels & 4)/4)*4 + groundedCores*2 : groundedCores*2;
		wingForce = 0.5F*airDensity*velocity*velocity*currentWingArea*wingLiftCoeff;
		aileronForce = 0.5F*airDensity*velocity*velocity*wingArea/5*aileronLiftCoeff;
		elevatorForce = 0.5F*airDensity*velocity*velocity*elevatorArea*elevatorLiftCoeff;			
		rudderForce = 0.5F*airDensity*velocity*velocity*rudderArea*rudderLiftCoeff;
		gravitationalForce = currentMass*(9.8/400);
					
		brakeTorque = ((groundedWheels & 2)/2 + (groundedWheels & 4)/4)*brakeForce*brakeDistance;
		aileronTorque = 2*aileronForce*wingspan*0.3;
		elevatorTorque = elevatorForce*tailDistance;
		rudderTorque = rudderForce*tailDistance;
		gravitationalTorque = gravitationalForce*currentCOG;
		
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
				
		//TODO get this working as well
		motionY += (bearingVec.yCoord*thrustForce - velocityVec.yCoord*dragForce + wingVec.yCoord*(wingForce + elevatorForce) - gravitationalForce)/currentMass;
		motionRoll = (float) (180/Math.PI*((1-bearingVec.yCoord)*aileronTorque)/momentRoll);
		motionPitch = (float) (180/Math.PI*((1-Math.abs(sideVec.yCoord))*elevatorTorque - sideVec.yCoord*(thrustTorque + rudderTorque) + (1-Math.abs(bearingVec.yCoord))*(gravitationalTorque + brakeTorque))/momentPitch);
		motionYaw = (float) (180/Math.PI*(bearingVec.yCoord*aileronTorque - wingVec.yCoord*(-thrustTorque - rudderTorque) + sideVec.yCoord*elevatorTorque)/momentYaw);
	}
	
	private void performGroundOperations(){
		if(groundedWheels >= 6 && Math.abs(rotationRoll) < 1){
			rotationRoll = motionRoll = 0;
		}
		if(groundedWheels == 7){
			if(motionY<0){motionY=0;}
			if(motionPitch*currentCOG > 0 ){motionPitch = 0;}
			if(Math.abs(rotationPitch) < 0.25){rotationPitch = 0;}
			motionYaw += 7*velocityVec.dotProduct(sideVec) + rudderAngle/(350*(0.5 + velocity*velocity));
			bearingVec = getLookVec();
			double groundSpeed = Math.hypot(motionX, motionZ);
			Vec3 groundVec = bearingVec.addVector(0, -bearingVec.yCoord, 0).normalize();
			motionX = groundVec.xCoord * groundSpeed;
			motionZ = groundVec.zCoord * groundSpeed;
		}
	}
	
	private void adjustXZMovement(){
		for(EntityChild child : getChildren()){
			xCollisionDepth = 0;
			zCollisionDepth = 0;
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
							removeChild(child.UUID);
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
			for(EntityChild child : getChildren()){
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
	
	private void adjustRollMovement(){
		prevRollChildOffset = 0;
		originalMotionRoll = motionRoll;
		do{
			rollChildOffset = 0;
			for(EntityChild child : getChildren()){
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
	
	private void adjustVerticalMovement(){
		prevPitchChildOffset = 0;
		originalMotionPitch = motionPitch;
		do{
			pitchChildOffset = 0;
			for(EntityChild child : getChildren()){
				offset = RotationHelper.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);				
				offset = offset.addVector(posX - child.posX + motionX*MFS.planeSpeedFactor, posY - child.posY + motionY*MFS.planeSpeedFactor, posZ - child.posZ + motionZ*MFS.planeSpeedFactor);				
				if(child.willCollideVerticallyWithOffset(offset.xCoord, offset.yCoord, offset.zCoord)){
					if(child.offsetZ != 0){
						prevPitchChildOffset = pitchChildOffset;
						pitchChildOffset = child.offsetZ;
						if(prevPitchChildOffset != 0 && (prevPitchChildOffset * pitchChildOffset < 0)){
							if((motionY += 0.1) > 0){
								pitchChildOffset = motionY = motionPitch = 0;
								return;
							}else{
								break;
							}
						}else{
							motionPitch += pitchChildOffset > 0 ? -0.1 : 0.1;
							if(Math.abs(motionPitch) >= 15){
								pitchChildOffset = motionPitch = 0;
								break;	
							}
						}
					}
				}
			}
		}while(pitchChildOffset != 0);
		
		for(EntityChild child : getChildren()){
			yCollisionDepth = 0;
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
							removeChild(child.UUID);
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
		for(EntityChild child : getChildren()){
			yCollisionDepth = 0;
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
		rotationPitch = (motionPitch + rotationPitch)%360;
		rotationYaw += motionYaw;
		setPosition(posX + motionX*MFS.planeSpeedFactor, posY + motionY*MFS.planeSpeedFactor, posZ + motionZ*MFS.planeSpeedFactor);
	}

	private void dampenControlSurfaces(){
		if(aileronCooldown==0){
			if(aileronAngle != 0){
				MFS.MFSNet.sendToAll(new AileronPacket(this.getEntityId(), aileronAngle < 0, (short) 0));
				aileronAngle += aileronAngle < 0 ? 2 : -2;
			}
		}else{
			--aileronCooldown;
		}
		if(elevatorCooldown==0){
			if(elevatorAngle != 0){
				MFS.MFSNet.sendToAll(new ElevatorPacket(this.getEntityId(), elevatorAngle < 0, (short) 0));
				elevatorAngle += elevatorAngle < 0 ? 6 : -6;
			}
		}else{
			--elevatorCooldown;
		}
		if(rudderCooldown==0){
			if(rudderAngle != 0){
				MFS.MFSNet.sendToAll(new RudderPacket(this.getEntityId(), rudderAngle < 0, (short) 0));
				rudderAngle += rudderAngle < 0 ? 6 : -6;
			}
		}else{
			--rudderCooldown;
		}
	}
	
	public double[] getDebugForces(){
		return new double[]{this.thrustForce, this.dragForce, this.wingForce, this.gravitationalForce};
	}
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.hasFlaps=tagCompound.getBoolean("hasFlaps");
		this.aileronAngle=tagCompound.getShort("aileronAngle");
		this.elevatorAngle=tagCompound.getShort("elevatorAngle");
		this.rudderAngle=tagCompound.getShort("rudderAngle");
		this.flapAngle=tagCompound.getShort("flapAngle");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("hasFlaps", this.hasFlaps);
		tagCompound.setShort("aileronAngle", this.aileronAngle);
		tagCompound.setShort("elevatorAngle", this.elevatorAngle);
		tagCompound.setShort("rudderAngle", this.rudderAngle);
		tagCompound.setShort("flapAngle", this.flapAngle);
	}
}