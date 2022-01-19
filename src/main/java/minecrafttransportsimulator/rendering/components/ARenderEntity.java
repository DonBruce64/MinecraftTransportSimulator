package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.baseclasses.Matrix4dPlus;
import minecrafttransportsimulator.baseclasses.Point3dPlus;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceRender;

/**Base Entity rendering class.  
 *
 * @author don_bruce
 */
public abstract class ARenderEntity<RenderedEntity extends AEntityC_Renderable>{
	private static final Matrix4dPlus interpolatedOrientationHolder = new Matrix4dPlus();
	private static final Matrix4dPlus translatedMatrix = new Matrix4dPlus();
	private static final Matrix4dPlus rotatedMatrix = new Matrix4dPlus();
	
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
			Point3dPlus entityPositionDelta = new Point3dPlus(entity.prevPosition);
			entityPositionDelta.interpolate(entity.position, (double)partialTicks);
			
			//Subtract the entity's position by the render entity position to get the delta for translating.
			entityPositionDelta.subtract(InterfaceClient.getRenderViewEntity().getRenderedPosition(partialTicks));
			
			//Get interpolated orientation.
			entity.getInterpolatedOrientation(interpolatedOrientationHolder, partialTicks);
			
			//Adjust position and orientation, if needed.
			adjustPositionOrientation(entity, entityPositionDelta, interpolatedOrientationHolder, partialTicks);
	       
	        //Set up lighting.
	        InterfaceRender.setLightingToPosition(entity.position);
	        
	        //Set up matrixes.
	        translatedMatrix.resetTransforms();
	        translatedMatrix.translate(entityPositionDelta);
			rotatedMatrix.set(translatedMatrix);
			rotatedMatrix.matrix(interpolatedOrientationHolder);
			rotatedMatrix.scale(entity.prevScale + (entity.scale - entity.prevScale)*partialTicks);
			
	        //Render the main model.
	        entity.world.endProfiling();
	        renderModel(entity, rotatedMatrix, blendingEnabled, partialTicks);
			
			//End rotation render matrix.
			//Render holoboxes.
			if(blendingEnabled){
				renderHolographicBoxes(entity, translatedMatrix);
			}
			
			//Render bounding boxes.
			if(!blendingEnabled && InterfaceRender.shouldRenderBoundingBoxes()){
				entity.world.beginProfiling("BoundingBoxes", true);
				renderBoundingBoxes(entity, translatedMatrix);
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
	 *  Called to do supplemental modifications to the position/orientation of the entity prior to rendering.
	 *  The passed-in position is where the code thinks the entity is: where you want to render
	 *  it may not be at this position/rotation.  Hence the ability to modify these parameters.
	 */
	protected void adjustPositionOrientation(RenderedEntity entity, Point3dPlus entityPositionDelta, Matrix4dPlus interpolatedOrientation, float partialTicks){}
	
	/**
	 *  Called to render the main model.  At this point the matrix state will be aligned
	 *  to the position and rotation of the entity relative to the player-camera.
	 */
	protected abstract void renderModel(RenderedEntity entity, Matrix4dPlus transform, boolean blendingEnabled, float partialTicks);
	
	/**
	 *  Called to render holdgraphic boxes.  These shouldn't rotate with the model, so rotation is not present here.
	 *  However, at this point the transforms will be set to the entity position, as it is assumed everything will
	 *  at least be relative to it.
	 *  Also, this method is only called when blending is enabled, because holographic stuff ain't solid.
	 */
	protected void renderHolographicBoxes(RenderedEntity entity, Matrix4dPlus transform){};
	
	/**
	 *  Renders the bounding boxes for the entity, if any are present.
	 *  At this point, the translation and rotation done for the rendering 
	 *  will be un-done, as boxes need to be rendered according to their world state.
	 *  The passed-in delta is the delta between the player and the entity.
	 */
	protected abstract void renderBoundingBoxes(RenderedEntity entity, Matrix4dPlus transform);
}
