package minecraftflightsimulator.blocks;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.sounds.BenchSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IChatComponent;

public class TileEntityPropellerBench extends TileEntity implements IInventory{
	public boolean isOn;
	public short propertyCode = 1120;
	public int timeLeft;
	public static final int opTime = 1000;
	private ItemStack[] contents = new ItemStack[5];
	private BenchSound benchSound;
	
	public TileEntityPropellerBench(){}
	
	@Override
	public void updateEntity(){
		if(!isMaterialCorrect() || !isMaterialSufficient()){
			timeLeft = 0;
			isOn = false;
		}
		if(isOn){
			if(timeLeft > 0){
				if(timeLeft%200 == 0){
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
				this.decrStackSize(1, 70+5*(propertyCode/1000) < 90 ? (propertyCode%100/10) : (propertyCode%100/10)*2);
				this.setInventorySlotContents(3, new ItemStack(MFS.proxy.propeller, 1, propertyCode));
				return;
			}
			--timeLeft;
		}
		benchSound = MFS.proxy.updateBenchSound(benchSound, this);
	}
	
	public boolean isMaterialCorrect(){
		switch(propertyCode%10){
			case(0): return getStackInSlot(1) != null ? getStackInSlot(1).getItem().equals(Item.getItemFromBlock(Blocks.planks)) : false;
			case(1): return getStackInSlot(1) != null ? getStackInSlot(1).getItem().equals(Items.iron_ingot) : false;
			case(2): return getStackInSlot(1) != null ? getStackInSlot(1).getItem().equals(Item.getItemFromBlock(Blocks.obsidian)) : false;
			default: return false;
		}
	}
	
	public boolean isMaterialSufficient(){
		return getStackInSlot(0) != null && (getStackInSlot(1) != null ? (getStackInSlot(1).stackSize >= (70+5*(propertyCode/1000) < 90 ? (propertyCode%100/10) : (propertyCode%100/10)*2)) : false);
	}
	
	public void markDirty(){super.markDirty();}
	public void clear(){}
	public void setField(int id, int value){}
	public void openInventory(){this.openInventory(null);}
	public void openInventory(EntityPlayer player){}
	public void closeInventory(){this.closeInventory(null);}
	public void closeInventory(EntityPlayer player){}

	public boolean hasCustomName(){return false;}    
	public boolean hasCustomInventoryName(){return false;}
	public boolean isUseableByPlayer(EntityPlayer player){return this.getDistanceFrom(player.posX, player.posY, player.posZ) <= 64;}
	public boolean isItemValidForSlot(int slot, ItemStack stack){return true;}
	public int getField(int id){return 0;}
	public int getFieldCount(){return 0;}
	public int getSizeInventory(){return contents.length;}
	public int getInventoryStackLimit(){return 64;}
	public String getName(){return "";}
	public String getInventoryName(){return "";}
	public IChatComponent getDisplayName(){return null;}
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
    public Packet getDescriptionPacket() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        writeToNBT(tagCompound);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tagCompound);
    }
    
    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt){
    	this.readFromNBT(pkt.func_148857_g());
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
