package minecrafttransportsimulator.blocks;

import javax.annotation.Nullable;

import minecrafttransportsimulator.baseclasses.MTSBlockRotateable;
import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.ItemFluidContainer;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;

public class BlockFuelPump extends MTSBlockRotateable{

	public BlockFuelPump(){
		super(Material.IRON);
		this.setHardness(5.0F);
		this.setResistance(10.0F);
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			FluidBucketWrapper handlerBucket = null;
			FluidHandlerItemStack handlerStack = null;
			FluidStack stackToAdd = null;
			
			ItemStack stack = player.getHeldItem(hand);
    		ICapabilityProvider capabilities = stack.getItem().initCapabilities(stack, stack.getTagCompound());
        	if(capabilities instanceof FluidBucketWrapper){
        		handlerBucket = ((FluidBucketWrapper) capabilities);
        		stackToAdd = handlerBucket.getFluid();
        	}else if(stack.getItem() instanceof ItemFluidContainer){
        		handlerStack = (FluidHandlerItemStack) capabilities;
    			stackToAdd = handlerStack.getFluid();
        	}
        	
        	TileEntityFuelPump pump = (TileEntityFuelPump) world.getTileEntity(pos); 
        	int amountToFill = pump.fill(stackToAdd, false);
    		if(amountToFill > 0){
            	if(handlerBucket != null){
            		if(amountToFill <= stackToAdd.amount){
            			pump.fill(stackToAdd, true);
            			if(!player.isCreative()){
            				handlerBucket.drain(stackToAdd, true);
            			}
            		}
            	}else{
            		pump.fill(stackToAdd, true);
            		if(!player.isCreative()){
            			handlerStack.drain(new FluidStack(stackToAdd.getFluid(), amountToFill), true);
        			}
            	}
    		}
		}
		return true;
	}
	
	@Override
	public MTSTileEntity getTileEntity(){
		return new TileEntityFuelPump();
	}
	
	@Override
	protected boolean canRotateDiagonal(){
		return false;
	}
}
