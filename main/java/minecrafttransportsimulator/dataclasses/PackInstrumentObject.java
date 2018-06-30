package minecrafttransportsimulator.dataclasses;

import java.util.ArrayList;
import java.util.List;

public class PackInstrumentObject{
    public PackInstrumentDefinition general = new PackInstrumentDefinition();
    public List<PackInstrumentComponent> components = new ArrayList<PackInstrumentComponent>();

    public class PackInstrumentDefinition{
    	public List<String> validVehicles;
    	public List<String> materials;
    	public int textureXSectorStart;
    	public int textureYSectorStart;
    }
    
    public class PackInstrumentComponent{
    	public String rotationVariable;
    	public int xRotationPositionOffset;
    	public int yRotationPositionOffset;
    	public float rotationOffset;
    	public float rotationFactor;
    	
    	public String visibilityVariable;
    	public int visibleSectionHeight;
    	public float visibilityOffset;
    	public float visibilityFactor;
    	public boolean dynamicVisibility;
    	
    	public boolean lightOverlay;
    }
}