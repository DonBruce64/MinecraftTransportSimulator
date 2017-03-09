package minecraftflightsimulator.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import minecraftflightsimulator.packets.general.TileEntityClientRequestDataPacket;
import minecraftflightsimulator.utilites.MFSCurve;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class TileEntitySurveyFlag extends TileEntity{
	public boolean renderedLastPass;
	public boolean isPrimary;
	public float angle;
	public MFSCurve linkedCurve;
		
	public TileEntitySurveyFlag(){
		super();
	}
	
	public TileEntitySurveyFlag(float angle){
		this.angle = angle;
	}
	
	@Override
    public void validate(){
		super.validate();
        if(worldObj.isRemote){
        	MFS.MFSNet.sendToServer(new TileEntityClientRequestDataPacket(this));
        }
    }
	
	public void linkToFlag(int[] linkedFlagCoords){
		TileEntitySurveyFlag linkedFlag = ((TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(worldObj, linkedFlagCoords[0], linkedFlagCoords[1], linkedFlagCoords[2]));
		linkedCurve = new MFSCurve(new int[]{this.xCoord, this.yCoord, this.zCoord}, linkedFlagCoords, angle, linkedFlag.angle);
	}
	
	public void clearFlagLinking(){
		if(linkedCurve != null){
			int[] linkedFlagCoords = linkedCurve.blockEndPoint;
			linkedCurve = null;
			((TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(worldObj, linkedFlagCoords[0], linkedFlagCoords[1], linkedFlagCoords[2])).clearFlagLinking();
		}
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox(){
		return INFINITE_EXTENT_AABB;
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared(){
        return 65536.0D;
    }
	
	@Override
    public void readFromNBT(NBTTagCompound tagCompound){
        super.readFromNBT(tagCompound);
        this.isPrimary = tagCompound.getBoolean("isPrimary");
        this.angle = tagCompound.getFloat("angle");
        int[] linkedFlagCoords = tagCompound.getIntArray("linkedFlagCoords");
        if(tagCompound.getIntArray("linkedFlagCoords").length != 0){
        	TileEntitySurveyFlag flag = ((TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(worldObj, linkedFlagCoords[0], linkedFlagCoords[1], linkedFlagCoords[2]));
        	linkedCurve = new MFSCurve(new int[]{this.xCoord, this.yCoord, this.zCoord}, linkedFlagCoords, tagCompound.getFloat("angle"), flag.angle);
        }
    }
    
	@Override
    public void writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("isPrimary", this.isPrimary);
        tagCompound.setFloat("angle", angle);
        if(linkedCurve != null){
        	tagCompound.setIntArray("linkedFlagCoords", linkedCurve.blockEndPoint);
        }
    }
}
