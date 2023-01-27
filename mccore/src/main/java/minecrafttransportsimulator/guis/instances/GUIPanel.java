package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
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
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.jsondefs.JSONPanel;
import minecrafttransportsimulator.jsondefs.JSONPanel.JSONPanelClickAction;
import minecrafttransportsimulator.jsondefs.JSONPanel.JSONPanelComponent;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketVehicleBeaconChange;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * A GUI/control system hybrid, this takes the place of the HUD when called up.
 * This class is abstract and contains the base code for rendering things common to
 * all vehicles, such as lights and engines.  Other things may be added as needed.
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

        JSONPanel testDef;
        try {
            String packID = vehicle.definition.motorized.panel.substring(0, vehicle.definition.motorized.panel.indexOf(':'));
            String systemName = vehicle.definition.motorized.panel.substring(packID.length() + 1);
            testDef = PackParser.getPackPanel(packID, systemName);
        } catch (Exception e) {
            InterfaceManager.clientInterface.getClientPlayer().displayChatMessage(JSONConfigLanguage.SYSTEM_DEBUG, "Could not display the requested panel " + vehicle.definition.motorized.panel + ". Report this to the pack author!  Default panel will be used.");
            if (vehicle.definition.motorized.isAircraft) {
                testDef = PackParser.getPackPanel(InterfaceManager.coreModID, "default_plane");
            } else {
                testDef = PackParser.getPackPanel(InterfaceManager.coreModID, "default_car");
            }
        }
        this.definition = testDef;
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
        for (APart part : entity.parts) {
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

        int engineIndex = 0;
        int trailerIndex = 0;
        int customVariableIndex = 0;
        for (JSONPanelComponent panelComponent : definition.panel.components) {
            AGUIComponent newComponent = null;
            String text = null;
            if (panelComponent.specialComponent == null) {
                newComponent = new GUIPanelButton(panelComponent) {
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

                    @Override
                    public int getState() {
                        return panelComponent.statusVariable != null && vehicle.isVariableActive(panelComponent.statusVariable) ? 1 : 0;
                    }
                };
                text = panelComponent.text;
            } else {
                String buttonVariable = null;
                switch (panelComponent.specialComponent) {
                    case CAR_LIGHT: {
                        if (vehicle.definition.motorized.hasRunningLights || vehicle.definition.motorized.hasHeadlights) {
                            newComponent = new GUIPanelButton(panelComponent) {
                                @Override
                                public void onClicked(boolean leftSide) {
                                    if (leftSide) {
                                        if (vehicle.isVariableActive(EntityVehicleF_Physics.HEADLIGHT_VARIABLE)) {
                                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.HEADLIGHT_VARIABLE));
                                        } else if (vehicle.isVariableActive(EntityVehicleF_Physics.RUNNINGLIGHT_VARIABLE)) {
                                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.RUNNINGLIGHT_VARIABLE));
                                        }
                                    } else {
                                        if (vehicle.definition.motorized.hasRunningLights && !vehicle.isVariableActive(EntityVehicleF_Physics.RUNNINGLIGHT_VARIABLE)) {
                                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.RUNNINGLIGHT_VARIABLE));
                                        } else if (vehicle.definition.motorized.hasHeadlights && !vehicle.isVariableActive(EntityVehicleF_Physics.HEADLIGHT_VARIABLE)) {
                                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.HEADLIGHT_VARIABLE));
                                        }
                                    }
                                }

                                @Override
                                public int getState() {
                                    return vehicle.isVariableActive(EntityVehicleF_Physics.HEADLIGHT_VARIABLE) ? 2 : (vehicle.isVariableActive(EntityVehicleF_Physics.RUNNINGLIGHT_VARIABLE) ? 1 : 0);
                                }
                            };
                            text = "LIGHTS";
                        }
                        break;
                    }
                    case TURN_SIGNAL: {
                        if (vehicle.definition.motorized.hasTurnSignals) {
                            newComponent = new GUIPanelButton(panelComponent) {
                                @Override
                                public void onClicked(boolean leftSide) {
                                    if (leftSide) {
                                        vehicle.toggleVariable(EntityVehicleF_Physics.LEFTTURNLIGHT_VARIABLE);
                                    } else {
                                        vehicle.toggleVariable(EntityVehicleF_Physics.RIGHTTURNLIGHT_VARIABLE);
                                    }
                                }

                                @Override
                                public int getState() {
                                    //This makes the indicator blink when on.
                                    if (inClockPeriod(20, 10)) {
                                        int returnValue = 0;
                                        if (vehicle.isVariableActive(EntityVehicleF_Physics.LEFTTURNLIGHT_VARIABLE)) {
                                            returnValue += 1;
                                        }
                                        if (vehicle.isVariableActive(EntityVehicleF_Physics.RIGHTTURNLIGHT_VARIABLE)) {
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
                        buttonVariable = EntityVehicleF_Physics.NAVIGATIONLIGHT_VARIABLE;
                        text = "NAV";
                        break;
                    }
                    case STROBE_LIGHT: {
                        buttonVariable = EntityVehicleF_Physics.STROBELIGHT_VARIABLE;
                        text = "STROBE";
                        break;
                    }
                    case TAXI_LIGHT: {
                        buttonVariable = EntityVehicleF_Physics.TAXILIGHT_VARIABLE;
                        text = "TAXI";
                        break;
                    }
                    case LANDING_LIGHT: {
                        buttonVariable = EntityVehicleF_Physics.LANDINGLIGHT_VARIABLE;
                        text = "LAND";
                        break;
                    }
                    case ENGINE_CONTROL: {
                        if (!vehicle.engines.isEmpty()) {
                            PartEngine engine;
                            if (vehicle.definition.motorized.hasSingleEngineControl) {
                                if (engineIndex == 1) {
                                    //Don't make more than one single-engine switch.
                                    break;
                                } else {
                                    engine = null;
                                    engineIndex = 1;
                                }
                                text = "ENGINES";
                            } else if (engineIndex < vehicle.engines.size()) {
                                engine = vehicle.engines.get(engineIndex++);
                                if (engine == null) {
                                    break;
                                }
                                text = "ENGINE " + engineIndex;
                            } else {
                                break;
                            }
                            newComponent = new GUIPanelEngineButton(panelComponent, engine, true, true);
                        }
                        break;
                    }
                    case ENGINE_ON: {
                        if (!vehicle.engines.isEmpty()) {
                            PartEngine engine;
                            if (vehicle.definition.motorized.hasSingleEngineControl) {
                                if (engineIndex == 1) {
                                    //Don't make more than one single-engine switch.
                                    break;
                                } else {
                                    engine = null;
                                    engineIndex = 1;
                                }
                                text = "MAG";
                            } else {
                                engine = vehicle.engines.get(engineIndex++);
                                if (engine == null) {
                                    break;
                                }
                                text = "MAG " + engineIndex;
                            }
                            newComponent = new GUIPanelEngineButton(panelComponent, engine, true, false);
                        }
                        break;
                    }
                    case ENGINE_START: {
                        if (!vehicle.engines.isEmpty()) {
                            PartEngine engine;
                            if (vehicle.definition.motorized.hasSingleEngineControl) {
                                if (engineIndex == 1) {
                                    //Don't make more than one single-engine switch.
                                    break;
                                } else {
                                    engine = null;
                                    engineIndex = 1;
                                }
                                text = "START";
                            } else {
                                engine = vehicle.engines.get(engineIndex++);
                                if (engine == null) {
                                    break;
                                }
                                text = "START " + engineIndex;
                            }
                            newComponent = new GUIPanelEngineButton(panelComponent, engine, false, true);
                        }
                        break;
                    }
                    case TRAILER: {
                        if (trailerSwitchDefs.size() > trailerIndex) {
                            SwitchEntry switchDef = trailerSwitchDefs.get(trailerIndex++);
                            text = switchDef.connectionGroup.groupName;
                            newComponent = new GUIPanelButton(panelComponent) {
                                @Override
                                public void onClicked(boolean leftSide) {
                                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(switchDef.connectionDefiner, AEntityG_Towable.TOWING_CONNECTION_REQUEST_VARIABLE, switchDef.connectionGroupIndex + 1));
                                }

                                @Override
                                public int getState() {
                                    return switchDef.isConnected() ? 1 : 0;
                                }
                            };
                        }
                        break;
                    }
                    case CUSTOM_VARIABLE: {
                        if (customVariables.size() > customVariableIndex) {
                            String customVariable = customVariables.get(customVariableIndex++);
                            buttonVariable = customVariable;
                            text = customVariable;
                        }
                        break;
                    }
                    case BEACON_BOX: {
                        if (vehicle.definition.motorized.hasRadioNav || ConfigSystem.settings.general.allPlanesWithNav.value) {
                            beaconBox = new GUIComponentTextBox(guiLeft + (int) panelComponent.pos.x, guiTop + (int) panelComponent.pos.y, panelComponent.width, panelComponent.height, vehicle.selectedBeaconName, ColorRGB.WHITE, 5, (int) panelComponent.textureStart.x, (int) panelComponent.textureStart.y, panelComponent.width, panelComponent.height) {
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
                }

                if (buttonVariable != null) {
                    //Need this to make complier shut up.
                    final String finalVar = buttonVariable;
                    newComponent = new GUIPanelButton(panelComponent) {
                        @Override
                        public void onClicked(boolean leftSide) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, finalVar));
                        }

                        @Override
                        public int getState() {
                            return vehicle.isVariableActive(finalVar) ? 1 : 0;
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
            button.textureYOffset = (int) button.component.textureStart.y + button.component.height * button.getState();
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

    private void handleClickAction(JSONPanelClickAction action) {
        switch (action.action) {
            case INCREMENT: {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(vehicle, action.variable, action.value));
                break;
            }
            case SET: {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle, action.variable, action.value));
                break;
            }
            case TOGGLE: {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, action.variable));
                break;
            }
        }
    }

    private abstract class GUIPanelButton extends GUIComponentButton {
        private final JSONPanelComponent component;

        private GUIPanelButton(JSONPanelComponent component) {
            super(guiLeft + (int) component.pos.x, guiTop + (int) component.pos.y, component.width, component.height, (int) component.textureStart.x, (int) component.textureStart.y, component.width, component.height);
            this.component = component;

            //Auto-add us when created to the appropriate objects.
            isDynamicTexture = true;
            componentButtons.add(this);
            addComponent(this);
        }

        public int getState() {
            return component.statusVariable != null ? (int) vehicle.getCleanRawVariableValue(component.statusVariable, 0) : 0;
        }
    }

    private class GUIPanelEngineButton extends GUIPanelButton {
        private final PartEngine engine;
        private final boolean allowMagneto;
        private final boolean allowStarter;

        private GUIPanelEngineButton(JSONPanelComponent component, PartEngine engine, boolean allowMagneto, boolean allowStarter) {
            super(component);
            this.engine = engine;
            this.allowMagneto = allowMagneto;
            this.allowStarter = allowStarter;
        }

        @Override
        public void onClicked(boolean leftSide) {
            if ((allowMagneto && !allowStarter) || (allowMagneto && leftSide)) {
                //Left side on multi clicked or any side on single switch.
                //Turn magneto off, if we haven't already.
                if (engine != null) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine, PartEngine.MAGNETO_VARIABLE, 0));
                } else {
                    for (PartEngine engine : vehicle.engines) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine, PartEngine.MAGNETO_VARIABLE, 0));
                    }
                }
            } else if ((allowStarter && !allowMagneto) || (allowStarter && !leftSide)) {
                //Right side on multi clicked or any side on single switch.
                //Either turn magneto on, or engage electric stater.
                if (engine != null) {
                    if (!engine.magnetoOn && allowMagneto) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine, PartEngine.MAGNETO_VARIABLE, 1));
                    } else if (!engine.definition.engine.disableAutomaticStarter && allowStarter) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine, PartEngine.ELECTRIC_STARTER_VARIABLE, 1));
                    }
                } else {
                    if (!vehicle.enginesOn && allowMagneto) {
                        for (PartEngine engine : vehicle.engines) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine, PartEngine.MAGNETO_VARIABLE, 1));
                        }
                    } else if (vehicle.enginesOn && allowStarter) {
                        for (PartEngine engine : vehicle.engines) {
                            if (!engine.definition.engine.disableAutomaticStarter) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine, PartEngine.ELECTRIC_STARTER_VARIABLE, 1));
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onReleased() {
            if (allowStarter) {
                //Disengage electric starter if possible.
                if (engine != null) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine, PartEngine.ELECTRIC_STARTER_VARIABLE, 0));
                } else {
                    for (PartEngine engine : vehicle.engines) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine, PartEngine.ELECTRIC_STARTER_VARIABLE, 0));
                    }
                }
            }
        }

        @Override
        public int getState() {
            if (allowMagneto && !allowStarter) {
                return (engine != null ? engine.magnetoOn : vehicle.enginesOn) ? 1 : 0;
            } else if (!allowMagneto && allowStarter) {
                return (engine != null ? engine.electricStarterEngaged : vehicle.enginesStarting) ? 1 : 0;
            } else {
                if (engine != null) {
                    return engine.electricStarterEngaged ? 2 : (engine.magnetoOn ? 1 : 0);
                } else {
                    return vehicle.enginesStarting ? 2 : (vehicle.enginesOn ? 1 : 0);
                }
            }
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
