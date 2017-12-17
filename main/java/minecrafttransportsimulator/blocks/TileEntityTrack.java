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
	public boolean hasTriedToConnectToOtherSegment;
	public TileEntityTrack connectedTrack;
	public MTSCurve curve;
	private List<BlockPos> fakeTracks = new ArrayList<BlockPos>();
	
	public TileEntityTrack(){
		super();
	}
	
	public TileEntityTrack(MTSCurve curve){
		this.curve = curve;
	}
	
	public void setFakeTracks(List<BlockPos> fakeTracks){
		this.fakeTracks = fakeTracks;
	}
	
	public List<BlockPos> getFakeTracks(){
		return this.fakeTracks;
	}
	
	public void removeFakeTracks(){
		this.invalidate();
		BlockTrackStructureFake.disableMainTrackBreakage();
		for(BlockPos fakePos : fakeTracks){
			worldObj.setBlockToAir(fakePos);
		}
		BlockTrackStructureFake.enableMainTrackBreakage();
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
        int[] endCoords = tagCompound.getIntArray("curveEndPoint");
        if(endCoords.length != 0){
        	curve = new MTSCurve(new BlockPos(endCoords[0], endCoords[1], endCoords[2]), tagCompound.getFloat("curveStartAngle"), tagCompound.getFloat("curveEndAngle"));
        }
        
        this.fakeTracks.clear();
        int[] fakeXCoords = tagCompound.getIntArray("fakeXCoords");
        int[] fakeYCoords = tagCompound.getIntArray("fakeYCoords");
        int[] fakeZCoords = tagCompound.getIntArray("fakeZCoords");
        for(int i=0; i<fakeXCoords.length; ++i){
        	fakeTracks.add(new BlockPos(fakeXCoords[i], fakeYCoords[i], fakeZCoords[i]));
        }
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        if(curve != null){
	        tagCompound.setFloat("curveStartAngle", curve.startAngle);
	        tagCompound.setFloat("curveEndAngle", curve.endAngle);
	        tagCompound.setIntArray("curveEndPoint", new int[]{curve.endPos.getX(), curve.endPos.getY(), curve.endPos.getZ()});
        }else{
        	this.invalidate();
        }
        
        int[] fakeXCoords = new int[fakeTracks.size()];
        int[] fakeYCoords = new int[fakeTracks.size()];
        int[] fakeZCoords = new int[fakeTracks.size()];
        for(int i=0;i<fakeTracks.size(); ++i){
        	fakeXCoords[i] = fakeTracks.get(i).getX();
        	fakeYCoords[i] = fakeTracks.get(i).getY();
        	fakeZCoords[i] = fakeTracks.get(i).getZ();
        }
        tagCompound.setIntArray("fakeXCoords", fakeXCoords);
        tagCompound.setIntArray("fakeYCoords", fakeYCoords);
        tagCompound.setIntArray("fakeZCoords", fakeZCoords);
		return tagCompound;
    }
}
