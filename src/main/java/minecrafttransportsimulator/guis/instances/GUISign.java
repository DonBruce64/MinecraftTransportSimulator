package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import mcinterface.BuilderGUI;
import mcinterface.InterfaceNetwork;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentOBJModel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;

public class GUISign extends AGUIBase{
	//Buttons.
	private GUIComponentButton confirmButton;
	
	//Sign model.
	private GUIComponentOBJModel modelRender;
	
	//Input boxes
	private final List<GUIComponentTextBox> signTextBoxes = new ArrayList<GUIComponentTextBox>();
	private final List<GUIComponentLabel> signTextLabels = new ArrayList<GUIComponentLabel>();
	
	//Pole and axis clicked on pole.
	private final TileEntityPole pole;
	private final Axis axis;
	
	public GUISign(TileEntityPole pole, Axis axis){
		this.pole = pole;
		this.axis = axis;
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		TileEntityPole_Sign sign = (TileEntityPole_Sign) pole.components.get(axis);
		
		//Add the render to render the sign.
		addOBJModel(modelRender = new GUIComponentOBJModel(guiLeft + 200, guiTop + 100, 64.0F, false, false, false));
		modelRender.modelDomain = sign.definition.packID;
		if(sign.definition.general.modelName != null){
			modelRender.modelLocation = "objmodels/poles/" + sign.definition.general.modelName + ".obj";
		}else{
			modelRender.modelLocation = "objmodels/poles/" + sign.definition.systemName + ".obj";
		}
		modelRender.textureDomain = sign.definition.packID;
		modelRender.textureLocation = "textures/poles/" + sign.definition.systemName + ".png";
		
		//Add text box components for every text.  Paired with labels to render text on the sign.
		List<JSONText> textObjects = pole.components.get(axis).definition.general.textObjects;
		for(byte i=0; i<textObjects.size(); ++i){
			JSONText textObject = textObjects.get(i);
			GUIComponentTextBox box = new GUIComponentTextBox(guiLeft + 20, guiTop + 54 + i*10, 100, sign.getTextLines().get(i), 10, Color.WHITE, Color.BLACK, textObject.maxLength);
			addTextBox(box);
			signTextBoxes.add(box);
			GUIComponentLabel label = new GUIComponentLabel(modelRender.x + (int) (textObject.pos[0]*64F), modelRender.y - (int) (textObject.pos[1]*64F), Color.decode(textObject.color), sign.getTextLines().get(i), textObject.scale*64F/16F, true, false, 0);
			addLabel(label);
			signTextLabels.add(label);
		}
		
		//Add the confirm button.
		addButton(confirmButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 80, BuilderGUI.translate("gui.trafficsignalcontroller.confirm")){
			@Override
			public void onClicked(){
				//Set sign text.
				List<String> textLines = new ArrayList<String>();
				for(GUIComponentTextBox box : signTextBoxes){
					textLines.add(box.getText());
				}
				InterfaceNetwork.sendToServer(new PacketTileEntityPoleChange(pole, axis, null, textLines, false));
				BuilderGUI.closeGUI();
			}
		});
	}
	
	@Override
	public void setStates(){
		confirmButton.enabled = true;
		for(byte i=0; i<signTextBoxes.size(); ++i){
			signTextLabels.get(i).text = signTextBoxes.get(i).getText();
		}
	}
}
