package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONInstrument extends AJSONItem<JSONInstrument.InstrumentGeneral>{
    public List<Component> components = new ArrayList<Component>();

    public class InstrumentGeneral extends AJSONItem<JSONInstrument.InstrumentGeneral>.General{
    	
    }
    
    public class Component{
    	public int xCenter;
    	public int yCenter;
    	public float scale;
    	@JSONRequired
        @JSONDescription("This is the center of the texture location of the X axis on the texture sheet.  Units are in pixels.")
    	public int textureXCenter;
    	@JSONRequired
        @JSONDescription("This is the center of the texture location of the Y axis on the texture sheet.  Units are in pixels.")
    	public int textureYCenter;
    	@JSONRequired
        @JSONDescription("The width, in pixels, of the texture section to render.")
    	public int textureWidth;
    	@JSONRequired
        @JSONDescription("The height, in pixels, of the texture section to render.")
    	public int textureHeight;
    	
    	public JSONText textObject;
    	@JSONRequired(dependentField="textObject")
        @JSONDescription("The factor to multiply the the returned value of the textObject's fieldName before displaying it.  Has no effect if no textObject is given.")
    	public float textFactor;
    	
        
        //Multi-line description required
    	public boolean rotateWindow;
        //Multi-line description required
    	public boolean extendWindow;
        //Multi-line description required
    	public boolean moveComponent;
        @JSONDescription("This is a list of animatedObjects that can be used to move this instrument based on the animation values.")
    	public List<JSONAnimationDefinition> animations;
    	
        @JSONDescription("If this is true, MTS will make this texture bright when the lights on the vehicle are on.")
    	public boolean lightUpTexture;
        //May require multi-line description
    	public boolean overlayTexture;
    	
    	@Deprecated
    	public boolean lightOverlay;
    	
    	@Deprecated
    	public String rotationVariable;
    	@Deprecated
    	public float rotationOffset;
    	@Deprecated
    	public float rotationFactor;
    	@Deprecated
    	public float rotationClampMin;
    	@Deprecated
    	public float rotationClampMax;
    	@Deprecated
    	public boolean rotationAbsoluteValue;
    	
    	@Deprecated
    	public String translationVariable;
    	@Deprecated
    	public boolean translateHorizontal;
    	@Deprecated
    	public float translationFactor;
    	@Deprecated
    	public float translationClampMin;
    	@Deprecated
    	public float translationClampMax;
    	@Deprecated
    	public boolean translationAbsoluteValue;
    }
}
