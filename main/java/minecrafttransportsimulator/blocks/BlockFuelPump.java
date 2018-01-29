package minecrafttransportsimulator.blocks;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSBlockRotateable;
import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import minecrafttransportsimulator.dataclasses.MTSAchievements;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.packets.general.ChatPacket;
import minecrafttransportsimulator.packets.general.FuelPumpConnectDisconnectPacket;
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
			TileEntityFuelPump pump = (TileEntityFuelPump) world.getTileEntity(pos);
			if(stack != null){
	    		ICapabilityProvider capabilities = stack.getItem().initCapabilities(stack, stack.getTagCompound());
	        	if(capabilities instanceof FluidBucketWrapper){
	        		handlerBucket = ((FluidBucketWrapper) capabilities);
	        		stackToAdd = handlerBucket.getFluid();
	        	}else if(stack.getItem() instanceof ItemFluidContainer){
	        		handlerStack = (FluidHandlerItemStack) capabilities;
	    			stackToAdd = handlerStack.getFluid();
	        	}
	        	
	        	if(stackToAdd != null){
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
		    		return true;
	        	}
			}
        	
    		if(pump.connectedVehicle == null){
    			Entity nearestEntity = null;
    			float lowestDistance = 99;
    			for(Entity entity : world.loadedEntityList){
    				if(entity instanceof EntityMultipartVehicle){
    					float distance = (float) Math.sqrt(entity.getPosition().distanceSq(pump.getPos()));
    					if(distance < lowestDistance){
    						lowestDistance = distance;
    						nearestEntity = entity;
    					}
    				}
    			}
    			if(nearestEntity != null){
					pump.connectedVehicle = (EntityMultipartVehicle) nearestEntity;
					pump.connectedVehicleUUID = pump.connectedVehicle.UUID;
					pump.totalTransfered = 0;
					MTS.MTSNet.sendToAll(new FuelPumpConnectDisconnectPacket(pump, pump.connectedVehicle.getEntityId()));
					MTS.MTSNet.sendTo(new ChatPacket("interact.fuelpump.connect"), (EntityPlayerMP) player);
					MTSAchievements.triggerFuel(player);
    			}else{
    				MTS.MTSNet.sendTo(new ChatPacket("interact.fuelpump.toofar"), (EntityPlayerMP) player);
    			}
    		}else{
    			pump.connectedVehicleUUID = "";
    			pump.connectedVehicle = null;
    			MTS.MTSNet.sendToAll(new FuelPumpConnectDisconnectPacket(pump, -1));
    			MTS.MTSNet.sendTo(new ChatPacket("interact.fuelpump.disconnect"), (EntityPlayerMP) player);
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
