package mcinterface1211;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.BoundingBoxHitResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@EventBusSubscriber(modid = InterfaceLoader.MODID, value = Dist.CLIENT)
public class FlashbackCompat {
    private static final String REPLAY_SERVER = "com.moulberry.flashback.playback.ReplayServer";
    public static final Map<UUID, UUID> BUILDERS_UUID = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> BUILDERS_ID = new ConcurrentHashMap<>();
    public static final Map<UUID, CompoundTag> DATA = new ConcurrentHashMap<>();
    public static final Map<UUID, Point3D[]> TRANSFORMS = new ConcurrentHashMap<>();
    public static final Set<UUID> MUTED = ConcurrentHashMap.newKeySet();

    public static boolean renderVehicles = true;
    public static boolean ghostPass = false;

    private static UUID selectedVehicle;
    private static BuilderEntityRenderForwarder localRenderForwarder;

    public static boolean wasFrozen;

    private static final Map<UUID, double[]> LAST_WRITTEN = new HashMap<>();
    private static final Map<UUID, double[]> LAST_ENGINE = new HashMap<>();
    private static long recordTick;
    private static final Point3D authPos = new Point3D();
    private static final Point3D authAngles = new Point3D();
    private static Field recorderField;
    private static Method writePacketMethod;
    private static boolean reflectionFailed;

    public static boolean pickingActive;
    private static long hiddenStateStamp;
    private static Method lookVectorMethod;

    // Cache
    private static boolean cachedReplayClient;
    private static Set<UUID> cachedHidden;
    private static boolean cachedExporting;


    // Queue
    private static long packetSequence;
    private static int queuedTotal;
    private static final Map<UUID, ArrayDeque<QueuedPacket>> QUEUED_PACKETS = new HashMap<>();
    private static final Map<UUID, Long> DATA_SEQUENCE = new HashMap<>();

    public static void markDataArrived(UUID builderUUID) {
        DATA_SEQUENCE.put(builderUUID, packetSequence);
    }

    public static void queuePacket(UUID target, APacketBase packet) {
        if (queuedTotal > 300_000) {
            QUEUED_PACKETS.clear();
            queuedTotal = 0;
        }
        ArrayDeque<QueuedPacket> queue = QUEUED_PACKETS.computeIfAbsent(target, uuid -> new ArrayDeque<>());
        queue.addLast(new QueuedPacket(++packetSequence, packet));
        ++queuedTotal;
        if (queue.size() > 20_000) {
            queue.removeFirst();
            --queuedTotal;
        }
    }

    public static void applyQueuedPackets(UUID builderUUID, AEntityB_Existing entity) {
        if (QUEUED_PACKETS.isEmpty()) {
            return;
        }
        long baseline = DATA_SEQUENCE.getOrDefault(builderUUID, 0L);
        List<QueuedPacket> toApply = new ArrayList<>();
        List<UUID> targets = new ArrayList<>();
        targets.add(entity.uniqueUUID);
        if (entity instanceof AEntityF_Multipart) {
            for (APart part : ((AEntityF_Multipart<?>) entity).allParts) {
                targets.add(part.uniqueUUID);
            }
        }
        for (UUID target : targets) {
            ArrayDeque<QueuedPacket> queue = QUEUED_PACKETS.remove(target);
            if (queue != null) {
                queuedTotal -= queue.size();
                for (QueuedPacket queuedPacket : queue) {
                    if (queuedPacket.sequence >= baseline) {
                        toApply.add(queuedPacket);
                    }
                }
            }
        }
        toApply.sort(Comparator.comparingLong(queuedPacket -> queuedPacket.sequence));
        AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
        for (QueuedPacket queuedPacket : toApply) {
            try {
                queuedPacket.packet.handle(world);
            } catch (Exception e) {
                // Ignore bad packet
            }
        }
    }

    public static boolean isReplayServer(MinecraftServer server) {
        return server != null && server.getClass().getName().equals(REPLAY_SERVER);
    }

    public static boolean isReplayServerLevel(Level level) {
        return !level.isClientSide() && isReplayServer(level.getServer());
    }

    public static boolean isReplayClient() {
        return isReplayServer(Minecraft.getInstance().getSingleplayerServer());
    }

    public static boolean isFrozen() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.tickRateManager().isFrozen();
    }

    public static boolean shouldSkipWorldTick(AWrapperWorld world) {
        cachedReplayClient = isReplayClient();
        if (cachedReplayClient) {
            ensureLocalRenderForwarder();
        } else {
            wasFrozen = false;
            return false;
        }
        boolean frozen = isFrozen();
        if (frozen && !wasFrozen) {
            for (EntityVehicleF_Physics vehicle : world.getEntitiesOfType(EntityVehicleF_Physics.class)) {
                vehicle.prevPosition.set(vehicle.position);
                vehicle.prevOrientation.set(vehicle.orientation);
            }
        }
        wasFrozen = frozen;
        return frozen;
    }


    private static void ensureLocalRenderForwarder() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (localRenderForwarder != null && (localRenderForwarder.isRemoved() || localRenderForwarder.level() != mc.level)) {
            localRenderForwarder = null;
        }
        if (localRenderForwarder == null) {
            BuilderEntityRenderForwarder renderForwarder = new BuilderEntityRenderForwarder(mc.player);
            renderForwarder.loadFromSavedNBT = true;
            renderForwarder.noCulling = true;
            mc.level.addEntity(renderForwarder);
            localRenderForwarder = renderForwarder;
        }

        Entity camera = mc.getCameraEntity();
        if (camera != null) {
            Vec3 look = camera.getLookAngle();
            //todo: maybe needs changing position
            localRenderForwarder.setPos(
                    camera.getX() + look.x * 1.5D,
                    camera.getY() + look.y * 1.5D,
                    camera.getZ() + look.z * 1.5D
            );
        }
    }

    public static boolean isLocalRenderForwarder(BuilderEntityRenderForwarder renderForwarder) {
        return localRenderForwarder == renderForwarder;
    }

    public static boolean isReplayFrozen() {
        return isReplayClient() && isFrozen();
    }

    private static void refreshHideState() {
        long now = System.nanoTime();
        if (now - hiddenStateStamp < 100_000_000L) {
            return;
        }
        hiddenStateStamp = now;
        cachedExporting = false;
        cachedHidden = null;
        if (reflectionFailed) {
            return;
        }
        try {
            cachedExporting = (Boolean) Class.forName("com.moulberry.flashback.Flashback").getMethod("isExporting").invoke(null);
            Object state = Class.forName("com.moulberry.flashback.state.EditorStateManager").getMethod("getCurrent").invoke(null);
            if (state != null) {
                cachedHidden = (Set<UUID>) state.getClass().getField("hideDuringExport").get(state);
            }
        } catch (Throwable t) {
            cachedExporting = false;
            cachedHidden = null;
        }
    }

    private static boolean isMarkedHidden(UUID vehicleUUID) {
        refreshHideState();
        UUID entityUUID = BUILDERS_UUID.get(vehicleUUID);
        return entityUUID != null && cachedHidden != null && cachedHidden.contains(entityUUID);
    }

    public static boolean isVehicleMarkedHidden(AEntityB_Existing entity) {
        AEntityB_Existing root = getRootEntity(entity);
        return root instanceof EntityVehicleF_Physics && isMarkedHidden(root.uniqueUUID);
    }

    public static void markHidden(UUID vehicleUUID, boolean hidden) {
        refreshHideState();
        UUID entityUUID = BUILDERS_UUID.get(vehicleUUID);
        if (hidden && !cachedHidden.contains(entityUUID)) {
            cachedHidden.add(entityUUID);
        } else {
            cachedHidden.remove(entityUUID);
        }

        //Mark dirty so Flashback saves it.
        try {
            Object state = Class.forName("com.moulberry.flashback.state.EditorStateManager")
                    .getMethod("getCurrent").invoke(null);
            if (state != null) {
                state.getClass().getMethod("markDirty").invoke(state);
            }
        } catch (Throwable ignored) {}
    }

    private static boolean isHiddenForExport(UUID vehicleUUID) {
        return isMarkedHidden(vehicleUUID) && cachedExporting;
    }

    private static AEntityB_Existing getRootEntity(AEntityB_Existing entity) {
        for (int i = 0; i < 5 && entity != null; i++) {
            if (entity instanceof APart) {
                entity = ((APart) entity).masterEntity;
            } else if (entity instanceof EntityParticle) {
                entity = ((EntityParticle) entity).getSpawningEntity();
            } else {
                break;
            }
        }
        return entity;
    }

    public static boolean isGhost(AEntityB_Existing entity) {
        if (!cachedReplayClient || !renderVehicles) {
            return false;
        }
        AEntityB_Existing root = getRootEntity(entity);
        return root instanceof EntityVehicleF_Physics && isMarkedHidden(root.uniqueUUID) && !cachedExporting;
    }

    public static boolean isVehicleHidden(AEntityB_Existing entity) {
        AEntityB_Existing root = getRootEntity(entity);
        if (!(root instanceof EntityVehicleF_Physics)) {
            return false;
        }

        return !renderVehicles || isHiddenForExport(root.uniqueUUID);
    }

    public static boolean isVehicleMuted(AEntityB_Existing entity) {
        if (!cachedReplayClient) {
            return false;
        }
        AEntityB_Existing root = getRootEntity(entity);
        if (!(root instanceof EntityVehicleF_Physics)) {
            return false;
        }
        return !renderVehicles || MUTED.contains(root.uniqueUUID) || isHiddenForExport(root.uniqueUUID);
    }

    public static boolean isVehicle(Entity entity) {
        return entity instanceof BuilderEntityExisting && ((BuilderEntityExisting) entity).entity instanceof EntityVehicleF_Physics;
    }

    public static void markSelected(Entity entity) {
        if (isVehicle(entity)) {
            selectedVehicle = ((BuilderEntityExisting) entity).entity.uniqueUUID;
        }
    }

    public static HitResult pickVehicle(HitResult flashbackResult) {
        if (!cachedReplayClient || !renderVehicles) {
            return null;
        }

        Vec3 look;
        try {
            if (lookVectorMethod == null) {
                lookVectorMethod = Class.forName("com.moulberry.flashback.editor.ui.ReplayUI")
                        .getDeclaredMethod("getMouseLookVector");
                lookVectorMethod.setAccessible(true);
            }
            look = (Vec3) lookVectorMethod.invoke(null);
        } catch (Throwable t) {
            return null;
        }
        if (look == null) {
            return null;
        }

        Minecraft mc = Minecraft.getInstance();
        Entity cameraEntity = mc.cameraEntity;
        if (cameraEntity == null) {
            return null;
        }

        float distance = 64f;

        Vec3 eye = cameraEntity.getEyePosition();
        Point3D from = new Point3D(eye.x, eye.y, eye.z);
        Point3D to = new Point3D(eye.x + look.x * distance, eye.y + look.y * distance, eye.z + look.z * distance);

        Entity bestEntity = null;
        Point3D bestPos = null;
        double bestDist = flashbackResult != null
                ? Math.min(flashbackResult.getLocation().distanceTo(cameraEntity.getEyePosition()), 64D) : 64D;
        AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
        for (EntityVehicleF_Physics vehicle : world.getEntitiesOfType(EntityVehicleF_Physics.class)) {
            Integer entityId = BUILDERS_ID.get(vehicle.uniqueUUID);
            if (entityId == null || !vehicle.isValid) {
                continue;
            }
            BoundingBoxHitResult hit = null;
            for (BoundingBox box : vehicle.allCollisionBoxes) {
                BoundingBoxHitResult boxHit = box.getIntersection(from, to);
                if (boxHit != null && (hit == null || from.isFirstCloserThanSecond(boxHit.position, hit.position))) {
                    hit = boxHit;
                }
            }
            if (hit != null) {
                double dist = from.distanceTo(hit.position);
                Entity entity = mc.level.getEntity(entityId);
                if (entity != null && dist < bestDist) {
                    bestEntity = entity;
                    bestPos = hit.position;
                    bestDist = dist;
                }
            }
        }
        if (bestEntity == null) {
            return null;
        }
        return new EntityHitResult(bestEntity, new Vec3(bestPos.x, bestPos.y, bestPos.z));
    }

    public static Packet<ClientCommonPacketListener> wrapPacket(APacketBase packet) {
        return new ClientboundCustomPayloadPacket(new InterfacePacket.WrapperPayload(packet));
    }

    public static void applyCachedTransform(UUID uuid, AEntityB_Existing entity) {
        Point3D[] transforms = TRANSFORMS.get(uuid);
        if (transforms != null && entity instanceof EntityVehicleF_Physics) {
            ((EntityVehicleF_Physics) entity).setReplayTransform(
                    transforms[0],
                    transforms[1],
                    transforms.length > 2 ? transforms[2].x : 0,
                    null,
                    true
            );
        }
    }

    public static void writeCustomSnapshot(Consumer<Packet<? super ClientGamePacketListener>> consumer) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof ABuilderEntityBase) {
                ABuilderEntityBase builderEntity = (ABuilderEntityBase) entity;
                if (!builderEntity.loadedFromSavedNBT || builderEntity.isRemoved()) {
                    continue;
                }
                if (builderEntity instanceof BuilderEntityExisting && ((BuilderEntityExisting) builderEntity).entity == null) {
                    continue;
                }
                try {
                    WrapperNBT data = new WrapperNBT(new CompoundTag());
                    builderEntity.saveWithoutId(data.tag);
                    consumer.accept(wrapPacket(new PacketEntityCSHandshakeServer(builderEntity, data)));
                } catch (Exception e) {
                    //Ignore bad data
                }
            }
        }
    }

    public static void onClientTickPost(AWrapperWorld world) {
        if (cachedReplayClient && Minecraft.getInstance().level != null) {
            for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
                if (entity instanceof BuilderEntityExisting builder && builder.entity instanceof EntityVehicleF_Physics vehicle) {
                    builder.xo = builder.xOld = vehicle.prevPosition.x;
                    builder.yo = builder.yOld = vehicle.prevPosition.y;
                    builder.zo = builder.zOld = vehicle.prevPosition.z;
                    builder.setPos(vehicle.position.x, vehicle.position.y, vehicle.position.z);
                }
            }
        }
        Object recorder = getRecorder();
        if (recorder == null) {
            if (!LAST_WRITTEN.isEmpty()) {
                LAST_WRITTEN.clear();
            }
            return;
        }
        ++recordTick;
        for (EntityVehicleF_Physics vehicle : world.getEntitiesOfType(EntityVehicleF_Physics.class)) {
            if (!vehicle.isValid) {
                continue;
            }
            authPos.set(vehicle.position);
            authAngles.set(vehicle.orientation.angles);
            double[] lastWritten = LAST_WRITTEN.get(vehicle.uniqueUUID);
            if (lastWritten == null) {
                lastWritten = new double[8];
                LAST_WRITTEN.put(vehicle.uniqueUUID, lastWritten);
            }
            boolean unchanged = Math.abs(lastWritten[0] - authPos.x) < 0.000001
                    && Math.abs(lastWritten[1] - authPos.y) < 0.000001
                    && Math.abs(lastWritten[2] - authPos.z) < 0.000001
                    && Math.abs(lastWritten[3] - authAngles.x) < 0.00001
                    && Math.abs(lastWritten[4] - authAngles.y) < 0.00001
                    && Math.abs(lastWritten[5] - authAngles.z) < 0.00001;
            if (unchanged && lastWritten[7] == 1 && recordTick - (long) lastWritten[6] < 20) {
                continue;
            }
            lastWritten[0] = authPos.x;
            lastWritten[1] = authPos.y;
            lastWritten[2] = authPos.z;
            lastWritten[3] = authAngles.x;
            lastWritten[4] = authAngles.y;
            lastWritten[5] = authAngles.z;
            lastWritten[6] = recordTick;
            lastWritten[7] = unchanged ? 1 : 0;

            try {
                writePacketMethod.invoke(recorder, wrapPacket(new PacketReplayTransform(
                        vehicle.uniqueUUID, authPos.copy(), authAngles.copy(), vehicle.velocity, vehicle.motion.copy())),
                        ConnectionProtocol.PLAY
                );
            } catch (Exception e) {
                reflectionFailed = true;
                return;
            }

            //Engine sounds
            List<UUID> engineUUIDs = null;
            List<Boolean> engineRunning = null;
            List<Float> engineRpms = null;
            for (PartEngine engine : world.getEntitiesOfType(PartEngine.class)) {
                if (!engine.isValid) {
                    continue;
                }
                float rpm = (float) engine.rpm;
                double[] last = LAST_ENGINE.get(engine.uniqueUUID);
                if (last == null) {
                    last = new double[3];
                    LAST_ENGINE.put(engine.uniqueUUID, last);
                } else if (last[0] == (engine.running ? 1 : 0) && Math.abs(last[1] - rpm) < 0.5 && recordTick - (long) last[2] < 20) {
                    continue;
                }
                last[0] = engine.running ? 1 : 0;
                last[1] = rpm;
                last[2] = recordTick;
                if (engineUUIDs == null) {
                    engineUUIDs = new ArrayList<>();
                    engineRunning = new ArrayList<>();
                    engineRpms = new ArrayList<>();
                }
                engineUUIDs.add(engine.uniqueUUID);
                engineRunning.add(engine.running);
                engineRpms.add(rpm);
            }
            if (engineUUIDs != null) {
                try {
                    writePacketMethod.invoke(recorder, wrapPacket(new PacketReplayEngines(engineUUIDs, engineRunning, engineRpms)), ConnectionProtocol.PLAY);
                } catch (Exception e) {
                    reflectionFailed = true;
                }
            }
        }
    }

    private static Object getRecorder() {
        if (reflectionFailed) {
            return null;
        }
        try {
            if (recorderField == null) {
                Class<?> flashback = Class.forName("com.moulberry.flashback.Flashback");
                recorderField = flashback.getField("RECORDER");
                writePacketMethod = Class.forName("com.moulberry.flashback.record.Recorder").getMethod("writePacketAsync", Packet.class, ConnectionProtocol.class);
            }
            return recorderField.get(null);
        } catch (Throwable e) {
            // If Flashback is not installed (or structure changed), so to not try every tick.
            reflectionFailed = true;
            return null;
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BUILDERS_UUID.clear();
        BUILDERS_ID.clear();
        DATA.clear();
        TRANSFORMS.clear();
        MUTED.clear();
        QUEUED_PACKETS.clear();
        DATA_SEQUENCE.clear();
        queuedTotal = 0;
        localRenderForwarder = null;
        LAST_WRITTEN.clear();
        cachedReplayClient = false;
        wasFrozen = false;
    }


    public static final class QueuedPacket {
        final long sequence;
        final APacketBase packet;

        QueuedPacket(long sequence, APacketBase packet) {
            this.sequence = sequence;
            this.packet = packet;
        }
    }
}
