package minecrafttransportsimulator.packets.instances;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BlockHitResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketPlayer;
import minecrafttransportsimulator.systems.ControlSystem;

/**
 * Handles completion and cancellation of timed vehicle deployment.  The initial server state is
 * created by the normal block-use callback, keeping the target authoritative and preserving the
 * vanilla reach checks that occur before item use.
 */
public class PacketVehicleDeployment extends APacketPlayer {
    private static final double INTERACTION_DISTANCE = 5.0D;
    private static final long MIN_PENDING_TIMEOUT_NANOS = 30000000000L;
    private static final long PENDING_TIMEOUT_PER_TICK_NANOS = 250000000L;
    private static final long MAX_VALIDATION_GAP_TICKS = 20L;
    private static final Map<UUID, PendingVehicleDeployment> pendingDeployments = new HashMap<>();
    private static int nextOperationID;

    private final Point3D blockPosition;
    private final Axis blockSide;
    private final Action action;
    private final int operationID;

    public PacketVehicleDeployment(IWrapperPlayer player, Point3D blockPosition, Axis blockSide, Action action, int operationID) {
        super(player);
        this.blockPosition = blockPosition;
        this.blockSide = blockSide;
        this.action = action;
        this.operationID = operationID;
    }

    public PacketVehicleDeployment(ByteBuf buf) {
        super(buf);
        this.blockPosition = readPoint3dCompactFromBuffer(buf);
        this.blockSide = Axis.values()[buf.readByte()];
        this.action = Action.values()[buf.readByte()];
        this.operationID = buf.readInt();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writePoint3dCompactToBuffer(blockPosition, buf);
        buf.writeByte(blockSide.ordinal());
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
            if (action == Action.GRANT) {
                boolean accepted = ControlSystem.authorizeVehicleDeployment(blockPosition, blockSide, operationID);
                InterfaceManager.packetInterface.sendToServer(new PacketVehicleDeployment(player, blockPosition, blockSide, accepted ? Action.ACK : Action.CANCEL, operationID));
            } else if (action == Action.STOP) {
                ControlSystem.finishVehicleDeployment(blockPosition, blockSide, operationID);
            }
            return;
        }

        removeExpiredDeployments();
        PendingVehicleDeployment pending = pendingDeployments.get(player.getID());
        boolean matches = pending != null && pending.world == world && pending.operationID == operationID
                && pending.blockPosition.equals(blockPosition) && pending.blockSide == blockSide;
        if (action == Action.CANCEL) {
            if (matches) {
                pendingDeployments.remove(player.getID());
            }
            return;
        }
        if (action != Action.ACK && action != Action.HEARTBEAT && action != Action.COMPLETE) {
            return;
        }

        IWrapperItemStack heldStack = player.getHeldStack();
        BlockHitResult currentTarget = world.getBlockHit(player.getEyePosition(), player.getLineOfSight(INTERACTION_DISTANCE));
        if (!matches || player.isSpectator() || !heldStack.isCompleteMatch(pending.vehicleStack)
                || !(heldStack.getItem() instanceof ItemVehicle) || ((ItemVehicle) heldStack.getItem()).definition.motorized.deployTime != pending.deployTime
                || currentTarget == null || !currentTarget.blockPosition.equals(blockPosition) || currentTarget.side != blockSide) {
            if (matches) {
                pendingDeployments.remove(player.getID());
            }
            player.sendPacket(new PacketVehicleDeployment(player, blockPosition, blockSide, Action.STOP, operationID));
            return;
        }

        long currentTick = world.getTickCount();
        if (action == Action.ACK) {
            if (!pending.activated) {
                pending.lastValidationTick = currentTick;
                pending.activated = true;
            }
            pending.refreshTimeout();
            return;
        }
        long validationGap = currentTick - pending.lastValidationTick;
        if (!pending.activated || validationGap < 0 || validationGap > MAX_VALIDATION_GAP_TICKS) {
            pendingDeployments.remove(player.getID());
            player.sendPacket(new PacketVehicleDeployment(player, blockPosition, blockSide, Action.STOP, operationID));
            return;
        }
        pending.validatedTicks += validationGap;
        pending.lastValidationTick = currentTick;
        pending.refreshTimeout();
        if (action == Action.COMPLETE && pending.validatedTicks >= pending.deployTime) {
            pendingDeployments.remove(player.getID());
            ((ItemVehicle) heldStack.getItem()).deployVehicle(world, player, pending.blockPosition);
            player.sendPacket(new PacketVehicleDeployment(player, blockPosition, blockSide, Action.STOP, operationID));
        }
    }

    public static void grantVehicleDeployment(IWrapperPlayer player, ItemVehicle vehicleItem, Point3D blockPosition, Axis blockSide) {
        AWrapperWorld world = player.getWorld();
        removeExpiredDeployments();
        int deployTime = vehicleItem.definition.motorized.deployTime;
        IWrapperItemStack heldStack = player.getHeldStack();
        if (deployTime <= 0 || heldStack.getItem() != vehicleItem || player.isSpectator()) {
            return;
        }
        PendingVehicleDeployment pending = pendingDeployments.get(player.getID());
        if (pending == null) {
            if (++nextOperationID == 0) {
                ++nextOperationID;
            }
            pending = new PendingVehicleDeployment(world, blockPosition, blockSide, heldStack.copy(), deployTime, nextOperationID);
            pendingDeployments.put(player.getID(), pending);
        }
        if (pending.world == world && pending.blockPosition.equals(blockPosition) && pending.blockSide == blockSide) {
            player.sendPacket(new PacketVehicleDeployment(player, blockPosition, blockSide, Action.GRANT, pending.operationID));
        }
    }

    public static void unloadDeployments(AWrapperWorld world) {
        pendingDeployments.values().removeIf(pending -> pending.world == world);
    }

    private static void removeExpiredDeployments() {
        long now = System.nanoTime();
        pendingDeployments.values().removeIf(pending -> pending.expirationTimeNanos <= now);
    }

    public enum Action {
        GRANT,
        ACK,
        HEARTBEAT,
        COMPLETE,
        CANCEL,
        STOP
    }

    private static class PendingVehicleDeployment {
        private final AWrapperWorld world;
        private final Point3D blockPosition;
        private final Axis blockSide;
        private final IWrapperItemStack vehicleStack;
        private final int deployTime;
        private final int operationID;
        private long lastValidationTick;
        private long validatedTicks;
        private long expirationTimeNanos;
        private boolean activated;

        private PendingVehicleDeployment(AWrapperWorld world, Point3D blockPosition, Axis blockSide, IWrapperItemStack vehicleStack, int deployTime, int operationID) {
            this.world = world;
            this.blockPosition = blockPosition.copy();
            this.blockSide = blockSide;
            this.vehicleStack = vehicleStack;
            this.deployTime = deployTime;
            this.operationID = operationID;
            refreshTimeout();
        }

        private void refreshTimeout() {
            expirationTimeNanos = System.nanoTime() + Math.max(MIN_PENDING_TIMEOUT_NANOS, deployTime * PENDING_TIMEOUT_PER_TICK_NANOS);
        }
    }
}
