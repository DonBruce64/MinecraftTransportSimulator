package minecrafttransportsimulator.baseclasses;

/**Integer implementation of point class.
 *
 * @author don_bruce
 */
public class Point3i extends APoint3<Integer, Point3i>{
	private static final Point3i ZERO = new Point3i(0, 0, 0);
	
	public int x;
	public int y;
	public int z;
	
	public Point3i(int x, int y, int z){
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
	public Point3i set(Integer x, Integer y, Integer z){
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	@Override
	public Point3i setTo(Point3i point){
		this.x = point.x;
		this.y = point.y;
		this.z = point.z;
		return this;
	}
	
	@Override
	public Point3i add(Integer x, Integer y, Integer z){
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}
	
	@Override
	public Point3i add(Point3i point){
		this.x += point.x;
		this.y += point.y;
		this.z += point.z;
		return this;
	}
	
	@Override
	public Point3i subtract(Point3i point){
		this.x -= point.x;
		this.y -= point.y;
		this.z -= point.z;
		return this;
	}
	
	@Override
	public Point3i multiply(Integer scale){
		this.x *= scale;
		this.y *= scale;
		this.z *= scale;
		return this;
	}

	@Override
	public Point3i multiply(Point3i point){
		this.x *= point.x;
		this.y *= point.y;
		this.z *= point.z;
		return this;
	}
	
	@Override
	public Integer distanceTo(Point3i point){
		int deltaX = point.x - this.x;
		int deltaY = point.y - this.y;
		int deltaZ = point.z - this.z;
		return (int) Math.sqrt(deltaX*deltaX + deltaY*deltaY + deltaZ*deltaZ);
	}
	
	@Override
	public Integer dotProduct(Point3i point){
		return this.x*point.x + this.y*point.y + this.z*point.z;
	}
	
	@Override
	public Point3i crossProduct(Point3i point){
		return new Point3i(this.y*point.z - this.z*point.y, this.z*point.x - this.x*point.z, this.x*point.y - this.y*point.x);
	}
	
	@Override
	public Integer length(){
		return (int) Math.sqrt(x*x + y*y + z*z);
	}
	
	@Override
	public Point3i normalize(){
		Integer length = length();
		x /= length;
		y /= length;
		z /= length;
		return this;
	}
	
	@Override
	public Point3i copy(){
		return new Point3i(this.x, this.y, this.z);
	}
	
	@Override
	public boolean isZero(){
		return this.equals(ZERO);
	}
}
