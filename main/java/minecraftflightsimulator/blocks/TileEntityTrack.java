package minecraftflightsimulator.blocks;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import minecraftflightsimulator.packets.general.TileEntityClientRequestDataPacket;
import minecraftflightsimulator.utilites.MFSCurve;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class TileEntityTrack extends TileEntity{
	public boolean renderedLastPass;
	public boolean isPrimary;
	public MFSCurve curve;
	private List<int[]> dummyTracks = new ArrayList<int[]>();
	
	public TileEntityTrack(){
		super();
	}
	
	public TileEntityTrack(MFSCurve curve, boolean isPrimary){
		this.curve = curve;
		this.isPrimary = isPrimary;
	}
	
	public void removeDummyTracks(){
		this.invalidate();
		for(int[] track : dummyTracks){
			BlockHelper.setBlockToAir(worldObj, track[0], track[1], track[2]);
		}
	}
	
	@Override
    public void validate(){
		super.validate();
        if(worldObj.isRemote){
        	MFS.MFSNet.sendToServer(new TileEntityClientRequestDataPacket(this));
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
        if(tagCompound.getIntArray("endPoint").length != 0){
        	curve = new MFSCurve(new int[]{this.xCoord, this.yCoord, this.zCoord}, tagCompound.getIntArray("endPoint"), tagCompound.getFloat("startAngle"), tagCompound.getFloat("endAngle"));
        }
    }
    
	@Override
    public void writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("isPrimary", this.isPrimary);
        tagCompound.setFloat("startAngle", curve.startAngle);
        tagCompound.setFloat("endAngle", curve.endAngle);
        tagCompound.setIntArray("endPoint", curve.blockEndPoint);
    }
}
