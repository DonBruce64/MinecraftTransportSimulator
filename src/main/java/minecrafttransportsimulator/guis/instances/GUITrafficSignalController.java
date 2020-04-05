package minecrafttransportsimulator.guis.instances;

import java.awt.Color;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockPoleCrossingSignal;
import minecrafttransportsimulator.blocks.instances.BlockPoleTrafficSignal;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityTrafficSignalController;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityTrafficSignalController.OpMode;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.packets.instances.PacketTrafficSignalControllerChange;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperNetwork;

public class GUITrafficSignalController extends AGUIBase{
	
	//Buttons.
	private GUIComponentButton orientationButton;
	private GUIComponentButton modeButton;
	private GUIComponentButton confirmButton;
	
	//Input boxes
	private GUIComponentTextBox scanDistanceText;
	private GUIComponentTextBox greenMainTimeText;
	private GUIComponentTextBox greenCrossTimeText;
	private GUIComponentTextBox yellowMainTimeText;
	private GUIComponentTextBox yellowCrossTimeText;
	private GUIComponentTextBox allRedTimeText;
	
	//Labels
	private GUIComponentLabel trafficSignalCount;
	private GUIComponentLabel crossingSignalCount;
	
	private final TileEntityTrafficSignalController controller;
	
	public GUITrafficSignalController(TileEntityTrafficSignalController controller){
		this.controller = controller;
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		addButton(new GUIComponentButton(guiLeft + 25, guiTop + 15, 200, WrapperGUI.translate("gui.trafficsignalcontroller.scan")){
			public void onClicked(){
				controller.trafficSignalLocations.clear();
				controller.crossingSignalLocations.clear();
				int scanDistance = Integer.valueOf(scanDistanceText.getText());
				for(int i=controller.position.x-scanDistance; i<=controller.position.x+scanDistance; ++i){
					for(int j=controller.position.y-scanDistance; j<=controller.position.y+scanDistance; ++j){
						for(int k=controller.position.z-scanDistance; k<=controller.position.z+scanDistance; ++k){
							Point3i point = new Point3i(i, j, k);
							ABlockBase block = controller.world.getBlock(point);
							if(block instanceof BlockPoleTrafficSignal){
								controller.trafficSignalLocations.add(point);
							}else if(block instanceof BlockPoleCrossingSignal){
								controller.crossingSignalLocations.add(point);
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
		addLabel(trafficSignalCount = new GUIComponentLabel(guiLeft + 135, guiTop + 55, Color.WHITE, " X " + String.valueOf(controller.trafficSignalLocations.size() - controller.crossingSignalLocations.size())));
		addItem(new GUIComponentItem(guiLeft + 170, guiTop + 50, 1.0F, "mts:crossingsignal", 1, -1));
		addLabel(crossingSignalCount = new GUIComponentLabel(guiLeft + 185, guiTop + 55, Color.WHITE, " X " + String.valueOf(controller.crossingSignalLocations.size())));
		
		addButton(orientationButton = new GUIComponentButton(guiLeft + 125, guiTop + 70, 100, controller.mainDirectionXAxis ? "X" : "Z", 15, true){
			@Override
			public void onClicked(){
				if(controller.mainDirectionXAxis){
					controller.mainDirectionXAxis = false;
					this.text = "Z";
				}else{
					controller.mainDirectionXAxis = true;
					this.text = "X";
				}
			}
		});
		addLabel(new GUIComponentLabel(guiLeft + 30, orientationButton.y + 5, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.primary")).setButton(orientationButton));
		
		addButton(modeButton = new GUIComponentButton(guiLeft + 125, guiTop + 85, 100, WrapperGUI.translate("gui.trafficsignalcontroller." + (controller.currentOpMode.equals(OpMode.VEHICLE_TRIGGER) ? "modetrigger" : (controller.currentOpMode.equals(OpMode.TIMED_CYCLE) ? "modetime" : "moderemote"))), 15, true){
			@Override
			public void onClicked(){
				if(controller.currentOpMode.equals(OpMode.VEHICLE_TRIGGER)){
					controller.currentOpMode = OpMode.TIMED_CYCLE;
					this.text = WrapperGUI.translate("gui.trafficsignalcontroller.modetime");
				}else{
					controller.currentOpMode = OpMode.VEHICLE_TRIGGER;
					this.text = WrapperGUI.translate("gui.trafficsignalcontroller.modetrigger");
				}
			}
		});
		addLabel(new GUIComponentLabel(guiLeft + 30, modeButton.y + 5, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.signalmode")).setButton(modeButton));
		
		addTextBox(greenMainTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 105, 40, String.valueOf(controller.greenMainTime), 10, Color.WHITE, Color.BLACK, 3));
		addLabel(new GUIComponentLabel(guiLeft + 30, greenMainTimeText.y, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.greenmaintime")).setBox(greenMainTimeText));
		
		addTextBox(greenCrossTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 115, 40, String.valueOf(controller.greenCrossTime), 10, Color.WHITE, Color.BLACK, 3));
		addLabel(new GUIComponentLabel(guiLeft + 30, greenCrossTimeText.y, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.greencrosstime")).setBox(greenCrossTimeText));
		
		addTextBox(yellowMainTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 125, 40, String.valueOf(controller.yellowMainTime), 10, Color.WHITE, Color.BLACK, 1));
		addLabel(new GUIComponentLabel(guiLeft + 30, yellowMainTimeText.y, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.yellowmaintime")).setBox(yellowMainTimeText));
		
		addTextBox(yellowCrossTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 135, 40, String.valueOf(controller.yellowCrossTime), 10, Color.WHITE, Color.BLACK, 1));
		addLabel(new GUIComponentLabel(guiLeft + 30, yellowCrossTimeText.y, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.yellowcrosstime")).setBox(yellowMainTimeText));
		
		addTextBox(allRedTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 145, 40, String.valueOf(controller.allRedTime), 10, Color.WHITE, Color.BLACK, 1));
		addLabel(new GUIComponentLabel(guiLeft + 30, allRedTimeText.y, Color.WHITE, WrapperGUI.translate("gui.trafficsignalcontroller.allredtime")).setBox(allRedTimeText));
		
		addButton(confirmButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 80, WrapperGUI.translate("gui.trafficsignalcontroller.confirm")){
			@Override
			public void onClicked(){
				//Convert strings to ints and send update packet to server.
				//Catch bad text if we have any as we don't want that to crash the server.
				try{
					controller.greenMainTime = Integer.valueOf(greenMainTimeText.getText());
					controller.greenCrossTime = Integer.valueOf(greenCrossTimeText.getText());
					controller.yellowMainTime = Integer.valueOf(yellowMainTimeText.getText());
					controller.yellowCrossTime = Integer.valueOf(yellowCrossTimeText.getText());
					controller.allRedTime = Integer.valueOf(allRedTimeText.getText());
				}catch(Exception e){
					return;
				}
				WrapperNetwork.sendToServer(new PacketTrafficSignalControllerChange(controller));
				WrapperGUI.closeGUI();
			}
		});
	}
	
	@Override
	public void setStates(){
		trafficSignalCount.text = " X " + String.valueOf(controller.trafficSignalLocations.size());
		crossingSignalCount.text = " X " + String.valueOf(controller.crossingSignalLocations.size());
		if(!controller.trafficSignalLocations.isEmpty()){
			orientationButton.enabled = true;
			modeButton.enabled = true;
			greenMainTimeText.enabled = true;
			greenCrossTimeText.enabled = true;
			yellowMainTimeText.enabled = true;
			yellowCrossTimeText.enabled = true;
			allRedTimeText.enabled = true;
			confirmButton.enabled = true;
		}else{
			orientationButton.enabled = false;
			modeButton.enabled = false;
			greenMainTimeText.enabled = false;
			greenCrossTimeText.enabled = false;
			yellowMainTimeText.enabled = false;
			yellowCrossTimeText.enabled = false;
			allRedTimeText.enabled = false;
			confirmButton.enabled = false;
		}
		greenMainTimeText.visible = controller.currentOpMode.equals(OpMode.TIMED_CYCLE);
	}
}
