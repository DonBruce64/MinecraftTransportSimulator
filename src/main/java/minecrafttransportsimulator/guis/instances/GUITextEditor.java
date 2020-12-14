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
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketTileEntityDecorTextChange;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleTextChange;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

public class GUITextEditor extends AGUIBase{
	//Buttons.
	private GUIComponentButton confirmButton;
	
	//Input boxes and index linking to 
	private final List<GUIComponentTextBox> textInputBoxes = new ArrayList<GUIComponentTextBox>();
	
	//Pole, axis clicked on pole, and label for sign.
	private final TileEntityPole pole;
	private final Axis axis;
	private final List<GUIComponentLabel> signTextLabels = new ArrayList<GUIComponentLabel>();
	
	//Decor clicked.
	private final TileEntityDecor decor;
	
	//Clicked vehicle.
	private final EntityVehicleF_Physics vehicle;
	
	//List of indexes of text boxes.  Each index corresponds to a text box.
	private final List<Integer> textLineIndexes = new ArrayList<Integer>();
	
	public GUITextEditor(TileEntityPole pole, Axis axis){
		this.pole = pole;
		this.axis = axis;
		this.decor = null;
		this.vehicle = null;
	}
	
	public GUITextEditor(TileEntityDecor decor){
		this.pole = null;
		this.axis = null;
		this.decor = decor;
		this.vehicle = null;
	}
	
	public GUITextEditor(EntityVehicleF_Physics vehicle){
		this.pole = null;
		this.axis = null;
		this.decor = null;
		this.vehicle = vehicle;
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		List<JSONText> textObjects;
		List<String> textLines;
		textInputBoxes.clear();
		if(pole != null){
			TileEntityPole_Sign sign = (TileEntityPole_Sign) pole.components.get(axis);
			
			//Add the render to render the sign.
			GUIComponentOBJModel modelRender = new GUIComponentOBJModel(guiLeft + getWidth()/2, guiTop + 160, 64.0F, false, false, false);
			addOBJModel(modelRender);
			modelRender.modelLocation = PackResourceLoader.getPackResource(sign.definition, ResourceType.OBJ, sign.definition.general.modelName != null ? sign.definition.general.modelName : sign.definition.systemName);
			modelRender.textureLocation = PackResourceLoader.getPackResource(sign.definition, ResourceType.PNG, sign.definition.systemName);
			
			//Set text and text objects.
			textObjects = pole.components.get(axis).definition.general.textObjects;
			textLines = sign.getTextLines();
			
			//Add render-able labels for the sign object.
			signTextLabels.clear();
			for(byte i=0; i<textObjects.size(); ++i){
				JSONText textObject = textObjects.get(i);
				GUIComponentLabel label = new GUIComponentLabel(modelRender.x + (int) (textObject.pos.x*64F), modelRender.y - (int) (textObject.pos.y*64F), Color.decode(textObject.color), sign.getTextLines().get(i), TextPosition.values()[textObject.renderPosition], textObject.wrapWidth, textObject.scale, textObject.autoScale){
					@Override
					public void renderText(){
						GL11.glPushMatrix();
						GL11.glTranslatef(x, y, 0);
						GL11.glScalef(64F/16F, 64F/16F, 64F/16F);
						MasterLoader.guiInterface.drawScaledText(text, 0, 0, color, renderMode, wrapWidth, scale, autoScale);
						GL11.glPopMatrix();
				    }
				};
				addLabel(label);
				signTextLabels.add(label);
			}
		}else if(decor != null){
			//Create internal textObjects if we don't have them already.
			//If we do have them, just use them as-is.
			if(decor.definition.general.textObjects == null){
				//TODO this should probably be generic...
				textObjects = new ArrayList<JSONText>();
				JSONText text = new JSONText();
				text.fieldName = "Beacon Name";
				text.maxLength = 5;
				textObjects.add(text);
				text = new JSONText();
				text.fieldName = "Glide Slope (Deg)";
				text.maxLength = 5;
				textObjects.add(text);
				text = new JSONText();
				text.fieldName = "Bearing (Deg)";
				text.maxLength = 5;
				textObjects.add(text);
			}else{
				textObjects = decor.definition.general.textObjects;
			}
			
			//Set text lines.
			textLines = decor.getTextLines();
		}else{
			//Set text and text objects.
			textObjects = new ArrayList<JSONText>();
			textLines = new ArrayList<String>();
			//Add all text objects and lines for the main vehicle.
			if(vehicle.definition.rendering.textObjects != null){
				for(JSONText textObject : vehicle.definition.rendering.textObjects){
					textObjects.add(textObject);
					textLines.add(vehicle.textLines.get(textObjects.indexOf(textObject)));
				}
			}
			//Add all text objects and lines for all parts on the vehicle.
			int checkedTextObjects = textObjects.size();
			for(APart part : vehicle.parts){
				if(part.definition.rendering != null && part.definition.rendering.textObjects != null){
					for(JSONText textObject : part.definition.rendering.textObjects){
						textObjects.add(textObject);
						int objectIndex = textObjects.indexOf(textObject);
						textLines.add(part.textLines.get(objectIndex - checkedTextObjects));
					}
					checkedTextObjects += part.definition.rendering.textObjects.size();
				}
			}
		}
		
		//Add text box components for every text.  Paired with labels to render the text name above the boxes.
		//Don't add multiple boxes per text field, however.  Those use the same box.
		List<String> renderedFieldNames = new ArrayList<String>();
		textLineIndexes.clear();
		int currentOffset = 0;
		int boxWidth = vehicle != null ? 200 : 100;
		for(JSONText textObject : textObjects){
			if(!renderedFieldNames.contains(textObject.fieldName)){
				//No text box present for the field name.  Create a new one.
				GUIComponentLabel label = new GUIComponentLabel(guiLeft + 20, guiTop + 30 + currentOffset, Color.BLACK, textObject.fieldName);
				addLabel(label);
				int textRowsRequired = 1 + 5*textObject.maxLength/boxWidth;
				GUIComponentTextBox box = new GUIComponentTextBox(guiLeft + 20, label.y + 10, boxWidth, textLines.get(textObjects.indexOf(textObject)), 12*textRowsRequired, Color.WHITE, Color.BLACK, textObject.maxLength);
				addTextBox(box);
				//Set the text index to the current index.
				textLineIndexes.add(textInputBoxes.size());
				//Add the new field name.
				renderedFieldNames.add(textObject.fieldName);
				textInputBoxes.add(box);
				currentOffset += box.height + 12;
			}else{
				//Set the text line index to the index of the existing field name.
				textLineIndexes.add(renderedFieldNames.indexOf(textObject.fieldName));
			}
		}
		
		//Add the confirm button.
		addButton(confirmButton = new GUIComponentButton(guiLeft + 150, guiTop + 15, 80, MasterLoader.coreInterface.translate("gui.trafficsignalcontroller.confirm")){
			@Override
			public void onClicked(){
				if(pole != null){
					//Copy text from boxes to string list.
					List<String> textLines = new ArrayList<String>();
					for(GUIComponentTextBox box : textInputBoxes){
						textLines.add(box.getText());
					}
					MasterLoader.networkInterface.sendToServer(new PacketTileEntityPoleChange(pole, axis, null, textLines, false));
				}else if(decor != null){
					//Copy text from boxes to string list.
					List<String> textLines = new ArrayList<String>();
					for(GUIComponentTextBox box : textInputBoxes){
						textLines.add(box.getText());
					}
					MasterLoader.networkInterface.sendToServer(new PacketTileEntityDecorTextChange(decor, textLines));
				}else{
					//Copy text from boxes to string list.
					List<String> textLines = new ArrayList<String>();
					for(Integer textBoxIndex : textLineIndexes){
						textLines.add(textInputBoxes.get(textBoxIndex).getText());
					}
					MasterLoader.networkInterface.sendToServer(new PacketVehicleTextChange(vehicle, textLines));
				}
				MasterLoader.guiInterface.closeGUI();
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
