package minecrafttransportsimulator.dataclasses;

import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;

public final class MTSControls{ 
    public enum Controls{
    	AIRCRAFT_THROTTLE(EntityMultipartF_Plane.class),
    	PARKING_BRAKE(EntityMultipartF_Plane.class),
    	AIRCRAFT_FLAPS(EntityMultipartF_Plane.class);
    	
    	public final Class<? extends EntityMultipartD_Moving>[] validClasses;
    	
    	@SafeVarargs
    	private Controls(Class<? extends EntityMultipartD_Moving>... validClasses){
    		this.validClasses = validClasses;
    	}
    }
}
