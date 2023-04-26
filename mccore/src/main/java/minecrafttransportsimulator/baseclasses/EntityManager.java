package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlacedPart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPlayerJoin;
import minecrafttransportsimulator.packets.instances.PacketWorldEntityData;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * Class that manages entities in a world or other area.
 * This class has various lists and methods for querying the entities.
 *
 * @author don_bruce
 */
public abstract class EntityManager {
    public final ConcurrentLinkedQueue<AEntityA_Base> allEntities = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<AEntityA_Base> allTickableEntities = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<AEntityC_Renderable> renderableEntities = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Class<? extends AEntityA_Base>, ConcurrentLinkedQueue<? extends AEntityA_Base>> entitiesByClass = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AEntityA_Base> trackedEntityMap = new ConcurrentHashMap<>();

    /**
     * Returns the world this manager is located on.  Each world has its own manager.
     */
    protected abstract AWrapperWorld getWorld();

    /**
     * Adds the entity to the world.  This will make it get update ticks and be rendered
     * and do collision checks, as applicable.  Note that this should only be called at
     * FULL construction.  As such, it is recommended to NOT put the call in the entity
     * constructor itself unless the class is final, as it is possible that extending
     * constructors won't complete before the entity is accessed from this list.
     */
    public <EntityType extends AEntityA_Base> void addEntity(EntityType entity) {
        allEntities.add(entity);
        if (entity.shouldAutomaticallyUpdate()) {
            allTickableEntities.add(entity);
        }
        if (entity instanceof AEntityC_Renderable) {
            renderableEntities.add((AEntityC_Renderable) entity);
        }

        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<EntityType> classList = (ConcurrentLinkedQueue<EntityType>) entitiesByClass.get(entity.getClass());
        if (classList == null) {
            classList = new ConcurrentLinkedQueue<>();
            entitiesByClass.put(entity.getClass(), classList);
        }
        classList.add(entity);
        if (entity.shouldSync()) {
            AEntityA_Base otherEntity = trackedEntityMap.get(entity.uniqueUUID);
            if (otherEntity != null) {
                InterfaceManager.coreInterface.logError("Attempting to add already-created and tracked entity with UUID:" + entity.uniqueUUID + " old entity is being replaced!");
                removeEntity(otherEntity);
            }
            trackedEntityMap.put(entity.uniqueUUID, entity);
        }
    }

    /**
     * Like {@link #addEntity(AEntityA_Base)}, except this adds the entity from the data used to save it.
     */
    public void addEntityByData(IWrapperNBT data) {
        AItemPack<?> packItem = PackParser.getItem(data.getString("packID"), data.getString("systemName"), data.getString("subName"));
        if (packItem instanceof AItemSubTyped) {
            AEntityD_Definable<?> entity = ((AItemSubTyped<?>) packItem).createEntityFromData(getWorld(), data);
            if (entity != null) {
                if (entity instanceof AEntityF_Multipart) {
                    ((AEntityF_Multipart<?>) entity).addPartsPostAddition(null, data);
                }
                addEntity(entity);
                return;
            }
        } else if (packItem == null) {
            InterfaceManager.coreInterface.logError("Tried to create entity from NBT but couldn't find an item to create it from.  Did a pack change?");
        } else {
            InterfaceManager.coreInterface.logError("Tried to create entity from NBT but found a pack item that wasn't a sub-typed item.  A pack could have changed, but this is probably a bug and should be reported.");
        }
    }

    /**
     * Gets the entity with the requested UUID.
     */
    @SuppressWarnings("unchecked")
    public <EntityType extends AEntityA_Base> EntityType getEntity(UUID uniqueUUID) {
        return (EntityType) trackedEntityMap.get(uniqueUUID);
    }

    /**
     * Gets the list of all entities of the specified class.
     */
    @SuppressWarnings("unchecked")
    public <EntityType extends AEntityA_Base> ConcurrentLinkedQueue<EntityType> getEntitiesOfType(Class<EntityType> entityClass) {
        ConcurrentLinkedQueue<EntityType> classListing = (ConcurrentLinkedQueue<EntityType>) entitiesByClass.get(entityClass);
        if (classListing == null) {
            classListing = new ConcurrentLinkedQueue<>();
            entitiesByClass.put(entityClass, classListing);
        }
        return classListing;
    }

    /**
     * Returns a new, mutable list, with all entities that are an instanceof the passed-in class.
     * Different than {@link #getEntitiesOfType(Class)}, which must MATCH the passed-in class.
     * It is preferred to use the former since it doesn't require looping lookups and is therefore
     * more efficient.
     */
    @SuppressWarnings("unchecked")
    public <EntityType extends AEntityA_Base> List<EntityType> getEntitiesExtendingType(Class<EntityType> entityClass) {
        List<EntityType> list = new ArrayList<>();
        allEntities.forEach(entity -> {
            if (entityClass.isAssignableFrom(entity.getClass())) {
                list.add((EntityType) entity);
            }
        });
        return list;
    }

    /**
     * Ticks all entities that exist and need ticking.  These are any entities that
     * are not parts, since parts are ticked by their parents.
     */
    public void tickAll() {
        for (AEntityA_Base entity : allTickableEntities) {
            if (!(entity instanceof AEntityG_Towable) || !(((AEntityG_Towable<?>) entity).blockMainUpdateCall())) {
                entity.world.beginProfiling("MTSEntity_" + entity.uniqueUUID, true);
                entity.update();
                if (entity instanceof AEntityD_Definable) {
                    ((AEntityD_Definable<?>) entity).doPostUpdateLogic();
                }
                entity.world.endProfiling();
            }
        }
    }

    /**
     * Gets the closest multipart intersected with, be it a vehicle, a part on that vehicle, or a placed part.
     * If nothing is intersected, null is returned.
     */
    public EntityInteractResult getMultipartEntityIntersect(Point3D startPoint, Point3D endPoint) {
        EntityInteractResult closestResult = null;
        BoundingBox vectorBounds = new BoundingBox(startPoint, endPoint);
        List<AEntityF_Multipart<?>> multiparts = new ArrayList<>();
        multiparts.addAll(getEntitiesOfType(EntityVehicleF_Physics.class));
        multiparts.addAll(getEntitiesOfType(EntityPlacedPart.class));

        for (AEntityF_Multipart<?> multipart : multiparts) {
            if (multipart.encompassingBox.intersects(vectorBounds)) {
                //Could have hit this multipart, check if and what we did via raytracing.
                for (BoundingBox box : multipart.allInteractionBoxes) {
                    if (box.intersects(vectorBounds)) {
                        Point3D intersectionPoint = box.getIntersectionPoint(startPoint, endPoint);
                        if (intersectionPoint != null) {
                            if (closestResult == null || startPoint.isFirstCloserThanSecond(intersectionPoint, closestResult.point)) {
                                APart part = multipart.getPartWithBox(box);
                                closestResult = new EntityInteractResult(part != null ? part : multipart, box, intersectionPoint);
                            }
                        }
                    }
                }
            }
        }
        return closestResult;
    }

    /**
     * Removes this entity from the world.  Taking it off the update/functional lists.
     */
    public void removeEntity(AEntityA_Base entity) {
        allEntities.remove(entity);
        allTickableEntities.remove(entity);
        if (entity instanceof AEntityC_Renderable) {
            renderableEntities.remove(entity);
        }
        entitiesByClass.get(entity.getClass()).remove(entity);
        if (entity.shouldSync()) {
            trackedEntityMap.remove(entity.uniqueUUID);
        }
    }


    /**
     * Called to save all entities that need saving to the world.
     * Only called on servers, as clients don't save anything.
     */
    public void saveEntities() {
        IWrapperNBT entityData = InterfaceManager.coreInterface.getNewNBTWrapper();
        int entityCount = 0;
        for (AEntityA_Base entity : trackedEntityMap.values()) {
            if (entity instanceof AEntityD_Definable) {
                AEntityD_Definable<?> definable = (AEntityD_Definable<?>) entity;
                if (definable.loadFromWorldData()) {
                    entityData.setData("entity" + entityCount++, entity.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
                }
            }
        }
        entityData.setInteger("entityCount", entityCount);
        getWorld().setData("entities", entityData);
    }

    /**
     * Called to load all entities that were previously saved in the world.
     * Only called on servers, as clients don't save anything.
     */
    public void loadEntities() {
        IWrapperNBT entityData = getWorld().getData("entities");
        int entityCount = entityData.getInteger("entityCount");
        for (int i = 0; i < entityCount; ++i) {
            addEntityByData(entityData.getData("entity" + entityCount));
        }
    }

    /**
     * Handles things that happen after the player joins.
     * Called on both server and client, though mostly just sends
     * over server entity data to them on their clients.
     * Note that this is called on the client itself on join,
     * but on the server this is called via a packet sent by the client.
     * This ensures the client is connected and ready to receive any data we give it.
     */
    public void onPlayerJoin(IWrapperPlayer player) {
        if (player.getWorld().isClient()) {
            //Send packet to the server to handle join logic.
            InterfaceManager.packetInterface.sendToServer(new PacketPlayerJoin(player));
        } else {
            //Send data to the client.
            for (AEntityA_Base entity : trackedEntityMap.values()) {
                if (entity instanceof AEntityD_Definable) {
                    AEntityD_Definable<?> definable = (AEntityD_Definable<?>) entity;
                    if (definable.loadFromWorldData()) {
                        player.sendPacket(new PacketWorldEntityData(definable));
                    }
                }
            }
        }
    }

    /**
     * Helper class for interact return data.
     */
    public static class EntityInteractResult {
        public final AEntityE_Interactable<?> entity;
        public final BoundingBox box;
        public final Point3D point;

        private EntityInteractResult(AEntityE_Interactable<?> entity, BoundingBox box, Point3D point) {
            this.entity = entity;
            this.box = box;
            this.point = point;
        }
    }
}