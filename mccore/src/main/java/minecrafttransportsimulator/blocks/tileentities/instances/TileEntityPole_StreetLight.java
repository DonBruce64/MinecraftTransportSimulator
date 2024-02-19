package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Lighted pole component.  Renders a constant beam when turned on.
 *
 * @author don_bruce
 */
public class TileEntityPole_StreetLight extends ATileEntityPole_Component {
    private float lightLevel;

    public TileEntityPole_StreetLight(TileEntityPole core, IWrapperPlayer placingPlayer, Axis axis, ItemPoleComponent item, IWrapperNBT data) {
        super(core, placingPlayer, axis, item, data);
    }

    @Override
    public void update() {
        //Need to do this before updating as these require knowledge of prior states.
        //If we call super, then it will overwrite the prior state.
        //We update both our variables and our part variables here.
        updateVariableModifiers();

        super.update();
    }

    @Override
    public float getLightProvided() {
        return lightLevel;
    }

    @Override
    public void updateVariableModifiers() {
        lightLevel = 12F / 15F;

        //Adjust current variables to modifiers, if any exist.
        if (definition.variableModifiers != null) {
            for (JSONVariableModifier modifier : definition.variableModifiers) {
                switch (modifier.variable) {
                    case "lightLevel":
                        lightLevel = (float) adjustVariable(modifier, lightLevel);
                        break;
                    default:
                    	ComputedVariable variable = getVariable(modifier.variable);
                    	variable.setTo(adjustVariable(modifier, variable.currentValue), false);
                        break;
                }
            }
        }
    }
}
