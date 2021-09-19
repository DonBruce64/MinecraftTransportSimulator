package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.NavBeacon;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.NavBeaconSystem;

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
abstract class AEntityVehicleE_Powered extends AEntityVehicleD_Moving{
	//Static variables used in logic that are kept in the global map.
	public static final String RUNNINGLIGHT_VARIABLE = "running_light";
	public static final String HEADLIGHT_VARIABLE = "headlight";
	public static final String NAVIGATIONLIGHT_VARIABLE = "navigation_light";
	public static final String STROBELIGHT_VARIABLE = "strobe_light";
	public static final String TAXILIGHT_VARIABLE = "taxi_light";
	public static final String LANDINGLIGHT_VARIABLE = "landing_light";
	public static final String HORN_VARIABLE = "horn";
	public static final String GEAR_VARIABLE = "gear_setpoint";
	
	//External state control.
	public boolean reverseThrust;
	public boolean beingFueled;
	public boolean enginesOn;
	public boolean enginesRunning;
	public static final byte MAX_THROTTLE = 100;
	public byte throttle;
	
	
	//Internal states.
	public int gearMovementTime;
	public double electricPower;
	public double electricUsage;
	public double electricFlow;
	public String selectedBeaconName;
	public NavBeacon selectedBeacon;
	public EntityFluidTank fuelTank;
	
	//Part maps.
	public final BiMap<Byte, PartEngine> engines = HashBiMap.create();
	
	//Map containing incoming missiles, sorted by distance, which is the value for this map.
	public final List<EntityBullet> missilesIncoming = new ArrayList<EntityBullet>();
	
	public AEntityVehicleE_Powered(WrapperWorld world, WrapperNBT data){
		super(world, data);
		
		//Load simple variables.
		this.reverseThrust = data.getBoolean("reverseThrust");
		this.throttle = (byte) data.getInteger("throttle");
		this.electricPower = data.getDouble("electricPower");
		this.selectedBeaconName = data.getString("selectedBeaconName");
		this.selectedBeacon = NavBeaconSystem.getBeacon(world, selectedBeaconName);
		this.fuelTank = new EntityFluidTank(world, data.getDataOrNew("fuelTank"), definition.motorized.fuelCapacity);
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			world.beginProfiling("VehicleE_Level", true);
			//If we have space for fuel, and we have tanks with it, transfer it.
			if(!world.isClient() && fuelTank.getFluidLevel() < definition.motorized.fuelCapacity - 100){
				for(APart part : parts){
					if(part instanceof PartInteractable && part.definition.interactable.feedsVehicles){
						EntityFluidTank tank = ((PartInteractable) part).tank;
						if(tank != null){
							double amountFilled = tank.drain(fuelTank.getFluid(), 1, true);
							if(amountFilled > 0){
								fuelTank.fill(fuelTank.getFluid(), amountFilled, true);
							}
						}
					}
				}
			}
			
			//Check to make sure the selected beacon is still correct.
			//It might not be valid if it has been removed from the world,
			//or one might have been placed that matches our selection.
			if(definition.motorized.isAircraft && ticksExisted%20 == 0){
				selectedBeacon = NavBeaconSystem.getBeacon(world, selectedBeaconName);
			}
			
			//Do trailer-specific logic, if we are one and towed.
			//Otherwise, do normal update logic for DRLs.
			if(definition.motorized.isTrailer){
				//If we are being towed set the brake state to the same as the towing vehicle.
				//If we aren't being towed, set the parking brake.
				if(towedByConnection != null){
					parkingBrakeOn = false;
					brake = ((AEntityVehicleE_Powered) towedByConnection.hitchBaseEntity).brake;
				}else{
					parkingBrakeOn = true;
					brake = 0;
				}
			}else{
				//Set engine state mapping variables.
				enginesOn = false;
				enginesRunning = false;
				for(PartEngine engine : engines.values()){
					if(engine.state.magnetoOn){
						enginesOn = true;
						if(engine.state.running){
							enginesRunning = true;
							break;
						}
					}
				}
			}
			
			//Set electric usage based on light status.
			//Don't do this if we are a trailer.  Instead, get the towing vehicle's electric power.
			if(definition.motorized.isTrailer){
				if(towedByConnection != null){
					electricPower = ((AEntityVehicleE_Powered) towedByConnection.hitchBaseEntity).electricPower;
				}
			}else{
				if(electricPower > 2 && renderTextLit()){
					electricUsage += 0.001F;
				}
				electricPower = Math.max(0, Math.min(13, electricPower -= electricUsage));
				electricFlow = electricUsage;
				electricUsage = 0;
			}
			
			//Adjust gear variables.
			if(variablesOn.contains(EntityVehicleF_Physics.GEAR_VARIABLE)){
				if(gearMovementTime < definition.motorized.gearSequenceDuration){
					++gearMovementTime;
				}
			}else{
				if(gearMovementTime > 0){
					--gearMovementTime;
				}
			}
			
			//Check that missiles are still valid.
			//If they are, update their distances. Otherwise, remove them.
			Iterator<EntityBullet> iterator = missilesIncoming.iterator();
			while(iterator.hasNext()){
				if(!iterator.next().isValid){
					iterator.remove();
				}
			}
			missilesIncoming.sort(new Comparator<EntityBullet>(){
				@Override
				public int compare(EntityBullet missle1, EntityBullet missile2){
					return missle1.targetDistance < missile2.targetDistance ? -1 : 1;
				}
			});
			world.endProfiling();
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public boolean addRider(WrapperEntity rider, Point3d riderLocation){
		if(super.addRider(rider, riderLocation)){
			if(world.isClient() && ConfigSystem.configObject.clientControls.autostartEng.value && rider.equals(InterfaceClient.getClientPlayer())){
				if(rider instanceof WrapperPlayer && locationRiderMap.containsValue(rider) && getPartAtLocation(locationRiderMap.inverse().get(rider)).placementDefinition.isController){
					for(PartEngine engine : engines.values()){
						InterfacePacket.sendToServer(new PacketPartEngine(engine, Signal.AS_ON));
					}
					if(parkingBrakeOn){
						InterfacePacket.sendToServer(new PacketEntityVariableToggle(this, PARKINGBRAKE_VARIABLE));
					}
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public void removeRider(WrapperEntity rider, Iterator<WrapperEntity> iterator){
		if(world.isClient() && ConfigSystem.configObject.clientControls.autostartEng.value && rider.equals(InterfaceClient.getClientPlayer())){
			if(rider instanceof WrapperPlayer && locationRiderMap.containsValue(rider)){
				APart riddenPart = getPartAtLocation(locationRiderMap.inverse().get(rider));
				boolean otherController = false;
				if(riddenPart.placementDefinition.isController){
					//Check if another player is in a controller seat.  If so, don't stop the engines.
					for(APart part : parts){
						if(!part.equals(riddenPart)){
							if(locationRiderMap.containsKey(part.placementOffset)){
								if(part.placementDefinition.isController){
									otherController = true;
									break;
								}
							}
						}
					}
					if(!otherController){
						for(PartEngine engine : engines.values()){
							InterfacePacket.sendToServer(new PacketPartEngine(engine, Signal.MAGNETO_OFF));
						}
						InterfacePacket.sendToServer(new PacketVehicleControlAnalog((EntityVehicleF_Physics) this, PacketVehicleControlAnalog.Controls.BRAKE, (short) 0, Byte.MAX_VALUE));
						if(!parkingBrakeOn){
							InterfacePacket.sendToServer(new PacketEntityVariableToggle(this, PARKINGBRAKE_VARIABLE));
						}
					}
				}
			}
		}
		super.removeRider(rider, iterator);
	}
	
	@Override
	public void destroy(BoundingBox box){
		super.destroy(box);
		//Spawn instruments in the world.
		for(ItemInstrument instrument : instruments.values()){
			world.spawnItem(instrument, null, box.globalCenter);
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
			world.spawnExplosion(box.globalCenter, explosivePower + fuelTank.getExplosiveness() + 1D, true);
		}
	}
	
	@Override
	public double getMass(){
		return super.getMass() + fuelTank.getMass();
	}
	
	@Override
	public void addPart(APart part, boolean sendPacket){
		super.addPart(part, sendPacket);
		if(part instanceof PartEngine){
			//Because parts is a list, the #1 engine will always come before the #2 engine.
			//We can use this to determine where in the list this engine needs to go.
			byte engineNumber = 0;
			for(JSONPartDefinition partDef : definition.parts){
				for(String type : partDef.types){
					if(type.startsWith("engine")){
						//Part goes into this slot.
						if(part.placementOffset.equals(partDef.pos)){
							engines.put(engineNumber, (PartEngine) part);
							return;
						}
						++engineNumber;
						break;
					}
				}
			}
			
			//Engine position not found.  Get the next free slot and add it.
			while(engines.containsKey(engineNumber++));
			engineNumber--;
			engines.put(engineNumber, (PartEngine) part);
		}else if(!part.placementDefinition.isSpare && part instanceof PartGroundDevice){
			groundDeviceCollective.addGroundDevice((PartGroundDevice) part);
		}
	}
	
	@Override
	public void removePart(APart part, Iterator<APart> iterator){
		super.removePart(part, iterator);
		engines.inverse().remove(part);
		if(!part.placementDefinition.isSpare && part instanceof PartGroundDevice){
			groundDeviceCollective.removeGroundDevice((PartGroundDevice) part);
		}
	}
	
	public void acquireMissile(EntityBullet missile){
		//Add this missile with its current distance
		if(!missilesIncoming.contains(missile)){
			missilesIncoming.add(missile);
		}
	}
	
	//-----START OF SOUND AND ANIMATION CODE-----
	@Override
	public boolean hasRadio(){
		return true;
	}
	
	@Override
	public boolean renderTextLit(){
		if(definition.motorized.hasRunningLights && variablesOn.contains(RUNNINGLIGHT_VARIABLE)) return electricPower > 3;
		if(definition.motorized.hasHeadlights && variablesOn.contains(HEADLIGHT_VARIABLE)) return electricPower > 3;
		if(definition.motorized.hasNavLights && variablesOn.contains(NAVIGATIONLIGHT_VARIABLE)) return electricPower > 3;
		if(definition.motorized.hasStrobeLights && variablesOn.contains(STROBELIGHT_VARIABLE)) return electricPower > 3;
		if(definition.motorized.hasTaxiLights && variablesOn.contains(TAXILIGHT_VARIABLE)) return electricPower > 3;
		if(definition.motorized.hasLandingLights && variablesOn.contains(LANDINGLIGHT_VARIABLE)) return electricPower > 3;
		return false;
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setBoolean("reverseThrust", reverseThrust);
		data.setInteger("throttle", throttle);
		data.setDouble("electricPower", electricPower);
		data.setString("selectedBeaconName", selectedBeaconName);
		data.setData("fuelTank", fuelTank.save(new WrapperNBT()));
		return data;
	}
}
