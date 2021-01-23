package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONLaneSector;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONLaneSectorEndPoint;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONLaneSectorPointSet;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Helper class for containing lane data.  Lanes contain a reference to the road
 * they are a part of,  the lane number they represent, the curves that define
 * the path this lane can take, the prior segment this lane connects to,
 * and the next segment for each curve this lane connects to.
 *
 * @author don_bruce
 */
public class RoadLane{
	public final TileEntityRoad road;
	public final int sectorNumber;
	public final int laneNumber;
	public final List<BezierCurve> curves;
	public final List<List<RoadLaneConnection>> priorConnections;
	public final List<List<RoadLaneConnection>> nextConnections;
	
	private static final double CURVE_CONNECTION_MAX_DISTANCE = 0.20;
	
	public RoadLane(TileEntityRoad road, int sectorNumber, int laneNumber, WrapperNBT data){
		this.road = road;
		this.sectorNumber = sectorNumber;
		this.laneNumber = laneNumber;
		this.curves = new ArrayList<BezierCurve>();
		generateCurves();
		this.priorConnections = new ArrayList<List<RoadLaneConnection>>();
		this.nextConnections = new ArrayList<List<RoadLaneConnection>>();
		for(int i=0; i<curves.size(); ++ i){
			priorConnections.add(new ArrayList<RoadLaneConnection>());
			nextConnections.add(new ArrayList<RoadLaneConnection>());
		}
		if(data != null){
			for(int i=0; i<curves.size(); ++i){
				int numberPriorConnections = data.getInteger("numberPriorConnections" + i);
				int numberNextConnections = data.getInteger("numberNextConnections" + i);
				
				for(int j=0; j<numberPriorConnections; ++j){
					WrapperNBT connectionData = data.getData("priorConnection" + i + "_" + j);
					priorConnections.get(i).add(new RoadLaneConnection(connectionData));
				}
				for(int j=0; j<numberNextConnections; ++j){
					WrapperNBT connectionData = data.getData("nextConnection" + i + "_" + j);
					nextConnections.get(i).add(new RoadLaneConnection(connectionData));
				}
			}
		}
	}
	
	private Point3d generateCurves(){
		//Curves are generated based on the definition of the road, and the points for our lane.
		//If we are a dynamic road, then we don't use end points.  Instead, we use an
		//end offset point.  If we were made from a dynamic road, then the road's curve will be non-null;
		if(road.dynamicCurve != null){
			//Only one curve, as we are a dynamic lane.
			BezierCurve dynamicCurve = new BezierCurve(road.dynamicCurve, road.definition.general.laneOffsets[laneNumber]);
			curves.add(dynamicCurve);
			return dynamicCurve.startPos;
		}else{
			//Generate all curves for our lane as defined by the static mapping.
			JSONLaneSector sector = road.definition.general.sectors.get(sectorNumber);
			JSONLaneSectorPointSet points = sector.lanes.get(laneNumber);
			for(JSONLaneSectorEndPoint endPoint : points.endPoints){
				curves.add(new BezierCurve(points.startPoint.copy().rotateY(road.rotation), endPoint.pos.copy().rotateY(road.rotation), sector.sectorStartAngle + (float) road.rotation, endPoint.angle + (float) road.rotation));
			}
			return points.startPoint;
		}
	}
	
	public void connectToPrior(TileEntityRoad priorRoad){
		//Iterate over all lane-curves in the road and connect to those that we line up with.
		Point3d ownCurveStart = curves.get(0).startPos.copy().add(road.position);
		
		for(RoadLane priorRoadLane : priorRoad.lanes){
			for(BezierCurve priorRoadCurve : priorRoadLane.curves){
				Point3d priorCurveStart = priorRoadCurve.startPos.copy().add(priorRoad.position);
				Point3d priorCurveEnd = priorRoadCurve.endPos.copy().add(priorRoad.position);
				
				if(ownCurveStart.distanceTo(priorCurveStart) < CURVE_CONNECTION_MAX_DISTANCE){
					//Start to start connection, add to all curves.
					for(List<RoadLaneConnection> connections : priorConnections){
						connections.add(new RoadLaneConnection(priorRoadLane.road.position, priorRoadLane.laneNumber, priorRoadLane.curves.indexOf(priorRoadCurve), true));
					}
				}else if(ownCurveStart.distanceTo(priorCurveEnd) < CURVE_CONNECTION_MAX_DISTANCE){
					//Start to end connection.
					for(List<RoadLaneConnection> connections : priorConnections){
						connections.add(new RoadLaneConnection(priorRoadLane.road.position, priorRoadLane.laneNumber, priorRoadLane.curves.indexOf(priorRoadCurve), false));
					}
				}
			}
		}
	}
	
	public void connectToNext(TileEntityRoad nextRoad){
		//Iterate over all lane-curves in the road and connect to those that we line up with.
		for(BezierCurve ownCurve : curves){
			Point3d ownCurveEnd = ownCurve.endPos.copy().add(road.position);
			
			List<RoadLaneConnection> connectionsForCurve = nextConnections.get(curves.indexOf(ownCurve));
			for(RoadLane nextRoadLane : nextRoad.lanes){
				for(BezierCurve nextRoadCurve : nextRoadLane.curves){
					Point3d nextCurveStart = nextRoadCurve.startPos.copy().add(nextRoad.position);
					Point3d nextCurveEnd = nextRoadCurve.endPos.copy().add(nextRoad.position);
					
					if(ownCurveEnd.distanceTo(nextCurveStart) < CURVE_CONNECTION_MAX_DISTANCE){
						connectionsForCurve.add(new RoadLaneConnection(nextRoadLane.road.position, nextRoadLane.laneNumber, nextRoadLane.curves.indexOf(nextRoadCurve), true));
					}else if(ownCurveEnd.distanceTo(nextCurveEnd) < CURVE_CONNECTION_MAX_DISTANCE){
						connectionsForCurve.add(new RoadLaneConnection(nextRoadLane.road.position, nextRoadLane.laneNumber, nextRoadLane.curves.indexOf(nextRoadCurve), false));
					}
				}
			}
		}
	}
	
	public void removeConnections(){
		try{
			for(List<RoadLaneConnection> curvePriorConnections : priorConnections){
				for(RoadLaneConnection priorConnection : curvePriorConnections){
					TileEntityRoad otherRoad = road.world.getTileEntity(priorConnection.tileLocation);
					for(RoadLane otherLane : otherRoad.lanes){
						otherLane.disconnectFrom(road);
					}
				}
			}
			for(List<RoadLaneConnection> curveNextConnections : nextConnections){
				for(RoadLaneConnection nextConnection : curveNextConnections){
					TileEntityRoad otherRoad = road.world.getTileEntity(nextConnection.tileLocation);
					for(RoadLane otherLane : otherRoad.lanes){
						otherLane.disconnectFrom(road);
					}
				}
			}
		}catch(Exception e){
			InterfaceCore.logError("Couldn't get TE to break road connection.  Was it changed?");
		}
	}
	
	private void disconnectFrom(TileEntityRoad otherRoad){
		for(List<RoadLaneConnection> curvePriorConnections : priorConnections){
			Iterator<RoadLaneConnection> iterator = curvePriorConnections.iterator();
			while(iterator.hasNext()){
				RoadLaneConnection priorConnection = iterator.next();
				if(priorConnection.tileLocation.equals(otherRoad.position)){
					iterator.remove();
				}
			}
		}
		for(List<RoadLaneConnection> curveNextConnections : nextConnections){
			Iterator<RoadLaneConnection> iterator = curveNextConnections.iterator();
			while(iterator.hasNext()){
				RoadLaneConnection nextConnection = iterator.next();
				if(nextConnection.tileLocation.equals(otherRoad.position)){
					iterator.remove();
				}
			}
		}
	}
	
	/**
	 * Returns the next curve for the passed-in curve, or null if it doesn't exist.
	 * If the requested curve number is too high for the number of curves, then
	 * the highest-indexed curve is returned instead.
	 */
	public RoadLaneConnection getConnection(BezierCurve curve, int requestedNextCurve, boolean nextCurve){
		List<RoadLaneConnection> connections = nextCurve ? nextConnections.get(curves.indexOf(curve)) : priorConnections.get(curves.indexOf(curve));
		if(connections.size() > requestedNextCurve){
			return connections.isEmpty() ? null : connections.get(connections.size() - 1);
		}else{
			return connections.isEmpty() ? null : connections.get(requestedNextCurve);
		}
	}
	
	public void save(WrapperNBT data){
		data.setInteger("sectorNumber", sectorNumber);
		data.setInteger("laneNumber", laneNumber);
		for(int i=0; i<curves.size(); ++i){
			List<RoadLaneConnection> priorCurveConnections = priorConnections.get(i);
			List<RoadLaneConnection> nextCurveConnections = nextConnections.get(i);
			for(int j=0; j<priorCurveConnections.size(); ++j){
				WrapperNBT connectionData = new WrapperNBT();
				priorCurveConnections.get(j).save(connectionData);
				data.setData("priorConnection" + i + "_" + j, connectionData);
			}
			data.setInteger("numberPriorConnections" + i, priorCurveConnections.size());
			
			for(int j=0; j<nextCurveConnections.size(); ++j){
				WrapperNBT connectionData = new WrapperNBT();
				nextCurveConnections.get(j).save(connectionData);
				data.setData("nextConnection" + i + "_" + j, connectionData);
			}
			data.setInteger("numberNextConnections" + i, nextCurveConnections.size());
		}
	}
}