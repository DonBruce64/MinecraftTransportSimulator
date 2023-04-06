package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;

import minecrafttransportsimulator.baseclasses.Point3D;

public class JSONDummyPartProvider extends AJSONPartProvider {

    public static JSONDummyPartProvider generateDummy() {
        JSONDummyPartProvider definition = new JSONDummyPartProvider();
        definition.packID = "dummy";
        definition.systemName = "dummy";
        definition.general = new AJSONItem.General();
        definition.general.health = 100;

        JSONPartDefinition fakeDef = new JSONPartDefinition();
        fakeDef.pos = new Point3D();
        fakeDef.types = new ArrayList<>();
        fakeDef.bypassSlotChecks = true;
        definition.parts = new ArrayList<>();
        definition.parts.add(fakeDef);

        JSONSubDefinition subDef = new JSONSubDefinition();
        subDef.subName = "";
        definition.definitions = new ArrayList<>();
        definition.definitions.add(subDef);

        definition.rendering = new JSONRendering();

        return definition;
    }
}
