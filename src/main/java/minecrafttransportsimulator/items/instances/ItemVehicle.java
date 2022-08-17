package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;

public class ItemVehicle extends AItemSubTyped<JSONVehicle> implements IItemEntityProvider<EntityVehicleF_Physics>{
	
	public ItemVehicle(JSONVehicle definition, String subName, String sourcePackID){
		super(definition, subName, sourcePackID);
	}
	
	@Override
	public boolean onBlockClicked(AWrapperWorld world, IWrapperPlayer player, Point3D position, Axis axis){
		if(!world.isClient()){
			IWrapperItemStack heldStack = player.getHeldStack();
			//Make sure the definition is set in the NBT we will be giving to our new entity.
			IWrapperNBT data = heldStack.getData();
			populateDefaultData(data);
			boolean wasSaved = !data.getString("uniqueUUID").isEmpty();
			
			//First construct the class.
			//This takes into account all saved data in the stack, so the vehicle will re-load its data from it
			//as if it has been saved in the world rather than into an item.  If there's no data,
			//then we just make a blank, new instance.
			EntityVehicleF_Physics vehicle = createEntity(world, player, data);
			vehicle.addPartsPostAddition(player, data);
			
			//If we have a default fuel, add it now as we SHOULD have an engine to tell
			//us what fuel type we will need to add.
			if(!wasSaved && vehicle.definition.motorized.defaultFuelQty > 0){
				for(APart part : vehicle.parts){
					if(part instanceof PartEngine){
						//Get the most potent fuel for the vehicle from the fuel configs.
						String mostPotentFluid = "";
						for(String fluidName : ConfigSystem.settings.fuel.fuels.get(part.definition.engine.fuelType).keySet()){
							if(mostPotentFluid.isEmpty() || ConfigSystem.settings.fuel.fuels.get(part.definition.engine.fuelType).get(mostPotentFluid) < ConfigSystem.settings.fuel.fuels.get(part.definition.engine.fuelType).get(fluidName)){
								mostPotentFluid = fluidName;
							}
						}
						vehicle.fuelTank.manuallySet(mostPotentFluid, vehicle.definition.motorized.defaultFuelQty);
						break;
					}
				}
				if(vehicle.fuelTank.getFluid().isEmpty()){
					player.sendPacket(new PacketPlayerChatMessage(player, "A defaultFuelQty was specified for: " + vehicle.definition.packID + ":" + vehicle.definition.systemName + ", but no engine was noted as a defaultPart, so we don't know what fuel to put in the vehicle.  Vehicle will be spawned without fuel and engine."));
				}
			}
			
			//Entity is valid.  Spawn it into the world.
			vehicle.world.spawnEntity(vehicle);
			
			//Decrement stack if we are not in creative.
			if(!player.isCreative()){
				player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
			}
		}
		return true;
	}
	
	@Override
	public void populateDefaultData(IWrapperNBT data){
		super.populateDefaultData(data);
		//Make sure we don't restore any world-based entity properties.
		data.setPoint3d("position", new Point3D());
		data.setPoint3d("motion", new Point3D());
		data.setPoint3d("angles", new Point3D());
		data.setPoint3d("rotation", new Point3D());
	}
	
	@Override
	public void repair(IWrapperNBT data){
		super.repair(data);
		data.setDouble("electricPower", 12);
	}

	@Override
	public EntityVehicleF_Physics createEntity(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data){
		return new EntityVehicleF_Physics(world, placingPlayer, data);
	}

	@Override
	public Class<EntityVehicleF_Physics> getEntityClass(){
		return EntityVehicleF_Physics.class;
	}
}
