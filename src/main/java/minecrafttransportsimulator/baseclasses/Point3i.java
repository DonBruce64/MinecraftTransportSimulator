package minecrafttransportsimulator.baseclasses;

/**Integer implementation of point class.
 *
 * @author don_bruce
 */
public class Point3i extends APoint3<Integer>{
	public static final Point3i ZERO = new Point3i(0, 0, 0);
	
	public int x;
	public int y;
	public int z;
	
	public Point3i(Integer x, Integer y, Integer z){
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
	public void set(Integer x, Integer y, Integer z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public void offset(Integer x, Integer y, Integer z){
		this.x += x;
		this.y += y;
		this.z += z;
	}
}
