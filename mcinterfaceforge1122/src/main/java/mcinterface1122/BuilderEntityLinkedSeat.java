package mcinterface1122;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**
 * Builder for an entity to sit in so they can ride another entity.  We use this rather
 * than a direct linking as entities with riders are removed by MC when the rider logs out.
 * This means that if we assigned this to the main entity, it would be removed when the rider left the server.
 * This is not ideal for things like trains where the engineer leaves and the main locomotive goes poof.
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class BuilderEntityLinkedSeat extends ABuilderEntityBase {

    /**
     * Current entity we are a seat on.  This MAY be null if we haven't loaded NBT from the server yet.
     **/
    protected AEntityE_Interactable<?> entity;
    /**
     * Current rider for this seat.  This MAY be null if we haven't loaded NBT from the server yet.
     **/
    protected WrapperEntity rider;
    /**
     * Set to true when the rider dismounts.  We set their position the next tick to override it.
     **/
    private boolean dismountedRider;

    public BuilderEntityLinkedSeat(World world) {
        super(world);
        setSize(0.05F, 0.05F);
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();

        //If our entity isn't null, update us to the entity position.
        //What really matters is the player's position, and that comes later.
        //This just gets us "close enough" so we don't de-spawn or something.
        if (entity != null) {
            //Check if the entity we are a seat on is still valid, or need to be set dead.
            if (!entity.isValid) {
                setDead();
            } else {
                setPosition(entity.position.x, entity.position.y, entity.position.z);

                //Constantly check for the rider.  They might take a bit to load in.
                //If the rider dismounted us, just die.
                List<Entity> riders = getPassengers();
                if (rider == null && !riders.isEmpty()) {
                    //If we already have a rider on the entity, just use it.  Otherwise, set it.
                    //This prevents us from changing the riding entity if a packet gets there before we do.
                    if (entity.rider != null) {
                        rider = (WrapperEntity) entity.rider;
                    } else {
                        rider = WrapperEntity.getWrapperFor(riders.get(0));
                        entity.setRider(rider, true);
                    }
                } else if (dismountedRider) {
                    setDead();
                }
            }
        } else {
            //If we have NBT, and haven't loaded it, do so now.
            if (!loadedFromSavedNBT && loadFromSavedNBT) {
                WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(world);
                try {
                    entity = worldWrapper.getEntity(lastLoadedNBT.getUniqueId("entityUUID"));
                    //If entity doesn't exist, wait until it does.
                    if (entity != null) {
                        loadedFromSavedNBT = true;
                        lastLoadedNBT = null;
                    }
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("Failed to load seat on builder from saved NBT.  Did a pack change?");
                    InterfaceManager.coreInterface.logError(e.getMessage());
                    setDead();
                }
            }
        }
        if (rider == null && ticksExisted > 100) {
            //We have existed for over 5 seconds without the rider loading in.  Clearly, they don't exist.
            if (!world.isRemote) {
                setDead();
            }
        }
    }

    @Override
    public void setDead() {
        super.setDead();
        //Notify internal entity of rider being removed.
        if (entity != null && rider != null) {
            if (!world.isRemote && rider.equals(entity.rider)) {
                entity.removeRider();
            }
            rider = null;
            entity = null;
        }
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (entity != null) {
            entity.updateRider();
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        //Need to make sure we actually have a rider as MC wipes out the rider list on init.
        //This sends a fake "dismount" command to saved entities and makes us dismount riders that shouldn't be.
        if (rider != null) {
            dismountedRider = true;
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return entity != null ? InterfaceEventsEntityRendering.renderCurrentRiderSitting : super.shouldRiderSit();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (entity != null) {
            //Entity is valid, save UUID and return the modified tag.
            tag.setUniqueId("entityUUID", entity.uniqueUUID);
        }
        return tag;
    }

    /**
     * Registers our own class for use.
     */
    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.create().entity(BuilderEntityLinkedSeat.class).id(new ResourceLocation(InterfaceManager.coreModID, "mts_entity_seat"), 1).tracker(32 * 16, 5, false).name("mts_entity_seat").build());
    }
}
