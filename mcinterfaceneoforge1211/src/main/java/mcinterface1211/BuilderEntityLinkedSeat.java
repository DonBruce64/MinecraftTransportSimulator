package mcinterface1211;

import java.util.List;
import java.util.UUID;

import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Builder for an entity to sit in so they can ride another entity.  We use this rather
 * than a direct linking as entities with riders are removed by MC when the rider logs out.
 * This means that if we assigned this to the main entity, it would be removed when the rider left the server.
 * This is not ideal for things like trains where the engineer leaves and the main locomotive goes poof.
 *
 * @author don_bruce
 */
public class BuilderEntityLinkedSeat extends ABuilderEntityBase {
    public static DeferredHolder<EntityType<?>, EntityType<BuilderEntityLinkedSeat>> E_TYPE3;

    /**
     * UUID of entity we are a seat on.  This MAY be null if we haven't loaded NBT from the server yet.
     **/
    private UUID entityUUID;
    /**
     * Current entity we are a seat on.  This MAY be null if we haven't loaded NBT from the server yet.
     **/
    public AEntityB_Existing entity;
    /**
     * Current rider for this seat.  This MAY be null if we haven't loaded NBT from the server yet.
     **/
    protected WrapperEntity rider;
    /**
     * Set to true when the rider dismounts.  We set their position the next tick to override it.
     **/
    private boolean dismountedRider;

    private int ticksWithoutRider;

    public BuilderEntityLinkedSeat(EntityType<? extends BuilderEntityLinkedSeat> eType, Level world) {
        super(eType, world);
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
                discard();
            } else {
                setPos(entity.position.x, entity.position.y, entity.position.z);

                //Constantly check for the rider.  They might take a bit to load in.
                //If the rider dismounted us, just die.
                List<Entity> riders = getPassengers();
                if (rider == null && !riders.isEmpty()) {
                    if (entity.rider != null) {
                        rider = (WrapperEntity) entity.rider;
                    } else {
                        rider = WrapperEntity.getWrapperFor(riders.get(0));
                        entity.setRider(rider, true);
                    }
                } else if (dismountedRider) {
                    //Need to delay this by a few ticks on the client, since this is seen on clients before servers and this
                    //will cause packet de-sync errors in the logs if we use this all the time.
                    if (!level().isClientSide || ++ticksWithoutRider == 10) {
                        discard();
                    }
                } else if (rider != null && riders.isEmpty()) {
                    if (!level().isClientSide || ++ticksWithoutRider == 10) {
                        discard();
                    }
                }
            }
        } else if(entityUUID != null){
        	if(tickCount < 100) {
                WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(level());
        		entity = worldWrapper.getEntity(entityUUID);
        	}else {
        		InterfaceManager.coreInterface.logError("Found a seat but no entity was found for it.  Did a pack change?");
                discard();
        	}
        }else if (loadFromSavedNBT) {
            if (lastLoadedNBT.contains("entityUUID")) {
                entityUUID = lastLoadedNBT.getUUID("entityUUID");
                loadedFromSavedNBT = true;
            } else {
                InterfaceManager.coreInterface.logError("Found a seat not linked to an entity?  The heck?");
                discard();
            }
        }
    }


    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        //Notify internal entity of rider being removed.
        if (entity != null && rider != null) {
            if (!level().isClientSide && rider.equals(entity.rider)) {
                entity.removeRider();
            }
            rider = null;
            entity = null;
        }
    }

    @Override
    protected void positionRider(Entity pPassenger, Entity.MoveFunction pCallback) {
        if (this.hasPassenger(pPassenger)) {
            //Forward passenger updates to the entity.
            //Need to verify the entity has a rider, it might not if we are on the
            //client and waiting for the rider packet.  Or on the server and waiting for loading of the player.
            if (entity != null && entity.rider != null) {
                entity.updateRider();
                pCallback.accept(pPassenger, pPassenger.getX(), pPassenger.getY(), pPassenger.getZ());
                //Call getters so it resets to current value.
                //This allows the calling of the method in other areas to see MC deltas.
                //Make sure the rider wasn't removed, however.
                if (entity.rider != null) {
                    entity.rider.getYawDelta();
                    entity.rider.getPitchDelta();
                }
            }
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        //Need to check if we have ticked.  MC, on loading this entity, first dismounts all riders.
        //This will cause IV to see a dismount when in actuality it's a loading sequence.
        if (tickCount > 0) {
            dismountedRider = true;
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return entity instanceof PartSeat ? !((PartSeat) entity).definition.seat.standing : super.shouldRiderSit();
    }

    @Override
    public CompoundTag saveWithoutId(CompoundTag tag) {
        super.saveWithoutId(tag);
        if (entity != null) {
            //Entity is valid, save UUID and return the modified tag.
            tag.putUUID("entityUUID", entity.uniqueUUID);
        }
        return tag;
    }
}
