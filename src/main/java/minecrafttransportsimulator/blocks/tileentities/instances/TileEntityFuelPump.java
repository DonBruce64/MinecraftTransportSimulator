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
    	this.tank = new FluidTank(data, 15000, world.isClient());
    }
	
	@Override
	public void update(){
		//Do fuel checks.  Fuel checks only occur on servers.  Clients get packets for state changes.
		if(connectedVehicle != null & !world.isClient()){
			//Don't fuel vehicles that don't exist.
			if(!connectedVehicle.isValid){
				connectedVehicle.beingFueled = false;
				connectedVehicle = null;
				return;
			}
			
			//Check distance to make sure the vehicle hasn't moved away.
			if(connectedVehicle.position.distanceTo(position) > 20){
				InterfaceNetwork.sendToAllClients(new PacketTileEntityPumpConnection(this, false));
				InterfaceNetwork.sendToClientsNear(new PacketPlayerChatMessage("interact.fuelpump.toofar"), world.getDimensionID(), position, 25);
				connectedVehicle.beingFueled = false;
				connectedVehicle = null;
				return;
			}
			//If we have room for fuel, try to add it to the vehicle.
			if(tank.getFluidLevel() > 0){
			double amountToFill = connectedVehicle.fuelTank.fill(tank.getFluid(), 10, false);
				if(amountToFill > 0){
					double amountToDrain = tank.drain(tank.getFluid(), amountToFill, false);
					connectedVehicle.fuelTank.fill(tank.getFluid(), amountToDrain, true);
					tank.drain(tank.getFluid(), amountToDrain, true);
				}else{
					//No more room in the vehicle.  Disconnect.
					InterfaceNetwork.sendToAllClients(new PacketTileEntityPumpConnection(this, false));
					connectedVehicle.beingFueled = false;
					connectedVehicle = null;
					InterfaceNetwork.sendToClientsNear(new PacketPlayerChatMessage("interact.fuelpump.complete"), world.getDimensionID(), position, 16);	
				}
			}else{
				//No more fuel.  Disconnect vehicle.
				InterfaceNetwork.sendToAllClients(new PacketTileEntityPumpConnection(this, false));
				connectedVehicle.beingFueled = false;
				connectedVehicle = null;
				InterfaceNetwork.sendToClientsNear(new PacketPlayerChatMessage("interact.fuelpump.empty"), world.getDimensionID(), position, 16);
			}
		}
	}
	
	@Override
	public FluidTank getTank(){
		return tank;
	}

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
