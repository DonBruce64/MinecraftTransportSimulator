package mcinterface1122;

import minecrafttransportsimulator.mcinterface.IWrapperNBT;

/**
 * Client-side bridge for opening the native inventory screen.  Keeping this interface free of
 * net.minecraft.client types lets the shared packet class invoke client behavior without putting
 * client-only class references in bytecode that is also loaded by dedicated servers.
 */
interface IInventoryGUIClient {
    void openInventoryGUI(IWrapperNBT inventoryData, int inventorySize, int stackSize, String texture, int heldPlayerSlot, int windowID);
}
