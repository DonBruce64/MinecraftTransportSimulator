package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONMuzzle {
    @JSONRequired
    @JSONDescription("The position of this muzzle.  This is where the bullet will be spawned on firing.")
    public Point3D pos;

    @JSONDescription("The rotation of this muzzle.  Allows for slight toe-in on barrels.")
    public RotationMatrix rot;

    @JSONDescription("This is the point this muzzle will rotate about when the gun's pitch is applied.  Used to define a point of rotation for things like tank barrels.  If the barrel is part of, and rotates, with the gun, don't include this.")
    public Point3D center;
}
