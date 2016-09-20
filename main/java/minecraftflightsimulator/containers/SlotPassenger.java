package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.entities.core.EntityVehicle;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class SlotPassenger extends SlotItem{
	private EntityVehicle vehicle;

	public SlotPassenger(EntityVehicle vehicle, int xDisplayPosition, int yDisplayPosition) {
		super(vehicle, xDisplayPosition, yDisplayPosition, vehicle.passengerSeatSlot, MFSRegistry.seat, new ItemStack(Blocks.chest).getItem());
		this.vehicle=vehicle;
	}
    
    public int getSlotStackLimit(){
        return vehicle.getNumberPassengerSeats();
    }
}
