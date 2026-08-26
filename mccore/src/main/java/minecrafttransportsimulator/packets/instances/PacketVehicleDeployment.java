package minecrafttransportsimulator.packets.instances;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BlockHitResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;
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
    private static final Map<Integer, PendingVehicleDeployment> pendingDeployments = new HashMap<>();
    private static final Set<AWrapperWorld> loadedDeploymentWorlds = Collections.newSetFromMap(new IdentityHashMap<AWrapperWorld, Boolean>());
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
                if (ControlSystem.authorizeVehicleDeployment(blockPosition, blockSide, operationID, true)) {
                    InterfaceManager.packetInterface.sendToServer(new PacketVehicleDeployment(player, blockPosition, blockSide, Action.ACK, operationID));
                } else {
                    InterfaceManager.packetInterface.sendToServer(new PacketVehicleDeployment(player, blockPosition, blockSide, Action.CANCEL, operationID));
                }
            } else if (action == Action.GRANT_SINGLE_CLICK) {
                ControlSystem.authorizeVehicleDeployment(blockPosition, blockSide, operationID, false);
            } else if (action == Action.STOP) {
                ControlSystem.finishVehicleDeployment(blockPosition, blockSide, operationID);
            }
            return;
        }

        loadedDeploymentWorlds.add(world);
        removeExpiredDeployments(System.nanoTime());
        PendingVehicleDeployment pendingDeployment = getPendingDeployment(player.getID(), operationID);
        if (pendingDeployment != null && !pendingDeployment.requiresHold) {
            //Single-click deployment is fully server-owned after the initial vanilla block-use callback.
            //Packets delayed from the initiating client must not cancel, accelerate, or otherwise alter it.
            return;
        }
        if (action == Action.CANCEL) {
            if (matchesOperation(pendingDeployment, world, blockPosition, blockSide, operationID)) {
                pendingDeployments.remove(operationID);
            }
        } else if (action == Action.ACK) {
            IWrapperItemStack heldStack = player.getHeldStack();
            if (matchesOperation(pendingDeployment, world, blockPosition, blockSide, operationID)
                    && heldStack.getItem() instanceof ItemVehicle
                    && heldStack.isCompleteMatch(pendingDeployment.vehicleStack)
                    && ((ItemVehicle) heldStack.getItem()).definition.motorized.deployTime == pendingDeployment.deployTime
                    && isDeploymentTargetValid(world, player, blockPosition, blockSide, pendingDeployment.requiresHold, pendingDeployment.blockName)) {
                if (!pendingDeployment.activated) {
                    pendingDeployment.lastValidationTick = world.getTickCount();
                    pendingDeployment.validatedTicks = 0;
                    pendingDeployment.activated = true;
                }
                pendingDeployment.expirationTimeNanos = System.nanoTime() + Math.max(MIN_PENDING_TIMEOUT_NANOS, pendingDeployment.deployTime * PENDING_TIMEOUT_PER_TICK_NANOS);
            } else {
                if (matchesOperation(pendingDeployment, world, blockPosition, blockSide, operationID)) {
                    pendingDeployments.remove(operationID);
                }
                player.sendPacket(new PacketVehicleDeployment(player, blockPosition, blockSide, Action.STOP, operationID));
            }
        } else if (action == Action.HEARTBEAT || action == Action.COMPLETE) {
            IWrapperItemStack heldStack = player.getHeldStack();
            if (matchesOperation(pendingDeployment, world, blockPosition, blockSide, operationID)
                    && pendingDeployment.activated
                    && heldStack.getItem() instanceof ItemVehicle
                    && heldStack.isCompleteMatch(pendingDeployment.vehicleStack)
                    && ((ItemVehicle) heldStack.getItem()).definition.motorized.deployTime == pendingDeployment.deployTime
                    && isDeploymentTargetValid(world, player, blockPosition, blockSide, pendingDeployment.requiresHold, pendingDeployment.blockName)) {
                long currentTick = world.getTickCount();
                long validationGap = currentTick - pendingDeployment.lastValidationTick;
                if (validationGap >= 0 && validationGap <= MAX_VALIDATION_GAP_TICKS) {
                    pendingDeployment.validatedTicks += validationGap;
                    pendingDeployment.lastValidationTick = currentTick;
                    pendingDeployment.expirationTimeNanos = System.nanoTime() + Math.max(MIN_PENDING_TIMEOUT_NANOS, pendingDeployment.deployTime * PENDING_TIMEOUT_PER_TICK_NANOS);
                } else {
                    pendingDeployments.remove(operationID);
                    player.sendPacket(new PacketVehicleDeployment(player, blockPosition, blockSide, Action.STOP, operationID));
                    return;
                }
                if (action == Action.COMPLETE && pendingDeployment.validatedTicks >= pendingDeployment.deployTime) {
                    pendingDeployments.remove(operationID);
                    ItemVehicle vehicleItem = (ItemVehicle) heldStack.getItem();
                    vehicleItem.deployVehicle(world, player, pendingDeployment.blockPosition);
                    player.sendPacket(new PacketVehicleDeployment(player, pendingDeployment.blockPosition, pendingDeployment.blockSide, Action.STOP, operationID));
                }
            } else {
                if (matchesOperation(pendingDeployment, world, blockPosition, blockSide, operationID)) {
                    pendingDeployments.remove(operationID);
                }
                player.sendPacket(new PacketVehicleDeployment(player, blockPosition, blockSide, Action.STOP, operationID));
            }
        }
    }

    public static void grantVehicleDeployment(IWrapperPlayer player, ItemVehicle vehicleItem, Point3D blockPosition, Axis blockSide) {
        AWrapperWorld world = player.getWorld();
        loadedDeploymentWorlds.add(world);
        long currentTimeNanos = System.nanoTime();
        removeExpiredDeployments(currentTimeNanos);
        int deployTime = vehicleItem.definition.motorized.deployTime;
        IWrapperItemStack heldStack = player.getHeldStack();
        if (deployTime > 0 && heldStack.getItem() == vehicleItem && !player.isSpectator()) {
            boolean requiresHold = !ConfigSystem.settings.general.singleClickVehicleDeployment.value;
            if (requiresHold) {
                PendingVehicleDeployment existingDeployment = getPendingDeployment(player.getID());
                if (existingDeployment != null) {
                    if (existingDeployment.world == world
                            && existingDeployment.blockPosition.equals(blockPosition)
                            && existingDeployment.blockSide == blockSide) {
                        player.sendPacket(new PacketVehicleDeployment(player, blockPosition, blockSide, Action.GRANT, existingDeployment.operationID));
                    }
                    return;
                }
            }

            int operationID = getNextOperationID();
            PendingVehicleDeployment pendingDeployment;
            if (requiresHold) {
                pendingDeployment = new PendingVehicleDeployment(player.getID(), world, blockPosition, blockSide, heldStack.copy(), world.getTickCount(), currentTimeNanos, deployTime, operationID);
            } else {
                int sourceSlot = player.getHotbarIndex();
                IWrapperItemStack reservedStack;
                boolean inventoryReserved = !player.isCreative();
                if (inventoryReserved) {
                    IWrapperItemStack sourceStack = player.getInventory().getStack(sourceSlot);
                    if (sourceStack.isEmpty() || sourceStack.getItem() != vehicleItem || !sourceStack.isCompleteMatch(heldStack)) {
                        return;
                    }
                    reservedStack = sourceStack.split(1);
                    player.getInventory().setStack(sourceStack, sourceSlot);
                } else {
                    reservedStack = heldStack.copy().split(1);
                }

                long startTick = world.getTickCount();
                pendingDeployment = new PendingVehicleDeployment(player.getID(), world, blockPosition, blockSide, reservedStack, startTick, deployTime, operationID, player.getYaw(), inventoryReserved);
            }
            pendingDeployments.put(operationID, pendingDeployment);
            player.sendPacket(new PacketVehicleDeployment(player, blockPosition, blockSide, requiresHold ? Action.GRANT : Action.GRANT_SINGLE_CLICK, operationID));
        }
    }

    private static int getNextOperationID() {
        do {
            ++nextOperationID;
            if (nextOperationID == 0) {
                ++nextOperationID;
            }
        } while (pendingDeployments.containsKey(nextOperationID));
        return nextOperationID;
    }

    private static PendingVehicleDeployment getPendingDeployment(UUID playerID) {
        for (PendingVehicleDeployment pendingDeployment : pendingDeployments.values()) {
            if (pendingDeployment.playerID.equals(playerID) && pendingDeployment.requiresHold) {
                return pendingDeployment;
            }
        }
        return null;
    }

    private static PendingVehicleDeployment getPendingDeployment(UUID playerID, int operationID) {
        PendingVehicleDeployment pendingDeployment = pendingDeployments.get(operationID);
        return pendingDeployment != null && pendingDeployment.playerID.equals(playerID) ? pendingDeployment : null;
    }

    private static boolean matchesOperation(PendingVehicleDeployment pendingDeployment, AWrapperWorld world, Point3D blockPosition, Axis blockSide, int operationID) {
        return pendingDeployment != null
                && pendingDeployment.world == world
                && pendingDeployment.blockPosition.equals(blockPosition)
                && pendingDeployment.blockSide == blockSide
                && pendingDeployment.operationID == operationID;
    }

    private static boolean isDeploymentTargetValid(AWrapperWorld world, IWrapperPlayer player, Point3D blockPosition, Axis blockSide, boolean requiresHold, String blockName) {
        if (player.isSpectator()) {
            return false;
        }
        if (requiresHold) {
            BlockHitResult currentTarget = world.getBlockHit(player.getEyePosition(), player.getLineOfSight(INTERACTION_DISTANCE));
            return currentTarget != null && currentTarget.blockPosition.equals(blockPosition) && currentTarget.side == blockSide;
        }
        if (!world.getBlockName(blockPosition).equals(blockName)) {
            return false;
        }
        Point3D eyePosition = player.getEyePosition();
        double deltaX = eyePosition.x < blockPosition.x ? blockPosition.x - eyePosition.x : eyePosition.x > blockPosition.x + 1.0D ? eyePosition.x - blockPosition.x - 1.0D : 0.0D;
        double deltaY = eyePosition.y < blockPosition.y ? blockPosition.y - eyePosition.y : eyePosition.y > blockPosition.y + 1.0D ? eyePosition.y - blockPosition.y - 1.0D : 0.0D;
        double deltaZ = eyePosition.z < blockPosition.z ? blockPosition.z - eyePosition.z : eyePosition.z > blockPosition.z + 1.0D ? eyePosition.z - blockPosition.z - 1.0D : 0.0D;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= INTERACTION_DISTANCE * INTERACTION_DISTANCE;
    }

    /**
     * Advances server-owned single-click deployments.  This is called once per server-world tick
     * from {@link AWrapperWorld#tickAll(boolean)} and never relies on the initiating player still
     * being present, nearby, or holding the original item.
     */
    public static void tickDeployments(AWrapperWorld world) {
        if (world.isClient()) {
            return;
        }
        loadedDeploymentWorlds.add(world);
        long currentTick = world.getTickCount();
        Iterator<Map.Entry<Integer, PendingVehicleDeployment>> iterator = pendingDeployments.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingVehicleDeployment pendingDeployment = iterator.next().getValue();
            if (pendingDeployment.world != world || pendingDeployment.requiresHold) {
                continue;
            }

            if (pendingDeployment.spawnedVehicle != null) {
                if (!pendingDeployment.spawnedVehicle.isValid || pendingDeployment.spawnedVehicle.ticksExisted > 0) {
                    iterator.remove();
                    sendStopPacket(pendingDeployment);
                }
                continue;
            }
            if (currentTick < pendingDeployment.completionTick) {
                continue;
            }

            try {
                world.loadChunk(pendingDeployment.blockPosition);
            } catch (RuntimeException e) {
                InterfaceManager.coreInterface.logError("Could not load the target chunk for a pending vehicle deployment.  The deployment will be retried.");
                e.printStackTrace();
                continue;
            }

            if (!(pendingDeployment.vehicleStack.getItem() instanceof ItemVehicle)) {
                InterfaceManager.coreInterface.logError("Could not complete a pending vehicle deployment because its vehicle item is no longer available.");
                refundReservedStack(pendingDeployment);
                iterator.remove();
                sendStopPacket(pendingDeployment);
                continue;
            }

            IWrapperPlayer player = findPlayer(pendingDeployment.playerID);
            try {
                pendingDeployment.spawnedVehicle = ((ItemVehicle) pendingDeployment.vehicleStack.getItem()).deployVehicle(world, player, pendingDeployment.blockPosition, pendingDeployment.vehicleStack, pendingDeployment.placementYaw, false, pendingDeployment.inventoryReserved);
                if (pendingDeployment.spawnedVehicle == null) {
                    refundReservedStack(pendingDeployment);
                    iterator.remove();
                    sendStopPacket(pendingDeployment);
                }
            } catch (RuntimeException | Error e) {
                InterfaceManager.coreInterface.logError("Could not complete a pending single-click vehicle deployment.  Its reserved item has been returned to the world.");
                e.printStackTrace();
                refundReservedStack(pendingDeployment);
                iterator.remove();
                sendStopPacket(pendingDeployment);
            }
        }
    }

    /**Called when a world wrapper is unloaded to release all static references to that world.*/
    public static void unloadDeployments(AWrapperWorld world) {
        if (world.isClient()) {
            return;
        }
        Iterator<PendingVehicleDeployment> iterator = pendingDeployments.values().iterator();
        while (iterator.hasNext()) {
            PendingVehicleDeployment pendingDeployment = iterator.next();
            if (pendingDeployment.world == world) {
                if (!pendingDeployment.requiresHold) {
                    if (pendingDeployment.spawnedVehicle == null) {
                        refundReservedStack(pendingDeployment);
                    }
                    sendStopPacket(pendingDeployment);
                }
                iterator.remove();
            }
        }
        loadedDeploymentWorlds.remove(world);
    }

    private static void sendStopPacket(PendingVehicleDeployment pendingDeployment) {
        IWrapperPlayer player = findPlayer(pendingDeployment.playerID);
        if (player != null) {
            player.sendPacket(new PacketVehicleDeployment(player, pendingDeployment.blockPosition, pendingDeployment.blockSide, Action.STOP, pendingDeployment.operationID));
        }
    }

    private static IWrapperPlayer findPlayer(UUID playerID) {
        for (AWrapperWorld loadedWorld : loadedDeploymentWorlds) {
            IWrapperEntity possiblePlayer = loadedWorld.getExternalEntity(playerID);
            if (possiblePlayer instanceof IWrapperPlayer) {
                return (IWrapperPlayer) possiblePlayer;
            }
        }
        return null;
    }

    private static void refundReservedStack(PendingVehicleDeployment pendingDeployment) {
        if (pendingDeployment.inventoryReserved && !pendingDeployment.vehicleStack.isEmpty()) {
            IWrapperItemStack stackToReturn = pendingDeployment.vehicleStack.copy();
            IWrapperPlayer player = findPlayer(pendingDeployment.playerID);
            if (player != null) {
                player.getInventory().addStack(stackToReturn);
            }
            if (!stackToReturn.isEmpty()) {
                pendingDeployment.world.spawnItemStack(stackToReturn, pendingDeployment.blockPosition.copy().add(0.5D, 1.0D, 0.5D), null);
            }
        }
    }

    private static void removeExpiredDeployments(long currentTimeNanos) {
        pendingDeployments.values().removeIf(pendingDeployment -> pendingDeployment.requiresHold && pendingDeployment.expirationTimeNanos <= currentTimeNanos);
    }

    public enum Action {
        GRANT,
        ACK,
        HEARTBEAT,
        COMPLETE,
        CANCEL,
        STOP,
        GRANT_SINGLE_CLICK
    }

    private static class PendingVehicleDeployment {
        private final UUID playerID;
        private final AWrapperWorld world;
        private final Point3D blockPosition;
        private final Axis blockSide;
        private final String blockName;
        private final IWrapperItemStack vehicleStack;
        private long lastValidationTick;
        private long validatedTicks;
        private boolean activated;
        private final boolean requiresHold;
        private final int deployTime;
        private final int operationID;
        private final double placementYaw;
        private final long completionTick;
        private final boolean inventoryReserved;
        private long expirationTimeNanos;
        private EntityVehicleF_Physics spawnedVehicle;

        private PendingVehicleDeployment(UUID playerID, AWrapperWorld world, Point3D blockPosition, Axis blockSide, IWrapperItemStack vehicleStack, long startTick, long startTimeNanos, int deployTime, int operationID) {
            this.playerID = playerID;
            this.world = world;
            this.blockPosition = blockPosition.copy();
            this.blockSide = blockSide;
            this.blockName = world.getBlockName(blockPosition);
            this.vehicleStack = vehicleStack;
            this.lastValidationTick = startTick;
            this.requiresHold = true;
            this.deployTime = deployTime;
            this.operationID = operationID;
            this.placementYaw = 0;
            this.completionTick = 0;
            this.inventoryReserved = false;
            this.expirationTimeNanos = startTimeNanos + Math.max(MIN_PENDING_TIMEOUT_NANOS, deployTime * PENDING_TIMEOUT_PER_TICK_NANOS);
        }

        private PendingVehicleDeployment(UUID playerID, AWrapperWorld world, Point3D blockPosition, Axis blockSide, IWrapperItemStack vehicleStack, long startTick, int deployTime, int operationID, double placementYaw, boolean inventoryReserved) {
            this.playerID = playerID;
            this.world = world;
            this.blockPosition = blockPosition.copy();
            this.blockSide = blockSide;
            this.blockName = world.getBlockName(blockPosition);
            this.vehicleStack = vehicleStack;
            this.lastValidationTick = startTick;
            this.activated = true;
            this.requiresHold = false;
            this.deployTime = deployTime;
            this.operationID = operationID;
            this.placementYaw = placementYaw;
            this.completionTick = startTick + deployTime;
            this.inventoryReserved = inventoryReserved;
        }
    }
}
