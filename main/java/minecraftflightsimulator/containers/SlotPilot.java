package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.entities.core.EntityVehicle;
import minecraftflightsimulator.items.ItemSeat;
import net.minecraft.item.ItemStack;

public class SlotPilot extends SlotItem{
	private EntityVehicle vehicle;

	public SlotPilot(EntityVehicle vehicle, int xDisplayPosition, int yDisplayPosition) {
		super(vehicle, xDisplayPosition, yDisplayPosition, vehicle.controllerSeatSlot, MFSRegistry.seat);
		this.vehicle = vehicle;
	}
    
    public int getSlotStackLimit(){
        return vehicle.getNumberControllerSeats();
    }
}
