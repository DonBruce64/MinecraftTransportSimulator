package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONText{
	@JSONRequired
	@JSONDescription("An entry of x, y, and z coordinates that define the center point of where the text should render.  Text may be left or right aligned by specifying the proper parameter.")
	public Point3d pos;
	
	@JSONRequired
	@JSONDescription("An entry of x, y, and z rotations that tell MTS how to rotate this text.  By default all text faces +z, on the model.")
    public Point3d rot;
	
	@JSONRequired
	@JSONDescription("The scale of the text.  1.0 will render text about ½ block high, as 1 text character pixel equates to one block texture pixel, and text is 8-pixels high.")
	public float scale;
	
	@JSONDescription("The name for this text field.  If two text fields share a name, then they both will be combined in the text GUI into one entry, and changing the text in the GUI will affect both of them.  Useful for license plates and route signs.\nNote: if this object is part of text-based rendering system, this defines which variable is displayed.")
	public String fieldName;
	
	@JSONRequired
	@JSONDescription("The default text to display.  This is what the field will have when the model is first placed down, and will persist until a player changes it.  Required, but may be blank.")
    public String defaultText;
	
	@JSONRequired
	@JSONDescription("The max number of characters this entry can have.")
    public int maxLength;
    
	@JSONRequired
    @JSONDescription("A hexadecimal color code.  This tells MTS what color this text should be.")
    public String color;
	
	@JSONDescription("If true, then this text will get its color from the definition section's secondColor parameter, if one exists.")
	public boolean colorInherited;
	
	@JSONDescription("If this is set, the rendered text will automatically wrap once it hits this many pixels in width.  Note that scaled text will still wrap based on the non-scaled pixel width, so adjust your wrapWidth accordingly if you're scaling text.")
    public int wrapWidth;
    
	@JSONDescription("If set, this text will be assumed to be part of the specified Animated Model Object, and will offset itself to always be in the same position relative to the object.  Useful for text on doors, trunks, or anything else that moves.")
    public String attachedTo;
	
	@JSONDescription("If true, then this text will light up when the model is lit up.")
	public boolean lightsUp;
    
    @JSONDescription("The mode for this text to render.  Position 0 is centered text, with the text anchored at the top-center, position 1 is left-aligned, where pos is the top-left point of the text.  Mode 2 is right-aligned, where pos is the top-right point.")
	public int renderPosition;
    
    @JSONDescription("If true, this text will be auto-scaled to fit inside the wrapWidth rather than actually wrapping to another line.  Has no affect unless you specify a wrapWidth!")
	public boolean autoScale;
}
