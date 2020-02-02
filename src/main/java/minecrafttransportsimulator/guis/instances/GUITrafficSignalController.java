package minecrafttransportsimulator.guis.instances;

import java.awt.Color;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityTrafficSignalController;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.packets.tileentities.PacketTrafficSignalControllerChange;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class GUITrafficSignalController extends AGUIBase{
	
	//Buttons.
	private GUIComponentButton orientationButton;
	private GUIComponentButton modeButton;
	private GUIComponentButton confirmButton;
	
	//Input boxes
	private GUIComponentTextBox scanDistanceText;
	private GUIComponentTextBox greenMainTimeText;
	private GUIComponentTextBox greenCrossTimeText;
	private GUIComponentTextBox yellowTimeText;
	private GUIComponentTextBox allRedTimeText;
	
	//Labels
	private GUIComponentLabel trafficSignalCount;
	private GUIComponentLabel crossingSignalCount;
	
	private final TileEntityTrafficSignalController signalController;
	
	public GUITrafficSignalController(TileEntityTrafficSignalController clicked){
		this.signalController = clicked;
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		addButton(new GUIComponentButton(guiLeft + 25, guiTop + 15, 200, WrapperGUI.translate("gui.trafficsignalcontroller.scan")){
			public void onClicked(){
				signalController.trafficSignalLocations.clear();
				int scanDistance = Integer.valueOf(scanDistanceText.getText());
				for(int i=signalController.getPos().getX()-scanDistance; i<=signalController.getPos().getX()+scanDistance; ++i){
					for(int j=signalController.getPos().getY()-scanDistance; j<=signalController.getPos().getY()+scanDistance; ++j){
						for(int k=signalController.getPos().getZ()-scanDistance; k<=signalController.getPos().getZ()+scanDistance; ++k){
							BlockPos pos = new BlockPos(i, j, k);
							Block block = signalController.getWorld().getBlockState(pos).getBlock();
							if(block.equals(MTSRegistry.trafficSignal)){
								signalController.trafficSignalLocations.add(pos);
							}else if(block.equals(MTSRegistry.crossingSignal)){
								signalController.trafficSignalLocations.add(pos);
								signalController.crossingSignalLocations.add(pos);
							}
						}
					}
				}
			}
		});
		addTextBox(scanDistanceText = new GUIComponentTextBox(guiLeft + 120, guiTop + 40, 40, "25", 10, Color.WHITE, Color.BLACK, 2));
		addLabel(new GUIComponentLabel(guiLeft + 30, scanDistanceText.y, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.scandistance")).setBox(scanDistanceText));
		
		addLabel(new GUIComponentLabel(guiLeft + 30, guiTop + 55, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.scanfound")));
		addItem(new GUIComponentItem(guiLeft + 120, guiTop + 50, 1.0F, "mts:trafficsignal", 1, -1));
		addLabel(trafficSignalCount = new GUIComponentLabel(guiLeft + 135, guiTop + 55, Color.WHITE, " X " + String.valueOf(signalController.trafficSignalLocations.size() - signalController.crossingSignalLocations.size())));
		addItem(new GUIComponentItem(guiLeft + 170, guiTop + 50, 1.0F, "mts:crossingsignal", 1, -1));
		addLabel(crossingSignalCount = new GUIComponentLabel(guiLeft + 185, guiTop + 55, Color.WHITE, " X " + String.valueOf(signalController.crossingSignalLocations.size())));
		
		addButton(orientationButton = new GUIComponentButton(guiLeft + 125, guiTop + 70, 100, signalController.orientedOnX ? "X" : "Z"){
			@Override
			public void onClicked(){
				if(signalController.orientedOnX){
					signalController.orientedOnX = false;
					this.text = "Z";
				}else{
					signalController.orientedOnX = true;
					this.text = "X";
				}
			}
		});
		addLabel(new GUIComponentLabel(guiLeft + 30, orientationButton.y + 5, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.primary")).setButton(orientationButton));
		
		addButton(modeButton = new GUIComponentButton(guiLeft + 125, guiTop + 90, 100, WrapperGUI.translate("gui.trafficsignalcontroller." + (signalController.triggerMode ? "modetrigger" : "modetime"))){
			@Override
			public void onClicked(){
				if(signalController.triggerMode){
					signalController.triggerMode = false;
					this.text = WrapperGUI.translate("gui.trafficsignalcontroller.modetime");
				}else{
					signalController.triggerMode = true;
					this.text = WrapperGUI.translate("gui.trafficsignalcontroller.modetrigger");
				}
			}
		});
		addLabel(new GUIComponentLabel(guiLeft + 30, modeButton.y + 5, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.signalmode")).setButton(modeButton));
		
		addTextBox(greenMainTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 115, 40, String.valueOf(signalController.greenMainTime), 10, Color.WHITE, Color.BLACK, 3));
		addLabel(new GUIComponentLabel(guiLeft + 30, greenMainTimeText.y, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.greenmaintime")).setBox(greenMainTimeText));
		
		addTextBox(greenCrossTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 125, 40, String.valueOf(signalController.greenCrossTime), 10, Color.WHITE, Color.BLACK, 3));
		addLabel(new GUIComponentLabel(guiLeft + 30, greenCrossTimeText.y, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.greencrosstime")).setBox(greenCrossTimeText));
		
		addTextBox(yellowTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 135, 40, String.valueOf(signalController.yellowTime), 10, Color.WHITE, Color.BLACK, 1));
		addLabel(new GUIComponentLabel(guiLeft + 30, yellowTimeText.y, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.yellowtime")).setBox(yellowTimeText));
		
		addTextBox(allRedTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 145, 40, String.valueOf(signalController.allRedTime), 10, Color.WHITE, Color.BLACK, 1));
		addLabel(new GUIComponentLabel(guiLeft + 30, allRedTimeText.y, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.allredtime")).setBox(allRedTimeText));
		
		addButton(confirmButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 80, WrapperGUI.translate("gui.trafficsignalcontroller.confirm")){
			@Override
			public void onClicked(){
				//Convert strings to ints and send update packet to server.
				//Catch bad text if we have any as we don't want that to crash the server.
				try{
					signalController.greenMainTime = Integer.valueOf(greenMainTimeText.getText());
					signalController.greenCrossTime = Integer.valueOf(greenCrossTimeText.getText());
					signalController.yellowTime = Integer.valueOf(yellowTimeText.getText());
					signalController.allRedTime = Integer.valueOf(allRedTimeText.getText());
				}catch(Exception e){
					return;
				}
				MTS.MTSNet.sendToServer(new PacketTrafficSignalControllerChange(signalController));
				WrapperGUI.closeGUI();
			}
		});
	}
	
	@Override
	public void setStates(){
		trafficSignalCount.text = " X " + String.valueOf(signalController.trafficSignalLocations.size() - signalController.crossingSignalLocations.size());
		crossingSignalCount.text = " X " + String.valueOf(signalController.crossingSignalLocations.size());
		if(!signalController.trafficSignalLocations.isEmpty()){
			orientationButton.enabled = true;
			modeButton.enabled = true;
			greenMainTimeText.enabled = true;
			greenCrossTimeText.enabled = true;
			yellowTimeText.enabled = true;
			allRedTimeText.enabled = true;
			confirmButton.enabled = true;
		}else{
			orientationButton.enabled = false;
			modeButton.enabled = false;
			greenMainTimeText.enabled = false;
			greenCrossTimeText.enabled = false;
			yellowTimeText.enabled = false;
			allRedTimeText.enabled = false;
			confirmButton.enabled = false;
		}
		greenMainTimeText.visible = !signalController.triggerMode;
	}
}
