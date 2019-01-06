package minecrafttransportsimulator.dataclasses;

import java.util.ArrayList;
import java.util.List;

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
    	public boolean openTop;
    	public int emptyMass;
    	public String type;
        public String[] materials;
    }
    
    public class PackFileDefinitions{
    	public String subName;
    	public String[] extraMaterials;
    }
    
    public class PackMotorizedConfig{
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
        public float[] rot;
        public boolean turnsWithSteer;
        public boolean isController;
        public boolean overrideMirror;
        public List<String> types;
        public List<String> customTypes;
        public float minValue;
        public float maxValue;
        public float[] dismountPos;
        public PackPart additionalPart;
    }
    
    public class PackCollisionBox{
        public float[] pos;
        public float width;
        public float height;
        public boolean isInterior;
    }
    
    public class PackInstrument{
        public float[] pos;
        public float[] rot;
        public float[] hudpos;
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
        public List<PackTranslatableModelObject> translatableModelObjects = new ArrayList<PackTranslatableModelObject>();
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
    
    public class PackTranslatableModelObject{
    	public String partName;
    	public float[] translationAxis;
    	public String translationVariable;
    }
}
