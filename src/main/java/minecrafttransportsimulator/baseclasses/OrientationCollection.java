package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.mcinterface.WrapperNBT;

/**A collection of four Orientation classes that represent the orientation of an object.  This contains
 * the X, Y, Z and net orientations for use with an entity.  Various methods exist here for querying the
 * orientation of the requested axis, or modifying the state of that axis.
 *
 * @author don_bruce
 */
public class OrientationCollection{
	public final Orientation3d x;
	public final Orientation3d y;
	public final Orientation3d z;
	public final Orientation3d net;
	public final Orientation3d netPrior;
	private static final Orientation3d interpolatedOrientation = new Orientation3d();
	
	public OrientationCollection(WrapperNBT data){
		this(data.getPoint3d("angles"));
	}
	
	public OrientationCollection(Point3d angles){
		this.x = new Orientation3d(new Point3d(1, 0, 0), angles.x);
		this.y = new Orientation3d(new Point3d(0, 1, 0), angles.y);
		this.z = new Orientation3d(new Point3d(0, 0, 1), angles.z);
		this.net = new Orientation3d();
		this.netPrior = new Orientation3d();
		updateNet();
	}
	
	/**
	 * Sets the x rotation value to the passed-in value.
	 */
	public void setX(double rotation){
		x.rotation = rotation;
		x.updateQuaternion(false);
	}
	
	/**
	 * Sets the y rotation value to the passed-in value.
	 */
	public void setY(double rotation){
		y.rotation = rotation;
		y.updateQuaternion(false);
	}
	
	/**
	 * Sets the z rotation value to the passed-in value.
	 */
	public void setZ(double rotation){
		z.rotation = rotation;
		z.updateQuaternion(false);
	}
	
	/**
	 * Sets all rotation values to the passed-in value.
	 * Also updates the net value, as it's expected that's wanted.
	 */
	public void setXYZ(Point3d angles){
		x.rotation = angles.x;
		y.rotation = angles.y;
		z.rotation = angles.z;
		updateNet();
	}
	
	/**
	 * Sets the orientation to the passed-in orientation.
	 * Also updates the net value, as it's expected that's wanted.
	 */
	public void setTo(OrientationCollection other){
		x.rotation = other.x.rotation;
		y.rotation = other.y.rotation;
		z.rotation = other.z.rotation;
		updateNet();
	}
	
	/**
	 * Updates the net value to the current component value.
	 */
	public void updateNet(){
		//Update the axis-rotation parameters first for all quanterions.
		//This is because they likely changed since this operation.
		x.updateQuaternion(false);
		y.updateQuaternion(false);
		z.updateQuaternion(false);
		net.setTo(y).multiplyBy(x, false).multiplyBy(z, true);
	}
	
	/**
	 * Updates the prior net value to the current value.
	 */
	public void updatePrior(){
		netPrior.setTo(net);
	}
	
	/**
	 * Returns the interpolated orientation for the net and net-prior orientation.
	 * If an orientation is passed-in, then the function will set the orientation
	 * to the interpolated value.  If null is passed-in, then a mutable object will
	 * be returned with the value.  This object will be re-used on subsequent calls,
	 * so do not keep a reference to it.
	 */
	public Orientation3d getInterpolated(Orientation3d store, float partialTicks){
		if(store == null){
			store = interpolatedOrientation;
		}
		return store.setTo(netPrior).interpolateTo(net, partialTicks);
	}

	public void save(WrapperNBT data){
		data.setPoint3d("angles", new Point3d(x.rotation, y.rotation, z.rotation));
	}
}
