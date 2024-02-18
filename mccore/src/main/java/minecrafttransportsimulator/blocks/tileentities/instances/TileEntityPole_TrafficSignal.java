package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.LightType;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.SignalGroup;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Traffic signal component for poles.  This doesn't tick, as the state of the light
 * is by default having the unlinked light on until changed by a {@link TileEntitySignalController}.
 *
 * @author don_bruce
 */
public class TileEntityPole_TrafficSignal extends ATileEntityPole_Component {
    public TileEntitySignalController linkedController;

    public TileEntityPole_TrafficSignal(TileEntityPole core, IWrapperPlayer placingPlayer, Axis axis, ItemPoleComponent item, IWrapperNBT data) {
        super(core, placingPlayer, axis, item, data);
    }

    @Override
    public void update() {
        super.update();
        if (linkedController != null) {
            //Remove all old lights, then add our new one.
            for (LightType light : LightType.values()) {
                light.lightNames.forEach(name -> getVariable(name).setTo(0, false));
            }
            if (linkedController.isValid && linkedController.controlledSignals.contains(this)) {
                for (SignalGroup group : linkedController.signalGroups.get(axis)) {
                    group.currentLight.lightNames.forEach(name -> getVariable(name).setTo(1, false));
                }
            } else {
                linkedController = null;
            }
        }
    }

    @Override
    public ComputedVariable createComputedVariable(String variable) {
        switch (variable) {
            case ("linked"):
                return new ComputedVariable(this, variable, partialTicks -> linkedController != null ? 1 : 0, false);
            default:
                return super.createComputedVariable(variable);
        }
    }
}
