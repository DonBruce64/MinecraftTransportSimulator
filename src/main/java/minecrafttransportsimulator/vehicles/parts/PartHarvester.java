package minecrafttransportsimulator.vehicles.parts;

import java.util.Iterator;

import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

public final class PartHarvester extends APartGroundEffector{
	
	public PartHarvester(EntityVehicleE_Powered vehicle, VehiclePart packVehicleDef, JSONPart definition, NBTTagCompound dataTag){
		super(vehicle, packVehicleDef, definition, dataTag);
	}
	
	@Override
	public void performEffectsAt(BlockPos pos){
		//Check if we have crops ready to harvest in our space.
		//If so, harvest them and add them to our inventory.
		//If we have bushes, just break them drop their items.
		IBlockState state = vehicle.world.getBlockState(pos);
		if((state.getBlock() instanceof BlockCrops && ((BlockCrops) state.getBlock()).isMaxAge(state)) || state.getBlock() instanceof BlockBush){
			Block harvestedBlock = state.getBlock();
			NonNullList<ItemStack> drops = NonNullList.create();
			vehicle.world.playSound(partPos.x, partPos.y, partPos.z, harvestedBlock.getSoundType(state, vehicle.world, pos, null).getBreakSound(), SoundCategory.BLOCKS, 1.0F, 1.0F, false);
			if(!vehicle.world.isRemote){
				harvestedBlock.getDrops(drops, vehicle.world, pos, state, 0);
				vehicle.world.setBlockToAir(pos);
				if(harvestedBlock instanceof BlockCrops){
					Iterator<ItemStack> iterator = drops.iterator();
					while(iterator.hasNext()){
						ItemStack stack = iterator.next();
						for(APart<? extends EntityVehicleA_Base> part : vehicle.getVehicleParts()){
							if(part instanceof PartCrate){
								InventoryBasic crateInventory = ((PartCrate) part).crateInventory;
								if(crateInventory.addItem(stack).getCount() == 0){
									iterator.remove();
									break;
								}
							}
						}
					}
				}
				
				//Check our drops.  If we couldn't add any of them to any inventory, drop them on the ground instead.
				for(ItemStack stack : drops){
					if(!stack.equals(ItemStack.EMPTY) && stack.getCount() > 0){
						vehicle.world.spawnEntity(new EntityItem(vehicle.world, partPos.x, partPos.y, partPos.z, stack));
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
