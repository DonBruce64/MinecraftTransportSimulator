package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.vehicles.main.AEntityBase;

/**Beacon class.  Responsible for containing the state of a beacon, which includes
 * the beacon's position, and other properties.
 * 
 * @author don_bruce
 */
public class RadioBeacon{
	public final String name;
	public final double glideSlope;
	public final double bearing;
	public final Point3i location;
	
	public RadioBeacon(WrapperNBT data){
		this.name = data.getString("name");
		this.glideSlope = data.getDouble("glideSlope");
		this.bearing = data.getDouble("bearing");
		this.location = data.getPoint3i("location");
	}
	
	public RadioBeacon(String name, double glideSlope, double bearing, Point3i location){
		this.name = name;
		this.glideSlope = glideSlope;
		this.bearing = bearing;
		this.location = location;
	}
	
	public double getBearingDelta(AEntityBase entity){
		//Add 180 to the bearing, as players enter the direction to land, but the bearing delta needs to return the
		//delta between the entity and the beacon's "beam".  This requires the beam to be opposite of the landing direction.
		//Normally we'd subtract the bearing here, but MC inverts y-coords, so players enter the bearing backwards.
		double delta = Math.toDegrees(Math.atan2(entity.position.x - location.x, entity.position.z - location.z)) + bearing + 180;
		while(delta < -180) delta += 360;
		while(delta > 180) delta -= 360;
		return delta;
	}
	
	public void save(WrapperNBT data){
		data.setString("name", name);
		data.setDouble("glideSlope", glideSlope);
		data.setDouble("bearing", bearing);
		data.setPoint3i("location", location);
	}
}