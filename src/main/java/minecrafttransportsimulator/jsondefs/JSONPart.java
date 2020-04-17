package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRotatableModelObject;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleTranslatableModelObject;

public class JSONPart extends AJSONMultiModel<JSONPart.PartGeneral>{
    public PartEngine engine;
    public PartWheel wheel;
    public PartPontoonConfig pontoon;
    public PartSkid skid;
    public PartTread tread;
    public PartPropeller propeller;
    public PartCrate crate;
    public PartBarrel barrel;
    public PartGun gun;
    public PartBullet bullet;
    public PartEffector effector;
    public PartCustom custom;
    public List<VehiclePart> subParts = new ArrayList<VehiclePart>();
    public PartRendering rendering;

    public class PartGeneral extends AJSONMultiModel<JSONPart.PartGeneral>.General{
    	public String type;
    	public String customType;
    	public boolean disableMirroring;
    	public boolean useVehicleTexture;
    }
    
    public class PartEngine{
    	    	public boolean isAutomatic;
    	public boolean flamesOnStartup;
    	public byte starterPower;
    	public int maxRPM;
		public boolean isCrankingNotPitched;
		public boolean customShifter;
		public float pitchRev;
		public float pitchIdle;
		public float volumeRev;
		public float volumeIdle;
    	public float fuelConsumption;
    	public float[] gearRatios;
		public int[] upShiftRPM;
		public int[] downShiftRPM;
    	public String fuelType;
		public float superchargerFuelConsumption;
		public float superchargerEfficiency;
	    public float scPitchRev;
		public float scPitchIdle;
		public float scVolumeRev;
		public float scVolumeIdle;
			public String customSound1;
			public float pitchRev1;
			public float pitchIdle1;
			public float volumeRev1;
			public float volumeIdle1;
			public String customSound2;
			public float pitchRev2;
			public float pitchIdle2;
			public float volumeRev2;
			public float volumeIdle2;
			public String customSound3;
			public float pitchRev3;
			public float pitchIdle3;
			public float volumeRev3;
			public float volumeIdle3;
    }
    
    public class PartWheel{
    	public float diameter;
        public float motiveFriction;
        public float lateralFriction;
    }
    
    public class PartSkid{
    	public float width;
    	public float lateralFriction;
    }
    
    public class PartPontoonConfig{
    	public float width;
    	public float lateralFriction;
        public float extraCollisionBoxOffset;
    }
    
    public class PartTread{
    	public float width;
    	public float motiveFriction;
        public float lateralFriction;
        public float extraCollisionBoxOffset;
        public float spacing;
    }
    
    public class PartPropeller{
    	public boolean isDynamicPitch;
    	public byte numberBlades;
    	public short pitch;
    	public int diameter;
    	public int startingHealth;
    }
    
    public class PartCrate{
    	public byte rows;
    }
    
    public class PartBarrel{
    	public int capacity;
    }
    
    public class PartGun{
    	public boolean autoReload;
    	public int capacity;
    	public int fireDelay;
    	public int reloadTime;
    	public int muzzleVelocity;
    	public float minPitch;
    	public float maxPitch;
    	public float minYaw;
    	public float maxYaw;
    	public float diameter;
    	public float length;
    }
    
    public class PartBullet{
    	public String type;
    	public int quantity;
    	public float diameter;
    }
    
    public class PartEffector{
    	public int blocksWide;
    }
    
    public class PartCustom{
    	public float width;
    	public float height;
    }
    
    public class PartRendering{
        public List<VehicleRotatableModelObject> rotatableModelObjects = new ArrayList<VehicleRotatableModelObject>();
        public List<VehicleTranslatableModelObject> translatableModelObjects = new ArrayList<VehicleTranslatableModelObject>();
    }
}