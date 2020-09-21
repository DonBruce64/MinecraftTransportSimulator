package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;

public class JSONVehicle extends AJSONCraftable<JSONVehicle.VehicleGeneral>{
	/**A generic name for this vehicle.  This is simply the {@link AJSONItem#systemName}, minus
	 * the {@link VehicleDefinition#subName}.  Set after JSON is parsed into an object and
	 * used when we want to treat this vehicle the same based on it's other definitions, 
	 * usually for rendering of models as those are the same for multiple vehicles.
	 */
	public String genericName;
	
	public List<VehicleDefinition> definitions;
    public VehicleMotorized motorized;
    @Deprecated
    public VehiclePlane plane;
    @Deprecated
    public VehicleBlimp blimp;
    @Deprecated
    public VehicleCar car;
    public List<VehiclePart> parts;
    public List<VehicleCollisionBox> collision;
    public List<VehicleDoor> doors;
    public VehicleRendering rendering;
    
    public class VehicleGeneral extends AJSONCraftable<JSONVehicle.VehicleGeneral>.General{
    	public boolean isAircraft;
    	public boolean isBlimp;
    	public boolean openTop;
    	public int emptyMass;
    	public String type;
    }
    
    public class VehicleDefinition{
    	public String subName;
    	public String name;
    	public String[] extraMaterials;
    }
    
    public class VehicleMotorized{
    	public boolean isBigTruck;
    	public boolean isTrailer;
    	public boolean isFrontWheelDrive;
    	public boolean isRearWheelDrive;
    	public boolean hasCruiseControl;
    	public boolean hasAutopilot;
    	public boolean hasFlaps;
    	public int fuelCapacity;
    	public int defaultFuelQty;
    	public int gearSequenceDuration;
    	public float axleRatio;
    	public float dragCoefficient;
    	public float tailDistance;
    	public float wingSpan;
        public float wingArea;
        public float aileronArea;
        public float elevatorArea;
        public float rudderArea;
        public float crossSectionalArea;
        public float ballastVolume;
    	public String hornSound;
    	public String sirenSound;
        public Point3d hitchPos;
        public String[] hitchTypes;
        public Point3d hookupPos;
        public String hookupType;
        public List<PackInstrument> instruments;
    }
    @Deprecated
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
    @Deprecated
    public class VehicleBlimp{
        public float crossSectionalArea;
        public float tailDistance;
        public float rudderArea;
        public float ballastVolume;
    }
    @Deprecated
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
        public Point3d pos;
        public Point3d rot;
        public boolean turnsWithSteer;
        public boolean isController;
        public boolean inverseMirroring;
        public List<String> types;
        public List<String> customTypes;
        public float minValue;
        public float maxValue;
        @Deprecated
        public VehiclePart additionalPart;
        public List<VehiclePart> additionalParts;
        public String defaultPart;
        public String linkedDoor;
        
        //Animation variables.
        public String translationVariable;
        public Point3d translationPosition;
        public float translationClampMin;
        public float translationClampMax;
        public boolean translationAbsolute;
        public String rotationVariable;
        public Point3d rotationPosition;
        public Point3d rotationAngles;
        public float rotationClampMin;
        public float rotationClampMax;
        public boolean rotationAbsolute;
        
        //Ground-specific variables.
        public float extraCollisionBoxOffset;
        public float treadDroopConstant;
        
        //Tread-specific part variables.
        @Deprecated
        public float[] treadYPoints;
        @Deprecated
        public float[] treadZPoints;
        @Deprecated
        public float[] treadAngles;
        
        //Seat-specific part variables.
        public Point3d dismountPos;
        
        //Engine-specific part variables.
        @Deprecated
        public float[] exhaustPos;
        @Deprecated
        public float[] exhaustVelocity;
        public List<ExhaustObject> exhaustObjects;
        public float intakeOffset;
        
        public class ExhaustObject{
        	public Point3d pos;
        	public Point3d velocity;
        	public float scale;
        }
    }
    
    public class VehicleCollisionBox{
        public Point3d pos;
        public float width;
        public float height;
        public boolean isInterior;
        public boolean collidesWithLiquids;
    }
    
    public class VehicleDoor{
        public String name;
    	public Point3d closedPos;
        public Point3d openPos;
        public float width;
        public float height;
    }
    
    public class PackInstrument{
        public Point3d pos;
        public Point3d rot;
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
        public List<JSONText> textObjects;
        public List<VehicleAnimatedObject> animatedObjects;
        public List<VehicleCameraObject> cameraObjects;
        public List<String> customVariables;
        
        //DEPRECIATED CODE!
        @Deprecated
        public int displayTextMaxLength;
        @Deprecated
        public boolean textLighted;
        @Deprecated
        public String defaultDisplayText;
        @Deprecated
        public List<VehicleDisplayText> textMarkings = new ArrayList<VehicleDisplayText>();
        @Deprecated
        public List<VehicleRotatableModelObject> rotatableModelObjects = new ArrayList<VehicleRotatableModelObject>();
        @Deprecated
        public List<VehicleTranslatableModelObject> translatableModelObjects = new ArrayList<VehicleTranslatableModelObject>();
    }
    @Deprecated
    public class VehicleDisplayText{
    	public Point3d pos;
        public Point3d rot;
        public float scale;
        public String color;
    }
    @Deprecated
    public class VehicleRotatableModelObject{
    	public String partName;
    	public Point3d rotationPoint;
    	public Point3d rotationAxis;
    	public String rotationVariable;
    	public float rotationClampMin;
    	public float rotationClampMax;
    	public boolean absoluteValue;
    }
    @Deprecated
    public class VehicleTranslatableModelObject{
    	public String partName;
    	public Point3d translationAxis;
    	public String translationVariable;
    	public float translationClampMin;
    	public float translationClampMax;
    	public boolean absoluteValue;
    }
    
    public class VehicleAnimatedObject{
    	public String objectName;
    	public String applyAfter;
    	public List<VehicleAnimationDefinition> animations;
    }
    
    public class VehicleAnimationDefinition{
    	public String animationType;
    	public String variable;
    	public Point3d centerPoint;
    	public Point3d axis;
    	public double offset;
    	public boolean addPriorOffset;
    	public double clampMin;
    	public double clampMax;
    	public boolean absolute;
    	public int duration;
    	public int forwardsDelay;
    	public int reverseDelay;
    	public String forwardsSound;
    	public String reverseSound;
    }
    
    public class VehicleCameraObject{
    	public Point3d pos;
    	public Point3d rot;
    	public List<VehicleAnimationDefinition> animations;
    }
}
