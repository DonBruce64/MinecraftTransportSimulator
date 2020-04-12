package minecrafttransportsimulator.vehicles.main;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.dataclasses.DamageSources.DamageSourceCrash;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.sound.IRadioProvider;
import minecrafttransportsimulator.sound.Radio;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import minecrafttransportsimulator.vehicles.parts.APartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.APartGun;
import minecrafttransportsimulator.vehicles.parts.PartBarrel;
import minecrafttransportsimulator.wrappers.WrapperAudio;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**This class adds engine components for vehicles, such as fuel, throttle,
 * and electricity.  Contains numerous methods for gauges, HUDs, and fuel systems.
 * This is added on-top of the D level to keep the crazy movement calculations
 * seperate from the vehicle power overhead bits.  This is the first level of
 * class that can be used for references in systems as it's the last common class for
 * vehicles.  All other sub-levels are simply functional building-blocks to keep this
 *  class from having 1000+ lines of code and to better segment things out.
 * 
 * @author don_bruce
 */
public abstract class EntityVehicleE_Powered extends EntityVehicleD_Moving implements IRadioProvider{
	public boolean soundsNeedInit;
	public boolean hornOn;
	public boolean sirenOn;
	
	public byte throttle;
	public byte totalGuns = 0;
	public double fuel;
	public boolean reverseThrust;
	public short reversePercent;
	
	public double electricPower = 12;
	public double electricUsage;
	public double electricFlow;
	public String fluidName = "";
	public Vec3d velocityVec = Vec3d.ZERO;
	
	//Collision maps.
	public final Map<Byte, ItemInstrument> instruments = new HashMap<Byte, ItemInstrument>();
	public final Map<Byte, APartEngine> engines = new HashMap<Byte, APartEngine>();
	public final List<APartGroundDevice> wheels = new ArrayList<APartGroundDevice>();
	public final List<APartGroundDevice> groundedWheels = new ArrayList<APartGroundDevice>();
	
	/**List containing all lights that are powered on (shining).  Created as a set to allow for add calls that don't add duplicates.**/
	public final Set<LightType> lightsOn = new HashSet<LightType>();
	
	//Internal radio variables.
	private final Radio radio = new Radio(this);
	private final FloatBuffer soundPosition = ByteBuffer.allocateDirect(3*Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
	private final FloatBuffer soundVelocity = ByteBuffer.allocateDirect(3*Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
	
	
	public EntityVehicleE_Powered(World world){
		super(world);
	}
	
	public EntityVehicleE_Powered(World world, float posX, float posY, float posZ, float playerRotation, JSONVehicle definition){
		super(world, posX, posY, posZ, playerRotation, definition);
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(definition != null){
			updateHeadingVec();
			if(fuel <= 0){
				fuel = 0;
				fluidName = "";
			}
			
			//Turn on the DRLs if we have an engine on.
			lightsOn.remove(LightType.DAYTIMERUNNINGLIGHT);
			for(APartEngine engine : engines.values()){
				if(engine.state.running){
					lightsOn.add(LightType.DAYTIMERUNNINGLIGHT);
					break;
				}
			}
			
			//Set electric usage based on light status.
			if(electricPower > 2){
				for(LightType light : lightsOn){
					if(light.hasBeam){
						electricUsage += 0.0005F;
					}
				}
			}
			electricPower = Math.max(0, Math.min(13, electricPower -= electricUsage));
			electricFlow = electricUsage;
			electricUsage = 0;
			
			//Adjust reverse thrust variables.
			if(reverseThrust && reversePercent < 20){
				++reversePercent;
			}else if(!reverseThrust && reversePercent > 0){
				--reversePercent;
			}
			
			//Populate grounded wheels.  Needs to be independent of non-wheeled ground devices.
			groundedWheels.clear();
			for(APartGroundDevice wheel : this.wheels){
				if(wheel.isOnGround()){
					groundedWheels.add(wheel);
				}
			}
			
			//Update sound variables.
			soundPosition.rewind();
			soundPosition.put((float) posX);
			soundPosition.put((float) posY);
			soundPosition.put((float) posZ);
			soundPosition.flip();
			soundVelocity.rewind();
			soundVelocity.put((float) motionX);
			soundVelocity.put((float) motionY);
			soundVelocity.put((float) motionZ);
			soundVelocity.flip();
		}
	}
	
	@Override
	public void destroyAtPosition(double x, double y, double z){
		super.destroyAtPosition(x, y, z);
		//Spawn instruments in the world.
		for(ItemInstrument instrument : this.instruments.values()){
			ItemStack stack = new ItemStack(instrument);
			world.spawnEntity(new EntityItem(world, posX, posY, posZ, stack));
		}
		
		//Now find the controller to see who to display as the killer in the death message.
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
				passenger.attackEntityFrom(new DamageSourceCrash(null, this.definition.general.type), (float) (ConfigSystem.configObject.damage.crashDamageFactor.value*velocity*20));
			}else{
				passenger.attackEntityFrom(new DamageSourceCrash(controller, this.definition.general.type), (float) (ConfigSystem.configObject.damage.crashDamageFactor.value*velocity*20));
			}
		}
		
		//Oh, and add explosions.  Because those are always fun.
		//Note that this is done after spawning all parts here and in the super call,
		//so although all parts are DROPPED, not all parts may actually survive the explosion.
		if(ConfigSystem.configObject.damage.explosions.value){
			double fuelPresent = this.fuel;
			for(APart part : getVehicleParts()){
				if(part instanceof PartBarrel){
					PartBarrel barrel = (PartBarrel) part;
					if(barrel.getFluid() != null){
						for(Map<String, Double> fuelEntry : ConfigSystem.configObject.fuel.fuels.values()){
							if(fuelEntry.containsKey(barrel.getFluid().getFluid())){
								fuelPresent += barrel.getFluidAmount()*fuelEntry.get(barrel.getFluid().getFluid());
								break;
							}
						}
					}
				}
			}
			world.newExplosion(this, x, y, z, (float) (fuelPresent/10000F + 1F), true, true);
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
			for(VehiclePart packPart : definition.parts){
				for(String type : packPart.types){
					if(type.startsWith("engine")){
						if(part.offset.x == packPart.pos[0] && part.offset.y == packPart.pos[1] && part.offset.z == packPart.pos[2]){
							engines.put(engineNumber, (APartEngine) part);
							return;
						}
						++engineNumber;
					}
				}
			}
		}else if(part instanceof APartGroundDevice){
			if(((APartGroundDevice) part).canBeDrivenByEngine()){
				wheels.add((APartGroundDevice) part);
			}
		}else if(part instanceof APartGun){
			++totalGuns;
		}
	}
	
	@Override
	public void removePart(APart part, boolean playBreakSound){
		super.removePart(part, playBreakSound);
		byte engineNumber = 0;
		for(VehiclePart packPart : definition.parts){
			for(String type : packPart.types){
				if(type.startsWith("engine")){
					if(part.offset.x == packPart.pos[0] && part.offset.y == packPart.pos[1] && part.offset.z == packPart.pos[2]){
						engines.remove(engineNumber);
						return;
					}
					++engineNumber;
				}
			}
		}
		if(wheels.contains(part)){
			wheels.remove(part);
		}else if(part instanceof APartGun){
			--totalGuns;
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
			Vec3d groundHeadingVec = new Vec3d(headingVec.x, 0, headingVec.z).normalize();
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
	
	
	//-----START OF SOUND CODE-----
	@Override
	public void updateProviderSound(SoundInstance sound){
		if(this.isDead){
			sound.stop();
		}else if(sound.soundName.equals(definition.motorized.hornSound)){
			if(!hornOn){
				sound.stop();
			}
		}else if(sound.soundName.equals(definition.motorized.sirenSound)){
			if(!sirenOn){
				sound.stop();
			}
		}
	}
	
	@Override
	public void restartSound(SoundInstance sound){
		if(sound.soundName.equals(definition.motorized.hornSound)){
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.motorized.hornSound, true));
		}else if(sound.soundName.equals(definition.motorized.sirenSound)){
			WrapperAudio.playQuickSound(new SoundInstance(this, definition.motorized.sirenSound, true));
		}
	}
    
	@Override
    public FloatBuffer getProviderPosition(){
		return soundPosition;
	}
    
	@Override
    public FloatBuffer getProviderVelocity(){
		return soundVelocity;
	}
	
	@Override
	public Radio getRadio(){
		return radio;
	}
	
	
	//-----START OF NBT CODE-----
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
    	this.soundsNeedInit = world.isRemote && definition == null; 
    	super.readFromNBT(tagCompound);
		this.throttle=tagCompound.getByte("throttle");
		this.fuel=tagCompound.getDouble("fuel");
		this.electricPower=tagCompound.getDouble("electricPower");
		this.fluidName=tagCompound.getString("fluidName");
		
		lightsOn.clear();
		String lightsOnString = tagCompound.getString("lightsOn");
		while(!lightsOnString.isEmpty()){
			String lightName = lightsOnString.substring(0, lightsOnString.indexOf(','));
			for(LightType light : LightType.values()){
				if(light.name().equals(lightName)){
					lightsOn.add(light);
					break;
				}
			}
			lightsOnString = lightsOnString.substring(lightsOnString.indexOf(',') + 1);
		}
		for(byte i = 0; i<definition.motorized.instruments.size(); ++i){
			String instrumentPackID;
			String instrumentSystemName;
			//Check to see if we were an old or new vehicle.  If we are old, load using the old naming convention.
			if(tagCompound.hasKey("vehicleName")){
				String instrumentInSlot = tagCompound.getString("instrumentInSlot" + i);
				if(!instrumentInSlot.isEmpty()){
					instrumentPackID = instrumentInSlot.substring(0, instrumentInSlot.indexOf(':'));
					instrumentSystemName =  instrumentInSlot.substring(instrumentInSlot.indexOf(':') + 1);
				}else{
					continue;
				}
			}else{
				instrumentPackID = tagCompound.getString("instrument" + i + "_packID");
				instrumentSystemName = tagCompound.getString("instrument" + i + "_systemName");
			}
			if(!instrumentPackID.isEmpty()){
				ItemInstrument instrument = (ItemInstrument) MTSRegistry.packItemMap.get(instrumentPackID).get(instrumentSystemName);
				//Check to prevent loading of faulty instruments for the wrong vehicle due to updates or stupid people.
				if(instrument != null && instrument.definition.general.validVehicles.contains(this.definition.general.type)){
					instruments.put(i, instrument);
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
		tagCompound.setString("fluidName", this.fluidName);
		
		String lightsOnString = "";
		for(LightType light : this.lightsOn){
			lightsOnString += light.name() + ",";
		}
		tagCompound.setString("lightsOn", lightsOnString);
		
		String[] instrumentsInSlots = new String[definition.motorized.instruments.size()];
		for(byte i=0; i<instrumentsInSlots.length; ++i){
			if(instruments.containsKey(i)){
				tagCompound.setString("instrument" + i + "_packID", instruments.get(i).definition.packID);
				tagCompound.setString("instrument" + i + "_systemName", instruments.get(i).definition.systemName);
			}
		}
		return tagCompound;
	}
	
	public static enum LightType{
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
		EMERGENCYLIGHT(false),
		DAYTIMERUNNINGLIGHT(false),
		
		//The following light types are only for block-based systems.
		STOPLIGHT(false),
		CAUTIONLIGHT(false),
		GOLIGHT(false),
		STREETLIGHT(true),
		DECORLIGHT(false);
		
		public final boolean hasBeam;
		private LightType(boolean hasBeam){
			this.hasBeam = hasBeam;
		}
	}
}
