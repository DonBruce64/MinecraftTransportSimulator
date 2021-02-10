package minecrafttransportsimulator.items.components;

import minecrafttransportsimulator.baseclasses.AEntityB_Existing;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Interface for items that create entities when used.
 *
 * @author don_bruce
 */
public interface IItemEntityProvider<EntityType extends AEntityB_Existing>{
	
	/**
	 *  Gets a new instance of the entity this item creates.
	 */
	public EntityType createEntity(WrapperWorld world, WrapperEntity wrapper, WrapperPlayer playerSpawning, WrapperNBT data);
	
	/**
	 *  Gets the class of the Entity this item makes.
	 *  This is for registration, not construction.  For construction,
	 *  use {@link #createEntity(WrapperWorld, WrapperNBT)}
	 */
	public Class<EntityType> getEntityClass();
}
