package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
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
	public final boolean lanesOccupied;
	public final boolean clickedStart;
	public final Point3d genPosition;
	public final float genRotation;

	public RoadClickData(TileEntityRoad roadClicked, JSONLaneSector sectorClicked, boolean clickedStart, boolean curveStart){
		this.roadClicked = roadClicked;
		this.sectorClicked = sectorClicked;
		this.clickedStart = clickedStart;
		
		//If we clicked a sector, line us up with it.  If we didn't click a sector, line us up with the dynamic-laned curve.
		if(roadClicked.definition.road.type.equals(RoadComponent.CORE_DYNAMIC)){
			//Get the position and rotation for this curve.  Note that curve rotation is flipped 180 on the end of curves.
			//So if we are calculating curve endpoints, the angles will be flipped 180 to account for this.
			if(clickedStart){
				//Clicked start of the road curve segment.
				//If this is for the start of the curve, we need to offset the position in the opposite direction to account for the different curve paths.
				//If this is for the end of a curve, just use the point as-is as the end point will be this curve's start point.
				//Rotation here needs to be the opposite of the start rotation of the clicked curve, as our curve is going the opposite direction.
				genRotation = roadClicked.dynamicCurve.startAngle + 180;
				if(curveStart){
					genPosition = new Point3d(-(roadClicked.definition.road.borderOffset), 0, 0).rotateY(genRotation).add(roadClicked.position).add(roadClicked.dynamicCurve.startPos);
				}else{
					genPosition = roadClicked.position.copy().add(roadClicked.dynamicCurve.startPos);
				}
			}else{
				//Clicked end of the road curve segment.
				//If this is for the start of the curve, just use the point as-is as the end point will be this curve's start point.
				//If this is for the end of a curve, we need to offset the position in the opposite direction to account for the different curve paths.
				//Rotation here needs to be the same as the end rotation of the clicked curve, as our curve is going the opposite direction.
				genRotation = roadClicked.dynamicCurve.endAngle + 180;
				if(curveStart){
					genPosition = roadClicked.position.copy().add(roadClicked.dynamicCurve.endPos);
				}else{
					genPosition = new Point3d((roadClicked.definition.road.borderOffset), 0, 0).rotateY(genRotation).add(roadClicked.position).add(roadClicked.dynamicCurve.endPos);
				}
			}
			lanesOccupied = areDynamicLanesOccupied();
		}else{
			//Get the first lane of the road sector, and use it for the rotation and positional data.
			//If this is for the start of the curve, we need to offset the position in the opposite direction to account for the different curve paths.
			//If this is for the end of a curve, just use the point as-is as the end point will be this curve's start point.
			//Rotation here needs to be the opposite of the start rotation of the starting sector segment, as our curve is going the opposite direction.
			genRotation = (float) (sectorClicked.sectorStartAngle + roadClicked.orientation.getAngles().y + 180);
			genPosition = roadClicked.orientation.rotatePoint(sectorClicked.sectorStartPos.copy().add(-0.5, 0, -0.5)).add(roadClicked.position);
			if(curveStart){
				genPosition.add(new Point3d(-(sectorClicked.borderOffset), 0, 0).rotateY(genRotation));
			}
			lanesOccupied = areStaticLanesOccupied();
		}
	}
	
	private boolean areDynamicLanesOccupied(){
		for(RoadLane lane : roadClicked.lanes){
			for(List<RoadLaneConnection> curveConnections : (clickedStart ? lane.priorConnections : lane.nextConnections)){
				if(!curveConnections.isEmpty()){
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean areStaticLanesOccupied(){
		for(RoadLane lane : roadClicked.lanes){
			if(lane.sectorNumber == roadClicked.definition.road.sectors.indexOf(sectorClicked)){
				for(List<RoadLaneConnection> curveConnections : lane.priorConnections){
					if(!curveConnections.isEmpty()){
						return true;
					}
				}
			}
		}
		return false;
	}
}