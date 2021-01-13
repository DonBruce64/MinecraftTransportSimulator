package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONLanePointSet;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Helper class for containing lane data.  Lanes contain a reference to the road
 * they are a part of,  the lane number they represent, the curve that defines
 * their path, and the prior and next road segments they are connected to.
 *
 * @author don_bruce
 */
public class RoadLane{
	public final TileEntityRoad road;
	public final int laneNumber;
	public final BezierCurve curve;
	public RoadLaneConnection priorConnection;
	public RoadLaneConnection nextConnection;
	
	public RoadLane(TileEntityRoad road, int laneNumber, WrapperNBT data){
		this.road = road;
		this.laneNumber = laneNumber;
		this.curve = generateCurve();
		if(data != null){
			WrapperNBT connectionData = data.getData("priorConnection");
			if(connectionData != null){
				priorConnection = new RoadLaneConnection(connectionData);
			}
			connectionData = data.getData("nextConnection");
			if(connectionData != null){
				nextConnection = new RoadLaneConnection(connectionData);
			}
		}
	}
	
	private BezierCurve generateCurve(){
		//Curves are generated based on the definition of the road, and the points for our lane.
		//If we are a dynamic road, then we don't use end points.  Instead, we use an
		//end offset point.  If we were made from a dynamic road, then the road's curve will be non-null;
		if(road.dynamicCurve != null){
			return new BezierCurve(road.dynamicCurve, road.definition.general.laneOffsets[laneNumber]);
		}else{
			JSONLanePointSet points = road.definition.general.lanePoints.get(laneNumber);
			return new BezierCurve(points.startPos, points.endPos, points.startAngle, points.endAngle);
		}
	}
	
	public void connectToPrior(TileEntityRoad priorRoad, int priorLaneNumber, boolean connectedToStart){
		priorConnection = new RoadLaneConnection(priorRoad.position, priorLaneNumber, connectedToStart);
	}
	
	public void connectToNext(TileEntityRoad nextRoad, int nextLaneNumber, boolean connectedToStart){
		nextConnection = new RoadLaneConnection(nextRoad.position, nextLaneNumber, connectedToStart);
	}
	
	public void removeConnections(){
		try{
			if(priorConnection != null){
				TileEntityRoad otherRoad = road.world.getTileEntity(priorConnection.tileLocation);
				if(priorConnection.connectedToStart){
					otherRoad.lanes.get(laneNumber).priorConnection = null;
				}else{
					otherRoad.lanes.get(laneNumber).nextConnection = null;
				}
			}
			if(nextConnection != null){
				TileEntityRoad otherRoad = road.world.getTileEntity(nextConnection.tileLocation);
				if(nextConnection.connectedToStart){
					otherRoad.lanes.get(laneNumber).priorConnection = null;
				}else{
					otherRoad.lanes.get(laneNumber).nextConnection = null;
				}
			}
		}catch(Exception e){
			InterfaceCore.logError("ERROR: Couldn't get TE to break road connection.  Was it changed?");
		}
	}
	
	public void save(WrapperNBT data){
		data.setInteger("laneNumber", laneNumber);
		if(priorConnection != null){
			WrapperNBT connectionData = new WrapperNBT();
			priorConnection.save(connectionData);
			data.setData("priorConnection", connectionData);
		}
		if(nextConnection != null){
			WrapperNBT connectionData = new WrapperNBT();
			nextConnection.save(connectionData);
			data.setData("nextConnection", connectionData);
		}
	}
}