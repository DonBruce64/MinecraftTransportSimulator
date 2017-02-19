package minecraftflightsimulator.entities.core;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.parts.EntityChest;
import minecraftflightsimulator.entities.parts.EntityPontoon;
import minecraftflightsimulator.entities.parts.EntityPropeller;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.minecrafthelpers.EntityHelper;
import minecraftflightsimulator.packets.control.AileronPacket;
import minecraftflightsimulator.packets.control.ElevatorPacket;
import minecraftflightsimulator.packets.control.RudderPacket;
import minecraftflightsimulator.systems.ConfigSystem;
import minecraftflightsimulator.systems.RotationSystem;
import minecraftflightsimulator.utilites.DamageSources.DamageSourcePlaneCrash;
import minecraftflightsimulator.utilites.MFSVector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

/**
 *Main base abstract class for planes.  To use it two things need to happen.
 *First, all plane variables need to be defined in {@link entitInit}
 *Next, all protected child UUID strings must be set in the constructor, otherwise errors will occur. 
 * @author don_bruce
 *
 */
public abstract class EntityPlane extends EntityVehicle{	
	//Visible plane variables
	public boolean hasFlaps;
	
	//Note that angle variable should be divided by 10 to get actual angle.
	public short aileronAngle;
	public short elevatorAngle;
	public short rudderAngle;
	public short flapAngle;
	public short aileronTrim;
	public short elevatorTrim;
	public short rudderTrim;
	
	public short aileronCooldown;
	public short elevatorCooldown;
	public short rudderCooldown;
	
	//Defined plane properties
	protected int fuelCapcacity;//1 bucket is 100 units
	protected float emptyMass;//kg
	protected float wingspan;//m
	protected float wingArea;//m^2
	protected float tailDistance;//m away from center of lift
	protected float rudderArea;//m^2
	protected float elevatorArea;//m^2
	protected float defaultElevatorAngle;//degrees	

	//Internal plane variables
	private boolean hasPontoons;
	private byte groundedWheels;
	private byte groundedCores;
	private float currentMass;
	private float addedMass;
	private float currentCOG;
	private float brakeDistance;
	private float momentRoll;
	private float momentPitch;
	private float momentYaw;
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
	
	private Entity rider;
	private AxisAlignedBB newChildBox;
	private AxisAlignedBB collidingBox;
	private MFSVector offset;
	
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
	
	@Override
	public void explodeAtPosition(double x, double y, double z){
		Entity pilot = null;
		for(EntityChild child : getChildren()){
			if(child instanceof EntitySeat){
				if(((EntitySeat) child).isController){
					if(EntityHelper.getRider(child) != null){
						pilot = EntityHelper.getRider(child);
						break;
					}
				}
			}
		}
		for(EntityChild child : getChildren()){
			if(EntityHelper.getRider(child) != null){
				if(EntityHelper.getRider(child).equals(pilot)){
					EntityHelper.getRider(child).attackEntityFrom(new DamageSourcePlaneCrash(null), (float) (ConfigSystem.getDoubleConfig("CrashDamageFactor")*velocity*20));
				}else{
					EntityHelper.getRider(child).attackEntityFrom(new DamageSourcePlaneCrash(pilot), (float) (ConfigSystem.getDoubleConfig("CrashDamageFactor")*velocity*20));
				}
			}
		}
		super.explodeAtPosition(x, y, z);
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
		//TODO set this up with physics.
		brakeDistance = groundedWheels = groundedCores = 0;
		for(EntityChild child : getChildren()){
			if(child instanceof EntityGroundDevice){
				if(!child.isDead){
					if(child.isOnGround()){
						if(child.offsetX != 0){
							brakeDistance = -child.offsetY;
							groundedWheels += child.offsetX > 0 ? 2 : 4;
							hasPontoons = child instanceof EntityPontoon;
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
			rider = EntityHelper.getRider(child);
			if(rider != null){
				if(rider instanceof EntityPlayer){
					addedMass = 100 + calculateInventoryWeight(((EntityPlayer) rider).inventory);
				}else{
					addedMass = 100;
				}
			}else if(child instanceof EntityChest){
				addedMass = calculateInventoryWeight((EntityChest) child);
			}else if(child instanceof EntityPropeller){
				addedMass = 50*(child.propertyCode%10);
			}
			currentCOG = (currentCOG*currentMass + child.offsetZ*addedMass)/(currentMass+addedMass);
			currentMass += addedMass;
		}
		momentRoll = (float) (emptyMass*(1.5F+(fuel/10000F)));
		momentPitch = 2*currentMass;
		momentYaw = 3*currentMass;
				
		currentWingArea = wingArea + wingArea*flapAngle/250F;
		updateHeadingVec();
		verticalVec = RotationSystem.getRotatedY(rotationPitch, rotationYaw, rotationRoll);
		sideVec = headingVec.cross(verticalVec);
		velocityVec.set(motionX, motionY, motionZ);
		velocity = velocityVec.dot(headingVec);
		velocityVec = velocityVec.normalize();
		
		trackAngle = Math.toDegrees(Math.atan2(velocityVec.dot(verticalVec), velocityVec.dot(headingVec)));
		dragCoeff = 0.0004F*Math.pow(trackAngle, 2) + 0.03F;
		wingLiftCoeff = getLiftCoeff(-trackAngle, 2 + flapAngle/350F);
		aileronLiftCoeff = getLiftCoeff((aileronAngle + aileronTrim)/10F, 2);
		elevatorLiftCoeff = getLiftCoeff(defaultElevatorAngle - trackAngle - (elevatorAngle + elevatorTrim)/10F, 2);
		rudderLiftCoeff = getLiftCoeff((rudderAngle + rudderTrim)/10F + Math.toDegrees(Math.atan2(velocityVec.dot(sideVec), velocityVec.dot(headingVec))), 2);
	}
	
	private void getForcesAndMotions(){
		thrustForce = thrustTorque = 0;
		for(EntityChild child : getChildren()){
			if(!child.isDead){
				if(child instanceof EntityPropeller){
					thrust = ((EntityPropeller) child).getThrustForce();
					thrustForce += thrust;
					if(!(groundedWheels == 7 && velocity == 0)){
						thrustTorque += thrust*child.offsetX;
					}
				}
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
				motionX = Math.max(motionX + (headingVec.xCoord*thrustForce - velocityVec.xCoord*(dragForce + brakeForce) + verticalVec.xCoord*(wingForce + elevatorForce))/currentMass, 0);
			}else if(motionX < 0){
				motionX = Math.min(motionX + (headingVec.xCoord*thrustForce - velocityVec.xCoord*(dragForce + brakeForce) + verticalVec.xCoord*(wingForce + elevatorForce))/currentMass, 0);
			}
			if(motionZ > 0){
				motionZ = Math.max(motionZ + (headingVec.zCoord*thrustForce - velocityVec.zCoord*(dragForce + brakeForce) + verticalVec.zCoord*(wingForce + elevatorForce))/currentMass, 0);	
			}else if(motionZ < 0){
				motionZ = Math.min(motionZ + (headingVec.zCoord*thrustForce - velocityVec.zCoord*(dragForce + brakeForce) + verticalVec.zCoord*(wingForce + elevatorForce))/currentMass, 0);
			}
		}else{
			motionX += (headingVec.xCoord*thrustForce - velocityVec.xCoord*dragForce + verticalVec.xCoord*(wingForce + elevatorForce))/currentMass;
			motionZ += (headingVec.zCoord*thrustForce - velocityVec.zCoord*dragForce + verticalVec.zCoord*(wingForce + elevatorForce))/currentMass;
		}		
				
		//TODO fix this to allow barrel rolls
		motionY += (headingVec.yCoord*thrustForce - velocityVec.yCoord*dragForce + verticalVec.yCoord*(wingForce + elevatorForce) - gravitationalForce)/currentMass;
		motionRoll = (float) (180/Math.PI*((1-headingVec.yCoord)*aileronTorque)/momentRoll);
		motionPitch = (float) (180/Math.PI*((1-Math.abs(sideVec.yCoord))*elevatorTorque - sideVec.yCoord*(thrustTorque + rudderTorque) + (1-Math.abs(headingVec.yCoord))*(gravitationalTorque + brakeTorque))/momentPitch);
		motionYaw = (float) (180/Math.PI*(headingVec.yCoord*aileronTorque - verticalVec.yCoord*(-thrustTorque - rudderTorque) + sideVec.yCoord*elevatorTorque)/momentYaw);
	}
	
	private void performGroundOperations(){
		//TODO make this physics!
		if(hasPontoons && groundedWheels >= 6){
			if(Math.abs(rotationRoll) <  5){
				rotationRoll = motionRoll = 0;
			}
			if(groundedWheels == 12){
				groundedWheels = 7;
			}
		}else if(groundedWheels >= 6 && Math.abs(rotationRoll) <  1){
			rotationRoll = motionRoll = 0;
		}
		if(groundedWheels == 7){
			if(motionY<0){motionY=0;}
			if(motionPitch*currentCOG > 0 ){motionPitch = 0;}
			if(Math.abs(rotationPitch) < 0.25){rotationPitch = 0;}
			motionYaw += 7*velocityVec.dot(sideVec) + rudderAngle/(350*(0.5 + velocity*velocity));
			updateHeadingVec();
			double groundSpeed = Math.hypot(motionX, motionZ);
			MFSVector groundVec = headingVec.add(0, -headingVec.yCoord, 0).normalize();
			motionX = groundVec.xCoord * groundSpeed;
			motionZ = groundVec.zCoord * groundSpeed;
		}
	}
	
	private void adjustXZMovement(){
		for(EntityChild child : getChildren()){
			xCollisionDepth = 0;
			zCollisionDepth = 0;
			newChildBox = child.getBoundingBox().getOffsetBoundingBox(motionX*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), 0, 0);
			collidingBoxes = this.getChildCollisions(child, newChildBox);
			for(int i=0; i < collidingBoxes.size(); ++i){
				collidingBox = collidingBoxes.get(i);
				if(newChildBox.maxX > collidingBox.minX && newChildBox.maxX < collidingBox.maxX){
					xCollisionDepth = Math.min(collidingBox.minX - newChildBox.maxX, xCollisionDepth);
				}else if(newChildBox.minX < collidingBox.maxX && newChildBox.minX > collidingBox.minX){
					xCollisionDepth = Math.max(collidingBox.maxX - newChildBox.minX, xCollisionDepth);
				}
			}
			
			newChildBox = child.getBoundingBox().getOffsetBoundingBox(0, 0, motionZ*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"));
			collidingBoxes = this.getChildCollisions(child, newChildBox);	
			for(int i=0; i < collidingBoxes.size(); ++i){
				collidingBox = collidingBoxes.get(i);
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
							removeChild(child.UUID, true);
						}
					}else{
						this.requestDataFromServer();
					}
					continue;
				}
				if(motionX > 0){
					motionX = Math.max(motionX + xCollisionDepth/ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), 0);
				}else{
					motionX = Math.min(motionX + xCollisionDepth/ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), 0);
				}
				if(motionZ > 0){
					motionZ = Math.max(motionZ + zCollisionDepth/ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), 0);
				}else{
					motionZ = Math.min(motionZ + zCollisionDepth/ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), 0);
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
				offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch, rotationYaw + motionYaw, rotationRoll);
				newChildBox = child.getBoundingBox().getOffsetBoundingBox(posX + offset.xCoord - child.posX + motionX*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), 0, posZ + offset.zCoord - child.posZ + motionZ*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"));
				child.isCollidedHorizontally = !this.getChildCollisions(child, newChildBox).isEmpty();
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
				offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
				offset = offset.add(posX - child.posX + motionX*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), posY - child.posY + motionY*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), posZ - child.posZ + motionZ*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"));
				if(!this.getChildCollisions(child, child.getBoundingBox().getOffsetBoundingBox(offset.xCoord, offset.yCoord, offset.zCoord)).isEmpty()){
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
				offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);				
				offset = offset.add(posX - child.posX + motionX*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), posY - child.posY + motionY*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), posZ - child.posZ + motionZ*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"));				
				if(!this.getChildCollisions(child, child.getBoundingBox().getOffsetBoundingBox(offset.xCoord, offset.yCoord, offset.zCoord)).isEmpty()){
					if(child.offsetZ != 0){
						prevPitchChildOffset = pitchChildOffset;
						pitchChildOffset = child.offsetZ;
						if(prevPitchChildOffset != 0 && (prevPitchChildOffset * pitchChildOffset < 0)){
							if((motionY += 0.1) > (hasPontoons ? 0.1 : 0)){
								pitchChildOffset = motionPitch = 0;
								motionY = hasPontoons ? 0.1 : 0;
								return;
							}else{
								break;
							}
						}else{
							motionPitch += pitchChildOffset > 0 ? -0.1 : 0.1;
							if(Math.abs(motionPitch) > 15){
								pitchChildOffset = 0;
								break;
							}
						}
					}
				}
			}
		}while(pitchChildOffset != 0);
		
		for(EntityChild child : getChildren()){
			yCollisionDepth = 0;
			offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
			newChildBox = child.getBoundingBox().getOffsetBoundingBox(posX + offset.xCoord - child.posX + motionX*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), posY + offset.yCoord - child.posY + motionY*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), posZ + offset.zCoord - child.posZ + motionZ*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"));
			collidingBoxes = this.getChildCollisions(child, newChildBox);
			for(int i=0; i < collidingBoxes.size(); ++i){
				collidingBox = collidingBoxes.get(i);
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
							removeChild(child.UUID, true);
						}
					}else{
						this.requestDataFromServer();
					}
					continue;
				}
				motionY += yCollisionDepth/ConfigSystem.getDoubleConfig("PlaneSpeedFactor");
			}
		}
	}
		
	private void movePlane(){
		rotationRoll += motionRoll;
		rotationPitch = (motionPitch + rotationPitch)%360;
		rotationYaw += motionYaw;
		setPosition(posX + motionX*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), posY + motionY*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"), posZ + motionZ*ConfigSystem.getDoubleConfig("PlaneSpeedFactor"));
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
		this.aileronTrim=tagCompound.getShort("aileronTrim");
		this.elevatorTrim=tagCompound.getShort("elevatorTrim");
		this.rudderTrim=tagCompound.getShort("rudderTrim");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("hasFlaps", this.hasFlaps);
		tagCompound.setShort("aileronAngle", this.aileronAngle);
		tagCompound.setShort("elevatorAngle", this.elevatorAngle);
		tagCompound.setShort("rudderAngle", this.rudderAngle);
		tagCompound.setShort("flapAngle", this.flapAngle);
		tagCompound.setShort("aileronTrim", this.aileronTrim);
		tagCompound.setShort("elevatorTrim", this.elevatorTrim);
		tagCompound.setShort("rudderTrim", this.rudderTrim);
	}
}
