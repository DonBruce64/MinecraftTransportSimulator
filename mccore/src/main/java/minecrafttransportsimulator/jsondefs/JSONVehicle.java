package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityD_Definable.ModifiableValue;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("This is the most complex area of the entire JSON system and is where you'll be spending the bulk of your time.  As the vehicle JSON defines positions that relate to your model, it is important to ensure that your model is correctly scaled, positioned, and oriented BEFORE making the JSON file.  If you have to re-do your model, you also may have to re-do JSON work, and that's extra work you don't want to do.\nTo ensure your vehicle's model is correct you can simply replace any OBJ model in a pack with your model.  This will cause MTS to load that model instead, allowing you to verify that the model is correct before you go to the work of making a JSON for it.  Once that is done, you're ready to tackle the JSON file.  JSON files can be rather long, so expect a lot of sub-sections here.\nA note about treads:\nTread paths are automatically created in the vehicle by checking for rollers in the format of \"$roller_xx\", where xx is the roller number.  The roller number must start at 0, and increment in a counter-clockwise direction when viewed from the left side.  The first roller MUST be the bottom-front roller in this case, with the second roller being behind it and also on the ground.  In other words, the rollers will increment in the direction of tread movement when the vehicle is going forwards.  For this reason, it is highly recommended that you simply make the 0 roller the one that's the first ground-contacting roller in the tread path and then follow the tread direction from there.  Also note that the name \"$roller_xx\" MUST be in lowercase.  From these rollers MTS will auto-create a tread path that follows said rollers, all without you needing to specify any points or do any JSON work!\nIn addition to creating a path, the MTS system will also auto-add all appropriate rotations to the JSON's animation sections.  This means you can simply name your rollers according to convention, set some JSON parameters for your part, and let MTS do the heavy-lifting calculating all the points and rotation speeds for your cogs and idlers.")
public class JSONVehicle extends AJSONPartProvider {
    @JSONRequired
    @JSONDescription("The motorized section contains all the core vehicle parameters.  This defines how your vehicle acts and handles.")
    public VehicleMotorized motorized;

    @Deprecated
    public VehiclePlane plane;
    @Deprecated
    public VehicleBlimp blimp;
    @Deprecated
    public VehicleCar car;

    public static class VehicleMotorized {
        @JSONDescription("If this is true, then MTS will consider this vehicle an aircraft and have it use the aircraft control system.  Has no other effect besides this.")
        public boolean isAircraft;

        @JSONDescription("If this is true, then MTS will consider this vehicle a blimp and have it use the blimp control system.  This modifies how engines reverse on this vehicle; blimps reverse the gears in their engines rather than invert the propeller pitch, so the signal needs to be different.  This does not have to be combined with the isAircraft flag, but it is recommended to do so for actual blimps.")
        public boolean isBlimp;

        @JSONDescription("If set to true, this vehicle will attempt to get and use the light states of any vehicle that is towing it. Useful for trailers where you want the lights to come on with the vehicle, but not towed cars where you want them to stay off.")
        public boolean isTrailer;

        @JSONDescription("Set this to true if you want the vehicle to have thrust vectoring.  False means only yaw-vectoring will occur for things like engine out situations.")
        public boolean hasThrustVectoring;

        @JSONDescription("Tells MTS that this vehicle does not have a roof.  This is used only for the SoundSystem and lets MTS know that sounds should be quieter when inside this vehicle.  This does not have an effect when in third-person, however, as the camera is considered outside of the vehicle so having a top or not does not matter.")
        public boolean hasOpenTop;

        @JSONDescription("Make this true to allow your plane to come equipped with autopilot. Perhaps not the best thing to have on WWII fighters, but right at home on jet airliners. No, this won't work on cars.  This is MTS, not TMS (Tesla Motors Simulator).")
        public boolean hasAutopilot;

        @JSONDescription("If set, this vehicle will be able to selecte and connect to beacons for directional wayfinding.")
        public boolean hasRadioNav;

        @JSONDescription("Set this to true if you want the vehicle to have skidSteer functionality.  This allows the vehicle to turn in-place when stopped in neutral.  This will also automatically invert the rotation of the wheels and treads to match the steering orientation, so no need to mess with JSON bits.  Do keep in mind, however, that variables tied to the driveshaft won't work, as these use the engine's current gear, which will be 0!")
        public boolean hasSkidSteer;

        @JSONDescription("Like hasSkidSteer, but in this case it's always active.  Used for things that don't have a wheelbase, like mechs and hovercraft.")
        public boolean hasPermanentSkidSteer;

        @JSONDescription("Set this to true if you want the vehicle to have incremental throttle.  This is only active for vehicles that are not aircraft, and will give the vehicle an aircraft-like throttle that increments in 1/100 units when the gas is pressed, and decrements in 1/100 units when the brake is pressed.  Mainly for boats and other constant-throttle vehicles.")
        public boolean hasIncrementalThrottle;

        @JSONDescription("Set this to true if you want only one engine control button to control all engines on the panel.  Useful if you have multiple engines in a vehicle, but want them all to start at the same time.")
        public boolean hasSingleEngineControl;

        @JSONDescription("Set this to true if the vehicle has these lights.  This will make the respective switch apper in the panel.")
        public boolean hasRunningLights;

        @JSONDescription("Set this to true if the vehicle has these lights.  This will make the respective switch apper in the panel.")
        public boolean hasHeadlights;

        @JSONDescription("Set this to true if the vehicle has these lights.  This will make the respective switch apper in the panel.")
        public boolean hasTurnSignals;

        @JSONDescription("Set this to true if the vehicle has these lights.  This will make the respective switch apper in the panel.")
        public boolean hasNavLights;

        @JSONDescription("Set this to true if the vehicle has these lights.  This will make the respective switch apper in the panel.")
        public boolean hasStrobeLights;

        @JSONDescription("Set this to true if the vehicle has these lights.  This will make the respective switch apper in the panel.")
        public boolean hasTaxiLights;

        @JSONDescription("Set this to true if the vehicle has these lights.  This will make the respective switch apper in the panel.")
        public boolean hasLandingLights;

        @JSONDescription("If this is true, the HUD will be always rendered as a half HUD.  Useful if you have a smaller HUD with nothing below the half-height.")
        public boolean halfHUDOnly;

        @JSONDescription("If this is true, the HUD will be always rendered as a full HUD.  Useful if you have specific large instruments.")
        public boolean fullHUDOnly;

        @JSONDescription("If this is true, and auto engine start is true, this vehicle will behave as if auto engine start is false.")
        public boolean overrideAutoStart;

        @JSONDescription("The mass of this vehicle, when empty, in kg.  Note that fuel, cargo, players, and player inventories all count as weight, so this mass will not be the mass of the vehicle during normal operation.  Not too important in cars, but in aircraft this value should be as close to the real-life value as possible to avoid physics issues.")
        public int emptyMass;

        @JSONDescription("The fuel capacity of this vehicle, in mb.")
        public int fuelCapacity;

        @JSONDescription("If this is set, this vehicle will come pre-fueled with the specified fuel amount.  Note that an engine must be present for MTS to know what type of fuel is required, so make sure you set one via a defaultPart in the parts section of this JSON.")
        public int defaultFuelQty;

        @JSONDescription("This parameter is optional.  If included, and set to anything besides 0, the vehicle will be considered to have landing gear, with the transition between up and down having the passed-in duration.  Most of the time you'll be using your own animations, so this is more just to make the gear lever appear in the panel and to tell MTS how to change the light states for it.")
        public int gearSequenceDuration;

        @JSONDescription("Set this to true if you want vehicles to ignore speed, and instead call from steeringForceFactor for their current steering force. Otherwise by default, vehicles will gradually lose their ability to steer as they gain speed.")
        public boolean steeringForceIgnoresSpeed;

        @ModifiableValue
        @JSONDescription("The amount of steering force output for cars, based either on current speed or as a whole, dependent on steeringForceIgnoresSpeed for choosing between such behavior. By default a value of 0 results in default MTS steering forces, while 1 allows full steering force at any speed. However if steeringForceIgnoresSpeed is set to true then 0 will result in no steering force at any speed, with 1 otherwise resulting in the same handling.")
        public float steeringForceFactor;

        @ModifiableValue
        @JSONDescription("A value dictating the oversteer force of a vehicle when skidding.")
        public float overSteer;

        @ModifiableValue
        @JSONDescription("A value dictating the understeer force of a vehicle when skidding.")
        public float underSteer;

        @JSONDescription("Used similarly to overSteer to control the exact rate of skidding during extreme acceleration.")
        public float overSteerAccel;

        @JSONDescription("Used similarly to underSteer to control the exact rate of skidding during extreme deceleration.")
        public float overSteerDecel;

        @ModifiableValue
        @JSONDescription("The gear ratio present for the axle of this vehicle.  This is a constant, vehicle-specific ratio that will be multiplied with the gear ratio of the currently-selected gear of the engine to determine the rotation of the wheels.  A good many cars have a 3.55 ratio, but other of course are possible.  All depends on how much power you expect your engine to have, and how fast you want your car to go.  Note that this parameter is required if you want your engine to drive wheels and you have isFrontWheelDrive or isRearWheelDrive set.")
        public float axleRatio;

        @ModifiableValue
        @JSONDescription("The factor for how effective the brakes are on this vehicle.  1.0 is default, with higher values making for more effective brakes.  Note that this doesn't affect braking in bad weather, with flat tires, or missing wheels, as should be obvious.")
        public float brakingFactor;

        @JSONDescription("The angle which this vehicle will try to tilt to at max turning.  Note that the vehicle may not reach this angle if it isn't going fast enough.  Designed for bikes and boats.")
        public float maxTiltAngle;

        @JSONDescription("How fast flaps deploy, in degrees/tick.  Only used if the vehicle has flap notches set.")
        public float flapSpeed;

        @ModifiableValue
        @JSONDescription("How areodynamic this vehicle is.  Defaults to 0.03 for aircraft, and 2.0 for cars, but can be adjusted to other values.  For things like cars this will make a significant difference in your high-speed performance.  So do some research before you slap some random value in here!  If you don't set this parameter, one will be automatically generated.  Planes and non-planes have a different formula, as planes are more areodynamic than most other vehicles.")
        public float dragCoefficient;

        @JSONDescription("The distance from the center of rotation of the model, to the center point of the tail, in the Z-axis, in meters.  This essentially tells MTS where the rudder and elevators are located so it knows where to apply the forces they create.")
        public float tailDistance;

        @ModifiableValue
        @JSONDescription("The wingspan of this vehicle, or distance between the wingtips, in meters.")
        public float wingSpan;

        @ModifiableValue
        @JSONDescription("The surface area of the wings of this vehicle, in square meters.  Make sure not to include the fuselage between the wings as that doesn't generate lift!")
        public float wingArea;

        @ModifiableValue
        @JSONDescription("Similar to wingArea, but for the ailerons.  Units are square meters.")
        public float aileronArea;

        @ModifiableValue
        @JSONDescription("Similar to wingArea, but for the elevators.  Units are square meters.")
        public float elevatorArea;

        @ModifiableValue
        @JSONDescription("Similar to wingArea, but for the rudder.  Units are square meters.")
        public float rudderArea;

        @JSONDescription("The cross-sectional area of this vehicle, at its thickest point.  This is used to calculate yaw-based drag on vehicles.  Auto-calculated if left blank, and not required for most vehicles (and has no effect on winged vehicles), but is pretty much required on blimps to ensure they generate enough dynamic drag to allow the rudder to change their direction.")
        public float crossSectionalArea;

        @ModifiableValue
        @JSONDescription("How big the ballast volume is for this vehicle.  An average value is 1/1000 of the empty weight.  Set higher or lower to your liking.  This will let your vehicle vertically ascend without power.  Used for blimps and janky elevators.")
        public float ballastVolume;

        @ModifiableValue
        @JSONDescription("The factor of which to apply ballast for water operations.  0 is no change, 0.5 makes them fall half speed, 1.0 makes vehicles neither float nor sink, and anything higher makes them float up.  Only applied when the vehicle is in water.")
        public float waterBallastFactor;

        @ModifiableValue
        @JSONDescription("The factor of which to apply gravity to this vehicle. If defined, will override the main mtsconfig file value.")
        public float gravityFactor;

        @JSONDescription("The speed at which 0% damage will be applied during crashes.  Crash damage values are optional and a default will be used if these are missing.")
        public float crashSpeedMin;

        @JSONDescription("The speed at which 100% damage will be applied.")
        public float crashSpeedMax;

        @JSONDescription("The speed at which the vehicle will blow up and be destroyed.  Needs to be higher than crashSpeedMax.")
        public float crashSpeedDestroyed;

        @JSONRequired
        @JSONDescription("When this variable is 1, the vehicle will be considered to be 'lit'.  This makes text and instruments light up, provided there's enough battery power to do so.")
        public String litVariable;

        @JSONRequired
        @JSONDescription("The panel to use for this vehicle.  Format is packID:panelName.")
        public String panel;

        @JSONDescription("If this is present, MTS will render this texture for the HUD rather than the default texture.  Make sure to include a _lit variant otherwise things will look weird!")
        public String hudTexture;

        @JSONDescription("Same as hudTexture, but for the panel.  This will override the value in panel if set.")
        public String panelTexture;

        @JSONDescription("The color for the text in the panel that renders below components.  If this is not included MTS will default to white.")
        public ColorRGB panelTextColor;

        @JSONDescription("Same as panelTextColor, but for the text when the vehicle's lights are on.")
        public ColorRGB panelLitTextColor;

        @JSONDescription("A listing of notches for flap deployment.  These will be used to determine the requested flap setting for vehicles that have them.  Only functional for vehicles where isAircraft is set to true.  Both 0 and the highest notch should be included")
        public List<Float> flapNotches;

        @JSONRequired(dependentField = "isTrailer", dependentValues = {"true"})
        @JSONDescription("A listing of variables that will be checked off the towing vehicle if this vehicle is a trailer and connected.  Used by trailers to get the states of their towing vehicles for light and door animations.")
        public List<String> hookupVariables;

        @Deprecated
        public boolean hasFlaps;
        @Deprecated
        public boolean isBigTruck;
        @Deprecated
        public boolean hasCruiseControl;
        @Deprecated
        public boolean isFrontWheelDrive;
        @Deprecated
        public boolean isRearWheelDrive;
        @Deprecated
        public float downForce;
        @Deprecated
        public String hornSound;
        @Deprecated
        public String sirenSound;
        @Deprecated
        public Point3D hitchPos;
        @Deprecated
        public List<String> hitchTypes;
        @Deprecated
        public Point3D hookupPos;
        @Deprecated
        public String hookupType;
        @Deprecated
        public List<JSONInstrumentDefinition> instruments;
    }

    @Deprecated
    public static class VehiclePlane {
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
    public static class VehicleBlimp {
        public float crossSectionalArea;
        public float tailDistance;
        public float rudderArea;
        public float ballastVolume;
    }

    @Deprecated
    public static class VehicleCar {
        public boolean isBigTruck;
        public boolean isFrontWheelDrive;
        public boolean isRearWheelDrive;
        public boolean hasCruiseControl;
        public float axleRatio;
        public float dragCoefficient;
        public String hornSound;
    }
}
