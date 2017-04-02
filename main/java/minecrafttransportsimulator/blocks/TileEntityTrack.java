package minecrafttransportsimulator.blocks;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecrafttransportsimulator.baseclasses.MTSCurve;
import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import minecrafttransportsimulator.minecrafthelpers.BlockHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;

public class TileEntityTrack extends MTSTileEntity{
	public boolean renderedLastPass;
	public boolean isPrimary;
	public MTSCurve curve;
	private List<int[]> fakeTracks = new ArrayList<int[]>();
	
	public TileEntityTrack(){
		super();
	}
	
	public TileEntityTrack(MTSCurve curve, boolean isPrimary){
		this.curve = curve;
		this.isPrimary = isPrimary;
	}
	
	public void setFakeTracks(List<int[]> fakeTracks){
		this.fakeTracks = fakeTracks;
	}
	
	public List<int[]> getFakeTracks(){
		return this.fakeTracks;
	}
	
	public void removeFakeTracks(){
		this.invalidate();
		for(int[] track : fakeTracks){
			BlockHelper.setBlockToAir(worldObj, track[0], track[1], track[2]);
		}
		BlockTrackFake.overrideBreakingBlocks = false;
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
        	curve = new MTSCurve(new int[]{this.xCoord, this.yCoord, this.zCoord}, tagCompound.getIntArray("endPoint"), tagCompound.getFloat("startAngle"), tagCompound.getFloat("endAngle"));
        }
        
        this.fakeTracks.clear();
        int[] fakeXCoords = tagCompound.getIntArray("fakeXCoords");
        int[] fakeYCoords = tagCompound.getIntArray("fakeYCoords");
        int[] fakeZCoords = tagCompound.getIntArray("fakeZCoords");
        int[] fakeHeights = tagCompound.getIntArray("fakeHeights");
        for(int i=0; i<fakeXCoords.length; ++i){
        	fakeTracks.add(new int[]{fakeXCoords[i], fakeYCoords[i], fakeZCoords[i], fakeHeights[i]});
        }
    }
    
	@Override
    public void writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        if(curve != null){
	        tagCompound.setBoolean("isPrimary", this.isPrimary);
	        tagCompound.setFloat("startAngle", curve.startAngle);
	        tagCompound.setFloat("endAngle", curve.endAngle);
	        tagCompound.setIntArray("endPoint", curve.blockEndPoint);
        }else{
        	this.invalidate();
        }
        
        int[] fakeXCoords = new int[fakeTracks.size()];
        int[] fakeYCoords = new int[fakeTracks.size()];
        int[] fakeZCoords = new int[fakeTracks.size()];
        int[] fakeHeights = new int[fakeTracks.size()];
        for(int i=0;i<fakeTracks.size(); ++i){
        	fakeXCoords[i] = fakeTracks.get(i)[0];
        	fakeYCoords[i] = fakeTracks.get(i)[1];
        	fakeZCoords[i] = fakeTracks.get(i)[2];
        	fakeHeights[i] = fakeTracks.get(i)[3];
        }
        tagCompound.setIntArray("fakeXCoords", fakeXCoords);
        tagCompound.setIntArray("fakeYCoords", fakeYCoords);
        tagCompound.setIntArray("fakeZCoords", fakeZCoords);
        tagCompound.setIntArray("fakeHeights", fakeHeights);
    }
}
