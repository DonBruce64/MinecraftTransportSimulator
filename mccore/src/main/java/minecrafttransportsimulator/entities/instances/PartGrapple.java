package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.DoublePredicate;

import minecrafttransportsimulator.baseclasses.BlockHitResult;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.BoundingBoxHitResult;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.EntityInteractResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TowingConnection;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartGrapple.LinePull;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemPartGrapple;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONCameraObject;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartGrapple;
import minecrafttransportsimulator.packets.instances.PacketPartGrappleHook;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;

/**
 * A grappling-hook launcher.  Firing launches the hook towards whatever is under the controller's
 * crosshair (or along the part's +Z axis with no controller).  It flies until it sticks into the first
 * block, entity or vehicle it hits, or runs out of line and is reeled back in.  Blocks hold the vehicle,
 * entities get reeled in, and vehicles share the pull by mass.
 * 
 * The hook is not a bullet.  Bullets are client-side particles that never sync or save, but the line
 * has to hold the same on every client and across reloads, so the server owns the hook and syncs it
 * to clients through variables.  Pack JSON drives and renders it through those same variables.
 * 
 * The line only ever pulls, and both of its ends get the same physics.  When it's taut, whatever is on
 * each end can't move further apart and gets pulled back to the line length.  A vehicle is pulled with
 * its whole tow chain, and the chain's tyres and brakes hold back what they can.  The line's torque turns
 * the vehicle so the point the line is on swings round towards the other end: it twists and pitches up
 * about its far wheels, or hangs below the line once clear of the ground.  Once a wheel or collision box at
 * the end of the launcher vehicle facing the hook touches a wall with the hook up on it, the line stops
 * pushing it about.  The vehicle squares up to the wall and the plane under its wheels and collision boxes is pitched up
 * the face and over the lip instead.  Near the top, it can then be hauled over the edge.  See {@link ClimbPath}.
 * 
 * All of this runs from {@link AEntityVehicleD_Moving} just before it moves, via {@link #updateLine()},
 * so the line's forces go through the vehicle's normal collision checks and movement sync.
 */
public class PartGrapple extends APart {
    public static final String VAR_FIRE = "grapple_fire";
    public static final String VAR_REEL = "grapple_reel";
    public static final String VAR_SLACK = "grapple_slack";
    public static final String VAR_STATE = "grapple_state";
    public static final String VAR_LINE_OUT = "grapple_line_out";
    public static final String VAR_ATTACHED = "grapple_attached";
    public static final String VAR_ROPE = "grapple_rope";
    public static final String VAR_MANTLE = "grapple_mantle";
    public static final String VAR_BODY_YAW = "grapple_body_yaw";
    public static final String VAR_BODY_PITCH = "grapple_body_pitch";
    public static final String VAR_LINE_YAW = "grapple_line_yaw";
    public static final String VAR_LINE_PITCH = "grapple_line_pitch";
    public static final String VAR_LENGTH = "grapple_length";
    public static final String VAR_SNAPPED = "grapple_snapped";
    public static final String VAR_CUT = "grapple_cut";
    public static final String VAR_MISSING_HOOK = "grapple_missing_hook";
    public static final String VAR_COOLDOWN = "grapple_cooldown";

    public static final int STATE_IDLE = 0;
    public static final int STATE_FLYING = 1;
    public static final int STATE_BLOCK = 2;
    public static final int STATE_ENTITY = 3;
    public static final int STATE_VEHICLE = 4;

    public static final int MANTLE_NONE = 0;
    /**Being winched up the wall.  How far up follows the line length.**/
    public static final int MANTLE_CLIMB = 1;
    /**Winched in as far as the line goes, close enough to the top to be pulled over it.  Holding reel for {@link #MANTLE_HOLD_TICKS} starts the pull.**/
    public static final int MANTLE_READY = 2;
    /**Being pulled up over the edge while reel is held, or slipping back to where it started while it isn't.**/
    public static final int MANTLE_PULL = 3;
    /**Flat on top of the wall, held there until the grapple is released.  Driving backwards goes back down.**/
    public static final int MANTLE_COMPLETE = 4;
    /**Driving backwards down the wall, letting the line out.**/
    public static final int MANTLE_DESCEND = 5;

    /**Fraction of any over-stretch pulled back each tick.  Higher is a stiffer line.**/
    private static final double STIFFNESS = 0.3;
    /**Cap on the pull-back speed from stretch, in blocks per tick, so the line can't fling anything.**/
    private static final double MAX_PULL = 0.6;
    /**Gravity on the dangling hook, in blocks per tick squared.**/
    private static final double DANGLE_GRAVITY = 9.81 / 400D;
    /**Fraction of the dangling hook's speed kept each tick, so its swing dies down.**/
    private static final double DANGLE_DAMPING = 0.95;
    /**How many times the range the two ends can be pulled apart before the line snaps.**/
    private static final double SNAP_FACTOR = 1.5;
    /**Cap on how fast a line can turn the vehicle the launcher is on, or its tow chain, in degrees per tick.**/
    private static final double MAX_PULL_TURN = 12;
    /**Cap on how fast a line can turn a vehicle we've hooked and are towing, or its tow chain, in degrees per tick.**/
    private static final double MAX_TOWED_PULL_TURN = 30;
    /**Most vehicles a tow chain is followed through to find the one at the front.  Far past any real chain, and only reached by one a bad save has looped back on itself.**/
    private static final int MAX_TOW_CHAIN_LENGTH = 1024;
    /**Turns smaller than this, in degrees, aren't applied to a vehicle on the line.**/
    private static final double MIN_PULL_TURN = 1.0E-3;
    /**A hook closer than this to the middle of a hooked vehicle's wheels, front to back, turns it about all its grounded wheels.**/
    private static final double PULL_PIVOT_END_DISTANCE = 0.25;
    /**Fraction of a tyre's lateral friction that holds against a line's sideways pull.  Brakes do most of the holding, so this only stops an unbraked vehicle sliding about like it's on ice.**/
    private static final double LINE_TYRE_GRIP = 0.25;
    /**A hook closer than this to the pivot, flat on the ground, is too close to swing the vehicle about it like a trailer.**/
    private static final double MIN_SWING_ARM = 0.5;
    /**Furthest a controller's camera can be from their seat, in blocks, for the server to take the camera aim they send.**/
    private static final double MAX_CAMERA_AIM_OFFSET = 64;
    private static final String HOOKED_MULTIPART_TAG = "grapple_hooked_multipart";
    private static final String HOOK_OFFSET_TAG = "grapple_hook_offset";
    /**How often dev mode logs where the hook sits on a hooked vehicle, in ticks.  See logServerHookDrift.  Temporary.**/
    private static final int HOOK_DRIFT_LOG_INTERVAL = 10;
    /**Converts kN to the kg*m/ticks^2 the vehicle physics uses, at 20 ticks a second.**/
    private static final double KN_TO_TICK_FORCE = 1000D / 400D;
    /**How many times the winch force the line snaps at when breakForce is left at 0.**/
    private static final double DEFAULT_BREAK_WINCH_MULTIPLE = 5;
    /**How many ticks in a row the line has to be held past its break force before it snaps, so a single jolt doesn't break it.**/
    private static final int BREAK_TICKS = 5;
    /**How close an attack has to pass to the line to cut it, in blocks.**/
    private static final double CUT_RADIUS = 0.3;
    /**How far a player can reach to cut a line, in blocks.**/
    public static final double CUT_REACH = 3.5;
    /**Extra reach the server allows when checking a cut, as its copies of the player and the line lag behind the client's.**/
    private static final double CUT_REACH_MARGIN = 2;
    /**How many times the vehicle's weight the winch pulls with while reeling when winchForce is left at 0.**/
    private static final double DEFAULT_WINCH_WEIGHTS = 2;
    /**Gravity in m/ticks^2, the same as {@link EntityVehicleF_Physics#getGravitationalForce()} uses.**/
    private static final double GRAVITY = 0.0245D;
    /**How often dev mode logs a line between two vehicles, in ticks.  See logTowPull.  Temporary.**/
    private static final int TOW_LOG_INTERVAL = 5;
    /**How often the server checks the anchor block still exists, in ticks.**/
    private static final int ANCHOR_CHECK_INTERVAL = 10;
    /**How long to wait after a climb path couldn't be built before trying again, in ticks.  Building one is costly.**/
    private static final int CLIMB_RETRY_INTERVAL = 10;

    /**How long reel has to be held at the top of the line before the pull over the edge starts, in ticks.**/
    private static final int MANTLE_HOLD_TICKS = 30;
    /**How fast the launcher-end wheels move along the wall while being pulled over the edge, in blocks per tick.**/
    private static final double MANTLE_SPEED = 5D / 64D;
    /**How fast they slip back while reel isn't held during the pull, in blocks per tick.**/
    private static final double MANTLE_SLIP_SPEED = 1D / 64D;
    /**How fast they move at full throttle when driving back down the wall, in blocks per tick.**/
    private static final double MANTLE_DESCEND_SPEED = 1D / 16D;
    /**Longest the hook's depth below the top of the wall plus the line's minimum length can be, in blocks, for the pull over the edge to be offered once the line is all the way in.**/
    private static final double MANTLE_REACH = 4;
    /**Most the climb pitches the vehicle up, in degrees.  Short of upright, where its angles go wrong.  See {@link ClimbPath#getPose}.**/
    private static final double MANTLE_MAX_PITCH = 89.5;
    /**How far ahead of the launcher-end wheels a wall face counts as touching them, in blocks.**/
    private static final double MANTLE_CONTACT = 0.25;
    /**How far round the wall the path looks, and how far up above the hook, in blocks.**/
    private static final double MANTLE_PATH_MARGIN = 2;
    /**How far our collision boxes are shrunk and lifted when checking they fit on top of a wall, so the top they rest on doesn't count as in the way.  Also how far above and below the climb plane is checked for air and a block to rest on.**/
    private static final double MANTLE_ROOM_MARGIN = 0.1;
    /**How far apart the points checked for room on top of a wall are, along and up from the climb plane.**/
    private static final double MANTLE_ROOM_STEP = 0.25;
    /**Longest path a climb can have, in blocks along the wall, so a bad hook can't search half the world.**/
    private static final double MANTLE_MAX_PATH = 48;
    /**Spacing of the points checked along the underside of each collision box, in blocks.**/
    private static final double MANTLE_UNDERSIDE_STEP = 1D / 8D;
    /**How far down below the wheels the path looks for the ground, in blocks.**/
    private static final int MANTLE_GROUND_SEARCH = 8;
    /**Spacing of the samples along the wall, in blocks.**/
    private static final double MANTLE_STEP = 1D / 16D;
    /**Half the size of the box used to check a point for blocks, in blocks.**/
    private static final double MANTLE_PROBE_RADIUS = 1D / 128D;
    /**Cap on how far the vehicle moves towards its place on the wall in a tick, in blocks, so starting a climb slides it there rather than snapping.**/
    private static final double MANTLE_MAX_MOVE = 0.2;
    /**Cap on how far the vehicle turns towards its place on the wall in a tick, in degrees.**/
    private static final double MANTLE_MAX_TURN = 6;
    /**Throttle below this doesn't count as driving.**/
    private static final double MANTLE_THROTTLE = 0.05;

    //Modifiable properties.
    private final ComputedVariable rangeVar;
    private final ComputedVariable hookSpeedVar;
    private final ComputedVariable reelSpeedVar;
    private final ComputedVariable winchForceVar;
    private final ComputedVariable breakForceVar;
    private final ComputedVariable minLengthVar;
    private final ComputedVariable idleLengthVar;
    private final ComputedVariable maxAimAngleHorizontalVar;
    private final ComputedVariable maxAimAngleVerticalVar;
    private final ComputedVariable aimStartPitchVar;
    private final ComputedVariable aimStartYawVar;
    private final ComputedVariable hookDamageVar;
    private final ComputedVariable isFixedVar;
    private final ComputedVariable disableBlockAttachVar;
    private final ComputedVariable disableEntityAttachVar;
    private final ComputedVariable disableVehicleAttachVar;
    private final ComputedVariable disableMantleVar;
    private final ComputedVariable disableClimbVar;
    private final ComputedVariable loseHookOnSnapVar;
    private final ComputedVariable snapCooldownVar;
    private final ComputedVariable cutToolDamageVar;

    //Inputs.
    public final ComputedVariable fireVar;
    public final ComputedVariable reelVar;
    public final ComputedVariable slackVar;

    //State.  Set by the server and synced to clients.
    public final ComputedVariable stateVar;
    private final ComputedVariable lineOutVar;
    private final ComputedVariable attachedVar;
    private final ComputedVariable ropeVar;
    /**How much of the line's pull this vehicle takes, from 0 to 1.  Decided by the server when the hook sticks.**/
    private final ComputedVariable selfShareVar;
    private final ComputedVariable anchorXVar;
    private final ComputedVariable anchorYVar;
    private final ComputedVariable anchorZVar;
    private final ComputedVariable mantleVar;
    /**How far the launcher-end wheels have gone along the climb path, in blocks.**/
    private final ComputedVariable mantleProgressVar;
    /**Progress the pull over the edge started from, and slips back to.**/
    private final ComputedVariable mantleReadyVar;
    //Where the climb path starts and which way it faces.  Clients build the same path from these.
    private final ComputedVariable mantleOriginXVar;
    private final ComputedVariable mantleOriginZVar;
    private final ComputedVariable mantleBaseVar;
    private final ComputedVariable mantleYawVar;
    /**1 if the front of the vehicle faces the wall, -1 if the back does.**/
    private final ComputedVariable mantleSideVar;
    /**How many times the line has snapped.  Clients play the snap when it changes.  See {@link #snapped}.**/
    private final ComputedVariable snapsVar;
    /**True for the tick the line snaps, for grapple_snapped.  Set on clients when they see snapsVar change.**/
    private boolean snapped;
    private double lastSnaps;
    private boolean lastSnapsValid;
    /**How many times the line has been cut.  Clients play the cut when it changes.  See {@link #cut}.**/
    private final ComputedVariable cutsVar;
    /**True for the tick the line is cut, for grapple_cut.  Set on clients when they see cutsVar change.**/
    private boolean cut;
    private double lastCuts;
    /**1 once the hook's been lost to a snap, until a player reloads it.  See {@link #reloadHook}.**/
    private final ComputedVariable missingHookVar;
    /**Ticks left before the grapple can fire again after a snap or cut.  Set by the server when the line snaps or is cut, then counted down on each side.**/
    private final ComputedVariable cooldownVar;

    //Server-only hook state that can't live in a variable.  Lost on reload, which drops flying hooks and hooks in entities.
    private boolean hookDataValid;
    private final Point3D hookPosition = new Point3D();
    private final Point3D hookVelocity = new Point3D();
    private IWrapperEntity hookedEntity;
    private AEntityF_Multipart<?> hookedMultipart;
    /**Where the hook sits on what it's stuck in.  World-aligned for entities, local for multiparts.  Clients get it for multiparts too.**/
    private final Point3D hookOffset = new Point3D();
    /**The most tension it took to hold the line on either end this tick, in kg*m/ticks^2.  Checked against the break force next tick.  Server side.**/
    private double tickHoldTension;
    /**How many ticks in a row the line has been held past its break force.**/
    private int overloadTicks;
    private int mantleHoldTicks;
    /**True once the driver's been told an overhang stops the climb, so they aren't told every tick.**/
    private boolean mantleBlockedSent;
    private String lastClimbDebug;
    //The climb path, built on each side from the mantle variables when it's first needed.
    private ClimbPath climbPath;
    private double[] climbPathKey;
    /**Tick a climb path last failed to build when starting a climb, so it isn't rebuilt every tick.  Server only.**/
    private long climbFailedTick = -CLIMB_RETRY_INTERVAL;

    /**The vehicle or placed part the hook is stuck in, on both sides, so clients can draw the hook on their own copy of it.**/
    private UUID hookedMultipartID;
    /**Client side: our copy of the multipart the hook is stuck in, looked up from hookedMultipartID when first needed.**/
    private AEntityF_Multipart<?> clientHookedMultipart;
    private final RotationMatrix hookedRenderOrientation = new RotationMatrix();

    //Client-only hook positions for the last two ticks, so the render can interpolate.
    private Point3D prevRenderAnchor;
    private Point3D renderAnchor;
    private final double[] aim = new double[5];
    private long aimTick = -1;
    //Client-only swing of the hook hanging off the launcher while the line is in.  Purely visual, so the server
    //never runs it.  The hook is stepped in world positions, but drawn from its offset from the tip, as the tip
    //moves with the vehicle after this runs.  Interpolating world positions would leave the hook a tick behind.
    private final Point3D dangleHook = new Point3D();
    private final Point3D prevDangleHook = new Point3D();
    private final Point3D dangleOffset = new Point3D();
    private final Point3D prevDangleOffset = new Point3D();
    private boolean dangleValid;
    private float aimPartialTicks = -1;
    //Launch direction for the current tick, so the turret and crosshair don't raycast every frame.
    private final Point3D launchDirection = new Point3D();
    private long launchDirectionTick = -1;
    //Where the hook would hook if fired now, worked out once a tick for the crosshair, which asks every frame.
    private final Point3D aimPoint = new Point3D();
    private long aimPointTick = -1;
    //Seats that control us: seats on us, the seat we're on, and seats linked to us.  If there are none, the vehicle's controller does.
    private final List<PartSeat> seatsControllingGrapple = new ArrayList<>();
    //Aim sent by a controller looking through a custom camera: where the camera is and which way it aims, relative to their seat.
    private UUID cameraAimControllerID;
    private final Point3D cameraAimOffset = new Point3D();
    private final RotationMatrix cameraAimOrientation = new RotationMatrix();
    //Client-only: the last aim this client sent, so it only sends when it changes.
    private UUID cameraAimSentID;
    private final Point3D cameraAimSentOffset = new Point3D();
    private final Point3D cameraAimSentAngles = new Point3D();
    //Client-only: the controller's camera aim this tick, worked out by getLocalCameraAim.
    private final Point3D localCameraOffset = new Point3D();
    private final Point3D localCameraAngles = new Point3D();
    private final Point3D localCameraPosition = new Point3D();
    private final RotationMatrix localCameraRotation = new RotationMatrix();
    //Client-only: whether the last camera checked turns with this launcher's aim, so it's only checked when the camera changes.
    private JSONCameraObject followCheckedCamera;
    private AEntityD_Definable<?> followCheckedCameraEntity;
    private boolean cameraFollowsLauncher;

    public PartGrapple(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, ItemPartGrapple item, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, item, data);
        //Saved so clients that load us while hooked know what we're hooked to.  The server drops the hook on reload anyway.
        //New parts, such as default ones on a vehicle being placed, have no data.
        if (data != null) {
            hookedMultipartID = data.getUUID(HOOKED_MULTIPART_TAG);
            if (hookedMultipartID != null) {
                hookOffset.set(data.getPoint3d(HOOK_OFFSET_TAG));
            }
        }
        addVariable(this.rangeVar = new ComputedVariable(this, "range"));
        addVariable(this.hookSpeedVar = new ComputedVariable(this, "hookSpeed"));
        addVariable(this.reelSpeedVar = new ComputedVariable(this, "reelSpeed"));
        addVariable(this.winchForceVar = new ComputedVariable(this, "winchForce"));
        addVariable(this.breakForceVar = new ComputedVariable(this, "breakForce"));
        addVariable(this.minLengthVar = new ComputedVariable(this, "minLength"));
        addVariable(this.idleLengthVar = new ComputedVariable(this, "idleLength"));
        addVariable(this.maxAimAngleHorizontalVar = new ComputedVariable(this, "maxAimAngleHorizontal"));
        addVariable(this.maxAimAngleVerticalVar = new ComputedVariable(this, "maxAimAngleVertical"));
        addVariable(this.aimStartPitchVar = new ComputedVariable(this, "aimStartPitch"));
        addVariable(this.aimStartYawVar = new ComputedVariable(this, "aimStartYaw"));
        addVariable(this.hookDamageVar = new ComputedVariable(this, "hookDamage"));
        addVariable(this.isFixedVar = new ComputedVariable(this, "isFixed"));
        addVariable(this.disableBlockAttachVar = new ComputedVariable(this, "disableBlockAttach"));
        addVariable(this.disableEntityAttachVar = new ComputedVariable(this, "disableEntityAttach"));
        addVariable(this.disableVehicleAttachVar = new ComputedVariable(this, "disableVehicleAttach"));
        addVariable(this.disableMantleVar = new ComputedVariable(this, "disableMantle"));
        addVariable(this.disableClimbVar = new ComputedVariable(this, "disableClimb"));
        addVariable(this.loseHookOnSnapVar = new ComputedVariable(this, "loseHookOnSnap"));
        addVariable(this.snapCooldownVar = new ComputedVariable(this, "snapCooldown"));
        addVariable(this.cutToolDamageVar = new ComputedVariable(this, "cutToolDamage"));

        addVariable(this.fireVar = new ComputedVariable(this, VAR_FIRE, data));
        addVariable(this.reelVar = new ComputedVariable(this, VAR_REEL));
        addVariable(this.slackVar = new ComputedVariable(this, VAR_SLACK));

        addVariable(this.stateVar = new ComputedVariable(this, VAR_STATE, data));
        addVariable(this.lineOutVar = new ComputedVariable(this, VAR_LINE_OUT, data));
        addVariable(this.attachedVar = new ComputedVariable(this, VAR_ATTACHED, data));
        addVariable(this.ropeVar = new ComputedVariable(this, VAR_ROPE, data));
        addVariable(this.selfShareVar = new ComputedVariable(this, "grapple_self_share", data));
        addVariable(this.anchorXVar = new ComputedVariable(this, "grapple_anchor_x", data));
        addVariable(this.anchorYVar = new ComputedVariable(this, "grapple_anchor_y", data));
        addVariable(this.anchorZVar = new ComputedVariable(this, "grapple_anchor_z", data));
        addVariable(this.mantleVar = new ComputedVariable(this, VAR_MANTLE, data));
        addVariable(this.mantleProgressVar = new ComputedVariable(this, "grapple_mantle_progress", data));
        addVariable(this.mantleReadyVar = new ComputedVariable(this, "grapple_mantle_ready", data));
        addVariable(this.mantleOriginXVar = new ComputedVariable(this, "grapple_mantle_origin_x", data));
        addVariable(this.mantleOriginZVar = new ComputedVariable(this, "grapple_mantle_origin_z", data));
        addVariable(this.mantleBaseVar = new ComputedVariable(this, "grapple_mantle_base", data));
        addVariable(this.mantleYawVar = new ComputedVariable(this, "grapple_mantle_yaw", data));
        addVariable(this.mantleSideVar = new ComputedVariable(this, "grapple_mantle_side", data));
        addVariable(this.snapsVar = new ComputedVariable(this, "grapple_snaps"));
        addVariable(this.cutsVar = new ComputedVariable(this, "grapple_cuts"));
        addVariable(this.missingHookVar = new ComputedVariable(this, VAR_MISSING_HOOK, data));
        addVariable(this.cooldownVar = new ComputedVariable(this, VAR_COOLDOWN, data));
    }

    @Override
    public void setVariableDefaults() {
        super.setVariableDefaults();
        rangeVar.setTo(definition.grapple.range, false);
        hookSpeedVar.setTo(definition.grapple.hookSpeed, false);
        reelSpeedVar.setTo(definition.grapple.reelSpeed, false);
        winchForceVar.setTo(definition.grapple.winchForce, false);
        breakForceVar.setTo(definition.grapple.breakForce, false);
        minLengthVar.setTo(definition.grapple.minLength, false);
        idleLengthVar.setTo(definition.grapple.idleLength, false);
        maxAimAngleHorizontalVar.setTo(definition.grapple.maxAimAngleHorizontal, false);
        maxAimAngleVerticalVar.setTo(definition.grapple.maxAimAngleVertical, false);
        aimStartPitchVar.setTo(definition.grapple.aimStartPitch, false);
        aimStartYawVar.setTo(definition.grapple.aimStartYaw, false);
        hookDamageVar.setTo(definition.grapple.hookDamage, false);
        isFixedVar.setTo(definition.grapple.isFixed ? 1 : 0, false);
        //The variant can add to these, so its item tooltip can show them.  See ItemPartGrapple.
        disableBlockAttachVar.setTo(definition.grapple.disableBlockAttach ? 1 : 0, false);
        disableEntityAttachVar.setTo(definition.grapple.disableEntityAttach ? 1 : 0, false);
        disableVehicleAttachVar.setTo(definition.grapple.disableVehicleAttach ? 1 : 0, false);
        disableMantleVar.setTo(definition.grapple.disableMantle ? 1 : 0, false);
        disableClimbVar.setTo(definition.grapple.disableClimb ? 1 : 0, false);
        loseHookOnSnapVar.setTo(definition.grapple.loseHookOnSnap ? 1 : 0, false);
        snapCooldownVar.setTo(definition.grapple.snapCooldown, false);
        cutToolDamageVar.setTo(definition.grapple.cutToolDamage, false);
    }

    /**
     * Returns true if the hook is stuck into something.
     */
    public boolean isAttached() {
        return getState() >= STATE_BLOCK;
    }

    /**
     * Returns true if the hook is out of the launcher, flying or attached.
     */
    public boolean isLineOut() {
        return lineOutVar.isActive;
    }

    /**
     * Returns this grapple's state for the vehicle's dev mode movement log: hook state, climb phase and
     * progress, line length against the launcher's distance from the hook, and the reel and slack inputs.  Temporary.
     */
    public String getDebugState() {
        return String.format("[grapple state %d mantle %d progress %.3f rope %.2f distance %.2f share %.2f reel %b slack %b winch %.0f stretch %.3f brake %.0f break %.0f overload %d | throttle %.2f brakeInput %.2f backwards %b engines%s]", getState(), getMantle(), mantleProgressVar.currentValue, ropeVar.currentValue, isAttached() ? position.distanceTo(getAnchor()) : 0, selfShareVar.currentValue, reelVar.isActive, slackVar.isActive, getWinchLimit(), reelVar.isActive ? getWinchStretch() : 0, getBrakeHold(vehicleOn), getBreakForce(), overloadTicks,
                vehicleOn.throttleVar.currentValue, vehicleOn.brakeVar.currentValue, isDrivingBackwards(), getEngineDebugState());
    }

    /**
     * Returns each engine's gear and RPM, for debug logging.  Temporary, for chasing the engine revving while driving back down from a mantle.
     */
    private String getEngineDebugState() {
        StringBuilder engines = new StringBuilder();
        for (PartEngine engine : vehicleOn.engines) {
            engines.append(String.format(" (gear %.0f rpm %.0f)", engine.currentGearVar.currentValue, engine.rpm));
        }
        return engines.toString();
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        if (hookedMultipartID != null) {
            data.setUUID(HOOKED_MULTIPART_TAG, hookedMultipartID);
            data.setPoint3d(HOOK_OFFSET_TAG, hookOffset);
        }
        return data;
    }

    @Override
    public void updatePartList() {
        super.updatePartList();
        seatsControllingGrapple.clear();
        addLinkedPartsToList(seatsControllingGrapple, PartSeat.class);
        if (entityOn instanceof PartSeat && !seatsControllingGrapple.contains(entityOn)) {
            seatsControllingGrapple.add((PartSeat) entityOn);
        }
        for (APart part : allParts) {
            if (part instanceof PartSeat && !seatsControllingGrapple.contains(part)) {
                seatsControllingGrapple.add((PartSeat) part);
            }
        }
    }

    /**
     * Returns the rider of the first of our seats that has one.  If we have no seats, returns the vehicle's
     * controller, so grapples not tied to seats work from the driver's seat.
     */
    @Override
    public IWrapperEntity getController() {
        if (seatsControllingGrapple.isEmpty()) {
            return super.getController();
        }
        for (PartSeat seat : seatsControllingGrapple) {
            if (seat.rider != null) {
                return seat.rider;
            }
        }
        return null;
    }

    /**
     * Returns true if the rider of the passed-in seat can control us.  Anyone can if we have no seats.
     */
    private boolean canBeControlledFrom(AEntityB_Existing seat) {
        return seatsControllingGrapple.isEmpty() || seatsControllingGrapple.contains(seat);
    }

    @Override
    public boolean canSeatChangeVariable(ComputedVariable variable, AEntityB_Existing seat) {
        if (isInputVariable(variable, fireVar) || isInputVariable(variable, reelVar) || isInputVariable(variable, slackVar)) {
            return canBeControlledFrom(seat);
        }
        return super.canSeatChangeVariable(variable, seat);
    }

    private static boolean isInputVariable(ComputedVariable variable, ComputedVariable input) {
        return variable == input || variable == input.invertedVariable;
    }

    /**
     * Called by the vehicle we are on, on both client and server, once per tick just before it moves.
     */
    public void updateLine() {
        //Both sides count the cooldown down themselves, so it doesn't need syncing every tick.
        //Capped at snapCooldown, so lowering it, such as by importing a JSON in dev mode, shortens one already running.
        if (cooldownVar.isActive) {
            cooldownVar.setTo(Math.max(0, Math.min(cooldownVar.currentValue, snapCooldownVar.currentValue) - 1), false);
        }
        //A hook lost before loseHookOnSnap was turned off, or before hookItem was removed, could never be reloaded, so give it back.
        if (!world.isClient() && missingHookVar.isActive && (!loseHookOnSnapVar.isActive || definition.grapple.hookItem == null)) {
            missingHookVar.setTo(0, true);
        }

        //The server owns the hook.  It flies it, decides hits and line length, and syncs them to clients.
        if (!world.isClient()) {
            updateServerHook();
        } else {
            //The first value we see is whatever the server had when we loaded, not a snap.
            snapped = lastSnapsValid && snapsVar.currentValue != lastSnaps;
            cut = lastSnapsValid && cutsVar.currentValue != lastCuts;
            lastSnaps = snapsVar.currentValue;
            lastCuts = cutsVar.currentValue;
            lastSnapsValid = true;
            updateRenderAnchor();
            updateDangle();
            sendCameraAim();
            logClientHookDrift();

            //Pull the hooked vehicle's share here too, the same as the server.  Clients only get its position from the
            //server, so without this our copy of it never moves by itself, falls blocks behind, and the line drags us back to it.
            AEntityF_Multipart<?> hooked = getHookedMultipart();
            double targetShare = 1 - selfShareVar.currentValue;
            if (hooked instanceof EntityVehicleF_Physics && targetShare > 0) {
                pullVehicle((EntityVehicleF_Physics) hooked, getLineAnchor(), ropeVar.currentValue, targetShare);
            }
        }

        //Both sides run the vehicle's side of the line so clients don't have to wait on server corrections.
        //How much of the pull the vehicle takes is decided by the server at attach time.
        //The climb moves our vehicle itself, so it only runs when we're at the front of our tow chain.
        if (isAttached() && selfShareVar.currentValue > 0) {
            boolean mantling = getMantle() != MANTLE_NONE && vehicleOn.towedByConnection == null;
            if (mantling) {
                updateMantle();
                //If the climb just finished, the line takes over this tick rather than leaving us stopped for one.
                mantling = getMantle() != MANTLE_NONE;
            }
            if (!mantling) {
                applyLineForces(getLineAnchor(), ropeVar.currentValue, selfShareVar.currentValue);
            }
        }
    }

    private void updateServerHook() {
        int state = getState();
        if (!fireVar.isActive || !isActiveVar.isActive || outOfHealth) {
            if (state != STATE_IDLE || fireVar.isActive) {
                release();
            }
            return;
        }
        if (state == STATE_IDLE) {
            if (missingHookVar.isActive || cooldownVar.isActive) {
                //Nothing to fire, or not ready to fire it yet.
                fireVar.setTo(0, true);
                return;
            }
            launch();
            //Take the first step straight away.  Otherwise clients get a tick with the hook sitting on the
            //launcher, and a zero-length cable has no direction, so it can point anywhere.
            fly();
            return;
        }
        if (state != STATE_BLOCK && !hookDataValid) {
            //Flying or stuck in an entity across a reload: we've lost the reference, so drop the line.
            release();
            return;
        }

        switch (state) {
            case STATE_FLYING: {
                fly();
                return;
            }
            case STATE_BLOCK: {
                if (vehicleOn.ticksExisted % ANCHOR_CHECK_INTERVAL == 0 && world.isAir(getAnchor())) {
                    release();
                    return;
                }
                break;
            }
            case STATE_ENTITY: {
                if (!hookedEntity.isValid()) {
                    release();
                    return;
                }
                setAnchor(hookedEntity.getPosition().copy().add(hookOffset));
                break;
            }
            case STATE_VEHICLE: {
                if (!hookedMultipart.isValid) {
                    release();
                    return;
                }
                if (hookedMultipart instanceof EntityVehicleF_Physics && isInOurTowChain((EntityVehicleF_Physics) hookedMultipart)) {
                    //Hitched to us since we hooked it.  The line would only pull us, so drop it.
                    release();
                    return;
                }
                logServerHookDrift();
                setAnchor(getHookPosition(hookedMultipart));
                break;
            }
        }

        Point3D anchor = getAnchor();
        if (!anchor.isDistanceToCloserThan(position, rangeVar.currentValue * SNAP_FACTOR)) {
            //Pulled or teleported too far apart.  The line snaps.
            snap();
            return;
        }
        if (isOverloaded()) {
            snap();
            return;
        }

        //Once it's being pulled over the edge, or driven, the mantle sets the line length itself.
        if (getMantle() < MANTLE_PULL) {
            double rope = ropeVar.currentValue;
            if (reelVar.isActive) {
                rope -= reelSpeedVar.currentValue / 20D;
            } else if (slackVar.isActive) {
                rope += reelSpeedVar.currentValue / 20D;
            }
            if (getMantle() == MANTLE_NONE) {
                double distance = position.distanceTo(anchor);
                //The line pulls no harder once it's stretched past MAX_PULL / STIFFNESS, so don't wind in any
                //more than that while whatever's on the end is stuck.  Otherwise holding reel against a wall
                //stores up line the vehicle then gets yanked by, and a climb that starts jumps up the wall to it.
                rope = Math.max(rope, distance - MAX_PULL / STIFFNESS);
                if (reelVar.isActive) {
                    //The winch only winds in what the far end keeps up with.  Past the stretch that reels at the reel speed, it
                    //stalls rather than storing up line, which would yank us in the moment reel is let go.  It never lets line out.
                    rope = Math.max(rope, Math.min(ropeVar.currentValue, distance - getWinchStretch()));
                }
                if (!slackVar.isActive && anchor.y > position.y && vehicleOn.groundDeviceCollective.groundedGroundDevices.isEmpty()) {
                    //Hanging off the hook, the winch takes in any slack, so we stay where we got to rather than
                    //dropping back down to where the line was when reel was let go.
                    rope = Math.min(rope, distance);
                }
            }
            ropeVar.setTo(clampRope(rope), true);
        }

        //Pull whatever we're stuck in.  The vehicle's share is applied on both sides in updateLine.
        double targetShare = 1 - selfShareVar.currentValue;
        if (state == STATE_ENTITY) {
            pullEntity(hookedEntity, anchor, ropeVar.currentValue, targetShare);
        } else if (state == STATE_VEHICLE && hookedMultipart instanceof EntityVehicleF_Physics && targetShare > 0) {
            pullVehicle((EntityVehicleF_Physics) hookedMultipart, anchor, ropeVar.currentValue, targetShare);
        }
    }

    /**
     * Fires the hook from the launcher at whatever is under the controller's crosshair.
     */
    private void launch() {
        hookDataValid = true;
        hookPosition.set(position);
        hookVelocity.set(getLaunchDirection()).scale(hookSpeedVar.currentValue / 20D);
        setAnchor(hookPosition);
        setState(STATE_FLYING);
    }

    /**
     * Returns where the hook would end up if fired now: the first thing in its path, or the end of the line.
     * The hook flies straight, so this is exact for anything that holds still.  Used by the aiming crosshair.
     */
    public Point3D getAimPoint() {
        if (aimPointTick != ticksExisted) {
            aimPointTick = ticksExisted;
            Point3D end = getCachedLaunchDirection().scale(rangeVar.currentValue).add(position);
            Hit hit = findHit(position, end);
            aimPoint.set(hit != null ? hit.position : end);
        }
        return aimPoint.copy();
    }

    /**
     * Like {@link #getLaunchDirection()}, but only worked out once a tick.  For rendering, which asks every frame.
     */
    private Point3D getCachedLaunchDirection() {
        if (launchDirectionTick != ticksExisted) {
            launchDirectionTick = ticksExisted;
            launchDirection.set(getLaunchDirection());
        }
        return launchDirection.copy();
    }

    /**
     * Returns the direction the hook leaves the launcher in, in world coordinates.
     */
    private Point3D getLaunchDirection() {
        double range = rangeVar.currentValue;
        Point3D target;
        IWrapperEntity controller = getController();
        if (controller != null) {
            //A custom camera shows the controller the world from somewhere other than their eyes, so sight along
            //the camera instead, or the crosshair won't be on what the middle of their screen is on.
            //Everywhere but the controller's own client, use the camera aim they sent, as nothing else knows it.
            AEntityB_Existing seat = controller.getEntityRiding();
            Point3D eye;
            Point3D sight;
            if (seat != null && getLocalCameraAim(controller, seat)) {
                eye = localCameraOffset.copy().rotate(seat.orientation).add(seat.position);
                sight = new Point3D(0, 0, range).rotate(new RotationMatrix().setToAngles(localCameraAngles)).rotate(seat.orientation);
            } else if (seat != null && controller.getID().equals(cameraAimControllerID)) {
                eye = cameraAimOffset.copy().rotate(seat.orientation).add(seat.position);
                sight = new Point3D(0, 0, range).rotate(cameraAimOrientation).rotate(seat.orientation);
            } else {
                eye = controller.getEyePosition().copy();
                sight = controller.getLineOfSight(range).copy();
            }

            //Find what the crosshair is on and aim the hook from the launcher at that point.
            //This way the hook goes where the controller is looking, even though the launcher is offset from their eyes.
            Point3D eyeEnd = eye.copy().add(sight);
            Hit aimHit = findHit(eye, eyeEnd);
            target = aimHit != null ? aimHit.position : eyeEnd;
        } else {
            target = getAimLocal(new Point3D(0, 0, range)).rotate(orientation).add(position);
        }

        Point3D direction = target.subtract(position).normalize();
        double maxHorizontal = maxAimAngleHorizontalVar.currentValue;
        double maxVertical = maxAimAngleVerticalVar.currentValue;
        if (maxHorizontal > 0 || maxVertical > 0) {
            //Keep the shot's yaw and pitch from the aim start direction inside their limits.  This only limits the shot, never where
            //the controller can look: packs that want the view to stop at the edge can clamp the camera's animations.
            RotationMatrix aimStart = getAimStartRotation();
            Point3D local = direction.copy().reOrigin(orientation);
            if (aimStart != null) {
                local = aimStart.reOrigin(local);
            }
            double yaw = getYaw(local);
            double pitch = getPitch(local);
            boolean clamped = false;
            if (maxHorizontal > 0 && Math.abs(yaw) > maxHorizontal) {
                yaw = Math.copySign(maxHorizontal, yaw);
                clamped = true;
            }
            if (maxVertical > 0 && Math.abs(pitch) > maxVertical) {
                pitch = Math.copySign(maxVertical, pitch);
                clamped = true;
            }
            if (clamped) {
                direction.set(getAimLocal(new Point3D(0, 0, 1).rotate(new RotationMatrix().setToAngles(new Point3D(pitch, yaw, 0))))).rotate(orientation);
            }
        }
        return direction;
    }

    /**
     * Turns the part-local vector by aimStartPitch and aimStartYaw, so +Z becomes the middle of the aim cone.
     */
    private Point3D getAimLocal(Point3D local) {
        RotationMatrix aimStart = getAimStartRotation();
        return aimStart != null ? local.rotate(aimStart) : local;
    }

    /**
     * Returns the rotation from the part's +Z to the aim start direction, or null if aimStartPitch and aimStartYaw are both 0.
     */
    private RotationMatrix getAimStartRotation() {
        double pitch = aimStartPitchVar.currentValue;
        double yaw = aimStartYawVar.currentValue;
        return pitch != 0 || yaw != 0 ? new RotationMatrix().setToAngles(new Point3D(pitch, yaw, 0)) : null;
    }

    /**
     * Client side: if the controller is our player and they're looking through a custom camera, sets
     * {@link #localCameraOffset} and {@link #localCameraAngles} to where the camera is and the pitch and yaw
     * it aims at, both relative to their seat, and returns true.
     */
    private boolean getLocalCameraAim(IWrapperEntity controller, AEntityB_Existing seat) {
        if (!world.isClient() || seat.activeCamera == null || !InterfaceManager.clientInterface.getClientPlayer().equals(controller)) {
            return false;
        }
        CameraSystem.getCameraTransform(seat, localCameraPosition, localCameraRotation, 1);
        localCameraOffset.set(localCameraPosition).subtract(seat.position).reOrigin(seat.orientation);
        if (seat.isRiderCameraInputActive && doesCameraFollowLauncher(seat)) {
            //The camera turns with this launcher, so aiming along it feeds the launcher's turn back into its own
            //aim and it drifts forever.  Aim with the rider's mouse input instead, the same as guns do.
            localCameraAngles.set(seat.riderCameraInputOrientation.angles.x, seat.riderCameraInputOrientation.angles.y, 0);
        } else {
            Point3D direction = new Point3D(0, 0, 1).rotate(localCameraRotation).reOrigin(seat.orientation);
            localCameraAngles.set(getPitch(direction), getYaw(direction), 0);
        }
        return true;
    }

    /**
     * Returns true if the seat's active camera is turned by this launcher's body aim.
     */
    private boolean doesCameraFollowLauncher(AEntityB_Existing seat) {
        if (seat.activeCamera != followCheckedCamera || seat.activeCameraEntity != followCheckedCameraEntity) {
            followCheckedCamera = seat.activeCamera;
            followCheckedCameraEntity = seat.activeCameraEntity;
            cameraFollowsLauncher = false;
            if (followCheckedCamera.animations != null) {
                for (JSONAnimationDefinition animation : followCheckedCamera.animations) {
                    ComputedVariable variable = followCheckedCameraEntity.getOrCreateVariable(animation.variable);
                    if (variable.entity == this && (VAR_BODY_YAW.equals(variable.variableKey) || VAR_BODY_PITCH.equals(variable.variableKey))) {
                        cameraFollowsLauncher = true;
                        break;
                    }
                }
            }
        }
        return cameraFollowsLauncher;
    }

    /**
     * Client side: if we're the controller and looking through a custom camera, sends where it is and where it
     * aims to the server when they change, so the hook goes where we're aiming rather than where our eyes are.
     * Tells the server once we stop, so it goes back to the way we face.
     */
    private void sendCameraAim() {
        IWrapperEntity controller = getController();
        AEntityB_Existing seat = controller != null ? controller.getEntityRiding() : null;
        if (seat != null && getLocalCameraAim(controller, seat)) {
            if (!controller.getID().equals(cameraAimSentID) || Math.abs(localCameraAngles.x - cameraAimSentAngles.x) > 1.0E-3 || Math.abs(localCameraAngles.y - cameraAimSentAngles.y) > 1.0E-3 || !localCameraOffset.isDistanceToCloserThan(cameraAimSentOffset, 1.0E-3)) {
                cameraAimSentID = controller.getID();
                cameraAimSentAngles.set(localCameraAngles);
                cameraAimSentOffset.set(localCameraOffset);
                InterfaceManager.packetInterface.sendToServer(new PacketPartGrapple(this, InterfaceManager.clientInterface.getClientPlayer(), cameraAimSentOffset, cameraAimSentAngles));
            }
        } else if (cameraAimSentID != null) {
            InterfaceManager.packetInterface.sendToServer(new PacketPartGrapple(this, InterfaceManager.clientInterface.getClientPlayer(), null, null));
            cameraAimSentID = null;
        }
    }

    /**
     * Sets the aim sent by a controller looking through a custom camera, as the camera's offset from their seat and
     * the pitch and yaw it aims at relative to their seat, or clears it if the angles are null.  Returns true if
     * it was taken, so it's passed on to clients.
     */
    public boolean setCameraAim(UUID controllerID, Point3D offset, Point3D angles) {
        if (controllerID == null) {
            return false;
        }
        if (angles == null || offset == null) {
            if (!controllerID.equals(cameraAimControllerID)) {
                return false;
            }
            cameraAimControllerID = null;
            return true;
        }
        if (!Double.isFinite(angles.x) || !Double.isFinite(angles.y) || !Double.isFinite(offset.x) || !Double.isFinite(offset.y) || !Double.isFinite(offset.z) || offset.length() > MAX_CAMERA_AIM_OFFSET) {
            return false;
        }
        IWrapperEntity controller = getController();
        if (controller == null || !controllerID.equals(controller.getID()) || controller.getEntityRiding() == null) {
            return false;
        }
        cameraAimControllerID = controllerID;
        cameraAimOffset.set(offset);
        cameraAimOrientation.setToAngles(new Point3D(Math.max(-90, Math.min(90, angles.x)), angles.y, 0));
        return true;
    }

    /**
     * Moves the hook one tick along its flight, sticking it into the first thing it hits.
     */
    private void fly() {
        Point3D end = hookPosition.copy().add(hookVelocity);
        Hit hit = findHit(hookPosition, end);
        if (hit != null) {
            if (canAttach(hit)) {
                attach(hit);
            } else {
                //Can't bite into this.  The hook bounces off and is reeled back in.
                release();
            }
            return;
        }

        hookPosition.set(end);
        if (!hookPosition.isDistanceToCloserThan(position, rangeVar.currentValue) || !world.chunkLoaded(hookPosition)) {
            //Out of line, or flying into unloaded chunks.  Reel it back.
            release();
            return;
        }
        setAnchor(hookPosition);
    }

    private boolean canAttach(Hit hit) {
        if (hit.entity != null) {
            return !disableEntityAttachVar.isActive;
        } else if (hit.multipart instanceof EntityVehicleF_Physics) {
            //Not our own tow chain, as pulling it would only pull us.
            return !disableVehicleAttachVar.isActive && !isInOurTowChain((EntityVehicleF_Physics) hit.multipart);
        } else {
            //Blocks and placed parts.
            return !disableBlockAttachVar.isActive;
        }
    }

    private void attach(Hit hit) {
        hookPosition.set(hit.position);
        double selfShare;
        int state;
        if (hit.entity != null) {
            //Entities with a mass in the config share the pull by mass, the same as a vehicle.  The rest are light next
            //to a vehicle, so the vehicle doesn't feel them, and we just reel them in.
            hookedEntity = hit.entity;
            hookOffset.set(hit.position).subtract(hit.entity.getPosition());
            state = STATE_ENTITY;
            double entityMass = getEntityMass(hit.entity);
            double ourMass = getTowChainLead(vehicleOn).currentMass;
            selfShare = entityMass > 0 ? entityMass / (entityMass + ourMass) : 0;
        } else if (hit.multipart != null) {
            //Store the hit in the other entity's local coordinates, so the hook stays put as it turns.
            hookedMultipart = hit.multipart;
            hookOffset.set(hit.position).subtract(hit.multipart.position).reOrigin(hit.multipart.orientation);
            hookedMultipartID = hit.multipart.uniqueUUID;
            InterfaceManager.packetInterface.sendToAllClients(new PacketPartGrappleHook(this, hookedMultipartID, hookOffset));
            state = STATE_VEHICLE;
            if (hit.multipart instanceof EntityVehicleF_Physics) {
                //Share the pull by mass: the lighter vehicle moves more.  A tow chain moves as one, so
                //weigh all of it.  The front vehicle's mass includes everything it tows.
                double otherMass = getTowChainLead((EntityVehicleF_Physics) hit.multipart).currentMass;
                double ourMass = getTowChainLead(vehicleOn).currentMass;
                //Packs can leave emptyMass out, so both could be massless.  Split evenly then.
                selfShare = otherMass + ourMass > 0 ? otherMass / (otherMass + ourMass) : 0.5;
            } else {
                //Placed parts don't move, so treat them like a block.
                selfShare = 1;
            }
        } else {
            state = STATE_BLOCK;
            selfShare = 1;
        }

        if (hookDamageVar.currentValue > 0 && state != STATE_BLOCK) {
            IWrapperEntity controller = getController();
            LanguageSystem.LanguageEntry language = controller != null ? LanguageSystem.DEATH_GRAPPLE_PLAYER : LanguageSystem.DEATH_GRAPPLE_NULL;
            if (hit.entity != null) {
                hit.entity.attack(new Damage(hookDamageVar.currentValue, boundingBox, this, controller, language));
            } else {
                hit.multipart.attack(new Damage(hookDamageVar.currentValue, hit.box, this, controller, language));
            }
        }

        setAnchor(hit.position);
        selfShareVar.setTo(selfShare, true);
        ropeVar.setTo(clampRope(position.distanceTo(hit.position)), true);
        setState(state);
    }

    /**
     * Releases the line because it broke, and tells clients so they can play the snap.  Server only.
     * Sent as a count rather than a one-tick flag, so clients can't miss it if the flag's on and off packets land in the same tick.
     */
    private void snap() {
        snapsVar.setTo(snapsVar.currentValue + 1, true);
        breakLine();
    }

    /**
     * Breaks the line, whether it snapped or was cut: the hook is lost if loseHookOnSnap is set, the cooldown
     * starts, and the line is released.  Snaps and cuts only differ in the count they bump for sounds and animations.  Server only.
     */
    private void breakLine() {
        //Without a hook item to reload with, a lost hook could never come back, so keep it.
        if (loseHookOnSnapVar.isActive && definition.grapple.hookItem != null) {
            missingHookVar.setTo(1, true);
        }
        if (snapCooldownVar.currentValue > 0) {
            cooldownVar.setTo(snapCooldownVar.currentValue, true);
        }
        release();
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        if (isActiveVar.isActive && reloadHook(player.getHeldStack()) && !player.isCreative()) {
            player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
        }
        return true;
    }

    /**
     * Reloads a hook lost to a snap or cut if the stack is our hookItem, and returns true if it did.  Server only.
     */
    private boolean reloadHook(IWrapperItemStack stack) {
        if (!missingHookVar.isActive || definition.grapple.hookItem == null) {
            return false;
        }
        AItemBase item = stack.getItem();
        if (item instanceof AItemPack) {
            String[] splitName = definition.grapple.hookItem.split(":");
            AJSONItem itemDefinition = ((AItemPack<?>) item).definition;
            //The bare systemName takes any variant, while systemName plus a subName takes only that variant.
            if (splitName.length == 2 && itemDefinition.packID.equals(splitName[0]) && (itemDefinition.systemName.equals(splitName[1]) || (item instanceof AItemSubTyped && (itemDefinition.systemName + ((AItemSubTyped<?>) item).subDefinition.subName).equals(splitName[1])))) {
                missingHookVar.setTo(0, true);
                return true;
            }
        }
        return false;
    }

    /**
     * Releases the line because a player cut it, and tells clients so they can play the cut.  Server only.
     * Counted separately from snaps, so packs can give it its own sound.  See {@link #snap()}.
     */
    private void cut() {
        cutsVar.setTo(cutsVar.currentValue + 1, true);
        breakLine();
    }

    private void release() {
        hookDataValid = false;
        tickHoldTension = 0;
        overloadTicks = 0;
        hookedEntity = null;
        hookedMultipart = null;
        if (hookedMultipartID != null) {
            hookedMultipartID = null;
            InterfaceManager.packetInterface.sendToAllClients(new PacketPartGrappleHook(this, null, null));
        }
        fireVar.setTo(0, true);
        //The line's all wound back in.
        ropeVar.setTo(0, true);
        setMantle(MANTLE_NONE);
        setState(STATE_IDLE);
    }

    /**
     * Returns the closest block, vehicle or entity on the segment, ignoring our own vehicle and anyone riding it.
     */
    private Hit findHit(Point3D start, Point3D end) {
        Hit closest = null;
        Point3D delta = end.copy().subtract(start);

        BlockHitResult blockHit = world.getBlockHit(start, delta);
        if (blockHit != null) {
            //Store the hit a hair inside the block, so the air check finds the block we hit and not the space in front of it.
            closest = new Hit(blockHit.hitPosition.copy().addScaled(delta.copy().normalize(), 0.01));
        }

        //Hook the same boxes bullets can hit, as well as the ones entities stand on.  Only the collision boxes packs
        //define though, not parts' own bounding boxes, or we'd hook hidden parts with no hitbox of their own.
        EntityInteractResult multipartHit = world.getMultipartEntityIntersect(start, end, vehicleOn, true, CollisionType.ENTITY, CollisionType.ATTACK, CollisionType.BULLET);
        if (multipartHit != null && (closest == null || start.isFirstCloserThanSecond(multipartHit.position, closest.position))) {
            closest = new Hit(multipartHit.position.copy());
            closest.multipart = multipartHit.entity instanceof APart ? ((APart) multipartHit.entity).masterEntity : (AEntityF_Multipart<?>) multipartHit.entity;
            closest.box = multipartHit.box;
        }

        for (IWrapperEntity entity : world.getEntitiesWithin(new BoundingBox(start, end))) {
            if (isRidingOurVehicle(entity)) {
                continue;
            }
            BoundingBoxHitResult entityHit = entity.getBounds().getIntersection(start, end);
            if (entityHit != null && (closest == null || start.isFirstCloserThanSecond(entityHit.position, closest.position))) {
                closest = new Hit(entityHit.position.copy());
                closest.entity = entity;
            }
        }
        return closest;
    }

    private boolean isRidingOurVehicle(IWrapperEntity entity) {
        AEntityB_Existing riding = entity.getEntityRiding();
        return riding == vehicleOn || (riding instanceof APart && ((APart) riding).masterEntity == vehicleOn);
    }

    /**
     * Reels an entity towards the launcher.  Server only.
     * 
     * An entity with no mass in the config is light next to a vehicle: it's stopped moving away from the launcher
     * and pulled back towards the line length, and the vehicle doesn't feel it.  One with a mass takes its share of
     * the line the same as a vehicle does, see {@link #getChainPull}: it only takes its share of stopping the two ends
     * pulling apart and of the pull back, and the vehicle takes the rest in {@link #applyLineForces}.
     * <p>
     * Other lines hooked into the entity share stopping it the same as on a vehicle, see {@link #getLinesHolding}.  Each line
     * pushes the entity as soon as it works out its pull, so they all work from its velocity at the start of the tick.
     * Otherwise the first line would stop it, take all the tension, and leave the others nothing to hold.
     *
     * @param share how much of the line's pull the entity takes, from 0 to 1.  1 for an entity with no mass.
     */
    private void pullEntity(IWrapperEntity entity, Point3D anchor, double rope, double share) {
        Point3D toLauncher = position.copy().subtract(anchor);
        double distance = toLauncher.length();
        if (distance <= rope || distance < 1.0E-3 || entity.getEntityRiding() != null) {
            return;
        }
        Point3D lineNormal = toLauncher.scale(1D / distance);
        Point3D motionAdded = new Point3D();
        LineShare lines = getLinesHolding(entity, lineNormal);
        double radialSpeed = getLineCensus(world).getVelocity(entity).dotProduct(lineNormal, false);
        if (share >= 1) {
            if (radialSpeed < 0) {
                motionAdded.addScaled(lineNormal, -radialSpeed / lines.stop);
            }
            motionAdded.addScaled(lineNormal, Math.min((distance - rope) * STIFFNESS, MAX_PULL) / lines.spring);
        } else {
            //Our motion gets scaled by speedFactor when applied, but the entity's velocity is how far it really moves.
            EntityVehicleF_Physics lead = getTowChainLead(vehicleOn);
            double closingSpeed = radialSpeed - lead.motion.dotProduct(lineNormal, false) * lead.speedFactor;
            double stopSpeed = Math.max(0, -closingSpeed) * share / lines.stop;
            double springSpeed = Math.max(0, Math.min((distance - rope) * STIFFNESS, MAX_PULL) * share - Math.max(0, closingSpeed) * share) / lines.spring;
            motionAdded.addScaled(lineNormal, stopSpeed + springSpeed);
            tickHoldTension = Math.max(tickHoldTension, stopSpeed * getEntityMass(entity));
        }
        //This marks the motion as changed, which players need as their movement is client-side.
        entity.applyMotion(motionAdded);
    }

    /**
     * Returns the entity's mass from the grappleEntityMasses config, in kg, or 0 if it isn't listed.
     */
    private static double getEntityMass(IWrapperEntity entity) {
        String name = entity.getRegistryName();
        Double mass = name != null ? ConfigSystem.settings.general.grappleEntityMasses.value.get(name) : null;
        return mass != null && mass > 0 ? mass : 0;
    }

    /**
     * Pulls another vehicle towards the launcher with its share of the line force.  Server only;
     * that vehicle syncs its own movement to clients.
     * 
     * A vehicle in a tow chain is pulled with the whole chain.  The vehicle at the front takes the
     * motion, as everything it tows follows it.  The line also turns the vehicles so the hook swings
     * round towards us.  A loose hitch is a joint, so the vehicle behind it swings about its hookup and
     * the pull carries on to the vehicle in front from the hitch.  A mounted or restricted hitch is
     * rigid, so the pull carries on from the hook itself.
     * 
     * The chain's tyres and brakes hold back what they can of the pull, so a braked or heavy chain needs the
     * line to stretch harder before it moves.  See {@link #getChainPull}.  The pulls are queued and applied in
     * each vehicle's own update, after its own tyres and brakes, so they aren't counted twice whichever order
     * the vehicles update in.  See {@link #queueChainPull} and {@link #applyPull(EntityVehicleF_Physics, LinePull)}.
     */
    private void pullVehicle(EntityVehicleF_Physics other, Point3D anchor, double rope, double share) {
        Point3D toLauncher = position.copy().subtract(anchor);
        double distance = toLauncher.length();
        if (distance <= rope || distance < 1.0E-3) {
            return;
        }
        Point3D towards = toLauncher.scale(1D / distance);
        Point3D launcherMotion = getTowChainLead(vehicleOn).motion;
        LineShare lines = getLinesHolding(getTowChainLead(other), towards);
        ChainPull pull = getChainPull(other, towards, distance - rope, share, lines, launcherMotion, getWinchLimit());
        if (!world.isClient()) {
            tickHoldTension = Math.max(tickHoldTension, pull.holdTension);
        }
        logTowPull("towed", other, towards, distance - rope, share, lines, launcherMotion, pull);
        queueChainPull(other, anchor, pull.force, pull.motion, MAX_TOWED_PULL_TURN / lines.count, lines.swing, launcherMotion.copy());
    }

    /**
     * In dev mode, logs how far the hook on a vehicle is off where it should be, every {@link #HOOK_DRIFT_LOG_INTERVAL}
     * ticks on the server.  Called before the anchor is moved this tick, so the stored anchor is from last tick.
     * Temporary, for chasing the hook drawing off the hooked vehicle.
     * 
     * offset: where the hook should be, in the hooked vehicle's own frame.  stale: how far the vehicle has carried the hook
     * since last tick's anchor, in blocks.  If the vehicle updates after us in a tick, the hook we sync is this far behind its body.  Pair with the client's GrappleHookClient lines.
     */
    private void logServerHookDrift() {
        if (!ConfigSystem.settings.general.devMode.value || vehicleOn.ticksExisted % HOOK_DRIFT_LOG_INTERVAL != 0) {
            return;
        }
        Point3D shouldBe = getHookPosition(hookedMultipart);
        Point3D motion = hookedMultipart instanceof AEntityVehicleD_Moving ? ((AEntityVehicleD_Moving) hookedMultipart).motion : new Point3D();
        InterfaceManager.coreInterface.logError(String.format("GrappleHookServer tick %d on %s: offset (%.3f %.3f %.3f) stale %.3f | vehicle pos (%.2f %.2f %.2f) yaw %.2f motion (%.3f %.3f %.3f)",
                vehicleOn.ticksExisted, hookedMultipart.definition.systemName, hookOffset.x, hookOffset.y, hookOffset.z, getAnchor().distanceTo(shouldBe),
                hookedMultipart.position.x, hookedMultipart.position.y, hookedMultipart.position.z, hookedMultipart.orientation.angles.y, motion.x, motion.y, motion.z));
    }

    /**
     * Client side, in dev mode: logs where the server's synced hook sits on our copy of the hooked vehicle, every
     * {@link #HOOK_DRIFT_LOG_INTERVAL} ticks.  Takes the hooked vehicle the server told us about, or the nearest
     * vehicle not in our tow chain if we weren't told.  Temporary, for chasing the hook drawing off the hooked vehicle.
     * 
     * offset: the server's hook in that vehicle's own frame.  It should match the server's offset if the server's hook
     * is on our copy's body, so the difference is how far off it would be drawn without the client working it out.
     * drawnOff: how far the server's hook is from where we now draw it.  known: whether we were told which vehicle it is.
     * backlog: how far the server has moved that vehicle that our copy hasn't caught up to yet.
     */
    private void logClientHookDrift() {
        if (!ConfigSystem.settings.general.devMode.value || getState() != STATE_VEHICLE || vehicleOn.ticksExisted % HOOK_DRIFT_LOG_INTERVAL != 0) {
            return;
        }
        Point3D anchor = getAnchor();
        AEntityF_Multipart<?> known = getHookedMultipart();
        EntityVehicleF_Physics nearest = known instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) known : null;
        double nearestDistance = nearest != null ? -1 : Double.MAX_VALUE;
        for (EntityVehicleF_Physics vehicle : world.getEntitiesOfType(EntityVehicleF_Physics.class)) {
            double distance = vehicle.position.distanceTo(anchor);
            if (distance < nearestDistance && !isInOurTowChain(vehicle)) {
                nearest = vehicle;
                nearestDistance = distance;
            }
        }
        if (nearest == null) {
            return;
        }
        Point3D offset = toLocal(nearest, anchor);
        Point3D backlog = nearest.getServerCorrectionBacklog();
        InterfaceManager.coreInterface.logError(String.format("GrappleHookClient tick %d on %s known %b: offset (%.3f %.3f %.3f) drawnOff %.3f | backlog %.3f (%.3f %.3f %.3f) | vehicle pos (%.2f %.2f %.2f) yaw %.2f velocity %.3f",
                vehicleOn.ticksExisted, nearest.definition.systemName, known != null, offset.x, offset.y, offset.z, anchor.distanceTo(getLineAnchor()), backlog.length(), backlog.x, backlog.y, backlog.z,
                nearest.position.x, nearest.position.y, nearest.position.z, nearest.orientation.angles.y, nearest.velocity));
    }

    /**
     * Returns the most tension the winch can reel the line in with this tick, in kg*m/ticks^2.  The line still holds
     * past that, but the winch stalls.  Unlimited while not reeling, so the line's own pull back to its length isn't cut.
     */
    private double getWinchLimit() {
        return reelVar.isActive ? getWinchForce() : Double.POSITIVE_INFINITY;
    }

    /**
     * Returns how hard the winch can pull, in kg*m/ticks^2: winchForce, or twice the weight of the vehicle we're on if that's 0.
     */
    private double getWinchForce() {
        //Go by mass rather than getGravitationalForce, which is 0 for floating blimps.  They still need a winch that can pull.
        return winchForceVar.currentValue > 0 ? winchForceVar.currentValue * KN_TO_TICK_FORCE : DEFAULT_WINCH_WEIGHTS * vehicleOn.currentMass * GRAVITY;
    }

    /**
     * Returns the tension the line snaps at, in kg*m/ticks^2: breakForce, or {@link #DEFAULT_BREAK_WINCH_MULTIPLE} times the winch force if that's 0.
     */
    private double getBreakForce() {
        return breakForceVar.currentValue > 0 ? breakForceVar.currentValue * KN_TO_TICK_FORCE : DEFAULT_BREAK_WINCH_MULTIPLE * getWinchForce();
    }

    /**
     * Returns true if the line has been held past its break force for {@link #BREAK_TICKS} ticks in a row, so it snaps.
     * Goes by the tension it took to hold the line last tick, then starts this tick's from nothing.  Server side.
     */
    private boolean isOverloaded() {
        overloadTicks = tickHoldTension > getBreakForce() ? overloadTicks + 1 : 0;
        tickHoldTension = 0;
        return overloadTicks >= BREAK_TICKS;
    }

    /**
     * Returns the most the winch lets the line stretch past its length while reeling, in blocks.  The spring closes the
     * two ends up at STIFFNESS times the stretch each tick, so this is the stretch that closes them at the reel speed.
     * The winch force caps how hard the spring speeds them up to that.  Any more stretch would only be stored up while
     * something holds against the winch, and yank it in the moment reel is let go.  See {@link #getChainPull}.
     */
    private double getWinchStretch() {
        return Math.min(MAX_PULL / STIFFNESS, reelSpeedVar.currentValue / 20D / STIFFNESS);
    }

    /**
     * In dev mode, logs how a line between two vehicles pulls on one end, every {@link #TOW_LOG_INTERVAL} ticks on the server.
     * Both ends log on the same tick, so the "towing" and "towed" lines pair up.  Temporary, for chasing weak vehicle tows.
     * 
     * radial: the chain's speed towards the other end before the line, in blocks per tick.  closing: how fast the
     * two ends close up, so negative is pulling apart.  stop: what the rigid line puts on it to stop the ends pulling apart.  spring: what the stretch pulls it in by.  along: what it actually gets towards
     * the other end, after tyres and brakes.  impulse: along times mass.  The two ends' impulses match if the line
     * passes all its momentum between them; a towing impulse bigger than the towed one is momentum the line loses.
     * thrust: what the engines added along the heading this tick.  brake: total brake hold on the grounded wheels.
     */
    private void logTowPull(String end, EntityVehicleF_Physics hooked, Point3D towards, double stretch, double share, LineShare lines, Point3D otherMotion, ChainPull pull) {
        if (!ConfigSystem.settings.general.devMode.value || world.isClient() || vehicleOn.ticksExisted % TOW_LOG_INTERVAL != 0) {
            return;
        }
        EntityVehicleF_Physics lead = getTowChainLead(hooked);
        double radial = lead.motion.dotProduct(towards, false);
        double closing = radial - otherMotion.dotProduct(towards, false);
        double stop = Math.max(0, -closing) * share / lines.stop;
        double spring = Math.max(0, Math.min(stretch * STIFFNESS, MAX_PULL) * share / lead.speedFactor - Math.max(0, closing) * share) / lines.spring;
        double along = pull.motion.dotProduct(towards, false);
        double brake = getBrakeHold(lead);
        InterfaceManager.coreInterface.logError(String.format("GrappleTow tick %d %s %s: mass %.0f share %.2f lines stop %.2f spring %.2f swing %.2f count %d stretch %.3f | radial %.4f closing %.4f stop %.4f spring %.4f along %.4f held %.4f | impulse %.1f tension %.1f winch %.1f | speed %.4f thrust %.5f brake %.1f grounded %d",
                vehicleOn.ticksExisted, end, lead.definition.systemName, lead.currentMass, share, lines.stop, lines.spring, lines.swing, lines.count, stretch,
                radial, closing, stop, spring, along, stop + spring - along,
                along * lead.currentMass, pull.force.length(), getWinchLimit(),
                lead.velocity, lead.getThrustSpeedGain(), brake, lead.groundDeviceCollective.groundedGroundDevices.size()));
    }

    /**
     * Works out how much a line pulls on a tow chain.  The same on both ends of the line.
     * 
     * The line stops the chain moving further away, and pulls it back towards the line length.  The
     * tension is the force required with the whole chain's mass.  The chain's tyres and brakes resist
     * what they can of the pull back, and the rest moves it.  If the line is lifting the vehicle it's on, that vehicle presses down
     * less, so its tyres grip less. 
     * 
     * The rigid part goes by how fast the two ends are pulling apart, not just how fast the chain is, and each end
     * takes its share of stopping that.  So when the other end drives off, the chain is dragged after it with the
     * speed the other end loses, and the line passes momentum between them rather than losing it.
     * 
     * Tyres and brakes don't resist the part of that which stops the chain itself moving away, as friction is
     * already slowing it down, so it helps the line instead.  They do resist the part that drags the chain after the other end.
     *
     * The rigid part always holds, as the winch's drum doesn't let line out however hard it's pulled.  Reeling in is
     * the spring part, and while reeling the winch can only pull so hard.  The spring only gets what the tension has
     * left under that once the line is held, so a chain driving away hard stalls the winch rather than being reeled in.
     *
     * When more than one line holds the chain, they share stopping it and pulling it back by how much they pull the
     * same way, so each only takes its part.  Every line works its pull out from the same motion before any are
     * applied, so without this each would take all of it: the chain would be stopped once per line and pulled back
     * once per line, flinging it past the hooks, and a load that one line can't hold would snap every line on it rather
     * than being spread between them.  Each winch still only reels in with its own force.  The chain's tyres and
     * brakes hold against all the lines together, so each line works out what they hold of the lines' total pull
     * and only takes its part, rather than every line getting their full grip.  See {@link #getLinesHolding}.
     *
     * @param hooked the vehicle in the chain the line is on.
     * @param towards which way the line pulls the chain.  Must be normalized.
     * @param stretch how far past its length the line is pulled, in blocks.
     * @param share how much of the line's pull the chain takes, from 0 to 1.
     * @param lines how many lines' worth hold the chain this way, counting this one.  See {@link #getLinesHolding}.
     * @param otherMotion the motion of the vehicle at the front of the chain on the other end, or null if that end doesn't move.
     * @param maxTension the most tension the winch can reel in with, in kg*m/ticks^2.  See {@link #getWinchLimit()}.
     */
    private static ChainPull getChainPull(EntityVehicleF_Physics hooked, Point3D towards, double stretch, double share, LineShare lines, Point3D otherMotion, double maxTension) {
        EntityVehicleF_Physics lead = getTowChainLead(hooked);
        if (!(lead.currentMass > 0)) {
            //Massless, as packs can leave emptyMass out.  There's nothing to share the pull with, so don't move it.
            return new ChainPull(new Point3D(), new Point3D(), 0);
        }
        //Rigid line: stop the ends pulling further apart.  The part that just stops us moving away is
        //stopping, and the rest drags us after the other end, which our tyres and brakes hold against.
        double radialSpeed = lead.motion.dotProduct(towards, false);
        double closingSpeed = otherMotion != null ? radialSpeed - otherMotion.dotProduct(towards, false) : radialSpeed;
        double stopSpeed = Math.max(0, -closingSpeed) * share / lines.stop;
        double ownStopSpeed = Math.min(stopSpeed, Math.max(0, -radialSpeed));
        Point3D stopping = towards.copy().scale(ownStopSpeed);
        Point3D dragging = towards.copy().scale(stopSpeed - ownStopSpeed);
        //Pull back towards the line length.  Motion gets scaled by speedFactor when applied, so undo that here.
        //Only make up what our share of the closing speed is short of.  Otherwise each tick's pull adds to the last,
        //and the line flings us past the hook.  Free-rolling wheels barely slow us, so this goes for the ground too.
        double springSpeed = Math.min(stretch * STIFFNESS, MAX_PULL) * share / lead.speedFactor;
        springSpeed = Math.max(0, springSpeed - Math.max(0, closingSpeed) * share) / lines.spring;
        Point3D spring = towards.copy().scale(springSpeed);

        //All three are along the line, so the tensions add.  Holding takes what it needs, and the winch reels in with what's left.
        double holdTension = (stopping.length() + dragging.length()) * lead.currentMass;
        double springTension = spring.length() * lead.currentMass;
        double reelTension = Math.max(0, maxTension - holdTension);
        if (springTension > reelTension) {
            spring.scale(reelTension / springTension);
        }
        Point3D force = stopping.copy().add(dragging).add(spring).scale(lead.currentMass);
        //The tyres and brakes hold against all the lines this way at once.  Every line's part is along its own line,
        //so scale ours up to the lines' total, take off what they hold of that, and keep our part of what's left.
        double ownMoving = (dragging.length() + spring.length()) * lead.currentMass;
        double totalMoving = (dragging.length() * lines.stop + spring.length() * lines.spring) * lead.currentMass;
        Point3D moving = dragging.add(spring).scale(lead.currentMass);
        if (ownMoving > 0) {
            double linesScale = totalMoving / ownMoving;
            moving.scale(linesScale);
            double lift = Math.max(0, force.y) * linesScale;
            for (EntityVehicleF_Physics chainVehicle : getTowChain(lead)) {
                moving.subtract(getTyreHold(chainVehicle, moving, chainVehicle == hooked ? lift : 0));
            }
            moving.scale(1D / linesScale);
        }
        return new ChainPull(stopping.add(moving.scale(1D / lead.currentMass)), force, holdTension);
    }

    /**
     * Queues a line's pull on a tow chain, for each vehicle to apply in its own update.  The vehicle at
     * the front takes the motion, as everything it tows follows it.  A loose hitch is a joint, so the
     * vehicle behind it swings about its hookup and the pull carries on to the vehicle in front from the
     * hitch.  A mounted or restricted hitch is rigid, so the pull carries on from the same point.
     *
     * @param hooked the vehicle in the chain the line is on.
     * @param point where the line is on it, in world coordinates.
     * @param maxTurn the most the line can turn each vehicle in the chain, in degrees per tick.
     * @param swingShare how many lines' worth swing the chain the way this one does.  See {@link LineShare#swing}.
     * @param otherMotion the motion of the front of the chain on the other end of the line, for the vehicle at the front of
     * this chain to swing after, or null for it to swing after its own motion.  See {@link #swingFromLine}.
     */
    private static void queueChainPull(EntityVehicleF_Physics hooked, Point3D point, Point3D force, Point3D motion, double maxTurn, double swingShare, Point3D otherMotion) {
        EntityVehicleF_Physics lead = getTowChainLead(hooked);
        Point3D pullPoint = point.copy();
        EntityVehicleF_Physics vehicle = hooked;
        while (vehicle != lead) {
            TowingConnection connection = vehicle.towedByConnection;
            if (!connection.hitchConnection.mounted && !connection.hitchConnection.restricted) {
                vehicle.grapplePulls.add(new LinePull(pullPoint, force, connection.hookupCurrentPosition.copy(), null, maxTurn, swingShare, null));
                pullPoint = connection.hitchCurrentPosition.copy();
            }
            vehicle = connection.towingVehicle;
        }
        lead.grapplePulls.add(new LinePull(pullPoint, force, null, motion, maxTurn, swingShare, otherMotion));
    }

    /**The taut lines in each world, worked out once a tick.  See {@link #getLineCensus}.**/
    private static final Map<AWrapperWorld, LineCensus> lineCensuses = new WeakHashMap<>();

    /**
     * Returns how many lines' worth hold the passed-in body the way this line pulls it, counting this one as 1.  Each
     * other taut line on it counts by how much it pulls the same way: the dot product of the two directions.  So a
     * line alongside ours counts fully and they share holding it, one at right angles counts for nothing as it holds
     * a different way, and one pulling the other way doesn't help hold it at all.  Dividing each line's pull by this
     * stops the body once between them, wherever the lines point: two lines at 45 degrees either side of where it's
     * going each stop their own part of its motion, and those add back up to all of it.  Lines only count towards
     * sharing what they're doing this tick, so one whose far end is already closing in doesn't take a share of stopping
     * the body.  The lines can be on any vehicle: ours, the body's own, or a third one hooked into it.  See {@link #getChainPull}.
     *
     * @param body the front of the tow chain the line holds, or the entity it's hooked into.
     * @param towards which way this line pulls the body.  Must be normalized.
     */
    private LineShare getLinesHolding(Object body, Point3D towards) {
        double stop = 1;
        double spring = 1;
        double swing = 1;
        int count = 1;
        List<LineHold> holds = getLineCensus(world).holds.get(body);
        if (holds != null) {
            for (LineHold hold : holds) {
                if (hold.grapple != this) {
                    double along = Math.max(0, hold.pull.dotProduct(towards, false));
                    swing += along;
                    ++count;
                    if (hold.stopping) {
                        stop += along;
                    }
                    if (hold.springing) {
                        spring += along;
                    }
                }
            }
        }
        return new LineShare(stop, spring, swing, count);
    }

    /**
     * Returns the taut lines in the passed-in world, grouped by the tow chain or entity they hold.  Every vehicle is only
     * looked through on the first call each tick; later calls that tick reuse it.  The client and server have
     * their own worlds, so they each get their own.
     */
    private static LineCensus getLineCensus(AWrapperWorld world) {
        long tick = world.getTickCount();
        LineCensus census = lineCensuses.get(world);
        if (census == null || census.tick != tick) {
            census = new LineCensus(tick);
            for (AEntityVehicleD_Moving vehicle : world.getEntitiesExtendingType(AEntityVehicleD_Moving.class)) {
                for (PartGrapple grapple : vehicle.grapples) {
                    grapple.addHolds(census);
                }
            }
            lineCensuses.put(world, census);
        }
        return census;
    }

    /**
     * Adds which way this grapple's line pulls each tow chain or entity it holds to the census.  Nothing if the line is slack.
     * Our end is held the same as in applyLineForces, and the hooked end the same as in pullVehicle or pullEntity.
     */
    private void addHolds(LineCensus census) {
        if (!isAttached()) {
            return;
        }
        Point3D toAnchor = getLineAnchor().copy().subtract(position);
        double distance = toAnchor.length();
        if (distance <= ropeVar.currentValue || distance < 1.0E-3) {
            return;
        }
        toAnchor.scale(1D / distance);

        //Whether the line has to stop the ends pulling apart, or pull them back together, this tick.  It's the same for
        //both ends, as it goes by how fast they close up.  Motion gets scaled by speedFactor when applied, so this uses
        //how far each end really moves.  Clients don't know which entity the hook is in, so they take it as still.
        EntityVehicleF_Physics ourLead = getTowChainLead(vehicleOn);
        double closingSpeed = ourLead.motion.dotProduct(toAnchor, false) * ourLead.speedFactor;
        AEntityF_Multipart<?> hooked = getHookedMultipart();
        boolean entityHeld = getState() == STATE_ENTITY && hookedEntity != null && hookedEntity.getEntityRiding() == null;
        if (hooked instanceof EntityVehicleF_Physics) {
            EntityVehicleF_Physics hookedLead = getTowChainLead((EntityVehicleF_Physics) hooked);
            closingSpeed -= hookedLead.motion.dotProduct(toAnchor, false) * hookedLead.speedFactor;
        } else if (entityHeld) {
            census.entityVelocities.computeIfAbsent(hookedEntity, k -> hookedEntity.getVelocity().copy());
            closingSpeed -= census.getVelocity(hookedEntity).dotProduct(toAnchor, false);
        }
        boolean stopping = closingSpeed < 0;
        boolean springing = closingSpeed < Math.min((distance - ropeVar.currentValue) * STIFFNESS, MAX_PULL);

        //The climb moves us itself, so the line isn't holding us then.
        if (selfShareVar.currentValue > 0 && getMantle() == MANTLE_NONE) {
            census.add(ourLead, this, toAnchor.copy(), stopping, springing);
        }
        if (hooked instanceof EntityVehicleF_Physics && 1 - selfShareVar.currentValue > 0) {
            census.add(getTowChainLead((EntityVehicleF_Physics) hooked), this, toAnchor.copy().scale(-1), stopping, springing);
        }
        //Only the server knows which entity the hook is in, and only it pulls entities.
        if (entityHeld) {
            census.add(hookedEntity, this, toAnchor.copy().scale(-1), stopping, springing);
        }
    }

    /**Every taut line in a world on one tick, by the vehicle at the front of each tow chain they hold.**/
    private static class LineCensus {
        final long tick;
        /**Keyed by the front of the tow chain each line holds, or the entity it's hooked into.**/
        final Map<Object, List<LineHold>> holds = new HashMap<>();
        /**Each hooked entity's velocity before any line pulled it this tick.  See {@link PartGrapple#pullEntity}.**/
        final Map<IWrapperEntity, Point3D> entityVelocities = new HashMap<>();

        LineCensus(long tick) {
            this.tick = tick;
        }

        void add(Object body, PartGrapple grapple, Point3D pull, boolean stopping, boolean springing) {
            holds.computeIfAbsent(body, k -> new ArrayList<>()).add(new LineHold(grapple, pull, stopping, springing));
        }

        /**Returns the entity's velocity before any line pulled it this tick, or its velocity now if no line holds it.**/
        Point3D getVelocity(IWrapperEntity entity) {
            Point3D velocity = entityVelocities.get(entity);
            return velocity != null ? velocity : entity.getVelocity();
        }
    }

    /**One line holding one tow chain or entity: the grapple, and which way it pulls it, normalized.**/
    private static class LineHold {
        final PartGrapple grapple;
        final Point3D pull;
        /**True if the line has to stop the ends pulling apart this tick.**/
        final boolean stopping;
        /**True if the line pulls the ends back towards its length this tick.**/
        final boolean springing;

        LineHold(PartGrapple grapple, Point3D pull, boolean stopping, boolean springing) {
            this.grapple = grapple;
            this.pull = pull;
            this.stopping = stopping;
            this.springing = springing;
        }
    }

    /**How many lines' worth share each part of holding a body the way one line pulls it, counting that line as 1.  See {@link PartGrapple#getLinesHolding}.**/
    private static class LineShare {
        /**Lines sharing stopping the body moving away.**/
        final double stop;
        /**Lines sharing pulling the body back towards their length.**/
        final double spring;
        /**Lines sharing swinging the body after where they go.  Swinging follows motion rather than tension, so every taut line counts, whatever it's doing.**/
        final double swing;
        /**How many taut lines are on the body in any direction.  They share how far it can be turned in a tick, so more lines can't turn it any faster.**/
        final int count;

        LineShare(double stop, double spring, double swing, int count) {
            this.stop = stop;
            this.spring = spring;
            this.swing = swing;
            this.count = count;
        }
    }

    /**
     * Returns the vehicle at the front of the tow chain the passed-in vehicle is in, which is itself if it isn't towed.
     */
    private static EntityVehicleF_Physics getTowChainLead(EntityVehicleF_Physics vehicle) {
        //Hitching refuses anything that would loop a chain back on itself, but a bad save could still have one.
        //Cap the steps rather than tracking every vehicle seen, as this runs many times a tick and almost never has a step to take.
        for (int steps = 0; vehicle.towedByConnection != null && steps < MAX_TOW_CHAIN_LENGTH; ++steps) {
            vehicle = vehicle.towedByConnection.towingVehicle;
        }
        return vehicle;
    }

    /**
     * Returns true if the passed-in vehicle is in the same tow chain as the vehicle we are on.
     */
    private boolean isInOurTowChain(EntityVehicleF_Physics vehicle) {
        return getTowChainLead(vehicle) == getTowChainLead(vehicleOn);
    }

    /**
     * Returns every vehicle in the tow chain led by the passed-in vehicle, front first.
     */
    private static List<EntityVehicleF_Physics> getTowChain(EntityVehicleF_Physics lead) {
        List<EntityVehicleF_Physics> chain = new ArrayList<>();
        chain.add(lead);
        for (int i = 0; i < chain.size(); ++i) {
            for (TowingConnection connection : chain.get(i).towingConnections) {
                if (!chain.contains(connection.towedVehicle)) {
                    chain.add(connection.towedVehicle);
                }
            }
        }
        return chain;
    }

    /**
     * Returns how much of the horizontal part of a force on the vehicle its grounded tyres and brakes hold against, in world coordinates.
     * The brakes are what hold a vehicle on a line.  They hold against the pull in any direction, with the same force they stop
     * the vehicle with.  The tyres hold against what's left of the sideways part by {@link #LINE_TYRE_GRIP} of their lateral
     * friction and the weight on them, so an unbraked vehicle doesn't slide about like it's on ice.  The wheels roll freely
     * along the heading unless the brakes are on.  The hold is never more than the force, so friction never pushes back past standing still.
     *
     * @param lift how much of the vehicle's weight the line takes, in kg*m/ticks^2.  The tyres and brakes grip less by that much.
     */
    private static Point3D getTyreHold(EntityVehicleF_Physics vehicle, Point3D force, double lift) {
        Point3D hold = new Point3D();
        Set<PartGroundDevice> grounded = vehicle.groundDeviceCollective.groundedGroundDevices;
        Point3D heading = new Point3D(vehicle.headingVector.x, 0, vehicle.headingVector.z);
        double weight = vehicle.getGravitationalForce();
        Point3D flat = new Point3D(force.x, 0, force.z);
        double flatForce = flat.length();
        if (grounded.isEmpty() || heading.length() < 1.0E-3 || weight <= 0 || flatForce < 1.0E-9) {
            return hold;
        }
        heading.normalize();
        Point3D side = new Point3D(heading.z, 0, -heading.x);

        //Our weight is shared evenly between the grounded wheels.
        double load = Math.max(0, weight - lift);
        double lateralGrip = 0;
        double brakeGrip = 0;
        for (PartGroundDevice groundDevice : grounded) {
            lateralGrip += groundDevice.lateralFrictionVar.currentValue * load / grounded.size();
            brakeGrip += vehicle.getWheelBrakingForce(groundDevice) * load / weight;
        }
        hold.addScaled(flat, Math.min(flatForce, brakeGrip) / flatForce);
        double across = flat.subtract(hold).dotProduct(side, false);
        hold.addScaled(side, Math.signum(across) * Math.min(Math.abs(across), lateralGrip * LINE_TYRE_GRIP));
        return hold;
    }

    /**
     * Applies a queued line pull to the vehicle.  Called by the vehicle during its update.
     * The motion has already had the chain's tyres and brakes taken off it, in {@link #getChainPull}.
     */
    public static void applyPull(EntityVehicleF_Physics vehicle, LinePull pull) {
        if (pull.motion != null) {
            vehicle.motion.add(pull.motion);
        }
        turnFromLine(vehicle, pull.point, pull.force, pull.pivot, pull.motion != null, pull.otherMotion, vehicle.getFreeWallBoxes(), pull.maxTurn, pull.swingShare);
    }

    /**
     * Turns the vehicle so the point the line is on swings round towards the line's other end.  The same on both ends of the line.
     * -On the ground, it turns about the grounded wheels at the far end from the hook, or about the hookup for a trailer.
     * With the brakes off, it swings about that like a trailer about its hitch.  See {@link #swingFromLine}.  With the
     * brakes on, the line has to twist it against them: the turn is the torque over the vehicle's moments, the same as its
     * own physics, less what the brakes resist, so a light tug won't turn a braked vehicle.  Either way, the line can lift
     * it up off its wheels against its weight.  The tyres and brakes can only push back so hard, so a pull past their grip
     * drags the vehicle along rather than tipping it further.
     * -In the air, it turns about its middle until it hangs from the hook.
     * 
     * It never turns past pointing the hook along the line, so it can't swing back and forth.  It never
     * tips a grounded wheel into the ground, and never swings a wheel or box into a wall.
     *
     * @param point where the line is on the vehicle, in world coordinates.
     * @param lineForce the line's tension, in kg*m/ticks^2.
     * @param pivotPoint what the vehicle turns about, in world coordinates, or null to work it out from the wheels.
     * @param takesMotion true if the vehicle was given the line's motion this tick, rather than only turning about a hookup.
     * @param otherMotion the motion of the front of the chain on the other end of the line, to swing after, or null to swing after our own motion.
     * @param freeBoxes the vehicle's boxes that aren't already inside a block, to check the turn against.
     * @param maxTurn the most the line can turn the vehicle, in degrees per tick.  Already divided between the lines on it.
     * @param swingShare how many lines' worth swing the vehicle the way this one does.  See {@link LineShare#swing}.
     */
    private static void turnFromLine(EntityVehicleF_Physics vehicle, Point3D point, Point3D lineForce, Point3D pivotPoint, boolean takesMotion, Point3D otherMotion, List<BoundingBox> freeBoxes, double maxTurn, double swingShare) {
        if (vehicle.lockedOnRoad) {
            //The road sets our heading.
            return;
        }

        //Work in the vehicle's own frame, as its moments are about its own axes.
        //There's no centre of mass, so take the middle of the wheels.  Without wheels, use the origin.
        Point3D center = new Point3D();
        int wheels = 0;
        for (APart part : vehicle.allParts) {
            if (part instanceof PartGroundDevice && !part.placementDefinition.isSpare) {
                center.add(toLocal(vehicle, part.position));
                ++wheels;
            }
        }
        if (wheels > 0) {
            center.scale(1D / wheels);
        }
        Point3D hook = toLocal(vehicle, point);
        Point3D up = new Point3D(0, 1, 0).reOrigin(vehicle.orientation);
        Set<PartGroundDevice> grounded = vehicle.groundDeviceCollective.groundedGroundDevices;
        double weight = vehicle.getGravitationalForce();

        //Only a pivot at an end can be swung about like a trailer.  A hook about the middle twists us instead.
        Point3D pivot;
        boolean swing = false;
        if (pivotPoint != null) {
            pivot = toLocal(vehicle, pivotPoint);
            swing = !grounded.isEmpty();
        } else if (!grounded.isEmpty()) {
            //Turn about the grounded wheels at the far end from the hook.  With the hook about the
            //middle, or nothing grounded at the far end, turn about all of them.
            Point3D farEnd = new Point3D();
            int farEndWheels = 0;
            Point3D allWheels = new Point3D();
            double hookSide = hook.z - center.z;
            for (PartGroundDevice groundDevice : grounded) {
                Point3D wheel = toLocal(vehicle, groundDevice.position);
                allWheels.add(wheel);
                if (Math.abs(hookSide) > PULL_PIVOT_END_DISTANCE && (wheel.z - center.z) * hookSide < 0) {
                    farEnd.add(wheel);
                    ++farEndWheels;
                }
            }
            pivot = farEndWheels > 0 ? farEnd.scale(1D / farEndWheels) : allWheels.scale(1D / grounded.size());
            swing = farEndWheels > 0;
        } else {
            pivot = center;
        }

        //Twisting slides each grounded wheel square to its arm from the pivot.  The brakes resist that in any
        //direction, and the further a wheel is from the pivot, the more it resists.  Any brakes on, and the line
        //has to twist us against them rather than swing us.  The line lightens us when it lifts us, so the brakes grip less.
        Point3D force = lineForce.copy();
        double lift = Math.max(0, force.y);
        double twistResistance = 0;
        if (!grounded.isEmpty() && weight > 0) {
            double load = Math.max(0, weight - lift);
            for (PartGroundDevice groundDevice : grounded) {
                double braking = vehicle.getWheelBrakingForce(groundDevice) * load / weight;
                if (braking > 0) {
                    swing = false;
                    Point3D wheelArm = toLocal(vehicle, groundDevice.position).subtract(pivot);
                    wheelArm.addScaled(up, -wheelArm.dotProduct(up, false));
                    twistResistance += braking * wheelArm.length();
                }
            }

            //For tipping, the ground only pushes back on the line as hard as the tyres and brakes hold.  Past
            //that we're dragged, which the line's motion already does, rather than tipped further.  Free-rolling
            //wheels don't push back along the heading at all.
            Point3D hold = getTyreHold(vehicle, force, lift);
            force.x = hold.x;
            force.z = hold.z;
        }
        Point3D forceLocal = force.reOrigin(vehicle.orientation);
        Point3D fullForceLocal = lineForce.copy().reOrigin(vehicle.orientation);

        //Torque from the line on the hook, and from our weight on our middle.  Tipping only comes from what
        //the ground holds, but twisting comes from the whole line force, as the brakes resisting it are
        //already counted in twistResistance.  Capping both left a strong sideways pull just sliding us.
        Point3D arm = hook.copy().subtract(pivot);
        Point3D lineTorque = arm.crossProduct(forceLocal);
        lineTorque.addScaled(up, arm.crossProduct(fullForceLocal).dotProduct(up, false) - lineTorque.dotProduct(up, false));
        Point3D torque = lineTorque.copy().add(center.copy().subtract(pivot).crossProduct(up.copy().scale(-weight)));
        if (!grounded.isEmpty()) {
            //Swinging does our twist.  Otherwise the brakes stop us twisting until the line beats them.
            double twist = torque.dotProduct(up, false);
            double twistLeft = swing ? 0 : Math.signum(twist) * Math.max(0, Math.abs(twist) - twistResistance);
            torque.addScaled(up, twistLeft - twist);
        }

        //The line and our weight set our pitch and roll here, so ground corrections shouldn't level us while the line has hold of us.
        vehicle.pitchHeldByGrapple = true;
        vehicle.rollHeldByGrapple = true;

        turnFromTorque(vehicle, torque, lineTorque, arm, fullForceLocal, pivot, up, freeBoxes, maxTurn);
        if (swing) {
            swingFromLine(vehicle, hook, pivot, up, lineForce, takesMotion, otherMotion, freeBoxes, maxTurn, swingShare);
        }
    }

    /**
     * Turns the vehicle about the pivot by the torque over its moments, the same as its own physics does.
     * See {@link #turnFromLine}.  All points and vectors are in the vehicle's own frame.
     */
    private static void turnFromTorque(EntityVehicleF_Physics vehicle, Point3D torque, Point3D lineTorque, Point3D arm, Point3D fullForceLocal, Point3D pivot, Point3D up, List<BoundingBox> freeBoxes, double maxTurn) {
        //A moment can be 0 when a pack leaves emptyMass out, which would turn us infinitely, so we don't turn about that axis.
        Point3D turn = new Point3D(getTurnRate(torque.x, vehicle.momentPitch), getTurnRate(torque.y, vehicle.momentYaw), getTurnRate(torque.z, vehicle.momentRoll));
        double angle = turn.length();
        if (!(angle > MIN_PULL_TURN) || Double.isInfinite(angle)) {
            //Too small to bother with.
            return;
        }
        Point3D axis = turn.scale(1D / angle);
        angle = Math.min(angle, maxTurn);
        if (axis.dotProduct(lineTorque, false) > 0 && arm.length() > 1.0E-3 && fullForceLocal.length() > 1.0E-6) {
            //The line is what's turning us, so don't turn past pointing the hook along it.
            angle = Math.min(angle, Math.toDegrees(Math.acos(arm.copy().normalize().dotProduct(fullForceLocal.copy().normalize(), true))));
        }

        //Lifting or tipping can't push a grounded wheel into the ground.  If it would, keep just the twist.
        if (pushesWheelIntoGround(vehicle, vehicle.groundDeviceCollective.groundedGroundDevices, pivot, axis, angle)) {
            double twist = axis.dotProduct(up, false) * angle;
            if (!(Math.abs(twist) > MIN_PULL_TURN)) {
                return;
            }
            axis = up.copy().scale(Math.signum(twist));
            angle = Math.abs(twist);
        }

        angle = vehicle.limitTurnToWalls(freeBoxes, pivot, axis, angle);
        if (angle != 0) {
            applyTurn(vehicle, pivot, axis, angle);
        }
    }

    /**
     * Swings a grounded, unbraked vehicle about its pivot like a trailer about its hitch.  A trailer turns to point at
     * where its hitch has moved to, and the hook is the hitch here: the vehicle turns so its arm from the pivot to the
     * hook points at where the hook is going this tick.  Its wheels roll rather than slide, so the sideways part of the
     * hook's move goes into the turn, and what's left moves the vehicle along its new arm.  All points and vectors are
     * in the vehicle's own frame, except the line's tension.
     * 
     * A vehicle towed by another vehicle's line swings after that vehicle, as the taut line takes the hook wherever the
     * other end goes, the same as a hitch.  Our own motion can't tell us that, as our wheels keep it along our heading.
     *
     * @param lineForce the line's tension, in kg*m/ticks^2, in world coordinates.
     * @param takesMotion true if the vehicle was given the line's motion this tick, so its motion carries the hook, and the
     * part of that the turn takes up comes off it.  Otherwise it only swings about a hookup, and the hook goes where the line pulls the chain.
     * @param otherMotion the motion of the front of the chain on the other end of the line, for the hook to go with, or null for it to go with ours.
     * @param swingShare how many lines' worth swing the vehicle the way this one does.  Each line works the swing out from
     * the same motion, so lines that agree each only take their part of it, rather than all turning it the whole way.
     */
    private static void swingFromLine(EntityVehicleF_Physics vehicle, Point3D hook, Point3D pivot, Point3D up, Point3D lineForce, boolean takesMotion, Point3D otherMotion, List<BoundingBox> freeBoxes, double maxTurn, double swingShare) {
        Point3D arm = hook.copy().subtract(pivot);
        arm.addScaled(up, -arm.dotProduct(up, false));
        if (arm.length() < MIN_SWING_ARM) {
            return;
        }

        //Where the hook goes this tick, flat on the ground.  Motion gets scaled by speedFactor when applied.
        Point3D hookMove;
        boolean followsOther = takesMotion && otherMotion != null;
        if (followsOther) {
            hookMove = otherMotion.copy().scale(vehicle.speedFactor);
        } else if (takesMotion) {
            hookMove = vehicle.motion.copy().scale(vehicle.speedFactor);
        } else {
            EntityVehicleF_Physics lead = getTowChainLead(vehicle);
            if (!(lead.currentMass > 0)) {
                return;
            }
            hookMove = lineForce.copy().scale(vehicle.speedFactor / lead.currentMass);
        }
        hookMove = hookMove.reOrigin(vehicle.orientation);
        hookMove.addScaled(up, -hookMove.dotProduct(up, false));
        Point3D target = arm.copy().add(hookMove);
        double twist = arm.crossProduct(target).dotProduct(up, false);
        if (target.length() < 1.0E-6 || twist == 0) {
            return;
        }

        //Turn the arm onto the target, the same as trailers turn onto their hitch.
        Point3D axis = up.copy().scale(Math.signum(twist));
        double angle = Math.toDegrees(Math.acos(arm.copy().normalize().dotProduct(target.copy().normalize(), true)));
        Point3D lineLocal = lineForce.copy().reOrigin(vehicle.orientation);
        lineLocal.addScaled(up, -lineLocal.dotProduct(up, false));
        if (lineLocal.length() > 1.0E-6 && arm.crossProduct(lineLocal).dotProduct(axis, false) > 0) {
            //Our own motion can carry the hook further round than the line, so don't turn past pointing the hook along it.
            angle = Math.min(angle, Math.toDegrees(Math.acos(arm.copy().normalize().dotProduct(lineLocal.normalize(), true))));
        } else if (followsOther) {
            //The other end has moved round the far side of the line from us, so the line hasn't swung that way yet.
            return;
        }
        //Lines that agree each take their part of the swing.  Capped after that, so one line turns the same as it always has.
        angle = Math.min(angle / swingShare, maxTurn);
        if (!(angle > MIN_PULL_TURN)) {
            return;
        }
        angle = vehicle.limitTurnToWalls(freeBoxes, pivot, axis, angle);
        if (angle != 0) {
            RotationMatrix turnMatrix = applyTurn(vehicle, pivot, axis, angle);
            if (takesMotion && !followsOther) {
                //The turn has carried the hook sideways, so our wheels don't slide it there too.
                //Not when we swing after the other end, as our motion never had that sideways move in it.
                //This stays off our motion for good, as the wheels rolled rather than slid.
                Point3D swung = arm.copy().rotate(turnMatrix).subtract(arm);
                vehicle.motion.subtract(swung.rotate(vehicle.orientation).scale(1D / vehicle.speedFactor));
            }
        }
    }

    /**
     * Turns the vehicle about the pivot, in its own frame, by the angle in degrees, and returns the turn.
     */
    private static RotationMatrix applyTurn(EntityVehicleF_Physics vehicle, Point3D pivot, Point3D axis, double angle) {
        RotationMatrix turnMatrix = new RotationMatrix().setToAxisAngle(axis, angle);
        //Vehicles turn about their origin, so shift the origin to keep the pivot where it is.
        Point3D pivotShift = pivot.copy().subtract(pivot.copy().rotate(turnMatrix)).rotate(vehicle.orientation).scale(1D / vehicle.speedFactor);
        vehicle.motion.add(pivotShift);
        vehicle.grappleTurnMotion.add(pivotShift);
        vehicle.rotation.angles.add(turnMatrix.convertToAngles());
        return turnMatrix;
    }

    /**
     * Returns how far a torque turns a vehicle about one axis this tick, in degrees, or 0 if it has no moment about that axis.
     */
    private static double getTurnRate(double torque, double moment) {
        return moment > 0 ? Math.toDegrees(torque / moment) : 0;
    }

    /**
     * Returns true if turning the vehicle about the pivot would move any grounded wheel down into the ground.
     */
    private static boolean pushesWheelIntoGround(EntityVehicleF_Physics vehicle, Set<PartGroundDevice> grounded, Point3D pivot, Point3D axis, double angle) {
        RotationMatrix turn = new RotationMatrix().setToAxisAngle(axis, angle);
        for (PartGroundDevice groundDevice : grounded) {
            Point3D wheel = toLocal(vehicle, groundDevice.position);
            Point3D moved = wheel.copy().subtract(pivot).rotate(turn).add(pivot).subtract(wheel);
            //Check the direction, not the size, as small turns into the ground would still add up tick by tick.
            if (moved.rotate(vehicle.orientation).y < -1.0E-6) {
                return true;
            }
        }
        return false;
    }

    private static Point3D toLocal(AEntityVehicleD_Moving vehicle, Point3D worldPosition) {
        return worldPosition.copy().subtract(vehicle.position).reOrigin(vehicle.orientation);
    }

    /**
     * Applies the vehicle's share of the line tension, pulling it towards the anchor.  This is the same
     * physics as the other end of the line gets: the whole tow chain is pulled, its tyres and brakes hold
     * back what they can, and the line's torque turns the vehicle.  See {@link #getChainPull} and
     * {@link #turnFromLine}.  On a towed vehicle, the pull is queued for the chain, so the vehicle at the
     * front takes it and we swing behind it.
     */
    private void applyLineForces(Point3D anchor, double rope, double share) {
        Point3D toAnchor = anchor.copy().subtract(position);
        double distance = toAnchor.length();
        if (distance <= rope || distance < 1.0E-3) {
            //Line is slack, nothing to do.
            return;
        }
        Point3D lineNormal = toAnchor.scale(1D / distance);
        //A hooked vehicle's chain moves too, so the rigid part of the line goes by how fast we're pulling apart from it.
        //So does a hooked entity heavy enough to pull on us.  Clients don't know which entity it is, so they treat it as still.
        AEntityF_Multipart<?> hooked = getHookedMultipart();
        Point3D otherMotion = hooked instanceof EntityVehicleF_Physics ? getTowChainLead((EntityVehicleF_Physics) hooked).motion : null;
        if (hookedEntity != null && getState() == STATE_ENTITY) {
            //Our motion gets scaled by speedFactor when applied, so match the entity's real velocity to that.
            otherMotion = hookedEntity.getVelocity().copy().scale(1D / getTowChainLead(vehicleOn).speedFactor);
        }
        LineShare lines = getLinesHolding(getTowChainLead(vehicleOn), lineNormal);
        ChainPull pull = getChainPull(vehicleOn, lineNormal, distance - rope, share, lines, otherMotion, getWinchLimit());
        if (!world.isClient()) {
            tickHoldTension = Math.max(tickHoldTension, pull.holdTension);
        }
        if (otherMotion != null) {
            logTowPull("towing", vehicleOn, lineNormal, distance - rope, share, lines, otherMotion, pull);
        }
        if (vehicleOn.towedByConnection != null) {
            queueChainPull(vehicleOn, position, pull.force, pull.motion, MAX_PULL_TURN / lines.count, lines.swing, null);
            return;
        }
        vehicleOn.motion.add(pull.motion);

        //Vehicles don't stop wheels going into walls, and the line usually pulls the vehicle towards one.
        List<BoundingBox> freeBoxes = vehicleOn.getFreeWallBoxes();
        vehicleOn.clampMotionToWalls(freeBoxes);

        //The line's own physics can't carry a vehicle up a wall: the wheels catch on the face and the
        //line's turning levels the vehicle as the line flattens near the top.  So once the end facing the
        //hook is up against a wall with the hook up on it, the server hands over to the climb, which carries
        //the vehicle up the wall and over the lip.  It doesn't wait for the wall checks to say we're blocked, as
        //they leave out wheels already touching a block, and a vehicle the line holds against a wall isn't moving into it.
        if (!world.isClient() && tryStartClimb()) {
            return;
        }

        turnFromLine(vehicleOn, position, pull.force, null, true, null, freeBoxes, MAX_PULL_TURN / lines.count, lines.swing);
        //Turning moves our origin to keep the pivot still, so check that against the walls too.
        vehicleOn.clampMotionToWalls(freeBoxes);
    }

    /**
     * Returns true if the vehicle can't climb walls, so the line pulls it like anywhere else.  Mantling needs the
     * climb to get up the wall first, so disableClimb only counts with disableMantle on too.
     */
    private boolean isClimbDisabled() {
        return disableClimbVar.isActive && disableMantleVar.isActive;
    }

    /**
     * Starts the climb if the end of the vehicle facing the hook is being reeled up against a wall too tall for
     * its wheels to drive up, by their climbHeight, with the hook up on it.  Server only.  The wall counts as touching if it's just in
     * front of any of the wheels at that end, or any of the collision boxes that hit blocks.  The hook can be on
     * top of the wall or in its face, even on a part sticking out past the face we touched, as long as it's above
     * the height the wheels could drive up.  The climb path works out how far up the line can take us.  Blocks are
     * checked by their collision boxes, so grass and flowers don't count as a wall.  This is cheap enough to
     * check every tick: a few block checks just in front of each wheel and box.
     * 
     * The climb turns the vehicle square onto the wall's face and keeps it there, so whichever end touched it
     * faces it, with no roll.  See {@link ClimbPath}.
     *
     * @return true if the climb started.
     */
    private boolean tryStartClimb() {
        //Only reeling moves a climb, so only start one while reeling.  Otherwise bumping a wall with the line
        //taut would pin us to it until the line was let out.  Not a debug message, as this is most ticks.
        if (getState() != STATE_BLOCK || !reelVar.isActive || isClimbDisabled()) {
            return false;
        }
        Point3D heading = new Point3D(vehicleOn.headingVector.x, 0, vehicleOn.headingVector.z);
        if (heading.length() < 1.0E-3) {
            debugClimb("vehicle is pointing straight up or down");
            return false;
        }
        heading.normalize();
        //The end facing the hook goes up the wall, whichever end the launcher is on.  Judge that from the
        //middle of the wheels, not the launcher.  Up against the wall, the hook is nearly straight above the
        //launcher, so which way the line leans from it is noise, and it can point at the wrong end.
        WheelSplit wheels = new WheelSplit(1);
        Point3D anchor = getAnchor();
        double side = toVehicleLocal(anchor).z >= wheels.centerZ ? 1 : -1;
        if (side < 0) {
            wheels = new WheelSplit(side);
        }
        Point3D ahead = heading.scale(side);
        if (wheels.launcherEnd.isEmpty() || wheels.farEnd.isEmpty()) {
            debugClimb("needs wheels at both ends");
            return false;
        }
        //The wheels drive up anything up to their climb height by themselves, so only a wall taller than that counts.
        //Go by the wheel at this end that climbs least, as that's the one the wall stops.
        APart lowest = wheels.getLowest(wheels.launcherEnd);
        double climbHeight = Double.MAX_VALUE;
        for (APart wheel : wheels.launcherEnd) {
            climbHeight = Math.min(climbHeight, ((PartGroundDevice) wheel).definition.ground.climbHeight);
        }
        double stepHeight = lowest.position.y - lowest.getHeight() / 2D + climbHeight + MANTLE_STEP;

        Point3D face = null;
        for (APart wheel : wheels.launcherEnd) {
            double radius = wheel.getHeight() / 2D;
            if ((face = findWallAhead(wheel.position, radius, radius, ahead, stepHeight)) != null) {
                break;
            }
        }
        if (face == null) {
            for (BoundingBox box : vehicleOn.allBlockCollisionBoxes) {
                if ((toVehicleLocal(box.globalCenter).z - wheels.centerZ) * side > 0) {
                    //Boxes don't turn with us, so take how far this one reaches out along our heading.
                    double reach = Math.abs(ahead.x) * box.widthRadius + Math.abs(ahead.z) * box.depthRadius;
                    if ((face = findWallAhead(box.globalCenter, reach, box.heightRadius, ahead, stepHeight)) != null) {
                        break;
                    }
                }
            }
        }
        if (face == null) {
            debugClimb("no wall too tall to drive up touching the wheels or collision boxes");
            logWallProbes(wheels, ahead, stepHeight);
            return false;
        }

        //Block faces line up with the world's axes, so square up to whichever one we're mostly heading along.
        Point3D wallNormal = Math.abs(ahead.x) > Math.abs(ahead.z) ? new Point3D(Math.signum(ahead.x), 0, 0) : new Point3D(0, 0, Math.signum(ahead.z));

        //The hook has to be above what the wheels could drive up, so the line pulls us up the wall.  It doesn't
        //have to be in or past the face we touched: the top can stick out over it, and the path steps out over
        //anything sticking out that the wheels could climb.  The path works out how far up we can get.
        if (anchor.y < stepHeight) {
            debugClimb("hook is below the part of the wall the wheels can't drive up");
            return false;
        }

        //Building a path is costly, so after one fails, give it a while before trying again.
        if (vehicleOn.ticksExisted - climbFailedTick < CLIMB_RETRY_INTERVAL) {
            return false;
        }

        //The path starts from the front edge of the plane under the wheels and block collision boxes.
        Point3D leadEdge = new ClimbPlane(wheels).frontLocal.rotate(vehicleOn.orientation).add(vehicleOn.position);
        double lowestBottom = Double.MAX_VALUE;
        for (APart other : wheels.launcherEnd) {
            lowestBottom = Math.min(lowestBottom, other.position.y - other.getHeight() / 2D);
        }
        for (APart other : wheels.farEnd) {
            lowestBottom = Math.min(lowestBottom, other.position.y - other.getHeight() / 2D);
        }
        for (BoundingBox box : vehicleOn.allBlockCollisionBoxes) {
            lowestBottom = Math.min(lowestBottom, box.globalCenter.y - box.heightRadius);
        }
        //Build the path before setting the mantle variables, as each one sends a packet to every client.
        double[] key = {leadEdge.x, leadEdge.z, lowestBottom - 0.5, Math.toDegrees(Math.atan2(wallNormal.x, wallNormal.z)), side, anchor.x, anchor.y, anchor.z};
        ClimbPath path = ClimbPath.build(this, key);
        if (path == null) {
            climbFailedTick = vehicleOn.ticksExisted;
            debugClimb("couldn't work out a path up the wall");
            return false;
        }
        climbPathKey = key;
        climbPath = path;

        //Set the path before the phase, so clients have it by the time they start following it.
        mantleOriginXVar.setTo(key[0], true);
        mantleOriginZVar.setTo(key[1], true);
        mantleBaseVar.setTo(key[2], true);
        mantleYawVar.setTo(key[3], true);
        mantleSideVar.setTo(key[4], true);
        debugClimb("started");
        //Start from where we really are on the wall.  The line might be stretched, and the climb follows its length.
        ropeVar.setTo(clampRope(position.distanceTo(anchor)), true);
        mantleProgressVar.setTo(0, true);
        mantleReadyVar.setTo(0, true);
        setMantle(MANTLE_CLIMB);
        return true;
    }

    /**
     * Looks for a wall just in front of a wheel or collision box, from its middle out to {@link #MANTLE_CONTACT}
     * past its front, at heights up its front.  The wall also has to be solid at the step height there, so a
     * kerb the wheels could roll up, or a ledge the vehicle could drive under, doesn't count.  Its whole front is
     * checked, not just the part above the step height, as a low nose hits the wall well before the wheels do.
     *
     * @param center the middle of the wheel or box.
     * @param reach how far its front is from its middle, along ahead.
     * @param halfHeight half its height.
     * @param ahead the horizontal direction towards the wall.  Must be normalized.
     * @param stepHeight the height the wall has to be solid at to count.
     * @return the first solid point found, which is on the middle if it's already in the wall, or null if there's no wall there.
     */
    private Point3D findWallAhead(Point3D center, double reach, double halfHeight, Point3D ahead, double stepHeight) {
        Point3D step = ahead.copy().scale(MANTLE_STEP);
        int steps = (int) Math.ceil((reach + MANTLE_CONTACT) / MANTLE_STEP);
        //Start just off the bottom, so a wheel resting on the ground doesn't find it.
        for (double y = center.y - halfHeight + MANTLE_STEP; y <= center.y + halfHeight; y += MANTLE_STEP * 4) {
            Point3D point = new Point3D(center.x, y, center.z);
            if (isSolid(point)) {
                //Already inside the wall here, as a taut line can drag us into it.  That's touching it too.
                if (isSolid(new Point3D(point.x, stepHeight, point.z))) {
                    return point;
                }
                continue;
            }
            for (int i = 0; i < steps; ++i) {
                Point3D next = point.copy().add(step);
                if (isSolid(next)) {
                    if (isSolid(new Point3D(next.x, stepHeight, next.z))) {
                        return next;
                    }
                    break;
                }
                point = next;
            }
        }
        return null;
    }

    /**
     * Dev mode only, temporary: logs where the wheels and boxes at the end facing the hook are, and how far
     * ahead of each the nearest solid block is, so a wall the climb misses can be found in the log.
     */
    private void logWallProbes(WheelSplit wheels, Point3D ahead, double stepHeight) {
        if (!ConfigSystem.settings.general.devMode.value || vehicleOn.ticksExisted % 10 != 0) {
            return;
        }
        StringBuilder log = new StringBuilder(String.format("GrappleWall tick %d: ahead (%.2f %.2f) stepHeight %.2f", vehicleOn.ticksExisted, ahead.x, ahead.z, stepHeight));
        for (APart wheel : wheels.launcherEnd) {
            double radius = wheel.getHeight() / 2D;
            log.append(String.format(" | wheel local z %.2f y %.2f to %.2f reach %.2f wall %s", toVehicleLocal(wheel.position).z, wheel.position.y - radius, wheel.position.y + radius, radius, getWallDistances(wheel.position, radius, ahead)));
        }
        for (BoundingBox box : vehicleOn.allBlockCollisionBoxes) {
            double localZ = toVehicleLocal(box.globalCenter).z;
            if ((localZ - wheels.centerZ) * wheels.side > 0) {
                log.append(String.format(" | box local z %.2f y %.2f to %.2f reach %.2f wall %s", localZ, box.globalCenter.y - box.heightRadius, box.globalCenter.y + box.heightRadius, Math.abs(ahead.x) * box.widthRadius + Math.abs(ahead.z) * box.depthRadius, getWallDistances(box.globalCenter, box.heightRadius, ahead)));
            }
        }
        InterfaceManager.coreInterface.logError(log.toString());
    }

    /**
     * Dev mode only, temporary: how far ahead of the middle the first solid block is at the bottom, middle and top, up to 4 blocks.
     */
    private String getWallDistances(Point3D center, double halfHeight, Point3D ahead) {
        StringBuilder distances = new StringBuilder();
        for (double y : new double[] {center.y - halfHeight + MANTLE_STEP, center.y, center.y + halfHeight - MANTLE_STEP}) {
            String found = "-";
            for (double distance = 0; distance <= 4; distance += MANTLE_STEP) {
                if (isSolid(new Point3D(center.x, y, center.z).addScaled(ahead, distance))) {
                    found = String.format("%.2f", distance);
                    break;
                }
            }
            distances.append(distances.length() == 0 ? "" : "/").append(found);
        }
        return distances.toString();
    }

    /**
     * Returns the climb path for the current mantle variables, building it if they've changed.  Null if no path could be built from them.
     */
    private ClimbPath getClimbPath() {
        double[] key = {mantleOriginXVar.currentValue, mantleOriginZVar.currentValue, mantleBaseVar.currentValue, mantleYawVar.currentValue, mantleSideVar.currentValue, anchorXVar.currentValue, anchorYVar.currentValue, anchorZVar.currentValue};
        if (climbPathKey == null || !Arrays.equals(key, climbPathKey)) {
            climbPathKey = key;
            climbPath = ClimbPath.build(this, key);
        }
        return climbPath;
    }

    /**
     * Carries the vehicle up the wall.  Runs on both client and server.  The server moves the progress
     * along the path and changes phase, and both sides put the vehicle where the path says for that progress:
     * -Climb and ready: the progress follows the line, so reeling in carries the vehicle up the wall and
     * letting out lowers it.  Once the line is all the way in, near enough the top, holding reel for
     * {@link #MANTLE_HOLD_TICKS} starts the pull.
     * -Pull: holding reel carries on along the path, over the edge and onto the top.  Letting go slips back
     * to where the pull started.
     * -Complete: flat on top, and held there until the grapple is released.  Driving backwards goes back
     * down the path, letting the line out.
     */
    private void updateMantle() {
        boolean server = !world.isClient();
        ClimbPath path = getClimbPath();
        if (path == null || isClimbDisabled()) {
            if (server) {
                setMantle(MANTLE_NONE);
            }
            return;
        }

        double progress = mantleProgressVar.currentValue;
        if (server) {
            Point3D anchor = getAnchor();
            int phase = getMantle();
            switch (phase) {
                case MANTLE_CLIMB:
                case MANTLE_READY: {
                    double rope = ropeVar.currentValue;
                    //With the mantle off, the line can carry us up the wall, but not over the edge.  An overhang we
                    //can't get past stops us under it, short of the edge, and with no room on top we stop at the edge.
                    boolean canMantle = !disableMantleVar.isActive && !path.blocked && !path.noRoomOnTop;
                    double end = canMantle ? path.completeAt : path.getLipProgress();
                    progress = path.getProgressForLine(progress, rope, anchor, end);
                    if (progress <= 0 && path.getLineLength(0, anchor) < rope - MANTLE_CONTACT) {
                        //Let back down to the bottom with line to spare.  The line takes over again.
                        setMantle(MANTLE_NONE);
                        return;
                    }
                    if (path.blocked || path.noRoomOnTop) {
                        if (progress >= end - ClimbPath.SAMPLE) {
                            if (!mantleBlockedSent && reelVar.isActive) {
                                mantleBlockedSent = true;
                                sendMantleMessage(path.blocked ? LanguageSystem.INTERACT_GRAPPLE_MANTLEBLOCKED : LanguageSystem.INTERACT_GRAPPLE_MANTLENOROOM);
                            }
                            if (isDrivingBackwards()) {
                                setMantle(MANTLE_DESCEND);
                            }
                        } else if (progress < end - MANTLE_CONTACT) {
                            //Tell them again if they come back up to it.
                            mantleBlockedSent = false;
                        }
                    }
                    if (canMantle && progress >= path.completeAt) {
                        progress = path.completeAt;
                        completeMantle();
                        break;
                    }
                    //If the path can't take the launcher any closer to the hook, the winch can't pull us any
                    //further, so that's as far in as the line goes.  Keep the line at that length, so letting
                    //it out lowers us straight away rather than first paying out line we never used.
                    boolean lineIn = rope <= minLengthVar.currentValue + 1.0E-6;
                    double length = path.getLineLength(progress, anchor);
                    if (length > rope + 1.0E-3) {
                        ropeVar.setTo(clampRope(length), true);
                        lineIn = true;
                    }
                    //Close enough to the edge if the hook's depth below the top, plus the line left at its
                    //shortest, is in reach.  A hook on top counts as no depth.
                    double reach = Math.max(0, path.top - anchor.y) + minLengthVar.currentValue;
                    if (canMantle && lineIn && reach <= MANTLE_REACH) {
                        if (phase == MANTLE_CLIMB) {
                            mantleReadyVar.setTo(progress, true);
                            mantleHoldTicks = 0;
                            setMantle(MANTLE_READY);
                            sendMantleMessage(LanguageSystem.INTERACT_GRAPPLE_MANTLEREADY);
                        } else if (!reelVar.isActive) {
                            mantleHoldTicks = 0;
                        } else if (++mantleHoldTicks >= MANTLE_HOLD_TICKS) {
                            setMantle(MANTLE_PULL);
                        }
                    } else if (phase == MANTLE_READY) {
                        setMantle(MANTLE_CLIMB);
                    } else if (lineIn && canMantle) {
                        debugClimb(String.format("line is in, but the hook's depth below the top plus the minimum line length is %.2f blocks, more than %.0f", reach, MANTLE_REACH));
                    }
                    break;
                }
                case MANTLE_PULL: {
                    if (reelVar.isActive) {
                        progress += MANTLE_SPEED;
                        if (progress >= path.completeAt) {
                            progress = path.completeAt;
                            completeMantle();
                        }
                    } else {
                        progress -= MANTLE_SLIP_SPEED;
                        if (progress <= mantleReadyVar.currentValue) {
                            progress = mantleReadyVar.currentValue;
                            mantleHoldTicks = 0;
                            setMantle(MANTLE_READY);
                        }
                    }
                    break;
                }
                case MANTLE_COMPLETE: {
                    //Held flat on top until the grapple is released, or we drive back down.
                    if (isDrivingBackwards()) {
                        setMantle(MANTLE_DESCEND);
                    }
                    break;
                }
                case MANTLE_DESCEND: {
                    if (reelVar.isActive) {
                        //Winching back up.  Over the edge, that's the pull, and below it, the line.
                        if (progress > mantleReadyVar.currentValue && mantleReadyVar.currentValue > 0) {
                            setMantle(MANTLE_PULL);
                        } else {
                            setMantle(MANTLE_CLIMB);
                        }
                    } else if (isDrivingBackwards()) {
                        progress -= MANTLE_DESCEND_SPEED * vehicleOn.throttleVar.currentValue;
                        if (progress <= 0) {
                            ropeVar.setTo(clampRope(path.getLineLength(0, anchor)), true);
                            setMantle(MANTLE_NONE);
                            return;
                        }
                    }
                    //Let the line out as we go down, so it's the right length when it takes over again.
                    ropeVar.setTo(clampRope(path.getLineLength(progress, anchor)), true);
                    break;
                }
            }
            mantleProgressVar.setTo(progress, true);
        }
        holdClimbPose(path, progress);
    }

    private void completeMantle() {
        setMantle(MANTLE_COMPLETE);
        sendMantleMessage(LanguageSystem.INTERACT_GRAPPLE_MANTLECOMPLETE);
    }

    /**
     * Moves the vehicle to where the path puts it, and tells it to leave its ground and collision checks
     * out this tick.  The move and turn are capped, so starting a climb slides the vehicle onto the wall
     * rather than snapping it there.
     */
    private void holdClimbPose(ClimbPath path, double progress) {
        Point3D targetPosition = new Point3D();
        RotationMatrix targetOrientation = new RotationMatrix();
        path.getPose(progress, targetPosition, targetOrientation);

        Point3D move = targetPosition.subtract(vehicleOn.position);
        if (move.length() > MANTLE_MAX_MOVE) {
            move.scale(MANTLE_MAX_MOVE / move.length());
        }

        //Turns are applied in our own frame, so take our orientation back off the one we want.  If that's
        //more than we can turn in a tick, turn part of the way about the same axis.  This isn't done with
        //RotationMatrix.interploate, as that goes wrong for vehicles facing about yaw 180, which is half of all walls.
        RotationMatrix turn = new RotationMatrix().multiplyTranspose(vehicleOn.orientation).multiply(targetOrientation);
        double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, (turn.m00 + turn.m11 + turn.m22 - 1) / 2D))));
        if (angle > MANTLE_MAX_TURN) {
            turn.setToAxisAngle(getRotationAxis(turn, angle), MANTLE_MAX_TURN);
        }
        vehicleOn.motion.set(move).scale(1D / vehicleOn.speedFactor);
        vehicleOn.rotation.angles.set(turn.convertToAngles());
        vehicleOn.climbingWithGrapple = true;
        vehicleOn.pitchHeldByGrapple = true;
        vehicleOn.rollHeldByGrapple = true;
    }

    /**
     * Returns the normalized axis a rotation turns about.
     *
     * @param angle how far it turns, in degrees.
     */
    private static Point3D getRotationAxis(RotationMatrix rotation, double angle) {
        double sin = Math.sin(Math.toRadians(angle));
        if (sin > 1.0E-3) {
            return new Point3D(rotation.m21 - rotation.m12, rotation.m02 - rotation.m20, rotation.m10 - rotation.m01).scale(0.5 / sin).normalize();
        }
        //Turned about half way round, where the terms above vanish.  The matrix is then about 2*axis*axis - 1,
        //so take the axis from its largest diagonal term and the row it's on.
        double xx = (rotation.m00 + 1) / 2D;
        double yy = (rotation.m11 + 1) / 2D;
        double zz = (rotation.m22 + 1) / 2D;
        Point3D axis;
        if (xx >= yy && xx >= zz) {
            double x = Math.sqrt(Math.max(xx, 1.0E-9));
            axis = new Point3D(x, rotation.m01 / (2 * x), rotation.m02 / (2 * x));
        } else if (yy >= zz) {
            double y = Math.sqrt(Math.max(yy, 1.0E-9));
            axis = new Point3D(rotation.m01 / (2 * y), y, rotation.m12 / (2 * y));
        } else {
            double z = Math.sqrt(Math.max(zz, 1.0E-9));
            axis = new Point3D(rotation.m02 / (2 * z), rotation.m12 / (2 * z), z);
        }
        return axis.normalize();
    }

    /**Tells whoever is driving how the mantle is going.  Server only.**/
    private void sendMantleMessage(LanguageSystem.LanguageEntry message) {
        IWrapperEntity controller = getController();
        if (controller instanceof IWrapperPlayer) {
            IWrapperPlayer player = (IWrapperPlayer) controller;
            player.sendPacket(new PacketPlayerChatMessage(player, message));
        }
    }

    private boolean isDrivingBackwards() {
        if (vehicleOn.throttleVar.currentValue < MANTLE_THROTTLE) {
            return false;
        }
        if (vehicleOn.reverseThrustVar.isActive) {
            return true;
        }
        for (PartEngine engine : vehicleOn.engines) {
            if (engine.currentGearVar.currentValue < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the vehicle fits flat on top of the wall at the end of the climb path.  The climb plane, see
     * {@link ClimbPlane}, has to have air above it everywhere, as high as the vehicle reaches above it, and rest on top of a solid
     * block somewhere.  Its collision boxes can't be in a block there either, for anything sticking out past the plane.
     * Boxes don't turn with us, so they're moved to where they'd be and checked as they are.
     *
     * @param planeFront the plane's edge at the end facing the wall, in our own frame.
     * @param planeRear the plane's edge at the far end, in our own frame.
     * @param tracks offsets across the plane to check along, from the middle of its edges.
     */
    private boolean hasRoomOnTop(ClimbPath path, Point3D planeFront, Point3D planeRear, List<Double> tracks) {
        Point3D topPosition = new Point3D();
        RotationMatrix topOrientation = new RotationMatrix();
        path.getPose(path.completeAt, topPosition, topOrientation);
        Point3D up = new Point3D(0, 1, 0).rotate(topOrientation);

        //How high the vehicle reaches above the lower edge of the plane.
        double planeBottom = Math.min(planeFront.y, planeRear.y);
        double reach = MANTLE_ROOM_MARGIN;
        for (BoundingBox box : vehicleOn.allBlockCollisionBoxes) {
            reach = Math.max(reach, toVehicleLocal(box.globalCenter).y + box.heightRadius - planeBottom);
        }
        for (APart part : vehicleOn.allParts) {
            reach = Math.max(reach, toVehicleLocal(part.boundingBox.globalCenter).y + part.boundingBox.heightRadius - planeBottom);
        }

        //Walk the plane, checking there's air above each point and a block just under at least one.
        int steps = Math.max(1, (int) Math.ceil(planeFront.distanceTo(planeRear) / MANTLE_ROOM_STEP));
        boolean supported = false;
        for (int i = 0; i <= steps; ++i) {
            Point3D along = planeRear.copy().interpolate(planeFront, i / (double) steps);
            for (double track : tracks) {
                Point3D point = along.copy().add(track, 0, 0).rotate(topOrientation).add(topPosition);
                for (double height = MANTLE_ROOM_MARGIN;; height = Math.min(reach, height + MANTLE_ROOM_STEP)) {
                    if (isSolid(point.copy().addScaled(up, height))) {
                        return false;
                    }
                    if (height >= reach) {
                        break;
                    }
                }
                supported |= isSolid(point.copy().addScaled(up, -MANTLE_ROOM_MARGIN));
            }
        }
        if (!supported) {
            return false;
        }

        for (BoundingBox box : vehicleOn.allBlockCollisionBoxes) {
            Point3D center = toVehicleLocal(box.globalCenter).rotate(topOrientation).add(topPosition);
            center.y += MANTLE_ROOM_MARGIN;
            BoundingBox onTop = new BoundingBox(center, Math.max(MANTLE_STEP, box.widthRadius - MANTLE_ROOM_MARGIN), Math.max(MANTLE_STEP, box.heightRadius - MANTLE_ROOM_MARGIN), Math.max(MANTLE_STEP, box.depthRadius - MANTLE_ROOM_MARGIN));
            if (world.checkForCollisions(onTop, new Point3D(), true, false)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if a block's collision box is at the point.
     */
    private boolean isSolid(Point3D point) {
        return world.checkForCollisions(new BoundingBox(point, MANTLE_PROBE_RADIUS, MANTLE_PROBE_RADIUS, MANTLE_PROBE_RADIUS), new Point3D(), true, false);
    }

    /**
     * The path a vehicle follows up a wall, worked out once from the shape of the wall when the climb starts.
     * 
     * It's built around the climb plane, see {@link ClimbPlane}, seen side-on in the upright plane square
     * onto the wall: u runs along the ground into the wall, and y is height.  The plane's front edge is the
     * furthest the wheels and block collision boxes reach towards the wall, and its back edge the furthest
     * they reach away from it, both at the lowest of their bottoms.  The
     * wall's shape is how high the blocks go at each u, taking the highest across the vehicle's width.  The
     * vehicle's outline, its wheels and the collision boxes that hit blocks, is kept out of that shape.  The
     * climb goes in three stages:
     * -Up the face: the front edge is pitched up against the wall while the back edge slides along the ground
     * behind it, so the plane forms a triangle with the wall and the floor.  If the plane is shorter than the
     * wall, it ends up upright, flat against the face, and slides straight up it.
     * -Over the lip: once the front edge is up level with the top, the plane rests on the lip and slides over it,
     * tipping forwards about it as it goes.  It's flat by the time its middle passes the lip.
     * -Onto the top: the flat plane slides on along the top until the back wheels are up there too.
     * How far the plane's edges have moved is the progress, and the vehicle's place and pitch at each progress
     * come straight from the plane.  The vehicle stays square onto the wall the whole way, with no roll.
     * 
     * Overhangs are taken as steps in the face, as if it were the ground.  One that sticks out from the face
     * below it no further than the front wheels' climbHeight, the plane steps out over on its way up, as the
     * wheels would drive up a step on flat ground.  One that sticks out further stops the path where the vehicle
     * meets its underside, and the path is blocked: it never gets to the lip.
     * 
     * Both sides build the same path from the mantle variables and the hook, so only the progress needs syncing.
     */
    private static class ClimbPath {
        /**Spacing of the poses worked out along the path, in blocks.**/
        private static final double SAMPLE = MANTLE_STEP / 2D;
        /**Spacing of the plane's pitch while it's pitched up the face, in degrees.**/
        private static final double TILT_STEP = 0.5;
        /**How far into the wall's shape the outline can be and still count as clear of it, in blocks.**/
        private static final double CLEARANCE = 1.0E-3;
        /**Most steps the plane slides to rest against the wall for each pose, so an odd wall can't search forever.**/
        private static final int MAX_SLIDE_STEPS = 256;
        /**Points round each wheel's rim in the outline.**/
        private static final int RIM_POINTS = 24;
        /**Spacing of the lines across the vehicle's width the wall's shape is taken along, in blocks.**/
        private static final double TRACK_STEP = 0.5;
        /**How many rows apart the face is checked going up it.  The rows between are only checked where it changes.**/
        private static final int FACE_ROW_SKIP = 4;
        /**How many columns apart the face is looked for going in.  The columns between are only checked before a hit.**/
        private static final int FACE_COLUMN_SKIP = 2;

        private final Point3D origin;
        private final Point3D normal;
        private final double yaw;
        private final double side;
        /**The middle of the plane's front edge, in our own frame.**/
        private final Point3D frontLocal;
        private final Point3D launcherLocal;
        /**Angle the plane points up at when the vehicle is level, in degrees.**/
        private final double planeAngle;
        /**Progress at each pose, rising along the path.**/
        private final double[] progress;
        //The plane's front edge and pitch at each pose.
        private final double[] frontU;
        private final double[] frontY;
        private final double[] tilt;
        /**How far each pose is lifted so the outline clears steps the plane slides over.**/
        private final double[] lift;
        /**Height of the top of the wall where the hook is.**/
        final double top;
        /**Progress at which the front edge reaches the top, so going on would take it over the lip.**/
        final double lipAt;
        /**Progress at which the vehicle is flat on top, or as far as it gets if it's blocked.**/
        final double completeAt;
        /**True if an overhang too big for the wheels stops the path short of the top.**/
        final boolean blocked;
        /**True if the vehicle wouldn't fit on top, so the path goes up the face but can't be taken over the lip.  See {@link PartGrapple#hasRoomOnTop}.**/
        boolean noRoomOnTop;
        /**See {@link #getClosestAhead}.**/
        private double[] closestAhead;
        private Point3D closestAheadAnchor;

        private ClimbPath(Point3D origin, Point3D normal, double yaw, double side, Point3D frontLocal, Point3D launcherLocal, double planeAngle, List<double[]> poses, double[] progress, Outline outline, double top, double lipAt, boolean blocked) {
            this.blocked = blocked;
            this.origin = origin;
            this.normal = normal;
            this.yaw = yaw;
            this.side = side;
            this.frontLocal = frontLocal;
            this.launcherLocal = launcherLocal;
            this.planeAngle = planeAngle;
            this.top = top;
            this.lipAt = lipAt;
            this.progress = progress;
            this.completeAt = progress[progress.length - 1];
            this.frontU = new double[poses.size()];
            this.frontY = new double[poses.size()];
            this.tilt = new double[poses.size()];
            this.lift = new double[poses.size()];
            for (int i = 0; i < poses.size(); ++i) {
                double[] pose = poses.get(i);
                frontU[i] = pose[0];
                frontY[i] = pose[1];
                tilt[i] = pose[2];
                lift[i] = Math.max(0, outline.getDepth(pose[2], pose[0], pose[1]));
            }
            //Ease the lift in going forwards, and out going back, so the vehicle rides up steps rather than hopping.
            for (int i = 1; i < lift.length; ++i) {
                lift[i] = Math.max(lift[i], lift[i - 1] - (progress[i] - progress[i - 1]));
            }
            for (int i = lift.length - 2; i >= 0; --i) {
                lift[i] = Math.max(lift[i], lift[i + 1] - (progress[i + 1] - progress[i]));
            }
        }

        /**
         * Builds the path from the values the grapple's mantle variables hold: {origin x, origin z, base, yaw, side,
         * hook x, hook y, hook z}.  Returns null if there's no wall to climb there, or it's too long a way.
         */
        static ClimbPath build(PartGrapple grapple, double[] key) {
            //Split the wheels about the end facing the wall, which isn't always the launcher's.
            if (key[4] == 0) {
                return null;
            }
            WheelSplit wheels = grapple.new WheelSplit(key[4]);
            if (wheels.launcherEnd.isEmpty() || wheels.farEnd.isEmpty()) {
                return null;
            }
            double side = wheels.side;
            double yaw = key[3];
            Point3D normal = new Point3D(Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
            Point3D across = new Point3D(normal.z, 0, -normal.x).scale(side);
            Point3D origin = new Point3D(key[0], 0, key[1]);
            double base = key[2];
            Point3D anchor = new Point3D(key[5], key[6], key[7]);

            //The plane under the wheels and block collision boxes, in our own frame, which doesn't change as we move.
            //Forwards along it is towards the wall, which is backwards for us when our back faces it.
            ClimbPlane plane = grapple.new ClimbPlane(wheels);
            Point3D frontLocal = plane.frontLocal;
            Point3D rearLocal = plane.rearLocal;
            double planeRun = (frontLocal.z - rearLocal.z) * side;
            double planeRise = frontLocal.y - rearLocal.y;
            double planeLength = Math.hypot(planeRun, planeRise);
            if (planeLength < MANTLE_STEP) {
                return null;
            }

            //The vehicle's outline.
            Outline outline = new Outline(frontLocal, planeRun / planeLength, planeRise / planeLength, side);
            List<APart> allWheels = new ArrayList<>(wheels.launcherEnd);
            allWheels.addAll(wheels.farEnd);
            for (APart wheel : allWheels) {
                outline.addWheel(grapple.toVehicleLocal(wheel.position), wheel.getHeight() / 2D);
            }
            Point3D heading = new Point3D(grapple.vehicleOn.headingVector.x, 0, grapple.vehicleOn.headingVector.z);
            if (heading.length() > 1.0E-3) {
                heading.normalize();
                for (BoundingBox box : grapple.vehicleOn.allBlockCollisionBoxes) {
                    //Boxes don't turn with us, so take how far they reach along our heading.
                    double depth = Math.abs(heading.x) * box.widthRadius + Math.abs(heading.z) * box.depthRadius;
                    outline.addBox(grapple.toVehicleLocal(box.globalCenter), depth, box.heightRadius);
                }
            }
            //The wall's shape is taken along lines spaced evenly across the plane's width, so all of it clears the wall.
            List<Double> tracks = new ArrayList<>();
            for (double x = plane.minX;; x = Math.min(plane.maxX, x + TRACK_STEP)) {
                tracks.add(x - frontLocal.x);
                if (x >= plane.maxX) {
                    break;
                }
            }

            //The stretch of wall to look at: from behind the vehicle to past the hook by a vehicle's length.
            double reach = outline.getReach();
            double uStart = -(reach + MANTLE_PATH_MARGIN);
            double uAnchor = anchor.copy().subtract(origin).dotProduct(normal, false);
            //The hook can be short of the front wheels, on a top that sticks out over the face, but not behind the back ones.
            double uEnd = Math.max(0, uAnchor) + reach + MANTLE_PATH_MARGIN;
            if (uAnchor <= -planeRun || uEnd - uStart > MANTLE_MAX_PATH) {
                return null;
            }
            double ceiling = anchor.y + MANTLE_PATH_MARGIN;
            int count = (int) Math.ceil((uEnd - uStart) / MANTLE_STEP) + 1;
            double[] heights = new double[count];
            for (int i = 0; i < count; ++i) {
                Point3D column = normal.copy().scale(uStart + i * MANTLE_STEP).add(origin);
                heights[i] = -Double.MAX_VALUE;
                for (double track : tracks) {
                    heights[i] = Math.max(heights[i], grapple.getSurfaceHeight(across.copy().scale(track).add(column), base, ceiling));
                }
            }
            //The top is the top of whatever the hook is in.  Not the heightmap there, as that's the ground under
            //the hook if it's on a ledge sticking out.
            int anchorColumn = Math.max(0, Math.min(count - 1, (int) Math.round((uAnchor - uStart) / MANTLE_STEP) + 1));
            double top = grapple.getSurfaceHeight(normal.copy().scale(uStart + anchorColumn * MANTLE_STEP).add(origin).add(across.copy().scale(anchor.copy().subtract(origin).dotProduct(across, false))), anchor.y - MANTLE_STEP, ceiling);

            //Find the face: the first rise ahead of the back wheels too tall for the front ones to roll up, and the
            //lip: where it reaches the top.  Start from the back wheels, as the line can drag the front ones into
            //the wall, where the ground under them would be the wall.  The climb backs them out of it.  Under a
            //ledge sticking out at the top, the ground never gets up to the top, so the lip is where the hook is.
            int startColumn = Math.max(0, (int) Math.round((-planeRun - uStart) / MANTLE_STEP));
            double floor = heights[startColumn];
            double frontRadius = wheels.getLargestRadius(wheels.launcherEnd);
            int faceColumn = -1;
            int lipColumn = -1;
            for (int i = startColumn; i < count && lipColumn < 0; ++i) {
                if (faceColumn < 0 && heights[i] > floor + frontRadius) {
                    faceColumn = i;
                }
                if (faceColumn >= 0 && heights[i] >= top - MANTLE_STEP) {
                    lipColumn = i;
                }
            }
            if (faceColumn < 0 || top <= floor + frontRadius) {
                return null;
            }
            if (lipColumn < 0) {
                lipColumn = Math.max(faceColumn, anchorColumn);
            }
            double uFace = uStart + (faceColumn - 0.5) * MANTLE_STEP;
            outline.setWall(heights, uStart, uFace);
            double uLip = uStart + (lipColumn - 0.5) * MANTLE_STEP;

            //The heightmap only sees the tops of the blocks, so it misses anything sticking out from the face above
            //the ground.  So also find how far out the face reaches at each height above where the front wheels
            //would roll up it.  An overhang that sticks out from the face below it no more than the wheels could
            //drive up, the vehicle steps out over, as it would a step on flat ground.  One that sticks out further
            //stops the climb under it: everything from its underside up is taken as solid.
            double climbHeight = Double.MAX_VALUE;
            for (APart wheel : wheels.launcherEnd) {
                climbHeight = Math.min(climbHeight, ((PartGroundDevice) wheel).definition.ground.climbHeight);
            }
            double rowStart = floor + frontRadius;
            int rows = Math.max(0, (int) Math.ceil((top - rowStart) / MANTLE_STEP));
            //Rows are checked FACE_ROW_SKIP apart, and the ones between only where those two differ, as faces are mostly whole blocks.
            int[] faceColumns = new int[rows];
            int lastRow = -1;
            for (int row = 0; row < rows; row = row == rows - 1 ? rows : Math.min(rows - 1, row + FACE_ROW_SKIP)) {
                faceColumns[row] = grapple.findFaceColumn(origin, normal, across, tracks, uStart, startColumn, count, rowStart + (row + 0.5) * MANTLE_STEP);
                for (int between = lastRow + 1; between < row; ++between) {
                    faceColumns[between] = faceColumns[row] == faceColumns[lastRow] ? faceColumns[row] : grapple.findFaceColumn(origin, normal, across, tracks, uStart, startColumn, count, rowStart + (between + 0.5) * MANTLE_STEP);
                }
                lastRow = row;
            }
            double[] faceU = new double[rows];
            boolean blocked = false;
            double faceBelow = uFace;
            for (int row = 0; row < rows; ++row) {
                faceU[row] = faceColumns[row] < 0 ? Double.MAX_VALUE : uStart + (faceColumns[row] - 0.5) * MANTLE_STEP;
                if (blocked || faceU[row] < faceBelow - climbHeight - CLEARANCE) {
                    blocked = true;
                    faceU[row] = -Double.MAX_VALUE;
                } else if (faceColumns[row] >= 0) {
                    faceBelow = faceU[row];
                }
            }
            outline.setFace(faceU, rowStart);

            //Up the face.  Pitch the plane up a bit at a time, with its back edge resting on the ground and
            //its front pushed up to the wall, until its front edge is level with the top, or it's upright.
            //Wherever the plane can't get clear of the face, it's stuck under an overhang it can't get past, and the
            //path stops there.
            List<double[]> poses = new ArrayList<>();
            double fu = 0;
            boolean atLip = false;
            boolean stuck = false;
            for (double tilt = 0;; tilt = Math.min(90, tilt + TILT_STEP)) {
                //Slide in as far as we can while still resting on the ground, but never with the front edge past the face.
                double pitch = tilt;
                fu = Outline.slide(fu, u -> {
                    if (u > outline.uFace) {
                        return false;
                    }
                    double rest = outline.getRestHeight(pitch, u);
                    return !Double.isNaN(rest) && !outline.hitsFace(pitch, u, rest);
                });
                if (Double.isNaN(fu)) {
                    stuck = true;
                    break;
                }
                double fy = outline.getRestHeight(tilt, fu);
                if (fy >= top) {
                    //Stop where the front edge gets to the top, between this pitch and the last.
                    double[] last = poses.isEmpty() ? new double[] {fu, top, tilt} : poses.get(poses.size() - 1);
                    double t = fy > last[1] ? (top - last[1]) / (fy - last[1]) : 1;
                    poses.add(new double[] {last[0] + (fu - last[0]) * t, top, last[2] + (tilt - last[2]) * t});
                    atLip = true;
                    break;
                }
                poses.add(new double[] {fu, fy, tilt});
                if (tilt >= 90) {
                    break;
                }
            }

            //Upright, and still short of the top.  Slide straight up the face, moving in over any steps on it, and
            //out over any overhangs.
            if (!atLip && !stuck) {
                double fy = poses.get(poses.size() - 1)[1];
                while (fy < top) {
                    fy = Math.min(top, fy + SAMPLE);
                    double height = fy;
                    fu = Outline.slide(fu, u -> outline.getDepth(90, u, height) <= CLEARANCE && !outline.hitsFace(90, u, height));
                    if (Double.isNaN(fu)) {
                        stuck = true;
                        break;
                    }
                    poses.add(new double[] {fu, fy, 90});
                }
            }
            if (stuck && !blocked) {
                //Not an overhang, just a wall the plane doesn't fit, so there's no way up.
                return null;
            }

            //Over the lip and onto the top.  The plane rests on the lip where its front edge got up to it, and
            //slides over, tipping forwards until it's flat by the time its middle is over.  Then it slides
            //on until the back wheels are past the lip by their size.  Not if an overhang stopped us.
            int lipPose = poses.size() - 1;
            if (stuck && lipPose < 1) {
                //Blocked before it got anywhere.
                return null;
            }
            if (!stuck) {
                double pivotU = poses.get(lipPose)[0];
                double lipTilt = poses.get(lipPose)[2];
                double end = Math.max(planeLength, planeLength + uLip - pivotU + wheels.getLargestRadius(wheels.farEnd));
                for (double along = SAMPLE;; along = Math.min(end, along + SAMPLE)) {
                    double tipped = Math.min(1, along / (planeLength / 2D));
                    double tilt = lipTilt * (1 - tipped * tipped * (3 - 2 * tipped));
                    poses.add(new double[] {pivotU + along * Math.cos(Math.toRadians(tilt)), top + along * Math.sin(Math.toRadians(tilt)), tilt});
                    if (along >= end) {
                        break;
                    }
                }
            }

            //The progress is how far the plane has moved: whichever of its edges moved further.
            double[] progress = new double[poses.size()];
            for (int i = 1; i < poses.size(); ++i) {
                double[] from = poses.get(i - 1);
                double[] to = poses.get(i);
                double[] backFrom = getBackEdge(from, planeLength);
                double[] backTo = getBackEdge(to, planeLength);
                double frontMove = Math.hypot(to[0] - from[0], to[1] - from[1]);
                double backMove = Math.hypot(backTo[0] - backFrom[0], backTo[1] - backFrom[1]);
                progress[i] = progress[i - 1] + Math.max(frontMove, backMove);
            }
            if (progress[progress.length - 1] < SAMPLE) {
                return null;
            }
            ClimbPath path = new ClimbPath(origin, normal, yaw, side, frontLocal, grapple.toVehicleLocal(grapple.position), Math.toDegrees(Math.atan2(planeRise, planeRun)), poses, progress, outline, top, progress[lipPose], stuck);
            //The top is only looked for up to a little above the hook, and only under the hook, so a hook in the face of
            //a taller wall, or a ledge with something over it, still gives a top that there's no room on.
            path.noRoomOnTop = !stuck && !grapple.hasRoomOnTop(path, frontLocal, rearLocal, tracks);
            return path;
        }

        /**Returns {u, y} of the plane's back edge for a {front edge u, front edge y, pitch} pose.**/
        private static double[] getBackEdge(double[] pose, double planeLength) {
            return new double[] {pose[0] - planeLength * Math.cos(Math.toRadians(pose[2])), pose[1] - planeLength * Math.sin(Math.toRadians(pose[2]))};
        }

        /**
         * Sets where the vehicle is and how it's turned at the progress.
         */
        void getPose(double progress, Point3D position, RotationMatrix orientation) {
            //Find the poses either side of the progress.  Poses can share a progress, so guard the fraction.
            progress = Math.max(0, Math.min(completeAt, progress));
            int i = Arrays.binarySearch(this.progress, progress);
            i = Math.max(0, Math.min(frontU.length - 2, i >= 0 ? i : -i - 2));
            double span = this.progress[i + 1] - this.progress[i];
            double fraction = span > 0 ? Math.max(0, Math.min(1, (progress - this.progress[i]) / span)) : 0;
            double fu = frontU[i] + (frontU[i + 1] - frontU[i]) * fraction;
            double fy = frontY[i] + (frontY[i + 1] - frontY[i]) * fraction;
            double planeTilt = tilt[i] + (tilt[i + 1] - tilt[i]) * fraction;

            //How far the end facing the wall points up.  Positive pitch is nose down, and with our back to
            //the wall we face away from it, so our nose goes down as the back goes up.  Never quite upright: our
            //orientation is rebuilt from its pitch, yaw and roll angles each tick, and those can't tell yaw from
            //roll when exactly upright, so the rebuild spins us about our nose.
            double pitchUp = Math.min(MANTLE_MAX_PITCH, planeTilt - planeAngle);
            orientation.setToAngles(side > 0 ? new Point3D(-pitchUp, yaw, 0) : new Point3D(pitchUp, yaw + 180, 0));
            position.set(normal).scale(fu).add(origin).add(0, fy, 0).subtract(frontLocal.copy().rotate(orientation));
            position.y += lift[i] + (lift[i + 1] - lift[i]) * fraction;
        }

        /**
         * Returns, for each {@link #SAMPLE} of progress, the shortest the line is from there to the end of the
         * path.  Worked out the first time it's asked for, as the hook doesn't move while we're on the path.
         */
        private double[] getClosestAhead(Point3D anchor) {
            if (closestAhead == null || !anchor.equals(closestAheadAnchor)) {
                closestAheadAnchor = anchor.copy();
                closestAhead = new double[(int) Math.ceil(completeAt / SAMPLE) + 1];
                double closest = Double.MAX_VALUE;
                for (int i = closestAhead.length - 1; i >= 0; --i) {
                    closest = Math.min(closest, getLineLength(Math.min(completeAt, i * SAMPLE), anchor));
                    closestAhead[i] = closest;
                }
            }
            return closestAhead;
        }

        /**Progress at which the front edge reaches the top of the wall, so going on would take it over the lip.**/
        double getLipProgress() {
            return lipAt;
        }

        /**How far the launcher is from the hook at the progress.**/
        double getLineLength(double progress, Point3D anchor) {
            Point3D position = new Point3D();
            RotationMatrix orientation = new RotationMatrix();
            getPose(progress, position, orientation);
            return launcherLocal.copy().rotate(orientation).add(position).distanceTo(anchor);
        }

        /**
         * Returns the progress, starting from the passed-in one, at which the launcher is the line's length
         * from the hook.  Moves on while the line is too long for where we are, and back while it's too short.
         * The line can pull us on as long as somewhere further on, before the end, the launcher is closer to the
         * hook than here.  It can get further for a bit, as stepping out over a bulge in the face moves us out
         * from the wall, but the line carries us over that.  Past the closest point left, the line would have to
         * get longer, so we stop there, short of the line length.
         */
        double getProgressForLine(double progress, double rope, Point3D anchor, double end) {
            double length = getLineLength(progress, anchor);
            if (length > rope) {
                double[] closestAhead = getClosestAhead(anchor);
                while (progress < end && length > rope) {
                    //The shortest the line gets from the next sample on.
                    int ahead = Math.min(closestAhead.length - 1, (int) Math.floor(progress / SAMPLE) + 1);
                    if (closestAhead[ahead] >= length - 1.0E-6) {
                        break;
                    }
                    progress = Math.min(end, progress + SAMPLE);
                    length = getLineLength(progress, anchor);
                }
            } else {
                while (progress > 0 && getLineLength(Math.max(0, progress - SAMPLE), anchor) <= rope) {
                    progress = Math.max(0, progress - SAMPLE);
                }
            }
            return progress;
        }
    }

    /**
     * The vehicle's side-on outline, as points measured from the front edge of the climb plane, see
     * {@link ClimbPlane}: how far along the plane towards the wall, and how far up off it.  Placed by where the plane's
     * front edge is and how far the plane is pitched up, against the wall's shape.  See {@link ClimbPath}.
     */
    private static class Outline {
        private final List<double[]> points = new ArrayList<>();
        private final Point3D frontLocal;
        //The plane's direction in our own frame, forwards being towards the wall, and up.
        private final double alongForward;
        private final double alongUp;
        private final double side;
        private double[] heights;
        private double uStart;
        /**Where the face of the wall is.  Points short of it are over the ground, and the rest over the wall.**/
        private double uFace;
        /**How far out the face reaches in each row of heights going up from rowStart.  See {@link #setFace}.**/
        private double[] faceU = new double[0];
        private double rowStart;

        Outline(Point3D frontLocal, double alongForward, double alongUp, double side) {
            this.frontLocal = frontLocal;
            this.alongForward = alongForward;
            this.alongUp = alongUp;
            this.side = side;
        }

        /**Adds a point in our own frame.**/
        private void addPoint(double z, double y) {
            double forward = (z - frontLocal.z) * side;
            double up = y - frontLocal.y;
            points.add(new double[] {forward * alongForward + up * alongUp, up * alongForward - forward * alongUp});
        }

        /**Adds the rim of a wheel, from its middle in our own frame.**/
        void addWheel(Point3D center, double radius) {
            for (int i = 0; i < ClimbPath.RIM_POINTS; ++i) {
                double angle = 2 * Math.PI * i / ClimbPath.RIM_POINTS;
                addPoint(center.z + radius * Math.cos(angle), center.y + radius * Math.sin(angle));
            }
        }

        /**Adds the edges of a collision box, from its middle in our own frame.**/
        void addBox(Point3D center, double depthRadius, double heightRadius) {
            for (double z = -depthRadius;; z = Math.min(depthRadius, z + MANTLE_UNDERSIDE_STEP)) {
                addPoint(center.z + z, center.y - heightRadius);
                addPoint(center.z + z, center.y + heightRadius);
                if (z >= depthRadius) {
                    break;
                }
            }
            for (double y = -heightRadius;; y = Math.min(heightRadius, y + MANTLE_UNDERSIDE_STEP)) {
                addPoint(center.z - depthRadius, center.y + y);
                addPoint(center.z + depthRadius, center.y + y);
                if (y >= heightRadius) {
                    break;
                }
            }
        }

        /**Furthest any point is from the plane's front edge.**/
        double getReach() {
            double reach = 0;
            for (double[] point : points) {
                reach = Math.max(reach, Math.hypot(point[0], point[1]));
            }
            return reach;
        }

        void setWall(double[] heights, double uStart, double uFace) {
            this.heights = heights;
            this.uStart = uStart;
            this.uFace = uFace;
        }

        /**
         * Sets how far out the face reaches in each row of heights, from rowStart up in rows {@link #MANTLE_STEP}
         * apart.  Everything past that in the row is taken as solid.
         */
        void setFace(double[] faceU, double rowStart) {
            this.faceU = faceU;
            this.rowStart = rowStart;
        }

        /**Returns true if any of the outline is in the face with the plane at the pose.**/
        boolean hitsFace(double tilt, double fu, double fy) {
            double cos = Math.cos(Math.toRadians(tilt));
            double sin = Math.sin(Math.toRadians(tilt));
            for (double[] point : points) {
                int row = (int) Math.floor((fy + point[0] * sin + point[1] * cos - rowStart) / MANTLE_STEP);
                if (row >= 0 && row < faceU.length && fu + point[0] * cos - point[1] * sin > faceU[row] + ClimbPath.CLEARANCE) {
                    return true;
                }
            }
            return false;
        }

        private double getHeight(double u) {
            int column = (int) Math.round((u - uStart) / MANTLE_STEP);
            return heights[Math.max(0, Math.min(heights.length - 1, column))];
        }

        /**
         * Returns how low the plane's front edge can be with the plane pitched up by the tilt and its front edge at
         * fu, resting on the ground short of the wall.  NaN if the outline would be resting on the wall there instead.
         */
        double getRestHeight(double tilt, double fu) {
            double cos = Math.cos(Math.toRadians(tilt));
            double sin = Math.sin(Math.toRadians(tilt));
            double groundRest = -Double.MAX_VALUE;
            double wallRest = -Double.MAX_VALUE;
            for (double[] point : points) {
                double u = fu + point[0] * cos - point[1] * sin;
                double rest = getHeight(u) - (point[0] * sin + point[1] * cos);
                if (u < uFace) {
                    groundRest = Math.max(groundRest, rest);
                } else {
                    wallRest = Math.max(wallRest, rest);
                }
            }
            return wallRest <= groundRest + ClimbPath.CLEARANCE ? groundRest : Double.NaN;
        }

        /**Returns how far the outline dips into the wall's shape with the plane at the pose.  0 or less if it's clear.**/
        double getDepth(double tilt, double fu, double fy) {
            double cos = Math.cos(Math.toRadians(tilt));
            double sin = Math.sin(Math.toRadians(tilt));
            double depth = -Double.MAX_VALUE;
            for (double[] point : points) {
                depth = Math.max(depth, getHeight(fu + point[0] * cos - point[1] * sin) - (fy + point[0] * sin + point[1] * cos));
            }
            return depth;
        }

        /**
         * Returns how far the plane's front edge can go towards the wall from fu, a step at a time, while the pose
         * there is clear.  Backs off first if it isn't.  NaN if it can't get clear.
         *
         * @param clear whether the pose with the front edge at a u is clear.
         */
        static double slide(double fu, DoublePredicate clear) {
            int steps = 0;
            while (!clear.test(fu)) {
                if (++steps > ClimbPath.MAX_SLIDE_STEPS) {
                    return Double.NaN;
                }
                fu -= ClimbPath.SAMPLE;
            }
            while (++steps <= ClimbPath.MAX_SLIDE_STEPS && clear.test(fu + ClimbPath.SAMPLE)) {
                fu += ClimbPath.SAMPLE;
            }
            return fu;
        }
    }

    /**
     * Returns the height of the top of the blocks at the point, looking up from base to ceiling.  If there's
     * nothing there at base, as when the climb starts with the vehicle already partway up the wall, looks down
     * for the ground, and returns {@link #MANTLE_GROUND_SEARCH} blocks below base if there's none.
     */
    private double getSurfaceHeight(Point3D column, double base, double ceiling) {
        Point3D point = new Point3D(column.x, base, column.z);
        if (!isSolid(point)) {
            //Go down a block at a time to the first solid point, then back up in small steps for the exact height.
            int blocksDown = 0;
            while (!isSolid(point)) {
                if (++blocksDown > MANTLE_GROUND_SEARCH) {
                    return base - MANTLE_GROUND_SEARCH;
                }
                point.y -= 1;
            }
            while (isSolid(point)) {
                point.y += MANTLE_STEP;
            }
            return point.y;
        }
        //Go up a block at a time to the first free point, then back down a block and up in small steps for the exact height.
        while (isSolid(point) && point.y < ceiling) {
            point.y += 1;
        }
        if (point.y >= ceiling) {
            return ceiling;
        }
        point.y -= 1;
        while (isSolid(point)) {
            point.y += MANTLE_STEP;
        }
        return point.y;
    }

    /**
     * Returns the first column of the climb path's wall, going in from startColumn, that's solid at the height on
     * any of the tracks across the vehicle, or -1 if none is.  See {@link ClimbPath#build}.
     */
    private int findFaceColumn(Point3D origin, Point3D normal, Point3D across, List<Double> tracks, double uStart, int startColumn, int count, double y) {
        for (int i = startColumn; i < count + ClimbPath.FACE_COLUMN_SKIP - 1; i += ClimbPath.FACE_COLUMN_SKIP) {
            int column = Math.min(count - 1, i);
            if (isColumnSolid(origin, normal, across, tracks, uStart + column * MANTLE_STEP, y)) {
                //Back up over the ones skipped, for the first.
                for (int skipped = Math.max(startColumn, column - ClimbPath.FACE_COLUMN_SKIP + 1); skipped < column; ++skipped) {
                    if (isColumnSolid(origin, normal, across, tracks, uStart + skipped * MANTLE_STEP, y)) {
                        return skipped;
                    }
                }
                return column;
            }
        }
        return -1;
    }

    private boolean isColumnSolid(Point3D origin, Point3D normal, Point3D across, List<Double> tracks, double u, double y) {
        Point3D column = normal.copy().scale(u).add(origin);
        for (double track : tracks) {
            Point3D point = across.copy().scale(track).add(column);
            point.y = y;
            if (isSolid(point)) {
                return true;
            }
        }
        return false;
    }

    private Point3D toVehicleLocal(Point3D worldPosition) {
        return worldPosition.copy().subtract(vehicleOn.position).reOrigin(vehicleOn.orientation);
    }

    private double clampRope(double rope) {
        return Math.max(minLengthVar.currentValue, Math.min(rangeVar.currentValue, rope));
    }

    private int getMantle() {
        return (int) mantleVar.currentValue;
    }

    /**Sets the mantle phase.  Server only, synced to clients, which follow it.**/
    private void setMantle(int phase) {
        if (phase != getMantle()) {
            debugClimb("phase " + phase);
        }
        mantleVar.setTo(phase, true);
    }

    /**
     * In dev mode, tells whoever is driving what the climb is doing, or why it won't start.  Only says
     * something when it changes, so it doesn't fill the chat every tick.  Server only.
     */
    private void debugClimb(String message) {
        if (ConfigSystem.settings.general.devMode.value && !message.equals(lastClimbDebug)) {
            lastClimbDebug = message;
            IWrapperEntity controller = getController();
            if (controller instanceof IWrapperPlayer) {
                IWrapperPlayer player = (IWrapperPlayer) controller;
                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.SYSTEM_DEBUG, "Grapple climb: " + message));
            }
        }
    }

    private int getState() {
        return (int) stateVar.currentValue;
    }

    private void setState(int state) {
        stateVar.setTo(state, true);
        lineOutVar.setTo(state != STATE_IDLE ? 1 : 0, true);
        attachedVar.setTo(state >= STATE_BLOCK ? 1 : 0, true);
    }

    /**
     * Returns true if the hook is stuck in an entity.  Clients know this too, though not which entity.
     */
    public boolean isHookedInEntity() {
        return getState() == STATE_ENTITY;
    }

    /**
     * Returns true if any grapple in the world is hooked into an entity.  Clients use this to only ask the
     * server to cut lines when there might be one in them.
     */
    public static boolean isAnyHookedInEntity(AWrapperWorld world) {
        for (AEntityVehicleD_Moving vehicle : world.getEntitiesExtendingType(AEntityVehicleD_Moving.class)) {
            for (PartGrapple grapple : vehicle.grapples) {
                if (grapple.isHookedInEntity()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Cuts the line of every grapple hooked into the passed-in player, with whatever they're holding.  Server only.
     */
    public static void cutLinesHookedInto(IWrapperPlayer player) {
        for (AEntityVehicleD_Moving vehicle : player.getWorld().getEntitiesExtendingType(AEntityVehicleD_Moving.class)) {
            for (PartGrapple grapple : vehicle.grapples) {
                if (grapple.getState() == STATE_ENTITY && player.equals(grapple.hookedEntity)) {
                    grapple.cutWith(player);
                }
            }
        }
    }

    /**
     * Returns the grapple whose line the player's attack passes through, or null if it misses them all.  Takes the
     * nearest along their look, and not one behind a block.  Client side, where the line is drawn.
     */
    public static PartGrapple getLineAttacked(IWrapperPlayer player) {
        Point3D eye = player.getEyePosition().copy();
        Point3D look = player.getLineOfSight(CUT_REACH).copy();
        BlockHitResult blockHit = player.getWorld().getBlockHit(eye, look);
        double reach = blockHit != null ? eye.distanceTo(blockHit.hitPosition) : CUT_REACH;
        PartGrapple nearest = null;
        for (AEntityVehicleD_Moving vehicle : player.getWorld().getEntitiesExtendingType(AEntityVehicleD_Moving.class)) {
            for (PartGrapple grapple : vehicle.grapples) {
                if (grapple.lineOutVar.isActive) {
                    double distance = getRayHitDistance(eye, look, grapple.position, grapple.getLineAnchor());
                    if (distance >= 0 && distance < reach) {
                        reach = distance;
                        nearest = grapple;
                    }
                }
            }
        }
        return nearest;
    }

    /**
     * Cuts the line for the player, if they're holding something sharp and can reach it.  They can always
     * reach it if the hook is stuck in them.  Server only.
     */
    public void cutLine(IWrapperPlayer player) {
        if (lineOutVar.isActive && canCutLines(player)) {
            boolean hookedInPlayer = getState() == STATE_ENTITY && player.equals(hookedEntity);
            if (hookedInPlayer || getDistanceToSegment(player.getEyePosition(), position, getLineAnchor()) < CUT_REACH + CUT_REACH_MARGIN) {
                cutWith(player);
            }
        }
    }

    /**
     * Cuts the line with what the player is holding, which loses cutToolDamage durability for it.  Server only.
     */
    private void cutWith(IWrapperPlayer player) {
        cut();
        if (cutToolDamageVar.currentValue > 0) {
            player.damageHeldStack((int) cutToolDamageVar.currentValue);
        }
    }

    /**
     * Returns how far along the ray from start along delta it passes within {@link #CUT_RADIUS} of the segment
     * from lineStart to lineEnd, or -1 if it doesn't.  Takes the points on each closest to the other.
     */
    private static double getRayHitDistance(Point3D start, Point3D delta, Point3D lineStart, Point3D lineEnd) {
        Point3D line = lineEnd.copy().subtract(lineStart);
        Point3D between = start.copy().subtract(lineStart);
        double rayLengthSquared = delta.dotProduct(delta, false);
        double lineLengthSquared = line.dotProduct(line, false);
        double alongRay = delta.dotProduct(between, false);
        double alongLine = line.dotProduct(between, false);
        double cross = delta.dotProduct(line, false);
        if (rayLengthSquared < 1.0E-6) {
            return -1;
        }

        //Fractions along the ray and line of the closest points, clamped to their ends.
        double rayFraction;
        double lineFraction;
        if (lineLengthSquared < 1.0E-6) {
            rayFraction = clamp01(-alongRay / rayLengthSquared);
            lineFraction = 0;
        } else {
            double denominator = rayLengthSquared * lineLengthSquared - cross * cross;
            rayFraction = denominator > 1.0E-6 ? clamp01((cross * alongLine - alongRay * lineLengthSquared) / denominator) : 0;
            lineFraction = (cross * rayFraction + alongLine) / lineLengthSquared;
            if (lineFraction < 0) {
                lineFraction = 0;
                rayFraction = clamp01(-alongRay / rayLengthSquared);
            } else if (lineFraction > 1) {
                lineFraction = 1;
                rayFraction = clamp01((cross - alongRay) / rayLengthSquared);
            }
        }
        Point3D rayPoint = start.copy().addScaled(delta, rayFraction);
        Point3D linePoint = lineStart.copy().addScaled(line, lineFraction);
        return rayPoint.isDistanceToCloserThan(linePoint, CUT_RADIUS) ? rayFraction * Math.sqrt(rayLengthSquared) : -1;
    }

    /**
     * Returns how far the point is from the nearest point on the segment from lineStart to lineEnd.
     */
    private static double getDistanceToSegment(Point3D point, Point3D lineStart, Point3D lineEnd) {
        Point3D line = lineEnd.copy().subtract(lineStart);
        double lineLengthSquared = line.dotProduct(line, false);
        double fraction = lineLengthSquared > 1.0E-6 ? clamp01(point.copy().subtract(lineStart).dotProduct(line, false) / lineLengthSquared) : 0;
        return point.distanceTo(lineStart.copy().addScaled(line, fraction));
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    /**
     * Returns true if the player is holding something sharp enough to cut a grapple line: a pack item with
     * canCutGrappleLines set, or an item in the grappleCuttingItems config.
     */
    public static boolean canCutLines(IWrapperPlayer player) {
        AItemBase heldItem = player.getHeldItem();
        if (heldItem instanceof AItemPack && ((AItemPack<?>) heldItem).definition.general.canCutGrappleLines) {
            return true;
        }
        IWrapperItemStack heldStack = player.getHeldStack();
        return !heldStack.isEmpty() && ConfigSystem.settings.general.grappleCuttingItems.value.contains(InterfaceManager.coreInterface.getStackItemName(heldStack));
    }

    /**
     * Client side: sets the multipart the hook is stuck in and where on it, or clears it if the ID is null.  See {@link PacketPartGrappleHook}.
     */
    public void setHookedMultipart(UUID multipartID, Point3D offset) {
        hookedMultipartID = multipartID;
        clientHookedMultipart = null;
        if (offset != null) {
            hookOffset.set(offset);
        }
    }

    /**
     * Returns the total brake hold on the vehicle's grounded wheels, in kg*m/ticks^2.  For debug logging.
     */
    private static double getBrakeHold(EntityVehicleF_Physics vehicle) {
        double brake = 0;
        for (PartGroundDevice groundDevice : vehicle.groundDeviceCollective.groundedGroundDevices) {
            brake += vehicle.getWheelBrakingForce(groundDevice);
        }
        return brake;
    }

    /**
     * Returns true if the hook is stuck in something and the winch is reeling the line in.
     */
    public boolean isReeling() {
        return isAttached() && reelVar.isActive;
    }

    /**
     * Returns the multipart the hook is stuck in, or null if it isn't stuck in one or the client doesn't have it.
     * The server's own one, or the client's copy of it.
     */
    private AEntityF_Multipart<?> getHookedMultipart() {
        if (getState() != STATE_VEHICLE) {
            return null;
        }
        if (!world.isClient()) {
            return hookedMultipart;
        }
        if (hookedMultipartID == null) {
            return null;
        }
        if (clientHookedMultipart == null || !clientHookedMultipart.isValid) {
            //Looked up again if it's gone, as it can unload and reload while we hold it.
            AEntityA_Base found = world.getEntity(hookedMultipartID);
            clientHookedMultipart = found instanceof AEntityF_Multipart ? (AEntityF_Multipart<?>) found : null;
        }
        return clientHookedMultipart;
    }

    /**
     * Returns where the line pulls from this tick.  On a multipart, that's worked out from our own copy of it.  A client's
     * copy can be a way off the server's while it's being pulled, so the line pulls where the hook is drawn.
     */
    private Point3D getLineAnchor() {
        AEntityF_Multipart<?> hooked = getHookedMultipart();
        return hooked != null ? getHookPosition(hooked) : getAnchor();
    }

    /**
     * Returns where the hook is on the passed-in multipart it's stuck in, in world coordinates.
     */
    private Point3D getHookPosition(AEntityF_Multipart<?> hooked) {
        return hookOffset.copy().rotate(hooked.orientation).add(hooked.position);
    }

    private Point3D getAnchor() {
        return new Point3D(anchorXVar.currentValue, anchorYVar.currentValue, anchorZVar.currentValue);
    }

    /**Sets the hook position.  Only sends packets for the axes that changed.**/
    private void setAnchor(Point3D anchor) {
        anchorXVar.setTo(anchor.x, true);
        anchorYVar.setTo(anchor.y, true);
        anchorZVar.setTo(anchor.z, true);
    }

    /**
     * Client side: remembers the hook position for this tick and the last one, so the render can
     * interpolate between them instead of the hook jumping several blocks a tick in flight.
     */
    private void updateRenderAnchor() {
        Point3D anchor = getAnchor();
        if (!lineOutVar.isActive) {
            //No line out.  Forget the last hook position, so the next launch doesn't sweep in from it.
            renderAnchor = null;
            prevRenderAnchor = null;
        } else if (renderAnchor == null) {
            //Line just went out.  Start the interpolation at the hook's first position.
            renderAnchor = anchor;
            prevRenderAnchor = anchor.copy();
        } else {
            prevRenderAnchor.set(renderAnchor);
            renderAnchor.set(anchor);
        }
    }

    private Point3D getRenderAnchor(float partialTicks) {
        AEntityF_Multipart<?> hooked = getHookedMultipart();
        if (hooked != null) {
            //Stuck in a multipart, so draw it on our copy of that, where it's being drawn, rather than where the server has it.
            hooked.getInterpolatedOrientation(hookedRenderOrientation, partialTicks);
            return hookOffset.copy().rotate(hookedRenderOrientation).add(hooked.prevPosition.copy().interpolate(hooked.position, partialTicks));
        }
        return renderAnchor != null ? prevRenderAnchor.copy().interpolate(renderAnchor, partialTicks) : getAnchor();
    }

    @Override
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        //The cable variables are evaluated at render time, so the cable tracks the interpolated
        //part position and doesn't lag a tick behind the vehicle.
        switch (variable) {
            case (VAR_BODY_YAW):
                return new ComputedVariable(this, variable, partialTicks -> getAim(partialTicks)[0], true);
            case (VAR_BODY_PITCH):
                return new ComputedVariable(this, variable, partialTicks -> getAim(partialTicks)[1], true);
            case (VAR_LINE_YAW):
                return new ComputedVariable(this, variable, partialTicks -> getAim(partialTicks)[2], true);
            case (VAR_LINE_PITCH):
                return new ComputedVariable(this, variable, partialTicks -> getAim(partialTicks)[3], true);
            case (VAR_LENGTH):
                return new ComputedVariable(this, variable, partialTicks -> getAim(partialTicks)[4], true);
            case (VAR_SNAPPED):
                return new ComputedVariable(this, variable, partialTicks -> snapped ? 1 : 0, false);
            case (VAR_CUT):
                return new ComputedVariable(this, variable, partialTicks -> cut ? 1 : 0, false);
            default:
                return super.createComputedVariable(variable, createDefaultIfNotPresent);
        }
    }

    /**
     * Returns {body yaw, body pitch, line yaw, line pitch, cable length} for the launcher.
     * Line angles are relative to the turned body.
     * The whole launcher turns by the body angles about the grapple pivot point.  The cable and hook then turn
     * by the line angles about the part's origin (the barrel tip), so the cable always leaves from the tip,
     * whether or not the launcher itself can turn.
     * While the line is out, a turning launcher points from its (moved) tip straight at the hook,
     * leaving the line angles near zero.  A fixed launcher stays put and the line angles do the work.
     * With the line in, a turning launcher points where the hook would be fired: where the controller is
     * looking, kept inside maxAimAngleHorizontal and maxAimAngleVertical.
     */
    private double[] getAim(float partialTicks) {
        if (aimTick == ticksExisted && aimPartialTicks == partialTicks) {
            return aim;
        }
        aimTick = ticksExisted;
        aimPartialTicks = partialTicks;

        Point3D pivot = definition.grapple.grapplePivotPoint;
        boolean fixed = isFixedVar.isActive;
        if (lineOutVar.isActive) {
            Point3D launcherPosition = prevPosition.copy().interpolate(position, partialTicks);
            Point3D anchorLocal = getRenderAnchor(partialTicks).subtract(launcherPosition).reOrigin(orientation);

            double bodyYaw = 0;
            double bodyPitch = 0;
            if (!fixed) {
                //The tip moves as the launcher turns about the pivot, so aim from the pivot first,
                //then re-aim from where that puts the tip.  A few passes converge well inside a pixel.
                Point3D direction = anchorLocal.copy().subtract(pivot);
                for (int i = 0; i < 3; ++i) {
                    bodyYaw = getYaw(direction);
                    bodyPitch = getPitch(direction);
                    direction = anchorLocal.copy().subtract(getTip(pivot, bodyYaw, bodyPitch));
                }
                bodyYaw = getYaw(direction);
                bodyPitch = getPitch(direction);
            }

            //Line from the tip to the hook, in the turned body's coordinates.
            Point3D line = anchorLocal.subtract(getTip(pivot, bodyYaw, bodyPitch));
            double length = line.length();
            line.rotate(new RotationMatrix().setToAxisAngle(0, 1, 0, -bodyYaw)).rotate(new RotationMatrix().setToAxisAngle(1, 0, 0, -bodyPitch));
            aim[0] = bodyYaw;
            aim[1] = bodyPitch;
            aim[2] = getYaw(line);
            aim[3] = getPitch(line);
            aim[4] = length;
            return aim;
        }

        double[] body = getIdleBodyAim();
        aim[0] = body[0];
        aim[1] = body[1];
        if (dangleValid) {
            //Hanging off the tip.  Same as the line being out, but the body keeps following the aim.
            Point3D line = prevDangleOffset.copy().interpolate(dangleOffset, partialTicks).reOrigin(orientation);
            aim[4] = line.length();
            line.rotate(new RotationMatrix().setToAxisAngle(0, 1, 0, -body[0])).rotate(new RotationMatrix().setToAxisAngle(1, 0, 0, -body[1]));
            aim[2] = getYaw(line);
            aim[3] = getPitch(line);
        } else {
            aim[2] = 0;
            aim[3] = 0;
            aim[4] = 0;
        }
        return aim;
    }

    /**
     * Returns {body yaw, body pitch} for the launcher while the line is in.
     */
    private double[] getIdleBodyAim() {
        if (isFixedVar.isActive) {
            return new double[] { 0, 0 };
        }
        //Point the barrel the way the hook would be fired, so it stays inside maxAimAngleHorizontal and maxAimAngleVertical.
        Point3D direction = getCachedLaunchDirection().reOrigin(orientation);
        return new double[] { getYaw(direction), getPitch(direction) };
    }

    /**
     * Swings the hook hanging idleLength below the barrel tip while the line is in.  Client only.
     * The hook is a point mass on a rigid line: it carries on at its speed, less some damping, falls,
     * then is pulled back to idleLength from the tip, so the vehicle moving the tip swings it.
     */
    private void updateDangle() {
        double length = idleLengthVar.currentValue;
        if (lineOutVar.isActive || length <= 0) {
            dangleValid = false;
            return;
        }
        double[] body = getIdleBodyAim();
        Point3D tip = getTip(definition.grapple.grapplePivotPoint, body[0], body[1]).rotate(orientation).add(position);
        if (!dangleValid || !dangleHook.isDistanceToCloserThan(tip, length * 2)) {
            //Just reeled in, or teleported.  Start hanging straight down.
            dangleHook.set(tip).add(0, -length, 0);
            prevDangleHook.set(dangleHook);
            dangleOffset.set(0, -length, 0);
            prevDangleOffset.set(dangleOffset);
            dangleValid = true;
            return;
        }
        Point3D velocity = dangleHook.copy().subtract(prevDangleHook).scale(DANGLE_DAMPING);
        prevDangleHook.set(dangleHook);
        dangleHook.add(velocity).add(0, -DANGLE_GRAVITY, 0);
        Point3D line = dangleHook.copy().subtract(tip);
        double distance = line.length();
        if (distance > 1.0E-3) {
            dangleHook.set(tip).addScaled(line, length / distance);
        } else {
            dangleHook.set(tip).add(0, -length, 0);
        }
        prevDangleOffset.set(dangleOffset);
        dangleOffset.set(dangleHook).subtract(tip);
    }

    /**Returns where the barrel tip (the part origin) ends up after aiming, in local coordinates.**/
    private static Point3D getTip(Point3D pivot, double yaw, double pitch) {
        return pivot.copy().invert().rotate(new RotationMatrix().setToAxisAngle(1, 0, 0, pitch)).rotate(new RotationMatrix().setToAxisAngle(0, 1, 0, yaw)).add(pivot);
    }

    /**Yaw that turns +Z towards the local vector, in degrees.  Zero for a zero vector.**/
    private static double getYaw(Point3D local) {
        return local.x == 0 && local.z == 0 ? 0 : Math.toDegrees(Math.atan2(local.x, local.z));
    }

    /**Pitch about +X that tilts +Z towards the local vector, in degrees.  Zero for a zero vector.**/
    private static double getPitch(Point3D local) {
        double length = local.length();
        return length > 1.0E-3 ? -Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, local.y / length)))) : 0;
    }

    /**The vehicle's wheels, split into the launcher end and the far end of the vehicle.  Spares don't count.**/
    private class WheelSplit {
        final List<APart> launcherEnd = new ArrayList<>();
        final List<APart> farEnd = new ArrayList<>();
        /**1 if the launcher, or the end asked for, is ahead of the middle of the wheels, -1 if behind.**/
        final double side;
        /**Middle of the wheels front to back, in the vehicle's frame.**/
        final double centerZ;

        WheelSplit() {
            this(0);
        }

        /**
         * @param side 1 or -1 to split the wheels about that end instead of the launcher's, as the climb does
         * with the end facing the wall.  0 for the launcher's end.
         */
        WheelSplit(double side) {
            List<APart> wheels = new ArrayList<>();
            double totalZ = 0;
            for (APart other : vehicleOn.allParts) {
                if (other instanceof PartGroundDevice && !other.placementDefinition.isSpare) {
                    wheels.add(other);
                    totalZ += toVehicleLocal(other.position).z;
                }
            }
            centerZ = wheels.isEmpty() ? 0 : totalZ / wheels.size();
            this.side = side != 0 ? Math.signum(side) : (toVehicleLocal(position).z >= centerZ ? 1 : -1);
            for (APart wheel : wheels) {
                ((toVehicleLocal(wheel.position).z - centerZ) * this.side > 0 ? launcherEnd : farEnd).add(wheel);
            }
        }

        /**The wheel in the list whose bottom is lowest.  The list must not be empty.**/
        APart getLowest(List<APart> wheels) {
            APart lowest = wheels.get(0);
            for (APart wheel : wheels) {
                if (wheel.position.y - wheel.getHeight() / 2D < lowest.position.y - lowest.getHeight() / 2D) {
                    lowest = wheel;
                }
            }
            return lowest;
        }

        /**Radius of the biggest wheel in the list.**/
        double getLargestRadius(List<APart> wheels) {
            double radius = 0;
            for (APart wheel : wheels) {
                radius = Math.max(radius, wheel.getHeight() / 2D);
            }
            return radius;
        }

    }

    /**
     * The flat plane the climb is built around, in the vehicle's frame.  It's at the lowest bottom of the wheels
     * and block collision boxes, and reaches as far as they do front to back and side to side.  See {@link ClimbPath}.
     */
    private class ClimbPlane {
        /**Middle of the plane's edge at the end facing the wall.**/
        final Point3D frontLocal;
        /**Middle of the plane's edge at the far end.**/
        final Point3D rearLocal;
        /**How far the plane reaches to either side.**/
        final double minX;
        final double maxX;

        ClimbPlane(WheelSplit wheels) {
            double bottom = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            double lowX = Double.MAX_VALUE;
            double highX = -Double.MAX_VALUE;
            List<APart> allWheels = new ArrayList<>(wheels.launcherEnd);
            allWheels.addAll(wheels.farEnd);
            for (APart wheel : allWheels) {
                Point3D center = toVehicleLocal(wheel.position);
                double radius = wheel.getHeight() / 2D;
                double halfWidth = wheel.getWidth() / 2D;
                bottom = Math.min(bottom, center.y - radius);
                minZ = Math.min(minZ, center.z - radius);
                maxZ = Math.max(maxZ, center.z + radius);
                lowX = Math.min(lowX, center.x - halfWidth);
                highX = Math.max(highX, center.x + halfWidth);
            }
            Point3D heading = new Point3D(vehicleOn.headingVector.x, 0, vehicleOn.headingVector.z);
            if (heading.length() > 1.0E-3) {
                heading.normalize();
                for (BoundingBox box : vehicleOn.allBlockCollisionBoxes) {
                    //Boxes don't turn with us, so take how far they reach along and across our heading.
                    Point3D center = toVehicleLocal(box.globalCenter);
                    double depth = Math.abs(heading.x) * box.widthRadius + Math.abs(heading.z) * box.depthRadius;
                    double width = Math.abs(heading.z) * box.widthRadius + Math.abs(heading.x) * box.depthRadius;
                    bottom = Math.min(bottom, center.y - box.heightRadius);
                    minZ = Math.min(minZ, center.z - depth);
                    maxZ = Math.max(maxZ, center.z + depth);
                    lowX = Math.min(lowX, center.x - width);
                    highX = Math.max(highX, center.x + width);
                }
            }
            minX = lowX;
            maxX = highX;
            double middleX = (minX + maxX) / 2D;
            frontLocal = new Point3D(middleX, bottom, wheels.side > 0 ? maxZ : minZ);
            rearLocal = new Point3D(middleX, bottom, wheels.side > 0 ? minZ : maxZ);
        }
    }

    /**
     * How a line pulls on one end's tow chain this tick.  See {@link #getChainPull}.
     */
    private static class ChainPull {
        /**The motion to add to the vehicle at the front of the chain.**/
        final Point3D motion;
        /**The line's tension, in kg*m/ticks^2.**/
        final Point3D force;
        /**How much of the tension it takes just to hold the chain to the line, before any reeling in, in kg*m/ticks^2.  This is what can snap the line.**/
        final double holdTension;

        ChainPull(Point3D motion, Point3D force, double holdTension) {
            this.motion = motion;
            this.force = force;
            this.holdTension = holdTension;
        }
    }

    /**
     * A pull from a grapple line, on the vehicle the hook is in or one further up its tow chain.
     * Queued on that vehicle, which applies it in its own update.
     */
    public static class LinePull {
        /**Where the line pulls, in world coordinates.**/
        final Point3D point;
        /**The line's tension, in kg*m/ticks^2.**/
        final Point3D force;
        /**What the vehicle turns about, in world coordinates, or null to work it out from the wheels.**/
        final Point3D pivot;
        /**Motion to add to the vehicle, or null if it only turns.**/
        final Point3D motion;
        /**The most the line can turn the vehicle, in degrees per tick.  Already divided between the lines on it.**/
        final double maxTurn;
        /**How many lines' worth swing the vehicle the way this one does.  See {@link LineShare#swing}.**/
        final double swingShare;
        /**The motion of the front of the chain on the other end of the line, to swing after, or null to swing after our own motion.**/
        final Point3D otherMotion;

        LinePull(Point3D point, Point3D force, Point3D pivot, Point3D motion, double maxTurn, double swingShare, Point3D otherMotion) {
            this.point = point;
            this.force = force;
            this.pivot = pivot;
            this.motion = motion;
            this.maxTurn = maxTurn;
            this.swingShare = swingShare;
            this.otherMotion = otherMotion;
        }
    }

    /**Something the hook ran into.  Neither entity nor multipart set means a block.**/
    private static class Hit {
        final Point3D position;
        IWrapperEntity entity;
        AEntityF_Multipart<?> multipart;
        BoundingBox box;

        Hit(Point3D position) {
            this.position = position;
        }
    }
}
