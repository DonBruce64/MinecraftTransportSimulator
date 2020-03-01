package minecrafttransportsimulator.rendering.vehicles;

/**This class is a helper class that's used for creating tread rollers on vehicles.
 * These rollers, and their relations to one another, are used to calculate the tread
 * paths in auto-tread mode.  In all cases, an angle of 0 implies the tread is facing 
 * down to the ground.  This class can't be created directly.  Rather, it's created
 * from a {@link RenderVehicle_RotatablePart}.
 *
 * @author don_bruce
 */
public final class RenderVehicle_TreadRoller{
	final double yPos;
	final double zPos;
	final double radius;
	final double circumference;
	
	double startY;
	double startZ;
	double startAngle;
	double endY;
	double endZ;
	double endAngle;
	
	public RenderVehicle_TreadRoller(RenderVehicle_RotatablePart roller, double minY, double maxY, double minZ, double maxZ){
		//Radius and center are based off of min/max points.
		radius = (maxZ - minZ)/2D;
		circumference = 2*Math.PI*radius;
		yPos = minY + (maxY - minY)/2D;
		zPos = minZ + (maxZ - minZ)/2D;
	}
	
	/**
	 * Calculates the end point of this roller and the
	 * start point of the passed-in roller using trigonometry.
	 * We can assume that we'll always be on the outside point of any roller.
	 * Additionally, we know we'll start on the bottom of a roller, so between
	 * those two things we can tell which tangent we should follow.
	 */
	public void calculateEndpoints(RenderVehicle_TreadRoller nextRoller){
		//What calculations we do depend on if the rollers are the same size.
		//If so, we can do simple calcs.  If not, we get to do trig.
		if(radius == nextRoller.radius){
			//First, get the angle from the vector from this roller to the next roller.
			//From this, we can calculate the end angle for this roller as perpendicular to
			//the vector.  We rotate 90 degrees as we know the roller orientation will be
			//counter-clockwise, and thus we always want the tread to be on that side.
			endAngle = Math.toDegrees(Math.atan2(nextRoller.zPos - zPos, nextRoller.yPos - yPos)) - 90D;
			nextRoller.startAngle = endAngle;
			
			//Now that we know the start and end angles, we can calculate the start and end points.
			//Simple polar to rectangular coord conversion here.
			endY = yPos + radius*Math.cos(Math.toRadians(endAngle));
			endZ = zPos + radius*Math.sin(Math.toRadians(endAngle));
			nextRoller.startY = nextRoller.yPos + nextRoller.radius*Math.cos(Math.toRadians(endAngle));
			nextRoller.startZ = nextRoller.zPos + nextRoller.radius*Math.sin(Math.toRadians(endAngle));
		}else{
			//First, get the distance between the roller centers.
			double centerDistance = Math.hypot(nextRoller.zPos - zPos, nextRoller.yPos - yPos);
			
			//The next parts depend which roller is bigger.  From here on out, the
			//smaller roller is r1, and the larger is r2.
			boolean nextRollerLarger = radius < nextRoller.radius;
			double r1CenterY = nextRollerLarger ? yPos : nextRoller.yPos;
			double r1CenterZ = nextRollerLarger ? zPos : nextRoller.zPos;
			double r2CenterY = !nextRollerLarger ? yPos : nextRoller.yPos;
			double r2CenterZ = !nextRollerLarger ? zPos : nextRoller.zPos;
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
			endY = yPos + radius*Math.cos(Math.toRadians(endAngle));
			endZ = zPos + radius*Math.sin(Math.toRadians(endAngle));
			nextRoller.startY = nextRoller.yPos + nextRoller.radius*Math.cos(Math.toRadians(endAngle));
			nextRoller.startZ = nextRoller.zPos + nextRoller.radius*Math.sin(Math.toRadians(endAngle));
		}
	}
}
