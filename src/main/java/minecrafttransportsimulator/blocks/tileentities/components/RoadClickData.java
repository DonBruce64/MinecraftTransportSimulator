package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;

/**Helper class for containing data of what was clicked on this road.
 * Note that laneClicked MAY be a negative number in order to allow
 * for offset road-linkings.  The position and rotation variables
 * are used to create a new road from this data, should this be desired.
 * Simply pass them in as the start or end position of a {@link BezierCurve}.
 *
 * @author don_bruce
 */
public class RoadClickData{
	public final TileEntityRoad roadClicked;
	public final RoadLane laneClicked;
	public final boolean clickedStart;
	public final boolean clickedSameDirection;
	public final Point3d genPosition;
	public final float genRotation;

	public RoadClickData(TileEntityRoad roadClicked, RoadLane laneClicked, boolean clickedStart, boolean clickedSameDirection){
		this.roadClicked = roadClicked;
		this.laneClicked = laneClicked;
		this.clickedStart = clickedStart;
		this.clickedSameDirection = clickedSameDirection;
		
		//FIXME need to match lanes here. This should also go in the clickedData block, as it's dependent on those states.
		if(clickedStart){
			if(clickedSameDirection){
				//Clicked start and in same direction.  Point and rotation for the curve is same as the existing curve.
				genPosition = new Point3d(roadClicked.position).add(roadClicked.curve.startPos);
				genRotation = roadClicked.curve.startAngle;
			}else{
				//Clicked start and in the opposite direction.  Adjust point to match lane 0 with clicked lane, and invert rotation.
				//First set position to where the curve would start.  Then offset it based on the lane we clicked.
				//FIXME do this.
				genPosition = new Point3d(roadClicked.position).add(roadClicked.curve.startPos);
				genRotation = roadClicked.curve.startAngle + 180;
			}
		}else{
			if(clickedSameDirection){
				//Clicked end and in the same direction.  Point is the end point of the curve, rotation is the same as the end rotation.
				genPosition = new Point3d(roadClicked.position).add(roadClicked.curve.endPos);
				genRotation = roadClicked.curve.endAngle;
			}else{
				//Clicked end and in the opposite direction.  Adjust point to match lane 0 with clicked lane, and invert rotation.
				//First set position to where the curve would start.  Then offset it based on the lane we clicked.
				//FIXME do this.
				genPosition = new Point3d(roadClicked.position).add(laneClicked.curve.endPos);
				genRotation = roadClicked.curve.endAngle + 180;
			}
		}
	}
}