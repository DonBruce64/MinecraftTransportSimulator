package minecrafttransportsimulator.baseclasses;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;

/**
 * A helper class of sorts for calculating computed variables.  This helper class wraps up a set of functions
 * for obtaining the variable value based on the various constructor arguments.  In more specific
 * terms, it parses the string-saved information and can return a specific value for this information.
 * The value to be provided is based on a functional interface provided to the object, the implementation
 * of this being left up to however this implementation is structured.
 *
 * @author don_bruce
 */
public class ComputedVariable {
    private final String variable;
    private final ComputedVariableOperator function;
    private final AEntityD_Definable<?> entity;
    private final boolean changesOnPartialTicks;
    private final boolean randomVariable;
    private final boolean isInverted;
    private long lastTickChecked;
    private double currentValue;

    public ComputedVariable(AEntityD_Definable<?> entity, String variable, ComputedVariableOperator function, boolean changesOnPartialTicks) {
        this.function = function;
        this.variable = variable;
        this.entity = entity;
        this.changesOnPartialTicks = changesOnPartialTicks;
        this.randomVariable = variable.startsWith("random");
        this.isInverted = variable.startsWith("!");
    }

    /**Constructor for variables with no logic, and instead maintained state.**/
    public ComputedVariable(AEntityD_Definable<?> entity, String variable) {
        this(entity, variable, null, false);
        if (variable.startsWith("#")) {
            this.currentValue = Double.parseDouble(variable.substring("#".length()));
        }
    }

    public static boolean isNumberedVariable(String variable) {
        return variable.matches("^.*_\\d+$");
    }

    /**
     * Helper method to get the index of the passed-in variable.  Indexes are defined by
     * variable names ending in _xx, where xx is a number.  The defined number is assumed
     * to be 1-indexed (JSON), but the returned number will be 0-indexed (Lists).  
     * If the variable doesn't define a number, then -1 is returned.
     */
    public static int getVariableNumber(String variable) {
        return Integer.parseInt(variable.substring(variable.lastIndexOf('_') + 1)) - 1;
    }

    public final double getValue() {
        return getValue(0);
    }

    public final double getValue(float partialTicks) {
        if (function != null) {
            if (randomVariable || (changesOnPartialTicks && partialTicks != 0)) {
                currentValue = function.apply(partialTicks);
            } else if (lastTickChecked != entity.ticksExisted) {
                currentValue = function.apply(partialTicks);
                lastTickChecked = entity.ticksExisted;
            }
            if (isInverted) {
                currentValue = currentValue > 0 ? 0 : 1;
            }
        }
        return currentValue;
    }

    public final void setTo(double value, boolean sendPacket) {
        currentValue = value;
        if (sendPacket) {
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableSet(entity, variable, currentValue));
        }
    }

    public final void toggle(boolean sendPacket) {
        currentValue = currentValue > 0 ? 0 : 1;
        if (sendPacket) {
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableToggle(entity, variable));
        }
    }

    /**
     * Returns true if the value was changed, false if not due to clamps.
     */
    public final boolean increment(double incrementValue, double minValue, double maxValue, boolean sendPacket) {
        double newValue = currentValue + incrementValue;
        if (minValue != 0 || maxValue != 0) {
            if (newValue < minValue) {
                newValue = minValue;
            } else if (newValue > maxValue) {
                newValue = maxValue;
            }
        }
        if (newValue != currentValue) {
            currentValue = newValue;
            if (sendPacket) {
                InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(entity, variable, incrementValue, minValue, maxValue));
            }
            return true;
        } else {
            return false;
        }
    }

    public final boolean isActive() {
        return isInverted ? getValue(0) == 0 : getValue(0) > 0;
    }

    public final void saveToNBT(List<String> savedNames, IWrapperNBT data) {
        if (function == null) {
            data.setDouble(variable, currentValue);
            savedNames.add(variable);
        }
    }

    @FunctionalInterface
    public interface ComputedVariableOperator {
        double apply(float operand);
    }
}
