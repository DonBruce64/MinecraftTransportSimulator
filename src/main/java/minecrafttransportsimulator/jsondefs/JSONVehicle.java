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
    	public boolean isAircraft;
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
    	public int gearSequenceDuration;
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
        public boolean hasAutopilot;
        public float wingSpan;
        public float wingArea;
        public float tailDistance;
        public float aileronArea;
        public float elevatorArea;
        public float rudderArea;
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
        public boolean hasCruiseControl;
        public float axleRatio;
        public float dragCoefficient;
    }
    
    public class VehiclePart{
    	public boolean isSubPart;
        public double[] pos;
        public double[] rot;
        public boolean turnsWithSteer;
        public boolean isController;
        public boolean inverseMirroring;
        public List<String> types;
        public List<String> customTypes;
        public float minValue;
        public float maxValue;
        public VehiclePart additionalPart;
        public List<VehiclePart> additionalParts;
        public String defaultPart;
        
        //Animation variables.
        public String translationVariable;
        public double[] translationPosition;
        public float translationClampMin;
        public float translationClampMax;
        public boolean translationAbsolute;
        public String rotationVariable;
        public double[] rotationPosition;
        public double[] rotationAngles;
        public float rotationClampMin;
        public float rotationClampMax;
        public boolean rotationAbsolute;
        
        
        //Seat-specific part variables.
        public float[] dismountPos;
        
        //Engine-specific part variables.
        public float[] exhaustPos;
        public float[] exhaustVelocity;
        public float intakeOffset;
        
        //Ground-specific variables.
        public float extraCollisionBoxOffset;
        
        //Tread-specific part variables.
        public float[] treadYPoints;
        public float[] treadZPoints;
        public float[] treadAngles;
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
        public int hudX;
        public int hudY;
        public float hudScale;
        public byte optionalPartNumber;
        public String defaultInstrument;
    }
    
    public class VehicleRendering{
        public String hudTexture;
        public String panelTexture;
        public String panelTextColor;
        public String panelLitTextColor;
        public List<JSONText> textObjects = new ArrayList<JSONText>();
        public List<VehicleAnimatedObject> animatedObjects = new ArrayList<VehicleAnimatedObject>();
    }
    
    public class VehicleAnimatedObject{
    	public String objectName;
    	public String applyAfter;
    	public List<VehicleAnimationDefinition> animations = new ArrayList<VehicleAnimationDefinition>();
    }
    
    public class VehicleAnimationDefinition{
    	public String animationType;
    	public String variable;
    	public double[] centerPoint;
    	public double[] axis;
    	public double offset;
    	public boolean addPriorOffset;
    	public double clampMin;
    	public double clampMax;
    	public boolean absolute;
    	public int duration;
    	public int forwardsDelay;
    	public int reverseDelay;
    	public String sound;
    }
}
