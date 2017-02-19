package minecraftflightsimulator.entities.core;

import java.util.Iterator;
import java.util.List;

import minecraftflightsimulator.MFS;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public abstract class EntityChildInventory extends EntityChild implements IInventory{
    public float lidAngle;
    public float prevLidAngle;
    public int numPlayersUsing;
    private int ticksSinceSync;
	private ItemStack[] contents = new ItemStack[36];
	
	public EntityChildInventory(World world){
		super(world);
	}
	
	public EntityChildInventory(World world, EntityVehicle vehicle, String parentUUID, float offsetX, float offsetY, float offsetZ, float width, float height){
		super(world, vehicle, parentUUID, offsetX, offsetY, offsetZ, width, height, 0);
	}
	
	protected abstract String getChildInventoryName();
	
	@Override
    public boolean performRightClickAction(EntityBase clicked, EntityPlayer player){
		player.openGui(MFS.instance, this.getEntityId(), worldObj, (int) posX, (int) posY, (int) posZ);
		return false;
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
	
	//Copied from chest code
	@Override
	public void onEntityUpdate(){
        super.onEntityUpdate();
        ++this.ticksSinceSync;
        float f;
        if(!this.worldObj.isRemote && this.numPlayersUsing != 0 && (this.ticksSinceSync + this.posX + this.posY + this.posZ) % 200 == 0){
            this.numPlayersUsing = 0;
            List list = this.worldObj.getEntitiesWithinAABB(EntityPlayer.class, this.getBoundingBox().expand(5, 5, 5));
            Iterator iterator = list.iterator();
            while (iterator.hasNext()){
                EntityPlayer entityplayer = (EntityPlayer)iterator.next();
                if(this.equals(entityplayer.openContainer)){
                	++this.numPlayersUsing;
                }
            }
        }
        this.prevLidAngle = this.lidAngle;
        f = 0.1F;
        if(this.numPlayersUsing > 0 && this.lidAngle == 0.0F){
        	MFS.proxy.playSound(this, "random.chestopen", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
        }

        if(this.numPlayersUsing == 0 && this.lidAngle > 0.0F || this.numPlayersUsing > 0 && this.lidAngle < 1.0F){
            float f1 = this.lidAngle;
            if(this.numPlayersUsing > 0){
                this.lidAngle += f;
            }else{
                this.lidAngle -= f;
            }

            if(this.lidAngle > 1.0F){
                this.lidAngle = 1.0F;
            }

            float f2 = 0.5F;

            if(this.lidAngle < f2 && f1 >= f2){
            	MFS.proxy.playSound(this, "random.chestclosed", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
            }

            if(this.lidAngle < 0.0F){
                this.lidAngle = 0.0F;
            }
        }
    }
	
	public void markDirty(){}
	public void clear(){}
	public void setField(int id, int value){}
	public void openInventory(){this.openInventory(null);}
	public void openInventory(EntityPlayer player){this.numPlayersUsing = this.numPlayersUsing < 0 ? 0 : this.numPlayersUsing + 1;}
	public void closeInventory(){this.closeInventory(null);}
    public void closeInventory(EntityPlayer player){--this.numPlayersUsing;}
    
	public boolean hasCustomInventoryName(){return false;}
	public boolean isUseableByPlayer(EntityPlayer player){return player.getDistanceToEntity(this) < 5;}
	public boolean isItemValidForSlot(int slot, ItemStack stack){return true;}
	public int getField(int id){return 0;}
	public int getFieldCount(){return 0;}
	public int getSizeInventory(){return 27;}
	public int getInventoryStackLimit(){return 64;}
	public String getInventoryName(){return StatCollector.translateToLocal(getChildInventoryName());}
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
        NBTTagList nbttaglist = tagCompound.getTagList("Items", 10);
        this.contents = new ItemStack[this.getSizeInventory()];
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
