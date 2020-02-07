package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.List;

public class JSONInstrument extends AJSONCraftable<JSONInstrument.InstrumentGeneral>{
    public List<Component> components = new ArrayList<Component>();

    public class InstrumentGeneral extends AJSONCraftable<JSONInstrument.InstrumentGeneral>.General{
    	public List<String> validVehicles;
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
    	
    	public String translationVariable;
    	public boolean extendWindow;
    	public boolean translateHorizontal;
    	public float translationFactor;

    	
    	public boolean lightOverlay;
    }
}