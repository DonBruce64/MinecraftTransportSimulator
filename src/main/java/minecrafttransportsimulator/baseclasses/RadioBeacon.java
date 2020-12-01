package minecrafttransportsimulator.baseclasses;

import minecrafttransportsimulator.mcinterface.IWrapperNBT;

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
	
	public RadioBeacon(IWrapperNBT data){
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
	
	public void save(IWrapperNBT data){
		data.setString("name", name);
		data.setDouble("glideSlope", glideSlope);
		data.setDouble("bearing", bearing);
		data.setPoint3i("location", location);
	}
}