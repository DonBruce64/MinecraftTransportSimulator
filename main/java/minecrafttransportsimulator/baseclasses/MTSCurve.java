package minecrafttransportsimulator.baseclasses;

/**Curve class used for rails.
 * Needs two points and two angles.
 * Note that this uses Minecraft's coordinate system
 * where 0 degrees is up, not to the right.
 * 
 * @author don_bruce
 */
public class MTSCurve{
	private static final byte curveIncrement = 16;
	public final float startAngle;
	public final float endAngle;
	public final float pathLength;
	public final int[] blockStartPoint;
	public final int[] blockEndPoint;
	
	private final float cpDist;
	private final float[] startPoint;
	private final float[] endPoint;
	private final float[] cpStart;
	private final float[] cpEnd;
	private final float[][] pathPoints;
	
	public MTSCurve(int[] sp, int[] ep, float sa, float ea){
		this.startAngle = sa;
		this.endAngle = ea;
		this.blockStartPoint = sp;
		this.blockEndPoint = ep;
		this.startPoint = new float[]{sp[0] + 0.5F, sp[1], sp[2] + 0.5F};
		this.endPoint = new float[]{ep[0] + 0.5F, ep[1], ep[2] + 0.5F};
		cpDist = (float) Math.sqrt(Math.pow(endPoint[0] - startPoint[0], 2) + Math.pow(endPoint[1] - startPoint[1], 2) + Math.pow(endPoint[2] - startPoint[2], 2))/3F;
		cpStart = new float[]{(float) (startPoint[0] - Math.sin(Math.toRadians(startAngle))*cpDist), startPoint[1], (float) (startPoint[2] + Math.cos(Math.toRadians(startAngle))*cpDist)};
		cpEnd = new float[]{(float) (endPoint[0] - Math.sin(Math.toRadians(endAngle))*cpDist), endPoint[1], (float) (endPoint[2] + Math.cos(Math.toRadians(endAngle))*cpDist)};

		this.pathLength = getPathLength(startPoint, endPoint, cpStart, cpEnd, cpDist);
		float[] pathPointsX = getPathPoints(startPoint[0], endPoint[0], cpStart[0], cpEnd[0], pathLength);
		float[] pathPointsY = getPathPoints(startPoint[1], endPoint[1], cpStart[1], cpEnd[1], pathLength);
		float[] pathPointsZ = getPathPoints(startPoint[2], endPoint[2], cpStart[2], cpEnd[2], pathLength);
		
		pathPoints = new float[Math.round(pathLength*curveIncrement) + 1][3];
		for(int i=0; i<pathPoints.length; ++i){
			pathPoints[i][0] = pathPointsX[i];
			pathPoints[i][1] = pathPointsY[i];
			pathPoints[i][2] = pathPointsZ[i];
		}
	}
	
	public float[] getPointAt(float segment){
		return pathPoints[Math.round(segment*pathLength*curveIncrement)];
	}
	
	public float[] getNextPointFromVelocity(float segment, float velocity){
		if(Math.round((segment + velocity/pathLength)*pathLength*curveIncrement) > pathPoints.length){
			return null;
		}else{
			return pathPoints[Math.round((segment + velocity/pathLength)*pathLength*curveIncrement)];
		}
	}
	
	public float getYawAngleAt(float segment){
		int pointIndex = Math.round(segment*pathLength*curveIncrement);
		if(pointIndex + 1 == pathPoints.length){
			pointIndex = pathPoints.length - 2;
		}
		return (float) (360 + Math.toDegrees(Math.atan2(pathPoints[pointIndex][0] - pathPoints[pointIndex + 1][0], pathPoints[pointIndex + 1][2] - pathPoints[pointIndex][2])))%360;
	}
	
	public float getPitchAngleAt(float segment){
		int pointIndex = Math.round(segment*pathLength*curveIncrement);
		if(pointIndex + 1 == pathPoints.length){
			pointIndex = pathPoints.length - 2;
		}
		return (float) -Math.toDegrees(Math.atan((pathPoints[pointIndex + 1][1] - pathPoints[pointIndex][1])/Math.hypot(pathPoints[pointIndex + 1][0] - pathPoints[pointIndex][0], pathPoints[pointIndex + 1][2] - pathPoints[pointIndex][2])));
	}
	
	private static float getPathLength(float[] startPoint, float[] endPoint, float[] cpStart, float[] cpEnd, float cpDist){
		float dist1 = cpDist*2;
		float dist2 = (float) Math.sqrt(Math.pow(cpStart[0] - startPoint[0], 2) + Math.pow(cpStart[1] - startPoint[1], 2) + Math.pow(cpStart[2] - startPoint[2], 2));
		float dist3 = (float) Math.sqrt(Math.pow(cpEnd[0] - cpStart[0], 2) + Math.pow(cpEnd[1] - cpStart[1], 2) + Math.pow(cpEnd[2] - cpStart[2], 2))/2F;
		float dist4 = (float) Math.sqrt(Math.pow(endPoint[0] - cpEnd[0], 2) + Math.pow(endPoint[1] - cpEnd[1], 2) + Math.pow(endPoint[2] - cpEnd[2], 2))/2F;
		return (dist1 + dist2 + dist3 + dist4)/2;
	}
	
	private static float[] getPathPoints(float startPoint, float endPoint, float cpStart, float cpEnd, float pathLength){
		float[] points = new float[Math.round(pathLength*curveIncrement) + 1];
		if(startPoint == endPoint){
			for(int i=0; i<points.length; ++i){
				points[i] = startPoint;
			}
		}else{
			float t;
			for(int i=0; i<points.length; ++i){
				t = i/(points.length*1F);
				points[i] = (float) (Math.pow(1-t, 3)*startPoint + 3*Math.pow(1-t, 2)*t*cpStart + 3*(1-t)*Math.pow(t, 2)*cpEnd + Math.pow(t, 3)*endPoint);
			}
		}
		return points;
	}
}
