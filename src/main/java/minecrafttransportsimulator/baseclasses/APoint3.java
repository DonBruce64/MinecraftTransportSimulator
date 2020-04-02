package minecrafttransportsimulator.baseclasses;

/**Basic 3D point class.  Allows for saving of positions in a less recourse-heavy
 * format than Minecraft's vectors or BlockPos.  This class is mutable to allow
 * the point to change, cause we don't need to create a new point every time we
 * move a thing.  This class is abstract, as it exists purely to define a framework
 * for the different point types.
 *
 * @author don_bruce
 */
public abstract class APoint3<DataType extends Number>{
	
	public APoint3(DataType x, DataType y, DataType z){
		set(x, y, z);
	}
	
	/**
	 * Sets the value of the point to the passed-in value.
	 */
	public abstract void set(DataType x, DataType y, DataType z);
	
	/**
	 * Offsets this point by the passed-in values.
	 */
	public abstract void offset(DataType x, DataType y, DataType z);
}
