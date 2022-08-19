package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONConnectionGroup {
    @JSONRequired
    @JSONDescription("The name of this group.  Will be displayed in the panel.")
    public String groupName;

    @JSONDescription("If set, a button will appear on the panel for making all the connections in this group.")
    public boolean canIntiateConnections;

    @JSONDescription("Normally, vehicles towing vehicle/trailers can't connect or disconnect trailers on the things they are towing.  If this is set, any buttons that would appear on the towed vehicle/trailer will also appear on the main vehicle's panel when connected via this group.  Useful for multi-trailer semi trucks.")
    public boolean canIntiateSubConnections;

    @JSONDescription("If true, this group will be allowed to tow hookup groups.")
    public boolean isHitch;

    @JSONDescription("If true, this group will be allowed to tow hitch groups.  Note that you can have a connection group as both a hookup and a hitch if you want connections for either.  Think train couplers.")
    public boolean isHookup;

    @JSONDescription("A listing of connections for this group.")
    public List<JSONConnection> connections;

    @Deprecated
    public boolean hookup;
}
