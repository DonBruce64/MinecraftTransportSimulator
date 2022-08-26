package minecrafttransportsimulator.baseclasses;

/**
 * 4D Matrix class for orientation operations.  This allows for
 * storing of a  rotational, translation, and scalar component
 * of orientation.  This is designed to be used with OpenGL
 * systems and positional systems where multiple transformations
 * may be performed in sequence.
 * All methods return this object for nested operations, unless
 * otherwise specified or void.
 *
 * @author don_bruce
 */
public class TransformationMatrix extends RotationMatrix {

    public double m03;

    public double m13;

    public double m23;

    public double m30;
    public double m31;
    public double m32;
    public double m33;

    public TransformationMatrix() {
        resetTransforms();
    }

    public TransformationMatrix(TransformationMatrix other) {
        this();
        set(other);
    }

    /**
     * Set this matrix to the passed-in matrix.
     */
    public TransformationMatrix set(TransformationMatrix other) {
        m00 = other.m00;
        m01 = other.m01;
        m02 = other.m02;
        m03 = other.m03;
        m10 = other.m10;
        m11 = other.m11;
        m12 = other.m12;
        m13 = other.m13;
        m20 = other.m20;
        m21 = other.m21;
        m22 = other.m22;
        m23 = other.m23;
        m30 = other.m30;
        m31 = other.m31;
        m32 = other.m32;
        m33 = other.m33;
        return this;
    }

    /**
     * Multiplies this matrix with the passed-in matrix.
     */
    public TransformationMatrix multiply(TransformationMatrix other) {
        double t00 = m00 * other.m00 + m01 * other.m10 + m02 * other.m20 + m03 * other.m30;
        double t01 = m00 * other.m01 + m01 * other.m11 + m02 * other.m21 + m03 * other.m31;
        double t02 = m00 * other.m02 + m01 * other.m12 + m02 * other.m22 + m03 * other.m32;
        double t03 = m00 * other.m03 + m01 * other.m13 + m02 * other.m23 + m03 * other.m33;

        double t10 = m10 * other.m00 + m11 * other.m10 + m12 * other.m20 + m13 * other.m30;
        double t11 = m10 * other.m01 + m11 * other.m11 + m12 * other.m21 + m13 * other.m31;
        double t12 = m10 * other.m02 + m11 * other.m12 + m12 * other.m22 + m13 * other.m32;
        double t13 = m10 * other.m03 + m11 * other.m13 + m12 * other.m23 + m13 * other.m33;

        double t20 = m20 * other.m00 + m21 * other.m10 + m22 * other.m20 + m23 * other.m30;
        double t21 = m20 * other.m01 + m21 * other.m11 + m22 * other.m21 + m23 * other.m31;
        double t22 = m20 * other.m02 + m21 * other.m12 + m22 * other.m22 + m23 * other.m32;
        double t23 = m20 * other.m03 + m21 * other.m13 + m22 * other.m23 + m23 * other.m33;

        double t30 = m30 * other.m00 + m31 * other.m10 + m32 * other.m20 + m33 * other.m30;
        double t31 = m30 * other.m01 + m31 * other.m11 + m32 * other.m21 + m33 * other.m31;
        double t32 = m30 * other.m02 + m31 * other.m12 + m32 * other.m22 + m33 * other.m32;
        double t33 = m30 * other.m03 + m31 * other.m13 + m32 * other.m23 + m33 * other.m33;

        m00 = t00;
        m01 = t01;
        m02 = t02;
        m03 = t03;
        m10 = t10;
        m11 = t11;
        m12 = t12;
        m13 = t13;
        m20 = t20;
        m21 = t21;
        m22 = t22;
        m23 = t23;
        m30 = t30;
        m31 = t31;
        m32 = t32;
        m33 = t33;
        return this;
    }

    /**
     * Resets this matrix transform to default.  This should be done
     * prior to applying any transforms on it.
     */
    public TransformationMatrix resetTransforms() {
        m00 = 1.0;
        m01 = 0.0;
        m02 = 0.0;
        m03 = 0.0;

        m10 = 0.0;
        m11 = 1.0;
        m12 = 0.0;
        m13 = 0.0;

        m20 = 0.0;
        m21 = 0.0;
        m22 = 1.0;
        m23 = 0.0;

        m30 = 0.0;
        m31 = 0.0;
        m32 = 0.0;
        m33 = 1.0;
        return this;
    }

    /**
     * Applies a translation transform to this transform.
     */
    public TransformationMatrix applyTranslation(double x, double y, double z) {
        m03 += m00 * x + m01 * y + m02 * z;
        m13 += m10 * x + m11 * y + m12 * z;
        m23 += m20 * x + m21 * y + m22 * z;
        return this;
    }

    /**
     * Like {@link #applyTranslation(double, double, double)}, just with a point object.
     */
    public TransformationMatrix applyTranslation(Point3D translation) {
        applyTranslation(translation.x, translation.y, translation.z);
        return this;
    }

    /**
     * Like {@link #applyTranslation(double, double, double)}, just with a point object inverted.
     */
    public TransformationMatrix applyInvertedTranslation(Point3D translation) {
        applyTranslation(-translation.x, -translation.y, -translation.z);
        return this;
    }

    /**
     * Sets the translation of this matrix to the passed-in coordinates.
     * Does not modify the rotational or scalar components.
     */
    public TransformationMatrix setTranslation(double x, double y, double z) {
        m03 = x;
        m13 = y;
        m23 = z;
        return this;
    }

    /**
     * Like {@link #setTranslation(double, double, double)}, just with a point object.
     */
    public TransformationMatrix setTranslation(Point3D translation) {
        m03 = translation.x;
        m13 = translation.y;
        m23 = translation.z;
        return this;
    }

    /**
     * Applies a the rotation transform to this transform.
     * Note that if there is a scaling component in this transform, it will
     * be lumped in to this transformation.  As such, it is recommended to
     * call this method LAST in operations.
     */
    public TransformationMatrix applyRotation(RotationMatrix rotation) {
        multiply(rotation);
        return this;
    }

    /**
     * Sets the rotation of this transform to the passed-in rotation.
     * This overwrites any scaling settings and sets them back to 1.0.
     * Does not modify the translational component of this transform.
     */
    public TransformationMatrix setRotation(RotationMatrix rotation) {
        set(rotation);
        return this;
    }

    /**
     * Applies a scaling transform to this object.
     * This will apply on top of any rotation or translation transform that was performed.
     */
    public TransformationMatrix applyScaling(double x, double y, double z) {
        m00 *= x;
        m01 *= y;
        m02 *= z;
        m10 *= x;
        m11 *= y;
        m12 *= z;
        m20 *= x;
        m21 *= y;
        m22 *= z;
        m30 *= x;
        m31 *= y;
        m32 *= z;
        return this;
    }

    /**
     * Like {@link #applyScaling(double, double, double)}, just with a point object.
     */
    public TransformationMatrix applyScaling(Point3D scaling) {
        return applyScaling(scaling.x, scaling.y, scaling.z);
    }

    /**
     * Transforms the passed-in point to align with this transformation matrix.
     */
    public Point3D transform(Point3D point) {
        double tx = m00 * point.x + m01 * point.y + m02 * point.z + m03;
        double ty = m10 * point.x + m11 * point.y + m12 * point.z + m13;
        point.z = m20 * point.x + m21 * point.y + m22 * point.z + m23;
        point.x = tx;
        point.y = ty;
        return point;
    }
}
