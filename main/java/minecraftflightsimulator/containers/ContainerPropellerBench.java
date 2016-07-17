package minecraftflightsimulator.containers;

import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ContainerPropellerBench extends Container{
	private final TileEntityPropellerBench tile;
	
	public ContainerPropellerBench(InventoryPlayer invPlayer, TileEntityPropellerBench tile){
		this.tile = tile;
		this.addSlotToContainer(new SlotItem(tile, 12, 54, 0, Items.iron_ingot));
		this.addSlotToContainer(new SlotItem(tile, 63, 54, 1, Item.getItemFromBlock(Blocks.planks), Items.iron_ingot, Item.getItemFromBlock(Blocks.obsidian)));
		this.addSlotToContainer(new SlotItem(tile, 12, 107, 2, Items.redstone));
		this.addSlotToContainer(new SlotItem(tile, 127, 54, 3));
		SlotItem dummy = new SlotItem(tile, 63, 36, 4);
		dummy.enabled = false;
		this.addSlotToContainer(dummy);

		for (int j=0; j<9; ++j){
            this.addSlotToContainer(new Slot(invPlayer, j, 8 + j * 18, 161 + 18*2 + 1));
        } 
		for(int j=0; j<3; ++j){
            for(int k=0; k<9; ++k){
                this.addSlotToContainer(new Slot(invPlayer, k + j * 9 + 9, 8 + k * 18, 103 + j * 18 + 18*2 + 1));
            }
        }
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return tile.isUseableByPlayer(player);
	}
	
	@Override
	public void onContainerClosed(EntityPlayer player){
		super.onContainerClosed(player);
		if(!player.worldObj.isRemote){
			for(Object slot :  this.inventorySlots){
				if(((Slot) slot).inventory.equals(this)){
					if(((Slot) slot).getStack() != null){
						player.worldObj.spawnEntityInWorld(new EntityItem(player.worldObj, player.posX, player.posY, player.posZ, ((Slot) slot).getStack()));
					}
				}
			}
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int sourceSlotIndex){
		ItemStack stack = getSlot(sourceSlotIndex).getStack();
		if(stack != null){
			for(int i=0; i<inventorySlots.size(); ++i){
				Slot slot = (Slot) inventorySlots.get(i);
				if(slot.inventory.equals(tile)){
					if(slot.isItemValid(stack) && stack.stackSize != 0){
						if(slot.getHasStack()){
							if(slot.getStack().equals(stack)){
								return null;
							}
							if(slot.getStack().stackSize < slot.getSlotStackLimit()){
								slot.getStack().stackSize += stack.splitStack(Math.min(slot.getSlotStackLimit() - slot.getStack().stackSize, stack.stackSize)).stackSize;
							}
						}else{
							ItemStack split = stack.splitStack(Math.min(slot.getSlotStackLimit(), stack.stackSize));
							slot.putStack(split);
						}
					}
				}
			}
			if(stack.stackSize == 0){
				((Slot) inventorySlots.get(sourceSlotIndex)).putStack(null);
			}
		}
		return null;		
	}
}
