package minecrafttransportsimulator.baseclasses;

/**Basic 3D vector interface.  Designed to operate on point class values.
 *
 * @author don_bruce
 */
public interface IVector3<DataType extends Number, PointType extends APoint3<DataType>>{
	
	/**
	 * Returns how long this vector is.
	 */
	public abstract DataType length();
	
	/**
	 * Normalizes this vector.
	 */
	public abstract void normalize();
}
