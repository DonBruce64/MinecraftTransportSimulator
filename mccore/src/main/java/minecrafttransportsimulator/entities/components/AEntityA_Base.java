package minecrafttransportsimulator.entities.components;

import java.util.UUID;

import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketWorldEntityRemove;

/**
 * Base entity class.  This class is the base for all in-game entities.  What these
 * entities are is up to the extending class.  They could be players, vehicles, blocks,
 * guns, etc.  The key part here is this base class keeps track of all these entities, and
 * is responsible for assigning them a global ID number when created.  This can be used to
 * obtain their instances via packets as the ID is the same between clients and servers.
 * <br><br>
 * Note that all entities of this type never "load" saved data.  Rather, they are created from it.
 * This means that there is no method call to load properties from data. Instead, data required
 * for loading will be passed-in to the constructor.  This data should be used to create the entity
 * in its loaded state.  For saving, which can happen multiple times in the entity's lifetime,
 * {@link #save(IWrapperNBT)} is called.  All data required in the constructor should be saved there.
 *
 * @author don_bruce
 */
public abstract class AEntityA_Base {
    /**
     * The world this entity is a part of.
     **/
    public final AWrapperWorld world;
    /**
     * A unique ID for this entity.  This is only set when this entity is first spawned, and never changes, even on save/load operations.  Ideal if you need a static reference to the entity.
     **/
    public final UUID uniqueUUID;
    /**
     * True as long as this entity is part of the world and being ticked.  May be set false internally or externally to remove this entity from the world.
     **/
    public boolean isValid = true;
    /**
     * Returns true if this entity was newly created and not loaded from saved data.  More formally, it checks if the {@link #uniqueUUID} was not stored in the data, or if the data was null.
     * In this case, we know that the data was not valid for the entity and thus the entity has to have been created without data.
     **/
    public final boolean newlyCreated;
    /**
     * Counter for how many ticks this entity has existed in the world.  Realistically, it's the number of update cycles.
     **/
    public long ticksExisted;

    public AEntityA_Base(AWrapperWorld world, IWrapperNBT data) {
        this.world = world;
        this.newlyCreated = data == null || data.getUUID("uniqueUUID") == null;

        //Get the map of entities we belong to.
        if (shouldSync() && !newlyCreated) {
            //Check to make sure we aren't a duplicate.
            //If we are, and we are on a server, we must be a copy.
            //If so, we need to make anew UUID for this entity so it doesn't foul packets.
            UUID savedUUID = data.getUUID("uniqueUUID");
            if (!world.isClient() && world.getEntity(savedUUID) != null) {
                this.uniqueUUID = UUID.randomUUID();
            } else {
                this.uniqueUUID = savedUUID;
            }
        } else {
            this.uniqueUUID = UUID.randomUUID();
        }
    }

    /**
     * Called to update this entity.  This  may not be called if the entity extending this class
     * is not slated for updates in some sort of system.
     */
    public void update() {
        ++ticksExisted;
    }

    /**
     * Returns the update type for the entity.
     */
    public EntityUpdateType getUpdateType() {
        return EntityUpdateType.MAIN;
    }

    /**
     * Normally, all entities sync across clients and servers via their {@link #uniqueUUID}.
     * However, some entities may be client-side, server-side only, or both sides but spawned so frequently
     * syncing isn't desirable.  These entities should return false here to prevent corrupting the lookup mappings.  
     * This also should prevent the loading of any NBT data in the constructor, as none exists to load from and that variable will
     * be null.
     */
    public boolean shouldSync() {
        return true;
    }

    /**
     * Called to remove this entity from the world.  Removal should perform any and all logic required to ensure
     * no references are left to this entity in any objects.  This ensures memory can be freed for use elsewhere,
     * and lingering references do not exist.  After removal, the entity should be removed from the world map
     * to let the world to not tick or render it anymore.
     */
    public void remove() {
        if (isValid) {
            isValid = false;
            world.removeEntity(this);
            if (!world.isClient() && shouldSync()) {
                //Send packet to all clients to notify them of entity removal.
                InterfaceManager.packetInterface.sendToAllClients(new PacketWorldEntityRemove(this));
            }
        }
    }

    /**
     * Returns the mass of this entity for physics calculations.  Normally un-used except in the multiple sub-classes,
     * but since anything can have mass, we put this at the A-level.
     */
    public double getMass() {
        return 0;
    }

    /**
     * Called when the entity needs to be saved to disk.  The passed-in IWrapper
     * should be written to at this point with any data needing to be saved.
     * Returns the passed-in data IWrapper for convenience.
     */
    public IWrapperNBT save(IWrapperNBT data) {
        data.setUUID("uniqueUUID", uniqueUUID);
        return data;
    }

    public static enum EntityUpdateType {
        /**Entity will never have its {@link AEntityA_Base#update()} method called.**/
        NONE,
        /**Entity will update during the main update calls.**/
        MAIN,
        /**Entity will update after all update calls have been processed, including external entities.
         * Useful for entities that depend on external entity processing for their states.**/
        LAST;

    }
}
