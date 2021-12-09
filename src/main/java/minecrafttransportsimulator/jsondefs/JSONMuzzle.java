package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONMuzzle {
	@JSONRequired
	@JSONDescription("The position of this muzzle.  This is where the bullet will be spawned on firing.")
	public Point3d pos;
	
	@JSONRequired
	@JSONDescription("The rotation of this muzzle.  Allows for slight toe-in on barrels.")
	public Point3d rot;
	
	@JSONRequired
	@JSONDescription("This is the point this muzzle will rotate about when the gun's pitch is applied.  Only uused on pitchIsInternal guns, as external pitch guns move the whole gun, not the muzzle.")
	public Point3d center;
}
