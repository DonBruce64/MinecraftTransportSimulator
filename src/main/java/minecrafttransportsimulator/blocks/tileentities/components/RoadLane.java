package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.MasterLoader;

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
	
	public RoadLane(TileEntityRoad road, IWrapperNBT data){
		this.road = road;
		this.laneNumber = data.getInteger("laneNumber");
		this.curve = new BezierCurve(road.curve, road.definition.general.laneOffsets[laneNumber]);
		int priorConnectionCount = data.getInteger("priorConnectionCount");
		for(int i=0; i<priorConnectionCount; ++i){
			IWrapperNBT connectionData = data.getData("priorConnection" + i);
			priorConnections.add(new RoadLaneConnection(connectionData));
		}
		int nextConnectionCount = data.getInteger("nextConnectionCount");
		for(int i=0; i<nextConnectionCount; ++i){
			IWrapperNBT connectionData = data.getData("nextConnection" + i);
			nextConnections.add(new RoadLaneConnection(connectionData));
		}
	}
	
	public void connectToPrior(TileEntityRoad road, int laneNumber, boolean connectedToStart){
		priorConnections.add(new RoadLaneConnection(road.position, laneNumber, connectedToStart));
	}
	
	public void connectToNext(TileEntityRoad road, int laneNumber, boolean connectedToStart){
		nextConnections.add(new RoadLaneConnection(road.position, laneNumber, connectedToStart));
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
			MasterLoader.coreInterface.logError("ERROR: Couldn't get TE to break road connection.  Was it changed?");
		}
	}
	
	public void save(IWrapperNBT data){
		data.setInteger("laneNumber", laneNumber);
		data.setInteger("priorConnectionCount", priorConnections.size());
		for(int i=0; i<priorConnections.size(); ++i){
			IWrapperNBT connectionData = MasterLoader.coreInterface.createNewTag();
			priorConnections.get(i).save(connectionData);
			data.setData("priorConnection" + i, connectionData);
		}
		data.setInteger("nextConnectionCount", nextConnections.size());
		for(int i=0; i<nextConnections.size(); ++i){
			IWrapperNBT connectionData = MasterLoader.coreInterface.createNewTag();
			nextConnections.get(i).save(connectionData);
			data.setData("nextConnection" + i, connectionData);
		}
	}
}