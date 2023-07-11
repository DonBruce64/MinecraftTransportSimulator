package mcinterface1182;

import minecrafttransportsimulator.items.components.AItemBase;

/**
 * Interface required for BuilderItem items to implement.
 * This allows for non-BuilderItem type wrappers, say if
 * another mod wants to use their own item class but still
 * note it as a MTS item.
 *
 * @author don_bruce
 */
public interface IBuilderItemInterface {
    AItemBase getWrappedItem();
}