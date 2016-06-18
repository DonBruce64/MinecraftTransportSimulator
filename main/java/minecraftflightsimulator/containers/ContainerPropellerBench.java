package minecraftflightsimulator.containers;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;

public class ContainerPropellerBench extends Container implements IInventory{
	private ItemStack[] items = new ItemStack[10];
	
	public ContainerPropellerBench(InventoryPlayer invPlayer){
		for (int i=0; i<9; ++i){
            this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 161 + 18*2 + 1));
        }
		for(int i=0; i<3; ++i){
            for(int j=0; j<9; ++j){
                this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 103 + i * 18 + 18*2 + 1));
            }
        }
        for (int i=0; i<9; ++i){
        	if(i!=4){
        		//this.addSlotToContainer(new SlotItem(this, 18+i*26 - (i/3)*78, 31 + (i/3)*26, i, Item.getItemFromBlock(Blocks.planks), Items.iron_ingot, Item.getItemFromBlock(Blocks.obsidian)));
        	}else{
        		//this.addSlotToContainer(new SlotItem(this, 18+i*26 - (i/3)*78, 31 + (i/3)*26, i, Items.iron_ingot));
        	}
        }
        //this.addSlotToContainer(new SlotItem(this, 134, 105, 9));
	}
	
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
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
		return null;		
	}
	
	public void markDirty(){}
	public void clear(){}
	public void setField(int id, int value){}
	public void openInventory(){}
	public void openInventory(EntityPlayer player){}
	public void closeInventory(){}
    public void closeInventory(EntityPlayer player){}
    
	public boolean hasCustomInventoryName(){return false;}
	public boolean isUseableByPlayer(EntityPlayer player){return true;}
	public boolean isItemValidForSlot(int slot, ItemStack stack){return false;}
	public int getField(int id){return 0;}
	public int getFieldCount(){return 0;}
	public int getSizeInventory(){return items.length;}
	public int getInventoryStackLimit(){return 64;}
	public String getInventoryName(){return StatCollector.translateToLocal("tile.mfs.Chest.name");}
	public ItemStack getStackInSlot(int slot){return items[slot];}
	public ItemStack getStackInSlotOnClosing(int slot){return null;}
	
	@Override
	public void setInventorySlotContents(int slot, ItemStack stack){
		items[slot] = stack;
	}
	
	@Override
    public ItemStack decrStackSize(int slot, int number){
        if(items[slot] != null){
            ItemStack itemstack;
            if(items[slot].stackSize <= number){
                itemstack = items[slot];
                items[slot] = null;
                return itemstack;
            }else{
                itemstack = items[slot].splitStack(number);
                if(items[slot].stackSize == 0){
                	items[slot] = null;
                }
                return itemstack;
            }
        }else{
            return null;
        }
    }
	
    public ItemStack removeStackFromSlot(int index){
		ItemStack removedStack = getStackInSlot(index);
		setInventorySlotContents(index, null);
		return removedStack;
	}
    
    public boolean hasCustomName(){return false;}
	public String getName(){return null;}
	public IChatComponent getDisplayName(){return null;}
}
