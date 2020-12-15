package minecrafttransportsimulator.items.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockRoad;
import minecrafttransportsimulator.blocks.instances.BlockRoadCollision;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadClickData;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadLane;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;

public class ItemRoadComponent extends AItemPack<JSONRoadComponent> implements IItemBlock{
	private final Map<IWrapperPlayer, Point3i> lastPositionClicked = new HashMap<IWrapperPlayer, Point3i>();
	private final Map<IWrapperPlayer, Double> lastRotationClicked = new HashMap<IWrapperPlayer, Double>();
	private final Map<IWrapperPlayer, RoadClickData> lastRoadClickedData = new HashMap<IWrapperPlayer, RoadClickData>();
	
	public ItemRoadComponent(JSONRoadComponent definition){
		super(definition);
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data){
		if(definition.general.type.equals("core")){
			for(byte i=1; i<=5; ++i){
				tooltipLines.add(MasterLoader.coreInterface.translate("info.item.roadcomponent.line" + String.valueOf(i)));
			}
		}
	}
	
	@Override
	public boolean onBlockClicked(IWrapperWorld world, IWrapperPlayer player, Point3i point, Axis axis){
		//Create curves between the prior clicked point and the current point.
		//If the prior point is null, set it when we click it.
		//This could be either a block or a road itself.
		//If we click a road, we need to figure out what lane number we will connect to.
		if(!world.isClient()){
			if(player.isSneaking() || !lastPositionClicked.containsKey(player)){
				//Set starting point.  This may or may not be a road segment.
				lastPositionClicked.put(player, point);
				lastRotationClicked.put(player, (double) player.getYaw());
				ABlockBase clickedBlock = world.getBlock(point);
				TileEntityRoad clickedRoad;
				if(clickedBlock instanceof BlockRoad){
					clickedRoad = world.getTileEntity(point);
				}else if(clickedBlock instanceof BlockRoadCollision){
					clickedRoad = ((BlockRoadCollision) clickedBlock).getRoadForBlock(world, point);
				}else{
					clickedRoad = null;
				}
				
				//If we clicked a road, get the lane number clicked.
				if(clickedRoad != null){
					lastRoadClickedData.put(player, clickedRoad.getClickData(point.copy().subtract(clickedRoad.position), player));
				}else{
					lastRoadClickedData.remove(player);
				}
				player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.set"));
			}else{
				if(point.distanceTo(lastPositionClicked.get(player)) < TileEntityRoad.MAX_SEGMENT_LENGTH){
					//Get the road we clicked, if we clicked one.
					//If we clicked a road for our starting point, then we need to auto-place the new road block.
					//If not, we place the new road block wherever we clicked.  Find this position now, as well as
					//the new curve starting point.  Also get the lane number and side clicked.
					final Point3i blockPlacementPoint;
					final Point3d startPosition;
					final double startRotation;
					final RoadClickData startingRoadData;
					
					ABlockBase clickedBlock = world.getBlock(point);
					if(clickedBlock instanceof BlockRoad){
						TileEntityRoad startingRoad = world.getTileEntity(point);
						startingRoadData = startingRoad.getClickData(point.copy().subtract(startingRoad.position), player);
					}else if(clickedBlock instanceof BlockRoadCollision){
						TileEntityRoad startingRoad = ((BlockRoadCollision) clickedBlock).getRoadForBlock(world, point);
						startingRoadData = startingRoad.getClickData(point.copy().subtract(startingRoad.position), player);
					}else{
						startingRoadData = null;
					}
					
					if(startingRoadData != null){
						if(startingRoadData.clickedStart){
							startPosition = new Point3d(startingRoadData.roadClicked.position).add(startingRoadData.roadClicked.startingOffset);
							startRotation = startingRoadData.clickedForward ? startingRoadData.roadClicked.curve.startAngle : startingRoadData.roadClicked.curve.startAngle + 180;
						}else{
							startPosition = new Point3d(startingRoadData.roadClicked.position).add(startingRoadData.roadClicked.startingOffset).add(startingRoadData.roadClicked.curve.endPos);
							startRotation = startingRoadData.clickedForward ? startingRoadData.roadClicked.curve.endAngle : startingRoadData.roadClicked.curve.endAngle + 180;
						}
						
						//Set the block position to be close to the start of the curve, but not on it.
						blockPlacementPoint = new Point3i(startPosition);
						boolean foundSpot = false;
						for(int i=-1; i<1 && !foundSpot; ++i){
							for(int j=-1; j<1 && !foundSpot; ++j){
								blockPlacementPoint.add(i, 0, j);
								ABlockBase testBlock = world.getBlock(blockPlacementPoint);
								if(testBlock == null || testBlock instanceof BlockRoadCollision){
									foundSpot = true;
								}else{
									blockPlacementPoint.add(-i, 0, -j);
								}
							}
						}
						
						if(!foundSpot){
							player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.conflict"));
							return true;
						} 
					}else{
						blockPlacementPoint = point.copy().add(0, 1, 0);
						startPosition = new Point3d(blockPlacementPoint);
						startRotation = player.getYaw();
					}
					
					
					//Get the end point and rotation.  This depends if we clicked a road or not.
					final Point3d endPosition;
					final double endRotation;
					final RoadClickData endingRoadData = lastRoadClickedData.get(player);
					if(endingRoadData != null){
						if(endingRoadData.clickedStart){
							endPosition = new Point3d(endingRoadData.roadClicked.position).add(endingRoadData.roadClicked.startingOffset);
							endRotation = endingRoadData.clickedForward ? endingRoadData.roadClicked.curve.startAngle : endingRoadData.roadClicked.curve.startAngle + 180;
						}else{
							endPosition = new Point3d(endingRoadData.roadClicked.position).add(endingRoadData.roadClicked.startingOffset).add(endingRoadData.roadClicked.curve.endPos);
							endRotation = endingRoadData.clickedForward ? endingRoadData.roadClicked.curve.endAngle : endingRoadData.roadClicked.curve.endAngle + 180;
						}
					}else{
						endPosition = new Point3d(lastPositionClicked.get(player));
						endRotation = lastRotationClicked.get(player);
					}
					
					
					//Now that we have the position for our block and curve points, create the new road segment.
					//This creation may require over-riding one of the collision blocks of the clicked road.
					//If so, we need to remove that collision box spot from the list of collision bits.
					ABlockBase oldBlock = world.getBlock(blockPlacementPoint);
					if(oldBlock instanceof BlockRoadCollision){
						TileEntityRoad road = ((BlockRoadCollision) oldBlock).getRoadForBlock(world, point);
						road.collisionBlockOffsets.remove(blockPlacementPoint.copy().subtract(road.position));
					}
					
					//New road block is ready to be set.  Do so now.
					if(world.setBlock(getBlock(), blockPlacementPoint, player, axis)){
						TileEntityRoad newRoad = world.getTileEntity(blockPlacementPoint);
						
						//Now that the road is placed, set the connections to the other roads and lanes.
						//First set the curve offset.
						newRoad.startingOffset.setTo(startPosition).add(-blockPlacementPoint.x, -blockPlacementPoint.y, blockPlacementPoint.z);
						
						//Next set the curve based on the starting and ending position deltas and rotations.
						newRoad.curve = new BezierCurve(endPosition.copy().subtract(startPosition), (float) startRotation, (float) endRotation);
						
						//Set the lane connections, as appropriate.
						//If we are butted-up to a segment, connect the connections in order.
						//If we are opposite to a segment, connect the connections in opposite pairs.
						//FIXME do connection logic here.
						if(startingRoadData != null){
							for(int laneNumber=0; laneNumber < newRoad.lanes.size(); ++laneNumber){
								if(startingRoadData.clickedForward){
									if(startingRoadData.laneClicked + laneNumber < startingRoadData.roadClicked.lanes.size()){
										int connectionLaneNumber = startingRoadData.laneClicked + laneNumber;
										RoadLane laneToConnect = startingRoadData.roadClicked.lanes.get(connectionLaneNumber);
										if(startingRoadData.clickedStart){
											//Clicked the start of the road, and in the same direction.
											//In this case, our road starts at the same position, and needs the same connections.
											newRoad.lanes.get(laneNumber).priorConnections.addAll(laneToConnect.priorConnections);
										}else{
											//Clicked the end of the road, and in the same direction.
											//In the case, our road starts at the end of the lane, so only add that connection.
											newRoad.lanes.get(laneNumber).connectToPrior(startingRoadData.roadClicked, connectionLaneNumber, false);
										}
									}
								}else{
									//FIXME connect in reverse here.
								}
							}
						}
						
						//Try to spawn all the collision blocks for this road.
						//FIXME  Need to spawn collision blocks based on the curve and road lane width.
						//player.sendPacket(new PacketPlayerChatMessage("interact.roadtool.blockingblocks"));
						
						
						lastRoadClickedData.put(player, newRoad.getClickData(blockPlacementPoint, player));
					}else{
						player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.blockedplacement"));
					}
				}else{
					player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.toofar"));
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public Class<? extends ABlockBase> getBlockClass(){
		return BlockRoad.class;
	}
}
