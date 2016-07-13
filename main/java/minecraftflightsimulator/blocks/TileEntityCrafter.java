package minecraftflightsimulator.blocks;

import minecraftflightsimulator.MFS;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

public class TileEntityCrafter extends TileEntity implements IInventory{
	public boolean isOn;
	public short propertyCode;
	public int timeLeft;
	private ItemStack[] contents;
	
	public TileEntityCrafter(){}
	
	public TileEntityCrafter(int numberSlots, short defaultPropertyCode){
		this.contents = new ItemStack[numberSlots];
		this.propertyCode = defaultPropertyCode;
	}
	
	@Override
	public void updateEntity(){
		if(isOn){
			if(timeLeft > 0){
				if(timeLeft%50 == 0){
					if(getStackInSlot(2) != null){
						decrStackSize(2, 1);
					}else{
						isOn = false;
						return;
					}
				}
			}else{
				isOn = false;
				//TODO make this work for any item selected in the GUI, as helicopter blades will be added.
				this.decrStackSize(0, 1);
				this.decrStackSize(1, 70+5*(propertyCode/1000) < 90 ? (propertyCode%10) : (propertyCode%10)*2);
				this.setInventorySlotContents(3, new ItemStack(MFS.proxy.propeller, 1, propertyCode));
				return;
			}
			--timeLeft;
		}
	}
	
	public void markDirty(){}
	public void clear(){}
	public void setField(int id, int value){}
	public void openInventory(){this.openInventory(null);}
	public void openInventory(EntityPlayer player){}
	public void closeInventory(){this.closeInventory(null);}
    public void closeInventory(EntityPlayer player){}
    
	public boolean hasCustomInventoryName(){return false;}
	public boolean isUseableByPlayer(EntityPlayer player){return this.getDistanceFrom(player.posX, player.posY, player.posZ) <= 64;}
	public boolean isItemValidForSlot(int slot, ItemStack stack){return true;}
	public int getField(int id){return 0;}
	public int getFieldCount(){return 0;}
	public int getSizeInventory(){return contents.length;}
	public int getInventoryStackLimit(){return 64;}
	public String getInventoryName(){return "";}
	public ItemStack getStackInSlot(int slot){return this.contents[slot];}
	public ItemStack getStackInSlotOnClosing(int slot){return null;}
	
    public void setInventorySlotContents(int slot, ItemStack item){
        this.contents[slot] = item;
        if(item != null && item.stackSize > this.getInventoryStackLimit()){
            item.stackSize = this.getInventoryStackLimit();
        }
    }
	
    public ItemStack decrStackSize(int slot, int number){
        if(this.contents[slot] != null){
            ItemStack itemstack;
            if(this.contents[slot].stackSize <= number){
                itemstack = this.contents[slot];
                this.contents[slot] = null;
                return itemstack;
            }else{
                itemstack = this.contents[slot].splitStack(number);
                if(this.contents[slot].stackSize == 0){
                    this.contents[slot] = null;
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
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        this.isOn = tagCompound.getBoolean("isOn");
        this.propertyCode = tagCompound.getShort("propertyCode");
        this.timeLeft = tagCompound.getInteger("timeLeft");
        NBTTagList nbttaglist = tagCompound.getTagList("Items", 10);
        this.contents = new ItemStack[tagCompound.getByte("inventorySize")];
        for (int i = 0; i < nbttaglist.tagCount(); ++i){
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
            int j = nbttagcompound1.getByte("Slot") & 255;
            if(j >= 0 && j < this.contents.length){
                this.contents[j] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
            }
        }
    }
    
	@Override
    public void writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("isOn", this.isOn);
        tagCompound.setShort("propertyCode", this.propertyCode);
        tagCompound.setInteger("timeLeft", this.timeLeft);
        tagCompound.setByte("inventorySize", (byte) this.getSizeInventory());
        NBTTagList nbttaglist = new NBTTagList();
        for(int i = 0; i < this.contents.length; ++i){
            if (this.contents[i] != null){
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte)i);
                this.contents[i].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }
        tagCompound.setTag("Items", nbttaglist);
    }
}
