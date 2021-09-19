package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponent3DModel;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityTextChange;
import minecrafttransportsimulator.rendering.instances.RenderText.TextAlignment;

public class GUITextEditor extends AGUIBase{
	//Buttons.
	private GUIComponentButton confirmButton;
	
	//Input boxes and their field names.
	private final List<GUIComponentTextBox> textInputBoxes = new ArrayList<GUIComponentTextBox>();
	private final List<String> textInputFieldNames = new ArrayList<String>();
	
	//Entity clicked.
	private final AEntityC_Definable<?> entity;
	
	//Labels for sign.  These do fancy rendering.
	private final List<GUIComponentLabel> signTextLabels = new ArrayList<GUIComponentLabel>();
	
	public GUITextEditor(AEntityC_Definable<?> entity){
		this.entity = entity;
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		int boxWidth;
		List<JSONText> textObjects;
		List<String> textLines;
		textInputBoxes.clear();
		if(entity instanceof TileEntityPole_Sign){
			//Add the render to render the sign.
			GUIComponent3DModel modelRender = new GUIComponent3DModel(guiLeft + getWidth()/2, guiTop + 160, 64.0F, false, false, false);
			addOBJModel(modelRender);
			modelRender.modelLocation = entity.definition.getModelLocation(entity.subName);
			modelRender.textureLocation = entity.definition.getTextureLocation(entity.subName);
			
			//Set text and text objects.
			boxWidth = 100;
			textObjects = new ArrayList<JSONText>();
			textLines = new ArrayList<String>();
			textObjects.addAll(entity.text.keySet());
			textLines.addAll(entity.text.values());
			
			//Add render-able labels for the sign object.
			signTextLabels.clear();
			for(byte i=0; i<textObjects.size(); ++i){
				JSONText textDef = textObjects.get(i);
				GUIComponentLabel label = new GUIComponentLabel(modelRender.x + (int) (textDef.pos.x*64F), modelRender.y - (int) (textDef.pos.y*64F), textDef.color, textLines.get(i), TextAlignment.values()[textDef.renderPosition], textDef.scale*64F/16F, textDef.wrapWidth, textDef.fontName);
				addLabel(label);
				signTextLabels.add(label);
			}
		}else{
			boxWidth = 200;
			textObjects = new ArrayList<JSONText>();
			textLines = new ArrayList<String>();
			textObjects.addAll(entity.text.keySet());
			textLines.addAll(entity.text.values());
			
			//Add part text objects if we are a multipart.
			if(entity instanceof AEntityE_Multipart){
				for(APart part : ((AEntityE_Multipart<?>) entity).parts){
					textObjects.addAll(part.text.keySet());
					textLines.addAll(part.text.values());
				}
			}
		}
		
		//Add text box components for every text.  Paired with labels to render the text name above the boxes.
		//Don't add multiple boxes per text field, however.  Those use the same box.
		textInputFieldNames.clear();
		int currentOffset = 0;
		for(JSONText textObject : textObjects){
			if(!textInputFieldNames.contains(textObject.fieldName)){
				//No text box present for the field name.  Create a new one.
				GUIComponentLabel label = new GUIComponentLabel(guiLeft + 20, guiTop + 30 + currentOffset, ColorRGB.BLACK, textObject.fieldName);
				addLabel(label);
				int textRowsRequired = 1 + 5*textObject.maxLength/boxWidth;
				GUIComponentTextBox box = new GUIComponentTextBox(guiLeft + 20, label.y + 10, boxWidth, textLines.get(textObjects.indexOf(textObject)), 12*textRowsRequired, ColorRGB.WHITE, ColorRGB.BLACK, textObject.maxLength);
				addTextBox(box);
				textInputBoxes.add(box);
				currentOffset += box.height + 12;
				textInputFieldNames.add(textObject.fieldName);
			}
		}
		
		//Add the confirm button.
		addButton(confirmButton = new GUIComponentButton(guiLeft + 150, guiTop + 15, 80, InterfaceCore.translate("gui.trafficsignalcontroller.confirm")){
			@Override
			public void onClicked(){
				List<String> packetTextLines = new ArrayList<String>();
				for(JSONText textObject : textObjects){
					packetTextLines.add(textInputBoxes.get(textInputFieldNames.indexOf(textObject.fieldName)).getText());
				}
				InterfacePacket.sendToServer(new PacketEntityTextChange(entity, packetTextLines));
				InterfaceGUI.closeGUI();
			}
		});
	}
	
	@Override
	public void setStates(){
		confirmButton.enabled = true;
		for(int i=0; i<signTextLabels.size(); ++i){
			signTextLabels.get(i).text = textInputBoxes.get(i).getText();
		}
	}
}
