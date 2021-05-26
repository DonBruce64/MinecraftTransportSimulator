package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

public abstract class AJSONInteractableEntity extends AJSONMultiModelProvider{
    
    @JSONDescription("The collision section is simply a list of boxes that MTS uses to determine the collision points of object.  For most cases you can get away with less than a dozen of these, but for larger models with complex shapes you may need more of them.")
    public List<JSONCollisionBox> collision;
    
    @JSONDescription("Doors are like collision boxes, except they don't affect how the object moves in the world.  Rather, they affect how players interact with the object.  Doors consist of a name, and a hitbox.  This hitbox has an open and closed position that controls where it is located.  Upon clicking a closed door, it moves to the open state, and vice-versa.  Doors themselves don't do much, but you can link them to part positions to prevent part access unless the door is open.  Doors also work as animation variables using their names, so you can actually animate the doors you are clicking with them.")
    public List<JSONDoor> doors;
    
    @JSONDescription("Connections are what they sound like: connections to other vehicles.  Connections come in various types and formats, but they all follow a standard idea: one connection has a “hitch” point that can connect to another connection's “hookup” point.  These are at defined positions, and when connected MTS will make the two points line up.  Note that connections may NOT share the same position, nor may you have a connection at 0, 0, 0, so keep this in mind.")
    public List<JSONConnectionGroup> connectionGroups;
    
    @JSONDescription("A list of Minecraft potion effects that any entities sitting on or in this object will have while in it. These effects behave the same as if they were caused by drinking a potion in game, but without the particles. They will be continuously reapplied to the rider, and will be removed immediately when the rider stops riding this object.")
    public List<JSONPotionEffect> effects;
    
	@JSONDescription("A list of instruments definitions.  Instrument definitions are used to tell MTS where to render instruments on the object, and where they correspond to on the HUD.  They may also specfy which part the instrument goes to.")
    public List<JSONInstrumentDefinition> instruments;
    
    @Deprecated
    public List<JSONConnection> connections;
}
