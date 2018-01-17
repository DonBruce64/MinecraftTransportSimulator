package minecrafttransportsimulator.entities.main;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.parts.EntityEngine;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.guis.GUIPanelAircraft;
import minecrafttransportsimulator.packets.control.AileronPacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;


public class EntityCar extends EntityMultipartVehicle{	
	//Note that angle variable should be divided by 10 to get actual angle.
	public short steeringAngle;
	
	public short steeringCooldown;
	
	//Internal car variables
	private float momentPitch;
	private double engineRPM;
	private double currentWheelSpeed;
	private double desiredWheelSpeed;
	
	private double wheelForce;//kg*m/ticks^2
	private double dragForce;//kg*m/ticks^2
	private double gravitationalForce;//kg*m/ticks^2
	private double gravitationalTorque;//kg*m^2/ticks^2
	
	public EntityCar(World world){
		super(world);
	}
	
	public EntityCar(World world, float posX, float posY, float posZ, float rotation, String name){
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
			moveCar();
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
		return -steeringAngle/10F;
	}
	
	private void getBasicProperties(){
		momentPitch = (float) (2*currentMass);
		velocityVec.set(motionX, motionY, motionZ);
		velocity = velocityVec.dot(headingVec);
		velocityVec = velocityVec.normalize();
	}
	
	private void getForcesAndMotions(){
		wheelForce = 0;
		for(EntityMultipartChild child : getChildren()){
			if(!child.isDead){
				if(child instanceof EntityEngine){
					engineRPM = ((EntityEngine) child).RPM;
					break;
				}
			}
		}
		//TODO either this needs to be done in the engine, or here.  Decide where to do wheel rotation NOW!
		for(EntityMultipartChild child : getChildren()){
			if(!child.isDead){
				if(child instanceof EntityWheel){
					if(!child.turnsWithSteer || pack.car.is4WD){
						EntityWheel wheel = ((EntityWheel) child);
						currentWheelSpeed = wheel.angularVelocity*wheel.wheelDiameter/2F;
						desiredWheelSpeed = engineRPM;
					}
				}
			}
		}
		dragForce = 0.5F*airDensity*velocity*velocity*0.75F*pack.car.dragCoefficient;		
		gravitationalForce = currentMass*(9.8/400);
		gravitationalTorque = gravitationalForce*1;
				
		motionX += (headingVec.xCoord*wheelForce - velocityVec.xCoord*dragForce)/currentMass;
		motionZ += (headingVec.zCoord*wheelForce - velocityVec.zCoord*dragForce)/currentMass;
		motionY += (headingVec.yCoord*wheelForce - velocityVec.yCoord*dragForce - gravitationalForce)/currentMass;
		
		motionPitch = (float) (((1-Math.abs(headingVec.yCoord))*gravitationalTorque)/momentPitch);
	}
		
	private void moveCar(){
		rotationPitch = (motionPitch + rotationPitch);
		setPosition(posX + motionX*ConfigSystem.getDoubleConfig("SpeedFactor"), posY + motionY*ConfigSystem.getDoubleConfig("SpeedFactor"), posZ + motionZ*ConfigSystem.getDoubleConfig("SpeedFactor"));
	}

	private void dampenControlSurfaces(){
		if(steeringCooldown==0){
			if(steeringAngle != 0){
				//TODO make new packet.
				MTS.MTSNet.sendToAll(new AileronPacket(this.getEntityId(), steeringAngle < 0, (short) 0));
				steeringAngle += steeringAngle < 0 ? 4 : -4;
			}
		}else{
			--steeringAngle;
		}
	}

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