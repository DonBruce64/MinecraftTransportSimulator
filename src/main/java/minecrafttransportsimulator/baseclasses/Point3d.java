package minecrafttransportsimulator.baseclasses;

/**Double implementation of point class.
 *
 * @author don_bruce
 */
public class Point3d extends APoint3<Double>{
	public static final Point3d ZERO = new Point3d(0D, 0D, 0D);
	
	public double x;
	public double y;
	public double z;
	
	public Point3d(Double x, Double y, Double z){
		super(x, y, z);
	}
	
	@Override
	public boolean equals(Object object){
		if(object instanceof Point3i){
			Point3i otherPoint = (Point3i) object;
			return x == otherPoint.x & y == otherPoint.y && z == otherPoint.z;
		}else{
			return false;
		}
	}
	
	@Override
	public void set(Double x, Double y, Double z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public void offset(Double x, Double y, Double z){
		this.x += x;
		this.y += y;
		this.z += z;
	}
}
