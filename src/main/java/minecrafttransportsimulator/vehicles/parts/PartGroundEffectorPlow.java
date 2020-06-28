package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

public final class PartGroundEffectorPlow extends APartGroundEffector{
	
	public PartGroundEffectorPlow(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
	}
	
	@Override
	public void performEffectsAt(BlockPos pos){
		//Check if we have a plow-able block below us, and plow it if so.
		IBlockState state = vehicle.world.getBlockState(pos);
		Block block = state.getBlock();
		if(block.equals(Blocks.GRASS) || block.equals(Blocks.GRASS_PATH)){
			setBlockFromPlow(Blocks.FARMLAND.getDefaultState(), pos);
		 }else if(block.equals(Blocks.DIRT)){
			 switch(state.getValue(BlockDirt.VARIANT)){
			 	case DIRT: setBlockFromPlow(Blocks.FARMLAND.getDefaultState(), pos); return;
			 	case COARSE_DIRT: setBlockFromPlow(Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.DIRT), pos); return;
			 	default: return;
             }
		}
	}
	
	private void setBlockFromPlow(IBlockState state, BlockPos pos){
		vehicle.world.playSound(worldPos.x, worldPos.y, worldPos.z, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
		if(!vehicle.world.isRemote){
			vehicle.world.setBlockState(pos, state);
		 }
	}
	
	@Override
	protected boolean effectIsBelowPart(){
		return true;
	}
}
