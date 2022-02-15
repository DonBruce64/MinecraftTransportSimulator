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
	public synchronized RotationMatrix setToAngles(Point3D angles){
		//Don't bother setting angles if they are already correct.
		if(!lastAnglesSet.equals(angles)){
			lastAnglesSet.set(angles);
			setToZero();
			rotateY(angles.y);
			rotateX(angles.x);
			rotateZ(angles.z);
		}
		return this;
	}
	
	/**
	 * Returns the angles that make up this matrix.  Note: these may not be the
	 * same as the passed-in angles to {@link #setToAngles(Point3D)}. as rotation
	 * matrix conversion has multiple solutions.  In general, these shouldn't be
	 * used for calculations and rather should just be used to store the state of 
	 * the matrix for later use where the orientation of the matrix matters, but
	 * the actual angles don't.
	 */
	public Point3D convertToAngles(){
		Point3D angles = new Point3D();   	 
        //Decompile the matrix based on this formula on how it is built.
	   	//[[1,0,0],[0,cosX,-sinX],[0,sinX,cosX]] X-Rotation
		//[[cosY,0,sinY],[0,1,0],[-sinY,0,cosY]] Y-Rotation
	   	//[[cosZ,-sinZ,0],[sinZ,cosZ,0],[0,0,1]] Z-Rotation
        
        //Y*X*Z Matrix result:
        //[[sinX*sinY*sinZ+cosY*cosZ,sinX*sinY*cosZ-cosY*sinZ,cosX*sinY],
        //[cosX*sinZ,cosX*cosZ,-sinX],
        //[sinX*cosY*sinZ-sinY*cosZ,sinX*cosY*cosZ+sinY*sinZ,cosX*cosY]]
        
        //m00 = sinX*sinY*sinZ+cosY*cosZ;
		//m01 = sinX*sinY*cosZ-cosY*sinZ;
		//m02 = cosX*sinY;
		//m10 = cosX*sinZ;
		//m11 = cosX*cosZ;
		//m12 = -sinX;
		//m20 = sinX*cosY*sinZ-sinY*cosZ;
		//m21 = sinX*cosY*cosZ+sinY*sinZ;
		//m22 = cosX*cosY;
        
        //By this formula, it is obvious that element m12 contains the -sin of the pitch.
        //Therefore, we can easily obtain the pitch with an aSin function.
        angles.x = Math.toDegrees(-Math.asin(m12));
        
        //We know that element m02 is cosX*sinY, and m22 is cosX*cosY.
        //m02/m22 results in sinY/cosY, which is the tangent function.
        //Therefore, we can do aTan on these values to get the angle of Y.
        angles.y = Math.toDegrees(Math.atan2(m02, m22));
        
        //We now need to check for gimbal lock.
        //This realistically only happens if we manually set the values.
        //For all normal operations, exact lock will never occur.
        //If we do get locked, just set roll to 0.  This is because
        //locks don't normally happen except on axial rotations, and
        //those don't normally do roll operations.  Since in both
        //locked cases yaw would equal roll, we just take yaw to be
        //the "true" rotation and mark roll as 0.
        if(m12 == -1){
        	//Gimbal lock with pitch at -90 (down).
        	angles.z = 0;
        }else if(m12 == 1){
        	//Gimbal lock with pitch at 90 (up).
        	angles.z = 0;
        }else{
        	//We know that element m10 is cosX*sinZ, and m11 is cosX*cosZ.
            //m10/m11 results in sinZ/cosZ, which is the tangent function.
            //Therefore, we can do aTan on these values to get the angle of Z.
        	angles.z = Math.toDegrees(Math.atan2(m10, m11));
        }
        return angles;
	}
	
	/**
	 * Rotates this matrix by the specified X-angle.
	 */
	public RotationMatrix rotateX(double angle){
		if(angle != 0){
			double sin = Math.sin(Math.toRadians(angle));
			double cos = Math.cos(Math.toRadians(angle));
			double t01 = m01*cos + m02*sin;
			double t02 = m01*-sin + m02*cos;
			double t11 = m11*cos + m12*sin;
			double t12 = m11*-sin + m12*cos;
			double t21 = m21*cos + m22*sin;
			double t22 = m21*-sin + m22*cos;
	        
			m01 = t01;
	        m02 = t02;
	        m11 = t11;
	        m12 = t12;
	        m21 = t21;
	        m22 = t22;
		}
		return this;
	}
	
	/**
	 * Rotates this matrix by the specified Y-angle.
	 */
	public RotationMatrix rotateY(double angle){
		if(angle != 0){
			double sin = Math.sin(Math.toRadians(angle));
			double cos = Math.cos(Math.toRadians(angle));
			double t00 = m00*cos + m02*-sin;
			double t02 = m00*sin + m02*cos;
			double t10 = m10*cos + m12*-sin;
			double t12 = m10*sin + m12*cos;
			double t20 = m20*cos + m22*-sin;
			double t22 = m20*sin + m22*cos;
			
	        m00 = t00;
	        m02 = t02;
	        m10 = t10;
	        m12 = t12;
	        m20 = t20;
	        m22 = t22;
		}
		return this;
	}
	
	/**
	 * Rotates this matrix by the specified Z-angle.
	 */
	public RotationMatrix rotateZ(double angle){
		if(angle != 0){
			double sin = Math.sin(Math.toRadians(angle));
			double cos = Math.cos(Math.toRadians(angle));
			double t00 = m00*cos + m01*sin;
			double t01 = m00*-sin + m01*cos;
			double t10 = m10*cos + m11*sin;
			double t11 = m10*-sin + m11*cos;
			double t20 = m20*cos + m21*sin;
			double t21 = m20*-sin + m21*cos;

	        m00 = t00;
	        m01 = t01;
	        m10 = t10;
	        m11 = t11;
	        m20 = t20;
	        m21 = t21;
		}
		return this;
	}
	
	/**
	 * Sets the rotation of this matrix via an axis-angle (degrees) format.
	 * Note that the x/y/z parameters MUST represent a normalized vector!
	 */
	public RotationMatrix setToAxisAngle(double x, double y, double z, double angle){
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
	 * Like {@link #setToAxisAngle(double, double, double, double)},
	 * but uses a point parameter.
	 */
	public RotationMatrix setToAxisAngle(Point3D point, double angle){
		return setToAxisAngle(point.x, point.y, point.z, angle);
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
