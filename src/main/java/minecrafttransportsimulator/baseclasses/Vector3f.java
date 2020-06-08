package minecrafttransportsimulator.baseclasses;

/**Float implementation of vector class.
*
* @author don_bruce
*/
public class Vector3f extends Point3f implements IVector3<Float, Point3f>{
	public static final Vector3f ZERO = new Vector3f(0F, 0F, 0F);
	
	public Vector3f(Float x, Float y, Float z){
		super(x, y, z);
	}

	@Override
	public Float length(){
		return (float) Math.sqrt(x*x + y*y + z*z);
	}
	
	@Override
	public void normalize(){
		Float length = length();
		x /= length;
		y /= length;
		z /= length;
	}
}
