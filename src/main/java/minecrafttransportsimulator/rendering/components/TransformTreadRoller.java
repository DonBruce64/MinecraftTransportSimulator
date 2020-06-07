package minecrafttransportsimulator.rendering.components;

import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRotatableModelObject;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;

/**A specific class of {@link TransformRotatable}, designed
 * for tread rollers.  Contains an extra method for calculating things.
 * Also auto-creates rotatableModelObject definitions in the relevant JSON.
 *
 * @author don_bruce
 */
public class TransformTreadRoller extends TransformRotatable{
	public final int rollerNumber;
	public final double yPos;
	public final double zPos;
	public final double radius;
	public final double circumference;
	
	public double startY;
	public double startZ;
	public double startAngle;
	public double endY;
	public double endZ;
	public double endAngle;
	
	private TransformTreadRoller(String modelName, String objectName, List<VehicleRotatableModelObject> rotatableModelObjects, double yPos, double zPos, double radius, double circumference){
		super(modelName, objectName, rotatableModelObjects);
		this.rollerNumber = Integer.valueOf(objectName.substring(objectName.lastIndexOf('_') + 1));
		this.zPos = zPos;
		this.yPos = yPos;
		this.radius = radius;
		this.circumference = circumference;
	}
	
	/**
	 * Helper function to create a tread roller.
	 */
	public static TransformTreadRoller create(String modelName, String objectName, EntityVehicleE_Powered vehicle, Float[][] vertices){
		//Get the points that define this roller.
		double minY = 999;
		double maxY = -999;
		double minZ = 999;
		double maxZ = -999;
		for(Float[] point : vertices){
			minY = Math.min(minY, point[1]);
			maxY = Math.max(maxY, point[1]);
			minZ = Math.min(minZ, point[2]);
			maxZ = Math.max(maxZ, point[2]);
		}
		double yPos = minY + (maxY - minY)/2D;
		double zPos = minZ + (maxZ - minZ)/2D;
		double radius = (maxZ - minZ)/2D;
		double circumference = 2*Math.PI*radius;
		
		//Add this roller as a rotatable if it doesn't exist.
		boolean existsInJSON = false;
		for(VehicleRotatableModelObject rotatable : vehicle.definition.rendering.rotatableModelObjects){
			if(rotatable.partName.endsWith(objectName)){
				existsInJSON = true;
				break;
			}
		}
		if(!existsInJSON){
			//We don't have this rotatable.  Add it.
			VehicleRotatableModelObject rotatable = vehicle.definition.new VehicleRotatableModelObject();
			rotatable.partName = objectName;
			rotatable.rotationVariable = "speed";
			rotatable.rotationPoint = new float[]{0, (float) yPos, (float) zPos};
			rotatable.rotationAxis = new float[]{(float) (1D/radius), 0, 0};
			vehicle.definition.rendering.rotatableModelObjects.add(rotatable);
		}
		
		//Create and return the roller.
		return new TransformTreadRoller(modelName, objectName, vehicle.definition.rendering.rotatableModelObjects, yPos, zPos, radius, circumference);
	}
	
	/**
	 * Calculates the end point of this roller and the
	 * start point of the passed-in roller using trigonometry.
	 * We can assume that we'll always be on the outside point of any roller.
	 * Additionally, we know we'll start on the bottom of a roller, so between
	 * those two things we can tell which tangent we should follow.
	 */
	public void calculateEndpoints(TransformTreadRoller nextRoller){
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
