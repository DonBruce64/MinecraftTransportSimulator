package minecrafttransportsimulator.blocks.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public class TileEntityTrafficSignalController extends TileEntityBase{
	public boolean orientedOnX;
	public boolean triggerMode;
	public int greenMainTime;
	public int greenCrossTime;
	public int yellowTime;
	public int allRedTime;
	public final List<BlockPos> trafficSignalLocations = new ArrayList<BlockPos>();
	
	public byte operationIndex;
	public long timeOperationStarted;	
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        this.orientedOnX = tagCompound.getBoolean("orientedOnX");
        this.triggerMode = tagCompound.getBoolean("triggerMode");
        this.greenMainTime = tagCompound.getInteger("greenMainTime");
        this.greenCrossTime = tagCompound.getInteger("greenCrossTime");
        this.yellowTime = tagCompound.getInteger("yellowTime");
        this.allRedTime = tagCompound.getInteger("allRedTime");
        
        trafficSignalLocations.clear();
        for(byte i=0; i<tagCompound.getInteger("trafficSignalCount"); ++i){
        	int[] posArray = tagCompound.getIntArray("trafficSignalLocation" + i);
        	trafficSignalLocations.add(new BlockPos(posArray[0], posArray[1], posArray[2]));
        }
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("orientedOnX", this.orientedOnX);
        tagCompound.setBoolean("triggerMode", this.triggerMode);
        tagCompound.setInteger("greenMainTime", this.greenMainTime);
        tagCompound.setInteger("greenCrossTime", this.greenCrossTime);
        tagCompound.setInteger("yellowTime", this.yellowTime);
        tagCompound.setInteger("allRedTime", this.allRedTime);
        
        //Save all pos data to NBT.
        for(byte i=0; i<trafficSignalLocations.size(); ++i){
        	BlockPos trafficSignalPos = trafficSignalLocations.get(i);
	    	tagCompound.setIntArray("trafficSignalLocation" + i, new int[]{trafficSignalPos.getX(), trafficSignalPos.getY(), trafficSignalPos.getZ()});
        }
        tagCompound.setInteger("trafficSignalCount", trafficSignalLocations.size());
        return tagCompound;
    }
}
