package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Orientation3d;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONConnection{
	@JSONRequired
	@JSONDescription("The type of connection.  This can be anything you want, but it is recommended to use a generic enough name that allows your pack to be compatible with other packs wishing to use the same connection.  “Tow” is a common one in use for a hitch position on vehicles that allows them to be towed by a tow truck, for example.")
	public String type;
	
	@JSONRequired
	@JSONDescription("The position of this connection on the vehicle.")
	public Point3d pos;
	
	@JSONRequired(dependentField="mounted", dependentValues={"true"})
	@JSONDescription("The rotation of this connection.  Required when using mounted connections.  Has no effect on other connections.")
	public Orientation3d rot;
	
	@JSONDescription("If true, then connecting vehicles will be mounted to this point rather than dragged.  Useful for things like flat-bed trailers, where you want the vehicle to stay in one place.")
	public boolean mounted;
	
	@JSONDescription("If true, then connecting vehicles will be restricted to only change in pitch, and won't allowed to yaw back and forth.")
	public boolean restricted;
	
	@JSONDescription("How far away this connection can connect.  Defaults to 2 blocks if not set.")
	public double distance;
	
	@Deprecated
	public boolean hookup;
}
