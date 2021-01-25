package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleConnection;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleDoor;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRendering;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONPart extends AJSONMultiModelProvider<JSONPart.JSONPartGeneral>{
	@JSONRequired(dependentField="type", dependentValues={"engine"}, subField="general")
    public JSONPartEngine engine;
	@JSONRequired(dependentField="type", dependentValues={"ground"}, subField="general")
    public JSONPartGroundDevice ground;
	@JSONRequired(dependentField="type", dependentValues={"propeller"}, subField="general")
    public JSONPartPropeller propeller;
	@JSONRequired(dependentField="type", dependentValues={"seat"}, subField="general")
    public JSONPartSeat seat;
	@JSONRequired(dependentField="type", dependentValues={"gun"}, subField="general")
    public JSONPartGun gun;
	@JSONRequired(dependentField="type", dependentValues={"bullet"}, subField="general")
    public JSONPartBullet bullet;
	@JSONRequired(dependentField="type", dependentValues={"interactable"}, subField="general")
    public JSONPartInteractable interactable;
	@JSONRequired(dependentField="type", dependentValues={"effector"}, subField="general")
    public JSONPartEffector effector;
	@JSONRequired(dependentField="type", dependentValues={"generic"}, subField="general")
    public JSONPartGeneric generic;
    public List<VehiclePart> subParts;
    public List<VehicleCollisionBox> collision;
    public List<VehicleDoor> doors;
    public List<VehicleConnection> connections;
    //TODO make this regular rendering when parts are all converted.
    public VehicleRendering rendering;
    
    @Deprecated
    public PartCustom custom;
    @Deprecated
    public PartWheel wheel;
    @Deprecated
    public PartPontoon pontoon;
    @Deprecated
    public PartSkid skid;
    @Deprecated
    public PartTread tread;

    public class JSONPartGeneral extends AJSONMultiModelProvider<JSONPart.JSONPartGeneral>.General{
    	@JSONRequired
    	public String type;
    	public String customType;
    	public boolean disableMirroring;
    	public boolean useVehicleTexture;
    }
    
    public class JSONPartEngine{
		@JSONDescription("Should the engine change gears on its own.  This only affects cars and will prevent users from shifting into higher or lower gears using shiftUp and shiftDown. Instead, the engine will attempt to choose the best gear for the situation.  Note that MTS's automatic transmission system isn't the best and may get confused when gear ratios are close together.  For this reason, it is recommended to either use manual transmissions on vehicles with more than 5-6 gears, or to define the RPM at which a gear is shifted up or down via upShiftRPM and downShiftRPM.")
    	public boolean isAutomatic;
		
		//FIXME add description
    	public boolean isSteamPowered;
		
		@JSONDescription("Should the engine spawn flames when starting?  If true, flames will be spawned at the exhaustPos on the vehicle this engine is in.  Note that if the vehicle doesn't have any exhaustPos, no flames will be spawned, even if this is set.")
    	public boolean flamesOnStartup;
		
		@JSONDescription("If your cranking sound is pitched or not. Useful for sounds that are designed in a particular way that smoothly transition from cranking to idle.  This does not affect the pitch change incurred due to a low battery; this only affects the pitch change that naturally happens due to RPM increases during engine startup.")
    	public boolean isCrankingNotPitched;
		
		@JSONDescription("This is how much “oomph” the starter outputs on a single firing.  When the starter key is held the engine RPM will be increased by this amount every 4 ticks, or every 0.2 seconds.  Note that for engines with high loads, such as those with larger propellers, its quite possible to make a starter power that literally can't start the engine.")
    	public int starterPower;
		
		@JSONDescription("For automatic gear boxes only.  This parameter is how long, in ticks, to wait to shift gears after shifting gears once.  This is needed as there's a slight delay between shifting and the engine RPM catching up, so without this parameter engines would just bounce between first and last gear.  Adjust this parameter to fit the power output and ratio spacing of your engine.  Engines with lots of gears or high power will likely need a smaller number than a gutless 3-speed.")
    	public int shiftSpeed;
		
		@JSONDescription("For manual gear boxes only.  This parameter defines how long the clutch variable is set to 1 for up-shifting and down-shifting animations.  Has no other affect than this, as vehicles don't simulate clutches.  That's a bit TOO realistic!")
    	public int clutchTime;
		
		@JSONDescription("How long it takes for the engine RPM to “catch up”, or how fast it revs. The lower the value is, the faster the engine will react to RPM changes. If ignored, MTS will set the value of this to a default of 10.")
    	public int revResistance;
		
		@JSONDescription("The max RPM for this engine.  This is how fast the engine will try to go with no load at 100% throttle.  The red-line value (max safe RPM) is lower than and auto-caluclated from this value.")
    	public int maxRPM;
		
		@JSONDescription("How many mb/t the engine uses, and how powerful the engine is. This value is calculated at maxRPM and scaled down based on the percentage of speed the engine is at, so an engine with a maxRPM of 4000 with a fuel consumption of 1.0 running at 2000RPM is actually running at a rate of 0.5.  This value is the key to determining the raw power output of your engine, as it's directly related to how much force the engine can output.  However, even the most powerful engines won't do much if they're geared improperly, and even weak engines with low consumption can reach high speeds if they can maintain high RPMs.")
    	public float fuelConsumption;
		
		@JSONDescription("If this is greater than 0, this engine will provide jet thrust.  This thrust is based on the bypassRatio and various fuel consumption parameters.  Note that you can set this on engines that provide power through other means, which will cause the jet power to be added to whatever other power the engine provides.  Useful for turboprops, or just pure jets.")
    	public float jetPowerFactor;
		
		@JSONDescription("Used only with jetPowerFactor for jet thrust calculations.  Higher bypass ratio engines will have better power when turning fast, and will allow for lower fuel consumptions, but will also have a lower top-speed.")
    	public float bypassRatio;
		
		@JSONDescription("This is a constant ratio that will be used for any propellers attached to this engine, and will override the value in gearRatios.  Useful when you want to gear-down a propeller on a vehicle that's normally land-bound.")
    	public float propellerRatio;
		
    	@JSONRequired
    	@JSONDescription("A list of gear ratios for this engine.  This should contain at a minimum 3 entries.  1 reverse gear, a 0 for neutral, and 1 or more forwards gears.  For vehicles such as aircraft that don't shift gears, a simple [-1, 0, 1] will do.  That is, unless you want the engine to come with a reducer, in which case use the appropriate ratio.  For example, having a setup of [-2, 0, 2, 1] would make the wheels/propeller of the vehicle turn half as fast as the engine in first gear and reverse, but the same speed in second. You can have at most 127 gears per engine, and only cars, boats, and blimps have gearboxes to shift into reverse, so keep this in mind when designing engines for these applications.  If you're having trouble with cross-platform applications, you might need to set a propellerRatio instead.")
		public float[] gearRatios;
		
		@JSONDescription("A list of engine RPM speeds that tells MTS when to shift up a gear.  This may be added in conjunction with downShiftRPM to specify the shift points in automatic transmissions to avoid bad shifting choices by the MTS code.")
		public int[] upShiftRPM;
		
		@JSONDescription("The same as upShiftRPM, but instead tells MTS when to shift down a gear. Be careful while configuring this and upShiftRPM, as you can create an infinite shifting loop if the RPMs are too close.  A rule of thumb is to use what RPM your engine lands at after shifting up and subtract a few hundred RPM.")
		public int[] downShiftRPM;
		
		@JSONRequired
    	@JSONDescription("What type of fuel this engine uses.  This is NOT the name of the fluid this engine uses, rather it's a generic type that basically lumps it in with other engines. Gasoline and diesel are two of the most common.  This type system allows for packs to group their engines by what fuels they take to make them more uniform, and allows server owners to configure their fluids to work with specific types of engines.")
		public String fuelType;
		
		@JSONDescription("Same as fuelConsumption, but for the supercharger on this engine (if any). Note that vehicles will only take the base fuel consumption of the engine into account when checking min/max values. This is to allow for higher-performance engines to be added to vehicles without poking pack creators to increase their maximum values. This variable can be omitted if your engine doesn’t have a supercharger.")
		public float superchargerFuelConsumption;
		
		@JSONDescription("The efficiency of the supercharger on this engine (if any). The supercharger fuel consumption of this engine (but not the base fuel consumption) will be multiplied by this value. A value of 1 will make the supercharger add the same amount of power for its fuel consumption as adding that number to the base fuel consumption, so make sure to set it to greater than that if you want your supercharger to have any power benefits!\nThis also affects the engine wear calculations, with a value of 1 or below leaving them the same as what it would be without a supercharger. By setting this value to significantly below 1 you can simulate inefficient, gas-guzzling engines if you have a high supercharger fuel consumption, as it won’t add much power but will make the engine use a lot more fuel.\nAs a final note: supercharged engines heat up faster than non-supercharged engines. A supercharger efficiency of 0 would make the calculations the same as a non-supercharged engine in this case; setting it to 1 will not make the engine heat up twice as fast. This is intended behavior, as real supercharged engines heat up faster than naturally aspirated ones even if the supercharger itself isn’t very efficient.  This variable can be omitted if your engine doesn’t have a supercharger.")
		public float superchargerEfficiency;
		
		@JSONDescription("This parameter is a list of sounds and properties that may be used in lieu of the default MTS sound set.  If this is included, neither the _running nor _supercharger sound will be played.  Instead, MTS will use the definitions set in this list to play the sounds.  Each entry is one sound.  When the engine starts, all sounds will be set to play, though it is quite possible to not hear multiple sounds in the set if their volume ends up at 0.  The parameters of each entry are as follows:")
		public EngineSound customSoundset[];
		
		public class EngineSound{
			@JSONRequired
			@JSONDescription("The name of the sound.  Similar to horns and sirens, this is in the form of packID:soundName.")
			public String soundName;
			
			@JSONDescription("The pitch of the sound at idle RPM.")
			public float pitchIdle;
			
			@JSONDescription("The pitch of the sound at maxRPM.")
			public float pitchMax;
			
			@JSONDescription("Similar to volumeLength, but for pitch. The longer the pitchLength, the higher the pitch will get, though this has a small effect over small pitchLength values.")
			public float pitchLength;
			
			@JSONDescription("The volume of the sound at idle RPM.")
			public float volumeIdle;
			
			@JSONDescription("The volume of the sound at maxRPM.")
			public float volumeMax;
			
			@JSONDescription("The amount in RPM the sound will last.")
			public float volumeLength;
			
			@JSONDescription("Same as volumeCenter, but for pitch.")
			public int pitchCenter;
			
			@JSONDescription("The center of where the sound will be located, in RPM")
			public int volumeCenter;
			
			@JSONDescription("Same as volumeAdvanced, but for pitch.")
			public boolean pitchAdvanced;
			
			@JSONDescription("Instead of linearly interpolating between Min and Max, the sound will last for a defined region, fading in and out.")
			public boolean volumeAdvanced;
		}
    }
    
    public class JSONPartGroundDevice{
    	public boolean isWheel;
    	public boolean isTread;
    	public boolean canFloat;
    	@Deprecated
		public boolean canGoFlat;
    	public float width;
    	public float height;
    	public float flatHeight;
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
    
    public class JSONPartSeat{
    	public boolean standing;
    	public float widthScale;
    	public float heightScale;
    }
    
    public class JSONPartGun{
    	public boolean autoReload;
    	public boolean isSemiAuto;
    	public boolean yawIsInternal;
    	public boolean pitchIsInternal;
    	public int capacity;
    	public int fireDelay;
    	public int reloadTime;
    	public int windupTime;
    	public int muzzleVelocity;
    	public List<Point3d> muzzlePositions;
    	public float minPitch;
    	public float maxPitch;
    	public float minYaw;
    	public float maxYaw;
    	public float diameter;
    	public float length;
    	public boolean fireSolo;
    	public boolean resetPosition;
    	public float defaultPitch;
    	public float defaultYaw;
        public List<JSONParticleObject> particleObjects;
        
        public boolean handHeld;
        @JSONRequired(dependentField="handHeld", dependentValues={"true"})
    	public Point3d handHeldNormalOffset;
        @JSONRequired(dependentField="handHeld", dependentValues={"true"})
    	public Point3d handHeldAimedOffset;
    }
    
    public class JSONPartBullet{
    	@Deprecated
    	public String type;
    	@JSONRequired
    	public List<String> types;
    	public int quantity;
    	public float diameter;
    	public float blastStrength;
    	public float armorPenetration;
    	public int burnTime;
    	public int accelerationTime;
    	public int maxVelocity;
    	public float maxOffAxis;
    	public float turnFactor;
    	public float angleOfAttack;
    	public float proximityFuze;
    	public int airBurstDelay;
    	public List<JSONParticleObject> particleObjects;
    }
    
    public class JSONPartInteractable{
    	@JSONRequired
    	public String interactionType;
    	public boolean feedsVehicles;
    	public int inventoryUnits;
    }
    
    public class JSONPartEffector{
    	@JSONRequired
    	public String type;
    	public int blocksWide;
    }
    
    public class JSONPartGeneric{
    	public float width;
    	public float height;
    }
    
    @Deprecated
    public class PartCustom{
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
