package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.baseclasses.AEntityC_Definable;

/**Base Entity rendering class.  
 *
 * @author don_bruce
 */
public abstract class ARenderEntity<RenderedEntity extends AEntityC_Definable<?>>{
	
	/**
	 *  Called to render this entity.  The currently-bound texture is undefined, so you will need
	 *  to bind whichever texture you see fit to do so.  This can be done via {@link InterfaceRender#bindTexture(String, String)}
	 */
	public abstract void render(RenderedEntity entity, float partialTicks);
}
