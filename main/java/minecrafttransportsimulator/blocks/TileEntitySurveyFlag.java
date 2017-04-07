package minecrafttransportsimulator.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSBlock;
import minecrafttransportsimulator.baseclasses.MTSCurve;
import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.minecrafthelpers.BlockHelper;
import minecrafttransportsimulator.packets.general.TileEntitySyncPacket;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;

public class TileEntitySurveyFlag extends MTSTileEntity{
	public boolean renderedLastPass;
	public boolean isPrimary;
	public MTSCurve linkedCurve;
		
	public TileEntitySurveyFlag(){
		super();
	}
	
	public void linkToFlag(int[] linkedFlagCoords, boolean isPrimary){
		if(linkedCurve != null){
			((TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(worldObj, linkedCurve.blockEndPoint[0], linkedCurve.blockEndPoint[1], linkedCurve.blockEndPoint[2])).clearFlagLinking();
		}
		this.isPrimary = isPrimary;
		TileEntitySurveyFlag linkedFlag = ((TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(worldObj, linkedFlagCoords[0], linkedFlagCoords[1], linkedFlagCoords[2]));
		linkedCurve = new MTSCurve(new int[]{this.xCoord, this.yCoord, this.zCoord}, linkedFlagCoords, rotation*45, linkedFlag.rotation*45);
		MTS.MFSNet.sendToAll(new TileEntitySyncPacket(this));
	}
	
	public void clearFlagLinking(){
		if(linkedCurve != null){
			int[] linkedFlagCoords = linkedCurve.blockEndPoint;
			linkedCurve = null;
			TileEntitySurveyFlag linkedFlag = ((TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(worldObj, linkedFlagCoords[0], linkedFlagCoords[1], linkedFlagCoords[2]));
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
	public int[] spawnDummyTracks(){
		float[] currentPoint;		
		float currentAngle;
		float currentSin;
		float currentCos;
		
		List<int[]> blockList = new ArrayList<int[]>();
		for(float f=0; f <= linkedCurve.pathLength; f = Math.min(f + 0.25F, linkedCurve.pathLength)){
			currentPoint = linkedCurve.getPointAt(f/linkedCurve.pathLength);
			currentAngle = linkedCurve.getYawAngleAt(f/linkedCurve.pathLength);
			currentSin = (float) Math.sin(Math.toRadians(currentAngle));
			currentCos = (float) Math.cos(Math.toRadians(currentAngle));

			int[] offset = new int[3];
			for(byte j=-1; j<=1; ++j){
				offset[0] = (int) Math.round(currentPoint[0] - 0.5 + j*currentCos);
				offset[1] = (int) Math.floor(currentPoint[1] + 0.01);
				offset[2] = (int) Math.round(currentPoint[2] - 0.5 + j*currentSin);
				if(BlockHelper.canPlaceBlockAt(worldObj, offset[0], offset[1], offset[2])){
					boolean isBlockInList = false;
					for(int[] coords : blockList){
						if(coords[0] == offset[0] && coords[2] == offset[2]){
							isBlockInList = true;
							break;
						}
					}
					if(!isBlockInList){
						blockList.add(new int[] {offset[0], offset[1], offset[2], (int) Math.max(((currentPoint[1] + 0.01)%1)*16, 1)});
					}
				}else{
					if(!(Arrays.equals(linkedCurve.blockStartPoint, offset) || Arrays.equals(linkedCurve.blockEndPoint, offset))){
						return offset;
					}
				}
			}
			if(f == linkedCurve.pathLength){
				break;
			}
		}
		
		int[] masterLocation = new int[]{this.xCoord, this.yCoord, this.zCoord};
		BlockTrackFake.overrideBreakingBlocks = true;
		for(int[] blockData : blockList){
			worldObj.setBlock(blockData[0], blockData[1], blockData[2], MTSRegistry.blockTrackFake);
			((MTSBlock) MTSRegistry.blockTrackFake).setBlockMetadata(worldObj, blockData[0], blockData[1], blockData[2], (byte) Math.max(blockData[3], 4));
			worldObj.markBlockForUpdate(blockData[0], blockData[1], blockData[2]);			
		}
		MTSCurve curve = linkedCurve;
		MTSCurve otherFlagCurve = ((TileEntitySurveyFlag) BlockHelper.getTileEntityFromCoords(worldObj, linkedCurve.blockEndPoint[0], linkedCurve.blockEndPoint[1], linkedCurve.blockEndPoint[2])).linkedCurve;
		boolean primary = isPrimary;
		
		worldObj.setBlock(curve.blockStartPoint[0], curve.blockStartPoint[1], curve.blockStartPoint[2], MTSRegistry.blockTrack);
		worldObj.setBlock(curve.blockEndPoint[0], curve.blockEndPoint[1], curve.blockEndPoint[2], MTSRegistry.blockTrack);
		
		worldObj.markBlockForUpdate(curve.blockStartPoint[0], curve.blockStartPoint[1], curve.blockStartPoint[2]);
		worldObj.markBlockForUpdate(curve.blockEndPoint[0], curve.blockEndPoint[1], curve.blockEndPoint[2]);
		
		TileEntityTrack startTile = new TileEntityTrack(curve);
		TileEntityTrack endTile = new TileEntityTrack(otherFlagCurve);
		startTile.setFakeTracks(blockList);
		endTile.setFakeTracks(blockList);
		worldObj.setTileEntity(curve.blockStartPoint[0], curve.blockStartPoint[1], curve.blockStartPoint[2], startTile);
		worldObj.setTileEntity(curve.blockEndPoint[0], curve.blockEndPoint[1], curve.blockEndPoint[2], endTile);
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
        	linkedCurve = new MTSCurve(new int[]{this.xCoord, this.yCoord, this.zCoord}, linkedFlagCoords, this.rotation*45, tagCompound.getFloat("linkedFlagAngle"));
        }else{
        	linkedCurve = null;
        }
    }
    
	@Override
    public void writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("isPrimary", this.isPrimary);
        if(linkedCurve != null){
        	tagCompound.setIntArray("linkedFlagCoords", linkedCurve.blockEndPoint);
        	tagCompound.setFloat("linkedFlagAngle", linkedCurve.endAngle);
        }
    }
}
