package minecrafttransportsimulator.blocks.core;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.packets.general.ChatPacket;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class BlockFuelPump extends ABlockRotateable{

	public BlockFuelPump(){
		super(Material.IRON);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			ItemStack stack = player.getHeldItem(hand);
			TileEntityFuelPump pump = (TileEntityFuelPump) world.getTileEntity(pos);
			if(stack != null){
				if(stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)){
					IFluidHandler handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
					FluidStack drainedStack = handler.drain(Integer.MAX_VALUE, false);
					if(drainedStack != null){
						int amountToDrain = pump.fill(drainedStack, false);
						drainedStack = handler.drain(amountToDrain, !player.capabilities.isCreativeMode);
						if(drainedStack != null){
							pump.fill(drainedStack, true);
						}
					}
					return true;
				}
			}
        	
    		if(pump.getConnectedVehicle() == null){
    			Entity nearestEntity = null;
    			float lowestDistance = 99;
    			for(Entity entity : world.loadedEntityList){
    				if(entity instanceof EntityMultipartE_Vehicle){
    					float distance = (float) Math.sqrt(entity.getPosition().distanceSq(pump.getPos()));
    					if(distance < lowestDistance){
    						lowestDistance = distance;
    						nearestEntity = entity;
    					}
    				}
    			}
    			if(nearestEntity != null){
    				pump.setConnectedVehicle((EntityMultipartE_Vehicle) nearestEntity);
					MTS.MTSNet.sendTo(new ChatPacket("interact.fuelpump.connect"), (EntityPlayerMP) player);
    			}else{
    				MTS.MTSNet.sendTo(new ChatPacket("interact.fuelpump.toofar"), (EntityPlayerMP) player);
    			}
    		}else{
    			pump.setConnectedVehicle(null);
    			MTS.MTSNet.sendTo(new ChatPacket("interact.fuelpump.disconnect"), (EntityPlayerMP) player);
    		}
		}
		return true;
	}
	
	@Override
	public ATileEntityRotatable createNewTileEntity(World worldIn, int meta){
		return new TileEntityFuelPump();
	}
	
	@Override
	protected boolean canRotateDiagonal(){
		return false;
	}
}
