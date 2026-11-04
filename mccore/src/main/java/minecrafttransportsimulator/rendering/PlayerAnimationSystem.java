package minecrafttransportsimulator.rendering;

import java.util.EnumMap;
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
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Resolves vehicle-defined player limb targets into Minecraft model rotations
 * and length scales. This class contains no Minecraft classes, so every game
 * interface shares selection, priority, interpolation, and trigger behavior.
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

    private static final Map<EntityVehicleF_Physics, VehicleAnimationState> vehicleStates = new WeakHashMap<>();
    private static final Map<IWrapperEntity, EnumMap<PlayerLimb, LimbAnimationState>> playerStates = new WeakHashMap<>();

    private PlayerAnimationSystem() {
    }

    /** Returns true when this player is in a seat with authored animations. */
    public static boolean hasAnimationsForSeat(IWrapperEntity player) {
        if (!playerTweaksEnabled()) {
            return false;
        }
        AEntityB_Existing ridingEntity = player.getEntityRiding();
        if (ridingEntity instanceof PartSeat) {
            PartSeat seat = (PartSeat) ridingEntity;
            List<JSONPlayerAnimation> definitions = getDefinitions(seat.vehicleOn);
            if (definitions != null) {
                for (JSONPlayerAnimation definition : definitions) {
                    if (definition.appliesToSeat(seat.placementSlot)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Returns exterior-controlled limbs, including their release transitions. */
    public static EnumSet<PlayerLimb> getExteriorLimbs(IWrapperEntity player, float partialTicks) {
        EnumSet<PlayerLimb> limbs = EnumSet.noneOf(PlayerLimb.class);
        if (!playerTweaksEnabled() || player.getEntityRiding() instanceof PartSeat) {
            return limbs;
        }
        for (PlayerLimb limb : PlayerLimb.values()) {
            PlayerLimbPose pose = new PlayerLimbPose();
            if (getPose(player, limb, partialTicks, pose)) {
                LimbAnimationState state = getExistingLimbState(player, limb);
                if (state != null && state.exterior) {
                    limbs.add(limb);
                }
            }
        }
        return limbs;
    }

    /** Mutable output object, intended to be cached by callers. Rotations are radians. */
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

    /** Populates output with the selected pose or release transition. */
    public static boolean getPose(IWrapperEntity player, PlayerLimb limb, float partialTicks, PlayerLimbPose output) {
        if (!playerTweaksEnabled()) {
            LimbAnimationState state = getExistingLimbState(player, limb);
            if (state != null) {
                state.clear();
            }
            return false;
        }
        AEntityB_Existing ridingEntity = player.getEntityRiding();
        return ridingEntity instanceof PartSeat
            ? getSeatedPose(player, (PartSeat) ridingEntity, limb, partialTicks, output)
            : getExteriorPose(player, limb, partialTicks, output);
    }

    /**
     * Records the authenticated player responsible for a client-originated
     * variable change.  This is visual state only: normal vehicle variables
     * and their packet behavior are not altered.  Part variables are credited
     * to the part's owning vehicle so vehicle-level player animations can use
     * the same attribution path.
     */
    public static void recordVariableInteraction(AEntityD_Definable<?> source, String variableKey, UUID playerID) {
        if (source == null || variableKey == null || playerID == null || !source.world.isClient()) {
            return;
        }
        EntityVehicleF_Physics vehicle = source instanceof EntityVehicleF_Physics
            ? (EntityVehicleF_Physics) source
            : source instanceof APart ? ((APart) source).vehicleOn : null;
        List<JSONPlayerAnimation> definitions = getDefinitions(vehicle);
        if (vehicle != null && definitions != null && !definitions.isEmpty()) {
            getVehicleState(vehicle, definitions).recordVariableInteraction(variableKey, playerID);
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
        EntityVehicleF_Physics vehicle = source instanceof EntityVehicleF_Physics
            ? (EntityVehicleF_Physics) source
            : source instanceof APart ? ((APart) source).vehicleOn : null;
        List<JSONPlayerAnimation> definitions = getDefinitions(vehicle);
        if (vehicle != null && definitions != null && !definitions.isEmpty()) {
            getVehicleState(vehicle, definitions).recordPlayerInteraction(playerID);
        }
    }

    private static boolean getSeatedPose(IWrapperEntity player, PartSeat seat, PlayerLimb limb, float partialTicks, PlayerLimbPose output) {
        EntityVehicleF_Physics vehicle = seat.vehicleOn;
        List<JSONPlayerAnimation> definitions = getDefinitions(vehicle);
        double renderTime = getRenderTime(vehicle, partialTicks);
        if (definitions == null || definitions.isEmpty()) {
            return releasePose(player, limb, vehicle, renderTime, output);
        }

        VehicleAnimationState vehicleState = getVehicleState(vehicle, definitions);
        vehicleState.prepareFrame(partialTicks, renderTime);
        SelectedTarget selected = null;
        for (JSONPlayerAnimation definition : definitions) {
            if (definition.limb == limb && definition.appliesToSeat(seat.placementSlot) && vehicleState.isEntrySelected(definition, player)) {
                selected = getSelectedTarget(vehicleState, definition, partialTicks);
                if (selected != null) {
                    break;
                }
            }
        }
        if (selected == null) {
            return releasePose(player, limb, vehicle, renderTime, output);
        }

        Point3D worldTarget = getWorldTarget(vehicle, selected, partialTicks);
        PlayerLimbPose targetPose = new PlayerLimbPose();
        if (worldTarget == null || !calculateSeatedTargetPose(player, seat, worldTarget, limb, selected.limbScale, partialTicks, targetPose)) {
            return releasePose(player, limb, vehicle, renderTime, output);
        }
        return applySelectedPose(player, limb, vehicle, selected, targetPose, renderTime, false, output);
    }

    private static boolean getExteriorPose(IWrapperEntity player, PlayerLimb limb, float partialTicks, PlayerLimbPose output) {
        SelectedTarget closestTarget = null;
        EntityVehicleF_Physics closestVehicle = null;
        Point3D closestWorldTarget = null;
        double closestDistance = Double.MAX_VALUE;
        Point3D playerPosition = player.getInterpolatedPosition(partialTicks);
        Point3D playerTorsoPosition = playerPosition.copy().add(0, player.getEyeHeight() * 2D / 3D, 0);

        if (player.getWorld() != null) {
            for (EntityVehicleF_Physics vehicle : player.getWorld().getEntitiesOfType(EntityVehicleF_Physics.class)) {
                List<JSONPlayerAnimation> definitions = getDefinitions(vehicle);
                if (definitions == null || definitions.isEmpty()) {
                    continue;
                }
                double renderTime = getRenderTime(vehicle, partialTicks);
                VehicleAnimationState vehicleState = getVehicleState(vehicle, definitions);
                vehicleState.prepareFrame(partialTicks, renderTime);

                //List priority applies within one vehicle. Across vehicles, the
                //nearest eligible authored point wins.
                for (JSONPlayerAnimation definition : definitions) {
                    if (definition.limb != limb || definition.seatSlot != null || definition.range == null || definition.range <= 0 || !vehicleState.isEntrySelected(definition, player)) {
                        continue;
                    }
                    SelectedTarget candidate = getSelectedTarget(vehicleState, definition, partialTicks);
                    Point3D worldTarget = candidate != null ? getWorldTarget(vehicle, candidate, partialTicks) : null;
                    if (worldTarget != null) {
                        double distance = playerTorsoPosition.distanceTo(worldTarget);
                        if (distance <= definition.range) {
                            if (distance < closestDistance) {
                                closestTarget = candidate;
                                closestVehicle = vehicle;
                                closestWorldTarget = worldTarget;
                                closestDistance = distance;
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (closestTarget == null) {
            LimbAnimationState state = getExistingLimbState(player, limb);
            if (state == null || !state.exterior || state.vehicle == null) {
                if (state != null) {
                    state.clear();
                }
                return false;
            }
            return releasePose(player, limb, state.vehicle, getRenderTime(state.vehicle, partialTicks), output);
        }

        PlayerLimbPose targetPose = new PlayerLimbPose();
        if (!calculateExteriorTargetPose(player, closestWorldTarget, limb, closestTarget.limbScale, partialTicks, targetPose)) {
            return releasePose(player, limb, closestVehicle, getRenderTime(closestVehicle, partialTicks), output);
        }
        return applySelectedPose(player, limb, closestVehicle, closestTarget, targetPose, getRenderTime(closestVehicle, partialTicks), true, output);
    }

    private static SelectedTarget getSelectedTarget(VehicleAnimationState state, JSONPlayerAnimation definition, float partialTicks) {
        JSONPlayerGripOverride override = state.getSelectedOverride(definition);
        String applyAfter = override != null && override.applyAfter != null ? override.applyAfter : definition.applyAfter;
        Point3D position = override != null ? override.pos : definition.pos;
        Point3D limbScale = override != null && override.limbScale != null ? override.limbScale : definition.limbScale;
        if (applyAfter == null || position == null || !state.isModelObjectActive(applyAfter, partialTicks)) {
            return null;
        }
        int duration = getDuration(override != null && override.duration != null ? override.duration : definition.duration);
        return new SelectedTarget(override != null ? override : definition, applyAfter, position, limbScale != null ? limbScale : DEFAULT_LIMB_SCALE, duration);
    }

    private static Point3D getWorldTarget(EntityVehicleF_Physics vehicle, SelectedTarget selected, float partialTicks) {
        if (vehicle == null) {
            return null;
        }
        Point3D target = selected.position.copy();
        AnimationSwitchbox objectSwitchbox = vehicle.animatedObjectSwitchboxes.get(selected.applyAfter);
        if (objectSwitchbox != null) {
            target.transform(objectSwitchbox.netMatrix);
        }
        Point3D vehicleScale = vehicle.prevScale.copy().interpolate(vehicle.scale, partialTicks);
        RotationMatrix vehicleOrientation = new RotationMatrix();
        vehicle.getInterpolatedOrientation(vehicleOrientation, partialTicks);
        return target.multiply(vehicleScale).rotate(vehicleOrientation).add(getInterpolatedPosition(vehicle, partialTicks));
    }

    private static boolean calculateSeatedTargetPose(IWrapperEntity player, PartSeat seat, Point3D worldTarget, PlayerLimb limb, Point3D limbScale, float partialTicks, PlayerLimbPose output) {
        RotationMatrix seatOrientation = new RotationMatrix();
        seat.getInterpolatedOrientation(seatOrientation, partialTicks);
        Point3D target = worldTarget.subtract(getInterpolatedPosition(seat, partialTicks)).reOrigin(seatOrientation);

        Point3D playerScale = seat.riderScale.copy();
        if (seat.definition.seat.playerScale != null) {
            playerScale.multiply(seat.definition.seat.playerScale);
        }
        if (seat.placementDefinition.playerScale != null) {
            playerScale.multiply(seat.placementDefinition.playerScale);
        }
        if (Math.abs(playerScale.x) < 1.0E-6 || Math.abs(playerScale.y) < 1.0E-6 || Math.abs(playerScale.z) < 1.0E-6) {
            return false;
        }
        target.x /= playerScale.x;
        target.y /= playerScale.y;
        target.z /= playerScale.z;
        return calculateLimbPose(target, player.getSeatOffset(), limb, limbScale, output);
    }

    private static boolean calculateExteriorTargetPose(IWrapperEntity player, Point3D worldTarget, PlayerLimb limb, Point3D limbScale, float partialTicks, PlayerLimbPose output) {
        RotationMatrix bodyOrientation = new RotationMatrix().setToAngles(new Point3D(0, player.getInterpolatedBodyYaw(partialTicks), 0));
        Point3D target = worldTarget.subtract(player.getInterpolatedPosition(partialTicks)).reOrigin(bodyOrientation);
        return calculateLimbPose(target, 0, limb, limbScale, output);
    }

    /** Aims the center of the distal hand/foot face at the authored point. */
    private static boolean calculateLimbPose(Point3D target, double seatOffset, PlayerLimb limb, Point3D limbScale, PlayerLimbPose output) {
        boolean left = limb == PlayerLimb.armLeft || limb == PlayerLimb.legLeft;
        boolean arm = limb == PlayerLimb.armLeft || limb == PlayerLimb.armRight;

        //PlayerRenderer applies its 15/16 scale after the MTS rider transform.
        //Move the requested point back into the unscaled model space in which
        //ModelPart pivots and cubes are defined.  The seat offset is outside
        //that vanilla scale, so remove it before doing the division.
        target.set(target.x / PLAYER_MODEL_SCALE, (target.y - seatOffset) / PLAYER_MODEL_SCALE, target.z / PLAYER_MODEL_SCALE);
        double pivotX = (left ? 1 : -1) * (arm ? ARM_PIVOT_X : LEG_PIVOT_X);
        double pivotY = arm ? ARM_PIVOT_Y : LEG_PIVOT_Y;
        Point3D endpoint = target.subtract(new Point3D(pivotX, pivotY, 0));

        //The vanilla 4px arm is asymmetric around its rotation axis: its end
        //face center is one pixel outward. Solve the length-axis endpoint
        //exactly so that endpoint plus the rotated one-pixel lateral offset is
        //the requested coordinate.  The old iterative estimate left a visible
        //error when limbScale made an arm very thin.
        if (arm) {
            double handCenterX = (left ? 1 : -1) * ARM_HAND_CENTER_X;
            double requestedHorizontalLength = Math.hypot(endpoint.x, endpoint.z);
            if (requestedHorizontalLength > Math.abs(handCenterX) + 1.0E-6) {
                double requestedHorizontalLengthSquared = requestedHorizontalLength * requestedHorizontalLength;
                double axisHorizontalLength = Math.sqrt(requestedHorizontalLengthSquared - handCenterX * handCenterX);
                double requestedX = endpoint.x;
                double requestedZ = endpoint.z;
                endpoint.x = axisHorizontalLength * (axisHorizontalLength * requestedX - handCenterX * requestedZ) / requestedHorizontalLengthSquared;
                endpoint.z = axisHorizontalLength * (handCenterX * requestedX + axisHorizontalLength * requestedZ) / requestedHorizontalLengthSquared;
            }
        }

        double length = endpoint.length();
        if (length < 1.0E-6) {
            return false;
        }
        double normalizedY = Math.max(-1, Math.min(1, endpoint.y / length));
        //Arms span -2px through +10px about their pivot. Scaling keeps the
        //-2px shoulder face fixed, so the proximal section is included here.
        double lengthScale = arm ? (length + ARM_PROXIMAL_OFFSET) / DEFAULT_LIMB_LENGTH : length / DEFAULT_LIMB_LENGTH;
        output.set(-Math.PI / 2D - Math.asin(normalizedY), -Math.atan2(endpoint.x, endpoint.z), 0, lengthScale, limbScale.x, limbScale.y, limbScale.z, 1);
        return true;
    }

    private static boolean applySelectedPose(IWrapperEntity player, PlayerLimb limb, EntityVehicleF_Physics vehicle, SelectedTarget selected, PlayerLimbPose targetPose, double renderTime, boolean exterior, PlayerLimbPose output) {
        LimbAnimationState limbState = getLimbState(player, limb);
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

    private static boolean releasePose(IWrapperEntity player, PlayerLimb limb, EntityVehicleF_Physics vehicle, double renderTime, PlayerLimbPose output) {
        LimbAnimationState limbState = getExistingLimbState(player, limb);
        if (limbState == null || (limbState.selectionKey == null && !limbState.releasing)) {
            return false;
        }
        if (!Double.isFinite(renderTime) || limbState.vehicle != vehicle) {
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

    private static VehicleAnimationState getVehicleState(EntityVehicleF_Physics vehicle, List<JSONPlayerAnimation> definitions) {
        VehicleAnimationState state = vehicleStates.get(vehicle);
        if (state == null || state.definitions != definitions) {
            state = new VehicleAnimationState(vehicle, definitions);
            vehicleStates.put(vehicle, state);
        }
        return state;
    }

    private static LimbAnimationState getLimbState(IWrapperEntity player, PlayerLimb limb) {
        EnumMap<PlayerLimb, LimbAnimationState> states = playerStates.get(player);
        if (states == null) {
            states = new EnumMap<>(PlayerLimb.class);
            playerStates.put(player, states);
        }
        LimbAnimationState state = states.get(limb);
        if (state == null) {
            state = new LimbAnimationState();
            states.put(limb, state);
        }
        return state;
    }

    private static LimbAnimationState getExistingLimbState(IWrapperEntity player, PlayerLimb limb) {
        EnumMap<PlayerLimb, LimbAnimationState> states = playerStates.get(player);
        return states != null ? states.get(limb) : null;
    }

    private static Point3D getInterpolatedPosition(AEntityB_Existing entity, float partialTicks) {
        return entity.requiresDeltaUpdates() ? entity.prevPosition.copy().interpolate(entity.position, partialTicks) : entity.position.copy();
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

    private static final class SelectedTarget {
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

        private boolean update(boolean active, boolean oneShot, boolean triggerOnChange, int duration, int lingerDuration, double renderTime, UUID recentPlayerID) {
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
                    //A packet arriving before this definition's first rendered
                    //frame is a real interaction, rather than initial loading.
                    if (hadPlayerInteraction && (triggerOnChange || active)) {
                        triggerStartedAt = renderTime;
                        selectedPlayerID = interactingPlayerID;
                    }
                    return Double.isFinite(triggerStartedAt) && renderTime <= triggerStartedAt + duration + lingerDuration;
                }
                selectedPlayerID = active && hadPlayerInteraction ? interactingPlayerID : null;
                return !oneShot && active;
            }
            if (oneShot) {
                if ((triggerOnChange && active != wasActive) || (!triggerOnChange && active && !wasActive)) {
                    triggerStartedAt = renderTime;
                    selectedPlayerID = hadPlayerInteraction ? interactingPlayerID : null;
                }
                wasActive = active;
                return Double.isFinite(triggerStartedAt) && renderTime <= triggerStartedAt + duration + lingerDuration;
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
                deactivatedAt = renderTime;
                //An explicit deactivation transfers ownership to its actor.
                //If a momentary control releases automatically, retain the
                //player who initiated it so linger can complete for that same
                //player rather than disappearing on the falling edge.
                if (hadPlayerInteraction) {
                    selectedPlayerID = interactingPlayerID;
                }
            }
            wasActive = active;
            return active || (wasEverActive && Double.isFinite(deactivatedAt) && renderTime <= deactivatedAt + lingerDuration);
        }
    }

    private static final class VehicleAnimationState {
        private final EntityVehicleF_Physics vehicle;
        private final List<JSONPlayerAnimation> definitions;
        private final Map<JSONPlayerAnimation, AnimationSwitchbox> entrySwitchboxes = new IdentityHashMap<>();
        private final Map<JSONPlayerGripOverride, AnimationSwitchbox> overrideSwitchboxes = new IdentityHashMap<>();
        private final Map<JSONPlayerAnimation, TriggerState> entryTriggers = new IdentityHashMap<>();
        private final Map<JSONPlayerGripOverride, TriggerState> overrideTriggers = new IdentityHashMap<>();
        private final Map<JSONPlayerAnimation, Boolean> selectedEntries = new IdentityHashMap<>();
        private final Map<JSONPlayerGripOverride, Boolean> selectedOverrides = new IdentityHashMap<>();
        private UUID recentPlayerID;
        private long recentInteractionTick = Long.MIN_VALUE;

        private VehicleAnimationState(EntityVehicleF_Physics vehicle, List<JSONPlayerAnimation> definitions) {
            this.vehicle = vehicle;
            this.definitions = definitions;
            for (JSONPlayerAnimation definition : definitions) {
                entryTriggers.put(definition, new TriggerState());
                if (definition.activeAnimations != null) {
                    entrySwitchboxes.put(definition, new AnimationSwitchbox(vehicle, definition.activeAnimations, null));
                }
                if (definition.gripOverrides != null) {
                    for (JSONPlayerGripOverride override : definition.gripOverrides) {
                        overrideTriggers.put(override, new TriggerState());
                        if (override.activeAnimations != null) {
                            overrideSwitchboxes.put(override, new AnimationSwitchbox(vehicle, override.activeAnimations, null));
                        }
                    }
                }
            }
        }

        private void prepareFrame(float partialTicks, double renderTime) {
            UUID recentActor = vehicle.ticksExisted - recentInteractionTick <= 2 ? recentPlayerID : null;
            for (JSONPlayerAnimation definition : definitions) {
                AnimationSwitchbox switchbox = entrySwitchboxes.get(definition);
                boolean active = switchbox == null || switchbox.runSwitchbox(partialTicks, false);
                selectedEntries.put(definition, entryTriggers.get(definition).update(active, definition.oneShot, definition.triggerOnChange, getDuration(definition.duration), getLingerDuration(definition.lingerDuration), renderTime, definition.initiatingPlayerOnly ? recentActor : null));

                if (definition.gripOverrides != null) {
                    for (JSONPlayerGripOverride override : definition.gripOverrides) {
                        if (override.activeAnimations == null) {
                            selectedOverrides.put(override, true);
                            continue;
                        }
                        AnimationSwitchbox overrideSwitchbox = overrideSwitchboxes.get(override);
                        boolean overrideActive = overrideSwitchbox == null || overrideSwitchbox.runSwitchbox(partialTicks, false);
                        int duration = getDuration(override.duration != null ? override.duration : definition.duration);
                        int linger = getLingerDuration(override.lingerDuration != null ? override.lingerDuration : definition.lingerDuration);
                        boolean oneShot = override.oneShot != null ? override.oneShot : definition.oneShot;
                        boolean triggerOnChange = override.triggerOnChange != null ? override.triggerOnChange : definition.triggerOnChange;
                        selectedOverrides.put(override, overrideTriggers.get(override).update(overrideActive, oneShot, triggerOnChange, duration, linger, renderTime, null));
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

        private JSONPlayerGripOverride getSelectedOverride(JSONPlayerAnimation definition) {
            if (definition.gripOverrides != null) {
                for (JSONPlayerGripOverride override : definition.gripOverrides) {
                    if (Boolean.TRUE.equals(selectedOverrides.get(override))) {
                        return override;
                    }
                }
            }
            return null;
        }

        private boolean isModelObjectActive(String applyAfter, float partialTicks) {
            AnimationSwitchbox switchbox = vehicle.animatedObjectSwitchboxes.get(applyAfter);
            return switchbox == null || switchbox.runSwitchbox(partialTicks, false);
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
