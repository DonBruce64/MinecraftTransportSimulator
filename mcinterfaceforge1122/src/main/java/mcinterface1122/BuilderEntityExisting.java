package mcinterface1122;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

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
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.items.components.IItemEntityProvider.IItemEntityFactory;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

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

    public BuilderEntityExisting(World world) {
        super(world);
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();

        //If our entity isn't null, update it and our position.
        if (entity != null) {
            //Check if we are still valid, or need to be set dead.
            if (!entity.isValid) {
                setDead();
            } else {
                //Start master profiling section.
                entity.world.beginProfiling("MTSEntity_" + getEntityId(), true);
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
                setPosition(entity.position.x, entity.position.y, entity.position.z);

                //If we are outside valid bounds on the server, set us as dead and exit.
                if (!world.isRemote && posY < 0 && world.isOutsideBuildHeight(getPosition())) {
                    setDead();
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
                        setSize((float) Math.max(interactable.encompassingBox.widthRadius * 2F, interactable.encompassingBox.depthRadius * 2F), (float) interactable.encompassingBox.heightRadius * 2F);
                        //Make sure the collision bounds for MC are big enough to collide with this entity.
                        if (World.MAX_ENTITY_RADIUS < interactable.encompassingBox.widthRadius || World.MAX_ENTITY_RADIUS < interactable.encompassingBox.heightRadius || World.MAX_ENTITY_RADIUS < interactable.encompassingBox.depthRadius) {
                            World.MAX_ENTITY_RADIUS = Math.max(Math.max(interactable.encompassingBox.widthRadius, interactable.encompassingBox.depthRadius), interactable.encompassingBox.heightRadius);
                        }
                    }
                }
                entity.world.endProfiling();
                entity.world.endProfiling();
            }
        } else {
            //If we have NBT, and haven't loaded it, do so now.
            if (!loadedFromSavedNBT && loadFromSavedNBT) {
                WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(world);
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
                    setDead();
                }
            }
        }
    }

    @Override
    public void setDead() {
        super.setDead();
        //Notify internal entity of it being invalid.
        if (entity != null) {
            entity.remove();
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (ConfigSystem.settings.damage.allowExternalDamage.value && !world.isRemote && entity instanceof AEntityE_Interactable) {
            AEntityE_Interactable<?> interactable = ((AEntityE_Interactable<?>) entity);
            Entity attacker = source.getImmediateSource();
            Entity trueSource = source.getTrueSource();
            WrapperPlayer playerSource = trueSource instanceof EntityPlayer ? WrapperPlayer.getWrapperFor((EntityPlayer) trueSource) : null;
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
                Point3D attackerPosition = new Point3D(attacker.posX, attacker.posY, attacker.posZ);
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
                    RayTraceResult hitRaytrace = interactionBoxes.calculateIntercept(attacker.getPositionVector(), attacker.getPositionVector().add(attacker.motionX, attacker.motionY, attacker.motionZ));
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
    public AxisAlignedBB getEntityBoundingBox() {
        //Override this to make interaction checks work with the multiple collision points.
        //We return the collision and interaction boxes here as we need a bounding box large enough to encompass both.
        return interactionBoxes != null ? interactionBoxes : super.getEntityBoundingBox();
    }

    @Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox() {
        //Override this to make collision checks work with the multiple collision points.
        //We only return collision boxes here as we don't want the player to collide with interaction boxes.
        return collisionBoxes != null ? collisionBoxes : super.getCollisionBoundingBox();
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        if (entity instanceof AEntityF_Multipart) {
            for (APart part : ((AEntityF_Multipart<?>) entity).parts) {
                for (BoundingBox box : part.interactionBoxes) {
                    if (box.isPointInside(new Point3D(target.hitVec.x, target.hitVec.y, target.hitVec.z))) {
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
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (entity != null) {
            //Entity is valid, save it and return the modified tag.
            //Also save the class ID so we know what to construct when MC loads this Entity back up.
            entity.save(new WrapperNBT(tag));
            tag.setString("entityid", entity.getClass().getSimpleName());
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
        if (!event.getWorld().isRemote) {
            lastExplosionPosition = new Point3D(event.getExplosion().getPosition().x, event.getExplosion().getPosition().y, event.getExplosion().getPosition().z);
        }
    }

    /**
     * Registers all builder instances that build our own entities into the game.
     */
    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        //Iterate over all pack items and find those that spawn entities.
        for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
            if (packItem instanceof IItemEntityProvider) {
                ((IItemEntityProvider) packItem).registerEntities(entityMap);
            }
        }

        //Now register our own classes.
        event.getRegistry().register(EntityEntryBuilder.create().entity(BuilderEntityExisting.class).id(new ResourceLocation(InterfaceManager.coreModID, "mts_entity"), 0).name("mts_entity").tracker(32 * 16, 5, false).build());
    }
}
