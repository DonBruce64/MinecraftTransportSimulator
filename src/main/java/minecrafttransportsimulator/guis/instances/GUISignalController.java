package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_TrafficSignal;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.IntersectionProperties;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController.SignalGroup;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketTileEntitySignalControllerChange;

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
	private GUIComponentTextBox scanDistanceText;
	private GUIComponentTextBox scanCenterXText;
	private GUIComponentTextBox scanCenterZText;
	private GUIComponentTextBox greenMainTimeText;
	private GUIComponentTextBox greenCrossTimeText;
	private GUIComponentTextBox yellowMainTimeText;
	private GUIComponentTextBox yellowCrossTimeText;
	private GUIComponentTextBox allRedTimeText;
	
	//Intersection property boxes.
	private final Set<GUIComponentIntersectionProperties> intersectionPropertyComponents = new HashSet<GUIComponentIntersectionProperties>();
	
	//Controller we're linked to.
	private final TileEntitySignalController controller;
	
	public GUISignalController(TileEntitySignalController controller){
		this.controller = controller;
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		int topOffset = guiTop + 15;
		int leftOffset = guiLeft + 20;
		int middleOffset = leftOffset + 80;
		int rowSpacing = 2;
		addButton(scanButton = new GUIComponentButton(leftOffset, topOffset, 220, InterfaceCore.translate("gui.trafficsignalcontroller.scan")){
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
				if(controller.intersectionCenterPoint.equals(controller.position)){
					controller.intersectionCenterPoint.set(minX + (maxX-minX)/2, controller.position.y, minZ + (maxZ - minZ)/2);
					scanCenterXText.setText(String.valueOf(controller.intersectionCenterPoint.x));
					scanCenterZText.setText(String.valueOf(controller.intersectionCenterPoint.z));
					double averageDistance = ((maxX - minX)/2D + (maxZ - minZ)/2D)/2D;
					for(Axis axis : controller.intersectionProperties.keySet()){
						IntersectionProperties properties = controller.intersectionProperties.get(axis);
						properties.centerLaneCount = 2;
						properties.laneWidth = averageDistance/2D;
						properties.centerDistance = averageDistance;
						for(GUIComponentIntersectionProperties component : intersectionPropertyComponents){
							if(component.axis.equals(axis)){
								component.centerLaneText.setText(String.valueOf(properties.centerLaneCount));
								component.laneWidthText.setText(String.valueOf(properties.laneWidth));
								component.centerDistanceText.setText(String.valueOf(properties.centerDistance));
								break;
							}
						}
					}
				}
				controller.initializeController(controller.save(new WrapperNBT()));
				controller.unsavedClientChangesPreset = true;
			}
		});
		topOffset += scanButton.height + rowSpacing;
		
		//Scan center.
		addTextBox(scanCenterXText = new GUIComponentNumericTextBox(middleOffset, topOffset, String.valueOf(controller.intersectionCenterPoint.x), 60){
			@Override
			public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
				super.handleKeyTyped(typedChar, typedCode, control);
				controller.intersectionCenterPoint.x = getDoubleValue();
				controller.initializeController(controller.save(new WrapperNBT()));
			}
		});
		addTextBox(scanCenterZText = new GUIComponentNumericTextBox(scanCenterXText.x + scanCenterXText.width + 5, topOffset, String.valueOf(controller.intersectionCenterPoint.z), 60){
			@Override
			public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
				super.handleKeyTyped(typedChar, typedCode, control);
				controller.intersectionCenterPoint.z = getDoubleValue();
				controller.initializeController(controller.save(new WrapperNBT()));
			}
		});
		addLabel(new GUIComponentLabel(leftOffset, topOffset, Color.WHITE, "Center (X/Z):").setBox(scanCenterXText));
		topOffset += scanCenterXText.height + rowSpacing;
		
		//Scan distance.
		addTextBox(scanDistanceText = new GUIComponentTextBox(middleOffset, topOffset, 60, "25", 10, Color.WHITE, Color.BLACK, 2));
		addLabel(new GUIComponentLabel(leftOffset, topOffset, Color.WHITE, "Radius:").setBox(scanDistanceText));
		
		//Found count.
		addLabel(trafficSignalCount = new GUIComponentLabel(scanCenterZText.x, topOffset, Color.WHITE, "Found: " + String.valueOf(controller.componentLocations.size())));
		topOffset += scanDistanceText.height + rowSpacing*3;
		middleOffset = leftOffset + 60;
		
		//Primary direction.
		addButton(directionButton = new GUIComponentButton(middleOffset, topOffset, 70, controller.mainDirectionNorthOrNortheast ? (controller.isDiagonalIntersection ? "NE/SW" : "N/S") : (controller.isDiagonalIntersection ? "SE/NW" : "E/W"), 15, true){
			@Override
			public void onClicked(){
				if(controller.mainDirectionNorthOrNortheast){
					controller.mainDirectionNorthOrNortheast = false;
					this.text = controller.isDiagonalIntersection ? "SE/NW" : "E/W";
				}else{
					controller.mainDirectionNorthOrNortheast = true;
					this.text = controller.isDiagonalIntersection ? "NE/SW" : "N/S";
				}
				controller.unsavedClientChangesPreset = true;
				controller.initializeController(controller.save(new WrapperNBT()));
			}
		});
		addLabel(new GUIComponentLabel(leftOffset, topOffset + 5, Color.WHITE, "Primary Dir:").setButton(directionButton));
		
		//Timed mode direction.
		addButton(cycleButton = new GUIComponentButton(middleOffset + 105, topOffset, 50, controller.timedMode ? "TIME" : "TRIGGER", 15, true){
			@Override
			public void onClicked(){
				controller.timedMode = !controller.timedMode;
				this.text = controller.timedMode ? "TIME" : "TRIGGER";
				controller.unsavedClientChangesPreset = true;
				controller.initializeController(controller.save(new WrapperNBT()));
			}
		});
		addLabel(new GUIComponentLabel(middleOffset + 75, topOffset + 5, Color.WHITE, "Mode:").setButton(cycleButton));
		topOffset += cycleButton.height + rowSpacing;
		
		//RHD/LHD switch.
		addButton(driveSideButton = new GUIComponentButton(middleOffset, topOffset, 70, controller.isRightHandDrive ? "RIGHT-HAND" : "LEFT-HAND", 15, true){
			@Override
			public void onClicked(){
				controller.isRightHandDrive = !controller.isRightHandDrive;
				this.text = controller.isRightHandDrive ? "RIGHT-HAND" : "LEFT-HAND";
				controller.unsavedClientChangesPreset = true;
				controller.initializeController(controller.save(new WrapperNBT()));
			}
		});
		addLabel(new GUIComponentLabel(leftOffset, topOffset + 5, Color.WHITE, "Drive Side:").setButton(driveSideButton));
		topOffset += 15 + rowSpacing*3;
		middleOffset = leftOffset + 100;
		
		//Time text.  These auto-forward their values.
		addTextBox(greenMainTimeText = new GUIComponentNumericTextBox(middleOffset, topOffset, String.valueOf(controller.greenMainTime)){
			@Override
			public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
				super.handleKeyTyped(typedChar, typedCode, control);
				controller.greenMainTime = getIntegerValue();
				controller.initializeController(controller.save(new WrapperNBT()));
			}
		});
		addLabel(new GUIComponentLabel(leftOffset, topOffset, Color.WHITE, "Green Time (Main):").setBox(greenMainTimeText));
		topOffset += GUIComponentNumericTextBox.NUMERIC_HEIGHT;
		
		addTextBox(greenCrossTimeText = new GUIComponentNumericTextBox(middleOffset, topOffset, String.valueOf(controller.greenCrossTime)){
			@Override
			public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
				super.handleKeyTyped(typedChar, typedCode, control);
				controller.greenCrossTime = getIntegerValue();
				controller.initializeController(controller.save(new WrapperNBT()));
			}
		});
		addLabel(new GUIComponentLabel(leftOffset, topOffset, Color.WHITE, "Green Time (Cross):").setBox(greenCrossTimeText));
		topOffset += GUIComponentNumericTextBox.NUMERIC_HEIGHT;
		
		addTextBox(yellowMainTimeText = new GUIComponentNumericTextBox(middleOffset, topOffset, String.valueOf(controller.yellowMainTime)){
			@Override
			public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
				super.handleKeyTyped(typedChar, typedCode, control);
				controller.yellowMainTime = getIntegerValue();
				controller.initializeController(controller.save(new WrapperNBT()));
			}
		});
		addLabel(new GUIComponentLabel(leftOffset, topOffset, Color.WHITE, "Yellow Time (Main):").setBox(yellowMainTimeText));
		topOffset += GUIComponentNumericTextBox.NUMERIC_HEIGHT;
		
		addTextBox(yellowCrossTimeText = new GUIComponentNumericTextBox(middleOffset, topOffset, String.valueOf(controller.yellowCrossTime)){
			@Override
			public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
				super.handleKeyTyped(typedChar, typedCode, control);
				controller.yellowCrossTime = getIntegerValue();
				controller.initializeController(controller.save(new WrapperNBT()));
			}
		});
		addLabel(new GUIComponentLabel(leftOffset, topOffset, Color.WHITE, "Yellow Time (Cross):").setBox(yellowCrossTimeText));
		topOffset += GUIComponentNumericTextBox.NUMERIC_HEIGHT;
		
		addTextBox(allRedTimeText = new GUIComponentNumericTextBox(middleOffset, topOffset, String.valueOf(controller.allRedTime)){
			@Override
			public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
				super.handleKeyTyped(typedChar, typedCode, control);
				controller.allRedTime = getIntegerValue();
				controller.initializeController(controller.save(new WrapperNBT()));
			}
		});
		addLabel(new GUIComponentLabel(leftOffset, topOffset, Color.WHITE, "All Red Time:").setBox(allRedTimeText));
		topOffset += GUIComponentNumericTextBox.NUMERIC_HEIGHT + rowSpacing*4;
		
		//Change screen button.
		addButton(new GUIComponentButton(leftOffset, topOffset, 100, onLaneScreen ? "SIGNAL SETTINGS" : "LANE SETTINGS"){
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
		leftOffset = guiLeft + baseLeftOffset;
		topOffset = guiTop + 10;
		intersectionPropertyComponents.clear();
		boolean addedUpperLabels = false;
		boolean addedLowerLabels = false;
		for(Axis axis : Axis.values()){
			if(axis.xzPlanar){
				GUIComponentIntersectionProperties propertiesComponent = new GUIComponentIntersectionProperties(guiLeft, guiTop, leftOffset, topOffset, axis);
				intersectionPropertyComponents.add(propertiesComponent);
				leftOffset += incrementalLeftOffset;
				if(leftOffset >= guiLeft + baseLeftOffset + 4*incrementalLeftOffset){
					leftOffset = guiLeft + baseLeftOffset;
					topOffset += 75;
				}
				
				if((axis.blockBased && !addedUpperLabels) || (!axis.blockBased && !addedLowerLabels)){
					addLabel(new GUIComponentLabel(guiLeft + 10, topOffset + 10, Color.WHITE, "# Left Lanes:", null, TextPosition.LEFT_ALIGNED, 0, 0.75F, false).setBox(propertiesComponent.leftLaneText));
					addLabel(new GUIComponentLabel(guiLeft + 10, topOffset + 20, Color.WHITE, "# Center Lanes:", null, TextPosition.LEFT_ALIGNED, 0, 0.75F, false).setBox(propertiesComponent.centerLaneText));
					addLabel(new GUIComponentLabel(guiLeft + 10, topOffset + 30, Color.WHITE, "# Right Lanes:", null, TextPosition.LEFT_ALIGNED, 0, 0.75F, false).setBox(propertiesComponent.rightLaneText));
					addLabel(new GUIComponentLabel(guiLeft + 10, topOffset + 40, Color.WHITE, "Lane Width:", null, TextPosition.LEFT_ALIGNED, 0, 0.75F, false).setBox(propertiesComponent.laneWidthText));
					addLabel(new GUIComponentLabel(guiLeft + 10, topOffset + 50, Color.WHITE, "Dist Center->Road:", null, TextPosition.LEFT_ALIGNED, 0, 0.75F, false).setBox(propertiesComponent.centerDistanceText));
					addLabel(new GUIComponentLabel(guiLeft + 10, topOffset + 60, Color.WHITE, "Dist Road->Median:", null, TextPosition.LEFT_ALIGNED, 0, 0.75F, false).setBox(propertiesComponent.centerOffsetText));
					
					if(axis.blockBased){
						addedUpperLabels = true;
					}else{
						addedLowerLabels = true;
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
		
		greenMainTimeText.visible = !onLaneScreen && controller.timedMode;
		greenCrossTimeText.visible = !onLaneScreen;
		yellowMainTimeText.visible = !onLaneScreen;
		yellowCrossTimeText.visible = !onLaneScreen;
		allRedTimeText.visible = !onLaneScreen;
		
		for(GUIComponentIntersectionProperties propertyComponent : intersectionPropertyComponents){
			boolean showGroup = false;
			if(onLaneScreen){
	        	for(SignalGroup signalGroup : controller.signalGroups){
	        		if(signalGroup.axis.equals(propertyComponent.axis)){
	        			showGroup = true;
	        			break;
	        		}
	        	}
			}
			
			propertyComponent.axisLabel.visible = showGroup;
			propertyComponent.leftLaneText.visible = showGroup;
			propertyComponent.centerLaneText.visible = showGroup;
			propertyComponent.rightLaneText.visible = showGroup;
			propertyComponent.laneWidthText.visible = showGroup;
			propertyComponent.centerDistanceText.visible = showGroup;
			propertyComponent.centerOffsetText.visible = showGroup;
		}
	}
	
	private class GUIComponentNumericTextBox extends GUIComponentTextBox{
		private final boolean floatingPoint;
		private static final int NUMERIC_HEIGHT = 10;

		public GUIComponentNumericTextBox(int x, int y, String text){
			super(x, y, 40, text, NUMERIC_HEIGHT, Color.WHITE, Color.BLACK, 5);
			this.floatingPoint = false;
		}
		
		public GUIComponentNumericTextBox(int x, int y, String text, int width){
			super(x, y, width, text, NUMERIC_HEIGHT, Color.WHITE, Color.BLACK, 7);
			this.floatingPoint = true;
		}
		
		@Override
		public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
			super.handleKeyTyped(typedChar, typedCode, control);
			controller.unsavedClientChangesPreset = true;
		}
		
		@Override
		public boolean validateText(String newText){
			//Only allow numbers.
			if(floatingPoint){
				return newText.matches("-?\\d+(\\.\\d+)?");
			}else{
				return newText.matches("[0-9]+");
			}
		}
		
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
		private final GUIComponentTextBox laneWidthText;
		private final GUIComponentTextBox centerDistanceText;
		private final GUIComponentTextBox centerOffsetText;
		
		
		private GUIComponentIntersectionProperties(int guiLeft, int guiTop, int leftOffset, int topOffset, Axis axis){
			this.axis = axis;
			IntersectionProperties currentProperties = controller.intersectionProperties.get(axis);
			addLabel(axisLabel = new GUIComponentLabel(leftOffset, topOffset, Color.WHITE, axis.name(), null, TextPosition.LEFT_ALIGNED, 0, axis.blockBased ? 1.0F : 0.65F, false));
			addTextBox(leftLaneText = new GUIComponentNumericTextBox(leftOffset, topOffset + 10, String.valueOf(currentProperties.leftLaneCount)){
				@Override
				public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
					super.handleKeyTyped(typedChar, typedCode, control);
					controller.intersectionProperties.get(axis).leftLaneCount = getIntegerValue();
					controller.initializeController(controller.save(new WrapperNBT()));
				}
			});
			
			addTextBox(centerLaneText = new GUIComponentNumericTextBox(leftOffset, topOffset + 20, String.valueOf(currentProperties.centerLaneCount)){
				@Override
				public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
					super.handleKeyTyped(typedChar, typedCode, control);
					controller.intersectionProperties.get(axis).centerLaneCount = getIntegerValue();
					controller.initializeController(controller.save(new WrapperNBT()));
				}
			});
			addTextBox(rightLaneText = new GUIComponentNumericTextBox(leftOffset, topOffset + 30, String.valueOf(currentProperties.rightLaneCount)){
				@Override
				public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
					super.handleKeyTyped(typedChar, typedCode, control);
					controller.intersectionProperties.get(axis).rightLaneCount = getIntegerValue();
					controller.initializeController(controller.save(new WrapperNBT()));
				}
			});
			addTextBox(laneWidthText = new GUIComponentNumericTextBox(leftOffset, topOffset + 40, String.valueOf(currentProperties.laneWidth), 40){
				@Override
				public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
					super.handleKeyTyped(typedChar, typedCode, control);
					controller.intersectionProperties.get(axis).laneWidth = getDoubleValue();
					controller.initializeController(controller.save(new WrapperNBT()));
				}
			});
			addTextBox(centerDistanceText = new GUIComponentNumericTextBox(leftOffset, topOffset + 50, String.valueOf(currentProperties.centerDistance), 40){
				@Override
				public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
					super.handleKeyTyped(typedChar, typedCode, control);
					controller.intersectionProperties.get(axis).centerDistance = getDoubleValue();
					controller.initializeController(controller.save(new WrapperNBT()));
				}
			});
			addTextBox(centerOffsetText = new GUIComponentNumericTextBox(leftOffset, topOffset + 60, String.valueOf(currentProperties.centerOffset), 40){
				@Override
				public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
					super.handleKeyTyped(typedChar, typedCode, control);
					controller.intersectionProperties.get(axis).centerOffset = getDoubleValue();
					controller.initializeController(controller.save(new WrapperNBT()));
				}
			});
		}
	}
}
