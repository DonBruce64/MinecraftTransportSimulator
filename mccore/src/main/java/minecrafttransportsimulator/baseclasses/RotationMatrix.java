package minecrafttransportsimulator.baseclasses;

/**
 * 3D Matrix class for rotation operations.  This should be used
 * instead of Euler angles for all things in the code.
 * All methods return this object for nested operations, unless
 * otherwise specified or void.
 *
 * @author don_bruce
 */
public class RotationMatrix {
    public double m00;
    public double m01;
    public double m02;
    public double m10;
    public double m11;
    public double m12;
    public double m20;
    public double m21;
    public double m22;
    /**
     * The current Euler angles that make up this matrix.  These are used to create the rotation
     * matrix on the first rotation operation.  Subsequent modifications to this variable will
     * re-create the matrix if a rotation operation is called.  However, they will NOT be updated
     * when matrix multiplication operations are performed.  The idea is to use them to create the
     * initial matrix, then multiply/transform as applicable.  If you need to get them back out of the
     * matrix, call {@link #convertToAngles()} which will convert the matrix back into the angles.
     * If you change these and want the matrix state to update to reflect them, call {@link #updateToAngles()}
     */
    public final Point3D angles = new Point3D();
    private final Point3D lastAngles = new Point3D();

    public RotationMatrix() {
        setToZero();
    }

    /**
     * Sets this matrix to the passed-in matrix.
     */
    public RotationMatrix set(RotationMatrix other) {
        angles.set(other.angles);
        lastAngles.set(other.lastAngles);
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
    public RotationMatrix multiply(RotationMatrix other) {
        double t00 = m00 * other.m00 + m01 * other.m10 + m02 * other.m20;
        double t01 = m00 * other.m01 + m01 * other.m11 + m02 * other.m21;
        double t02 = m00 * other.m02 + m01 * other.m12 + m02 * other.m22;

        double t10 = m10 * other.m00 + m11 * other.m10 + m12 * other.m20;
        double t11 = m10 * other.m01 + m11 * other.m11 + m12 * other.m21;
        double t12 = m10 * other.m02 + m11 * other.m12 + m12 * other.m22;

        double t20 = m20 * other.m00 + m21 * other.m10 + m22 * other.m20;
        double t21 = m20 * other.m01 + m21 * other.m11 + m22 * other.m21;
        double t22 = m20 * other.m02 + m21 * other.m12 + m22 * other.m22;

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
     * Multiplies this matrix by the transpose of the passed-in matrix.
     * This essentially "un-does" the passed-in rotation from this rotation.
     * Does not actually set the passed-in matrix to its transpose.
     */
    public RotationMatrix multiplyTranspose(RotationMatrix other) {
        double t00 = m00 * other.m00 + m01 * other.m01 + m02 * other.m02;
        double t01 = m00 * other.m10 + m01 * other.m11 + m02 * other.m12;
        double t02 = m00 * other.m20 + m01 * other.m21 + m02 * other.m22;

        double t10 = m10 * other.m00 + m11 * other.m01 + m12 * other.m02;
        double t11 = m10 * other.m10 + m11 * other.m11 + m12 * other.m12;
        double t12 = m10 * other.m20 + m11 * other.m21 + m12 * other.m22;

        double t20 = m20 * other.m00 + m21 * other.m01 + m22 * other.m02;
        double t21 = m20 * other.m10 + m21 * other.m11 + m22 * other.m12;
        double t22 = m20 * other.m20 + m21 * other.m21 + m22 * other.m22;

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
    public RotationMatrix setToZero() {
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

    /**
     * Sets the rotation of this matrix to the passed-in angles.
     */
    public RotationMatrix setToAngles(Point3D angles) {
        this.angles.set(angles);
        updateToAngles();
        return this;
    }

    /**
     * Sets the rotation of this matrix to correspond to one that
     * would result in the passed-in vector being rotated to its orientation.
     * This also updates {@link #angles} to match the vector's angles.
     */
    public RotationMatrix setToVector(Point3D vector, boolean normalize) {
        angles.set(vector);
        if (normalize) {
            angles.normalize();
        }
        //X-rotation (pitch), is the -aSin of the Y coord.
        //This is because normally the Y coord is 0.
        //When Y coord is positive, then there is pitch upward (counter-clockwise).
        //When Y coord is negative, then there is pitch downward (clockwise).
        double pitch = -Math.toDegrees(Math.asin(angles.y));

        //Y-rotation (yaw), is the aTan of the planar components x and z.
        //Normally aTan expects y, and x.  But we need to give it x and z due to our coordinate definition.
        //We can also set x to pich, as we're done with it now.
        angles.y = Math.toDegrees(Math.atan2(angles.x, angles.z));
        angles.x = pitch;

        //Z-rotation (roll) is always 0.  Vectors can't represent roll.
        angles.z = 0;
        updateToAngles();
        return this;
    }

    /**
     * Returns the angles that make up this matrix.  Note: these may not be the
     * same as the value in the {@link #angles} parameter, as rotation
     * matrix conversion has multiple solutions.  In general, these shouldn't be
     * used for calculations and rather should just be used to store the state of
     * the matrix for later use where the orientation of the matrix matters, but
     * the actual angles don't.  Note that in addition to returning the angles,
     * the value of {@link #angles} is set.
     */
    public Point3D convertToAngles() {
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
        if (m12 == -1) {
            //Gimbal lock with pitch at -90 (down).
            angles.z = 0;
        } else if (m12 == 1) {
            //Gimbal lock with pitch at 90 (up).
            angles.z = 0;
        } else {
            //We know that element m10 is cosX*sinZ, and m11 is cosX*cosZ.
            //m10/m11 results in sinZ/cosZ, which is the tangent function.
            //Therefore, we can do aTan on these values to get the angle of Z.
            angles.z = Math.toDegrees(Math.atan2(m10, m11));
        }
        return angles;
    }

    /**
     * Like {@link #convertToAngles()}, but this method doesn't calculate the angles.
     * Instead, it prevents their calculation and checks on future {@link #rotate(Point3D)}
     * and {@link #reOrigin(Point3D)} calls.  This can be done if the angles might 
     * be not current, but this won't matter for future calls.
     */
    public void bypassAngles() {
        angles.set(0, 0, 0);
        lastAngles.set(angles);
    }

    /**
     * Rotates this matrix by the specified X-angle.
     */
    public RotationMatrix rotateX(double angle) {
        if (angle != 0) {
            double sin = Math.sin(Math.toRadians(angle));
            double cos = Math.cos(Math.toRadians(angle));
            double t01 = m01 * cos + m02 * sin;
            double t02 = m01 * -sin + m02 * cos;
            double t11 = m11 * cos + m12 * sin;
            double t12 = m11 * -sin + m12 * cos;
            double t21 = m21 * cos + m22 * sin;
            double t22 = m21 * -sin + m22 * cos;

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
    public RotationMatrix rotateY(double angle) {
        if (angle != 0) {
            double sin = Math.sin(Math.toRadians(angle));
            double cos = Math.cos(Math.toRadians(angle));
            double t00 = m00 * cos + m02 * -sin;
            double t02 = m00 * sin + m02 * cos;
            double t10 = m10 * cos + m12 * -sin;
            double t12 = m10 * sin + m12 * cos;
            double t20 = m20 * cos + m22 * -sin;
            double t22 = m20 * sin + m22 * cos;

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
    public RotationMatrix rotateZ(double angle) {
        if (angle != 0) {
            double sin = Math.sin(Math.toRadians(angle));
            double cos = Math.cos(Math.toRadians(angle));
            double t00 = m00 * cos + m01 * sin;
            double t01 = m00 * -sin + m01 * cos;
            double t10 = m10 * cos + m11 * sin;
            double t11 = m10 * -sin + m11 * cos;
            double t20 = m20 * cos + m21 * sin;
            double t21 = m20 * -sin + m21 * cos;

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
    public RotationMatrix setToAxisAngle(double x, double y, double z, double angle) {
        if (angle != 0) {
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
        } else {
            setToZero();
        }
        return this;
    }

    /**
     * Like {@link #setToAxisAngle(double, double, double, double)},
     * but uses a point parameter.
     */
    public RotationMatrix setToAxisAngle(Point3D point, double angle) {
        return setToAxisAngle(point.x, point.y, point.z, angle);
    }

    /**
     * Rotates the point about this matrix.
     */
    public Point3D rotate(Point3D point) {
        if (!lastAngles.equals(angles)) {
            updateToAngles();
        }
        double tx = m00 * point.x + m01 * point.y + m02 * point.z;
        double ty = m10 * point.x + m11 * point.y + m12 * point.z;
        point.z = m20 * point.x + m21 * point.y + m22 * point.z;
        point.x = tx;
        point.y = ty;
        return point;
    }

    /**
     * Aligns the passed-in point to the matrix origin.  Essentially, this leaves
     * the point in its current position, but changes the coordinate system
     * to be aligned to the coordinate system of this matrix.
     * More specifically, this is an inverted rotation by the transpose of the matrix.
     */
    public Point3D reOrigin(Point3D point) {
        if (!lastAngles.equals(angles)) {
            updateToAngles();
        }
        double tx = m00 * point.x + m10 * point.y + m20 * point.z;
        double ty = m01 * point.x + m11 * point.y + m21 * point.z;
        point.z = m02 * point.x + m12 * point.y + m22 * point.z;
        point.x = tx;
        point.y = ty;
        return point;
    }

    /**
     * Updates this rotation of this matrix to match it to the current
     * value of its internal {@link #angles} variable.  This is an internal
     * function and is normally only called prior to rotation operations
     * (as needed) as it does some rather lengthy calculations.  However,
     * it may be called any time the angles have changed and you want
     * this matrix to reflect those changes.
     */
    public RotationMatrix updateToAngles() {
        setToZero();
        rotateY(angles.y);
        rotateX(angles.x);
        rotateZ(angles.z);
        lastAngles.set(angles);
        return this;
    }

    /**
     * Interpolates between the two passed-in matrixes, storing the result in this matrix.
     * Note that this matrix is told to bypass rotation-angle checks after this
     * call, since interpolation changes the matrix elements, not the angles,
     * and it is assumed that rotation about this matrix will be more
     * probable than using the actual angles for some operation.
     */
    public void interploate(RotationMatrix start, RotationMatrix end, double delta) {
        //Convert start and end matrix to quaternions.
        double quatStartw = Math.sqrt(1 + start.m00 + start.m11 + start.m22) / 2D;
        double quatEndw = Math.sqrt(1 + end.m00 + end.m11 + end.m22) / 2D;
        if (quatStartw < 1.0e-6 && quatEndw < 1.0e-6) {
            //Quaternions are axis-aligned.  If we continue, we get NaNed or get funny results.
            //TODO find better maths to handle this condition rather than just not interpolating.
            this.set(start);
            return;
        }

        double quatStarti = 1 / (4 * quatStartw) * (start.m21 - start.m12);
        double quatStartj = 1 / (4 * quatStartw) * (start.m02 - start.m20);
        double quatStartk = 1 / (4 * quatStartw) * (start.m10 - start.m01);

        double quatEndi = 1 / (4 * quatEndw) * (end.m21 - end.m12);
        double quatEndj = 1 / (4 * quatEndw) * (end.m02 - end.m20);
        double quatEndk = 1 / (4 * quatEndw) * (end.m10 - end.m01);

        //Use quternion SLERP to get interpolated quaternion.
        //Copied from javax.vecmath.Quat4d
        double dotProduct = quatStarti * quatEndi + quatStartj * quatEndj + quatStartk * quatEndk + quatStartw * quatEndw;
        if (dotProduct < 0) {
            dotProduct = -dotProduct;
            quatEndi = -quatEndi;
            quatEndj = -quatEndj;
            quatEndk = -quatEndk;
            quatEndw = -quatEndw;
        }

        double segment1;
        double segment2;
        //Need to not do interpolation at really small values, as it leads to issues.
        if (1.0 - dotProduct > 1.0e-6) {
            double angle = Math.acos(dotProduct);
            double sinAngle = Math.sin(angle);
            segment1 = Math.sin((1.0 - delta) * angle) / sinAngle;
            segment2 = Math.sin(delta * angle) / sinAngle;
        } else {
            segment1 = 1.0 - delta;
            segment2 = delta;
        }
        double quatNetr = segment1 * quatStartw + segment2 * quatEndw;
        double quatNeti = segment1 * quatStarti + segment2 * quatEndi;
        double quatNetj = segment1 * quatStartj + segment2 * quatEndj;
        double quatNetk = segment1 * quatStartk + segment2 * quatEndk;

        //Now convert this quaternion back into this matrix.
        m00 = (1.0 - 2.0 * quatNetj * quatNetj - 2.0 * quatNetk * quatNetk);
        m10 = (2.0 * (quatNeti * quatNetj + quatNetr * quatNetk));
        m20 = (2.0 * (quatNeti * quatNetk - quatNetr * quatNetj));

        m01 = (2.0 * (quatNeti * quatNetj - quatNetr * quatNetk));
        m11 = (1.0 - 2.0 * quatNeti * quatNeti - 2.0 * quatNetk * quatNetk);
        m21 = (2.0 * (quatNetj * quatNetk + quatNetr * quatNeti));

        m02 = (2.0 * (quatNeti * quatNetk + quatNetr * quatNetj));
        m12 = (2.0 * (quatNetj * quatNetk - quatNetr * quatNeti));
        m22 = (1.0 - 2.0 * quatNeti * quatNeti - 2.0 * quatNetj * quatNetj);

        bypassAngles();
    }
}
