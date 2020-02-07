package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.List;

public class JSONVehicle extends AJSONCraftable<JSONVehicle.VehicleGeneral>{
	/**A generic name for this vehicle.  This is simply the {@link AJSONItem#systemName}, minus
	 * the {@link VehicleDefinition#subName}.  Set after JSON is parsed into an object and
	 * used when we want to treat this vehicle the same based on it's other definitions, 
	 * usually for rendering of models as those are the same for multiple vehicles.
	 */
	public String genericName;
	
	public List<VehicleDefinition> definitions = new ArrayList<VehicleDefinition>();
    public VehicleMotorizedConfig motorized;
    public VehiclePlane plane;
    public VehicleBlimp blimp;
    public VehicleCar car;
    public List<VehiclePart> parts = new ArrayList<VehiclePart>();
    public List<VehicleCollisionBox> collision = new ArrayList<VehicleCollisionBox>();
    public VehicleRendering rendering;
    
    public class VehicleGeneral extends AJSONCraftable<JSONVehicle.VehicleGeneral>.General{
    	public boolean openTop;
    	public int emptyMass;
    	public String type;
    }
    
    public class VehicleDefinition{
    	public String subName;
    	public String name;
    	public String[] extraMaterials;
    }
    
    public class VehicleMotorizedConfig{
    	public int fuelCapacity;
    	public int defaultFuelQty;
    	public String hornSound;
    	public String sirenSound;
        public float[] hitchPos;
        public String[] hitchTypes;
        public float[] hookupPos;
        public String hookupType;
        public boolean isTrailer;
        public List<PackInstrument> instruments = new ArrayList<PackInstrument>();
    }
    
    public class VehiclePlane{
        public boolean hasFlaps;
        public float wingSpan;
        public float wingArea;
        public float tailDistance;
        public float rudderArea;
        public float elevatorArea;
    }
    
    public class VehicleBlimp{
        public float crossSectionalArea;
        public float tailDistance;
        public float rudderArea;
        public float ballastVolume;
    }
    
    public class VehicleCar{
        public boolean isBigTruck;
        public boolean isFrontWheelDrive;
        public boolean isRearWheelDrive;
        public float axleRatio;
        public float dragCoefficient;
    }
    
    public class VehiclePart{
        public float[] pos;
        public float[] rot;
        public boolean turnsWithSteer;
        public float[] steerRotationOffset;
        public boolean isController;
        public boolean inverseMirroring;
        public List<String> types;
        public List<String> customTypes;
        public float minValue;
        public float maxValue;
        public float[] dismountPos;
        public float[] exhaustPos;
        public float[] exhaustVelocity;
        public float intakeOffset;
        public VehiclePart additionalPart;
        public float[] treadYPoints;
        public float[] treadZPoints;
        public float[] treadAngles;
        public String defaultPart;
    }
    
    public class VehicleCollisionBox{
        public float[] pos;
        public float width;
        public float height;
        public boolean isInterior;
        public boolean collidesWithLiquids;
    }
    
    public class PackInstrument{
        public float[] pos;
        public float[] rot;
        public float[] hudpos;
        public float scale;
        public float hudScale;
        public byte optionalEngineNumber;
    }
    
    public class VehicleRendering{
    	public int displayTextMaxLength;
        public boolean textLighted;
        public String defaultDisplayText;
        public float[] hudBackplaneTexturePercentages;
        public float[] hudMouldingTexturePercentages;
        public List<VehicleDisplayText> textMarkings = new ArrayList<VehicleDisplayText>();
        public List<VehicleRotatableModelObject> rotatableModelObjects = new ArrayList<VehicleRotatableModelObject>();
        public List<VehicleTranslatableModelObject> translatableModelObjects = new ArrayList<VehicleTranslatableModelObject>();
    }
    
    public class VehicleDisplayText{
    	public float[] pos;
        public float[] rot;
        public float scale;
        public String color;
    }
    
    public class VehicleRotatableModelObject{
    	public String partName;
    	public float[] rotationPoint;
    	public float[] rotationAxis;
    	public String rotationVariable;
    }
    
    public class VehicleTranslatableModelObject{
    	public String partName;
    	public float[] translationAxis;
    	public String translationVariable;
    }
}
