package minecrafttransportsimulator.rendering.components;

import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.jsondefs.JSONText;

/**This class represents a section of a model that uses a texture from an online source.
 * The only transform this applies is binding the PNG from the URL to the model prior to rendering.
 *
 * @author don_bruce
 */
public class TransformOnlineTexture extends ATransform{
	private final String objectName;
	
	public TransformOnlineTexture(String objectName){
		super(null);
		this.objectName = objectName;
	}
	
	@Override
	public boolean shouldRender(IAnimationProvider provider, float partialTicks){
		//Make sure the provider has a texture for us.
		Map<JSONText, String> textLines = ((ITextProvider) provider).getText();
		for(JSONText text : ((ITextProvider) provider).getText().keySet()){
			if(text.fieldName.equals(objectName)){
				if(!textLines.get(text).isEmpty()){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public double applyTransform(IAnimationProvider provider, float partialTicks, double offset){
		if(InterfaceRender.getRenderPass() != 1){
			//Get the texture from the text objects of the provider.
			for(Entry<JSONText, String> textEntry : ((ITextProvider) provider).getText().entrySet()){
				if(textEntry.getKey().fieldName.equals(objectName)){
					if(!textEntry.getValue().isEmpty()){
						String errorString = InterfaceRender.bindURLTexture(textEntry.getValue());
						if(errorString != null){
							textEntry.setValue(errorString);
						}
						return 0;
					}
				}
			}
		}
		return 0;
	}
	
	@Override
	public void doPostRenderLogic(IAnimationProvider provider, float partialTicks){
		if(InterfaceRender.getRenderPass() != 1){
			//Un-bind the URL texture.
			InterfaceRender.recallTexture();
		}
	}
}
