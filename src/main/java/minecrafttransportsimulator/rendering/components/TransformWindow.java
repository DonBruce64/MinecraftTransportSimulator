package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.systems.ConfigSystem;

/**This class represents a window object of a model.  The only transform this applies is binding
 * the glass texture prior to rendering, and rendering the window inner parts if set in the config.
 *
 * @author don_bruce
 */
public class TransformWindow<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	private final boolean interior;
	
	public TransformWindow(boolean interior){
		this.interior = interior;
	}
	
	@Override
	public boolean shouldRender(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		if(interior){
			return ConfigSystem.configObject.clientRendering.renderWindows.value && ConfigSystem.configObject.clientRendering.innerWindows.value;
		}else{
			return ConfigSystem.configObject.clientRendering.renderWindows.value; 
		}
	}

	@Override
	public double applyTransform(AnimationEntity entity, boolean blendingEnabled, float partialTicks, double offset){
		InterfaceRender.bindTexture("mts:textures/rendering/glass.png");
		return 0;
	}
	
	@Override
	public void doPostRenderLogic(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		//Un-bind the glass texture.
		InterfaceRender.recallTexture();
	}
}
