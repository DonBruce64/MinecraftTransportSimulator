package minecraftflightsimulator.planes.Trimotor;

import minecraftflightsimulator.entities.EntityPlane;
import minecraftflightsimulator.modelrenders.ModelRenderHelper;
import minecraftflightsimulator.modelrenders.RenderPlane;
import minecraftflightsimulator.models.ModelPlane;

public class RenderTrimotor extends RenderPlane {

	public RenderTrimotor(){
		super(new ModelTrimotor());
	}

	@Override
	protected void renderWindows(){
		this.renderManager.renderEngine.bindTexture(ModelPlane.windowTexture);
    	ModelRenderHelper.startRender();
		
		ModelRenderHelper.endRender();
	}

	@Override
	protected void renderConsole(EntityPlane plane){
			
	}
}
