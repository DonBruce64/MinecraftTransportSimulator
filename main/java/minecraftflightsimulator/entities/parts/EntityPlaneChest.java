package minecraftflightsimulator.entities.parts;

import java.util.Iterator;
import java.util.List;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityParent;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public class EntityPlaneChest extends EntityChild implements IInventory{
    public float lidAngle;
    public float prevLidAngle;
    public int numPlayersUsing;
    private int ticksSinceSync;
	private ItemStack[] chestContents = new ItemStack[36];
	
	public EntityPlaneChest(World world){
		super(world);
	}
	
	public EntityPlaneChest(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, 0);
	}
	
	@Override
    public boolean interactFirst(EntityPlayer player){
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
	
	@Override
	public void onEntityUpdate(){
        super.onEntityUpdate();
        ++this.ticksSinceSync;
        float f;
        if(!this.worldObj.isRemote && this.numPlayersUsing != 0 && (this.ticksSinceSync + this.posX + this.posY + this.posZ) % 200 == 0){
            this.numPlayersUsing = 0;
            f = 5.0F;
            List list = this.worldObj.getEntitiesWithinAABB(EntityPlayer.class, AxisAlignedBB.getBoundingBox((double)((float)this.posX - f), (double)((float)this.posY - f), (double)((float)this.posZ - f), (double)((float)(this.posX + 1) + f), (double)((float)(this.posY + 1) + f), (double)((float)(this.posZ + 1) + f)));
            Iterator iterator = list.iterator();
            while (iterator.hasNext()){
                EntityPlayer entityplayer = (EntityPlayer)iterator.next();
                if(entityplayer.openContainer instanceof ContainerChest){
                    IInventory iinventory = ((ContainerChest)entityplayer.openContainer).getLowerChestInventory();
                    if(iinventory == this || iinventory instanceof InventoryLargeChest && ((InventoryLargeChest)iinventory).isPartOfLargeChest(this)){
                        ++this.numPlayersUsing;
                    }
                }
            }
        }
        this.prevLidAngle = this.lidAngle;
        f = 0.1F;
        if(this.numPlayersUsing > 0 && this.lidAngle == 0.0F){
            this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.chestopen", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
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
                this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.chestclosed", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
            }

            if(this.lidAngle < 0.0F){
                this.lidAngle = 0.0F;
            }
        }
    }
	
	@Override
	public int getSizeInventory(){
		return 27;
	}
	
	@Override
	public ItemStack getStackInSlot(int slot){
		return this.chestContents[slot];
	}
	
	@Override
    public ItemStack decrStackSize(int slot, int number){
        if(this.chestContents[slot] != null){
            ItemStack itemstack;
            if(this.chestContents[slot].stackSize <= number){
                itemstack = this.chestContents[slot];
                this.chestContents[slot] = null;
                return itemstack;
            }else{
                itemstack = this.chestContents[slot].splitStack(number);
                if(this.chestContents[slot].stackSize == 0){
                    this.chestContents[slot] = null;
                }
                return itemstack;
            }
        }else{
            return null;
        }
    }
	
	@Override
    public ItemStack getStackInSlotOnClosing(int slot){
        if(this.chestContents[slot] != null){
            ItemStack itemstack = this.chestContents[slot];
            this.chestContents[slot] = null;
            return itemstack;
        }else{
            return null;
        }
    }
	
	@Override
    public void setInventorySlotContents(int slot, ItemStack item){
        this.chestContents[slot] = item;
        if(item != null && item.stackSize > this.getInventoryStackLimit()){
            item.stackSize = this.getInventoryStackLimit();
        }
    }
	
	@Override
	public String getInventoryName(){
		return StatCollector.translateToLocal("entity.mfs.Chest.name");
	}
	
	@Override
	public boolean hasCustomInventoryName(){
		return false;
	}
	
	@Override
	public int getInventoryStackLimit(){
		return 64;
	}
	
	@Override
	public void markDirty(){}
	
	@Override
	public boolean isUseableByPlayer(EntityPlayer player){
		return player.getDistanceToEntity(this) < 5;
	}
	
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack){
		return true;
	}

	@Override
    public void openInventory(){
        if(this.numPlayersUsing < 0){
            this.numPlayersUsing = 0;
        }
        ++this.numPlayersUsing;
    }

	@Override
    public void closeInventory(){
    	--this.numPlayersUsing;
    }
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        NBTTagList nbttaglist = tagCompound.getTagList("Items", 10);
        this.chestContents = new ItemStack[this.getSizeInventory()];
        for (int i = 0; i < nbttaglist.tagCount(); ++i){
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
            int j = nbttagcompound1.getByte("Slot") & 255;
            if(j >= 0 && j < this.chestContents.length){
                this.chestContents[j] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
            }
        }
    }
    
	@Override
    public void writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        NBTTagList nbttaglist = new NBTTagList();
        for(int i = 0; i < this.chestContents.length; ++i){
            if (this.chestContents[i] != null){
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte)i);
                this.chestContents[i].writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }
        tagCompound.setTag("Items", nbttaglist);
    }
}
