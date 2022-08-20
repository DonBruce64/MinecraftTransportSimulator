package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("What's a city without road signs or traffic lights?  A bland street, that's what!  While you are by no means required to make pole-based items in your pack, you may do so if you chose by using some textures and a single JSON file.  In fact, you may choose to release a pack consisting entirely of road equipment!  Wouldn't be the first time someone made a thing for Minecraft that just has road stuff.")
public class JSONPoleComponent extends AJSONMultiModelProvider {

    @JSONDescription("Pole-specific properties.")
    public JSONPoleGeneric pole;

    public static class JSONPoleGeneric {
        @JSONRequired
        @JSONDescription("The type of this pole.  This defines its properties.")
        public PoleComponentType type;

        @JSONRequired
        @JSONDescription("This parameter tells MTS how much to offset components put on this pole.\nThis is because some poles may be larger than others, and making it so models always render at the same point would lead to clipping on large poles and floating on small ones. \nFor all cases, you should set this to the offset from the center where all components should attach to your pole.")
        public float radius;

        @JSONDescription("Normally poles don't allow components to be placed diagonally on them.  If you want this with your poles, set this to true.")
        public boolean allowsDiagonals;
    }
}
