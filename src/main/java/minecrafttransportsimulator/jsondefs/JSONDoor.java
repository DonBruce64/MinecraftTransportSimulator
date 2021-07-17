package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONDoor{
	@JSONRequired
	@JSONDescription("The name for this door.  This is the animation variable used for this door in other sections.  Note that if two doors share the same name, both will open or close when their state changes.  Door names are per-object, so if you have multiple doors with the same name defined here, and you open one door, all other doors will open.")
	public String name;
	
    @JSONRequired
    @JSONDescription("An entry of x, y, and z coordinates that define the center point of where this collision box is relative to the center of the object when closed.")
    public Point3d closedPos;
    
	@JSONRequired
	@JSONDescription("Like closedPos, but for the door being open.")
	public Point3d openPos;
	
	@JSONDescription("The width of this collision box, in meters.  Note that since the pos parameter is the center of the box the box will actually extend ½ the width in the X and Z direction.")
    public float width;
	
	@JSONDescription("Same as width, just for the Y direction.")
    public float height;
	
	@JSONDescription("How much armor this door box has.  Values greater than 0 will make this box use armor code to block bullets from passing through it.  Leaving this value out will make all bullets pass through it (no armor).")
    public float armorThickness;
	
	@JSONDescription("Normally, all doors are open when the object is spawned to let players access core areas such as the engine bay and interior to put in engines and seats.  However, this may not be desirable for some doors.  Setting this to true will make the door closed when the object is spawned rather than open.")
    public boolean closedByDefault;
	
	@JSONDescription("If true, then this door will automatically close when the vehicle starts moving.  Useful for cars when you don't want to have to close all the doors when you first place the car, so you just leave them open and drive off and they'll close themselves.  Think GTA doors.")
    public boolean closeOnMovement;
	
	@JSONDescription("If true, then this door will automatically close when the player sits in any linked part that references this door, and will automatically open when they leave the linked part.  Useful to prevent the need to close the door when you get into a vehicle, and open it when you leave it.")
    public boolean activateOnSeated;
	
	@JSONDescription("If true, then this door won't open when clicked, but will still move when the door itself is opened.  Useful for having moving door hitboxes for larger door segments that should only be activated by clicking specific locations, such as cargo doors on vehicles.")
    public boolean ignoresClicks;
}
