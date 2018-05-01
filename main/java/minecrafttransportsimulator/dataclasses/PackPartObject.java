package minecrafttransportsimulator.dataclasses;

public class PackPartObject{
	public PartGeneralConfig general;
    public PartEngineConfig engine;
    public PartGroundDeviceConfig groundDevice;

    public class PartGeneralConfig{
    	public String packID;
    	public String type;
    	public String partUniqueName;
    	public String partDisplayName;
    	public String[] craftingIngredients;
    }
    
    public class PartEngineConfig{
    	public String type;
    	public boolean isAutomatic;
    	public byte starterPower;
    	public byte starterDuration;
    	public byte maxRPM;
    	public float fuelConsumption;
    	public float[] gearRatios;
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
}
