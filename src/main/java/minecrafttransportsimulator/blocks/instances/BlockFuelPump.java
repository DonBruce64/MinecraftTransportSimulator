package minecrafttransportsimulator.blocks.instances;

import mcinterface.InterfaceNetwork;
import mcinterface.WrapperItemStack;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.items.instances.ItemJerrycan;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPumpConnection;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;

public class BlockFuelPump extends ABlockBase implements IBlockTileEntity<TileEntityFuelPump>{
	
	public BlockFuelPump(){
		super(10.0F, 5.0F);
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3i point, Axis axis, WrapperPlayer player){
		//Only check right-clicks on the server.
		if(!world.isClient()){
			TileEntityFuelPump pump = (TileEntityFuelPump) world.getTileEntity(point);
			FluidTank tank = pump.getTank();
			
			//If we are holding an item, interact with the pump.
			WrapperItemStack stack = player.getHeldStack();
			if(stack.interactWithTank(tank, player) > 0){
				return true;
			}
			
			//Check if the item is a jerrycan.
			if(stack.getItem() instanceof ItemJerrycan){
				WrapperNBT data = stack.getData();
				if(!data.getBoolean("isFull")){
					if(tank.getFluidLevel() >= 1000){
						data.setBoolean("isFull", true);
						data.setString("fluidName", tank.getFluid());
						stack.setData(data);
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
    								InterfaceNetwork.sendToAllClients(new PacketTileEntityPumpConnection(pump, true));
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
    			InterfaceNetwork.sendToAllClients(new PacketTileEntityPumpConnection(pump, false));
    			pump.connectedVehicle.beingFueled = false;
    			pump.connectedVehicle = null;
    			player.sendPacket(new PacketPlayerChatMessage("interact.fuelpump.disconnect"));
    		}
		}
		return true;
	}
	
    @Override
	public TileEntityFuelPump createTileEntity(WrapperWorld world, Point3i position, WrapperNBT data) {
		return new TileEntityFuelPump(world, position, data);
	}

	@Override
	public Class<TileEntityFuelPump> getTileEntityClass(){
		return TileEntityFuelPump.class;
	}
}
