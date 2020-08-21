package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import mcinterface.InterfaceRender;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**This class represents a window object of a model.  The only transform this applies is binding
 * the glass texture prior to rendering, and rendering the window inner parts if set in the config.
 *
 * @author don_bruce
 */
public class TransformWindow extends ATransformRenderable{
	private final Float[][] vertices;
	
	public TransformWindow(Float[][] vertices){
		super(null);
		this.vertices = vertices;
	}
	
	@Override
	public boolean shouldRender(){
		return ConfigSystem.configObject.client.renderWindows.value;
	}

	@Override
	public double applyTransform(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks, double offset){
		//FIXME add window textur ehere.
		InterfaceRender.bindTexture("minecraft", "textures/blocks/glass.png");
		return 0;
	}
	
	@Override
	public void doPostRenderLogic(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks){
		//Render inner windows, if set.
		if(ConfigSystem.configObject.client.innerWindows.value){
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(int j=vertices.length - 1; j>=0; --j){
				GL11.glTexCoord2f(vertices[j][3], vertices[j][4]);
				GL11.glNormal3f(vertices[j][5], vertices[j][6], vertices[j][7]);
				GL11.glVertex3f(vertices[j][0], vertices[j][1], vertices[j][2]);
			}
			GL11.glEnd();
		}
		
		//Un-bind the glass texture.
		InterfaceRender.recallTexture();
	}
}
