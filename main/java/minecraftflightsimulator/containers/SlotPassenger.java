package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.entities.core.EntityVehicle;

public class SlotPassenger extends SlotItem{
	private EntityVehicle vehicle;

	public SlotPassenger(EntityVehicle vehicle, int xDisplayPosition, int yDisplayPosition) {
		super(vehicle, xDisplayPosition, yDisplayPosition, vehicle.passengerSeatSlot, MFSRegistry.seat, MFS.proxy.getStackByItemName("chest", -1).getItem());
		this.vehicle=vehicle;
	}
    
    public int getSlotStackLimit(){
        return vehicle.getNumberPassengerSeats();
    }
}
