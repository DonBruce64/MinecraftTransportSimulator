package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

import java.util.List;

public class JSONMuzzleGroup {
    @JSONRequired
    @JSONDescription("A listing of muzzles that are in this group. They will all be fired when this group is active.")
    public List<JSONMuzzle> muzzles;
}
