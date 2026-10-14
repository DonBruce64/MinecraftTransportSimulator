package minecrafttransportsimulator.packets.instances;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.EntityInteractResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;

/**
 * Server-authoritative state for packing a vehicle back into an item after holding sneak and
 * left-click with the required tool.
 */
public class PacketVehiclePacking extends APacketPlayer {
    private static final double INTERACTION_DISTANCE = 3.5D;
    private static final long MIN_PENDING_TIMEOUT_MILLIS = 30000L;
    private static final long PENDING_TIMEOUT_PER_TICK_MILLIS = 250L;
    private static final long MAX_VALIDATION_GAP_TICKS = 20L;
    private static final Map<UUID, PendingVehiclePacking> pendingPackings = new HashMap<>();

    private final UUID vehicleID;
    private final Action action;
    private final int operationID;

    public PacketVehiclePacking(IWrapperPlayer player, EntityVehicleF_Physics vehicle, Action action, int operationID) {
        this(player, vehicle.uniqueUUID, action, operationID);
    }

    private PacketVehiclePacking(IWrapperPlayer player, UUID vehicleID, Action action, int operationID) {
        super(player);
        this.vehicleID = vehicleID;
        this.action = action;
        this.operationID = operationID;
    }

    public PacketVehiclePacking(ByteBuf buf) {
        super(buf);
        this.vehicleID = readUUIDFromBuffer(buf);
        this.action = Action.values()[buf.readByte()];
        this.operationID = buf.readInt();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(vehicleID, buf);
        buf.writeByte(action.ordinal());
        buf.writeInt(operationID);
    }

    @Override
    public void handleFromClient(AWrapperWorld world, IWrapperPlayer sendingPlayer) {
        handle(world, sendingPlayer);
    }

    @Override
    public void handle(AWrapperWorld world, IWrapperPlayer player) {
        if (world.isClient()) {
            if (action == Action.STOP) {
                ControlSystem.finishVehiclePacking(vehicleID, operationID);
            }
            return;
        }

        removeExpiredPackings(System.currentTimeMillis());
        if (action == Action.CANCEL) {
            PendingVehiclePacking pendingPacking = pendingPackings.get(player.getID());
            if (pendingPacking != null && pendingPacking.world == world && pendingPacking.vehicleID.equals(vehicleID) && pendingPacking.operationID == operationID) {
                pendingPackings.remove(player.getID());
            }
            return;
        } else if (action == Action.STOP) {
            return;
        }

        AEntityA_Base foundEntity = world.getEntity(vehicleID);
        EntityVehicleF_Physics vehicle = foundEntity instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) foundEntity : null;
        EntityInteractResult currentTarget = getCurrentPackingTarget(player, vehicle);
        IWrapperItemStack heldStack = player.getHeldStack();
        if (action == Action.START) {
            if (isVehiclePackingValid(vehicle, currentTarget, player)) {
                pendingPackings.put(player.getID(), new PendingVehiclePacking(world, vehicle, heldStack.copy(), operationID));
            } else {
                player.sendPacket(new PacketVehiclePacking(player, vehicleID, Action.STOP, operationID));
            }
        } else if (action == Action.HEARTBEAT || action == Action.COMPLETE) {
            PendingVehiclePacking pendingPacking = pendingPackings.get(player.getID());
            if (pendingPacking != null
                    && vehicle != null
                    && pendingPacking.vehicleID.equals(vehicleID)
                    && pendingPacking.operationID == operationID
                    && pendingPacking.world == world
                    && pendingPacking.packTime == vehicle.definition.motorized.packTime
                    && heldStack.isCompleteMatch(pendingPacking.toolStack)
                    && isVehiclePackingValid(vehicle, currentTarget, player)) {
                long currentTick = vehicle.ticksExisted;
                long validationGap = currentTick - pendingPacking.lastValidationTick;
                if (validationGap >= 0 && validationGap <= MAX_VALIDATION_GAP_TICKS) {
                    pendingPacking.validatedTicks += validationGap;
                    pendingPacking.lastValidationTick = currentTick;
                    pendingPacking.expirationTimeMillis = System.currentTimeMillis() + Math.max(MIN_PENDING_TIMEOUT_MILLIS, pendingPacking.packTime * PENDING_TIMEOUT_PER_TICK_MILLIS);
                } else {
                    pendingPackings.remove(player.getID());
                    player.sendPacket(new PacketVehiclePacking(player, vehicleID, Action.STOP, operationID));
                    return;
                }
                if (action == Action.COMPLETE && pendingPacking.validatedTicks >= pendingPacking.packTime) {
                    pendingPackings.remove(player.getID());
                    player.sendPacket(new PacketVehiclePacking(player, vehicleID, Action.STOP, operationID));
                    ItemItem.packVehicle(vehicle, currentTarget.box.globalCenter);
                }
            } else {
                if (pendingPacking != null
                        && pendingPacking.world == world
                        && pendingPacking.vehicleID.equals(vehicleID)
                        && pendingPacking.operationID == operationID) {
                    pendingPackings.remove(player.getID());
                }
                player.sendPacket(new PacketVehiclePacking(player, vehicleID, Action.STOP, operationID));
            }
        }
    }

    private static EntityInteractResult getCurrentPackingTarget(IWrapperPlayer player, EntityVehicleF_Physics vehicle) {
        if (vehicle == null || !vehicle.isValid) {
            return null;
        }
        Point3D startPosition = player.getEyePosition();
        Point3D endPosition = player.getLineOfSight(INTERACTION_DISTANCE).add(startPosition);
        EntityInteractResult currentTarget = player.getWorld().getMultipartEntityIntersect(startPosition, endPosition);
        if (currentTarget != null && currentTarget.entity instanceof AEntityF_Multipart && getVehicleForMultipart((AEntityF_Multipart<?>) currentTarget.entity) == vehicle) {
            return currentTarget;
        }
        return null;
    }

    private static boolean isVehiclePackingValid(EntityVehicleF_Physics vehicle, EntityInteractResult currentTarget, IWrapperPlayer player) {
        return vehicle != null
                && vehicle.isValid
                && vehicle.definition.motorized.packTime > 0
                && currentTarget != null
                && !player.isSpectator()
                && player.isSneaking()
                && !vehicle.lockedVar.isActive
                && (player.isHoldingItemType(ItemComponentType.WRENCH) || player.isHoldingItemType(ItemComponentType.SCREWDRIVER))
                && (!ConfigSystem.settings.general.opPickupVehiclesOnly.value || player.isOP())
                && (!ConfigSystem.settings.general.creativePickupVehiclesOnly.value || player.isCreative());
    }

    private static EntityVehicleF_Physics getVehicleForMultipart(AEntityF_Multipart<?> multipart) {
        return multipart instanceof EntityVehicleF_Physics ? (EntityVehicleF_Physics) multipart : (multipart instanceof APart ? ((APart) multipart).vehicleOn : null);
    }

    private static void removeExpiredPackings(long currentTimeMillis) {
        pendingPackings.values().removeIf(pendingPacking -> pendingPacking.expirationTimeMillis <= currentTimeMillis);
    }

    public enum Action {
        START,
        HEARTBEAT,
        COMPLETE,
        CANCEL,
        STOP
    }

    private static class PendingVehiclePacking {
        private final AWrapperWorld world;
        private final UUID vehicleID;
        private final IWrapperItemStack toolStack;
        private long lastValidationTick;
        private long validatedTicks;
        private final int packTime;
        private final int operationID;
        private long expirationTimeMillis;

        private PendingVehiclePacking(AWrapperWorld world, EntityVehicleF_Physics vehicle, IWrapperItemStack toolStack, int operationID) {
            this.world = world;
            this.vehicleID = vehicle.uniqueUUID;
            this.toolStack = toolStack;
            this.lastValidationTick = vehicle.ticksExisted;
            this.packTime = vehicle.definition.motorized.packTime;
            this.operationID = operationID;
            this.expirationTimeMillis = System.currentTimeMillis() + Math.max(MIN_PENDING_TIMEOUT_MILLIS, packTime * PENDING_TIMEOUT_PER_TICK_MILLIS);
        }
    }
}
