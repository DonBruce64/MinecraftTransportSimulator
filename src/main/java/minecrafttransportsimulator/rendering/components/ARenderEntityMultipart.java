package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**Entity rendering class for muliparts.  
 *
 * @author don_bruce
 */
public abstract class ARenderEntityMultipart<RenderedEntity extends AEntityE_Multipart<?>> extends ARenderEntity<RenderedEntity>{
	
	@Override
	protected void renderBoundingBoxes(RenderedEntity entity, Point3d entityPositionDelta){
		super.renderBoundingBoxes(entity, entityPositionDelta);
		//Draw part center points.
		InterfaceRender.setColorState(ColorRGB.YELLOW);
		GL11.glBegin(GL11.GL_LINES);
		for(APart part : ((AEntityE_Multipart<?>) entity).parts){
			if(!part.isFake()){
				Point3d partCenterDelta = part.position.copy().subtract(entity.position).add(entityPositionDelta);
				GL11.glVertex3d(partCenterDelta.x, partCenterDelta.y - part.getHeight(), partCenterDelta.z);
				GL11.glVertex3d(partCenterDelta.x, partCenterDelta.y + part.getHeight(), partCenterDelta.z);
			}
		}
		GL11.glEnd();
		InterfaceRender.setColorState(ColorRGB.WHITE);
	}
}
