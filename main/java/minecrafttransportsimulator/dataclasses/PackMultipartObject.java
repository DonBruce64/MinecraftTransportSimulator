package minecrafttransportsimulator.dataclasses;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.systems.PackParserSystem;

public class PackMultipartObject{
	public PackGeneralConfig general;
	public List<PackFileDefinitions> definitions = new ArrayList<PackFileDefinitions>();
    public PackMotorizedConfig motorized;
    public PackPlane plane;
    public PackCar car;
    public List<PackPart> parts = new ArrayList<PackPart>();
    public List<PackCollisionBox> collision = new ArrayList<PackCollisionBox>();
    public PackRenderingConfig rendering;
    
    public class PackGeneralConfig{
    	/**Set this to true to disable the quieter sounds while riding this vehicle.**/
    	public boolean openTop;
    	/**Vehicle mass while empty (kg).**/
    	public int emptyMass;
        /**Vehicle type.  See {@link PackParserSystem} for a complete list.**/
    	public String type;
        /**Ingredients that need to be present to craft this vehicle.
         * Should be a list of items in the format of [itemname]:[metadata]:[qty].
         * Note that the itemname MUST contain the modId if modded materials are used.
         * This is the same format as the /give command, so use that for reference.
         * As an example, 
         */
        public String[] materials;
    }
    
    public class PackFileDefinitions{
    	/**A bit of text to be appended to the main vehicle name to allow for sub-types (different colors, configurations, etc.).
    	 * Completely optional and may be left blank for single-vehicle JSONs.**/
    	public String subName;
    	/**Additional materials, if any, that need to be present to create this specific model of vehicle.
    	 * Note that these can be omitted even for JSONs that have multiple definitions in one file.
    	 * Formatting is the same as the general materials.
    	 **/
    	public String[] extraMaterials;
    }
    
    public class PackMotorizedConfig{
    	/**Capacity of the vehicle's fuel tank (in mB).**/
    	public int fuelCapacity;
    	public String hornSound;
    	public String sirenSound;
        public List<PackInstrument> instruments = new ArrayList<PackInstrument>();
        public List<PackControl> controls = new ArrayList<PackControl>();
    }
    
    public class PackPlane{
        public boolean hasFlaps;
        public float wingSpan;
        public float wingArea;
        public float tailDistance;
        public float rudderArea;
        public float elevatorArea;
        public float defaultElevatorAngle;
    }
    
    public class PackCar{
        public boolean isBigTruck;
        public boolean isFrontWheelDrive;
        public boolean isRearWheelDrive;
        public float dragCoefficient;
    }
    
    public class PackPart{
        public float[] pos;
        public boolean turnsWithSteer;
        public boolean isController;
        public List<String> types;
        public List<String> customTypes;
        public float minValue;
        public float maxValue;
        public PackPart additionalPart;
    }
    
    public class PackCollisionBox{
        public float[] pos;
        public float width;
        public float height;
    }
    
    public class PackInstrument{
        public float[] pos;
        public float[] rot;
        public int[] hudpos;
        public float scale;
        public float hudScale;
        public byte optionalEngineNumber;
    }
    
    public class PackControl{
        public float[] pos;
        public int[] hudpos;
        public String controlName;
    }
    
    public class PackRenderingConfig{
    	public int displayTextMaxLength;
    	public byte numberWindows;
        public String defaultDisplayText;
        public float[] hudBackplaneTexturePercentages;
        public float[] hudMouldingTexturePercentages;
        public List<PackDisplayText> textMarkings = new ArrayList<PackDisplayText>();
        public List<PackRotatableModelObject> rotatableModelObjects = new ArrayList<PackRotatableModelObject>();
    }
    
    public class PackDisplayText{
        public float[] pos;
        public float[] rot;
        public float scale;
        public String color;
    }
    
    public class PackRotatableModelObject{
    	public String partName;
    	public float[] rotationPoint;
    	public float[] rotationAxis;
    	public String rotationVariable;
    }
}
