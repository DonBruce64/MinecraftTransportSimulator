package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.items.instances.ItemJerrycan;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.packets.components.NetworkSystem;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import net.minecraft.item.ItemStack;

public class BlockFuelPump extends ABlockBaseDecor<TileEntityFuelPump>{
	
	public BlockFuelPump(){
		super();
	}
	
	@Override
	public boolean onClicked(IWrapperWorld world, Point3i point, Axis axis, IWrapperPlayer player){
		//Only check right-clicks on the server.
		if(!world.isClient()){
			TileEntityFuelPump pump = (TileEntityFuelPump) world.getTileEntity(point);
			FluidTank tank = pump.getTank();
			
			//If we are holding an item, interact with the pump.
			ItemStack stack = player.getHeldStack();
			if(tank.interactWith(player) > 0){
				return true;
			}
			
			//Check if the item is a jerrycan.
			if(player.getHeldItem() instanceof ItemJerrycan){
				WrapperNBT data = new WrapperNBT(stack);
				if(!data.getBoolean("isFull")){
					if(tank.getFluidLevel() >= 1000){
						data.setBoolean("isFull", true);
						data.setString("fluidName", tank.getFluid());
						stack.setTagCompound(data.tag);
						tank.drain(tank.getFluid(), 1000, true);
					}
				}
				return true;
			}
        	
			//We don't have a vehicle connected.  Try to connect one now.
    		if(pump.connectedVehicle == null){
    			//Get the closest vehicle within a 16-block radius.
    			EntityVehicleF_Physics nearestVehicle = null;
    			double lowestDistance = 16D;
    			for(AEntityBase entity : AEntityBase.createdServerEntities){
    				if(entity instanceof EntityVehicleF_Physics){
    					double entityDistance = entity.position.distanceTo(point);
    					if(entityDistance < lowestDistance){
    						lowestDistance = entityDistance;
    						nearestVehicle = (EntityVehicleF_Physics) entity;
    					}
    				}
    			}
    			
    			//Have a vehicle, try to connect to it.
    			if(nearestVehicle != null){
    				if(tank.getFluidLevel() == 0){
    					//No fuel in the pump.
    					player.sendPacket(new PacketPlayerChatMessage("interact.fuelpump.nofuel"));
    				}else{
        				//Check to make sure this vehicle can take this fuel pump's fuel type.
    					if(!nearestVehicle.fuelTank.getFluid().isEmpty()){
    						if(!tank.getFluid().equals(nearestVehicle.fuelTank.getFluid())){
    							player.sendPacket(new PacketPlayerChatMessage("interact.fuelpump.wrongtype"));
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
    								NetworkSystem.sendToAllClients(new PacketTileEntityFuelPumpConnection(pump, true));
    								player.sendPacket(new PacketPlayerChatMessage("interact.fuelpump.connect"));
    	    						return true;
    							}
    						}
    					}
    					player.sendPacket(new PacketPlayerChatMessage("interact.fuelpump.wrongengines"));
    				}
    			}else{
    				player.sendPacket(new PacketPlayerChatMessage("interact.fuelpump.toofar"));
    			}
    		}else{
    			//Connected vehicle exists, disconnect it.
    			NetworkSystem.sendToAllClients(new PacketTileEntityFuelPumpConnection(pump, false));
    			pump.connectedVehicle.beingFueled = false;
    			pump.connectedVehicle = null;
    			player.sendPacket(new PacketPlayerChatMessage("interact.fuelpump.disconnect"));
    		}
		}
		return true;
	}
	
    @Override
	public TileEntityFuelPump createTileEntity(IWrapperWorld world, Point3i position, WrapperNBT data){
		return new TileEntityFuelPump(world, position, data);
	}

	@Override
	public Class<TileEntityFuelPump> getTileEntityClass(){
		return TileEntityFuelPump.class;
	}
}
