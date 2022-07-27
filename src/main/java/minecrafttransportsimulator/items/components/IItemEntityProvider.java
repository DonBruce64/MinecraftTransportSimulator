package minecrafttransportsimulator.items.components;

import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Interface for items that create entities when used.
 *
 * @author don_bruce
 */
public interface IItemEntityProvider<EntityType extends AEntityB_Existing> {

    /**
     * Gets a new instance of the entity this item creates.
     */
    EntityType createEntity(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data);

    /**
     * Gets the class of the Entity this item makes.
     * This is for registration, not construction. For construction,
     * use {@link #createEntity(AWrapperWorld, IWrapperPlayer, IWrapperNBT)}
     */
    Class<EntityType> getEntityClass();
}
