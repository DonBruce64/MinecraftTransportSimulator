package minecrafttransportsimulator.dataclasses;

import java.util.ArrayList;
import java.util.List;

public class MTSPackObject{
	public List<PackFileDefinitions> definitions = new ArrayList<PackFileDefinitions>();
	public PackGeneralConfig general;
    public PackMotorizedConfig motorized;
    public PackPlane plane;
    public PackCar car;
    public List<PackPart> parts = new ArrayList<PackPart>();
    public List<PackCollisionBox> collision = new ArrayList<PackCollisionBox>();
    public PackRenderingConfig rendering;

    public class PackFileDefinitions{
    	public String uniqueName;
    	public String itemDisplayName;
    	public String modelTexture;
        public String backplateTexture;
        public String mouldingTexture;
    	public String[] recipe;
    }
    
    public class PackGeneralConfig{
    	public boolean openTop;
    	public int emptyMass;
    	public int displayTextMaxLength;
        public String name;
    	public String type;
    	public String description;
        public String defaultDisplayText;
    }
    
    public class PackMotorizedConfig{
    	public int fuelCapacity;
    	public String lightSetup;
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
        public boolean is4WD;
        public float dragCoefficient;
        public String hornSound;
    }
    
    public class PackPart{
        public float[] pos;
        public boolean turnsWithSteer;
        public boolean isController;
        public String[] names;
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
        public String staticInstrument;
    }
    
    public class PackControl{
        public float[] pos;
        public int[] hudpos;
        public String controlName;
    }
    
    public class PackRenderingConfig{
        public String modelName;
        public List<PackWindow> windows = new ArrayList<PackWindow>();
        public List<PackDisplayText> textMarkings = new ArrayList<PackDisplayText>();
        public List<PackLight> lights = new ArrayList<PackLight>();
        public List<PackBeacon> beacons = new ArrayList<PackBeacon>();
        public List<PackRotatableModelObject> rotatableModelObjects = new ArrayList<PackRotatableModelObject>();
    }
    
    public class PackWindow{
    	public float[] pos1;
    	public float[] pos2;
    	public float[] pos3;
    	public float[] pos4;
    }
    
    public class PackDisplayText{
        public float[] pos;
        public float[] rot;
        public float scale;
        public String color;
    }
    
    public class PackLight{
        public float[] pos;
        public float[] rot;
        public float[] lightRot;
        public int width;
        public int length;
        public int brightness;
        public int switchNumber;
        public String color;
        public int beamDistance;
        public int beamDiameter;
    }
    
    public class PackBeacon{
        public float[] pos;
        public float[] rot;
        public int width;
        public int length;
        public int height;
        public int brightness;
        public int switchNumber;
        public String color;
        public boolean flashing;
    }
    
    public class PackRotatableModelObject{
    	public String partName;
    	public float[] rotationPoint;
    	public float[] rotationAxisDir;
    	public String rotationVariable;
    }
}
