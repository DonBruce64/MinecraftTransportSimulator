package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentOBJModel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketTileEntityDecorTextChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleTextChange;
import minecrafttransportsimulator.rendering.components.ITextProvider;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

public class GUITextEditor extends AGUIBase{
	//Buttons.
	private GUIComponentButton confirmButton;
	
	//Input boxes and index linking to 
	private final List<GUIComponentTextBox> textInputBoxes = new ArrayList<GUIComponentTextBox>();
	private final List<String> textInputFieldNames = new ArrayList<String>();
	
	//Provider clicked.
	private final ITextProvider provider;
	
	//Pole, axis clicked on pole, and label for sign.
	private final TileEntityPole pole;
	private final Axis axis;
	private final List<GUIComponentLabel> signTextLabels = new ArrayList<GUIComponentLabel>();
	
	public GUITextEditor(TileEntityPole pole, Axis axis){
		this.provider = null;
		this.pole = pole;
		this.axis = axis;
	}
	
	public GUITextEditor(ITextProvider provider){
		this.provider = provider;
		this.pole = null;
		this.axis = null;
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		List<JSONText> textObjects;
		List<String> textLines;
		textInputBoxes.clear();
		if(pole != null){
			TileEntityPole_Sign component = (TileEntityPole_Sign) pole.components.get(axis);
			
			//Add the render to render the sign.
			GUIComponentOBJModel modelRender = new GUIComponentOBJModel(guiLeft + getWidth()/2, guiTop + 160, 64.0F, false, false, false);
			addOBJModel(modelRender);
			modelRender.modelLocation = component.definition.getModelLocation();
			modelRender.textureLocation = component.definition.getTextureLocation(component.item.subName);
			
			//Set text and text objects.
			textObjects = new ArrayList<JSONText>();
			textLines = new ArrayList<String>();
			textObjects.addAll(component.getText().keySet());
			textLines.addAll(component.getText().values());
			
			//Add render-able labels for the sign object.
			signTextLabels.clear();
			for(byte i=0; i<textObjects.size(); ++i){
				JSONText textObject = textObjects.get(i);
				GUIComponentLabel label = new GUIComponentLabel(modelRender.x + (int) (textObject.pos.x*64F), modelRender.y - (int) (textObject.pos.y*64F), Color.decode(textObject.color), textLines.get(i), TextPosition.values()[textObject.renderPosition], textObject.wrapWidth, textObject.scale, textObject.autoScale){
					@Override
					public void renderText(){
						GL11.glPushMatrix();
						GL11.glTranslatef(x, y, 0);
						GL11.glScalef(64F/16F, 64F/16F, 64F/16F);
						InterfaceGUI.drawScaledText(text, 0, 0, color, renderMode, wrapWidth, scale, autoScale);
						GL11.glPopMatrix();
				    }
				};
				addLabel(label);
				signTextLabels.add(label);
			}
		}else{
			textObjects = new ArrayList<JSONText>();
			textLines = new ArrayList<String>();
			textObjects.addAll(provider.getText().keySet());
			textLines.addAll(provider.getText().values());
			
			//Add part text objects if we are a vehicle.
			if(provider instanceof EntityVehicleF_Physics){
				for(APart part : ((EntityVehicleF_Physics) provider).parts){
					textObjects.addAll(part.getText().keySet());
					textLines.addAll(part.getText().values());
				}
			}
		}
		
		//Add text box components for every text.  Paired with labels to render the text name above the boxes.
		//Don't add multiple boxes per text field, however.  Those use the same box.
		textInputFieldNames.clear();
		int currentOffset = 0;
		int boxWidth = pole == null ? 200 : 100;
		for(JSONText textObject : textObjects){
			if(!textInputFieldNames.contains(textObject.fieldName)){
				//No text box present for the field name.  Create a new one.
				GUIComponentLabel label = new GUIComponentLabel(guiLeft + 20, guiTop + 30 + currentOffset, Color.BLACK, textObject.fieldName);
				addLabel(label);
				int textRowsRequired = 1 + 5*textObject.maxLength/boxWidth;
				GUIComponentTextBox box = new GUIComponentTextBox(guiLeft + 20, label.y + 10, boxWidth, textLines.get(textObjects.indexOf(textObject)), 12*textRowsRequired, Color.WHITE, Color.BLACK, textObject.maxLength);
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
				//First copy all the appropriate text box text to a string list for sending out.
				List<String> packetTextLines = new ArrayList<String>();
				for(JSONText textObject : textObjects){
					packetTextLines.add(textInputBoxes.get(textInputFieldNames.indexOf(textObject.fieldName)).getText());
				}
				
				//Now send the appropriate packet.
				if(pole != null){
					InterfacePacket.sendToServer(new PacketTileEntityPoleChange(pole, axis, null, packetTextLines, false));
				}else if(provider instanceof EntityVehicleF_Physics){
					InterfacePacket.sendToServer(new PacketVehicleTextChange((EntityVehicleF_Physics) provider, packetTextLines));
				}else{
					InterfacePacket.sendToServer(new PacketTileEntityDecorTextChange((TileEntityDecor) provider, packetTextLines));
				}
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
