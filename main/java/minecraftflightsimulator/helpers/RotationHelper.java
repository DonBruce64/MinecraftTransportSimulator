package minecraftflightsimulator.helpers;

import net.minecraft.util.MathHelper;

public class RotationHelper {	
	/**
	 * Takes a set of points and rotates it about a specified pitch, roll, and yaw.
	 * Used for the complex positioning of child entities and force calculations.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param z Z-coordinate
	 * @param pitch The pitch of the aircraft (in degrees).
	 * @param yaw The yaw of the aircraft (in degrees).
	 * @param roll The roll of the aircraft (in degrees).
	 * @return A Vec3 with the rotated points.
	 */
	public static MFSVector getRotatedPoint(float x, float y, float z, float pitch, float yaw, float roll){
		float f1 = MathHelper.cos(pitch * 0.017453292F);//A
		float f2 = MathHelper.sin(pitch * 0.017453292F);//B
		float f3 = MathHelper.cos(yaw * 0.017453292F);//C
		float f4 = MathHelper.sin(yaw * 0.017453292F);//D
		float f5 = MathHelper.cos(roll * 0.017453292F);//E
		float f6 = MathHelper.sin(roll * 0.017453292F);//F
		float f7 = x*(f3*f5-f2*f4*f6) + y*(-f2*f4*f5-f3*f6) + z*(-f1*f4);
		float f8 = x*(f1*f6)          + y*(f1*f5)           + z*(-f2);
		float f9 = x*(f4*f5+f2*f3*f6) + y*(f2*f3*f5-f4*f6)  + z*(f1*f3);
		return new MFSVector(f7, f8, f9);
	}
	
	/**
	 * Returns a vector of [0,1,0] rotated using the specified pitch, roll, and yaw.
	 * Used for calculating the wing vector in aircraft.
	 * @param pitch The pitch of the aircraft (in degrees).
	 * @param yaw The yaw of the aircraft (in degrees).
	 * @param roll The roll of the aircraft (in degrees).
	 * @return A Vec3 with the rotated unit vector.
	 */
	public static MFSVector getRotatedY(float pitch, float yaw, float roll){
		float f1 = MathHelper.cos(pitch * 0.017453292F);
		float f2 = MathHelper.sin(pitch * 0.017453292F);
		float f3 = MathHelper.cos(yaw * 0.017453292F);
		float f4 = MathHelper.sin(yaw * 0.017453292F);
		float f5 = MathHelper.cos(roll * 0.017453292F);
		float f6 = MathHelper.sin(roll * 0.017453292F);
		return new MFSVector((-f3*f6 - f2*f4*f5), (f1*f5), (f2*f3*f5 - f4*f6));
	}
	
	/*For reference, here are the rotation matrixes.
	 * Note that for the wing vector the resultant matrix, R=Ry*Rx*Rz
	 * has been simplified to only deal with the unit vector [0,1,0]
	 * Also note that the resultant rotation matrix follows the Yaw*Pitch*Roll format.
	 * Rx=[[1,0,0],[0,cos(P),-sin(P)],[0,sin(P),cos(P)]]
	 * Ry=[[cos(Y),0,-sin(Y)],[0,1,0],[sin(Y),0,cos(Y)]]
	 * Rz=[[cos(R),-sin(R),0],[sin(R),cos(R),0],[0,0,1]]
	 * {[C,0,-D],[0,1,0],[D,0,C]}*{[1,0,0],[0,A,-B],[0,B,A]}*{[E,-F,0],[F,E,0],[0,0,1]}
	 */
}
