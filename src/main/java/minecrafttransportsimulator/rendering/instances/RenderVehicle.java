package minecrafttransportsimulator.rendering.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.guis.components.AGUIBase.TextPosition;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.rendering.components.ARenderEntityMultipart;
import minecrafttransportsimulator.sound.InterfaceSound;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Main render class for all vehicles.  Renders the vehicle, along with all parts.
 * As entities don't render above 255 well due to the new chunk visibility system, 
 * this code is called both from the regular render loop and manually from
 * the event-based last pass.  This pass is -1, and should allow both the regular
 * and blending operations to run.
 *
 * @author don_bruce
 */
public final class RenderVehicle extends ARenderEntityMultipart<EntityVehicleF_Physics>{	
	
	@Override
	public void renderAdditionalModels(EntityVehicleF_Physics vehicle, boolean blendingEnabled, float partialTicks){
		//Render holograms for missing parts.
		if(blendingEnabled){
			renderPartBoxes(vehicle);
		}
	}
	
	@Override
	protected void renderBoundingBoxes(EntityVehicleF_Physics vehicle, Point3d entityPositionDelta){
		super.renderBoundingBoxes(vehicle, entityPositionDelta);
		//Draw the ground bounding boxes.
		InterfaceRender.setColorState(0.0F, 0.0F, 1.0F, 1.0F);
		for(BoundingBox box : vehicle.groundDeviceCollective.getGroundBounds()){
			Point3d boxCenterDelta = box.globalCenter.copy().subtract(vehicle.position).add(entityPositionDelta);
			GL11.glTranslated(boxCenterDelta.x, boxCenterDelta.y, boxCenterDelta.z);
			RenderBoundingBox.renderWireframe(box);
			GL11.glTranslated(-boxCenterDelta.x, -boxCenterDelta.y, -boxCenterDelta.z);
		}
		InterfaceRender.setColorState(1.0F, 1.0F, 1.0F, 1.0F);
	}
	
	/**
	 *  Renders holographic part boxes when holding parts that can go on this vehicle.  This
	 *  needs to be rendered in pass 1 to do alpha blending.
	 */
	private static void renderPartBoxes(EntityVehicleF_Physics vehicle){
		//Disable lighting and texture rendering.
		InterfaceRender.setLightingState(false);
		InterfaceRender.setTextureState(false);
		
		//If we are holding a part, render the valid slots.
		//If we are holding a scanner, render all slots, but only render the looked-at one with items above it.
		WrapperPlayer player = InterfaceClient.getClientPlayer();
		AItemBase heldItem = player.getHeldItem();
		
		if(heldItem instanceof AItemPart){
			AItemPart heldPart = (AItemPart) heldItem;
			for(Entry<BoundingBox, JSONPartDefinition> partSlotEntry : vehicle.activePartSlotBoxes.entrySet()){
				boolean isHoldingPart = false;
				boolean isPartValid = false;
				
				if(heldPart.isPartValidForPackDef(partSlotEntry.getValue(), vehicle.subName, false)){
					isHoldingPart = true;
					if(heldPart.isPartValidForPackDef(partSlotEntry.getValue(), vehicle.subName, true)){
						isPartValid = true;
					}
				}
						
				if(isHoldingPart){
					BoundingBox partBox = partSlotEntry.getKey();
					GL11.glPushMatrix();
					GL11.glRotated(-vehicle.angles.z, 0, 0, 1);
					GL11.glRotated(-vehicle.angles.x, 1, 0, 0);
					GL11.glRotated(-vehicle.angles.y, 0, 1, 0);
					GL11.glTranslated(partBox.globalCenter.x - vehicle.position.x, partBox.globalCenter.y - vehicle.position.y, partBox.globalCenter.z - vehicle.position.z);
					if(isPartValid){
						InterfaceRender.setColorState(0, 1, 0, 0.5F);
						RenderBoundingBox.renderSolid(partBox);
					}else{
						InterfaceRender.setColorState(1, 0, 0, 0.5F);
						RenderBoundingBox.renderSolid(partBox);
					}
					GL11.glPopMatrix();
				}
			}
		}else if(heldItem instanceof ItemItem && ((ItemItem) heldItem).definition.item.type.equals(ItemComponentType.SCANNER)){
			Point3d playerEyes = player.getPosition().add(0, player.getEyeHeight(), 0);
			Point3d playerLookVector = playerEyes.copy().add(new Point3d(0, 0, 10).rotateFine(new Point3d(player.getPitch(), player.getHeadYaw(), 0)));
			BoundingBox highlightedBox = null;
			GL11.glPushMatrix();
			GL11.glRotated(-vehicle.angles.z, 0, 0, 1);
			GL11.glRotated(-vehicle.angles.x, 1, 0, 0);
			GL11.glRotated(-vehicle.angles.y, 0, 1, 0);
			for(Entry<BoundingBox, JSONPartDefinition> partSlotEntry : vehicle.allPartSlotBoxes.entrySet()){
				JSONPartDefinition placementDefinition = partSlotEntry.getValue();
				if(!vehicle.areDoorsBlocking(placementDefinition, player) && (placementDefinition.validSubNames == null || placementDefinition.validSubNames.contains(vehicle.subName))){
					InterfaceRender.setColorState(0, 0, 1, 0.5F);
					BoundingBox currentBox = partSlotEntry.getKey();
					GL11.glPushMatrix();
					GL11.glTranslated(currentBox.globalCenter.x - vehicle.position.x, currentBox.globalCenter.y - vehicle.position.y, currentBox.globalCenter.z - vehicle.position.z);
					RenderBoundingBox.renderSolid(currentBox);
					GL11.glPopMatrix();
					if(currentBox.getIntersectionPoint(playerEyes, playerLookVector) != null){
						if(highlightedBox == null || (currentBox.globalCenter.distanceTo(playerEyes) < highlightedBox.globalCenter.distanceTo(playerEyes))){
							highlightedBox = currentBox;
						}
					}
				}
			}
			GL11.glPopMatrix();
			
			if(highlightedBox != null){
				//Get the definition for this box.
				JSONPartDefinition packVehicleDef = vehicle.allPartSlotBoxes.get(highlightedBox);
				
				//Re-enable 2D texture before rendering item and text.
				InterfaceRender.setTextureState(true);
				
				//Get all parts that go to this boxes position.
				List<AItemPart> validParts = new ArrayList<AItemPart>();
				for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
					if(packItem instanceof AItemPart){
						AItemPart part = (AItemPart) packItem;
						if(part.isPartValidForPackDef(packVehicleDef, vehicle.subName, true)){
							validParts.add(part);
						}
					}
				}
				
				//Render the type, min/max, and customTypes info.
				//To do this, we first need to translate to the top-center of the bounding box.
				//We also rotate to face the player.
				GL11.glPushMatrix();
				GL11.glTranslated(highlightedBox.localCenter.x, highlightedBox.localCenter.y + highlightedBox.heightRadius, highlightedBox.localCenter.z);
				GL11.glRotated(player.getHeadYaw() - vehicle.angles.y, 0, 1, 0);
				
				//Rotate by 180 on the z-axis.  This changes the X and Y coords from GUI to world coords. 
				GL11.glRotated(180, 0, 0, 1);
				
				//Translate to the spot above where the item would render and render the standard text.
				GL11.glTranslated(0, -1.75F, 0);
				InterfaceGUI.drawScaledText("Types: " + packVehicleDef.types.toString(), null, 0, 0, Color.BLACK, TextPosition.CENTERED, 0, 1/64F, false);
				GL11.glTranslated(0, 0.15F, 0);
				InterfaceGUI.drawScaledText("Min/Max: " + String.valueOf(packVehicleDef.minValue) + "/" + String.valueOf(packVehicleDef.maxValue), null, 0, 0, Color.BLACK, TextPosition.CENTERED, 0, 1/64F, false);
				GL11.glTranslated(0, 0.15F, 0);
				if(packVehicleDef.customTypes != null){
					InterfaceGUI.drawScaledText("CustomTypes: " + packVehicleDef.customTypes.toString(), null, 0, 0, Color.BLACK, TextPosition.CENTERED, 0, 1/64F, false);
				}else{
					InterfaceGUI.drawScaledText("CustomTypes: None", null, 0, 0, Color.BLACK, TextPosition.CENTERED, 0, 1/64F, false);
				}
				GL11.glTranslated(0, 0.25F, 0);
				
				//If we have valid parts, render one of them.
				if(!validParts.isEmpty()){
					//Get current part to render based on the cycle.
					int cycle = player.isSneaking() ? 30 : 15;
					AItemPart partToRender = validParts.get((int) ((vehicle.world.getTick()/cycle)%validParts.size()));
					
					//If we are on the start of the cycle, beep.
					if(vehicle.world.getTick()%cycle == 0){
						InterfaceSound.playQuickSound(new SoundInstance(vehicle, MasterLoader.resourceDomain + ":scanner_beep"));
					}
					
					//Render the part's name.
					InterfaceGUI.drawScaledText(partToRender.getItemName(), null, 0, 0, Color.BLACK, TextPosition.CENTERED, 0, 1/64F, false);
					
					//Do translations to get to the center of where the item will render and render it.
					//Items also need to be offset by -150 units due to how MC does rendering.
					//Also need to translate to the center as items are rendered from the top-left corner.
					GL11.glTranslated(-0.5D, 0.25F, -150D/16D);
					InterfaceGUI.drawItem(partToRender.getNewStack(), 0, 0, 1F/16F);
				}
				GL11.glPopMatrix();
			}
		}
		
		//Re-enable lighting and texture rendering.
		InterfaceRender.setLightingState(true);
		InterfaceRender.setTextureState(true);
	}
}
