package minecrafttransportsimulator.blocks.core;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.parts.ItemPartPropeller;
import minecrafttransportsimulator.packets.crafting.PropellerBenchUpdatePacket;
import minecrafttransportsimulator.sounds.BenchSound;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.SFXSystem.SoundPart;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityPropellerBench extends ATileEntityRotatable implements SoundPart, ITickable{
	public long timeOperationFinished = 0;
	public ItemPartPropeller selectedPropeller = null;
	public ItemPartPropeller propellerOnBench = null;
	private BenchSound benchSound;
	
	public TileEntityPropellerBench(){
		super();
	}
	
	@Override
	public void update(){
		if(timeOperationFinished == worldObj.getTotalWorldTime()){
			timeOperationFinished = 0;
			propellerOnBench = selectedPropeller;
		}
		MTS.proxy.updateSoundPart(this, worldObj);
	}
	
	public boolean isRunning(){
		return timeOperationFinished != 0 && timeOperationFinished > worldObj.getTotalWorldTime();
	}
	
	public void dropPropellerAt(double x, double y, double z){
		if(!worldObj.isRemote && propellerOnBench != null){
			ItemStack propellerStack = new ItemStack(propellerOnBench);
			NBTTagCompound stackTag = new NBTTagCompound();
			stackTag.setFloat("health", PackParserSystem.getPartPack(propellerOnBench.partName).propeller.startingHealth);
			propellerStack.setTagCompound(stackTag);
			worldObj.spawnEntityInWorld(new EntityItem(worldObj, x, y, z, propellerStack));
			propellerOnBench = null;
			MTS.MTSNet.sendToAll(new PropellerBenchUpdatePacket(this, null));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public MovingSound getNewSound(){
		return new BenchSound(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSoundBePlaying(){
		return this.isInvalid() ? false : this.isRunning();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Vec3d getSoundPosition(){
		return new Vec3d(this.pos.getX(), this.pos.getY(), this.pos.getZ());
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public Vec3d getSoundMotion(){
		return Vec3d.ZERO;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getSoundVolume(){
		return 1.0F;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public float getSoundPitch(){
		return 1.0F;
	}
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
    	this.timeOperationFinished = tagCompound.getLong("timeOperationFinished");
    	if(tagCompound.hasKey("selectedPropeller")){
    		this.selectedPropeller = (ItemPartPropeller) MTSRegistry.partItemMap.get(tagCompound.getString("selectedPropeller"));
    	}
    	if(tagCompound.hasKey("propellerOnBench")){
    		this.propellerOnBench = (ItemPartPropeller) MTSRegistry.partItemMap.get(tagCompound.getString("propellerOnBench"));
    	}
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setLong("timeOperationFinished", timeOperationFinished);
        if(selectedPropeller != null){
        	tagCompound.setString("selectedPropeller", selectedPropeller.partName);
        }
        if(propellerOnBench != null){
        	tagCompound.setString("propellerOnBench", propellerOnBench.partName);
        }
		return tagCompound;
    }
}
