package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONDoor;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.JSONInstrumentDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.item.ItemStack;

public class ItemVehicle extends AItemSubTyped<JSONVehicle> implements IItemEntityProvider<EntityVehicleF_Physics>{
	
	public ItemVehicle(JSONVehicle definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}
	
	@Override
	public boolean onBlockClicked(WrapperWorld world, WrapperPlayer player, Point3d position, Axis axis){
		if(!world.isClient()){
			ItemStack heldStack = player.getHeldStack();
			//We want to spawn above this block.
			++position.y;
			
			//Make sure the definition is set in the NBT we will be giving to our new entity.
			WrapperNBT data = validateData(new WrapperNBT(heldStack));
			boolean wasSaved = !data.getString("uniqueUUID").isEmpty();
			
			//First construct the class.
			//This takes into account all saved data in the stack, so the vehicle will re-load its data from it
			//as if it has been saved in the world rather than into an item.  If there's no data,
			//then we just make a blank, new instance.
			EntityVehicleF_Physics newVehicle = createEntity(world, player, data);
			
			//Set position to the spot that was clicked by the player.
			//Add a -90 rotation offset so the vehicle is facing perpendicular.
			//Makes placement easier and is less likely for players to get stuck.
			newVehicle.position.set(position.x, position.y, position.z);
			newVehicle.prevPosition.setTo(newVehicle.position);
			newVehicle.angles.y = player.getYaw() + 90;
			newVehicle.prevAngles.setTo(newVehicle.angles);
			
			//If we are a new vehicle, set default properties, if required.
			if(!wasSaved){
				//Set initial electrical power.
				newVehicle.electricPower = 12;
				
				//Add default parts via the vehicle's recursion.
				newVehicle.addDefaultParts(newVehicle.definition.parts, true);
				
				//Add default instruments.
				for(JSONInstrumentDefinition packInstrument : newVehicle.definition.motorized.instruments){
					if(packInstrument.defaultInstrument != null){
						try{
							String instrumentPackID = packInstrument.defaultInstrument.substring(0, packInstrument.defaultInstrument.indexOf(':'));
							String instrumentSystemName = packInstrument.defaultInstrument.substring(packInstrument.defaultInstrument.indexOf(':') + 1);
							try{
								ItemInstrument instrument = PackParserSystem.getItem(instrumentPackID, instrumentSystemName);
								if(instrument != null){
									newVehicle.instruments.put(newVehicle.definition.motorized.instruments.indexOf(packInstrument), instrument);
									continue;
								}
							}catch(NullPointerException e){}
							throw new IllegalArgumentException("Attempted to add defaultInstrument: " + instrumentPackID + ":" + instrumentSystemName + " to: " + newVehicle.definition.packID + ":" + newVehicle.definition.systemName + " but that instrument doesn't exist in the pack item registry.");
						}catch(IndexOutOfBoundsException e){
							throw new IllegalArgumentException("Could not parse defaultInstrument definition: " + packInstrument.defaultInstrument + ".  Format should be \"packId:instrumentName\"");
						}
					}
				}
				
				//If we have a default fuel, add it now as we SHOULD have an engine to tell
				//us what fuel type we will need to add.
				if(newVehicle.definition.motorized.defaultFuelQty > 0){
					for(APart part : newVehicle.partsFromNBT){
						if(part instanceof PartEngine){
							//Get the most potent fuel for the vehicle from the fuel configs.
							String mostPotentFluid = "";
							for(String fluidName : ConfigSystem.configObject.fuel.fuels.get(part.definition.engine.fuelType).keySet()){
								if(mostPotentFluid.isEmpty() || ConfigSystem.configObject.fuel.fuels.get(part.definition.engine.fuelType).get(mostPotentFluid) < ConfigSystem.configObject.fuel.fuels.get(part.definition.engine.fuelType).get(fluidName)){
									mostPotentFluid = fluidName;
								}
							}
							newVehicle.fuelTank.manuallySet(mostPotentFluid, newVehicle.definition.motorized.defaultFuelQty);
							break;
						}
					}
					if(newVehicle.fuelTank.getFluid().isEmpty()){
						throw new IllegalArgumentException("A defaultFuelQty was specified for: " + newVehicle.definition.packID + ":" + newVehicle.definition.systemName + ", but no engine was noted as a defaultPart, so we don't know what fuel to put in the vehicle.");
					}
				}
				
				//Open all doors.  This lets players know we can close them and put things in slots.
				if(definition.doors != null){
					for(JSONDoor door : definition.doors){
						if(!door.closedByDefault){
							newVehicle.variablesOn.add(door.name);
						}
					}
				}
				for(APart part : newVehicle.parts){
					if(part.definition.doors != null){
						for(JSONDoor door : part.definition.doors){
							if(!door.closedByDefault){
								newVehicle.variablesOn.add(door.name);
							}
						}
					}
				}
			}
			
			//Get how far above the ground the vehicle needs to be, and move it to that position.
			//First boost Y based on collision boxes.
			double furthestDownPoint = 0;
			for(JSONCollisionBox collisionBox : newVehicle.definition.collision){
				furthestDownPoint = Math.min(collisionBox.pos.y - collisionBox.height/2F, furthestDownPoint);
			}
			
			//Next, boost based on parts.
			for(APart part : newVehicle.parts){
				furthestDownPoint = Math.min(part.placementOffset.y - part.getHeight()/2F, furthestDownPoint);
			}
			
			//Add on -0.1 blocks for the default collision clamping.
			//This prevents the clamping of the collision boxes from hitting the ground if they were clamped.
			furthestDownPoint += -0.1;
			
			//Apply the boost, and check collisions.
			//If the core collisions are colliding, set the vehicle as dead and abort.
			//We need to update the boxes first, however, as they haven't been updated yet.
			newVehicle.position.y += -furthestDownPoint;
			for(BoundingBox coreBox : newVehicle.blockCollisionBoxes){
				coreBox.updateToEntity(newVehicle, null);
				if(coreBox.updateCollidingBlocks(newVehicle.world, new Point3d(0D, -furthestDownPoint, 0D))){
					//New vehicle shouldn't be spawned.  Bail out.
					player.sendPacket(new PacketPlayerChatMessage("interact.failure.nospace"));
					return false;
				}
			}
			
			//Entity is valid.  Spawn it into the world.
			newVehicle.world.spawnEntity(newVehicle);
			
			//Decrement stack if we are not in creative.
			if(!player.isCreative()){
				player.getInventory().removeStack(heldStack, 1);
			}
		}
		return true;
	}
	
	@Override
	protected void populateDefaultData(WrapperNBT data){
		super.populateDefaultData(data);
		//Make sure we don't restore any world-based entity properties.
		data.setPoint3d("position", new Point3d());
		data.setPoint3d("motion", new Point3d());
		data.setPoint3d("angles", new Point3d());
		data.setPoint3d("rotation", new Point3d());
	}

	@Override
	public EntityVehicleF_Physics createEntity(WrapperWorld world, WrapperPlayer playerSpawning, WrapperNBT data){
		return new EntityVehicleF_Physics(world, data);
	}

	@Override
	public Class<EntityVehicleF_Physics> getEntityClass(){
		return EntityVehicleF_Physics.class;
	}
}
