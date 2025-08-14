package minecrafttransportsimulator.jsondefs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONPartDefinition {
    public transient boolean bypassSlotMinMax;

    @JSONRequired
    @JSONDescription("An entry of x, y, and z coordinates that define the center point of where this part will attach to the object.")
    public Point3D pos;

    @JSONDescription("The x, y, and z rotations that tell MTS how to rotate this part.  If no rotations are desired, this can be omitted.")
    public RotationMatrix rot;

    @JSONDescription("Like pos, but in this case this is where the player should dismount the seat that is placed in this position.  By default, MTS will attempt to dismount the player directly to the left or right of the seat (depends on X position).  If this is set, MTS will instead attempt to dismount the player at this position.  Note that in either case if the dismount position is blocked MTS will fall back to allowing Minecraft to dismount the player, which can lead to the player becoming stuck in a vehicle.  If you have a vehicle with a complex hitbox, make sure you set this!")
    public Point3D dismountPos;

    @JSONDescription("This parameter is optional.  If included and set to true, the part will be used to steer vehicles.  If you want the part to visually turn, you will need to add an animation block and specify this.")
    public boolean turnsWithSteer;

    @JSONDescription("This parameter is optional.  If included and set to true, the seat at this location will be marked as a controller seat.  This allows the player in this seat to control the vehicle.  You may have multiple controller seats per vehicle (such as a pilot and co-pilot seat for a plane), but inputs from the controllers are all handled equally, so just because you are in the �first� seat does not mean you get full control.")
    public boolean isController;

    @JSONDescription("Tells MTS that this part is located oustide the vehicle.  Used only for seats, this will result in MTS treating the player like they are standing outside of the vehicle for sound purpouses.")
    public boolean isExterior;

    @JSONDescription("This parameter is optional.  If included and set to true, and this is on a gun part, and the part in this slot is also a gun, then when the part-slot gun is active, it will be allowed to control the main gun.  This allows for switching to SMGs placed on turrets while still moving the turret, but not firing from it.")
    public boolean isCoAxial;

    @JSONDescription("This parameter is optional.  If included and set to true, any part in this position will not be able to be removed.  Useful in conjunction with a defaultPart.")
    public boolean isPermanent;

    @JSONDescription("This parameter is optional.  If included and set to true, then this part will be marked as a spare.  If the part is a ground type, then it will not be used for ground device operations, and if it is a gun it will not be fire-able or select-able.  Useful for decorations like spare tires and gun racks.")
    public boolean isSpare;

    @JSONDescription("This parameter is optional.  If included and set to true, this part will be flagged as mirroed.  This has no effect except for setting this property on the part, which can then be queried by the part's animations, and making ground devices return a negative rotation for any rotation variables.  Mainly used for rotating wheels around on the other side of vehicles, or swapping out seat/door models.")
    public boolean isMirrored;

    @JSONDescription("This parameter is optional.  If included and set to true, then when selecting guns from this seat the ability to select no gun will be present.  Useful for seats controlling guns that you may not want to always be active.")
    public boolean canDisableGun;

    @JSONDescription("If true, this gun will return to its default yaw and pitch if it is not active. This is useful for anyone who likes to keep their large assortment of weapons nice and tidy.")
    public boolean resetPosition;

    @JSONDescription("This parameter is optional.  If included and set to true, then when a custom camera is available for use, then this custom camera will ALWAYS be used in first-person mode.  This is designed for seats where first-person mode is not desired, such as tanks or gunner pods.  Does not affect third-person mode.")
    public boolean forceCameras;

    @JSONDescription("This parameter is optional.  If included and set to true, then this part will not block the removal of its parent.")
    public boolean allowParentRemoval;

    @JSONDescription("This parameter is optional.  If set, then whenever the paint gun is used on the vehicle this part is on, this part will attempt to match the tone specified by the index.  If the specified tone does not exist, the part will not change.  Default parts will automatically attempt to match tones, but hand-placed parts will not change unless painted.")
    public int toneIndex;

    @JSONDescription("How many levels deep parts in this slot allow parts to be stacked on it.")
    public int maxPartLevels;

    @JSONDescription("The minimum value that this slot will accept.  Parts with values lower than this will be invalid and show up as red holograms.  This differs depending on the part, with engines using fuelConsumption as their value, ground_devices using their diameter, and propellers using their diameter as well.  This allows you an easy way to make sure people don't add oversized engines to your little econo-box or put monster tires on the family sedan.  See the details for each part for what parameter is used for min/max calculations.")
    public float minValue;

    @JSONDescription("Same as minValue, but the maximum.")
    public float maxValue;

    @JSONDescription("The minimum yaw for this part.  Used only on guns.  Note that if the minYaw is -180, and maxYaw is 180, then this will bypass all min/max yaw checks and allow the gun to rotate a full 360, even if the gun's JSON specifies bounds besides this.")
    public float minYaw;

    @JSONDescription("Like minYaw, but the max value.")
    public float maxYaw;

    @JSONDescription("The default yaw for the gun in this slot.  If the gun has resetPosition to true, then it will move to this position if it's not active.  This takes into account the gun's min/max yaw, and if this default exceedes those bounds, it will not be used.")
    public float defaultYaw;

    @JSONDescription("How fast, in degrees/tick, the gun can rotate in the yaw direction.  Note that if this value, and the value on the part are both specified, the lower of the two values will be used.")
    public float yawSpeed;

    @JSONDescription("Like minYaw, but the pitch value.")
    public float minPitch;

    @JSONDescription("Like minPitch, but the max value.")
    public float maxPitch;

    @JSONDescription("Like defaultYaw, but for pitch.")
    public float defaultPitch;

    @JSONDescription("Like yawSpeed, but for pitch.")
    public float pitchSpeed;

    @JSONDescription("If set, this part will create an extra collision box offset in the +Z direction by this amount when placed on a vehicle.  This collision box will have all the same properties as this part (wheel, floating, friction, etc.).  Useful for treads, where the length depends on the vehicle the tread is placed on.  This parameter overrides the same-named parameter in the part JSON, if that parameter is set.")
    public float extraCollisionBoxOffset;

    @JSONDescription("The width of the holographic part slot box for this slot.  Does not apply to generic parts which use their defined size.")
    public float slotWidth;

    @JSONDescription("Like slotWidth, but height.")
    public float slotHeight;

    @JSONDescription("If set, this will cause treads hanging along the top rollers to droop.  The amount they droop is defined by this constant, with higher values equating to less droop.  Note that if you want to use this parameter, it is recommended to keep your idler rollers at about the same spacing from one another, as it is possible to have too much droop on one set and not enough on another.  Adjust to suit your vehicle.")
    public float treadDroopConstant;

    @JSONDescription("If this part is an engine, this is how far above the engine the intake is. Used to prevent the engine from drowning while submerged.  Mainly used in SUVs and military vehicles.")
    public float intakeOffset;

    @JSONDescription("If included, the part will be scaled along to this X, Y, and Z value.  This will be the base scale, and future scaling will multiply this value.")
    public Point3D partScale;

    @JSONDescription("If included, the player will be scaled along to this X, Y, and Z value when sitting in this seat.  You can also use this to make the player invisible with a small enough size.  This value is multiplied by any scaling applied by the seat.  So if a seat scales 0.5, and you put 0.5 here, the scale will be 0.5 x 0.5 = 0.25.")
    public Point3D playerScale;

    @JSONDescription("Normally vehicles come bare-bones, but in the case you want to have the part in this position come with the vehicle, you can set this.  If a part name is put here, MTS will automatically add said part when the vehicle is spawned for the first time.  Note that MTS won't check if the part actually exists, so either keep things in-house, or require packs you use as a dependency.  Note that putting this on an additionalPart without the parent part having a defaultPart WILL crash the game!")
    public String defaultPart;

    @JSONDescription("This variable can be used to request parts transfer into and out of this slot.  Setting to true without a part will make it try to grab a part from the world, setting it true with a part will make it drop the part into the world, preferably into a part slot on something.  Only works with part that can be placed on the ground, and will be set to false after the operation completes.")
    public String transferVariable;

    @JSONDescription("Like defaultPart, but this is a map of variable values associated with individual parts.  If the variable is true, then that default part will be used.  If multiple possible parts are present, the first valid one is used.")
    public LinkedHashMap<String, String> conditionalDefaultParts;

    @JSONRequired
    @JSONDescription("A list of part types that can go in this position.  Normally you'll only have one entry in this list, as there's really no reason to have a ground_device at the same location as an engine.  There are some exceptions to this, however.  One is for interior equipment, like seats and chests, where you want players to be able to choose what they put in that position. ")
    public List<String> types;

    @JSONDescription("This parameter is optional.  If included, this slot will ONLY accept parts whose customType parameter matches one of the items in this list.  If the part's customType is not defined, and a blank entry of �� is not present in the list, it will be invalid and cannot be added.  Useful for pack creators who don't want their vehicles to play nice with other content packs, or slots that should only accept sub-sets of a type of part (such as specific styles of seats).")
    public List<String> customTypes;

    @JSONDescription("A list of subNames this part can go on.  If included, this part will only be allowed to go on the specified definition sub-name variants.  Used to allow for variant-specific configurations, such as police variants of cars.")
    public List<String> validSubNames;

    @JSONDescription("A list of objects on the model that define the path of the tread placed in this part slot.  The tread will start on the bottom of the first roller, and will follow the rollers as defined in this path, eventually going back to the first roller.")
    public List<String> treadPath;

    @JSONDescription("A double-nested list of variables.  If this is set, then this part will not be interactable with unless a variable in each of the sets is true (think of variables in a set being an OR condition, and each set being AND ed together).  Additionally, should the part not exist, it will not be able to be placed.  Useful for hoods covering engines, doors covering seats, switches activating sub-parts, and trunks covering luggage.  If this part is a seat, and the player enters the seat, then all of these variables will be set to false.  Similarly, if the player exits this seat, all the variables will be set to true.  Useful for auto opening/closing of doors.")
    public List<List<String>> interactableVariables;

    @JSONDescription("A double-nested list of variables.  If this is set, then this part will be locked in place unless all variables in each of the sets is true (think of variables in a set being an OR condition, and each set being AND ed together).")
    public List<List<String>> lockingVariables;

    @JSONDescription("A list of indexes for parts that are linked to this one.  Normally not used, but is required to link wheels to engines, propellers to engines, guns to seats that control them, guns to ammo crates for them to pull from, and effectors to crates they can pull supplies from and feed drops into.  The linking is bi-directional: you can link an engine to a wheel, or a wheel to an engine.  Also, linking is recursive.  So if you link an engine to an axle custom part, any wheels on that part when placed will be linked as if they were in that slot.  Similar logic applies for seats to hardmount points that might hold guns.")
    public List<Integer> linkedParts;

    @JSONDescription("If this part is a seat, this list of potion effects will be applied to the rider. This section works the same as the normal vehicle-based effects, but apply only to the rider of a specific seat, rather than the entire vehicle")
    public List<JSONPotionEffect> seatEffects;

    @JSONDescription("If this is set, then the animations on this part slot will first use the animations for this object (not the part) from the rendering section instead of the animations defined here.  If the specified object has applyAfter on it itself, then the animations will be gotten recursively until an applyAfter is not found.")
    public String applyAfter;

    @JSONDescription("This parameter is optional.  If set, then this ground part will be forcibly added to the specified GDB.")
    public JSONGroundDevicePosition groundDevicePosition;

    @JSONDescription("This is a list of animatedObjects that can be used to move this part based on the animation values. In general, this should only be used in the part needs to physically move, such as wheels being retracted into plane landing gear, or a gun that needs to have an offset mounting track.  However, since these objects actually move the part rather than change how it looks, there are some caveats and quirks that aren't normally present for all other animations.  They are as follows:\naddPriorOffset has no function with part movement animations.  This is because that relies of vector-based clamping, which part movement does not support.\nAs part rotation is angle-based rather than vector-based, the axis parameter is actually the angles the part will move in, not the axis the part will move around.  For simple rotations that only are applied in one axis, this has no effect on the JSON.  However, for multiple animations it may cause issues.  In particular, the order the animations are applied is key to proper function.  This is because as an animation is applied, it changes the axis for following animations.  So if you have an animation that rotates the part 90 degrees on the Y-axis, and then want to rotate it on the +X axis, you'd actually have to put in +Z rotation as the part has been rotated to a different orientation.")
    public List<JSONAnimationDefinition> animations;

    @JSONDescription("A listing of animation objects for determining if this part is active.  Leaving this blank will make for a part that is always active.  Visibility transforms will turn the part on and off.  Inhibitor and activator transforms may be used in conjunction with these for advanced on/off logic.  The exact thing that an 'active' part does depends on the part.  Effectors only effect when they are active.  Guns can only be used when active.  Seats can only be sat in when active.  etc.  Note that this does NOT block the placement of the part or interaction.  That logic is for the linkedVariables.")
    public List<JSONAnimationDefinition> activeAnimations;

    @JSONDescription("A mapping of constant-value variable names to values.  These variables will be added into the listing of active variables the moment the JSON is loaded. Note that they CAN be over-written if referenced as such, so keep this in mind. If defined here (in the part slot), will override the parent CVs.")
    public Map<String, Double> constantValues;

    @Deprecated
    public JSONPartDefinition additionalPart;
    @Deprecated
    public List<JSONPartDefinition> additionalParts;
    @Deprecated
    public String linkedDoor;
    @Deprecated
    public List<String> linkedDoors;
    @Deprecated
    public List<String> linkedVariables;
    @Deprecated
    public String translationVariable;
    @Deprecated
    public Point3D translationPosition;
    @Deprecated
    public float translationClampMin;
    @Deprecated
    public float translationClampMax;
    @Deprecated
    public boolean translationAbsolute;
    @Deprecated
    public String rotationVariable;
    @Deprecated
    public Point3D rotationPosition;
    @Deprecated
    public Point3D rotationAngles;
    @Deprecated
    public float rotationClampMin;
    @Deprecated
    public float rotationClampMax;
    @Deprecated
    public boolean rotationAbsolute;
    @Deprecated
    public float widthScale;
    @Deprecated
    public float heightScale;
    @Deprecated
    public boolean inverseMirroring;

    //Engine-specific part variables.
    @Deprecated
    public float[] exhaustPos;
    @Deprecated
    public float[] exhaustVelocity;
    @Deprecated
    public List<ExhaustObject> exhaustObjects;
    @Deprecated
    public List<JSONParticle> particleObjects;

    @Deprecated
    public static class ExhaustObject {
        public Point3D pos;
        public Point3D velocity;
        public float scale;
    }

    public static enum JSONGroundDevicePosition {
        FRONT_LEFT(true, true, false),
        FRONT_RIGHT(true, false, true),
        FRONT_CENTER(true, true, true),
        REAR_LEFT(false, true, false),
        REAR_RIGHT(false, false, true),
        REAR_CENTER(false, true, true);

        public final boolean isFront;
        public final boolean isLeft;
        public final boolean isRight;

        private JSONGroundDevicePosition(boolean isFront, boolean isLeft, boolean isRight) {
            this.isFront = isFront;
            this.isLeft = isLeft;
            this.isRight = isRight;
        }
    }
}
