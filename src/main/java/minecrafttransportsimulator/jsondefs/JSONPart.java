package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRendering;

public class JSONPart extends AJSONMultiModel<JSONPart.PartGeneral>{
    public PartEngine engine;
    public PartGroundDevice ground;
    public PartPropeller propeller;
    public PartGun gun;
    public PartBullet bullet;
    public PartInteractable interactable;
    public PartEffector effector;
    public PartCustom custom;
    public List<VehiclePart> subParts = new ArrayList<VehiclePart>();
    public List<VehicleCollisionBox> collision = new ArrayList<VehicleCollisionBox>();
    public VehicleRendering rendering;
    
    //Depreciated blocks.  Used only for legacy compat.
    public PartWheel wheel;
    public PartPontoon pontoon;
    public PartSkid skid;
    public PartTread tread;

    public class PartGeneral extends AJSONMultiModel<JSONPart.PartGeneral>.General{
    	public String type;
    	public String customType;
    	public boolean disableMirroring;
    	public boolean useVehicleTexture;
    }
    
    public class PartEngine{
    	public boolean isAutomatic;
    	public boolean isSteamPowered;
    	public boolean flamesOnStartup;
    	public boolean isCrankingNotPitched;
    	public byte starterPower;
    	public byte shiftSpeed;
    	public int maxRPM;
    	public float fuelConsumption;
    	public float jetPowerFactor;
    	public float bypassRatio;
    	public float propellerRatio;
    	public float[] gearRatios;
		public int[] upShiftRPM;
		public int[] downShiftRPM;
    	public String fuelType;
		public float superchargerFuelConsumption;
		public float superchargerEfficiency;
		public EngineSound customSoundset[];
		
		public class EngineSound{
			public String soundName;
			public float pitchIdle;
			public float pitchMax;
			public float volumeIdle;
			public float volumeMax;
		}
    }
    
    public class PartGroundDevice{
    	public boolean isWheel;
    	public boolean isTread;
    	public boolean canFloat;
		public boolean canGoFlat;
    	public float width;
    	public float height;
    	public float motiveFriction;
        public float lateralFriction;
        public float extraCollisionBoxOffset;
        public float spacing;
    }
    
    public class PartPropeller{
    	public boolean isDynamicPitch;
    	public boolean isRotor;
    	public short pitch;
    	public int diameter;
    	public int startingHealth;
    }
    
    public class PartGun{
    	public boolean autoReload;
    	public boolean isTurret;
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
    
    public class PartInteractable{
    	public String interactionType;
    	public boolean feedsVehicles;
    	public byte inventoryUnits;
    }
    
    public class PartEffector{
    	public String type;
    	public int blocksWide;
    }
    
    public class PartCustom{
    	public float width;
    	public float height;
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
    
    public class PartPontoon{
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
}