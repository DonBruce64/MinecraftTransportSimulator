package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityA_Base.EntityUpdateType;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlacedPart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Class that manages entities in a world or other area.
 * This class has various lists and methods for querying the entities.
 *
 * @author don_bruce
 */
public abstract class EntityManager {
    protected final ConcurrentLinkedQueue<AEntityA_Base> allEntities = new ConcurrentLinkedQueue<>();
    protected final ConcurrentLinkedQueue<AEntityA_Base> allMainTickableEntities = new ConcurrentLinkedQueue<>();
    protected final ConcurrentLinkedQueue<AEntityA_Base> allLastTickableEntities = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<AEntityC_Renderable> renderableEntities = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<AEntityE_Interactable<?>> collidableEntities = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Class<? extends AEntityA_Base>, ConcurrentLinkedQueue<? extends AEntityA_Base>> entitiesByClass = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<UUID, AEntityA_Base> trackedEntityMap = new ConcurrentHashMap<>();

    /**
     * Adds the entity to the world.  This will make it get update ticks and be rendered
     * and do collision checks, as applicable.  Note that this should only be called at
     * FULL construction.  As such, it is recommended to NOT put the call in the entity
     * constructor itself unless the class is final, as it is possible that extending
     * constructors won't complete before the entity is accessed from this list.
     */
    public <EntityType extends AEntityA_Base> void addEntity(EntityType entity) {
        allEntities.add(entity);
        if (entity.getUpdateType() == EntityUpdateType.MAIN) {
            allMainTickableEntities.add(entity);
        } else if (entity.getUpdateType() == EntityUpdateType.LAST) {
            allLastTickableEntities.add(entity);
        }
        if (entity instanceof AEntityC_Renderable) {
            renderableEntities.add((AEntityC_Renderable) entity);
            if (entity instanceof AEntityE_Interactable && ((AEntityE_Interactable<?>) entity).canCollide()) {
                collidableEntities.add((AEntityE_Interactable<?>) entity);
            }
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
     * Removes this entity from the world.  Taking it off the update/functional lists.
     */
    public void removeEntity(AEntityA_Base entity) {
        allEntities.remove(entity);
        if (entity.getUpdateType() == EntityUpdateType.MAIN) {
            allMainTickableEntities.remove(entity);
        } else if (entity.getUpdateType() == EntityUpdateType.LAST) {
            allLastTickableEntities.remove(entity);
        }
        if (entity instanceof AEntityC_Renderable) {
            renderableEntities.remove(entity);
            if (entity instanceof AEntityE_Interactable && ((AEntityE_Interactable<?>) entity).canCollide()) {
                collidableEntities.remove(entity);
            }
        }
        entitiesByClass.get(entity.getClass()).remove(entity);
        if (entity.shouldSync()) {
            trackedEntityMap.remove(entity.uniqueUUID);
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