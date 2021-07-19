package minecrafttransportsimulator.rendering.components;

/**Light types.  These are common to all lighting, be it vehicles or blocks or something else.
 * 
 * @author don_bruce
 */
public enum LightType{
	//The following light types are only for block-based systems.
	STOPLIGHT,
	CAUTIONLIGHT,
	GOLIGHT,
	
	STOPLIGHTLEFT,
	CAUTIONLIGHTLEFT,
	GOLIGHTLEFT,
	
	STOPLIGHTRIGHT,
	CAUTIONLIGHTRIGHT,
	GOLIGHTRIGHT;
	
	public final String lowercaseName;
	
	private LightType(){
		this.lowercaseName = name().toLowerCase();
	}
}
