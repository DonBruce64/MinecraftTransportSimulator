package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.baseclasses.IFluidTankProvider;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

public class TileEntityFuelPump extends TileEntityDecor implements ITileEntityTickable, IFluidTankProvider{
	public EntityVehicleF_Physics connectedVehicle;
    private FluidTank tank;

    public TileEntityFuelPump(IWrapperWorld world, Point3i position, IWrapperNBT data){
    	super(world, position, data);
    	this.tank = new FluidTank(data, 15000, world.isClient());
    }
	
	@Override
	public void update(){
		//Update text lines to the current tank status if required.
		//Only do this on clients, as servers don't render any text.
		if(world.isClient() && definition.general.textObjects != null){
			textLines.clear();
			String fluidName = tank.getFluidLevel() > 0 ? MasterLoader.coreInterface.getFluidName(tank.getFluid()).toUpperCase() : "";
			String fluidLevel = MasterLoader.coreInterface.translate("tile.fuelpump.level") + String.format("%04.1f", tank.getFluidLevel()/1000F) + "b";
			String fluidDispensed = MasterLoader.coreInterface.translate("tile.fuelpump.dispensed") + String.format("%04.1f", tank.getAmountDispensed()/1000F) + "b";
			for(int i=0; i<definition.general.textObjects.size(); ++i){
				switch(i%3){
					case(0) : textLines.add(fluidName); break;
					case(1) : textLines.add(fluidLevel); break;
					case(2) : textLines.add(fluidDispensed); break;
				}
			}
		}
		
		//Do fuel checks.  Fuel checks only occur on servers.  Clients get packets for state changes.
		if(connectedVehicle != null && !world.isClient()){
			//Don't fuel vehicles that don't exist.
			if(!connectedVehicle.isValid){
				connectedVehicle.beingFueled = false;
				connectedVehicle = null;
				return;
			}
			
			//Check distance to make sure the vehicle hasn't moved away.
			if(connectedVehicle.position.distanceTo(position) > 16){
				MasterLoader.networkInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
				for(IWrapperEntity entity : world.getEntitiesWithin(new BoundingBox(new Point3d(position), 25, 25, 25))){
					if(entity instanceof IWrapperPlayer){
						((IWrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage("interact.fuelpump.toofar"));
					}
				}
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
					MasterLoader.networkInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
					connectedVehicle.beingFueled = false;
					connectedVehicle = null;
					for(IWrapperEntity entity : world.getEntitiesWithin(new BoundingBox(new Point3d(position), 16, 16, 16))){
						if(entity instanceof IWrapperPlayer){
							((IWrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage("interact.fuelpump.complete"));
						}
					}
				}
			}else{
				//No more fuel.  Disconnect vehicle.
				MasterLoader.networkInterface.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
				connectedVehicle.beingFueled = false;
				connectedVehicle = null;
				for(IWrapperEntity entity : world.getEntitiesWithin(new BoundingBox(new Point3d(position), 16, 16, 16))){
					if(entity instanceof IWrapperPlayer){
						((IWrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage("interact.fuelpump.empty"));
					}
				}
			}
		}
	}
	
	@Override
	public FluidTank getTank(){
		return tank;
	}
	
	@Override
	public void save(IWrapperNBT data){
		super.save(data);
		tank.save(data);
	}
}
