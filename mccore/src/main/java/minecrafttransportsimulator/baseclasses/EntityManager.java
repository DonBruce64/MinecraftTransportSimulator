package minecrafttransportsimulator.baseclasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityA_Base.EntityAutoUpdateTime;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.EntityPlacedPart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Class that manages entities in a world.
 * This class has various lists and methods for querying the entities.
 *
 * @author don_bruce
 */
public abstract class EntityManager {
    public final ConcurrentLinkedQueue<AEntityA_Base> allEntities = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<AEntityA_Base> allNormalTickableEntities = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<AEntityA_Base> allPlayerTickableEntities = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<AEntityC_Renderable> renderableEntities = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Class<? extends AEntityA_Base>, ConcurrentLinkedQueue<? extends AEntityA_Base>> entitiesByClass = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AEntityA_Base> trackedEntityMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PartGun> gunMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Map<Integer, EntityBullet>> bulletMap = new ConcurrentHashMap<>();
    
    private static final byte hotloadCountdownPreset = 20;
    private static byte hotloadCountdown;
    private static byte hotloadStep;
    private static HotloadFunction hotloadFunction;
    private static final Set<EntityManager> managersToHotload = new HashSet<>();
    
    private final Map<IWrapperNBT, ItemVehicle> hotloadedVehicles = new HashMap<>();
    private final Set<IWrapperNBT> hotloadedPlacedParts = new HashSet<>();
    private final Map<UUID, UUID> hotloadedRiderIDs = new HashMap<>();
    
    private static final List<EntityManager> managers = new ArrayList<>();

    public EntityManager() {
    	managers.add(this);
    }

    public abstract AWrapperWorld getWorld();

    /**
     * Adds the entity to the world.  This will make it get update ticks and be rendered
     * and do collision checks, as applicable.  Note that this should only be called at
     * FULL construction.  As such, it is recommended to NOT put the call in the entity
     * constructor itself unless the class is final, as it is possible that extending
     * constructors won't complete before the entity is accessed from this list.
     */
    public <EntityType extends AEntityA_Base> void addEntity(EntityType entity) {
        allEntities.add(entity);
        if (entity.getUpdateTime() == EntityAutoUpdateTime.NORMAL) {
            allNormalTickableEntities.add(entity);
        } else if (entity.getUpdateTime() == EntityAutoUpdateTime.AFTER_PLAYER) {
            allPlayerTickableEntities.add(entity);
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
    public void tickAll(boolean beforePlayer) {
        AWrapperWorld world = getWorld();
        if (world.isClient()) {
            if (beforePlayer) {
                world.beginProfiling("MTS_ClientEntityUpdatesPre", true);
            } else {
                world.beginProfiling("MTS_ClientEntityUpdatesPost", true);
            }
        } else {
            if (beforePlayer) {
                world.beginProfiling("MTS_ServerEntityUpdatesPre", true);
            } else {
                world.beginProfiling("MTS_ServerEntityUpdatesPost", true);
            }
        }
        if (beforePlayer) {
            for (AEntityA_Base entity : allNormalTickableEntities) {
                if (!(entity instanceof AEntityG_Towable) || !(((AEntityG_Towable<?>) entity).blockMainUpdateCall())) {
                    doTick(entity);
                }
            }

            //Do hotload operations.
            //This operates on all threads concurrently as long as we're counting down.
            if (hotloadStep > 0) {
                switch (hotloadStep) {
                    case (1): {
                        if (world.isClient()) {
                            //Client manager, set counter to let entities sync.
                            if (hotloadCountdown == 0) {
                                hotloadCountdown = hotloadCountdownPreset;
                            }
                        } else {
                            //Server manager, remove all entities in this manager for reloading.
                            if (managersToHotload.contains(this)) {
                                for (AEntityA_Base entity : allNormalTickableEntities) {
                                    if (entity instanceof EntityVehicleF_Physics || entity instanceof EntityPlacedPart) {
                                        AEntityD_Definable<?> definable = (AEntityD_Definable<?>) entity;
                                        //First need to save/remove riders, since we don't want to save them with this data since they aren't being unloaded.
                                        if (entity instanceof AEntityF_Multipart) {
                                            ((AEntityF_Multipart<?>) entity).allParts.forEach(part -> {
                                                if (part.rider != null) {
                                                    hotloadedRiderIDs.put(part.uniqueUUID, part.rider.getID());
                                                    part.removeRider();
                                                }
                                            });
                                        }

                                        //Now store data for countdown and remove entity.
                                        if (entity instanceof EntityVehicleF_Physics) {
                                            hotloadedVehicles.put(definable.save(InterfaceManager.coreInterface.getNewNBTWrapper()), (ItemVehicle) definable.cachedItem);
                                        } else if (entity instanceof EntityPlacedPart) {
                                            hotloadedPlacedParts.add(definable.save(InterfaceManager.coreInterface.getNewNBTWrapper()));
                                        }
                                        //Remove will cause entity to get removed client-side, no need to remove on that thread.
                                        definable.remove();
                                    }
                                }
                                managersToHotload.remove(this);
                            }
                        }
                        break;
                    }
                    case (2): {
                        if (world.isClient()) {
                            //Client manager, apply hotloads once on this client.
                            hotloadFunction.apply();
                            //No need to wait, all systems will be ready next tick.
                            hotloadCountdown = 1;
                        }
                        break;
                    }
                    case (3): {
                        if (world.isClient()) {
                            //Client manager, set counter to let entities sync.
                            if (hotloadCountdown == 0) {
                                hotloadCountdown = hotloadCountdownPreset;
                            }
                        } else {
                            //Server manager, load back in saved entities while we wait for client to count down.
                            hotloadedVehicles.forEach((data, item) -> {
                                EntityVehicleF_Physics vehicle = new EntityVehicleF_Physics(getWorld(), null, item, data);
                                vehicle.addPartsPostAddition(null, data);
                                vehicle.world.spawnEntity(vehicle);
                            });
                            hotloadedVehicles.clear();
                            hotloadedPlacedParts.forEach(data -> {
                                EntityPlacedPart placedPart = new EntityPlacedPart(getWorld(), null, data);
                                placedPart.addPartsPostAddition(null, data);
                                placedPart.world.spawnEntity(placedPart);
                            });
                            hotloadedPlacedParts.clear();
                        }
                        break;
                    }
                    case (4): {
                        if (world.isClient()) {
                            //Client manager, set counter to let riders sync.
                            if (hotloadCountdown == 0) {
                                hotloadCountdown = hotloadCountdownPreset;
                            }
                        } else {
                            //Server manager, load back all seated riders.
                            hotloadedRiderIDs.forEach((seatID, riderID) -> {
                                getWorld().getExternalEntity(riderID).setRiding(getWorld().getEntity(seatID));
                            });
                            hotloadedRiderIDs.clear();

                        }
                        break;
                    }
                    case (5): {
                        //Done with hotloads, set to step 0.
                        hotloadStep = 0;
                        break;
            		}
                }

                //Countdown timer to go to next step.
                //Only countdown on the client, since we know there's only one client and will only tick once per update.
                if (hotloadStep != 0 && world.isClient()) {
                    if (--hotloadCountdown == 0) {
                        ++hotloadStep;
                    }
                    ;
                }
            }
        } else {
            for (AEntityA_Base entity : allPlayerTickableEntities) {
                if (!(entity instanceof AEntityG_Towable) || !(((AEntityG_Towable<?>) entity).blockMainUpdateCall())) {
                    doTick(entity);
                }
            }
        }
        world.endProfiling();
    }

    public static void doTick(AEntityA_Base entity) {
        entity.world.beginProfiling("MTSEntity_" + entity.uniqueUUID, true);
        if (entity instanceof AEntityD_Definable) {
            AEntityD_Definable<?> definable = (AEntityD_Definable<?>) entity;
            //Need to do this before updating as these require knowledge of prior states.
            entity.world.beginProfiling("VariableModifiers", true);
            definable.setVariableDefaults();
            definable.updateVariableModifiers();
            entity.world.beginProfiling("MainUpdate", false);
            entity.update();
            entity.world.beginProfiling("PostUpdate", false);
            definable.doPostUpdateLogic();
            entity.world.endProfiling();
        } else {
            entity.update();
        }
        entity.world.endProfiling();
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
            if (multipart.encompassingBox.intersects(vectorBounds) && multipart.canBeClicked()) {
                //Could have hit this multipart, check if and what we did via raytracing.
                for (BoundingBox box : multipart.allCollisionBoxes) {
                    if (box.collisionTypes.contains(CollisionType.CLICK) && box.intersects(vectorBounds)) {
                        BoundingBoxHitResult intersectionPoint = box.getIntersection(startPoint, endPoint);
                        if (intersectionPoint != null) {
                            if (closestResult == null || startPoint.isFirstCloserThanSecond(intersectionPoint.position, closestResult.position)) {
                                APart part = multipart.getPartWithBox(box);
                                if (part != null) {
                                    if (part.canBeClicked()) {
                                        closestResult = new EntityInteractResult(part, box, intersectionPoint.position);
                                    }
                                } else {
                                    closestResult = new EntityInteractResult(multipart, box, intersectionPoint.position);
                                }
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
        allNormalTickableEntities.remove(entity);
        allPlayerTickableEntities.remove(entity);
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
    
    public void adjustHeightForRain(Point3D position) {
        for (EntityVehicleF_Physics vehicle : getEntitiesOfType(EntityVehicleF_Physics.class)) {
            if (vehicle.encompassingBox.isPointInsideAndBelow(position)) {
                //Point is inside the box, but we might not be blocked by a collision box.  If we are, we need to block rain.
                for (BoundingBox box : vehicle.allCollisionBoxes) {
                    if (box.collisionTypes.contains(CollisionType.ENTITY)) {
                        //Check all four corners.
                        //We might only be blocking partially, but we need to block the whole block.
                        position.x -= 0.5;
                        position.z -= 0.5;
                        if (box.isPointInsideAndBelow(position)) {
                            position.y = box.globalCenter.y + box.heightRadius;
                            position.x += 0.5;
                            position.z += 0.5;
                        } else {
                            position.x += 1.0;
                            if (box.isPointInsideAndBelow(position)) {
                                position.y = box.globalCenter.y + box.heightRadius;
                                position.x -= 0.5;
                                position.z += 0.5;
                            } else {
                                position.z += 1.0;
                                if (box.isPointInsideAndBelow(position)) {
                                    position.y = box.globalCenter.y + box.heightRadius;
                                    position.x -= 0.5;
                                    position.z -= 0.5;
                                } else {
                                    position.x -= 1.0;
                                    if (box.isPointInsideAndBelow(position)) {
                                        position.y = box.globalCenter.y + box.heightRadius;
                                    }
                                    position.x += 0.5;
                                    position.z -= 0.5;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Tells the manager to import JSONs in all worlds.
     * This has to do a sequenced-handshake where entities are removed, JSONs applied, and then added back again.
     * The removal has to happen for all worlds before the importing can occur.
     * If we don't do this, concurrency errors can result in crashes.
     * If a file is specified, only that JSON will be imported.  Otherwise, all JSONs will be imported.
     *  
     */
    public static void doImports(HotloadFunction hotloadFunction) {
    	for(EntityManager manager : managers) {
    		if(!manager.getWorld().isClient()) {
    			//Only add server managers for hotloading since we don't remove entities on clients.
    			managersToHotload.add(manager);
    		}
    	}
    	EntityManager.hotloadFunction = hotloadFunction;
    	hotloadStep = 1;
	}
    
    @FunctionalInterface
    public static abstract interface HotloadFunction{
    	public void apply();
    }
}