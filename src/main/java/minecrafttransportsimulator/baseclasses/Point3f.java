package minecrafttransportsimulator.baseclasses;

/**Float implementation of point class.
 *
 * @author don_bruce
 */
public class Point3f extends APoint3<Float>{
	public static final Point3f ZERO = new Point3f(0F, 0F, 0F);
	
	public float x;
	public float y;
	public float z;
	
	public Point3f(Float x, Float y, Float z){
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
	public void set(Float x, Float y, Float z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public void offset(Float x, Float y, Float z){
		this.x += x;
		this.y += y;
		this.z += z;
	}
	
	@Override
	public Point3f newOffset(Float x, Float y, Float z){
		return new Point3f(this.x + x, this.y + y, this.z + z);
	}
}
