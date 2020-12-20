package minecrafttransportsimulator.baseclasses;

/**Curve class used for paths.
 * Start point is at 0,0,0.  Offset as needed in whatever class encloses this curve.
 * Needs an end point, a start angle, and an end angle to calculate the curve.
 * For all calls, segmentPoint should be a value between 0 and the pathLength of this curve.
 * Additionally, returned Point3d objects are not linked to the curve, so they may be modified without affecting
 * the curve's properties.  However, they are also re-used for future return calls, so they should not be saved
 * as reference in code calling the functions.  If the value needs to be saved, it should be copied to a different
 * Point3d instance.
 * 
 * @author don_bruce
 */
public class BezierCurve{
	public final Point3d endPos;
	public final float startAngle;
	public final float endAngle;
	public final float pathLength;
	
	//Internal curve parameter arrays.
	private final float[] startPoint;
	private final float[] endPoint;
	private final float[] startCurvePoint;
	private final float[] endCurvePoint;
	
	//Cached point data.
	private final float[][] cachedPathPoints;
	private final float[][] cachedPathRotations;
	
	/**Steps between curve calculations.  This is how many intermediate calculations we do between 1-block steps.**/
	private static final int CURVE_STEP = 16;
	
	public BezierCurve(Point3d endPos, float startAngle, float endAngle){
		this.endPos = endPos;
		this.startAngle = startAngle;
		this.endAngle = endAngle;
		this.startPoint = new float[]{0, 0, 0};
		this.endPoint = new float[]{(float)endPos.x, (float)endPos.y, (float)endPos.z};
		float midPointDistance = (float) Math.sqrt(Math.pow(endPoint[0] - startPoint[0], 2) + Math.pow(endPoint[1] - startPoint[1], 2) + Math.pow(endPoint[2] - startPoint[2], 2))/3F;
		this.startCurvePoint = new float[]{(float) (startPoint[0] + Math.sin(Math.toRadians(startAngle))*midPointDistance), startPoint[1], (float) (startPoint[2] + Math.cos(Math.toRadians(startAngle))*midPointDistance)};
		this.endCurvePoint = new float[]{(float) (endPoint[0] + Math.sin(Math.toRadians(endAngle))*midPointDistance), endPoint[1], (float) (endPoint[2] + Math.cos(Math.toRadians(endAngle))*midPointDistance)};

		this.pathLength = getPathLength(startPoint, endPoint, startCurvePoint, endCurvePoint);
		float[] pathPointsX = getCachedPathPoints(startPoint[0], endPoint[0], startCurvePoint[0], endCurvePoint[0], pathLength);
		float[] pathPointsY = getCachedPathPoints(startPoint[1], endPoint[1], startCurvePoint[1], endCurvePoint[1], pathLength);
		float[] pathPointsZ = getCachedPathPoints(startPoint[2], endPoint[2], startCurvePoint[2], endCurvePoint[2], pathLength);
		
		this.cachedPathPoints = new float[Math.round(pathLength*CURVE_STEP) + 1][3];
		this.cachedPathRotations = new float[Math.round(pathLength*CURVE_STEP) + 1][3];
		for(int i=0; i<cachedPathPoints.length; ++i){
			cachedPathPoints[i][0] = pathPointsX[i];
			cachedPathPoints[i][1] = pathPointsY[i];
			cachedPathPoints[i][2] = pathPointsZ[i];
			if(i > 0){
				cachedPathRotations[i][0] = (float) -Math.toDegrees(Math.atan((cachedPathPoints[i][1] - cachedPathPoints[i - 1][1])/Math.hypot(cachedPathPoints[i][0] - cachedPathPoints[i - 1][0], cachedPathPoints[i][2] - cachedPathPoints[i - 1][2])));
				cachedPathRotations[i][1] = (float) ((360 + Math.toDegrees(Math.atan2(cachedPathPoints[i][0] - cachedPathPoints[i - 1][0], cachedPathPoints[i][2] - cachedPathPoints[i - 1][2])))%360);
				cachedPathRotations[i][2] = 0;
			}
		}
		
		//Add point 0 rotations.  We couldn't do those in the loop.
		cachedPathRotations[0] = cachedPathRotations[1]; 
	}
	
	/**
	 * Returns the actual point at the passed-in segment location.
	 * This calculates the exact point position on the curve, so only use this if you
	 * need exact data.  If you can, use {@link #getPointAt(float)} for efficiency.
	 */
	public Point3d getActualPointAt(float segmentPoint){
		float segmentPercentage = segmentPoint/pathLength;
		return new Point3d(
			Math.pow(1-segmentPercentage, 3)*startPoint[0] + 3*Math.pow(1-segmentPercentage, 2)*segmentPercentage*startCurvePoint[0] + 3*(1-segmentPercentage)*Math.pow(segmentPercentage, 2)*endCurvePoint[0] + Math.pow(segmentPercentage, 3)*endPoint[0],
			Math.pow(1-segmentPercentage, 3)*startPoint[1] + 3*Math.pow(1-segmentPercentage, 2)*segmentPercentage*startCurvePoint[1] + 3*(1-segmentPercentage)*Math.pow(segmentPercentage, 2)*endCurvePoint[1] + Math.pow(segmentPercentage, 3)*endPoint[1],
			Math.pow(1-segmentPercentage, 3)*startPoint[2] + 3*Math.pow(1-segmentPercentage, 2)*segmentPercentage*startCurvePoint[2] + 3*(1-segmentPercentage)*Math.pow(segmentPercentage, 2)*endCurvePoint[2] + Math.pow(segmentPercentage, 3)*endPoint[2]
		);
	}
	
	/**
	 * Sets the passed-in Point3d to the cached value of the point at the passed-in segment location.
	 */
	public void setPointToPositionAt(Point3d point, float segmentPoint){
		float[] cachedPoint = cachedPathPoints[Math.round(segmentPoint*CURVE_STEP)];
		point.set(cachedPoint[0], cachedPoint[1], cachedPoint[2]);
	}
	
	/**
	 * Offsets the passed-in Point3d by the cached value of the point at the passed-in segment location.
	 */
	public void offsetPointbyPositionAt(Point3d point, float segmentPoint){
		float[] cachedPoint = cachedPathPoints[Math.round(segmentPoint*CURVE_STEP)];
		point.add(cachedPoint[0], cachedPoint[1], cachedPoint[2]);
	}
	
	/**
	 * Sets the passed-in Point3d to the cached value of rotation point at the passed-in segment location.
	 */
	public void setPointToRotationAt(Point3d rotation, float segmentPoint){
		float[] cachedRotation = cachedPathRotations[Math.round(segmentPoint*CURVE_STEP)];
		rotation.set(cachedRotation[0], cachedRotation[1], cachedRotation[2]);
	}
	
	/**
	 * Helper function to calculate the total path length for the passed-in parameters.
	 */
	private static float getPathLength(float[] startPoint, float[] endPoint, float[] startCurvePoint, float[] endCurvePoint){
		float dist1 = (float) Math.sqrt(Math.pow(endPoint[0] - startPoint[0], 2) + Math.pow(endPoint[1] - startPoint[1], 2) + Math.pow(endPoint[2] - startPoint[2], 2));
		float dist2 = (float) Math.sqrt(Math.pow(startCurvePoint[0] - startPoint[0], 2) + Math.pow(startCurvePoint[1] - startPoint[1], 2) + Math.pow(startCurvePoint[2] - startPoint[2], 2));
		float dist3 = (float) Math.sqrt(Math.pow(endCurvePoint[0] - startCurvePoint[0], 2) + Math.pow(endCurvePoint[1] - startCurvePoint[1], 2) + Math.pow(endCurvePoint[2] - startCurvePoint[2], 2));
		float dist4 = (float) Math.sqrt(Math.pow(endPoint[0] - endCurvePoint[0], 2) + Math.pow(endPoint[1] - endCurvePoint[1], 2) + Math.pow(endPoint[2] - endCurvePoint[2], 2));
		return (dist1 + dist2 + dist3 + dist4)/2;
	}
	
	/**
	 * Helper function to calculate the cached path points for the passed-in parameters.
	 */
	private static float[] getCachedPathPoints(float startPoint, float endPoint, float startCurvePoint, float endCurvePoint, float pathLength){
		float[] points = new float[Math.round(pathLength*CURVE_STEP) + 1];
		if(startPoint == endPoint){
			for(int i=0; i<points.length; ++i){
				points[i] = startPoint;
			}
		}else{
			float segmentPercentage;
			for(int i=0; i<points.length; ++i){
				segmentPercentage = i/(points.length*1F);
				points[i] = (float) (Math.pow(1-segmentPercentage, 3)*startPoint + 3*Math.pow(1-segmentPercentage, 2)*segmentPercentage*startCurvePoint + 3*(1-segmentPercentage)*Math.pow(segmentPercentage, 2)*endCurvePoint + Math.pow(segmentPercentage, 3)*endPoint);
			}
		}
		return points;
	}
}