package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Helper class for containing lane data.
 *
 * @author don_bruce
 */
public class RoadLane{
	public final TileEntityRoad road;
	public final int laneNumber;
	public final BezierCurve curve;
	public final List<RoadLaneConnection> priorConnections = new ArrayList<RoadLaneConnection>();
	public final List<RoadLaneConnection> nextConnections = new ArrayList<RoadLaneConnection>();
	
	public RoadLane(TileEntityRoad road, int laneNumber){
		this.road = road;
		this.laneNumber = laneNumber;
		this.curve = new BezierCurve(road.curve, road.definition.general.laneOffsets[laneNumber]);
	}
	
	public RoadLane(TileEntityRoad road, WrapperNBT data){
		this.road = road;
		this.laneNumber = data.getInteger("laneNumber");
		this.curve = new BezierCurve(road.curve, road.definition.general.laneOffsets[laneNumber]);
		int priorConnectionCount = data.getInteger("priorConnectionCount");
		for(int i=0; i<priorConnectionCount; ++i){
			WrapperNBT connectionData = data.getData("priorConnection" + i);
			priorConnections.add(new RoadLaneConnection(connectionData));
		}
		int nextConnectionCount = data.getInteger("nextConnectionCount");
		for(int i=0; i<nextConnectionCount; ++i){
			WrapperNBT connectionData = data.getData("nextConnection" + i);
			nextConnections.add(new RoadLaneConnection(connectionData));
		}
	}
	
	public void connectToPrior(TileEntityRoad priorRoad, int priorLaneNumber, boolean connectedToStart){
		priorConnections.add(new RoadLaneConnection(priorRoad.position, priorLaneNumber, connectedToStart));
	}
	
	public void connectToNext(TileEntityRoad nextRoad, int nextLaneNumber, boolean connectedToStart){
		nextConnections.add(new RoadLaneConnection(nextRoad.position, nextLaneNumber, connectedToStart));
	}
	
	public void removeConnections(){
		try{
			for(RoadLaneConnection connection : priorConnections){
				TileEntityRoad otherRoad = road.world.getTileEntity(connection.tileLocation);
				for(RoadLane otherLane : otherRoad.lanes){
					Iterator<RoadLaneConnection> iterator = otherLane.priorConnections.iterator();
					while(iterator.hasNext()){
						if(iterator.next().tileLocation.equals(road.position)){
							iterator.remove();
						}
					}
					iterator = otherLane.nextConnections.iterator();
					while(iterator.hasNext()){
						if(iterator.next().tileLocation.equals(road.position)){
							iterator.remove();
						}
					}
				}
			}
			for(RoadLaneConnection connection : nextConnections){
				TileEntityRoad otherRoad = road.world.getTileEntity(connection.tileLocation);
				for(RoadLane otherLane : otherRoad.lanes){
					Iterator<RoadLaneConnection> iterator = otherLane.priorConnections.iterator();
					while(iterator.hasNext()){
						if(iterator.next().tileLocation.equals(road.position)){
							iterator.remove();
						}
					}
					iterator = otherLane.nextConnections.iterator();
					while(iterator.hasNext()){
						if(iterator.next().tileLocation.equals(road.position)){
							iterator.remove();
						}
					}
				}
			}
		}catch(Exception e){
			InterfaceCore.logError("ERROR: Couldn't get TE to break road connection.  Was it changed?");
		}
	}
	
	public void save(WrapperNBT data){
		data.setInteger("laneNumber", laneNumber);
		data.setInteger("priorConnectionCount", priorConnections.size());
		for(int i=0; i<priorConnections.size(); ++i){
			WrapperNBT connectionData = new WrapperNBT();
			priorConnections.get(i).save(connectionData);
			data.setData("priorConnection" + i, connectionData);
		}
		data.setInteger("nextConnectionCount", nextConnections.size());
		for(int i=0; i<nextConnections.size(); ++i){
			WrapperNBT connectionData = new WrapperNBT();
			nextConnections.get(i).save(connectionData);
			data.setData("nextConnection" + i, connectionData);
		}
	}
}