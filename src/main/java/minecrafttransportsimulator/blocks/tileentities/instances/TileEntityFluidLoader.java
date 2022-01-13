package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.jsondefs.JSONDecor.DecorComponentType;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFluidLoaderConnection;

public class TileEntityFluidLoader extends TileEntityDecor implements ITileEntityFluidTankProvider{
	public PartInteractable connectedPart;
    private EntityFluidTank tank;
    public final boolean isUnloader;

    public TileEntityFluidLoader(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, position, placingPlayer, data);
    	this.tank = new EntityFluidTank(world, data.getDataOrNew("tank"), definition.decor.fuelCapacity);
    	this.isUnloader = definition.decor.type.equals(DecorComponentType.FLUID_UNLOADER);
    }
	
    @Override
	public boolean update(){
		if(super.update()){
			//Do load/unload checks.  Checks only occur on servers.  Clients get packets for state changes.
			if(!world.isClient()){
				if(connectedPart == null){
					//Check for a new part every second.  We don't want every tick as this would increase server loads.
					if(ticksExisted%20 == 0){
						updateNearestPart();
					}
				}else{
					//Don't fuel parts that don't exist.
					//Also check distance to make sure the part hasn't moved away.
					if(!connectedPart.isValid || connectedPart.position.distanceTo(position) > 10){
						updateNearestPart();
					}
				}
	
				//If we have a connected part, try to load or unload from it depending on our state.
				if(connectedPart != null){
					if(isUnloader){
						String fluidToUnload = connectedPart.tank.getFluid();
						double amountToUnload = connectedPart.tank.drain(fluidToUnload, definition.decor.pumpRate, false);
						if(amountToUnload > 0){
							amountToUnload = tank.fill(fluidToUnload, amountToUnload, true);
							connectedPart.tank.drain(fluidToUnload, amountToUnload, true);
						}else{
							updateNearestPart();
						}
					}else{
						String fluidToLoad = tank.getFluid();
						double amountToLoad = connectedPart.tank.fill(fluidToLoad, definition.decor.pumpRate, false);
						if(amountToLoad > 0){
							amountToLoad = tank.drain(fluidToLoad, amountToLoad, true);
							connectedPart.tank.fill(fluidToLoad, amountToLoad, true);
						}else{
							updateNearestPart();
						}
					}
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	private void updateNearestPart(){
		//Don't bother searching if we can't fill or drain.
		PartInteractable nearestPart = null;
		double nearestDistance = 999;
		if((tank.getFluidLevel() > 0 && !isUnloader) || (tank.getFluidLevel() < tank.getMaxLevel() && isUnloader)){
			for(PartInteractable interactablePart : world.getEntitiesOfType(PartInteractable.class)){
				if(interactablePart.tank != null){
					if(isUnloader){
						if(interactablePart.tank.drain(tank.getFluid(), 1, false) > 0){
							if(interactablePart.position.distanceTo(position) < nearestDistance){
								nearestPart = interactablePart;
							}
						}
					}else{
						if(interactablePart.tank.fill(tank.getFluid(), 1, false) > 0){
							if(interactablePart.position.distanceTo(position) < nearestDistance){
								nearestPart = interactablePart;
							}
						}
					}
				}
			}
		}
		if(nearestPart != null){
			connectedPart = nearestPart;
			InterfacePacket.sendToAllClients(new PacketTileEntityFluidLoaderConnection(this, true));
		}else if(connectedPart != null){
			InterfacePacket.sendToAllClients(new PacketTileEntityFluidLoaderConnection(this, false));
			connectedPart = null;
		}
	}
	
	@Override
	public EntityFluidTank getTank(){
		return tank;
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setData("tank", tank.save(new WrapperNBT()));
		return data;
	}
}
