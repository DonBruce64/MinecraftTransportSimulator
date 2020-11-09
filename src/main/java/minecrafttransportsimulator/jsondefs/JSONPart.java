package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRendering;

import minecrafttransportsimulator.baseclasses.Point3d;

public class JSONPart extends AJSONMultiModelProvider<JSONPart.JSONPartGeneral>{
    public JSONPartEngine engine;
    public JSONPartGroundDevice ground;
    public JSONPartPropeller propeller;
    public JSONPartGun gun;
    public JSONPartBullet bullet;
    public JSONPartInteractable interactable;
    public JSONPartEffector effector;
    public JSONPartCustom custom;
    public List<VehiclePart> subParts;
    public List<VehicleCollisionBox> collision;
    public VehicleRendering rendering;
    
    @Deprecated
    public PartWheel wheel;
    @Deprecated
    public PartPontoon pontoon;
    @Deprecated
    public PartSkid skid;
    @Deprecated
    public PartTread tread;

    public class JSONPartGeneral extends AJSONMultiModelProvider<JSONPart.JSONPartGeneral>.General{
    	public String type;
    	public String customType;
    	public boolean disableMirroring;
    	public boolean useVehicleTexture;
    }
    
    public class JSONPartEngine{
    	public boolean isAutomatic;
    	public boolean isSteamPowered;
    	public boolean flamesOnStartup;
    	public boolean isCrankingNotPitched;
    	public int starterPower;
    	public int shiftSpeed;
    	public int revResistance;
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
			public float pitchLength;
			public float volumeIdle;
			public float volumeMax;
			public float volumeLength;
			public int pitchCenter;
			public int volumeCenter;
			public boolean pitchAdvanced;
			public boolean volumeAdvanced;
		}
    }
    
    public class JSONPartGroundDevice{
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
    
    public class JSONPartPropeller{
    	public boolean isDynamicPitch;
    	public boolean isRotor;
    	public short pitch;
    	public int diameter;
    	public int startingHealth;
    }
    
    public class JSONPartGun{
    	public boolean autoReload;
    	public boolean isTurret;
    	public int capacity;
    	public int fireDelay;
    	public int reloadTime;
    	public int muzzleVelocity;
    	public List<Point3d> muzzlePositions;
    	public float minPitch;
    	public float maxPitch;
    	public float minYaw;
    	public float maxYaw;
    	public float diameter;
    	public float length;
    }
    
    public class JSONPartBullet{
    	@Deprecated
    	public String type;
    	public List<String> types;
    	public int quantity;
    	public float diameter;
    	public float blastStrength;
    	public float armorPenetration;
    	public int burnTime;
    	public float maxOffAxis;
    	public float turnFactor;
    	public float angleOfAttack;
    	public float proximityFuze;
    	public int airBurstDelay;
    	public List<ParticleObject> particleObjects;
    	
    	public class ParticleObject{
    		public String type;
    		public String color;
    		public String toColor;
    		public float transparency;
    		public float toTransparency;
    		public float scale;
    		public float toScale;
        	public Point3d pos;
        	public float velocity;
        	public int quantity;
    	}
    }
    
    public class JSONPartInteractable{
    	public String interactionType;
    	public boolean feedsVehicles;
    	public int inventoryUnits;
    }
    
    public class JSONPartEffector{
    	public String type;
    	public int blocksWide;
    }
    
    public class JSONPartCustom{
    	public float width;
    	public float height;
    }
    @Deprecated
    public class PartWheel{
    	public float diameter;
        public float motiveFriction;
        public float lateralFriction;
    }
    @Deprecated
    public class PartSkid{
    	public float width;
    	public float lateralFriction;
    }
    @Deprecated
    public class PartPontoon{
    	public float width;
    	public float lateralFriction;
        public float extraCollisionBoxOffset;
    }
    @Deprecated
    public class PartTread{
    	public float width;
    	public float motiveFriction;
        public float lateralFriction;
        public float extraCollisionBoxOffset;
        public float spacing;
    }
}
