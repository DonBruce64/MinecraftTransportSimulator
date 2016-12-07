package minecraftflightsimulator.containers;

import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.entities.core.EntityVehicle;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class SlotLoadable extends SlotItem{
	private static final Item chestItem = new ItemStack(Blocks.chest).getItem();
	private final EntityVehicle vehicle;
	private final SeatTypes type;

	public SlotLoadable(EntityVehicle vehicle, int xDisplayPosition, int yDisplayPosition, SeatTypes type){
		super(vehicle, xDisplayPosition, yDisplayPosition, type.getSlot(vehicle), type.validItems);
		this.vehicle = vehicle;
		this.type = type;
	}
    
    public int getSlotStackLimit(){
    	return type.getStackLimit(vehicle);
    }
    
    public enum SeatTypes{
    	CONTROLLER(MFSRegistry.seat),
    	PASSENGER(MFSRegistry.seat),
    	CARGO(chestItem),
    	FORWARD_MIXED(MFSRegistry.seat, chestItem),
    	AFT_MIXED(MFSRegistry.seat, chestItem);
    	
    	private final Item[] validItems;
    	private SeatTypes(Item... validItems){
    		this.validItems = validItems;
    	}
    	
    	private int getSlot(EntityVehicle vehicle){
    		switch (this){
				case CONTROLLER: return vehicle.controllerSlot;
				case PASSENGER: return vehicle.passengerSlot;
				case CARGO: return vehicle.cargoSlot;
				case FORWARD_MIXED: return vehicle.forwardMixedSlot;
				case AFT_MIXED: return vehicle.aftMixedSlot;
				default: return -1;
    		}
    	}
    	
    	private int getStackLimit(EntityVehicle vehicle){
    		switch (this){
				case CONTROLLER: return vehicle.getControllerCapacity();
				case PASSENGER: return vehicle.getPassengerCapacity();
				case CARGO: return vehicle.getCargoCapacity();
				case FORWARD_MIXED: return vehicle.getForwardMixedCapacity();
				case AFT_MIXED: return vehicle.getAftMixedCapacity();
				default: return 0;
    		}
    	}
    }
}
