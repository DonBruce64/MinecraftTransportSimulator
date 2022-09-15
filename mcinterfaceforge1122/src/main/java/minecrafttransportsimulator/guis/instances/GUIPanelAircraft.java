package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentSelector;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketVehicleBeaconChange;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * A GUI/control system hybrid, this takes the place of the HUD when called up.
 * Used for controlling engines, lights, trim, and other things.
 *
 * @author don_bruce
 */
public class GUIPanelAircraft extends AGUIPanel {
    private static final int NAVIGATION_TEXTURE_WIDTH_OFFSET = 200;
    private static final int NAVIGATION_TEXTURE_HEIGHT_OFFSET = 216;
    private static final int STROBE_TEXTURE_WIDTH_OFFSET = NAVIGATION_TEXTURE_WIDTH_OFFSET + 20;
    private static final int STROBE_TEXTURE_HEIGHT_OFFSET = 216;
    private static final int TAXI_TEXTURE_WIDTH_OFFSET = STROBE_TEXTURE_WIDTH_OFFSET + 20;
    private static final int TAXI_TEXTURE_HEIGHT_OFFSET = 216;
    private static final int LANDING_TEXTURE_WIDTH_OFFSET = TAXI_TEXTURE_WIDTH_OFFSET + 20;
    private static final int LANDING_TEXTURE_HEIGHT_OFFSET = 216;
    private static final int ENGINEMAG_TEXTURE_WIDTH_OFFSET = LANDING_TEXTURE_WIDTH_OFFSET + 20;
    private static final int ENGINEMAG_TEXTURE_HEIGHT_OFFSET = 216;
    private static final int ENGINESTART_TEXTURE_WIDTH_OFFSET = ENGINEMAG_TEXTURE_WIDTH_OFFSET + 20;
    private static final int ENGINESTART_TEXTURE_HEIGHT_OFFSET = 196;
    private static final int REVERSE_TEXTURE_WIDTH_OFFSET = ENGINESTART_TEXTURE_WIDTH_OFFSET + 20;
    private static final int REVERSE_TEXTURE_HEIGHT_OFFSET = 216;
    private static final int TRIM_TEXTURE_WIDTH_OFFSET = REVERSE_TEXTURE_WIDTH_OFFSET + 20;
    private static final int TRIM_TEXTURE_HEIGHT_OFFSET = 216;
    private static final int AUTOPILOT_TEXTURE_WIDTH_OFFSET = TRIM_TEXTURE_WIDTH_OFFSET + 40;
    private static final int AUTOPILOT_TEXTURE_HEIGHT_OFFSET = 216;
    private static final int GEAR_TEXTURE_WIDTH_OFFSET = AUTOPILOT_TEXTURE_WIDTH_OFFSET + 20;
    private static final int GEAR_TEXTURE_HEIGHT_OFFSET = 176;
    private static final int CUSTOM_TEXTURE_WIDTH_OFFSET = GEAR_TEXTURE_WIDTH_OFFSET + 20;
    private static final int CUSTOM_TEXTURE_HEIGHT_OFFSET = 216;
    private static final int TRAILER_TEXTURE_WIDTH_OFFSET = CUSTOM_TEXTURE_WIDTH_OFFSET + 20;
    private static final int TRAILER_TEXTURE_HEIGHT_OFFSET = 216;

    private final Map<String, GUIComponentSelector> lightSelectors = new HashMap<>();
    private final List<GUIComponentSelector> magnetoSelectors = new ArrayList<>();
    private final List<GUIComponentSelector> starterSelectors = new ArrayList<>();
    private final List<GUIComponentSelector> customSelectors = new ArrayList<>();
    private GUIComponentSelector aileronTrimSelector;
    private GUIComponentSelector elevatorTrimSelector;
    private GUIComponentSelector rudderTrimSelector;
    private GUIComponentSelector reverseSelector;
    private GUIComponentSelector autopilotSelector;
    private GUIComponentSelector gearSelector;
    private GUIComponentSelector trailerSelector;
    private GUIComponentTextBox beaconBox;

    private GUIComponentSelector selectedTrimSelector;
    private String selectedTrimVariable = null;
    private double selectedTrimBounds = 0;
    private double selectedTrimIncrement = 0;
    private boolean appliedTrimThisRender;

    public GUIPanelAircraft(EntityVehicleF_Physics aircraft) {
        super(aircraft);
    }

    @Override
    protected void setupLightComponents(int guiLeft, int guiTop) {
        lightSelectors.clear();
        //Create up to four lights depending on how many this vehicle has.
        if (vehicle.definition.motorized.hasNavLights) {
            GUIComponentSelector lightSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + lightSelectors.size() * (GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, "NAV", vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, NAVIGATION_TEXTURE_WIDTH_OFFSET, NAVIGATION_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.NAVIGATIONLIGHT_VARIABLE));
                }
            };
            lightSelectors.put(EntityVehicleF_Physics.NAVIGATIONLIGHT_VARIABLE, lightSwitch);
            addComponent(lightSwitch);
        }
        if (vehicle.definition.motorized.hasStrobeLights) {
            GUIComponentSelector lightSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + lightSelectors.size() * (GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, "STROBE", vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, STROBE_TEXTURE_WIDTH_OFFSET, STROBE_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.STROBELIGHT_VARIABLE));
                }
            };
            lightSelectors.put(EntityVehicleF_Physics.STROBELIGHT_VARIABLE, lightSwitch);
            addComponent(lightSwitch);
        }
        if (vehicle.definition.motorized.hasTaxiLights) {
            GUIComponentSelector lightSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + lightSelectors.size() * (GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, "TAXI", vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, TAXI_TEXTURE_WIDTH_OFFSET, TAXI_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.TAXILIGHT_VARIABLE));
                }
            };
            lightSelectors.put(EntityVehicleF_Physics.TAXILIGHT_VARIABLE, lightSwitch);
            addComponent(lightSwitch);
        }
        if (vehicle.definition.motorized.hasLandingLights) {
            GUIComponentSelector lightSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + lightSelectors.size() * (GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, "LAND", vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, LANDING_TEXTURE_WIDTH_OFFSET, LANDING_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.LANDINGLIGHT_VARIABLE));
                }
            };
            lightSelectors.put(EntityVehicleF_Physics.LANDINGLIGHT_VARIABLE, lightSwitch);
            addComponent(lightSwitch);
        }
    }

    @Override
    protected void setupEngineComponents(int guiLeft, int guiTop) {
        magnetoSelectors.clear();
        starterSelectors.clear();
        if (vehicle.definition.motorized.hasSingleEngineControl) {
            if (!vehicle.engines.isEmpty()) {
                GUIComponentSelector magnetoSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS, SELECTOR_SIZE, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_MAGNETO.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, ENGINEMAG_TEXTURE_WIDTH_OFFSET, ENGINEMAG_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                    @Override
                    public void onClicked(boolean leftSide) {
                        vehicle.engines.forEach(engine -> {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.MAGNETO_VARIABLE));
                        });
                    }
                };
                magnetoSelectors.add(magnetoSwitch);
                addComponent(magnetoSwitch);

                GUIComponentSelector starterSwitch = new GUIComponentSelector(magnetoSwitch.constructedX + SELECTOR_SIZE, magnetoSwitch.constructedY, SELECTOR_SIZE, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_START.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, ENGINESTART_TEXTURE_WIDTH_OFFSET, ENGINESTART_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                    @Override
                    public void onClicked(boolean leftSide) {
                        vehicle.engines.forEach(engine -> {
                            if (engine.magnetoOn && !engine.electricStarterEngaged) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.ELECTRIC_STARTER_VARIABLE));
                            }
                        });
                    }

                    @Override
                    public void onReleased() {
                        vehicle.engines.forEach(engine -> {
                            if (engine.magnetoOn && engine.electricStarterEngaged) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.ELECTRIC_STARTER_VARIABLE));
                            }
                        });
                    }
                };
                starterSelectors.add(starterSwitch);
                addComponent(starterSwitch);
            }
        } else {
            //Create magneto and stater selectors for the engines.
            vehicle.engines.forEach(engine -> {
                //Go to next column if we are on our 5th engine.
                int engineNumber = vehicle.engines.indexOf(engine);
                if (engineNumber == 4) {
                    xOffset += 2 * SELECTOR_SIZE + GAP_BETWEEN_SELECTORS;
                }

                GUIComponentSelector magnetoSwitch = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS) * (engineNumber % 4), SELECTOR_SIZE, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_MAGNETO.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, ENGINEMAG_TEXTURE_WIDTH_OFFSET, ENGINEMAG_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                    @Override
                    public void onClicked(boolean leftSide) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle.engines.get(engineNumber), PartEngine.MAGNETO_VARIABLE));
                    }
                };
                magnetoSelectors.add(magnetoSwitch);
                addComponent(magnetoSwitch);

                GUIComponentSelector starterSwitch = new GUIComponentSelector(magnetoSwitch.constructedX + SELECTOR_SIZE, magnetoSwitch.constructedY, SELECTOR_SIZE, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_START.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, ENGINESTART_TEXTURE_WIDTH_OFFSET, ENGINESTART_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                    @Override
                    public void onClicked(boolean leftSide) {
                        PartEngine engine = vehicle.engines.get(engineNumber);
                        if (engine.magnetoOn && !engine.electricStarterEngaged) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.ELECTRIC_STARTER_VARIABLE));
                        }
                    }

                    @Override
                    public void onReleased() {
                        PartEngine engine = vehicle.engines.get(engineNumber);
                        if (engine.magnetoOn && engine.electricStarterEngaged) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.ELECTRIC_STARTER_VARIABLE));
                        }
                    }
                };
                starterSelectors.add(starterSwitch);
                addComponent(starterSwitch);
            });
        }

        //Need to offset the xOffset by the selector size to account for the two engine controls.
        xOffset += SELECTOR_SIZE;
    }

    @Override
    protected void setupGeneralComponents(int guiLeft, int guiTop) {
        //Add the trim selectors first.
        aileronTrimSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS, SELECTOR_SIZE * 2, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_TRIM_ROLL.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, TRIM_TEXTURE_WIDTH_OFFSET, TRIM_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE * 2, SELECTOR_TEXTURE_SIZE) {
            @Override
            public void onClicked(boolean leftSide) {
                selectedTrimSelector = this;
                selectedTrimVariable = EntityVehicleF_Physics.AILERON_TRIM_VARIABLE;
                selectedTrimBounds = EntityVehicleF_Physics.MAX_AILERON_TRIM;
                selectedTrimIncrement = !leftSide ? 0.1 : -0.1;
            }

            @Override
            public void onReleased() {
                selectedTrimVariable = null;
            }
        };
        addComponent(aileronTrimSelector);

        elevatorTrimSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE * 2, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_TRIM_PITCH.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, TRIM_TEXTURE_WIDTH_OFFSET, TRIM_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE * 2, SELECTOR_TEXTURE_SIZE) {
            @Override
            public void onClicked(boolean leftSide) {
                selectedTrimSelector = this;
                selectedTrimVariable = EntityVehicleF_Physics.ELEVATOR_TRIM_VARIABLE;
                selectedTrimBounds = EntityVehicleF_Physics.MAX_ELEVATOR_TRIM;
                selectedTrimIncrement = leftSide ? 0.1 : -0.1;
            }

            @Override
            public void onReleased() {
                selectedTrimVariable = null;
            }
        };
        addComponent(elevatorTrimSelector);

        rudderTrimSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 2 * (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE * 2, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_TRIM_YAW.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, TRIM_TEXTURE_WIDTH_OFFSET, TRIM_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE * 2, SELECTOR_TEXTURE_SIZE) {
            @Override
            public void onClicked(boolean leftSide) {
                selectedTrimSelector = this;

                selectedTrimVariable = EntityVehicleF_Physics.RUDDER_TRIM_VARIABLE;
                selectedTrimBounds = EntityVehicleF_Physics.MAX_RUDDER_TRIM;
                selectedTrimIncrement = !leftSide ? 0.1 : -0.1;
            }

            @Override
            public void onReleased() {
                selectedTrimVariable = null;
            }
        };
        addComponent(rudderTrimSelector);

        //If we have both reverse thrust AND autopilot, render them side-by-side. Otherwise just render one in the middle
        if (haveReverseThrustOption && vehicle.definition.motorized.hasAutopilot) {
            reverseSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 3 * (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_REVERSE.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, REVERSE_TEXTURE_WIDTH_OFFSET, REVERSE_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.REVERSE_THRUST_VARIABLE));
                }
            };
            addComponent(reverseSelector);

            autopilotSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE, guiTop + GAP_BETWEEN_SELECTORS + 3 * (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_AUTOPILOT.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, AUTOPILOT_TEXTURE_WIDTH_OFFSET, AUTOPILOT_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    if (vehicle.autopilotSetting == 0) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle, EntityVehicleF_Physics.AUTOPILOT_VARIABLE, vehicle.position.y));
                    } else {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle, EntityVehicleF_Physics.AUTOPILOT_VARIABLE, 0));
                    }
                }
            };
            addComponent(autopilotSelector);
        } else if (haveReverseThrustOption) {
            reverseSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE / 2, guiTop + GAP_BETWEEN_SELECTORS + 3 * (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_REVERSE.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, REVERSE_TEXTURE_WIDTH_OFFSET, REVERSE_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.REVERSE_THRUST_VARIABLE));
                }
            };
            addComponent(reverseSelector);
        } else if (vehicle.definition.motorized.hasAutopilot) {
            autopilotSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE / 2, guiTop + GAP_BETWEEN_SELECTORS + 3 * (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_AUTOPILOT.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, AUTOPILOT_TEXTURE_WIDTH_OFFSET, AUTOPILOT_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    if (vehicle.autopilotSetting == 0) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle, EntityVehicleF_Physics.AUTOPILOT_VARIABLE, vehicle.position.y));
                    } else {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle, EntityVehicleF_Physics.AUTOPILOT_VARIABLE, 0));
                    }
                }
            };
            addComponent(autopilotSelector);
        }

        //Need to offset the xOffset by the selector size to account for the double-width trim controls.
        xOffset += SELECTOR_SIZE;
    }

    @Override
    public void setupCustomComponents(int guiLeft, int guiTop) {
        //Add custom selectors if we have any.
        //These are the right-most selector and are vehicle-specific.
        //We render two rows of side-by-side selectors here.
        Set<String> customVariables = new LinkedHashSet<>();
        if (vehicle.definition.rendering.customVariables != null) {
            customVariables.addAll(vehicle.definition.rendering.customVariables);
        }
        for (APart part : vehicle.allParts) {
            if (part.definition.rendering != null && part.definition.rendering.customVariables != null) {
                customVariables.addAll(part.definition.rendering.customVariables);
            }
        }

        int variableNumber = 0;
        for (String customVariable : customVariables) {
            GUIComponentSelector customSelector = new GUIComponentSelector(guiLeft + xOffset + (variableNumber % 2) * SELECTOR_SIZE, guiTop + GAP_BETWEEN_SELECTORS + (variableNumber / 2) * (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, customVariable, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, CUSTOM_TEXTURE_WIDTH_OFFSET, CUSTOM_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, this.text));
                }
            };
            customSelectors.add(customSelector);
            addComponent(customSelector);
            ++variableNumber;
        }

        if (vehicle.definition.motorized.hasRadioNav || ConfigSystem.settings.general.allPlanesWithNav.value) {
            //Add beacon text box.  This is stacked below the custom selectors.
            beaconBox = new GUIComponentTextBox(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 2 * (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE * 2, SELECTOR_SIZE, vehicle.selectedBeaconName, vehicle.selectedBeacon != null ? ColorRGB.GREEN : ColorRGB.RED, 5, BEACON_TEXTURE_WIDTH_OFFSET, BEACON_TEXTURE_HEIGHT_OFFSET, 40, 20) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                    //Update the vehicle beacon state.
                    InterfaceManager.packetInterface.sendToServer(new PacketVehicleBeaconChange(vehicle, getText()));
                }
            };
            addComponent(beaconBox);

            //Add beacon text box label.
            addComponent(new GUIComponentLabel(beaconBox.constructedX + beaconBox.width / 2, beaconBox.constructedY + beaconBox.height + 1, vehicle.definition.motorized.panelTextColor != null ? vehicle.definition.motorized.panelTextColor : ColorRGB.WHITE, JSONConfigLanguage.GUI_PANEL_BEACON.value, TextAlignment.CENTERED, 0.75F).setBox(beaconBox));
        }

        //If we have both gear and a trailer hookup, render them side-by-side. Otherwise just render one in the middle
        if (vehicle.definition.motorized.gearSequenceDuration != 0 && !trailerSwitchDefs.isEmpty()) {
            gearSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 3 * (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_GEAR.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, GEAR_TEXTURE_WIDTH_OFFSET, GEAR_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.GEAR_VARIABLE));
                }
            };
            addComponent(gearSelector);

            trailerSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE, guiTop + GAP_BETWEEN_SELECTORS + 3 * (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, trailerSwitchDefs.get(0).connectionGroup.groupName, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, TRAILER_TEXTURE_WIDTH_OFFSET, TRAILER_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    SwitchEntry switchDef = trailerSwitchDefs.get(0);
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(switchDef.connectionDefiner, AEntityG_Towable.TOWING_CONNECTION_REQUEST_VARIABLE, switchDef.connectionGroupIndex + 1));
                }
            };
            addComponent(trailerSelector);
        } else if (vehicle.definition.motorized.gearSequenceDuration != 0) {
            gearSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE / 2, guiTop + GAP_BETWEEN_SELECTORS + 3 * (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, JSONConfigLanguage.GUI_PANEL_GEAR.value, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, GEAR_TEXTURE_WIDTH_OFFSET, GEAR_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.GEAR_VARIABLE));
                }
            };
            addComponent(gearSelector);
        } else if (!trailerSwitchDefs.isEmpty()) {
            trailerSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE / 2, guiTop + GAP_BETWEEN_SELECTORS + 3 * (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, trailerSwitchDefs.get(0).connectionGroup.groupName, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, TRAILER_TEXTURE_WIDTH_OFFSET, TRAILER_TEXTURE_HEIGHT_OFFSET, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE) {
                @Override
                public void onClicked(boolean leftSide) {
                    SwitchEntry switchDef = trailerSwitchDefs.get(0);
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(switchDef.connectionDefiner, AEntityG_Towable.TOWING_CONNECTION_REQUEST_VARIABLE, switchDef.connectionGroupIndex + 1));
                }
            };
            addComponent(trailerSelector);
        }
    }

    @Override
    public void setStates() {
        super.setStates();
        //Set the states of the light selectors.
        for (Entry<String, GUIComponentSelector> lightEntry : lightSelectors.entrySet()) {
            lightEntry.getValue().selectorState = vehicle.isVariableActive(lightEntry.getKey()) ? 1 : 0;
        }

        //Set the states of the magneto selectors.
        if (vehicle.definition.motorized.hasSingleEngineControl) {
            if (!vehicle.engines.isEmpty()) {
                PartEngine engine = vehicle.engines.get(0);
                magnetoSelectors.get(0).selectorState = engine.magnetoOn ? 1 : 0;
            }
        } else {
            for (GUIComponentSelector magnetoSelector : magnetoSelectors) {
                magnetoSelector.selectorState = vehicle.engines.get(magnetoSelectors.indexOf(magnetoSelector)).magnetoOn ? 1 : 0;
            }
        }

        //Set the states of the starter selectors.
        if (vehicle.definition.motorized.hasSingleEngineControl) {
            PartEngine engine = vehicle.engines.get(0);
            starterSelectors.get(0).selectorState = engine.magnetoOn ? (engine.electricStarterEngaged ? 2 : 1) : 0;
            starterSelectors.get(0).visible = !engine.definition.engine.disableAutomaticStarter && engine.definition.engine.type == JSONPart.EngineType.NORMAL;
        } else {
            for (GUIComponentSelector starterSelector : starterSelectors) {
                PartEngine engine = vehicle.engines.get(starterSelectors.indexOf(starterSelector));
                starterSelector.selectorState = engine.magnetoOn ? (engine.electricStarterEngaged ? 2 : 1) : 0;
                starterSelector.visible = !engine.definition.engine.disableAutomaticStarter;
            }
        }

        //For every 3 ticks we have one of the trim selectors pressed, do the corresponding trim action.
        if (selectedTrimVariable != null) {
            if (inClockPeriod(3, 1)) {
                if (!appliedTrimThisRender) {
                    double currentTrim = vehicle.getVariable(selectedTrimVariable);
                    if (currentTrim + selectedTrimIncrement > -selectedTrimBounds && currentTrim + selectedTrimIncrement < selectedTrimBounds) {
                        selectedTrimSelector.selectorState = selectedTrimSelector.selectorState == 0 ? 1 : 0;
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(vehicle, selectedTrimVariable, selectedTrimIncrement, -selectedTrimBounds, selectedTrimBounds));
                    }
                    appliedTrimThisRender = true;
                }
            } else {
                appliedTrimThisRender = false;
            }
        }

        //If we have reverse thrust, set the selector state.
        if (reverseSelector != null) {
            if (vehicle.definition.motorized.isBlimp) {
                reverseSelector.selectorState = 0;
                for (PartEngine engine : vehicle.engines) {
                    if (engine.currentGear < 0) {
                        reverseSelector.selectorState = 1;
                        break;
                    }
                }
            } else {
                reverseSelector.selectorState = vehicle.reverseThrust ? 1 : 0;
            }
        }

        //If we have autopilot, set the selector state.
        if (autopilotSelector != null) {
            autopilotSelector.selectorState = vehicle.autopilotSetting != 0 ? 1 : 0;
        }

        //If we have gear, set the selector state.
        if (gearSelector != null) {
            if (vehicle.isVariableActive(EntityVehicleF_Physics.GEAR_VARIABLE)) {
                gearSelector.selectorState = vehicle.gearMovementTime == vehicle.definition.motorized.gearSequenceDuration ? 2 : 3;
            } else {
                gearSelector.selectorState = vehicle.gearMovementTime == 0 ? 0 : 1;
            }
        }

        //If we have a hitch, set the selector state.
        if (trailerSelector != null) {
            trailerSwitchDefs.get(0).updateSelectorState(trailerSelector);
        }

        //Set the beaconBox text color depending on if we have an active beacon.
        if (beaconBox != null) {
            beaconBox.fontColor = vehicle.selectedBeacon != null ? ColorRGB.GREEN : ColorRGB.RED;
        }

        //Iterate through custom selectors and set their states.
        for (GUIComponentSelector customSelector : customSelectors) {
            customSelector.selectorState = vehicle.isVariableActive(customSelector.text) ? 1 : 0;
        }
    }
}
