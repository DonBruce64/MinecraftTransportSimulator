package minecrafttransportsimulator.items.components;

import java.util.Map;

import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Interface for items that create entities when used.
 *
 * @author don_bruce
 */
public interface IItemEntityProvider {

    /**
     * Registers all applicable entities this item can create.
     * An item can create multiple entities, provided those entities
     * have unique class names.
     */
    void registerEntities(Map<String, IItemEntityFactory> entityMap);

    public static interface IItemEntityFactory {
        AEntityB_Existing createEntity(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data);
    }
}
