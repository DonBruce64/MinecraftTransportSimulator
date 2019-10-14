package minecrafttransportsimulator.baseclasses;

/**This class is essentially a wrapper for a 3D point in space.  It contains multiple
 * methods used for point manipulation.  Most importantly, the point's values are NOT
 * static, which means no more of the crap of making new class instances when we want
 * to move the point.  It also allows for cleaner modification in the code by applying
 * deltas to the point instead of re-making the point entirely.
 * 
 * Note that this class does not contain functions for vector math such as dot and
 * cross products that the vector systems in MC have.  Use vectors for vectors, and
 * points for points.
 * 
 * @author don_bruce
 */
public class Point{
	public double x;
	public double y;
	public double z;
	
	public Point(double x, double y, double z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void offset(double xOffset, double yOffset, double zOffset){
		this.x += xOffset;
		this.y += xOffset;
		this.z += xOffset;
	}
	
	public double distanceTo(Point otherPoint){
		return Math.sqrt((this.x - otherPoint.x)*(this.x - otherPoint.x) + (this.y - otherPoint.y)*(this.y - otherPoint.y) + (this.z - otherPoint.z)*(this.z - otherPoint.z));
	}
}
