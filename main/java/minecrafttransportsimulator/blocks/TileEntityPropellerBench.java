package minecrafttransportsimulator.blocks;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.sounds.BenchSound;
import minecrafttransportsimulator.systems.SFXSystem.SFXEntity;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityPropellerBench extends MTSTileEntity implements SFXEntity, ITickable{
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
	public void update(){
		if(timeOperationFinished == worldObj.getTotalWorldTime()){
			timeOperationFinished = 0;
			propellerOnBench = new ItemStack(MTSRegistry.propeller, 1, this.propellerType);
			NBTTagCompound stackTag = new NBTTagCompound();
			stackTag.setByte("type", propellerType);
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
			propellerOnBench.setTagCompound(stackTag);
		}
		MTS.proxy.updateSFXEntity(this, worldObj);
	}
	
	public boolean isRunning(){
		return timeOperationFinished != 0 && timeOperationFinished > worldObj.getTotalWorldTime();
	}
	
	public ItemStack getPropellerOnBench(){
		return propellerOnBench;
	}
	
	public void dropPropellerAt(double x, double y, double z){
		System.out.println("DROP");
		if(!worldObj.isRemote){
			worldObj.spawnEntityInWorld(new EntityItem(worldObj, x, y, z, propellerOnBench));
		}
		propellerOnBench = null;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasSound(){
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public MovingSound getNewSound(){
		return new BenchSound(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public MovingSound getCurrentSound(){
		return benchSound;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void setCurrentSound(MovingSound sound){
		benchSound = (BenchSound) sound;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSoundBePlaying(){
		return this.isRunning();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void spawnParticles(){}
	
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
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setByte("propellerType", propellerType);
        tagCompound.setByte("numberBlades", numberBlades);
        tagCompound.setByte("pitch", pitch);
        tagCompound.setByte("diameter", diameter);
        tagCompound.setLong("timeOperationFinished", timeOperationFinished);
        if(propellerOnBench != null){
        	tagCompound.setTag("propellerOnBench", propellerOnBench.writeToNBT(new NBTTagCompound()));
        }
		return tagCompound;
    }
}
