package minecrafttransportsimulator.baseclasses;

/**Point class designed for discrete positions.  This is used by blocks, which do not allow
 * for fractional position locations.  Allows for saving of positions in a less recourse-heavy
 * format than Minecraft's BlockPos.  This class is mutable to allow
 * the point to change, cause we don't need to create a new point every time we
 * move a thing.
 *
 * @author don_bruce
 */
public class Point3i{
	private static final Point3i ZERO = new Point3i(0, 0, 0);
	
	public int x;
	public int y;
	public int z;
	
	public Point3i(int x, int y, int z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/**
	 * Helper constructor for casting-down double points to ints.
	 */
	public Point3i(Point3d point){
		this((int) point.x, (int) point.y, (int) point.z);
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
	
	/**
	 * Sets the value of the point to the passed-in values.
	 * Returns the called object for nested operations.
	 */
	public Point3i set(int x, int y, int z){
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	/**
	 * Sets the value of the point to the values of the passed-in point.
	 * Returns the called object for nested operations.
	 */
	public Point3i setTo(Point3i point){
		this.x = point.x;
		this.y = point.y;
		this.z = point.z;
		return this;
	}
	
	/**
	 * Adds the passed-in values to the point.
	 * Returns the called object for nested operations.
	 */
	public Point3i add(int x, int y, int z){
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}
	
	/**
	 * Adds the passed-in point's values to this point.
	 * Returns the called object for nested operations.
	 */
	public Point3i add(Point3i point){
		this.x += point.x;
		this.y += point.y;
		this.z += point.z;
		return this;
	}
	
	/**
	 * Subtracts the passed-in point's values to this point.
	 * Returns the called object for nested operations.
	 */
	public Point3i subtract(Point3i point){
		this.x -= point.x;
		this.y -= point.y;
		this.z -= point.z;
		return this;
	}
	
	/**
	 * Returns a copy of this point.
	 */
	public Point3i copy(){
		return new Point3i(this.x, this.y, this.z);
	}
	
	/**
	 * Returns true if this point is 0,0,0.
	 */
	public boolean isZero(){
		return this.equals(ZERO);
	}
}
