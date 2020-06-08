package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.guis.instances.GUIPanelAircraft;
import minecrafttransportsimulator.guis.instances.GUIPanelGround;
import minecrafttransportsimulator.guis.instances.GUIRadio;
import minecrafttransportsimulator.jsondefs.JSONConfig.ConfigJoystick;
import minecrafttransportsimulator.jsondefs.JSONConfig.ConfigKeyboard;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.packets.parts.PacketPartGunSignal;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Ground;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartGun;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperInput;
import minecrafttransportsimulator.wrappers.WrapperNetwork;
import net.minecraft.client.Minecraft;

/**Class that handles all control operations.
 * 
 * @author don_bruce
 */
public final class ControlSystem{	
	private static final int NULL_COMPONENT = 999;	
	
	/**
	 * Static initializer for the wrapper inputs, as we need to iterate through the enums to initialize them
	 * prior to using them in any of the methods contained in this wrapper (cause they'll be null).
	 * Joystick enums need to come first, as the Keyboard enums take them as constructor args.
	 * After we initialize the keboard enums, we set their default values.  If we don't initialize
	 * them first, we hit a switch error in {@link WrapperInput#getDefaultKeyCode(ControlsKeyboard)}.
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
				control.config.keyCode = WrapperInput.getDefaultKeyCode(control);
			}
		}
		ConfigSystem.saveToDisk();
	}

	
	public static void controlVehicle(EntityVehicleE_Powered vehicle, boolean isPlayerController){
		if(vehicle instanceof EntityVehicleF_Air){
			controlAircraft((EntityVehicleF_Air) vehicle, isPlayerController);
		}else if(vehicle instanceof EntityVehicleF_Ground){
			controlGroundVehicle((EntityVehicleF_Ground) vehicle, isPlayerController);
		}
	}
	
	private static void controlCamera(ControlsKeyboard camLock, ControlsKeyboard zoomIn, ControlsKeyboard zoomOut, ControlsJoystick changeView){
		if(camLock.isPressed()){
			ClientEventSystem.lockedView = !ClientEventSystem.lockedView; 
		}
		
		if(zoomIn.isPressed() && ClientEventSystem.zoomLevel > 0){
			ClientEventSystem.zoomLevel -= 2;
		}
		if(zoomOut.isPressed()){
			ClientEventSystem.zoomLevel += 2;
		}
		
		if(changeView.isPressed()){
			if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 2){
				Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
			}else{
				++Minecraft.getMinecraft().gameSettings.thirdPersonView;
			}
		}
	}
	
	private static void rotateCamera(ControlsJoystick lookR, ControlsJoystick lookL, ControlsJoystick lookU, ControlsJoystick lookD, ControlsJoystick lookA){
		if(lookR.isPressed()){
			Minecraft.getMinecraft().player.rotationYaw+=3;
		}
		if(lookL.isPressed()){
			Minecraft.getMinecraft().player.rotationYaw-=3;
		}
		if(lookU.isPressed()){
			Minecraft.getMinecraft().player.rotationPitch-=3;
		}
		if(lookD.isPressed()){
			Minecraft.getMinecraft().player.rotationPitch+=3;
		}
		
		float pollData = lookA.getMultistateValue();
		if(pollData != 0){
			if(pollData >= 0.125F && pollData <= 0.375F){
				Minecraft.getMinecraft().player.rotationPitch+=3;
			}
			if(pollData >= 0.375F && pollData <= 0.625F){
				Minecraft.getMinecraft().player.rotationYaw+=3;
			}
			if(pollData >= 0.625F && pollData <= 0.875F){
				Minecraft.getMinecraft().player.rotationPitch-=3;
			}
			if(pollData >= 0.875F || pollData <= 0.125F){
				Minecraft.getMinecraft().player.rotationYaw-=3;
			}
		}
	}
	
	private static void controlBrake(EntityVehicleE_Powered vehicle, ControlsKeyboardDynamic dynamic, ControlsJoystick analogBrake, ControlsJoystick pBrake){
		//If the analog brake is set, do brake state based on that rather than the keyboard.
		boolean isParkingBrakePressed = analogBrake.config.joystickName != null ? pBrake.isPressed() : dynamic.isPressed() || pBrake.isPressed();
		boolean isBrakePressed = analogBrake.config.joystickName != null ? analogBrake.getAxisState((short) 0) > 25 : dynamic.mainControl.isPressed();
		if(isParkingBrakePressed){
			if(!vehicle.parkingBrakeOn){
				WrapperNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.P_BRAKE, true));
			}
		}else if(isBrakePressed){
			if(!vehicle.brakeOn){
				WrapperNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.BRAKE, true));
			}
			if(vehicle.parkingBrakeOn){
				WrapperNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.P_BRAKE, false));
			}
		}else{
			if(vehicle.brakeOn){
				WrapperNetwork.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.BRAKE, false));
			}
		}
	}
	
	private static void controlGun(EntityVehicleE_Powered vehicle, ControlsKeyboard gun){
		PartSeat seat = vehicle.getSeatForRider(Minecraft.getMinecraft().player);
		if(seat != null){
			//If we are seated, attempt to control guns.
			//Only control guns our seat is a part of, or guns with no seats part of them.
			//First check our parent part.
			if(seat.parentPart instanceof PartGun){
				MTS.MTSNet.sendToServer(new PacketPartGunSignal((PartGun) seat.parentPart, Minecraft.getMinecraft().player.getEntityId(), gun.isPressed()));
			}
			//Now check subParts of our seat.
			for(APart subPart : seat.childParts){
				if(subPart instanceof PartGun){
					MTS.MTSNet.sendToServer(new PacketPartGunSignal((PartGun) subPart, Minecraft.getMinecraft().player.getEntityId(), gun.isPressed()));
				}
			}
			//If we are the vehicle controller, check for guns that don't have seats. 
			if(seat.vehicleDefinition.isController){
				for(APart part : vehicle.getVehicleParts()){
					if(part instanceof PartGun){
						if(!(part.parentPart instanceof PartSeat)){
							boolean hasControllingSeats = false;
							for(APart subPart : part.childParts){
								if(subPart instanceof PartSeat){
									hasControllingSeats = true;
								}
							}
							if(!hasControllingSeats){
								MTS.MTSNet.sendToServer(new PacketPartGunSignal((PartGun) part, Minecraft.getMinecraft().player.getEntityId(), gun.isPressed()));
							}
						}
					}
				}
			}
		}
	}
	
	private static void controlRadio(EntityVehicleE_Powered vehicle, ControlsKeyboard radio){
		if(radio.isPressed()){
			if(WrapperGUI.isGUIActive(null)){
				WrapperGUI.openGUI(new GUIRadio(vehicle));
			}else if(WrapperGUI.isGUIActive(GUIRadio.class)){
				WrapperGUI.closeGUI();
			}
		}
	}
	
	private static void controlAircraft(EntityVehicleF_Air aircraft, boolean isPlayerController){
		controlCamera(ControlsKeyboard.AIRCRAFT_CAMLOCK, ControlsKeyboard.AIRCRAFT_ZOOM_I, ControlsKeyboard.AIRCRAFT_ZOOM_O, ControlsJoystick.AIRCRAFT_CHANGEVIEW);
		rotateCamera(ControlsJoystick.AIRCRAFT_LOOK_R, ControlsJoystick.AIRCRAFT_LOOK_L, ControlsJoystick.AIRCRAFT_LOOK_U, ControlsJoystick.AIRCRAFT_LOOK_D, ControlsJoystick.AIRCRAFT_LOOK_A);
		controlRadio(aircraft, ControlsKeyboard.AIRCRAFT_RADIO);
		controlGun(aircraft, ControlsKeyboard.AIRCRAFT_GUN);
		if(!isPlayerController){
			return;
		}
		controlBrake(aircraft, ControlsKeyboardDynamic.AIRCRAFT_PARK, ControlsJoystick.AIRCRAFT_BRAKE_ANALOG, ControlsJoystick.AIRCRAFT_PARK);
		
		//Open or close the panel.
		if(ControlsKeyboard.AIRCRAFT_PANEL.isPressed()){
			if(WrapperGUI.isGUIActive(null)){
				WrapperGUI.openGUI(new GUIPanelAircraft(aircraft));
			}else if(WrapperGUI.isGUIActive(GUIPanelAircraft.class)){
				WrapperGUI.closeGUI();
			}
		}
		
		//Check for thrust reverse button.
		if(ControlsJoystick.AIRCRAFT_REVERSE.isPressed()){
			WrapperNetwork.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.REVERSE, !aircraft.reverseThrust));
		}
		
		//Increment or decrement throttle.
		if(ControlsJoystick.AIRCRAFT_THROTTLE.config.joystickName != null){
			WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.THROTTLE, ControlsJoystick.AIRCRAFT_THROTTLE.getAxisState((short) 0), Byte.MAX_VALUE));
		}else{
			if(ControlsKeyboard.AIRCRAFT_THROTTLE_U.isPressed()){
				WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 1, (byte) 0));
			}
			if(ControlsKeyboard.AIRCRAFT_THROTTLE_D.isPressed()){
				WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.THROTTLE, (short) -1, (byte) 0));
			}
		}		
		
		//Check flaps.
		if(aircraft.definition.plane != null && aircraft.definition.plane.hasFlaps){
			if(ControlsKeyboard.AIRCRAFT_FLAPS_U.isPressed()){
				WrapperNetwork.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.FLAPS, false));
			}
			if(ControlsKeyboard.AIRCRAFT_FLAPS_D.isPressed()){
				WrapperNetwork.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.FLAPS, true));
			}
		}
		
		//Check yaw.
		if(ControlsJoystick.AIRCRAFT_YAW.config.joystickName != null){
			WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.RUDDER, ControlsJoystick.AIRCRAFT_YAW.getAxisState(aircraft.MAX_RUDDER_ANGLE), Byte.MAX_VALUE));
		}else{
			if(ControlsKeyboard.AIRCRAFT_YAW_R.isPressed()){
				WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.RUDDER, (short) (ConfigSystem.configObject.client.steeringIncrement.value.shortValue()*(aircraft.rudderAngle < 0 ? 2 : 1)), ConfigSystem.configObject.client.controlSurfaceCooldown.value.byteValue()));
			}
			if(ControlsKeyboard.AIRCRAFT_YAW_L.isPressed()){
				WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.RUDDER, (short) (-ConfigSystem.configObject.client.steeringIncrement.value.shortValue()*(aircraft.rudderAngle > 0 ? 2 : 1)), ConfigSystem.configObject.client.controlSurfaceCooldown.value.byteValue()));
			}
		}
		if(ControlsJoystick.AIRCRAFT_TRIM_YAW_R.isPressed()){
			WrapperNetwork.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.TRIM_YAW, true));
		}
		if(ControlsJoystick.AIRCRAFT_TRIM_YAW_L.isPressed()){
			WrapperNetwork.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.TRIM_YAW, false));
		}
		
		//Check is mouse yoke is enabled.  If so do controls by mouse rather than buttons.
		if(ConfigSystem.configObject.client.mouseYoke.value){
			if(ClientEventSystem.lockedView && WrapperGUI.isGUIActive(null)){
				long mousePosition = WrapperInput.getTrackedMouseInfo();
				WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.AILERON, (short) (mousePosition >> Integer.SIZE), Byte.MAX_VALUE));
				WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.ELEVATOR, (short) ((int) -mousePosition), Byte.MAX_VALUE));
				
			}
		}else{
			//Check pitch.
			if(ControlsJoystick.AIRCRAFT_PITCH.config.joystickName != null){
				WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.ELEVATOR, ControlsJoystick.AIRCRAFT_PITCH.getAxisState(aircraft.MAX_ELEVATOR_ANGLE), Byte.MAX_VALUE));
			}else{
				if(ControlsKeyboard.AIRCRAFT_PITCH_U.isPressed()){
					WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.ELEVATOR, (short) (ConfigSystem.configObject.client.flightIncrement.value.shortValue()*(aircraft.elevatorAngle < 0 ? 2 : 1)), ConfigSystem.configObject.client.controlSurfaceCooldown.value.byteValue()));
				}
				if(ControlsKeyboard.AIRCRAFT_PITCH_D.isPressed()){
					WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.ELEVATOR, (short) (-ConfigSystem.configObject.client.flightIncrement.value.shortValue()*(aircraft.elevatorAngle > 0 ? 2 : 1)), ConfigSystem.configObject.client.controlSurfaceCooldown.value.byteValue()));
				}
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_PITCH_U.isPressed()){
				WrapperNetwork.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.TRIM_PITCH, true));
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_PITCH_D.isPressed()){
				WrapperNetwork.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.TRIM_PITCH, false));
			}
			
			//Check roll.
			if(ControlsJoystick.AIRCRAFT_ROLL.config.joystickName != null){
				WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.AILERON, ControlsJoystick.AIRCRAFT_ROLL.getAxisState(aircraft.MAX_AILERON_ANGLE), Byte.MAX_VALUE));
			}else{
				if(ControlsKeyboard.AIRCRAFT_ROLL_R.isPressed()){
					WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.AILERON, (short) (ConfigSystem.configObject.client.flightIncrement.value.shortValue()*(aircraft.aileronAngle < 0 ? 2 : 1)), ConfigSystem.configObject.client.controlSurfaceCooldown.value.byteValue()));
				}
				if(ControlsKeyboard.AIRCRAFT_ROLL_L.isPressed()){
					WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(aircraft, PacketVehicleControlAnalog.Controls.AILERON, (short) (-ConfigSystem.configObject.client.flightIncrement.value.shortValue()*(aircraft.aileronAngle > 0 ? 2 : 1)), ConfigSystem.configObject.client.controlSurfaceCooldown.value.byteValue()));
				}
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_ROLL_R.isPressed()){
				WrapperNetwork.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.TRIM_ROLL, true));
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_ROLL_L.isPressed()){
				WrapperNetwork.sendToServer(new PacketVehicleControlDigital(aircraft, PacketVehicleControlDigital.Controls.TRIM_ROLL, false));
			}
		}
	}
	
	private static void controlGroundVehicle(EntityVehicleF_Ground powered, boolean isPlayerController){
		controlCamera(ControlsKeyboard.CAR_CAMLOCK, ControlsKeyboard.CAR_ZOOM_I, ControlsKeyboard.CAR_ZOOM_O, ControlsJoystick.CAR_CHANGEVIEW);
		rotateCamera(ControlsJoystick.CAR_LOOK_R, ControlsJoystick.CAR_LOOK_L, ControlsJoystick.CAR_LOOK_U, ControlsJoystick.CAR_LOOK_D, ControlsJoystick.CAR_LOOK_A);
		controlRadio(powered, ControlsKeyboard.CAR_RADIO);
		controlGun(powered, ControlsKeyboard.CAR_GUN);
		if(!isPlayerController){
			return;
		}
		controlBrake(powered, ControlsKeyboardDynamic.CAR_PARK, ControlsJoystick.CAR_BRAKE_ANALOG, ControlsJoystick.CAR_PARK);
		
		//Open or close the panel.
		if(ControlsKeyboard.CAR_PANEL.isPressed()){
			if(WrapperGUI.isGUIActive(null)){
				WrapperGUI.openGUI(new GUIPanelGround(powered));
			}else if(WrapperGUI.isGUIActive(GUIPanelGround.class)){
				WrapperGUI.closeGUI();
			}
		}
		
		//Change gas to on or off.
		if(ControlsJoystick.CAR_GAS.config.joystickName != null){
			WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, ControlsJoystick.CAR_GAS.getAxisState((short) 0), Byte.MAX_VALUE));
		}else{
			if(ControlsKeyboardDynamic.CAR_SLOW.isPressed()){
				WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 50, Byte.MAX_VALUE));
			}else if(ControlsKeyboard.CAR_GAS.isPressed()){
				WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 100, Byte.MAX_VALUE));
			}else{
				//Don't send gas off packet if we have cruise on.
				if(!powered.cruiseControl){
					WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 0, Byte.MAX_VALUE));
				}
			}
		}
		
		//Check steering, turn signals, and lights.
		//Check is mouse yoke is enabled.  If so do controls by mouse rather than buttons.
		if(ConfigSystem.configObject.client.mouseYoke.value){
			if(ClientEventSystem.lockedView && WrapperGUI.isGUIActive(null)){
				long mousePosition = WrapperInput.getTrackedMouseInfo();
				WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.STEERING, (short) (mousePosition >> Integer.SIZE), Byte.MAX_VALUE));
			}
		}else{
			if(ControlsJoystick.CAR_TURN.config.joystickName != null){
				WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.STEERING, ControlsJoystick.CAR_TURN.getAxisState(powered.MAX_STEERING_ANGLE), Byte.MAX_VALUE));
			}else{
				//Depending on what we are pressing, send out packets.
				//If we are turning in the opposite direction of our current angle, send out a packet with twice the value.
				boolean turningRight = ControlsKeyboard.CAR_TURN_R.isPressed();
				boolean turningLeft = ControlsKeyboard.CAR_TURN_L.isPressed();
				if(turningRight && !turningLeft){
					WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.STEERING, (short) (ConfigSystem.configObject.client.steeringIncrement.value.shortValue()*(powered.steeringAngle < 0 ? 2 : 1)), ConfigSystem.configObject.client.controlSurfaceCooldown.value.byteValue()));
				}else if(turningLeft && !turningRight){
					WrapperNetwork.sendToServer(new PacketVehicleControlAnalog(powered, PacketVehicleControlAnalog.Controls.STEERING, (short) (-ConfigSystem.configObject.client.steeringIncrement.value.shortValue()*(powered.steeringAngle > 0 ? 2 : 1)), ConfigSystem.configObject.client.controlSurfaceCooldown.value.byteValue()));
				}
			}
		}
		
		//Check if we are shifting.
		if(ControlsKeyboard.CAR_SHIFT_U.isPressed()){
			WrapperNetwork.sendToServer(new PacketVehicleControlDigital(powered, PacketVehicleControlDigital.Controls.SHIFT, true));
		}
		if(ControlsKeyboard.CAR_SHIFT_D.isPressed()){
			WrapperNetwork.sendToServer(new PacketVehicleControlDigital(powered, PacketVehicleControlDigital.Controls.SHIFT, false));
		}
		
		//Check if horn button is pressed.
		if(ControlsKeyboard.CAR_HORN.isPressed() && !powered.hornOn){
			WrapperNetwork.sendToServer(new PacketVehicleControlDigital(powered, PacketVehicleControlDigital.Controls.HORN, true));
		}else if(!ControlsKeyboard.CAR_HORN.isPressed() && powered.hornOn){
			WrapperNetwork.sendToServer(new PacketVehicleControlDigital(powered, PacketVehicleControlDigital.Controls.HORN, false));
		}
	}

	/**List of enums representing all controls present.  Add new controls by adding their enum values here, and
	 * setting their default values in {@link WrapperInput#getDefaultKeyCode(ControlsKeyboard)}.  This split is done
	 * to keep the keyboard-specific code out of this class.
	 *
	 * @author don_bruce
	 */
	public static enum ControlsKeyboard{
		AIRCRAFT_MOD(ControlsJoystick.AIRCRAFT_MOD, false),
		AIRCRAFT_CAMLOCK(ControlsJoystick.AIRCRAFT_CAMLOCK, true),
		AIRCRAFT_YAW_R(ControlsJoystick.AIRCRAFT_YAW, false),
		AIRCRAFT_YAW_L(ControlsJoystick.AIRCRAFT_YAW, false),
		AIRCRAFT_PITCH_U(ControlsJoystick.AIRCRAFT_PITCH, false),
		AIRCRAFT_PITCH_D(ControlsJoystick.AIRCRAFT_PITCH, false),
		AIRCRAFT_ROLL_R( ControlsJoystick.AIRCRAFT_ROLL, false),
		AIRCRAFT_ROLL_L(ControlsJoystick.AIRCRAFT_ROLL, false),
		AIRCRAFT_THROTTLE_U(ControlsJoystick.AIRCRAFT_THROTTLE, false),
		AIRCRAFT_THROTTLE_D(ControlsJoystick.AIRCRAFT_THROTTLE, false),
		AIRCRAFT_FLAPS_U(ControlsJoystick.AIRCRAFT_FLAPS_U, true),
		AIRCRAFT_FLAPS_D(ControlsJoystick.AIRCRAFT_FLAPS_D, true),
		AIRCRAFT_BRAKE(ControlsJoystick.AIRCRAFT_BRAKE, false),
		AIRCRAFT_PANEL(ControlsJoystick.AIRCRAFT_PANEL, true),
		AIRCRAFT_RADIO(ControlsJoystick.AIRCRAFT_RADIO, false),
		AIRCRAFT_GUN(ControlsJoystick.AIRCRAFT_GUN, false),
		AIRCRAFT_ZOOM_I(ControlsJoystick.AIRCRAFT_ZOOM_I, true),
		AIRCRAFT_ZOOM_O(ControlsJoystick.AIRCRAFT_ZOOM_O, true),
		
		CAR_MOD(ControlsJoystick.CAR_MOD, false),
		CAR_CAMLOCK(ControlsJoystick.CAR_CAMLOCK, true),
		CAR_TURN_R(ControlsJoystick.CAR_TURN, false),
		CAR_TURN_L(ControlsJoystick.CAR_TURN, false),
		CAR_GAS(ControlsJoystick.CAR_GAS, false),
		CAR_BRAKE(ControlsJoystick.CAR_BRAKE, false),
		CAR_PANEL(ControlsJoystick.CAR_PANEL, true),
		CAR_SHIFT_U(ControlsJoystick.CAR_SHIFT_U, true),
		CAR_SHIFT_D(ControlsJoystick.CAR_SHIFT_D, true),
		CAR_HORN(ControlsJoystick.CAR_HORN, false),
		CAR_RADIO(ControlsJoystick.CAR_RADIO, true),
		CAR_GUN(ControlsJoystick.CAR_GUN, false),
		CAR_ZOOM_I(ControlsJoystick.CAR_ZOOM_I, true),
		CAR_ZOOM_O(ControlsJoystick.CAR_ZOOM_O, true);
		
		
		public final boolean isMomentary;
		public final String systemName;
		public final String translatedName;
		public final ConfigKeyboard config;
		private final ControlsJoystick linkedJoystick;
		
		private boolean wasPressedLastCall;
		
		private ControlsKeyboard(ControlsJoystick linkedJoystick, boolean isMomentary){
			this.linkedJoystick = linkedJoystick;
			this.isMomentary = isMomentary;
			this.systemName = this.name().toLowerCase().replaceFirst("_", ".");
			this.translatedName = WrapperGUI.translate("input." + systemName);
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
			}else if(linkedJoystick.config.joystickName != null && ConfigSystem.configObject.client.kbOverride.value){
				return false;
			}else{
				if(isMomentary){
					if(wasPressedLastCall){
						wasPressedLastCall = WrapperInput.isKeyPressed(config.keyCode); 
						return false;
					}else{
						wasPressedLastCall = WrapperInput.isKeyPressed(config.keyCode);
						return wasPressedLastCall;
					}
				}else{
					return WrapperInput.isKeyPressed(config.keyCode);
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
		AIRCRAFT_FLAPS_U(false, true),
		AIRCRAFT_FLAPS_D(false, true),
		AIRCRAFT_BRAKE(false, false),
		AIRCRAFT_BRAKE_ANALOG(true, false),
		AIRCRAFT_PANEL(false, true),
		AIRCRAFT_PARK(false, true),
		AIRCRAFT_RADIO(false, true),
		AIRCRAFT_GUN(false, false),
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
		
		
		CAR_MOD(false, false),
		CAR_CAMLOCK(false, true),
		CAR_TURN(true, false),
		CAR_GAS(true, false),
		CAR_BRAKE(false, false),
		CAR_BRAKE_ANALOG(true, false),
		CAR_PANEL(false, true),
		CAR_SHIFT_U(false, true),
		CAR_SHIFT_D(false, true),
		CAR_HORN(false, false),
		CAR_PARK(false, true),
		CAR_RADIO(false, true),
		CAR_GUN(false, false),
		CAR_ZOOM_I(false, true),
		CAR_ZOOM_O(false, true),
		CAR_CHANGEVIEW(false, true),
		CAR_LOOK_L(false, false),
		CAR_LOOK_R(false, false),
		CAR_LOOK_U(false, false),
		CAR_LOOK_D(false, false),
		CAR_LOOK_A(false, false);
		
		
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
			this.translatedName = WrapperGUI.translate("input." + systemName);
			if(ConfigSystem.configObject.controls.joystick.containsKey(systemName)){
				this.config = ConfigSystem.configObject.controls.joystick.get(systemName);
			}else{
				this.config = new ConfigJoystick();
			}
		}
		
		public boolean isPressed(){
			if(isMomentary){
				if(wasPressedLastCall){
					wasPressedLastCall = WrapperInput.getJoystickInputValue(config.joystickName, config.buttonIndex) > 0; 
					return false;
				}else{
					wasPressedLastCall = WrapperInput.getJoystickInputValue(config.joystickName, config.buttonIndex) > 0;
					return wasPressedLastCall;
				}
			}else{
				return WrapperInput.getJoystickInputValue(config.joystickName, config.buttonIndex) > 0;
			}
		}
		
		private float getMultistateValue(){
			return WrapperInput.getJoystickInputValue(config.joystickName, config.buttonIndex);
		}
		
		//Return type is short to allow for easier packet transmission.
		private short getAxisState(short pollBounds){
			float pollValue = getMultistateValue();
			if(Math.abs(pollValue) > ConfigSystem.configObject.client.joystickDeadZone.value || pollBounds == 0){
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
		CAR_SLOW(ControlsKeyboard.CAR_GAS, ControlsKeyboard.CAR_MOD);		
		
		public final String translatedName;
		public final ControlsKeyboard mainControl;
		public final ControlsKeyboard modControl;
		
		private ControlsKeyboardDynamic(ControlsKeyboard mainControl, ControlsKeyboard modControl){
			this.translatedName = WrapperGUI.translate("input." + name().toLowerCase().replaceFirst("_", "."));
			this.mainControl = mainControl;
			this.modControl = modControl;
		}
		
		public boolean isPressed(){
			return this.modControl.isPressed() ? this.mainControl.isPressed() : false;
		}
	}
}
