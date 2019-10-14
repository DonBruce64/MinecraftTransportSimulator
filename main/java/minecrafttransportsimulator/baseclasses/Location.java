package minecrafttransportsimulator.baseclasses;

/**Like the Point class, except we use integers rather than doubles.
 * This is due to MC block locations being whole integers, while
 * entities and other things can be at any fraction of a Point
 * between those. 
 * 
 * @author don_bruce
 */
public class Location{
	public int x;
	public int y;
	public int z;
	
	public Location(int x, int y, int z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void offset(int xOffset, int yOffset, int zOffset){
		this.x += xOffset;
		this.y += xOffset;
		this.z += xOffset;
	}
	
	public double distanceTo(Location otherPoint){
		return Math.sqrt((this.x - otherPoint.x)*(this.x - otherPoint.x) + (this.y - otherPoint.y)*(this.y - otherPoint.y) + (this.z - otherPoint.z)*(this.z - otherPoint.z));
	}
}
