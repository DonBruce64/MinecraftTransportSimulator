package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

public abstract class AJSONPartProvider extends AJSONMultiModelProvider{
    
	@JSONDescription("The parts section is simply a list of part spots for this object, and what parts can go into those spots.  These are also used in parts themselves to allow for sub-parts.  Note that all parts with an X pos that is negative will be mirrored along the Z-axis.  This is to allow for asymmetrical parts such as wheels with hubcaps and seats with armrests.  Note that parts may behave differently depending on what they are placed on.  An engine might work well in a car, but it won't do anything on a hoist!")
    public List<JSONPartDefinition> parts;
    
    @JSONDescription("The collision section is simply a list of boxes that MTS uses to determine the collision points of object.  For most cases you can get away with less than a dozen of these, but for larger models with complex shapes you may need more of them.")
    public List<JSONCollisionBox> collision;
    
    @JSONDescription("Doors are like collision boxes, except they don't affect how the object moves in the world.  Rather, they affect how players interact with the object.  Doors consist of a name, and a hitbox.  This hitbox has an open and closed position that controls where it is located.  Upon clicking a closed door, it moves to the open state, and vice-versa.  Doors themselves don't do much, but you can link them to part positions to prevent part access unless the door is open.  Doors also work as animation variables using their names, so you can actually animate the doors you are clicking with them.")
    public List<JSONDoor> doors;
    
    @JSONDescription("A list of Minecraft potion effects that any entities sitting on or in this object will have while in it. These effects behave the same as if they were caused by drinking a potion in game, but without the particles. They will be continuously reapplied to the rider, and will be removed immediately when the rider stops riding this object.")
    public List<JSONPotionEffect> effects;
}
