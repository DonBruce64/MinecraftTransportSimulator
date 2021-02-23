package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.AEntityA_Base;
import minecrafttransportsimulator.baseclasses.AEntityE_Multipart;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.baseclasses.IFluidTankProvider;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFluidLoaderConnection;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartInteractable;

public class TileEntityFluidLoader extends TileEntityDecor implements ITileEntityTickable, IFluidTankProvider{
	public PartInteractable connectedPart;
	public boolean unloadMode;
	public boolean loading;
	public boolean unloading;
    private FluidTank tank;

    public TileEntityFluidLoader(WrapperWorld world, Point3d position, WrapperNBT data){
    	super(world, position, data);
    	this.tank = new FluidTank(world, data, 1000);
    	this.unloadMode = data.getBoolean("unloadMode");
    }
	
	@Override
	public void update(){
		//Do load/unload checks.  Checks only occur on servers.  Clients get packets for state changes.
		if(!world.isClient()){
			if(connectedPart == null){
				//Check for a new part every second.  We don't want every tick as this would increase server loads.
				if(world.getTick()%20 == 0){
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
				//Check load/unload state.
				if(world.getTick()%20 == 0){
					unloadMode = world.getRedstonePower(position) > 0;
				}
				
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
			for(AEntityA_Base entity : AEntityA_Base.getEntities(world)){
				if(entity instanceof AEntityE_Multipart){
					AEntityE_Multipart<?> multipart = (AEntityE_Multipart<?>) entity;
					if(multipart.position.distanceTo(position) < 100){
						for(APart part : multipart.parts){
							if(part.position.distanceTo(position) < 10){
								if(part instanceof PartInteractable){
									FluidTank partTank = ((PartInteractable) part).tank;
									if(partTank != null){
										if(unloadMode){
											if(partTank.drain(tank.getFluid(), 1, false) > 0){
												if(part.position.distanceTo(position) < nearestDistance){
													nearestPart = (PartInteractable) part;
												}
											}
										}else{
											if(partTank.fill(tank.getFluid(), 1, false) > 0){
												if(part.position.distanceTo(position) < nearestDistance){
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
			InterfacePacket.sendToAllClients(new PacketTileEntityFluidLoaderConnection(this, true));
		}else if(connectedPart != null){
			InterfacePacket.sendToAllClients(new PacketTileEntityFluidLoaderConnection(this, false));
			connectedPart = null;
		}
	}
	
	@Override
	public FluidTank getTank(){
		return tank;
	}
	
	@Override
	public boolean canConnectOnAxis(Axis axis){
		return !axis.equals(Axis.UP);
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		tank.save(data);
		data.setBoolean("unloadMode", unloadMode);
	}
}
