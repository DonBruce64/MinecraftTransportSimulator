package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("This is the most complex area of the entire JSON system and is where you'll be spending the bulk of your time.  As the vehicle JSON defines positions that relate to your model, it is important to ensure that your model is correctly scaled, positioned, and oriented BEFORE making the JSON file.  If you have to re-do your model, you also may have to re-do JSON work, and that's extra work you don't want to do.\nTo ensure your vehicle's model is correct you can simply replace any OBJ model in a pack with your model.  This will cause MTS to load that model instead, allowing you to verify that the model is correct before you go to the work of making a JSON for it.  Once that is done, you're ready to tackle the JSON file.  JSON files can be rather long, so expect a lot of sub-sections here.\nA note about treads:\nTread paths are automatically created in the vehicle by checking for rollers in the format of \"$roller_xx\", where xx is the roller number.  The roller number must start at 0, and increment in a counter-clockwise direction when viewed from the left side.  The first roller MUST be the bottom-front roller in this case, with the second roller being behind it and also on the ground.  In other words, the rollers will increment in the direction of tread movement when the vehicle is going forwards.  For this reason, it is highly recommended that you simply make the 0 roller the one that's the first ground-contacting roller in the tread path and then follow the tread direction from there.  Also note that the name \"$roller_xx\" MUST be in lowercase.  From these rollers MTS will auto-create a tread path that follows said rollers, all without you needing to specify any points or do any JSON work!\nIn addition to creating a path, the MTS system will also auto-add all appropriate rotations to the JSON's animation sections.  This means you can simply name your rollers according to convention, set some JSON parameters for your part, and let MTS do the heavy-lifting calculating all the points and rotation speeds for your cogs and idlers.")
public class JSONVehicle extends AJSONPartProvider{
	@JSONRequired
	@JSONDescription("The motorized section contains all the core vehicle parameters.  This defines how your vehicle acts and handles.")
    public VehicleMotorized motorized;
    
	@Deprecated
    public VehiclePlane plane;
    @Deprecated
    public VehicleBlimp blimp;
    @Deprecated
    public VehicleCar car;
    
    public class VehicleMotorized{
    	@JSONDescription("If this is true, then MTS will consider this vehicle an aircraft and have it use the aircraft control system.  Has no other effect besides this.")
    	public boolean isAircraft;
    	
    	@JSONDescription("If this is true, then MTS will consider this vehicle a blimp and have it use the blimp control system.  This modifies how engines reverse on this vehicle; blimps reverse the gears in their engines rather than invert the propeller pitch, so the signal needs to be different.  This does not have to be combined with the isAircraft flag, but it is recommended to do so for actual blimps.")
    	public boolean isBlimp;
    	
    	@JSONDescription("If set to true, this vehicle will attempt to get and use the light states of any vehicle that is towing it. Useful for trailers where you want the lights to come on with the vehicle, but not towed cars where you want them to stay off.")
    	public boolean isTrailer;
    	
    	@JSONDescription("Set to true to have the engine power the front wheels.  This can be set in tandem with isRearWheelDrive to create allWheelDrive vehicles.")
    	public boolean isFrontWheelDrive;
    	
    	@JSONDescription("Same as isFrontWheelDrive, but for the rear wheels.")
    	public boolean isRearWheelDrive;
    	
    	@JSONDescription("Tells MTS that this vehicle does not have a roof.  This is used only for the SoundSystem and lets MTS know that sounds should be quieter when inside this vehicle.  This does not have an effect when in third-person, however, as the camera is considered outside of the vehicle so having a top or not does not matter.")
    	public boolean hasOpenTop;
    	
    	@JSONDescription("Make this true to allow your plane to come equipped with autopilot. Perhaps not the best thing to have on WWII fighters, but right at home on jet airliners. No, this won't work on cars.  This is MTS, not TMS (Tesla Motors Simulator).")
    	public boolean hasAutopilot;
    	
    	@JSONDescription("If set, this vehicle will be able to selecte and connect to beacons for directional wayfinding.")
    	public boolean hasRadioNav;
    	
    	@JSONDescription("Does the plane have flaps?  If so, set this to true.  Note that many older airplanes (pre-1950) were not equipped with flaps so just because you don't have any on your model does not mean it will fail to function.")
    	public boolean hasFlaps;
    	
    	@JSONDescription("Set this to true if you want the vehicle to have skidSteer functionality.  This allows the vehicle to turn in-place when stopped in neutral.  This will also automatically invert the rotation of the wheels and treads to match the steering orientation, so no need to mess with JSON bits.  Do keep in mind, however, that variables tied to the driveshaft won't work, as these use the engine's current gear, which will be 0!")
    	public boolean hasSkidSteer;
    	
    	@JSONDescription("The mass of this vehicle, when empty, in kg.  Note that fuel, cargo, players, and player inventories all count as weight, so this mass will not be the mass of the vehicle during normal operation.  Not too important in cars, but in aircraft this value should be as close to the real-life value as possible to avoid physics issues.")
    	public int emptyMass;
    	
    	@JSONDescription("The fuel capacity of this vehicle, in mb.")
    	public int fuelCapacity;
    	
    	@JSONDescription("If this is set, this vehicle will come pre-fueled with the specified fuel amount.  Note that an engine must be present for MTS to know what type of fuel is required, so make sure you set one via a defaultPart in the parts section of this JSON.")
    	public int defaultFuelQty;
    	
    	@JSONDescription("This parameter is optional.  If included, and set to anything besides 0, the vehicle will be considered to have landing gear, with the transition between up and down having the passed-in duration.  Most of the time you'll be using your own animations, so this is more just to make the gear lever appear in the panel and to tell MTS how to change the light states for it.")
    	public int gearSequenceDuration;
    	
    	@JSONDescription("The amount of steering force output for cars. The value functions between 0 and 1, with 1 being full steering force at any speed and 0 being normal MTS steering force.")
        public float downForce;
	
    	@JSONDescription("Controls the amount of spinning force a vehicle has while skidding. Can be set to negative for understeer. In most applications, it is ideal to use this in combination with downForce. anything under 8 or 9 should be good, except for specific cases.")
    	public float overSteer;
    	
    	@JSONDescription("The gear ratio present for the axle of this vehicle.  This is a constant, vehicle-specific ratio that will be multiplied with the gear ratio of the currently-selected gear of the engine to determine the rotation of the wheels.  A good many cars have a 3.55 ratio, but other of course are possible.  All depends on how much power you expect your engine to have, and how fast you want your car to go.  Note that this parameter is required if you want your engine to drive wheels and you have isFrontWheelDrive or isRearWheelDrive set.")
    	public float axleRatio;
    	
    	@JSONDescription("The factor for how effective the brakes are on this vehicle.  1.0 is default, with higher values making for more effective brakes.  Note that this doesn't affect braking in bad weather, with flat tires, or missing wheels, as should be obvious.")
    	public float brakingFactor;
    	
    	@JSONDescription("The angle which this vehicle will try to tilt to at max turning.  Note that the vehicle may not reach this angle if it isn't going fast enough.  Designed for bikes and boats.")
    	public float maxTiltAngle;
    	
    	@JSONDescription("How areodynamic this vehicle is.  Not required, but for things like cars this will make a significant difference in your high-speed performance.  So do some research before you slap some random value in here!  If you don't set this parameter, one will be automatically generated.  Planes and non-planes have a different formula, as planes are more areodynamic than most other vehicles.")
    	public float dragCoefficient;
    	
    	@JSONDescription("The distance from the center of rotation of the model, to the center point of the tail, in the Z-axis, in meters.  This essentially tells MTS where the rudder and elevators are located so it knows where to apply the forces they create.")
    	public float tailDistance;
    	
    	@JSONDescription("The wingspan of this vehicle, or distance between the wingtips, in meters.")
    	public float wingSpan;
    	
    	@JSONDescription("The surface area of the wings of this vehicle, in square meters.  Make sure not to include the fuselage between the wings as that doesn't generate lift!")
        public float wingArea;
    	
    	@JSONDescription("Similar to wingArea, but for the ailerons.  Units are square meters.")
        public float aileronArea;
    	
    	@JSONDescription("Similar to wingArea, but for the elevators.  Units are square meters.")
        public float elevatorArea;
    	
    	@JSONDescription("Similar to wingArea, but for the rudder.  Units are square meters.")
        public float rudderArea;
    	
    	@JSONDescription("The cross-sectional area of this vehicle, at its thickest point.  This is used to calculate yaw-based drag on vehicles.  Auto-calculated if left blank, and not required for most vehicles (and has no effect on winged vehicles), but is pretty much required on blimps to ensure they generate enough dynamic drag to allow the rudder to change their direction.")
        public float crossSectionalArea;
    	
    	@JSONDescription("How big the ballast volume is for this vehicle.  An average value is 1/1000 of the empty weight.  Set higher or lower to your liking.  This will let your vehicle vertically ascend without power.  Used for blimps and janky elevators.")
        public float ballastVolume;
        
        @JSONDescription("If this is present, MTS will render this texture for the HUD rather than the default texture.  Make sure to include a _lit variant otherwise things will look weird!")
        public String hudTexture;
        
        @JSONDescription("Same as hudTexture, but for the panel.")
        public String panelTexture;
        
        @JSONDescription("The color for the text in the panel that renders below components.  If this is not included MTS will default to white.")
        public String panelTextColor;
        
        @JSONDescription("Same as panelTextColor, but for the text when the vehicle's lights are on.")
        public String panelLitTextColor;
    	
    	@JSONRequired
    	@JSONDescription("A list of instruments definitions.  Instrument definitions are used to tell MTS where to render instruments on the vehicle, and where they correspond to on the HUD.  They also are used to tell MTS if the instrument is specific to an engine or not on vehicles that have multiple engines.")
        public List<JSONInstrumentDefinition> instruments;
    	
        
    	@Deprecated
    	public boolean isBigTruck;
    	@Deprecated
    	public boolean hasCruiseControl;
        @Deprecated
    	public String hornSound;
        @Deprecated
    	public String sirenSound;
    	@Deprecated
        public Point3d hitchPos;
    	@Deprecated
        public List<String> hitchTypes;
    	@Deprecated
        public Point3d hookupPos;
    	@Deprecated
        public String hookupType;
    }
    
    @Deprecated
    public class VehiclePlane{
        public boolean hasFlaps;
        public boolean hasAutopilot;
        public float wingSpan;
        public float wingArea;
        public float tailDistance;
        public float aileronArea;
        public float elevatorArea;
        public float rudderArea;
    }
    @Deprecated
    public class VehicleBlimp{
        public float crossSectionalArea;
        public float tailDistance;
        public float rudderArea;
        public float ballastVolume;
    }
    @Deprecated
    public class VehicleCar{
        public boolean isBigTruck;
        public boolean isFrontWheelDrive;
        public boolean isRearWheelDrive;
        public boolean hasCruiseControl;
        public float axleRatio;
        public float dragCoefficient;
    }
    
    
    
    public class JSONInstrumentDefinition{
    	@JSONRequired
    	@JSONDescription("An entry of x, y, and z coordinates that define the center of the instrument on the vehicle.")
    	public Point3d pos;
    	
    	@JSONRequired
    	@JSONDescription("An entry of x, y, and z rotations that tell MTS how to rotate this instrument.  By default all instruments face -z, or the rear of the vehicle.  This can be used to change rotation to fit a different spot of the vehicle if desired.")
        public Point3d rot;
    	
    	@JSONDescription("The scale of the instrument.  By default instruments are 128x128.")
        public float scale;
    	
    	@JSONDescription("The x-coordinate for the center of this instrument on the HUD, in pixels.")
        public int hudX;
    	
    	@JSONDescription("The y-coordinate for the center of this instrument on the HUD, in pixels.")
        public int hudY;
    	
    	@JSONDescription("Like scale, but for the HUD and Panel instead.")
        public float hudScale;
    	
    	@JSONDescription("If included and set, then MTS will try to grab this part number for any animation done by this instrument, unless the instrument already has a part number hard-coded.  Note that this will only happen if the animation is for a part, so instruments with non-part animations may safely be put in this slot.  This will also move it to the panel rather than the main HUD.  Useful in multi-engine vehicles.")
        public int optionalPartNumber;
    	
    	@JSONDescription("Normally vehicles come bare-bones, but in the case you want to have the instrument in this position come with the vehicle, you can set this.  If an instrument name is put here, MTS will automatically add said instrument when the vehicle is spawned for the first time.  Note that MTS won't check if the instrument actually exists, so either keep things in-house, or require packs you use as a dependency.  Also note that it is possible to combine this with an inaccessible hudX and hudY coordinate to put the instrument off the HUD.  This will effectively make this instrument permanently attached to the vehicle.")
        public String defaultInstrument;
    	
    	@JSONDescription("This is a list of animatedObjects that can be used to move this instrument on the vehicle based on the animation values.  Note that the instrument animations are applied AFTER the instrument is moved to its initial potion and rotation, and all animations are applied relative to that orientation.  As such, you will have to adjust your parameters to accommodate this.")
        public List<JSONAnimationDefinition> animations;
    }
}
