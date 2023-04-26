package mcinterface1165;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;

/**
 * Builder for an entity to sit in so they can ride another entity.  We use this rather
 * than a direct linking as entities with riders are removed by MC when the rider logs out.
 * This means that if we assigned this to the main entity, it would be removed when the rider left the server.
 * This is not ideal for things like trains where the engineer leaves and the main locomotive goes poof.
 *
 * @author don_bruce
 */
public class BuilderEntityLinkedSeat extends ABuilderEntityBase {
    public static RegistryObject<EntityType<BuilderEntityLinkedSeat>> E_TYPE3;

    /**
     * Current entity we are a seat on.  This MAY be null if we haven't loaded NBT from the server yet.
     **/
    public AEntityE_Interactable<?> entity;
    /**
     * Current rider for this seat.  This MAY be null if we haven't loaded NBT from the server yet.
     **/
    protected WrapperEntity rider;
    /**
     * Set to true when the rider dismounts.  We set their position the next tick to override it.
     **/
    private boolean dismountedRider;

    public BuilderEntityLinkedSeat(EntityType<? extends BuilderEntityLinkedSeat> eType, World world) {
        super(BuilderEntityLinkedSeat.E_TYPE3.get(), world);
    }

    @Override
    public void baseTick() {
        super.baseTick();

        //If our entity isn't null, update us to the entity position.
        //What really matters is the player's position, and that comes later.
        //This just gets us "close enough" so we don't de-spawn or something.
        if (entity != null) {
            //Check if the entity we are a seat on is still valid, or need to be set dead.
            if (!entity.isValid) {
                remove();
            } else {
                setPos(entity.position.x, entity.position.y, entity.position.z);

                //Constantly check for the rider.  They might take a bit to load in.
                //If the rider dismounted us, just die.
                List<Entity> riders = getPassengers();
                if (rider == null && !riders.isEmpty()) {
                    rider = WrapperEntity.getWrapperFor(riders.get(0));
                    //Check if the entity has a rider, if so, set ourselves to it.
                    //Otherwise, set it to ourselves.  We can get a rider here if we
                    //load it from saved disk, or we might be given one if the entity is clicked.
                    //Only set the rider loaded on the server though: clients will get packets.
                    if (entity.rider != null) {
                        rider = (WrapperEntity) entity.rider;
                    } else if (!level.isClientSide) {
                        entity.setRider(rider, true);
                    }
                } else if (dismountedRider) {
                    remove();
                }
            }
        } else {
            //If we have NBT, and haven't loaded it, do so now.
            if (!loadedFromSavedNBT && loadFromSavedNBT) {
                WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(level);
                try {
                    entity = worldWrapper.getEntity(lastLoadedNBT.getUUID("entityUUID"));
                    loadedFromSavedNBT = true;
                    lastLoadedNBT = null;
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("Failed to load seat on builder from saved NBT.  Did a pack change?");
                    InterfaceManager.coreInterface.logError(e.getMessage());
                    remove();
                }
            }
        }
    }

    @Override
    public void remove() {
        super.remove();
        //Notify internal entity of rider being removed.
        if (entity != null && rider != null) {
            if (!level.isClientSide && rider.equals(entity.rider)) {
                entity.removeRider();
            }
            rider = null;
            entity = null;
        }
    }

    @Override
    public void positionRider(Entity passenger) {
        if (entity != null) {
            entity.updateRider();
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        dismountedRider = true;
    }

    @Override
    public boolean shouldRiderSit() {
        return entity instanceof PartSeat ? !((PartSeat) entity).definition.seat.standing : super.shouldRiderSit();
    }

    @Override
    public CompoundNBT saveWithoutId(CompoundNBT tag) {
        super.saveWithoutId(tag);
        if (entity != null) {
            //Entity is valid, save UUID and return the modified tag.
            tag.putUUID("entityUUID", entity.uniqueUUID);
        }
        return tag;
    }
}
