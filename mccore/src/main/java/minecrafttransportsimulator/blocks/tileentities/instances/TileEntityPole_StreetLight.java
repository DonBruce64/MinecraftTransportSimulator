package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
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

    public TileEntityPole_StreetLight(TileEntityPole core, IWrapperPlayer placingPlayer, Axis axis, IWrapperNBT data) {
        super(core, placingPlayer, axis, data);
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
    protected void updateVariableModifiers() {
        lightLevel = 12F / 15F;

        //Adjust current variables to modifiers, if any exist.
        if (definition.variableModifiers != null) {
            for (JSONVariableModifier modifier : definition.variableModifiers) {
                switch (modifier.variable) {
                    case "lightLevel":
                        lightLevel = adjustVariable(modifier, lightLevel);
                        break;
                    default:
                        setVariable(modifier.variable, adjustVariable(modifier, (float) getVariable(modifier.variable)));
                        break;
                }
            }
        }
    }
}
