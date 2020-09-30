package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.List;

public class JSONInstrument extends AJSONItem<JSONInstrument.InstrumentGeneral>{
    public List<Component> components = new ArrayList<Component>();

    public class InstrumentGeneral extends AJSONItem<JSONInstrument.InstrumentGeneral>.General{
    	
    }
    
    public class Component{
    	public int xCenter;
    	public int yCenter;
    	
    	public int textureXCenter;
    	public int textureYCenter;
    	public int textureWidth;
    	public int textureHeight;
    	
    	public String rotationVariable;
    	public boolean rotateWindow;
    	public float rotationOffset;
    	public float rotationFactor;
    	public float rotationClampMin;
    	public float rotationClampMax;
    	public boolean rotationAbsoluteValue;
    	
    	public String translationVariable;
    	public boolean extendWindow;
    	public boolean translateHorizontal;
    	public float translationFactor;
    	public float translationClampMin;
    	public float translationClampMax;
    	public boolean translationAbsoluteValue;

    	
    	public boolean lightOverlay;
    }
}