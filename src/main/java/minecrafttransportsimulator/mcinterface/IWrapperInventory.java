package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.baseclasses.IInventoryProvider;

/**
 * IWrapper for inventories.  This is mainly for the player, but works for any inventory in the game.
 * Say for inventories of MC crates.  This allows for adjustment of things for translating MC inventories
 * into our own internal framework.
 *
 * @author don_bruce
 */
public interface IWrapperInventory extends IInventoryProvider {

}