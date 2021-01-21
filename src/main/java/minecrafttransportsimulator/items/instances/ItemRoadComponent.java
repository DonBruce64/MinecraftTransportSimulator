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
import minecrafttransportsimulator.blocks.tileentities.components.RoadClickData;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.RoadGeneral;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;

public class ItemRoadComponent extends AItemSubTyped<JSONRoadComponent> implements IItemBlock{
	private final Map<WrapperPlayer, Point3i> lastPositionClicked = new HashMap<WrapperPlayer, Point3i>();
	private final Map<WrapperPlayer, Double> lastRotationClicked = new HashMap<WrapperPlayer, Double>();
	private final Map<WrapperPlayer, RoadClickData> lastRoadClickedData = new HashMap<WrapperPlayer, RoadClickData>();
	
	public ItemRoadComponent(JSONRoadComponent definition, String subName){
		super(definition, subName);
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		if(definition.general.type.equals("core")){
			for(byte i=1; i<=5; ++i){
				tooltipLines.add(InterfaceCore.translate("info.item.roadcomponent.line" + String.valueOf(i)));
			}
		}
	}
	
	@Override
	public boolean onBlockClicked(WrapperWorld world, WrapperPlayer player, Point3i point, Axis axis){
		//Only do logic on the server.
		if(!world.isClient()){
			//If we clicked an inactive road, don't do anything.
			//The road block will handle this operation.
			ABlockBase clickedBlock = world.getBlock(point);
			if(clickedBlock instanceof BlockRoad){
				TileEntityRoad clickedRoad = world.getTileEntity(point);
				if(!clickedRoad.isActive){
					return false;
				}
			}
			
			//It we are a dynamic road, create curves between the prior clicked point and the current point.
			//If the prior point is null, set it when we click it.
			//This could be either a block or a road itself.
			//If we click a road, we need to figure out what lane number we will connect to.
			//If we are a static road, just try to place us down as-is.
			if(definition.general.isDynamic){
				//If we don't have a click position or are sneaking, set the starting position.
				if(player.isSneaking() || !lastPositionClicked.containsKey(player)){
					lastRotationClicked.put(player, (double) Math.round(player.getYaw()/15)*15);
					TileEntityRoad clickedRoad;
					if(clickedBlock instanceof BlockRoad){
						clickedRoad = world.getTileEntity(point);
					}else if(clickedBlock instanceof BlockRoadCollision){
						clickedRoad = ((BlockRoadCollision) clickedBlock).getRoadForBlock(world, point);
					}else{
						clickedRoad = null;
						lastPositionClicked.put(player, point.copy().add(0, 1, 0));
					}
					
					//If we clicked a road, get the lane number clicked.
					if(clickedRoad != null){
						lastPositionClicked.put(player, point);
						lastRoadClickedData.put(player, clickedRoad.getClickData(point.copy().subtract(clickedRoad.position), false));
					}else{
						lastRoadClickedData.remove(player);
					}
					player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.set"));
				}else if(!player.isSneaking() && lastPositionClicked.containsKey(player)){
					//Clicked with the road not-sneaking with valid points.  Check end-points to make sure we aren't too long.
					if(point.distanceTo(lastPositionClicked.get(player)) < TileEntityRoad.MAX_SEGMENT_LENGTH){
						//Get the road we clicked, if we clicked one.
						//If we clicked a road for our starting point, then we need to auto-place the new road block.
						//If not, we place the new road block wherever we clicked.  Find this position now, as well as
						//the new curve starting point.  Also get the lane number and side clicked.
						final Point3i blockPlacementPoint;
						final Point3d startPosition;
						final double startRotation;
						final RoadClickData startingRoadData;
						
						if(clickedBlock instanceof BlockRoad){
							TileEntityRoad startingRoad = world.getTileEntity(point);
							startingRoadData = startingRoad.getClickData(point.copy().subtract(startingRoad.position), true);
						}else if(clickedBlock instanceof BlockRoadCollision){
							TileEntityRoad startingRoad = ((BlockRoadCollision) clickedBlock).getRoadForBlock(world, point);
							startingRoadData = startingRoad.getClickData(point.copy().subtract(startingRoad.position), true);
						}else{
							startingRoadData = null;
						}
						
						if(startingRoadData != null){
							startPosition = startingRoadData.genPosition;
							startRotation = startingRoadData.genRotation;
							
							//Set the block position to be close to the start of the curve, but not on it.
							blockPlacementPoint = new Point3i(startPosition);
							boolean foundSpot = false;
							for(int i=-1; i<1 && !foundSpot; ++i){
								for(int j=-1; j<1 && !foundSpot; ++j){
									blockPlacementPoint.add(i, 0, j);
									if(world.isAir(blockPlacementPoint)){
										foundSpot = true;
									}else{
										blockPlacementPoint.add(-i, 0, -j);
									}
								}
							}
							
							if(!foundSpot){
								player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.blockedplacement"));
								return true;
							}
						}else{
							blockPlacementPoint = point.copy().add(0, 1, 0);
							startRotation = Math.round(player.getYaw()/15)*15;
							//Need to offset startPosition to the corner of the block we clicked.
							startPosition = new Point3d(blockPlacementPoint).add(new Point3d(-0.5, 0.0, -0.5).rotateFine(new Point3d(0, startRotation, 0)));
						}
						
						
						//Get the end point and rotation.  This depends if we clicked a road or not.
						//If we clicked a road, we need to adjust our angle to match the road's angle.
						final Point3d endPosition;
						final double endRotation;
						final RoadClickData endingRoadData = lastRoadClickedData.get(player);
						if(endingRoadData != null){
							endPosition = endingRoadData.genPosition;
							endRotation = endingRoadData.genRotation;
						}else{
							endPosition = new Point3d(lastPositionClicked.get(player));
							endRotation = lastRotationClicked.get(player);
							//Need to offset endPosition to the corner of the block we clicked.
							//However, this needs to be on the back of the curve, so we need inverted rotation.
							endPosition.add(new Point3d(-0.5, 0.0, 0.5).rotateFine(new Point3d(0, endRotation + 180, 0)));
						}
						
						
						//Check the start and end roads, if we have them, and make sure they match our lane setup.
						if(startingRoadData != null){
							RoadGeneral roadGeneralDefinition = startingRoadData.roadClicked.definition.general;
							if((roadGeneralDefinition.isDynamic ? roadGeneralDefinition.laneOffsets.length : startingRoadData.sectorClicked.lanes.size()) != definition.general.laneOffsets.length){
								player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.lanemismatchstart"));
								return true;
							}
						}
						if(endingRoadData != null){
							RoadGeneral roadGeneralDefinition = endingRoadData.roadClicked.definition.general;
							if((roadGeneralDefinition.isDynamic ? roadGeneralDefinition.laneOffsets.length : endingRoadData.sectorClicked.lanes.size()) != definition.general.laneOffsets.length){
								player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.lanemismatchend"));
								return true;
							}
						}
						
						//FIXME check if the roads we want to connect to already have connections.
						
						//Check if the start and end position are the same.
						if(startPosition.equals(endPosition)){
							player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.sameblock"));
							return true;
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
							
							//Now that the road is placed, create the dynamic curve.
							//These can't get set in the constructor as it can't take the curve for dynamic roads.
							newRoad.dynamicCurve = new BezierCurve(startPosition.copy().subtract(blockPlacementPoint), endPosition.copy().subtract(blockPlacementPoint), (float) startRotation, (float) endRotation);
							
							//Try to spawn all the collision blocks for this road.
							//If we spawn blocks, we create all collision points and join the road's connections.
							if(newRoad.spawnCollisionBlocks(player)){
								//Set the lane connections, as appropriate.
								if(startingRoadData != null){
									for(RoadLane lane : newRoad.lanes){
										lane.connectToPrior(startingRoadData.roadClicked);
									}
									for(RoadLane lane : startingRoadData.roadClicked.lanes){
										lane.connectToPrior(newRoad);
										lane.connectToNext(newRoad);
									}
								}
								if(endingRoadData != null){
									for(RoadLane lane : newRoad.lanes){
										lane.connectToNext(endingRoadData.roadClicked);
									}
									for(RoadLane lane : endingRoadData.roadClicked.lanes){
										lane.connectToPrior(newRoad);
										lane.connectToNext(newRoad);
									}
								}
								//FIXME need a way to update lane connections client-side.  Need a packet.
								
								//Set new points.
								lastRoadClickedData.put(player, newRoad.getClickData(blockPlacementPoint, false));
								lastPositionClicked.put(player, newRoad.position);
								lastRotationClicked.put(player, startRotation + 180D);
							}
						}else{
							player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.blockedplacement"));
						}
					}else{
						player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.toofar"));
					}
					return true;
				}
			}else{
				//Get placement position for the segment.
				Point3i blockPlacementPoint = point.copy().add(0, 1, 0);
				
				//Now that we have the position for our road segment, create it.
				//Don't override any collision blocks here for other roads, as we shouldn't replace them with static roads.
				//For this, we let the TE collision method flag the road with blocking blocks so players know where they are.
				if(world.setBlock(getBlock(), blockPlacementPoint, player, axis)){
					TileEntityRoad newRoad = world.getTileEntity(blockPlacementPoint);
					
					//Try to spawn all the collision blocks for this road.
					if(newRoad.spawnCollisionBlocks(player)){
						lastRoadClickedData.put(player, newRoad.getClickData(blockPlacementPoint, false));
						lastPositionClicked.put(player, newRoad.position);
						lastRotationClicked.put(player, Math.round(player.getYaw()/15)*15 + 180D);
					}
				}else{
					player.sendPacket(new PacketPlayerChatMessage("interact.roadcomponent.blockedplacement"));
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
