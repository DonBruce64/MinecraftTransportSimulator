package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.baseclasses.IFluidTankProvider;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFluidLoaderConnection;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;

public class TileEntityFluidLoader extends TileEntityDecor implements ITileEntityTickable, IFluidTankProvider{
	public PartInteractable connectedPart;
	public boolean unloadMode;
	public boolean loading;
	public boolean unloading;
    private FluidTank tank;

    public TileEntityFluidLoader(IWrapperWorld world, Point3i position, IWrapperNBT data){
    	super(world, position, data);
    	this.tank = new FluidTank(data, 15000, world.isClient());
    	this.unloadMode = data.getBoolean("unloadMode");
    }
	
	@Override
	public void update(){
		//Do load/unload checks.  Checks only occur on servers.  Clients get packets for state changes.
		if(!world.isClient()){
			if(connectedPart == null){
				//Check for a new part every second.  We don't want every tick as this would increase server loads.
				if(world.getTime()%20 == 0){
					updateNearestPart();
				}
			}else{
				//Don't fuel vehicles or parts that don't exist.
				//Also check distance to make sure the part hasn't moved away.
				if(!connectedPart.vehicle.isValid || !connectedPart.isValid || connectedPart.worldPos.distanceTo(position) > 10){
					updateNearestPart();
				}
			}

			//If we have a connected part, try to load or unload from it depending on our state.
			if(connectedPart != null){
				if(unloadMode){
					String fluidToUnload = connectedPart.tank.getFluid();
					double amountToUnload = connectedPart.tank.drain(fluidToUnload, 100, false);
					if(amountToUnload > 0){
						amountToUnload = tank.fill(fluidToUnload, amountToUnload, true);
						connectedPart.tank.drain(fluidToUnload, amountToUnload, true);
					}else{
						updateNearestPart();
					}
				}else{
					String fluidToLoad = tank.getFluid();
					double amountToLoad = connectedPart.tank.fill(fluidToLoad, 100, false);
					if(amountToLoad > 0){
						amountToLoad = tank.drain(fluidToLoad, amountToLoad, true);
						connectedPart.tank.fill(fluidToLoad, amountToLoad, true);
					}else{
						updateNearestPart();
					}
				}
			}
		}
	}
	
	private void updateNearestPart(){
		//Don't bother searching if we can't fill or drain.
		PartInteractable nearestPart = null;
		double nearestDistance = 999;
		if((tank.getFluidLevel() > 0 && !unloadMode) || (tank.getFluidLevel() < tank.getMaxLevel() && unloadMode)){
			for(AEntityBase entity : AEntityBase.createdServerEntities){
				if(entity instanceof EntityVehicleF_Physics){
					if(entity.position.distanceTo(position) < 100){
						for(APart part : ((EntityVehicleF_Physics) entity).parts){
							if(part.worldPos.distanceTo(position) < 10){
								if(part instanceof PartInteractable){
									FluidTank partTank = ((PartInteractable) part).tank;
									if(partTank != null){
										if(unloadMode){
											if(partTank.drain(tank.getFluid(), 1, false) > 0){
												if(part.worldPos.distanceTo(position) < nearestDistance){
													nearestPart = (PartInteractable) part;
												}
											}
										}else{
											if(partTank.fill(tank.getFluid(), 1, false) > 0){
												if(part.worldPos.distanceTo(position) < nearestDistance){
													nearestPart = (PartInteractable) part;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		if(nearestPart != null){
			connectedPart = nearestPart;
			MasterLoader.networkInterface.sendToAllClients(new PacketTileEntityFluidLoaderConnection(this, true));
		}else if(connectedPart != null){
			MasterLoader.networkInterface.sendToAllClients(new PacketTileEntityFluidLoaderConnection(this, false));
			connectedPart = null;
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
		data.setBoolean("unloadMode", unloadMode);
	}
}
