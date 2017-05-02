package minecrafttransportsimulator.blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSCurve;
import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.packets.general.TileEntitySyncPacket;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntitySurveyFlag extends MTSTileEntity{
	public boolean renderedLastPass;
	public boolean isPrimary;
	public MTSCurve linkedCurve;
		
	public TileEntitySurveyFlag(){
		super();
	}
	
	public void linkToFlag(BlockPos linkedFlagPos, boolean isPrimary){
		if(linkedCurve != null){
			((TileEntitySurveyFlag) worldObj.getTileEntity(linkedCurve.blockEndPos)).clearFlagLinking();
		}
		this.isPrimary = isPrimary;
		TileEntitySurveyFlag linkedFlag = ((TileEntitySurveyFlag) worldObj.getTileEntity(linkedFlagPos));
		linkedCurve = new MTSCurve(this.pos, linkedFlagPos, rotation*45, linkedFlag.rotation*45);
		MTS.MFSNet.sendToAll(new TileEntitySyncPacket(this));
	}
	
	public void clearFlagLinking(){
		if(linkedCurve != null){
			linkedCurve = null;
			TileEntitySurveyFlag linkedFlag = ((TileEntitySurveyFlag) worldObj.getTileEntity(linkedCurve.blockEndPos));
			if(linkedFlag != null){
				linkedFlag.clearFlagLinking();
			}
			MTS.MFSNet.sendToAll(new TileEntitySyncPacket(this));
		}
	}
	
	/**
	 * Spawns dummy tracks based on flag linking.  Returns null if successful or
	 * the coordinates of the location where an existing block is if not.
	 * 
	 */
	public BlockPos spawnDummyTracks(){
		float[] currentPoint;		
		float currentAngle;
		float currentSin;
		float currentCos;
		
		Map<BlockPos, Byte> blockMap = new HashMap<BlockPos, Byte>();
		for(float f=0; f <= linkedCurve.pathLength; f = Math.min(f + 0.25F, linkedCurve.pathLength)){
			currentPoint = linkedCurve.getCachedPointAt(f/linkedCurve.pathLength);
			currentAngle = linkedCurve.getCachedYawAngleAt(f/linkedCurve.pathLength);
			currentSin = (float) Math.sin(Math.toRadians(currentAngle));
			currentCos = (float) Math.cos(Math.toRadians(currentAngle));

			for(byte j=-1; j<=1; ++j){
				BlockPos placementPos = new BlockPos(Math.round(currentPoint[0] - 0.5 + j*currentCos), Math.floor(currentPoint[1] + 0.01), Math.round(currentPoint[2] - 0.5 + j*currentSin));
				if(worldObj.getBlockState(placementPos).getBlock().canPlaceBlockAt(worldObj, placementPos)){
					boolean isBlockInList = false;
					if(blockMap.containsKey(placementPos)){
						isBlockInList = true;
						break;
					}
					if(!isBlockInList){
						blockMap.put(placementPos, (byte) Math.max(((currentPoint[1] + 0.01)%1)*16, 1));
					}
				}else{
					if(!(linkedCurve.blockStartPos.equals(placementPos) || linkedCurve.blockEndPos.equals(placementPos))){
						return placementPos;
					}
				}
			}
			if(f == linkedCurve.pathLength){
				break;
			}
		}
		
		BlockTrackFake.overrideBreakingBlocks = true;
		for(BlockPos placementPos : blockMap.keySet()){
			worldObj.setBlockState(placementPos, MTSRegistry.blockTrackFake.getDefaultState().withProperty(BlockTrackFake.height, Math.max(blockMap.get(placementPos), 4)));
			//TODO make sure this is not needed.  I had issues with Tile Entities and blocks not spawning in 1.7.10.
			//worldObj.markBlockForUpdate(blockData[0], blockData[1], blockData[2]);			
		}
		MTSCurve curve = linkedCurve;
		MTSCurve otherFlagCurve = ((TileEntitySurveyFlag) worldObj.getTileEntity(linkedCurve.blockEndPos)).linkedCurve;
		boolean primary = isPrimary;
		
		worldObj.setBlockState(curve.blockStartPos, MTSRegistry.blockTrack.getDefaultState());
		worldObj.setBlockState(curve.blockEndPos, MTSRegistry.blockTrack.getDefaultState());
		
		//TODO make sure this is not needed.  I had issues with Tile Entities and blocks not spawning in 1.7.10.
		//worldObj.markBlockForUpdate(curve.blockStartPos[0], curve.blockStartPos[1], curve.blockStartPos[2]);
		//worldObj.markBlockForUpdate(curve.blockEndPos[0], curve.blockEndPos[1], curve.blockEndPos[2]);
		
		TileEntityTrack startTile = new TileEntityTrack(curve);
		TileEntityTrack endTile = new TileEntityTrack(otherFlagCurve);
		startTile.setFakeTracks(new ArrayList<BlockPos>(blockMap.keySet()));
		endTile.setFakeTracks(new ArrayList<BlockPos>(blockMap.keySet()));
		worldObj.setTileEntity(curve.blockStartPos, startTile);
		worldObj.setTileEntity(curve.blockEndPos, endTile);
		BlockTrackFake.overrideBreakingBlocks = false;
		return null;
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
        int[] linkedFlagCoords = tagCompound.getIntArray("linkedFlagCoords");
        if(tagCompound.getIntArray("linkedFlagCoords").length != 0){
        	linkedCurve = new MTSCurve(this.pos, new BlockPos(linkedFlagCoords[0], linkedFlagCoords[1], linkedFlagCoords[2]), this.rotation*45, tagCompound.getFloat("linkedFlagAngle"));
        }else{
        	linkedCurve = null;
        }
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("isPrimary", this.isPrimary);
        if(linkedCurve != null){
        	tagCompound.setIntArray("linkedFlagCoords", new int[]{linkedCurve.blockEndPos.getX(), linkedCurve.blockEndPos.getY(), linkedCurve.blockEndPos.getZ()});
        	tagCompound.setFloat("linkedFlagAngle", linkedCurve.endAngle);
        }
        return tagCompound;
    }
}
