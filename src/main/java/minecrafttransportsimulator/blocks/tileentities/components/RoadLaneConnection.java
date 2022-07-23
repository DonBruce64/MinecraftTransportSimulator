package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;

/**
 * Helper class for containing connection data.
 * Contains location of connected road, as well
 * the lane connected to, and if we are connected
 * to the start of that lane or not.
 *
 * @author don_bruce
 */
public class RoadLaneConnection {
    public final Point3D tileLocation;
    public final int laneNumber;
    public final int curveNumber;
    public final double curveNetAngle;
    public final boolean connectedToStart;

    public RoadLaneConnection(RoadLane lane, BezierCurve curve, boolean connectedToStart) {
        this.tileLocation = lane.road.position;
        this.laneNumber = lane.laneNumber;
        this.curveNumber = lane.curves.indexOf(curve);
        //Get the angle that the curve is trying to "turn" though its path.
        //This is either the start->end angle, or end->start if it's a reverse connection.
        Point3D vector = new Point3D(0, 0, 1);
        RotationMatrix helperRotator = new RotationMatrix();
        if (connectedToStart) {
            helperRotator.set(curve.endRotation).rotateY(180);
            vector.rotate(helperRotator).reOrigin(curve.startRotation);
        } else {
            helperRotator.set(curve.startRotation).rotateY(180);
            vector.rotate(helperRotator).reOrigin(curve.endRotation);
        }
        this.curveNetAngle = vector.getAngles(false).y;
        this.connectedToStart = connectedToStart;
    }

    public RoadLaneConnection(Point3D tileLocation, int laneNumber, int curveNumber, double curveNetAngle, boolean connectedToStart) {
        this.tileLocation = tileLocation;
        this.laneNumber = laneNumber;
        this.curveNumber = curveNumber;
        this.curveNetAngle = curveNetAngle;
        this.connectedToStart = connectedToStart;
    }

    public RoadLaneConnection(IWrapperNBT data) {
        this.tileLocation = data.getPoint3dCompact("tileLocation");
        this.laneNumber = data.getInteger("laneNumber");
        this.curveNumber = data.getInteger("curveNumber");
        this.curveNetAngle = (float) data.getDouble("curveNetAngle");
        this.connectedToStart = data.getBoolean("connectedToStart");
    }

    public void save(IWrapperNBT data) {
        data.setPoint3dCompact("tileLocation", tileLocation);
        data.setInteger("laneNumber", laneNumber);
        data.setInteger("curveNumber", curveNumber);
        data.setDouble("curveNetAngle", curveNetAngle);
        data.setBoolean("connectedToStart", connectedToStart);
    }
}