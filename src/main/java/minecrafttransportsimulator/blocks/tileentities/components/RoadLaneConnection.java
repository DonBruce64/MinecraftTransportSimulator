package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Helper class for containing connection data.
 * Contains location of connected road, as well
 * the lane connected to, and if we are connected
 * to the start of that lane or not.
 *
 * @author don_bruce
 */
public class RoadLaneConnection{
	public final Point3d tileLocation;
	public final int laneNumber;
	public final int curveNumber;
	public final float curveNetAngle;
	public final boolean connectedToStart;
	
	public RoadLaneConnection(RoadLane lane, BezierCurve curve, boolean connectedToStart){
		this.tileLocation = lane.road.position;
		this.laneNumber = lane.laneNumber;
		this.curveNumber = lane.curves.indexOf(curve);
		float angle = connectedToStart ? (curve.endAngle + 180) - curve.startAngle  : (curve.startAngle + 180) - curve.endAngle;
		while(angle > 180){angle -= 360;}
		while(angle < -180){angle += 360;}
		this.curveNetAngle = angle;
		this.connectedToStart = connectedToStart;
	}
	
	public RoadLaneConnection(Point3d tileLocation, int laneNumber, int curveNumber, float curveNetAngle, boolean connectedToStart){
		this.tileLocation = tileLocation;
		this.laneNumber = laneNumber;
		this.curveNumber = curveNumber;
		this.curveNetAngle = curveNetAngle;
		this.connectedToStart = connectedToStart;
	}
	
	public RoadLaneConnection(WrapperNBT data){
		this.tileLocation = data.getPoint3dCompact("tileLocation");
		this.laneNumber = data.getInteger("laneNumber");
		this.curveNumber = data.getInteger("curveNumber");
		this.curveNetAngle = (float) data.getDouble("curveNetAngle");
		this.connectedToStart = data.getBoolean("connectedToStart");
	}
	
	public void save(WrapperNBT data){
		data.setPoint3dCompact("tileLocation",tileLocation);
		data.setInteger("laneNumber", laneNumber);
		data.setInteger("curveNumber", curveNumber);
		data.setDouble("curveNetAngle", curveNetAngle);
		data.setBoolean("connectedToStart", connectedToStart);
	}
}