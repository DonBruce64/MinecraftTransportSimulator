package minecrafttransportsimulator.entities.instances;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.jsondefs.JSONParticle.ParticleSpawningOrientation;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityParticleEmitter;

/**
 * Lightweight, generic synchronization point for a long-range particle definition.
 * The server owns one of these while the definition's active animations pass and
 * sends only transform snapshots.  Clients extrapolate the point and use the normal
 * {@link EntityParticle} implementation to create the actual effect locally.
 */
public final class EntityParticleEmitter extends AEntityC_Renderable {
    public static final int UPDATE_INTERVAL = 4;
    private static final int START_REFRESH_INTERVAL = 100;
    private static final int CLIENT_TIMEOUT = 100;
    private static final int MAX_DISTANCE_SEGMENTS_PER_TICK = 256;
    private static final RotationMatrix FACING_ADJUSTMENT = new RotationMatrix().rotateX(-90);

    private final UUID emitterUUID;
    private final JSONParticle definition;
    private final String packID;
    private final String systemName;
    private final String subName;
    private final int particleIndex;
    private final AEntityD_Definable<?> sourceEntity;
    private final Point3D extrapolationMotion = new Point3D();
    private final Point3D lastParticlePosition = new Point3D();
    private final Point3D previousServerPosition = new Point3D();
    private final RotationMatrix facingOrientation = new RotationMatrix();
    private final Map<UUID, IWrapperPlayer> subscribedPlayers = new HashMap<>();
    private final Set<UUID> eligiblePlayerIDs = new HashSet<>();
    private boolean hasServerPosition;
    private boolean hasParticlePosition;
    private boolean spawnedOnce;
    private boolean skipNextLocalSpawn;
    private int ticksSinceNetworkUpdate;

    private EntityParticleEmitter(AEntityD_Definable<?> sourceEntity, JSONParticle definition, int particleIndex) {
        super(sourceEntity.world, sourceEntity.position, ZERO_FOR_CONSTRUCTOR, ZERO_FOR_CONSTRUCTOR);
        this.emitterUUID = uniqueUUID;
        this.definition = definition;
        this.packID = sourceEntity.definition.packID;
        this.systemName = sourceEntity.definition.systemName;
        this.subName = sourceEntity.subDefinition.subName;
        this.particleIndex = particleIndex;
        this.sourceEntity = sourceEntity;
    }

    public EntityParticleEmitter(AWrapperWorld world, UUID emitterUUID, JSONParticle definition, String packID, String systemName, String subName, int particleIndex, Point3D position, Point3D extrapolationMotion, Point3D inheritedMotion, Point3D orientationAngles, Point3D scale) {
        super(world, position, inheritedMotion, orientationAngles);
        this.emitterUUID = emitterUUID;
        this.definition = definition;
        this.packID = packID;
        this.systemName = systemName;
        this.subName = subName;
        this.particleIndex = particleIndex;
        this.sourceEntity = null;
        this.extrapolationMotion.set(extrapolationMotion);
        this.scale.set(scale);
        this.prevScale.set(scale);
        this.hasServerPosition = true;
    }

    /**
     * Creates and starts a server-side emitter.  A null return means the requested
     * FACING particle does not currently have a valid face to spawn against.
     */
    public static EntityParticleEmitter createServerEmitter(AEntityD_Definable<?> sourceEntity, JSONParticle definition, int particleIndex, AnimationSwitchbox spawningSwitchbox) {
        EntityParticleEmitter emitter = new EntityParticleEmitter(sourceEntity, definition, particleIndex);
        if (!emitter.updateFromSource(spawningSwitchbox)) {
            return null;
        }
        sourceEntity.world.addEntity(emitter);
        emitter.synchronizePlayers(true);
        return emitter;
    }

    /** Updates the exact server anchor after the source entity has ticked. */
    public boolean updateFromSource(AnimationSwitchbox spawningSwitchbox) {
        previousServerPosition.set(position);
        RotationMatrix positionOrientation;
        if (definition.spawningOrientation == ParticleSpawningOrientation.WORLD) {
            positionOrientation = null;
            orientation.set(sourceEntity.orientation);
        } else if (definition.spawningOrientation == ParticleSpawningOrientation.FACING) {
            if (!(sourceEntity instanceof EntityBullet)) {
                return false;
            }
            Axis sideHit = ((EntityBullet) sourceEntity).sideHit;
            if (sideHit == null || sideHit == Axis.NONE) {
                return false;
            }
            facingOrientation.set(sideHit.facingRotation);
            facingOrientation.multiplyTranspose(FACING_ADJUSTMENT);
            positionOrientation = facingOrientation;
            orientation.set(facingOrientation);
        } else {
            positionOrientation = sourceEntity.orientation;
            orientation.set(sourceEntity.orientation);
        }

        EntityParticle.setPointToSpawn(sourceEntity.position, positionOrientation, definition.pos, sourceEntity.scale, spawningSwitchbox, position);
        if (hasServerPosition) {
            extrapolationMotion.set(position).subtract(previousServerPosition);
        }

        //EntityParticle normally applies vehicle speedFactor when inheriting motion.
        //The remote emitter is not itself a vehicle/part, so apply that factor here.
        motion.set(sourceEntity.motion);
        if (sourceEntity instanceof EntityVehicleF_Physics) {
            motion.scale(((EntityVehicleF_Physics) sourceEntity).speedFactor);
        } else if (sourceEntity instanceof APart) {
            APart sourcePart = (APart) sourceEntity;
            if (sourcePart.vehicleOn != null) {
                motion.scale(sourcePart.vehicleOn.speedFactor);
            }
        }
        if (!hasServerPosition) {
            //Until a second anchor exists, the scaled inherited motion is the best
            //estimate of the anchor's per-tick displacement.
            extrapolationMotion.set(motion);
            hasServerPosition = true;
        }
        scale.set(sourceEntity.scale);
        return true;
    }

    /** Applies a server correction while retaining per-tick local extrapolation. */
    public void applyNetworkState(Point3D serverPosition, Point3D serverExtrapolationMotion, Point3D inheritedMotion, Point3D orientationAngles, Point3D serverScale, boolean initialState) {
        if (initialState) {
            position.set(serverPosition);
            prevPosition.set(serverPosition);
            orientation.setToAngles(orientationAngles);
            prevOrientation.set(orientation);
        } else {
            position.set(serverPosition);
            orientation.setToAngles(orientationAngles);
        }
        extrapolationMotion.set(serverExtrapolationMotion);
        motion.set(inheritedMotion);
        scale.set(serverScale);
        if (initialState) {
            prevScale.set(serverScale);
        }
        ticksSinceNetworkUpdate = 0;
    }

    @Override
    public void update() {
        if (!isValid) {
            return;
        }
        super.update();
        if (world.isClient()) {
            if (++ticksSinceNetworkUpdate > CLIENT_TIMEOUT) {
                remove();
                return;
            }
            position.add(extrapolationMotion);
            if (skipNextLocalSpawn) {
                skipNextLocalSpawn = false;
            } else {
                spawnLocalParticles();
            }
        } else if (sourceEntity == null || !sourceEntity.isValid) {
            remove();
        } else if (ticksExisted % UPDATE_INTERVAL == 0) {
            //An occasional START upsert makes missed initial snapshots self-healing.
            synchronizePlayers(ticksExisted % START_REFRESH_INTERVAL == 0);
        }
    }

    private void spawnLocalParticles() {
        IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();
        if (clientPlayer == null || !definition.isWithinLongRangeRenderDistanceMax(position, clientPlayer.getPosition())) {
            //Do not bridge a trail across an interval that was outside the configured range.
            hasParticlePosition = false;
            return;
        }
        if (definition.spawningOrientation == ParticleSpawningOrientation.TRAIL) {
            if (!hasParticlePosition) {
                lastParticlePosition.set(position);
                hasParticlePosition = true;
                return;
            }
            if (definition.distance == 0 || !lastParticlePosition.isDistanceToCloserThan(position, definition.distance)) {
                Point3D trailDelta = position.copy().subtract(lastParticlePosition);
                double trailLength = trailDelta.length();
                if (trailLength > 0) {
                    Point3D trailPosition = lastParticlePosition.copy().interpolate(position, 0.5);
                    for (int i = 0; i < definition.quantity; ++i) {
                        EntityParticle particle = new EntityParticle(this, definition, trailPosition, trailDelta.copy().getAngles(true), null, true, true);
                        particle.setTrailSegmentLength(trailLength);
                        world.addEntity(particle);
                    }
                }
                lastParticlePosition.set(position);
            }
        } else if (definition.distance > 0) {
            if (!hasParticlePosition) {
                lastParticlePosition.set(position);
                hasParticlePosition = true;
                return;
            }
            int segmentsSpawned = 0;
            while (!lastParticlePosition.isDistanceToCloserThan(position, definition.distance) && segmentsSpawned++ < MAX_DISTANCE_SEGMENTS_PER_TICK) {
                double distanceFactor = definition.distance / position.distanceTo(lastParticlePosition);
                Point3D spawningPosition = lastParticlePosition.copy().interpolate(position, distanceFactor);
                Point3D spawningAngles = definition.spawningOrientation == ParticleSpawningOrientation.STREAK ? spawningPosition.copy().subtract(lastParticlePosition).getAngles(true) : null;
                for (int i = 0; i < definition.quantity; ++i) {
                    world.addEntity(new EntityParticle(this, definition, spawningPosition, spawningAngles, null, true, true));
                }
                lastParticlePosition.set(spawningPosition);
            }
            if (segmentsSpawned > MAX_DISTANCE_SEGMENTS_PER_TICK) {
                //A teleport or stale correction must not create an unbounded particle burst.
                lastParticlePosition.set(position);
            }
        } else if (!spawnedOnce || definition.spawnEveryTick) {
            for (int i = 0; i < definition.quantity; ++i) {
                world.addEntity(new EntityParticle(this, definition, position, null, null, true, true));
            }
            spawnedOnce = true;
        }
    }

    /**
     * Seeds the client emitter as soon as its START packet is handled.  This is
     * important for one-tick effects whose STOP packet may arrive before the next
     * client tick; distance/trail emitters simply remember their first anchor here.
     */
    public void spawnInitialParticles() {
        if (world.isClient()) {
            spawnLocalParticles();
            skipNextLocalSpawn = definition.spawnEveryTick && definition.spawningOrientation != ParticleSpawningOrientation.TRAIL && definition.distance <= 0;
        }
    }

    public void sendStart(IWrapperPlayer player) {
        if (isPlayerEligible(player)) {
            player.sendPacket(new PacketEntityParticleEmitter(this, true));
            subscribedPlayers.put(player.getID(), player);
        }
    }

    public static void sendActiveEmittersTo(AWrapperWorld world, IWrapperPlayer player) {
        for (EntityParticleEmitter emitter : world.getEntitiesOfType(EntityParticleEmitter.class)) {
            if (emitter.isValid && !world.isClient()) {
                emitter.sendStart(player);
            }
        }
    }

    public static EntityParticleEmitter getClientEmitter(AWrapperWorld world, UUID emitterUUID) {
        for (EntityParticleEmitter emitter : world.getEntitiesOfType(EntityParticleEmitter.class)) {
            if (emitter.emitterUUID.equals(emitterUUID)) {
                return emitter;
            }
        }
        return null;
    }

    @Override
    public void remove() {
        if (isValid && !world.isClient()) {
            PacketEntityParticleEmitter stopPacket = new PacketEntityParticleEmitter(this);
            for (IWrapperPlayer player : subscribedPlayers.values()) {
                if (player.isValid() && player.getWorld() == world) {
                    player.sendPacket(stopPacket);
                }
            }
            subscribedPlayers.clear();
            eligiblePlayerIDs.clear();
        }
        super.remove();
    }

    /** Reconciles this emitter's same-world recipients without a large spatial entity query. */
    private void synchronizePlayers(boolean forceStart) {
        eligiblePlayerIDs.clear();
        PacketEntityParticleEmitter startPacket = null;
        PacketEntityParticleEmitter updatePacket = null;
        for (IWrapperPlayer player : world.getPlayers()) {
            if (!isPlayerEligible(player)) {
                continue;
            }

            UUID playerID = player.getID();
            eligiblePlayerIDs.add(playerID);
            if (forceStart || !subscribedPlayers.containsKey(playerID)) {
                if (startPacket == null) {
                    startPacket = new PacketEntityParticleEmitter(this, true);
                }
                player.sendPacket(startPacket);
            } else {
                if (updatePacket == null) {
                    updatePacket = new PacketEntityParticleEmitter(this, false);
                }
                player.sendPacket(updatePacket);
            }
            subscribedPlayers.put(playerID, player);
        }

        PacketEntityParticleEmitter stopPacket = null;
        Iterator<Map.Entry<UUID, IWrapperPlayer>> iterator = subscribedPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, IWrapperPlayer> entry = iterator.next();
            if (!eligiblePlayerIDs.contains(entry.getKey())) {
                IWrapperPlayer player = entry.getValue();
                if (player.isValid() && player.getWorld() == world) {
                    if (stopPacket == null) {
                        stopPacket = new PacketEntityParticleEmitter(this);
                    }
                    player.sendPacket(stopPacket);
                }
                iterator.remove();
            }
        }
    }

    private boolean isPlayerEligible(IWrapperPlayer player) {
        return player != null && player.isValid() && player.getWorld() == world && definition.isWithinLongRangeRenderDistanceMax(position, player.getPosition());
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return true;
    }

    @Override
    public boolean shouldSync() {
        return false;
    }

    @Override
    public boolean shouldSavePosition() {
        return false;
    }

    @Override
    public int getWorldLightValue() {
        return 0x00F000F0;
    }

    @Override
    protected void renderModel(TransformationMatrix transform, boolean blendingEnabled, float partialTicks) {
        //Emitters are synchronization points only and have no geometry of their own.
    }

    @Override
    protected boolean disableRendering() {
        return true;
    }

    public UUID getEmitterUUID() {
        return emitterUUID;
    }

    public String getPackID() {
        return packID;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getSubName() {
        return subName;
    }

    public int getParticleIndex() {
        return particleIndex;
    }

    public Point3D getExtrapolationMotion() {
        return extrapolationMotion;
    }
}
