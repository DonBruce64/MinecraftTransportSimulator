package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.BlockHitResult;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.BoundingBoxHitResult;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.EntityInteractResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TowingConnection;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartGrapple.LinePull;
import minecrafttransportsimulator.items.instances.ItemPartGrapple;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.LanguageSystem;

/**
 * A grappling-hook launcher.  Firing launches the hook towards whatever is under the controller's
 * crosshair (or along the part's +Z axis with no controller).  It flies until it sticks into the first
 * block, entity or vehicle it hits, or runs out of line and is reeled back in.  Blocks hold the vehicle,
 * entities get reeled in, and vehicles share the pull by mass.
 * <p>
 * The hook is not a bullet.  Bullets are client-side particles that never sync or save, but the line
 * has to hold the same on every client and across reloads, so the server owns the hook and syncs it
 * to clients through variables.  Pack JSON drives and renders it through those same variables.
 * <p>
 * The line only ever pulls, and both of its ends get the same physics.  When it's taut, whatever is on
 * each end can't move further apart and gets pulled back to the line length.  A vehicle is pulled with
 * its whole tow chain, and the chain's tyres and brakes hold back what they can.  The line's torque turns
 * the vehicle so the point the line is on swings round towards the other end: it twists and pitches up
 * about its far wheels, or hangs below the line once clear of the ground.  At the top of a wall, the
 * launcher vehicle is tipped over the lip, and the mantle takes over and hauls it up and onto the top.
 * <p>
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

    public static final int STATE_IDLE = 0;
    public static final int STATE_FLYING = 1;
    public static final int STATE_BLOCK = 2;
    public static final int STATE_ENTITY = 3;
    public static final int STATE_VEHICLE = 4;

    public static final int MANTLE_NONE = 0;
    /**Hanging at the lip for {@link #MANTLE_PAUSE_TICKS}.**/
    public static final int MANTLE_PAUSE = 1;
    /**Lifting straight up while tilting towards the hook, until the launcher-end wheels clear the top.**/
    public static final int MANTLE_LIFT = 2;
    /**Moving towards the hook while tilting down to level, until the far wheels are over the top.**/
    public static final int MANTLE_ROLL = 3;

    /**Fraction of any over-stretch pulled back each tick.  Higher is a stiffer line.**/
    private static final double STIFFNESS = 0.3;
    /**Cap on the pull-back speed from stretch, in blocks per tick, so the line can't fling anything.**/
    private static final double MAX_PULL = 0.6;
    /**How many times the range the two ends can be pulled apart before the line snaps.**/
    private static final double SNAP_FACTOR = 1.5;
    /**Cap on the pitch added by the wall-top assist and the mantle, in degrees per tick.**/
    private static final double MAX_TILT_RATE = 4;
    /**Fraction of the angle error to pointing along the line corrected each tick by the wall-top assist.**/
    private static final double HANG_ASSIST = 0.15;
    /**Cap on how fast a line can turn a vehicle on either end of it, in degrees per tick.**/
    private static final double MAX_PULL_TURN = 6;
    /**Turns smaller than this, in degrees, aren't applied to a vehicle on the line.**/
    private static final double MIN_PULL_TURN = 1.0E-3;
    /**A hook closer than this to the middle of a hooked vehicle's wheels, front to back, turns it about all its grounded wheels.**/
    private static final double PULL_PIVOT_END_DISTANCE = 0.25;
    /**How often the server checks the anchor block still exists, in ticks.**/
    private static final int ANCHOR_CHECK_INTERVAL = 10;

    /**How long the vehicle hangs at the lip before the mantle can start, in ticks.**/
    private static final int MANTLE_PAUSE_TICKS = 10;
    /**How fast the mantle lifts and rolls the vehicle, in blocks per tick.**/
    private static final double MANTLE_SPEED = 1D / 16D;
    /**Pitch the vehicle is tilted to, in degrees from level, by the time its launcher-end wheels reach the top.**/
    private static final double MANTLE_ROLL_PITCH = 45;
    /**How far above the top the launcher-end wheels are carried, in blocks, so they don't catch the lip.**/
    private static final double MANTLE_CLEARANCE = 1D / 16D;
    /**The vehicle counts as climbing a wall when its launcher end points further up than this (sine of about 37 degrees).**/
    private static final double MANTLE_CLIMB_STEEPNESS = 0.6;
    /**The line has stopped lifting the vehicle once it points up less than this (sine of 30 degrees).**/
    private static final double MANTLE_FLAT_LINE = 0.5;
    /**How many 1/16 block steps ahead of the wheels we look for the wall face.  48 is 3 blocks.**/
    private static final int MANTLE_FACE_SEARCH = 48;
    /**How many blocks above the wheels we look for the top of the wall.**/
    private static final int MANTLE_LIP_SEARCH = 4;

    //Modifiable properties.
    private final ComputedVariable rangeVar;
    private final ComputedVariable hookSpeedVar;
    private final ComputedVariable reelSpeedVar;
    private final ComputedVariable minLengthVar;
    private final ComputedVariable maxAimAngleVar;
    private final ComputedVariable hookDamageVar;
    private final ComputedVariable isFixedVar;
    private final ComputedVariable disableBlockAttachVar;
    private final ComputedVariable disableEntityAttachVar;
    private final ComputedVariable disableVehicleAttachVar;
    private final ComputedVariable disableMantleVar;

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
    private final ComputedVariable mantleLipVar;
    private final ComputedVariable mantleEdgeXVar;
    private final ComputedVariable mantleEdgeZVar;
    private final ComputedVariable mantleYawVar;

    //Server-only hook state that can't live in a variable.  Lost on reload, which drops flying hooks and hooks in entities.
    private boolean hookDataValid;
    private final Point3D hookPosition = new Point3D();
    private final Point3D hookVelocity = new Point3D();
    private IWrapperEntity hookedEntity;
    private AEntityF_Multipart<?> hookedMultipart;
    /**Where the hook sits on what it's stuck in.  World-aligned for entities, local for multiparts.**/
    private final Point3D hookOffset = new Point3D();
    private int mantlePauseTicks;

    //Client-only hook positions for the last two ticks, so the render can interpolate.
    private Point3D prevRenderAnchor;
    private Point3D renderAnchor;
    private final double[] aim = new double[5];
    private long aimTick = -1;
    private float aimPartialTicks = -1;
    //Launch direction for the current tick, so the turret and crosshair don't raycast every frame.
    private final Point3D launchDirection = new Point3D();
    private long launchDirectionTick = -1;

    public PartGrapple(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, ItemPartGrapple item, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, item, data);
        addVariable(this.rangeVar = new ComputedVariable(this, "range"));
        addVariable(this.hookSpeedVar = new ComputedVariable(this, "hookSpeed"));
        addVariable(this.reelSpeedVar = new ComputedVariable(this, "reelSpeed"));
        addVariable(this.minLengthVar = new ComputedVariable(this, "minLength"));
        addVariable(this.maxAimAngleVar = new ComputedVariable(this, "maxAimAngle"));
        addVariable(this.hookDamageVar = new ComputedVariable(this, "hookDamage"));
        addVariable(this.isFixedVar = new ComputedVariable(this, "isFixed"));
        addVariable(this.disableBlockAttachVar = new ComputedVariable(this, "disableBlockAttach"));
        addVariable(this.disableEntityAttachVar = new ComputedVariable(this, "disableEntityAttach"));
        addVariable(this.disableVehicleAttachVar = new ComputedVariable(this, "disableVehicleAttach"));
        addVariable(this.disableMantleVar = new ComputedVariable(this, "disableMantle"));

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
        addVariable(this.mantleLipVar = new ComputedVariable(this, "grapple_mantle_lip", data));
        addVariable(this.mantleEdgeXVar = new ComputedVariable(this, "grapple_mantle_edge_x", data));
        addVariable(this.mantleEdgeZVar = new ComputedVariable(this, "grapple_mantle_edge_z", data));
        addVariable(this.mantleYawVar = new ComputedVariable(this, "grapple_mantle_yaw", data));
    }

    @Override
    public void setVariableDefaults() {
        super.setVariableDefaults();
        rangeVar.setTo(definition.grapple.range, false);
        hookSpeedVar.setTo(definition.grapple.hookSpeed, false);
        reelSpeedVar.setTo(definition.grapple.reelSpeed, false);
        minLengthVar.setTo(definition.grapple.minLength, false);
        maxAimAngleVar.setTo(definition.grapple.maxAimAngle, false);
        hookDamageVar.setTo(definition.grapple.hookDamage, false);
        isFixedVar.setTo(definition.grapple.isFixed ? 1 : 0, false);
        disableBlockAttachVar.setTo(definition.grapple.disableBlockAttach ? 1 : 0, false);
        disableEntityAttachVar.setTo(definition.grapple.disableEntityAttach ? 1 : 0, false);
        disableVehicleAttachVar.setTo(definition.grapple.disableVehicleAttach ? 1 : 0, false);
        disableMantleVar.setTo(definition.grapple.disableMantle ? 1 : 0, false);
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
     * Called by the vehicle we are on, on both client and server, once per tick just before it moves.
     */
    public void updateLine() {
        //The server owns the hook.  It flies it, decides hits and line length, and syncs them to clients.
        if (!world.isClient()) {
            updateServerHook();
        } else {
            updateRenderAnchor();
        }

        //Both sides run the vehicle's side of the line so clients don't have to wait on server corrections.
        //How much of the pull the vehicle takes is decided by the server at attach time.
        //The mantle moves our vehicle itself, so it only runs when we're at the front of our tow chain.
        if (isAttached() && selfShareVar.currentValue > 0) {
            if (getMantle() != MANTLE_NONE && vehicleOn.towedByConnection == null) {
                updateMantle();
            } else {
                applyLineForces(getAnchor(), ropeVar.currentValue, selfShareVar.currentValue);
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
                setAnchor(hookOffset.copy().rotate(hookedMultipart.orientation).add(hookedMultipart.position));
                break;
            }
        }

        Point3D anchor = getAnchor();
        if (!anchor.isDistanceToCloserThan(position, rangeVar.currentValue * SNAP_FACTOR)) {
            //Pulled or teleported too far apart.  The line snaps.
            release();
            return;
        }

        double rope = ropeVar.currentValue;
        if (reelVar.isActive) {
            rope -= reelSpeedVar.currentValue / 20D;
        } else if (slackVar.isActive) {
            rope += reelSpeedVar.currentValue / 20D;
        }
        if (getMantle() != MANTLE_NONE) {
            //The mantle carries the vehicle, not the line.  Keep the line just taut, so it doesn't yank
            //the vehicle when the mantle hands back to it.
            rope = position.distanceTo(anchor);
        }
        ropeVar.setTo(clampRope(rope), true);

        //Pull whatever we're stuck in.  The vehicle's share is applied on both sides in updateLine.
        double targetShare = 1 - selfShareVar.currentValue;
        if (state == STATE_ENTITY) {
            pullEntity(hookedEntity, anchor, ropeVar.currentValue);
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
        Point3D end = getCachedLaunchDirection().scale(rangeVar.currentValue).add(position);
        Hit hit = findHit(position, end);
        return hit != null ? hit.position : end;
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
            //Find what the crosshair is on and aim the hook from the launcher at that point.
            //This way the hook goes where the controller is looking, even though the launcher is offset from their eyes.
            Point3D eye = controller.getEyePosition().copy();
            Point3D eyeEnd = eye.copy().add(controller.getLineOfSight(range));
            Hit aimHit = findHit(eye, eyeEnd);
            target = aimHit != null ? aimHit.position : eyeEnd;
        } else {
            target = new Point3D(0, 0, range).rotate(orientation).add(position);
        }

        Point3D direction = target.subtract(position).normalize();
        double maxAimAngle = maxAimAngleVar.currentValue;
        if (maxAimAngle > 0) {
            //Keep the shot inside the cone about the launcher's +Z axis.
            Point3D forward = new Point3D(0, 0, 1).rotate(orientation);
            double angle = Math.toDegrees(Math.acos(forward.dotProduct(direction, true)));
            if (angle > maxAimAngle) {
                Point3D turnAxis = forward.crossProduct(direction);
                if (turnAxis.length() < 1.0E-6) {
                    //Aimed straight backwards, so any axis square to +Z will do.
                    turnAxis.set(0, 1, 0).rotate(orientation);
                }
                direction.set(forward).rotate(new RotationMatrix().setToAxisAngle(turnAxis.normalize(), maxAimAngle));
            }
        }
        return direction;
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
            //Entities are light next to a vehicle, so the vehicle doesn't feel them.  We just reel them in.
            hookedEntity = hit.entity;
            hookOffset.set(hit.position).subtract(hit.entity.getPosition());
            state = STATE_ENTITY;
            selfShare = 0;
        } else if (hit.multipart != null) {
            //Store the hit in the other entity's local coordinates, so the hook stays put as it turns.
            hookedMultipart = hit.multipart;
            hookOffset.set(hit.position).subtract(hit.multipart.position).reOrigin(hit.multipart.orientation);
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

    private void release() {
        hookDataValid = false;
        hookedEntity = null;
        hookedMultipart = null;
        fireVar.setTo(0, true);
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

        //Hook the same boxes bullets can hit, as well as the ones entities stand on.
        EntityInteractResult multipartHit = world.getMultipartEntityIntersect(start, end, vehicleOn, CollisionType.ENTITY, CollisionType.ATTACK, CollisionType.BULLET);
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
     */
    private void pullEntity(IWrapperEntity entity, Point3D anchor, double rope) {
        Point3D toLauncher = position.copy().subtract(anchor);
        double distance = toLauncher.length();
        if (distance <= rope || distance < 1.0E-3 || entity.getEntityRiding() != null) {
            return;
        }
        Point3D lineNormal = toLauncher.scale(1D / distance);
        Point3D motionAdded = new Point3D();
        double radialSpeed = entity.getVelocity().dotProduct(lineNormal, false);
        if (radialSpeed < 0) {
            motionAdded.addScaled(lineNormal, -radialSpeed);
        }
        motionAdded.addScaled(lineNormal, Math.min((distance - rope) * STIFFNESS, MAX_PULL));
        //This marks the motion as changed, which players need as their movement is client-side.
        entity.applyMotion(motionAdded);
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
        Point3D[] pull = getChainPull(other, toLauncher.scale(1D / distance), distance - rope, share);
        queueChainPull(other, anchor, pull[1], pull[0]);
    }

    /**
     * Works out how much a line pulls on a tow chain.  Returns {the motion to add to the vehicle at the front of
     * the chain, the line's tension in kg*m/ticks^2}.  The same on both ends of the line.
     * 
     * The line stops the chain moving further away, and pulls it back towards the line length.  The
     * tension is the force required with the whole chain's mass.  The chain's tyres and brakes resist
     * what they can of the pull back, and the rest moves it.  If the line is lifting the vehicle it's on, that vehicle presses down
     * less, so its tyres grip less. 
     * 
     * Tyres and brakes don't resist when the line is stopping the chain from moving. When the chain is
     * moving away, friction is already slowing it down, so it helps the line instead.
     *
     * @param hooked the vehicle in the chain the line is on.
     * @param towards which way the line pulls the chain.  Must be normalized.
     * @param stretch how far past its length the line is pulled, in blocks.
     * @param share how much of the line's pull the chain takes, from 0 to 1.
     */
    private static Point3D[] getChainPull(EntityVehicleF_Physics hooked, Point3D towards, double stretch, double share) {
        EntityVehicleF_Physics lead = getTowChainLead(hooked);
        if (!(lead.currentMass > 0)) {
            //Massless, as packs can leave emptyMass out.  There's nothing to share the pull with, so don't move it.
            return new Point3D[] {new Point3D(), new Point3D()};
        }
        //Rigid line: stop the chain moving further away.
        Point3D stopping = new Point3D();
        double radialSpeed = lead.motion.dotProduct(towards, false);
        if (radialSpeed < 0) {
            stopping.addScaled(towards, -radialSpeed * share);
        }
        //Pull back towards the line length.  Motion gets scaled by speedFactor when applied, so undo that here.
        Point3D spring = towards.copy().scale(Math.min(stretch * STIFFNESS, MAX_PULL) * share / lead.speedFactor);

        Point3D force = stopping.copy().add(spring).scale(lead.currentMass);
        Point3D moving = spring.copy().scale(lead.currentMass);
        for (EntityVehicleF_Physics chainVehicle : getTowChain(lead)) {
            moving.subtract(getTyreHold(chainVehicle, moving, chainVehicle == hooked ? Math.max(0, force.y) : 0));
        }
        return new Point3D[] {stopping.add(moving.scale(1D / lead.currentMass)), force};
    }

    /**
     * Queues a line's pull on a tow chain, for each vehicle to apply in its own update.  The vehicle at
     * the front takes the motion, as everything it tows follows it.  A loose hitch is a joint, so the
     * vehicle behind it swings about its hookup and the pull carries on to the vehicle in front from the
     * hitch.  A mounted or restricted hitch is rigid, so the pull carries on from the same point.
     *
     * @param hooked the vehicle in the chain the line is on.
     * @param point where the line is on it, in world coordinates.
     */
    private static void queueChainPull(EntityVehicleF_Physics hooked, Point3D point, Point3D force, Point3D motion) {
        EntityVehicleF_Physics lead = getTowChainLead(hooked);
        Point3D pullPoint = point.copy();
        EntityVehicleF_Physics vehicle = hooked;
        while (vehicle != lead) {
            TowingConnection connection = vehicle.towedByConnection;
            if (!connection.hitchConnection.mounted && !connection.hitchConnection.restricted) {
                vehicle.grapplePulls.add(new LinePull(pullPoint, force, connection.hookupCurrentPosition.copy(), null));
                pullPoint = connection.hitchCurrentPosition.copy();
            }
            vehicle = connection.towingVehicle;
        }
        lead.grapplePulls.add(new LinePull(pullPoint, force, null, motion));
    }

    /**
     * Returns the vehicle at the front of the tow chain the passed-in vehicle is in, which is itself if it isn't towed.
     */
    private static EntityVehicleF_Physics getTowChainLead(EntityVehicleF_Physics vehicle) {
        //Guard against bad trailer linkings looping back on themselves, the same as the mass checks do.
        Set<EntityVehicleF_Physics> checked = new HashSet<>();
        while (vehicle.towedByConnection != null && checked.add(vehicle)) {
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
     * The tyres hold against the sideways part by their lateral friction and the weight on them.  The wheels
     * roll freely along the heading unless the brakes are on, which then hold with the same force they stop
     * the vehicle with.  The hold is never more than the force, so friction never pushes back past standing still.
     *
     * @param lift how much of the vehicle's weight the line takes, in kg*m/ticks^2.  The tyres and brakes grip less by that much.
     */
    private static Point3D getTyreHold(EntityVehicleF_Physics vehicle, Point3D force, double lift) {
        Point3D hold = new Point3D();
        Set<PartGroundDevice> grounded = vehicle.groundDeviceCollective.groundedGroundDevices;
        Point3D heading = new Point3D(vehicle.headingVector.x, 0, vehicle.headingVector.z);
        double weight = vehicle.getGravitationalForce();
        if (grounded.isEmpty() || heading.length() < 1.0E-3 || weight <= 0) {
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
        double along = force.dotProduct(heading, false);
        double across = force.dotProduct(side, false);
        hold.addScaled(heading, Math.signum(along) * Math.min(Math.abs(along), brakeGrip));
        hold.addScaled(side, Math.signum(across) * Math.min(Math.abs(across), lateralGrip));
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
        turnFromLine(vehicle, pull.point, pull.force, pull.pivot, vehicle.getBoxesOutsideBlocks(vehicle.getBoxesForWallChecks()));
    }

    /**
     * Turns the vehicle with the torque a line puts on it, so the point the line is on swings round
     * towards the line's other end.  The same on both ends of the line.  The turn is
     * the torque over the vehicle's moments, the same as its own physics:
     * -On the ground, it turns about the grounded wheels at the far end from the hook, or about the
     * hookup for a trailer.  Its weight resists lifting it.  Its tyres' lateral friction and its brakes
     * resist twisting it, so a light tug won't turn a heavy or braked vehicle.  The tyres and brakes can
     * only push back so hard, so a pull past their grip drags the vehicle along rather than tipping it further.
     * -In the air, it turns about its middle until it hangs from the hook.
     * <p>
     * It never turns past pointing the hook along the line, so it can't swing back and forth.  It never
     * tips a grounded wheel into the ground, and never swings a wheel or box into a wall.
     *
     * @param point where the line is on the vehicle, in world coordinates.
     * @param lineForce the line's tension, in kg*m/ticks^2.
     * @param pivotPoint what the vehicle turns about, in world coordinates, or null to work it out from the wheels.
     * @param freeBoxes the vehicle's boxes that aren't already inside a block, to check the turn against.
     */
    private static void turnFromLine(EntityVehicleF_Physics vehicle, Point3D point, Point3D lineForce, Point3D pivotPoint, List<BoundingBox> freeBoxes) {
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

        Point3D pivot;
        if (pivotPoint != null) {
            pivot = toLocal(vehicle, pivotPoint);
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
        } else {
            pivot = center;
        }

        //Twisting slides each grounded wheel square to its arm from the pivot.  The tyre resists the
        //sideways part of that by its lateral friction, and the brakes resist the rolling part.  The
        //further a wheel is from the pivot, the more it resists.  The line lightens us when it lifts us,
        //so the tyres and brakes grip less.
        Point3D force = lineForce.copy();
        double lift = Math.max(0, force.y);
        double twistResistance = 0;
        if (!grounded.isEmpty() && weight > 0) {
            double load = Math.max(0, weight - lift);
            for (PartGroundDevice groundDevice : grounded) {
                Point3D wheelArm = toLocal(vehicle, groundDevice.position).subtract(pivot);
                wheelArm.addScaled(up, -wheelArm.dotProduct(up, false));
                double armLength = wheelArm.length();
                if (armLength > 1.0E-3) {
                    Point3D slide = up.crossProduct(wheelArm).scale(1D / armLength);
                    double sideways = groundDevice.lateralFrictionVar.currentValue * load / grounded.size() * Math.abs(slide.x);
                    double rolling = vehicle.getWheelBrakingForce(groundDevice) * load / weight * Math.abs(slide.z);
                    twistResistance += (sideways + rolling) * armLength;
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
        //the ground holds, but twisting comes from the whole line force, as the tyres resisting it are
        //already counted in twistResistance.  Capping both left a strong sideways pull just sliding us.
        Point3D arm = hook.copy().subtract(pivot);
        Point3D lineTorque = arm.crossProduct(forceLocal);
        lineTorque.addScaled(up, arm.crossProduct(fullForceLocal).dotProduct(up, false) - lineTorque.dotProduct(up, false));
        Point3D torque = lineTorque.copy().add(center.copy().subtract(pivot).crossProduct(up.copy().scale(-weight)));
        if (!grounded.isEmpty()) {
            //The tyres stop us twisting until the line beats their friction.
            double twist = torque.dotProduct(up, false);
            double twistLeft = Math.signum(twist) * Math.max(0, Math.abs(twist) - twistResistance);
            torque.addScaled(up, twistLeft - twist);
        }

        //The line and our weight set our pitch and roll here, so ground corrections shouldn't level us while the line has hold of us.
        vehicle.pitchHeldByGrapple = true;
        vehicle.rollHeldByGrapple = true;

        //Turn by the torque over our moments, the same as our own physics does.  A moment can be 0 when a
        //pack leaves emptyMass out, which would turn us infinitely, so we don't turn about that axis.
        Point3D turn = new Point3D(getTurnRate(torque.x, vehicle.momentPitch), getTurnRate(torque.y, vehicle.momentYaw), getTurnRate(torque.z, vehicle.momentRoll));
        double angle = turn.length();
        if (!(angle > MIN_PULL_TURN) || Double.isInfinite(angle)) {
            //Too small to bother with.
            return;
        }
        Point3D axis = turn.scale(1D / angle);
        angle = Math.min(angle, MAX_PULL_TURN);
        if (axis.dotProduct(lineTorque, false) > 0 && arm.length() > 1.0E-3 && fullForceLocal.length() > 1.0E-6) {
            //The line is what's turning us, so don't turn past pointing the hook along it.
            angle = Math.min(angle, Math.toDegrees(Math.acos(arm.copy().normalize().dotProduct(fullForceLocal.copy().normalize(), true))));
        }

        //Lifting or tipping can't push a grounded wheel into the ground.  If it would, keep just the twist.
        if (pushesWheelIntoGround(vehicle, grounded, pivot, axis, angle)) {
            double twist = axis.dotProduct(up, false) * angle;
            if (!(Math.abs(twist) > MIN_PULL_TURN)) {
                return;
            }
            axis = up.copy().scale(Math.signum(twist));
            angle = Math.abs(twist);
        }

        angle = vehicle.limitTurnToWalls(freeBoxes, pivot, axis, angle);
        if (angle != 0) {
            RotationMatrix turnMatrix = new RotationMatrix().setToAxisAngle(axis, angle);
            //Vehicles turn about their origin, so shift the origin to keep the pivot where it is.
            Point3D pivotShift = pivot.copy().subtract(pivot.copy().rotate(turnMatrix));
            vehicle.motion.addScaled(pivotShift.rotate(vehicle.orientation), 1D / vehicle.speedFactor);
            vehicle.rotation.angles.add(turnMatrix.convertToAngles());
        }
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
        Point3D[] pull = getChainPull(vehicleOn, lineNormal, distance - rope, share);
        if (vehicleOn.towedByConnection != null) {
            queueChainPull(vehicleOn, position, pull[1], pull[0]);
            return;
        }
        vehicleOn.motion.add(pull[0]);

        List<BoundingBox> freeBoxes = vehicleOn.getBoxesOutsideBlocks(vehicleOn.getBoxesForWallChecks());
        if (!applyWallTopTilt(lineNormal, freeBoxes)) {
            turnFromLine(vehicleOn, position, pull[1], null, freeBoxes);
        }

        //Vehicles don't stop wheels going into walls, and the line usually pulls the vehicle towards one.
        Point3D blocked = vehicleOn.clampMotionToWalls(freeBoxes);

        //A straight line to a hook on top of a wall stops lifting once the launcher is level with the
        //hook, which leaves the wheels below the lip.  When that happens, the server hands over to the
        //mantle to haul the vehicle over the top.  These checks are cheap, so the block search in
        //tryStartMantle only runs while the vehicle is actually stuck against a wall.
        Point3D towardsHook = new Point3D(lineNormal.x, 0, lineNormal.z);
        if (!world.isClient() && !disableMantleVar.isActive && lineNormal.y < MANTLE_FLAT_LINE && towardsHook.length() > 1.0E-3 && blocked.dotProduct(towardsHook.normalize(), false) > 0) {
            tryStartMantle(towardsHook);
        }
    }

    /**
     * Pitches the vehicle over the top of a wall when only its launcher-end wheels are up on it, such as
     * the front wheels just over the lip.  The vehicle pitches about those wheels until it points along
     * the line, so the line drags it over the edge and down flat onto the top.  The line's torque alone
     * would let the far end tip back down off the edge, as the middle of the vehicle hangs past the wheels,
     * so this takes over from it here.  This runs whichever way the line points, as at the top it usually
     * runs level or downhill.  The vehicle never tilts past pointing along the line.
     *
     * @param freeBoxes the vehicle's boxes that aren't already inside a block, to check the tilt against.
     * @return true if only the launcher-end wheels are grounded, so this has turned the vehicle instead of the line's torque.
     */
    private boolean applyWallTopTilt(Point3D lineNormal, List<BoundingBox> freeBoxes) {
        //Work in the vehicle's own frame, where pitch is a turn about X, so only Y and Z matter.
        //There's no centre of mass, so take the middle of the wheels.  Without wheels, use the origin.
        Point3D launcher = toVehicleLocal(position);
        Point3D center = new Point3D();
        int wheels = 0;
        for (APart other : vehicleOn.allParts) {
            if (other instanceof PartGroundDevice && !other.placementDefinition.isSpare) {
                center.add(toVehicleLocal(other.position));
                ++wheels;
            }
        }
        if (wheels > 0) {
            center.scale(1D / wheels);
        }
        Point3D arm = launcher.copy().subtract(center);
        if (Math.hypot(arm.y, arm.z) < 0.1) {
            //Launcher sits on the middle of the vehicle, so it has no launcher end.
            return false;
        }

        //Sort the grounded wheels into the launcher end and the far end of the vehicle.
        Point3D launcherEnd = new Point3D();
        int launcherEndWheels = 0;
        for (PartGroundDevice groundDevice : vehicleOn.groundDeviceCollective.groundedGroundDevices) {
            Point3D wheel = toVehicleLocal(groundDevice.position);
            if ((wheel.z - center.z) * arm.z < 0) {
                return false;
            }
            launcherEnd.add(wheel);
            ++launcherEndWheels;
        }
        if (launcherEndWheels == 0) {
            return false;
        }

        //Tilt that would point the vehicle along the line.  Positive is nose down.
        Point3D pivot = launcherEnd.scale(1D / launcherEndWheels);
        Point3D lineLocal = lineNormal.copy().reOrigin(vehicleOn.orientation);
        double overError = wrapAngle(getTiltAngle(new Point3D(0, 0, Math.signum(arm.z))) - getTiltAngle(lineLocal));
        double tilt = Math.max(-MAX_TILT_RATE, Math.min(MAX_TILT_RATE, overError * HANG_ASSIST));

        //Turns are only checked against the collision boxes, so a tilt could swing the wheels into a wall.
        tilt = vehicleOn.limitTiltToWalls(freeBoxes, pivot, tilt);
        if (tilt != 0) {
            vehicleOn.rotation.angles.x += tilt;
            //Vehicles turn about their origin, so shift the origin to keep the pivot where it is.
            Point3D pivotShift = pivot.copy().subtract(pivot.copy().rotate(new RotationMatrix().setToAxisAngle(1, 0, 0, tilt)));
            vehicleOn.motion.addScaled(pivotShift.rotate(vehicleOn.orientation), 1D / vehicleOn.speedFactor);
        }
        vehicleOn.pitchHeldByGrapple = true;
        vehicleOn.rollHeldByGrapple = true;
        return true;
    }

    /**
     * Starts the mantle if the vehicle is climbing a wall and the top of the wall is in reach.  Server only.
     * Looks for the wall face ahead of the lowest launcher-end wheel, then for the first air block
     * above the face, which is the top of the wall.
     */
    private void tryStartMantle(Point3D towardsHook) {
        WheelSplit wheels = new WheelSplit();
        if (wheels.launcherEnd.isEmpty() || wheels.farEnd.isEmpty() || vehicleOn.headingVector.y * wheels.side < MANTLE_CLIMB_STEEPNESS) {
            return;
        }

        Point3D face = wheels.getLowestLauncherEnd().position.copy();
        Point3D step = towardsHook.copy().scale(1D / 16D);
        boolean foundFace = false;
        for (int i = 0; i < MANTLE_FACE_SEARCH && !foundFace; ++i) {
            face.add(step);
            foundFace = !world.isAir(face);
        }
        if (!foundFace) {
            return;
        }

        Point3D column = new Point3D(Math.floor(face.x) + 0.5, Math.floor(face.y) + 0.5, Math.floor(face.z) + 0.5);
        for (int i = 0; i < MANTLE_LIP_SEARCH; ++i) {
            column.y += 1;
            if (world.isAir(column)) {
                //Set the path before the phase, so clients have it by the time they start following it.
                mantleLipVar.setTo(Math.floor(column.y), true);
                mantleEdgeXVar.setTo(face.x, true);
                mantleEdgeZVar.setTo(face.z, true);
                mantleYawVar.setTo(Math.toDegrees(Math.atan2(towardsHook.x, towardsHook.z)), true);
                mantlePauseTicks = 0;
                setMantle(MANTLE_PAUSE);
                return;
            }
        }
    }

    /**
     * Hauls the vehicle over the top of a wall.  Runs on both client and server.  The mantle moves the
     * vehicle itself along a set path, so it looks the same every time and costs next to nothing a tick:
     * -Pause: hang at the lip for {@link #MANTLE_PAUSE_TICKS}.
     * -Lift: rise straight up while tilting towards the hook, pivoting on the launcher-end wheels,
     * until they clear the top.  The tilt is spread over the height left, so the vehicle reaches
     * {@link #MANTLE_ROLL_PITCH} just as the wheels clear the lip.
     * -Roll: move towards the hook with the launcher-end wheels skimming the top, tilting down to
     * level over the distance left, so the far wheels come up over the lip as the vehicle levels out.
     *
     * Lift and roll only move while reel is held.  Letting go holds the vehicle where it is, and slack
     * drops out of the mantle and back to the line.  Only the server changes phase; clients follow.
     */
    private void updateMantle() {
        boolean server = !world.isClient();
        //Nothing else moves the vehicle during the mantle, so cancel gravity, tyres and steering.
        vehicleOn.motion.set(0, 0, 0);
        vehicleOn.rotation.angles.set(0, 0, 0);
        vehicleOn.pitchHeldByGrapple = true;

        if (slackVar.isActive) {
            if (server) {
                setMantle(MANTLE_NONE);
            }
            return;
        }
        int phase = getMantle();
        if (phase == MANTLE_PAUSE) {
            if (server && ++mantlePauseTicks >= MANTLE_PAUSE_TICKS) {
                setMantle(MANTLE_LIFT);
            }
            return;
        }
        if (!reelVar.isActive) {
            return;
        }

        WheelSplit wheels = new WheelSplit();
        if (wheels.launcherEnd.isEmpty() || wheels.farEnd.isEmpty()) {
            if (server) {
                setMantle(MANTLE_NONE);
            }
            return;
        }
        double lip = mantleLipVar.currentValue;
        double wheelBottom = wheels.getLauncherEndBottom();
        //How far the launcher end points up from level, in degrees.
        double pitchUp = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, vehicleOn.headingVector.y * wheels.side))));

        Point3D move = new Point3D();
        double tilt;
        if (phase == MANTLE_LIFT) {
            double remaining = lip + MANTLE_CLEARANCE - wheelBottom;
            if (remaining <= 0) {
                if (server) {
                    setMantle(MANTLE_ROLL);
                }
                return;
            }
            double rise = Math.min(MANTLE_SPEED, remaining);
            move.y = rise;
            tilt = Math.max(0, pitchUp - MANTLE_ROLL_PITCH) * rise / remaining;
        } else {
            double yaw = Math.toRadians(mantleYawVar.currentValue);
            Point3D direction = new Point3D(Math.sin(yaw), 0, Math.cos(yaw));
            Point3D edge = new Point3D(mantleEdgeXVar.currentValue, 0, mantleEdgeZVar.currentValue);
            //Distance left until every far wheel is a radius past the wall face, so over the top.
            double remaining = -Double.MAX_VALUE;
            for (APart wheel : wheels.farEnd) {
                Point3D flatWheel = new Point3D(wheel.position.x, 0, wheel.position.z);
                remaining = Math.max(remaining, edge.copy().subtract(flatWheel).dotProduct(direction, false) + wheel.getHeight() / 2D);
            }
            if (remaining <= 0) {
                //Over the top.  The vehicle settles onto its wheels and the line takes over again.
                if (server) {
                    setMantle(MANTLE_NONE);
                }
                return;
            }
            double travel = Math.min(MANTLE_SPEED, remaining);
            move.set(direction).scale(travel);
            move.y = Math.max(-MANTLE_SPEED, Math.min(MANTLE_SPEED, lip + MANTLE_CLEARANCE - wheelBottom));
            tilt = Math.max(0, pitchUp) * travel / remaining;
        }
        //Cap the rate so a short distance left can't snap the vehicle round.  Positive tilt is nose down,
        //so flip it when the launcher is at the back.
        tilt = Math.min(tilt, MAX_TILT_RATE) * wheels.side;

        //Pivot on the launcher-end wheels, so they follow the path above and the far end swings round them.
        Point3D pivot = wheels.getLauncherEndCenter();
        vehicleOn.rotation.angles.x = tilt;
        Point3D pivotShift = pivot.copy().subtract(pivot.copy().rotate(new RotationMatrix().setToAxisAngle(1, 0, 0, tilt)));
        move.add(pivotShift.rotate(vehicleOn.orientation));
        vehicleOn.motion.set(move).scale(1D / vehicleOn.speedFactor);
    }

    private Point3D toVehicleLocal(Point3D worldPosition) {
        return worldPosition.copy().subtract(vehicleOn.position).reOrigin(vehicleOn.orientation);
    }

    /**Pitch of a local vector above the +Z axis, in degrees.**/
    private static double getTiltAngle(Point3D local) {
        return Math.toDegrees(Math.atan2(local.y, local.z));
    }

    /**Wraps an angle to -180 to 180 degrees.**/
    private static double wrapAngle(double angle) {
        return ((angle % 360) + 540) % 360 - 180;
    }

    private double clampRope(double rope) {
        return Math.max(minLengthVar.currentValue, Math.min(rangeVar.currentValue, rope));
    }

    private int getMantle() {
        return (int) mantleVar.currentValue;
    }

    private void setMantle(int phase) {
        mantleVar.setTo(phase, true);
    }

    private int getState() {
        return (int) stateVar.currentValue;
    }

    private void setState(int state) {
        stateVar.setTo(state, true);
        lineOutVar.setTo(state != STATE_IDLE ? 1 : 0, true);
        attachedVar.setTo(state >= STATE_BLOCK ? 1 : 0, true);
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
     * looking, kept inside maxAimAngle.
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

        if (fixed) {
            aim[0] = 0;
            aim[1] = 0;
        } else {
            //Point the barrel the way the hook would be fired, so it stays inside maxAimAngle.
            Point3D direction = getCachedLaunchDirection().reOrigin(orientation);
            aim[0] = getYaw(direction);
            aim[1] = getPitch(direction);
        }
        aim[2] = 0;
        aim[3] = 0;
        aim[4] = 0;
        return aim;
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
        /**1 if the launcher is ahead of the middle of the wheels, -1 if behind.**/
        final double side;

        WheelSplit() {
            List<APart> wheels = new ArrayList<>();
            double centerZ = 0;
            for (APart other : vehicleOn.allParts) {
                if (other instanceof PartGroundDevice && !other.placementDefinition.isSpare) {
                    wheels.add(other);
                    centerZ += toVehicleLocal(other.position).z;
                }
            }
            if (!wheels.isEmpty()) {
                centerZ /= wheels.size();
            }
            side = toVehicleLocal(position).z >= centerZ ? 1 : -1;
            for (APart wheel : wheels) {
                ((toVehicleLocal(wheel.position).z - centerZ) * side > 0 ? launcherEnd : farEnd).add(wheel);
            }
        }

        APart getLowestLauncherEnd() {
            APart lowest = launcherEnd.get(0);
            for (APart wheel : launcherEnd) {
                if (wheel.position.y - wheel.getHeight() / 2D < lowest.position.y - lowest.getHeight() / 2D) {
                    lowest = wheel;
                }
            }
            return lowest;
        }

        /**World height of the bottom of the lowest launcher-end wheel.**/
        double getLauncherEndBottom() {
            APart lowest = getLowestLauncherEnd();
            return lowest.position.y - lowest.getHeight() / 2D;
        }

        /**Middle of the launcher-end wheels, in the vehicle's frame.**/
        Point3D getLauncherEndCenter() {
            Point3D center = new Point3D();
            for (APart wheel : launcherEnd) {
                center.add(toVehicleLocal(wheel.position));
            }
            return center.scale(1D / launcherEnd.size());
        }
    }

    /**
     * A pull from a grapple line, on the vehicle the hook is in or one further up its tow chain.
     * Queued by the server on that vehicle, which applies it in its own update.
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

        LinePull(Point3D point, Point3D force, Point3D pivot, Point3D motion) {
            this.point = point;
            this.force = force;
            this.pivot = pivot;
            this.motion = motion;
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
