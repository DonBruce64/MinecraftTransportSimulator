package minecrafttransportsimulator.rendering.components;

import java.util.Map.Entry;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**This class represents a section of a model that uses a texture from an online source.
 * The only transform this applies is binding the PNG from the URL to the model prior to rendering.
 *
 * @author don_bruce
 */
public class TransformOnlineTexture<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	private final String objectName;
	
	public TransformOnlineTexture(String objectName){
		this.objectName = objectName;
	}
	
	@Override
	public boolean shouldRender(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		//Make sure the provider has a texture for us.
		for(JSONText text : entity.text.keySet()){
			if(text.fieldName.equals(objectName)){
				if(!entity.text.get(text).isEmpty()){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public double applyTransform(AnimationEntity entity, boolean blendingEnabled, float partialTicks, double offset){
		//Get the texture from the text objects of the entity.
		for(Entry<JSONText, String> textEntry : entity.text.entrySet()){
			if(textEntry.getKey().fieldName.equals(objectName)){
				if(!textEntry.getValue().isEmpty() && !textEntry.getValue().contains(" ")){
					String errorString = InterfaceRender.bindURLTexture(textEntry.getValue());
					if(errorString != null){
						textEntry.setValue(errorString);
					}
					return 0;
				}
			}
		}
		return 0;
	}
	
	@Override
	public void doPostRenderLogic(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		//Un-bind the URL texture.
		InterfaceRender.recallTexture();
	}
}
