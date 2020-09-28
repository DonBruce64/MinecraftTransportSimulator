package minecrafttransportsimulator.baseclasses;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**Double implementation of point class.
 *
 * @author don_bruce
 */
public class Point3d extends APoint3<Double, Point3d>{
	private static final Point3d ZERO = new Point3d(0D, 0D, 0D);
	
	public double x;
	public double y;
	public double z;
	
	public Point3d(double x, double y, double z){
		super(x, y, z);
	}
	
	public Point3d(Point3i point){
		this(point.x, point.y, point.z);
	}
	
	@Override
	public boolean equals(Object object){
		if(object instanceof Point3d){
			Point3d otherPoint = (Point3d) object;
			return (float)x == (float)otherPoint.x && (float)y == (float)otherPoint.y && (float)z == (float)otherPoint.z;
		}else{
			return false;
		}
	}
	
	@Override
	public String toString(){
		return "[" + x + ", " + y + ", " + z + "]";
	}
	
	@Override
	public Point3d set(Double x, Double y, Double z){
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	@Override
	public Point3d setTo(Point3d point){
		this.x = point.x;
		this.y = point.y;
		this.z = point.z;
		return this;
	}
	
	@Override
	public Point3d add(Double x, Double y, Double z){
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}
	
	@Override
	public Point3d add(Point3d point){
		this.x += point.x;
		this.y += point.y;
		this.z += point.z;
		return this;
	}
	
	@Override
	public Point3d subtract(Point3d point){
		this.x -= point.x;
		this.y -= point.y;
		this.z -= point.z;
		return this;
	}
	
	@Override
	public Point3d multiply(Double scale){
		this.x *= scale;
		this.y *= scale;
		this.z *= scale;
		return this;
	}

	@Override
	public Point3d multiply(Point3d point){
		this.x *= point.x;
		this.y *= point.y;
		this.z *= point.z;
		return this;
	}
	
	@Override
	public Double distanceTo(Point3d point){
		double deltaX = point.x - this.x;
		double deltaY = point.y - this.y;
		double deltaZ = point.z - this.z;
		return Math.sqrt(deltaX*deltaX + deltaY*deltaY + deltaZ*deltaZ);
	}
	
	public Double distanceTo(Point3i point){
		double deltaX = point.x - this.x;
		double deltaY = point.y - this.y;
		double deltaZ = point.z - this.z;
		return Math.sqrt(deltaX*deltaX + deltaY*deltaY + deltaZ*deltaZ);
	}
	
	@Override
	public Double dotProduct(Point3d point){
		return this.x*point.x + this.y*point.y + this.z*point.z;
	}
	
	@Override
	public Point3d crossProduct(Point3d point){
		return new Point3d(this.y*point.z - this.z*point.y, this.z*point.x - this.x*point.z, this.x*point.y - this.y*point.x);
	}
	
	@Override
	public Double length(){
		return Math.sqrt(x*x + y*y + z*z);
	}
	
	@Override
	public Point3d normalize(){
		Double length = length();
		if(length > 1.0E-4D){
			x /= length;
			y /= length;
			z /= length;
		}
		return this;
	}
	
	@Override
	public Point3d copy(){
		return new Point3d(this.x, this.y, this.z);
	}
	
	@Override
	public boolean isZero(){
		return this.equals(ZERO);
	}
	
	/**
     * Returns a new point with the x value equal to the second parameter, provided the X value
     * is between this point and the passed-in point, and the passed-in point's x-value is not
     * equal to this point's x-value.  If such conditions are not satisfied, null is returned.
     */
    public Point3d getIntermediateWithXValue(Point3d endPoint, double targetX){
    	Point3d delta = endPoint.copy().subtract(this);
        if(delta.x*delta.x < 1.0E-7D){
        	//Point delta is 0, so there's no difference here.
            return null;
        }else{
        	//Return point as a factored-percentage of total length.
        	double factor = (targetX - this.x)/delta.x;
            return factor >= 0.0D && factor <= 1.0D ? delta.multiply(factor).add(this) : null;
        }
    }

    /**
     * Returns a new point with the y value equal to the second parameter, provided the Y value
     * is between this point and the passed-in point, and the passed-in point's y-value is not
     * equal to this point's y-value.  If such conditions are not satisfied, null is returned.
     */
    public Point3d getIntermediateWithYValue(Point3d endPoint, double targetY){
    	Point3d delta = endPoint.copy().subtract(this);
        if(delta.y*delta.y < 1.0E-7D){
        	//Point delta is 0, so there's no difference here.
            return null;
        }else{
        	//Return point as a factored-percentage of total length.
        	double factor = (targetY - this.y)/delta.y;
            return factor >= 0.0D && factor <= 1.0D ? delta.multiply(factor).add(this) : null;
        }
    }
    
    /**
     * Returns a new point with the z value equal to the second parameter, provided the Z value
     * is between this point and the passed-in point, and the passed-in point's z-value is not
     * equal to this point's z-value.  If such conditions are not satisfied, null is returned.
     */
    public Point3d getIntermediateWithZValue(Point3d endPoint, double targetZ){
    	Point3d delta = endPoint.copy().subtract(this);
        if(delta.z*delta.z < 1.0E-7D){
        	//Point delta is 0, so there's no difference here.
            return null;
        }else{
        	//Return point as a factored-percentage of total length.
        	double factor = (targetZ - this.z)/delta.z;
            return factor >= 0.0D && factor <= 1.0D ? delta.multiply(factor).add(this) : null;
        }
    }
	
	private static final Double[] sinTable = new Double[361];
	private static final Double[] cosTable = new Double[361];
	/**
     * Rotates this point about the passed-in angles.  Rotation is done using a 360-degree
     * static lookup table, so rotation is done to the nearest degree.  This is faster than
     * {@link #rotateFine(Point3d)} as actual sin and cos calculations are not performed.
     */
	public Point3d rotateCoarse(Point3d angles){
		//Init sin and cos tables, if they aren't ready.
		if(sinTable[0] == null){
			for(int i=0; i<=360; ++i){
				sinTable[i] = Math.sin(Math.toRadians(i));
				cosTable[i] = Math.cos(Math.toRadians(i));
			}
		}
		
		//Clamp values to 0-360;
		double xRot = (angles.x%360 + 360)%360;
		double yRot = (angles.y%360 + 360)%360;
		double zRot = (angles.z%360 + 360)%360;
		
		//Rotate based on tabled values.
		double cosX = cosTable[(int) xRot];//A
		double sinX = sinTable[(int) xRot];//B
		double cosY = cosTable[(int) yRot];//C
		double sinY = sinTable[(int) yRot];//D
		double cosZ = cosTable[(int) zRot];//E
		double sinZ = sinTable[(int) zRot];//F
		set(	x*(cosY*cosZ-sinX*-sinY*sinZ) 	+ y*(-sinX*-sinY*cosZ-cosY*sinZ) 	+ z*(-cosX*-sinY),
				x*(cosX*sinZ)           		+ y*(cosX*cosZ)            			+ z*(-sinX),
				x*(-sinY*cosZ+sinX*cosY*sinZ) 	+ y*(sinX*cosY*cosZ+sinY*sinZ)  	+ z*(cosX*cosY)
		);
		return this;
	}
		
	/**
     * Rotates this point about the passed-in angles.  Rotation is done using actual sin
     * and cos calls via a rotation matrix, so only use this when precision is required (say
     * in rendering operations).  If only a rough approximation is required, use {@link #rotateCoarse(Point3d)}
     */
	public Point3d rotateFine(Point3d angles){
		if(!angles.isZero()){
			double cosX = Math.cos(Math.toRadians(angles.x));//A
			double sinX = Math.sin(Math.toRadians(angles.x));//B
			double cosY = Math.cos(Math.toRadians(angles.y));//C
			double sinY = Math.sin(Math.toRadians(angles.y));//D
			double cosZ = Math.cos(Math.toRadians(angles.z));//E
			double sinZ = Math.sin(Math.toRadians(angles.z));//F
			set(	x*(cosY*cosZ-sinX*-sinY*sinZ) 	+ y*(-sinX*-sinY*cosZ-cosY*sinZ) 	+ z*(-cosX*-sinY),
					x*(cosX*sinZ)           		+ y*(cosX*cosZ)            			+ z*(-sinX),
					x*(-sinY*cosZ+sinX*cosY*sinZ) 	+ y*(sinX*cosY*cosZ+sinY*sinZ)  	+ z*(cosX*cosY)
			);
		}
		return this;
	}
	
	/*For reference, here are the rotation matrixes.
	 * Note that the resultant rotation matrix follows the Yaw*Pitch*Roll format.
	 * Rx=[[1,0,0],[0,cos(P),-sin(P)],[0,sin(P),cos(P)]]
	 * Ry=[[cos(Y),0,sin(Y)],[0,1,0],[-sin(Y),0,cos(Y)]]
	 * Rz=[[cos(R),-sin(R),0],[sin(R),cos(R),0],[0,0,1]]
	 * {[C,0,-D],[0,1,0],[D,0,C]}*{[1,0,0],[0,A,-B],[0,B,A]}*{[E,-F,0],[F,E,0],[0,0,1]}
	 */
	
	/**Class for use in JSON operations.
	 *
	 * @author don_bruce
	 */
	public static final TypeAdapter<Point3d> adapter = new TypeAdapter<Point3d>(){
		
		@Override
		public Point3d read(JsonReader reader) throws IOException{
			if(reader.peek() == JsonToken.NULL){
				reader.nextNull();
				return null;
			}else{
				reader.beginArray();
				Point3d point = new Point3d(reader.nextDouble(), reader.nextDouble(), reader.nextDouble());
				reader.endArray();
				return point;
			}
		}
		
		@Override
		public void write(JsonWriter writer, Point3d point) throws IOException{
			if(point == null){
				writer.nullValue();
				return;
			}else{
				writer.beginArray();
				writer.value(point.x);
				writer.value(point.y);
				writer.value(point.z);
				writer.endArray();
			}
		}
	};
}
