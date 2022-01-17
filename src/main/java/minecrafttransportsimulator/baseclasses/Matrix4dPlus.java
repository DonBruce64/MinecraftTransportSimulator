package minecrafttransportsimulator.baseclasses;

import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;

/**Upgraded Matrix4d class that allows for a method to convert
 * Euler angles into the matrix as the rotational component. 
 *
 * @author don_bruce
 */
public class Matrix4dPlus extends Matrix4d{
	public final Point3dPlus lastAnglesSet = new Point3dPlus();

	private Matrix3d matX = new Matrix3d();
	private Matrix3d matY = new Matrix3d();
	private Matrix3d matZ = new Matrix3d();
	
	public Matrix4dPlus(){
		super();
		setIdentity();
	}
	
	public Matrix4dPlus(Matrix4dPlus other){
		super(other);
	}
	
	public void setAngles(Point3dPlus angles){
		//Don't bother setting angles if they are already correct.
		if(!lastAnglesSet.equals(angles)){
			lastAnglesSet.set(angles);
			matX.rotX(Math.toRadians(angles.x));
			matY.rotY(Math.toRadians(angles.y));
			matZ.rotZ(Math.toRadians(angles.z));
			matY.mul(matX);
			matY.mul(matZ);
			
			setRotationScale(matY);
			//Need to set this in case we didn't set ourselves as an identity before.
			m33 = 1.0;
		}
	}
}
