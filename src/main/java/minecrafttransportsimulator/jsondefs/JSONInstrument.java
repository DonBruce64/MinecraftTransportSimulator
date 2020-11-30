package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimationDefinition;

public class JSONInstrument extends AJSONItem<JSONInstrument.InstrumentGeneral>{
    public List<Component> components = new ArrayList<Component>();

    public class InstrumentGeneral extends AJSONItem<JSONInstrument.InstrumentGeneral>.General{
    	
    }
    
    public class Component{
    	public int xCenter;
    	public int yCenter;
    	public float scale;
    	public int textureXCenter;
    	public int textureYCenter;
    	public int textureWidth;
    	public int textureHeight;
    	
    	public JSONText textObject;
    	public float textFactor;
    	
    	public boolean rotateWindow;
    	public boolean extendWindow;
    	public boolean moveComponent;
    	public List<VehicleAnimationDefinition> animations;
    	
    	public boolean lightUpTexture;
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