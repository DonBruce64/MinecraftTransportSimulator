package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.jsondefs.JSONPart.CrafterComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.JSONPartInteractable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketCrafterFuelAdd;
import minecrafttransportsimulator.packets.instances.PacketCrafterTimeSet;

/**
 * Basic crafter class.  Class is essentially an inventory that holds state of crafting
 * operations.  Has a method for ticking and abstract methods for crafter-specific operations.
 *
 * @author don_bruce
 */
public abstract class AEntityCrafter extends EntityInventoryContainer {
    public static final int FUEL_ITEM_SLOT = 0;
    protected static final IWrapperItemStack EMPTY_STACK = InterfaceManager.coreInterface.getStackForProperties("NOTHING", 0, 1);

    public int ticksFuelProvides;
    public int ticksLeftOfFuel;
    public int ticksNeededToCraft;
    public int ticksLeftToCraft;
    public double powerToDrawPerTick;
    public final JSONPartInteractable definition;

    private final int[] inputSlots;
    private final int[] outputSlots;

    public AEntityCrafter(AWrapperWorld world, IWrapperNBT data, int maxSlots, JSONPartInteractable definition, int[] inputSlots, int[] outputSlots) {
        super(world, data, maxSlots);
        this.definition = definition;
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
        if (data != null) {
            this.ticksFuelProvides = data.getInteger("ticksFuelProvides");
            this.ticksLeftOfFuel = data.getInteger("ticksLeftOfFuel");
            this.ticksNeededToCraft = data.getInteger("ticksNeededToCraft");
            this.ticksLeftToCraft = data.getInteger("ticksLeftToCraft");
            this.powerToDrawPerTick = data.getDouble("powerToDrawPerTick");
        }
    }

    @Override
    public void update() {
        super.update();
        if (ticksLeftToCraft > 0) {
            //If we have no fuel, and are a standard type, get fuel from the stack in us.
            if (!world.isClient() && ticksLeftOfFuel == 0 && definition.crafterType == CrafterComponentType.STANDARD) {
                IWrapperItemStack fuelStack = getStack(FUEL_ITEM_SLOT);
                if (!fuelStack.isEmpty()) {
                    ticksFuelProvides = getFuelTime(fuelStack);
                    ticksLeftOfFuel = ticksFuelProvides;
                    InterfaceManager.packetInterface.sendToAllClients(new PacketCrafterFuelAdd(this));
                    removeFromSlot(FUEL_ITEM_SLOT, 1);
                }
            }

            //Make sure we have inputs.  These could have been removed.
            if (ticksNeededToCraft > 0 && !world.isClient()) {
                boolean foundInput = false;
                for (int i = 0; i < inputSlots.length; ++i) {
                    IWrapperItemStack inputStack = getStack(inputSlots[i]);
                    if (!inputStack.isEmpty()) {
                        foundInput = true;
                        break;
                    }
                }
                if (!foundInput) {
                    ticksNeededToCraft = 0;
                    ticksLeftToCraft = ticksNeededToCraft;
                    InterfaceManager.packetInterface.sendToAllClients(new PacketCrafterTimeSet(this));
                }
            }

            //We are crafting and have fuel, continue the process.
            if (ticksLeftOfFuel > 0) {
                --ticksLeftOfFuel;
                if (world.isClient()) {
                    if (ticksLeftToCraft > 0) {
                        --ticksLeftToCraft;
                    }
                } else {
                    if (--ticksLeftToCraft == 0) {
                        //Remove from input and add to output.
                        //Need to set the stack in case the output is empty or we have multiple items.
                        for (int i = 0; i < inputSlots.length; ++i) {
                            //Do this in this order in case input and output slots are common, ensures slot is empty when checking outputs.
                            IWrapperItemStack craftingOutput = getResultForSlot(inputSlots[i]);
                            if (!craftingOutput.isEmpty()) {
                                removeFromSlot(inputSlots[i], 1);
                                IWrapperItemStack existingOutput = getStack(outputSlots[i]);
                                if (existingOutput.isEmpty()) {
                                    existingOutput = craftingOutput;
                                } else {
                                    existingOutput.add(craftingOutput.getSize());
                                }
                                setStack(existingOutput, outputSlots[i]);
                            }
                        }
                        ticksNeededToCraft = 0;
                        performPostCraftingOperations();
                    }
                }
            } else {
                ticksFuelProvides = 0;
            }
        } else {
            //Not currently crafting, see if we can craft anything.
            if (!world.isClient()) {
                for (int i = 0; i < inputSlots.length; ++i) {
                    IWrapperItemStack inputStack = getStack(inputSlots[i]);
                    IWrapperItemStack outputStack = getResultForSlot(inputSlots[i]);
                    if (!outputStack.isEmpty()) {
                        ticksNeededToCraft = (int) (getTimeForItem(inputStack) * 1F / definition.crafterRate);
                        ticksLeftToCraft = ticksNeededToCraft;
                        InterfaceManager.packetInterface.sendToAllClients(new PacketCrafterTimeSet(this));
                    }
                }
            }
        }
    }

    /**
     * Returns the internal fuel type name for this crafter.
     */
    public abstract String getFuelName();

    /**
     * Returns the fuel time for the stack.
     */
    protected abstract int getFuelTime(IWrapperItemStack stack);

    /**
     * Returns the resulting item for a crafting operation at the specific slot.  This should
     * check all inputs and outputs, but does not need to check fuel as that's common code.
     */
    protected abstract IWrapperItemStack getResultForSlot(int index);

    /**
     * Returns the time it takes to process this item.
     * Note that {@link #getResultForSlot(int)} will be called before this, so this
     * method can be assured to be called only on processable items.
     */
    protected abstract int getTimeForItem(IWrapperItemStack stack);

    /**
     * Called after crafting has completed and all stacks are set.
     * Normally does nothing, but can do something if desired.
     * Only called on servers.
     */
    protected void performPostCraftingOperations() {
    }

    @Override
    public boolean isStackValid(IWrapperItemStack stackToCheck, int index) {
        if (index == FUEL_ITEM_SLOT) {
            return definition.crafterType == CrafterComponentType.STANDARD && getFuelTime(stackToCheck) != 0;
        } else {
            return false;
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setInteger("ticksFuelProvides", ticksFuelProvides);
        data.setInteger("ticksLeftOfFuel", ticksLeftOfFuel);
        data.setInteger("ticksNeededToCraft", ticksNeededToCraft);
        data.setInteger("ticksLeftToCraft", ticksLeftToCraft);
        data.setDouble("powerToDrawPerTick", powerToDrawPerTick);
        return data;
    }
}
