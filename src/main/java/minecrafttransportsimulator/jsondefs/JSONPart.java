package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONConfig.ConfigFuel.FuelDefaults;
import minecrafttransportsimulator.packloading.JSONParser.JSONDefaults;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("Parts go on vehicles.  Simple, no?  There's not much to most part JSONs, and some parts, like seats, will have less than 10 lines to mess with.\nNote: while every part type has its own section, there is one cross-over: the “generic” section.  Being generic, it can be used on all parts to define their generic properties.  This may not apply on some parts, such as wheels, which define properties like height based on other parameters, but it will work on the majority of parts for more fine-tuning of things like interaction box size.")
public class JSONPart extends AJSONPartProvider{
	
	@JSONRequired(dependentField="type", dependentValues={"generic"}, subField="general")
	@JSONDescription("Properties for all parts.")
    public JSONPartGeneric generic;
	
	@JSONRequired(dependentField="type", dependentValues={"engine"}, subField="general")
	@JSONDescription("Properties for engines.")
    public JSONPartEngine engine;
	
	@JSONRequired(dependentField="type", dependentValues={"ground"}, subField="general")
	@JSONDescription("Properties for ground devices.")
    public JSONPartGroundDevice ground;
	
	@JSONRequired(dependentField="type", dependentValues={"propeller"}, subField="general")
	@JSONDescription("Properties for propellers.")
    public JSONPartPropeller propeller;
	
	@JSONRequired(dependentField="type", dependentValues={"seat"}, subField="general")
	@JSONDescription("Properties for seats.")
    public JSONPartSeat seat;
	
	@JSONRequired(dependentField="type", dependentValues={"gun"}, subField="general")
	@JSONDescription("Properties for guns.")
    public JSONPartGun gun;
	
	@JSONRequired(dependentField="type", dependentValues={"bullet"}, subField="general")
	@JSONDescription("Properties for bullets.")
    public JSONPartBullet bullet;
	
	@JSONRequired(dependentField="type", dependentValues={"interactable"}, subField="general")
	@JSONDescription("Properties for interactables.")
    public JSONPartInteractable interactable;
	
	@JSONRequired(dependentField="type", dependentValues={"effector"}, subField="general")
	@JSONDescription("Properties for effectors.")
    public JSONPartEffector effector;

	@Deprecated
	public List<JSONPartDefinition> subParts;
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
    
    public static enum PartType{
    	@JSONDescription("This isn't so much a part as it is an add-on component to your main vehicle.  Generic parts don't do anything and can't be interacted with, but that's actually a GOOD thing as they won't try to move your vehicle or let players sit in random locations.\nWant a set of spoilers to add to your sports cars without making new models?  Generic parts.\nWant to make a freight vehicle like the GMC with different rear-ends?  Generic parts.\nWant to make different tops for your Jeep?  Generic parts.\nGeneric parts are particularly versatile when combined with sub-parts.  In this capacity they can be used to add truck beds with places to put crates for cargo slots, axles to allow for more wheels and mounts for guns that wouldn't normally be on the vehicle.\nIf you do plan on using the generic type, it is highly suggested that you give them a unique, yet common, type name.  For example, generic_bumpersticker, or generic_pickupbed.  This will not only ensure that other generic parts will go on your vehicle, but also lets other packs have the same generic parts for cross-pack compatibility.")
		GENERIC,
    	@JSONDescription("Engines are the most complex JSON in MTS.  While all engines use the same generic code under the hood, each application has specific tweaks that will need to be made.  For example, an aircraft engine that has propellers on it will need to have a propeller subPart to allow for a propeller to be placed on it, or an additionalPart on the vehicle, should the propeller be a vehicle option rather than an engine option (such as a car that has a boat propeller for water travel).  Because of this, the forces that come out of your engine depend on what it's put in and what's attached to it more than anything else.\nWhile engines have a “type” parameter, this is only used for classifying engines into distinct categories to prevent users from putting jet engines on semi trucks.")
		ENGINE,
		@JSONDescription("It's a device, that touches the ground.  Therefore, it's a ground device part.  Simple, no?  Ground devices come in all shapes and sizes.  From the pinnacle of modern motion, wheels, to simple devices like pontoons, ground devices are the bread and butter of movement in MTS.  Unless you're making a boat, you'll probably need a few of these.")
		GROUND,
		@JSONDescription("This part is used on aircraft and is designed to fit on aircraft engines.  It is unique in that it must either be a sub-part of an engine part, or an additional part of an engine.")
		PROPELLER,
		@JSONDescription("Seats allow users to ride vehicles.  While most seats won't have anything special about them, there are a few parameters that you set to change how they function.")
		SEAT,
		@JSONDescription("What's a vehicle without guns?  That's right: it's a target!  Guns may either be placed on vehicles or held in the player's hand to allow them to shoot things.\nNote that if a gun is placed as a subPart of a seat or an additionalPart on a vehicle where a seat is required to place it, then the gun will be controlled by the player in the seat.  The player may then rotate the gun to the direction they are facing.  The limits of how far the gun rotates and the speed at which it rotates is dictated by both the gun's parameters, and the seat's parameters.  Should a gun be placed on a vehicle normally it will be controlled by players in seats marked as controllers.  These guns can also rotate, though, so be mindful of this when designing tanks and other such vehicles.")
		GUN,
		@JSONDescription("Unlike guns, bullets are pretty simple.  They only have a few parameters that define their basic properties, as the good chunk of the complex work of actually firing the bullet is done by the gun.  As to damage dealt, that is dependent on the speed and size of the bullet, and cannot be directly configured in the JSON (though some bullet types will do more damage than others).")
		BULLET,
		@JSONDescription("Interactable is a generic bucket for parts that can be interacted with.  These parts don't do anything to the vehicle.  Rather, they are mainly for player interaction.  They may display a GUI for the player to use, store items, or run tasks set by the player.  Exactly what they do depends on the interactionType parameter.")
		INTERACTABLE,
		@JSONDescription("Effectors are parts that effect the world they are in.  Different effectors do different things, but the one thing they have in common is they do something with blocks in the world.  Unlike interactable parts, they do not have any inventories or GUIs.  To have such functionality, they should be combined with an interactable part.  For example, a planter combined with a crate to hold seeds for the planter.")
		EFFECTOR;
	}
    
    public class JSONPartGeneric{
    	@JSONRequired
    	@JSONDefaults(PartType.class)
    	@JSONDescription("The type-name for this part.  This MUST start with the name of the part section you are wanting to make.  For example engine_somename, gun_othername.  Other than that, there are no restrictions.  There is, however, a generally-agreed on naming format for most parts made in community packs, so check with the community if you want inter-pack compatibility.  All possible prefixes are in the default list, so while you may use any type you want, it should start with one of these.")
    	public String type;
    	
    	@JSONDescription("NOTE: Using a unique 'type' name is preferred over customType parameters.  See the various part conventions ebfore using this!\n\nThis parameter is optional and should only be used for parts that you need in specific places.  This will restrict this part to only work in part definitions with customTypes defined, and only if they contain this customType.")
    	public String customType;
    	
    	@JSONDescription("This parameter is optional.  If included and set to true, this part, and all sub-parts, will not be mirrored, no matter the settings in the vehicle JSON. Useful on things like lights and signage, where mirroring would make the lights or signs render backwards, or on turrets where you don't want seats to mirror themselves.")
    	public boolean disableMirroring;
    	
    	@JSONDescription("This parameter is optional.  If included and set to true, this part will use the texture of the vehicle rather than the texture that corresponds to the part.  Useful for parts that need to pull vehicle textures for their rendering, such as tank turrets and vehicle bolt-on components.")
    	public boolean useVehicleTexture;
    	
		@JSONDescription("The width of the part.")
    	public float width;
		
		@JSONDescription("The height of the part.")
    	public float height;
    }
    
    public class JSONPartEngine{
		@JSONDescription("Should the engine change gears on its own.  This only affects cars and will prevent users from shifting into higher or lower gears using shiftUp and shiftDown. Instead, the engine will attempt to choose the best gear for the situation.  Note that MTS's automatic transmission system isn't the best and may get confused when gear ratios are close together.  For this reason, it is recommended to either use manual transmissions on vehicles with more than 5-6 gears, or to define the RPM at which a gear is shifted up or down via upShiftRPM and downShiftRPM.")
    	public boolean isAutomatic;
		
    	public boolean isSteamPowered;
		
		@JSONDescription("Should the engine spawn flames when starting?  If true, flames will be spawned at the exhaustPos on the vehicle this engine is in.  Note that if the vehicle doesn't have any exhaustPos, no flames will be spawned, even if this is set.")
    	public boolean flamesOnStartup;
		
		@JSONDescription("This is how much 'oomph' the starter outputs on a single firing.  When the starter key is held the engine RPM will be increased by this amount every 4 ticks, or every 0.2 seconds.  Note that for engines with high loads, such as those with larger propellers, its quite possible to make a starter power that literally can't start the engine.")
    	public int starterPower;
		
		@JSONDescription("For automatic gear boxes only.  This parameter is how long, in ticks, to wait to shift gears after shifting gears once.  This is needed as there's a slight delay between shifting and the engine RPM catching up, so without this parameter engines would just bounce between first and last gear.  Adjust this parameter to fit the power output and ratio spacing of your engine.  Engines with lots of gears or high power will likely need a smaller number than a gutless 3-speed.")
    	public int shiftSpeed;
		
		@JSONDescription("For manual gear boxes only.  This parameter defines how long the clutch variable is set to 1 for up-shifting and down-shifting animations.  Has no other affect than this, as vehicles don't simulate clutches.  That's a bit TOO realistic!")
    	public int clutchTime;
		
		@JSONDescription("How long it takes for the engine RPM to 'catch up', or how fast it revs. The lower the value is, the faster the engine will react to RPM changes. If ignored, MTS will set the value of this to a default of 10.")
    	public int revResistance;
		
		@JSONDescription("The max RPM for this engine.  This is how fast the engine will try to go with no load at 100% throttle.  The red-line value (max safe RPM) is lower than and auto-caluclated from this value.")
    	public int maxRPM;
		
		@JSONDescription("The max safe (redline) RPM for this engine.  If left out, MTS will auto-calculate this value for you.  Normally this is fine, but SOME folks may not like the math, so manually-specifiying it is an option.")
    	public int maxSafeRPM;
		
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
		@JSONDefaults(FuelDefaults.class)
    	@JSONDescription("What type of fuel this engine uses.  This is NOT the name of the fluid this engine uses, rather it's a generic type that basically lumps it in with other engines. Gasoline and diesel are two of the most common.  This type system allows for packs to group their engines by what fuels they take to make them more uniform, and allows server owners to configure their fluids to work with specific types of engines.")
		public String fuelType;
		
		@JSONDescription("Same as fuelConsumption, but for the supercharger on this engine (if any). Note that vehicles will only take the base fuel consumption of the engine into account when checking min/max values. This is to allow for higher-performance engines to be added to vehicles without poking pack creators to increase their maximum values. This variable can be omitted if your engine doesn't have a supercharger.")
		public float superchargerFuelConsumption;
		
		@JSONDescription("The efficiency of the supercharger on this engine (if any). The supercharger fuel consumption of this engine (but not the base fuel consumption) will be multiplied by this value. A value of 1 will make the supercharger add the same amount of power for its fuel consumption as adding that number to the base fuel consumption, so make sure to set it to greater than that if you want your supercharger to have any power benefits!\nThis also affects the engine wear calculations, with a value of 1 or below leaving them the same as what it would be without a supercharger. By setting this value to significantly below 1 you can simulate inefficient, gas-guzzling engines if you have a high supercharger fuel consumption, as it won't add much power but will make the engine use a lot more fuel.\nAs a final note: supercharged engines heat up faster than non-supercharged engines. A supercharger efficiency of 0 would make the calculations the same as a non-supercharged engine in this case; setting it to 1 will not make the engine heat up twice as fast. This is intended behavior, as real supercharged engines heat up faster than naturally aspirated ones even if the supercharger itself isn't very efficient.  This variable can be omitted if your engine doesn't have a supercharger.")
		public float superchargerEfficiency;

		@Deprecated
    	public boolean isCrankingNotPitched;
		
		@Deprecated
		public EngineSound customSoundset[];
		
		@Deprecated
		public class EngineSound{
			@Deprecated
			public String soundName;
			
			@Deprecated
			public float pitchIdle;
			
			@Deprecated
			public float pitchMax;
			
			@Deprecated
			public float pitchLength;
			
			@Deprecated
			public float volumeIdle;
			
			@Deprecated
			public float volumeMax;
			
			@Deprecated
			public float volumeLength;
			
			@Deprecated
			public int pitchCenter;
			
			@Deprecated
			public int volumeCenter;
			
			@Deprecated
			public boolean pitchAdvanced;
			
			@Deprecated
			public boolean volumeAdvanced;
		}
    }
    
    public class JSONPartGroundDevice{
		@JSONDescription("If true, then this part will be considered a wheel.  Wheels can transmit power from engines to the ground, making them the go-to choice for ground-based vehicles.")
    	public boolean isWheel;
		
		@JSONDescription("If true, then this part is considered a tread.  Treads can provide power, and will trigger tread-rendering logic.\nFor the actual tread, you want to simply model a single link.  This link will be repeated by MTS and rendered either around the rollers in the vehicle model.  The spacing parameter defined in the part JSON tells MTS how long each link is so it can space them at appropriate intervals.")
    	public boolean isTread;
		
		@JSONDescription("If true, then this part will consider water blocks solid and will float on the top of them.  This may be used in conjunction with floating hitboxes.")
    	public boolean canFloat;
		
		@JSONDescription("How wide this part is.  Used for collision detection.")
    	public float width;
		
		@JSONDescription("How tall this part is.  This is used as the min-max parameter.  In cars, this also affects the max ground speed as the rotation of a larger wheel will cause more movement than the rotation of a smaller wheel.")
    	public float height;
		
		@JSONDescription("If set, then this part can go 'flat', and the height of the part will be set to this.  This also reduces the friction it provides.")
    	public float flatHeight;
		
		@JSONDescription("How much friction this part has for forwards movement. Used in cars to determine how much grip wheels have with the ground and if they should spin out, and used with all vehicles when brakes are applied to determine how much force they should be able to apply.")
    	public float motiveFriction;
		
		@JSONDescription("How much friction this part has for sideways movement. Used during turning operations to determine how much the vehicle will skid sideways when turning at speed.")
        public float lateralFriction;
		
		@JSONDescription("If set, this part will create an extra collision box offset in the +Z direction by this amount when placed on a vehicle.  This collision box will have all the same properties as this part (wheel, floating, friction, etc.).  Useful for longer parts like pontoons or helicopter landing skids.  Note that if this parameter is set in the vehicle JSON the vehicle value will override this value.  Since treads are normally vehicle-dependent in their size, it is recommended to NOT set this here for treads and use the vehicle parameter.")
        public float extraCollisionBoxOffset;
		
		@JSONDescription("The spacing between repeated tread links to be animated in the model.  Used only if isTread is true.")
        public float spacing;
		
    	@Deprecated
		public boolean canGoFlat;
    }
    
    public class JSONPartPropeller{
		@JSONDescription("If this is present and set, the propeller will have a dynamic pitch.  Propellers with dynamic pitch automatically change their pitch to keep their speed at the top end of the max RPM of the engine.  Below that range their pitch will decrease to a minimum of 45, and above that range it will increase to whatever value is specified by the 'pitch' parameter.  Dynamic pitch propellers are also able to provide reverse thrust, though at a significantly reduced power level to their forward-thrust capabilities.")
    	public boolean isDynamicPitch;
		
		@JSONDescription("If true, MTS will consider this propeller a rotor and will angle it when the aircraft control keys are pressed.  This will cause the thrust to be vectored in different directions.  Designed for helicopters, which use rotors to control their movement.")
    	public boolean isRotor;
		
		@JSONDescription("Pitch is a critical defining characteristic of this part.  In essence, pitch is how far forward, in inches, the propeller tries to move in one revolution.  This, coupled with the RPM and gear ratio of the engine, determines the max possible speed of the aircraft this propeller is attached to.  Note, however, that propellers with higher pitches are less efficient at slower speeds so they require a longer runway.  This should be considered carefully before designing a propeller with a high pitch...")
    	public int pitch;
		
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
		
		@JSONDescription("How long, in ticks, this gun takes to start firing after pulling the trigger.  This is designed for chain-gun type guns that need a short period of wind-up before they can start firing.  When the trigger is released, the gun will wind-down for the same amount of time it took to wind up.  If the gun doesn't wind all the way down before pulling the trigger again, it will start to wind back up from that point rather than 0.")
    	public int windupTime;
		
		@JSONDescription("How fast, in m/s, the bullet will exit the barrel of this gun.  May be 0 in cases where bombers are concerned, as the exit velocity of the barrel is this value PLUS the velocity of the vehicle that's firing the bullet.")
    	public int muzzleVelocity;
		
		@JSONDescription("How much velocity, each tick, should be added in the -Y direction.  Used to make bullets travel in arcs.")
    	public float gravitationalVelocity;
		
		@JSONDescription("How much spread the bullet will have when fired.  0 is no spread, higher values have higher spread.")
    	public float bulletSpreadFactor;
		
		@JSONDescription("An optional list of positions. Bullets will be fired the defined positions (or the origin if no positions are defined) plus one barrel-length in the +Z axis in the direction the gun is rotated. There are 2 possible cases when using muzzlePositions:\nIf there are the same number of muzzlePositions as the capacity of the gun, the gun will cycle through each of the muzzle positions in order. The order will be the same every time, and reloading will reset the order proportionate to how many bullets were reloaded. This is useful for rocket pods, missile launchers, and bombs.\nIf the number of muzzlePositions doesn't match the capacity, the gun will cycle through the positions, resetting to the first muzzle once the last one has been used. This is useful for guns with multiple barrels, like anti-air/flak guns and some airplane turrets. If only one muzzle position is defined, it will use the same position every time.")
    	public List<Point3d> muzzlePositions;
		
		@JSONDescription("The minimum pitch this gun can angle downwards when controlled.  Note that if this is 0, and it is specified in the part JSON section on the vehicle, the vehicle's parameter will override this parameter.")
    	public float minPitch;
		
		@JSONDescription("The maximum pitch this gun can angle upwards when controlled.  Note that if this is 0, and it is specified in the part JSON section on the vehicle, the vehicle's parameter will override this parameter.")
    	public float maxPitch;
		
		@JSONDescription("The minimum yaw this gun can turn counter-clockwise when controlled.  Note that if this is 0, and it is specified in the part JSON section on the vehicle, the vehicle's parameter will override this parameter.")
    	public float minYaw;
		
		@JSONDescription("The maximum yaw this gun can turn clockwise when controlled.  Note that if this is 0, and it is specified in the part JSON section on the vehicle, the vehicle's parameter will override this parameter.")
    	public float maxYaw;
		
		@JSONDescription("The diameter of this gun.  This defines what ammo diameter may be used with it, and is what corresponds to the min-max parameters in the vehicle JSON.  It is also used to calculate rotation speed.  Units are in mm.")
    	public float diameter;
		
		@JSONDescription("The length of the barrel of this gun.  Longer barrels will result in slower-turning guns (unless their travel speed is specified), but greater accuracy at long ranges.  Units are in meters.")
    	public float length;
		
		@JSONDescription("The minimum case length of bullets that can go into this gun.  Useful for preventing extra-long bullets from going into a gun that normally fires short ones.  Units are in mm.")
    	public float minCaseLength;
		
		@JSONDescription("Like minCaseLength, but the maximum.")
    	public float maxCaseLength;
		
		@JSONDescription("How fast, in degrees/tick, the gun can rotate.  This is normally auto-calculated from the gun's length, but it may be specified here if desired.")
    	public float travelSpeed;
		
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
		
		@JSONDescription("The diameter of the bullet.  This determines what guns can fire it, as well as the damage it inflicts.  Units are in mm.")
    	public float diameter;
		
		@JSONDescription("The case length of the bullet.  This determines what guns can fire it, but does not affect damage.  Units are in mm.")
    	public float caseLength;
		
		@JSONDescription("Only affects explosive bullets.  The damage dealt and size of the blast radius are normally determined by the diameter of the bullet, but you can override that by setting this value. A value of 1 is about equivalent to a single block of TNT. Useful if you want a little more oomph in your explosions, or if you want to tone them down.")
    	public float blastStrength;
		
		@JSONDescription("How much armor this bullet can penetrate, in mm.  This allows the bullet to pass through any collision boxes with armorThickness set less than this value.  Note that as the bullet slows down, this value will decrease, so a bullet with 100 penetration may not pass through a collision box with 90 armor if it slows down enough prior to contact.")
    	public float armorPenetration;
		
		@JSONDescription("How much velocity, each tick, should be deducted from the bullet's velocity.")
    	public float slowdownSpeed;
		
		@JSONDescription("How long, in ticks, the bullet should keep its initial velocity. This simulates a rocket motor that is present in rockets and missiles. The bullet will not be affected by gravity or slow down until this amount of time has elapsed.")
    	public int burnTime;
		
		@JSONDescription("How long, in ticks, the bullet should take to accelerate from its initial velocity to its maxVelocity (if maxVelocity is used). Note that if this is greater than burnTime, it will not continue to accelerate once burnTime has expired.")
    	public int accelerationTime;
		
		@JSONDescription("The maximum velocity of this bullet, in m/s. If this and accelerationTime are used, the bullet will be spawned with the gun's muzzleVelocity + the vehicle's motion, then it will accelerate at a constant rate and reach maxVelocity when the accelerationTime is about to expire.")
    	public int maxVelocity;
		
		@JSONDescription("If used and set to anything greater than 0, the bullet will have guided behavior like a missile, and turnFactor will affect how quickly it can turn. When fired, the bullet will try to find an entity or block that the player is looking at, up to 2000 blocks away. While in the air, it will constantly try to turn and fly toward the target it was given at the time of firing. A good baseline turnFactor is 1.0. Higher values will cause the bullet to turn more quickly, while values between 0 and 1 will turn more slowly.")
    	public float turnFactor;
		
		@JSONDescription("If used, this defines the size of the vertical angle (in degrees), from which a guided bullet will try to approach its target. The bullet will stay level or even climb up to come down on its target on this angle. This works like a Javelin missile in Call of Duty, and it's useful for making sure that the bullet doesn't hit the ground before it reaches your target. Note that this only affects bullets where the turnFactor is > 1, and this should be a positive number.")
    	public float angleOfAttack;
		
		@JSONDescription("If this is a guided bullet, it will detonate this many meters away from its target. For a non-guided bullet, it will detonate when it has a block this many meters or less in front of it. This allows things like missiles and artillery rounds that blow up in the air right above the target or the ground, ensuring that the target receives some of the blast rather than it all going into the ground. If used on a non-explosive bullet, it will not detonate, but will despawn at this distance.")
    	public float proximityFuze;
		
		@JSONDescription("Causes the bullet to explode or despawn after this many ticks. This is a 'dumber' cousin of the proximityFuze, but may be useful for anti-aircraft rounds that explode in mid-air.")
    	public int airBurstDelay;
		
		@JSONDescription("A list of particleObject. If present in a bullet JSON, these particles will be spawned as the bullet flies, like a rocket leaving a trail of smoke. If the bullet's type is 'smoke', these particles will just be spawned at the gun, so that the gun will create a stream of smoke.")
    	public List<JSONParticleObject> particleObjects;
    }
    
    public class JSONPartInteractable{
    	@JSONRequired
		@JSONDescription("What this interactable does when interacted with.")
    	public InteractableComponentType interactionType;
		
		@JSONDescription("If set, this part's inventory can be used by the vehicle and its parts.  This does not affect loader/unloader operations.")
    	public boolean feedsVehicles;
		
		@JSONDescription("If this part is a crate or barrel, this defines the size of its inventory. This is also what is used for min/max value calculations on vehicles.  For crates, this is how many rows (of 9 slots) the inventory has.  For barrels, this is how many buckets the barrel can store x10.  The idea being that 1 unit for crates holds a bit less than 1 unit of barrels, as with barrels you're storing the raw material, and not the container.")
    	public int inventoryUnits;
		
		@JSONDescription("A optional crafting definition for this interactable.  Requires an interactable type of crafting_bench to do anything.")
		public JSONCraftingBench crafting;
    }
    
    public static enum InteractableComponentType{
		@JSONDescription("Stores items.  If feedsVehicles is set, then this crate will allow other parts such as guns, engines, and effectors to pull inventory out for their operations.  If the vehicle containing this crate explodes, and the crate contains ammo, the explosion size will be increased based on the amount and size of the ammo.  Additionally, if this crate has ammo and is struck by a bullet, it will blow up, taking the vehicle with it.")
		CRATE,
		@JSONDescription("Stores liquid.  If feedsVehicles is set, and this barrel contains fuel, then fuel will be taken out of this barrel to be put into the main fuel tank.  If the vehicle containing this barrel explodes, and the barrel contains fuel, the explosion size will be increased as if the vehicle had the fuel in the barrel in its fuel tank.")
		BARREL,
		@JSONDescription("Works as a standard crafting table when clicked.")
		CRAFTING_TABLE,
		@JSONDescription("Works as a standard furnace when clicked.  Required standard fuel for furnace operations; does not use vehicle fuel for smelting operations.")
		FURNACE,
		@JSONDescription("Works as a standard brewing stand when clicked.")
		BREWING_STAND,
		@JSONDescription("Works as a jerrycan, allowing for fuel to be stored inside and then used to fuel vehicles without a fuel pump.")
		JERRYCAN,
		@JSONDescription("Works as a MTS crafting bench when clicked.  This requires supplemental parameters.")
		CRAFTING_BENCH;
	}
    
    public class JSONPartEffector{
    	@JSONRequired
		@JSONDescription("The type of the effector.  This defines what this effector does.")
    	public EffectorComponentType type;
		
		@JSONDescription("This determines the width of the part and how wide an area it effects.  This should be an odd number (1, 3, 5, etc.), as it's centered on the part point.  Even numbers may work, but will cause un-defined behavior.  Use with caution.")
    	public int blocksWide;
    }
    
    public static enum EffectorComponentType{
		@JSONDescription("Checks for plants on the ground and applies bonemeal to them contained in crates.")
		FERTILIZER,
		@JSONDescription("Will harvest plants that pass through it, depositing items into vehicle crates, or on the ground if no crates are present, or their inventories are full.  Will also break “bush” blocks like tall grass, flowers, and saplings, depositing them on the ground.")
		HARVESTER,
		@JSONDescription("Will plant any plant-able items located in vehicle crate parts onto farmland. Does not work with cactus, reeds, or other non-farmland-based plants.")
		PLANTER,
		@JSONDescription("Turns dirt and grass into farmland, and coarse dirt into dirt.")
		PLOW;
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
