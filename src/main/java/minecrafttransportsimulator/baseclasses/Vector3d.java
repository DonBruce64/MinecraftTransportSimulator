package minecrafttransportsimulator.baseclasses;

/**Enhanced 3D point class with vector support.  Constructed the way way as
 * the parent point class, but contains a Quaternion pre-calculated by which we 
 * can rotate points about.  This greatly helps in both world-orientation,
 * and rendering pre-calculation of matrixes.  Should the values of this vector be changed,
 * it should be flagged for a re-calculation of its Quaternion to ensure proper state.
 *
 * @author don_bruce
 */
public class Vector3d extends Point3d{
	private double qX;
	private double qY;
	private double qZ;
	private double qW;
	
	public Vector3d(){
		this(0, 0, 0);
	}
	
	public Vector3d(double x, double y, double z){
		super(x, y, z);
		updateQuaternion();
	}
	
	/**
	 * Updates the Quaternion.  This should be done any time the position
	 * or magnitude of the rotation of this vector changes.
	 */
	public void updateQuaternion(){
		double rotationInRad = Math.toRadians(length());
		double sinRad = Math.sin(rotationInRad/2D);
		double cosRad = Math.cos(rotationInRad/2D);
		
		qX = x*sinRad;
		qY = y*sinRad;
		qZ = z*sinRad;
		qW = cosRad;
	}
}
