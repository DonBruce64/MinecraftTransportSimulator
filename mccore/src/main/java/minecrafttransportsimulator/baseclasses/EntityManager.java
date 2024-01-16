package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.EntityPlacedPart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGun;

/**
 * Class that manages entities in a world or other area.
 * This class has various lists and methods for querying the entities.
 *
 * @author don_bruce
 */
public class EntityManager {
    public final ConcurrentLinkedQueue<AEntityA_Base> allEntities = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<AEntityA_Base> allTickableEntities = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<AEntityC_Renderable> renderableEntities = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Class<? extends AEntityA_Base>, ConcurrentLinkedQueue<? extends AEntityA_Base>> entitiesByClass = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AEntityA_Base> trackedEntityMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PartGun> gunMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Map<Integer, EntityBullet>> bulletMap = new ConcurrentHashMap<>();

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
        if (entity instanceof PartGun) {
            gunMap.put(entity.uniqueUUID, (PartGun) entity);
            bulletMap.put(entity.uniqueUUID, new HashMap<>());
        }
        if (entity instanceof EntityBullet) {
            EntityBullet bullet = (EntityBullet) entity;
            bulletMap.get(bullet.gun.uniqueUUID).put(bullet.bulletNumber, bullet);
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
     * Returns the gun associated with the gunID.  Guns are saved when they are seen in the world and
     * remain here for query even when removed.  This allows for referencing their properties for bullets
     * that were fired from a gun that was put away, moved out of render distance, etc.  If the gun is re-loaded
     * at some point, it simply replaces the reference returned by the function with the new instance.
     */
    public PartGun getBulletGun(UUID gunID) {
        return gunMap.get(gunID);
    }

    /**
     * Gets the bullet associated with the gun and bulletNumber.
     * This bullet MAY be null if we have had de-syncs across worlds that fouled the indexing.
     */
    public EntityBullet getBullet(UUID gunID, int bulletNumber) {
        return bulletMap.get(gunID).get(bulletNumber);
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
        //Also tick active explosions.
        Explosion.tickActiveExplosions();
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
        if (entity instanceof EntityBullet) {
            EntityBullet bullet = (EntityBullet) entity;
            bulletMap.get(bullet.gun.uniqueUUID).remove(bullet.bulletNumber);
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