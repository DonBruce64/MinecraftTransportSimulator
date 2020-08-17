package minecrafttransportsimulator.blocks.instances;

import mcinterface.InterfaceNetwork;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.FluidTank;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPumpConnection;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

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
			
			//If we are holding an item, try to add it to the fuel pump.
			ItemStack stack = player.getHeldStack();
			if(stack != null){
				if(stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)){
					//Item can provide fuel.  Check if we can accept it.
					IFluidHandlerItem handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
					FluidStack drainedStack = handler.drain(Integer.MAX_VALUE, false);
					if(drainedStack != null){
						//Able to take fuel from item, attempt to do so.
						int amountToDrain = tank.fill(drainedStack.getFluid().getName(), drainedStack.amount, false);
						drainedStack = handler.drain(amountToDrain, !player.isCreative());
						if(drainedStack != null){
							//Was able to provide fuel from item.  Fill the pump.
							tank.fill(drainedStack.getFluid().getName(), drainedStack.amount, true);
							player.setHeldStack(handler.getContainer());
						}
					}
					return true;
				}else if(stack.getItem().equals(MTSRegistry.jerrycan)){
					//Have a jerrycan.  Attempt to fill it up.
					if(!stack.hasTagCompound() || !stack.getTagCompound().getBoolean("isFull")){
						if(tank.getFluidLevel() >= 1000){
							NBTTagCompound stackTag = new NBTTagCompound();
							stackTag.setBoolean("isFull", true);
							stackTag.setString("fluidName", tank.getFluid());
							stack.setTagCompound(stackTag);
							tank.drain(tank.getFluid(), 1000, true);
						}
					}
					return true;
				}
			}
        	
			//We don't have a vehicle connected.  Try to connect one now.
    		if(pump.connectedVehicle == null){
    			//Get the closest vehicle within a 16-block radius.
    			EntityVehicleF_Physics nearestVehicle = null;
    			double lowestDistance = 16D;
    			for(AEntityBase entity : AEntityBase.createdServerEntities.values()){
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
    					if(!nearestVehicle.fluidName.isEmpty()){
    						if(!tank.getFluid().equals(nearestVehicle.fluidName)){
    							player.sendPacket(new PacketPlayerChatMessage("interact.fuelpump.wrongtype"));
    							return true;
    						}
    					}
    					
    					//Fuel type can be taken by vehicle, check to make sure engines can take it.
    					for(APart part : nearestVehicle.parts){
    						if(part instanceof PartEngine){
    							if(ConfigSystem.configObject.fuel.fuels.get(part.definition.engine.fuelType).containsKey(tank.getFluid())){
    								pump.connectedVehicle = nearestVehicle;
    								tank.resetAmountDispensed();;
    								InterfaceNetwork.sendToAllClients(new PacketTileEntityPumpConnection(pump));
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
    			pump.connectedVehicle = null;
    			InterfaceNetwork.sendToAllClients(new PacketTileEntityPumpConnection(pump));
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
