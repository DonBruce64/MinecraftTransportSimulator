package minecrafttransportsimulator.vehicles.main;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mcinterface.InterfaceAudio;
import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.sound.IRadioProvider;
import minecrafttransportsimulator.sound.Radio;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;

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
	
	//Internal states.
	public byte totalGuns;
	public int gearMovementTime;
	public double electricPower;
	public double electricUsage;
	public double electricFlow;
	public FluidTank fuelTank;
	/**List containing all lights that are powered on (shining).  Created as a set to allow for add calls that don't add duplicates.**/
	public final Set<LightType> lightsOn = new HashSet<LightType>();
	/**List containing all active custom variable indexes.    Created as a set to allow for add calls that don't add duplicates.**/
	public final Set<Byte> customsOn = new HashSet<Byte>();
	/**List containing text lines for saved text.  Note that parts have their own text, so it's not saved here.**/
	public final List<String> textLines = new ArrayList<String>();
	
	//Collision maps.
	public final Map<Byte, JSONInstrument> instruments = new HashMap<Byte, JSONInstrument>();
	public final Map<Byte, PartEngine> engines = new HashMap<Byte, PartEngine>();
	public final List<PartGroundDevice> wheels = new ArrayList<PartGroundDevice>();
	public final List<PartGroundDevice> groundedWheels = new ArrayList<PartGroundDevice>();
	
	//Internal radio variables.
	private final Radio radio = new Radio(this);
	private final FloatBuffer soundPosition = ByteBuffer.allocateDirect(3*Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
	
	public EntityVehicleE_Powered(WrapperWorld world, WrapperNBT data){
		super(world, data);
		
		//Load simple variables.
		this.hornOn = data.getBoolean("hornOn");
		this.sirenOn = data.getBoolean("sirenOn");
		this.reverseThrust = data.getBoolean("reverseThrust");
		this.gearUpCommand = data.getBoolean("gearUpCommand");
		this.throttle = (byte) data.getInteger("throttle");
		this.electricPower = data.getDouble("electricPower");
		this.fuelTank = new FluidTank(data, definition.motorized.fuelCapacity, world.isClient());
		
		//Load lights.
		lightsOn.clear();
		String lightsOnString = data.getString("lightsOn");
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
		
		//Load custom variables.
		if(definition.rendering.customVariables != null){
			customsOn.clear();
			String customsOnString = data.getString("customsOn");
			while(!customsOnString.isEmpty()){
				byte customIndex = Byte.valueOf(customsOnString.substring(0, customsOnString.indexOf(',')));
				customsOn.add(customIndex);
				customsOnString = customsOnString.substring(customsOnString.indexOf(',') + 1);
			}
		}
		
		//Load text.
		if(definition.rendering.textObjects != null){
			for(byte i=0; i<definition.rendering.textObjects.size(); ++i){
				textLines.add(data.getString("textLine" + i));
			}
		}
		
		//Load instruments.
		for(byte i = 0; i<definition.motorized.instruments.size(); ++i){
			String instrumentPackID = data.getString("instrument" + i + "_packID");
			String instrumentSystemName = data.getString("instrument" + i + "_systemName");
			if(!instrumentPackID.isEmpty()){
				JSONInstrument instrument = (JSONInstrument) MTSRegistry.packItemMap.get(instrumentPackID).get(instrumentSystemName).definition;
				//Check to prevent loading of faulty instruments due to updates.
				if(instrument != null){
					instruments.put(i, instrument);
				}
			}
		}
	}
	
	@Override
	public void update(){
		super.update();
		//Start sounds if we haven't already.  We have to do this via the update check, as some mods will create
		//vehicles in random locations for their code.  I'm looking at YOU, The One Probe!
		if(ticksExisted == 1 && world.isClient()){
			startSounds();
			for(APart part : parts){
				part.startSounds();
			}
		}
		if(fuelTank.getFluidLevel() < definition.motorized.fuelCapacity - 100){
			//If we have space for fuel, and we have tanks with it, transfer it.
			for(APart part : parts){
				if(part instanceof PartInteractable && part.definition.interactable.feedsVehicles){
					FluidTank tank = ((PartInteractable) part).tank;
					if(tank != null){
						double amountFilled = tank.drain(fuelTank.getFluid(), 1, true);
						if(amountFilled > 0){
							fuelTank.fill(fuelTank.getFluid(), amountFilled, true);
						}
					}
				}
			}
		}
		
		//Do trailer-specific logic, if we are one and towed.
		//Otherwise, do normal update logic for DRLs.
		if(definition.motorized.isTrailer){
			//Check to make sure vehicle isn't dead for some reason.
			if(towedByVehicle != null && !towedByVehicle.isValid){
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
		soundPosition.put((float) position.x);
		soundPosition.put((float) position.y);
		soundPosition.put((float) position.z);
		soundPosition.flip();
	}
	
	@Override
	public boolean isLitUp(){
		return ConfigSystem.configObject.client.vehicleBlklt.value && (lightsOn.contains(LightType.DAYTIMERUNNINGLIGHT) ? lightsOn.size() > 1 : !lightsOn.isEmpty());
	}
	
	 /**
     * Returns true if the interior lights on this vehicle are on.  This is taken to mean the interior
     * lights that cause the instrument cluster to light up, as well as any outer text markings.
     */
	public boolean areInteriorLightsOn(){
		return (lightsOn.contains(LightType.NAVIGATIONLIGHT) || lightsOn.contains(LightType.RUNNINGLIGHT) || lightsOn.contains(LightType.HEADLIGHT)) && electricPower > 3;
	}
	
	@Override
	public void destroyAtPosition(Point3d position){
		super.destroyAtPosition(position);
		//Spawn instruments in the world.
		for(JSONInstrument instrument : instruments.values()){
			ItemInstrument item = (ItemInstrument) MTSRegistry.packItemMap.get(instrument.packID).get(instrument.systemName);
			world.spawnItem(item, null, position);
		}
		
		//Oh, and add explosions.  Because those are always fun.
		//Note that this is done after spawning all parts here and in the super call,
		//so although all parts are DROPPED, not all parts may actually survive the explosion.
		if(ConfigSystem.configObject.damage.explosions.value){
			double explosivePower = 0;
			for(APart part : parts){
				if(part instanceof PartInteractable){
					explosivePower += ((PartInteractable) part).getExplosiveContribution();
				}
			}
			world.spawnExplosion(this, position, explosivePower + fuelTank.getExplosiveness() + 1D, true);
		}
		
		//Finally, if we are being towed, unhook us from our tower.
		if(towedByVehicle != null){
			towedByVehicle.towedVehicle = null;
			towedByVehicle = null;
		}
	}
	
	@Override
	protected float getCurrentMass(){
		return (float) (super.getCurrentMass() + fuelTank.getWeight());
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
						if(part.placementOffset.equals(packPart.pos)){
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
	public void removePart(APart part, Iterator<APart> iterator){
		super.removePart(part, iterator);
		byte engineNumber = 0;
		for(VehiclePart packPart : definition.parts){
			for(String type : packPart.types){
				if(type.startsWith("engine")){
					if(part.placementOffset.equals(packPart.pos)){
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
	public void startSounds(){
		if(hornOn){
			InterfaceAudio.playQuickSound(new SoundInstance(this, definition.motorized.hornSound, true));
		}else if(sirenOn){
			InterfaceAudio.playQuickSound(new SoundInstance(this, definition.motorized.sirenSound, true));
		}
	}
	
	@Override
	public void updateProviderSound(SoundInstance sound){
		if(!isValid){
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
    public FloatBuffer getProviderPosition(){
		return soundPosition;
	}
    
	@Override
    public Point3d getProviderVelocity(){
		return motion;
	}
	
	@Override
    public int getProviderDimension(){
		return world.getDimensionID();
	}
	
	@Override
	public Radio getRadio(){
		return radio;
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setBoolean("hornOn", hornOn);
		data.setBoolean("sirenOn", sirenOn);
		data.setBoolean("reverseThrust", reverseThrust);
		data.setBoolean("gearUpCommand", gearUpCommand);
		data.setInteger("throttle", throttle);
		data.setDouble("electricPower", electricPower);
		fuelTank.save(data);
		
		String lightsOnString = "";
		for(LightType light : lightsOn){
			lightsOnString += light.name() + ",";
		}
		data.setString("lightsOn", lightsOnString);
		
		if(definition.rendering.customVariables != null){
			String customsOnString = "";
			for(byte customIndex : customsOn){
				customsOnString += customIndex + ",";
			}
			data.setString("customsOn", customsOnString);
		}
		
		if(definition.rendering.textObjects != null){
			for(byte i=0; i<definition.rendering.textObjects.size(); ++i){
				data.setString("textLine" + i, textLines.get(i));
			}
		}
		
		String[] instrumentsInSlots = new String[definition.motorized.instruments.size()];
		for(byte i=0; i<instrumentsInSlots.length; ++i){
			if(instruments.containsKey(i)){
				data.setString("instrument" + i + "_packID", instruments.get(i).packID);
				data.setString("instrument" + i + "_systemName", instruments.get(i).systemName);
			}
		}
	}
}
