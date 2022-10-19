package minecrafttransportsimulator.items.instances;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.instances.BlockRoad;
import minecrafttransportsimulator.blocks.tileentities.components.RoadClickData;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONRoadGeneric;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;

public class ItemRoadComponent extends AItemSubTyped<JSONRoadComponent> implements IItemBlock {
    private final Map<IWrapperPlayer, Point3D> lastRoadGenPositionClicked = new HashMap<>();
    private final Map<IWrapperPlayer, RotationMatrix> lastRoadGenRotationClicked = new HashMap<>();
    private final Map<IWrapperPlayer, RoadClickData> lastRoadGenClickedData = new HashMap<>();

    public ItemRoadComponent(JSONRoadComponent definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public boolean onBlockClicked(AWrapperWorld world, IWrapperPlayer player, Point3D position, Axis axis) {
        //Only do logic on the server.
        if (!world.isClient()) {
            //If we clicked an inactive road, don't do anything.
            //The road block will handle this operation.
            ABlockBase clickedBlock = world.getBlock(position);
            if (clickedBlock instanceof BlockRoad) {
                TileEntityRoad clickedRoad = world.getTileEntity(position);
                if (!clickedRoad.isActive()) {
                    return false;
                }
            }

            //It we are a dynamic road, create curves between the prior clicked point and the current point.
            //If the prior point is null, set it when we click it.
            //This could be either a block or a road itself.
            //If we click a road, we need to figure out what lane number we will connect to.
            //If we are a static road, just try to place us down as-is.
            if (definition.road.type.equals(RoadComponent.CORE_DYNAMIC)) {
                //Get the road we clicked, if we clicked one.
                TileEntityRoad clickedRoad;
                if (clickedBlock instanceof BlockRoad) {
                    clickedRoad = world.getTileEntity(position);
                } else if (clickedBlock instanceof BlockCollision) {
                    clickedRoad = ((BlockCollision) clickedBlock).getMasterRoad(world, position);
                } else {
                    clickedRoad = null;
                }

                //If we don't have a click position or are sneaking, set the starting position.
                if (player.isSneaking() || !lastRoadGenPositionClicked.containsKey(player)) {
                    lastRoadGenPositionClicked.remove(player);
                    lastRoadGenRotationClicked.remove(player);
                    lastRoadGenClickedData.remove(player);

                    //If we clicked a road, get the data for it.  Otherwise, generate it.
                    if (clickedRoad != null) {
                        RoadClickData clickedRoadData = clickedRoad.getClickData(position, false);
                        JSONRoadGeneric roadDefinition = clickedRoadData.roadClicked.definition.road;
                        if ((roadDefinition.type.equals(RoadComponent.CORE_DYNAMIC) ? roadDefinition.laneOffsets.length : clickedRoadData.sectorClicked.lanes.size()) != definition.road.laneOffsets.length) {
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_ROAD_LANEMISMATCHFIRST));
                            return true;
                        } else if (clickedRoadData.lanesOccupied) {
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_ROAD_ALREADYCONNECTED));
                            return true;
                        }
                        lastRoadGenPositionClicked.put(player, clickedRoadData.genPosition);
                        lastRoadGenRotationClicked.put(player, clickedRoadData.genRotation);
                        lastRoadGenClickedData.put(player, clickedRoadData);
                    } else {
                        RotationMatrix genRotation = new RotationMatrix().rotateY(Math.round(player.getYaw() / 15) * 15);
                        //Start with rotated corner offset to local coords.
                        Point3D genPosition = definition.road.cornerOffset.copy().invert();
                        genPosition.z -= definition.road.segmentLength;
                        genPosition.rotate(genRotation);

                        //Offset position by 0.5 to be in the center of the clicked block, and up 1 to the block above.
                        genPosition.add(position).add(0.5, 1, 0.5);
                        lastRoadGenPositionClicked.put(player, genPosition);
                        lastRoadGenRotationClicked.put(player, genRotation);
                    }
                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_ROAD_SET));
                } else if (!player.isSneaking() && lastRoadGenPositionClicked.containsKey(player)) {
                    if (position.isDistanceToCloserThan(lastRoadGenPositionClicked.get(player), ConfigSystem.settings.general.roadMaxLength.value)) {
                        //If we clicked a road for our starting point, then we need to auto-place the new road block.
                        //If not, we place the new road block wherever we clicked.  Find this position now, as well as
                        //the new curve starting point and lane/side clicked on the road.
                        final Point3D startPosition;
                        final RotationMatrix startRotation;
                        final Point3D blockPlacementPoint;
                        if (clickedRoad != null) {
                            //Check the road we clicked, if it exists, and make sure we aren't doing a bad connection.
                            final RoadClickData startingRoadData = clickedRoad.getClickData(position, true);
                            JSONRoadGeneric roadDefinition = startingRoadData.roadClicked.definition.road;
                            if ((roadDefinition.type.equals(RoadComponent.CORE_DYNAMIC) ? roadDefinition.laneOffsets.length : startingRoadData.sectorClicked.lanes.size()) != definition.road.laneOffsets.length) {
                                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_ROAD_LANEMISMATCHSECOND));
                                return true;
                            } else if (startingRoadData.lanesOccupied) {
                                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_ROAD_ALREADYCONNECTED));
                                return true;
                            }

                            //Valid data.  Generate start and end positions.
                            startPosition = startingRoadData.genPosition;
                            startRotation = startingRoadData.genRotation;

                            //Set the block position to be close to the start of the curve, but not on it.
                            blockPlacementPoint = startPosition.copy();
                            blockPlacementPoint.x = Math.floor(blockPlacementPoint.x);
                            blockPlacementPoint.z = Math.floor(blockPlacementPoint.z);
                            boolean foundSpot = false;
                            for (int i = -1; i < 1 && !foundSpot; ++i) {
                                for (int j = -1; j < 1 && !foundSpot; ++j) {
                                    blockPlacementPoint.add(i, 0, j);
                                    if (world.isAir(blockPlacementPoint)) {
                                        foundSpot = true;
                                    } else {
                                        blockPlacementPoint.add(-i, 0, -j);
                                    }
                                }
                            }

                            if (!foundSpot) {
                                player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_ROAD_BLOCKED));
                                return true;
                            }
                        } else {
                            blockPlacementPoint = position.copy().add(0, 1, 0);
                            startRotation = new RotationMatrix().rotateY(Math.round(player.getYaw() / 15) * 15);
                            startPosition = definition.road.cornerOffset.copy().rotate(startRotation).add(0.5, 0, 0.5).add(blockPlacementPoint);
                        }

                        //Get the end point and rotation.  This depends if we clicked a road or not.
                        //If we clicked a road, we need to adjust our angle to match the road's angle.
                        final Point3D endPosition;
                        final RotationMatrix endRotation;
                        if (lastRoadGenClickedData.containsKey(player)) {
                            RoadClickData endingRoadData = lastRoadGenClickedData.get(player);
                            endPosition = endingRoadData.genPosition;
                            endRotation = endingRoadData.genRotation;
                        } else {
                            endPosition = lastRoadGenPositionClicked.get(player);
                            endRotation = lastRoadGenRotationClicked.get(player);
                        }

                        //Check if the start and end position are the same.
                        if (startPosition.equals(lastRoadGenPositionClicked.get(player))) {
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_ROAD_SAME));
                            return true;
                        }

                        //Now that we have the position for our block and curve points, create the new road segment.
                        //This creation may require over-riding one of the collision blocks of the clicked road.
                        //If so, we need to remove that collision box spot from the list of collision bits.
                        ABlockBase oldBlock = world.getBlock(blockPlacementPoint);
                        if (oldBlock instanceof BlockCollision) {
                            TileEntityRoad road = ((BlockCollision) oldBlock).getMasterRoad(world, position);
                            road.collisionBlockOffsets.remove(blockPlacementPoint.copy().subtract(road.position));
                        }

                        //New road block is ready to be set.  Do so now.
                        if (world.setBlock(getBlock(), blockPlacementPoint, player, axis)) {
                            TileEntityRoad newRoad = world.getTileEntity(blockPlacementPoint);

                            //Now that the road is placed, create the dynamic curve.
                            //These can't get set in the constructor as it can't take the curve for dynamic roads.
                            newRoad.dynamicCurve = new BezierCurve(startPosition, endPosition, startRotation, endRotation);

                            //Try to spawn all the collision blocks for this road.
                            //If we spawn blocks, we create all collision points and join the road's connections.
                            if (newRoad.spawnCollisionBlocks(player)) {
                                for (RoadLane lane : newRoad.lanes) {
                                    lane.generateConnections();
                                }

                                //Set new points.
                                RoadClickData clickData = newRoad.getClickData(newRoad.position, false);
                                lastRoadGenClickedData.put(player, clickData);
                                lastRoadGenPositionClicked.put(player, clickData.genPosition);
                                lastRoadGenRotationClicked.put(player, clickData.genRotation);
                            }
                        } else {
                            player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_ROAD_BLOCKED));
                        }
                    } else {
                        player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_ROAD_TOOFAR));
                    }
                    return true;
                }
            } else if (definition.road.type.equals(RoadComponent.CORE_STATIC)) {
                //Get placement position for the segment.
                Point3D blockPlacementPoint = position.copy().add(0, 1, 0);

                //Now that we have the position for our road segment, create it.
                //Don't override any collision blocks here for other roads, as we shouldn't replace them with static roads.
                //For this, we let the TE collision method flag the road with blocking blocks so players know where they are.
                if (world.setBlock(getBlock(), blockPlacementPoint, player, axis)) {
                    TileEntityRoad newRoad = world.getTileEntity(blockPlacementPoint);

                    //Try to spawn all the collision blocks for this road.
                    if (newRoad.spawnCollisionBlocks(player)) {
                        for (RoadLane lane : newRoad.lanes) {
                            lane.generateConnections();
                        }
                    }
                } else {
                    player.sendPacket(new PacketPlayerChatMessage(player, JSONConfigLanguage.INTERACT_ROAD_BLOCKED));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Class<? extends ABlockBase> getBlockClass() {
        return BlockRoad.class;
    }
}
