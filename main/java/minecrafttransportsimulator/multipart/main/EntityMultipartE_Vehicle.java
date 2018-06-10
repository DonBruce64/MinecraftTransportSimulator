package minecrafttransportsimulator.multipart.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.dataclasses.DamageSources.DamageSourceCrash;
import minecrafttransportsimulator.dataclasses.MTSInstruments.Instruments;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackInstrument;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.multipart.parts.APartEngine;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**This class is built on the base multipart D level and is is tailored for moving vehicles 
 * such as planes, helicopters, and automobiles.
 * Contains numerous methods for gauges, HUDs, and fuel systems.
 * Essentially, if it has parts and an engine, use this.
 * 
 * @author don_bruce
 */
public abstract class EntityMultipartE_Vehicle extends EntityMultipartD_Moving{
	public byte throttle;
	public double fuel;
	public double electricPower = 12;
	public double electricUsage;
	public double electricFlow;
	public Vec3d velocityVec = Vec3d.ZERO;
	
	private byte numberEngineBays = 0;
	private final Map<Byte, APartEngine> engineByNumber = new HashMap<Byte, APartEngine>();
	private final Map<Byte, Instruments> instruments = new HashMap<Byte, Instruments>();
	private final List<LightTypes> lightsOn = new ArrayList<LightTypes>();
	
	public EntityMultipartE_Vehicle(World world){
		super(world);
	}
	
	public EntityMultipartE_Vehicle(World world, float posX, float posY, float posZ, float playerRotation, String name){
		super(world, posX, posY, posZ, playerRotation, name);
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(pack != null){
			updateHeadingVec();
			if(fuel < 0){fuel = 0;}
			if(electricPower > 2){
				for(LightTypes light : lightsOn){
					if(light.hasBeam){
						electricUsage += 0.00005F;
					}else{
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
	protected void destroyAtPosition(double x, double y, double z){
		super.destroyAtPosition(x, y, z);
		//First find the controller to see who to display as the killer in the death message.
		Entity controller = null;
		for(Entity passenger : this.getPassengers()){
			if(this.getSeatForRider(passenger).isController && controller != null){
				controller = passenger;
				break;
			}
		}
		
		//Now damage all passengers, including the controller.
		for(Entity passenger : this.getPassengers()){
			if(passenger.equals(controller)){
				passenger.attackEntityFrom(new DamageSourceCrash(null, this.pack.general.type), (float) (ConfigSystem.getDoubleConfig("CrashDamageFactor")*velocity*20));
			}else{
				passenger.attackEntityFrom(new DamageSourceCrash(controller, this.pack.general.type), (float) (ConfigSystem.getDoubleConfig("CrashDamageFactor")*velocity*20));
			}
		}
		
		//Oh, and add explosions.  Because those are always fun.
		if(ConfigSystem.getBooleanConfig("Explosions")){
			worldObj.newExplosion(this, x, y, z, (float) (fuel/1000F + 1F), true, true);
		}
	}
	
	@Override
	protected float getCurrentMass(){
		return (float) (super.getCurrentMass() + this.fuel/50);
	}
	
	@Override
	public void addPart(APart part, boolean ignoreCollision){
		super.addPart(part, ignoreCollision);
		if(part instanceof APartEngine){
			//Because parts is a list, the #1 engine will always come before the #2 engine.
			//We can use this to determine where in the list this engine needs to go.
			byte engineNumber = 0;
			for(PackPart packPart : pack.parts){
				for(String type : packPart.types){
					if(type.contains("engine")){
						++engineNumber;
						if(part.offset.xCoord == packPart.pos[0] && part.offset.yCoord == packPart.pos[1] && part.offset.zCoord == packPart.pos[2]){
							engineByNumber.put(engineNumber, (APartEngine) part);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void removePart(APart part, boolean playBreakSound){
		super.removePart(part, playBreakSound);
		byte engineNumber = 0;
		for(PackPart packPart : pack.parts){
			for(String type : packPart.types){
				if(type.contains("engine")){
					++engineNumber;
					if(part.offset.xCoord == packPart.pos[0] && part.offset.yCoord == packPart.pos[1] && part.offset.zCoord == packPart.pos[2]){
						engineByNumber.remove(engineNumber);
						return;
					}
				}
			}
		}
	}
	
	protected void performGroundOperations(){
		float brakingFactor = getBrakingForceFactor();
		if(brakingFactor > 0){
			double groundSpeed = Math.hypot(motionX, motionZ)*Math.signum(velocity);
			groundSpeed -= 20F*brakingFactor/currentMass*Math.signum(velocity);
			if(Math.abs(groundSpeed) > 0.1){
				reAdjustGroundSpeed(groundSpeed);
			}else{
				motionX = 0;
				motionZ = 0;
				motionYaw = 0;
			}
		}
		
		float skiddingFactor = getSkiddingFactor();
		if(skiddingFactor != 0){
			Vec3d groundVelocityVec = new Vec3d(motionX, 0, motionZ).normalize();
			Vec3d groundHeadingVec = new Vec3d(headingVec.xCoord, 0, headingVec.zCoord).normalize();
			float vectorDelta = (float) groundVelocityVec.distanceTo(groundHeadingVec);
			byte velocitySign = (byte) (vectorDelta < 1 ? 1 : -1);
			if(vectorDelta > 0.001){
				vectorDelta = Math.min(skiddingFactor, vectorDelta);
				float yawTemp = rotationYaw;
				rotationYaw += vectorDelta;
				updateHeadingVec();
				reAdjustGroundSpeed(Math.hypot(motionX, motionZ)*velocitySign);
				rotationYaw = yawTemp;
			}
		}
		
		motionYaw += getTurningFactor();
	}
	
	/**
	 * Gets the number of bays available for engines.
	 * Cached for efficiency.
	 */
	public byte getNumberEngineBays(){
		if(numberEngineBays == 0){
			for(PackPart part : pack.parts){
				for(String type : part.types){
					if(type.contains("engine")){
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
	public APartEngine getEngineByNumber(byte number){
		return engineByNumber.get(number);
	}
	
	public void changeLightStatus(LightTypes light, boolean isOn){
		if(isOn){
			if(!lightsOn.contains(light)){
				lightsOn.add(light);
			}
		}else{
			if(lightsOn.contains(light)){
				lightsOn.remove(light);
			}
		}
	}
	
	public boolean isLightOn(LightTypes light){
		return lightsOn.contains(light);
	}
	
	public Instruments getInstrumentNumber(byte number){
		return instruments.containsKey(number) ? instruments.get(number) : getBlankInstrument();
	}
	
	public void setInstrumentNumber(byte number, Instruments instrument){
		instruments.put(number, instrument);
	}
	
	public abstract Instruments getBlankInstrument();
	
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.throttle=tagCompound.getByte("throttle");
		this.fuel=tagCompound.getDouble("fuel");
		this.electricPower=tagCompound.getDouble("electricPower");
		
		lightsOn.clear();
		String lightsOnString = tagCompound.getString("lightsOn");
		while(!lightsOnString.isEmpty()){
			String lightName = lightsOnString.substring(0, lightsOnString.indexOf(','));
			for(LightTypes light : LightTypes.values()){
				if(light.name().equals(lightName)){
					lightsOn.add(light);
					break;
				}
			}
			lightsOnString = lightsOnString.substring(lightsOnString.indexOf(',') + 1);
		}
		
		byte[] instrumentsInSlots = tagCompound.getByteArray("instrumentsInSlots");
		for(byte i = 0; i<pack.motorized.instruments.size(); ++i){
			PackInstrument packDef = pack.motorized.instruments.get(i);
			//Check to prevent loading of faulty instruments for the wrong vehicle due to updates or stupid people.
			for(Class<? extends EntityMultipartD_Moving>  validClass : Instruments.values()[instrumentsInSlots[i]].validClasses){
				if(validClass.isAssignableFrom(this.getClass())){
					instruments.put(i, Instruments.values()[instrumentsInSlots[i]]);
				}
			}
		}
	}
    
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);		
		tagCompound.setByte("throttle", this.throttle);
		tagCompound.setDouble("fuel", this.fuel);
		tagCompound.setDouble("electricPower", this.electricPower);
		
		String lightsOnString = "";
		for(LightTypes light : this.lightsOn){
			lightsOnString += light.name() + ",";
		}
		tagCompound.setString("lightsOn", lightsOnString);
		
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
	
	public enum LightTypes{
		NAVIGATIONLIGHT(false),
		STROBELIGHT(false),
		TAXILIGHT(true),
		LANDINGLIGHT(true),
		BRAKELIGHT(false),
		BACKUPLIGHT(false),
		LEFTTURNLIGHT(false),
		RIGHTTURNLIGHT(false),
		LEFTINDICATORLIGHT(false),
		RIGHTINDICATORLIGHT(false),
		RUNNINGLIGHT(false),
		HEADLIGHT(true),
		EMERGENCYLIGHT(false);
		
		public final boolean hasBeam;
		
		private LightTypes(boolean hasBeam){
			this.hasBeam = hasBeam;
		}
	}
}
