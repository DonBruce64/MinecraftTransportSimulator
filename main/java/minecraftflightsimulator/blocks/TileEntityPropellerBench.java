package minecraftflightsimulator.blocks;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.minecrafthelpers.ItemStackHelper;
import minecraftflightsimulator.packets.general.PropellerBenchSyncPacket;
import minecraftflightsimulator.sounds.BenchSound;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityPropellerBench extends TileEntity{
	private byte propellerType = 0;
	private byte numberBlades = 2;
	private byte pitch = 64;
	private byte diameter = 70;
	private byte iron = 0;
	private byte redstone = 0;
	private byte propMaterialQty = 0;
	private long timeOperationFinished = 0;
	private ItemStack propellerOnBench = null;
	private BenchSound benchSound;
	
	public TileEntityPropellerBench(){
		super();
	}
	
	@Override
    public void setWorldObj(World world){
        this.worldObj = world;
        if(worldObj.isRemote){
        	MFS.MFSNet.sendToServer(new PropellerBenchSyncPacket(this));
        }
    }
	
	@Override
	public void updateEntity(){
		if(timeOperationFinished == worldObj.getTotalWorldTime()){
			timeOperationFinished = 0;
			propellerOnBench = new ItemStack(MFSRegistry.propeller);
			NBTTagCompound stackTag = new NBTTagCompound();
			stackTag.setInteger("model", (diameter - 70)/5*1000 + (pitch - 55)/3*100 + numberBlades*10 + propellerType);
			stackTag.setInteger("numberBlades", numberBlades);
			stackTag.setInteger("pitch", pitch);
			stackTag.setInteger("diameter", diameter);
			if(propellerType%10==1){
				stackTag.setFloat("health", 500);
			}else if(propellerType%10==2){
				stackTag.setFloat("health", 1000);
			}else{
				stackTag.setFloat("health", 100);
			}
			//TODO send to SFX system.
			ItemStackHelper.setStackNBT(propellerOnBench, stackTag);
		}
		benchSound = MFS.proxy.updateBenchSound(benchSound, this);
	}
	
	public boolean isRunning(){
		return timeOperationFinished != 0 && timeOperationFinished > worldObj.getTotalWorldTime();
	}
	
	public Item getMaterialItem(){
		switch(propellerType){
			case(0): return ItemStackHelper.getItemByName("planks");
			case(1): return ItemStackHelper.getItemByName("iron_ingot");
			case(2): return ItemStackHelper.getItemByName("obsidian");
			default: return null;
		}
	}
	
	public byte getQtyRequiredMaterial(){
		return (byte) (diameter < 90 ? numberBlades : numberBlades*2);
	}
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
    	private ItemStack propellerOnBench = null;
    	this.propellerType = tagCompound.getByte("propellerType");
    	this.numberBlades = tagCompound.getByte("numberBlades");
    	this.pitch = tagCompound.getByte("pitch");
    	this.diameter = tagCompound.getByte("diameter");
    	this.iron = tagCompound.getByte("iron");
    	this.redstone = tagCompound.getByte("redstone");
    	this.propMaterialQty = tagCompound.getByte("propMaterialQty");
    	this.timeOperationFinished = tagCompound.getLong("timeOperationFinished");
    	this.propellerOnBench = tagCompound.getByte("propellerOnBench");

        NBTTagList nbttaglist = tagCompound.getTagList("Items", 10);
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
