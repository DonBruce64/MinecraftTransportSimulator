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
    	public int textureXCenter;
    	@JSONRequired
    	public int textureYCenter;
    	@JSONRequired
    	public int textureWidth;
    	@JSONRequired
    	public int textureHeight;
    	
    	public JSONText textObject;
    	@JSONRequired(dependentField="textObject")
    	public float textFactor;
    	
    	public boolean rotateWindow;
    	public boolean extendWindow;
    	public boolean moveComponent;
    	public List<JSONAnimationDefinition> animations;
    	
    	public boolean lightUpTexture;
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