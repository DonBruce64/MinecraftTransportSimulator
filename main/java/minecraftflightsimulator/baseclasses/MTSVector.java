package minecraftflightsimulator.baseclasses;

/**Proxy class used in place of Vec3.
 * Mojang can't keep their class conventions straight
 * and I'm tired of changing names every update.
 * 
 * @author don_bruce
 */
public class MTSVector {
	public double xCoord;
	public double yCoord;
	public double zCoord;
	    
	public MTSVector(double x, double y, double z){
        this.set(x, y, z);
	}
	
	public void set(double x, double y, double z){
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }
	
	public MTSVector add(double x, double y, double z){
		set(this.xCoord + x, this.yCoord + y, this.zCoord + z);
		return this;
	}
	
    public double distanceTo(MTSVector vec2){
        double distX = vec2.xCoord - this.xCoord;
        double distY = vec2.yCoord - this.yCoord;
        double distZ = vec2.zCoord - this.zCoord;
        return Math.sqrt(distX * distX + distY * distY + distZ * distZ);
    }
	
    public double dot(MTSVector vec2){
        return this.xCoord * vec2.xCoord + this.yCoord * vec2.yCoord + this.zCoord * vec2.zCoord;
    }
	
	public MTSVector cross(MTSVector vec2){
		return new MTSVector(this.yCoord * vec2.zCoord - this.zCoord * vec2.yCoord, this.zCoord * vec2.xCoord - this.xCoord * vec2.zCoord, this.xCoord * vec2.yCoord - this.yCoord * vec2.xCoord);
	}
	
	public MTSVector normalize(){
        double length = getLength();
        return length < 1.0E-4D ? new MTSVector(0, 0, 0) : new MTSVector(this.xCoord / length, this.yCoord / length, this.zCoord / length);
	}
	
    public double getLength(){
        return Math.sqrt(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
    }
}
