package minecrafttransportsimulator.entities.main;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.parts.EntityPropeller;
import minecrafttransportsimulator.guis.GUIPanelAircraft;
import minecrafttransportsimulator.packets.control.AileronPacket;
import minecrafttransportsimulator.packets.control.ElevatorPacket;
import minecrafttransportsimulator.packets.control.RudderPacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;


public class EntityPlane extends EntityMultipartVehicle{	
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
	
	public MTSVector verticalVec = new MTSVector(0, 0, 0);
	public MTSVector sideVec = new MTSVector(0, 0, 0);

	//Internal plane variables
	private float momentRoll;
	private float momentPitch;
	private float momentYaw;
	private double currentWingArea;
	private double dragCoeff;
	private double wingLiftCoeff;
	private double aileronLiftCoeff;
	private double elevatorLiftCoeff;
	private double rudderLiftCoeff;
	private double thrust;
	
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
	private MTSVector offset;
	
	private List<AxisAlignedBB> collidingBoxes = new ArrayList<AxisAlignedBB>();
	
	public EntityPlane(World world){
		super(world);
	}
	
	public EntityPlane(World world, float posX, float posY, float posZ, float rotation, String name){
		super(world, posX, posY, posZ, rotation, name);
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(linked){
			getBasicProperties();
			getForcesAndMotions();
			performGroundOperations();
			checkPlannedMovement();
			movePlane();
			moveChildren();
			if(!worldObj.isRemote){
				dampenControlSurfaces();
			}
		}
	}

	@Override
	public Instruments getBlankInstrument(){
		return Instruments.AIRCRAFT_BLANK;
	}
	
	@Override
	public GuiScreen getPanel(){
		return new GUIPanelAircraft(this);
	}
	
	@Override
	public float getSteerAngle(){
		return -rudderAngle/10F;
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
	
	private void getBasicProperties(){
		momentRoll = (float) (pack.general.emptyMass*(1.5F+(fuel/10000F)));
		momentPitch = (float) (2*currentMass);
		momentYaw = (float) (3*currentMass);
		currentWingArea = pack.plane.wingArea + pack.plane.wingArea*flapAngle/250F;
		
		verticalVec = RotationSystem.getRotatedY(rotationPitch, rotationYaw, rotationRoll);
		sideVec = headingVec.cross(verticalVec);
		velocityVec.set(motionX, motionY, motionZ);
		velocity = velocityVec.dot(headingVec);
		velocityVec = velocityVec.normalize();
		
		trackAngle = Math.toDegrees(Math.atan2(velocityVec.dot(verticalVec), velocityVec.dot(headingVec)));
		dragCoeff = 0.0004F*Math.pow(trackAngle, 2) + 0.03F;
		wingLiftCoeff = getLiftCoeff(-trackAngle, 2 + flapAngle/350F);
		aileronLiftCoeff = getLiftCoeff((aileronAngle + aileronTrim)/10F, 2);
		elevatorLiftCoeff = getLiftCoeff(pack.plane.defaultElevatorAngle - trackAngle - (elevatorAngle + elevatorTrim)/10F, 2);
		rudderLiftCoeff = getLiftCoeff((rudderAngle + rudderTrim)/10F + Math.toDegrees(Math.atan2(velocityVec.dot(sideVec), velocityVec.dot(headingVec))), 2);
	}
	
	private void getForcesAndMotions(){
		thrustForce = thrustTorque = 0;
		for(EntityMultipartChild child : getChildren()){
			if(!child.isDead){
				if(child instanceof EntityPropeller){
					thrust = ((EntityPropeller) child).getThrustForce();
					thrustForce += thrust;
					thrustTorque += thrust*child.offsetX;
				}
			}
		}
		
		dragForce = 0.5F*airDensity*velocity*velocity*currentWingArea*(dragCoeff + wingLiftCoeff*wingLiftCoeff/(Math.PI*pack.plane.wingSpan*pack.plane.wingSpan/currentWingArea*0.8));		
		wingForce = 0.5F*airDensity*velocity*velocity*currentWingArea*wingLiftCoeff;
		aileronForce = 0.5F*airDensity*velocity*velocity*pack.plane.wingArea/5*aileronLiftCoeff;
		elevatorForce = 0.5F*airDensity*velocity*velocity*pack.plane.elevatorArea*elevatorLiftCoeff;			
		rudderForce = 0.5F*airDensity*velocity*velocity*pack.plane.rudderArea*rudderLiftCoeff;
		gravitationalForce = currentMass*(9.8/400);
					
		aileronTorque = 2*aileronForce*pack.plane.wingSpan*0.3;
		elevatorTorque = elevatorForce*pack.plane.tailDistance;
		rudderTorque = rudderForce*pack.plane.tailDistance;
		gravitationalTorque = gravitationalForce*1;
				
		//TODO fix this to allow barrel rolls
		motionX += (headingVec.xCoord*thrustForce - velocityVec.xCoord*dragForce + verticalVec.xCoord*(wingForce + elevatorForce))/currentMass;
		motionZ += (headingVec.zCoord*thrustForce - velocityVec.zCoord*dragForce + verticalVec.zCoord*(wingForce + elevatorForce))/currentMass;
		motionY += (headingVec.yCoord*thrustForce - velocityVec.yCoord*dragForce + verticalVec.yCoord*(wingForce + elevatorForce) - gravitationalForce)/currentMass;
		
		motionRoll = (float) (180/Math.PI*((1-headingVec.yCoord)*aileronTorque)/momentRoll);
		motionPitch = (float) (180/Math.PI*((1-Math.abs(sideVec.yCoord))*elevatorTorque - sideVec.yCoord*(thrustTorque + rudderTorque) + (1-Math.abs(headingVec.yCoord))*gravitationalTorque)/momentPitch);
		motionYaw = (float) (180/Math.PI*(headingVec.yCoord*aileronTorque - verticalVec.yCoord*(-thrustTorque - rudderTorque) + sideVec.yCoord*elevatorTorque)/momentYaw);
	}
	/*
	private void adjustXZMovement(){
		for(EntityMultipartChild child : getChildren()){
			xCollisionDepth = 0;
			zCollisionDepth = 0;
			newChildBox = EntityHelper.getOffsetBoundingBox(child.getEntityBoundingBox(), motionX*ConfigSystem.getDoubleConfig("SpeedFactor"), 0, 0);
			this.getChildCollisions(child, newChildBox, collidingBoxes);
			for(int i=0; i < collidingBoxes.size(); ++i){
				collidingBox = collidingBoxes.get(i);
				if(newChildBox.maxX > collidingBox.minX && newChildBox.maxX < collidingBox.maxX){
					xCollisionDepth = Math.min(collidingBox.minX - newChildBox.maxX, xCollisionDepth);
				}else if(newChildBox.minX < collidingBox.maxX && newChildBox.minX > collidingBox.minX){
					xCollisionDepth = Math.max(collidingBox.maxX - newChildBox.minX, xCollisionDepth);
				}
			}
			
			newChildBox = EntityHelper.getOffsetBoundingBox(child.getEntityBoundingBox(), 0, 0, motionZ*ConfigSystem.getDoubleConfig("SpeedFactor"));
			this.getChildCollisions(child, newChildBox, collidingBoxes);	
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
					motionX = Math.max(motionX + xCollisionDepth/ConfigSystem.getDoubleConfig("SpeedFactor"), 0);
				}else{
					motionX = Math.min(motionX + xCollisionDepth/ConfigSystem.getDoubleConfig("SpeedFactor"), 0);
				}
				if(motionZ > 0){
					motionZ = Math.max(motionZ + zCollisionDepth/ConfigSystem.getDoubleConfig("SpeedFactor"), 0);
				}else{
					motionZ = Math.min(motionZ + zCollisionDepth/ConfigSystem.getDoubleConfig("SpeedFactor"), 0);
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
			for(EntityMultipartChild child : getChildren()){				
				offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch, rotationYaw + motionYaw, rotationRoll);
				newChildBox = EntityHelper.getOffsetBoundingBox(child.getEntityBoundingBox(), posX + offset.xCoord - child.posX + motionX*ConfigSystem.getDoubleConfig("SpeedFactor"), 0, posZ + offset.zCoord - child.posZ + motionZ*ConfigSystem.getDoubleConfig("SpeedFactor"));
				this.getChildCollisions(child, newChildBox, collidingBoxes);
				child.isCollidedHorizontally = !collidingBoxes.isEmpty();
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
			for(EntityMultipartChild child : getChildren()){
				offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
				offset = offset.add(posX - child.posX + motionX*ConfigSystem.getDoubleConfig("SpeedFactor"), posY - child.posY + motionY*ConfigSystem.getDoubleConfig("SpeedFactor"), posZ - child.posZ + motionZ*ConfigSystem.getDoubleConfig("SpeedFactor"));
				this.getChildCollisions(child, EntityHelper.getOffsetBoundingBox(child.getEntityBoundingBox(), offset.xCoord, offset.yCoord, offset.zCoord), collidingBoxes);
				if(!collidingBoxes.isEmpty()){
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
			for(EntityMultipartChild child : getChildren()){
				offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);				
				offset = offset.add(posX - child.posX + motionX*ConfigSystem.getDoubleConfig("SpeedFactor"), posY - child.posY + motionY*ConfigSystem.getDoubleConfig("SpeedFactor"), posZ - child.posZ + motionZ*ConfigSystem.getDoubleConfig("SpeedFactor"));
				this.getChildCollisions(child, EntityHelper.getOffsetBoundingBox(child.getEntityBoundingBox(), offset.xCoord, offset.yCoord, offset.zCoord), collidingBoxes);
				if(!collidingBoxes.isEmpty()){
					if(child.offsetZ != 0){
						prevPitchChildOffset = pitchChildOffset;
						pitchChildOffset = child.offsetZ;
						if(prevPitchChildOffset != 0 && (prevPitchChildOffset * pitchChildOffset < 0)){
							if((motionY += 0.1) > 0){
								pitchChildOffset = motionPitch = 0;
								motionY = 0;
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
		
		for(EntityMultipartChild child : getChildren()){
			yCollisionDepth = 0;
			offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch + motionPitch, rotationYaw + motionYaw, rotationRoll + motionRoll);
			newChildBox = EntityHelper.getOffsetBoundingBox(child.getEntityBoundingBox(), posX + offset.xCoord - child.posX + motionX*ConfigSystem.getDoubleConfig("SpeedFactor"), posY + offset.yCoord - child.posY + motionY*ConfigSystem.getDoubleConfig("SpeedFactor"), posZ + offset.zCoord - child.posZ + motionZ*ConfigSystem.getDoubleConfig("SpeedFactor"));
			this.getChildCollisions(child, newChildBox, collidingBoxes);
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
				motionY += yCollisionDepth/ConfigSystem.getDoubleConfig("SpeedFactor");
			}
		}
	}*/
		
	private void movePlane(){
		rotationRoll = (motionRoll + rotationRoll)%360;
		rotationPitch = (motionPitch + rotationPitch)%360;
		rotationYaw = (motionYaw + rotationYaw)%360;
		setPosition(posX + motionX*ConfigSystem.getDoubleConfig("SpeedFactor"), posY + motionY*ConfigSystem.getDoubleConfig("SpeedFactor"), posZ + motionZ*ConfigSystem.getDoubleConfig("SpeedFactor"));
	}

	private void dampenControlSurfaces(){
		if(aileronCooldown==0){
			if(aileronAngle != 0){
				MTS.MTSNet.sendToAll(new AileronPacket(this.getEntityId(), aileronAngle < 0, (short) 0));
				aileronAngle += aileronAngle < 0 ? 2 : -2;
			}
		}else{
			--aileronCooldown;
		}
		if(elevatorCooldown==0){
			if(elevatorAngle != 0){
				MTS.MTSNet.sendToAll(new ElevatorPacket(this.getEntityId(), elevatorAngle < 0, (short) 0));
				elevatorAngle += elevatorAngle < 0 ? 6 : -6;
			}
		}else{
			--elevatorCooldown;
		}
		if(rudderCooldown==0){
			if(rudderAngle != 0){
				MTS.MTSNet.sendToAll(new RudderPacket(this.getEntityId(), rudderAngle < 0, (short) 0));
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
		this.aileronAngle=tagCompound.getShort("aileronAngle");
		this.elevatorAngle=tagCompound.getShort("elevatorAngle");
		this.rudderAngle=tagCompound.getShort("rudderAngle");
		this.flapAngle=tagCompound.getShort("flapAngle");
		this.aileronTrim=tagCompound.getShort("aileronTrim");
		this.elevatorTrim=tagCompound.getShort("elevatorTrim");
		this.rudderTrim=tagCompound.getShort("rudderTrim");
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setShort("aileronAngle", this.aileronAngle);
		tagCompound.setShort("elevatorAngle", this.elevatorAngle);
		tagCompound.setShort("rudderAngle", this.rudderAngle);
		tagCompound.setShort("flapAngle", this.flapAngle);
		tagCompound.setShort("aileronTrim", this.aileronTrim);
		tagCompound.setShort("elevatorTrim", this.elevatorTrim);
		tagCompound.setShort("rudderTrim", this.rudderTrim);
		return tagCompound;
	}
}