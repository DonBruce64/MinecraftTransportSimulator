package minecraftflightsimulator.blocks;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.minecrafthelpers.ItemStackHelper;
import minecraftflightsimulator.packets.general.PropellerBenchSyncPacket;
import minecraftflightsimulator.sounds.BenchSound;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityPropellerBench extends TileEntity{
	public byte propellerType = 0;
	public byte numberBlades = 2;
	public byte pitch = 64;
	public byte diameter = 70;
	public long timeOperationFinished = 0;
	
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
			propellerOnBench = new ItemStack(MFSRegistry.propeller, 1, propellerType);
			NBTTagCompound stackTag = new NBTTagCompound();
			stackTag.setInteger("numberBlades", numberBlades);
			stackTag.setInteger("pitch", pitch);
			stackTag.setInteger("diameter", diameter);
			if(propellerType==1){
				stackTag.setFloat("health", 500);
			}else if(propellerType==2){
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
	
	public ItemStack getPropellerOnBench(){
		return propellerOnBench;
	}
	
	public void dropPropellerAt(double x, double y, double z){
		if(propellerOnBench != null){
			worldObj.spawnEntityInWorld(new EntityItem(worldObj, x, y, z, propellerOnBench));
			propellerOnBench = null;
			MFS.MFSNet.sendToAll(new PropellerBenchSyncPacket(this));
		}
	}
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
    	this.propellerType = tagCompound.getByte("propellerType");
    	this.numberBlades = tagCompound.getByte("numberBlades");
    	this.pitch = tagCompound.getByte("pitch");
    	this.diameter = tagCompound.getByte("diameter");
    	this.timeOperationFinished = tagCompound.getLong("timeOperationFinished");
    	NBTTagCompound itemTag = tagCompound.getCompoundTag("propellerOnBench");
    	if(itemTag != null){
    		this.propellerOnBench = ItemStack.loadItemStackFromNBT(itemTag);
    	}
    }
    
	@Override
    public void writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setByte("propellerType", propellerType);
        tagCompound.setByte("numberBlades", numberBlades);
        tagCompound.setByte("pitch", pitch);
        tagCompound.setByte("diameter", diameter);
        tagCompound.setLong("timeOperationFinished", timeOperationFinished);
        if(propellerOnBench != null){
        	tagCompound.setTag("propellerOnBench", propellerOnBench.writeToNBT(new NBTTagCompound()));
        }
    }
}
