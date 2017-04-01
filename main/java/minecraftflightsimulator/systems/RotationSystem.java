package minecraftflightsimulator.systems;

import minecraftflightsimulator.baseclasses.MTSVector;

/**Lots of math here.  Move along, nothing to see.
 * 
 * @author don_bruce
 */
public final class RotationSystem{
	private static double d1;
	private static double d2;
	private static double d3;
	private static double d4;
	private static double d5;
	private static double d6;
	private static double d7;
	private static double d8;
	private static double d9;
	
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
	public static MTSVector getRotatedPoint(float x, float y, float z, float pitch, float yaw, float roll){
		d1 = Math.cos(pitch * 0.017453292F);//A
		d2 = Math.sin(pitch * 0.017453292F);//B
		d3 = Math.cos(yaw * 0.017453292F);//C
		d4 = Math.sin(yaw * 0.017453292F);//D
		d5 = Math.cos(roll * 0.017453292F);//E
		d6 = Math.sin(roll * 0.017453292F);//F
		d7 = x*(d3*d5-d2*d4*d6) + y*(-d2*d4*d5-d3*d6) + z*(-d1*d4);
		d8 = x*(d1*d6)          + y*(d1*d5)           + z*(-d2);
		d9 = x*(d4*d5+d2*d3*d6) + y*(d2*d3*d5-d4*d6)  + z*(d1*d3);
		return new MTSVector(d7, d8, d9);
	}
	
	/**
	 * Returns a vector of [0,1,0] rotated using the specified pitch, roll, and yaw.
	 * Used for calculating the wing vector in aircraft.
	 * @param pitch The pitch of the aircraft (in degrees).
	 * @param yaw The yaw of the aircraft (in degrees).
	 * @param roll The roll of the aircraft (in degrees).
	 * @return A Vec3 with the rotated unit vector.
	 */
	public static MTSVector getRotatedY(float pitch, float yaw, float roll){
		d1 = Math.cos(pitch * 0.017453292F);
		d2 = Math.sin(pitch * 0.017453292F);
		d3 = Math.cos(yaw * 0.017453292F);
		d4 = Math.sin(yaw * 0.017453292F);
		d5 = Math.cos(roll * 0.017453292F);
		d6 = Math.sin(roll * 0.017453292F);
		return new MTSVector((-d3*d6 - d2*d4*d5), (d1*d5), (d2*d3*d5 - d4*d6));
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
