package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.TowingConnection;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.AGUIComponent;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.jsondefs.JSONPanel;
import minecrafttransportsimulator.jsondefs.JSONPanel.JSONPanelClickAction;
import minecrafttransportsimulator.jsondefs.JSONPanel.JSONPanelComponent;
import minecrafttransportsimulator.jsondefs.JSONPanel.SpecialComponent;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketVehicleBeaconChange;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;

/**
 * A GUI/control system hybrid, this takes the place of the HUD when called up.
 * This class is contains the base code for rendering things common to all vehicles, 
 * specifics are added in the JSON as applicable.
 *
 * @author don_bruce
 */
public class GUIPanel extends AGUIBase {

    //Properties.
    private final EntityVehicleF_Physics vehicle;
    private final JSONPanel definition;
    private final List<SwitchEntry> trailerSwitchDefs = new ArrayList<>();
    private final List<String> customVariables = new ArrayList<>();

    //Created components.
    private final List<GUIPanelButton> componentButtons = new ArrayList<>();
    private final List<GUIComponentLabel> labels = new ArrayList<>();
    private GUIComponentTextBox beaconBox;

    public GUIPanel(EntityVehicleF_Physics vehicle) {
        super();
        this.vehicle = vehicle;
        this.definition = getDefinitionFor(vehicle);
    }

    public static JSONPanel getDefinitionFor(EntityVehicleF_Physics vehicle) {
        JSONPanel panel = null;
        try {
            String packID = vehicle.definition.motorized.panel.substring(0, vehicle.definition.motorized.panel.indexOf(':'));
            String systemName = vehicle.definition.motorized.panel.substring(packID.length() + 1);
            panel = PackParser.getPackPanel(packID, systemName);
        } catch (Exception e) {
            //Do nothing, we just use default and throw an error if bad format or missing.
        }
        if (panel != null) {
            return panel;
        } else {
            InterfaceManager.clientInterface.getClientPlayer().displayChatMessage(LanguageSystem.SYSTEM_DEBUG, "Could not display the requested panel " + vehicle.definition.motorized.panel + ". Report this to the pack author!  Default panel will be used.");
            if (vehicle.definition.motorized.isAircraft) {
                return PackParser.getPackPanel(InterfaceManager.coreModID, "default_plane");
            } else {
                return PackParser.getPackPanel(InterfaceManager.coreModID, "default_car");
            }
        }
    }

    private void setupTowingButtons(AEntityG_Towable<?> entity) {
        //Add trailer switch defs to allow switches to be displayed.
        //These depend on our connections, and our part's connections.
        //This method allows for recursion for connected trailers.
        if (entity.definition.connectionGroups != null) {
            for (JSONConnectionGroup connectionGroup : entity.definition.connectionGroups) {
                if (connectionGroup.canInitiateConnections) {
                    trailerSwitchDefs.add(new SwitchEntry(entity, connectionGroup));
                }
            }

            //Also check things we are towing, if we are set to do so.
            for (TowingConnection connection : entity.towingConnections) {
                if (connection.hookupConnectionGroup.canInitiateSubConnections) {
                    setupTowingButtons(connection.towedVehicle);
                }
            }
        }

        //Check parts, if we have any.
        for (APart part : entity.allParts) {
            if (part.definition.connectionGroups != null) {
                for (JSONConnectionGroup connectionGroup : part.definition.connectionGroups) {
                    if (connectionGroup.canInitiateConnections) {
                        trailerSwitchDefs.add(new SwitchEntry(part, connectionGroup));
                    }
                }
            }
        }
    }

    /**
     * Call this if this GUI is open and a trailer connection changes.  This allows this GUI to
     * reset its states on a trailer change, if the trailer that state was changed was one of our switches.
     */
    public void handleConnectionChange(TowingConnection connection) {
        boolean recreatePanel = false;
        for (SwitchEntry entry : trailerSwitchDefs) {
            if (entry.vehicleOn.equals(connection.towingVehicle) || entry.vehicleOn.equals(connection.towedVehicle)) {
                recreatePanel = true;
                break;
            }
        }
        if (recreatePanel) {
            setupComponents();
        }
    }

    @Override
    public final void setupComponents() {
        super.setupComponents();
        //Setup towing, this is recursive.
        trailerSwitchDefs.clear();
        setupTowingButtons(vehicle);

        //Populate variables for any handlers we may have.
        customVariables.clear();
        if (vehicle.definition.rendering.customVariables != null) {
            for (String variable : vehicle.definition.rendering.customVariables) {
                if (!customVariables.contains(variable)) {
                    customVariables.add(variable);
                }
            }
        }
        for (APart part : vehicle.allParts) {
            if (part.definition.rendering != null && part.definition.rendering.customVariables != null) {
                for (String variable : part.definition.rendering.customVariables) {
                    if (!customVariables.contains(variable)) {
                        customVariables.add(variable);
                    }
                }
            }
        }

        //Setup defined general components.
        componentButtons.clear();
        labels.clear();

        boolean populatedSingleEngineControl = false;
        boolean populatedSingleEngineMag = false;
        boolean populatedSingleEngineStarter = false;
        int engineControlIndex = 0;
        int engineMagIndex = 0;
        int engineStarterIndex = 0;
        int trailerIndex = 0;
        int customVariableIndex = 0;
        for (JSONPanelComponent panelComponent : definition.panel.components) {
            AGUIComponent newComponent = null;
            String text = null;
            if (panelComponent.specialComponent == null) {
                newComponent = new GUIPanelButton(this, panelComponent) {
                    @Override
                    public void onClicked(boolean leftSide) {
                        if (panelComponent.clickAction != null) {
                            handleClickAction(panelComponent.clickAction);
                        }
                        if (panelComponent.clickActionLeft != null && leftSide) {
                            handleClickAction(panelComponent.clickActionLeft);
                        }
                        if (panelComponent.clickActionRight != null && !leftSide) {
                            handleClickAction(panelComponent.clickActionRight);
                        }
                    }
                };
                text = panelComponent.text;
            } else {
                ComputedVariable buttonVariable = null;
                switch (panelComponent.specialComponent) {
                    case CAR_LIGHT: {
                        if (vehicle.definition.motorized.hasRunningLights || vehicle.definition.motorized.hasHeadlights) {
                            newComponent = new GUIPanelButton(this, panelComponent) {
                                @Override
                                public void onClicked(boolean leftSide) {
                                    if (leftSide) {
                                        if (vehicle.headLightVar.isActive) {
                                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle.headLightVar));
                                        } else if (vehicle.runningLightVar.isActive) {
                                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle.runningLightVar));
                                        }
                                    } else {
                                        if (vehicle.definition.motorized.hasRunningLights && !vehicle.runningLightVar.isActive) {
                                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle.runningLightVar));
                                        } else if (vehicle.definition.motorized.hasHeadlights && !vehicle.headLightVar.isActive) {
                                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle.headLightVar));
                                        }
                                    }
                                }

                                @Override
                                public int getState() {
                                    return vehicle.headLightVar.isActive ? 2 : (vehicle.runningLightVar.isActive ? 1 : 0);
                                }
                            };
                            text = "LIGHTS";
                        }
                        break;
                    }
                    case TURN_SIGNAL: {
                        if (vehicle.definition.motorized.hasTurnSignals) {
                            newComponent = new GUIPanelButton(this, panelComponent) {
                                @Override
                                public void onClicked(boolean leftSide) {
                                    if (leftSide) {
                                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle.leftTurnLightVar));
                                    } else {
                                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle.rightTurnLightVar));
                                    }
                                }

                                @Override
                                public int getState() {
                                    //This makes the indicator blink when on.
                                    if (inClockPeriod(20, 10)) {
                                        int returnValue = 0;
                                        if (vehicle.leftTurnLightVar.isActive) {
                                            returnValue += 1;
                                        }
                                        if (vehicle.rightTurnLightVar.isActive) {
                                            returnValue += 2;
                                        }
                                        return returnValue;
                                    } else {
                                        return 0;
                                    }
                                }
                            };
                            text = "TURNSGNL";
                        }
                        break;
                    }
                    case NAVIGATION_LIGHT: {
                        if (vehicle.definition.motorized.hasNavLights) {
                            buttonVariable = vehicle.navigationLightVar;
                            text = "NAV";
                        }
                        break;
                    }
                    case STROBE_LIGHT: {
                        if (vehicle.definition.motorized.hasStrobeLights) {
                            buttonVariable = vehicle.strobeLightVar;
                            text = "STROBE";
                        }
                        break;
                    }
                    case TAXI_LIGHT: {
                        if (vehicle.definition.motorized.hasTaxiLights) {
                            buttonVariable = vehicle.taxiLightVar;
                            text = "TAXI";
                        }
                        break;
                    }
                    case LANDING_LIGHT: {
                        if (vehicle.definition.motorized.hasLandingLights) {
                            buttonVariable = vehicle.landingLightVar;
                            text = "LAND";
                        }
                        break;
                    }
                    case ENGINE_CONTROL: {
                        if (!vehicle.engines.isEmpty()) {
                            if (vehicle.definition.motorized.hasSingleEngineControl) {
                                if (!populatedSingleEngineControl) {
                                    newComponent = new GUIPanelEngineButton(this, panelComponent, null);
                                    text = "ENGINE";
                                    populatedSingleEngineControl = true;
                                }
                            } else {
                                PartEngine engine = getEngineBySwitchIndex(engineControlIndex++);
                                if (engine != null) {
                                    newComponent = new GUIPanelEngineButton(this, panelComponent, engine);
                                    text = "ENGINE";
                                }
                            }
                        }
                        break;
                    }
                    case ENGINE_ON: {
                        if (!vehicle.engines.isEmpty()) {
                            if (vehicle.definition.motorized.hasSingleEngineControl) {
                                if (!populatedSingleEngineMag) {
                                    newComponent = new GUIPanelEngineButton(this, panelComponent, null);
                                    text = "MAG";
                                    populatedSingleEngineMag = true;
                                }
                            } else {
                                PartEngine engine = getEngineBySwitchIndex(engineMagIndex++);
                                if (engine != null) {
                                    newComponent = new GUIPanelEngineButton(this, panelComponent, engine);
                                    text = "MAG";
                                }
                            }
                        }
                        break;
                    }
                    case ENGINE_START: {
                        if (!vehicle.engines.isEmpty()) {
                            if (vehicle.definition.motorized.hasSingleEngineControl) {
                                if (!populatedSingleEngineStarter) {
                                    newComponent = new GUIPanelEngineButton(this, panelComponent, null);
                                    text = "START";
                                    populatedSingleEngineStarter = true;
                                }
                            } else {
                                PartEngine engine = getEngineBySwitchIndex(engineStarterIndex++);
                                if (engine != null) {
                                    newComponent = new GUIPanelEngineButton(this, panelComponent, engine);
                                    text = "START";
                                }
                            }
                        }
                        break;
                    }
                    case TRAILER: {
                        if (trailerSwitchDefs.size() > trailerIndex) {
                            SwitchEntry switchDef = trailerSwitchDefs.get(trailerIndex++);
                            text = switchDef.connectionGroup.groupName;
                            newComponent = new GUIPanelButton(this, panelComponent) {
                                @Override
                                public void onClicked(boolean leftSide) {
                                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(switchDef.connectionDefiner.towingConnectionVar, switchDef.connectionGroupIndex + 1));
                                }

                                @Override
                                public int getState() {
                                    return switchDef.isConnected() ? 1 : 0;
                                }
                            };
                        }
                        break;
                    }
                    case AUTOPILOT: {
                    	if (vehicle.autopilotValueVar.isActive) {
                    		InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle.autopilotValueVar, 0));
                        }else if (vehicle.definition.motorized.isAircraft) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle.autopilotValueVar, vehicle.position.y));
                        } else {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle.autopilotValueVar, vehicle.indicatedSpeed));
                        }
                        break;
                    }
                    case CUSTOM_VARIABLE: {
                        if (customVariables.size() > customVariableIndex) {
                            String customVariable = customVariables.get(customVariableIndex++);
                            buttonVariable = vehicle.getVariable(customVariable);
                            text = customVariable;
                        }
                        break;
                    }
                    case BEACON_BOX: {
                        if (vehicle.definition.motorized.hasRadioNav || ConfigSystem.settings.general.allPlanesWithNav.value) {
                            beaconBox = new GUIComponentTextBox(this, guiLeft + (int) panelComponent.pos.x, guiTop + (int) panelComponent.pos.y, panelComponent.width, panelComponent.height, vehicle.selectedBeaconName, ColorRGB.WHITE, 5, (int) panelComponent.textureStart.x, (int) panelComponent.textureStart.y, panelComponent.width, panelComponent.height) {
                                @Override
                                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                                    super.handleKeyTyped(typedChar, typedCode, control);
                                    //Update the vehicle beacon state.
                                    InterfaceManager.packetInterface.sendToServer(new PacketVehicleBeaconChange(vehicle, getText()));
                                }
                            };
                            addComponent(beaconBox);
                            newComponent = beaconBox;
                            text = "BEACON";
                        }
                        break;
                    }
                    case ROLL_TRIM: {
                        newComponent = new GUIPanelTrimButton(this, panelComponent, vehicle.aileronTrimVar, -0.1, EntityVehicleF_Physics.MAX_AILERON_TRIM);
                        text = "ROLL TRIM";
                        break;
                    }
                    case PITCH_TRIM: {
                        newComponent = new GUIPanelTrimButton(this, panelComponent, vehicle.elevatorTrimVar, 0.1, EntityVehicleF_Physics.MAX_ELEVATOR_TRIM);
                        text = "PITCH TRIM";
                        break;
                    }
                    case YAW_TRIM: {
                        newComponent = new GUIPanelTrimButton(this, panelComponent, vehicle.rudderTrimVar, -0.1, EntityVehicleF_Physics.MAX_RUDDER_TRIM);
                        text = "YAW TRIM";
                        break;
                    }
                }

                if (buttonVariable != null) {
                    //Need this to make complier shut up.
                    final ComputedVariable finalVar = buttonVariable;
                    newComponent = new GUIPanelButton(this, panelComponent) {
                        @Override
                        public void onClicked(boolean leftSide) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(finalVar));
                        }

                        @Override
                        public int getState() {
                            return finalVar.isActive ? 1 : 0;
                        }
                    };
                }
            }

            //Add label if we have a component and either default text or manually requested.
            if (newComponent != null) {
                if (panelComponent.text != null) {
                    text = panelComponent.text;
                }
                if (text != null) {
                    GUIComponentLabel label = new GUIComponentLabel(newComponent.constructedX + newComponent.width / 2, newComponent.constructedY + newComponent.height + 1, ColorRGB.WHITE, text, TextAlignment.CENTERED, 0.75F).setComponent(newComponent);
                    labels.add(label);
                    addComponent(label);
                }
            }
        }
        
        trailerSwitchDefs.clear();
        setupTowingButtons(vehicle);
        
        //Add instruments.  These go wherever they are specified in the JSON.
        for (int i = 0; i < vehicle.instruments.size(); ++i) {
            if (vehicle.instruments.get(i) != null && vehicle.definition.instruments.get(i).placeOnPanel) {
                addComponent(new GUIComponentInstrument(guiLeft, guiTop, vehicle, i));
            }
        }
        //Now add part instruments.
        for (APart part : vehicle.allParts) {
            for (int i = 0; i < part.instruments.size(); ++i) {
                if (part.instruments.get(i) != null && part.definition.instruments.get(i).placeOnPanel) {
                    addComponent(new GUIComponentInstrument(guiLeft, guiTop, part, i));
                }
            }
        }
    }

    @Override
    public void setStates() {
        for (GUIPanelButton button : componentButtons) {
            List<List<String>> visibilityVariables = button.component.visibilityVariables;
            if (visibilityVariables != null) {
                button.visible = vehicle.isVariableListTrue(visibilityVariables);
            }
            if(button.visible) {
                button.textureYOffset = (int) button.component.textureStart.y + button.component.height * button.getState();
            }
        }

        //Set the beaconBox text color depending on if we have an active beacon.
        if (beaconBox != null) {
            beaconBox.fontColor = vehicle.selectedBeacon != null ? ColorRGB.GREEN : ColorRGB.RED;
        }

        //Set label text colors.
        for (GUIComponentLabel label : labels) {
            ColorRGB color = getGUILightMode() == GUILightingMode.LIT ? definition.panel.litTextColor : definition.panel.textColor;
            label.color = color != null ? color : ColorRGB.WHITE;
        }
    }

    @Override
    protected GUILightingMode getGUILightMode() {
        return vehicle.renderTextLit() ? GUILightingMode.LIT : GUILightingMode.DARK;
    }

    @Override
    protected EntityVehicleF_Physics getGUILightSource() {
        return vehicle;
    }

    @Override
    protected boolean canStayOpen() {
        return super.canStayOpen() && vehicle.isValid;
    }

    @Override
    public int getWidth() {
        return definition.panel.backgroundWidth;
    }

    @Override
    public int getHeight() {
        return definition.panel.backgroundHeight;
    }

    @Override
    public boolean renderFlushBottom() {
        return true;
    }

    @Override
    public boolean renderTranslucent() {
        return true;
    }

    @Override
    protected String getTexture() {
        return vehicle.definition.motorized.panelTexture != null ? vehicle.definition.motorized.panelTexture : definition.panel.texture;
    }

    private PartEngine getEngineBySwitchIndex(int index) {
        int engineIndex = 0;
        JSONPartDefinition engineDef = null;
        for (JSONPartDefinition partDef : vehicle.definition.parts) {
            for (String partType : partDef.types) {
                if (partType.startsWith("engine_")) {
                    if (engineIndex++ == index) {
                        engineDef = partDef;
                    }
                    break;
                }
            }
            if (engineDef != null) {
                break;
            }
        }
        if (engineDef != null) {
            for (APart part : vehicle.parts) {
                if (part.placementDefinition == engineDef) {
                    return (PartEngine) part;
                }
            }
        } else {
            //Didn't find engineDef in main parts, check other parts.
            for (APart part : vehicle.parts) {
                if (part instanceof PartEngine && !vehicle.parts.contains(part)) {
                    if (engineIndex++ == index) {
                        return (PartEngine) part;
                    }
                }
            }
        }
        return null;
    }

    private void handleClickAction(JSONPanelClickAction action) {
    	ComputedVariable variable = vehicle.getVariable(action.variable);
        switch (action.action) {
            case INCREMENT: {
                if (variable.currentValue + action.value >= action.clampMin && variable.currentValue + action.value <= action.clampMax) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, action.value));
                }
                break;
            }
            case SET: {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(variable, action.value));
                break;
            }
            case TOGGLE: {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(variable));
                break;
            }
        }
    }

    private abstract class GUIPanelButton extends GUIComponentButton {
        protected final JSONPanelComponent component;

        private GUIPanelButton(AGUIBase gui, JSONPanelComponent component) {
            super(gui, guiLeft + (int) component.pos.x, guiTop + (int) component.pos.y, component.width, component.height, (int) component.textureStart.x, (int) component.textureStart.y, component.width, component.height);
            this.component = component;

            //Auto-add us when created to the appropriate objects.
            isDynamicTexture = true;
            componentButtons.add(this);
            addComponent(this);
        }

        public int getState() {
            return component.statusVariable != null && vehicle.getVariable(component.statusVariable).getValue() > 0 ? 1 : 0;
        }
    }

    private class GUIPanelEngineButton extends GUIPanelButton {
        private final PartEngine engine;

        private GUIPanelEngineButton(AGUIBase gui, JSONPanelComponent component, PartEngine engine) {
            super(gui, component);
            this.engine = engine;
        }

        @Override
        public void onClicked(boolean leftSide) {
            if (component.specialComponent == SpecialComponent.ENGINE_ON) {
                //Only one side, toggle magneto state.
                if (engine != null) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.magnetoVar));
                } else {
                    for (PartEngine engine : vehicle.engines) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.magnetoVar, vehicle.enginesOn ? 0 : 1));
                    }
                }
            } else if (component.specialComponent == SpecialComponent.ENGINE_START) {
                //Only one side, engage starter, but only if the magneto is on.
                if (engine != null ? engine.magnetoVar.isActive : vehicle.enginesOn) {
                    if (engine != null) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.electricStarterVar, 1));
                    } else {
                        for (PartEngine engine : vehicle.engines) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.electricStarterVar, 1));
                        }
                    }
                }
            } else {
                //Multi-control switch, check which side we clicked first.
                if (leftSide) {
                    //Left side.  Turn off magneto.
                    if (engine != null) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.magnetoVar, 0));
                    } else {
                        for (PartEngine engine : vehicle.engines) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.magnetoVar, 0));
                        }
                    }
                } else {
                    //Right side.  Either turn on magneto, or engage starter.
                    if (engine != null ? engine.magnetoVar.isActive : vehicle.enginesOn) {
                        //Magneto is on, engage starter.
                        if (engine != null) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.electricStarterVar, 1));
                        } else {
                            for (PartEngine engine : vehicle.engines) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.electricStarterVar, 1));
                            }
                        }
                    } else {
                        //Magneto is off, turn on.
                        if (engine != null) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.magnetoVar, 1));
                        } else {
                            for (PartEngine engine : vehicle.engines) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.magnetoVar, 1));
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onReleased() {
            if (component.specialComponent == SpecialComponent.ENGINE_CONTROL || component.specialComponent == SpecialComponent.ENGINE_START) {
                //Disengage electric starter if possible.
                if (engine != null) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.electricStarterVar, 0));
                } else {
                    for (PartEngine engine : vehicle.engines) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.electricStarterVar, 0));
                    }
                }
            }
        }

        @Override
        public int getState() {
            if (component.specialComponent == SpecialComponent.ENGINE_ON) {
                return (engine != null ? engine.magnetoVar.isActive : vehicle.enginesOn) ? 1 : 0;
            } else if (component.specialComponent == SpecialComponent.ENGINE_START) {
                if (engine != null) {
                    return engine.magnetoVar.isActive ? (engine.electricStarterVar.isActive ? 2 : 1) : 0;
                } else {
                    return vehicle.enginesOn ? (vehicle.enginesStarting ? 2 : 1) : 0;
                }
            } else {
                if (engine != null) {
                    return engine.electricStarterVar.isActive ? 2 : (engine.magnetoVar.isActive ? 1 : 0);
                } else {
                    return vehicle.enginesStarting ? 2 : (vehicle.enginesOn ? 1 : 0);
                }
            }
        }
    }

    private class GUIPanelTrimButton extends GUIPanelButton {
        private final ComputedVariable trimVariable;
        private final double leftIncrement;
        private final double bounds;
        private double trimIncrement;
        private boolean appliedTrimThisRender;
        private boolean trimCycleVar;

        private GUIPanelTrimButton(AGUIBase gui, JSONPanelComponent component, ComputedVariable trimVariable, double leftIncrement, double bounds) {
            super(gui, component);
            this.trimVariable = trimVariable;
            this.leftIncrement = leftIncrement;
            this.bounds = bounds;
        }

        @Override
        public void onClicked(boolean leftSide) {
            trimIncrement = leftSide ? leftIncrement : -leftIncrement;
        }

        @Override
        public void onReleased() {
            trimIncrement = 0;
        }

        @Override
        public int getState() {
            //Trim is sneaky and updates each frame the dial is held to allow for hold-action action.
            if (trimIncrement != 0 && inClockPeriod(3, 1)) {
                if (!appliedTrimThisRender) {
                    if (trimVariable.currentValue + trimIncrement > -bounds && trimVariable.currentValue + trimIncrement < bounds) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(trimVariable, trimIncrement, -bounds, bounds));
                        trimCycleVar = !trimCycleVar;
                    }
                    appliedTrimThisRender = true;
                }
            } else {
                appliedTrimThisRender = false;
            }
            return trimCycleVar ? 1 : 0;
        }
    }

    private static class SwitchEntry {
        protected final AEntityE_Interactable<?> connectionDefiner;
        protected final EntityVehicleF_Physics vehicleOn;
        protected final JSONConnectionGroup connectionGroup;
        protected final int connectionGroupIndex;

        private SwitchEntry(AEntityE_Interactable<?> connectionDefiner, JSONConnectionGroup connectionGroup) {
            this.connectionDefiner = connectionDefiner;
            this.vehicleOn = connectionDefiner instanceof APart ? ((APart) connectionDefiner).vehicleOn : (EntityVehicleF_Physics) connectionDefiner;
            this.connectionGroup = connectionGroup;
            this.connectionGroupIndex = connectionDefiner.definition.connectionGroups.indexOf(connectionGroup);
        }

        protected boolean isConnected() {
            if (connectionGroup.isHookup) {
                if (vehicleOn.towedByConnection != null && vehicleOn.towedByConnection.hookupGroupIndex == connectionGroupIndex) {
                    return true;
                }
            }
            if (connectionGroup.isHitch) {
                for (TowingConnection connection : vehicleOn.towingConnections) {
                    if (connectionDefiner.equals(connection.towingEntity) && connection.hitchGroupIndex == connectionGroupIndex) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
