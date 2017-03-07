package minecraftflightsimulator.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.packets.general.TileEntityClientRequestDataPacket;
import minecraftflightsimulator.utilites.MFSCurve;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class TileEntityRail extends TileEntity{
	public boolean renderedLastPass;
	public boolean isPrimary;
	public MFSCurve curve;
	
	public TileEntityRail(){
		super();
	}
	
	public TileEntityRail(int[] startPoint, int[] endPoint, float startAngle, float endAngle, boolean isPrimary){
		curve = new MFSCurve(startPoint, endPoint, startAngle, endAngle);
		this.isPrimary = isPrimary;
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
        tagCompound.setIntArray("endPoint", new int[]{(int) (curve.endPoint[0] - 0.5F), (int) curve.endPoint[1], (int) (curve.endPoint[2] - 0.5F)});
    }
}
