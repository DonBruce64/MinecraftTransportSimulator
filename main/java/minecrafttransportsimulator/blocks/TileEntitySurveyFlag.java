package minecrafttransportsimulator.blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSCurve;
import minecrafttransportsimulator.baseclasses.MTSTileEntity;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.packets.general.TileEntitySyncPacket;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntitySurveyFlag extends MTSTileEntity{
	public MTSCurve linkedCurve;
		
	public TileEntitySurveyFlag(){
		super();
	}
	
	public void linkToFlag(BlockPos linkedFlagPos){
		if(linkedCurve != null){
			((TileEntitySurveyFlag) worldObj.getTileEntity(linkedCurve.endPos.add(this.pos))).clearFlagLinking();
		}
		TileEntitySurveyFlag linkedFlag = ((TileEntitySurveyFlag) worldObj.getTileEntity(linkedFlagPos));
		linkedCurve = new MTSCurve(linkedFlagPos.subtract(this.pos), rotation*45, linkedFlag.rotation*45);
		MTS.MTSNet.sendToAll(new TileEntitySyncPacket(this));
	}
	
	public void clearFlagLinking(){
		if(linkedCurve != null){
			TileEntitySurveyFlag linkedFlag = ((TileEntitySurveyFlag) worldObj.getTileEntity(linkedCurve.endPos.add(this.pos)));
			linkedCurve = null;
			if(linkedFlag != null){
				linkedFlag.clearFlagLinking();
			}
			MTS.MTSNet.sendToAll(new TileEntitySyncPacket(this));
		}
	}
	
	/**
	 * Spawns dummy tracks based on flag linking.  Returns null if successful or
	 * the coordinates of the location where an existing block is if not.
	 */
	public BlockPos spawnDummyTracks(){
		MTSCurve curve = linkedCurve;
		MTSCurve otherFlagCurve = ((TileEntitySurveyFlag) worldObj.getTileEntity(this.pos.add(linkedCurve.endPos))).linkedCurve;
		
		Map<BlockPos, Byte> blockMap = new HashMap<BlockPos, Byte>();
		
		//Need to see which end of the curve is higher.
		//If we go top-down, the fake tracks are too high and ballast looks weird.
		BlockPos blockingBlock = addFakeTracksToMap(curve.endPos.getY() >= 0 ? curve : otherFlagCurve, blockMap);
		if(blockingBlock != null){
			return blockingBlock;
		}
		//Make sure that if we went from the other direction we wouldn't miss ballast below the track.
		//Steep hills tend to do this.
		blockingBlock = addFakeTracksToMap(curve.endPos.getY() <= 0 ? otherFlagCurve : curve, blockMap);
		if(blockingBlock != null){
			return blockingBlock;
		}
		
		BlockTrackStructureFake.disableMainTrackBreakage();
		for(BlockPos placementPos : blockMap.keySet()){
			worldObj.setBlockState(placementPos, MTSRegistry.trackStructureFake.getDefaultState().withProperty(BlockTrackStructureFake.height, (int) blockMap.get(placementPos)));			
		}
		
		worldObj.setBlockState(this.pos, MTSRegistry.trackStructure.getDefaultState());
		worldObj.setBlockState(this.pos.add(curve.endPos), MTSRegistry.trackStructure.getDefaultState());
		TileEntityTrack startTile = new TileEntityTrack(curve);
		TileEntityTrack endTile = new TileEntityTrack(otherFlagCurve);
		startTile.setFakeTracks(new ArrayList<BlockPos>(blockMap.keySet()));
		endTile.setFakeTracks(new ArrayList<BlockPos>(blockMap.keySet()));
		worldObj.setTileEntity(this.pos, startTile);
		worldObj.setTileEntity(this.pos.add(curve.endPos), endTile);
		BlockTrackStructureFake.enableMainTrackBreakage();
		return null;
	}
	
	private BlockPos addFakeTracksToMap(MTSCurve curve, Map<BlockPos, Byte> blockMap){
		float[] currentPoint;		
		float currentAngle;
		float currentSin;
		float currentCos;
		
		for(float f=0; f <= curve.pathLength; f = Math.min(f + 0.05F, curve.pathLength)){
			currentPoint = curve.getCachedPointAt(f/curve.pathLength);
			currentAngle = curve.getCachedYawAngleAt(f/curve.pathLength);
			currentSin = (float) Math.sin(Math.toRadians(currentAngle));
			currentCos = (float) Math.cos(Math.toRadians(currentAngle));
			//Offset the current point slightly to account for the height of the ties.
			//We don't want to judge fake block height by the bottom of the ties,
			//rather we need to judge from the middle of them.
			currentPoint[1] += 1/16F;

			for(byte j=-1; j<=1; ++j){
				BlockPos placementPos = new BlockPos(Math.round(currentPoint[0] - 0.5 + j*currentCos), currentPoint[1], Math.round(currentPoint[2] - 0.5 + j*currentSin)).add(this.pos);
				if(!worldObj.getBlockState(placementPos).getBlock().canPlaceBlockAt(worldObj, placementPos)){
					if(!(this.pos.equals(placementPos) || this.pos.add(curve.endPos).equals(placementPos))){
						return placementPos;
					}
				}
				boolean isBlockInList = false;
				if(blockMap.containsKey(placementPos)){
					isBlockInList = true;
					break;
				}
				if(!isBlockInList){
					blockMap.put(placementPos, (byte) (currentPoint[1]%1*16F));
					//Double-check to see if there's a block already in the list below this one.
					//If so, we're on a slope and that block needs a height of 16.
					if(blockMap.containsKey(placementPos.down())){
						blockMap.put(placementPos.down(), (byte) 15);
					}
				}
			}
			if(f == curve.pathLength){
				//Before we break off, check if we need to add 'spacers' for the beginning and end segments.
				//This is needed for diagonals to have fake tracks in the joins.
				//Do this for the start and end of this curve.
				if(curve.startAngle%90 != 0){
					BlockPos blocker = addSpacersToMap(this.pos, blockMap);
					if(blocker != null){
						return blocker;
					}
				}
				if(curve.endAngle%90 != 0){
					BlockPos blocker = addSpacersToMap(this.pos.add(curve.endPos), blockMap);
					if(blocker != null){
						return blocker;
					}
				}
				break;
			}
		}
		return null;
	}
	
	private BlockPos addSpacersToMap(BlockPos posToCheckAround, Map<BlockPos, Byte> blockMap){
		for(byte i=-1; i<=1; ++i){
			for(byte j=-1; j<=1; ++j){
				if((i == 0 || j == 0) && i!=j){
					BlockPos testPos = posToCheckAround.add(i, 0, j);
					if(!worldObj.getBlockState(testPos).getBlock().canPlaceBlockAt(worldObj, testPos)){
						Block blockingBlock = worldObj.getBlockState(testPos).getBlock();
						if(blockingBlock.equals(MTSRegistry.trackStructure) || blockingBlock.equals(MTSRegistry.trackStructureFake)){
							continue;
						}else{
							return testPos;
						}
					}else if(!blockMap.containsKey(testPos)){
						blockMap.put(testPos, (byte) 1);
					}
				}
			}
		}
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
        int[] linkedFlagCoords = tagCompound.getIntArray("linkedFlagCoords");
        if(tagCompound.getIntArray("linkedFlagCoords").length != 0){
        	linkedCurve = new MTSCurve(new BlockPos(linkedFlagCoords[0], linkedFlagCoords[1], linkedFlagCoords[2]).subtract(this.pos), this.rotation*45, tagCompound.getFloat("linkedFlagAngle"));
        }else{
        	linkedCurve = null;
        }
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound){
        super.writeToNBT(tagCompound);
        if(linkedCurve != null){
        	tagCompound.setIntArray("linkedFlagCoords", new int[]{linkedCurve.endPos.getX() + this.pos.getX(), linkedCurve.endPos.getY() + this.pos.getY(), linkedCurve.endPos.getZ() + this.pos.getZ()});
        	tagCompound.setFloat("linkedFlagAngle", linkedCurve.endAngle);
        }
        return tagCompound;
    }
}
