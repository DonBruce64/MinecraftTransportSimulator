package minecraftflightsimulator.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.packets.general.TileEntityFakeTrackHeightPacket;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class TileEntityTrackFake extends TileEntity{
	public float height;
	public int[] masterTrackPos;
	
	public TileEntityTrackFake(){
		super();
	}
	
	public TileEntityTrackFake(float height, int[] masterTrackPos){
		this.height = height;
		this.masterTrackPos = masterTrackPos;
	}
	
	@Override
    public void validate(){
		super.validate();
        if(worldObj.isRemote){
        	MFS.MFSNet.sendToServer(new TileEntityFakeTrackHeightPacket(this));
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
        this.height = tagCompound.getFloat("height");
        this.masterTrackPos = tagCompound.getIntArray("masterTrackPos");
    }
    
	@Override
    public void writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setFloat("height", height);
        tagCompound.setIntArray("masterTrackPos", masterTrackPos);
    }
}
