package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_TrafficSignal;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.IntersectionProperties;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketTileEntitySignalControllerChange;
import minecrafttransportsimulator.rendering.instances.RenderText.TextAlignment;

public class GUISignalController extends AGUIBase{
	
	//Buttons.
	private GUIComponentButton scanButton;
	private GUIComponentButton directionButton;
	private GUIComponentButton cycleButton;
	private GUIComponentButton driveSideButton;
	private boolean onLaneScreen;
	
	//Label for scan results.
	private GUIComponentLabel trafficSignalCount;
	
	//Input boxes
	private GUIComponentNumericTextBox scanDistanceText;
	private GUIComponentNumericTextBox scanCenterXText;
	private GUIComponentNumericTextBox scanCenterZText;
	private GUIComponentNumericTextBox laneWidthText;
	private GUIComponentNumericTextBox greenMainTimeText;
	private GUIComponentNumericTextBox greenCrossTimeText;
	private GUIComponentNumericTextBox yellowMainTimeText;
	private GUIComponentNumericTextBox yellowCrossTimeText;
	private GUIComponentNumericTextBox allRedTimeText;
	
	//Intersection property boxes.
	private final Set<GUIComponentIntersectionProperties> intersectionPropertyComponents = new HashSet<GUIComponentIntersectionProperties>();
	private final List<GUIComponentLabel> upperPropertyLabels = new ArrayList<GUIComponentLabel>();
	private final List<GUIComponentLabel> lowerPropertyLabels = new ArrayList<GUIComponentLabel>();
	
	//Controller we're linked to.
	private final TileEntitySignalController controller;
	
	public GUISignalController(TileEntitySignalController controller){
		this.controller = controller;
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		int topOffset = guiTop + 15;
		int leftTextOffset = guiLeft + 20;
		int leftObjectOffset = guiLeft + 100;
		int middleObjectOffset = guiLeft + 140;
		int rowSpacing = 2;
		
		//Main scan button.
		addButton(scanButton = new GUIComponentButton(leftTextOffset, topOffset, 220, InterfaceCore.translate("gui.trafficsignalcontroller.scan"), 15, true){
			@Override
			public void onClicked(){
				controller.componentLocations.clear();
				int scanDistance = Integer.valueOf(scanDistanceText.getText());
				double minX = Double.MAX_VALUE;
				double maxX = -Double.MAX_VALUE;
				double minZ = Double.MAX_VALUE;
				double maxZ = -Double.MAX_VALUE;
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
				scanCenterXText.setText(String.valueOf(controller.intersectionCenterPoint.x));
				scanCenterZText.setText(String.valueOf(controller.intersectionCenterPoint.z));
				double averageDistance = ((maxX - minX)/2D + (maxZ - minZ)/2D)/2D;
				for(Axis axis : controller.intersectionProperties.keySet()){
					IntersectionProperties properties = controller.intersectionProperties.get(axis);
					properties.roadWidth = averageDistance;
					properties.centerLaneCount = (int) Math.max(1, properties.roadWidth/laneWidthText.getDoubleValue());
					properties.centerDistance = averageDistance;
					if(controller.isRightHandDrive){
						properties.centerOffset = -averageDistance; 
					}
					for(GUIComponentIntersectionProperties component : intersectionPropertyComponents){
						if(component.axis.equals(axis)){
							component.centerLaneText.setText(String.valueOf(properties.centerLaneCount));
							component.roadWidthText.setText(String.valueOf(properties.roadWidth));
							component.centerDistanceText.setText(String.valueOf(properties.centerDistance));
							component.centerOffsetText.setText(String.valueOf(properties.centerOffset));
							break;
						}
					}
				}
				controller.initializeController(null);
				controller.unsavedClientChangesPreset = true;
			}
		});
		topOffset += scanButton.height + rowSpacing;
		
		
		
		
		
		//Scan center.
		addTextBox(scanCenterXText = new GUIComponentNumericTextBox(leftObjectOffset, topOffset, String.valueOf(controller.intersectionCenterPoint.x), 60){
			@Override
			public void setVariable(){
				controller.intersectionCenterPoint.x = getDoubleValue();
			}
		});
		addTextBox(scanCenterZText = new GUIComponentNumericTextBox(scanCenterXText.x + scanCenterXText.width + 5, topOffset, String.valueOf(controller.intersectionCenterPoint.z), 60){
			@Override
			public void setVariable(){
				controller.intersectionCenterPoint.z = getDoubleValue();
			}
		});
		addLabel(new GUIComponentLabel(leftTextOffset, topOffset, ColorRGB.WHITE, "Center (X/Z):").setBox(scanCenterXText));
		topOffset += scanCenterXText.height + rowSpacing;
		
		
		
		
		
		//Scan distance.
		addTextBox(scanDistanceText = new GUIComponentNumericTextBox(leftObjectOffset, topOffset, "25"));
		addLabel(new GUIComponentLabel(leftTextOffset, topOffset, ColorRGB.WHITE, "Radius:").setBox(scanDistanceText));
		
		//Found count.
		addLabel(trafficSignalCount = new GUIComponentLabel(scanDistanceText.x + scanDistanceText.width + 5, topOffset, ColorRGB.WHITE, "Found: " + String.valueOf(controller.componentLocations.size())));
		topOffset += scanDistanceText.height + rowSpacing*3;
		
		
		
		
		
		//RHD/LHD switch.
		addButton(driveSideButton = new GUIComponentButton(leftTextOffset, topOffset, 115, controller.isRightHandDrive ? "Right-Hand Drive" : "Left-Hand Drive", 15, true){
			@Override
			public void onClicked(){
				controller.isRightHandDrive = !controller.isRightHandDrive;
				this.text = controller.isRightHandDrive ? "Right-Hand Drive" : "Left-Hand Drive";
				controller.unsavedClientChangesPreset = true;
				controller.initializeController(null);
			}
		});
		
		//Timed mode direction.
		addButton(cycleButton = new GUIComponentButton(middleObjectOffset, topOffset, 100, controller.timedMode ? "Time Delay" : "Vehicle Trigger", 15, true){
			@Override
			public void onClicked(){
				controller.timedMode = !controller.timedMode;
				this.text = controller.timedMode ? "Time Delay" : "Vehicle Trigger";
				controller.unsavedClientChangesPreset = true;
				controller.initializeController(null);
			}
		});
		topOffset += cycleButton.height + rowSpacing;

		
		
		
		
		//Primary direction.
		addButton(directionButton = new GUIComponentButton(leftTextOffset, topOffset, 115, "Main Axis: " + controller.mainDirectionAxis.name(), 15, true){
			@Override
			public void onClicked(){
				switch(controller.mainDirectionAxis){
					case NORTH: controller.mainDirectionAxis = Axis.EAST; break;
					case EAST: controller.mainDirectionAxis = Axis.NORTHEAST; break;
					case NORTHEAST: controller.mainDirectionAxis = Axis.NORTHWEST; break;
					default: controller.mainDirectionAxis = Axis.NORTH; break;
				}
				this.text = "Main Axis: " + controller.mainDirectionAxis.name();
				controller.unsavedClientChangesPreset = true;
				controller.initializeController(null);
			}
		});
		
		//Lane width defaults.
		addTextBox(laneWidthText = new GUIComponentNumericTextBox(middleObjectOffset + 60, topOffset, "4.0", 40));
		addLabel(new GUIComponentLabel(middleObjectOffset, topOffset, ColorRGB.WHITE, "Lane Width:").setBox(laneWidthText));
		topOffset += 15 + rowSpacing*3;
		
		
		
		
		
		//Time text.  These auto-forward their values.
		addTextBox(greenMainTimeText = new GUIComponentNumericTextBox(middleObjectOffset, topOffset, String.valueOf(controller.greenMainTime/20)){
			@Override
			public void setVariable(){
				controller.greenMainTime = getIntegerValue()*20;
			}
		});
		addLabel(new GUIComponentLabel(leftTextOffset, topOffset, ColorRGB.WHITE, "Green Time (Main):").setBox(greenMainTimeText));
		topOffset += GUIComponentNumericTextBox.NUMERIC_HEIGHT;
		
		addTextBox(greenCrossTimeText = new GUIComponentNumericTextBox(middleObjectOffset, topOffset, String.valueOf(controller.greenCrossTime/20)){
			@Override
			public void setVariable(){
				controller.greenCrossTime = getIntegerValue()*20;
			}
		});
		addLabel(new GUIComponentLabel(leftTextOffset, topOffset, ColorRGB.WHITE, "Green Time (Cross):").setBox(greenCrossTimeText));
		topOffset += GUIComponentNumericTextBox.NUMERIC_HEIGHT;
		
		addTextBox(yellowMainTimeText = new GUIComponentNumericTextBox(middleObjectOffset, topOffset, String.valueOf(controller.yellowMainTime/20)){
			@Override
			public void setVariable(){
				controller.yellowMainTime = getIntegerValue()*20;
			}
		});
		addLabel(new GUIComponentLabel(leftTextOffset, topOffset, ColorRGB.WHITE, "Yellow Time (Main):").setBox(yellowMainTimeText));
		topOffset += GUIComponentNumericTextBox.NUMERIC_HEIGHT;
		
		addTextBox(yellowCrossTimeText = new GUIComponentNumericTextBox(middleObjectOffset, topOffset, String.valueOf(controller.yellowCrossTime/20)){
			@Override
			public void setVariable(){
				controller.yellowCrossTime = getIntegerValue()*20;
			}
		});
		addLabel(new GUIComponentLabel(leftTextOffset, topOffset, ColorRGB.WHITE, "Yellow Time (Cross):").setBox(yellowCrossTimeText));
		topOffset += GUIComponentNumericTextBox.NUMERIC_HEIGHT;
		
		addTextBox(allRedTimeText = new GUIComponentNumericTextBox(middleObjectOffset, topOffset, String.valueOf(controller.allRedTime/20)){
			@Override
			public void setVariable(){
				controller.allRedTime = getIntegerValue()*20;
			}
		});
		addLabel(new GUIComponentLabel(leftTextOffset, topOffset, ColorRGB.WHITE, "All Red Time:").setBox(allRedTimeText));
		topOffset += GUIComponentNumericTextBox.NUMERIC_HEIGHT + rowSpacing*4;
		
		
		
		
		
		//Change screen button.
		addButton(new GUIComponentButton(leftTextOffset, topOffset, 100, onLaneScreen ? "SIGNAL SETTINGS" : "LANE SETTINGS"){
			@Override
			public void onClicked(){
				onLaneScreen = !onLaneScreen;
				this.text = onLaneScreen ? "SIGNAL SETTINGS" : "LANE SETTINGS";
			}
		});
		
		//Confirm button.
		addButton(new GUIComponentButton(guiLeft + getWidth() - 100, topOffset, 80, "CONFIRM"){
			@Override
			public void onClicked(){
				InterfacePacket.sendToServer(new PacketTileEntitySignalControllerChange(controller));
				controller.unsavedClientChangesPreset = false;
				InterfaceGUI.closeGUI();
			}
		});
		
		
		
		
		
		//Properties.
		int baseLeftOffset = 80;
		int incrementalLeftOffset = 40;
		leftTextOffset = guiLeft + baseLeftOffset;
		topOffset = guiTop + 10;
		intersectionPropertyComponents.clear();
		upperPropertyLabels.clear();
		lowerPropertyLabels.clear();
		for(Axis axis : Axis.values()){
			if(axis.xzPlanar){
				GUIComponentIntersectionProperties propertiesComponent = new GUIComponentIntersectionProperties(guiLeft, guiTop, leftTextOffset, topOffset, axis);
				intersectionPropertyComponents.add(propertiesComponent);
				leftTextOffset += incrementalLeftOffset;
				if(leftTextOffset >= guiLeft + baseLeftOffset + 4*incrementalLeftOffset){
					leftTextOffset = guiLeft + baseLeftOffset;
					topOffset += 75;
				}
				
				List<GUIComponentLabel> currentList = axis.blockBased ? upperPropertyLabels : lowerPropertyLabels;
				if(currentList.isEmpty()){
					currentList.add(new GUIComponentLabel(guiLeft + 10, topOffset + 10, ColorRGB.WHITE, "# Left Lanes:", TextAlignment.LEFT_ALIGNED, 0.75F));
					currentList.add(new GUIComponentLabel(guiLeft + 10, topOffset + 20, ColorRGB.WHITE, "# Center Lanes:", TextAlignment.LEFT_ALIGNED, 0.75F));
					currentList.add(new GUIComponentLabel(guiLeft + 10, topOffset + 30, ColorRGB.WHITE, "# Right Lanes:", TextAlignment.LEFT_ALIGNED, 0.75F));
					currentList.add(new GUIComponentLabel(guiLeft + 10, topOffset + 40, ColorRGB.WHITE, "Road Width:", TextAlignment.LEFT_ALIGNED, 0.75F));
					currentList.add(new GUIComponentLabel(guiLeft + 10, topOffset + 50, ColorRGB.WHITE, "Dist Center->Road:", TextAlignment.LEFT_ALIGNED, 0.75F));
					currentList.add(new GUIComponentLabel(guiLeft + 10, topOffset + 60, ColorRGB.WHITE, "Dist Road->Median:", TextAlignment.LEFT_ALIGNED, 0.75F));
					for(GUIComponentLabel label : currentList){
						addLabel(label);
					}
				}
			}
		}
	}
	
	@Override
	public void setStates(){
		trafficSignalCount.text = "Found: " + String.valueOf(controller.componentLocations.size());
		
		scanButton.visible = !onLaneScreen;
		directionButton.visible = !onLaneScreen;
		cycleButton.visible = !onLaneScreen;
		driveSideButton.visible = !onLaneScreen;
		
		scanCenterXText.visible = !onLaneScreen;
		scanCenterZText.visible = !onLaneScreen;
		scanDistanceText.visible = !onLaneScreen;
		trafficSignalCount.visible = !onLaneScreen;
		laneWidthText.visible = !onLaneScreen;
		
		greenMainTimeText.visible = !onLaneScreen;
		greenCrossTimeText.visible = !onLaneScreen;
		yellowMainTimeText.visible = !onLaneScreen;
		yellowCrossTimeText.visible = !onLaneScreen;
		allRedTimeText.visible = !onLaneScreen;
		
		boolean upperLabelsVisible = false;
		boolean lowerLabelsVisible = false;
		for(GUIComponentIntersectionProperties propertyComponent : intersectionPropertyComponents){
			boolean showGroup = onLaneScreen && controller.intersectionProperties.get(propertyComponent.axis).isActive;
			propertyComponent.axisLabel.visible = showGroup;
			propertyComponent.leftLaneText.visible = showGroup;
			propertyComponent.centerLaneText.visible = showGroup;
			propertyComponent.rightLaneText.visible = showGroup;
			propertyComponent.roadWidthText.visible = showGroup;
			propertyComponent.centerDistanceText.visible = showGroup;
			propertyComponent.centerOffsetText.visible = showGroup;
			if(showGroup){
				if(propertyComponent.axis.blockBased){
					upperLabelsVisible = true;
				}else{
					lowerLabelsVisible = true;
				}
			}
		}
		
		for(GUIComponentLabel label : upperPropertyLabels){
			label.visible = upperLabelsVisible;
		}
		for(GUIComponentLabel label : lowerPropertyLabels){
			label.visible = lowerLabelsVisible;
		}
	}
	
	private class GUIComponentNumericTextBox extends GUIComponentTextBox{
		private final boolean floatingPoint;
		private static final int NUMERIC_HEIGHT = 10;

		public GUIComponentNumericTextBox(int x, int y, String text){
			super(x, y, 40, text, NUMERIC_HEIGHT, ColorRGB.WHITE, ColorRGB.BLACK, 5);
			this.floatingPoint = false;
		}
		
		public GUIComponentNumericTextBox(int x, int y, String text, int width){
			super(x, y, width, text, NUMERIC_HEIGHT, ColorRGB.WHITE, ColorRGB.BLACK, 7);
			this.floatingPoint = true;
		}
		
		@Override
		public void handleTextChange(){
			controller.unsavedClientChangesPreset = true;
			setVariable();
			controller.initializeController(null);
		}
		
		@Override
		public boolean isTextValid(String newText){
			//Only allow numbers.
			if(newText.isEmpty()){
				return true;
			}else{
				if(floatingPoint){
					return newText.matches("-?\\d+(\\.\\d+)?");
				}else{
					return newText.matches("[0-9]+");
				}
			}
		}
		
		protected void setVariable(){};
		
		protected int getIntegerValue(){
			String text = getText();
			return text.isEmpty() ? 0 : Integer.valueOf(text);
		}
		
		protected double getDoubleValue(){
			String text = getText();
			return text.isEmpty() ? 0 : Double.valueOf(text);
		}
	}
	
	private class GUIComponentIntersectionProperties{
		private final Axis axis;
		private final GUIComponentLabel axisLabel;
		private final GUIComponentTextBox leftLaneText;
		private final GUIComponentTextBox centerLaneText;
		private final GUIComponentTextBox rightLaneText;
		private final GUIComponentTextBox roadWidthText;
		private final GUIComponentTextBox centerDistanceText;
		private final GUIComponentTextBox centerOffsetText;
		
		
		private GUIComponentIntersectionProperties(int guiLeft, int guiTop, int leftOffset, int topOffset, Axis axis){
			this.axis = axis;
			IntersectionProperties properties = controller.intersectionProperties.get(axis);
			addLabel(axisLabel = new GUIComponentLabel(leftOffset, topOffset, ColorRGB.WHITE, axis.name(), TextAlignment.LEFT_ALIGNED, axis.blockBased ? 1.0F : 0.65F));
			addTextBox(leftLaneText = new GUIComponentNumericTextBox(leftOffset, topOffset + 10, String.valueOf(properties.leftLaneCount)){
				@Override
				public void setVariable(){
					controller.intersectionProperties.get(axis).leftLaneCount = getIntegerValue();
				}
			});
			
			addTextBox(centerLaneText = new GUIComponentNumericTextBox(leftOffset, topOffset + 20, String.valueOf(properties.centerLaneCount)){
				@Override
				public void setVariable(){
					controller.intersectionProperties.get(axis).centerLaneCount = getIntegerValue();
				}
			});
			addTextBox(rightLaneText = new GUIComponentNumericTextBox(leftOffset, topOffset + 30, String.valueOf(properties.rightLaneCount)){
				@Override
				public void setVariable(){
					controller.intersectionProperties.get(axis).rightLaneCount = getIntegerValue();
				}
			});
			addTextBox(roadWidthText = new GUIComponentNumericTextBox(leftOffset, topOffset + 40, String.valueOf(properties.roadWidth), 40){
				@Override
				public void setVariable(){
					controller.intersectionProperties.get(axis).roadWidth = getDoubleValue();
				}
			});
			addTextBox(centerDistanceText = new GUIComponentNumericTextBox(leftOffset, topOffset + 50, String.valueOf(properties.centerDistance), 40){
				@Override
				public void setVariable(){
					controller.intersectionProperties.get(axis).centerDistance = getDoubleValue();
				}
			});
			addTextBox(centerOffsetText = new GUIComponentNumericTextBox(leftOffset, topOffset + 60, String.valueOf(properties.centerOffset), 40){
				@Override
				public void setVariable(){
					controller.intersectionProperties.get(axis).centerOffset = getDoubleValue();
				}
			});
		}
	}
}
