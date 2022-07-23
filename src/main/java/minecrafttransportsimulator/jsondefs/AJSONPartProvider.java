package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

import java.util.List;

public abstract class AJSONPartProvider extends AJSONInteractableEntity {

    @JSONDescription("The parts section is simply a list of part spots for this object, and what parts can go into those spots. These are also used in parts themselves to allow for sub-parts. Note that all parts with an X pos that is negative will be mirrored along the Z-axis. This is to allow for asymmetrical parts such as wheels with hubcaps and seats with armrests. Note that parts may behave differently depending on what they are placed on. An engine might work well in a car, but it won't do anything on a hoist!")
    public List<JSONPartDefinition> parts;

}
