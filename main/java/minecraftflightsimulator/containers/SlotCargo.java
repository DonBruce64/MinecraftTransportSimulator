package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.entities.core.EntityVehicle;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class SlotCargo extends SlotItem{
	private EntityVehicle vehicle;

	public SlotCargo(EntityVehicle vehicle, int xDisplayPosition, int yDisplayPosition) {
		super(vehicle, xDisplayPosition, yDisplayPosition, vehicle.cargoSlot, new ItemStack(Blocks.chest).getItem());
		this.vehicle=vehicle;
	}
    
    public int getSlotStackLimit(){
    	ItemStack seatStack = vehicle.getStackInSlot(vehicle.passengerSeatSlot);
    	byte numberSeats = (byte) (seatStack != null ? seatStack.stackSize : 0);
    	return vehicle.getNumberPassengerSeats() - numberSeats;
    }
}
