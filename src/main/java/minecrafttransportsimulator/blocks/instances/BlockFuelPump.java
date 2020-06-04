package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPumpConnection;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import minecrafttransportsimulator.wrappers.WrapperNetwork;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class BlockFuelPump extends ABlockBase implements IBlockTileEntity<JSONDecor>{
	
	public BlockFuelPump(){
		super(10.0F, 5.0F);
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3i point, Axis axis, WrapperPlayer player){
		//Only check right-clicks on the server.
		if(!world.isClient()){
			TileEntityFuelPump pump = (TileEntityFuelPump) world.getTileEntity(point);
			
			//If we are holding an item, try to add it to the fuel pump.
			ItemStack stack = player.getHeldStack();
			if(stack != null){
				if(stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)){
					//Item can provide fuel.  Check if we can accept it.
					IFluidHandlerItem handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
					FluidStack drainedStack = handler.drain(Integer.MAX_VALUE, false);
					if(drainedStack != null){
						//Able to take fuel from item, attempt to do so.
						int amountToDrain = pump.fill(drainedStack.getFluid().getName(), drainedStack.amount, false);
						drainedStack = handler.drain(amountToDrain, !player.isCreative());
						if(drainedStack != null){
							//Was able to provide fuel from item.  Fill the pump.
							pump.fill(drainedStack.getFluid().getName(), drainedStack.amount, true);
							player.setHeldStack(handler.getContainer());
						}
					}
					return true;
				}else if(stack.getItem().equals(MTSRegistry.jerrycan)){
					//Have a jerrycan.  Attempt to fill it up.
					if(!stack.hasTagCompound() || !stack.getTagCompound().getBoolean("isFull")){
						if(pump.getFluidLevel() >= 1000){
							NBTTagCompound stackTag = new NBTTagCompound();
							stackTag.setBoolean("isFull", true);
							stackTag.setString("fluidName", pump.getFluid());
							stack.setTagCompound(stackTag);
							pump.drain(pump.getFluid(), 1000, true);
						}
					}
					return true;
				}
			}
        	
			//We don't have a vehicle connected.  Try to connect one now.
    		if(pump.connectedVehicle == null){
    			//Get the closest vehicle within a 16-block radius.
    			EntityVehicleE_Powered nearestVehicle = null;
    			float lowestDistance = 16;
    			for(EntityVehicleE_Powered vehicle : world.getVehiclesWithin(new BoundingBox(point.x, point.y, point.z, lowestDistance, lowestDistance, lowestDistance))){
    				float distance = (float) vehicle.getDistance(point.x, point.y, point.z);
					if(distance < lowestDistance){
						lowestDistance = distance;
						nearestVehicle = vehicle;
					}
    			}
    			
    			//Have a vehicle, try to connect to it.
    			if(nearestVehicle != null){
    				if(pump.getFluidLevel() == 0){
    					//No fuel in the pump.
    					player.sendPacket(new PacketPlayerChatMessage("interact.fuelpump.nofuel"));
    				}else{
        				//Check to make sure this vehicle can take this fuel pump's fuel type.
    					if(!nearestVehicle.fluidName.isEmpty()){
    						if(!pump.getFluid().equals(nearestVehicle.fluidName)){
    							player.sendPacket(new PacketPlayerChatMessage("interact.fuelpump.wrongtype"));
    							return true;
    						}
    					}
    					
    					//Fuel type can be taken by vehicle, check to make sure engines can take it.
    					for(APart part : nearestVehicle.getVehicleParts()){
    						if(part instanceof PartEngine){
    							if(ConfigSystem.configObject.fuel.fuels.get(part.definition.engine.fuelType).containsKey(pump.getFluid())){
    								pump.connectedVehicle = nearestVehicle;
    								pump.totalTransfered = 0;
    								WrapperNetwork.sendToAllClients(new PacketTileEntityPumpConnection(pump));
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
    			WrapperNetwork.sendToAllClients(new PacketTileEntityPumpConnection(pump));
    			player.sendPacket(new PacketPlayerChatMessage("interact.fuelpump.disconnect"));
    		}
		}
		return true;
	}
	
    @Override
	public TileEntityFuelPump createTileEntity(){
		return new TileEntityFuelPump();
	}
}
