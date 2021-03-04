package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;

/**Entity rendering class for muliparts.  
 *
 * @author don_bruce
 */
public abstract class ARenderEntityMultipart<RenderedEntity extends AEntityE_Multipart<?>> extends ARenderEntity<RenderedEntity>{
	
	@Override
	protected void renderSupplementalModels(RenderedEntity entity, boolean blendingEnabled, float partialTicks){
		for(APart part : ((AEntityE_Multipart<?>) entity).parts){
			part.getRenderer().render(part, blendingEnabled, partialTicks);
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
	
	@Override
	protected void renderBoundingBoxes(RenderedEntity entity, Point3d entityPositionDelta){
		super.renderBoundingBoxes(entity, entityPositionDelta);
		//Draw part center points.
		InterfaceRender.setColorState(1.0F, 1.0F, 0.0F, 1.0F);
		GL11.glBegin(GL11.GL_LINES);
		for(APart part : ((AEntityE_Multipart<?>) entity).parts){
			if(!part.isFake()){
				Point3d partCenterDelta = part.position.copy().subtract(entity.position).add(entityPositionDelta);
				GL11.glVertex3d(partCenterDelta.x, partCenterDelta.y - part.getHeight(), partCenterDelta.z);
				GL11.glVertex3d(partCenterDelta.x, partCenterDelta.y + part.getHeight(), partCenterDelta.z);
			}
		}
		GL11.glEnd();
		InterfaceRender.setColorState(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
