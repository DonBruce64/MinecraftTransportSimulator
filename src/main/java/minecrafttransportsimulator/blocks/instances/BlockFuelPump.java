package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemItem.ItemComponentType;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.item.ItemStack;

public class BlockFuelPump extends ABlockBaseDecor<TileEntityFuelPump>{
	
	public BlockFuelPump(){
		super();
	}
	
	@Override
	public void onPlaced(WrapperWorld world, Point3d position, WrapperPlayer player){
		//Set placing player for reference.
		TileEntityFuelPump pump = world.getTileEntity(position);
		pump.placingPlayerID = player.getID();
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3d position, Axis axis, WrapperPlayer player){
		//Only check right-clicks on the server.
		if(!world.isClient()){
			TileEntityFuelPump pump = (TileEntityFuelPump) world.getTileEntity(position);
			EntityFluidTank tank = pump.getTank();
			
			//If we are holding an item, interact with the pump.
			ItemStack stack = player.getHeldStack();
			AItemBase item = player.getHeldItem();
			if(tank.interactWith(player) > 0){
				return true;
			}
			
			//Check if the item is a jerrycan.
			if(item instanceof ItemPartInteractable){
				ItemPartInteractable interactable = (ItemPartInteractable) item;
				if(interactable.definition.interactable.interactionType.equals(InteractableComponentType.JERRYCAN)){
					WrapperNBT data = new WrapperNBT(stack);
					if(data.getString("jerrycanFluid").isEmpty()){
						if(tank.getFluidLevel() >= 1000){
							data.setString("jerrycanFluid", tank.getFluid());
							stack.setTagCompound(data.tag);
							tank.drain(tank.getFluid(), 1000, true);
						}
					}
					return true;
				}
			}
			
			//Check if the item is a wrench, and the player can configure this pump..
			AItemBase heldItem = player.getHeldItem();
			if(heldItem instanceof ItemItem && ((ItemItem) heldItem).definition.item.type.equals(ItemComponentType.WRENCH) && (player.getID().equals(pump.placingPlayerID) || player.isOP())){
				player.sendPacket(new PacketEntityGUIRequest(pump, player, PacketEntityGUIRequest.EntityGUIType.FUEL_PUMP_CONFIG));
				return true;
			}
			
			//If we aren't a creative pump, and we don't have fuel, bring up the GUI so the player can buy some.
			if(!pump.isCreative && pump.fuelPurchasedRemaining == 0 && tank.getFluidLevel() <= 1){
				player.sendPacket(new PacketEntityGUIRequest(pump, player, PacketEntityGUIRequest.EntityGUIType.FUEL_PUMP));
				return true;
			}
        	
			//We don't have a vehicle connected.  Try to connect one now.
    		if(pump.connectedVehicle == null){
    			//Get the closest vehicle within a 16-block radius.
    			EntityVehicleF_Physics nearestVehicle = null;
    			double lowestDistance = 16D;
    			for(AEntityA_Base entity : AEntityA_Base.getEntities(world)){
    				if(entity instanceof EntityVehicleF_Physics){
    					EntityVehicleF_Physics testVehicle = (EntityVehicleF_Physics) entity;
    					double vehicleDistance = testVehicle.position.distanceTo(position);
    					if(vehicleDistance < lowestDistance){
    						lowestDistance = vehicleDistance;
    						nearestVehicle = testVehicle;
    					}
    				}
    			}
    			
    			//Have a vehicle, try to connect to it.
    			if(nearestVehicle != null){
    				if(tank.getFluidLevel() == 0){
    					//No fuel in the pump.
    					player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelpump.nofuel"));
    				}else{
        				//Check to make sure this vehicle can take this fuel pump's fuel type.
    					if(!nearestVehicle.fuelTank.getFluid().isEmpty()){
    						if(!tank.getFluid().equals(nearestVehicle.fuelTank.getFluid())){
    							player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelpump.wrongtype"));
    							return true;
    						}
    					}
    					
    					//Fuel type can be taken by vehicle, check to make sure engines can take it.
    					for(APart part : nearestVehicle.parts){
    						if(part instanceof PartEngine){
    							if(ConfigSystem.configObject.fuel.fuels.get(part.definition.engine.fuelType).containsKey(tank.getFluid())){
    								pump.connectedVehicle = nearestVehicle;
    								pump.connectedVehicle.beingFueled = true;
    								tank.resetAmountDispensed();
    								InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(pump, true));
    								player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelpump.connect"));
    	    						return true;
    							}
    						}
    					}
    					player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelpump.wrongengines"));
    				}
    			}else{
    				player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelpump.toofar"));
    			}
    		}else{
    			//Connected vehicle exists, disconnect it.
    			InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(pump, false));
    			pump.connectedVehicle.beingFueled = false;
    			pump.connectedVehicle = null;
    			player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelpump.disconnect"));
    		}
		}
		return true;
	}
	
    @Override
	public TileEntityFuelPump createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityFuelPump(world, position, data);
	}

	@Override
	public Class<TileEntityFuelPump> getTileEntityClass(){
		return TileEntityFuelPump.class;
	}
}
