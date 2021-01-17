package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONVehicle extends AJSONMultiModelProvider<JSONVehicle.VehicleGeneral>{
	@JSONRequired
    public VehicleMotorized motorized;
    @Deprecated
    public VehiclePlane plane;
    @Deprecated
    public VehicleBlimp blimp;
    @Deprecated
    public VehicleCar car;
    @JSONRequired
    public List<VehiclePart> parts;
    @JSONRequired
    public List<VehicleCollisionBox> collision;
    public List<VehicleDoor> doors;
    public List<VehicleConnection> connections;
    public List<JSONPotionEffect> effects;
    @JSONRequired
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
    	public boolean hasSkidSteer;
    	public int fuelCapacity;
    	public int defaultFuelQty;
    	public int gearSequenceDuration;
        public float downForce;
    	public float axleRatio;
    	public float maxTiltAngle;
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
    	@JSONRequired
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
    	@JSONRequired
        public Point3d pos;
        public Point3d rot;
        public boolean turnsWithSteer;
        public boolean isController;
        public boolean isPermanent;
        public boolean isSpare;
        public boolean inverseMirroring;
        public boolean canDisableGun;
        public boolean forceCameras;
        @JSONRequired
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
        public List<JSONPotionEffect> seatEffects; 
        
        //Engine-specific part variables.
        @Deprecated
        public float[] exhaustPos;
        @Deprecated
        public float[] exhaustVelocity;
        @Deprecated
        public List<ExhaustObject> exhaustObjects;
        public List<JSONParticleObject> particleObjects;
        public float intakeOffset;
        
        @Deprecated
    	public class ExhaustObject{
        	@SuppressWarnings("hiding")
			public Point3d pos;
        	public Point3d velocity;
        	public float scale;
        }
    }
    
    public class VehicleCollisionBox{
    	@JSONRequired
    	public Point3d pos;
        public float width;
        public float height;
        public boolean isInterior;
        public boolean collidesWithLiquids;
        public float armorThickness;
    }
    
    public class VehicleDoor{
    	@JSONRequired
    	public String name;
        @JSONRequired
        public Point3d closedPos;
    	@JSONRequired
    	public Point3d openPos;
        public float width;
        public float height;
        public boolean closedByDefault;
        public boolean closeOnMovement;
        public boolean activateOnSeated;
        public boolean ignoresClicks;
    }
    
    public class VehicleConnection{
    	public boolean hookup;
    	@JSONRequired
    	public String type;
    	@JSONRequired
    	public Point3d pos;
    	public boolean mounted;
    	public List<VehicleConnectionConnector> connectors;
    	
    	public class VehicleConnectionConnector{
    		@JSONRequired
        	public String modelName;
    		@JSONRequired
        	public Point3d startingPos;
    		@JSONRequired
        	public Point3d endingPos;
        	public double segmentLength;
        }
    }
    
    public class PackInstrument{
    	@JSONRequired
    	public Point3d pos;
    	@JSONRequired
        public Point3d rot;
        public float scale;
        public int hudX;
        public int hudY;
        public float hudScale;
        public int optionalPartNumber;
        public String defaultInstrument;
        public List<JSONAnimationDefinition> animations;
    }
    
    public class VehicleRendering extends JSONRendering{
        public String hudTexture;
        public String panelTexture;
        public String panelTextColor;
        public String panelLitTextColor;
        
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
}
