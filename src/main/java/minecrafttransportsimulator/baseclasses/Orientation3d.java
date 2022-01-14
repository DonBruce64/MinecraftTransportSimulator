package minecrafttransportsimulator.baseclasses;

/**Basic Orientation class.  Stores an axis for direction, and rotation about that axis as a double.
 * Maintains a Quaternion of these, which allows it to be multiplied with other objects to 
 * produce a final rotation.  Used to store the orientation of objects and render them without the use 
 * of Euler Angles.  This allow for multiple axis of rotation to be applied and prevents odd bugs with 
 * interpolation due to sign differences.  Note that you should NEVER modify axis, and should always modify
 * rotation via {@link #updateRotation(double)} to ensure proper state is maintained.
 *
 * @author don_bruce
 */
public class Orientation3d{
	public final Point3d axis;
	public double rotation;
	
	private double w;
	private double x;
	private double y;
	private double z;
	
	public Orientation3d(){
		this(new Point3d(0, 0, 1), 0);
	}
	
	public Orientation3d(Point3d axis, double rotation){
		this.axis = axis;
		this.rotation = rotation;
		updateQuaternion(false);
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
	public void updateQuaternion(boolean useQuaternionValues){
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
	 * Sets the rotation for this orientation, updating values as appropriate.
	 */
	public void updateRotation(double newRotation){
		rotation = newRotation;
		updateQuaternion(false);
	}
	
	/**
	 * Multiplies this orientation by the passed-in orientation, storing the result in this orientation.
	 * Updates this orientation as part of this operation if the passed-in boolean is true.
	 * Returns this orientation for nested operations.  For this method, the current orientation will be on
	 * the left, and the orientation to multiply by will be on the right.  
	 */
	public Orientation3d multiplyBy(Orientation3d multiplyBy, boolean updateAxisRotation){
		//Need to put these into variables as we use all four WXYZ params.
		double mW = w * multiplyBy.w - x * multiplyBy.x - y * multiplyBy.y - z * multiplyBy.z;
		double mX = w * multiplyBy.x + x * multiplyBy.w + y * multiplyBy.z - z * multiplyBy.y;
		double mY = w * multiplyBy.y - x * multiplyBy.z + y * multiplyBy.w + z * multiplyBy.x;
		double mZ = w * multiplyBy.z + x * multiplyBy.y - y * multiplyBy.x + z * multiplyBy.w;
		
		this.x = mX;
		this.y = mY;
		this.z = mZ;
		this.w = mW;
		if(updateAxisRotation){
			updateQuaternion(true);
		}
		return this;
	}
	
	/**
	 * Interpolate between the current orientation an the passed-in orientation, storing
	 * the result in the current orientation.  Mainly used for rendering.
	 * Returns the storing object for nested operations.
	 */
	public Orientation3d interpolateTo(Orientation3d other, float partialTicks){
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
				double mW = w * ratioA + other.w * ratioB;
				double mX = x * ratioA + other.x * ratioB;
				double mY = y * ratioA + other.y * ratioB;
				double mZ = z * ratioA + other.z * ratioB;
				
				this.x = mX;
				this.y = mY;
				this.z = mZ;
				this.w = mW;
			}
		}
		updateQuaternion(true);
		return this;
	}
	
	/**
	 * Rotates the passed-in point, storing it in the passed-in point object.
	 * Returns the storing object for nested operations.
	 */
	public Point3d rotatePoint(Point3d rotate, Point3d storeIn){
		//First multiply the point by the quaternion.  Point has imaginary w param of 0.
		double mW = - x * rotate.x - y * rotate.y - z * rotate.z;
		double mX = w * rotate.x + y * rotate.z - z * rotate.y;
		double mY = w * rotate.y - x * rotate.z + z * rotate.x;
		double mZ = w * rotate.z + x * rotate.y - y * rotate.x;
		
		//Next, multiply the result by the conjugate of the quaternion.
		//We don't need to calculate the W parameter here as it has to be 0.
		//storeIn.w = mW * w - mX * -x - mY * -y - mZ * -z;
		storeIn.x = mW * -x + mX * w + mY * -z - mZ * -y;
		storeIn.y = mW * -y - mX * -z + mY * w + mZ * -x;
		storeIn.z = mW * -z + mX * -y - mY * -x + mZ * w;
		return storeIn;
	}
	
	/**
	 * Short-hand method of {@link #rotatePoint(Point3d, Point3d)}
	 * that has the same point to rotate and store the rotation in.
	 */
	public Point3d rotatePoint(Point3d rotate){
		return rotatePoint(rotate, rotate);
	}
}
