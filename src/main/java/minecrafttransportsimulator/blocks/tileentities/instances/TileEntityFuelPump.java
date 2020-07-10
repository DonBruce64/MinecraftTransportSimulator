package minecrafttransportsimulator.blocks.tileentities.instances;

import mcinterface.InterfaceNetwork;
import mcinterface.WrapperNBT;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityFluidTank;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPumpConnection;
import minecrafttransportsimulator.rendering.instances.RenderDecor;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public class TileEntityFuelPump extends ATileEntityFluidTank<JSONDecor>implements ITileEntityTickable{
	public JSONDecor definition;
	public EntityVehicleF_Physics connectedVehicle;
    public int totalTransfered;
	
	@Override
	public void update(){
		//Do fuel checks.  Fuel checks only occur on servers.  Clients get packets for state changes.
		if(connectedVehicle != null & !world.isClient()){
			//Don't fuel vehicles that don't exist.
			if(connectedVehicle.isValid){
				connectedVehicle = null;
				return;
			}
			
			//Check distance to make sure the vehicle hasn't moved away.
			if(connectedVehicle.position.distanceTo(position) > 20){
				connectedVehicle = null;
				InterfaceNetwork.sendToAllClients(new PacketTileEntityPumpConnection(this));
				InterfaceNetwork.sendToClientsNear(new PacketPlayerChatMessage("interact.fuelpump.toofar"), world.getDimensionID(), position, 25);
				return;
			}
			//If we have room for fuel, try to add it to the vehicle.
			if(connectedVehicle.definition.motorized.fuelCapacity - connectedVehicle.fuel >= 10){
				if(!getFluid().isEmpty()){
					connectedVehicle.fluidName = getFluid();
					int fuelDrained = drain(getFluid(), 10, true);
					connectedVehicle.fuel += fuelDrained;
					totalTransfered += fuelDrained;
				}else{
					//No more fuel.  Disconnect vehicle.
					connectedVehicle = null;
					InterfaceNetwork.sendToAllClients(new PacketTileEntityPumpConnection(this));
					InterfaceNetwork.sendToClientsNear(new PacketPlayerChatMessage("interact.fuelpump.empty"), world.getDimensionID(), position, 16);
				}
			}else{
				//No more room in the vehicle.  Disconnect.
				connectedVehicle = null;
				InterfaceNetwork.sendToClientsNear(new PacketPlayerChatMessage("interact.fuelpump.complete"), world.getDimensionID(), position, 16);
			}
		}
	}
	
	@Override
	public int drain(String fluid, int maxAmount, boolean doDrain){
		int fuelDrained = super.drain(fluid, maxAmount, doDrain);
		//Need to fuel vehicle with fuel we've drained on the server.
		if(world.isClient() && fuelDrained < 1000){
			if(connectedVehicle != null){
				connectedVehicle.fuel += fuelDrained;
				totalTransfered += fuelDrained;
			}
		}
		return fuelDrained;
	}
	
	@Override
	public int getMaxLevel(){
		return 15000;
	}

	@Override
	public RenderDecor getRenderer(){
		return new RenderDecor();
	}
	
	@Override
	public void load(WrapperNBT data){
		super.load(data);
		totalTransfered = data.getInteger("totalTransfered");
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setInteger("totalTransfered", totalTransfered);
	}
}
