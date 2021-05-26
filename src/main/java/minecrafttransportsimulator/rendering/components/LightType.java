package minecrafttransportsimulator.rendering.components;

import java.util.Collection;

/**Light types.  These are common to all lighting, be it vehicles or blocks or something else.
 * 
 * @author don_bruce
 */
public enum LightType{
	NAVIGATIONLIGHT(false, true),
	STROBELIGHT(false, false),
	TAXILIGHT(true, true),
	LANDINGLIGHT(true, true),
	BRAKELIGHT(false, false),
	BACKUPLIGHT(false, false),
	LEFTTURNLIGHT(false, false),
	RIGHTTURNLIGHT(false, false),
	LEFTINDICATORLIGHT(false, false),
	RIGHTINDICATORLIGHT(false, false),
	RUNNINGLIGHT(false, true),
	HEADLIGHT(true, true),
	EMERGENCYLIGHT(false, false),
	DAYTIMELIGHT(false, false),
	GENERICLIGHT(false, false),
	
	//The following light types are only for block-based systems.
	STOPLIGHT(false, false),
	CAUTIONLIGHT(false, false),
	GOLIGHT(false, false),
	
	STOPLIGHTLEFT(false, false),
	CAUTIONLIGHTLEFT(false, false),
	GOLIGHTLEFT(false, false),
	
	STOPLIGHTRIGHT(false, false),
	CAUTIONLIGHTRIGHT(false, false),
	GOLIGHTRIGHT(false, false),
	
	STREETLIGHT(true, true),
	DECORLIGHT(false, false);
	
	public final boolean hasBeam;
	public final boolean providesLight;
	public final String lowercaseName;
	
	private LightType(boolean hasBeam, boolean providesLight){
		this.hasBeam = hasBeam;
		this.providesLight = providesLight;
		this.lowercaseName = name().toLowerCase();
	}
	
	public boolean isInCollection(Collection<String> collection){
		return collection.contains(lowercaseName);
	}
	
	public static boolean isCollectionProvidingLight(Collection<String> collection){
		for(LightType light : LightType.values()){
			if(light.providesLight && collection.contains(light.lowercaseName)){
				return true;
			}
		}
		return false;
	}
}
