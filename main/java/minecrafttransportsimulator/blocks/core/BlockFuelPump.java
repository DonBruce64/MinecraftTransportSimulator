package minecrafttransportsimulator.blocks.core;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.blocks.ItemBlockRotatable;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class BlockFuelPump extends BlockRotatable implements ITileEntityProvider{

	public BlockFuelPump(EnumFacing orientation, ItemBlockRotatable item){
		super(orientation, item);
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			ItemStack stack = player.getHeldItem(hand);
			TileEntityFuelPump pump = (TileEntityFuelPump) world.getTileEntity(pos);
			if(stack != null){
				if(stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)){
					IFluidHandlerItem handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
					FluidStack drainedStack = handler.drain(Integer.MAX_VALUE, false);
					if(drainedStack != null){
						int amountToDrain = pump.fill(drainedStack, false);
						drainedStack = handler.drain(amountToDrain, !player.capabilities.isCreativeMode);
						if(drainedStack != null){
							pump.fill(drainedStack, true);
							player.setHeldItem(hand, handler.getContainer());
						}
					}
					return true;
				}else if(stack.getItem().equals(MTSRegistry.jerrycan)){
					if(!stack.hasTagCompound() || !stack.getTagCompound().getBoolean("isFull")){
						if(pump.getFluid() != null && pump.getFluidAmount() >= 1000){
							NBTTagCompound stackTag = new NBTTagCompound();
							stackTag.setBoolean("isFull", true);
							stackTag.setString("fluidName", FluidRegistry.getFluidName(pump.getFluid().getFluid()));
							stack.setTagCompound(stackTag);
							pump.drain(1000, true);
						}
					}
					return true;
				}
			}
        	
    		if(pump.getConnectedVehicle() == null){
    			Entity nearestEntity = null;
    			float lowestDistance = 16;
    			for(Entity entity : world.loadedEntityList){
    				if(entity instanceof EntityVehicleE_Powered){
    					float distance = (float) Math.sqrt(entity.getPosition().distanceSq(pump.getPos()));
    					if(distance < lowestDistance){
    						lowestDistance = distance;
    						nearestEntity = entity;
    					}
    				}
    			}
    			if(nearestEntity != null){
    				EntityVehicleE_Powered nearestVehicle = (EntityVehicleE_Powered) nearestEntity;
    				if(pump.getFluid() == null){
    					MTS.MTSNet.sendTo(new PacketChat("interact.fuelpump.nofuel"), (EntityPlayerMP) player);
    				}else{
        				String fluidName = FluidRegistry.getFluidName(pump.getFluid().getFluid());
    					if(!nearestVehicle.fluidName.isEmpty()){
    						if(!fluidName.equals(nearestVehicle.fluidName)){
    							MTS.MTSNet.sendTo(new PacketChat("interact.fuelpump.wrongtype"), (EntityPlayerMP) player);
    							return true;
    						}
    					}
    					
    					boolean isFluidValidFuelForEngines = false;
    					for(APart part : nearestVehicle.getVehicleParts()){
    						if(part instanceof APartEngine){
    							if(ConfigSystem.getFuelValue(part.pack.engine.fuelType, fluidName) > 0){
    								isFluidValidFuelForEngines = true;
    								pump.setConnectedVehicle((EntityVehicleE_Powered) nearestEntity);
    	    						MTS.MTSNet.sendTo(new PacketChat("interact.fuelpump.connect"), (EntityPlayerMP) player);
    	    						return true;
    							}
    						}
    					}
    					MTS.MTSNet.sendTo(new PacketChat("interact.fuelpump.wrongengines"), (EntityPlayerMP) player);
    				}
    			}else{
    				MTS.MTSNet.sendTo(new PacketChat("interact.fuelpump.toofar"), (EntityPlayerMP) player);
    			}
    		}else{
    			pump.setConnectedVehicle(null);
    			MTS.MTSNet.sendTo(new PacketChat("interact.fuelpump.disconnect"), (EntityPlayerMP) player);
    		}
		}
		return true;
	}
	
	@Override
    public void breakBlock(World world, BlockPos pos, IBlockState state){
        super.breakBlock(world, pos, state);
        world.removeTileEntity(pos);
    }
	
	@Override
	public TileEntityFuelPump createNewTileEntity(World worldIn, int meta){
		return new TileEntityFuelPump();
	}
}
