package minecrafttransportsimulator.rendering;

import static minecrafttransportsimulator.jsondefs.JSONPlayerAnimation.DEFAULT_DURATION;
import static minecrafttransportsimulator.jsondefs.JSONPlayerAnimation.JSONPlayerGripOverride.INHERIT_DURATION;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
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
 * Resolves vehicle and part player limb targets into Minecraft model rotations
 * and length scales. This class contains no Minecraft classes, so every game
 * interface shares selection, priority, interpolation, and trigger behavior.
 * <br><br>
 * Each source evaluates its entries and trigger state once per client tick.
 * Rendering reads that selection and interpolates the reach and release.
 * Everything here runs on the single client thread, so scratch objects are shared.
 */
public final class PlayerAnimationSystem {
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

    //Only sources with animation definitions have state. Parts unregister on removal.
    private static final Map<AEntityF_Multipart<?>, AnimationState> animationStates = new IdentityHashMap<>();
    private static final Map<AEntityF_Multipart<?>, List<AnimationState>> partStatesByAssembly = new IdentityHashMap<>();
    private static final List<AnimationState> exteriorStates = new ArrayList<>();
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

    /** Evaluates the source's entries once per tick and registers it for pose lookup. */
    public static void updateSource(AEntityF_Multipart<?> source) {
        List<JSONPlayerAnimation> definitions = getDefinitions(source);
        if (source.isValid && definitions != null && !definitions.isEmpty() && playerTweaksEnabled()) {
            getSourceState(source, definitions).tick();
        } else if (!animationStates.isEmpty()) {
            //Discard old trigger state when player tweaks are turned off.
            removeState(animationStates.get(source));
        }
    }

    /**
     * Returns the limbs of this player that currently have a pose, either selected or releasing.
     * This is empty for anything
     * that is not a player and while the playerTweaks setting is off. The result is cached per
     * player for the rendered frame, so it is cheap to call from several render events. The
     * returned set is shared: do not modify it.
     */
    public static EnumSet<PlayerLimb> getActiveLimbs(IWrapperEntity entity, float partialTicks) {
        PlayerState state = entity instanceof IWrapperPlayer ? getPlayerState((IWrapperPlayer) entity, partialTicks) : null;
        return state != null ? state.activeLimbs : NO_LIMBS;
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
        if (state == null || !state.activeLimbs.contains(limb)) {
            return false;
        }
        output.set(state.poses[limb.ordinal()]);
        return true;
    }

    /** Credits the actor to the source and any other animated parts that read its changed variable. */
    public static void recordVariableInteraction(AEntityD_Definable<?> source, ComputedVariable variable, UUID playerID) {
        recordInteraction(source, variable, playerID);
    }

    /** Records GUI open/close for the source's own animations and its vehicle's derived triggers. */
    public static void recordPlayerInteraction(AEntityD_Definable<?> source, UUID playerID) {
        recordInteraction(source, null, playerID);
    }

    private static void recordInteraction(AEntityD_Definable<?> source, ComputedVariable variable, UUID playerID) {
        if (!(source instanceof AEntityF_Multipart) || !source.world.isClient() || playerID == null || !playerTweaksEnabled()) {
            return;
        }
        AEntityF_Multipart<?> assembly = source instanceof APart ? ((APart) source).masterEntity : (AEntityF_Multipart<?>) source;
        if (assembly instanceof EntityVehicleF_Physics) {
            AnimationState state = getInteractionState(assembly);
            if (state != null) {
                state.recordInteraction(variable, playerID, true);
            }
        }
        if (source instanceof APart) {
            AnimationState state = getInteractionState((APart) source);
            if (state != null) {
                state.recordInteraction(variable, playerID, true);
            }
        }
        List<AnimationState> partStates = partStatesByAssembly.get(assembly);
        if (variable != null && partStates != null) {
            for (AnimationState state : partStates) {
                if (state.source != source) {
                    state.recordInteraction(variable, playerID, false);
                }
            }
        }
    }

    private static AnimationState getInteractionState(AEntityF_Multipart<?> source) {
        List<JSONPlayerAnimation> definitions = getDefinitions(source);
        return source.isValid && definitions != null && !definitions.isEmpty() ? getSourceState(source, definitions) : null;
    }

    private static PlayerState getPlayerState(IWrapperPlayer player, float partialTicks) {
        PlayerState state = playerStates.get(player);
        if (state == null) {
            if (animationStates.isEmpty()) {
                return null;
            }
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
        PartSeat seat = ridingEntity instanceof PartSeat ? (PartSeat) ridingEntity : null;
        AnimationState vehicleState = seat != null ? animationStates.get(seat.masterEntity) : null;
        List<AnimationState> partStates = seat != null ? partStatesByAssembly.get(seat.masterEntity) : null;
        for (PlayerLimb limb : LIMBS) {
            int index = limb.ordinal();
            LimbAnimationState limbState = state.limbs[index];
            PlayerLimbPose pose = state.poses[index];
            boolean hasPose;
            if (!enabled) {
                limbState.clear();
                hasPose = false;
            } else if (seat != null) {
                hasPose = resolveSeatedPose(player, seat, vehicleState, partStates, limb, limbState, partialTicks, pose);
            } else {
                hasPose = resolveExteriorPose(player, limb, limbState, partialTicks, pose);
            }
            if (hasPose) {
                state.activeLimbs.add(limb);
            }
        }
        return state;
    }

    private static boolean resolveSeatedPose(IWrapperPlayer player, PartSeat seat, AnimationState vehicleState, List<AnimationState> partStates,
            PlayerLimb limb, LimbAnimationState limbState, float partialTicks, PlayerLimbPose output) {
        AEntityF_Multipart<?> assembly = seat.masterEntity;
        SelectedTarget selected = vehicleState != null ? vehicleState.getSeatedTarget(seat, player, limb, partialTicks) : null;
        if (selected == null && partStates != null) {
            for (int i = 0; i < partStates.size() && selected == null; ++i) {
                selected = partStates.get(i).getSeatedTarget(seat, player, limb, partialTicks);
            }
        }
        double renderTime = getRenderTime(assembly, partialTicks);
        if (selected != null) {
            calculateWorldTarget(selected, partialTicks, scratchTarget);
            if (calculateSeatedTargetPose(player, seat, scratchTarget, limb, selected.limbScale, partialTicks, targetPose)) {
                return applySelectedPose(limbState, assembly, selected, targetPose, renderTime, false, output);
            }
        }
        return releasePose(limbState, assembly, renderTime, output);
    }

    /** The index belongs to the entity the seat is mounted on. */
    private static boolean matchesSeatSlot(AEntityF_Multipart<?> source, PartSeat seat) {
        while (source instanceof APart && source != seat.entityOn) {
            source = ((APart) source).entityOn;
        }
        return source == seat.entityOn;
    }

    private static boolean resolveExteriorPose(IWrapperPlayer player, PlayerLimb limb, LimbAnimationState limbState, float partialTicks, PlayerLimbPose output) {
        SelectedTarget closestTarget = null;
        double closestDistance = Double.MAX_VALUE;
        if (!exteriorStates.isEmpty()) {
            scratchTorso.set(player.getInterpolatedPosition(partialTicks)).add(0, player.getEyeHeight() * 2D / 3D, 0);
        }

        for (int stateIndex = 0; stateIndex < exteriorStates.size(); ++stateIndex) {
            AnimationState state = exteriorStates.get(stateIndex);
            AEntityF_Multipart<?> source = state.source;
            if (!source.isValid) {
                removeState(state);
                --stateIndex;
                continue;
            }
            if (!source.world.equals(player.getWorld()) || !state.isAvailable()) {
                continue;
            }
            scratchScale.set(source.prevScale).interpolate(source.scale, partialTicks);
            double scale = Math.max(Math.abs(scratchScale.x), Math.max(Math.abs(scratchScale.y), Math.abs(scratchScale.z)));
            setInterpolatedPosition(source, partialTicks, scratchPosition);
            if (!scratchTorso.isDistanceToCloserThan(scratchPosition, state.exteriorReach * scale + state.maxExteriorRange + EXTERIOR_CULL_MARGIN)) {
                continue;
            }

            //List priority applies within one source. Across sources the closest eligible target wins.
            for (int i = 0; i < state.definitions.size(); ++i) {
                JSONPlayerAnimation definition = state.definitions.get(i);
                if (definition.limb != limb || definition.range <= 0 || !state.isEntrySelected(definition, player)) {
                    continue;
                }
                SelectedTarget candidate = state.getSelectedTarget(definition, partialTicks);
                if (candidate != null) {
                    calculateWorldTarget(candidate, partialTicks, scratchTarget);
                    double distance = scratchTorso.distanceTo(scratchTarget);
                    if (distance <= definition.range) {
                        if (distance < closestDistance) {
                            closestTarget = candidate;
                            scratchBestTarget.set(scratchTarget);
                            closestDistance = distance;
                        }
                        break;
                    }
                }
            }
        }

        if (closestTarget == null) {
            if (!limbState.exterior || limbState.anchor == null) {
                limbState.clear();
                return false;
            }
            return releasePose(limbState, limbState.anchor, getRenderTime(limbState.anchor, partialTicks), output);
        }

        AEntityF_Multipart<?> source = closestTarget.source;
        double renderTime = getRenderTime(source, partialTicks);
        if (!calculateExteriorTargetPose(player, scratchBestTarget, limb, closestTarget.limbScale, partialTicks, targetPose)) {
            return releasePose(limbState, source, renderTime, output);
        }
        return applySelectedPose(limbState, source, closestTarget, targetPose, renderTime, true, output);
    }

    /** Transforms the target's authored point with the same interpolated transform as its model. */
    private static void calculateWorldTarget(SelectedTarget selected, float partialTicks, Point3D output) {
        AEntityF_Multipart<?> source = selected.source;
        output.set(selected.position);
        AnimationSwitchbox objectSwitchbox = source.animatedObjectSwitchboxes.get(selected.applyAfter);
        if (objectSwitchbox != null) {
            output.transform(objectSwitchbox.netMatrix);
        }
        scratchScale.set(source.prevScale).interpolate(source.scale, partialTicks);
        source.getInterpolatedOrientation(scratchOrientation, partialTicks);
        output.multiply(scratchScale).rotate(scratchOrientation);
        setInterpolatedPosition(source, partialTicks, scratchPosition);
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

    private static boolean applySelectedPose(LimbAnimationState limbState, AEntityF_Multipart<?> anchor, SelectedTarget selected, PlayerLimbPose targetPose, double renderTime, boolean exterior, PlayerLimbPose output) {
        if (limbState.anchor != anchor || limbState.selection != selected || limbState.exterior != exterior) {
            boolean freshSelection = limbState.anchor != anchor || (limbState.selection == null && !limbState.releasing) || limbState.exterior != exterior;
            limbState.captureCurrentPose(renderTime);
            if (freshSelection) {
                limbState.transitionPose.set(targetPose);
                limbState.transitionPose.blend = 0;
            }
            limbState.anchor = anchor;
            limbState.selection = selected;
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

    private static boolean releasePose(LimbAnimationState limbState, AEntityF_Multipart<?> anchor, double renderTime, PlayerLimbPose output) {
        if (limbState.selection == null && !limbState.releasing) {
            return false;
        }
        if (anchor == null || !anchor.isValid || !Double.isFinite(renderTime) || limbState.anchor != anchor) {
            limbState.clear();
            return false;
        }
        if (!limbState.releasing) {
            limbState.captureCurrentPose(renderTime);
            limbState.selection = null;
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

    private static List<JSONPlayerAnimation> getDefinitions(AEntityF_Multipart<?> source) {
        return source.definition.rendering != null ? source.definition.rendering.playerAnimations : null;
    }

    /** Creates state for a source or replaces it if its definition was reloaded. */
    private static AnimationState getSourceState(AEntityF_Multipart<?> source, List<JSONPlayerAnimation> definitions) {
        AnimationState state = animationStates.get(source);
        if (state == null || state.definitions != definitions) {
            removeState(state);
            state = new AnimationState(source, definitions);
            animationStates.put(source, state);
            if (state.maxExteriorRange > 0) {
                exteriorStates.add(state);
            }
            if (source instanceof APart) {
                AEntityF_Multipart<?> assembly = ((APart) source).masterEntity;
                partStatesByAssembly.computeIfAbsent(assembly, ignored -> new ArrayList<>()).add(state);
            }
        }
        return state;
    }

    private static void removeState(AnimationState state) {
        if (state != null) {
            animationStates.remove(state.source);
            exteriorStates.remove(state);
            if (state.source instanceof APart) {
                AEntityF_Multipart<?> assembly = ((APart) state.source).masterEntity;
                List<AnimationState> partStates = partStatesByAssembly.get(assembly);
                partStates.remove(state);
                if (partStates.isEmpty()) {
                    partStatesByAssembly.remove(assembly);
                }
            }
        }
    }

    /** Unregisters a removed source and invalidates any player poses resolved against it this frame. */
    public static void removeSource(AEntityF_Multipart<?> source) {
        AnimationState state = animationStates.get(source);
        if (state == null) {
            return;
        }
        removeState(state);
        for (PlayerState playerState : playerStates.values()) {
            for (LimbAnimationState limbState : playerState.limbs) {
                if (limbState.anchor == source) {
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
        private final AEntityF_Multipart<?> source;
        private final String applyAfter;
        private final Point3D position;
        private final Point3D limbScale;
        private final int duration;

        private SelectedTarget(AEntityF_Multipart<?> source, String applyAfter, Point3D position, Point3D limbScale, int duration) {
            this.source = source;
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
        //Seated transitions share their assembly's clock, even when the target part changes.
        private AEntityF_Multipart<?> anchor;
        private SelectedTarget selection;
        private final PlayerLimbPose transitionPose = new PlayerLimbPose();
        private final PlayerLimbPose currentPose = new PlayerLimbPose();
        private double transitionStart;
        private double lastRenderTime;
        private double startBlend;
        private int transitionDuration = DEFAULT_DURATION;
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
            anchor = null;
            selection = null;
            transitionPose.set(0, 0, 0, 1, 1, 1, 1, 0);
            currentPose.set(transitionPose);
            transitionStart = 0;
            lastRenderTime = 0;
            startBlend = 0;
            transitionDuration = DEFAULT_DURATION;
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

        /** Advances this trigger by one source tick. Returns whether the entry is selected. */
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

    private static final class AnimationState {
        private final AEntityF_Multipart<?> source;
        private final List<JSONPlayerAnimation> definitions;
        private final boolean hasSeatedEntries;
        /** Farthest exterior target from the source origin, in unscaled model units. */
        private double exteriorReach;
        private double maxExteriorRange;
        private final Map<JSONPlayerAnimation, AnimationSwitchbox> entrySwitchboxes = new IdentityHashMap<>();
        private final Map<JSONPlayerGripOverride, AnimationSwitchbox> overrideSwitchboxes = new IdentityHashMap<>();
        private final List<JSONPlayerAnimation> actorDefinitions = new ArrayList<>();
        private final Map<JSONPlayerAnimation, TriggerState> entryTriggers = new IdentityHashMap<>();
        private final Map<JSONPlayerGripOverride, TriggerState> overrideTriggers = new IdentityHashMap<>();
        private final Map<JSONPlayerAnimation, Boolean> selectedEntries = new IdentityHashMap<>();
        private final Map<JSONPlayerGripOverride, Boolean> selectedOverrides = new IdentityHashMap<>();
        /** Keyed by entry or grip override. */
        private final Map<Object, SelectedTarget> targets = new IdentityHashMap<>();
        private UUID recentPlayerID;
        private long recentInteractionTick = Long.MIN_VALUE;

        private AnimationState(AEntityF_Multipart<?> source, List<JSONPlayerAnimation> definitions) {
            this.source = source;
            this.definitions = definitions;
            boolean seated = false;
            for (JSONPlayerAnimation definition : definitions) {
                seated |= definition.range <= 0;
                if (definition.range > 0) {
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
                if (definition.initiatingPlayerOnly) {
                    actorDefinitions.add(definition);
                }
                entryTriggers.put(definition, new TriggerState());
                addTarget(definition, definition.applyAfter, definition.pos, definition.limbScale, definition.duration);
                if (definition.activeAnimations != null) {
                    entrySwitchboxes.put(definition, new AnimationSwitchbox(source, definition.activeAnimations, null));
                }
                if (definition.gripOverrides != null) {
                    for (JSONPlayerGripOverride override : definition.gripOverrides) {
                        overrideTriggers.put(override, new TriggerState());
                        addTarget(override, override.applyAfter != null ? override.applyAfter : definition.applyAfter, override.pos, override.limbScale != null ? override.limbScale : definition.limbScale, override.duration != INHERIT_DURATION ? override.duration : definition.duration);
                        if (override.activeAnimations != null) {
                            overrideSwitchboxes.put(override, new AnimationSwitchbox(source, override.activeAnimations, null));
                        }
                    }
                }
            }
            this.hasSeatedEntries = seated;
        }

        private void addTarget(Object key, String applyAfter, Point3D position, Point3D limbScale, int duration) {
            if (applyAfter != null && position != null) {
                targets.put(key, new SelectedTarget(source, applyAfter, position, limbScale != null ? limbScale : DEFAULT_LIMB_SCALE, Math.max(0, duration)));
            }
        }

        /** Evaluates every entry and grip override for this source tick. */
        private void tick() {
            double time = source.ticksExisted;
            UUID recentActor = source.ticksExisted - recentInteractionTick <= 2 ? recentPlayerID : null;
            for (JSONPlayerAnimation definition : definitions) {
                AnimationSwitchbox switchbox = entrySwitchboxes.get(definition);
                boolean active = switchbox == null || switchbox.runSwitchbox(0, false);
                selectedEntries.put(definition, entryTriggers.get(definition).update(active, definition.oneShot, definition.triggerOnChange, Math.max(0, definition.duration), Math.max(0, definition.lingerDuration), time, definition.initiatingPlayerOnly ? recentActor : null));

                if (definition.gripOverrides != null) {
                    for (JSONPlayerGripOverride override : definition.gripOverrides) {
                        if (override.activeAnimations == null) {
                            selectedOverrides.put(override, true);
                            continue;
                        }
                        AnimationSwitchbox overrideSwitchbox = overrideSwitchboxes.get(override);
                        boolean overrideActive = overrideSwitchbox == null || overrideSwitchbox.runSwitchbox(0, false);
                        int duration = Math.max(0, override.duration != INHERIT_DURATION ? override.duration : definition.duration);
                        int linger = Math.max(0, override.lingerDuration != INHERIT_DURATION ? override.lingerDuration : definition.lingerDuration);
                        boolean oneShot = override.oneShot != null ? override.oneShot : definition.oneShot;
                        boolean triggerOnChange = override.triggerOnChange != null ? override.triggerOnChange : definition.triggerOnChange;
                        selectedOverrides.put(override, overrideTriggers.get(override).update(overrideActive, oneShot, triggerOnChange, duration, linger, time, null));
                    }
                }
            }
        }

        private void recordInteraction(ComputedVariable variable, UUID playerID, boolean allowRecentActor) {
            if (actorDefinitions.isEmpty()) {
                return;
            }
            if (allowRecentActor) {
                recentPlayerID = playerID;
                recentInteractionTick = source.ticksExisted;
            }
            if (variable != null) {
                for (JSONPlayerAnimation definition : actorDefinitions) {
                    if (referencesVariable(definition.activeAnimations, variable)) {
                        entryTriggers.get(definition).recordPlayerInteraction(playerID);
                    }
                }
            }
        }

        private boolean isAvailable() {
            return source.isValid && (!(source instanceof APart) || !((APart) source).disableRendering()) && definitions == getDefinitions(source);
        }

        private SelectedTarget getSeatedTarget(PartSeat seat, IWrapperPlayer player, PlayerLimb limb, float partialTicks) {
            if (!hasSeatedEntries || !isAvailable()) {
                return null;
            }
            boolean matchesSlot = matchesSeatSlot(source, seat);
            for (int i = 0; i < definitions.size(); ++i) {
                JSONPlayerAnimation definition = definitions.get(i);
                if (definition.limb != limb || definition.range > 0) {
                    continue;
                }
                boolean matches = definition.seatSlot != null ? matchesSlot && definition.appliesToSeat(seat.placementSlot) : source == seat;
                if (matches && isEntrySelected(definition, player)) {
                    SelectedTarget target = getSelectedTarget(definition, partialTicks);
                    if (target != null) {
                        return target;
                    }
                }
            }
            return null;
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
            AnimationSwitchbox objectSwitchbox = source.animatedObjectSwitchboxes.get(target.applyAfter);
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

        private boolean referencesVariable(List<JSONAnimationDefinition> animations, ComputedVariable changedVariable) {
            if (animations == null) {
                return false;
            }
            for (JSONAnimationDefinition animation : animations) {
                if (animation.variable != null) {
                    ComputedVariable watchedVariable = source.getOrCreateVariable(normalizeVariable(animation.variable));
                    if (watchedVariable.entity == changedVariable.entity
                            && watchedVariable.variableKey.equals(normalizeVariable(changedVariable.variableKey))) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static String normalizeVariable(String variable) {
            return variable.startsWith(ComputedVariable.INVERTED_PREFIX) ? variable.substring(ComputedVariable.INVERTED_PREFIX.length()) : variable;
        }
    }
}
