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

	public RoadClickData(TileEntityRoad roadClicked, RoadLane laneClicked, boolean clickedStart, boolean clickedSameDirection, boolean curveStart, float laneOffset){
		this.roadClicked = roadClicked;
		this.laneClicked = laneClicked;
		this.clickedStart = clickedStart;
		this.clickedSameDirection = clickedSameDirection;
		
		//Get the position and rotation for this curve.  Note that curve rotation is flipped 180 on the end of curves.
		//So if we are calculating curve endpoints, the angles will be flipped 180 to account for this.
		if(clickedStart){
			if(clickedSameDirection){
				//Clicked start and in the same direction.
				//If this is for the start of the curve, just use the point as-is as the start point will be this curve's start point.
				//If this is for the end of a curve, we need to offset the position in the opposite direction to account for the different curve paths.
				//Rotation here needs to be the same as the start rotation of the clicked curve, as our curve is going the same direction.
				genRotation = roadClicked.curve.startAngle;
				if(curveStart){
					genPosition = new Point3d(roadClicked.position).add(roadClicked.curve.startPos);
				}else{
					genPosition = new Point3d(roadClicked.definition.general.laneOffsets[laneClicked.laneNumber] + laneOffset, 0, 0).rotateFine(new Point3d(0, genRotation, 0)).add(roadClicked.position).add(roadClicked.curve.startPos);
				}
			}else{
				//Clicked start and in the opposite direction.
				//If this is for the start of the curve, we need to offset the position in the opposite direction to account for the different curve paths.
				//If this is for the end of a curve, just use the point as-is as the end point will be this curve's start point.
				//Rotation here needs to be the opposite of the start rotation of the clicked curve, as our curve is going the opposite direction.
				genRotation = roadClicked.curve.startAngle + 180;
				if(curveStart){
					genPosition = new Point3d(-(roadClicked.definition.general.laneOffsets[laneClicked.laneNumber] + laneOffset), 0, 0).rotateFine(new Point3d(0, genRotation, 0)).add(roadClicked.position).add(roadClicked.curve.startPos);
				}else{
					genPosition = new Point3d(roadClicked.position).add(roadClicked.curve.startPos);
				}
			}
		}else{
			if(clickedSameDirection){
				//Clicked end and in the same direction.
				//If this is for the start of the curve, we just use the point as-is as the starting point will be this curve's end point..
				//If this is for the end of a curve, offset the position in the opposite direction to account for the different curve paths.
				//Rotation here needs to be the opposite of the end rotation of the clicked curve, as our curve is going the same direction.
				genRotation = roadClicked.curve.endAngle + 180;
				if(curveStart){
					genPosition = new Point3d(roadClicked.position).add(roadClicked.curve.endPos);
				}else{
					genPosition = new Point3d(roadClicked.definition.general.laneOffsets[laneClicked.laneNumber] + laneOffset, 0, 0).rotateFine(new Point3d(0, genRotation, 0)).add(roadClicked.position).add(roadClicked.curve.endPos);
				}
			}else{
				//Clicked end and in the opposite direction.
				//If this is for the start of the curve, we need to offset the position in the opposite direction to account for the different curve paths.
				//If this is for the end of a curve, just use the point as-is as the end point will be this curve's end point.
				//Rotation here needs to be the same as the end rotation of the clicked curve, as our curve is going the opposite direction.
				genRotation = roadClicked.curve.endAngle;
				if(curveStart){
					genPosition = new Point3d(-(roadClicked.definition.general.laneOffsets[laneClicked.laneNumber] + laneOffset), 0, 0).rotateFine(new Point3d(0, genRotation, 0)).add(roadClicked.position).add(roadClicked.curve.endPos);
				}else{
					genPosition = new Point3d(roadClicked.position).add(roadClicked.curve.endPos);
				}
			}
		}
	}
}