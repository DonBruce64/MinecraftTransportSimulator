package minecrafttransportsimulator.dataclasses;

import minecrafttransportsimulator.vehicles.main.EntityVehicleD_Moving;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Plane;

public final class MTSControls{ 
    public enum Controls{
    	AIRCRAFT_THROTTLE(EntityVehicleF_Plane.class),
    	PARKING_BRAKE(EntityVehicleF_Plane.class),
    	AIRCRAFT_FLAPS(EntityVehicleF_Plane.class);
    	
    	public final Class<? extends EntityVehicleD_Moving>[] validClasses;
    	
    	@SafeVarargs
    	private Controls(Class<? extends EntityVehicleD_Moving>... validClasses){
    		this.validClasses = validClasses;
    	}
    }
}
