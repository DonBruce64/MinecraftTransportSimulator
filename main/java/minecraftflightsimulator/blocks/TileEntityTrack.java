package minecraftflightsimulator.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.minecrafthelpers.BlockHelper;
import minecraftflightsimulator.minecrafthelpers.PlayerHelper;
import minecraftflightsimulator.packets.general.ChatPacket;
import minecraftflightsimulator.packets.general.TileEntityClientRequestDataPacket;
import minecraftflightsimulator.utilites.MFSCurve;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
	
	public TileEntityTrack(int[] startPoint, int[] endPoint, float startAngle, float endAngle, boolean isPrimary){
		curve = new MFSCurve(startPoint, endPoint, startAngle, endAngle);
		this.isPrimary = isPrimary;
	}
	
	public boolean setDummyTracks(EntityPlayer setter){
		float[] currentPoint;		
		float currentAngle;
		float currentSin;
		float currentCos;
		
		//First make sure blocks can be placed.
		List<int[]> blockList = new ArrayList<int[]>();
		for(short i=0; i <= curve.pathLength; ++i){
			currentPoint = curve.getPointAt(i/curve.pathLength);
			currentAngle = 90 + curve.getYawAngleAt(i/curve.pathLength);
			currentSin = (float) Math.sin(Math.toRadians(currentAngle));
			currentCos = (float) Math.cos(Math.toRadians(currentAngle));

			int[] offset = new int[3];
			for(byte j=-1; j<=1; ++j){
				offset[0] = (int) (currentPoint[0] + j*currentSin);
				offset[1] = (int) currentPoint[1];
				offset[2] = (int) (currentPoint[2] + j*currentCos);
				if(BlockHelper.canPlaceBlockAt(worldObj, offset[0], offset[1], offset[2])){
					blockList.add(new int[] {offset[0], offset[1], offset[2], (int) (currentPoint[1]%1)*16});
				}else{
					if(!Arrays.equals(curve.blockEndPoint, offset)){
						MFS.MFSNet.sendTo(new ChatPacket(PlayerHelper.getTranslatedText("interact.wand.failure.blockage") + " X:" + offset[0] + " Y:" + offset[1] + " Z:" + offset[2]), (EntityPlayerMP) setter);
						return false;
					}
				}
			}
		}
		//TODO spawn TES and blocks.
		return true;
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
