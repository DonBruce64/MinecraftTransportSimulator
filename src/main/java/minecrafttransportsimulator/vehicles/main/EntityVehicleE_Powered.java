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
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.sound.IRadioProvider;
import minecrafttransportsimulator.sound.Radio;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartBarrel;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import minecrafttransportsimulator.wrappers.WrapperAudio;
import minecrafttransportsimulator.wrappers.WrapperBlockFakeLight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**This class adds engine components for vehicles, such as fuel, throttle,
 * and electricity.  Contains numerous methods for gauges, HUDs, and fuel systems.
 * This is added on-top of the D level to keep the crazy movement calculations
 * separate from the vehicle power overhead bits.  This is the first level of
 * class that can be used for references in systems as it's the last common class for
 * vehicles.  All other sub-levels are simply functional building-blocks to keep this
 *  class from having 1000+ lines of code and to better segment things out.
 * 
 * @author don_bruce
 */
abstract class EntityVehicleE_Powered extends EntityVehicleD_Moving implements IRadioProvider{
	
	//External state control.
	public boolean hornOn;
	public boolean sirenOn;
	public boolean reverseThrust;
	public boolean gearUpCommand;
	public byte throttle;
	public double fuel;
	
	//Internal states.
	public byte totalGuns = 0;
	public short reversePercent;
	public int gearMovementTime;
	public double electricPower = 12;
	public double electricUsage;
	public double electricFlow;
	public String fluidName = "";
	private BlockPos fakeLightPosition;
	public EntityVehicleF_Physics towedVehicle;
	public EntityVehicleF_Physics towedByVehicle;
	/**List containing all lights that are powered on (shining).  Created as a set to allow for add calls that don't add duplicates.**/
	public final Set<LightType> lightsOn = new HashSet<LightType>();
	
	//Collision maps.
	public final Map<Byte, ItemInstrument> instruments = new HashMap<Byte, ItemInstrument>();
	public final Map<Byte, PartEngine> engines = new HashMap<Byte, PartEngine>();
	public final List<PartGroundDevice> wheels = new ArrayList<PartGroundDevice>();
	public final List<PartGroundDevice> groundedWheels = new ArrayList<PartGroundDevice>();
	
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
			
			//Do trailer-specific logic, if we are one and towed.
			//Otherwise, do normal update logic for DRLs.
			if(definition.motorized.isTrailer){
				//Check to make sure vehicle isn't dead for some reason.
				if(towedByVehicle != null && towedByVehicle.isDead){
					towedByVehicle = null;
				}else{
					//If we are being towed update our lights to match the vehicle we are being towed by.
					//Also set the brake state to the same as the towing vehicle.
					//If we aren't being towed, set the parking brake.
					if(towedByVehicle != null){
						lightsOn.clear();
						lightsOn.addAll(towedByVehicle.lightsOn);
						parkingBrakeOn = false;
						brakeOn = towedByVehicle.brakeOn;
					}else{
						parkingBrakeOn = true;
					}
				}
			}else{
				//Turn on the DRLs if we have an engine on.
				lightsOn.remove(LightType.DAYTIMERUNNINGLIGHT);
				for(PartEngine engine : engines.values()){
					if(engine.state.running){
						lightsOn.add(LightType.DAYTIMERUNNINGLIGHT);
						break;
					}
				}
			}
			
			//Make the light bright at our position if lights are on.
			//DRLs are always on, so check for that.
			if(world.isRemote){
				if(ConfigSystem.configObject.client.vehicleBlklt.value){
					if(lightsOn.contains(LightType.DAYTIMERUNNINGLIGHT) ? lightsOn.size() > 1 : !lightsOn.isEmpty()){
						BlockPos newPos = getPosition();
						//Check to see if we need to place a light.
						if(!newPos.equals(fakeLightPosition)){
							//If our prior position is not null, remove that block.
							if(fakeLightPosition != null){
								world.setBlockToAir(fakeLightPosition);
								world.checkLight(fakeLightPosition);
								fakeLightPosition = null;
							}
							//Set block in world and update pos.  Only do this if the block is air.
							if(world.isAirBlock(newPos)){
								world.setBlockState(newPos, WrapperBlockFakeLight.instance.getDefaultState());
								world.checkLight(newPos);
								fakeLightPosition = newPos;
							}
						}
					}else if(fakeLightPosition != null){
						//Lights are off, turn off fake light.
						world.setBlockToAir(fakeLightPosition);
						world.checkLight(fakeLightPosition);
						fakeLightPosition = null;
					}
				}else if(fakeLightPosition != null){
					//Fake light config was on, but was turned off.  Get rid of the remaining fake light.
					world.setBlockToAir(fakeLightPosition);
					world.checkLight(fakeLightPosition);
					fakeLightPosition = null;
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
			
			//Adjust gear variables.
			if(gearUpCommand && gearMovementTime < definition.motorized.gearSequenceDuration){
				++gearMovementTime;
			}else if(!gearUpCommand && gearMovementTime > 0){
				--gearMovementTime;
			}
			
			//Populate grounded wheels.  Needs to be independent of non-wheeled ground devices.
			groundedWheels.clear();
			for(PartGroundDevice wheel : this.wheels){
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
	public void setDead(){
		super.setDead();
		if(fakeLightPosition != null){
			world.setBlockToAir(fakeLightPosition);
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
			if(this.getSeatForRider(passenger).vehicleDefinition.isController && controller != null){
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
		
		//Finally, if we are being towed, unhook us from our tower.
		if(towedByVehicle != null){
			towedByVehicle.towedVehicle = null;
			towedByVehicle = null;
		}
	}
	
	@Override
	protected float getCurrentMass(){
		return (float) (super.getCurrentMass() + this.fuel/50);
	}
	
	@Override
	public void addPart(APart part, boolean ignoreCollision){
		super.addPart(part, ignoreCollision);
		if(part instanceof PartEngine){
			//Because parts is a list, the #1 engine will always come before the #2 engine.
			//We can use this to determine where in the list this engine needs to go.
			byte engineNumber = 0;
			for(VehiclePart packPart : definition.parts){
				for(String type : packPart.types){
					if(type.startsWith("engine")){
						if(part.placementOffset.x == packPart.pos[0] && part.placementOffset.y == packPart.pos[1] && part.placementOffset.z == packPart.pos[2]){
							engines.put(engineNumber, (PartEngine) part);
							return;
						}
						++engineNumber;
					}
				}
			}
		}else if(part instanceof PartGroundDevice){
			if(part.definition.ground.isWheel || part.definition.ground.isTread){
				wheels.add((PartGroundDevice) part);
			}
		}else if(part instanceof PartGun){
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
					if(part.placementOffset.x == packPart.pos[0] && part.placementOffset.y == packPart.pos[1] && part.placementOffset.z == packPart.pos[2]){
						engines.remove(engineNumber);
						return;
					}
					++engineNumber;
				}
			}
		}
		if(wheels.contains(part)){
			wheels.remove(part);
		}else if(part instanceof PartGun){
			--totalGuns;
		}
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
    public int getProviderDimension(){
		return world.provider.getDimension();
	}
	
	@Override
	public Radio getRadio(){
		return radio;
	}
	
	
	//-----START OF NBT CODE-----
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
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
				//Check to prevent loading of faulty instruments due to updates.
				if(instrument != null){
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
}
