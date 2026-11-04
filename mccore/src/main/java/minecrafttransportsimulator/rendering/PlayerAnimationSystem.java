package minecrafttransportsimulator.rendering;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONPlayerAnimation;
import minecrafttransportsimulator.jsondefs.JSONPlayerAnimation.JSONPlayerGripOverride;
import minecrafttransportsimulator.jsondefs.JSONPlayerAnimation.PlayerLimb;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Resolves vehicle-defined player limb targets into Minecraft model rotations
 * and length scales. This class contains no Minecraft classes, so every game
 * interface shares selection, priority, interpolation, and trigger behavior.
 * <br><br>
 * Which entries are active, and all trigger state, is evaluated once per client
 * vehicle tick by {@link #updateVehicle(EntityVehicleF_Physics)}. Rendering only
 * reads that selection and interpolates the reach and release transitions with
 * render time (vehicle ticks plus partial ticks). Everything here runs on the
 * single client thread, so the scratch objects below are shared.
 */
public final class PlayerAnimationSystem {
    private static final int DEFAULT_TRANSITION_TICKS = 10;
    /** Vanilla's player renderer uniformly scales the model after posing it. */
    private static final double PLAYER_MODEL_SCALE = 15D / 16D;
    /** Vanilla translates the model by -1.501 before flipping its Y axis. */
    private static final double PLAYER_MODEL_ORIGIN_HEIGHT = 1.501D;
    private static final double DEFAULT_LIMB_LENGTH = 12D / 16D;
    private static final double ARM_PROXIMAL_OFFSET = 2D / 16D;
    private static final double ARM_PIVOT_X = 5D / 16D;
    private static final double ARM_PIVOT_Y = PLAYER_MODEL_ORIGIN_HEIGHT - 2D / 16D;
    private static final double ARM_HAND_CENTER_X = 1D / 16D;
    private static final double LEG_PIVOT_X = 1.9D / 16D;
    private static final double LEG_PIVOT_Y = PLAYER_MODEL_ORIGIN_HEIGHT - 12D / 16D;
    private static final Point3D DEFAULT_LIMB_SCALE = new Point3D(1, 1, 1);
    private static final PlayerLimb[] LIMBS = PlayerLimb.values();
    private static final EnumSet<PlayerLimb> NO_LIMBS = EnumSet.noneOf(PlayerLimb.class);
    /** Calls in the same tick whose partial ticks differ by less than this share one resolved result. */
    private static final float SAME_FRAME_EPSILON = 0.001F;
    /**
     * Slack, in blocks, added to the exterior distance cull for applyAfter animations that move a
     * target away from its authored position (an opening door, for example).
     */
    private static final double EXTERIOR_CULL_MARGIN = 4D;

    /**
     * States hold their vehicle strongly, so a weak map could never release them. Entries are
     * removed by {@link #removeVehicle(EntityVehicleF_Physics)} when the vehicle is removed.
     */
    private static final Map<EntityVehicleF_Physics, VehicleAnimationState> vehicleStates = new IdentityHashMap<>();
    /** The states, a subset of {@link #vehicleStates}, that have exterior entries and so may affect any nearby player. */
    private static final List<VehicleAnimationState> exteriorStates = new ArrayList<>();
    private static final Map<IWrapperPlayer, PlayerState> playerStates = new WeakHashMap<>();

    private static final PlayerLimbPose targetPose = new PlayerLimbPose();
    private static final Point3D scratchTarget = new Point3D();
    private static final Point3D scratchBestTarget = new Point3D();
    private static final Point3D scratchTorso = new Point3D();
    private static final Point3D scratchPosition = new Point3D();
    private static final Point3D scratchScale = new Point3D();
    private static final Point3D scratchAngles = new Point3D();
    private static final RotationMatrix scratchOrientation = new RotationMatrix();

    private PlayerAnimationSystem() {
    }

    /**
     * Mutable output object, intended to be cached by callers. Rotations are radians.
     */
    public static final class PlayerLimbPose {
        public double rotationX;
        public double rotationY;
        public double rotationZ;
        public double lengthScale = 1;
        public double scaleX = 1;
        public double scaleY = 1;
        public double scaleZ = 1;
        public double blend;

        private PlayerLimbPose set(PlayerLimbPose other) {
            rotationX = other.rotationX;
            rotationY = other.rotationY;
            rotationZ = other.rotationZ;
            lengthScale = other.lengthScale;
            scaleX = other.scaleX;
            scaleY = other.scaleY;
            scaleZ = other.scaleZ;
            blend = other.blend;
            return this;
        }

        private PlayerLimbPose set(double rotationX, double rotationY, double rotationZ, double lengthScale, double scaleX, double scaleY, double scaleZ, double blend) {
            this.rotationX = rotationX;
            this.rotationY = rotationY;
            this.rotationZ = rotationZ;
            this.lengthScale = lengthScale;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.blend = blend;
            return this;
        }

        private PlayerLimbPose interpolate(PlayerLimbPose target, double factor) {
            rotationX = interpolateAngle(rotationX, target.rotationX, factor);
            rotationY = interpolateAngle(rotationY, target.rotationY, factor);
            rotationZ = interpolateAngle(rotationZ, target.rotationZ, factor);
            lengthScale += (target.lengthScale - lengthScale) * factor;
            scaleX += (target.scaleX - scaleX) * factor;
            scaleY += (target.scaleY - scaleY) * factor;
            scaleZ += (target.scaleZ - scaleZ) * factor;
            blend += (target.blend - blend) * factor;
            return this;
        }
    }

    /**
     * Evaluates the vehicle's player animation entries for this tick: which entries and grip
     * overrides are active, and the one-shot, linger, and initiating-player state that follows.
     * Call this on the client once per vehicle tick. It also registers vehicles with exterior
     * entries so players walking up to them can be found without scanning the world.
     */
    public static void updateVehicle(EntityVehicleF_Physics vehicle) {
        if (!vehicle.world.isClient()) {
            return;
        }
        List<JSONPlayerAnimation> definitions = getDefinitions(vehicle);
        if (definitions == null || definitions.isEmpty()) {
            if (!vehicleStates.isEmpty()) {
                removeState(vehicleStates.get(vehicle));
            }
            return;
        }
        if (playerTweaksEnabled()) {
            getVehicleState(vehicle, definitions).tick();
        } else if (!vehicleStates.isEmpty()) {
            //Drop the trigger state, so turning playerTweaks back on starts fresh rather than
            //seeing every change made meanwhile as a one-shot edge.
            removeState(vehicleStates.get(vehicle));
        }
    }

    /**
     * Returns the limbs of this player that currently have a pose from a vehicle, either selected
     * or still releasing, whether seated or standing near a vehicle. This is empty for anything
     * that is not a player and while the playerTweaks setting is off. The result is cached per
     * player for the rendered frame, so it is cheap to call from several render events. The
     * returned set is shared: do not modify it.
     */
    public static EnumSet<PlayerLimb> getActiveLimbs(IWrapperEntity entity, float partialTicks) {
        return entity instanceof IWrapperPlayer ? getPlayerState((IWrapperPlayer) entity, partialTicks).activeLimbs : NO_LIMBS;
    }

    /**
     * Populates output with the pose of the limb and returns true if the limb is in
     * {@link #getActiveLimbs(IWrapperEntity, float)}. Returns false for anything that is not a player.
     * Repeated calls within one frame return the same pose.
     */
    public static boolean getPose(IWrapperEntity entity, PlayerLimb limb, float partialTicks, PlayerLimbPose output) {
        if (!(entity instanceof IWrapperPlayer)) {
            return false;
        }
        PlayerState state = getPlayerState((IWrapperPlayer) entity, partialTicks);
        if (!state.activeLimbs.contains(limb)) {
            return false;
        }
        output.set(state.poses[limb.ordinal()]);
        return true;
    }

    /**
     * Records the authenticated player responsible for a client-originated
     * variable change.  This is visual state only: normal vehicle variables
     * and their packet behavior are not altered.  Part variables are credited
     * to the part's owning vehicle so vehicle-level player animations can use
     * the same attribution path.  The next {@link #updateVehicle} consumes it.
     */
    public static void recordVariableInteraction(AEntityD_Definable<?> source, String variableKey, UUID playerID) {
        if (source == null || variableKey == null || playerID == null || !source.world.isClient()) {
            return;
        }
        VehicleAnimationState state = getStateForInteraction(source);
        if (state != null) {
            state.recordVariableInteraction(variableKey, playerID);
        }
    }

    /**
     * Records a player interaction whose resulting animation variable may be
     * derived rather than directly synchronized, such as player_interacting or
     * an engine clutch-shift pulse.
     */
    public static void recordPlayerInteraction(AEntityD_Definable<?> source, UUID playerID) {
        if (source == null || playerID == null || !source.world.isClient()) {
            return;
        }
        VehicleAnimationState state = getStateForInteraction(source);
        if (state != null) {
            state.recordPlayerInteraction(playerID);
        }
    }

    private static VehicleAnimationState getStateForInteraction(AEntityD_Definable<?> source) {
        EntityVehicleF_Physics vehicle = source instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) source : source instanceof APart ? ((APart) source).vehicleOn : null;
        List<JSONPlayerAnimation> definitions = getDefinitions(vehicle);
        return definitions != null && !definitions.isEmpty() && playerTweaksEnabled() ? getVehicleState(vehicle, definitions) : null;
    }

    private static PlayerState getPlayerState(IWrapperPlayer player, float partialTicks) {
        PlayerState state = playerStates.get(player);
        if (state == null) {
            state = new PlayerState();
            playerStates.put(player, state);
        }
        long tick = player.getWorld() != null ? player.getWorld().getTickCount() : 0;
        if (state.resolvedTick == tick && Math.abs(state.resolvedPartialTicks - partialTicks) < SAME_FRAME_EPSILON) {
            return state;
        }
        state.resolvedTick = tick;
        state.resolvedPartialTicks = partialTicks;
        state.activeLimbs.clear();

        boolean enabled = player.getWorld() != null && playerTweaksEnabled();
        AEntityB_Existing ridingEntity = enabled ? player.getEntityRiding() : null;
        for (PlayerLimb limb : LIMBS) {
            int index = limb.ordinal();
            LimbAnimationState limbState = state.limbs[index];
            PlayerLimbPose pose = state.poses[index];
            boolean hasPose;
            if (!enabled) {
                limbState.clear();
                hasPose = false;
            } else if (ridingEntity instanceof PartSeat) {
                hasPose = resolveSeatedPose(player, (PartSeat) ridingEntity, limb, limbState, partialTicks, pose);
            } else {
                hasPose = resolveExteriorPose(player, limb, limbState, partialTicks, pose);
            }
            if (hasPose) {
                state.activeLimbs.add(limb);
            }
        }
        return state;
    }

    /**
     * Only seats mounted directly on the vehicle take part: for those, the seat's placement slot
     * is an index into the vehicle's own part list, which is what seatSlot names.
     */
    private static boolean resolveSeatedPose(IWrapperPlayer player, PartSeat seat, PlayerLimb limb, LimbAnimationState limbState, float partialTicks, PlayerLimbPose output) {
        EntityVehicleF_Physics vehicle = seat.vehicleOn;
        List<JSONPlayerAnimation> definitions = getDefinitions(vehicle);
        double renderTime = getRenderTime(vehicle, partialTicks);
        VehicleAnimationState vehicleState = definitions != null && !definitions.isEmpty() && seat.entityOn == vehicle ? getExistingVehicleState(vehicle, definitions) : null;
        if (vehicleState == null) {
            return releasePose(limbState, vehicle, renderTime, output);
        }

        SelectedTarget selected = null;
        for (int i = 0; i < definitions.size() && selected == null; ++i) {
            JSONPlayerAnimation definition = definitions.get(i);
            if (definition.limb == limb && definition.appliesToSeat(seat.placementSlot) && vehicleState.isEntrySelected(definition, player)) {
                selected = vehicleState.getSelectedTarget(definition, partialTicks);
            }
        }
        if (selected == null) {
            return releasePose(limbState, vehicle, renderTime, output);
        }
        calculateWorldTarget(vehicle, selected, partialTicks, scratchTarget);
        if (!calculateSeatedTargetPose(player, seat, scratchTarget, limb, selected.limbScale, partialTicks, targetPose)) {
            return releasePose(limbState, vehicle, renderTime, output);
        }
        return applySelectedPose(limbState, vehicle, selected, targetPose, renderTime, false, output);
    }

    private static boolean resolveExteriorPose(IWrapperPlayer player, PlayerLimb limb, LimbAnimationState limbState, float partialTicks, PlayerLimbPose output) {
        SelectedTarget closestTarget = null;
        EntityVehicleF_Physics closestVehicle = null;
        double closestDistance = Double.MAX_VALUE;
        scratchTorso.set(player.getInterpolatedPosition(partialTicks)).add(0, player.getEyeHeight() * 2D / 3D, 0);

        for (int stateIndex = 0; stateIndex < exteriorStates.size(); ++stateIndex) {
            VehicleAnimationState vehicleState = exteriorStates.get(stateIndex);
            EntityVehicleF_Physics vehicle = vehicleState.vehicle;
            if (!vehicle.isValid) {
                removeState(vehicleState);
                --stateIndex;
                continue;
            }
            if (!vehicle.world.equals(player.getWorld())) {
                continue;
            }
            double vehicleScale = Math.max(vehicle.scale.x, Math.max(vehicle.scale.y, vehicle.scale.z));
            if (scratchTorso.distanceTo(vehicle.position) > vehicleState.exteriorReach * vehicleScale + vehicleState.maxExteriorRange + EXTERIOR_CULL_MARGIN) {
                continue;
            }

            //List priority applies within one vehicle. Across vehicles, the
            //nearest eligible authored point wins.
            List<JSONPlayerAnimation> definitions = vehicleState.definitions;
            for (int i = 0; i < definitions.size(); ++i) {
                JSONPlayerAnimation definition = definitions.get(i);
                if (definition.limb != limb || definition.seatSlot != null || definition.range == null || definition.range <= 0 || !vehicleState.isEntrySelected(definition, player)) {
                    continue;
                }
                SelectedTarget candidate = vehicleState.getSelectedTarget(definition, partialTicks);
                if (candidate != null) {
                    calculateWorldTarget(vehicle, candidate, partialTicks, scratchTarget);
                    double distance = scratchTorso.distanceTo(scratchTarget);
                    if (distance <= definition.range) {
                        if (distance < closestDistance) {
                            closestTarget = candidate;
                            closestVehicle = vehicle;
                            scratchBestTarget.set(scratchTarget);
                            closestDistance = distance;
                        }
                        break;
                    }
                }
            }
        }

        if (closestTarget == null) {
            if (!limbState.exterior || limbState.vehicle == null) {
                limbState.clear();
                return false;
            }
            return releasePose(limbState, limbState.vehicle, getRenderTime(limbState.vehicle, partialTicks), output);
        }

        double renderTime = getRenderTime(closestVehicle, partialTicks);
        if (!calculateExteriorTargetPose(player, scratchBestTarget, limb, closestTarget.limbScale, partialTicks, targetPose)) {
            return releasePose(limbState, closestVehicle, renderTime, output);
        }
        return applySelectedPose(limbState, closestVehicle, closestTarget, targetPose, renderTime, true, output);
    }

    /** Transforms the target's authored point into world space, writing it to output. */
    private static void calculateWorldTarget(EntityVehicleF_Physics vehicle, SelectedTarget selected, float partialTicks, Point3D output) {
        output.set(selected.position);
        AnimationSwitchbox objectSwitchbox = vehicle.animatedObjectSwitchboxes.get(selected.applyAfter);
        if (objectSwitchbox != null) {
            output.transform(objectSwitchbox.netMatrix);
        }
        scratchScale.set(vehicle.prevScale).interpolate(vehicle.scale, partialTicks);
        vehicle.getInterpolatedOrientation(scratchOrientation, partialTicks);
        output.multiply(scratchScale).rotate(scratchOrientation);
        setInterpolatedPosition(vehicle, partialTicks, scratchPosition);
        output.add(scratchPosition);
    }

    /** Converts worldTarget, which is modified, into the seat's frame and aims the limb at it. */
    private static boolean calculateSeatedTargetPose(IWrapperPlayer player, PartSeat seat, Point3D worldTarget, PlayerLimb limb, Point3D limbScale, float partialTicks, PlayerLimbPose output) {
        setInterpolatedPosition(seat, partialTicks, scratchPosition);
        seat.getInterpolatedOrientation(scratchOrientation, partialTicks);
        worldTarget.subtract(scratchPosition).reOrigin(scratchOrientation);

        scratchScale.set(seat.riderScale);
        if (seat.definition.seat.playerScale != null) {
            scratchScale.multiply(seat.definition.seat.playerScale);
        }
        if (seat.placementDefinition.playerScale != null) {
            scratchScale.multiply(seat.placementDefinition.playerScale);
        }
        if (Math.abs(scratchScale.x) < 1.0E-6 || Math.abs(scratchScale.y) < 1.0E-6 || Math.abs(scratchScale.z) < 1.0E-6) {
            return false;
        }
        return calculateLimbPose(worldTarget.x / scratchScale.x, worldTarget.y / scratchScale.y, worldTarget.z / scratchScale.z, player.getSeatOffset(), limb, limbScale, output);
    }

    /** Converts worldTarget, which is modified, into the player's body frame and aims the limb at it. */
    private static boolean calculateExteriorTargetPose(IWrapperPlayer player, Point3D worldTarget, PlayerLimb limb, Point3D limbScale, float partialTicks, PlayerLimbPose output) {
        scratchAngles.set(0, player.getInterpolatedBodyYaw(partialTicks), 0);
        scratchOrientation.setToAngles(scratchAngles);
        worldTarget.subtract(player.getInterpolatedPosition(partialTicks)).reOrigin(scratchOrientation);
        return calculateLimbPose(worldTarget.x, worldTarget.y, worldTarget.z, 0, limb, limbScale, output);
    }

    /** Aims the center of the distal hand/foot face at the authored point, given in the player's frame. */
    private static boolean calculateLimbPose(double targetX, double targetY, double targetZ, double seatOffset, PlayerLimb limb, Point3D limbScale, PlayerLimbPose output) {
        boolean left = limb == PlayerLimb.armLeft || limb == PlayerLimb.legLeft;
        boolean arm = limb == PlayerLimb.armLeft || limb == PlayerLimb.armRight;

        //PlayerRenderer applies its 15/16 scale after the MTS rider transform.
        //Move the requested point back into the unscaled model space in which
        //ModelPart pivots and cubes are defined.  The seat offset is outside
        //that vanilla scale, so remove it before doing the division.
        double pivotX = (left ? 1 : -1) * (arm ? ARM_PIVOT_X : LEG_PIVOT_X);
        double pivotY = arm ? ARM_PIVOT_Y : LEG_PIVOT_Y;
        double endpointX = targetX / PLAYER_MODEL_SCALE - pivotX;
        double endpointY = (targetY - seatOffset) / PLAYER_MODEL_SCALE - pivotY;
        double endpointZ = targetZ / PLAYER_MODEL_SCALE;

        //The vanilla 4px arm is asymmetric around its rotation axis: its end
        //face center is one pixel outward. Solve the length-axis endpoint
        //exactly so that endpoint plus the rotated one-pixel lateral offset is
        //the requested coordinate.
        if (arm) {
            double handCenterX = (left ? 1 : -1) * ARM_HAND_CENTER_X;
            double requestedHorizontalLength = Math.hypot(endpointX, endpointZ);
            if (requestedHorizontalLength > Math.abs(handCenterX) + 1.0E-6) {
                double requestedHorizontalLengthSquared = requestedHorizontalLength * requestedHorizontalLength;
                double axisHorizontalLength = Math.sqrt(requestedHorizontalLengthSquared - handCenterX * handCenterX);
                double requestedX = endpointX;
                double requestedZ = endpointZ;
                endpointX = axisHorizontalLength * (axisHorizontalLength * requestedX - handCenterX * requestedZ) / requestedHorizontalLengthSquared;
                endpointZ = axisHorizontalLength * (handCenterX * requestedX + axisHorizontalLength * requestedZ) / requestedHorizontalLengthSquared;
            }
        }

        double length = Math.sqrt(endpointX * endpointX + endpointY * endpointY + endpointZ * endpointZ);
        if (length < 1.0E-6) {
            return false;
        }
        double normalizedY = Math.max(-1, Math.min(1, endpointY / length));
        //Arms span -2px through +10px about their pivot. Scaling keeps the
        //-2px shoulder face fixed, so the proximal section is included here.
        double lengthScale = arm ? (length + ARM_PROXIMAL_OFFSET) / DEFAULT_LIMB_LENGTH : length / DEFAULT_LIMB_LENGTH;
        output.set(-Math.PI / 2D - Math.asin(normalizedY), -Math.atan2(endpointX, endpointZ), 0, lengthScale, limbScale.x, limbScale.y, limbScale.z, 1);
        return true;
    }

    private static boolean applySelectedPose(LimbAnimationState limbState, EntityVehicleF_Physics vehicle, SelectedTarget selected, PlayerLimbPose targetPose, double renderTime, boolean exterior, PlayerLimbPose output) {
        if (limbState.vehicle != vehicle || limbState.selectionKey != selected.selectionKey || limbState.exterior != exterior) {
            boolean freshSelection = limbState.vehicle != vehicle || (limbState.selectionKey == null && !limbState.releasing) || limbState.exterior != exterior;
            limbState.captureCurrentPose(renderTime);
            if (freshSelection) {
                limbState.transitionPose.set(targetPose);
                limbState.transitionPose.blend = 0;
            }
            limbState.vehicle = vehicle;
            limbState.selectionKey = selected.selectionKey;
            limbState.transitionStart = renderTime;
            limbState.transitionDuration = selected.duration;
            limbState.exterior = exterior;
        }

        double factor = getTransitionFactor(renderTime, limbState.transitionStart, limbState.transitionDuration);
        if (factor < 1) {
            output.set(limbState.transitionPose).interpolate(targetPose, factor);
        } else {
            output.set(targetPose);
            output.blend = 1;
        }
        limbState.currentPose.set(output);
        limbState.lastRenderTime = renderTime;
        return true;
    }

    private static boolean releasePose(LimbAnimationState limbState, EntityVehicleF_Physics vehicle, double renderTime, PlayerLimbPose output) {
        if (limbState.selectionKey == null && !limbState.releasing) {
            return false;
        }
        if (vehicle == null || !vehicle.isValid || !Double.isFinite(renderTime) || limbState.vehicle != vehicle) {
            limbState.clear();
            return false;
        }
        if (!limbState.releasing) {
            limbState.captureCurrentPose(renderTime);
            limbState.selectionKey = null;
            limbState.transitionStart = renderTime;
            limbState.startBlend = limbState.currentPose.blend;
            limbState.releasing = true;
        }
        double factor = getTransitionFactor(renderTime, limbState.transitionStart, limbState.transitionDuration);
        if (factor >= 1) {
            limbState.clear();
            return false;
        }
        output.set(limbState.transitionPose);
        output.blend = limbState.startBlend * (1 - factor);
        limbState.currentPose.set(output);
        limbState.lastRenderTime = renderTime;
        return true;
    }

    private static List<JSONPlayerAnimation> getDefinitions(EntityVehicleF_Physics vehicle) {
        return vehicle != null && vehicle.definition.rendering != null ? vehicle.definition.rendering.playerAnimations : null;
    }

    /** Returns the vehicle's state, creating it, or replacing it if the definition was reloaded. Tick-side only. */
    private static VehicleAnimationState getVehicleState(EntityVehicleF_Physics vehicle, List<JSONPlayerAnimation> definitions) {
        VehicleAnimationState state = vehicleStates.get(vehicle);
        if (state == null || state.definitions != definitions) {
            removeState(state);
            state = new VehicleAnimationState(vehicle, definitions);
            vehicleStates.put(vehicle, state);
            if (state.hasExteriorEntries) {
                exteriorStates.add(state);
            }
        }
        return state;
    }

    /** Returns the state created by the vehicle's ticking, or null if it hasn't ticked yet. */
    private static VehicleAnimationState getExistingVehicleState(EntityVehicleF_Physics vehicle, List<JSONPlayerAnimation> definitions) {
        VehicleAnimationState state = vehicleStates.get(vehicle);
        return state != null && state.definitions == definitions ? state : null;
    }

    private static void removeState(VehicleAnimationState state) {
        if (state != null) {
            vehicleStates.remove(state.vehicle);
            exteriorStates.remove(state);
        }
    }

    /**
     * Drops all state for a removed client vehicle, including limbs still posed or releasing
     * against it. Called from the vehicle's remove(), which also runs for every entity on
     * world unload.
     */
    public static void removeVehicle(EntityVehicleF_Physics vehicle) {
        removeState(vehicleStates.get(vehicle));
        for (PlayerState playerState : playerStates.values()) {
            for (LimbAnimationState limbState : playerState.limbs) {
                if (limbState.vehicle == vehicle) {
                    limbState.clear();
                }
            }
            playerState.resolvedTick = Long.MIN_VALUE;
        }
    }

    private static void setInterpolatedPosition(AEntityB_Existing entity, float partialTicks, Point3D output) {
        if (entity.requiresDeltaUpdates()) {
            output.set(entity.prevPosition.x + (entity.position.x - entity.prevPosition.x) * partialTicks, entity.prevPosition.y + (entity.position.y - entity.prevPosition.y) * partialTicks, entity.prevPosition.z + (entity.position.z - entity.prevPosition.z) * partialTicks);
        } else {
            output.set(entity.position);
        }
    }

    private static double getRenderTime(AEntityB_Existing entity, float partialTicks) {
        return entity != null ? entity.ticksExisted + partialTicks : Double.NaN;
    }

    private static int getDuration(Integer duration) {
        return duration != null ? Math.max(0, duration) : DEFAULT_TRANSITION_TICKS;
    }

    private static int getLingerDuration(Integer lingerDuration) {
        return lingerDuration != null ? Math.max(0, lingerDuration) : 0;
    }

    private static boolean playerTweaksEnabled() {
        return ConfigSystem.client != null && ConfigSystem.client.renderingSettings.playerTweaks.value;
    }

    private static double getTransitionFactor(double renderTime, double startTime, int duration) {
        return duration == 0 ? 1 : Math.max(0, Math.min(1, (renderTime - startTime) / duration));
    }

    private static double interpolateAngle(double start, double end, double factor) {
        double delta = end - start;
        while (delta > Math.PI) {
            delta -= Math.PI * 2;
        }
        while (delta < -Math.PI) {
            delta += Math.PI * 2;
        }
        return start + delta * factor;
    }

    /** The resolved target of an entry or one of its grip overrides. Constant for the life of the definition. */
    private static final class SelectedTarget {
        /** The entry, or the grip override that replaces its target. Identifies the target across frames. */
        private final Object selectionKey;
        private final String applyAfter;
        private final Point3D position;
        private final Point3D limbScale;
        private final int duration;

        private SelectedTarget(Object selectionKey, String applyAfter, Point3D position, Point3D limbScale, int duration) {
            this.selectionKey = selectionKey;
            this.applyAfter = applyAfter;
            this.position = position;
            this.limbScale = limbScale;
            this.duration = duration;
        }
    }

    /** Per-player data: the per-limb transition state, plus the pose resolved for the last rendered frame. */
    private static final class PlayerState {
        private final LimbAnimationState[] limbs = new LimbAnimationState[LIMBS.length];
        private final PlayerLimbPose[] poses = new PlayerLimbPose[LIMBS.length];
        private final EnumSet<PlayerLimb> activeLimbs = EnumSet.noneOf(PlayerLimb.class);
        private long resolvedTick = Long.MIN_VALUE;
        private float resolvedPartialTicks;

        private PlayerState() {
            for (int i = 0; i < LIMBS.length; ++i) {
                limbs[i] = new LimbAnimationState();
                poses[i] = new PlayerLimbPose();
            }
        }
    }

    private static final class LimbAnimationState {
        private EntityVehicleF_Physics vehicle;
        private Object selectionKey;
        private final PlayerLimbPose transitionPose = new PlayerLimbPose();
        private final PlayerLimbPose currentPose = new PlayerLimbPose();
        private double transitionStart;
        private double lastRenderTime;
        private double startBlend;
        private int transitionDuration = DEFAULT_TRANSITION_TICKS;
        private boolean releasing;
        private boolean exterior;

        private void captureCurrentPose(double renderTime) {
            transitionPose.set(currentPose);
            if (lastRenderTime == 0 || renderTime < lastRenderTime) {
                transitionPose.blend = 0;
            }
            releasing = false;
        }

        private void clear() {
            vehicle = null;
            selectionKey = null;
            transitionPose.set(0, 0, 0, 1, 1, 1, 1, 0);
            currentPose.set(transitionPose);
            transitionStart = 0;
            lastRenderTime = 0;
            startBlend = 0;
            transitionDuration = DEFAULT_TRANSITION_TICKS;
            releasing = false;
            exterior = false;
        }
    }

    private static final class TriggerState {
        private boolean initialized;
        private boolean wasActive;
        private boolean wasEverActive;
        private double deactivatedAt = Double.NaN;
        private double triggerStartedAt = Double.NaN;
        private UUID selectedPlayerID;
        private UUID pendingPlayerID;
        private boolean playerInteractionPending;

        private void recordPlayerInteraction(UUID playerID) {
            pendingPlayerID = playerID;
            playerInteractionPending = true;
        }

        private boolean isSelectedFor(IWrapperEntity player) {
            return selectedPlayerID != null && selectedPlayerID.equals(player.getID());
        }

        /** Advances this trigger by one vehicle tick, with time being the vehicle's ticksExisted. Returns whether the entry is selected. */
        private boolean update(boolean active, boolean oneShot, boolean triggerOnChange, int duration, int lingerDuration, double time, UUID recentPlayerID) {
            boolean stateChanged = initialized && active != wasActive;
            boolean hadPlayerInteraction = playerInteractionPending || (recentPlayerID != null && (!initialized || stateChanged));
            UUID interactingPlayerID = playerInteractionPending ? pendingPlayerID : recentPlayerID;
            playerInteractionPending = false;
            pendingPlayerID = null;
            if (!initialized) {
                initialized = true;
                wasActive = active;
                wasEverActive = active;
                if (oneShot) {
                    //A packet arriving before this definition's first evaluation
                    //is a real interaction, rather than initial loading.
                    if (hadPlayerInteraction && (triggerOnChange || active)) {
                        triggerStartedAt = time;
                        selectedPlayerID = interactingPlayerID;
                    }
                    return Double.isFinite(triggerStartedAt) && time <= triggerStartedAt + duration + lingerDuration;
                }
                selectedPlayerID = active && hadPlayerInteraction ? interactingPlayerID : null;
                return active;
            }
            if (oneShot) {
                if ((triggerOnChange && active != wasActive) || (!triggerOnChange && active && !wasActive)) {
                    triggerStartedAt = time;
                    selectedPlayerID = hadPlayerInteraction ? interactingPlayerID : null;
                }
                wasActive = active;
                return Double.isFinite(triggerStartedAt) && time <= triggerStartedAt + duration + lingerDuration;
            }
            if (active) {
                wasEverActive = true;
                deactivatedAt = Double.NaN;
                if (hadPlayerInteraction) {
                    selectedPlayerID = interactingPlayerID;
                } else if (!wasActive) {
                    //An automatic or server-originated activation has no actor.
                    selectedPlayerID = null;
                }
            } else if (wasActive) {
                deactivatedAt = time;
                //An explicit deactivation transfers ownership to its actor.
                //If a momentary control releases automatically, retain the
                //player who initiated it so linger can complete for that same
                //player rather than disappearing on the falling edge.
                if (hadPlayerInteraction) {
                    selectedPlayerID = interactingPlayerID;
                }
            }
            wasActive = active;
            return active || (wasEverActive && Double.isFinite(deactivatedAt) && time <= deactivatedAt + lingerDuration);
        }
    }

    private static final class VehicleAnimationState {
        private final EntityVehicleF_Physics vehicle;
        private final List<JSONPlayerAnimation> definitions;
        private final boolean hasExteriorEntries;
        /** Farthest exterior target from the vehicle origin, in unscaled model units. */
        private double exteriorReach;
        private double maxExteriorRange;
        private final Map<JSONPlayerAnimation, AnimationSwitchbox> entrySwitchboxes = new IdentityHashMap<>();
        private final Map<JSONPlayerGripOverride, AnimationSwitchbox> overrideSwitchboxes = new IdentityHashMap<>();
        private final Map<JSONPlayerAnimation, TriggerState> entryTriggers = new IdentityHashMap<>();
        private final Map<JSONPlayerGripOverride, TriggerState> overrideTriggers = new IdentityHashMap<>();
        private final Map<JSONPlayerAnimation, Boolean> selectedEntries = new IdentityHashMap<>();
        private final Map<JSONPlayerGripOverride, Boolean> selectedOverrides = new IdentityHashMap<>();
        /** Keyed by entry or grip override. */
        private final Map<Object, SelectedTarget> targets = new IdentityHashMap<>();
        private UUID recentPlayerID;
        private long recentInteractionTick = Long.MIN_VALUE;

        private VehicleAnimationState(EntityVehicleF_Physics vehicle, List<JSONPlayerAnimation> definitions) {
            this.vehicle = vehicle;
            this.definitions = definitions;
            boolean exterior = false;
            for (JSONPlayerAnimation definition : definitions) {
                boolean exteriorEntry = definition.seatSlot == null && definition.range != null && definition.range > 0;
                exterior |= exteriorEntry;
                if (exteriorEntry) {
                    maxExteriorRange = Math.max(maxExteriorRange, definition.range);
                    if (definition.pos != null) {
                        exteriorReach = Math.max(exteriorReach, definition.pos.length());
                    }
                    if (definition.gripOverrides != null) {
                        for (JSONPlayerGripOverride override : definition.gripOverrides) {
                            if (override.pos != null) {
                                exteriorReach = Math.max(exteriorReach, override.pos.length());
                            }
                        }
                    }
                }
                entryTriggers.put(definition, new TriggerState());
                addTarget(definition, definition, definition.applyAfter, definition.pos, definition.limbScale, definition.duration);
                if (definition.activeAnimations != null) {
                    entrySwitchboxes.put(definition, new AnimationSwitchbox(vehicle, definition.activeAnimations, null));
                }
                if (definition.gripOverrides != null) {
                    for (JSONPlayerGripOverride override : definition.gripOverrides) {
                        overrideTriggers.put(override, new TriggerState());
                        addTarget(override, override, override.applyAfter != null ? override.applyAfter : definition.applyAfter, override.pos, override.limbScale != null ? override.limbScale : definition.limbScale, override.duration != null ? override.duration : definition.duration);
                        if (override.activeAnimations != null) {
                            overrideSwitchboxes.put(override, new AnimationSwitchbox(vehicle, override.activeAnimations, null));
                        }
                    }
                }
            }
            this.hasExteriorEntries = exterior;
        }

        private void addTarget(Object key, Object selectionKey, String applyAfter, Point3D position, Point3D limbScale, Integer duration) {
            if (applyAfter != null && position != null) {
                targets.put(key, new SelectedTarget(selectionKey, applyAfter, position, limbScale != null ? limbScale : DEFAULT_LIMB_SCALE, getDuration(duration)));
            }
        }

        /** Evaluates every entry and grip override for this vehicle tick. */
        private void tick() {
            double time = vehicle.ticksExisted;
            UUID recentActor = vehicle.ticksExisted - recentInteractionTick <= 2 ? recentPlayerID : null;
            for (JSONPlayerAnimation definition : definitions) {
                AnimationSwitchbox switchbox = entrySwitchboxes.get(definition);
                boolean active = switchbox == null || switchbox.runSwitchbox(0, false);
                selectedEntries.put(definition, entryTriggers.get(definition).update(active, definition.oneShot, definition.triggerOnChange, getDuration(definition.duration), getLingerDuration(definition.lingerDuration), time, definition.initiatingPlayerOnly ? recentActor : null));

                if (definition.gripOverrides != null) {
                    for (JSONPlayerGripOverride override : definition.gripOverrides) {
                        if (override.activeAnimations == null) {
                            selectedOverrides.put(override, true);
                            continue;
                        }
                        AnimationSwitchbox overrideSwitchbox = overrideSwitchboxes.get(override);
                        boolean overrideActive = overrideSwitchbox == null || overrideSwitchbox.runSwitchbox(0, false);
                        int duration = getDuration(override.duration != null ? override.duration : definition.duration);
                        int linger = getLingerDuration(override.lingerDuration != null ? override.lingerDuration : definition.lingerDuration);
                        boolean oneShot = override.oneShot != null ? override.oneShot : definition.oneShot;
                        boolean triggerOnChange = override.triggerOnChange != null ? override.triggerOnChange : definition.triggerOnChange;
                        selectedOverrides.put(override, overrideTriggers.get(override).update(overrideActive, oneShot, triggerOnChange, duration, linger, time, null));
                    }
                }
            }
        }

        private void recordVariableInteraction(String variableKey, UUID playerID) {
            recordPlayerInteraction(playerID);
            for (JSONPlayerAnimation definition : definitions) {
                if (definition.initiatingPlayerOnly && referencesVariable(definition.activeAnimations, variableKey)) {
                    entryTriggers.get(definition).recordPlayerInteraction(playerID);
                }
            }
        }

        private void recordPlayerInteraction(UUID playerID) {
            recentPlayerID = playerID;
            recentInteractionTick = vehicle.ticksExisted;
        }

        private boolean isEntrySelected(JSONPlayerAnimation definition, IWrapperEntity player) {
            return Boolean.TRUE.equals(selectedEntries.get(definition)) && (!definition.initiatingPlayerOnly || entryTriggers.get(definition).isSelectedFor(player));
        }

        /**
         * Returns the target of the entry, or of its first selected grip override, if the animated
         * object it follows is currently visible. The object's switchbox is run for this frame, so
         * its transform is current for the caller.
         */
        private SelectedTarget getSelectedTarget(JSONPlayerAnimation definition, float partialTicks) {
            JSONPlayerGripOverride override = getSelectedOverride(definition);
            SelectedTarget target = targets.get(override != null ? override : definition);
            if (target == null) {
                return null;
            }
            AnimationSwitchbox objectSwitchbox = vehicle.animatedObjectSwitchboxes.get(target.applyAfter);
            return objectSwitchbox == null || objectSwitchbox.runSwitchbox(partialTicks, false) ? target : null;
        }

        private JSONPlayerGripOverride getSelectedOverride(JSONPlayerAnimation definition) {
            if (definition.gripOverrides != null) {
                for (int i = 0; i < definition.gripOverrides.size(); ++i) {
                    JSONPlayerGripOverride override = definition.gripOverrides.get(i);
                    if (Boolean.TRUE.equals(selectedOverrides.get(override))) {
                        return override;
                    }
                }
            }
            return null;
        }

        private static boolean referencesVariable(List<JSONAnimationDefinition> animations, String changedVariable) {
            if (animations == null) {
                return false;
            }
            String normalizedChangedVariable = normalizeVariable(changedVariable);
            for (JSONAnimationDefinition animation : animations) {
                if (animation.variable != null && normalizeVariable(animation.variable).equals(normalizedChangedVariable)) {
                    return true;
                }
            }
            return false;
        }

        private static String normalizeVariable(String variable) {
            return variable.startsWith("!") ? variable.substring(1) : variable;
        }
    }
}
