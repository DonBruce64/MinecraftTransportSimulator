package minecrafttransportsimulator.baseclasses;

import javax.vecmath.Matrix4d;
import javax.vecmath.Quat4d;

/**3D Matrix class for rotation operations.  This should be used
 * instead of Euler angles for all things in the code.
 * All methods return this object for nested operations, unless
 * otherwise specified or void.
 *
 * @author don_bruce
 */
public class RotationMatrix{
	private static final RotationMatrix helperMatrix = new RotationMatrix();
	private static final Quat4d interpHelperQuatStart = new Quat4d();
	private static final Matrix4d interpHelperMatrixStart = new Matrix4d();
	private static final Quat4d interpHelperQuatEnd = new Quat4d();
	private static final Matrix4d interpHelperMatrixEnd = new Matrix4d();
	
	public double m00;
	public double m01;
	public double m02;
	public double m10;
	public double m11;
	public double m12;
	public double m20;
	public double m21;
	public double m22;
	public final Point3D lastAnglesSet = new Point3D();
	
	public RotationMatrix(){
		setToZero();
	}
	
	/**
	 * Sets this matrix to the passed-in matrix.
	 */
	public RotationMatrix set(RotationMatrix other){
        m00 = other.m00;
        m01 = other.m01;
        m02 = other.m02;
        m10 = other.m10;
        m11 = other.m11;
        m12 = other.m12;
        m20 = other.m20;
        m21 = other.m21;
        m22 = other.m22;
        return this;
    }
	
	/**
	 * Multiplies this matrix with the passed-in matrix.
	 */
	public RotationMatrix multiply(RotationMatrix other){
        double t00 = m00*other.m00 + m01*other.m10 + m02*other.m20;
		double t01 = m00*other.m01 + m01*other.m11 + m02*other.m21;
		double t02 = m00*other.m02 + m01*other.m12 + m02*other.m22;
		
		double t10 = m10*other.m00 + m11*other.m10 + m12*other.m20;
		double t11 = m10*other.m01 + m11*other.m11 + m12*other.m21;
		double t12 = m10*other.m02 + m11*other.m12 + m12*other.m22;
		
		double t20 = m20*other.m00 + m21*other.m10 + m22*other.m20;
		double t21 = m20*other.m01 + m21*other.m11 + m22*other.m21;
		double t22 = m20*other.m02 + m21*other.m12 + m22*other.m22;

        m00 = t00;
        m01 = t01;
        m02 = t02;
        m10 = t10;
        m11 = t11;
        m12 = t12;
        m20 = t20;
        m21 = t21;
        m22 = t22;
        return this;
    }
	
	/**
	 * Sets the rotation of this matrix to 0 for all angles.
	 */
	public RotationMatrix setToZero(){
		m00 = 1.0;
		m01 = 0.0;
		m02 = 0.0;
		
		m10 = 0.0;
		m11 = 1.0;
		m12 = 0.0;
		
		m20 = 0.0;
		m21 = 0.0;
		m22 = 1.0;
		return this;
	}
    
	/**Sets the rotation of this matrix to the angles (degrees)
	 * of the passed-in point. If {@link #lastAnglesSet} is
	 * equal to the angles, no operations are performed.
	 * 
	 * Note that this function is not efficient, nor is it
	 * safe to have it running on multiple threads or multiple
	 * object instances at the same time.  Basically, don't use
	 * if you can avoid it.
	 */
	public synchronized RotationMatrix setAngleRotation(Point3D angles){
		//Don't bother setting angles if they are already correct.
		if(!lastAnglesSet.equals(angles)){
			lastAnglesSet.set(angles);
			
			//First set rotation to y rotation.
			double sin = Math.sin(Math.toRadians(angles.y));
			double cos = Math.cos(Math.toRadians(angles.y));
			m00 = cos;
			m01 = 0.0;
			m02 = sin;
			m10 = 0.0;
			m11 = 1.0;
			m12 = 0.0;
			m20 = -sin;
			m21 = 0.0;
			m22 = cos;
			
			//Now set helper to x rotation, and multiply.
			sin = Math.sin(Math.toRadians(angles.x));
			cos = Math.cos(Math.toRadians(angles.x));
			helperMatrix.m00 = 1.0;
			helperMatrix.m01 = 0.0;
			helperMatrix.m02 = 0.0;
			helperMatrix.m10 = 0.0;
			helperMatrix.m11 = cos;
			helperMatrix.m12 = -sin;
			helperMatrix.m20 = 0.0;
			helperMatrix.m21 = sin;
			helperMatrix.m22 = cos;
			multiply(helperMatrix);
			
			//Finally, set helper to z rotation, and multiply.
			sin = Math.sin(Math.toRadians(angles.z));
			cos = Math.cos(Math.toRadians(angles.z));
			helperMatrix.m00 = cos;
			helperMatrix.m01 = -sin;
			helperMatrix.m02 = 0.0;
			helperMatrix.m10 = sin;
			helperMatrix.m11 = cos;
			helperMatrix.m12 = 0.0;
			helperMatrix.m20 = 0.0;
			helperMatrix.m21 = 0.0;
			helperMatrix.m22 = 1.0;
			multiply(helperMatrix);
		}
		return this;
	}
	
	/**
	 * Sets the rotation of this matrix via an axis-angle (degrees) format.
	 * Note that the x/y/z parameters MUST represent a normalized vector!
	 */
	public RotationMatrix setAxisAngleRotation(double x, double y, double z, double angle){
		if(angle != 0){
			double sin = Math.sin(Math.toRadians(angle));
			double cos = Math.cos(Math.toRadians(angle));
			double invCos = 1.0 - cos;
			
			double xz = x * z;
			double xy = x * y;
			double yz = y * z;
			
			m00 = invCos * x * x + cos;
			m01 = invCos * xy - sin * z;
			m02 = invCos * xz + sin * y;
			
			m10 = invCos * xy + sin * z;
			m11 = invCos * y * y + cos;
			m12 = invCos * yz - sin * x;
			
			m20 = invCos * xz - sin * y;
			m21 = invCos * yz + sin * x;
			m22 = invCos * z * z + cos;
		}else{
			setToZero();
		}
		return this;
	}
	
	/**
	 * Like {@link #setAxisAngleRotation(double, double, double, double)},
	 * but uses a point parameter.
	 */
	public RotationMatrix setAxisAngleRotation(Point3D point, double angle){
		return setAxisAngleRotation(point.x, point.y, point.z, angle);
	}
	
	/**
	 * Interpolates between the two passed-in matrixes, storing the result
	 * in this matrix.
	 */
	public RotationMatrix interploate(RotationMatrix start, RotationMatrix end, double delta){
		//TODO code this internally without all the roudabout code hacks.
		interpHelperMatrixStart.m00 = start.m00;
		interpHelperMatrixStart.m01 = start.m01;
		interpHelperMatrixStart.m02 = start.m02;
		interpHelperMatrixStart.m10 = start.m10;
		interpHelperMatrixStart.m11 = start.m11;
		interpHelperMatrixStart.m12 = start.m12;
		interpHelperMatrixStart.m20 = start.m20;
		interpHelperMatrixStart.m21 = start.m21;
		interpHelperMatrixStart.m22 = start.m22;
		interpHelperMatrixStart.m33 = 1;
		interpHelperMatrixStart.get(interpHelperQuatStart);
		
		interpHelperMatrixEnd.m00 = end.m00;
		interpHelperMatrixEnd.m01 = end.m01;
		interpHelperMatrixEnd.m02 = end.m02;
		interpHelperMatrixEnd.m10 = end.m10;
		interpHelperMatrixEnd.m11 = end.m11;
		interpHelperMatrixEnd.m12 = end.m12;
		interpHelperMatrixEnd.m20 = end.m20;
		interpHelperMatrixEnd.m21 = end.m21;
		interpHelperMatrixEnd.m22 = end.m22;
		interpHelperMatrixEnd.m33 = 1;
		interpHelperMatrixEnd.get(interpHelperQuatEnd);
		
		interpHelperQuatStart.interpolate(interpHelperQuatEnd, delta);
		interpHelperMatrixEnd.set(interpHelperQuatStart);
		
		m00 = interpHelperMatrixEnd.m00;
		m01 = interpHelperMatrixEnd.m01;
		m02 = interpHelperMatrixEnd.m02;
		m10 = interpHelperMatrixEnd.m10;
		m11 = interpHelperMatrixEnd.m11;
		m12 = interpHelperMatrixEnd.m12;
		m20 = interpHelperMatrixEnd.m20;
		m21 = interpHelperMatrixEnd.m21;
		m22 = interpHelperMatrixEnd.m22;
		return this;
	}
}
