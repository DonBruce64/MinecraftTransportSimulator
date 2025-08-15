package minecrafttransportsimulator.systems;

import java.util.Locale;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.EntityInteractResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlacedPart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIPanel;
import minecrafttransportsimulator.guis.instances.GUIRadio;
import minecrafttransportsimulator.jsondefs.JSONConfigClient.ConfigJoystick;
import minecrafttransportsimulator.jsondefs.JSONConfigClient.ConfigKeyboard;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityCameraChange;
import minecrafttransportsimulator.packets.instances.PacketEntityCustomKeypress;
import minecrafttransportsimulator.packets.instances.PacketEntityInteract;
import minecrafttransportsimulator.packets.instances.PacketEntityInteractGUI;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.packets.instances.PacketPartSeat;
import minecrafttransportsimulator.packets.instances.PacketPartSeat.SeatAction;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlNotification;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

/**
 * Class that handles all control operations.
 *
 * @author don_bruce
 */
public final class ControlSystem {
    private static final int NULL_COMPONENT = 999;
    private static boolean joysticksInhibited = false;
    private static IWrapperPlayer clientPlayer;

    private static boolean clickingLeft = false;
    private static boolean clickingRight = false;

    private static double throttleRequestLastCheck;
    private static double brakeRequestLastCheck;

    private static EntityInteractResult interactResult = null;

    /**
     * Static initializer for the IWrapper inputs, as we need to iterate through the enums to initialize them
     * prior to using them in any of the methods contained in this IWrapper (cause they'll be null).
     * Joystick enums need to come first, as the Keyboard enums take them as constructor args.
     * After we initialize the keboard enums, we set their default values.
     * Once all this is done, save the results back to the disk to ensure the systems are synced.
     * Note that since this class won't be called until the world loads because we won't process inputs
     * out-of-world, it can be assumed that the ConfigSystem has already been initialized.
     */
    static {
        for (ControlsJoystick control : ControlsJoystick.values()) {
            ConfigSystem.client.controls.joystick.put(control.systemName, control.config);
        }
        for (ControlsKeyboard control : ControlsKeyboard.values()) {
            ConfigSystem.client.controls.keyboard.put(control.systemName, control.config);
        }
        for (ControlsKeyboard control : ControlsKeyboard.values()) {
            if (control.config.keyCode <= 0) {
                control.config.keyCode = InterfaceManager.inputInterface.getKeyCodeForName(control.defaultKeyName);
            }
        }
        ConfigSystem.saveToDisk();
    }

    public static void controlGlobal(IWrapperPlayer player) {
        EntityPlayerGun playerGun = EntityPlayerGun.playerClientGuns.get(player.getID());
        if (InterfaceManager.inputInterface.isLeftMouseButtonDown()) {
            if (!clickingLeft) {
                clickingLeft = true;
                handleClick(player, playerGun, true, false, false, false);
            }
        } else if (clickingLeft) {
            clickingLeft = false;
            handleClick(player, playerGun, false, true, false, false);
        }
        if (InterfaceManager.inputInterface.isRightMouseButtonDown()) {
            if (!clickingRight) {
                clickingRight = true;
                handleClick(player, playerGun, false, false, true, false);
            }
        } else if (clickingRight) {
            clickingRight = false;
            handleClick(player, playerGun, false, false, false, true);
        }

        if (playerGun != null && playerGun.activeGun != null && !InterfaceManager.clientInterface.isGUIOpen() && ControlsKeyboard.GENERAL_RELOAD.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, PacketPartGun.Request.RELOAD_HAND));
        }
    }

    private static void handleClick(IWrapperPlayer player, EntityPlayerGun playerGun, boolean leftClickDown, boolean leftClickUp, boolean rightClickDown, boolean rightClickUp) {
        //Either change the gun trigger state (if we are holding a gun),
        //or try to interact with entities if we are not.
        if (playerGun != null && playerGun.activeGun != null) {
            if (leftClickDown) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, PacketPartGun.Request.TRIGGER_ON));
            } else if (leftClickUp) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, PacketPartGun.Request.TRIGGER_OFF));
            } else if (rightClickDown) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, PacketPartGun.Request.AIM_ON));
            } else if (rightClickUp) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, PacketPartGun.Request.AIM_OFF));
            }
        }
        if (leftClickDown || rightClickDown) {
            Point3D startPosition = player.getEyePosition();
            Point3D endPosition = player.getLineOfSight(3.5).add(startPosition);

            interactResult = player.getWorld().getMultipartEntityIntersect(startPosition, endPosition);
            if (interactResult != null) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(interactResult.entity, player, interactResult.box, leftClickDown, rightClickDown));
            }
        } else if (interactResult != null) {
            //Fire off un-click to entity last clicked.
            InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(interactResult.entity, player, interactResult.box, false, false));
            interactResult = null;
        }
    }

    public static void controlMultipart(AEntityF_Multipart<?> multipart, boolean isPlayerController) {
        clientPlayer = InterfaceManager.clientInterface.getClientPlayer();
        if (multipart instanceof EntityVehicleF_Physics) {
            EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) multipart;
            if (vehicle.definition.motorized.isAircraft) {
                controlAircraft(vehicle, isPlayerController);
            } else {
                controlGroundVehicle(vehicle, isPlayerController);
            }
        } else if (multipart instanceof EntityPlacedPart) {
            controlCamera(ControlsKeyboard.CAR_ZOOM_I, ControlsKeyboard.CAR_ZOOM_O, ControlsKeyboard.CAR_CHANGEVIEW, ControlsJoystick.CAR_LOOK_UD, ControlsJoystick.CAR_LOOK_LR);
            rotateCamera(ControlsJoystick.CAR_LOOK_R, ControlsJoystick.CAR_LOOK_L, ControlsJoystick.CAR_LOOK_U, ControlsJoystick.CAR_LOOK_D, ControlsJoystick.CAR_LOOK_A);
            controlGun(multipart, ControlsKeyboard.CAR_GUN_FIRE, ControlsKeyboard.CAR_GUN_SWITCH);
        }

        if (ControlsKeyboard.GENERAL_CUSTOM1.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 1, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM1.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 1, false));
        }
        if (ControlsKeyboard.GENERAL_CUSTOM2.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 2, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM2.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 2, false));
        }
        if (ControlsKeyboard.GENERAL_CUSTOM3.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 3, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM3.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 3, false));
        }
        if (ControlsKeyboard.GENERAL_CUSTOM4.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 4, true));
        } else if (ControlsKeyboard.GENERAL_CUSTOM4.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityCustomKeypress(multipart, 4, false));
        }
    }

    private static void controlCamera(ControlsKeyboard zoomIn, ControlsKeyboard zoomOut, ControlsKeyboard changeView, ControlsJoystick viewUD, ControlsJoystick viewLR) {
        AEntityB_Existing riding = clientPlayer.getEntityRiding();
        if (riding instanceof PartSeat) {
            PartSeat sittingSeat = (PartSeat) riding;
            if (zoomIn.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartSeat(sittingSeat, SeatAction.ZOOM_IN));
            }
            if (zoomOut.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketPartSeat(sittingSeat, SeatAction.ZOOM_OUT));
            }
            if (changeView.isPressed()) {
            	InterfaceManager.packetInterface.sendToServer(new PacketEntityCameraChange(sittingSeat));
            }
            if (!(viewLR.isJoystickActive() || viewUD.isJoystickActive())) {
                riding.hasHeadTracking = false;
                riding.headTrackingOrientation.set(0, 0, 0);
            } else {
                riding.hasHeadTracking = true;
                riding.headTrackingOrientation.x = -(viewUD.getAxisState(true) - 0.5) * 170;
                riding.headTrackingOrientation.y = -(viewLR.getAxisState(true) - 0.5) * 180;
            }
        }
    }

    private static void rotateCamera(ControlsJoystick lookR, ControlsJoystick lookL, ControlsJoystick lookU, ControlsJoystick lookD, ControlsJoystick lookA) {
        //TODO this causes yaw de-syncs.
        if (lookR.isPressed()) {
            clientPlayer.setYaw(clientPlayer.getYaw() - 3);
        }
        if (lookL.isPressed()) {
            clientPlayer.setYaw(clientPlayer.getYaw() + 3);
        }
        if (lookU.isPressed()) {
            clientPlayer.setPitch(clientPlayer.getPitch() - 3);
        }
        if (lookD.isPressed()) {
            clientPlayer.setPitch(clientPlayer.getPitch() + 3);
        }

        float pollData = lookA.getMultistateValue();
        if (pollData != 0) {
            if (pollData >= 0.125F && pollData <= 0.375F) {
                clientPlayer.setPitch(clientPlayer.getPitch() + 3);
            }
            if (pollData >= 0.375F && pollData <= 0.625F) {
                clientPlayer.setYaw(clientPlayer.getYaw() - 3);
            }
            if (pollData >= 0.625F && pollData <= 0.875F) {
                clientPlayer.setPitch(clientPlayer.getPitch() - 3);
            }
            if (pollData >= 0.875F || pollData <= 0.125F) {
                clientPlayer.setYaw(clientPlayer.getYaw() + 3);
            }
        }
    }

    private static void controlBrake(EntityVehicleF_Physics vehicle, ControlsJoystick joystickBrakeAxis, ControlsJoystick joystickBrakeButton, ControlsKeyboard keyboardBrakeButton, ControlsKeyboard parkingBrakeButton) {
        if (parkingBrakeButton.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle.parkingBrakeVar));
        }
        double brakeValue = joystickBrakeAxis.isJoystickActive() ? joystickBrakeAxis.getAxisState(true) : ((joystickBrakeButton.isPressed() || keyboardBrakeButton.isPressed()) ? EntityVehicleF_Physics.MAX_BRAKE : 0);
        if (brakeValue != brakeRequestLastCheck) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle.brakeVar, brakeValue));
        }
        brakeRequestLastCheck = brakeValue;
    }

    private static void controlGun(AEntityF_Multipart<?> multipart, ControlsKeyboard gunTrigger, ControlsKeyboard gunSwitch) {
        boolean gunSwitchPressedThisScan = gunSwitch.isPressed();
        for (APart part : multipart.allParts) {
            if (part instanceof PartGun) {
                PartGun gun = (PartGun) part;
                if (clientPlayer.equals(gun.getGunController())) {
                    if (gunTrigger.isPressed()) {
                        InterfaceManager.packetInterface.sendToServer(new PacketPartGun(gun, PacketPartGun.Request.TRIGGER_ON));
                    } else {
                        InterfaceManager.packetInterface.sendToServer(new PacketPartGun(gun, PacketPartGun.Request.TRIGGER_OFF));
                    }
                }
            } else if (part instanceof PartSeat) {
                if (gunSwitchPressedThisScan) {
                    if (clientPlayer.equals(part.rider)) {
                        InterfaceManager.packetInterface.sendToServer(new PacketPartSeat((PartSeat) part, SeatAction.CHANGE_GUN));
                    }
                }
            }
        }
    }

    private static void controlPanel(EntityVehicleF_Physics vehicle, ControlsKeyboard panel) {
        if (panel.isPressed()) {
            if (vehicle.canPlayerStartEngines(clientPlayer)) {
                if (AGUIBase.activeInputGUI instanceof GUIPanel && !AGUIBase.activeInputGUI.editingText) {
                    AGUIBase.activeInputGUI.close();
                } else if (!InterfaceManager.clientInterface.isGUIOpen()) {
                    new GUIPanel(vehicle);
                }
            }
        }
    }

    private static void controlRadio(EntityVehicleF_Physics vehicle, ControlsKeyboard radio) {
        if (radio.isPressed()) {
            if (AGUIBase.activeInputGUI instanceof GUIRadio) {
                AGUIBase.activeInputGUI.close();
            } else if (!InterfaceManager.clientInterface.isGUIOpen()) {
                new GUIRadio(vehicle.radio);
                InterfaceManager.packetInterface.sendToServer(new PacketEntityInteractGUI(vehicle, InterfaceManager.clientInterface.getClientPlayer(), true));
            }
        }
    }

    private static void controlJoystick(EntityVehicleF_Physics vehicle, ControlsKeyboard joystickInhibit) {
        if (joystickInhibit.isPressed()) {
            joysticksInhibited = !joysticksInhibited;
        }
    }

    private static void controlControlSurface(EntityVehicleF_Physics vehicle, ControlsJoystick axis, ControlsKeyboard increment, ControlsKeyboard decrement, double rate, double bounds, ComputedVariable variable, double dampenRate) {
        if (axis.isJoystickActive()) {
            double axisValue = axis.getAxisState(false);
            if (axisValue == 0) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(variable, 0));
            } else {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(variable, bounds * (-1 + 2 * axisValue)));
            }
        } else {
            if (increment.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, rate * (variable.currentValue < 0 ? 2 : 1), -bounds, bounds));
                InterfaceManager.packetInterface.sendToServer(new PacketVehicleControlNotification(vehicle, clientPlayer));
            } else if (decrement.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, -rate * (variable.currentValue > 0 ? 2 : 1), -bounds, bounds));
                InterfaceManager.packetInterface.sendToServer(new PacketVehicleControlNotification(vehicle, clientPlayer));
            } else if (clientPlayer.equals(vehicle.lastController)) {
                if (variable.currentValue > dampenRate) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, -dampenRate, 0, bounds));
                } else if (variable.currentValue < -dampenRate) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, dampenRate, -bounds, 0));
                } else if (variable.currentValue != 0) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(variable, 0));
                }
            }
        }
    }

    private static void controlControlTrim(EntityVehicleF_Physics vehicle, ControlsJoystick increment, ControlsJoystick decrement, double bounds, ComputedVariable variable) {
        if (increment.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, 0.1, -bounds, bounds));
        } else if (decrement.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(variable, -0.1, -bounds, bounds));
        }
    }

    private static void controlAircraft(EntityVehicleF_Physics aircraft, boolean isPlayerController) {
        controlCamera(ControlsKeyboard.AIRCRAFT_ZOOM_I, ControlsKeyboard.AIRCRAFT_ZOOM_O, ControlsKeyboard.AIRCRAFT_CHANGEVIEW, ControlsJoystick.AIRCRAFT_LOOK_UD, ControlsJoystick.AIRCRAFT_LOOK_LR);
        rotateCamera(ControlsJoystick.AIRCRAFT_LOOK_R, ControlsJoystick.AIRCRAFT_LOOK_L, ControlsJoystick.AIRCRAFT_LOOK_U, ControlsJoystick.AIRCRAFT_LOOK_D, ControlsJoystick.AIRCRAFT_LOOK_A);
        controlGun(aircraft, ControlsKeyboard.AIRCRAFT_GUN_FIRE, ControlsKeyboard.AIRCRAFT_GUN_SWITCH);
        controlRadio(aircraft, ControlsKeyboard.AIRCRAFT_RADIO);
        controlJoystick(aircraft, ControlsKeyboard.AIRCRAFT_JS_INHIBIT);

        if (!isPlayerController) {
            return;
        }
        //Open or close the panel.
        controlPanel(aircraft, ControlsKeyboard.AIRCRAFT_PANEL);

        //Check brake status.
        controlBrake(aircraft, ControlsJoystick.AIRCRAFT_BRAKE, ControlsJoystick.AIRCRAFT_BRAKE_DIGITAL, ControlsKeyboard.AIRCRAFT_BRAKE, ControlsKeyboard.AIRCRAFT_PARK);

        //Check for thrust reverse button.
        if (ControlsJoystick.AIRCRAFT_REVERSE.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(aircraft.reverseThrustVar));
        }

        //Check for gear button.
        if (ControlsJoystick.AIRCRAFT_GEAR.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(aircraft.retractGearVar));
        }

        //Increment or decrement throttle.
        if (ControlsJoystick.AIRCRAFT_THROTTLE.isJoystickActive()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.throttleVar, ControlsJoystick.AIRCRAFT_THROTTLE.getAxisState(true) * EntityVehicleF_Physics.MAX_THROTTLE));
        } else {
            if (ControlsKeyboard.AIRCRAFT_THROTTLE_U.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(aircraft.throttleVar, EntityVehicleF_Physics.MAX_THROTTLE / 100D, 0, EntityVehicleF_Physics.MAX_THROTTLE));
            }
            if (ControlsKeyboard.AIRCRAFT_THROTTLE_D.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(aircraft.throttleVar, -EntityVehicleF_Physics.MAX_THROTTLE / 100D, 0, EntityVehicleF_Physics.MAX_THROTTLE));
            }
        }

        //Check flaps.
        if (aircraft.definition.motorized.flapNotches != null && !aircraft.definition.motorized.flapNotches.isEmpty()) {
            if (ControlsKeyboard.AIRCRAFT_FLAPS_D.isPressed()) {
                int currentFlapSetting = aircraft.definition.motorized.flapNotches.indexOf((float) aircraft.flapDesiredAngleVar.currentValue);
                if (currentFlapSetting == -1) {
                    //Get next-highest notch since we're going down.
                    for (int i = 0; i < aircraft.definition.motorized.flapNotches.size(); ++i) {
                        float flapNotch = aircraft.definition.motorized.flapNotches.get(i);
                        if (flapNotch > aircraft.flapDesiredAngleVar.currentValue) {
                            currentFlapSetting = i;
                            break;
                        }
                    }
                }
                if (currentFlapSetting != -1 && currentFlapSetting + 1 < aircraft.definition.motorized.flapNotches.size()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.flapDesiredAngleVar, aircraft.definition.motorized.flapNotches.get(currentFlapSetting + 1)));
                }
            } else if (ControlsKeyboard.AIRCRAFT_FLAPS_U.isPressed()) {
                int currentFlapSetting = aircraft.definition.motorized.flapNotches.indexOf((float) aircraft.flapDesiredAngleVar.currentValue);
                if (currentFlapSetting == -1) {
                    //Get next-lowest notch since we're going up.
                    for (int i = aircraft.definition.motorized.flapNotches.size() - 1; i <= 0; --i) {
                        float flapNotch = aircraft.definition.motorized.flapNotches.get(i);
                        if (flapNotch < aircraft.flapDesiredAngleVar.currentValue) {
                            currentFlapSetting = i;
                            break;
                        }
                    }
                }
                if (currentFlapSetting > 0) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.flapDesiredAngleVar, aircraft.definition.motorized.flapNotches.get(currentFlapSetting - 1)));
                }
            }
        }

        //Check yaw.  Blimps don't use rudder keys.
        if (!aircraft.definition.motorized.isBlimp) {
            controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_YAW, ControlsKeyboard.AIRCRAFT_YAW_R, ControlsKeyboard.AIRCRAFT_YAW_L, ConfigSystem.client.controlSettings.steeringControlRate.value, EntityVehicleF_Physics.MAX_RUDDER_ANGLE, aircraft.rudderInputVar, EntityVehicleF_Physics.RUDDER_DAMPEN_RATE);
            controlControlTrim(aircraft, ControlsJoystick.AIRCRAFT_TRIM_YAW_R, ControlsJoystick.AIRCRAFT_TRIM_YAW_L, EntityVehicleF_Physics.MAX_RUDDER_TRIM, aircraft.rudderTrimVar);
        }

        //Check pitch.
        controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_PITCH, ControlsKeyboard.AIRCRAFT_PITCH_U, ControlsKeyboard.AIRCRAFT_PITCH_D, ConfigSystem.client.controlSettings.flightControlRate.value, EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, aircraft.elevatorInputVar, EntityVehicleF_Physics.ELEVATOR_DAMPEN_RATE);
        controlControlTrim(aircraft, ControlsJoystick.AIRCRAFT_TRIM_PITCH_U, ControlsJoystick.AIRCRAFT_TRIM_PITCH_D, EntityVehicleF_Physics.MAX_ELEVATOR_TRIM, aircraft.elevatorTrimVar);

        //Check roll.  Blimps use roll for rudder for steering.
        if (aircraft.definition.motorized.isBlimp) {
            controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_ROLL, ControlsKeyboard.AIRCRAFT_ROLL_R, ControlsKeyboard.AIRCRAFT_ROLL_L, ConfigSystem.client.controlSettings.steeringControlRate.value, EntityVehicleF_Physics.MAX_RUDDER_ANGLE, aircraft.rudderInputVar, EntityVehicleF_Physics.RUDDER_DAMPEN_RATE);
        } else {
            controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_ROLL, ControlsKeyboard.AIRCRAFT_ROLL_R, ControlsKeyboard.AIRCRAFT_ROLL_L, ConfigSystem.client.controlSettings.flightControlRate.value, EntityVehicleF_Physics.MAX_AILERON_ANGLE, aircraft.aileronInputVar, EntityVehicleF_Physics.AILERON_DAMPEN_RATE);
        }
        controlControlTrim(aircraft, ControlsJoystick.AIRCRAFT_TRIM_ROLL_R, ControlsJoystick.AIRCRAFT_TRIM_ROLL_L, EntityVehicleF_Physics.MAX_AILERON_TRIM, aircraft.aileronTrimVar);

        //Check to see if we request a different auto-level state.
        if (ConfigSystem.client.controlSettings.heliAutoLevel.value ^ aircraft.autolevelEnabledVar.isActive) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft.autolevelEnabledVar, ConfigSystem.client.controlSettings.heliAutoLevel.value ? 1 : 0));
        }
    }

    private static void controlGroundVehicle(EntityVehicleF_Physics powered, boolean isPlayerController) {
        controlCamera(ControlsKeyboard.CAR_ZOOM_I, ControlsKeyboard.CAR_ZOOM_O, ControlsKeyboard.CAR_CHANGEVIEW, ControlsJoystick.CAR_LOOK_UD, ControlsJoystick.CAR_LOOK_LR);
        rotateCamera(ControlsJoystick.CAR_LOOK_R, ControlsJoystick.CAR_LOOK_L, ControlsJoystick.CAR_LOOK_U, ControlsJoystick.CAR_LOOK_D, ControlsJoystick.CAR_LOOK_A);
        controlGun(powered, ControlsKeyboard.CAR_GUN_FIRE, ControlsKeyboard.CAR_GUN_SWITCH);
        controlRadio(powered, ControlsKeyboard.CAR_RADIO);
        controlJoystick(powered, ControlsKeyboard.CAR_JS_INHIBIT);

        if (!isPlayerController) {
            return;
        }
        //Open or close the panel.
        controlPanel(powered, ControlsKeyboard.CAR_PANEL);

        //Check brake and gas.  Depends on how the controls are configured.
        if (powered.definition.motorized.hasIncrementalThrottle) {
            //Check brake and gas.  Brake always changes, gas goes up-down.
            controlBrake(powered, ControlsJoystick.CAR_BRAKE, ControlsJoystick.CAR_BRAKE_DIGITAL, ControlsKeyboard.CAR_BRAKE, ControlsKeyboard.CAR_PARK);
            if (ControlsJoystick.CAR_GAS.isJoystickActive()) {
                //Send throttle over if throttle if cruise control is off, or if throttle is less than the axis level.
                double throttleLevel = ControlsJoystick.CAR_GAS.getAxisState(true) * EntityVehicleF_Physics.MAX_THROTTLE;
                if (!powered.autopilotValueVar.isActive || powered.throttleVar.currentValue < throttleLevel) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.throttleVar, throttleLevel));
                }
            } else {
                if (ControlsKeyboard.CAR_GAS.isPressed()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(powered.throttleVar, EntityVehicleF_Physics.MAX_THROTTLE / 100D, 0, EntityVehicleF_Physics.MAX_THROTTLE));
                }
                if (ControlsKeyboard.CAR_BRAKE.isPressed() || ControlsJoystick.CAR_BRAKE_DIGITAL.isPressed()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(powered.throttleVar, -EntityVehicleF_Physics.MAX_THROTTLE / 100D, 0, EntityVehicleF_Physics.MAX_THROTTLE));
                }
            }
        } else {
            double throttleRequest = -999;
            if (ConfigSystem.client.controlSettings.simpleThrottle.value) {
                if (!powered.engines.isEmpty()) {
                    //Get the brake value.
                    double brakeRequest = -999;
                    final double brakeValue;
                    if (ControlsJoystick.CAR_BRAKE.isJoystickActive()) {
                        brakeValue = ControlsJoystick.CAR_BRAKE.getAxisState(true);
                    } else if (ControlsKeyboard.CAR_BRAKE.isPressed() || ControlsJoystick.CAR_BRAKE_DIGITAL.isPressed()) {
                        brakeValue = EntityVehicleF_Physics.MAX_BRAKE;
                    } else {
                        brakeValue = 0;
                    }

                    //Get the throttle value.
                    final double throttleValue;
                    if (ControlsJoystick.CAR_GAS.isJoystickActive()) {
                        throttleValue = ControlsJoystick.CAR_GAS.getAxisState(true) * EntityVehicleF_Physics.MAX_THROTTLE;
                    } else if (ControlsKeyboardDynamic.CAR_SLOW.isPressed()) {
                        throttleValue = ConfigSystem.client.controlSettings.halfThrottle.value ? EntityVehicleF_Physics.MAX_THROTTLE : EntityVehicleF_Physics.MAX_THROTTLE / 2D;
                    } else if (ControlsKeyboard.CAR_GAS.isPressed()) {
                        throttleValue = ConfigSystem.client.controlSettings.halfThrottle.value ? EntityVehicleF_Physics.MAX_THROTTLE / 2D : EntityVehicleF_Physics.MAX_THROTTLE;
                    } else {
                        throttleValue = 0;
                    }

                    //If we are going slow, and don't have gas or brake, automatically set the brake.
                    //Otherwise send normal values if we are in neutral or forwards,
                    //and invert controls if we are in a reverse gear (and not using a shifter).
                    //Use only the first engine for this.
                    if (throttleValue == 0 && brakeValue == 0 && powered.axialVelocity < PartEngine.MAX_SHIFT_SPEED) {
                        throttleRequest = 0;
                        brakeRequest = EntityVehicleF_Physics.MAX_BRAKE;
                    } else if (powered.engines.get(0).currentGearVar.currentValue >= 0 || ConfigSystem.client.controlSettings.useShifter.value) {
                        brakeRequest = brakeValue;

                        //Send throttle over if throttle if cruise control is off, or if the throttle is pressed, or was released this check.
                        if (!powered.autopilotValueVar.isActive || throttleValue > 0 || throttleRequestLastCheck > 0) {
                            throttleRequest = throttleValue;
                        }
                    } else {
                        throttleRequest = brakeValue;
                        brakeRequest = throttleValue;
                    }

                    if (!ConfigSystem.client.controlSettings.useShifter.value) {
                        powered.engines.forEach(engine -> {
                            //If we don't have velocity, and we have the appropriate control, shift.
                            if (brakeValue > EntityVehicleF_Physics.MAX_BRAKE / 4F && engine.currentGearVar.currentValue >= 0 && powered.axialVelocity < 0.01F) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.shiftDownVar, 1));
                            } else if (throttleValue > EntityVehicleF_Physics.MAX_THROTTLE / 4F && engine.currentGearVar.currentValue <= 0 && powered.axialVelocity < 0.01F) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.shiftUpVar, 1));
                            }
                        });
                    }

                    if (brakeRequest != -999 && brakeRequestLastCheck != brakeRequest) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.brakeVar, brakeRequest));
                    }
                    brakeRequestLastCheck = brakeRequest;
                }
            } else {
                //Check brake and gas and set to on or off.
                controlBrake(powered, ControlsJoystick.CAR_BRAKE, ControlsJoystick.CAR_BRAKE_DIGITAL, ControlsKeyboard.CAR_BRAKE, ControlsKeyboard.CAR_PARK);
                if (ControlsJoystick.CAR_GAS.isJoystickActive()) {
                    //Send throttle over if throttle if cruise control is off, or if throttle is greater than the current value.
                    double throttleLevel = ControlsJoystick.CAR_GAS.getAxisState(true);
                    if (!powered.autopilotValueVar.isActive || throttleLevel > powered.throttleVar.currentValue) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.throttleVar, throttleLevel));
                    }
                } else {
                    if (ControlsKeyboardDynamic.CAR_SLOW.isPressed()) {
                        if (!ConfigSystem.client.controlSettings.halfThrottle.value) {
                            throttleRequest = EntityVehicleF_Physics.MAX_THROTTLE / 2D;
                        } else {
                            throttleRequest = EntityVehicleF_Physics.MAX_THROTTLE;
                        }
                    } else if (ControlsKeyboard.CAR_GAS.isPressed()) {
                        if (!ConfigSystem.client.controlSettings.halfThrottle.value) {
                            throttleRequest = EntityVehicleF_Physics.MAX_THROTTLE;
                        } else {
                            throttleRequest = EntityVehicleF_Physics.MAX_THROTTLE / 2D;
                        }
                    } else {
                        //Send gas off packet if we don't have cruise on, or if we do and we pressed the throttle last check.
                        if (!powered.autopilotValueVar.isActive || throttleRequestLastCheck > 0) {
                            throttleRequest = 0;
                        }
                    }
                }
            }
            if (throttleRequest != -999 && throttleRequestLastCheck != throttleRequest) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.throttleVar, throttleRequest));
            }
            //Check if we have throttle request with brake on.  Brakes can be left on from simple throttle and such of other players.
            //Take the brake off here if so, since otherwise it will stay on unless we press the brake key.
            if (throttleRequest > 0 && powered.brakeVar.currentValue > 0) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.brakeVar, 0));
            }
            throttleRequestLastCheck = throttleRequest;

        }

        //Check steering.  Don't check while on a road, since we auto-drive on those.
        if (!powered.lockedOnRoad) {
            controlControlSurface(powered, ControlsJoystick.CAR_TURN, ControlsKeyboard.CAR_TURN_R, ControlsKeyboard.CAR_TURN_L, ConfigSystem.client.controlSettings.steeringControlRate.value, EntityVehicleF_Physics.MAX_RUDDER_ANGLE, powered.rudderInputVar, ConfigSystem.client.controlSettings.steeringReturnRate.value);
        }

        //Check if we are shifting.
        if (ConfigSystem.client.controlSettings.useShifter.value) {
            final int gearNumber;
            if (ControlsJoystick.CAR_SHIFT_1.isPressed()) {
                gearNumber = 1;
            } else if (ControlsJoystick.CAR_SHIFT_2.isPressed()) {
                gearNumber = 2;
            } else if (ControlsJoystick.CAR_SHIFT_3.isPressed()) {
                gearNumber = 3;
            } else if (ControlsJoystick.CAR_SHIFT_4.isPressed()) {
                gearNumber = 4;
            } else if (ControlsJoystick.CAR_SHIFT_5.isPressed()) {
                gearNumber = 5;
            } else if (ControlsJoystick.CAR_SHIFT_6.isPressed()) {
                gearNumber = 6;
            } else if (ControlsJoystick.CAR_SHIFT_7.isPressed()) {
                gearNumber = 7;
            } else if (ControlsJoystick.CAR_SHIFT_8.isPressed()) {
                gearNumber = 8;
            } else if (ControlsJoystick.CAR_SHIFT_9.isPressed()) {
                gearNumber = 9;
            } else if (ControlsJoystick.CAR_SHIFT_R.isPressed()) {
                gearNumber = 10;
            } else {
                gearNumber = 11;
            }
            powered.engines.forEach(engine -> {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(engine.shiftSelectionVar, gearNumber));
            });
        } else {
            if (ControlsKeyboardDynamic.CAR_SHIFT_NU.isPressed() || ControlsKeyboardDynamic.CAR_SHIFT_ND.isPressed()) {
                powered.engines.forEach(engine -> {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftNeutralVar));
                });
            } else {
                if (ControlsKeyboard.CAR_SHIFT_U.isPressed()) {
                    powered.engines.forEach(engine -> {
                        if (engine.isAutomaticVar.isActive) {
                            if (engine.currentGearVar.currentValue < 0) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftNeutralVar));
                            } else if (engine.currentGearVar.currentValue == 0) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftUpVar));
                            }
                        } else {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftUpVar));
                        }
                    });
                }
                if (ControlsKeyboard.CAR_SHIFT_D.isPressed()) {
                    powered.engines.forEach(engine -> {
                        if (engine.isAutomaticVar.isActive) {
                            if (engine.currentGearVar.currentValue > 0) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftNeutralVar));
                            } else if (engine.currentGearVar.currentValue == 0) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftDownVar));
                            }
                        } else {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftDownVar));
                        }
                    });
                }
            }
        }

        //Check if horn button is pressed.
        if (ControlsKeyboard.CAR_HORN.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.hornVar, 1));
        } else if (ControlsKeyboard.CAR_HORN.justReleased()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered.hornVar, 0));
        }

        //Check for lights.
        if (ControlsKeyboard.CAR_LIGHTS.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.runningLightVar));
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.headLightVar));
        }
        if (ControlsKeyboard.CAR_TURNSIGNAL_L.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.leftTurnLightVar));
        }
        if (ControlsKeyboard.CAR_TURNSIGNAL_R.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.rightTurnLightVar));
        }

        //Change turn signal status depending on turning status.
        //Keep signals on until we have been moving without turning in the
        //pressed direction for 2 seconds, or if we turn in the other direction.
        //This only happens if the signals are set to automatic.  For manual signals, we let the player control them.
        if (ConfigSystem.client.controlSettings.autoTrnSignals.value) {
            if (!powered.turningLeft && powered.rudderInputVar.currentValue < -20) {
                powered.turningLeft = true;
                powered.turningCooldown = 40;
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.leftTurnLightVar));
            }
            if (!powered.turningRight && powered.rudderInputVar.currentValue > 20) {
                powered.turningRight = true;
                powered.turningCooldown = 40;
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.rightTurnLightVar));
            }
            if (powered.turningLeft && (powered.rudderInputVar.currentValue > 0 || powered.turningCooldown == 0)) {
                powered.turningLeft = false;
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.leftTurnLightVar));
            }
            if (powered.turningRight && (powered.rudderInputVar.currentValue < 0 || powered.turningCooldown == 0)) {
                powered.turningRight = false;
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered.rightTurnLightVar));
            }
            if (powered.velocity != 0 && powered.turningCooldown > 0 && powered.rudderInputVar.currentValue == 0) {
                --powered.turningCooldown;
            }
        }
    }

    /**
     * List of enums representing all controls present.  Add new controls by adding their enum values here
     *
     * @author don_bruce
     */
    public enum ControlsKeyboard {
        GENERAL_CUSTOM1(ControlsJoystick.GENERAL_CUSTOM1, true, "NUMPAD0", LanguageSystem.INPUT_CUSTOM1),
        GENERAL_CUSTOM2(ControlsJoystick.GENERAL_CUSTOM2, true, "NUMPAD1", LanguageSystem.INPUT_CUSTOM2),
        GENERAL_CUSTOM3(ControlsJoystick.GENERAL_CUSTOM3, true, "NUMPAD2", LanguageSystem.INPUT_CUSTOM3),
        GENERAL_CUSTOM4(ControlsJoystick.GENERAL_CUSTOM4, true, "NUMPAD3", LanguageSystem.INPUT_CUSTOM4),
        GENERAL_RELOAD(ControlsJoystick.GENERAL_RELOAD, true, "R", LanguageSystem.INPUT_GUN_RELOAD),

        AIRCRAFT_YAW_R(ControlsJoystick.AIRCRAFT_YAW, false, "L", LanguageSystem.INPUT_YAW_R),
        AIRCRAFT_YAW_L(ControlsJoystick.AIRCRAFT_YAW, false, "J", LanguageSystem.INPUT_YAW_L),
        AIRCRAFT_PITCH_U(ControlsJoystick.AIRCRAFT_PITCH, false, "S", LanguageSystem.INPUT_PITCH_U),
        AIRCRAFT_PITCH_D(ControlsJoystick.AIRCRAFT_PITCH, false, "W", LanguageSystem.INPUT_PITCH_D),
        AIRCRAFT_ROLL_R(ControlsJoystick.AIRCRAFT_ROLL, false, "D", LanguageSystem.INPUT_ROLL_R),
        AIRCRAFT_ROLL_L(ControlsJoystick.AIRCRAFT_ROLL, false, "A", LanguageSystem.INPUT_ROLL_L),
        AIRCRAFT_THROTTLE_U(ControlsJoystick.AIRCRAFT_THROTTLE, false, "I", LanguageSystem.INPUT_THROTTLE_U),
        AIRCRAFT_THROTTLE_D(ControlsJoystick.AIRCRAFT_THROTTLE, false, "K", LanguageSystem.INPUT_THROTTLE_D),
        AIRCRAFT_FLAPS_U(ControlsJoystick.AIRCRAFT_FLAPS_U, true, "Y", LanguageSystem.INPUT_FLAPS_U),
        AIRCRAFT_FLAPS_D(ControlsJoystick.AIRCRAFT_FLAPS_D, true, "H", LanguageSystem.INPUT_FLAPS_D),
        AIRCRAFT_BRAKE(ControlsJoystick.AIRCRAFT_BRAKE, false, "B", LanguageSystem.INPUT_BRAKE),
        AIRCRAFT_PARK(ControlsJoystick.AIRCRAFT_PARK, true, "N", LanguageSystem.INPUT_PARK),
        AIRCRAFT_PANEL(ControlsJoystick.AIRCRAFT_PANEL, true, "U", LanguageSystem.INPUT_PANEL),
        AIRCRAFT_RADIO(ControlsJoystick.AIRCRAFT_RADIO, true, "MINUS", LanguageSystem.INPUT_RADIO),
        AIRCRAFT_GUN_FIRE(ControlsJoystick.AIRCRAFT_GUN_FIRE, false, "SPACE", LanguageSystem.INPUT_GUN_FIRE),
        AIRCRAFT_GUN_SWITCH(ControlsJoystick.AIRCRAFT_GUN_SWITCH, true, "V", LanguageSystem.INPUT_GUN_SWITCH),
        AIRCRAFT_ZOOM_I(ControlsJoystick.AIRCRAFT_ZOOM_I, true, "PRIOR", LanguageSystem.INPUT_ZOOM_I),
        AIRCRAFT_ZOOM_O(ControlsJoystick.AIRCRAFT_ZOOM_O, true, "NEXT", LanguageSystem.INPUT_ZOOM_O),
        AIRCRAFT_CHANGEVIEW(ControlsJoystick.AIRCRAFT_CHANGEVIEW, true, "X", LanguageSystem.INPUT_CHANGEVIEW),
        AIRCRAFT_JS_INHIBIT(ControlsJoystick.AIRCRAFT_JS_INHIBIT, true, "SCROLL", LanguageSystem.INPUT_JS_INHIBIT),

        CAR_MOD(ControlsJoystick.CAR_MOD, false, "RSHIFT", LanguageSystem.INPUT_MOD),
        CAR_TURN_R(ControlsJoystick.CAR_TURN, false, "D", LanguageSystem.INPUT_TURN_R),
        CAR_TURN_L(ControlsJoystick.CAR_TURN, false, "A", LanguageSystem.INPUT_TURN_L),
        CAR_GAS(ControlsJoystick.CAR_GAS, false, "W", LanguageSystem.INPUT_GAS),
        CAR_BRAKE(ControlsJoystick.CAR_BRAKE, false, "S", LanguageSystem.INPUT_BRAKE),
        CAR_PARK(ControlsJoystick.CAR_PARK, true, "N", LanguageSystem.INPUT_PARK),
        CAR_PANEL(ControlsJoystick.CAR_PANEL, true, "U", LanguageSystem.INPUT_PANEL),
        CAR_SHIFT_U(ControlsJoystick.CAR_SHIFT_U, true, "R", LanguageSystem.INPUT_SHIFT_U),
        CAR_SHIFT_D(ControlsJoystick.CAR_SHIFT_D, true, "F", LanguageSystem.INPUT_SHIFT_D),
        CAR_HORN(ControlsJoystick.CAR_HORN, true, "C", LanguageSystem.INPUT_HORN),
        CAR_RADIO(ControlsJoystick.CAR_RADIO, true, "MINUS", LanguageSystem.INPUT_RADIO),
        CAR_GUN_FIRE(ControlsJoystick.CAR_GUN_FIRE, false, "SPACE", LanguageSystem.INPUT_GUN_FIRE),
        CAR_GUN_SWITCH(ControlsJoystick.CAR_GUN_SWITCH, true, "V", LanguageSystem.INPUT_GUN_SWITCH),
        CAR_ZOOM_I(ControlsJoystick.CAR_ZOOM_I, true, "PRIOR", LanguageSystem.INPUT_ZOOM_I),
        CAR_ZOOM_O(ControlsJoystick.CAR_ZOOM_O, true, "NEXT", LanguageSystem.INPUT_ZOOM_O),
        CAR_CHANGEVIEW(ControlsJoystick.CAR_CHANGEVIEW, true, "X", LanguageSystem.INPUT_CHANGEVIEW),
        CAR_LIGHTS(ControlsJoystick.CAR_LIGHTS, true, "NUMPAD5", LanguageSystem.INPUT_LIGHTS),
        CAR_TURNSIGNAL_L(ControlsJoystick.CAR_TURNSIGNAL_L, true, "NUMPAD4", LanguageSystem.INPUT_TURNSIGNAL_L),
        CAR_TURNSIGNAL_R(ControlsJoystick.CAR_TURNSIGNAL_R, true, "NUMPAD6", LanguageSystem.INPUT_TURNSIGNAL_R),
        CAR_JS_INHIBIT(ControlsJoystick.CAR_JS_INHIBIT, true, "SCROLL", LanguageSystem.INPUT_JS_INHIBIT);

        public final boolean isMomentary;
        public final String systemName;
        public final LanguageEntry language;
        public final String defaultKeyName;
        public final ConfigKeyboard config;
        private final ControlsJoystick linkedJoystick;

        private boolean wasPressedThisCall;
        private boolean wasPressedLastCall;

        ControlsKeyboard(ControlsJoystick linkedJoystick, boolean isMomentary, String defaultKeyName, LanguageEntry language) {
            this.linkedJoystick = linkedJoystick;
            this.isMomentary = isMomentary;
            this.systemName = this.name().toLowerCase(Locale.ROOT).replaceFirst("_", ".");
            this.language = language;
            this.defaultKeyName = defaultKeyName;
            if (ConfigSystem.client.controls.keyboard.containsKey(systemName)) {
                this.config = ConfigSystem.client.controls.keyboard.get(systemName);
            } else {
                this.config = new ConfigKeyboard();
            }
        }

        /**
         * Returns true if the given key is currently pressed.  If our linked
         * joystick is pressed, return true.  If the joystick is not, but it
         * is bound, and we are using keyboard overrides, return false.
         * Otherwise return the actual key state.
         */
        public boolean isPressed() {
            wasPressedLastCall = wasPressedThisCall;
            if (linkedJoystick.isPressed()) {
                //Joystick pressed.
                wasPressedThisCall = true;
            } else if (linkedJoystick.isJoystickActive() && ConfigSystem.client.controlSettings.kbOverride.value) {
                //Joystick found, but not pressed, and is overriding keyboard inputs, so return false.
                wasPressedThisCall = false;
            } else {
                wasPressedThisCall = InterfaceManager.inputInterface.isKeyPressed(config.keyCode);
                if (isMomentary && wasPressedLastCall) {
                    return false;
                }
            }
            return wasPressedThisCall;
        }

        /**
         * MUST be called after only a single call to {@link #wasPressedThisCall}
         */
        public boolean justReleased() {
            return !wasPressedThisCall && wasPressedLastCall;
        }
    }

    public enum ControlsJoystick {
        GENERAL_CUSTOM1(false, true, LanguageSystem.INPUT_CUSTOM1),
        GENERAL_CUSTOM2(false, true, LanguageSystem.INPUT_CUSTOM2),
        GENERAL_CUSTOM3(false, true, LanguageSystem.INPUT_CUSTOM3),
        GENERAL_CUSTOM4(false, true, LanguageSystem.INPUT_CUSTOM4),
        GENERAL_RELOAD(false, true, LanguageSystem.INPUT_GUN_RELOAD),

        AIRCRAFT_CAMLOCK(false, true, LanguageSystem.INPUT_CAMLOCK),
        AIRCRAFT_YAW(true, false, LanguageSystem.INPUT_YAW),
        AIRCRAFT_PITCH(true, false, LanguageSystem.INPUT_PITCH),
        AIRCRAFT_ROLL(true, false, LanguageSystem.INPUT_ROLL),
        AIRCRAFT_THROTTLE(true, false, LanguageSystem.INPUT_THROTTLE),
        AIRCRAFT_BRAKE(true, false, LanguageSystem.INPUT_BRAKE),
        AIRCRAFT_BRAKE_DIGITAL(false, false, LanguageSystem.INPUT_BRAKE),
        AIRCRAFT_GEAR(false, true, LanguageSystem.INPUT_GEAR),
        AIRCRAFT_FLAPS_U(false, true, LanguageSystem.INPUT_FLAPS_U),
        AIRCRAFT_FLAPS_D(false, true, LanguageSystem.INPUT_FLAPS_D),
        AIRCRAFT_PANEL(false, true, LanguageSystem.INPUT_PANEL),
        AIRCRAFT_PARK(false, true, LanguageSystem.INPUT_PARK),
        AIRCRAFT_RADIO(false, true, LanguageSystem.INPUT_RADIO),
        AIRCRAFT_GUN_FIRE(false, false, LanguageSystem.INPUT_GUN_FIRE),
        AIRCRAFT_GUN_SWITCH(false, true, LanguageSystem.INPUT_GUN_SWITCH),
        AIRCRAFT_ZOOM_I(false, true, LanguageSystem.INPUT_ZOOM_I),
        AIRCRAFT_ZOOM_O(false, true, LanguageSystem.INPUT_ZOOM_O),
        AIRCRAFT_CHANGEVIEW(false, true, LanguageSystem.INPUT_CHANGEVIEW),
        AIRCRAFT_LOOK_UD(true, false, LanguageSystem.INPUT_LOOK_UD),
        AIRCRAFT_LOOK_LR(true, false, LanguageSystem.INPUT_LOOK_LR),
        AIRCRAFT_LOOK_L(false, false, LanguageSystem.INPUT_LOOK_L),
        AIRCRAFT_LOOK_R(false, false, LanguageSystem.INPUT_LOOK_R),
        AIRCRAFT_LOOK_U(false, false, LanguageSystem.INPUT_LOOK_U),
        AIRCRAFT_LOOK_D(false, false, LanguageSystem.INPUT_LOOK_D),
        AIRCRAFT_LOOK_A(false, false, LanguageSystem.INPUT_LOOK_A),
        AIRCRAFT_TRIM_YAW_R(false, false, LanguageSystem.INPUT_TRIM_YAW_R),
        AIRCRAFT_TRIM_YAW_L(false, false, LanguageSystem.INPUT_TRIM_YAW_L),
        AIRCRAFT_TRIM_PITCH_U(false, false, LanguageSystem.INPUT_TRIM_PITCH_U),
        AIRCRAFT_TRIM_PITCH_D(false, false, LanguageSystem.INPUT_TRIM_PITCH_D),
        AIRCRAFT_TRIM_ROLL_R(false, false, LanguageSystem.INPUT_TRIM_ROLL_R),
        AIRCRAFT_TRIM_ROLL_L(false, false, LanguageSystem.INPUT_TRIM_ROLL_L),
        AIRCRAFT_REVERSE(false, true, LanguageSystem.INPUT_REVERSE),
        AIRCRAFT_JS_INHIBIT(false, true, LanguageSystem.INPUT_JS_INHIBIT),

        CAR_MOD(false, false, LanguageSystem.INPUT_MOD),
        CAR_CAMLOCK(false, true, LanguageSystem.INPUT_CAMLOCK),
        CAR_TURN(true, false, LanguageSystem.INPUT_TURN),
        CAR_GAS(true, false, LanguageSystem.INPUT_GAS),
        CAR_BRAKE(true, false, LanguageSystem.INPUT_BRAKE),
        CAR_BRAKE_DIGITAL(false, false, LanguageSystem.INPUT_BRAKE),
        CAR_PANEL(false, true, LanguageSystem.INPUT_PANEL),
        CAR_SHIFT_U(false, true, LanguageSystem.INPUT_SHIFT_U),
        CAR_SHIFT_D(false, true, LanguageSystem.INPUT_SHIFT_D),
        CAR_SHIFT_1(false, false, LanguageSystem.INPUT_SHIFT_1),
        CAR_SHIFT_2(false, false, LanguageSystem.INPUT_SHIFT_2),
        CAR_SHIFT_3(false, false, LanguageSystem.INPUT_SHIFT_3),
        CAR_SHIFT_4(false, false, LanguageSystem.INPUT_SHIFT_4),
        CAR_SHIFT_5(false, false, LanguageSystem.INPUT_SHIFT_5),
        CAR_SHIFT_6(false, false, LanguageSystem.INPUT_SHIFT_6),
        CAR_SHIFT_7(false, false, LanguageSystem.INPUT_SHIFT_7),
        CAR_SHIFT_8(false, false, LanguageSystem.INPUT_SHIFT_8),
        CAR_SHIFT_9(false, false, LanguageSystem.INPUT_SHIFT_9),
        CAR_SHIFT_R(false, false, LanguageSystem.INPUT_SHIFT_R),
        CAR_HORN(false, true, LanguageSystem.INPUT_HORN),
        CAR_PARK(false, true, LanguageSystem.INPUT_PARK),
        CAR_RADIO(false, true, LanguageSystem.INPUT_RADIO),
        CAR_GUN_FIRE(false, false, LanguageSystem.INPUT_GUN_FIRE),
        CAR_GUN_SWITCH(false, true, LanguageSystem.INPUT_GUN_SWITCH),
        CAR_ZOOM_I(false, true, LanguageSystem.INPUT_ZOOM_I),
        CAR_ZOOM_O(false, true, LanguageSystem.INPUT_ZOOM_O),
        CAR_CHANGEVIEW(false, true, LanguageSystem.INPUT_CHANGEVIEW),
        CAR_LOOK_UD(true, false, LanguageSystem.INPUT_LOOK_UD),
        CAR_LOOK_LR(true, false, LanguageSystem.INPUT_LOOK_LR),
        CAR_LOOK_L(false, false, LanguageSystem.INPUT_LOOK_L),
        CAR_LOOK_R(false, false, LanguageSystem.INPUT_LOOK_R),
        CAR_LOOK_U(false, false, LanguageSystem.INPUT_LOOK_U),
        CAR_LOOK_D(false, false, LanguageSystem.INPUT_LOOK_D),
        CAR_LOOK_A(false, false, LanguageSystem.INPUT_LOOK_A),
        CAR_LIGHTS(false, true, LanguageSystem.INPUT_LIGHTS),
        CAR_TURNSIGNAL_L(false, true, LanguageSystem.INPUT_TURNSIGNAL_L),
        CAR_TURNSIGNAL_R(false, true, LanguageSystem.INPUT_TURNSIGNAL_R),
        CAR_JS_INHIBIT(false, true, LanguageSystem.INPUT_JS_INHIBIT);

        public final boolean isAxis;
        public final boolean isMomentary;
        public final String systemName;
        public final LanguageEntry language;
        public final ConfigJoystick config;

        private boolean wasPressedLastCall;

        ControlsJoystick(boolean isAxis, boolean isMomentary, LanguageEntry language) {
            this.isAxis = isAxis;
            this.isMomentary = isMomentary;
            this.systemName = this.name().toLowerCase(Locale.ROOT).replaceFirst("_", ".");
            this.language = language;
            if (ConfigSystem.client.controls.joystick.containsKey(systemName)) {
                this.config = ConfigSystem.client.controls.joystick.get(systemName);
            } else {
                this.config = new ConfigJoystick();
            }
        }

        public boolean isJoystickActive() {
            return !joysticksInhibited && InterfaceManager.inputInterface.isJoystickPresent(config.joystickName);
        }

        public boolean isPressed() {
            if (isJoystickActive()) {
                if (isMomentary) {
                    if (wasPressedLastCall) {
                        wasPressedLastCall = InterfaceManager.inputInterface.getJoystickButtonValue(config.joystickName, config.buttonIndex);
                        return false;
                    } else {
                        wasPressedLastCall = InterfaceManager.inputInterface.getJoystickButtonValue(config.joystickName, config.buttonIndex);
                        return wasPressedLastCall;
                    }
                } else {
                    return InterfaceManager.inputInterface.getJoystickButtonValue(config.joystickName, config.buttonIndex);
                }
            } else {
                return false;
            }
        }

        private float getMultistateValue() {
            return InterfaceManager.inputInterface.getJoystickAxisValue(config.joystickName, config.buttonIndex);
        }

        private double getAxisState(boolean ignoreDeadzone) {
            double pollValue = getMultistateValue();
            if ((config.axisMaxTravel != config.axisMinTravel) && (ignoreDeadzone || Math.abs(pollValue) > ConfigSystem.client.controlSettings.joystickDeadZone.value)) {
                //Clamp the poll value to the defined axis bounds set during config to prevent over and under-runs.
                pollValue = Math.max(config.axisMinTravel, pollValue);
                pollValue = Math.min(config.axisMaxTravel, pollValue);

                //Divide the poll value plus the min bounds by the span to get it in the range of 0-1.
                pollValue = (pollValue - config.axisMinTravel) / (config.axisMaxTravel - config.axisMinTravel);

                //If axis is inverted, invert poll.
                if (config.invertedAxis) {
                    pollValue = 1 - pollValue;
                }

                //Now return the value.
                return pollValue;
            } else {
                return 0;
            }
        }

        public void setControl(String joystickName, int buttonIndex) {
            config.joystickName = joystickName;
            config.buttonIndex = buttonIndex;
            ConfigSystem.client.controls.joystick.put(systemName, config);
            ConfigSystem.saveToDisk();
        }

        public void setAxisControl(String joystickName, int buttonIndex, double axisMinTravel, double axisMaxTravel, boolean invertedAxis) {
            config.axisMinTravel = axisMinTravel;
            config.axisMaxTravel = axisMaxTravel;
            config.invertedAxis = invertedAxis;
            setControl(joystickName, buttonIndex);
        }

        public void clearControl() {
            setControl(null, NULL_COMPONENT);
        }
    }

    public enum ControlsKeyboardDynamic {
        CAR_SLOW(ControlsKeyboard.CAR_GAS, ControlsKeyboard.CAR_MOD, LanguageSystem.INPUT_SLOW),
        CAR_SHIFT_NU(ControlsKeyboard.CAR_SHIFT_U, ControlsKeyboard.CAR_MOD, LanguageSystem.INPUT_SHIFT_N),
        CAR_SHIFT_ND(ControlsKeyboard.CAR_SHIFT_D, ControlsKeyboard.CAR_MOD, LanguageSystem.INPUT_SHIFT_N);

        public final LanguageEntry language;
        public final ControlsKeyboard mainControl;
        public final ControlsKeyboard modControl;

        ControlsKeyboardDynamic(ControlsKeyboard mainControl, ControlsKeyboard modControl, LanguageEntry language) {
            this.language = language;
            this.mainControl = mainControl;
            this.modControl = modControl;
        }

        public boolean isPressed() {
            return this.modControl.isPressed() && this.mainControl.isPressed();
        }
    }
}
