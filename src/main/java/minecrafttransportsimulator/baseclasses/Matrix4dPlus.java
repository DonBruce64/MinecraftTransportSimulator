package minecrafttransportsimulator.baseclasses;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;

/**Basic Orientation class.  Stores an axis for direction, and rotation about that axis as a double.
 * Maintains a Quaternion of these, which allows it to be multiplied with other objects to 
 * produce a final rotation.  Used to store the orientation of objects and render them without the use 
 * of Euler Angles.  This allow for multiple axis of rotation to be applied and prevents odd bugs with 
 * interpolation due to sign differences.  Note that you should NEVER modify axis, and should always modify
 * rotation via {@link #setRotation(double)} to ensure proper state is maintained.
 *
 * @author don_bruce
 */
public class Matrix4dPlus extends Matrix4d{
	public final Point3dPlus lastAnglesSet = new Point3dPlus();
	private final AxisAngle4d helperAxisAngle = new AxisAngle4d();
	//Can't be created on class construction or we'll infinite loop.
	private Matrix4dPlus helperMatrix;
	
	public Matrix4dPlus(){
		super();
		setIdentity();
	}
	
	public Matrix4dPlus(Matrix4dPlus other){
		super(other);
	}
	
	public void setAngles(Point3dPlus angles){
		lastAnglesSet.set(angles);
		if(helperMatrix == null){
			helperMatrix = new Matrix4dPlus();
		}
		
		setIdentity();
		if(angles.y != 0){
			helperAxisAngle.set(0, 1, 0, Math.toRadians(angles.y));
			setRotation(helperAxisAngle);
		}
		
		if(angles.x != 0){
			helperAxisAngle.set(1, 0, 0, Math.toRadians(angles.x));
			helperMatrix.setIdentity();
			helperMatrix.setRotation(helperAxisAngle);
			mul(helperMatrix);
		}
		
		if(angles.z != 0){
			helperAxisAngle.set(0, 0, 1, Math.toRadians(angles.z));
			helperMatrix.setIdentity();
			helperMatrix.setRotation(helperAxisAngle);
			mul(helperMatrix);
		}
	}
}
