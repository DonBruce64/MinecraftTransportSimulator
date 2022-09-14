package minecrafttransportsimulator.baseclasses;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;

/**
 * Class that manages entities in a world or other area.
 * This class has various lists and methods for querying the entities.
 *
 * @author don_bruce
 */
public class EntityManager {
    public final ConcurrentLinkedQueue<AEntityA_Base> allEntities = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<AEntityC_Renderable> renderableEntities = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Class<? extends AEntityA_Base>, ConcurrentLinkedQueue<? extends AEntityA_Base>> entitiesByClass = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AEntityA_Base> trackedEntityMap = new ConcurrentHashMap<>();

    /**
     * Adds the entity to the world.  This will make it get update ticks and be rendered
     * and do collision checks, as applicable.  Note that this should only be called at
     * FULL construction.  As such, it is recommended to NOT put the call in the entity
     * constructor itself unless the class is final, as it is possible that extending
     * constructors won't complete before the entity is accessed from this list.
     */
    public <EntityType extends AEntityA_Base> void addEntity(EntityType entity) {
        allEntities.add(entity);
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
            trackedEntityMap.put(entity.uniqueUUID, entity);
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
     * Returns the closest entity of the specified class that intersects the ray-traced line,
     * or null if none does. This up-scales the entity Bounding Boxes to
     * allow for a somewhat easier targeting scheme if generalArea is true.
     */
    public <EntityType extends AEntityE_Interactable<?>> EntityType getRaytraced(Class<EntityType> entityClass, Point3D start, Point3D end, boolean generalArea, EntityType entityToIgnore) {
        BoundingBox closestBox = null;
        EntityType closestEntity = null;
        BoundingBox clickBounds = new BoundingBox(start, end);
        for (EntityType entity : getEntitiesOfType(entityClass)) {
            if (!entity.equals(entityToIgnore) && entity.encompassingBox.intersects(clickBounds)) {
                //Could have hit this entity, check if we did via raytracing.
                for (BoundingBox box : entity.getInteractionBoxes()) {
                    boolean intersects;
                    if (generalArea) {
                        box.widthRadius += 2;
                        box.heightRadius += 2;
                        box.depthRadius += 2;
                        intersects = box.intersects(clickBounds) && box.getIntersectionPoint(start, end) != null;
                        box.widthRadius -= 2;
                        box.heightRadius -= 2;
                        box.depthRadius -= 2;
                    } else {
                        intersects = box.intersects(clickBounds) && box.getIntersectionPoint(start, end) != null;
                    }
                    if (intersects) {
                        if (closestBox == null || start.isFirstCloserThanSecond(box.globalCenter, closestBox.globalCenter)) {
                            closestBox = box;
                            closestEntity = entity;
                        }
                    }
                }
            }
        }
        return closestEntity;
    }

    /**
     * Removes this entity from the world.  Taking it off the update/functional lists.
     */
    public void removeEntity(AEntityA_Base entity) {
        allEntities.remove(entity);
        if (entity instanceof AEntityC_Renderable) {
            renderableEntities.remove(entity);
        }
        entitiesByClass.get(entity.getClass()).remove(entity);
        if (entity.shouldSync()) {
            trackedEntityMap.remove(entity.uniqueUUID);
        }
    }
}