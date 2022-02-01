package minecrafttransportsimulator.rendering.instances;

import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.rendering.components.ARenderEntityDefinable;
import minecrafttransportsimulator.rendering.components.RenderableObject;

/**Main render class for all vehicles.  Renders the vehicle, along with all parts.
 * As entities don't render above 255 well due to the new chunk visibility system, 
 * this code is called both from the regular render loop and manually from
 * the event-based last pass.  This pass is -1, and should allow both the regular
 * and blending operations to run.
 *
 * @author don_bruce
 */
public class RenderVehicle extends ARenderEntityDefinable<EntityVehicleF_Physics>{	
	
	@Override
	protected void renderModel(EntityVehicleF_Physics vehicle, boolean blendingEnabled, float partialTicks){
		super.renderModel(vehicle, blendingEnabled, partialTicks);
		
		//Render holograms for missing parts.
		vehicle.world.beginProfiling("PartHoloboxes", true);
		if(blendingEnabled){
			//If we are holding a part, render the valid slots.
			//If we are holding a scanner, render all slots.
			WrapperPlayer player = InterfaceClient.getClientPlayer();
			AItemBase heldItem = player.getHeldItem();
			AItemPart heldPart = heldItem instanceof AItemPart ? (AItemPart) heldItem : null;
			boolean holdingScanner = player.isHoldingItemType(ItemComponentType.SCANNER);
			if(heldPart != null || holdingScanner){
				//Undo rotation for bounding boxes.
				GL11.glPushMatrix();
				GL11.glRotated(-vehicle.angles.z, 0, 0, 1);
				GL11.glRotated(-vehicle.angles.x, 1, 0, 0);
				GL11.glRotated(-vehicle.angles.y, 0, 1, 0);
				if(holdingScanner){
					for(Entry<BoundingBox, JSONPartDefinition> partSlotEntry : vehicle.allPartSlotBoxes.entrySet()){
						BoundingBox box = partSlotEntry.getKey();
						JSONPartDefinition placementDefinition = partSlotEntry.getValue();
						if(!vehicle.areVariablesBlocking(placementDefinition, player) && (placementDefinition.validSubNames == null || placementDefinition.validSubNames.contains(vehicle.subName))){
							RenderableObject renderable = vehicle.allPartSlotRenderables.get(box);
							GL11.glPushMatrix();
							GL11.glTranslated(box.globalCenter.x - vehicle.position.x, box.globalCenter.y - vehicle.position.y, box.globalCenter.z - vehicle.position.z);
							
							//Need to re-add vertices in case the part box size changed from the held item.
							//Also need to change the color to blue, as it might not be blue if we did part checks before.
							renderable.setHolographicBoundingBox(box);
							renderable.color.setTo(ColorRGB.BLUE);
							renderable.render();
							GL11.glPopMatrix();
						}
					}
				}else{
					for(Entry<BoundingBox, JSONPartDefinition> partSlotEntry : vehicle.activePartSlotBoxes.entrySet()){
						boolean isHoldingCorrectTypePart = false;
						boolean isHoldingCorrectParamPart = false;
						
						if(heldPart.isPartValidForPackDef(partSlotEntry.getValue(), vehicle.subName, false)){
							isHoldingCorrectTypePart = true;
							if(heldPart.isPartValidForPackDef(partSlotEntry.getValue(), vehicle.subName, true)){
								isHoldingCorrectParamPart = true;
							}
						}
								
						if(isHoldingCorrectTypePart){
							BoundingBox box = partSlotEntry.getKey();
							RenderableObject renderable = vehicle.allPartSlotRenderables.get(box);
							
							//Renderable may be null if we don't have this slot anymore.
							if(renderable != null){
								GL11.glPushMatrix();
								GL11.glTranslated(box.globalCenter.x - vehicle.position.x, box.globalCenter.y - vehicle.position.y, box.globalCenter.z - vehicle.position.z);
								
								//Need to re-add vertices in case the part box size changed from the held item.
								//Also need to change the color to match held state.
								renderable.setHolographicBoundingBox(box);
								renderable.color.setTo(isHoldingCorrectParamPart ? ColorRGB.GREEN : ColorRGB.RED);
								renderable.render();
								GL11.glPopMatrix();
							}
						}
					}
				}
				GL11.glPopMatrix();
			}
		}
		vehicle.world.endProfiling();
	}
	
	@Override
	protected void renderBoundingBoxes(EntityVehicleF_Physics vehicle, Point3d entityPositionDelta){
		super.renderBoundingBoxes(vehicle, entityPositionDelta);
		//Draw the ground bounding boxes.
		for(BoundingBox box : vehicle.groundDeviceCollective.getGroundBounds()){
			Point3d boxCenterDelta = box.globalCenter.copy().subtract(vehicle.position).add(entityPositionDelta);
			GL11.glTranslated(boxCenterDelta.x, boxCenterDelta.y, boxCenterDelta.z);
			
			//Need to re-add vertices in case the wheels changed since creation of the bounding box.
			//Also need to change the color to blue.
			box.renderable.color.setTo(ColorRGB.BLUE);
			box.renderable.setWireframeBoundingBox(box);
			box.renderable.render();
			GL11.glTranslated(-boxCenterDelta.x, -boxCenterDelta.y, -boxCenterDelta.z);
		}
	}
}
