package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Lighted pole component.  Renders a constant beam when turned on.
 *
 * @author don_bruce
 */
public class TileEntityPole_StreetLight extends ATileEntityPole_Component {
    private ComputedVariable lightLevelVar;

    public TileEntityPole_StreetLight(TileEntityPole core, IWrapperPlayer placingPlayer, Axis axis, ItemPoleComponent item, IWrapperNBT data) {
        super(core, placingPlayer, axis, item, data);
        this.lightLevelVar = new ComputedVariable(this, "lightLevel");
    }

    @Override
    public float getLightProvided() {
        return (float) lightLevelVar.currentValue;
    }

    @Override
    public void setVariableDefaults() {
        super.setVariableDefaults();
        lightLevelVar.setTo(12F / 15F, false);
    }
}
