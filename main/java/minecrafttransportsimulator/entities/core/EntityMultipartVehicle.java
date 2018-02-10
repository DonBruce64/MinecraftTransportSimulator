package minecrafttransportsimulator.entities.core;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackBeacon;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackInstrument;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackLight;
import minecrafttransportsimulator.dataclasses.MTSPackObject.PackPart;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.parts.EntityEngine;
import minecrafttransportsimulator.systems.PackParserSystem.MultipartTypes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**This class is tailored for moving vehicles such as planes, helicopters, and automobiles.
 * Contains numerous methods for gauges, HUDs, and fuel systems.
 * Essentially, if it has parts and an engine, use this.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartVehicle extends EntityMultipartMoving{
	public byte throttle;
	public int lightStatus;
	public double fuel;
	public double electricPower = 12;
	public double electricUsage;
	public double electricFlow;
	public double airDensity;
	public MTSVector velocityVec = new MTSVector(0, 0, 0);
	public MTSVector headingVec = new MTSVector(0, 0, 0);
	
	private byte numberEngineBays = 0;
	private final Map<Byte, EntityEngine> engineByNumber = new HashMap<Byte, EntityEngine>();
	private final Map<Byte, Instruments> instruments = new HashMap<Byte, Instruments>();
	
	public EntityMultipartVehicle(World world){
		super(world);
	}
	
	public EntityMultipartVehicle(World world, float posX, float posY, float posZ, float playerRotation, String name){
		super(world, posX, posY, posZ, playerRotation, name);
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(linked){
			updateHeadingVec();
			airDensity = 1.225*Math.pow(2, -posY/500);
			if(fuel < 0){fuel = 0;}
			if(electricPower > 2){
				for(PackLight light : pack.rendering.lights){
					if((lightStatus>>(light.switchNumber-1) & 1) == 1){
						electricUsage += light.beamDiameter/5F*light.beamDistance/15F*0.002F;
					}
				}
				for(PackBeacon beacon : pack.rendering.beacons){
					if((lightStatus>>(beacon.switchNumber-1) & 1) == 1){
						electricUsage += 0.00001F;
					}
				}
			}
			electricPower = Math.max(0, Math.min(13, electricPower -= electricUsage));
			electricFlow = electricUsage;
			electricUsage = 0;
		}
	}

	@Override
	public void setDead(){
		if(!worldObj.isRemote){
			for(Instruments instrument : this.instruments.values()){
				if(!instrument.equals(this.getBlankInstrument())){
					ItemStack stack = new ItemStack(MTSRegistry.instrument, 1, instrument.ordinal());
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, stack));
				}
			}
		}
		super.setDead();
	}
	
	@Override
	protected float getExplosionStrength(){
		return (float) (fuel/1000 + 1F);
	}
	
	@Override
	protected float getCurrentMass(){
		return (float) (super.getCurrentMass() + this.fuel/50);
	}
	
	protected void performGroundOperations(){
		float brakingFactor = getBrakingForceFactor();
		if(brakingFactor > 0){
			double groundSpeed = Math.hypot(motionX, motionZ);	
			groundSpeed -= 20F*brakingFactor/currentMass;
			if(groundSpeed > 0.1){
				reAdjustGroundSpeed(groundSpeed);
			}else{
				motionX = 0;
				motionZ = 0;
				motionYaw = 0;
			}
		}
		
		float skiddingFactor = getSkiddingFactor();
		if(skiddingFactor != 0 && (motionX != 0 || motionZ !=0)){
			MTSVector groundVelocityVec = new MTSVector(motionX, 0, motionZ).normalize();
			MTSVector groundHeadingVec = new MTSVector(headingVec.xCoord, 0, headingVec.zCoord).normalize();
			if(groundVelocityVec.distanceTo(groundHeadingVec) > 0.001){
				//Technically not correct, but close enough!
				float yawDeviationAngle = (float) (Math.toDegrees(Math.acos(groundVelocityVec.dot(groundHeadingVec)))*Math.signum(groundVelocityVec.cross(groundHeadingVec).yCoord));
				float yawCorrection = (float) Math.min(Math.abs(yawDeviationAngle), skiddingFactor)*Math.signum(yawDeviationAngle);
				//Now that we know how much yaw to correct for, adjust the velocity accordingly.
				float yawTemp = rotationYaw;
				rotationYaw = rotationYaw - yawDeviationAngle + yawCorrection;
				updateHeadingVec();
				reAdjustGroundSpeed(Math.hypot(motionX, motionZ));
				rotationYaw = yawTemp;
			}
		}
		
		motionYaw += getTurningFactor();
	}
	
	private void updateHeadingVec(){
        double f1 = Math.cos(-this.rotationYaw * 0.017453292F - (float)Math.PI);
        double f2 = Math.sin(-this.rotationYaw * 0.017453292F - (float)Math.PI);
        double f3 = -Math.cos(-this.rotationPitch * 0.017453292F);
        double f4 = Math.sin(-this.rotationPitch * 0.017453292F);
        headingVec.set((f2 * f3), f4, (f1 * f3));
   	}
	
	private void reAdjustGroundSpeed(double groundSpeed){
		MTSVector groundVec = new MTSVector(headingVec.xCoord, 0, headingVec.zCoord).normalize();
		motionX = groundVec.xCoord * groundSpeed;
		motionZ = groundVec.zCoord * groundSpeed;
	}
	
	/**
	 * Handles engine packets.
	 * 0 is magneto off.
	 * 1 is magneto on.
	 * 2 is electric starter off.
	 * 3 is electric starter on.
	 * 4 is hand starter on.
	 * 5 is a backfire from a high-hour engine.
	 * 6 is start.
	 * 7 is out of fuel.
	 * 8 is stalled due to low RPM.
	 * 9 is drown.
	 */
	public void handleEngineSignal(EntityEngine engine, byte signal){
		switch (signal){
			case 0: engine.setMagnetoStatus(false); break;
			case 1: engine.setMagnetoStatus(true); break;
			case 2: engine.setElectricStarterStatus(false); break;
			case 3: engine.setElectricStarterStatus(true); break;
			case 4: engine.handStartEngine(); break;
			case 5: engine.backfireEngine(); break;
			case 6: engine.startEngine(); break;
			default: engine.stallEngine((byte) (signal - 6)); break;
		}
	}
	
	/**
	 * Gets the number of bays available for engines.
	 * Cached for efficiency.
	 */
	public byte getNumberEngineBays(){
		if(numberEngineBays == 0){
			for(PackPart part : pack.parts){
				for(String name : part.names){
					if(name.contains("engine")){
						++numberEngineBays;
					}
				}
			}
		}
		return numberEngineBays;
	}
	
	/**
	 * Gets the 'numbered' engine.
	 * Cached for efficiency.
	 */
	public EntityEngine getEngineByNumber(byte number){
		if(engineByNumber.containsKey(number)){
			//If the engine isn't null, it must either be present or just been killed.
			if(engineByNumber.get(number) != null){
				if(engineByNumber.get(number).isDead){
					engineByNumber.put(number, null);
				}else{
					return engineByNumber.get(number);
				}
			}
		}
		//The only way to get here is if an engine in the map is null, or the map isn't populated.
		//Re-populate it and return the engine.
		//Because parts is a list, the #1 engine will always come before the #2 engine.
		byte engineNumber = 1;
		for(PackPart part : pack.parts){
			for(String name : part.names){
				if(name.contains("engine")){
					engineByNumber.put(engineNumber, null);
					for(EntityMultipartChild child : this.getChildren()){
						if(child instanceof EntityEngine){
							if(child.offsetX == part.pos[0] && child.offsetY == part.pos[1] && child.offsetZ == part.pos[2]){
								engineByNumber.put(engineNumber, (EntityEngine) child);
							}
						}
					}
					++engineNumber;
				}
			}
		}
		return engineByNumber.get(number);
	}
	
	public Instruments getInstrumentNumber(byte number){
		return instruments.containsKey(number) ? instruments.get(number) : getBlankInstrument();
	}
	
	public void setInstrumentNumber(byte number, Instruments instrument){
		instruments.put(number, instrument);
	}
	
	public abstract Instruments getBlankInstrument();
	
	public float getLightBrightness(byte lightBank){
		return (lightBank & this.lightStatus) != 0 ? (electricPower > 2 ? (float) electricPower/12F : 0) : 0;
	}
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.throttle=tagCompound.getByte("throttle");
		this.lightStatus=tagCompound.getInteger("lightStatus");
		this.fuel=tagCompound.getDouble("fuel");
		this.electricPower=tagCompound.getDouble("electricPower");
		
		byte[] instrumentsInSlots = tagCompound.getByteArray("instrumentsInSlots");
		for(byte i = 0; i<pack.motorized.instruments.size(); ++i){
			PackInstrument packDef = pack.motorized.instruments.get(i);
			//Check to prevent loading of faulty instruments for the wrong vehicle due to updates or stupid people.
			for(MultipartTypes type : Instruments.values()[instrumentsInSlots[i]].validTypes){
				if(type.name().toLowerCase().equals(pack.general.type)){
					instruments.put(i, Instruments.values()[instrumentsInSlots[i]]);
				}
			}
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);		
		tagCompound.setByte("throttle", this.throttle);
		tagCompound.setInteger("lightStatus", this.lightStatus);
		tagCompound.setDouble("fuel", this.fuel);
		tagCompound.setDouble("electricPower", this.electricPower);
		
		byte[] instrumentsInSlots = new byte[pack.motorized.instruments.size()];
		for(byte i=0; i<instrumentsInSlots.length; ++i){
			if(instruments.containsKey(i)){
				instrumentsInSlots[i] = (byte) instruments.get(i).ordinal();
			}else{
				instrumentsInSlots[i] = (byte) this.getBlankInstrument().ordinal();
			}
		}
		tagCompound.setByteArray("instrumentsInSlots", instrumentsInSlots);
		return tagCompound;
	}
}
