package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLaneConnection;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet sent to road lanes to change their connections.  This is sent from servers to all clients
 * when a connection state changes.  The data sent consists of the lane/curve that we need
 * to update the connection for, as well as connection data to update it with.  If this data
 * is null, then we just remove the specified connection.  Note that the road/lane we are connecting
 * to with this data MAY not be loaded.  This is due to chunk populations on servers/clients being different.
 * 
 * @author don_bruce
 */
public class PacketTileEntityRoadConnectionUpdate extends APacketEntity<TileEntityRoad>{
	private final int laneNumber;
	private final int curveNumber;
	private final boolean connectedToStart;
	
	private final Point3d otherPosition;
	private final int otherLaneNumber;
	private final int otherCurveNumber;
	private final float otherCurveNetAngle;
	private final boolean otherConnectedToStart;
	
	public PacketTileEntityRoadConnectionUpdate(RoadLane lane, int curveNumber, boolean connectedToStart, RoadLaneConnection otherConnection){
		super(lane.road);
		this.laneNumber = lane.laneNumber;
		this.curveNumber = curveNumber;
		this.connectedToStart = connectedToStart;
		if(otherConnection != null){
			this.otherPosition = otherConnection.tileLocation;
			this.otherLaneNumber = otherConnection.laneNumber;
			this.otherCurveNumber = otherConnection.curveNumber;
			this.otherCurveNetAngle = otherConnection.curveNetAngle;
			this.otherConnectedToStart = otherConnection.connectedToStart;
		}else{
			this.otherPosition = null;
			this.otherLaneNumber = 0;
			this.otherCurveNumber = 0;
			this.otherCurveNetAngle = 0;
			this.otherConnectedToStart = false;
		}
	}
	
	public PacketTileEntityRoadConnectionUpdate(RoadLane lane,int curveNumber, boolean priorConnection){
		this(lane, curveNumber, priorConnection, null);
	}
	
	public PacketTileEntityRoadConnectionUpdate(ByteBuf buf){
		super(buf);
		this.laneNumber = buf.readInt();
		this.curveNumber = buf.readInt();
		this.connectedToStart = buf.readBoolean();
		if(buf.readBoolean()){
			this.otherPosition = readPoint3dFromBuffer(buf);
			this.otherLaneNumber = buf.readInt();
			this.otherCurveNumber = buf.readInt();
			this.otherCurveNetAngle = buf.readFloat();
			this.otherConnectedToStart = buf.readBoolean();
		}else{
			this.otherPosition = null;
			this.otherLaneNumber = 0;
			this.otherCurveNumber = 0;
			this.otherCurveNetAngle = 0;
			this.otherConnectedToStart = false;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(laneNumber);
		buf.writeInt(curveNumber);
		buf.writeBoolean(connectedToStart);
		if(otherPosition != null){
			buf.writeBoolean(true);
			writePoint3dToBuffer(otherPosition, buf);
			buf.writeInt(otherLaneNumber);
			buf.writeInt(otherCurveNumber);
			buf.writeFloat(otherCurveNetAngle);
			buf.writeBoolean(otherConnectedToStart);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	protected boolean handle(WrapperWorld world, TileEntityRoad road){
		RoadLane lane = road.lanes.get(laneNumber);
		if(otherPosition != null){
			//Connecting to another curve.  Create connection from this curve to the other one.
			if(connectedToStart){
				lane.priorConnections.get(curveNumber).add(new RoadLaneConnection(otherPosition, otherLaneNumber, otherCurveNumber, otherCurveNetAngle, otherConnectedToStart));
			}else{
				lane.nextConnections.get(curveNumber).add(new RoadLaneConnection(otherPosition, otherLaneNumber, otherCurveNumber, otherCurveNetAngle, otherConnectedToStart));
			}
		}else{
			//No other curve.  This is a connection deletion request.
			if(connectedToStart){
				lane.priorConnections.get(curveNumber).clear();
			}else{
				lane.nextConnections.get(curveNumber).clear();
			}
		}
		return true;
	}
}
