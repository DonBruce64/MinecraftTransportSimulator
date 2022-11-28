package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlacedPart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIPanelAircraft;
import minecrafttransportsimulator.guis.instances.GUIPanelGround;
import minecrafttransportsimulator.guis.instances.GUIRadio;
import minecrafttransportsimulator.jsondefs.JSONConfigClient.ConfigJoystick;
import minecrafttransportsimulator.jsondefs.JSONConfigClient.ConfigKeyboard;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityInteract;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.packets.instances.PacketPartSeat;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlNotification;

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

    private static boolean parkingBrakePressedLastCheck = false;

    private static BoundingBox closestBox = null;
    private static AEntityF_Multipart<?> closestEntity = null;

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
            if (control.config.keyCode == 0) {
                control.config.keyCode = InterfaceManager.inputInterface.getKeyCodeForName(control.defaultKeyName);
            }
        }
        ConfigSystem.saveToDisk();
    }

    public static void controlGlobal(IWrapperPlayer player) {
        if (InterfaceManager.inputInterface.isLeftMouseButtonDown()) {
            if (!clickingLeft) {
                clickingLeft = true;
                handleClick(player);
            }
        } else if (clickingLeft) {
            clickingLeft = false;
            handleClick(player);
        }
        if (InterfaceManager.inputInterface.isRightMouseButtonDown()) {
            if (!clickingRight) {
                clickingRight = true;
                handleClick(player);
            }
        } else if (clickingRight) {
            clickingRight = false;
            handleClick(player);
        }
    }

    private static void handleClick(IWrapperPlayer player) {
        //Either change the gun trigger state (if we are holding a gun),
        //or try to interact with entities if we are not.
        EntityPlayerGun playerGun = EntityPlayerGun.playerClientGuns.get(player.getID());
        if (playerGun != null && playerGun.activeGun != null) {
            InterfaceManager.packetInterface.sendToServer(new PacketPartGun(playerGun.activeGun, clickingLeft, clickingRight));
        }
        if (clickingLeft || clickingRight) {
            Point3D startPosition = player.getPosition();
            startPosition.y += (player.getEyeHeight() + player.getSeatOffset()) * player.getVerticalScale();
            Point3D endPosition = player.getLineOfSight(3.5);
            endPosition.add(startPosition);
            BoundingBox clickBounds = new BoundingBox(startPosition, endPosition);

            closestBox = null;
            closestEntity = null;
            for (EntityVehicleF_Physics vehicle : player.getWorld().getEntitiesOfType(EntityVehicleF_Physics.class)) {
                if (vehicle.encompassingBox.intersects(clickBounds)) {
                    //Could have hit this vehicle, check if and what we did via raytracing.
                    for (BoundingBox box : vehicle.allInteractionBoxes) {
                        if (box.intersects(clickBounds) && box.getIntersectionPoint(startPosition, endPosition) != null) {
                            if (closestBox == null || startPosition.isFirstCloserThanSecond(box.globalCenter, closestBox.globalCenter)) {
                                closestBox = box;
                                closestEntity = vehicle.getPartWithBox(closestBox);
                                if (closestEntity == null) {
                                    closestEntity = vehicle;
                                }
                            }
                        }
                    }
                }
            }
            for (EntityPlacedPart placer : player.getWorld().getEntitiesOfType(EntityPlacedPart.class)) {
                if (placer.encompassingBox.intersects(clickBounds)) {
                    //Could have hit this part, check if and what we did via raytracing.
                    for (BoundingBox box : placer.allInteractionBoxes) {
                        if (box.intersects(clickBounds) && box.getIntersectionPoint(startPosition, endPosition) != null) {
                            if (closestBox == null || startPosition.isFirstCloserThanSecond(box.globalCenter, closestBox.globalCenter)) {
                                APart clickedPart = placer.getPartWithBox(box);
                                //Only register clicks on the part-boxes, not the placer itself.  It just encompasses them.
                                if (clickedPart != null) {
                                    closestEntity = clickedPart;
                                    closestBox = box;
                                }
                            }
                        }
                    }
                }
            }
            if (closestBox != null) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(closestEntity, player, closestBox, clickingLeft, clickingRight));
            }
        } else if (closestBox != null) {
            //Fire off un-click to entity last clicked.
            InterfaceManager.packetInterface.sendToServer(new PacketEntityInteract(closestEntity, player, closestBox, false, false));
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
        } else {
            controlCamera(ControlsKeyboard.CAR_ZOOM_I, ControlsKeyboard.CAR_ZOOM_O, ControlsJoystick.CAR_CHANGEVIEW);
            rotateCamera(ControlsJoystick.CAR_LOOK_R, ControlsJoystick.CAR_LOOK_L, ControlsJoystick.CAR_LOOK_U, ControlsJoystick.CAR_LOOK_D, ControlsJoystick.CAR_LOOK_A);
            controlGun(multipart, ControlsKeyboard.CAR_GUN_FIRE, ControlsKeyboard.CAR_GUN_SWITCH);
        }
    }

    private static void controlCamera(ControlsKeyboard zoomIn, ControlsKeyboard zoomOut, ControlsJoystick changeView) {
        if (zoomIn.isPressed()) {
            CameraSystem.changeCameraZoom(true);
        }
        if (zoomOut.isPressed()) {
            CameraSystem.changeCameraZoom(false);
        }

        if (changeView.isPressed()) {
            InterfaceManager.clientInterface.toggleFirstPerson();
        }
    }

    private static void rotateCamera(ControlsJoystick lookR, ControlsJoystick lookL, ControlsJoystick lookU, ControlsJoystick lookD, ControlsJoystick lookA) {
        //FIXME this causes yaw de-syncs.
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

    private static void controlBrake(EntityVehicleF_Physics vehicle, ControlsKeyboardDynamic brakeMod, ControlsJoystick brakeJoystick, ControlsJoystick brakeButton, ControlsJoystick pBrake) {
        //If the analog brake is set, do brake state based on that rather than the keyboard.
        boolean isParkingBrakePressed = InterfaceManager.inputInterface.isJoystickPresent(brakeJoystick.config.joystickName) ? pBrake.isPressed() : brakeMod.isPressed() || pBrake.isPressed();
        if (isParkingBrakePressed) {
            if (!parkingBrakePressedLastCheck) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.PARKINGBRAKE_VARIABLE));
                parkingBrakePressedLastCheck = true;
            }
        } else {
            parkingBrakePressedLastCheck = false;
            double brakeValue = InterfaceManager.inputInterface.isJoystickPresent(brakeJoystick.config.joystickName) ? brakeJoystick.getAxisState(true) : (brakeMod.mainControl.isPressed() || brakeButton.isPressed() ? EntityVehicleF_Physics.MAX_BRAKE : 0);
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle, EntityVehicleF_Physics.BRAKE_VARIABLE, brakeValue));
        }
    }

    private static void controlGun(AEntityF_Multipart<?> multipart, ControlsKeyboard gunTrigger, ControlsKeyboard gunSwitch) {
        boolean gunSwitchPressedThisScan = gunSwitch.isPressed();
        IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();
        for (APart part : multipart.allParts) {
            if (part instanceof PartGun) {
                PartGun gun = (PartGun) part;
                if (clientPlayer.equals(gun.getGunController())) {
                    InterfaceManager.packetInterface.sendToServer(new PacketPartGun(gun, gunTrigger.isPressed(), false));
                }
            } else if (part instanceof PartSeat) {
                if (gunSwitchPressedThisScan) {
                    if (clientPlayer.equals(part.rider)) {
                        InterfaceManager.packetInterface.sendToServer(new PacketPartSeat((PartSeat) part));
                    }
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
            }
        }
    }

    private static void controlJoystick(EntityVehicleF_Physics vehicle, ControlsKeyboard joystickInhibit) {
        if (joystickInhibit.isPressed()) {
            joysticksInhibited = !joysticksInhibited;
            InterfaceManager.inputInterface.inhibitJoysticks(joysticksInhibited);
        }
    }

    private static void controlControlSurface(EntityVehicleF_Physics vehicle, ControlsJoystick axis, ControlsKeyboard increment, ControlsKeyboard decrement, double rate, double bounds, String variable, double currentValue, double dampenRate) {
        if (InterfaceManager.inputInterface.isJoystickPresent(axis.config.joystickName)) {
            double axisValue = axis.getAxisState(false);
            if (Double.isNaN(axisValue)) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle, variable, 0));
            } else {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle, variable, bounds * (-1 + 2 * axisValue)));
            }
        } else {
            if (increment.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(vehicle, variable, rate * (currentValue < 0 ? 2 : 1), -bounds, bounds));
                InterfaceManager.packetInterface.sendToServer(new PacketVehicleControlNotification(vehicle, clientPlayer));
            } else if (decrement.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(vehicle, variable, -rate * (currentValue > 0 ? 2 : 1), -bounds, bounds));
                InterfaceManager.packetInterface.sendToServer(new PacketVehicleControlNotification(vehicle, clientPlayer));
            } else if (clientPlayer.equals(vehicle.lastController)) {
                if (currentValue > dampenRate) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(vehicle, variable, -dampenRate, 0, bounds));
                } else if (currentValue < -dampenRate) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(vehicle, variable, dampenRate, -bounds, 0));
                } else if (currentValue != 0) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicle, variable, 0));
                }
            }
        }
    }

    private static void controlControlTrim(EntityVehicleF_Physics vehicle, ControlsJoystick increment, ControlsJoystick decrement, double bounds, String variable) {
        if (increment.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(vehicle, variable, 0.1, -bounds, bounds));
        } else if (decrement.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(vehicle, variable, -0.1, -bounds, bounds));
        }
    }

    private static void controlAircraft(EntityVehicleF_Physics aircraft, boolean isPlayerController) {
        controlCamera(ControlsKeyboard.AIRCRAFT_ZOOM_I, ControlsKeyboard.AIRCRAFT_ZOOM_O, ControlsJoystick.AIRCRAFT_CHANGEVIEW);
        rotateCamera(ControlsJoystick.AIRCRAFT_LOOK_R, ControlsJoystick.AIRCRAFT_LOOK_L, ControlsJoystick.AIRCRAFT_LOOK_U, ControlsJoystick.AIRCRAFT_LOOK_D, ControlsJoystick.AIRCRAFT_LOOK_A);
        controlGun(aircraft, ControlsKeyboard.AIRCRAFT_GUN_FIRE, ControlsKeyboard.AIRCRAFT_GUN_SWITCH);
        controlRadio(aircraft, ControlsKeyboard.AIRCRAFT_RADIO);
        controlJoystick(aircraft, ControlsKeyboard.AIRCRAFT_JS_INHIBIT);

        if (!isPlayerController) {
            return;
        }
        //Open or close the panel.
        if (ControlsKeyboard.AIRCRAFT_PANEL.isPressed()) {
            if (aircraft.canPlayerStartEngines(clientPlayer)) {
                if (AGUIBase.activeInputGUI instanceof GUIPanelAircraft) {
                    AGUIBase.activeInputGUI.close();
                } else if (!InterfaceManager.clientInterface.isGUIOpen()) {
                    new GUIPanelAircraft(aircraft);
                }
            }
        }

        //Check brake status.
        controlBrake(aircraft, ControlsKeyboardDynamic.AIRCRAFT_PARK, ControlsJoystick.AIRCRAFT_BRAKE, ControlsJoystick.AIRCRAFT_BRAKE_DIGITAL, ControlsJoystick.AIRCRAFT_PARK);

        //Check for thrust reverse button.
        if (ControlsJoystick.AIRCRAFT_REVERSE.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(aircraft, EntityVehicleF_Physics.REVERSE_THRUST_VARIABLE));
        }

        //Check for gear button.
        if (ControlsJoystick.AIRCRAFT_GEAR.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(aircraft, EntityVehicleF_Physics.GEAR_VARIABLE));
        }

        //Increment or decrement throttle.
        if (InterfaceManager.inputInterface.isJoystickPresent(ControlsJoystick.AIRCRAFT_THROTTLE.config.joystickName)) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft, EntityVehicleF_Physics.THROTTLE_VARIABLE, ControlsJoystick.AIRCRAFT_THROTTLE.getAxisState(true) * EntityVehicleF_Physics.MAX_THROTTLE));
        } else {
            if (ControlsKeyboard.AIRCRAFT_THROTTLE_U.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(aircraft, EntityVehicleF_Physics.THROTTLE_VARIABLE, EntityVehicleF_Physics.MAX_THROTTLE / 100D, 0, EntityVehicleF_Physics.MAX_THROTTLE));
            }
            if (ControlsKeyboard.AIRCRAFT_THROTTLE_D.isPressed()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(aircraft, EntityVehicleF_Physics.THROTTLE_VARIABLE, -EntityVehicleF_Physics.MAX_THROTTLE / 100D, 0, EntityVehicleF_Physics.MAX_THROTTLE));
            }
        }

        //Check flaps.
        if (aircraft.definition.motorized.flapNotches != null && !aircraft.definition.motorized.flapNotches.isEmpty()) {
            int currentFlapSetting = aircraft.definition.motorized.flapNotches.indexOf((float) aircraft.flapDesiredAngle);
            if (ControlsKeyboard.AIRCRAFT_FLAPS_D.isPressed() && currentFlapSetting + 1 < aircraft.definition.motorized.flapNotches.size()) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft, EntityVehicleF_Physics.FLAPS_VARIABLE, aircraft.definition.motorized.flapNotches.get(currentFlapSetting + 1)));
            } else if (ControlsKeyboard.AIRCRAFT_FLAPS_U.isPressed() && currentFlapSetting > 0) {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft, EntityVehicleF_Physics.FLAPS_VARIABLE, aircraft.definition.motorized.flapNotches.get(currentFlapSetting - 1)));
            }
        }

        //Check yaw.
        controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_YAW, ControlsKeyboard.AIRCRAFT_YAW_R, ControlsKeyboard.AIRCRAFT_YAW_L, ConfigSystem.client.controlSettings.steeringControlRate.value, EntityVehicleF_Physics.MAX_RUDDER_ANGLE, EntityVehicleF_Physics.RUDDER_INPUT_VARIABLE, aircraft.rudderInput, EntityVehicleF_Physics.RUDDER_DAMPEN_RATE);
        controlControlTrim(aircraft, ControlsJoystick.AIRCRAFT_TRIM_YAW_R, ControlsJoystick.AIRCRAFT_TRIM_YAW_L, EntityVehicleF_Physics.MAX_RUDDER_TRIM, EntityVehicleF_Physics.RUDDER_TRIM_VARIABLE);

        //Check pitch.
        controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_PITCH, ControlsKeyboard.AIRCRAFT_PITCH_U, ControlsKeyboard.AIRCRAFT_PITCH_D, ConfigSystem.client.controlSettings.flightControlRate.value, EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE, EntityVehicleF_Physics.ELEVATOR_INPUT_VARIABLE, aircraft.elevatorInput, EntityVehicleF_Physics.ELEVATOR_DAMPEN_RATE);
        controlControlTrim(aircraft, ControlsJoystick.AIRCRAFT_TRIM_PITCH_U, ControlsJoystick.AIRCRAFT_TRIM_PITCH_D, EntityVehicleF_Physics.MAX_ELEVATOR_TRIM, EntityVehicleF_Physics.ELEVATOR_TRIM_VARIABLE);

        //Check roll.
        controlControlSurface(aircraft, ControlsJoystick.AIRCRAFT_ROLL, ControlsKeyboard.AIRCRAFT_ROLL_R, ControlsKeyboard.AIRCRAFT_ROLL_L, ConfigSystem.client.controlSettings.flightControlRate.value, EntityVehicleF_Physics.MAX_AILERON_ANGLE, EntityVehicleF_Physics.AILERON_INPUT_VARIABLE, aircraft.aileronInput, EntityVehicleF_Physics.AILERON_DAMPEN_RATE);
        controlControlTrim(aircraft, ControlsJoystick.AIRCRAFT_TRIM_ROLL_R, ControlsJoystick.AIRCRAFT_TRIM_ROLL_L, EntityVehicleF_Physics.MAX_AILERON_TRIM, EntityVehicleF_Physics.AILERON_TRIM_VARIABLE);

        //Check to see if we request a different auto-level state.
        boolean aircraftIsAutolevel = aircraft.getVariable(EntityVehicleF_Physics.AUTOLEVEL_VARIABLE) != 0;
        if (ConfigSystem.client.controlSettings.heliAutoLevel.value ^ aircraftIsAutolevel) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(aircraft, EntityVehicleF_Physics.AUTOLEVEL_VARIABLE, ConfigSystem.client.controlSettings.heliAutoLevel.value ? 1 : 0));
        }
    }

    private static void controlGroundVehicle(EntityVehicleF_Physics powered, boolean isPlayerController) {
        controlCamera(ControlsKeyboard.CAR_ZOOM_I, ControlsKeyboard.CAR_ZOOM_O, ControlsJoystick.CAR_CHANGEVIEW);
        rotateCamera(ControlsJoystick.CAR_LOOK_R, ControlsJoystick.CAR_LOOK_L, ControlsJoystick.CAR_LOOK_U, ControlsJoystick.CAR_LOOK_D, ControlsJoystick.CAR_LOOK_A);
        controlGun(powered, ControlsKeyboard.CAR_GUN_FIRE, ControlsKeyboard.CAR_GUN_SWITCH);
        controlRadio(powered, ControlsKeyboard.CAR_RADIO);
        controlJoystick(powered, ControlsKeyboard.CAR_JS_INHIBIT);

        if (!isPlayerController) {
            return;
        }
        //Open or close the panel.
        if (ControlsKeyboard.CAR_PANEL.isPressed()) {
            if (powered.canPlayerStartEngines(clientPlayer)) {
                if (AGUIBase.activeInputGUI instanceof GUIPanelGround) {
                    AGUIBase.activeInputGUI.close();
                } else if (!InterfaceManager.clientInterface.isGUIOpen()) {
                    new GUIPanelGround(powered);
                }
            }
        }

        //Check brake and gas.  Depends on how the controls are configured.
        if (powered.definition.motorized.hasIncrementalThrottle) {
            //Check brake and gas.  Brake always changes, gas goes up-down.
            controlBrake(powered, ControlsKeyboardDynamic.CAR_PARK, ControlsJoystick.CAR_BRAKE, ControlsJoystick.CAR_BRAKE_DIGITAL, ControlsJoystick.CAR_PARK);
            if (InterfaceManager.inputInterface.isJoystickPresent(ControlsJoystick.CAR_GAS.config.joystickName)) {
                //Send throttle over if throttle if cruise control is off, or if throttle is less than the axis level.
                double throttleLevel = ControlsJoystick.CAR_GAS.getAxisState(true) * EntityVehicleF_Physics.MAX_THROTTLE;
                if (powered.autopilotSetting == 0 || powered.throttle < throttleLevel) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered, EntityVehicleF_Physics.THROTTLE_VARIABLE, throttleLevel));
                }
            } else {
                if (ControlsKeyboard.CAR_GAS.isPressed()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(powered, EntityVehicleF_Physics.THROTTLE_VARIABLE, EntityVehicleF_Physics.MAX_THROTTLE / 100D, 0, EntityVehicleF_Physics.MAX_THROTTLE));
                }
                if (ControlsKeyboard.CAR_BRAKE.isPressed() || ControlsJoystick.CAR_BRAKE_DIGITAL.isPressed()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableIncrement(powered, EntityVehicleF_Physics.THROTTLE_VARIABLE, -EntityVehicleF_Physics.MAX_THROTTLE / 100D, 0, EntityVehicleF_Physics.MAX_THROTTLE));
                }
            }
        } else {
            if (ConfigSystem.client.controlSettings.simpleThrottle.value) {
                if (!powered.engines.isEmpty()) {
                    //Get the brake value.
                    final double brakeValue;
                    if (InterfaceManager.inputInterface.isJoystickPresent(ControlsJoystick.CAR_BRAKE.config.joystickName)) {
                        brakeValue = ControlsJoystick.CAR_BRAKE.getAxisState(true);
                    } else if (ControlsKeyboard.CAR_BRAKE.isPressed() || ControlsJoystick.CAR_BRAKE_DIGITAL.isPressed()) {
                        brakeValue = EntityVehicleF_Physics.MAX_BRAKE;
                    } else {
                        brakeValue = 0;
                    }

                    //Get the throttle value.
                    final double throttleValue;
                    if (InterfaceManager.inputInterface.isJoystickPresent(ControlsJoystick.CAR_GAS.config.joystickName)) {
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
                    //and invert controls if we are in a reverse gear.
                    //Use only the first engine for this.
                    if (throttleValue == 0 && brakeValue == 0 && powered.axialVelocity < PartEngine.MAX_SHIFT_SPEED) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered, EntityVehicleF_Physics.BRAKE_VARIABLE, EntityVehicleF_Physics.MAX_BRAKE));
                    } else if (powered.engines.get(0).currentGear >= 0) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered, EntityVehicleF_Physics.BRAKE_VARIABLE, brakeValue));
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered, EntityVehicleF_Physics.THROTTLE_VARIABLE, throttleValue));
                    } else {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered, EntityVehicleF_Physics.BRAKE_VARIABLE, throttleValue));
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered, EntityVehicleF_Physics.THROTTLE_VARIABLE, brakeValue));
                    }

                    powered.engines.forEach(engine -> {
                        //If we don't have velocity, and we have the appropriate control, shift.
                        if (brakeValue > EntityVehicleF_Physics.MAX_BRAKE / 4F && engine.currentGear >= 0 && powered.axialVelocity < 0.01F) {
                            if (engine.currentGear > 0) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.NEUTRAL_SHIFT_VARIABLE));
                            } else {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.DOWN_SHIFT_VARIABLE));
                            }
                        } else if (throttleValue > EntityVehicleF_Physics.MAX_THROTTLE / 4F && engine.currentGear <= 0 && powered.axialVelocity < 0.01F) {
                            if (engine.currentGear < 0) {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.NEUTRAL_SHIFT_VARIABLE));
                            } else {
                                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.UP_SHIFT_VARIABLE));
                            }
                        }
                    });
                }
            } else {
                //Check brake and gas and set to on or off.
                controlBrake(powered, ControlsKeyboardDynamic.CAR_PARK, ControlsJoystick.CAR_BRAKE, ControlsJoystick.CAR_BRAKE_DIGITAL, ControlsJoystick.CAR_PARK);
                if (InterfaceManager.inputInterface.isJoystickPresent(ControlsJoystick.CAR_GAS.config.joystickName)) {
                    //Send throttle over if throttle if cruise control is off, or if throttle is less than the axis level.
                    double throttleLevel = ControlsJoystick.CAR_GAS.getAxisState(true);
                    if (powered.autopilotSetting == 0 || powered.throttle < throttleLevel) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered, EntityVehicleF_Physics.THROTTLE_VARIABLE, throttleLevel));
                    }
                } else {
                    if (ControlsKeyboardDynamic.CAR_SLOW.isPressed()) {
                        if (!ConfigSystem.client.controlSettings.halfThrottle.value) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered, EntityVehicleF_Physics.THROTTLE_VARIABLE, EntityVehicleF_Physics.MAX_THROTTLE / 2D));
                        } else {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered, EntityVehicleF_Physics.THROTTLE_VARIABLE, EntityVehicleF_Physics.MAX_THROTTLE));
                        }
                    } else if (ControlsKeyboard.CAR_GAS.isPressed()) {
                        if (!ConfigSystem.client.controlSettings.halfThrottle.value) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered, EntityVehicleF_Physics.THROTTLE_VARIABLE, EntityVehicleF_Physics.MAX_THROTTLE));
                        } else {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered, EntityVehicleF_Physics.THROTTLE_VARIABLE, EntityVehicleF_Physics.MAX_THROTTLE / 2D));
                        }
                    } else {
                        //Send gas off packet if we don't have cruise on.
                        if (powered.autopilotSetting == 0) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(powered, EntityVehicleF_Physics.THROTTLE_VARIABLE, 0D));
                        }
                    }
                }
            }
        }

        //Check steering.  Don't check while on a road, since we auto-drive on those.
        if (!powered.lockedOnRoad) {
            controlControlSurface(powered, ControlsJoystick.CAR_TURN, ControlsKeyboard.CAR_TURN_R, ControlsKeyboard.CAR_TURN_L, ConfigSystem.client.controlSettings.steeringControlRate.value, EntityVehicleF_Physics.MAX_RUDDER_ANGLE, EntityVehicleF_Physics.RUDDER_INPUT_VARIABLE, powered.rudderInput, EntityVehicleF_Physics.RUDDER_DAMPEN_RATE);
        }

        //Check if we are shifting.
        if (ControlsKeyboardDynamic.CAR_SHIFT_NU.isPressed() || ControlsKeyboardDynamic.CAR_SHIFT_ND.isPressed()) {
            powered.engines.forEach(engine -> {
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.NEUTRAL_SHIFT_VARIABLE));
            });
        } else {
            if (ControlsKeyboard.CAR_SHIFT_U.isPressed()) {
                powered.engines.forEach(engine -> {
                    if (engine.definition.engine.isAutomatic) {
                        if (engine.currentGear < 0) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.NEUTRAL_SHIFT_VARIABLE));
                        } else if (engine.currentGear == 0) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.UP_SHIFT_VARIABLE));
                        }
                    } else {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.UP_SHIFT_VARIABLE));
                    }
                });
            }
            if (ControlsKeyboard.CAR_SHIFT_D.isPressed()) {
                powered.engines.forEach(engine -> {
                    if (engine.definition.engine.isAutomatic) {
                        if (engine.currentGear > 0) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.NEUTRAL_SHIFT_VARIABLE));
                        } else if (engine.currentGear == 0) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.DOWN_SHIFT_VARIABLE));
                        }
                    } else {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine, PartEngine.DOWN_SHIFT_VARIABLE));
                    }
                });
            }
        }

        //Check if horn button is pressed.
        if (ControlsKeyboard.CAR_HORN.isPressed() && !powered.isVariableActive(EntityVehicleF_Physics.HORN_VARIABLE)) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.HORN_VARIABLE));
        } else if (!ControlsKeyboard.CAR_HORN.isPressed() && powered.isVariableActive(EntityVehicleF_Physics.HORN_VARIABLE)) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.HORN_VARIABLE));
        }

        //Check for lights.
        if (ControlsKeyboard.CAR_LIGHTS.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.RUNNINGLIGHT_VARIABLE));
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.HEADLIGHT_VARIABLE));
        }
        if (ControlsKeyboard.CAR_TURNSIGNAL_L.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.LEFTTURNLIGHT_VARIABLE));
        }
        if (ControlsKeyboard.CAR_TURNSIGNAL_R.isPressed()) {
            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.RIGHTTURNLIGHT_VARIABLE));
        }

        //Change turn signal status depending on turning status.
        //Keep signals on until we have been moving without turning in the
        //pressed direction for 2 seconds, or if we turn in the other direction.
        //This only happens if the signals are set to automatic.  For manual signals, we let the player control them.
        if (ConfigSystem.client.controlSettings.autoTrnSignals.value) {
            if (!powered.turningLeft && powered.rudderInput < -20) {
                powered.turningLeft = true;
                powered.turningCooldown = 40;
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.LEFTTURNLIGHT_VARIABLE));
            }
            if (!powered.turningRight && powered.rudderInput > 20) {
                powered.turningRight = true;
                powered.turningCooldown = 40;
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.RIGHTTURNLIGHT_VARIABLE));
            }
            if (powered.turningLeft && (powered.rudderInput > 0 || powered.turningCooldown == 0)) {
                powered.turningLeft = false;
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.LEFTTURNLIGHT_VARIABLE));
            }
            if (powered.turningRight && (powered.rudderInput < 0 || powered.turningCooldown == 0)) {
                powered.turningRight = false;
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.RIGHTTURNLIGHT_VARIABLE));
            }
            if (powered.velocity != 0 && powered.turningCooldown > 0 && powered.rudderInput == 0) {
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
        AIRCRAFT_MOD(ControlsJoystick.AIRCRAFT_MOD, false, "RSHIFT", JSONConfigLanguage.INPUT_MOD),
        AIRCRAFT_YAW_R(ControlsJoystick.AIRCRAFT_YAW, false, "L", JSONConfigLanguage.INPUT_YAW_R),
        AIRCRAFT_YAW_L(ControlsJoystick.AIRCRAFT_YAW, false, "J", JSONConfigLanguage.INPUT_YAW_L),
        AIRCRAFT_PITCH_U(ControlsJoystick.AIRCRAFT_PITCH, false, "S", JSONConfigLanguage.INPUT_PITCH_U),
        AIRCRAFT_PITCH_D(ControlsJoystick.AIRCRAFT_PITCH, false, "W", JSONConfigLanguage.INPUT_PITCH_D),
        AIRCRAFT_ROLL_R(ControlsJoystick.AIRCRAFT_ROLL, false, "D", JSONConfigLanguage.INPUT_ROLL_R),
        AIRCRAFT_ROLL_L(ControlsJoystick.AIRCRAFT_ROLL, false, "A", JSONConfigLanguage.INPUT_ROLL_L),
        AIRCRAFT_THROTTLE_U(ControlsJoystick.AIRCRAFT_THROTTLE, false, "I", JSONConfigLanguage.INPUT_THROTTLE_U),
        AIRCRAFT_THROTTLE_D(ControlsJoystick.AIRCRAFT_THROTTLE, false, "K", JSONConfigLanguage.INPUT_THROTTLE_D),
        AIRCRAFT_FLAPS_U(ControlsJoystick.AIRCRAFT_FLAPS_U, true, "Y", JSONConfigLanguage.INPUT_FLAPS_U),
        AIRCRAFT_FLAPS_D(ControlsJoystick.AIRCRAFT_FLAPS_D, true, "H", JSONConfigLanguage.INPUT_FLAPS_D),
        AIRCRAFT_BRAKE(ControlsJoystick.AIRCRAFT_BRAKE, false, "B", JSONConfigLanguage.INPUT_BRAKE),
        AIRCRAFT_PANEL(ControlsJoystick.AIRCRAFT_PANEL, true, "U", JSONConfigLanguage.INPUT_PANEL),
        AIRCRAFT_RADIO(ControlsJoystick.AIRCRAFT_RADIO, true, "MINUS", JSONConfigLanguage.INPUT_RADIO),
        AIRCRAFT_GUN_FIRE(ControlsJoystick.AIRCRAFT_GUN_FIRE, false, "SPACE", JSONConfigLanguage.INPUT_GUN_FIRE),
        AIRCRAFT_GUN_SWITCH(ControlsJoystick.AIRCRAFT_GUN_SWITCH, true, "V", JSONConfigLanguage.INPUT_GUN_SWITCH),
        AIRCRAFT_ZOOM_I(ControlsJoystick.AIRCRAFT_ZOOM_I, true, "PRIOR", JSONConfigLanguage.INPUT_ZOOM_I),
        AIRCRAFT_ZOOM_O(ControlsJoystick.AIRCRAFT_ZOOM_O, true, "NEXT", JSONConfigLanguage.INPUT_ZOOM_O),
        AIRCRAFT_JS_INHIBIT(ControlsJoystick.AIRCRAFT_JS_INHIBIT, true, "SCROLL", JSONConfigLanguage.INPUT_JS_INHIBIT),

        CAR_MOD(ControlsJoystick.CAR_MOD, false, "RSHIFT", JSONConfigLanguage.INPUT_MOD),
        CAR_TURN_R(ControlsJoystick.CAR_TURN, false, "D", JSONConfigLanguage.INPUT_TURN_R),
        CAR_TURN_L(ControlsJoystick.CAR_TURN, false, "A", JSONConfigLanguage.INPUT_TURN_L),
        CAR_GAS(ControlsJoystick.CAR_GAS, false, "W", JSONConfigLanguage.INPUT_GAS),
        CAR_BRAKE(ControlsJoystick.CAR_BRAKE, false, "S", JSONConfigLanguage.INPUT_BRAKE),
        CAR_PANEL(ControlsJoystick.CAR_PANEL, true, "U", JSONConfigLanguage.INPUT_PANEL),
        CAR_SHIFT_U(ControlsJoystick.CAR_SHIFT_U, true, "R", JSONConfigLanguage.INPUT_SHIFT_U),
        CAR_SHIFT_D(ControlsJoystick.CAR_SHIFT_D, true, "F", JSONConfigLanguage.INPUT_SHIFT_D),
        CAR_HORN(ControlsJoystick.CAR_HORN, false, "C", JSONConfigLanguage.INPUT_HORN),
        CAR_RADIO(ControlsJoystick.CAR_RADIO, true, "MINUS", JSONConfigLanguage.INPUT_RADIO),
        CAR_GUN_FIRE(ControlsJoystick.CAR_GUN_FIRE, false, "SPACE", JSONConfigLanguage.INPUT_GUN_FIRE),
        CAR_GUN_SWITCH(ControlsJoystick.CAR_GUN_SWITCH, true, "V", JSONConfigLanguage.INPUT_GUN_SWITCH),
        CAR_ZOOM_I(ControlsJoystick.CAR_ZOOM_I, true, "PRIOR", JSONConfigLanguage.INPUT_ZOOM_I),
        CAR_ZOOM_O(ControlsJoystick.CAR_ZOOM_O, true, "NEXT", JSONConfigLanguage.INPUT_ZOOM_O),
        CAR_LIGHTS(ControlsJoystick.CAR_LIGHTS, true, "NUMPAD5", JSONConfigLanguage.INPUT_LIGHTS),
        CAR_TURNSIGNAL_L(ControlsJoystick.CAR_TURNSIGNAL_L, true, "NUMPAD4", JSONConfigLanguage.INPUT_TURNSIGNAL_L),
        CAR_TURNSIGNAL_R(ControlsJoystick.CAR_TURNSIGNAL_R, true, "NUMPAD6", JSONConfigLanguage.INPUT_TURNSIGNAL_R),
        CAR_JS_INHIBIT(ControlsJoystick.CAR_JS_INHIBIT, true, "SCROLL", JSONConfigLanguage.INPUT_JS_INHIBIT);

        public final boolean isMomentary;
        public final String systemName;
        public final LanguageEntry language;
        public final String defaultKeyName;
        public final ConfigKeyboard config;
        private final ControlsJoystick linkedJoystick;

        private boolean wasPressedLastCall;

        ControlsKeyboard(ControlsJoystick linkedJoystick, boolean isMomentary, String defaultKeyName, LanguageEntry language) {
            this.linkedJoystick = linkedJoystick;
            this.isMomentary = isMomentary;
            this.systemName = this.name().toLowerCase().replaceFirst("_", ".");
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
            if (linkedJoystick.isPressed()) {
                return true;
            } else if (InterfaceManager.inputInterface.isJoystickPresent(linkedJoystick.config.joystickName) && ConfigSystem.client.controlSettings.kbOverride.value) {
                return false;
            } else {
                if (isMomentary) {
                    if (wasPressedLastCall) {
                        wasPressedLastCall = InterfaceManager.inputInterface.isKeyPressed(config.keyCode);
                        return false;
                    } else {
                        wasPressedLastCall = InterfaceManager.inputInterface.isKeyPressed(config.keyCode);
                        return wasPressedLastCall;
                    }
                } else {
                    return InterfaceManager.inputInterface.isKeyPressed(config.keyCode);
                }
            }
        }
    }

    public enum ControlsJoystick {
        AIRCRAFT_MOD(false, false, JSONConfigLanguage.INPUT_MOD),
        AIRCRAFT_CAMLOCK(false, true, JSONConfigLanguage.INPUT_CAMLOCK),
        AIRCRAFT_YAW(true, false, JSONConfigLanguage.INPUT_YAW),
        AIRCRAFT_PITCH(true, false, JSONConfigLanguage.INPUT_PITCH),
        AIRCRAFT_ROLL(true, false, JSONConfigLanguage.INPUT_ROLL),
        AIRCRAFT_THROTTLE(true, false, JSONConfigLanguage.INPUT_THROTTLE),
        AIRCRAFT_BRAKE(true, false, JSONConfigLanguage.INPUT_BRAKE),
        AIRCRAFT_BRAKE_DIGITAL(false, false, JSONConfigLanguage.INPUT_BRAKE),
        AIRCRAFT_GEAR(false, true, JSONConfigLanguage.INPUT_GEAR),
        AIRCRAFT_FLAPS_U(false, true, JSONConfigLanguage.INPUT_FLAPS_U),
        AIRCRAFT_FLAPS_D(false, true, JSONConfigLanguage.INPUT_FLAPS_D),
        AIRCRAFT_PANEL(false, true, JSONConfigLanguage.INPUT_PANEL),
        AIRCRAFT_PARK(false, true, JSONConfigLanguage.INPUT_PARK),
        AIRCRAFT_RADIO(false, true, JSONConfigLanguage.INPUT_RADIO),
        AIRCRAFT_GUN_FIRE(false, false, JSONConfigLanguage.INPUT_GUN_FIRE),
        AIRCRAFT_GUN_SWITCH(false, true, JSONConfigLanguage.INPUT_GUN_SWITCH),
        AIRCRAFT_ZOOM_I(false, true, JSONConfigLanguage.INPUT_ZOOM_I),
        AIRCRAFT_ZOOM_O(false, true, JSONConfigLanguage.INPUT_ZOOM_O),
        AIRCRAFT_CHANGEVIEW(false, true, JSONConfigLanguage.INPUT_CHANGEVIEW),
        AIRCRAFT_LOOK_L(false, false, JSONConfigLanguage.INPUT_LOOK_L),
        AIRCRAFT_LOOK_R(false, false, JSONConfigLanguage.INPUT_LOOK_R),
        AIRCRAFT_LOOK_U(false, false, JSONConfigLanguage.INPUT_LOOK_U),
        AIRCRAFT_LOOK_D(false, false, JSONConfigLanguage.INPUT_LOOK_D),
        AIRCRAFT_LOOK_A(false, false, JSONConfigLanguage.INPUT_LOOK_A),
        AIRCRAFT_TRIM_YAW_R(false, false, JSONConfigLanguage.INPUT_TRIM_YAW_R),
        AIRCRAFT_TRIM_YAW_L(false, false, JSONConfigLanguage.INPUT_TRIM_YAW_L),
        AIRCRAFT_TRIM_PITCH_U(false, false, JSONConfigLanguage.INPUT_TRIM_PITCH_U),
        AIRCRAFT_TRIM_PITCH_D(false, false, JSONConfigLanguage.INPUT_TRIM_PITCH_D),
        AIRCRAFT_TRIM_ROLL_R(false, false, JSONConfigLanguage.INPUT_TRIM_ROLL_R),
        AIRCRAFT_TRIM_ROLL_L(false, false, JSONConfigLanguage.INPUT_TRIM_ROLL_L),
        AIRCRAFT_REVERSE(false, true, JSONConfigLanguage.INPUT_REVERSE),
        AIRCRAFT_JS_INHIBIT(false, true, JSONConfigLanguage.INPUT_JS_INHIBIT),

        CAR_MOD(false, false, JSONConfigLanguage.INPUT_MOD),
        CAR_CAMLOCK(false, true, JSONConfigLanguage.INPUT_CAMLOCK),
        CAR_TURN(true, false, JSONConfigLanguage.INPUT_TURN),
        CAR_GAS(true, false, JSONConfigLanguage.INPUT_GAS),
        CAR_BRAKE(true, false, JSONConfigLanguage.INPUT_BRAKE),
        CAR_BRAKE_DIGITAL(false, false, JSONConfigLanguage.INPUT_BRAKE),
        CAR_PANEL(false, true, JSONConfigLanguage.INPUT_PANEL),
        CAR_SHIFT_U(false, true, JSONConfigLanguage.INPUT_SHIFT_U),
        CAR_SHIFT_D(false, true, JSONConfigLanguage.INPUT_SHIFT_D),
        CAR_HORN(false, false, JSONConfigLanguage.INPUT_HORN),
        CAR_PARK(false, true, JSONConfigLanguage.INPUT_PARK),
        CAR_RADIO(false, true, JSONConfigLanguage.INPUT_RADIO),
        CAR_GUN_FIRE(false, false, JSONConfigLanguage.INPUT_GUN_FIRE),
        CAR_GUN_SWITCH(false, true, JSONConfigLanguage.INPUT_GUN_SWITCH),
        CAR_ZOOM_I(false, true, JSONConfigLanguage.INPUT_ZOOM_I),
        CAR_ZOOM_O(false, true, JSONConfigLanguage.INPUT_ZOOM_O),
        CAR_CHANGEVIEW(false, true, JSONConfigLanguage.INPUT_CHANGEVIEW),
        CAR_LOOK_L(false, false, JSONConfigLanguage.INPUT_LOOK_L),
        CAR_LOOK_R(false, false, JSONConfigLanguage.INPUT_LOOK_R),
        CAR_LOOK_U(false, false, JSONConfigLanguage.INPUT_LOOK_U),
        CAR_LOOK_D(false, false, JSONConfigLanguage.INPUT_LOOK_D),
        CAR_LOOK_A(false, false, JSONConfigLanguage.INPUT_LOOK_A),
        CAR_LIGHTS(false, true, JSONConfigLanguage.INPUT_LIGHTS),
        CAR_TURNSIGNAL_L(false, true, JSONConfigLanguage.INPUT_TURNSIGNAL_L),
        CAR_TURNSIGNAL_R(false, true, JSONConfigLanguage.INPUT_TURNSIGNAL_R),
        CAR_JS_INHIBIT(false, true, JSONConfigLanguage.INPUT_JS_INHIBIT);

        public final boolean isAxis;
        public final boolean isMomentary;
        public final String systemName;
        public final LanguageEntry language;
        public final ConfigJoystick config;

        private boolean wasPressedLastCall;

        ControlsJoystick(boolean isAxis, boolean isMomentary, LanguageEntry language) {
            this.isAxis = isAxis;
            this.isMomentary = isMomentary;
            this.systemName = this.name().toLowerCase().replaceFirst("_", ".");
            this.language = language;
            if (ConfigSystem.client.controls.joystick.containsKey(systemName)) {
                this.config = ConfigSystem.client.controls.joystick.get(systemName);
            } else {
                this.config = new ConfigJoystick();
            }
        }

        public boolean isPressed() {
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
        }

        private float getMultistateValue() {
            return InterfaceManager.inputInterface.getJoystickAxisValue(config.joystickName, config.buttonIndex);
        }

        private double getAxisState(boolean ignoreDeadzone) {
            double pollValue = getMultistateValue();
            if (ignoreDeadzone || Math.abs(pollValue) > ConfigSystem.client.controlSettings.joystickDeadZone.value) {
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
                return Double.NaN;
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
        AIRCRAFT_PARK(ControlsKeyboard.AIRCRAFT_BRAKE, ControlsKeyboard.AIRCRAFT_MOD, JSONConfigLanguage.INPUT_PARK),

        CAR_PARK(ControlsKeyboard.CAR_BRAKE, ControlsKeyboard.CAR_MOD, JSONConfigLanguage.INPUT_PARK),
        CAR_SLOW(ControlsKeyboard.CAR_GAS, ControlsKeyboard.CAR_MOD, JSONConfigLanguage.INPUT_SLOW),
        CAR_SHIFT_NU(ControlsKeyboard.CAR_SHIFT_U, ControlsKeyboard.CAR_MOD, JSONConfigLanguage.INPUT_SHIFT_N),
        CAR_SHIFT_ND(ControlsKeyboard.CAR_SHIFT_D, ControlsKeyboard.CAR_MOD, JSONConfigLanguage.INPUT_SHIFT_N);

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