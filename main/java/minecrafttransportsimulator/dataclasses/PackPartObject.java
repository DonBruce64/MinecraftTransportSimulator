package minecrafttransportsimulator.dataclasses;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackRotatableModelObject;

public class PackPartObject{
	public PartGeneralConfig general;
    public PartEngineConfig engine;
    public PartGroundDeviceConfig groundDevice;
    public PartPropellerConfig propeller;
    public PartCustomConfig custom;
    public List<PackPart> subParts = new ArrayList<PackPart>();
    public PartRenderingConfig rendering;

    public class PartGeneralConfig{
    	public String type;
    	public String modelName;
    	public String[] materials;
    	public String customType;
    }
    
    public class PartEngineConfig{
    	public boolean isAutomatic;
    	public byte starterPower;
    	public byte starterDuration;
    	public int maxRPM;
    	public float fuelConsumption;
    	public float[] gearRatios;
    	public String[] repairMaterials;
    }
    
    public class PartGroundDeviceConfig{
    	public boolean canBeFlat;
    	public boolean canFloat;
        public boolean rotatesOnShaft;
        public boolean isLongPart;
        public float diameter;
        public float thickness;
        public float motiveFriction;
        public float lateralFriction;
        
        //These parameters are extra and depend on what is chosen in the booleans above.
        public float extraCollisionBoxOffset;
        public float flatDiameter;
    }
    
    public class PartPropellerConfig{
    	public boolean isDynamicPitch;
    	public byte numberBlades;
    	public short pitch;
    	public int diameter;
    	public int startingHealth;
    }
    
    public class PartCustomConfig{
    	public float width;
    	public float height;
    }
    
    public class PartRenderingConfig{
        public List<PackRotatableModelObject> rotatableModelObjects = new ArrayList<PackRotatableModelObject>();
    }
}