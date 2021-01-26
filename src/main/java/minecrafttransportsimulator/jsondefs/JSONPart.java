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
		@JSONDescription("If true, then this part will be considered a wheel.  Wheels can transmit power from engines to the ground, making them the go-to choice for ground-based vehicles.")
    	public boolean isWheel;
		
		@JSONDescription("")
    	public boolean isTread;
		
		@JSONDescription("If true, then this part will consider water blocks solid and will float on the top of them.  This may be used in conjunction with floating hitboxes.")
    	public boolean canFloat;
		
    	@Deprecated
		public boolean canGoFlat;
		
		@JSONDescription("How wide this part is.  Used for collision detection.")
    	public float width;
		
		@JSONDescription("How tall this part is.  This is used as the min-max parameter.  In cars, this also affects the max ground speed as the rotation of a larger wheel will cause more movement than the rotation of a smaller wheel.")
    	public float height;
		
		@JSONDescription("If set, then this part can go “flat”, and the height of the part will be set to this.  This also reduces the friction it provides.")
    	public float flatHeight;
		
		@JSONDescription("How much friction this part has for forwards movement. Used in cars to determine how much grip wheels have with the ground and if they should spin out, and used with all vehicles when brakes are applied to determine how much force they should be able to apply.")
    	public float motiveFriction;
		
		@JSONDescription("How much friction this part has for sideways movement. Used during turning operations to determine how much the vehicle will skid sideways when turning at speed.")
        public float lateralFriction;
		
		@JSONDescription("If set, this part will create an extra collision box offset in the +Z direction by this amount when placed on a vehicle.  This collision box will have all the same properties as this part (wheel, floating, friction, etc.).  Useful for longer parts like pontoons or helicopter landing skids.  Note that if this parameter is set in the vehicle JSON the vehicle value will override this value.")
        public float extraCollisionBoxOffset;
		
		@JSONDescription("")
        public float spacing;
    }
    
    public class JSONPartPropeller{
		@JSONDescription("If this is present and set, the propeller will have a dynamic pitch.  Propellers with dynamic pitch automatically change their pitch to keep their speed at the top end of the max RPM of the engine.  Below that range their pitch will decrease to a minimum of 45, and above that range it will increase to whatever value is specified by the “pitch” parameter.  Dynamic pitch propellers are also able to provide reverse thrust, though at a significantly reduced power level to their forward-thrust capabilities.")
    	public boolean isDynamicPitch;
		
		@JSONDescription("If true, MTS will consider this propeller a rotor and will angle it when the aircraft control keys are pressed.  This will cause the thrust to be vectored in different directions.  Designed for helicopters, which use rotors to control their movement.")
    	public boolean isRotor;
		
		@JSONDescription("Pitch is a critical defining characteristic of this part.  In essence, pitch is how far forward, in inches, the propeller tries to move in one revolution.  This, coupled with the RPM and gear ratio of the engine, determines the max possible speed of the aircraft this propeller is attached to.  Note, however, that propellers with higher pitches are less efficient at slower speeds so they require a longer runway.  This should be considered carefully before designing a propeller with a high pitch...")
    	public short pitch;
		
		@JSONDescription("The diameter of this propeller, in inches.  Higher-diameter propellers provide more thrust at the same RPM as lower-diameter units.  However, the higher the diameter the more force they take to turn and the more powerful an engine will need to be to drive them (some low-power engines may not even be able to start with large propellers on them).  Additionally, the higher the diameter the lower the maximum RPM the propeller can turn.  Should the propeller exceed this speed it will break off and the engine it was attached to will suddenly not have a load and will rev up rapidly.")
    	public int diameter;
		
		@JSONDescription("How much health this propeller has when first created.  This value is decreased every time the propeller is damaged by something (arrows, swords, Anti-Aircraft Flak, etc.).  It is also lowered every time the propeller is hitting the ground while turning.  Once this value reaches 0, the propeller will break off.")
    	public int startingHealth;
    }
    
    public class JSONPartSeat{
		@JSONDescription("If true, the player will stand in this seat rather than sit.  Note that some mods may mess this up and force the player to sit, so be advised of this.")
    	public boolean standing;
		
		@JSONDescription("If included, the player's width will be scaled to this value when sitting in this seat.  Useful for times when you can't fit a regular seat.  You can also use this to make the player invisible with a small enough size.  Keep in mind, however, that this parameter changes the eye position of the player, so the lower you set this the lower they sit!")
    	public float widthScale;
		
		@JSONDescription("If included, the player's height will be scaled to this value when sitting in this seat.  Similar to widthScale, but this parameter will also affect the player's eye height.  Keep this in mind, as the lower you set this the lower they sit!")
    	public float heightScale;
    }
    
    public class JSONPartGun{
		@JSONDescription("If set, this causes the gun to automatically reload from the vehicle's inventory when its ammo count hits 0.  Guns will prefer to reload the same ammo that was previously in the gun, and will only reload different (yet compatible) ammo if the old ammo is not found.")
    	public boolean autoReload;
		
		@JSONDescription("If set, the gun will only be able to be fired once per button press.")
    	public boolean isSemiAuto;
		
		@JSONDescription("Normally, guns will physically move themselves when the player looks to the right or left.  If you want this to be a virtual movement, say for a casement-mounted Stug gun, then set this to true.  This will make the yaw applied only internally and won't modify the gun's actual position.")
    	public boolean yawIsInternal;
		
		@JSONDescription("The same as above, just for pitch.  You'll likely want this on all turret-mounted guns to prevent the entire turret from moving.")
    	public boolean pitchIsInternal;
		
		@JSONDescription("The capacity of the gun, in number of bullets.")
    	public int capacity;
		
		@JSONDescription("The delay, in ticks, between the firing of bullets.  Setting this value to 1 or less will make a bullet fire every tick.  This means that the max firing rate for guns is 1200 rounds a minute.  While this is less than some guns in real-life, this is also Minecraft and spawning things between ticks and at rapid rates leads to lag.  And that's more deadly than a gun.")
    	public int fireDelay;
		
		@JSONDescription("How long, in ticks, this gun takes to reload.  This is applied for hand-held reloading as well as automatic reloading.  This value should be similar to the duration of your gun _reloading sound to ensure players don't get confused about why they can't fire their guns.")
    	public int reloadTime;
		
		@JSONDescription("How long, in ticks, this gun takes to start firing after pulling the trigger.  This is designed for chain-gun type guns that need a short period of wind-up before they can start firing.  When the trigger is released, the gun will wind-down for the same amount of time it took to wind up.  If the gun doesn’t wind all the way down before pulling the trigger again, it will start to wind back up from that point rather than 0.")
    	public int windupTime;
		
		@JSONDescription("How fast, in m/s, the bullet will exit the barrel of this gun.  May be 0 in cases where bombers are concerned, as the exit velocity of the barrel is this value PLUS the velocity of the vehicle that's firing the bullet.")
    	public int muzzleVelocity;
		
		@JSONDescription("An optional list of positions. Bullets will be fired the defined positions (or the origin if no positions are defined) plus one barrel-length in the +Z axis in the direction the gun is rotated. There are 2 possible cases when using muzzlePositions:\nIf there are the same number of muzzlePositions as the capacity of the gun, the gun will cycle through each of the muzzle positions in order. The order will be the same every time, and reloading will reset the order proportionate to how many bullets were reloaded. This is useful for rocket pods, missile launchers, and bombs.\nIf the number of muzzlePositions doesn’t match the capacity, the gun will cycle through the positions, resetting to the first muzzle once the last one has been used. This is useful for guns with multiple barrels, like anti-air/flak guns and some airplane turrets. If only one muzzle position is defined, it will use the same position every time.")
    	public List<Point3d> muzzlePositions;
		
		@JSONDescription("The minimum pitch this gun can angle downwards when controlled.")
    	public float minPitch;
		
		@JSONDescription("The maximum pitch this gun can angle upwards when controlled.")
    	public float maxPitch;
		
		@JSONDescription("The minimum yaw this gun can turn counter-clockwise when controlled.")
    	public float minYaw;
		
		@JSONDescription("The maximum yaw this gun can turn clockwise when controlled.")
    	public float maxYaw;
		
		@JSONDescription("The diameter of this gun.  This defines what ammo diameter may be used with it, and is what corresponds to the min-max parameters in the vehicle JSON.  It is also used to calculate rotation speed.  Units are in mm.")
    	public float diameter;
		
		@JSONDescription("The length of the barrel of this gun.  Longer barrels will result in slower-turning guns, but greater accuracy at long ranges.  Units are in meters.")
    	public float length;
		
		@JSONDescription("If true, this makes it so that only one of this type of gun can be selected and fired at a time. This is useful for missiles and bombs that have different types of ammunition, as you can load different guns with different types of ammunition, and switch between the individual guns. If not used or set to false, cycling through weapons will select all weapons of the same type.")
    	public boolean fireSolo;
		
		@JSONDescription("If true, this gun will return to its default yaw and pitch if it is not active. This is useful for anyone who likes to keep their large assortment of weapons nice and tidy.")
    	public boolean resetPosition;
		
		@JSONDescription("Used when resetPosition is true. Defaults to 0 if not set.")
    	public float defaultPitch;
		
		@JSONDescription("Used when resetPosition is true. Defaults to 0 if not set.")
    	public float defaultYaw;
		
		@JSONDescription("A list of particleObjects.  If present in a gun JSON, these particles will be spawned as a bullet is fired. This allows things like gun smoke.")
        public List<JSONParticleObject> particleObjects;
        
		@JSONDescription("If set and true, then this gun part will be able to be held and fired from the player's hand.  All animations, and lighting applies here, so keep this in mind. If this is set, then handHeldNormalOffset and handHeldAimingOffset MUST be included!  Note that custom cameras will work when hand-held, but they will not be activated via the standard F5 cycling.  Instead, they will be activated when the player sneaks.  This is intended to allow for scopes and the like.")
        public boolean handHeld;
		
        @JSONRequired(dependentField="handHeld", dependentValues={"true"})
    	@JSONDescription("The offset where this gun will be when held normally by the player.  An offset of 0,0,0 will render the gun in the center of the player's right shoulder rotation point.  For reference, this is 0.3125 blocks to the right, and 1.375 blocks from the bottom-center of the player's feet.")
		public Point3d handHeldNormalOffset;
		
        @JSONRequired(dependentField="handHeld", dependentValues={"true"})
    	@JSONDescription("Like the normal offset, but this applies when the player starts sneaking/aiming.")
		public Point3d handHeldAimedOffset;
    }
    
    public class JSONPartBullet{
    	@Deprecated
    	public String type;
		
    	@JSONRequired
		@JSONDescription("A list of strings describing the bullet.  This defines how it inflicts damage on whatever it hits.")
    	public List<String> types;
		
		@JSONDescription("How many bullets are in the bullet item crafted at the bullet bench. Because nobody wants to have to craft 500 bullets one by one...")
    	public int quantity;
		
		@JSONDescription("The diameter of the bullet.  This determines what guns can fire it, as well as the damage it inflicts.")
    	public float diameter;
		
		@JSONDescription("Only affects explosive bullets.  The damage dealt and size of the blast radius are normally determined by the diameter of the bullet, but you can override that by setting this value. A value of 1 is about equivalent to a single block of TNT. Useful if you want a little more oomph in your explosions, or if you want to tone them down.")
    	public float blastStrength;
		
		@JSONDescription("How much armor this bullet can penetrate.  This allows the bullet to pass through any collision boxes with armorThickness set less than this value.  Note that as the bullet slows down, this value will decrease, so a bullet with 100 penetration may not pass through a collision box with 90 armor if it slows down enough prior to contact.")
    	public float armorPenetration;
		
		@JSONDescription("How long, in ticks, the bullet should keep its initial velocity. This simulates a rocket motor that is present in rockets and missiles. The bullet will not be affected by gravity or slow down until this amount of time has elapsed.")
    	public int burnTime;
		
		@JSONDescription("How long, in ticks, the bullet should take to accelerate from its initial velocity to its maxVelocity (if maxVelocity is used). Note that if this is greater than burnTime, it will not continue to accelerate once burnTime has expired.")
    	public int accelerationTime;
		
		@JSONDescription("The maximum velocity of this bullet, in m/s. If this and accelerationTime are used, the bullet will be spawned with the gun’s muzzleVelocity + the vehicle’s motion, then it will accelerate at a constant rate and reach maxVelocity when the accelerationTime is about to expire.")
    	public int maxVelocity;
		
		//FIXME add description
    	public float maxOffAxis;
		
		@JSONDescription("If used and set to anything greater than 0, the bullet will have guided behavior like a missile, and turnFactor will affect how quickly it can turn. When fired, the bullet will try to find an entity or block that the player is looking at, up to 2000 blocks away. While in the air, it will constantly try to turn and fly toward the target it was given at the time of firing. A good baseline turnFactor is 1.0. Higher values will cause the bullet to turn more quickly, while values between 0 and 1 will turn more slowly.")
    	public float turnFactor;
		
		@JSONDescription("If used, this defines the size of the vertical angle (in degrees), from which a guided bullet will try to approach its target. The bullet will stay level or even climb up to come down on its target on this angle. This works like a Javelin missile in Call of Duty, and it’s useful for making sure that the bullet doesn’t hit the ground before it reaches your target. Note that this only affects bullets where the turnFactor is > 1, and this should be a positive number.")
    	public float angleOfAttack;
		
		@JSONDescription("If this is a guided bullet, it will detonate this many meters away from its target. For a non-guided bullet, it will detonate when it has a block this many meters or less in front of it. This allows things like missiles and artillery rounds that blow up in the air right above the target or the ground, ensuring that the target receives some of the blast rather than it all going into the ground. If used on a non-explosive bullet, it will not detonate, but will despawn at this distance.")
    	public float proximityFuze;
		
		@JSONDescription("Causes the bullet to explode or despawn after this many ticks. This is a “dumber” cousin of the proximityFuze, but may be useful for anti-aircraft rounds that explode in mid-air.")
    	public int airBurstDelay;
		
		@JSONDescription("A list of particleObject. If present in a bullet JSON, these particles will be spawned as the bullet flies, like a rocket leaving a trail of smoke. If the bullet’s type is “smoke”, these particles will just be spawned at the gun, so that the gun will create a stream of smoke.")
    	public List<JSONParticleObject> particleObjects;
    }
    
    public class JSONPartInteractable{
    	@JSONRequired
		@JSONDescription(" What this interactable does when interacted with.  Valid types are:")
    	public String interactionType;
		
		@JSONDescription("If set, this part's inventory can be used by the vehicle and its parts.  This does not affect loader/unloader operations.")
    	public boolean feedsVehicles;
		
		@JSONDescription("If this part is a crate or barrel, this defines the size of its inventory. This is also what is used for min/max value calculations on vehicles.  For crates, this is how many rows (of 9 slots) the inventory has.  For barrels, this is how many buckets the barrel can store x10.  The idea being that 1 unit for crates holds a bit less than 1 unit of barrels, as with barrels you're storing the raw material, and not the container.")
    	public int inventoryUnits;
    }
    
    public class JSONPartEffector{
    	@JSONRequired
		@JSONDescription("The type of the effector.  This defines what this effector does.  Valid types are:")
    	public String type;
		
		@JSONDescription("This determines the width of the part and how wide an area it effects.  This should be an odd number (1, 3, 5, etc.), as it's centered on the part point.  Even numbers may work, but will cause un-defined behavior.  Use with caution.")
    	public int blocksWide;
    }
    
    public class JSONPartGeneric{
		@JSONDescription("The width of the part.")
    	public float width;
		
		@JSONDescription("The height of the part.")
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
