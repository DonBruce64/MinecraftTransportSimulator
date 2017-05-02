package minecrafttransportsimulator.blocks;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.MTSCurve;
import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityTrack extends MTSTileEntity{
	public boolean renderedLastPass;
	public boolean hasTriedToConnectToOtherSegment;
	public TileEntityTrack connectedTrack;
	public MTSCurve curve;
	private List<int[]> fakeTracks = new ArrayList<int[]>();
	
	public TileEntityTrack(){
		super();
	}
	
	public TileEntityTrack(MTSCurve curve){
		this.curve = curve;
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
			worldObj.setBlockToAir(new BlockPos(track[0], track[1], track[2]));
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
        if(tagCompound.getIntArray("endPoint").length != 0){
        	curve = new MTSCurve(new int[]{this.pos.getX(), this.pos.getY(), this.pos.getZ()}, tagCompound.getIntArray("endPoint"), tagCompound.getFloat("startAngle"), tagCompound.getFloat("endAngle"));
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
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        if(curve != null){
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
		return tagCompound;
    }
}
