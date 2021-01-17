package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent.JSONLaneSector;

/**Helper class for containing data of what was clicked on this road.
 * The position and rotation variables are used to create a new road from this data, 
 * should this be desired.  Simply pass them in as the start or end position of a {@link BezierCurve}.
 *
 * @author don_bruce
 */
public class RoadClickData{
	public final TileEntityRoad roadClicked;
	public final JSONLaneSector sectorClicked;
	public final boolean clickedStart;
	public final Point3d genPosition;
	public final float genRotation;

	public RoadClickData(TileEntityRoad roadClicked, JSONLaneSector sectorClicked, boolean clickedStart, boolean curveStart){
		this.roadClicked = roadClicked;
		this.sectorClicked = sectorClicked;
		this.clickedStart = clickedStart;
		
		//If we clicked a sector, line us up with it.  If we didn't click a sector, line us up with the dynamic-laned curve.
		if(roadClicked.definition.general.isDynamic){
			//Get the position and rotation for this curve.  Note that curve rotation is flipped 180 on the end of curves.
			//So if we are calculating curve endpoints, the angles will be flipped 180 to account for this.
			if(clickedStart){
				//Clicked start of the road curve segment.
				//If this is for the start of the curve, we need to offset the position in the opposite direction to account for the different curve paths.
				//If this is for the end of a curve, just use the point as-is as the end point will be this curve's start point.
				//Rotation here needs to be the opposite of the start rotation of the clicked curve, as our curve is going the opposite direction.
				genRotation = roadClicked.dynamicCurve.startAngle + 180;
				if(curveStart){
					genPosition = new Point3d(-(roadClicked.definition.general.borderOffset), 0, 0).rotateY(genRotation).add(roadClicked.position).add(roadClicked.dynamicCurve.startPos);
				}else{
					genPosition = new Point3d(roadClicked.position).add(roadClicked.dynamicCurve.startPos);
				}
			}else{
				//Clicked end of the road curve segment.
				//If this is for the start of the curve, just use the point as-is as the end point will be this curve's start point.
				//If this is for the end of a curve, we need to offset the position in the opposite direction to account for the different curve paths.
				//Rotation here needs to be the same as the end rotation of the clicked curve, as our curve is going the opposite direction.
				genRotation = roadClicked.dynamicCurve.endAngle + 180;
				if(curveStart){
					genPosition = new Point3d(roadClicked.position).add(roadClicked.dynamicCurve.endPos);
				}else{
					genPosition = new Point3d((roadClicked.definition.general.borderOffset), 0, 0).rotateY(genRotation).add(roadClicked.position).add(roadClicked.dynamicCurve.endPos);
				}
			}
		}else{
			//Get the first lane of the road sector, and use it for the rotation and positional data.
			//If this is for the start of the curve, we need to offset the position in the opposite direction to account for the different curve paths.
			//If this is for the end of a curve, just use the point as-is as the end point will be this curve's start point.
			//Rotation here needs to be the opposite of the start rotation of the starting sector segment, as our curve is going the opposite direction.
			genRotation = (float) (sectorClicked.sectorStartAngle + roadClicked.rotation + 180);
			if(curveStart){
				genPosition = new Point3d(-(sectorClicked.borderOffset), 0, 0).rotateY(genRotation).add(roadClicked.position).add(sectorClicked.sectorStartPos.copy().rotateY(roadClicked.rotation));
			}else{
				genPosition = new Point3d(roadClicked.position).add(sectorClicked.sectorStartPos.copy().rotateY(roadClicked.rotation));
			}
		}
	}
}