package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.List;

public class JSONInstrument extends AJSONCraftable<JSONInstrument.InstrumentGeneral>{
    public List<InstrumentComponent> components = new ArrayList<InstrumentComponent>();

    public class InstrumentGeneral extends AJSONCraftable.General{
    	public List<String> validVehicles;
    	public int textureXSectorStart;
    	public int textureYSectorStart;
    }
    
    public class InstrumentComponent{
    	public String rotationVariable;
    	public int xRotationPositionOffset;
    	public int yRotationPositionOffset;
    	public float rotationOffset;
    	public float rotationFactor;
    	
    	public String visibilityVariable;
    	public boolean dynamicVisibility;
    	public int visibleSectionHeight;
    	public float visibilityOffset;
    	public float visibilityFactor;
    	
    	public boolean lightOverlay;
    }
}