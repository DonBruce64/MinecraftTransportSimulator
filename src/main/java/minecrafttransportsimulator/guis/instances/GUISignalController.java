package minecrafttransportsimulator.guis.instances;

import java.awt.Color;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_TrafficSignal;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentItem;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketTileEntitySignalControllerChange;
import net.minecraft.item.ItemStack;

public class GUISignalController extends AGUIBase{
	
	//Buttons.
	private GUIComponentButton rightHandDriveButton;
	private GUIComponentButton orientationButton;
	private GUIComponentButton confirmButton;
	
	//Input boxes
	private GUIComponentTextBox scanDistanceText;
	private GUIComponentTextBox mainLaneWidthText;
	private GUIComponentTextBox crossLaneWidthText;
	private GUIComponentTextBox mainLeftLaneCountText;
	private GUIComponentTextBox mainCenterLaneCountText;
	private GUIComponentTextBox mainRightLaneCountText;
	private GUIComponentTextBox crossLeftLaneCountText;
	private GUIComponentTextBox crossCenterLaneCountText;
	private GUIComponentTextBox crossRightLaneCountText;
	private GUIComponentTextBox intersectionCenterPointXText;
	private GUIComponentTextBox intersectionCenterPointyText;
	private GUIComponentTextBox intersectionCenterPointZText;
	
	private GUIComponentTextBox greenMainTimeText;
	private GUIComponentTextBox greenCrossTimeText;
	private GUIComponentTextBox yellowMainTimeText;
	private GUIComponentTextBox yellowCrossTimeText;
	private GUIComponentTextBox allRedTimeText;
	
	//Labels and items for scan results.
	private GUIComponentLabel trafficSignalCount;
	
	private final TileEntitySignalController controller;
	
	public GUISignalController(TileEntitySignalController controller){
		this.controller = controller;
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		addButton(new GUIComponentButton(guiLeft + 25, guiTop + 15, 200, InterfaceCore.translate("gui.trafficsignalcontroller.scan")){
			@Override
			public void onClicked(){
				controller.componentLocations.clear();
				int scanDistance = Integer.valueOf(scanDistanceText.getText());
				double minX = Double.MAX_VALUE;
				double maxX = Double.MIN_VALUE;
				double minZ = Double.MAX_VALUE;
				double maxZ = Double.MIN_VALUE;
				for(double i=controller.position.x-scanDistance; i<=controller.position.x+scanDistance; ++i){
					for(double j=controller.position.y-scanDistance; j<=controller.position.y+scanDistance; ++j){
						for(double k=controller.position.z-scanDistance; k<=controller.position.z+scanDistance; ++k){
							Point3d checkPosition = new Point3d(i, j, k);
							ATileEntityBase<?> tile = controller.world.getTileEntity(checkPosition);
							if(tile instanceof TileEntityPole){
								for(ATileEntityPole_Component component : ((TileEntityPole) tile).components.values()){
									if(component instanceof TileEntityPole_TrafficSignal){
										controller.componentLocations.add(checkPosition);
										minX = Math.min(minX, checkPosition.x);
										maxX = Math.max(maxX, checkPosition.x);
										minZ = Math.min(minZ, checkPosition.z);
										maxZ = Math.max(maxZ, checkPosition.z);
										break;
									}
								}
							}
						}
					}
				}
				controller.intersectionCenterPoint.set(minX + (maxX-minX)/2, controller.position.y, minZ + (maxZ - minZ)/2);
			}
		});
		addTextBox(scanDistanceText = new GUIComponentTextBox(guiLeft + 120, guiTop + 40, 40, "25", 10, Color.WHITE, Color.BLACK, 2));
		addLabel(new GUIComponentLabel(guiLeft + 30, scanDistanceText.y, Color.WHITE, InterfaceCore.translate("gui.trafficsignalcontroller.scandistance")).setBox(scanDistanceText));
		addLabel(new GUIComponentLabel(guiLeft + 30, guiTop + 55, Color.WHITE, InterfaceCore.translate("gui.trafficsignalcontroller.scanfound")));
		
		//Traffic signal scan results.
		addLabel(trafficSignalCount = new GUIComponentLabel(guiLeft + 135, guiTop + 56, Color.WHITE, " X" + controller.componentLocations.size()));
		
		
		addButton(orientationButton = new GUIComponentButton(guiLeft + 125, guiTop + 70, 100, controller.mainDirectionNorthOrNortheast ? (controller.isDiagonalIntersection ? "NE/SW" : "N/S") : (controller.isDiagonalIntersection ? "SE/NW" : "E/W"), 15, true){
			@Override
			public void onClicked(){
				if(controller.mainDirectionNorthOrNortheast){
					controller.mainDirectionNorthOrNortheast = false;
					this.text = controller.isDiagonalIntersection ? "SE/NW" : "E/W";
				}else{
					controller.mainDirectionNorthOrNortheast = true;
					this.text = controller.isDiagonalIntersection ? "NE/SW" : "N/S";
				}
			}
		});
		addLabel(new GUIComponentLabel(guiLeft + 30, orientationButton.y + 5, Color.WHITE, InterfaceCore.translate("gui.trafficsignalcontroller.primary")).setButton(orientationButton));
		
		addTextBox(greenMainTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 105, 40, String.valueOf(controller.greenMainTime), 10, Color.WHITE, Color.BLACK, 3));
		addLabel(new GUIComponentLabel(guiLeft + 30, greenMainTimeText.y, Color.WHITE, InterfaceCore.translate("gui.trafficsignalcontroller.greenmaintime")).setBox(greenMainTimeText));
		
		addTextBox(greenCrossTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 115, 40, String.valueOf(controller.greenCrossTime), 10, Color.WHITE, Color.BLACK, 3));
		addLabel(new GUIComponentLabel(guiLeft + 30, greenCrossTimeText.y, Color.WHITE, InterfaceCore.translate("gui.trafficsignalcontroller.greencrosstime")).setBox(greenCrossTimeText));
		
		addTextBox(yellowMainTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 125, 40, String.valueOf(controller.yellowMainTime), 10, Color.WHITE, Color.BLACK, 1));
		addLabel(new GUIComponentLabel(guiLeft + 30, yellowMainTimeText.y, Color.WHITE, InterfaceCore.translate("gui.trafficsignalcontroller.yellowmaintime")).setBox(yellowMainTimeText));
		
		addTextBox(yellowCrossTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 135, 40, String.valueOf(controller.yellowCrossTime), 10, Color.WHITE, Color.BLACK, 1));
		addLabel(new GUIComponentLabel(guiLeft + 30, yellowCrossTimeText.y, Color.WHITE, InterfaceCore.translate("gui.trafficsignalcontroller.yellowcrosstime")).setBox(yellowMainTimeText));
		
		addTextBox(allRedTimeText = new GUIComponentTextBox(guiLeft + 180, guiTop + 145, 40, String.valueOf(controller.allRedTime), 10, Color.WHITE, Color.BLACK, 1));
		addLabel(new GUIComponentLabel(guiLeft + 30, allRedTimeText.y, Color.WHITE, InterfaceCore.translate("gui.trafficsignalcontroller.allredtime")).setBox(allRedTimeText));
		
		addButton(confirmButton = new GUIComponentButton(guiLeft + 25, guiTop + 160, 80, InterfaceCore.translate("gui.trafficsignalcontroller.confirm")){
			@Override
			public void onClicked(){
				InterfacePacket.sendToServer(new PacketTileEntitySignalControllerChange(controller));
				InterfaceGUI.closeGUI();
			}
		});
	}
	
	@Override
	public void setStates(){
		trafficSignalCount.text = " X" + controller.componentLocations.size();
		if(controller.componentLocations.isEmpty()){
			orientationButton.enabled = false;
			greenMainTimeText.enabled = false;
			greenCrossTimeText.enabled = false;
			yellowMainTimeText.enabled = false;
			yellowCrossTimeText.enabled = false;
			allRedTimeText.enabled = false;
			confirmButton.enabled = false;
		}else{
			orientationButton.enabled = true;
			greenMainTimeText.enabled = true;
			greenCrossTimeText.enabled = true;
			yellowMainTimeText.enabled = true;
			yellowCrossTimeText.enabled = true;
			allRedTimeText.enabled = true;
			confirmButton.enabled = true;
		}
	}
}
