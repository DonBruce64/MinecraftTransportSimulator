package minecrafttransportsimulator.baseclasses;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.lwjgl.opengl.GL11;

/**Basic Orientation class.  Stores an axis for direction, and rotation about that axis as a double.
 * Maintains a Quaternion of these, which allows it to be multiplied with other objects to 
 * produce a final rotation.  Used to store the orientation of objects and render them without the use 
 * of Euler Angles.  This allow for multiple axis of rotation to be applied and prevents odd bugs with 
 * interpolation due to sign differences.  Note that you should NEVER modify axis, and should always modify
 * rotation via {@link #setRotation(double)} to ensure proper state is maintained.
 *
 * @author don_bruce
 */
public class Matrix4dPlus extends Matrix4d{
	private final DoubleBuffer buffer = ByteBuffer.allocateDirect(16*Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
	
	public final Point3dPlus lastAnglesSet = new Point3dPlus();
	private final AxisAngle4d helperAxisAngle = new AxisAngle4d();
	private final Quat4d helperQuat = new Quat4d();
	private final Vector3d helperVector = new Vector3d();
	//Can't be created on class construction or we'll infinite loop.
	private Matrix4dPlus helperMatrix;
	
	public Matrix4dPlus(){
		super();
		setIdentity();
	}
	
	public Matrix4dPlus(Matrix4dPlus other){
		super(other);
	}
	
	public void setAngles(Point3dPlus angles){
		lastAnglesSet.set(angles);
		if(helperMatrix == null){
			helperMatrix = new Matrix4dPlus();
		}
		
		setIdentity();
		if(angles.y != 0){
			helperAxisAngle.set(0, 1, 0, Math.toRadians(angles.y));
			setRotation(helperAxisAngle);
		}
		
		if(angles.x != 0){
			helperAxisAngle.set(1, 0, 0, Math.toRadians(angles.x));
			helperMatrix.setIdentity();
			helperMatrix.setRotation(helperAxisAngle);
			mul(helperMatrix);
		}
		
		if(angles.z != 0){
			helperAxisAngle.set(0, 0, 1, Math.toRadians(angles.z));
			helperMatrix.setIdentity();
			helperMatrix.setRotation(helperAxisAngle);
			mul(helperMatrix);
		}
	}
	
	public void applyTransformOpenGL(){
		//FIXME move this to floatbuffer when we get a chance.
		buffer.clear();
		buffer.put(m00);
		buffer.put(m10);
		buffer.put(m20);
		buffer.put(m30);
		buffer.put(m01);
		buffer.put(m11);
		buffer.put(m21);
		buffer.put(m31);
		buffer.put(m02);
		buffer.put(m12);
		buffer.put(m22);
		buffer.put(m32);
		buffer.put(m03);
		buffer.put(m13);
		buffer.put(m23);
		buffer.put(m33);
		buffer.flip();
		GL11.glMultMatrix(buffer);
	}
	
	public Matrix4dPlus getInterpolated(Matrix4dPlus other, double partialTicks){
		this.get(helperQuat);
		other.get(other.helperQuat);
		helperQuat.interpolate(helperQuat, partialTicks);
		
		this.get(helperVector);
		other.get(other.helperVector);
		helperVector.interpolate(other.helperVector, partialTicks);
		
		if(helperMatrix == null){
			helperMatrix = new Matrix4dPlus();
		}
		helperMatrix.set(this);
		helperMatrix.setRotation(helperQuat);
		helperMatrix.setTranslation(helperVector);
		return helperMatrix;
	}
}
