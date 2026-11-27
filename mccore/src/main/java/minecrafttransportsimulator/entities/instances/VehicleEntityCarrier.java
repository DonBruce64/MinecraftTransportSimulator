package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketVehicleCarriedEntity;

import java.util.*;

public class VehicleEntityCarrier {
    /** How far an entity's feet can be from the top of the box to be still standing on it.*/
    private static final double STANDING_TOLERANCE = 0.1;
    /** How high above a boxe can entity be to be still attached. For instance when jumping.*/
    private static final double AIRBORNE_HEIGHT = 2.0;
    /** Hom many ticks to smooth a remote entity*/
    private static final int REMOTE_LERP_STEPS = 3;
    /** For how many ticks to keep entity attached after it stops answering*/
    private static final int REMOTE_TIMEOUT = 10;

    private final AEntityVehicleD_Moving vehicle;
    private Set<IWrapperEntity> attachedEntities = new HashSet<>();
    private final RotationMatrix rotationDelta = new RotationMatrix();
    private boolean moved;

    private static VehicleEntityCarrier clientPlayerCarrier;
    private boolean clientPlayerCarried;
    private boolean clientPlayerOnGround;
    private final Point3D clientPlayerCarryDelta = new Point3D();
    private double clientPlayerPendingYaw;
    private double clientPlayerAppliedYaw;

    private final Map<IWrapperEntity, RemoteEntity> remoteEntities = new HashMap<>();

    public VehicleEntityCarrier(AEntityVehicleD_Moving vehicle) {
        this.vehicle = vehicle;
    }

    /**
     * Called every vehicle update tick, after it and the boxes move.
     * Attached and moves entities whose movement this side controls.
     */
    public void update() {
        moved = !vehicle.position.equals(vehicle.prevPosition)
                || !vehicle.orientation.angles.equals(vehicle.prevOrientation.angles);
        rotationDelta.set(vehicle.orientation).multiplyTranspose(vehicle.prevOrientation);
        rotationDelta.convertToAngles();
        clientPlayerCarried = false;

        Set<IWrapperEntity> attachedThisTick = new HashSet<>();
        if (vehicle.world.isClient()) {
            turnClientPlayer(clientPlayerPendingYaw - clientPlayerAppliedYaw);
            clientPlayerPendingYaw = 0;
            clientPlayerAppliedYaw = 0;

            IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();
            if (clientPlayer != null && isNear(clientPlayer)) {
                carry(clientPlayer, attachedThisTick);
            }
            if (clientPlayerCarried) {
                clientPlayerCarrier = this;
            } else if (clientPlayerCarrier == this) {
                clientPlayerCarrier = null;
            }
        } else if (moved && !vehicle.allCollisionBoxes.isEmpty()) {
            vehicle.encompassingBox.heightRadius += AIRBORNE_HEIGHT;
            List<IWrapperEntity> nearbyEntities = vehicle.world.getEntitiesWithin(vehicle.encompassingBox);
            vehicle.encompassingBox.heightRadius -= AIRBORNE_HEIGHT;
            for (IWrapperEntity entity : nearbyEntities) {
                carry(entity, attachedThisTick);
            }
        }

        if (moved) {
            for (IWrapperEntity entity : attachedEntities) {
                if (!attachedThisTick.contains(entity) && entity.isValid()) {
                    Point3D carriedPosition = getCarriedPosition(entity.getPosition());
                    entity.setVelocity(carriedPosition.subtract(entity.getPosition()).add(entity.getVelocity()));
                }
            }
        }
        attachedEntities = attachedThisTick;
    }

    /**
     * Attach the entity if it's on the vehicle, and move with it.
     * If it's not on the vehicle and we hit it, push it.
     */
    private void carry(IWrapperEntity entity, Set<IWrapperEntity> attachedThisTick) {
        if (entity.getEntityRiding() != null || (entity instanceof IWrapperPlayer && ((IWrapperPlayer) entity).isSpectator())) {
            return;
        }

        Point3D carriedPostion = getCarriedPosition(entity.getPosition());
        BoundingBox entityBounds = entity.getBounds();
        double standingHeight = Double.NaN;
        boolean justAbove = false;
        for (BoundingBox box : vehicle.allCollisionBoxes) {
            if (box.collisionTypes.contains(CollisionType.ENTITY)
                    && Math.abs(carriedPostion.x - box.globalCenter.x) < entityBounds.widthRadius + box.widthRadius
                    && Math.abs(carriedPostion.z - box.globalCenter.z) < entityBounds.depthRadius + box.depthRadius) {
                double heightAboveBox = carriedPostion.y - (box.globalCenter.y + box.heightRadius);
                if (Math.abs(heightAboveBox) <= STANDING_TOLERANCE) {
                    standingHeight = Double.isNaN(standingHeight) ? heightAboveBox : Math.min(standingHeight, heightAboveBox);
                } else if (heightAboveBox > 0 && heightAboveBox <= AIRBORNE_HEIGHT) {
                    justAbove = true;
                }
            }
        }

        boolean onGround;
        if (!Double.isNaN(standingHeight)) {
            carriedPostion.y -= standingHeight;
            onGround = true;
        } else if (justAbove && attachedEntities.contains(entity)) {
            onGround = false;
        } else {
            if (moved) {
                pushOut(entity);
            }
            return;
        }

        attachedThisTick.add(entity);
        if (vehicle.world.isClient()) {
            clientPlayerCarried = true;
            clientPlayerOnGround = onGround;
            clientPlayerCarryDelta.set(carriedPostion).subtract(entity.getPosition());
            clientPlayerPendingYaw = rotationDelta.angles.y;
        } else {
            entity.setYaw(entity.getYaw() + rotationDelta.angles.y);
            entity.setBodyYaw(entity.getBodyYaw() + rotationDelta.angles.y);
            if (moved && !(entity instanceof IWrapperPlayer)) {
                InterfaceManager.packetInterface.sendToAllClients(
                        new PacketVehicleCarriedEntity((EntityVehicleF_Physics) vehicle,
                        entity,
                        toLocal(carriedPostion),
                        onGround)
                );
            }
        }
        entity.carryPosition(carriedPostion, onGround);
        pushOutOfWalls(entity);
    }

    private void pushOutOfWalls(IWrapperEntity entity) {
        BoundingBox entityBounds = entity.getBounds();
        double entityRadius = entityBounds.widthRadius;
        double entityHeight = entityBounds.heightRadius * 2;
        Point3D localPosition = toLocal(entity.getPosition().copy());
        Point3D boxPosition = new Point3D();
        boolean pushed = false;
        for (BoundingBox box : vehicle.allCollisionBoxes) {
            if (box.collisionTypes.contains(CollisionType.ENTITY)) {
                toLocal(boxPosition.set(box.globalCenter));
                double depthX = entityRadius + box.widthRadius - Math.abs(localPosition.x - boxPosition.x);
                double depthZ = entityRadius + box.depthRadius - Math.abs(localPosition.z - boxPosition.z);
                if (depthX > 0 && depthZ > 0
                        && boxPosition.y - box.heightRadius < localPosition.y + entityHeight
                        && boxPosition.y + box.heightRadius - localPosition.y > 0.5
                        && Math.min(depthX, depthZ) <= 0.5) {
                    if (depthX <= depthZ) {
                        localPosition.x += localPosition.x < boxPosition.x ? -depthX : depthX;
                    } else {
                        localPosition.z += localPosition.z < boxPosition.z ? -depthZ : depthZ;
                    }
                    pushed = true;
                }
            }
        }
        if (pushed) {
            entity.correctHorizontalPosition(localPosition.rotate(vehicle.orientation).add(vehicle.position));
        }
    }

    private void pushOut(IWrapperEntity entity) {
        BoundingBox entityBounds = entity.getBounds();
        boolean movingUp = entity.getVelocity().y > 0;
        for (BoundingBox box : vehicle.allCollisionBoxes) {
            if (box.collisionTypes.contains(CollisionType.ENTITY) && entityBounds.intersects(box)) {
                Point3D entityPosition = entity.getPosition();
                double topDepth = box.globalCenter.y + box.heightRadius - entityPosition.y;
                double bottomDepth = entityPosition.y + entityBounds.heightRadius * 2 - (box.globalCenter.y - box.heightRadius);
                if (topDepth > 0.5 || movingUp) {
                    double correctX = entityPosition.x < box.globalCenter.x
                            ? box.globalCenter.x - box.widthRadius - entityBounds.widthRadius
                            : box.globalCenter.x + box.widthRadius + entityBounds.widthRadius;
                    double correctZ = entityPosition.z < box.globalCenter.z
                            ? box.globalCenter.z - box.depthRadius - entityBounds.depthRadius
                            : box.globalCenter.z + box.depthRadius + entityBounds.depthRadius;
                    double depthX = Math.abs(correctX - entityPosition.x);
                    double depthZ = Math.abs(correctZ - entityPosition.z);
                    double horizontalDepth = Math.min(depthX, depthZ);
                    if (movingUp && bottomDepth > 0 && bottomDepth <= 0.5 && bottomDepth < topDepth && bottomDepth <= horizontalDepth) {
                        entity.correctCeilingPosition(Math.nextDown(box.globalCenter.y - box.heightRadius - entityBounds.heightRadius * 2));
                        entityBounds = entity.getBounds();
                    } else if (topDepth > 0.5 && horizontalDepth <= 0.5 && horizontalDepth < bottomDepth) {
                        if (depthX <= depthZ) {
                            entityPosition.x = correctX < entityPosition.x ? Math.nextDown(correctX) : Math.nextUp(correctX);
                        } else {
                            entityPosition.z = correctZ < entityPosition.z ? Math.nextDown(correctZ) : Math.nextUp(correctZ);
                        }
                        entity.correctHorizontalPosition(entityPosition);
                        entityBounds = entity.getBounds();
                    }
                }
            }
        }
    }

    public void updateAfterPlayers() {
        if (clientPlayerCarried) {
            clientPlayerCarried = false;
            IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();
            if (clientPlayer != null) {
                clientPlayer.addToPriorPosition(clientPlayerCarryDelta.invert());
                if (moved) {
                    InterfaceManager.packetInterface.sendToServer(new PacketVehicleCarriedEntity(
                            (EntityVehicleF_Physics) vehicle,
                            clientPlayer,
                            toLocal(clientPlayer.getPosition().copy()),
                            clientPlayerOnGround
                    ));
                }
            }
        }
        if (!remoteEntities.isEmpty()) {
            for (Iterator<Map.Entry<IWrapperEntity, RemoteEntity>> iterator = remoteEntities.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<IWrapperEntity, RemoteEntity> entry = iterator.next();
                IWrapperEntity entity = entry.getKey();
                RemoteEntity remote = entry.getValue();
                if (!entity.isValid() || vehicle.ticksExisted - remote.lastUpdateTick > REMOTE_TIMEOUT) {
                    iterator.remove();
                } else {
                    if (remote.lerpSteps > 0) {
                        remote.localPostion.add(remote.targetPosition.copy().subtract(remote.localPostion).scale(1D / remote.lerpSteps));
                        --remote.lerpSteps;
                    }
                    entity.carryPosition(remote.localPostion.copy().rotate(vehicle.orientation).add(vehicle.position), remote.onGround);
                }
            }
        }
    }

    public void setRemotePosition(IWrapperEntity entity, Point3D localPosition, boolean onGround) {
        RemoteEntity remote = remoteEntities.get(entity);
        if (remote == null) {
            remote = new RemoteEntity();
            toLocal(remote.localPostion.set(entity.getPosition()));
            remoteEntities.put(entity, remote);
        }
        remote.targetPosition.set(localPosition);
        remote.lerpSteps = REMOTE_LERP_STEPS;
        remote.onGround = onGround;
        remote.lastUpdateTick = vehicle.ticksExisted;
    }

    public static double updateClientPlayerFrame(float partialTicks) {
        return clientPlayerCarrier != null ? clientPlayerCarrier.updateFrame(partialTicks) : 0;
    }

    public double updateFrame(float partialTicks) {
        if (clientPlayerPendingYaw != 0) {
            double targetYaw = clientPlayerPendingYaw * partialTicks;
            double yawDelta = targetYaw - clientPlayerAppliedYaw;
            turnClientPlayer(yawDelta);
            clientPlayerAppliedYaw = targetYaw;
            return yawDelta;
        } else {
            return 0;
        }
    }

    private void turnClientPlayer(double yawDelta) {
        if (yawDelta != 0) {
            IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();
            if (clientPlayer != null) {
                clientPlayer.setYaw(clientPlayer.getYaw() + yawDelta);
                clientPlayer.setBodyYaw(clientPlayer.getBodyYaw() + yawDelta);
            }
        }
    }

    private boolean isNear(IWrapperEntity entity) {
        vehicle.encompassingBox.heightRadius += AIRBORNE_HEIGHT;
        boolean near = vehicle.encompassingBox.isPointInside(entity.getPosition(), null);
        vehicle.encompassingBox.heightRadius -= AIRBORNE_HEIGHT;
        return near;
    }

    private Point3D getCarriedPosition(Point3D worldPostion) {
        return worldPostion.copy().subtract(vehicle.prevPosition).rotate(rotationDelta).add(vehicle.position);
    }

    private Point3D toLocal(Point3D worldPosition) {
        return worldPosition.subtract(vehicle.position).reOrigin(vehicle.orientation);
    }

    private static class RemoteEntity {
        private final Point3D localPostion = new Point3D();
        private final Point3D targetPosition = new Point3D();
        private int lerpSteps;
        private boolean onGround;
        private long lastUpdateTick;
    }
}
