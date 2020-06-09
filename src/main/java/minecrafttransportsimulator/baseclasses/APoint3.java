package minecrafttransportsimulator.baseclasses;

/**Basic 3D point class.  Allows for saving of positions in a less recourse-heavy
 * format than Minecraft's vectors or BlockPos.  This class is mutable to allow
 * the point to change, cause we don't need to create a new point every time we
 * move a thing.  This class is abstract, as it exists purely to define a framework
 * for the different point types.
 *
 * @author don_bruce
 */
public abstract class APoint3<DataType extends Number, PointType extends APoint3<DataType, PointType>>{
	
	public APoint3(DataType x, DataType y, DataType z){
		set(x, y, z);
	}
	
	/**
	 * Sets the value of the point to the passed-in value.
	 * Returns the called object for nested operations.
	 */
	public abstract APoint3<DataType, PointType> set(DataType x, DataType y, DataType z);
	
	/**
	 * Adds the passed-in values to the point.
	 * Returns the called object for nested operations.
	 */
	public abstract APoint3<DataType, PointType> add(DataType x, DataType y, DataType z);
	
	/**
	 * Adds the passed-in point's values to this point.
	 * Returns the called object for nested operations.
	 */
	public abstract APoint3<DataType, PointType> add(PointType point);
	
	/**
	 * Subtracts the passed-in point's values to this point.
	 * Returns the called object for nested operations.
	 */
	public abstract APoint3<DataType, PointType> subtract(PointType point);
	
	/**
	 * Multiplies all values of this point by the passed-in factor.
	 * Returns the called object for nested operations.
	 */
	public abstract APoint3<DataType, PointType> multiply(DataType factor);
	
	/**
	 * Multiplies all values of this point by the values of the passed-in point.
	 * Returns the called object for nested operations.
	 */
	public abstract APoint3<DataType, PointType> multiply(PointType point);
	
	/**
	 * Returns the distance between this point and the passed-in point.
	 * Return value complies to the data type of the point class called.
	 */
	public abstract DataType distanceTo(PointType point);
	
	/**
	 * Returns the dot product between this point and the passed-in point.
	 * Return value complies to the data type of the point class called.
	 */
	public abstract DataType dotProduct(PointType point);
	
	/**
	 * Returns the cross product between this point and the passed-in point.
	 * Return value is a new point that is the cross product of the object
	 * this was invoked on, and the passed-in object.  Neither object is
	 * modified by this operation.
	 */
	public abstract PointType crossProduct(PointType point);
	
	/**
	 * Returns the length of this point as if it was a vector.
	 */
	public abstract DataType length();
	
	/**
	 * Normalizes this point to be a unit vector.  Note that the
	 * result of this operation on non-floating-point typed points is undefined.
	 */
	public abstract PointType normalize();
	
	/**
	 * Returns a copy of this point.
	 */
	public abstract APoint3<DataType, PointType> copy();
	
	/**
	 * Returns true if this point is 0,0,0.
	 */
	public abstract boolean isZero();
}
