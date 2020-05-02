package minecrafttransportsimulator.baseclasses;

/**Basic Axis-Aligned bounding box.  This class is mutable and allows for quick setting of values
 * without the need to make a new instance every time.  Also is based on a center point and
 * height and width parameters rather than min/max, though such parameters are calculated to be
 * used in bounds checks.  Note that rather than width and height we use radius here.  The idea
 * being that addition is quicker than multiplication, and most of the time we're doing checks
 * for things a specific distance away rather than within a specific width, height, and depth.
 * For reference, depth is in the Z-direction, while width is in the X-direction.
 *
 * @author don_bruce
 */
public class BoundingBox extends Point3d{
	public double widthRadius;
	public double heightRadius;
	public double depthRadius;
	
	public BoundingBox(double x, double y, double z, double widthRadius, double heightRadius, double depthRadius){
		super(x, y, z);
		this.widthRadius = widthRadius;
		this.heightRadius = heightRadius;
		this.depthRadius = depthRadius;
	}
	
	/**
	 *  Returns true if the passed-in point is inside this box.
	 */
	public boolean isPointInside(Point3d point){
		return 	point.x > x - widthRadius && point.x < x + widthRadius &&
				point.y > y - heightRadius && point.y < y + heightRadius &&
				point.z > z - depthRadius && point.z < z + depthRadius;
	}
	
	/**
	 *  Returns a new BoundingBox that is this bounding box rotated by 90 degrees.
	 */
	public BoundingBox getRotated90(){
		return new BoundingBox(x, y, z, depthRadius, heightRadius, widthRadius);
	}
}
