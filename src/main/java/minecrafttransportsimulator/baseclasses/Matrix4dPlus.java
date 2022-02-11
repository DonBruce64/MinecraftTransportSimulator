package minecrafttransportsimulator.baseclasses;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;

/**Upgraded Matrix4d class that allows for a method to convert
 * Euler angles into the matrix as the rotational component. 
 *
 * @author don_bruce
 */
public class Matrix4dPlus extends Matrix4d{
	public final Point3dPlus lastAnglesSet = new Point3dPlus();

	private final Matrix3d matX = new Matrix3d();
	private final Matrix3d matY = new Matrix3d();
	private final Matrix3d matZ = new Matrix3d();
	private final Matrix4d helperTranslationTransform = new Matrix4d();
	private final Matrix4d helperRotationTransform = new Matrix4d();
	private final Matrix4d helperScalingTransform = new Matrix4d();
	private final AxisAngle4d helperAxisAngle = new AxisAngle4d();
	
	public Matrix4dPlus(){
		super();
		setIdentity();
		helperTranslationTransform.setIdentity();
		helperRotationTransform.setIdentity();
		helperScalingTransform.setIdentity();
	}
	
	public Matrix4dPlus(Matrix4dPlus other){
		this();
		set(other);
	}
	
	public Matrix4dPlus(Point3dPlus angles){
		this();
		setToAngles(angles);
	}
	
	public void setToAngles(Point3dPlus angles){
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
	
	/**Resets this matrix transform to default.  This should be done
	 * prior to applying any transforms on it.
	 */
	public void resetTransforms(){
		setIdentity();
	}
	
	/**Applies a translation transform to this object.
	 * This is here to prevent the need to create transform objects
	 * in all classes that just want to translate renderables.
	 */
	public void translate(double x, double y, double z){
		helperTranslationTransform.m03 = x;
		helperTranslationTransform.m13 = y;
		helperTranslationTransform.m23 = z;
		helperTranslationTransform.m33 = 1;
		mul(helperTranslationTransform);
	}
	
	/**Applies a translation transform to this object.
	 * This is here to prevent the need to create transform objects
	 * in all classes that just want to translate renderables.
	 */
	public void translate(Point3dPlus translation){
		translate(translation.x, translation.y, translation.z);
	}
	
	/**Applies a rotation transform to this object.
	 * This is here to prevent the need to create transform objects
	 * in all classes that just want to rotate renderables.
	 */
	public void rotate(double angle, double x, double y, double z){
		if(angle != 0){
			helperAxisAngle.set(x, y, z, Math.toRadians(angle));
			helperRotationTransform.set(helperAxisAngle); 
			mul(helperRotationTransform);
		}
	}
	
	/**Applies a uniform scaling transform to this object.
	 * This is here to prevent the need to create transform objects
	 * in all classes that just want to scale renderables.
	 */
	public void scale(double scaling){
		if(scaling != 1.0){
			helperScalingTransform.m00 = scaling;
			helperScalingTransform.m11 = scaling;
			helperScalingTransform.m22 = scaling;
			helperScalingTransform.m33 = 1;
			mul(helperScalingTransform);
		}
	}
	
	/**Applies a scaling transform to this object.
	 * This is here to prevent the need to create transform objects
	 * in all classes that just want to scale renderables.
	 */
	public void scale(Point3dPlus scaling){
		scale(scaling.x, scaling.y, scaling.z);
	}
	
	/**Applies a scaling transform to this object.
	 * This is here to prevent the need to create transform objects
	 * in all classes that just want to scale renderables.
	 */
	public void scale(double x, double y, double z){
		helperScalingTransform.m00 = x;
		helperScalingTransform.m11 = y;
		helperScalingTransform.m22 = z;
		helperScalingTransform.m33 = 1;
		mul(helperScalingTransform);
	}
	
	/**Applies a general transform to this object.
	 */
	public void matrix(Matrix4d transformToApply){
		mul(transformToApply);
	}
	
	/**Applies a pre-transform to this object.
	 * This basically applies a transform that happens before
	 * the current object's transform operation.
	 */
	public void matrixPrior(Matrix4d transformToApply){
		mul(transformToApply, this);
	}
}
