package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.baseclasses.BezierCurve;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane.LaneSelectionRequest;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;

/**Helper class for containing the following state of a road.
 * Contains the road, lane, and curve tracked, as well
 * as if the follower is oriented forwards or backwards
 * on the curve.  Also contains methods for updating the
 * curve state.
 *
 * @author don_bruce
 */
public class RoadFollowingState{
	public final RoadLane lane;
	public final BezierCurve curve;
	public final boolean goingForwards;
	public float currentSegment;
	
	
	public RoadFollowingState(RoadLane lane, BezierCurve curve, boolean goingForwards, float currentSegment){
		this.lane = lane;
		this.curve = curve;
		this.goingForwards = goingForwards;
		this.currentSegment = currentSegment;
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj instanceof RoadFollowingState){
			RoadFollowingState other = (RoadFollowingState) obj;
			return this.lane.equals(other.lane) && this.curve.equals(other.curve);
		}else{
			return false;
		}
	}
	
	/**
	 * Updates this curve to the proper point, returning itself.
	 * If the point delta requested has gone past the curve's bounds, then the next
	 * follower is returned instead.  If no follower is present because this is the
	 * end of the curve, null is returned.
	 */
	public RoadFollowingState updateCurvePoints(float segmentDelta, LaneSelectionRequest requestedNextCurve){
		//Check that our TE is still active.  It might have been destroyed.
		if(lane.road.isActive()){
			currentSegment += goingForwards ? segmentDelta : -segmentDelta;
			
			if(currentSegment < 0){
				//Get segment from prior curve.
				RoadLaneConnection priorConnection = lane.getConnection(curve, requestedNextCurve, false);
				if(priorConnection != null){
					TileEntityRoad priorRoad = lane.road.world.getTileEntity(priorConnection.tileLocation);
					if(priorRoad != null){
						RoadLane priorLane = priorRoad.lanes.get(priorConnection.laneNumber);
						BezierCurve priorCurve = priorLane.curves.get(priorConnection.curveNumber);
						boolean newForwardsState = priorConnection.connectedToStart ? !goingForwards : goingForwards;
						float segmentRemainder = priorConnection.connectedToStart ? -currentSegment : currentSegment;
						if(!newForwardsState){
							segmentRemainder = -segmentRemainder;
						}
						//Do recursion here in case we went two segments at once.
						return new RoadFollowingState(priorLane, priorCurve, newForwardsState, priorConnection.connectedToStart ? 0 : priorCurve.pathLength).updateCurvePoints(segmentRemainder, requestedNextCurve);
					}
				}
				return null;
			}else if(currentSegment > curve.pathLength){
				//Get segment from next curve.
				RoadLaneConnection nextConnection = lane.getConnection(curve, requestedNextCurve, true);
				if(nextConnection != null){
					TileEntityRoad nextRoad = lane.road.world.getTileEntity(nextConnection.tileLocation);
					if(nextRoad != null){
						RoadLane nextLane = nextRoad.lanes.get(nextConnection.laneNumber);
						BezierCurve nextCurve = nextLane.curves.get(nextConnection.curveNumber);
						boolean newForwardsState = nextConnection.connectedToStart ? goingForwards : !goingForwards;
						float segmentRemainder = nextConnection.connectedToStart ? currentSegment - curve.pathLength : -(currentSegment - curve.pathLength);
						if(!newForwardsState){
							segmentRemainder = -segmentRemainder;
						}
						//Do recursion here in case we went two segments at once.
						return new RoadFollowingState(nextLane, nextCurve, newForwardsState, nextConnection.connectedToStart ? 0 : nextCurve.pathLength).updateCurvePoints(segmentRemainder, requestedNextCurve);
					}
				}
				return null;
			}else{
				return this;
			}
		}else{
			return null;
		}
	}
	
	/**
	 * Returns the current point on this curve we set to in the world.
	 * This should be called AFTER {@link #updateCurvePoints(float, int)},
	 * otherwise you may get out of the curve's bounds.
	 */
	public Point3d getCurrentPoint(){
		Point3d currentPoint = lane.road.position.copy().add(0.5, lane.road.definition.road.collisionHeight/16F, 0.5);
		curve.offsetPointByPositionAt(currentPoint, currentSegment);
		return currentPoint;
	}
}