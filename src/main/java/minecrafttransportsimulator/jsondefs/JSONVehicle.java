package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONVehicle extends AJSONPartProvider{
	@JSONRequired
    public VehicleMotorized motorized;
    @Deprecated
    public VehiclePlane plane;
    @Deprecated
    public VehicleBlimp blimp;
    @Deprecated
    public VehicleCar car;
    public List<VehicleConnection> connections;
    
    public class VehicleMotorized{
    	public boolean isAircraft;
    	public boolean isBlimp;
    	public boolean isTrailer;
    	public boolean isFrontWheelDrive;
    	public boolean isRearWheelDrive;
    	public boolean hasOpenTop;
    	public boolean hasCruiseControl;
    	public boolean hasAutopilot;
    	public boolean hasFlaps;
    	public boolean hasSkidSteer;
    	public int emptyMass;
    	public int fuelCapacity;
    	public int defaultFuelQty;
    	public int gearSequenceDuration;
        public float downForce;
    	public float axleRatio;
    	public float brakingFactor;
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
        public String hudTexture;
        public String panelTexture;
        public String panelTextColor;
        public String panelLitTextColor;
        
    	@Deprecated
    	public boolean isBigTruck;
        @Deprecated
    	public String hornSound;
        @Deprecated
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
}
