package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Sign pole component.  Renders a sign texture and text.
 *
 * @author don_bruce
 */
public class TileEntityPole_Sign extends ATileEntityPole_Component {

    public TileEntityPole_Sign(TileEntityPole core, IWrapperPlayer placingPlayer, Axis axis, ItemPoleComponent item, IWrapperNBT data) {
        super(core, placingPlayer, axis, item, data);
    }
}
