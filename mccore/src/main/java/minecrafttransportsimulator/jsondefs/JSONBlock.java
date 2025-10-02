package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

@JSONDescription("A basic block.  Nothing special happens here except some definitions for hardness, blast resistance, orentation, and collision.")
public class JSONBlock extends AJSONItem {

    @JSONDescription("Block-specific properties.")
    public JSONBlockGeneric block;

    public static class JSONBlockGeneric {
        @JSONDescription("How wide the block is.  1 is a full block width.")
        public float width;

        @JSONDescription("How high the block is.  1 is a full block height.  Numbers over 1 will result in unpredictable operations, so don't use them.")
        public float height;

        @JSONDescription("How much light this block gives off.  A value from 0-1, with 1 being full possible light.")
        public float lightLevel;
    }
}