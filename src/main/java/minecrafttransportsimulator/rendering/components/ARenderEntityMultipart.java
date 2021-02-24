package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;

/**Entity rendering class for muliparts.  
 *
 * @author don_bruce
 */
public abstract class ARenderEntityMultipart<RenderedEntity extends AEntityE_Multipart<?>> extends ARenderEntity<RenderedEntity>{
	
	@Override
	protected void renderSupplementalModels(RenderedEntity entity, float partialTicks){
		for(APart part : ((AEntityE_Multipart<?>) entity).parts){
			part.getRenderer().render(part, partialTicks);
		}
	}
	
	@Override
	public void clearObjectCaches(RenderedEntity entity){
		super.clearObjectCaches(entity);
		for(APart part : ((AEntityE_Multipart<?>) entity).parts){
			part.getRenderer().clearObjectCaches(part);
		}
	}
	
	@Override
	public boolean doesEntityHaveLight(RenderedEntity entity, LightType light){
		if(!super.doesEntityHaveLight(entity, light)){
			for(APart part : ((AEntityE_Multipart<?>) entity).parts){
				if(part.getRenderer().doesEntityHaveLight(part, light)){
					return true;
				}
			}
			return false;
		}else{
			return true;
		}
	}
}
