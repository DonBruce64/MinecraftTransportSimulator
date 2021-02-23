package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.mcinterface.InterfaceClient;

/**Base Entity rendering class.  
 *
 * @author don_bruce
 */
public abstract class ARenderEntity<RenderedEntity extends AEntityC_Definable<?>>{
	
	/**
	 *  Called to render this entity.  This is the setup method that sets states to the appropriate values.
	 *  After this, the main model rendering method is called.
	 */
	public final void render(RenderedEntity entity, float partialTicks){
		//Get render pass.  Render data uses 2 for pass -1 as it uses arrays and arrays can't have a -1 index.
		int renderPass = InterfaceRender.getRenderPass();
		if(renderPass == -1){
			renderPass = 2;
		}
		
		//If we need to render, do so now.
		if(entity.renderData.shouldRender(renderPass, partialTicks)){
			//Get the render offset.
			//This is the interpolated movement, plus the prior position.
			Point3d entityPosition = entity.prevPosition.getInterpolatedPoint(entity.position, partialTicks);
			
			//Subtract the entity's position by the render entity position to get the delta for translating.
			entityPosition.subtract(InterfaceClient.getRenderViewEntity().getRenderedPosition(partialTicks));
			
			//Get the entity rotation.
			Point3d entityRotation = entity.prevAngles.getInterpolatedPoint(entity.angles, partialTicks);
	       
	        //Set up lighting.
	        InterfaceRender.setLightingToPosition(entity.position);
	        
	        //Use smooth shading for main model rendering.
			GL11.glShadeModel(GL11.GL_SMOOTH);
	        
	        //Push the matrix on the stack and translate and rotate to the enitty's position.
			adjustPositionRotation(entity, entityPosition, entityRotation);
	        GL11.glPushMatrix();
	        GL11.glTranslated(entityPosition.x, entityPosition.y, entityPosition.z);
	        GL11.glRotated(entityRotation.y, 0, 1, 0);
	        GL11.glRotated(entityRotation.x, 1, 0, 0);
	        GL11.glRotated(entityRotation.z, 0, 0, 1);
			
	        //Render the main model.
	        renderModel(entity, partialTicks);
			
			//End render matrix and reset states.
			GL11.glShadeModel(GL11.GL_FLAT);
			GL11.glPopMatrix();
			InterfaceRender.resetStates();
			
			//Spawn particles, if we aren't paused and this is the main render pass.
			if(InterfaceRender.getRenderPass() != 1 && !InterfaceClient.isGamePaused()){
				entity.spawnParticles();
			}
			
			//Render parts, if we are they type of entity to have them.
			if(entity instanceof AEntityE_Multipart){
				for(APart part : ((AEntityE_Multipart<?>) entity).parts){
					part.getRenderer().render(part, partialTicks);
				}
			}
		}
	}
	
	/**
	 *  Called to do supplemental modifications to the position and rotation of the entity prior to rendering.
	 *  The passed-in position and rotation are where the code thinks the entity is: where you want to render
	 *  it may not be at this position/rotation.  Hence the ability to modify these parameters.
	 */
	public void adjustPositionRotation(RenderedEntity entity, Point3d entityPosition, Point3d entityRotation){}
	
	/**
	 *  Called to render this entity.  The currently-bound texture is undefined, so you will need
	 *  to bind whichever texture you see fit to do so.  This can be done via {@link InterfaceRender#bindTexture(String, String)}
	 *  Rendering happens normally in pass 0 (solid) and 1 (transparent), but may happen in the special pass -1 (end) if the entity
	 *  wasn't rendered in either pass 0 or 1 due to chunk render culling.  Some rendering routines only run on specific passes, 
	 *  so see the comments on the called methods for information on what is rendered when.
	 */
	public abstract void renderModel(RenderedEntity entity, float partialTicks);
}
