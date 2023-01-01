package minecrafttransportsimulator.jsondefs;

import java.util.LinkedHashMap;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.BlockMaterial;
import minecrafttransportsimulator.entities.components.AEntityD_Definable.ModifiableValue;
import minecrafttransportsimulator.jsondefs.JSONConfigSettings.ConfigFuel.FuelDefaults;
import minecrafttransportsimulator.packloading.JSONParser.JSONDefaults;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("Parts go on vehicles.  Simple, no?  There's not much to most part JSONs, and some parts, like seats, will have less than 10 lines to mess with.\nNote: while every part type has its own section, there is one cross-over: the generic section.  Being generic, it can be used on all parts to define their generic properties.  This may not apply on some parts, such as wheels, which define properties like height based on other parameters, but it will work on the majority of parts for more fine-tuning of things like interaction box size.")
public class JSONPart extends AJSONPartProvider {

    @JSONRequired
    @JSONDescription("Properties for all parts.")
    public JSONPartGeneric generic;

    @JSONRequired(dependentField = "type", dependentValues = {"engine"}, subField = "generic")
    @JSONDescription("Properties for engines.")
    public JSONPartEngine engine;

    @JSONRequired(dependentField = "type", dependentValues = {"ground"}, subField = "generic")
    @JSONDescription("Properties for ground devices.")
    public JSONPartGroundDevice ground;

    @JSONRequired(dependentField = "type", dependentValues = {"propeller"}, subField = "generic")
    @JSONDescription("Properties for propellers.")
    public JSONPartPropeller propeller;

    @JSONRequired(dependentField = "type", dependentValues = {"seat"}, subField = "generic")
    @JSONDescription("Properties for seats.")
    public JSONPartSeat seat;

    @JSONRequired(dependentField = "type", dependentValues = {"gun"}, subField = "generic")
    @JSONDescription("Properties for guns.")
    public JSONPartGun gun;

    @JSONRequired(dependentField = "type", dependentValues = {"interactable"}, subField = "generic")
    @JSONDescription("Properties for interactables.")
    public JSONPartInteractable interactable;

    @JSONRequired(dependentField = "type", dependentValues = {"effector"}, subField = "generic")
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

    public enum PartType {
        @JSONDescription("This isn't so much a part as it is an add-on component to your main vehicle.  Generic parts don't do anything and can't be interacted with, but that's actually a GOOD thing as they won't try to move your vehicle or let players sit in random locations.\nWant a set of spoilers to add to your sports cars without making new models?  Generic parts.\nWant to make a freight vehicle like the GMC with different rear-ends?  Generic parts.\nWant to make different tops for your Jeep?  Generic parts.\nGeneric parts are particularly versatile when combined with sub-parts.  In this capacity they can be used to add truck beds with places to put crates for cargo slots, axles to allow for more wheels and mounts for guns that wouldn't normally be on the vehicle.\nIf you do plan on using the generic type, it is highly suggested that you give them a unique, yet common, type name.  For example, generic_bumpersticker, or generic_pickupbed.  This will not only ensure that other generic parts will go on your vehicle, but also lets other packs have the same generic parts for cross-pack compatibility.")
        GENERIC,
        @JSONDescription("Engines are the most complex JSON in MTS.  While all engines use the same generic code under the hood, each application has specific tweaks that will need to be made.  For example, an aircraft engine that has propellers on it will need to have a propeller subPart to allow for a propeller to be placed on it, or an additionalPart on the vehicle, should the propeller be a vehicle option rather than an engine option (such as a car that has a boat propeller for water travel).  Because of this, the forces that come out of your engine depend on what it's put in and what's attached to it more than anything else.\nWhile engines have a type parameter, this is only used for classifying engines into distinct categories to prevent users from putting jet engines on semi trucks.")
        ENGINE,
        @JSONDescription("It's a device, that touches the ground.  Therefore, it's a ground device part.  Simple, no?  Ground devices come in all shapes and sizes.  From the pinnacle of modern motion, wheels, to simple devices like pontoons, ground devices are the bread and butter of movement in MTS.  Unless you're making a boat, you'll probably need a few of these.")
        GROUND,
        @JSONDescription("This part is used on aircraft and is designed to fit on aircraft engines.  It is unique in that it must either be a sub-part of an engine part, or an additional part of an engine.")
        PROPELLER,
        @JSONDescription("Seats allow users to ride vehicles.  While most seats won't have anything special about them, there are a few parameters that you set to change how they function.")
        SEAT,
        @JSONDescription("What's a vehicle without guns?  That's right: it's a target!  Guns may either be placed on vehicles or held in the player's hand to allow them to shoot things.\nNote that if a gun is placed as a subPart of a seat or an additionalPart on a vehicle where a seat is required to place it, then the gun will be controlled by the player in the seat.  The player may then rotate the gun to the direction they are facing.  The limits of how far the gun rotates and the speed at which it rotates is dictated by both the gun's parameters, and the seat's parameters.  Should a gun be placed on a vehicle normally it will be controlled by players in seats marked as controllers.  These guns can also rotate, though, so be mindful of this when designing tanks and other such vehicles.")
        GUN,
        @JSONDescription("Interactable is a generic bucket for parts that can be interacted with.  These parts don't do anything to the vehicle.  Rather, they are mainly for player interaction.  They may display a GUI for the player to use, store items, or run tasks set by the player.  Exactly what they do depends on the interactionType parameter.")
        INTERACTABLE,
        @JSONDescription("Effectors are parts that effect the world they are in.  Different effectors do different things, but the one thing they have in common is they do something with blocks in the world.  Unlike interactable parts, they do not have any inventories or GUIs.  To have such functionality, they should be combined with an interactable part.  For example, a planter combined with a crate to hold seeds for the planter.")
        EFFECTOR
    }

    public static class JSONPartGeneric {
        @JSONRequired
        @JSONDefaults(PartType.class)
        @JSONDescription("The type-name for this part.  This MUST start with the name of the part section you are wanting to make.  For example engine_somename, gun_othername.  Other than that, there are no restrictions.  There is, however, a generally-agreed on naming format for most parts made in community packs, so check with the community if you want inter-pack compatibility.  All possible prefixes are in the default list, so while you may use any type you want, it should start with one of these.")
        public String type;

        @JSONDescription("NOTE: Using a unique 'type' name is preferred over customType parameters.  See the various part conventions ebfore using this!\n\nThis parameter is optional and should only be used for parts that you need in specific places.  This will restrict this part to only work in part definitions with customTypes defined, and only if they contain this customType.")
        public String customType;

        @JSONDescription("This parameter is optional.  If included and set to true, this part will use the texture of the vehicle rather than the texture that corresponds to the part.  Useful for parts that need to pull vehicle textures for their rendering, such as tank turrets and vehicle bolt-on components.")
        public boolean useVehicleTexture;

        @JSONDescription("If true, this part will be able to be removed by hand and without a wrench.  This also bypasses owner requirements (but not vehicle locking).  Useful for small parts like luggage that anyone should be able to remove at any time.")
        public boolean canBeRemovedByHand;

        @JSONDescription("If true, this part will be able to be removed by hand and without a wrench.  This also bypasses owner requirements (but not vehicle locking).  Useful for small parts like luggage that anyone should be able to remove at any time.")
        public boolean mustBeRemovedByScrewdriver;

        @JSONDescription("If true, this part will forward damage onto the vehicle it is on when hit by a bullet.  This will also cause the bullet to stop when it hits this part.  Engines ignore this behavior and always forward damage.")
        public boolean forwardsDamage;

        @JSONDescription("If true, this part can be placed on the ground.  It will be placed axis-aligned when placed.")
        public boolean canBePlacedOnGround;

        @JSONDescription("If true, this part will fall to the ground when placed, if it's not on the ground already when placed.  Only valid for parts with canBePlacedOnGround as true.")
        public boolean fallsToGround;

        @JSONDescription("If true, then when this part runs out of health it will be destroyed and removed rather than just become inoperable.")
        public boolean destroyable;

        @JSONDescription("The width of the part.")
        public float width;

        @JSONDescription("The height of the part.")
        public float height;

        @JSONDescription("The offset, in the Y direction, as to where this part will exist when placed in the world.  Has no effect if canBePlacedOnGround is false.")
        public float placedOffset;

        @JSONDescription("The mass of this part.  Is normally 0 to avoid heavy seats, but may be used for generic parts or engines or the like.")
        public int mass;

        @JSONDescription("This parameter is optional.  If included, then the model will be rendered with this texture in the part bench, even if it has a different one defined or uses the vehicle texture.")
        public String benchTexture;

        @JSONDescription("This is a list of animatedObjects that can be used to move this part based on the animation values. This movement applies before the movement defined in the part slot.  This is primarially used to move the part based on properties of sub-parts, mostly for guns, but may be used for other things.")
        public List<JSONAnimationDefinition> movementAnimations;

        @JSONDescription("A listing of animation objects for determining if this part is active.  Leaving this blank will make for a part that is always active.  Visibility transforms will turn the part on and off.  Inhibitor and activator transforms may be used in conjunction with these for advanced on/off logic.  The exact thing that an 'active' part does depends on the part.  Effectors only effect when they are active.  Guns can only be used when active.  Seats can only be sat in when active.  etc.")
        public List<JSONAnimationDefinition> activeAnimations;
    }

    public static class JSONPartEngine {
        @JSONRequired
        @JSONDescription("The type of engine.  Different engines use different paramters.  But at least one type must be specified.")
        public EngineType type;

        @JSONDescription("Should the engine change gears on its own.  This only affects cars and will prevent users from shifting into higher or lower gears using shiftUp and shiftDown. Instead, the engine will attempt to choose the best gear for the situation.  Note that MTS's automatic transmission system isn't the best and may get confused when gear ratios are close together.  For this reason, it is recommended to either use manual transmissions on vehicles with more than 5-6 gears, or to define the RPM at which a gear is shifted up or down via upShiftRPM and downShiftRPM.")
        public boolean isAutomatic;

        @JSONDescription("If true, the automatic starter will be distabled for this engine.  Instead, it must be started by hand.  Note that while normally this requires hitting the propeller, but in this case the engine itself may be hit too.  This is for outboard motors and the like.")
        public boolean disableAutomaticStarter;

        @JSONDescription("This is how much 'oomph' the starter outputs on a single firing.  When the starter key is held the engine RPM will be increased by this amount every 4 ticks, or every 0.2 seconds.  Note that for engines with high loads, such as those with larger propellers, its quite possible to make a starter power that literally can't start the engine.")
        public int starterPower;

        @JSONDescription("For automatic gear boxes only.  This parameter is how long, in ticks, to wait to shift gears after shifting gears once.  This is needed as there's a slight delay between shifting and the engine RPM catching up, so without this parameter engines would just bounce between first and last gear.  Adjust this parameter to fit the power output and ratio spacing of your engine.  Engines with lots of gears or high power will likely need a smaller number than a gutless 3-speed.")
        public int shiftSpeed;

        @JSONDescription("For manual gear boxes only.  This parameter defines how long the clutch variable is set to 1 for up-shifting and down-shifting animations.  Has no other affect than this, as vehicles don't simulate clutches.  That's a bit TOO realistic!")
        public int clutchTime;

        @JSONDescription("Normally when shifting into a gear in the opposite direction from which the vehicle is travelling the engine will refuse the shift and return 'bad_shift'. if this is true, the shift will be forced.")
        public boolean forceShift;

        @JSONDescription("How long it takes for the engine RPM to 'catch up', or how fast it revs. The lower the value is, the faster the engine will react to RPM changes. If ignored, MTS will set the value of this to a default of 10.")
        public int revResistance;

        @ModifiableValue
        @JSONDescription("The max RPM for this engine.  This is how fast the engine will try to go with no load at 100% throttle.  The red-line value (max safe RPM) is lower than and auto-caluclated from this value.")
        public int maxRPM;

        @ModifiableValue
        @JSONDescription("The max safe (redline) RPM for this engine.  If left out, MTS will auto-calculate this value for you.  Normally this is fine, but SOME folks may not like the math, so manually-specifiying it is an option.")
        public int maxSafeRPM;

        @ModifiableValue
        @JSONDescription("The RPM where this engine will idle, provided it is turned on and isn't drowned or out of fuel.")
        public int idleRPM;

        @JSONDescription("The RPM where this engine will start, after starting the engine will try to maintain the speed set by the idleRPM and maxRPM.  May be lower than the idleRPM, as the engine will stall based on the stallRPM, not this value.")
        public int startRPM;

        @ModifiableValue
        @JSONDescription("The RPM at which the engine's rev limiter kicks in. If left out, MTS will auto-calculate this value for you. If set to -1, revlimiting will be disabled for this engine.")
        public int revlimitRPM;

        @JSONDescription("How hard the rev limiter bounces in neutral.")
        public int revlimitBounce;

        @JSONDescription("The RPM where this engine will stall.  Should be below the idleRPM to prevent the engine automatically shutting off on low throttle.")
        public int stallRPM;

        @JSONDescription("The rate at which this engine's RPM winds down per tick after sputtering out or being turned off. 10 by default, and can be configured to make engines wind down quicker or slower.")
        public int engineWinddownRate;

        @JSONDescription("Normally, engines run off of the fuel from the main vehicle.  However, one can give them rocket fuel, which will be used rather than the vehicle's fuel.  The moment the engine turns on, the fuel will ignite and run as if full throttle until the fuel runs out, at which point they must be crafted again to re-fuel them for their next use.")
        public int rocketFuel;

        @ModifiableValue
        @JSONDescription("The rate at which this engine heats up, which gets lobbed into the math with fuel consumption, velocity... etc")
        public float heatingCoefficient;

        @ModifiableValue
        @JSONDescription("The rate at which this engine cools down, which gets lobbed into the math with fuel consumption, velocity... etc")
        public float coolingCoefficient;

        @JSONDescription("The rate at which this engine accrues hours. If you make it lower, it'll be more reliable. If you make it higher, it'll collect that wearout like the infinity stones.")
        public float engineWearFactor;

        @ModifiableValue
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
        public List<Float> gearRatios;

        @JSONDescription("A list of engine RPM speeds that tells MTS when to shift up a gear.  This may be added in conjunction with downShiftRPM to specify the shift points in automatic transmissions to avoid bad shifting choices by the MTS code.")
        public List<Integer> upShiftRPM;

        @JSONDescription("The same as upShiftRPM, but instead tells MTS when to shift down a gear. Be careful while configuring this and upShiftRPM, as you can create an infinite shifting loop if the RPMs are too close.  A rule of thumb is to use what RPM your engine lands at after shifting up and subtract a few hundred RPM.")
        public List<Integer> downShiftRPM;

        @JSONRequired
        @JSONDefaults(FuelDefaults.class)
        @JSONDescription("What type of fuel this engine uses.  This is NOT the name of the fluid this engine uses, rather it's a generic type that basically lumps it in with other engines. Gasoline and diesel are two of the most common.  This type system allows for packs to group their engines by what fuels they take to make them more uniform, and allows server owners to configure their fluids to work with specific types of engines.")
        public String fuelType;

        @ModifiableValue
        @JSONDescription("Same as fuelConsumption, but for the supercharger on this engine (if any). Note that vehicles will only take the base fuel consumption of the engine into account when checking min/max values. This is to allow for higher-performance engines to be added to vehicles without poking pack creators to increase their maximum values. This variable can be omitted if your engine doesn't have a supercharger.")
        public float superchargerFuelConsumption;

        @ModifiableValue
        @JSONDescription("The efficiency of the supercharger on this engine (if any). The supercharger fuel consumption of this engine (but not the base fuel consumption) will be multiplied by this value. A value of 1 will make the supercharger add the same amount of power for its fuel consumption as adding that number to the base fuel consumption, so make sure to set it to greater than that if you want your supercharger to have any power benefits!\nThis also affects the engine wear calculations, with a value of 1 or below leaving them the same as what it would be without a supercharger. By setting this value to significantly below 1 you can simulate inefficient, gas-guzzling engines if you have a high supercharger fuel consumption, as it won't add much power but will make the engine use a lot more fuel.\nAs a final note: supercharged engines heat up faster than non-supercharged engines. A supercharger efficiency of 0 would make the calculations the same as a non-supercharged engine in this case; setting it to 1 will not make the engine heat up twice as fast. This is intended behavior, as real supercharged engines heat up faster than naturally aspirated ones even if the supercharger itself isn't very efficient.  This variable can be omitted if your engine doesn't have a supercharger.")
        public float superchargerEfficiency;

        @Deprecated
        public boolean isCrankingNotPitched;

        @Deprecated
        public EngineSound[] customSoundset;

        @Deprecated
        public class EngineSound {
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

    public enum EngineType {
        @JSONDescription("A standard internal-combustion engine.  Requires fuel from the vehicle's fuel tanks to run.")
        NORMAL,
        @JSONDescription("A rocket-powered engine.  Uses only internal fuel and must be rebuilt each use.")
        ROCKET,
        @JSONDescription("An electric engine.  Gets power from chargers connected to the grid.")
        ELECTRIC;
    }

    public static class JSONPartGroundDevice {
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

        @ModifiableValue
        @JSONDescription("How much friction this part has for forwards movement. Used in cars to determine how much grip wheels have with the ground and if they should spin out, and used with all vehicles when brakes are applied to determine how much force they should be able to apply.")
        public float motiveFriction;

        @ModifiableValue
        @JSONDescription("How much friction this part has for sideways movement. Used during turning operations to determine how much the vehicle will skid sideways when turning at speed.")
        public float lateralFriction;

        @JSONDescription("If set, this part will create an extra collision box offset in the +Z direction by this amount when placed on a vehicle.  This collision box will have all the same properties as this part (wheel, floating, friction, etc.).  Useful for longer parts like pontoons or helicopter landing skids.  Note that if this parameter is set in the vehicle JSON the vehicle value will override this value.  Since treads are normally vehicle-dependent in their size, it is recommended to NOT set this here for treads and use the vehicle parameter.")
        public float extraCollisionBoxOffset;

        @JSONDescription("The spacing between repeated tread links to be animated in the model.  Used only if isTread is true.")
        public float spacing;

        @JSONDescription("The object order to render for the treads.  Allows rendering of patterend tread links with multiple objects in the same model, rendering in the defined sequence.  Used only if isTread is true.")
        public List<String> treadOrder;

        @JSONRequired
        @JSONDescription("A mapping of friction modifiers.  These are used to determine the friction change when on specific surfaces.  Defaults to -0.1 (except for treads) on wet surfaces, and -0.2 on ice and snow for all ground devices.  Valid surfaces are: normal, normal_wet, dirt, dirt_wet, sand, sand_wet, snow, and ice.")
        public LinkedHashMap<BlockMaterial, Float> frictionModifiers;

        @Deprecated
        public boolean canGoFlat;
    }

    public static class JSONPartPropeller {
        @JSONDescription("If this is present and set, the propeller will have a dynamic pitch.  Propellers with dynamic pitch automatically change their pitch to keep their speed at the top end of the max RPM of the engine.  Below that range their pitch will decrease to a minimum of 45, and above that range it will increase to whatever value is specified by the 'pitch' parameter.  Dynamic pitch propellers are also able to provide reverse thrust, though at a significantly reduced power level to their forward-thrust capabilities.")
        public boolean isDynamicPitch;

        @JSONDescription("If true, MTS will consider this propeller a rotor and will angle it when the aircraft control keys are pressed.  This will cause the thrust to be vectored in different directions.  Designed for helicopters, which use rotors to control their movement.")
        public boolean isRotor;

        @JSONDescription("Pitch is a critical defining characteristic of this part.  In essence, pitch is how far forward, in inches, the propeller tries to move in one revolution.  This, coupled with the RPM and gear ratio of the engine, determines the max possible speed of the aircraft this propeller is attached to.  Note, however, that propellers with higher pitches are less efficient at slower speeds so they require a longer runway.  This should be considered carefully before designing a propeller with a high pitch...")
        public int pitch;

        @JSONDescription("The diameter of this propeller, in inches.  Higher-diameter propellers provide more thrust at the same RPM as lower-diameter units.  However, the higher the diameter the more force they take to turn and the more powerful an engine will need to be to drive them (some low-power engines may not even be able to start with large propellers on them).  Additionally, the higher the diameter the lower the maximum RPM the propeller can turn.  Should the propeller exceed this speed it will break off and the engine it was attached to will suddenly not have a load and will rev up rapidly.")
        public int diameter;
    }

    public static class JSONPartSeat {
        @JSONDescription("If true, the player will stand in this seat rather than sit.  Note that some mods may mess this up and force the player to sit, so be advised of this.")
        public boolean standing;

        @JSONDescription("If included, the player will be scaled along to this X, Y, and Z value when sitting in this seat.  Useful for times when you can't fit a regular seat.  You can also use this to make the player invisible with a small enough size.")
        public Point3D playerScale;

        @Deprecated
        public float widthScale;

        @Deprecated
        public float heightScale;
    }

    public static class JSONPartGun {
        @JSONDescription("How a gun that has guided bullets determines if it has a lock.")
        public LockOnType lockOnType;

        @JSONDescription("Type of target this gun can lock on to.")
        public TargetType targetType;
        
        @JSONDescription("If set, this causes the gun to automatically reload from the vehicle's inventory when its ammo count hits 0.  Guns will prefer to reload the same ammo that was previously in the gun, and will only reload different (yet compatible) ammo if the old ammo is not found.")
        public boolean autoReload;

        @JSONDescription("If set and true, then this gun part will be able to be held and fired from the player's hand.  All animations, and lighting applies here, so keep this in mind. If this is set, then handHeldNormalOffset and handHeldAimingOffset MUST be included!  Note that custom cameras will work when hand-held, but they will not be activated via the standard F5 cycling.  Instead, they will be activated when the player sneaks.  This is intended to allow for scopes and the like.")
        public boolean handHeld;

        @JSONDescription("If true, then this gun will force the custom camera when hand-held.  Useful for custom HUDs.  Does not affect third-person mode.")
        public boolean forceHandheldCameras;

        @JSONDescription("If set, the gun will only be able to be fired once per button press.")
        public boolean isSemiAuto;

        @JSONDescription("If true, this makes it so that only one of this type of gun can be selected and fired at a time. This is useful for missiles and bombs that have different types of ammunition, as you can load different guns with different types of ammunition, and switch between the individual guns. If not used or set to false, cycling through weapons will select all weapons of the same type.")
        public boolean fireSolo;

        @JSONDescription("If true, this gun will return to its default yaw and pitch if it is not active. This is useful for anyone who likes to keep their large assortment of weapons nice and tidy.")
        public boolean resetPosition;

        @JSONDescription("If true, then this gun will fire bullets to align with itself only, and not with the muzzle rot paramter.  However, the initial velocity will still align with the rot parameter.  This allows the muzzle to be rotated to adjust the firing direction without modifying the orientation of the spawned bullet.  Think bomb bays and rocket launchers with a jettison before burn.")
        public boolean disableMuzzleOrientation;

        @JSONDescription("The capacity of the gun, in number of bullets.")
        public int capacity;

        @JSONDescription("How long, in ticks, this gun takes to reload.  This is applied for hand-held reloading as well as automatic reloading.  This value should be similar to the duration of your gun _reloading sound to ensure players don't get confused about why they can't fire their guns.")
        public int reloadTime;

        @JSONDescription("How long, in ticks, this gun takes to start firing after pulling the trigger.  This is designed for chain-gun type guns that need a short period of wind-up before they can start firing.  When the trigger is released, the gun will wind-down for the same amount of time it took to wind up.  If the gun doesn't wind all the way down before pulling the trigger again, it will start to wind back up from that point rather than 0.")
        public int windupTime;

        @JSONDescription("How fast, in m/s, the bullet will exit the barrel of this gun.  May be 0 in cases where bombers are concerned, as the exit velocity of the barrel is this value PLUS the velocity of the vehicle that's firing the bullet.")
        public int muzzleVelocity;

        @JSONDescription("The delay, in ticks, between the firing of bullets.")
        public float fireDelay;

        @JSONDescription("How much velocity, each tick, should be added in the -Y direction.  Used to make bullets travel in arcs.")
        public float gravitationalVelocity;

        @JSONDescription("How much spread the bullet will have when fired.  0 is no spread, higher values have higher spread.")
        public float bulletSpreadFactor;

        @JSONDescription("The minimum yaw this gun can turn counter-clockwise when controlled.  Note that if this is 0, and it is specified in the part JSON section on the vehicle, the vehicle's parameter will override this parameter.")
        public float minYaw;

        @JSONDescription("The maximum yaw this gun can turn clockwise when controlled.  Note that if this is 0, and it is specified in the part JSON section on the vehicle, the vehicle's parameter will override this parameter.")
        public float maxYaw;

        @JSONDescription("The minimum pitch this gun can angle downwards when controlled.  Note that if this is 0, and it is specified in the part JSON section on the vehicle, the vehicle's parameter will override this parameter.")
        public float minPitch;

        @JSONDescription("The maximum pitch this gun can angle upwards when controlled.  Note that if this is 0, and it is specified in the part JSON section on the vehicle, the vehicle's parameter will override this parameter.")
        public float maxPitch;

        @JSONDescription("The diameter of this gun.  This defines what ammo diameter may be used with it, and is what corresponds to the min-max parameters in the vehicle JSON.  It is also used to calculate rotation speed.  Units are in mm.")
        public float diameter;

        @JSONDescription("The minimum case length of bullets that can go into this gun.  Useful for preventing extra-long bullets from going into a gun that normally fires short ones.  Units are in mm.")
        public float minCaseLength;

        @JSONDescription("Like minCaseLength, but the maximum.")
        public float maxCaseLength;

        @JSONDescription("How fast, in degrees/tick, the gun can rotate in the yaw direction.  Note that if this value, and the value on the part slot are both specified, the lower of the two values will be used.")
        public float yawSpeed;

        @JSONDescription("Like yawSpeed, but for pitch.")
        public float pitchSpeed;

        @JSONDescription("Used when resetPosition is true. Defaults to 0 if not set.")
        public float defaultYaw;

        @JSONDescription("Used when resetPosition is true. Defaults to 0 if not set.")
        public float defaultPitch;
        
        @JSONDescription("How far away the gun will be able to lock targets.")
        public int lockRange;

        @JSONDescription("Angle in degrees around gun's orientation that it wil see targets.")
        public double lockMaxAngle;

        @JSONRequired(dependentField = "handHeld", dependentValues = {"true"})
        @JSONDescription("The offset where this gun will be when held normally by the player.  An offset of 0,0,0 will render the gun in the center of the player's right shoulder rotation point.  For reference, this is 0.3125 blocks to the right, and 1.375 blocks from the bottom-center of the player's feet.")
        public Point3D handHeldNormalOffset;

        @JSONRequired(dependentField = "handHeld", dependentValues = {"true"})
        @JSONDescription("Like the normal offset, but this applies when the player starts sneaking/aiming.")
        public Point3D handHeldAimedOffset;

        @JSONRequired
        @JSONDescription("A list of muzzle groups.  When firing this gun, the list is cycled though, and each group of muzzles takes turns firing.  If there are multiple muzzles in the group, they are all fired.  This allows for guns with muzzles that fire in sequence, or all at once.")
        public List<JSONMuzzleGroup> muzzleGroups;

        @Deprecated
        public float length;
    }
    
    public enum LockOnType {
        @JSONDescription("Look at stuff to get a lock")
        DEFAULT,
        @JSONDescription("The gun itself must see the target.")
        BORESIGHT,
        @JSONDescription("Goes where the player is pointed")
        MANUAL,
        @JSONDescription("Goes where the radar tells it to.")
        RADAR
    }

    public enum TargetType {
        @JSONDescription("Will lock on to anything. Default")
        ALL,
        @JSONDescription("Only Locks onto ground vehicles.")
        GROUND,
        @JSONDescription("Only locks on to aircraft.")
        AIRCRAFT,
        @JSONDescription("Will lock either aircraft or ground vehicles. Hard targets.")
        HARD,
        @JSONDescription("Only locks on to players or mobs.")
        SOFT
    }

    public static class JSONPartInteractable {
        @JSONRequired
        @JSONDescription("What this interactable does when interacted with.")
        public InteractableComponentType interactionType;

        @JSONRequired(dependentField = "interactionType", dependentValues = {"FURNACE"})
        @JSONDescription("What type of furnace this is.  Only required if this is a furnace component.")
        public FurnaceComponentType furnaceType;

        @JSONDescription("The processing rate of this furnace.  This will make the furnace process items faster.  This does NOT affect the fuel used, so a 2x multiplier here will make the furnace process and use fuel twice as fast.")
        public float furnaceRate;

        @JSONDescription("The efficiency of the furnace.  A value of 1.0 will make it use the standard rate of fuel for processing.  Lower values will use less fuel, higher values more fuel.  For FUEL furnaces, a value of 1.0 makes for 20 ticks of burn time for 1mb.  For ELECTRIC furnaces, a value of 1.0 gives 500 ticks burn time for 1 electric unit.")
        public float furnaceEfficiency;

        @JSONDescription("If set, this part's inventory can be used by the vehicle and its parts.  This does not affect loader/unloader operations.")
        public boolean feedsVehicles;

        @JSONDescription("If this part is a crate with ammo, or barrel with fuel, and it is hit, then when it is destroyed it will not make a massive explosion like normal.  Used for specialty ammo crates that shouldn't nuke the vehicle they are on if compromised.")
        public boolean hasBlowoutPanels;

        @JSONDescription("If set, this inventory will be able to be opened while in the player's hand.  Only valid for chests.")
        public boolean canBeOpenedInHand;

        @JSONDescription("If this part is a crate or barrel, this defines the size of its inventory. This is also what is used for min/max value calculations on vehicles.  For crates, this is how many rows (of 9 slots) the inventory has.  For barrels, this is how many buckets the barrel can store x10.  The idea being that 1 unit for crates holds a bit less than 1 unit of barrels, as with barrels you're storing the raw material, and not the container.")
        public float inventoryUnits;

        @JSONDescription("The texture for the GUI for this interactable part.  Only used if this part has a GUI.  If not set, the default is used.")
        public String inventoryTexture;

        @JSONDescription("A optional crafting definition for this interactable.  Requires an interactable type of crafting_bench to do anything.")
        public JSONCraftingBench crafting;
    }

    public enum InteractableComponentType {
        @JSONDescription("Stores items.  If feedsVehicles is set, then this crate will allow other parts such as guns, engines, and effectors to pull inventory out for their operations.  If the vehicle containing this crate explodes, and the crate contains ammo, the explosion size will be increased based on the amount and size of the ammo.  Additionally, if this crate has ammo and is struck by a bullet, it will blow up, taking the vehicle with it.")
        CRATE,
        @JSONDescription("Stores liquid.  If feedsVehicles is set, and this barrel contains fuel, then fuel will be taken out of this barrel to be put into the main fuel tank.  If the vehicle containing this barrel explodes, and the barrel contains fuel, the explosion size will be increased as if the vehicle had the fuel in the barrel in its fuel tank.")
        BARREL,
        @JSONDescription("Works as a standard crafting table when clicked.")
        CRAFTING_TABLE,
        @JSONDescription("Works as a furnace when clicked.  Will take fuel internally, or externally depending on the furnace type.")
        FURNACE,
        @JSONDescription("Works as a jerrycan, allowing for fuel to be stored inside and then used to fuel vehicles without a fuel pump.")
        JERRYCAN,
        @JSONDescription("Works as a MTS crafting bench when clicked.  This requires supplemental parameters.")
        CRAFTING_BENCH
    }

    public enum FurnaceComponentType {
        @JSONDescription("Standard furnace with Vanilla burnable fuel.  Will pull from crates if those feed vehicles.")
        STANDARD,
        @JSONDescription("Runs off fuel liquid stored in barrels on the vehicle.")
        FUEL,
        @JSONDescription("Runs off electric power.  Only valid for vehicles.")
        ELECTRIC
    }

    public static class JSONPartEffector {
        @JSONRequired
        @JSONDescription("The type of the effector.  This defines what this effector does.")
        public EffectorComponentType type;

        @JSONDescription("How hard a block the effector can break.  Only valid for drills.")
        public float drillHardness;

        @JSONDescription("How fast a drill can break a block at the specified hardness.  Softer blocks will break quicker.")
        public float drillSpeed;

        @JSONDescription("How many blocks the drill can break before it itself breaks.")
        public int drillDurability;
    }

    public enum EffectorComponentType {
        @JSONDescription("Checks for plants on the ground and applies bonemeal to them contained in crates.")
        FERTILIZER,
        @JSONDescription("Will harvest plants that pass through it, depositing items into vehicle crates, or on the ground if no crates are present, or their inventories are full.  Will also break bush blocks like tall grass, flowers, and saplings, depositing them on the ground.")
        HARVESTER,
        @JSONDescription("Will plant any plant-able items located in vehicle crate parts onto farmland. Does not work with cactus, reeds, or other non-farmland-based plants.")
        PLANTER,
        @JSONDescription("Turns dirt and grass into farmland, and coarse dirt into dirt.")
        PLOW,
        @JSONDescription("Removes snow from the world when touched.")
        SNOWPLOW,
        @JSONDescription("Removes blocks matching the parameters from the world when touched.")
        DRILL
    }

    @Deprecated
    public static class PartCustom {
        public float width;
        public float height;
    }

    @Deprecated
    public static class PartWheel {
        public float diameter;
        public float motiveFriction;
        public float lateralFriction;
    }

    @Deprecated
    public static class PartSkid {
        public float width;
        public float lateralFriction;
    }

    @Deprecated
    public static class PartPontoon {
        public float width;
        public float lateralFriction;
        public float extraCollisionBoxOffset;
    }

    @Deprecated
    public static class PartTread {
        public float width;
        public float motiveFriction;
        public float lateralFriction;
        public float extraCollisionBoxOffset;
        public float spacing;
    }
}
