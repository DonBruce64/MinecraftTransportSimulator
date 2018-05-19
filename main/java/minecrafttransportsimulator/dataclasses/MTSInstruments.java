package minecrafttransportsimulator.dataclasses;

import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;

public final class MTSInstruments{
	/**Number of blank-type instruments present at the top of the enum.
	 * Skip this many enums to get actual placeable instruments.
	 */
	public static final byte numberBlanks = 1;
	
	public enum Instruments{
		AIRCRAFT_BLANK(false, EntityMultipartF_Plane.class),
    	AIRCRAFT_ATTITUDE(false, EntityMultipartF_Plane.class),
    	AIRCRAFT_ALTIMETER(false, EntityMultipartF_Plane.class),
    	AIRCRAFT_HEADING(false, EntityMultipartF_Plane.class),
    	AIRCRAFT_AIRSPEED(false, EntityMultipartF_Plane.class),
    	AIRCRAFT_TURNCOORD(false, EntityMultipartF_Plane.class),
    	AIRCRAFT_TURNSLIP(false, EntityMultipartF_Plane.class),
    	AIRCRAFT_VERTICALSPEED(false, EntityMultipartF_Plane.class),
    	AIRCRAFT_LIFTRESERVE(false, EntityMultipartF_Plane.class),
    	AIRCRAFT_TRIM(false, EntityMultipartF_Plane.class),
    	AIRCRAFT_ELECTRIC(true, EntityMultipartF_Plane.class),
    	AIRCRAFT_FUELQTY(false, EntityMultipartF_Plane.class),
    	AIRCRAFT_TACHOMETER(true, EntityMultipartF_Plane.class),
    	AIRCRAFT_FUELFLOW(true, EntityMultipartF_Plane.class),
    	AIRCRAFT_ENGINETEMP(true, EntityMultipartF_Plane.class),
    	AIRCRAFT_OILPRESSURE(true, EntityMultipartF_Plane.class);
    	
    	public final boolean canConnectToEngines;
    	public final Class<? extends EntityMultipartD_Moving>[] validClasses;
    	
    	private Instruments(boolean canConnectToEngines, Class<? extends EntityMultipartD_Moving>... validClasses){
    		this.canConnectToEngines = canConnectToEngines;
    		this.validClasses = validClasses;
    	}
    }
    
    public enum Controls{
    	AIRCRAFT_THROTTLE(EntityMultipartF_Plane.class),
    	PARKING_BRAKE(EntityMultipartF_Plane.class),
    	AIRCRAFT_FLAPS(EntityMultipartF_Plane.class);
    	
    	public final Class<? extends EntityMultipartD_Moving>[] validClasses;
    	
    	private Controls(Class<? extends EntityMultipartD_Moving>... validClasses){
    		this.validClasses = validClasses;
    	}
    }
}
