package minecraftflightsimulator.planes.Trimotor;

import minecraftflightsimulator.entities.EntityPlane;
import minecraftflightsimulator.helpers.RenderHelper;
import minecraftflightsimulator.modelrenders.RenderPlane;
import minecraftflightsimulator.models.ModelPlane;

public class RenderTrimotor extends RenderPlane {

	public RenderTrimotor(){
		super(new ModelTrimotor());
	}

	@Override
	protected void renderWindows(){
		RenderHelper.bindTexture(ModelPlane.windowTexture);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
			
	}
}
