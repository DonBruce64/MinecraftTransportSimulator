package minecrafttransportsimulator.baseclasses;

/**Double implementation of point class.
 *
 * @author don_bruce
 */
public class Point3d extends APoint3<Double, Point3d>{
	private static final Point3d ZERO = new Point3d(0D, 0D, 0D);
	
	public double x;
	public double y;
	public double z;
	
	public Point3d(double x, double y, double z){
		super(x, y, z);
	}
	
	@Override
	public boolean equals(Object object){
		if(object instanceof Point3d){
			Point3d otherPoint = (Point3d) object;
			return x == otherPoint.x & y == otherPoint.y && z == otherPoint.z;
		}else{
			return false;
		}
	}
	
	@Override
	public Point3d set(Double x, Double y, Double z){
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	@Override
	public Point3d add(Double x, Double y, Double z){
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}
	
	@Override
	public Point3d add(Point3d point){
		this.x += point.x;
		this.y += point.y;
		this.z += point.z;
		return this;
	}
	
	@Override
	public Point3d subtract(Point3d point){
		this.x -= point.x;
		this.y -= point.y;
		this.z -= point.z;
		return this;
	}
	
	@Override
	public Point3d multiply(Double scale){
		this.x *= scale;
		this.y *= scale;
		this.z *= scale;
		return this;
	}

	@Override
	public Point3d multiply(Point3d point){
		this.x *= point.x;
		this.y *= point.y;
		this.z *= point.z;
		return this;
	}
	
	@Override
	public Double distanceTo(Point3d point){
		double deltaX = point.x - this.x;
		double deltaY = point.y - this.y;
		double deltaZ = point.z - this.z;
		return Math.sqrt(deltaX*deltaX + deltaY*deltaY + deltaZ*deltaZ);
	}
	
	@Override
	public Double dotProduct(Point3d point){
		return this.x*point.x + this.y*point.y + this.z*point.z;
	}
	
	@Override
	public Point3d crossProduct(Point3d point){
		return new Point3d(this.y*point.z - this.z*point.y, this.z*point.x - this.x*point.z, this.x*point.y - this.y*point.x);
	}
	
	@Override
	public Double length(){
		return Math.sqrt(x*x + y*y + z*z);
	}
	
	@Override
	public Point3d normalize(){
		Double length = length();
		if(length > 1.0E-4D){
			x /= length;
			y /= length;
			z /= length;
		}
		return this;
	}
	
	@Override
	public Point3d copy(){
		return new Point3d(this.x, this.y, this.z);
	}
	
	@Override
	public boolean isZero(){
		return this.equals(ZERO);
	}
}
