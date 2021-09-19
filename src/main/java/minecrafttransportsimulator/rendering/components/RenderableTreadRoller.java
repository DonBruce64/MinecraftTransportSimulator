package minecrafttransportsimulator.rendering.components;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;

/**A specific class of {@link RenderableModelObject}, designed
 * for tread rollers.  Contains an extra method for calculating things.
 * Also auto-creates rotatableModelObject definitions in the relevant JSON.
 *
 * @author don_bruce
 */
public class RenderableTreadRoller<AnimationEntity extends AEntityC_Definable<?>> extends RenderableModelObject<AnimationEntity>{
	public final boolean isLeft;
	public final int rollerNumber;
	public final Point3d centerPoint;
	public final double radius;
	public final double circumference;
	
	
	public double startY;
	public double startZ;
	public double startAngle;
	public double endY;
	public double endZ;
	public double endAngle;
	
	public RenderableTreadRoller(String modelLocation, String objectName, List<RenderableModelObject<AnimationEntity>> allObjects, float[][] vertices){
		super(modelLocation, objectName, allObjects, vertices);
		this.isLeft = objectName.toLowerCase().startsWith("l");
		this.rollerNumber = Integer.valueOf(objectName.substring(objectName.lastIndexOf('_') + 1));
		
		//Calculate the center and radius from the model.
		double minY = 999;
		double maxY = -999;
		double minZ = 999;
		double maxZ = -999;
		for(float[] point : vertices){
			minY = Math.min(minY, point[1]);
			maxY = Math.max(maxY, point[1]);
			minZ = Math.min(minZ, point[2]);
			maxZ = Math.max(maxZ, point[2]);
		}
		this.centerPoint = new Point3d(0, minY + (maxY - minY)/2D, minZ + (maxZ - minZ)/2D);
		this.radius = (maxZ - minZ)/2D;
		this.circumference = 2*Math.PI*radius;
	}
	
	/**
	 * Calculates the end point of this roller and the
	 * start point of the passed-in roller using trigonometry.
	 * We can assume that we'll always be on the outside point of any roller.
	 * Additionally, we know we'll start on the bottom of a roller, so between
	 * those two things we can tell which tangent we should follow.
	 */
	public void calculateEndpoints(RenderableTreadRoller<AnimationEntity> nextRoller){
		//What calculations we do depend on if the rollers are the same size.
		//If so, we can do simple calcs.  If not, we get to do trig.
		if(radius == nextRoller.radius){
			//First, get the angle from the vector from this roller to the next roller.
			//From this, we can calculate the end angle for this roller as perpendicular to
			//the vector.  We rotate 90 degrees as we know the roller orientation will be
			//counter-clockwise, and thus we always want the tread to be on that side.
			endAngle = Math.toDegrees(Math.atan2(nextRoller.centerPoint.z - centerPoint.z, nextRoller.centerPoint.y - centerPoint.y)) - 90D;
			nextRoller.startAngle = endAngle;
			
			//Now that we know the start and end angles, we can calculate the start and end points.
			//Simple polar to rectangular coord conversion here.
			endY = centerPoint.y + radius*Math.cos(Math.toRadians(endAngle));
			endZ = centerPoint.z + radius*Math.sin(Math.toRadians(endAngle));
			nextRoller.startY = nextRoller.centerPoint.y + nextRoller.radius*Math.cos(Math.toRadians(endAngle));
			nextRoller.startZ = nextRoller.centerPoint.z + nextRoller.radius*Math.sin(Math.toRadians(endAngle));
		}else{
			//First, get the distance between the roller centers.
			double centerDistance = Math.hypot(nextRoller.centerPoint.z - centerPoint.z, nextRoller.centerPoint.y - centerPoint.y);
			
			//The next parts depend which roller is bigger.  From here on out, the
			//smaller roller is r1, and the larger is r2.
			boolean nextRollerLarger = radius < nextRoller.radius;
			double r1CenterY = nextRollerLarger ? centerPoint.y : nextRoller.centerPoint.y;
			double r1CenterZ = nextRollerLarger ? centerPoint.z : nextRoller.centerPoint.z;
			double r2CenterY = !nextRollerLarger ? centerPoint.y : nextRoller.centerPoint.y;
			double r2CenterZ = !nextRollerLarger ? centerPoint.z : nextRoller.centerPoint.z;
			double r1Radius = nextRollerLarger ? radius : nextRoller.radius;
			double r2Radius = !nextRollerLarger ? radius : nextRoller.radius;
			
			//Get the angle of the vector between the two centers.
			double centerVectorAngle = Math.atan2(r2CenterZ - r1CenterZ, r2CenterY - r1CenterY);
			
			//If were were to draw a circle with a radius equal to r3 = r2 - r1, then
			//if we were to use a point on that circle as the center of r2, then we could
			//make the assumption that r1 and r3 are of equal diameter and our easy method
			//above would work.  To do this, we inscribe a circle of radius r3 with the center
			//point of r2, and then get the angle between r1, r2, and r3t, where r3t is the point
			//of the tangent line from r1 to r3t. This angle ie easy to calculate as we already
			//know what two of the lengths of the triangle are: the distance between the
			//two center points, and the radius of r3.
			double inscribedVectorAngle = Math.asin((r2Radius - r1Radius)/centerDistance);
			
			//Now that we have this angle, we know the angle for the line from c1 to r3t.
			//Since r3t is essentially the center of a circle with radius r1, we know that
			//our r1r2 tangent line must be perpendicular to this line.  Find the angle for 
			//this line, and use it to calculate our actual start angle for r1.
			//The final angle depends on which roller we are using as r1.
			double netAngle = centerVectorAngle + (nextRollerLarger ? -inscribedVectorAngle - Math.PI/2D : inscribedVectorAngle + Math.PI/2D);						
			endAngle = Math.toDegrees(netAngle);
			nextRoller.startAngle = endAngle;
			
			//Now that we know the start and end angles, we can calculate the start and end points.
			//Simple polar to rectangular coord conversion here.
			endY = centerPoint.y + radius*Math.cos(Math.toRadians(endAngle));
			endZ = centerPoint.z + radius*Math.sin(Math.toRadians(endAngle));
			nextRoller.startY = nextRoller.centerPoint.y + nextRoller.radius*Math.cos(Math.toRadians(endAngle));
			nextRoller.startZ = nextRoller.centerPoint.z + nextRoller.radius*Math.sin(Math.toRadians(endAngle));
		}
	}
}
