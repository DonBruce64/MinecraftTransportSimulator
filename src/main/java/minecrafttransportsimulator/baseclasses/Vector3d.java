package minecrafttransportsimulator.baseclasses;

/**Double implementation of vector class.
*
* @author don_bruce
*/
public class Vector3d extends Point3d implements IVector3<Double, Point3d>{
	
	public Vector3d(Double x, Double y, Double z){
		super(x, y, z);
	}

	@Override
	public Double length(){
		return Math.sqrt(x*x + y*y + z*z);
	}
	
	@Override
	public void normalize(){
		Double length = length();
		x /= length;
		y /= length;
		z /= length;
	}
}
