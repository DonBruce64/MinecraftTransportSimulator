package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public final class PartGroundEffectorFertilizer extends APartGroundEffector{
	
	public PartGroundEffectorFertilizer(EntityVehicleF_Physics vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
	}
	
	@Override
	public void performEffectsAt(BlockPos pos){
		//Check if we are in crops.
		IBlockState cropState = vehicle.world.getBlockState(pos);
		Block cropBlock = cropState.getBlock();
		if(cropBlock instanceof IGrowable){
            IGrowable growable = (IGrowable)cropState.getBlock();
            if(growable.canGrow(vehicle.world, pos, cropState, vehicle.world.isRemote)){
            	if(!vehicle.world.isRemote){
            		//Check for bonemeal in crates.
            		for(APart part : vehicle.getVehicleParts()){
    					if(part instanceof PartCrate){
    						InventoryBasic crateInventory = ((PartCrate) part).crateInventory;
    						for(byte i=0; i<crateInventory.getSizeInventory(); ++i){
    							ItemStack stack = crateInventory.getStackInSlot(i);
    							if(stack.getItem().equals(Items.DYE)){
    								ItemDye.applyBonemeal(stack, vehicle.world, pos);
    								crateInventory.markDirty();
    								return;
    							}
    						}
    					}
            		}
                }
            }
		}
	}
	
	@Override
	protected boolean effectIsBelowPart(){
		return false;
	}
}
