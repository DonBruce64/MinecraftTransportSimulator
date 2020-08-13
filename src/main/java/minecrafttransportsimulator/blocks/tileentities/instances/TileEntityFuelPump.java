package minecrafttransportsimulator.blocks.tileentities.instances;

import mcinterface.InterfaceNetwork;
import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.baseclasses.IFluidTankProvider;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPumpConnection;
import minecrafttransportsimulator.rendering.instances.RenderDecor;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public class TileEntityFuelPump extends ATileEntityBase<JSONDecor>implements ITileEntityTickable, IFluidTankProvider{
	public JSONDecor definition;
	public EntityVehicleF_Physics connectedVehicle;
    private FluidTank tank;

    public TileEntityFuelPump(WrapperWorld world, Point3i position, WrapperNBT data){
    	super(world, position, data);
    	this.tank = new FluidTank(data, 15000, !world.isClient());
    }
	
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
				if(!tank.getFluid().isEmpty()){
					connectedVehicle.fluidName = tank.getFluid();
					int fuelDrained = tank.drain(tank.getFluid(), 10, true);
					connectedVehicle.fuel += fuelDrained;
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
	public FluidTank getTank(){
		return tank;
	}

	@SuppressWarnings("unchecked")
	@Override
	public RenderDecor getRenderer(){
		return new RenderDecor();
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		tank.save(data);
	}
}
