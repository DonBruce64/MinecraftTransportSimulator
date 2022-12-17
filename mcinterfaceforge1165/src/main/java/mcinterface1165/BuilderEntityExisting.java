package mcinterface1165;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.IItemEntityProvider.IItemEntityFactory;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Builder for the main entity classes for MTS.  This builder allows us to create a new entity
 * class that we can control that doesn't have the wonky systems the MC entities have, such
 * as no roll axis, a single hitbox, and tons of immutable objects that get thrown away every update.
 * Constructor simply takes in a world instance per default MC standards, but doesn't create the actual
 * {@link AEntityB_Existing} until later.  This is because we can't build our entity at the same time MC creates
 * this instance as we might not yet have NBT data.  Instead, we simply hold on to the class and construct
 * it whenever we get called to do so.
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class BuilderEntityExisting extends ABuilderEntityBase {
    protected static EntityType<BuilderEntityExisting> E_TYPE2;
    private EntitySize mutableDims = new EntitySize(1.0F, 1.0F, false);

    /**
     * Maps Entity class names to instances of the IItemEntityProvider class that creates them.
     **/
    protected static final Map<String, IItemEntityFactory> entityMap = new HashMap<>();

    /**
     * Current entity we are built around.  This MAY be null if we haven't loaded NBT from the server yet.
     **/
    protected AEntityB_Existing entity;
    /**
     * Last saved explosion position (used for damage calcs).
     **/
    private static Point3D lastExplosionPosition;
    /**
     * Collective for interaction boxes.  These are used by this entity to allow players to interact with it.
     **/
    private WrapperAABBCollective interactionBoxes;
    /**
     * Collective for collision boxes.  These are used by this entity to make things collide with it.
     **/
    private WrapperAABBCollective collisionBoxes;

    public BuilderEntityExisting(EntityType<? extends BuilderEntityExisting> eType, World world) {
        super(eType, world);
    }

    @Override
    public void baseTick() {
        super.baseTick();

        //If our entity isn't null, update it and our position.
        if (entity != null) {
            //Check if we are still valid, or need to be set dead.
            if (!entity.isValid) {
                remove();
            } else {
                //Start master profiling section.
                entity.world.beginProfiling("MTSEntity_" + getUUID(), true);
                entity.world.beginProfiling("Main_Execution", true);

                //Forward the update call.
                if (!(entity instanceof AEntityG_Towable) || !(((AEntityG_Towable<?>) entity).blockMainUpdateCall())) {
                    entity.update();
                    if (entity instanceof AEntityD_Definable) {
                        ((AEntityD_Definable<?>) entity).doPostUpdateLogic();
                    }
                }

                //Set the new position.
                entity.world.beginProfiling("MovementOverhead", false);
                setPos(entity.position.x, entity.position.y, entity.position.z);

                //If we are outside valid bounds on the server, set us as dead and exit.
                if (!level.isClientSide && position().y < 0 && World.isOutsideBuildHeight(blockPosition())) {
                    remove();
                    entity.world.endProfiling();
                    entity.world.endProfiling();
                    return;
                }

                if (entity instanceof AEntityE_Interactable) {
                    AEntityE_Interactable<?> interactable = ((AEntityE_Interactable<?>) entity);

                    //Update AABBs.
                    //We need to know if we need to increase the max world collision bounds to detect this entity.
                    //Only do this after the first tick of the entity, as we might have some states that need updating
                    //on that first tick that would cause bad maths.
                    //We also do this only every second, as it prevents excess checks.
                    entity.world.beginProfiling("CollisionOverhead", false);
                    interactionBoxes = new WrapperAABBCollective(interactable.encompassingBox, interactable.getInteractionBoxes());
                    collisionBoxes = new WrapperAABBCollective(interactable.encompassingBox, interactable.getCollisionBoxes());
                    if (interactable.ticksExisted > 1 && interactable.ticksExisted % 20 == 0) {
                        mutableDims = new EntitySize((float) Math.max(interactable.encompassingBox.widthRadius * 2F, interactable.encompassingBox.depthRadius * 2F), (float) interactable.encompassingBox.heightRadius * 2F, false);
                        //Make sure the collision bounds for MC are big enough to collide with this entity.
                        double maxEntityRadius = level.getMaxEntityRadius();
                        if (maxEntityRadius < interactable.encompassingBox.widthRadius || maxEntityRadius < interactable.encompassingBox.heightRadius || maxEntityRadius < interactable.encompassingBox.depthRadius) {
                            level.increaseMaxEntityRadius(Math.max(Math.max(interactable.encompassingBox.widthRadius, interactable.encompassingBox.depthRadius), interactable.encompassingBox.heightRadius));
                        }
                    }
                }
                entity.world.endProfiling();
                entity.world.endProfiling();
            }
        } else {
            //If we have NBT, and haven't loaded it, do so now.
            if (!loadedFromSavedNBT && loadFromSavedNBT) {
                WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(level);
                try {
                    WrapperNBT data = new WrapperNBT(lastLoadedNBT);
                    entity = entityMap.get(lastLoadedNBT.getString("entityid")).createEntity(worldWrapper, null, data);
                    if (entity instanceof AEntityF_Multipart) {
                        ((AEntityF_Multipart<?>) entity).addPartsPostAddition(null, data);
                    }
                    entity.world.addEntity(entity);
                    loadedFromSavedNBT = true;
                    lastLoadedNBT = null;
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("Failed to load entity on builder from saved NBT.  Did a pack change?");
                    InterfaceManager.coreInterface.logError(e.getMessage());
                    remove();
                }
            }
        }
    }

    @Override
    public EntitySize getDimensions(Pose pPose) {
        return mutableDims;
    }

    @Override
    public void remove() {
        super.remove();
        //Notify internal entity of it being invalid.
        if (entity != null) {
            entity.remove();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (ConfigSystem.settings.damage.allowExternalDamage.value && !level.isClientSide && entity instanceof AEntityE_Interactable) {
            AEntityE_Interactable<?> interactable = ((AEntityE_Interactable<?>) entity);
            Entity attacker = source.getDirectEntity();
            Entity trueSource = source.getEntity();
            WrapperPlayer playerSource = trueSource instanceof PlayerEntity ? WrapperPlayer.getWrapperFor((PlayerEntity) trueSource) : null;
            if (lastExplosionPosition != null && source.isExplosion()) {
                //We encountered an explosion.  These may or may not have have entities linked to them.  Depends on if
                //it's a player firing a gun that had a bullet, or a random TNT lighting in the world.
                //Explosions, unlike other damage sources, can hit multiple collision boxes on an entity at once.
                BoundingBox explosiveBounds = new BoundingBox(lastExplosionPosition, amount, amount, amount);
                for (BoundingBox box : interactionBoxes.boxes) {
                    if (box.intersects(explosiveBounds)) {
                        interactable.attack(new Damage(amount, box, null, playerSource, null).setExplosive());
                    }
                }
                lastExplosionPosition = null;
            } else if (attacker != null) {
                Damage damage = null;
                //Check the damage at the current position of the attacker.
                Vector3d attackerMcPos = attacker.position();
                Point3D attackerPosition = new Point3D(attackerMcPos.x, attackerMcPos.y, attackerMcPos.z);
                for (BoundingBox box : interactionBoxes.boxes) {
                    if (box.isPointInside(attackerPosition)) {
                        damage = new Damage(amount, box, null, playerSource, null);
                        break;
                    }
                }

                if (damage == null) {
                    //Check the theoretical position of the entity should it have moved.
                    //Some projectiles may call their attacking code before updating their positions.
                    //We do raytracing here to catch this movement.
                    Optional<Vector3d> hitRaytrace = interactionBoxes.clip(attackerMcPos, attackerMcPos.add(attacker.getDeltaMovement()));
                    if (hitRaytrace != null) {
                        damage = new Damage(amount, interactionBoxes.lastBoxRayTraced, null, playerSource, null);
                    }
                }

                //If we have damage on a point, attack it now.
                if (damage != null) {
                    interactable.attack(damage);
                }
            }
        }
        return true;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        //Override this to make interaction checks work with the multiple collision points.
        //We return the collision and interaction boxes here as we need a bounding box large enough to encompass both.
        return interactionBoxes != null ? interactionBoxes : super.getBoundingBox();
        //FIXME need to swap these out for collision boxes or something, to prevent excess collisions with entities.
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        if (entity instanceof AEntityF_Multipart) {
            for (APart part : ((AEntityF_Multipart<?>) entity).parts) {
                for (BoundingBox box : part.interactionBoxes) {
                    if (box.isPointInside(new Point3D(target.getLocation().x, target.getLocation().y, target.getLocation().z))) {
                        AItemPack<?> partItem = part.getItem();
                        if (partItem != null) {
                            return ((WrapperItemStack) part.getItem().getNewStack(part.save(InterfaceManager.coreInterface.getNewNBTWrapper()))).stack;
                        }
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canBeCollidedWith() {
        //This gets overridden to allow players to interact with this entity.
        return collisionBoxes != null && !collisionBoxes.boxes.isEmpty();
    }

    @Override
    public CompoundNBT saveWithoutId(CompoundNBT tag) {
        super.saveWithoutId(tag);
        if (entity != null) {
            //Entity is valid, save it and return the modified tag.
            //Also save the class ID so we know what to construct when MC loads this Entity back up.
            entity.save(new WrapperNBT(tag));
            tag.putString("entityid", entity.getClass().getSimpleName());
        }
        return tag;
    }

    /**
     * We need to use explosion events here as we don't know where explosions occur in the world.
     * This results in them being position-less, so we can't get the collision box they hit for damage.
     * Whenever we have an explosion detonated in the world, save it's position.  We can then use it
     * in {@link #attackEntityFrom(DamageSource, float)} to tell the system which part to attack.
     */
    @SubscribeEvent
    public static void on(ExplosionEvent.Detonate event) {
        if (!event.getWorld().isClientSide) {
            lastExplosionPosition = new Point3D(event.getExplosion().getPosition().x, event.getExplosion().getPosition().y, event.getExplosion().getPosition().z);
        }
    }
}
