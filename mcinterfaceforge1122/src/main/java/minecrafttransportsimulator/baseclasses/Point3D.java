package minecrafttransportsimulator.baseclasses;

/**
 * Basic 3D point class.  Allows for saving of positions in a less recourse-heavy
 * format than Minecraft's vectors.  This class is mutable to allow
 * the point to change, cause we don't need to create a new point every time we
 * move a thing.  As this point can be used for vectors, methods exist for vector
 * operations such as dot product, cross product, and rotation.
 * Note that all methods return this object for nested operations, unless otherwise
 * specified.
 *
 * @author don_bruce
 */
public class Point3D {

    public double x;
    public double y;
    public double z;

    public Point3D() {
        this(0, 0, 0);
    }

    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object object) {
        //TODO see if this is needed anymore, or if FPEs aren't a thing.
        if (object instanceof Point3D) {
            Point3D otherPoint = (Point3D) object;
            return (float) x == (float) otherPoint.x && (float) y == (float) otherPoint.y && (float) z == (float) otherPoint.z;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "[X:" + x + ", Y:" + y + ", Z:" + z + "]";
    }

    /**
     * Sets the point to the passed-in values.
     */
    public Point3D set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Sets the point to the passed-in point.
     */
    public Point3D set(Point3D point) {
        this.x = point.x;
        this.y = point.y;
        this.z = point.z;
        return this;
    }

    /**
     * Adds the passed-in values to the point.
     */
    public Point3D add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    /**
     * Adds the passed-in point's values to this point.
     */
    public Point3D add(Point3D point) {
        this.x += point.x;
        this.y += point.y;
        this.z += point.z;
        return this;
    }

    /**
     * Adds the scaled value of the scale multiplied by the
     * passed-in vector to this point.  This is useful
     * if you don't want to modify the vector, but want
     * to translate along it's path.
     */
    public Point3D addScaled(Point3D point, double scale) {
        this.x += point.x * scale;
        this.y += point.y * scale;
        this.z += point.z * scale;
        return this;
    }

    /**
     * Subtracts the passed-in point's values from this point.
     */
    public Point3D subtract(Point3D point) {
        this.x -= point.x;
        this.y -= point.y;
        this.z -= point.z;
        return this;
    }

    /**
     * Multiplies this point by the passed-in point, via the X/Y/Z component multiplication.
     */
    public Point3D multiply(Point3D point) {
        this.x *= point.x;
        this.y *= point.y;
        this.z *= point.z;
        return this;
    }

    /**
     * Scales all values of this point by the passed-in factor.
     */
    public Point3D scale(double scale) {
        this.x *= scale;
        this.y *= scale;
        this.z *= scale;
        return this;
    }

    /**
     * Sets the point to the interpolation between itself, and the passed-in point,
     * with the distance passed-in.
     */
    public Point3D interpolate(Point3D point, double distance) {
        this.x += (point.x - x) * distance;
        this.y += (point.y - y) * distance;
        this.z += (point.z - z) * distance;
        return this;
    }

    /**
     * Inverts the sign on this point.  This should be used instead of multiplying
     * by -1 as it's quicker and more accurate.
     */
    public Point3D invert() {
        this.x = -x;
        this.y = -y;
        this.z = -z;
        return this;
    }

    /**
     * Returns the distance between this point and the passed-in point.
     */
    public double distanceTo(Point3D point) {
        double deltaX = point.x - this.x;
        double deltaY = point.y - this.y;
        double deltaZ = point.z - this.z;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    /**
     * Returns true if the distance between this point and the passed-in point
     * is less than the distance specified.  This is an optimized method of
     * {@link #distanceTo(Point3D)} as it doesn't do square root calls.
     */
    public boolean isDistanceToCloserThan(Point3D point, double distance) {
        double deltaX = point.x - this.x;
        double deltaY = point.y - this.y;
        double deltaZ = point.z - this.z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ < distance * distance;
    }

    /**
     * Returns true if the distance between this point and the first passed-in
     * point is less than the distance between this point and the second passed-in
     * point.  This is an optimized method to check which of two points are closer
     * versus using {@link #distanceTo(Point3D)} as it doesn't do square root calls.
     */
    public boolean isFirstCloserThanSecond(Point3D first, Point3D second) {
        double deltaX = first.x - this.x;
        double deltaY = first.y - this.y;
        double deltaZ = first.z - this.z;
        double firstDistance = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

        deltaX = second.x - this.x;
        deltaY = second.y - this.y;
        deltaZ = second.z - this.z;
        return firstDistance < deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    /**
     * Returns the dot product between this point and the passed-in point.
     * Note: if normalized is true, then this method assumes both points are
     * normalized and clamps between-1.0 and 1.0 to prevent out-of-bounds errors
     * when the return value is used in trigonometric functions, even if the math
     * doesn't strictly result in that value.  If the parameter is false, then
     * the actual dot product is returned.
     */
    public double dotProduct(Point3D point, boolean normalized) {
        if (normalized) {
            double dotProduct = this.x * point.x + this.y * point.y + this.z * point.z;
            if (dotProduct > 1) {
                return 1;
            } else if (dotProduct < -1) {
                return -1;
            } else {
                return dotProduct;
            }
        } else {
            return this.x * point.x + this.y * point.y + this.z * point.z;
        }
    }

    /**
     * Returns the cross product between this point and the passed-in point.
     * Return value is a new point that is the cross product of the object
     * this was invoked on, and the passed-in object.  Neither object is
     * modified by this operation.
     */
    public Point3D crossProduct(Point3D point) {
        return new Point3D(this.y * point.z - this.z * point.y, this.z * point.x - this.x * point.z, this.x * point.y - this.y * point.x);
    }

    /**
     * Returns the length of this point as if it was a vector.
     */
    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Normalizes this point to be a unit vector.
     */
    public Point3D normalize() {
        double length = length();
        if (length > 1.0E-8D) {
            x /= length;
            y /= length;
            z /= length;
        }
        return this;
    }

    /**
     * Returns the difference between the passed-in value and this point's Y value, between
     * the range of -180 to 180.  Placed here as Y is frequently used in yaw angle for in-game
     * entities and needs to be clamped to this domain for calculations.
     */
    public double getClampedYDelta(double otherY) {
        double deltaYaw = this.y - otherY;
        while (deltaYaw > 180) {
            deltaYaw -= 360;
        }
        while (deltaYaw < -180) {
            deltaYaw += 360;
        }
        return deltaYaw;
    }

    /**
     * Sets this point to the angle values defined by it.  If the point is not normalized,
     * pass in true to the boolean to perform this operation.
     * Note that since there is no "roll" for vectors, the z-value will always be 0.
     * Returns the called object for nested operations.
     */
    public Point3D getAngles(boolean normalize) {
        if (normalize) {
            normalize();
        }
        double theta = Math.asin(y);
        double phi = Math.atan2(x, z);
        set(-Math.toDegrees(theta), Math.toDegrees(phi), 0);
        return this;
    }

    /**
     * Sets this point to the angles required to rotate a vector between the two passed-in matrixes.
     * This will have a 0-component for z-rotation.  Useful if you want to adjust something in yaw/pitch
     * format that doesn't easily convert to matrix format.
     * Returns the called object for nested operations.
     */
    public Point3D computeVectorAngles(RotationMatrix start, RotationMatrix end) {
        return set(0, 0, 1).rotate(start).reOrigin(end).getAngles(false);
    }

    /**
     * Clamps the values to this point from -180 to 180 as if they were angles in that domain.
     * Returns the called object for nested operations.
     */
    public Point3D clamp180() {
        while (x > 180)
            x -= 360;
        while (x < -180)
            x += 360;
        while (y > 180)
            y -= 360;
        while (y < -180)
            y += 360;
        while (z > 180)
            z -= 360;
        while (z < -180)
            z += 360;
        return this;
    }

    /**
     * Returns a copy of this point as a new object.
     */
    public Point3D copy() {
        return new Point3D(this.x, this.y, this.z);
    }

    /**
     * Returns true if this point is 0,0,0.
     */
    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }

    /**
     * Returns a new point with the x value equal to the second parameter, provided the X value
     * is between this point and the passed-in point, and the passed-in point's x-value is not
     * equal to this point's x-value.  If such conditions are not satisfied, null is returned.
     */
    public Point3D getIntermediateWithXValue(Point3D endPoint, double targetX) {
        Point3D delta = endPoint.copy().subtract(this);
        if (delta.x * delta.x < 1.0E-7D) {
            //Point delta is 0, so there's no difference here.
            return null;
        } else {
            //Return point as a factored-percentage of total length.
            double factor = (targetX - this.x) / delta.x;
            return factor >= 0.0D && factor <= 1.0D ? delta.scale(factor).add(this) : null;
        }
    }

    /**
     * Returns a new point with the y value equal to the second parameter, provided the Y value
     * is between this point and the passed-in point, and the passed-in point's y-value is not
     * equal to this point's y-value.  If such conditions are not satisfied, null is returned.
     */
    public Point3D getIntermediateWithYValue(Point3D endPoint, double targetY) {
        Point3D delta = endPoint.copy().subtract(this);
        if (delta.y * delta.y < 1.0E-7D) {
            //Point delta is 0, so there's no difference here.
            return null;
        } else {
            //Return point as a factored-percentage of total length.
            double factor = (targetY - this.y) / delta.y;
            return factor >= 0.0D && factor <= 1.0D ? delta.scale(factor).add(this) : null;
        }
    }

    /**
     * Returns a new point with the z value equal to the second parameter, provided the Z value
     * is between this point and the passed-in point, and the passed-in point's z-value is not
     * equal to this point's z-value.  If such conditions are not satisfied, null is returned.
     */
    public Point3D getIntermediateWithZValue(Point3D endPoint, double targetZ) {
        Point3D delta = endPoint.copy().subtract(this);
        if (delta.z * delta.z < 1.0E-7D) {
            //Point delta is 0, so there's no difference here.
            return null;
        } else {
            //Return point as a factored-percentage of total length.
            double factor = (targetZ - this.z) / delta.z;
            return factor >= 0.0D && factor <= 1.0D ? delta.scale(factor).add(this) : null;
        }
    }

    /**
     * Forwarded function to {@link RotationMatrix#rotate(Point3D)} for nested calling.
     */
    public Point3D rotate(RotationMatrix matrix) {
        return matrix.rotate(this);
    }

    /**
     * Forwarded function to {@link RotationMatrix#reOrigin(Point3D)} for nested calling.
     */
    public Point3D reOrigin(RotationMatrix matrix) {
        return matrix.reOrigin(this);
    }

    /**
     * Forwarded function to {@link TransformationMatrix#transform(Point3D)} for nested calling.
     */
    public Point3D transform(TransformationMatrix matrix) {
        return matrix.transform(this);
    }
}
