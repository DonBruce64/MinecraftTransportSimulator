package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONSubDefinition{
	@JSONRequired
	@JSONDescription("This text value will be appended to the JSON filename to get the name of the component.  This name is then used to tell MTS the name of the texture and item to use when rendering this component.  Remember, the subName is APPENDED to the JSON file name.  If you name your textures off the sub name MTS will not be able to find them!  If you are making a single-variant model you can leave this field empty (“”), but it must be present otherwise MTS will crash on load.")
	public String subName;
	
	@JSONDescription("his parameter is optional, and is only used for vehicles.  If set, when the vehicle's color is changed with the paint gun to this variant, all parts that have this subType will have their subType changed to this value.  Allows for two-tone vehicles, where you want seats and accessories to not be the same color as the vehicle, but still be based off the vehicle's color.")
	public String secondTone;
	
	@JSONDescription("This parameter is optional.  If set, any textObjects marked as colorInherited will use this color rather than their own.  Useful when you have multiple textures for your model that would cause issues with a single text color.")
	public String secondColor;
	
	@JSONRequired
	@JSONDescription("The name of this component.  Will be displayed in item form and in the benches.  Note that this is just a display name, and is NOT used in any file-linking operations like subName is, so you can put whatever you want here.  Also note that this overrides the “name” parameter in the general section.")
	public String name;
	
	@JSONDescription("An optional description.  This will be appended to the main description if it is present.  This allows for dynamic descriptions for different variants.")
	public String description;
	
	@JSONRequired
	@JSONDescription("These materials are simply extra ones that are needed to craft the variant specified.  This is where you can put dyes for color variants to differentiate them from one another.  These are also what will show up in the paint gun GUI.  Format is the same as the materials entry defined in the general section. ")
	public List<String> extraMaterials;
}
