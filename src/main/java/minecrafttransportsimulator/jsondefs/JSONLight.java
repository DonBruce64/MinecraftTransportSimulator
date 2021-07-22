package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONLight{
	@JSONRequired
	@JSONDescription("The name of the object in the model this light definition will act on.")
	public String objectName;
	
	@JSONDescription("If true, this light will be emissive and render a solid-color of light when on.  If false, only the texture will light up.  Does not affect rendering of blendableComponents.")
	public boolean emissive;
	
	@JSONDescription("If true, this light will have a glass cover rendered over it.  This cover will light-up with the light, so keep this in mind.")
	public boolean covered;
	
	@JSONDescription("If true, this light will be considered a beam and will do beam-blending.  Useful for creating your own custom beam shapes.")
	public boolean isBeam;
	
	@JSONRequired(dependentField="emissive", dependentValues={"true"})
    @JSONDescription("A hexadecimal color code.  This tells MTS what color this light should be.  Required for emissive lights and lights with blendableComponents.")
    public String color;
	
	@JSONDescription("A listing of animation objects for determining the light brightness.  Leaving this blank or not having any active translation transforms will make for a light that is always on at 100% brightness, which you probably don't want.  Translation transforms (using the y-axis) will adjust the brightness, with 0 being off and 1 being fully bright.  Transforms are multiplied together for the final brightness value.  Inhibitor and activator transforms may be used in conjunction with these for advanced brightness logic.  If you want the light to turn off, set the brightness to 0.  Do NOT us a visibility transform as these have no effect.")
	public List<JSONAnimationDefinition> brightnessAnimations;
	
	@JSONDescription("A listing of blendable components for this light.  Used to allow for multiple flares or beams for a single light object.")
	public List<JSONLightBlendableComponent> blendableComponents;
	
	public class JSONLightBlendableComponent{
		@JSONDescription("The position at which the blendable component will be rendered at.")
		public Point3d pos;
		
		@JSONDescription("The axis that defines the 'front' of the blendable component.  This will be the 'normal' for the flare texture, or the direction of the beam.")
		public Point3d axis;
		
		@JSONDescription("The height of the flare to render.")
		public float flareHeight;
		
		@JSONDescription("The width of the flare to render.")
		public float flareWidth;
		
		@JSONDescription("The diameter of the beam to render.")
		public float beamDiameter;
		
		@JSONDescription("The length of the beam to render.")
		public float beamLength;
	}
}
