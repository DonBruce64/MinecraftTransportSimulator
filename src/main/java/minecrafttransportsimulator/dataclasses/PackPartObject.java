package minecrafttransportsimulator.dataclasses;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackRotatableModelObject;

public class PackPartObject{
	public PartGeneralConfig general;
    public PartEngineConfig engine;
    public PartWheelConfig wheel;
    public PartPontoonConfig pontoon;
    public PartSkidConfig skid;
    public PartTreadConfig tread;
    public PartPropellerConfig propeller;
    public PartCrateConfig crate;
    public PartBarrelConfig barrel;
    public PartGunConfig gun;
    public PartBulletConfig bullet;
    public PartEffectorConfig effector;
    public PartCustomConfig custom;
    public List<PackPart> subParts = new ArrayList<PackPart>();
    public PartRenderingConfig rendering;

    public class PartGeneralConfig{
    	public String type;
    	public String modelName;
    	public String[] materials;
    	public String customType;
    	public boolean disableMirroring;
    }
    
    public class PartEngineConfig{
    	public boolean isAutomatic;
    	public byte starterPower;
    	public byte starterDuration;
    	public int maxRPM;
    	public float fuelConsumption;
    	public float[] gearRatios;
    	public String fuelType;
    }
    
    public class PartWheelConfig{
    	public float diameter;
        public float motiveFriction;
        public float lateralFriction;
    }
    
    public class PartSkidConfig{
    	public float width;
    	public float lateralFriction;
    }
    
    public class PartPontoonConfig{
    	public float width;
    	public float lateralFriction;
        public float extraCollisionBoxOffset;
    }
    
    public class PartTreadConfig{
    	public float width;
    	public float motiveFriction;
        public float lateralFriction;
        public float extraCollisionBoxOffset;
        public float spacing;
        public float[] yPoints;
        public float[] zPoints;
        public float[] angles;
    }
    
    public class PartPropellerConfig{
    	public boolean isDynamicPitch;
    	public byte numberBlades;
    	public short pitch;
    	public int diameter;
    	public int startingHealth;
    }
    
    public class PartCrateConfig{
    	public byte rows;
    }
    
    public class PartBarrelConfig{
    	public int capacity;
    }
    
    public class PartGunConfig{
    	public boolean autoReload;
    	public int capacity;
    	public int fireDelay;
    	public int reloadTime;
    	public int muzzleVelocity;
    	public int minPitch;
    	public int maxPitch;
    	public int minYaw;
    	public int maxYaw;
    	public float diameter;
    	public float length;
    }
    
    public class PartBulletConfig{
    	public String type;
    	public int quantity;
    	public float diameter;
    	public float texturePercentage;
    }
    
    public class PartEffectorConfig{
    	public int blocksWide;
    }
    
    public class PartCustomConfig{
    	public float width;
    	public float height;
    }
    
    public class PartRenderingConfig{
        public List<PackRotatableModelObject> rotatableModelObjects = new ArrayList<PackRotatableModelObject>();
    }
}