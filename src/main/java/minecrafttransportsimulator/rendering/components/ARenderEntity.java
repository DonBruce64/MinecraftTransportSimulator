package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**Base Entity rendering class.  
 *
 * @author don_bruce
 */
public abstract class ARenderEntity<RenderedEntity extends AEntityC_Renderable>{
	
	/**
	 *  Called to render this entity.  This is the setup method that sets states to the appropriate values.
	 *  After this, the main model rendering method is called.
	 */
	public final void render(RenderedEntity entity, boolean blendingEnabled, float partialTicks){
		//If we need to render, do so now.
		entity.world.beginProfiling("RenderSetup", true);
		if(!disableRendering(entity, partialTicks)){
			//Get the render offset.
			//This is the interpolated movement, plus the prior position.
			Point3d entityPositionDelta = entity.prevPosition.getInterpolatedPoint(entity.position, partialTicks);
			
			//Subtract the entity's position by the render entity position to get the delta for translating.
			entityPositionDelta.subtract(InterfaceClient.getRenderViewEntity().getRenderedPosition(partialTicks));
			
			//Get the entity rotation.
			Point3d entityRotation = entity.prevAngles.getInterpolatedPoint(entity.angles, partialTicks);
	       
	        //Set up lighting.
	        InterfaceRender.setLightingToPosition(entity.position);
	        
	        //Push the matrix on the stack and translate and rotate to the enitty's position.
			adjustPositionRotation(entity, entityPositionDelta, entityRotation, partialTicks);
			GL11.glPushMatrix();
	        GL11.glTranslated(entityPositionDelta.x, entityPositionDelta.y, entityPositionDelta.z);
	        GL11.glRotated(entityRotation.y, 0, 1, 0);
	        GL11.glRotated(entityRotation.x, 1, 0, 0);
	        GL11.glRotated(entityRotation.z, 0, 0, 1);
			
	        //Render the main model.
	        entity.world.endProfiling();
	        renderModel(entity, blendingEnabled, partialTicks);
			
			//End rotation render matrix.
			GL11.glPopMatrix();
			
			//Render bounding boxes.
			if(!blendingEnabled && InterfaceRender.shouldRenderBoundingBoxes()){
				entity.world.beginProfiling("BoundingBoxes", true);
				renderBoundingBoxes(entity, entityPositionDelta);
				 entity.world.endProfiling();
			}
			
			//Handle sounds.  These will be partial-tick only ones.
			//Normal sounds are handled on the main tick loop.
			entity.world.beginProfiling("Sounds", true);
			entity.updateSounds(partialTicks);
		}
		entity.world.endProfiling();
	}
	
	/**
	 *  If rendering needs to be skipped for any reason, return true here.
	 */
	protected boolean disableRendering(RenderedEntity entity, float partialTicks){
		//Don't render on the first tick, as we might have not created some variables yet.
		return entity.ticksExisted == 0;
	}
	
	/**
	 *  Called to do supplemental modifications to the position and rotation of the entity prior to rendering.
	 *  The passed-in position and rotation are where the code thinks the entity is: where you want to render
	 *  it may not be at this position/rotation.  Hence the ability to modify these parameters.
	 */
	protected void adjustPositionRotation(RenderedEntity entity, Point3d entityPositionDelta, Point3d entityRotationDelta, float partialTicks){}
	
	/**
	 *  Called to render the main model.  At this point the matrix state will be aligned
	 *  to the position and rotation of the entity relative to the player-camera.
	 */
	protected abstract void renderModel(RenderedEntity entity, boolean blendingEnabled, float partialTicks);
	
	/**
	 *  Renders the bounding boxes for the entity, if any are present.
	 *  At this point, the translation and rotation done for the rendering 
	 *  will be un-done, as boxes need to be rendered according to their world state.
	 *  The passed-in delta is the delta between the player and the entity.
	 */
	protected abstract void renderBoundingBoxes(RenderedEntity entity, Point3d entityPositionDelta);
}
