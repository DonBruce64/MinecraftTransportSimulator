package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.components.IFluidTankProvider;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;

public class TileEntityFuelPump extends TileEntityDecor implements ITileEntityTickable, IFluidTankProvider{
	public EntityVehicleF_Physics connectedVehicle;
    private EntityFluidTank tank;

    public TileEntityFuelPump(WrapperWorld world, Point3d position, WrapperNBT data){
    	super(world, position, data);
    	this.tank = new EntityFluidTank(world, data, 15000);
    }
	
	@Override
	public void update(){
		//Update text lines to the current tank status if required.
		//Only do this on clients, as servers don't render any text.
		if(world.isClient() && definition.rendering != null && definition.rendering.textObjects != null){
			text.clear();
			String fluidName = tank.getFluidLevel() > 0 ? InterfaceCore.getFluidName(tank.getFluid()).toUpperCase() : "";
			String fluidLevel = InterfaceCore.translate("tile.fuelpump.level") + String.format("%04.1f", tank.getFluidLevel()/1000F) + "b";
			String fluidDispensed = InterfaceCore.translate("tile.fuelpump.dispensed") + String.format("%04.1f", tank.getAmountDispensed()/1000F) + "b";
			for(int i=0; i<definition.rendering.textObjects.size(); ++i){
				switch(i%3){
					case(0) : text.put(definition.rendering.textObjects.get(i), fluidName); break;
					case(1) : text.put(definition.rendering.textObjects.get(i),fluidLevel); break;
					case(2) : text.put(definition.rendering.textObjects.get(i),fluidDispensed); break;
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
				InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
				for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 25, 25, 25))){
					if(entity instanceof WrapperPlayer){
						((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((WrapperPlayer) entity, "interact.fuelpump.toofar"));
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
					InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
					connectedVehicle.beingFueled = false;
					connectedVehicle = null;
					for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))){
						if(entity instanceof WrapperPlayer){
							((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((WrapperPlayer) entity, "interact.fuelpump.complete"));
						}
					}
				}
			}else{
				//No more fuel.  Disconnect vehicle.
				InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
				connectedVehicle.beingFueled = false;
				connectedVehicle = null;
				for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))){
					if(entity instanceof WrapperPlayer){
						((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((WrapperPlayer) entity, "interact.fuelpump.empty"));
					}
				}
			}
		}
	}
	
	@Override
	public EntityFluidTank getTank(){
		return tank;
	}
	
	@Override
	public boolean canConnectOnAxis(Axis axis){
		return axis.equals(Axis.DOWN);
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		tank.save(data);
	}
}
