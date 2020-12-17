package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;

public class JSONVehicle extends AJSONMultiModelProvider<JSONVehicle.VehicleGeneral>{
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
    public List<VehicleConnection> connections;
    public List<VehicleEffect> effects;
    public VehicleRendering rendering;
    
    public class VehicleGeneral extends AJSONMultiModelProvider<JSONVehicle.VehicleGeneral>.General{
    	public boolean isAircraft;
    	public boolean isBlimp;
    	public boolean openTop;
    	public int emptyMass;
    	@Deprecated
    	public String type;
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
        public float downForce;
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
    	
    	@Deprecated
        public Point3d hitchPos;
    	@Deprecated
        public List<String> hitchTypes;
    	@Deprecated
        public Point3d hookupPos;
    	@Deprecated
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
        public boolean isPermanent;
        public boolean inverseMirroring;
        public List<String> types;
        public List<String> customTypes;
        public float minValue;
        public float maxValue;
        public float minYaw;
        public float maxYaw;
        public float minPitch;
        public float maxPitch;
        @Deprecated
        public VehiclePart additionalPart;
        public List<VehiclePart> additionalParts;
        public List<String> linkedDoors;
        public String defaultPart;
        @Deprecated
        public String linkedDoor;
        
        //Animation variables.
        public List<JSONAnimationDefinition> animations;
        @Deprecated
        public String translationVariable;
        @Deprecated
        public Point3d translationPosition;
        @Deprecated
        public float translationClampMin;
        @Deprecated
        public float translationClampMax;
        @Deprecated
        public boolean translationAbsolute;
        @Deprecated
        public String rotationVariable;
        @Deprecated
        public Point3d rotationPosition;
        @Deprecated
        public Point3d rotationAngles;
        @Deprecated
        public float rotationClampMin;
        @Deprecated
        public float rotationClampMax;
        @Deprecated
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
        public List<VehicleEffect> seatEffects; 
        
        //Engine-specific part variables.
        @Deprecated
        public float[] exhaustPos;
        @Deprecated
        public float[] exhaustVelocity;
        @Deprecated
        public List<ExhaustObject> exhaustObjects;
        public List<ParticleObject> particleObjects;
        public float intakeOffset;
        
        @Deprecated
    	public class ExhaustObject{
        	public Point3d pos;
        	public Point3d velocity;
        	public float scale;
        }
        
    	public class ParticleObject{
    		public String type;
    		public String color;
    		public String toColor;
    		public float transparency;
    		public float toTransparency;
    		public float scale;
    		public float toScale;
        	public Point3d pos;
        	@Deprecated
        	public float velocity;
        	public Point3d velocityVector;
        	public int quantity;
        	public int duration;
    	}
    }
    
    public class VehicleCollisionBox{
        public Point3d pos;
        public float width;
        public float height;
        public boolean isInterior;
        public boolean collidesWithLiquids;
        public float armorThickness;
    }
    
    public class VehicleDoor{
        public String name;
    	public Point3d closedPos;
        public Point3d openPos;
        public float width;
        public float height;
        public boolean closedByDefault;
        public boolean closeOnMovement;
        public boolean activateOnSeated;
    }
    
    public class VehicleConnection{
    	public boolean hookup;
    	public String type;
    	public Point3d pos;
    	public boolean mounted;
    	public List<VehicleConnectionConnector> connectors;
    	
    	public class VehicleConnectionConnector{
        	public String modelName;
        	public Point3d startingPos;
        	public Point3d endingPos;
        	public double segmentLength;
        }
    }
    
    public class VehicleEffect{
    	public String name;
    	public int duration;
    	public int amplifier;
    }
    
    public class PackInstrument{
        public Point3d pos;
        public Point3d rot;
        public float scale;
        public int hudX;
        public int hudY;
        public float hudScale;
        public int optionalPartNumber;
        public String defaultInstrument;
        public List<JSONAnimationDefinition> animations;
    }
    
    public class VehicleRendering{
        public String hudTexture;
        public String panelTexture;
        public String panelTextColor;
        public String panelLitTextColor;
        public List<JSONText> textObjects;
        public List<JSONAnimatedObject> animatedObjects;
        public List<VehicleCameraObject> cameraObjects;
        public List<String> customVariables;
        
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
    
    public class VehicleCameraObject{
    	public Point3d pos;
    	public Point3d rot;
    	public float fovOverride;
    	public String overlay;
    	public List<JSONAnimationDefinition> animations;
    }
}
