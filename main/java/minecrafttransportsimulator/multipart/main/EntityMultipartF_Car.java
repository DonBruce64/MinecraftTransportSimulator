package minecrafttransportsimulator.multipart.main;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.multipart.parts.PartEngineCar;
import minecrafttransportsimulator.multipart.parts.PartGroundDevice;
import minecrafttransportsimulator.packets.control.SteeringPacket;
import minecrafttransportsimulator.sounds.AttenuatedSound;
import minecrafttransportsimulator.systems.SFXSystem.SoundPart;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public final class EntityMultipartF_Car extends EntityMultipartE_Vehicle implements SoundPart{	
	public boolean hornOn;
	//Note that angle variable should be divided by 10 to get actual angle.
	public short steeringAngle;
	public short steeringCooldown;
	public List<PartGroundDevice> wheels = new ArrayList<PartGroundDevice>();
	
	//Internal car variables
	private float momentPitch;
	private double wheelForce;//kg*m/ticks^2
	private double dragForce;//kg*m/ticks^2
	private double gravitationalForce;//kg*m/ticks^2
	private double gravitationalTorque;//kg*m^2/ticks^2
	private PartEngineCar engine;
	private AttenuatedSound hornSound;
	
	public EntityMultipartF_Car(World world){
		super(world);
	}
	
	public EntityMultipartF_Car(World world, float posX, float posY, float posZ, float rotation, String name){
		super(world, posX, posY, posZ, rotation, name);
	}
	
	@Override
	protected void getBasicProperties(){
		momentPitch = (float) (2*currentMass);
		velocityVec = new Vec3d(motionX, motionY, motionZ);
		velocity = velocityVec.dotProduct(headingVec);
		velocityVec = velocityVec.normalize();
		
		//Turn on brake/indicator and backup lights if they are activated.
		changeLightStatus(LightTypes.BRAKELIGHT, brakeOn);
		changeLightStatus(LightTypes.LEFTINDICATORLIGHT, brakeOn && !this.isLightOn(LightTypes.LEFTTURNLIGHT));
		changeLightStatus(LightTypes.RIGHTINDICATORLIGHT, brakeOn && !this.isLightOn(LightTypes.RIGHTTURNLIGHT));
		changeLightStatus(LightTypes.BACKUPLIGHT, this.engine != null && this.engine.currentGear < 0);
	}
	
	@Override
	protected void getForcesAndMotions(){
		if(engine != null){
			wheelForce = engine.getForceOutput();
		}else{
			wheelForce = 0;
		}
		
		dragForce = 0.5F*airDensity*velocity*velocity*5.0F*pack.car.dragCoefficient;
		gravitationalForce = currentMass*(9.8/400);
		gravitationalTorque = gravitationalForce*1;
				
		motionX += (headingVec.xCoord*wheelForce - velocityVec.xCoord*dragForce)/currentMass;
		motionZ += (headingVec.zCoord*wheelForce - velocityVec.zCoord*dragForce)/currentMass;
		motionY += (headingVec.yCoord*wheelForce - velocityVec.yCoord*dragForce - gravitationalForce)/currentMass;
		
		motionYaw = 0;
		motionPitch = (float) (((1-Math.abs(headingVec.yCoord))*gravitationalTorque)/momentPitch);
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
	public void addPart(APart part){
		super.addPart(part);
		if(part instanceof PartGroundDevice){
			if(((PartGroundDevice) part).pack.groundDevice.rotatesOnShaft){
				wheels.add((PartGroundDevice) part);
			}
		}else if(part instanceof PartEngineCar){
			engine = (PartEngineCar) part;
		}
	}
	
	@Override
	public void removePart(APart part, boolean playBreakSound){
		super.removePart(part, playBreakSound);
		if(wheels.contains(part)){
			wheels.remove(part);
		}
		if(part.equals(engine)){
			engine = null;
		}
	}

	@Override
	public Instruments getBlankInstrument(){
		return Instruments.AIRCRAFT_BLANK;
	}
	
	@Override
	public float getSteerAngle(){
		return -steeringAngle/10F;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public MovingSound getNewSound(){
		return new AttenuatedSound(pack.car.hornSound, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSoundBePlaying(){
		return hornOn && !isDead;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Vec3d getSoundPosition(){
		return this.getPositionVector();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Vec3d getSoundMotion(){
		return new Vec3d(this.motionX, this.motionY, this.motionZ);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getSoundVolume(){
		return 5.0F;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getSoundPitch(){
		return 1.0F;
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