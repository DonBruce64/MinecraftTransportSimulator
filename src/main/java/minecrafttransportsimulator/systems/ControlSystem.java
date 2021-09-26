package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.guis.instances.GUIPanelAircraft;
import minecrafttransportsimulator.guis.instances.GUIPanelGround;
import minecrafttransportsimulator.guis.instances.GUIRadio;
import minecrafttransportsimulator.jsondefs.JSONConfig.ConfigJoystick;
import minecrafttransportsimulator.jsondefs.JSONConfig.ConfigKeyboard;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.mcinterface.InterfaceInput;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketPartGun;
import minecrafttransportsimulator.packets.instances.PacketPartSeat;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;

/**Class that handles all control operations.
 * 
 * @author don_bruce
 */
public final class ControlSystem{	
	private static final int NULL_COMPONENT = 999;	
	private static boolean joysticksInhibited = false;
	private static WrapperPlayer clientPlayer;
	
	/**
	 * Static initializer for the wrapper inputs, as we need to iterate through the enums to initialize them
	 * prior to using them in any of the methods contained in this wrapper (cause they'll be null).
	 * Joystick enums need to come first, as the Keyboard enums take them as constructor args.
	 * After we initialize the keboard enums, we set their default values.
	 * Once all this is done, save the results back to the disk to ensure the systems are synced.
	 * Note that since this class won't be called until the world loads because we won't process inputs
	 * out-of-world, it can be assumed that the ConfigSystem has already been initialized.
	 */
	static{
		for(ControlsJoystick control : ControlsJoystick.values()){
			ConfigSystem.configObject.controls.joystick.put(control.systemName, control.config);
		}
		for(ControlsKeyboard control : ControlsKeyboard.values()){
			ConfigSystem.configObject.controls.keyboard.put(control.systemName, control.config);
		}
		for(ControlsKeyboard control : ControlsKeyboard.values()){
			if(control.config.keyCode == 0){
				control.config.keyCode = InterfaceInput.getKeyCodeForName(control.defaultKeyName);
			}
		}
		ConfigSystem.saveToDisk();
	}

	
	public static void controlPlayerGun(EntityPlayerGun entity){
		//Don't send state changes unless we're holding a gun.
		if(entity.activeGun != null){
			InterfacePacket.sendToServer(new PacketPartGun(entity.activeGun, InterfaceInput.isLeftMouseButtonDown() && !entity.player.isSpectator(), InterfaceInput.isRightMouseButtonDown()));
		}
	}
	
	public static void controlVehicle(EntityVehicleF_Physics vehicle, boolean isPlayerController){
		clientPlayer = InterfaceClient.getClientPlayer();
		if(vehicle.definition.motorized.isAircraft){
			controlAircraft(vehicle, isPlayerController);
		}else{
			controlGroundVehicle(vehicle, isPlayerController);
		}
	}
	
	private static void controlCamera(ControlsKeyboard camLock, ControlsKeyboard zoomIn, ControlsKeyboard zoomOut, ControlsJoystick changeView){
		if(camLock.isPressed()){
			EntityVehicleF_Physics.lockCameraToMovement = !EntityVehicleF_Physics.lockCameraToMovement; 
		}
		
		if(zoomIn.isPressed()){
			CameraSystem.changeCameraZoom(true);
		}
		if(zoomOut.isPressed()){
			CameraSystem.changeCameraZoom(false);
		}
		
		if(changeView.isPressed()){
			InterfaceClient.toggleFirstPerson();
		}
	}
	
	private static void rotateCamera(ControlsJoystick lookR, ControlsJoystick lookL, ControlsJoystick lookU, ControlsJoystick lookD, ControlsJoystick lookA){
		if(lookR.isPressed()){
			clientPlayer.setYaw(clientPlayer.getYaw() - 3);
		}
		if(lookL.isPressed()){
			clientPlayer.setYaw(clientPlayer.getYaw() + 3);
		}
		if(lookU.isPressed()){
			clientPlayer.setPitch(clientPlayer.getPitch() - 3);
		}
		if(lookD.isPressed()){
			clientPlayer.setPitch(clientPlayer.getPitch() + 3);
		}
		
		float pollData = lookA.getMultistateValue();
		if(pollData != 0){
			if(pollData >= 0.125F && pollData <= 0.375F){
				clientPlayer.setPitch(clientPlayer.getPitch() + 3);
			}
			if(pollData >= 0.375F && pollData <= 0.625F){
				clientPlayer.setYaw(clientPlayer.getYaw() - 3);
			}
			if(pollData >= 0.625F && pollData <= 0.875F){
				clientPlayer.setPitch(clientPlayer.getPitch() - 3);
			}
			if(pollData >= 0.875F || pollData <= 0.125F){
				clientPlayer.setYaw(clientPlayer.getYaw() + 3);
			}
		}
	}
	
	private static void controlBrake(EntityVehicleF_Physics vehicle, ControlsKeyboardDynamic brakeMod, ControlsJoystick brakeJoystick, ControlsJoystick brakeButton, ControlsJoystick pBrake){
		//If the analog brake is set, do brake state based on that rather than the keyboard.
		boolean isParkingBrakePressed = InterfaceInput.isJoystickPresent(brakeJoystick.config.joystickName) ? pBrake.isPressed() : brakeMod.isPressed() || pBrake.isPressed();
		byte brakeValue = InterfaceInput.isJoystickPresent(brakeJoystick.config.joystickName) ? (byte) brakeJoystick.getAxisState((short) 0) : (brakeMod.mainControl.isPressed() || brakeButton.isPressed() ? EntityVehicleF_Physics.MAX_BRAKE : 0);
		if(isParkingBrakePressed ? !vehicle.parkingBrakeOn :  (brakeValue > 0 && vehicle.parkingBrakeOn)){
			InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, EntityVehicleF_Physics.PARKINGBRAKE_VARIABLE));
		}
		InterfacePacket.sendToServer(new PacketVehicleControlAnalog(vehicle, PacketVehicleControlAnalog.Controls.BRAKE, brakeValue, Byte.MAX_VALUE));
	}
	
	private static void controlGun(EntityVehicleF_Physics vehicle, ControlsKeyboard gunTrigger, ControlsKeyboard gunSwitch){
		boolean gunSwitchPressedThisScan = gunSwitch.isPressed();
		for(APart part : vehicle.parts){
			if(part instanceof PartGun){
				PartGun gun = (PartGun) part;
				if(InterfaceClient.getClientPlayer().equals(gun.getController())){
					InterfacePacket.sendToServer(new PacketPartGun(gun, gunTrigger.isPressed(), false));
				}
			}else if(part instanceof PartSeat){
				if(gunSwitchPressedThisScan){
					if(InterfaceClient.getClientPlayer().equals(vehicle.locationRiderMap.get(part.placementOffset))){
						InterfacePacket.sendToServer(new PacketPartSeat((PartSeat) part));
					}
				}
			}
		}
	}
	
	private static void controlRadio(EntityVehicleF_Physics vehicle, ControlsKeyboard radio){
		if(radio.isPressed()){
			if(InterfaceGUI.getActiveGUI() instanceof GUIRadio){
				InterfaceGUI.closeGUI();
			}else if(InterfaceGUI.getActiveGUI() == null){
				InterfaceGUI.openGUI(new GUIRadio(vehicle.radio));
			}
		}
	}
	
	private static void controlJoystick(EntityVehicleF_Physics vehicle, ControlsKeyboard joystickInhibit){
		if(joystickInhibit.isPressed()){
			joysticksInhibited = !joysticksInhibited;
			InterfaceInput.inhibitJoysticks(joysticksInhibited);
		}
	}
	
	private static void controlAircraft(EntityVehicleF_Physics aircraft, boolean isPlayerController){
		controlCamera(ControlsKeyboard.AIRCRAFT_CAMLOCK, ControlsKeyboard.AIRCRAFT_ZOOM_I, ControlsKeyboard.AIRCRAFT_ZOOM_O, ControlsJoystick.AIRCRAFT_CHANGEVIEW);
		rotateCamera(ControlsJoystick.AIRCRAFT_LOOK_R, ControlsJoystick.AIRCRAFT_LOOK_L, ControlsJoystick.AIRCRAFT_LOOK_U, ControlsJoystick.AIRCRAFT_LOOK_D, ControlsJoystick.AIRCRAFT_LOOK_A);
		controlGun(aircraft, ControlsKeyboard.AIRCRAFT_GUN_FIRE, ControlsKeyboard.AIRCRAFT_GUN_SWITCH);
		controlRadio(aircraft, ControlsKeyboard.AIRCRAFT_RADIO);
		controlJoystick(aircraft, ControlsKeyboard.AIRCRAFT_JS_INHIBIT);
		
		if(!isPlayerController){
			return;
		}		
		//Open or close the panel.
		if(ControlsKeyboard.AIRCRAFT_PANEL.isPressed()){
			if(InterfaceGUI.getActiveGUI() instanceof GUIPanelAircraft){
				InterfaceGUI.closeGUI();
			}else if(InterfaceGUI.getActiveGUI() == null){
				InterfaceGUI.openGUI(new GUIPanelAircraft(aircraft));
			}
		}
		
		//Check brake status.
		controlBrake(aircraft, ControlsKeyboardDynamic.AIRCRAFT_PARK, ControlsJoystick.AIRCRAFT_BRAKE, ControlsJoystick.AIRCRAFT_BRAKE_DIGITAL, ControlsJoystick.AIRCRAFT_PARK);
		
		//Check for thrust reverse button.
		if(ControlsJoystick.AIRCRAFT_REVERSE.isPressed()){
			InterfacePacket.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.REVERSE, !aircraft.reverseThrust));
		}
		
		//Increment or decrement throttle.
		if(InterfaceInput.isJoystickPresent(ControlsJoystick.AIRCRAFT_THROTTLE.config.joystickName)){
			InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.THROTTLE, ControlsJoystick.AIRCRAFT_THROTTLE.getAxisState((short) 0), Byte.MAX_VALUE));
		}else{
			if(ControlsKeyboard.AIRCRAFT_THROTTLE_U.isPressed()){
				InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 1, (byte) 0));
			}
			if(ControlsKeyboard.AIRCRAFT_THROTTLE_D.isPressed()){
				InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.THROTTLE, (short) -1, (byte) 0));
			}
		}		
		
		//Check flaps.
		if(aircraft.definition.motorized.flapNotches != null && !aircraft.definition.motorized.flapNotches.isEmpty()){
			if(ControlsKeyboard.AIRCRAFT_FLAPS_U.isPressed()){
				InterfacePacket.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.FLAPS, false));
			}
			if(ControlsKeyboard.AIRCRAFT_FLAPS_D.isPressed()){
				InterfacePacket.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.FLAPS, true));
			}
		}
		
		//Check yaw.
		if(InterfaceInput.isJoystickPresent(ControlsJoystick.AIRCRAFT_YAW.config.joystickName)){
			InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.RUDDER, ControlsJoystick.AIRCRAFT_YAW.getAxisState(EntityVehicleF_Physics.MAX_RUDDER_ANGLE), Byte.MAX_VALUE));
		}else{
			if(ControlsKeyboard.AIRCRAFT_YAW_R.isPressed()){
				InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.RUDDER, (short) (ConfigSystem.configObject.clientControls.steeringIncrement.value.shortValue()*(aircraft.rudderAngle < 0 ? 2 : 1)), ConfigSystem.configObject.clientControls.controlSurfaceCooldown.value.byteValue()));
			}
			if(ControlsKeyboard.AIRCRAFT_YAW_L.isPressed()){
				InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.RUDDER, (short) (-ConfigSystem.configObject.clientControls.steeringIncrement.value.shortValue()*(aircraft.rudderAngle > 0 ? 2 : 1)), ConfigSystem.configObject.clientControls.controlSurfaceCooldown.value.byteValue()));
			}
		}
		if(ControlsJoystick.AIRCRAFT_TRIM_YAW_R.isPressed()){
			InterfacePacket.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.TRIM_YAW, true));
		}
		if(ControlsJoystick.AIRCRAFT_TRIM_YAW_L.isPressed()){
			InterfacePacket.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.TRIM_YAW, false));
		}
		
		//Check is mouse yoke is enabled.  If so do controls by mouse rather than buttons.
		if(ConfigSystem.configObject.clientControls.mouseYoke.value){
			if(EntityVehicleF_Physics.lockCameraToMovement && InterfaceGUI.getActiveGUI() == null){
				long mousePosition = InterfaceInput.getTrackedMouseInfo();
				InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.AILERON, (short) (mousePosition >> Integer.SIZE), Byte.MAX_VALUE));
				InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.ELEVATOR, (short) ((int) -mousePosition), Byte.MAX_VALUE));
				
			}
		}else{
			//Check pitch.
			if(InterfaceInput.isJoystickPresent(ControlsJoystick.AIRCRAFT_PITCH.config.joystickName)){
				InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.ELEVATOR, ControlsJoystick.AIRCRAFT_PITCH.getAxisState(EntityVehicleF_Physics.MAX_ELEVATOR_ANGLE), Byte.MAX_VALUE));
			}else{
				if(ControlsKeyboard.AIRCRAFT_PITCH_U.isPressed()){
					InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.ELEVATOR, (short) (ConfigSystem.configObject.clientControls.flightIncrement.value.shortValue()*(aircraft.elevatorAngle < 0 ? 2 : 1)), ConfigSystem.configObject.clientControls.controlSurfaceCooldown.value.byteValue()));
				}
				if(ControlsKeyboard.AIRCRAFT_PITCH_D.isPressed()){
					InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.ELEVATOR, (short) (-ConfigSystem.configObject.clientControls.flightIncrement.value.shortValue()*(aircraft.elevatorAngle > 0 ? 2 : 1)), ConfigSystem.configObject.clientControls.controlSurfaceCooldown.value.byteValue()));
				}
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_PITCH_U.isPressed()){
				InterfacePacket.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.TRIM_PITCH, true));
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_PITCH_D.isPressed()){
				InterfacePacket.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.TRIM_PITCH, false));
			}
			
			//Check roll.
			if(InterfaceInput.isJoystickPresent(ControlsJoystick.AIRCRAFT_ROLL.config.joystickName)){
				InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.AILERON, ControlsJoystick.AIRCRAFT_ROLL.getAxisState(EntityVehicleF_Physics.MAX_AILERON_ANGLE), Byte.MAX_VALUE));
			}else{
				if(ControlsKeyboard.AIRCRAFT_ROLL_R.isPressed()){
					InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.AILERON, (short) (ConfigSystem.configObject.clientControls.flightIncrement.value.shortValue()*(aircraft.aileronAngle < 0 ? 2 : 1)), ConfigSystem.configObject.clientControls.controlSurfaceCooldown.value.byteValue()));
				}
				if(ControlsKeyboard.AIRCRAFT_ROLL_L.isPressed()){
					InterfacePacket.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.AILERON, (short) (-ConfigSystem.configObject.clientControls.flightIncrement.value.shortValue()*(aircraft.aileronAngle > 0 ? 2 : 1)), ConfigSystem.configObject.clientControls.controlSurfaceCooldown.value.byteValue()));
				}
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_ROLL_R.isPressed()){
				InterfacePacket.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.TRIM_ROLL, true));
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_ROLL_L.isPressed()){
				InterfacePacket.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.TRIM_ROLL, false));
			}
		}
	}
	
	private static void controlGroundVehicle(EntityVehicleF_Physics powered, boolean isPlayerController){
		controlCamera(ControlsKeyboard.CAR_CAMLOCK, ControlsKeyboard.CAR_ZOOM_I, ControlsKeyboard.CAR_ZOOM_O, ControlsJoystick.CAR_CHANGEVIEW);
		rotateCamera(ControlsJoystick.CAR_LOOK_R, ControlsJoystick.CAR_LOOK_L, ControlsJoystick.CAR_LOOK_U, ControlsJoystick.CAR_LOOK_D, ControlsJoystick.CAR_LOOK_A);
		controlGun(powered, ControlsKeyboard.CAR_GUN_FIRE, ControlsKeyboard.CAR_GUN_SWITCH);
		controlRadio(powered, ControlsKeyboard.CAR_RADIO);
		controlJoystick(powered, ControlsKeyboard.CAR_JS_INHIBIT);
		
		if(!isPlayerController){
			return;
		}
		//Open or close the panel.
		if(ControlsKeyboard.CAR_PANEL.isPressed()){
			if(InterfaceGUI.getActiveGUI() instanceof GUIPanelGround){
				InterfaceGUI.closeGUI();
			}else if(InterfaceGUI.getActiveGUI() == null){
				InterfaceGUI.openGUI(new GUIPanelGround(powered));
			}
		}
		
		//Check brake and gas.  Depends on how the controls are configured.
		if(powered.definition.motorized.hasIncrementalThrottle){
			//Check brake and gas.  Brake always changes, gas goes up-down.
			controlBrake(powered, ControlsKeyboardDynamic.CAR_PARK, ControlsJoystick.CAR_BRAKE, ControlsJoystick.CAR_BRAKE_DIGITAL, ControlsJoystick.CAR_PARK);
			if(InterfaceInput.isJoystickPresent(ControlsJoystick.CAR_GAS.config.joystickName)){
				//Send throttle over if throttle if cruise control is off, or if throttle is less than the axis level.
				short throttleLevel = ControlsJoystick.CAR_GAS.getAxisState((short) 0);
				if(!powered.autopilot || powered.throttle < throttleLevel){
					InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, ControlsJoystick.CAR_GAS.getAxisState((short) 0), Byte.MAX_VALUE));
				}
			}else{
				if(ControlsKeyboard.CAR_GAS.isPressed()){
					InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 1, (byte) 0));
				}
				if(ControlsKeyboard.CAR_BRAKE.isPressed() || ControlsJoystick.CAR_BRAKE_DIGITAL.isPressed()){
					InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, (short) -1, (byte) 0));
				}
			}
		}else{
			if(ConfigSystem.configObject.clientControls.simpleThrottle.value){
				if(!powered.engines.values().isEmpty()){
					//Get the current gear.
					byte currentGear = 0;
					for(PartEngine engine : powered.engines.values()){
						currentGear = engine.currentGear;
					}
					
					//Get the brake value.
					short brakeValue = 0;
					if(InterfaceInput.isJoystickPresent(ControlsJoystick.CAR_BRAKE.config.joystickName)){
						brakeValue = ControlsJoystick.CAR_BRAKE.getAxisState((short) 0);
					}else if(ControlsKeyboard.CAR_BRAKE.isPressed() || ControlsJoystick.CAR_BRAKE_DIGITAL.isPressed()){
						 brakeValue = EntityVehicleF_Physics.MAX_BRAKE;
					}
					
					//Get the throttle value.
					short throttleValue = 0;
					if(InterfaceInput.isJoystickPresent(ControlsJoystick.CAR_GAS.config.joystickName)){
						throttleValue = ControlsJoystick.CAR_GAS.getAxisState((short) 0);
					}else if(ControlsKeyboardDynamic.CAR_SLOW.isPressed()){
						throttleValue = ConfigSystem.configObject.clientControls.halfThrottle.value ? EntityVehicleF_Physics.MAX_THROTTLE : EntityVehicleF_Physics.MAX_THROTTLE/2; 
					}else if(ControlsKeyboard.CAR_GAS.isPressed()){
						throttleValue = ConfigSystem.configObject.clientControls.halfThrottle.value ? EntityVehicleF_Physics.MAX_THROTTLE/2 : EntityVehicleF_Physics.MAX_THROTTLE;
					}
					
					//If we don't have velocity, and we have the appropriate control, shift.
					if(brakeValue > EntityVehicleF_Physics.MAX_BRAKE/4F && currentGear >= 0 && powered.axialVelocity < 0.01F){
						InterfacePacket.sendToServer(new PacketVehicleControlDigital(powered, PacketVehicleControlDigital.Controls.SHIFT_DN, false));
					}else if(throttleValue > EntityVehicleF_Physics.MAX_THROTTLE/4F && currentGear <= 0 && powered.axialVelocity < 0.01F){
						InterfacePacket.sendToServer(new PacketVehicleControlDigital(powered, PacketVehicleControlDigital.Controls.SHIFT_UP, false));
					}
					
					//If we are going slow, and don't have gas or brake, automatically set the brake.
					//Otherwise send normal values if we are in neutral or forwards,
					//and invert controls if we are in a reverse gear.
					if(throttleValue == 0 && brakeValue == 0 && powered.axialVelocity < PartEngine.MAX_SHIFT_SPEED){
						InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.BRAKE, EntityVehicleF_Physics.MAX_BRAKE, Byte.MAX_VALUE));
					}else if(currentGear >= 0){
						InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.BRAKE, brakeValue, Byte.MAX_VALUE));
						InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, throttleValue, Byte.MAX_VALUE));
					}else{
						InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.BRAKE, throttleValue, Byte.MAX_VALUE));
						InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, brakeValue, Byte.MAX_VALUE));
					}
				}
			}else{
				//Check brake and gas and set to on or off.
				controlBrake(powered, ControlsKeyboardDynamic.CAR_PARK, ControlsJoystick.CAR_BRAKE, ControlsJoystick.CAR_BRAKE_DIGITAL, ControlsJoystick.CAR_PARK);
				if(InterfaceInput.isJoystickPresent(ControlsJoystick.CAR_GAS.config.joystickName)){
					//Send throttle over if throttle if cruise control is off, or if throttle is less than the axis level.
					short throttleLevel = ControlsJoystick.CAR_GAS.getAxisState((short) 0);
					if(!powered.autopilot || powered.throttle < throttleLevel){
						InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, ControlsJoystick.CAR_GAS.getAxisState((short) 0), Byte.MAX_VALUE));
					}
				}else{
					if(ControlsKeyboardDynamic.CAR_SLOW.isPressed()){
						if(!ConfigSystem.configObject.clientControls.halfThrottle.value){
							InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, (short) (EntityVehicleF_Physics.MAX_THROTTLE/2), Byte.MAX_VALUE));
						}else{
							InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, EntityVehicleF_Physics.MAX_THROTTLE, Byte.MAX_VALUE));
						}
					}else if(ControlsKeyboard.CAR_GAS.isPressed()){
						if(!ConfigSystem.configObject.clientControls.halfThrottle.value){
							InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, EntityVehicleF_Physics.MAX_THROTTLE, Byte.MAX_VALUE));
						}else{
							InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, (short) (EntityVehicleF_Physics.MAX_THROTTLE/2), Byte.MAX_VALUE));
						}
					}else{
						//Don't send gas off packet if we have cruise on.
						if(!powered.autopilot){
							InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 0, Byte.MAX_VALUE));
						}
					}
				}
			}
		}
		
		//Check steering.  If mouse yoke is enabled, we do controls by mouse rather than buttons.
		if(ConfigSystem.configObject.clientControls.mouseYoke.value){
			if(EntityVehicleF_Physics.lockCameraToMovement && InterfaceGUI.getActiveGUI() == null){
				long mousePosition = InterfaceInput.getTrackedMouseInfo();
				InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.RUDDER, (short) (mousePosition >> Integer.SIZE), Byte.MAX_VALUE));
			}
		}else{
			if(InterfaceInput.isJoystickPresent(ControlsJoystick.CAR_TURN.config.joystickName)){
				InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.RUDDER, ControlsJoystick.CAR_TURN.getAxisState(EntityVehicleF_Physics.MAX_RUDDER_ANGLE), Byte.MAX_VALUE));
				if(powered.locationRiderMap.containsValue(clientPlayer)){
					if(powered.slipping){
						InterfaceInput.setJoystickRumble(ControlsJoystick.CAR_TURN.config.joystickName, (float) Math.max(powered.velocity, 1));
					}else{
						InterfaceInput.setJoystickRumble(ControlsJoystick.CAR_TURN.config.joystickName, 0);
					}
				}
			}else{
				//Depending on what we are pressing, send out packets.
				//If we are turning in the opposite direction of our current angle, send out a packet with twice the value.
				boolean turningRight = ControlsKeyboard.CAR_TURN_R.isPressed();
				boolean turningLeft = ControlsKeyboard.CAR_TURN_L.isPressed();
				if(turningRight && !turningLeft){
					InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.RUDDER, (short) (ConfigSystem.configObject.clientControls.steeringIncrement.value.shortValue()*(powered.rudderAngle < 0 ? 2 : 1)), ConfigSystem.configObject.clientControls.controlSurfaceCooldown.value.byteValue()));
				}else if(turningLeft && !turningRight){
					InterfacePacket.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.RUDDER, (short) (-ConfigSystem.configObject.clientControls.steeringIncrement.value.shortValue()*(powered.rudderAngle > 0 ? 2 : 1)), ConfigSystem.configObject.clientControls.controlSurfaceCooldown.value.byteValue()));
				}
			}
		}
		
		//Check if we are shifting.
		if(ControlsKeyboardDynamic.CAR_SHIFT_NU.isPressed() || ControlsKeyboardDynamic.CAR_SHIFT_ND.isPressed()){
			InterfacePacket.sendToServer(new PacketVehicleControlDigital(powered, PacketVehicleControlDigital.Controls.SHIFT_NEUTRAL, false));
		}else{
			if(ControlsKeyboard.CAR_SHIFT_U.isPressed()){
				InterfacePacket.sendToServer(new PacketVehicleControlDigital(powered, PacketVehicleControlDigital.Controls.SHIFT_UP, false));
			}
			if(ControlsKeyboard.CAR_SHIFT_D.isPressed()){
				InterfacePacket.sendToServer(new PacketVehicleControlDigital(powered, PacketVehicleControlDigital.Controls.SHIFT_DN, false));
			}
		}
		
		//Check if horn button is pressed.
		if(ControlsKeyboard.CAR_HORN.isPressed() && !powered.variablesOn.contains(EntityVehicleF_Physics.HORN_VARIABLE)){
			InterfacePacket.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.HORN_VARIABLE));
		}else if(!ControlsKeyboard.CAR_HORN.isPressed() && powered.variablesOn.contains(EntityVehicleF_Physics.HORN_VARIABLE)){
			InterfacePacket.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.HORN_VARIABLE));
		}
		
		//Check for lights.
		if(ControlsKeyboard.CAR_LIGHTS.isPressed()){
			InterfacePacket.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.RUNNINGLIGHT_VARIABLE));
			InterfacePacket.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.HEADLIGHT_VARIABLE));
		}
		if(ControlsKeyboard.CAR_TURNSIGNAL_L.isPressed()){
			InterfacePacket.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.LEFTTURNLIGHT_VARIABLE));
		}
		if(ControlsKeyboard.CAR_TURNSIGNAL_R.isPressed()){
			InterfacePacket.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.RIGHTTURNLIGHT_VARIABLE));
		}

		//Change turn signal status depending on turning status.
		//Keep signals on until we have been moving without turning in the
		//pressed direction for 2 seconds, or if we turn in the other direction.
		//This only happens if the signals are set to automatic.  For manual signals, we let the player control them.
		if(ConfigSystem.configObject.clientControls.autoTrnSignals.value){
			if(!powered.turningLeft && powered.rudderAngle < -200){
				powered.turningLeft = true;
				powered.turningCooldown = 40;
				InterfacePacket.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.LEFTTURNLIGHT_VARIABLE));
			}
			if(!powered.turningRight && powered.rudderAngle > 200){
				powered.turningRight = true;
				powered.turningCooldown = 40;
				InterfacePacket.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.RIGHTTURNLIGHT_VARIABLE));
			}
			if(powered.turningLeft && (powered.rudderAngle > 0 || powered.turningCooldown == 0)){
				powered.turningLeft = false;
				InterfacePacket.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.LEFTTURNLIGHT_VARIABLE));
			}
			if(powered.turningRight && (powered.rudderAngle < 0 || powered.turningCooldown == 0)){
				powered.turningRight = false;
				InterfacePacket.sendToServer(new PacketEntityVariableToggle(powered, EntityVehicleF_Physics.RIGHTTURNLIGHT_VARIABLE));
			}
			if(powered.velocity != 0 && powered.turningCooldown > 0 && powered.rudderAngle == 0){
				--powered.turningCooldown;
			}
		}
	}

	/**List of enums representing all controls present.  Add new controls by adding their enum values here
	 *
	 * @author don_bruce
	 */
	public static enum ControlsKeyboard{
		AIRCRAFT_MOD(ControlsJoystick.AIRCRAFT_MOD, false, "RSHIFT"),
		AIRCRAFT_CAMLOCK(ControlsJoystick.AIRCRAFT_CAMLOCK, true, "RCONTROL"),
		AIRCRAFT_YAW_R(ControlsJoystick.AIRCRAFT_YAW, false, "L"),
		AIRCRAFT_YAW_L(ControlsJoystick.AIRCRAFT_YAW, false, "J"),
		AIRCRAFT_PITCH_U(ControlsJoystick.AIRCRAFT_PITCH, false, "S"),
		AIRCRAFT_PITCH_D(ControlsJoystick.AIRCRAFT_PITCH, false, "W"),
		AIRCRAFT_ROLL_R( ControlsJoystick.AIRCRAFT_ROLL, false, "D"),
		AIRCRAFT_ROLL_L(ControlsJoystick.AIRCRAFT_ROLL, false, "A"),
		AIRCRAFT_THROTTLE_U(ControlsJoystick.AIRCRAFT_THROTTLE, false, "I"),
		AIRCRAFT_THROTTLE_D(ControlsJoystick.AIRCRAFT_THROTTLE, false, "K"),
		AIRCRAFT_FLAPS_U(ControlsJoystick.AIRCRAFT_FLAPS_U, true, "Y"),
		AIRCRAFT_FLAPS_D(ControlsJoystick.AIRCRAFT_FLAPS_D, true, "H"),
		AIRCRAFT_BRAKE(ControlsJoystick.AIRCRAFT_BRAKE, false, "B"),
		AIRCRAFT_PANEL(ControlsJoystick.AIRCRAFT_PANEL, true, "U"),
		AIRCRAFT_RADIO(ControlsJoystick.AIRCRAFT_RADIO, true, "MINUS"),
		AIRCRAFT_GUN_FIRE(ControlsJoystick.AIRCRAFT_GUN_FIRE, false, "SPACE"),
		AIRCRAFT_GUN_SWITCH(ControlsJoystick.AIRCRAFT_GUN_SWITCH, true, "V"),
		AIRCRAFT_ZOOM_I(ControlsJoystick.AIRCRAFT_ZOOM_I, true, "PRIOR"),
		AIRCRAFT_ZOOM_O(ControlsJoystick.AIRCRAFT_ZOOM_O, true, "NEXT"),
		AIRCRAFT_JS_INHIBIT(ControlsJoystick.AIRCRAFT_JS_INHIBIT, true, "SCROLL"),
		
		CAR_MOD(ControlsJoystick.CAR_MOD, false, "RSHIFT"),
		CAR_CAMLOCK(ControlsJoystick.CAR_CAMLOCK, true, "RCONTROL"),
		CAR_TURN_R(ControlsJoystick.CAR_TURN, false, "D"),
		CAR_TURN_L(ControlsJoystick.CAR_TURN, false, "A"),
		CAR_GAS(ControlsJoystick.CAR_GAS, false, "W"),
		CAR_BRAKE(ControlsJoystick.CAR_BRAKE, false, "S"),
		CAR_PANEL(ControlsJoystick.CAR_PANEL, true, "U"),
		CAR_SHIFT_U(ControlsJoystick.CAR_SHIFT_U, true, "R"),
		CAR_SHIFT_D(ControlsJoystick.CAR_SHIFT_D, true, "F"),
		CAR_HORN(ControlsJoystick.CAR_HORN, false, "C"),
		CAR_RADIO(ControlsJoystick.CAR_RADIO, true, "MINUS"),
		CAR_GUN_FIRE(ControlsJoystick.CAR_GUN_FIRE, false, "SPACE"),
		CAR_GUN_SWITCH(ControlsJoystick.CAR_GUN_SWITCH, true, "V"),
		CAR_ZOOM_I(ControlsJoystick.CAR_ZOOM_I, true, "PRIOR"),
		CAR_ZOOM_O(ControlsJoystick.CAR_ZOOM_O, true, "NEXT"),
		CAR_LIGHTS(ControlsJoystick.CAR_LIGHTS, true, "NUMPAD5"),
		CAR_TURNSIGNAL_L(ControlsJoystick.CAR_TURNSIGNAL_L, true, "NUMPAD4"),
		CAR_TURNSIGNAL_R(ControlsJoystick.CAR_TURNSIGNAL_R, true, "NUMPAD6"),
		CAR_JS_INHIBIT(ControlsJoystick.CAR_JS_INHIBIT, true, "SCROLL");
		
		
		public final boolean isMomentary;
		public final String systemName;
		public final String translatedName;
		public final String defaultKeyName;
		public final ConfigKeyboard config;
		private final ControlsJoystick linkedJoystick;
		
		private boolean wasPressedLastCall;
		
		private ControlsKeyboard(ControlsJoystick linkedJoystick, boolean isMomentary, String defaultKeyName){
			this.linkedJoystick = linkedJoystick;
			this.isMomentary = isMomentary;
			this.systemName = this.name().toLowerCase().replaceFirst("_", ".");
			this.translatedName = InterfaceCore.translate("input." + systemName);
			this.defaultKeyName = defaultKeyName;
			if(ConfigSystem.configObject.controls.keyboard.containsKey(systemName)){
				this.config = ConfigSystem.configObject.controls.keyboard.get(systemName);
			}else{
				this.config = new ConfigKeyboard();
			}
		}
		
		/**
		 *  Returns true if the given key is currently pressed.  If our linked
		 *  joystick is pressed, return true.  If the joystick is not, but it
		 *  is bound, and we are using keyboard overrides, return false.
		 *  Otherwise return the actual key state.
		 */
		public boolean isPressed(){
			if(linkedJoystick.isPressed()){
				return true;
			}else if(InterfaceInput.isJoystickPresent(linkedJoystick.config.joystickName) && ConfigSystem.configObject.clientControls.kbOverride.value){
				return false;
			}else{
				if(isMomentary){
					if(wasPressedLastCall){
						wasPressedLastCall = InterfaceInput.isKeyPressed(config.keyCode); 
						return false;
					}else{
						wasPressedLastCall = InterfaceInput.isKeyPressed(config.keyCode);
						return wasPressedLastCall;
					}
				}else{
					return InterfaceInput.isKeyPressed(config.keyCode);
				}
			}
		}
	}
	
	public static enum ControlsJoystick{
		AIRCRAFT_MOD(false, false),
		AIRCRAFT_CAMLOCK(false, true),
		AIRCRAFT_YAW(true, false),
		AIRCRAFT_PITCH(true, false),
		AIRCRAFT_ROLL(true, false),
		AIRCRAFT_THROTTLE(true, false),
		AIRCRAFT_BRAKE(true, false),
		AIRCRAFT_BRAKE_DIGITAL(false, false),
		AIRCRAFT_FLAPS_U(false, true),
		AIRCRAFT_FLAPS_D(false, true),
		AIRCRAFT_PANEL(false, true),
		AIRCRAFT_PARK(false, true),
		AIRCRAFT_RADIO(false, true),
		AIRCRAFT_GUN_FIRE(false, false),
		AIRCRAFT_GUN_SWITCH(false, true),
		AIRCRAFT_ZOOM_I(false, true),
		AIRCRAFT_ZOOM_O(false, true),
		AIRCRAFT_CHANGEVIEW(false, true),
		AIRCRAFT_LOOK_L(false, false),
		AIRCRAFT_LOOK_R(false, false),
		AIRCRAFT_LOOK_U(false, false),
		AIRCRAFT_LOOK_D(false, false),
		AIRCRAFT_LOOK_A(false, false),
		AIRCRAFT_TRIM_YAW_R(false, false),
		AIRCRAFT_TRIM_YAW_L(false, false),
		AIRCRAFT_TRIM_PITCH_U(false, false),
		AIRCRAFT_TRIM_PITCH_D(false, false),
		AIRCRAFT_TRIM_ROLL_R(false, false),
		AIRCRAFT_TRIM_ROLL_L(false, false),
		AIRCRAFT_REVERSE(false, true),
		AIRCRAFT_JS_INHIBIT(false, true),
		
		
		CAR_MOD(false, false),
		CAR_CAMLOCK(false, true),
		CAR_TURN(true, false),
		CAR_GAS(true, false),
		CAR_BRAKE(true, false),
		CAR_BRAKE_DIGITAL(false, false),
		CAR_PANEL(false, true),
		CAR_SHIFT_U(false, true),
		CAR_SHIFT_D(false, true),
		CAR_HORN(false, false),
		CAR_PARK(false, true),
		CAR_RADIO(false, true),
		CAR_GUN_FIRE(false, false),
		CAR_GUN_SWITCH(false, true),
		CAR_ZOOM_I(false, true),
		CAR_ZOOM_O(false, true),
		CAR_CHANGEVIEW(false, true),
		CAR_LOOK_L(false, false),
		CAR_LOOK_R(false, false),
		CAR_LOOK_U(false, false),
		CAR_LOOK_D(false, false),
		CAR_LOOK_A(false, false),
		CAR_LIGHTS(false, true),
		CAR_TURNSIGNAL_L(false, true),
		CAR_TURNSIGNAL_R(false, true),
		CAR_JS_INHIBIT(false, true);
		
		
		public final boolean isAxis;
		public final boolean isMomentary;
		public final String systemName;
		public final String translatedName;
		public final ConfigJoystick config;
		
		private boolean wasPressedLastCall;
		
		private ControlsJoystick(boolean isAxis, boolean isMomentary){
			this.isAxis=isAxis;
			this.isMomentary=isMomentary;
			this.systemName = this.name().toLowerCase().replaceFirst("_", ".");
			this.translatedName = InterfaceCore.translate("input." + systemName);
			if(ConfigSystem.configObject.controls.joystick.containsKey(systemName)){
				this.config = ConfigSystem.configObject.controls.joystick.get(systemName);
			}else{
				this.config = new ConfigJoystick();
			}
		}
		
		public boolean isPressed(){
			if(isMomentary){
				if(wasPressedLastCall){
					wasPressedLastCall = InterfaceInput.getJoystickButtonValue(config.joystickName, config.buttonIndex); 
					return false;
				}else{
					wasPressedLastCall = InterfaceInput.getJoystickButtonValue(config.joystickName, config.buttonIndex);
					return wasPressedLastCall;
				}
			}else{
				return InterfaceInput.getJoystickButtonValue(config.joystickName, config.buttonIndex);
			}
		}
		
		private float getMultistateValue(){
			return InterfaceInput.getJoystickAxisValue(config.joystickName, config.buttonIndex);
		}
		
		//Return type is short to allow for easier packet transmission.
		private short getAxisState(short pollBounds){
			float pollValue = getMultistateValue();
			if(Math.abs(pollValue) > ConfigSystem.configObject.clientControls.joystickDeadZone.value || pollBounds == 0){
				//Clamp the poll value to the defined axis bounds set during config to prevent over and under-runs.
				pollValue = (float) Math.max(config.axisMinTravel, pollValue);
				pollValue = (float) Math.min(config.axisMaxTravel, pollValue);
				
				//If we don't need to normalize the axis, return it as-is.  Otherwise do a normalization from 0-1.
				if(pollBounds != 0){
					return (short) (config.invertedAxis ? (-pollBounds*pollValue) : (pollBounds*pollValue));
				}else{
					//Divide the poll value plus the min bounds by the span to get it in the range of 0-1.
					pollValue = (float) ((pollValue - config.axisMinTravel)/(config.axisMaxTravel - config.axisMinTravel));
					
					//If axis is inverted, invert poll.
					if(config.invertedAxis){
						pollValue = 1 - pollValue;
					}
					
					//Now return this value in a range from 0-100.
					return (short) (pollValue*100);
				}
			}else{
				return 0;
			}
		}
		
		public void setControl(String joystickName, int buttonIndex){
			config.joystickName = joystickName;
			config.buttonIndex = buttonIndex;
			ConfigSystem.saveToDisk();
		}
		
		public void setAxisControl(String joystickName, int buttonIndex, double axisMinTravel, double axisMaxTravel, boolean invertedAxis){
			config.axisMinTravel = axisMinTravel;
			config.axisMaxTravel = axisMaxTravel;
			config.invertedAxis = invertedAxis;
			setControl(joystickName, buttonIndex);
		}
		
		public void clearControl(){
			setControl(null, NULL_COMPONENT);
		}
	}
	
	public static enum ControlsKeyboardDynamic{
		AIRCRAFT_PARK(ControlsKeyboard.AIRCRAFT_BRAKE, ControlsKeyboard.AIRCRAFT_MOD),
		
		CAR_PARK(ControlsKeyboard.CAR_BRAKE, ControlsKeyboard.CAR_MOD),
		CAR_SLOW(ControlsKeyboard.CAR_GAS, ControlsKeyboard.CAR_MOD),
		CAR_SHIFT_NU(ControlsKeyboard.CAR_SHIFT_U, ControlsKeyboard.CAR_MOD),
		CAR_SHIFT_ND(ControlsKeyboard.CAR_SHIFT_D, ControlsKeyboard.CAR_MOD);
		
		public final String translatedName;
		public final ControlsKeyboard mainControl;
		public final ControlsKeyboard modControl;
		
		private ControlsKeyboardDynamic(ControlsKeyboard mainControl, ControlsKeyboard modControl){
			this.translatedName = InterfaceCore.translate("input." + name().toLowerCase().replaceFirst("_", "."));
			this.mainControl = mainControl;
			this.modControl = modControl;
		}
		
		public boolean isPressed(){
			return this.modControl.isPressed() ? this.mainControl.isPressed() : false;
		}
	}
}
