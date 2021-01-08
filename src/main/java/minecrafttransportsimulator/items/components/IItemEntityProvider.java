package minecrafttransportsimulator.items.components;

import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.vehicles.main.AEntityBase;

/**Interface for items that create entities when used.
 *
 * @author don_bruce
 */
public interface IItemEntityProvider<EntityType extends AEntityBase>{
	
	/**
	 *  Gets a new instance of the entity this item creates.
	 */
	public EntityType createEntity(IWrapperWorld world, IWrapperEntity wrapper, IWrapperPlayer playerSpawning, WrapperNBT data);
	
	/**
	 *  Gets the class of the Entity this item makes.
	 *  This is for registration, not construction.  For construction,
	 *  use {@link #createEntity(IWrapperWorld, WrapperNBT)}
	 */
	public Class<EntityType> getEntityClass();
}
