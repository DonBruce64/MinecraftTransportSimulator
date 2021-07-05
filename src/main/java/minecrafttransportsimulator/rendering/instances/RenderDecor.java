package minecrafttransportsimulator.rendering.instances;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.SignalDirection;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.SignalGroup;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;

public class RenderDecor extends ARenderTileEntityBase<TileEntityDecor>{
	
	@Override
	public void renderAdditionalModels(TileEntityDecor decor, boolean blendingEnabled, float partialTicks){
		//Render lane holo-boxes if we are a signal controller that's being edited.
		if(blendingEnabled && decor instanceof TileEntitySignalController){
			TileEntitySignalController controller = (TileEntitySignalController) decor;
			if(controller.unsavedClientChangesPreset || InterfaceRender.shouldRenderBoundingBoxes()){
				//Disable lighting and texture rendering.
				InterfaceRender.setLightingState(false);
				InterfaceRender.setTextureState(false);
				
				for(SignalGroup signalGroup : controller.signalGroups){ 
					if(signalGroup.signalLineWidth != 0 && !signalGroup.controlledSignals.isEmpty()){
						
						BoundingBox box = new BoundingBox(signalGroup.signalLineCenter.copy().add(0, 0, 8).rotateY(signalGroup.axis.yRotation).add(controller.intersectionCenterPoint).subtract(controller.position), signalGroup.signalLineWidth/2D, 2, 8);
						GL11.glPushMatrix();
						GL11.glRotated(-controller.angles.y, 0, 1, 0);
						GL11.glTranslated(box.globalCenter.x, box.globalCenter.y, box.globalCenter.z);
						GL11.glRotated(signalGroup.axis.yRotation, 0, 1, 0);
						
						
						if(signalGroup.direction.equals(SignalDirection.LEFT)){
							InterfaceRender.setColorState(0, 0, 1, 0.5F);
							RenderBoundingBox.renderSolid(box);
						}else if(signalGroup.direction.equals(SignalDirection.RIGHT)){
							InterfaceRender.setColorState(1, 1, 0, 0.5F);
							RenderBoundingBox.renderSolid(box);
						}else if(signalGroup.direction.equals(SignalDirection.CENTER)){
							InterfaceRender.setColorState(0, 1, 0, 0.5F);
							RenderBoundingBox.renderSolid(box);
						}
						GL11.glPopMatrix();
					}
				}
				
				//Re-enable lighting and texture rendering.
				InterfaceRender.setLightingState(true);
				InterfaceRender.setTextureState(true);
			}
		}
	}
}
