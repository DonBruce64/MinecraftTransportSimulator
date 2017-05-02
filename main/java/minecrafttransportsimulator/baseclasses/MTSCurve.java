package minecrafttransportsimulator.baseclasses;

import net.minecraft.util.math.BlockPos;

/**Curve class used for rails.
 * Needs two points and two angles.
 * Note that this uses Minecraft's coordinate system
 * where 0 degrees is up, not to the right.
 * 
 * @author don_bruce
 */
public class MTSCurve{
	public final float startAngle;
	public final float endAngle;
	public final float pathLength;
	public final BlockPos blockStartPos;
	public final BlockPos blockEndPos;
	
	private final float cpDist;
	private final float[] startPoint;
	private final float[] endPoint;
	private final float[] cpStart;
	private final float[] cpEnd;
	
	private static final byte cachedCurveIncrement = 16;
	private final float[][] cachedPathPoints;
	
	public MTSCurve(BlockPos sp, BlockPos ep, float sa, float ea){
		this.startAngle = sa;
		this.endAngle = ea;
		this.blockStartPos = sp;
		this.blockEndPos = ep;
		this.startPoint = new float[]{sp.getX() + 0.5F, sp.getY(), sp.getZ() + 0.5F};
		this.endPoint = new float[]{ep.getX() + 0.5F, ep.getY(), ep.getZ() + 0.5F};
		cpDist = (float) Math.sqrt(Math.pow(endPoint[0] - startPoint[0], 2) + Math.pow(endPoint[1] - startPoint[1], 2) + Math.pow(endPoint[2] - startPoint[2], 2))/3F;
		cpStart = new float[]{(float) (startPoint[0] - Math.sin(Math.toRadians(startAngle))*cpDist), startPoint[1], (float) (startPoint[2] + Math.cos(Math.toRadians(startAngle))*cpDist)};
		cpEnd = new float[]{(float) (endPoint[0] - Math.sin(Math.toRadians(endAngle))*cpDist), endPoint[1], (float) (endPoint[2] + Math.cos(Math.toRadians(endAngle))*cpDist)};

		this.pathLength = getPathLength(startPoint, endPoint, cpStart, cpEnd, cpDist);
		float[] pathPointsX = getAxisCachedPathPoints(startPoint[0], endPoint[0], cpStart[0], cpEnd[0], pathLength);
		float[] pathPointsY = getAxisCachedPathPoints(startPoint[1], endPoint[1], cpStart[1], cpEnd[1], pathLength);
		float[] pathPointsZ = getAxisCachedPathPoints(startPoint[2], endPoint[2], cpStart[2], cpEnd[2], pathLength);
		
		cachedPathPoints = new float[Math.round(pathLength*cachedCurveIncrement) + 1][3];
		for(int i=0; i<cachedPathPoints.length; ++i){
			cachedPathPoints[i][0] = pathPointsX[i];
			cachedPathPoints[i][1] = pathPointsY[i];
			cachedPathPoints[i][2] = pathPointsZ[i];
		}
	}
	
	public float[] getPointAt(float segment){
		return new float[]{
			(float) (Math.pow(1-segment, 3)*startPoint[0] + 3*Math.pow(1-segment, 2)*segment*cpStart[0] + 3*(1-segment)*Math.pow(segment, 2)*cpEnd[0] + Math.pow(segment, 3)*endPoint[0]),
			(float) (Math.pow(1-segment, 3)*startPoint[0] + 3*Math.pow(1-segment, 2)*segment*cpStart[0] + 3*(1-segment)*Math.pow(segment, 2)*cpEnd[0] + Math.pow(segment, 3)*endPoint[0]),
			(float) (Math.pow(1-segment, 3)*startPoint[0] + 3*Math.pow(1-segment, 2)*segment*cpStart[0] + 3*(1-segment)*Math.pow(segment, 2)*cpEnd[0] + Math.pow(segment, 3)*endPoint[0])
		};
	}
	
	public float[] getCachedPointAt(float segment){
		return cachedPathPoints[Math.round(segment*pathLength*cachedCurveIncrement)];
	}
	
	public float getCachedYawAngleAt(float segment){
		int pointIndex = Math.round(segment*pathLength*cachedCurveIncrement);
		if(pointIndex + 1 == cachedPathPoints.length){
			pointIndex = cachedPathPoints.length - 2;
		}
		return (float) (360 + Math.toDegrees(Math.atan2(cachedPathPoints[pointIndex][0] - cachedPathPoints[pointIndex + 1][0], cachedPathPoints[pointIndex + 1][2] - cachedPathPoints[pointIndex][2])))%360;
	}
	
	public float getCachedPitchAngleAt(float segment){
		int pointIndex = Math.round(segment*pathLength*cachedCurveIncrement);
		if(pointIndex + 1 == cachedPathPoints.length){
			pointIndex = cachedPathPoints.length - 2;
		}
		return (float) -Math.toDegrees(Math.atan((cachedPathPoints[pointIndex + 1][1] - cachedPathPoints[pointIndex][1])/Math.hypot(cachedPathPoints[pointIndex + 1][0] - cachedPathPoints[pointIndex][0], cachedPathPoints[pointIndex + 1][2] - cachedPathPoints[pointIndex][2])));
	}
	
	private static float getPathLength(float[] startPoint, float[] endPoint, float[] cpStart, float[] cpEnd, float cpDist){
		float dist1 = cpDist*2;
		float dist2 = (float) Math.sqrt(Math.pow(cpStart[0] - startPoint[0], 2) + Math.pow(cpStart[1] - startPoint[1], 2) + Math.pow(cpStart[2] - startPoint[2], 2));
		float dist3 = (float) Math.sqrt(Math.pow(cpEnd[0] - cpStart[0], 2) + Math.pow(cpEnd[1] - cpStart[1], 2) + Math.pow(cpEnd[2] - cpStart[2], 2))/2F;
		float dist4 = (float) Math.sqrt(Math.pow(endPoint[0] - cpEnd[0], 2) + Math.pow(endPoint[1] - cpEnd[1], 2) + Math.pow(endPoint[2] - cpEnd[2], 2))/2F;
		return (dist1 + dist2 + dist3 + dist4)/2;
	}
	
	private static float[] getAxisCachedPathPoints(float startPoint, float endPoint, float cpStart, float cpEnd, float pathLength){
		float[] points = new float[Math.round(pathLength*cachedCurveIncrement) + 1];
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
