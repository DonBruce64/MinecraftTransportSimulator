package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockCollision;
import minecrafttransportsimulator.blocks.instances.BlockRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONLaneSector;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONLaneSectorEndPoint;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONLaneSectorPointSet;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketTileEntityRoadConnectionUpdate;

/**
 * Helper class for containing lane data.  Lanes contain a reference to the road
 * they are a part of,  the lane number they represent, the curves that define
 * the path this lane can take, the prior segment this lane connects to,
 * and the next segment for each curve this lane connects to.
 *
 * @author don_bruce
 */
public class RoadLane {
    public final TileEntityRoad road;
    public final int sectorNumber;
    public final int sectorLaneNumber;
    public final int laneNumber;
    public final List<BezierCurve> curves;
    public final List<List<RoadLaneConnection>> priorConnections;
    public final List<List<RoadLaneConnection>> nextConnections;

    private static final double CURVE_CONNECTION_MAX_DISTANCE = 0.20;

    public RoadLane(TileEntityRoad road, int sectorNumber, int laneNumber, int sectorLaneNumber, IWrapperNBT data) {
        this.road = road;
        this.sectorNumber = sectorNumber;
        this.sectorLaneNumber = sectorLaneNumber;
        this.laneNumber = laneNumber;
        this.curves = new ArrayList<>();
        generateCurves();
        this.priorConnections = new ArrayList<>();
        this.nextConnections = new ArrayList<>();
        for (int i = 0; i < curves.size(); ++i) {
            priorConnections.add(new ArrayList<>());
            nextConnections.add(new ArrayList<>());
        }
        if (data != null) {
            for (int i = 0; i < curves.size(); ++i) {
                int numberPriorConnections = data.getInteger("numberPriorConnections" + i);
                int numberNextConnections = data.getInteger("numberNextConnections" + i);

                for (int j = 0; j < numberPriorConnections; ++j) {
                    IWrapperNBT connectionData = data.getData("priorConnection" + i + "_" + j);
                    priorConnections.get(i).add(new RoadLaneConnection(connectionData));
                }
                for (int j = 0; j < numberNextConnections; ++j) {
                    IWrapperNBT connectionData = data.getData("nextConnection" + i + "_" + j);
                    nextConnections.get(i).add(new RoadLaneConnection(connectionData));
                }
            }
        }
    }

    private void generateCurves() {
        //Curves are generated based on the definition of the road, and the points for our lane.
        //If we are a dynamic road, then we don't use end points.  Instead, we use an
        //end offset point.  If we were made from a dynamic road, then the road's curve will be non-null;
        if (road.dynamicCurve != null) {
            //Only one curve, as we are a dynamic lane.
            BezierCurve dynamicCurve = road.dynamicCurve.generateOffsetCurve(new Point3D(road.definition.road.laneOffsets[laneNumber], road.definition.road.collisionHeight / 16D, 0));
            curves.add(dynamicCurve);
        } else {
            //Generate all curves for our lane as defined by the static mapping.)
            JSONLaneSector sector = road.definition.road.sectors.get(sectorNumber);
            JSONLaneSectorPointSet points = sector.lanes.get(sectorLaneNumber);
            for (JSONLaneSectorEndPoint endPoint : points.endPoints) {
                Point3D start = points.startPoint.copy().rotate(road.orientation).add(road.position);
                Point3D end = endPoint.pos.copy().rotate(road.orientation).add(road.position);
                RotationMatrix startRotation = new RotationMatrix().set(road.orientation).multiply(sector.sectorStartAngles);
                RotationMatrix endRotation = new RotationMatrix().set(road.orientation).multiply(endPoint.angles);
                curves.add(new BezierCurve(start, end, startRotation, endRotation));
            }
        }
    }

    /**
     * Attempts to set this lane's connection points.  This should be done on initial placement only.
     * If a connection is made, it is saved, and a packet is sent to establish it on clients.
     * For times when a road is broken, use {@link #removeConnections()} on the lanes of the road being broken,
     * not this method on the lane that was connected to the broken road.
     */
    public void generateConnections() {
        //Iterate over all lane-curves in the road and connect to those that we line up with.
        //Start with prior connections.  For these, we get the blocks surrounding the start
        //point, and check if they are part of a road.  If so, we get the curves on that road
        //and check if they connect to ourselves.  After this, we do the end points.
        for (BezierCurve curve : curves) {
            checkAndAddConnections(curve, true);
            checkAndAddConnections(curve, false);
        }
    }

    /**
     * Helper method for adding connections.
     */
    private void checkAndAddConnections(BezierCurve curve, boolean checkingStart) {
        int curveNumber = curves.indexOf(curve);
        for (int j = -1; j >= -2; --j) {
            boolean foundRoadsThisCheck = false;
            Point3D offsetPoint;
            if (checkingStart) {
                offsetPoint = new Point3D(0, 0, j).rotate(curve.startRotation).add(curve.startPos);
            } else {
                offsetPoint = new Point3D(0, 0, j).rotate(curve.endRotation).add(curve.endPos);
            }

            ABlockBase block = road.world.getBlock(offsetPoint);
            if (block instanceof BlockCollision || block instanceof BlockRoad) {
                TileEntityRoad otherRoad = block instanceof BlockRoad ? road.world.getTileEntity(offsetPoint) : ((BlockCollision) block).getMasterRoad(road.world, offsetPoint);
                if (!otherRoad.equals(road)) {
                    foundRoadsThisCheck = true;
                    for (RoadLane otherRoadLane : otherRoad.lanes) {
                        for (BezierCurve otherRoadCurve : otherRoadLane.curves) {
                            int otherCurveNumber = otherRoadLane.curves.indexOf(otherRoadCurve);

                            //For any connection we make here, we send the packet to the OTHER curve.
                            //The reason being that this curve/lane won't be generated on the client
                            //before the packet gets to it, so it won't execute.  By sending the packet
                            //to the other curve, we update it, and then let the normal NBT data sync
                            //transfer work for this lane/road's new generated connections.
                            //We also don't want to make start-start or end-end connections to static roads.
                            //This is due to how junctions are handled, where we don't want to
                            //end up going backwards on one of them.
                            boolean disableSameSideConnections = road.definition.road.type.equals(RoadComponent.CORE_STATIC) && otherRoad.definition.road.type.equals(RoadComponent.CORE_STATIC);
                            if (checkingStart) {
                                if (!disableSameSideConnections && curve.startPos.isDistanceToCloserThan(otherRoadCurve.startPos, CURVE_CONNECTION_MAX_DISTANCE)) {
                                    //Start to start connection.
                                    priorConnections.get(curveNumber).add(new RoadLaneConnection(otherRoadLane, otherRoadCurve, true));
                                    RoadLaneConnection ourConnection = new RoadLaneConnection(this, curve, true);
                                    otherRoadLane.priorConnections.get(otherCurveNumber).add(ourConnection);
                                    InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityRoadConnectionUpdate(otherRoadLane, otherCurveNumber, true, ourConnection));
                                } else if (curve.startPos.isDistanceToCloserThan(otherRoadCurve.endPos, CURVE_CONNECTION_MAX_DISTANCE)) {
                                    //Start to end connection.
                                    priorConnections.get(curveNumber).add(new RoadLaneConnection(otherRoadLane, otherRoadCurve, false));
                                    RoadLaneConnection ourConnection = new RoadLaneConnection(this, curve, true);
                                    otherRoadLane.nextConnections.get(otherCurveNumber).add(ourConnection);
                                    InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityRoadConnectionUpdate(otherRoadLane, otherCurveNumber, false, ourConnection));
                                }
                            } else {
                                if (curve.endPos.isDistanceToCloserThan(otherRoadCurve.startPos, CURVE_CONNECTION_MAX_DISTANCE)) {
                                    //End to start connection.
                                    nextConnections.get(curveNumber).add(new RoadLaneConnection(otherRoadLane, otherRoadCurve, true));
                                    RoadLaneConnection ourConnection = new RoadLaneConnection(this, curve, false);
                                    otherRoadLane.priorConnections.get(otherCurveNumber).add(ourConnection);
                                    InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityRoadConnectionUpdate(otherRoadLane, otherCurveNumber, true, ourConnection));
                                } else if (!disableSameSideConnections && curve.endPos.isDistanceToCloserThan(otherRoadCurve.endPos, CURVE_CONNECTION_MAX_DISTANCE)) {
                                    //End to end connection.
                                    nextConnections.get(curveNumber).add(new RoadLaneConnection(otherRoadLane, otherRoadCurve, false));
                                    RoadLaneConnection ourConnection = new RoadLaneConnection(this, curve, false);
                                    otherRoadLane.nextConnections.get(otherCurveNumber).add(ourConnection);
                                    InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityRoadConnectionUpdate(otherRoadLane, otherCurveNumber, false, ourConnection));
                                }
                            }
                        }
                    }
                }
            }

            if (foundRoadsThisCheck) {
                break;
            }
        }
    }

    /**
     * Removes all connections from this lane, and removes those connections from other
     * connected lanes/roads.  This should be done when this lane or road is removed from the world.
     * Call this ONLY from the server: clients will get update packets as appropriate.
     */
    public void removeConnections() {
        //Iterate over all our prior connections for all lanes.
        for (List<RoadLaneConnection> curvePriorConnections : priorConnections) {
            //Iterate over all our prior connections for all curves of this lane.
            for (RoadLaneConnection curvePriorConnection : curvePriorConnections) {
                try {
                    //Get the road and lane that the prior connection is connected to.
                    TileEntityRoad otherRoad = road.world.getTileEntity(curvePriorConnection.tileLocation);
                    RoadLane otherLane = otherRoad.lanes.get(curvePriorConnection.laneNumber);
                    //If the prior connection is connected to the start of the curve,
                    //remove the priorConnection in that curve for the curve we are checking.
                    //If it's connected to the end, remove the next connections.
                    if (curvePriorConnection.connectedToStart) {
                        otherLane.priorConnections.get(curvePriorConnection.curveNumber).clear();
                        InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityRoadConnectionUpdate(otherLane, curvePriorConnection.curveNumber, true, null));
                    } else {
                        otherLane.nextConnections.get(curvePriorConnection.curveNumber).clear();
                        InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityRoadConnectionUpdate(otherLane, curvePriorConnection.curveNumber, false, null));
                    }
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("Couldn't get TE at position " + curvePriorConnection.tileLocation + " to break prior road connection.  Was it changed?");
                }
            }
        }
        //Iterate over all our next connections for all lanes.
        for (List<RoadLaneConnection> curveNextConnections : nextConnections) {
            //Iterate over all our next connections for all curves of this lane.
            for (RoadLaneConnection curveNextConnection : curveNextConnections) {
                try {
                    //Get the road and lane that the prior connection is connected to.
                    TileEntityRoad otherRoad = road.world.getTileEntity(curveNextConnection.tileLocation);
                    RoadLane otherLane = otherRoad.lanes.get(curveNextConnection.laneNumber);
                    //If the next connection is connected to the start of the curve,
                    //remove the priorConnection in that curve for the curve we are checking.
                    //If it's connected to the end, remove the next connections.
                    if (curveNextConnection.connectedToStart) {
                        otherLane.priorConnections.get(curveNextConnection.curveNumber).clear();
                        InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityRoadConnectionUpdate(otherLane, curveNextConnection.curveNumber, true, null));
                    } else {
                        otherLane.nextConnections.get(curveNextConnection.curveNumber).clear();
                        InterfaceManager.packetInterface.sendToAllClients(new PacketTileEntityRoadConnectionUpdate(otherLane, curveNextConnection.curveNumber, false, null));
                    }
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("Couldn't get TE at position " + curveNextConnection.tileLocation + " to break next road connection.  Was it changed?");
                }
            }
        }
    }

    /**
     * Returns the next curve for the passed-in curve and request, or null if it doesn't exist.
     */
    public RoadLaneConnection getConnection(BezierCurve curve, LaneSelectionRequest requestedNextCurve, boolean nextCurve) {
        List<RoadLaneConnection> connections = nextCurve ? nextConnections.get(curves.indexOf(curve)) : priorConnections.get(curves.indexOf(curve));
        if (!connections.isEmpty()) {
            //Sort the connections by curve net angle.
            connections.sort((arg0, arg1) -> Double.compare(arg0.curveNetAngle, arg1.curveNetAngle));

            //Get the connection requested.
            switch (requestedNextCurve) {
                case LEFT:
                    return connections.get(connections.size() - 1);
                case RIGHT:
                    return connections.get(0);
                case NONE:
                    return connections.get(connections.size() / 2);
            }
        }
        return null;
    }

    public IWrapperNBT getData() {
        IWrapperNBT data = InterfaceManager.coreInterface.getNewNBTWrapper();
        data.setInteger("sectorNumber", sectorNumber);
        data.setInteger("laneNumber", laneNumber);
        for (int i = 0; i < curves.size(); ++i) {
            List<RoadLaneConnection> priorCurveConnections = priorConnections.get(i);
            List<RoadLaneConnection> nextCurveConnections = nextConnections.get(i);
            for (int j = 0; j < priorCurveConnections.size(); ++j) {
                IWrapperNBT connectionData = InterfaceManager.coreInterface.getNewNBTWrapper();
                priorCurveConnections.get(j).save(connectionData);
                data.setData("priorConnection" + i + "_" + j, connectionData);
            }
            data.setInteger("numberPriorConnections" + i, priorCurveConnections.size());

            for (int j = 0; j < nextCurveConnections.size(); ++j) {
                IWrapperNBT connectionData = InterfaceManager.coreInterface.getNewNBTWrapper();
                nextCurveConnections.get(j).save(connectionData);
                data.setData("nextConnection" + i + "_" + j, connectionData);
            }
            data.setInteger("numberNextConnections" + i, nextCurveConnections.size());
        }
        return data;
    }

    public enum LaneSelectionRequest {
        LEFT,
        RIGHT,
        NONE
    }
}