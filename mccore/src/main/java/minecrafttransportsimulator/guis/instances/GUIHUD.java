package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentInstrument;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboard;

/**
 * A GUI that is used to render the HUG.  This is used in {@link GUIInstruments}
 * as well as the {@link InterfaceManager.renderingInterface} to render the HUD.  Note that when
 * the HUD is rendered in the vehicle it will NOT inhibit key inputs as the
 * HUD there is designed to be an overlay rather than an actual GUI.
 *
 * @author don_bruce
 */
public class GUIHUD extends AGUIBase {
    private static final int HUD_WIDTH = 400;
    private static final int HUD_HEIGHT = 140;
    private static final ControlsKeyboard[] customKeybindControls = new ControlsKeyboard[] { ControlsKeyboard.GENERAL_CUSTOM1, ControlsKeyboard.GENERAL_CUSTOM2, ControlsKeyboard.GENERAL_CUSTOM3, ControlsKeyboard.GENERAL_CUSTOM4 };

    private final EntityVehicleF_Physics vehicle;
    private final PartSeat seat;
    private final List<GUIComponentInstrument> instruments = new ArrayList<>();
    private GUIComponentLabel healthLabel;
    private GUIComponentLabel gunTypeLabel;
    private Map<Byte, GUIComponentLabel> customKeybindLabels = new HashMap<>();
    private Map<Byte, Set<String>> customKeybindNames = new HashMap<>();

    private boolean halfHUDActive;

    public GUIHUD(EntityVehicleF_Physics vehicle, PartSeat seat) {
        super();
        this.vehicle = vehicle;
        this.seat = seat;
        this.halfHUDActive = vehicle.definition.motorized.halfHUDOnly || (!vehicle.definition.motorized.fullHUDOnly && (InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON ? !ConfigSystem.client.renderingSettings.fullHUD_1P.value : !ConfigSystem.client.renderingSettings.fullHUD_3P.value));
    }

    @Override
    public void setupComponents() {
        //Only show instruments and background if we are a controller.
        if (seat.placementDefinition.isController) {
            //Need to adjust GUITop if we are a half hud.  This makes everything go down 1/4 height.
            if (halfHUDActive) {
                guiTop += getHeight() / 2;
            }
            super.setupComponents();

            //Add instruments.  These go wherever they are specified in the JSON.
            instruments.clear();
            for (int i = 0; i < vehicle.instruments.size(); ++i) {
                if (vehicle.instruments.get(i) != null && !vehicle.definition.instruments.get(i).placeOnPanel) {
                    GUIComponentInstrument instrument = new GUIComponentInstrument(guiLeft, guiTop, vehicle, i);
                    instruments.add(instrument);
                    addComponent(instrument);
                }
            }
            //Now add part instruments.
            for (APart part : vehicle.parts) {
                for (int i = 0; i < part.instruments.size(); ++i) {
                    if (part.instruments.get(i) != null && !part.definition.instruments.get(i).placeOnPanel) {
                        GUIComponentInstrument instrument = new GUIComponentInstrument(guiLeft, guiTop, part, i);
                        instruments.add(instrument);
                        addComponent(instrument);
                    }
                }
            }

            //Set top back to normal.
            if (halfHUDActive) {
                guiTop -= getHeight() / 2;
            }
        } else {
            super.setupComponents();
        }

        //Add labels.
        addComponent(healthLabel = new GUIComponentLabel(screenWidth, 0, ColorRGB.WHITE, "", TextAlignment.RIGHT_ALIGNED, 1.0F));
        healthLabel.ignoreGUILightingState = true;
        addComponent(gunTypeLabel = new GUIComponentLabel(screenWidth, 8, ColorRGB.WHITE, "", TextAlignment.RIGHT_ALIGNED, 1.0F));
        gunTypeLabel.ignoreGUILightingState = true;
        customKeybindLabels.clear();
        populateKeybindLabel(vehicle);
        vehicle.allParts.forEach(part -> populateKeybindLabel(part));
    }

    private void populateKeybindLabel(AEntityE_Interactable<?> entity) {
        if (entity.definition.customKeybinds != null) {
            entity.definition.customKeybinds.forEach(customKeybind -> {
                if (!customKeybindLabels.containsKey(customKeybind.keyIndex)) {
                    GUIComponentLabel keybindLabel = new GUIComponentLabel(screenWidth, 16 + 8 * customKeybindLabels.size(), ColorRGB.WHITE, "" + customKeybind.name, TextAlignment.RIGHT_ALIGNED, 1.0F);
                    addComponent(keybindLabel);
                    customKeybindLabels.put(customKeybind.keyIndex, keybindLabel);
                }
                customKeybindNames.computeIfAbsent(customKeybind.keyIndex, f -> new HashSet<>()).add(customKeybind.name);
            });
        }
    }

    @Override
    public void setStates() {
        //Check to see if HUD setting changed.  If so, we need to re-create our components.
        //Do this before doing anything else.
        if (halfHUDActive ^ (vehicle.definition.motorized.halfHUDOnly || (!vehicle.definition.motorized.fullHUDOnly && (InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON ? !ConfigSystem.client.renderingSettings.fullHUD_1P.value : !ConfigSystem.client.renderingSettings.fullHUD_3P.value)))) {
            halfHUDActive = !halfHUDActive;
            setupComponents();
        }

        super.setStates();
        //Set all instrument invisible if we're not rendering the main HUD.
        //Otherwise, set them all visible.
        for (GUIComponentInstrument instrument : instruments) {
            instrument.visible = CameraSystem.customCameraOverlay == null && seat.placementDefinition.isController && (InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON ? ConfigSystem.client.renderingSettings.renderHUD_1P.value : ConfigSystem.client.renderingSettings.renderHUD_3P.value);
        }

        //Set health label text and visibility.
        healthLabel.text = String.format("Health: %d/%d", (int) Math.ceil(vehicle.definition.general.health - vehicle.damageVar.currentValue), vehicle.definition.general.health);
        healthLabel.visible = seat.placementDefinition.isController || seat.canControlGuns;
        healthLabel.color = vehicle.outOfHealth ? ColorRGB.RED : ColorRGB.WHITE;

        //Set gun label text, if we are in a seat that has one.
        //If we are in a seat controlling a gun, render a text line for it.
        if (seat.canControlGuns && !InterfaceManager.clientInterface.isChatOpen()) {
            gunTypeLabel.visible = true;
            gunTypeLabel.text = "Active Gun: ";
            if (seat.activeGunItem != null) {
                gunTypeLabel.text += seat.activeGunItem.getItemName() + (seat.activeGunItem.definition.gun.fireSolo ? " [" + (seat.gunIndex + 1) + "]" : "");
            } else {
                gunTypeLabel.text += "None";
            }
        } else {
            gunTypeLabel.visible = false;
        }

        //Set custom keybind text.
        customKeybindLabels.entrySet().forEach(entry -> {
            byte keyIndex = entry.getKey();
            GUIComponentLabel label = entry.getValue();
            label.text = InterfaceManager.inputInterface.getNameForKeyCode(customKeybindControls[keyIndex - 1].config.keyCode) + "->";
            customKeybindNames.get(keyIndex).forEach(name -> {
                if (!label.text.endsWith(">")) {
                    label.text += ", ";
                }
                label.text += name;
            });
        });
    }

    @Override
    protected boolean renderBackground() {
        return CameraSystem.customCameraOverlay == null && seat.placementDefinition.isController && (InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON ? (ConfigSystem.client.renderingSettings.renderHUD_1P.value && !ConfigSystem.client.renderingSettings.transpHUD_1P.value) : (ConfigSystem.client.renderingSettings.renderHUD_3P.value && !ConfigSystem.client.renderingSettings.transpHUD_3P.value));
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
    public boolean capturesPlayer() {
        return false;
    }

    @Override
    protected boolean canStayOpen() {
        return super.canStayOpen() && seat.rider != null;
    }

    @Override
    public int getWidth() {
        return HUD_WIDTH;
    }

    @Override
    public int getHeight() {
        return HUD_HEIGHT;
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
        return vehicle.definition.motorized.hudTexture != null ? vehicle.definition.motorized.hudTexture : "mts:textures/guis/hud.png";
    }
}
