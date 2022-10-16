package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.TowingConnection;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartPropeller;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.guis.components.GUIComponentSelector;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;

/**
 * A GUI/control system hybrid, this takes the place of the HUD when called up.
 * This class is abstract and contains the base code for rendering things common to
 * all vehicles, such as lights and engines.  Other things may be added as needed.
 *
 * @author don_bruce
 */
public abstract class AGUIPanel extends AGUIBase {
    protected static final int PANEL_WIDTH = 400;
    protected static final int PANEL_HEIGHT = 140;
    protected static final int GAP_BETWEEN_SELECTORS = 12;
    protected static final int SELECTOR_SIZE = 20;
    protected static final int SELECTOR_TEXTURE_SIZE = 20;
    protected static final int BEACON_TEXTURE_WIDTH_OFFSET = 340;
    protected static final int BEACON_TEXTURE_HEIGHT_OFFSET = 196;

    protected final EntityVehicleF_Physics vehicle;
    protected final boolean haveReverseThrustOption;
    protected final List<SwitchEntry> trailerSwitchDefs = new ArrayList<>();
    protected int xOffset;

    public AGUIPanel(EntityVehicleF_Physics vehicle) {
        super();
        this.vehicle = vehicle;
        //If we have propellers with reverse thrust capabilities, or are a blimp, or have jet engines, render the reverse thrust selector.
        if (vehicle.definition.motorized.isBlimp) {
            haveReverseThrustOption = true;
        } else {
            boolean foundReversingPart = false;
            for (APart part : vehicle.parts) {
                if (part instanceof PartPropeller) {
                    if (part.definition.propeller.isDynamicPitch) {
                        foundReversingPart = true;
                        break;
                    }
                } else if (part instanceof PartEngine && part.definition.engine.jetPowerFactor > 0) {
                    foundReversingPart = true;
                    break;
                }
            }
            haveReverseThrustOption = foundReversingPart;
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
        trailerSwitchDefs.clear();
        setupTowingButtons(vehicle);

        //Tracking variable for how far to the left we are rendering things.
        //This allows for things to be on different columns depending on vehicle configuration.
        //We make this method final and create an abstract method to use instead of this one for
        //setting up any extra components.
        xOffset = (int) (1.25D * GAP_BETWEEN_SELECTORS);

        //Add light selectors.  These are on the left-most side of the panel.
        setupLightComponents(guiLeft, guiTop);
        xOffset += GAP_BETWEEN_SELECTORS + SELECTOR_SIZE;

        //Add engine selectors.  These are to the right of the light switches.
        setupEngineComponents(guiLeft, guiTop);
        xOffset += GAP_BETWEEN_SELECTORS + SELECTOR_SIZE;

        //Add general selectors.  These are panel-specific, and to the right of the engine selectors.
        setupGeneralComponents(guiLeft, guiTop);
        xOffset += GAP_BETWEEN_SELECTORS + SELECTOR_SIZE;

        //Add custom selectors.  These are vehicle-specific, and their placement is panel-specific.
        //These are rendered to the right of the general selectors.
        setupCustomComponents(guiLeft, guiTop);

        //Add instruments.  These go wherever they are specified in the JSON.
        for (int i = 0; i < vehicle.instruments.size(); ++i) {
            if (vehicle.instruments.get(i) != null && vehicle.definition.instruments.get(i).placeOnPanel) {
                addComponent(new GUIComponentInstrument(guiLeft, guiTop, vehicle, i));
            }
        }
        //Now add part instruments.
        for (APart part : vehicle.parts) {
            for (int i = 0; i < part.instruments.size(); ++i) {
                if (part.instruments.get(i) != null && part.definition.instruments.get(i).placeOnPanel) {
                    addComponent(new GUIComponentInstrument(guiLeft, guiTop, part, i));
                }
            }
        }
    }

    protected abstract void setupLightComponents(int guiLeft, int guiTop);

    protected abstract void setupEngineComponents(int guiLeft, int guiTop);

    protected abstract void setupGeneralComponents(int guiLeft, int guiTop);

    protected abstract void setupCustomComponents(int guiLeft, int guiTop);

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
        return PANEL_WIDTH;
    }

    @Override
    public int getHeight() {
        return PANEL_HEIGHT;
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
        return vehicle.definition.motorized.panelTexture != null ? vehicle.definition.motorized.panelTexture : "mts:textures/guis/panel.png";
    }

    @Override
    public void close() {
        super.close();
        //Turn starters off.  This prevents stuck engine starters.
        vehicle.engines.forEach(engine -> {
            if (engine.electricStarterEngaged) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.ELECTRIC_STARTER_VARIABLE));
            }
        });
    }

    protected static class SwitchEntry {
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

        protected void updateSelectorState(GUIComponentSelector trailerSelector) {
            trailerSelector.selectorState = 1;
            if (connectionGroup.isHookup) {
                if (vehicleOn.towedByConnection != null && vehicleOn.towedByConnection.hookupGroupIndex == connectionGroupIndex) {
                    trailerSelector.selectorState = 0;
                    return;
                }
            }
            if (connectionGroup.isHitch) {
                for (TowingConnection connection : vehicleOn.towingConnections) {
                    if (connectionDefiner.equals(connection.towingEntity) && connection.hitchGroupIndex == connectionGroupIndex) {
                        trailerSelector.selectorState = 0;
                        return;
                    }
                }
            }
        }
    }
}
