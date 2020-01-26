package minecrafttransportsimulator.guis.instances;

import java.awt.Color;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityTrafficSignalController;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.components.GUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.packets.tileentities.PacketTrafficSignalControllerChange;
import minecrafttransportsimulator.wrappers.CrossingSignalData;
import minecrafttransportsimulator.wrappers.TrafficSignalData;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class GUITrafficSignalController extends GUIBase {
	
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
		addButton(new GUIComponentButton(guiLeft + 25, guiTop + 15, 200, WrapperGUI.translate("trafficsignalcontroller.scan")){
			public void onClicked(){
				signalController.trafficSignals.clear();
				int scanDistance = Integer.valueOf(scanDistanceText.getText());
				for(int i=signalController.getPos().getX()-scanDistance; i<=signalController.getPos().getX()+scanDistance; ++i){
					for(int j=signalController.getPos().getY()-scanDistance; j<=signalController.getPos().getY()+scanDistance; ++j){
						for(int k=signalController.getPos().getZ()-scanDistance; k<=signalController.getPos().getZ()+scanDistance; ++k){
							BlockPos pos = new BlockPos(i, j, k);
							Block block = signalController.getWorld().getBlockState(pos).getBlock();
							if(block.equals(MTSRegistry.trafficSignal)){
								signalController.trafficSignals.put(pos, new TrafficSignalData(pos));
							}else if(block.equals(MTSRegistry.crossingSignal)){
								signalController.trafficSignals.put(pos, new TrafficSignalData(pos));
								signalController.crossingSignals.put(pos, new CrossingSignalData());
							}
						}
					}
				}
			}
		});
		addTextBox(scanDistanceText = new GUIComponentTextBox(guiLeft + 120, guiTop + 40, 40, "25", 10, Color.WHITE, Color.BLACK, 2));
		addLabel(new GUIComponentLabel(guiLeft + 30, scanDistanceText.y, Color.WHITE, WrapperGUI.translate("trafficsignalcontroller.scandistance")).setBox(scanDistanceText));
		
		addLabel(new GUIComponentLabel(guiLeft + 30, guiTop + 55, Color.WHITE, WrapperGUI.translate("trafficsignalcontroller.scanfound")));
		addItem(new GUIComponentItem(guiLeft + 120, guiTop + 50, 1.0F, "mts:trafficsignal", 1, -1));
		addLabel(trafficSignalCount = new GUIComponentLabel(guiLeft + 135, guiTop + 55, Color.WHITE, " X " + String.valueOf(signalController.trafficSignals.size() - signalController.crossingSignals.size())));
		addItem(new GUIComponentItem(guiLeft + 170, guiTop + 50, 1.0F, "mts:crossingsignal", 1, -1));
		addLabel(crossingSignalCount = new GUIComponentLabel(guiLeft + 185, guiTop + 55, Color.WHITE, " X " + String.valueOf(signalController.crossingSignals.size())));
		
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
		addLabel(new GUIComponentLabel(guiLeft + 30, orientationButton.y + 5, Color.WHITE, WrapperGUI.translate("trafficsignalcontroller.primary")).setButton(orientationButton));
		
		addButton(modeButton = new GUIComponentButton(guiLeft + 125, guiTop + 90, 100, WrapperGUI.translate("trafficsignalcontroller." + getModeName(signalController.mode))){
			@Override
			public void onClicked(){
				byte newMode = (byte) (signalController.mode == 3 ? 0 : signalController.mode+1);
				signalController.mode = newMode;
				this.text = WrapperGUI.translate("trafficsignalcontroller." + getModeName(newMode));
			}
		});
		addLabel(new GUIComponentLabel(guiLeft + 30, modeButton.y + 5, Color.WHITE, WrapperGUI.translate("trafficsignalcontroller.signalmode")).setButton(modeButton));
		
		addTextBox(greenMainTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 115, 40, String.valueOf(signalController.greenMainTime), 10, Color.WHITE, Color.BLACK, 3));
		addLabel(new GUIComponentLabel(guiLeft + 30, greenMainTimeText.y, Color.WHITE, WrapperGUI.translate("trafficsignalcontroller.greenmaintime")).setBox(greenMainTimeText));
		
		addTextBox(greenCrossTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 125, 40, String.valueOf(signalController.greenCrossTime), 10, Color.WHITE, Color.BLACK, 3));
		addLabel(new GUIComponentLabel(guiLeft + 30, greenCrossTimeText.y, Color.WHITE, WrapperGUI.translate("trafficsignalcontroller.greencrosstime")).setBox(greenCrossTimeText));
		
		addTextBox(yellowTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 135, 40, String.valueOf(signalController.yellowTime), 10, Color.WHITE, Color.BLACK, 1));
		addLabel(new GUIComponentLabel(guiLeft + 30, yellowTimeText.y, Color.WHITE, WrapperGUI.translate("trafficsignalcontroller.yellowtime")).setBox(yellowTimeText));
		
		addTextBox(allRedTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 145, 40, String.valueOf(signalController.allRedTime), 10, Color.WHITE, Color.BLACK, 1));
		addLabel(new GUIComponentLabel(guiLeft + 30, allRedTimeText.y, Color.WHITE, WrapperGUI.translate("trafficsignalcontroller.allredtime")).setBox(allRedTimeText));
		
		addButton(confirmButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 80, WrapperGUI.translate("trafficsignalcontroller.confirm")){
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
				WrapperGUI.closeScreen();
			}
		});
	}

	private String getModeName(int mode) {
		switch (mode) {
			case 1: return "modemanual";
			case 2: return "modetime";
			case 3: return "modetrigger";
			default: return "disabled";
		}
	}
	
	@Override
	public void setStates(){
		trafficSignalCount.text = " X " + String.valueOf(signalController.trafficSignals.size() - signalController.crossingSignals.size());
		crossingSignalCount.text = " X " + String.valueOf(signalController.crossingSignals.size());
		if (!signalController.trafficSignals.isEmpty()) {
			orientationButton.enabled = true;
			modeButton.enabled = true;
			greenMainTimeText.enabled = true;
			greenCrossTimeText.enabled = true;
			yellowTimeText.enabled = true;
			allRedTimeText.enabled = true;
			confirmButton.enabled = true;
		} else {
			orientationButton.enabled = false;
			modeButton.enabled = false;
			greenMainTimeText.enabled = false;
			greenCrossTimeText.enabled = false;
			yellowTimeText.enabled = false;
			allRedTimeText.enabled = false;
			confirmButton.enabled = false;
		}

		if (signalController.mode <= 1) {
			greenMainTimeText.visible = false;
			greenCrossTimeText.visible = false;
			yellowTimeText.visible = false;
			allRedTimeText.visible = false;
		} else {
			greenMainTimeText.visible = signalController.mode != 3;
			greenCrossTimeText.visible = true;
			yellowTimeText.visible = true;
			allRedTimeText.visible = true;
		}
	}
}
