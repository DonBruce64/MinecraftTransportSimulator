package minecrafttransportsimulator.entities.main;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

public abstract class EntityChildInventory extends EntityMultipartChild implements IInventory{
	private ItemStack[] contents = new ItemStack[27];
	
	public EntityChildInventory(World world){
		super(world);
	}
	
	public EntityChildInventory(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, 0);
	}
	
	protected abstract String getChildInventoryName();
	
	@Override
	public boolean interactPart(EntityPlayer player){
		if(!worldObj.isRemote){
			player.openGui(MTS.instance, this.getEntityId(), worldObj, (int) posX, (int) posY, (int) posZ);
		}
		return true;
    }
    
	@Override
	public void setDead(){
		super.setDead();
		if(!worldObj.isRemote){
			for(int i=0; i<this.getSizeInventory(); ++i){
				ItemStack item = getStackInSlot(i);
				if(item != null){
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, item));
				}
			}
		}
	}
	
	public void markDirty(){}
	public void clear(){}
	public void setField(int id, int value){}
	public void openInventory(EntityPlayer player){}
    public void closeInventory(EntityPlayer player){}
    
	public boolean isUseableByPlayer(EntityPlayer player){return player.getDistanceToEntity(this) < 5;}
	public boolean isItemValidForSlot(int slot, ItemStack stack){return true;}
	public boolean isEmpty(){return false;};
	public int getField(int id){return 0;}
	public int getFieldCount(){return 0;}
	public int getSizeInventory(){return 27;}
	public int getInventoryStackLimit(){return 64;}
	public ItemStack getStackInSlot(int slot){return this.contents[slot];}
	
    public void setInventorySlotContents(int slot, ItemStack stack){
        this.contents[slot] = stack;
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
        NBTTagList nbttaglist = tagCompound.getTagList("Items", 10);
        this.contents = new ItemStack[27];
        for (int i = 0; i < nbttaglist.tagCount(); ++i){
            NBTTagCompound itemTag = nbttaglist.getCompoundTagAt(i);
            int slot = itemTag.getByte("Slot") & 255;
            if(slot >= 0 && slot < 27){
                this.contents[slot] = ItemStack.loadItemStackFromNBT(itemTag);
            }
        }
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        NBTTagList nbttaglist = new NBTTagList();
        for(int slot = 0; slot < 27; ++slot){
            if (this.contents[slot] != null){
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte)slot);
                this.contents[slot].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }
        tagCompound.setTag("Items", nbttaglist);
        return tagCompound;
    }
}
