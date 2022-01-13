package minecrafttransportsimulator.rendering.instances;

import java.util.Set;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.SignalGroup;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.rendering.components.ARenderTileEntityBase;

public class RenderDecor extends ARenderTileEntityBase<TileEntityDecor>{
	
	@Override
	protected void renderModel(TileEntityDecor decor, boolean blendingEnabled, float partialTicks){
		super.renderModel(decor, blendingEnabled, partialTicks);
		
		//Render lane holo-boxes if we are a signal controller that's being edited.
		if(blendingEnabled && decor instanceof TileEntitySignalController){
			TileEntitySignalController controller = (TileEntitySignalController) decor;
			if(controller.unsavedClientChangesPreset || InterfaceRender.shouldRenderBoundingBoxes()){
				for(Set<SignalGroup> signalGroupSet : controller.signalGroups.values()){
		        	for(SignalGroup signalGroup : signalGroupSet){ 
						if(signalGroup.signalLineWidth != 0 && controller.intersectionProperties.get(signalGroup.axis).isActive){
							BoundingBox box = new BoundingBox(signalGroup.signalLineCenter.copy().add(0, 0, 8).rotateY(signalGroup.axis.yRotation).add(controller.intersectionCenterPoint).subtract(controller.position), signalGroup.signalLineWidth/2D, 2, 8);
							signalGroup.renderable.setHolographicBoundingBox(box);
							
							GL11.glPushMatrix();
							GL11.glRotated(-controller.angles.y, 0, 1, 0);
							GL11.glTranslated(box.globalCenter.x, box.globalCenter.y, box.globalCenter.z);
							GL11.glRotated(signalGroup.axis.yRotation, 0, 1, 0);
							signalGroup.renderable.render();
							GL11.glPopMatrix();
						}
		        	}
				}
			}
		}
	}
}
