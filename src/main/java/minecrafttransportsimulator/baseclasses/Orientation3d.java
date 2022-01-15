package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**Basic Orientation class.  Stores an axis for direction, and rotation about that axis as a double.
 * Maintains a Quaternion of these, which allows it to be multiplied with other objects to 
 * produce a final rotation.  Used to store the orientation of objects and render them without the use 
 * of Euler Angles.  This allow for multiple axis of rotation to be applied and prevents odd bugs with 
 * interpolation due to sign differences.  Note that you should NEVER modify axis, and should always modify
 * rotation via {@link #setRotation(double)} to ensure proper state is maintained.
 *
 * @author don_bruce
 */
public class Orientation3d{
	private static final Orientation3d mutableOrientationX = new Orientation3d(new Point3d(1, 0, 0), 0);
	private static final Orientation3d mutableOrientationY = new Orientation3d(new Point3d(0, 1, 0), 0);
	private static final Orientation3d mutableOrientationZ = new Orientation3d(new Point3d(0, 0, 1), 0);
	private static final Orientation3d mutableOrientationPoint = new Orientation3d(new Point3d(0, 0, 0), 0);
	private static final Orientation3d interpolatedOrientation = new Orientation3d(new Point3d(0, 0, 1), 0);
	private final Point3d mutablePoint = new Point3d();
	
	public final Point3d axis;
	public double rotation;
	
	private double w;
	private double x;
	private double y;
	private double z;
	
	private final Point3d angles = new Point3d();
	
	public Orientation3d(Orientation3d other){
		this(other.axis, other.rotation);
	}
	
	public Orientation3d(Point3d angles){
		this.axis = new Point3d();
		setAngles(angles);
	}
	
	public Orientation3d(Point3d axis, double rotation){
		this.axis = axis.copy();
		this.rotation = rotation;
		updateQuaternion(false);
	}
	
	public Orientation3d(WrapperNBT data){
		this(data.getPoint3d("angles"));
	}
	
	@Override
	public String toString(){
		return "[" + w + ", " + x + ", " + y + ", " + z + "; " + axis.toString() + ", " + String.valueOf(rotation) + "]";
	}
	
	/**
	 * Updates the Quaternion.  This should be done any time rotation or quaternion parameters XYZW changes.
	 * If the boolean parameter is set, then the quanternion values will be used to calculate the axis.
	 * If it is false, then they will be calculated from the axis.
	 */
	private void updateQuaternion(boolean useQuaternionValues){
		if(useQuaternionValues){
			double scale = Math.sqrt(1 - w*w);
			if(scale == 0){
				axis.x = 0;
				axis.y = 0;
				axis.z = 1;
			}else{
				axis.x = x/scale;
				axis.y = y/scale;
				axis.z = z/scale;	
			}
			rotation = Math.toDegrees(Math.acos(w))*2;
		}else{
			double rotationInRad = Math.toRadians(rotation/2);
			double sinRad = Math.sin(rotationInRad);
			double cosRad = Math.cos(rotationInRad);
			w = cosRad;
			x = axis.x*sinRad;
			y = axis.y*sinRad;
			z = axis.z*sinRad;
		}
	}
	
	/**
	 * Sets the rotation for this orientation, updating values as appropriate.
	 * Internal method only for MXY calcs.
	 */
	public Orientation3d setRotation(double newRotation){
		rotation = newRotation;
		updateQuaternion(false);
		return this;
	}
	
	/**
	 * Sets this orientation to the parameters of the passed-in orientation.
	 * Returns this object for nested operations.
	 */
	public Orientation3d setTo(Orientation3d other){
		this.axis.setTo(other.axis);
		this.rotation = other.rotation;
		this.w = other.w;
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
		return this;
	}
	
	/**
	 * Sets this orientation to the passed-in axis/rotation representation.
	 * Returns this object for nested operations.
	 */
	public Orientation3d setTo(Point3d axis, double rotation){
		this.axis.setTo(axis);
		this.rotation = rotation;
		updateQuaternion(false);
		return this;
	}
	
	/**
	 * Sets the angles for this orientation, updating values as appropriate.
	 * Returns this object for nested operations.
	 */
	public Orientation3d setAngles(Point3d angles){
		this.angles.setTo(angles);
		if(angles.isZero()){
			//Skip maths and just reset us.
			w = 1;
			x = 0;
			y = 0;
			z = 0;
			rotation = 0;
		}else{
			//Create component orientation, then consolidate into just one.
			mutableOrientationX.setRotation(angles.x);
			mutableOrientationY.setRotation(angles.y);
			mutableOrientationZ.setRotation(angles.z);
			this.setTo(mutableOrientationY).multiplyBy(mutableOrientationX).multiplyBy(mutableOrientationZ);
			updateQuaternion(true);
		}
		return this;
	}
	
	/**
	 * Rotates this orientation about it's current X-axis reference.
	 * Returns this object for nested operations.
	 */
	public Orientation3d rotateX(double addedRotation){
		angles.x += addedRotation;
		return multiplyBy(mutableOrientationX.setRotation(addedRotation));
	}
	
	/**
	 * Rotates this orientation about it's current Y-axis reference.
	 * Returns this object for nested operations.
	 */
	public Orientation3d rotateY(double addedRotation){
		angles.y += addedRotation;
		return multiplyBy(mutableOrientationY.setRotation(addedRotation));
	}
	
	/**
	 * Rotates this orientation about it's current Z-axis reference.
	 * Returns this object for nested operations.
	 */
	public Orientation3d rotateZ(double addedRotation){
		angles.z += addedRotation;
		return multiplyBy(mutableOrientationZ.setRotation(addedRotation));
	}
	
	/**
	 * Multiplies this orientation by the passed-in orientation, storing the result in this orientation.
	 * Updates this orientation as part of this operation if the passed-in boolean is true.
	 * Returns this orientation for nested operations.  For this method, the current orientation will be on
	 * the left, and the orientation to multiply by will be on the right.  
	 * Returns this object for nested operations.
	 */
	public Orientation3d multiplyBy(Orientation3d multiplyBy){
		//Check if we even need to multiply.  If the one to multiply by has a rotation 0, it's a null transform.
		if(multiplyBy.rotation != 0){
			//If we are 0-rotation, we can just set ourselves to the multipied orientation.
			if(rotation == 0){
				return setTo(multiplyBy);
			}else{
				//Need to put these into variables as we use all four WXYZ params.
				double mW = w * multiplyBy.w - x * multiplyBy.x - y * multiplyBy.y - z * multiplyBy.z;
				double mX = w * multiplyBy.x + x * multiplyBy.w + y * multiplyBy.z - z * multiplyBy.y;
				double mY = w * multiplyBy.y - x * multiplyBy.z + y * multiplyBy.w + z * multiplyBy.x;
				double mZ = w * multiplyBy.z + x * multiplyBy.y - y * multiplyBy.x + z * multiplyBy.w;
				
				this.x = mX;
				this.y = mY;
				this.z = mZ;
				this.w = mW;
				updateQuaternion(true);
			}
		}
		return this;
	}
	
	/**
	 * Interpolate between the current orientation and the passed-in orientation.
	 * A mutable object is returned.  This object will be re-used on subsequent calls,
	 * so do not keep a reference to it.  The only exception to this rule is if the
	 * interpolation function determines that there is no delta between the orientations.
	 * In this case, the called orientation is returned.
	 */
	public Orientation3d getInterpolated(Orientation3d other, float partialTicks){
		//Don't do interpolation if we don't need to because we are the same.
		if(w != other.w || x != other.x || y != other.y || z != other.z){
			double cosHalfTheta = w * other.w + x * other.x + y * other.y + z * other.z;
			double sinHalfTheta = Math.sqrt(1.0 - cosHalfTheta*cosHalfTheta);
			if(cosHalfTheta < 1){
				//Actual change found.
				double halfThetaRad = Math.acos(cosHalfTheta);
				double ratioA = Math.sin((1 - partialTicks) * halfThetaRad) / sinHalfTheta;
				double ratioB = Math.sin(partialTicks * halfThetaRad) / sinHalfTheta;
				
				//Need to put these into variables as we use all four WXYZ params.
				interpolatedOrientation.w = w * ratioA + other.w * ratioB;
				interpolatedOrientation.x = x * ratioA + other.x * ratioB;
				interpolatedOrientation.y = y * ratioA + other.y * ratioB;
				interpolatedOrientation.z = z * ratioA + other.z * ratioB;
				interpolatedOrientation.updateQuaternion(true);
				return interpolatedOrientation;
			}
		}
		return this;
	}
	
	/**
	 * Rotates the passed-in point, storing the result in the point itself.
	 * Returns the storing object for nested operations.
	 */
	public Point3d rotatePoint(Point3d rotate){
		//Check if we even need to rotate.  If we have a rotation 0, or the point is zero, it's a null transform.
		if(rotation != 0 || !rotate.isZero()){
			//Make the point a quaternion.
			mutableOrientationPoint.w = 0;
			mutableOrientationPoint.x = rotate.x;
			mutableOrientationPoint.y = rotate.y;
			mutableOrientationPoint.z = rotate.z;
			
			//Multiply us and the mutable together
			//double mW = w * mutableOrientationPoint.w - x * mutableOrientationPoint.x - y * mutableOrientationPoint.y - z * mutableOrientationPoint.z;
			//double mX = w * mutableOrientationPoint.x + x * mutableOrientationPoint.w + y * mutableOrientationPoint.z - z * mutableOrientationPoint.y;
			//double mY = w * mutableOrientationPoint.y - x * mutableOrientationPoint.z + y * mutableOrientationPoint.w + z * mutableOrientationPoint.x;
			//double mZ = w * mutableOrientationPoint.z + x * mutableOrientationPoint.y - y * mutableOrientationPoint.x + z * mutableOrientationPoint.w;
			double mW = - x * rotate.x - y * rotate.y - z * rotate.z;
			double mX = w * rotate.x + y * rotate.z - z * rotate.y;
			double mY = w * rotate.y - x * rotate.z + z * rotate.x;
			double mZ = w * rotate.z + x * rotate.y - y * rotate.x;
			
			//Now multiply the result and the conjugate.
			double mW2 = mW * w - mX * -x - mY * -y - mZ * -z;
			double mX2 = mW * -x + mX * w + mY * -z - mZ * -y;
			double mY2 = mW * -y - mX * -z + mY * w + mZ * -x;
			double mZ2 = mW * -z + mX * -y - mY * -x + mZ * w;
			
			//Now store this result in the point.
			rotate.x = mX2;
			rotate.y = mY2;
			rotate.z = mZ2;
			//Next, multiply the result by the conjugate of the quaternion.
			//We don't need to calculate the W parameter here as it has to be 0.
			//storeIn.w = mW * w - mX * -x - mY * -y - mZ * -z;
			//rotate.x = mW * -x + mX * w + mY * -z - mZ * -y;
			//rotate.y = mW * -y - mX * -z + mY * w + mZ * -x;
			//rotate.z = mW * -z + mX * -y - mY * -x + mZ * w;
			
			/*
			//First multiply the point by the quaternion.  Point has imaginary w param of 0.
			
			
			*/
		}
		return rotate;
	}
	
	/**
	 * Similar to {@link #rotatePoint(Point3d)}, but the result
	 * is added to the second parameter and the first parameter is un-changed.
	 * Returns the second parameter for nested calls.
	 */
	public Point3d rotateAndAddTo(Point3d rotate, Point3d addTo){
		return addTo.add(rotatePoint(mutablePoint.setTo(rotate)));
	}
	
	/**
	 * Like {@link #rotatePoint(Point3d)}, but this method allows
	 * for an offset.  This essentially allows for rotation about a
	 * point other than the center of the coordinate system.  Note
	 * that this method assumes the offset will be relative to the current
	 * orientation, which themselves may or may not be relative to the world 
	 * coordinates depending on prior operations.  This is similar to how
	 * OpenGL operates with "stacked" transforms.  The returned value
	 * will be the passed-in rotate object with the rotation-translation delta applied to it.
	 */
	public Point3d rotateWithOffset(Point3d rotate, Point3d offset){
		rotate.subtract(offset);
		rotatePoint(rotate);
		rotate.add(offset);
		return rotate;
	}
	
	/**
	 * Returns the angles for this orientation.  These
	 * are not automatically updated with the rest of the
	 * variables and are only valid if the angle-based
	 * constructor is called and {@link #rotateX(double)},
	 * {@link #rotateY(double)}, and {@link #rotateZ(double)}
	 * are the only methods subsequently called.
	 */
	public Point3d getAngles(){
		return angles;
	}
	
	public void save(WrapperNBT data){
		data.setPoint3d("angles", angles);
	}
}
