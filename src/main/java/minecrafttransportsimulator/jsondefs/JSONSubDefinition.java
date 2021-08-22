package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONSubDefinition{
	@JSONRequired
	@JSONDescription("This text value will be appended to the JSON filename to get the name of the component.  This name is then used to tell MTS the name of the texture and item to use when rendering this component.  Remember, the subName is APPENDED to the JSON file name.  If you name your textures off the sub name MTS will not be able to find them!  If you are making a single-variant model you can leave this field empty (“”), but it must be present otherwise MTS will crash on load.")
	public String subName;
	
	@JSONDescription("This parameter is optional, and is only used for vehicles and parts.  If set, when the vehicle's color is changed with the paint gun the new color's definition is checked for a secondTone.  If one is found, all parts will be checked.  If any part has definition with a subName matching the vehicle's secondTone, then the part will be swapped for the one with the appropriate definition.")
	public String secondTone;
	
	@JSONDescription("This parameter is optional.  If set, any textObjects marked as colorInherited will use this color rather than their own.  Useful when you have multiple textures for your model that would cause issues with a single text color.")
	public ColorRGB secondColor;
	
	@JSONDescription("This parameter is optional.  If set, then the model with this name will be used for this definition rather than the default one.  This model must be in the same folder as all other models for this component, but it may be in sub-folders if desired.")
	public String modelName;
	
	@JSONDescription("Like modelName, but used to override the texture.  Useful if you have multiple models with the same texture (say different orientations of the same thing).")
	public String textureName;
	
	@JSONDescription("The name of this component.  Will be displayed in item form and in the benches.  Note that this is just a display name, and is NOT used in any file-linking operations like subName is, so you can put whatever you want here.  Also note that this overrides the “name” parameter in the general section.")
	public String name;
	
	@JSONDescription("An optional description.  This will be appended to the main description if it is present.  This allows for dynamic descriptions for different variants.")
	public String description;
	
	@JSONRequired
	@JSONDescription("These materials are simply extra ones that are needed to craft the variant specified.  This is where you can put dyes for color variants to differentiate them from one another.  These are also what will show up in the paint gun GUI.  Format is the same as the materials entry defined in the general section. ")
	public List<String> extraMaterials;
}
