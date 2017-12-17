package minecrafttransportsimulator.dataclasses;

import minecrafttransportsimulator.systems.PackParserSystem.MultipartTypes;

public final class MTSInstruments{
	/**Number of blank-type instruments present at the top of the enum.
	 * Skip this many enums to get actual placeable instruments.
	 */
	public static final byte numberBlanks = 1;
	
	public enum Instruments{
		AIRCRAFT_BLANK(false, MultipartTypes.PLANE),
    	AIRCRAFT_ATTITUDE(false, MultipartTypes.PLANE),
    	AIRCRAFT_ALTIMETER(false, MultipartTypes.PLANE),
    	AIRCRAFT_HEADING(false, MultipartTypes.PLANE),
    	AIRCRAFT_AIRSPEED(false, MultipartTypes.PLANE),
    	AIRCRAFT_TURNCOORD(false, MultipartTypes.PLANE),
    	AIRCRAFT_TURNSLIP(false, MultipartTypes.PLANE),
    	AIRCRAFT_VERTICALSPEED(false, MultipartTypes.PLANE),
    	AIRCRAFT_LIFTRESERVE(false, MultipartTypes.PLANE),
    	AIRCRAFT_TRIM(false, MultipartTypes.PLANE),
    	AIRCRAFT_ELECTRIC(true, MultipartTypes.PLANE),
    	AIRCRAFT_FUELQTY(false, MultipartTypes.PLANE),
    	AIRCRAFT_TACHOMETER(true, MultipartTypes.PLANE),
    	AIRCRAFT_FUELFLOW(true, MultipartTypes.PLANE),
    	AIRCRAFT_ENGINETEMP(true, MultipartTypes.PLANE),
    	AIRCRAFT_OILPRESSURE(true, MultipartTypes.PLANE);
    	
    	public final boolean canConnectToEngines;
    	public final MultipartTypes[] validTypes;
    	
    	private Instruments(boolean canConnectToEngines, MultipartTypes... validTypes){
    		this.canConnectToEngines = canConnectToEngines;
    		this.validTypes = validTypes;
    	}
    }
    
    public enum Controls{
    	AIRCRAFT_THROTTLE(MultipartTypes.PLANE),
    	PARKING_BRAKE(MultipartTypes.PLANE),
    	AIRCRAFT_FLAPS(MultipartTypes.PLANE);
    	
    	public final MultipartTypes[] vaildTypes;
    	
    	private Controls(MultipartTypes... validTypes){
    		this.vaildTypes = validTypes;
    	}
    }
}
