package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.systems.ConfigSystem;

/**This class represents a window object of a model.  The only transform this applies is binding
 * the glass texture prior to rendering, and rendering the window inner parts if set in the config.
 *
 * @author don_bruce
 */
public class TransformWindow<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	private final Float[][] vertices;
	
	public TransformWindow(Float[][] vertices){
		super(null);
		this.vertices = vertices;
	}
	
	@Override
	public boolean shouldRender(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		return ConfigSystem.configObject.clientRendering.renderWindows.value;
	}

	@Override
	public double applyTransform(AnimationEntity entity, boolean blendingEnabled, float partialTicks, double offset){
		InterfaceRender.bindTexture("mts:textures/rendering/glass.png");
		return 0;
	}
	
	@Override
	public void doPostRenderLogic(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		//Render inner windows, if set.
		if(ConfigSystem.configObject.clientRendering.innerWindows.value){
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
