package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

public abstract class AJSONInteractableEntity extends AJSONMultiModelProvider {

    @JSONDescription("Collision groups are groups of collision boxes.  These boxes are grouped to allow for common movement, say for doors, as well as common visibility toggling.")
    public List<JSONCollisionGroup> collisionGroups;

    @JSONDescription("Connections are what they sound like: connections to other vehicles.  Connections come in various types and formats, but they all follow a standard idea: one connection has a 'hitch' point that can connect to another connection's 'hookup' point.  These are at defined positions, and when connected MTS will make the two points line up.  Note that connections may NOT share the same position, nor may you have a connection at 0, 0, 0, so keep this in mind.")
    public List<JSONConnectionGroup> connectionGroups;

    @JSONDescription("A list of instruments definitions.  Instrument definitions are used to tell MTS where to render instruments on the object, and where they correspond to on the HUD.  They may also specfy which part the instrument goes to.")
    public List<JSONInstrumentDefinition> instruments;

    @Deprecated
    public List<JSONConnection> connections;
    @Deprecated
    public List<JSONCollisionBox> collision;
    @Deprecated
    public List<JSONDoor> doors;
}
