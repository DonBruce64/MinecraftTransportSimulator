package minecrafttransportsimulator.rendering.components;

import java.util.Collection;

/**Light types.  These are common to all lighting, be it vehicles or blocks or something else.
 * 
 * @author don_bruce
 */
public enum LightType{
	NAVIGATIONLIGHT(false),
	STROBELIGHT(false),
	TAXILIGHT(true),
	LANDINGLIGHT(true),
	BRAKELIGHT(false),
	BACKUPLIGHT(false),
	LEFTTURNLIGHT(false),
	RIGHTTURNLIGHT(false),
	LEFTINDICATORLIGHT(false),
	RIGHTINDICATORLIGHT(false),
	RUNNINGLIGHT(false),
	HEADLIGHT(true),
	EMERGENCYLIGHT(false),
	DAYTIMELIGHT(false),
	GENERICLIGHT(false),
	
	//The following light types are only for block-based systems.
	UNLINKEDLIGHT(false),
	STOPLIGHT(false),
	CAUTIONLIGHT(false),
	GOLIGHT(false),
	STREETLIGHT(true),
	DECORLIGHT(false);
	
	public final boolean hasBeam;
	public final String lowercaseName;
	
	private LightType(boolean hasBeam){
		this.hasBeam = hasBeam;
		this.lowercaseName = name().toLowerCase();
	}
	
	public boolean isInCollection(Collection<String> collection){
		return collection.contains(lowercaseName);
	}
}
