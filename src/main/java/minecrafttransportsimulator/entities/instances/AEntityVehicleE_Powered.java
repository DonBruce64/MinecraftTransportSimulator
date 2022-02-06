package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.NavBeacon;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.systems.ConfigSystem;

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
	public static final String THROTTLE_VARIABLE = "throttle";
	public static final String REVERSE_THRUST_VARIABLE = "reverser";
	
	//External state control.
	@DerivedValue
	public boolean reverseThrust;
	public boolean beingFueled;
	public boolean enginesOn;
	public boolean enginesRunning;
	@DerivedValue
	public double throttle;
	public static final double MAX_THROTTLE = 1.0D;
	
	
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
	
	public AEntityVehicleE_Powered(WrapperWorld world, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, placingPlayer, data);
		
		//Load simple variables.
		this.electricPower = data.getDouble("electricPower");
		this.selectedBeaconName = data.getString("selectedBeaconName");
		this.selectedBeacon = NavBeacon.getByNameFromWorld(world, selectedBeaconName);
		this.fuelTank = new EntityFluidTank(world, data.getDataOrNew("fuelTank"), definition.motorized.fuelCapacity);
		world.addEntity(fuelTank);
		
		if(newlyCreated){
			//Set initial electrical power.
			electricPower = 12;
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			world.beginProfiling("VehicleE_Level", true);
			//Get throttle and reverse state.
			throttle = getVariable(THROTTLE_VARIABLE);
			reverseThrust = isVariableActive(REVERSE_THRUST_VARIABLE);
			
			//If we have space for fuel, and we have tanks with it, transfer it.
			if(!world.isClient() && fuelTank.getFluidLevel() < definition.motorized.fuelCapacity - 100){
				for(APart part : parts){
					if(part instanceof PartInteractable && part.isActive && part.definition.interactable.feedsVehicles){
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
				if(!selectedBeaconName.isEmpty()){
					selectedBeacon = NavBeacon.getByNameFromWorld(world, selectedBeaconName);
				}else{
					selectedBeacon = null;
				}
			}
			
			//Do trailer-specific logic, if we are one and towed.
			//Otherwise, do normal update logic for DRLs.
			if(definition.motorized.isTrailer){
				//If we are being towed set the brake state to the same as the towing vehicle.
				//If we aren't being towed, set the parking brake.
				if(towedByConnection != null){
					if(parkingBrakeOn){
						toggleVariable(PARKINGBRAKE_VARIABLE);
					}
					setVariable(BRAKE_VARIABLE, towedByConnection.hitchVehicle.brake);
				}else{
					if(!parkingBrakeOn){
						toggleVariable(PARKINGBRAKE_VARIABLE);
					}
					if(brake != 0){
						setVariable(BRAKE_VARIABLE, 0);
					}
				}
			}else{
				//Set engine state mapping variables.
				enginesOn = false;
				enginesRunning = false;
				for(PartEngine engine : engines.values()){
					if(engine.magnetoOn){
						enginesOn = true;
						if(engine.running){
							enginesRunning = true;
							break;
						}
					}
				}
			}
			
			//Set electric usage based on light status.
			//Don't do this if we are a trailer.  Instead, get the towing vehicle's electric power.
			//If we are too damaged, don't hold any charge.
			if(definition.motorized.isTrailer){
				if(towedByConnection != null){
					electricPower = towedByConnection.hitchVehicle.electricPower;
				}
			}else if(damageAmount < definition.general.health){
				if(electricPower > 2 && renderTextLit()){
					electricUsage += 0.001F;
				}
				electricPower = Math.max(0, Math.min(13, electricPower -= electricUsage));
				electricFlow = electricUsage;
				electricUsage = 0;
			}else{
				electricPower = 0;
				electricFlow = 0;
				electricUsage = 0;
			}
			
			//Adjust gear variables.
			if(isVariableActive(EntityVehicleF_Physics.GEAR_VARIABLE)){
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
				if(rider instanceof WrapperPlayer && locationRiderMap.containsValue(rider) && getPartAtLocation(locationRiderMap.inverse().get(rider)).placementDefinition.isController && canPlayerStartEngines((WrapperPlayer) rider)){
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
							if(engine.magnetoOn){
								InterfacePacket.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.MAGNETO_VARIABLE));
							}
							if(engine.electricStarterEngaged){
								InterfacePacket.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.ELECTRIC_STARTER_VARIABLE));
							}
						}
						InterfacePacket.sendToServer(new PacketEntityVariableSet(this, BRAKE_VARIABLE, 0));
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
		}
	}
	
	@Override
	public void removePart(APart part, Iterator<APart> iterator){
		super.removePart(part, iterator);
		engines.inverse().remove(part);
	}
	
	public void acquireMissile(EntityBullet missile){
		//Add this missile with its current distance
		if(!missilesIncoming.contains(missile)){
			missilesIncoming.add(missile);
		}
	}
	
	public boolean canPlayerStartEngines(WrapperPlayer player){
		if(!ConfigSystem.configObject.general.keyRequiredToStartVehicles.value){
			return true;
		}else{
			if(player.isHoldingItemType(ItemComponentType.KEY)){
				String uniqueUUIDString = player.getHeldStack().getData().getString("vehicle");
				if(!uniqueUUIDString.isEmpty() && uniqueUUID.equals(UUID.fromString(uniqueUUIDString))){
					return true;
				}
			}
			if(world.isClient()){
				player.displayChatMessage(InterfaceCore.translate("interact.key.failure.needvehiclekey"));
			}
			return false;
		}
	}
	
	//-----START OF SOUND AND ANIMATION CODE-----
	@Override
	public boolean hasRadio(){
		return true;
	}
	
	@Override
	public boolean renderTextLit(){
		if(definition.motorized.hasRunningLights && isVariableActive(RUNNINGLIGHT_VARIABLE)) return electricPower > 3 && super.renderTextLit();
		if(definition.motorized.hasHeadlights && isVariableActive(HEADLIGHT_VARIABLE)) return electricPower > 3 && super.renderTextLit();
		if(definition.motorized.hasNavLights && isVariableActive(NAVIGATIONLIGHT_VARIABLE)) return electricPower > 3 && super.renderTextLit();
		if(definition.motorized.hasStrobeLights && isVariableActive(STROBELIGHT_VARIABLE)) return electricPower > 3 && super.renderTextLit();
		if(definition.motorized.hasTaxiLights && isVariableActive(TAXILIGHT_VARIABLE)) return electricPower > 3 && super.renderTextLit();
		if(definition.motorized.hasLandingLights && isVariableActive(LANDINGLIGHT_VARIABLE)) return electricPower > 3 && super.renderTextLit();
		return false;
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setDouble("electricPower", electricPower);
		data.setString("selectedBeaconName", selectedBeaconName);
		data.setData("fuelTank", fuelTank.save(new WrapperNBT()));
		return data;
	}
}
