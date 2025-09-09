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
    public static final String INVERTED_PREFIX = "!";
    public static final String CONSTANT_PREFIX = "#";

	/**The key of this variable, required to be unique to all variables on the entity.**/
    public final String variableKey;
    /**The entity this variable is defined on.**/
    public AEntityD_Definable<?> entity;
    private ComputedVariableOperator function;
    private boolean changesOnPartialTicks;
    private boolean randomVariable;
    private boolean isConstant;
    private boolean shouldSaveToNBT;
    private boolean changed;
    public boolean shouldReset = true;
    private long lastTickChecked;
    /**The current value of this variable.  Only change by calling one of the functions in this class.**/
    public double currentValue;
    /**True if {@link #currentValue} is greater than 1, false otherwise.  Used for quicker boolean operations.**/
    public boolean isActive;
    /**Internal variable for the inverted state of this variable.  Is read-only since we just set its states when ours change.
     * Is null on the inverted variable itself.**/
    public final ComputedVariable invertedVariable;

    /**Constructor for variables with logic that can change each tick or frame.**/
    public ComputedVariable(AEntityD_Definable<?> entity, String variable, ComputedVariableOperator function, boolean changesOnPartialTicks) {
        this.function = function;
        this.variableKey = variable;
        this.entity = entity;
        this.changesOnPartialTicks = changesOnPartialTicks;
        this.randomVariable = variable.startsWith("random");
        this.isConstant = variable.startsWith(CONSTANT_PREFIX);
        if (!variable.startsWith(INVERTED_PREFIX)) {
            if (isConstant) {
                this.invertedVariable = new ComputedVariable(entity, INVERTED_PREFIX + variable, null, changesOnPartialTicks);
            } else {
                this.invertedVariable = new ComputedVariable(entity, INVERTED_PREFIX + variable, partialTicks -> this.computeInvertedValue(partialTicks), changesOnPartialTicks);
            }
        } else {
            this.invertedVariable = null;
        }
        if(isConstant) {
            setInternal(Double.valueOf(variable.substring(CONSTANT_PREFIX.length())), true);
        }else {
            setInternal(0, true);
        }
    }

    /**Constructor for variables with no logic, and instead saved state.**/
    public ComputedVariable(AEntityD_Definable<?> entity, String variable, IWrapperNBT data) {
        this(entity, variable, null, false);
        if(data != null) {
            setInternal(data.getDouble(variableKey), true);
        }
        this.shouldSaveToNBT = true;
        this.shouldReset = false;
    }
    
    /**Constructor for variables with no logic, but also that don't save themselves, but also should not reset on changes.**/
    public ComputedVariable(AEntityD_Definable<?> entity, String variable) {
        this(entity, variable, null, false);
        this.shouldReset = false;
    }

    /**Constructor for making a constant of either true or false.**/
    public ComputedVariable(boolean bool) {
        this(null, bool ? "#1" : "#0", null, false);
    }

    @Override
    public String toString() {
        return variableKey + ":" + currentValue;
    }

    public static boolean isNumberedVariable(String variable) {
        return variable.matches("^.*_\\d+$");
    }

    /**
     * Helper method to get the index of the passed-in variable.  Indexes are defined by
     * variable names ending in _xx, where xx is a number.  The defined number is assumed
     * to be 1-indexed (JSON), but the returned number will be 0-indexed (Lists).  
     * When calling this method, ensure the variable defines a number, preferably by calling
     * {@link #isNumberedVariable(String)} before, otherwise this will cause a formatting crash.
     */
    public static int getVariableNumber(String variable) {
        return Integer.parseInt(variable.substring(variable.lastIndexOf('_') + 1)) - 1;
    }

    /**Helper to set the value, does other functions as well to maintain state.**/
    private final boolean setInternal(double value, boolean bypassChangeChecks) {
        if (currentValue != value || bypassChangeChecks) {
            currentValue = value;
            isActive = currentValue > 0;
            changed = true;
            if (invertedVariable != null) {
                invertedVariable.currentValue = currentValue > 0 ? 0 : 1;
                invertedVariable.isActive = !this.isActive;
            }
            if (!bypassChangeChecks) {
                changed = true;
            }
            return true;
        }
        return false;
    }

    public final void setFunctionTo(ComputedVariable other) {
        this.entity = other.entity;
        this.function = other.function;
        this.isConstant = other.isConstant;
        this.changesOnPartialTicks = other.changesOnPartialTicks;
        this.randomVariable = other.randomVariable;
        setInternal(other.currentValue, true);
    }

    public final double getValue() {
        return computeValue(0);
    }

    /**
     * Computes the value of this variable, updating {@link #currentValue}, and returning it.
     */
    public final double computeValue(float partialTicks) {
        if (function != null) {
            if (randomVariable || (changesOnPartialTicks && partialTicks != 0)) {
                setInternal(function.apply(partialTicks), false);
            } else if (lastTickChecked != entity.ticksExisted) {
                setInternal(function.apply(partialTicks), false);
                lastTickChecked = entity.ticksExisted;
            }
        }
        return currentValue;
    }

    private final double computeInvertedValue(float partialTicks) {
        computeValue(partialTicks);
        return invertedVariable.currentValue;
    }

    public final void setTo(double value, boolean sendPacket) {
        if (!isConstant && setInternal(value, false) && sendPacket) {
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableSet(this, currentValue));
        }
    }
    
    public final void setActive(boolean active, boolean sendPacket) {
        setTo(active ? 1 : 0, sendPacket);
    }

    public final void adjustBy(double value, boolean sendPacket) {
        if (!isConstant && setInternal(currentValue + value, false) && sendPacket) {
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, value));
        }
    }

    public final void toggle(boolean sendPacket) {
        if (!isConstant && setInternal(currentValue + currentValue > 0 ? 0 : 1, false) && sendPacket) {
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableToggle(this));
        }
    }

    /**
     * Returns true if the value was changed, false if not due to clamps.
     */
    public final boolean increment(double incrementValue, double minValue, double maxValue, boolean sendPacket) {
        if (!isConstant) {
            double newValue = currentValue + incrementValue;
            if (minValue != 0 || maxValue != 0) {
                if (newValue < minValue) {
                    newValue = minValue;
                } else if (newValue > maxValue) {
                    newValue = maxValue;
                }
            }
            //Need to round to avoid FPE math errors.
            newValue = Math.round(newValue * 1000) / 1000D;
            if (newValue != currentValue) {
                incrementValue = newValue - currentValue;
                if (setInternal(newValue, false) && sendPacket) {
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, incrementValue, minValue, maxValue));
                }
                return true;
            }
        }
        return false;
    }

    public boolean hasChanged() {
        if (changed) {
            changed = false;
            return true;
        }
        return false;
    }

    public final void saveToNBT(List<String> savedNames, IWrapperNBT data) {
        if (shouldSaveToNBT && currentValue != 0) {
            data.setDouble(variableKey, currentValue);
            savedNames.add(variableKey);
        }
    }

    @FunctionalInterface
    public interface ComputedVariableOperator {
        double apply(float operand);
    }
}
