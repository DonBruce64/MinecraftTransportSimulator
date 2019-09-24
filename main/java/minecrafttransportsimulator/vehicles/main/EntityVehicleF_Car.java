package minecrafttransportsimulator.vehicles.main;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.control.SteeringPacket;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.PartEngineCar;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;


public final class EntityVehicleF_Car extends EntityVehicleE_Powered{	
	//Note that angle variable should be divided by 10 to get actual angle.
	public short steeringAngle;
	public short steeringCooldown;
	public List<APartGroundDevice> wheels = new ArrayList<APartGroundDevice>();
	public List<APartGroundDevice> groundedWheels = new ArrayList<APartGroundDevice>();
	
	//Internal car variables
	private double wheelForce;//kg*m/ticks^2
	private double dragForce;//kg*m/ticks^2
	private double gravitationalForce;//kg*m/ticks^2
	
	public EntityVehicleF_Car(World world){
		super(world);
	}
	
	public EntityVehicleF_Car(World world, float posX, float posY, float posZ, float rotation, String vehicleName){
		super(world, posX, posY, posZ, rotation, vehicleName);
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(pack != null){
			//Populate grounded wheels.  Needs to be independent of non-wheeled ground devices.
			groundedWheels.clear();
			for(APartGroundDevice wheel : this.wheels){
				if(wheel.isOnGround()){
					groundedWheels.add(wheel);
				}
			}
		}
	}
	
	@Override
	protected void getBasicProperties(){
		velocityVec = new Vec3d(motionX, motionY, motionZ);
		velocity = velocityVec.dotProduct(headingVec);
		velocityVec = velocityVec.normalize();
		
		//Turn on brake/indicator and backup lights if they are activated.
		changeLightStatus(LightTypes.BRAKELIGHT, brakeOn);
		changeLightStatus(LightTypes.LEFTINDICATORLIGHT, brakeOn && !this.isLightOn(LightTypes.LEFTTURNLIGHT));
		changeLightStatus(LightTypes.RIGHTINDICATORLIGHT, brakeOn && !this.isLightOn(LightTypes.RIGHTTURNLIGHT));
		changeLightStatus(LightTypes.BACKUPLIGHT, getEngineByNumber((byte) 0) != null && ((PartEngineCar) getEngineByNumber((byte) 0)).getGearshiftRotation() < 0);
	}
	
	@Override
	protected void getForcesAndMotions(){
		if(getEngineByNumber((byte) 0) != null){
			wheelForce = getEngineByNumber((byte) 0).getForceOutput();
		}else{
			wheelForce = 0;
		}
		
		dragForce = 0.5F*airDensity*velocity*velocity*5.0F*pack.car.dragCoefficient;
		gravitationalForce = currentMass*(9.8/400);
				
		motionX += (headingVec.x*wheelForce - velocityVec.x*dragForce)/currentMass;
		motionZ += (headingVec.z*wheelForce - velocityVec.z*dragForce)/currentMass;
		motionY += (headingVec.y*wheelForce - velocityVec.y*dragForce - gravitationalForce)/currentMass;
		
		motionYaw = 0;
		motionPitch = 0;
		motionRoll = 0;
	}
	
	@Override
	protected void dampenControlSurfaces(){
		if(steeringCooldown==0){
			if(steeringAngle != 0){
				MTS.MTSNet.sendToAll(new SteeringPacket(this.getEntityId(), steeringAngle < 0, (short) 0));
				steeringAngle += steeringAngle < 0 ? 20 : -20;
			}
		}else{
			--steeringCooldown;
		}
	}
	
	
	@Override
	public void addPart(APart part, boolean ignoreCollision){
		super.addPart(part, ignoreCollision);
		if(part instanceof APartGroundDevice){
			if(((APartGroundDevice) part).canBeDrivenByEngine()){
				wheels.add((APartGroundDevice) part);
			}
		}
	}
	
	@Override
	public void removePart(APart part, boolean playBreakSound){
		super.removePart(part, playBreakSound);
		if(wheels.contains(part)){
			wheels.remove(part);
		}
	}
	
	@Override
	public float getSteerAngle(){
		return -steeringAngle/10F;
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