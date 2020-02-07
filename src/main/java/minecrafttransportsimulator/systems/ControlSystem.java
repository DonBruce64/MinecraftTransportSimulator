package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.ClientProxy;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.guis.GUIPanelAircraft;
import minecrafttransportsimulator.guis.GUIPanelGround;
import minecrafttransportsimulator.guis.instances.GUIRadio;
import minecrafttransportsimulator.jsondefs.JSONConfig.ConfigJoystick;
import minecrafttransportsimulator.jsondefs.JSONConfig.ConfigKeyboard;
import minecrafttransportsimulator.packets.control.AileronPacket;
import minecrafttransportsimulator.packets.control.BrakePacket;
import minecrafttransportsimulator.packets.control.ElevatorPacket;
import minecrafttransportsimulator.packets.control.FlapPacket;
import minecrafttransportsimulator.packets.control.HornPacket;
import minecrafttransportsimulator.packets.control.ReverseThrustPacket;
import minecrafttransportsimulator.packets.control.RudderPacket;
import minecrafttransportsimulator.packets.control.ShiftPacket;
import minecrafttransportsimulator.packets.control.SteeringPacket;
import minecrafttransportsimulator.packets.control.ThrottlePacket;
import minecrafttransportsimulator.packets.control.TrimPacket;
import minecrafttransportsimulator.packets.parts.PacketPartGunSignal;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Ground;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartGun;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperInput;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**Class that handles all control operations.
 * Keybinding lists are initiated during the {@link ClientProxy} init method.
 * 
 * @author don_bruce
 */
public final class ControlSystem{	
	private static final int NULL_COMPONENT = 999;	
	
	/**
	 * Init the wrapper system for inputs, then iterate through the enums to initialize them.
	 * Joystick enums need to come first, as the Keyboard enums take them as constructor args.
	 * After we initialize the keboard enums, we set their default values.  If we don't initialize
	 * them first, we hit a switch error in {@link WrapperInput#getDefaultKeyCode(ControlsKeyboard)}.
	 * Once all this is done, save the results back to the disk to ensure the systems are synced.
	 */
	public static void init(){
		WrapperInput.init();
		
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
	
	private static void controlCamera(ControlsKeyboardDynamic dynamic, ControlsKeyboard zoomIn, ControlsKeyboard zoomOut, ControlsJoystick changeView){
		if(dynamic.isPressed()){
			if(CameraSystem.hudMode == 3){
				CameraSystem.hudMode = 0;
			}else{
				++CameraSystem.hudMode;
			}
		}else if(dynamic.mainControl.isPressed()){
			CameraSystem.changeCameraLock();
		}
		
		if(zoomIn.isPressed()){
			CameraSystem.changeCameraZoom(false);
		}
		if(zoomOut.isPressed()){
			CameraSystem.changeCameraZoom(true);
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
	
	private static void controlBrake(ControlsKeyboardDynamic dynamic, ControlsJoystick analogBrake, ControlsJoystick pBrake, int entityID){
		if(analogBrake.config.joystickName != null){
			if(pBrake.isPressed()){
				MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 12));
			}else if(analogBrake.getAxisState((short) 0) > 25){
				MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 11));
			}else{
				MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 2));
			}
		}else{
			if(dynamic.isPressed() || pBrake.isPressed()){
				MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 12));
			}else if(dynamic.mainControl.isPressed()){
				MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 11));
			}else{
				MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 2));
			}
		}
	}
	
	private static void controlGun(EntityVehicleE_Powered vehicle, ControlsKeyboard gun){
		PartSeat seat = vehicle.getSeatForRider(Minecraft.getMinecraft().player);
		if(seat != null){
			//If we are seated, attempt to control guns.
			//Only control guns our seat is a part of, or guns with no seats part of them.
			//First check our parent part.
			if(seat.parentPart instanceof APartGun){
				MTS.MTSNet.sendToServer(new PacketPartGunSignal((APartGun) seat.parentPart, Minecraft.getMinecraft().player.getEntityId(), gun.isPressed()));
			}
			//Now check subParts of our seat.
			for(APart<? extends EntityVehicleE_Powered> subPart : seat.childParts){
				if(subPart instanceof APartGun){
					MTS.MTSNet.sendToServer(new PacketPartGunSignal((APartGun) subPart, Minecraft.getMinecraft().player.getEntityId(), gun.isPressed()));
				}
			}
			//If we are the vehicle controller, check for guns that don't have seats. 
			if(seat.isController){
				for(APart<? extends EntityVehicleA_Base> part : vehicle.getVehicleParts()){
					if(part instanceof APartGun){
						if(!(part.parentPart instanceof PartSeat)){
							boolean hasControllingSeats = false;
							for(APart<? extends EntityVehicleE_Powered> subPart : part.childParts){
								if(subPart instanceof PartSeat){
									hasControllingSeats = true;
								}
							}
							if(!hasControllingSeats){
								MTS.MTSNet.sendToServer(new PacketPartGunSignal((APartGun) part, Minecraft.getMinecraft().player.getEntityId(), gun.isPressed()));
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
			}
		}
	}
	
	private static void controlAircraft(EntityVehicleF_Air aircraft, boolean isPlayerController){
		controlCamera(ControlsKeyboardDynamic.AIRCRAFT_CHANGEHUD, ControlsKeyboard.AIRCRAFT_ZOOM_I, ControlsKeyboard.AIRCRAFT_ZOOM_O, ControlsJoystick.AIRCRAFT_CHANGEVIEW);
		rotateCamera(ControlsJoystick.AIRCRAFT_LOOK_R, ControlsJoystick.AIRCRAFT_LOOK_L, ControlsJoystick.AIRCRAFT_LOOK_U, ControlsJoystick.AIRCRAFT_LOOK_D, ControlsJoystick.AIRCRAFT_LOOK_A);
		controlRadio(aircraft, ControlsKeyboard.AIRCRAFT_RADIO);
		controlGun(aircraft, ControlsKeyboard.AIRCRAFT_GUN);
		if(!isPlayerController){
			return;
		}
		controlBrake(ControlsKeyboardDynamic.AIRCRAFT_PARK, ControlsJoystick.AIRCRAFT_BRAKE_ANALOG, ControlsJoystick.AIRCRAFT_PARK, aircraft.getEntityId());
		
		//Open or close the panel.
		if(ControlsKeyboard.AIRCRAFT_PANEL.isPressed()){
			if(WrapperGUI.isGUIActive(null)){
				FMLCommonHandler.instance().showGuiScreen(new GUIPanelAircraft(aircraft));
			}else if(WrapperGUI.isGUIActive(GUIPanelAircraft.class)){
				WrapperGUI.closeGUI();
			}
		}
		
		//Check for thrust reverse button.
		if(ControlsJoystick.AIRCRAFT_REVERSE.isPressed()){
			MTS.proxy.playSound(aircraft.getPositionVector(), MTS.MODID + ":panel_buzzer", 1.0F, 1.0F);
			MTS.MTSNet.sendToServer(new ReverseThrustPacket(aircraft.getEntityId(), !aircraft.reverseThrust));
		}
		
		//Increment or decrement throttle.
		if(ControlsJoystick.AIRCRAFT_THROTTLE.config.joystickName != null){
			MTS.MTSNet.sendToServer(new ThrottlePacket(aircraft.getEntityId(), (byte) ControlsJoystick.AIRCRAFT_THROTTLE.getAxisState((short) 100)));
		}else{
			if(ControlsKeyboard.AIRCRAFT_THROTTLE_U.isPressed()){
				MTS.MTSNet.sendToServer(new ThrottlePacket(aircraft.getEntityId(), Byte.MAX_VALUE));
			}
			if(ControlsKeyboard.AIRCRAFT_THROTTLE_D.isPressed()){
				MTS.MTSNet.sendToServer(new ThrottlePacket(aircraft.getEntityId(), Byte.MIN_VALUE));
			}
		}		
		
		//Check flaps.
		if(aircraft.definition.plane != null && aircraft.definition.plane.hasFlaps){
			if(ControlsKeyboard.AIRCRAFT_FLAPS_U.isPressed()){
				MTS.MTSNet.sendToServer(new FlapPacket(aircraft.getEntityId(), (byte) -50));
			}
			if(ControlsKeyboard.AIRCRAFT_FLAPS_D.isPressed()){
				MTS.MTSNet.sendToServer(new FlapPacket(aircraft.getEntityId(), (byte) 50));
			}
		}
		
		//Check yaw.
		if(ControlsJoystick.AIRCRAFT_YAW.config.joystickName != null){
			MTS.MTSNet.sendToServer(new RudderPacket(aircraft.getEntityId(), ControlsJoystick.AIRCRAFT_YAW.getAxisState((short) 250)));
		}else{
			if(ControlsKeyboard.AIRCRAFT_YAW_R.isPressed()){
				MTS.MTSNet.sendToServer(new RudderPacket(aircraft.getEntityId(), (short) (ConfigSystem.configObject.client.steeringIncrement.value.shortValue()*(aircraft.rudderAngle < 0 ? 2 : 1)), ConfigSystem.configObject.client.controlSurfaceCooldown.value.byteValue()));
			}
			if(ControlsKeyboard.AIRCRAFT_YAW_L.isPressed()){
				MTS.MTSNet.sendToServer(new RudderPacket(aircraft.getEntityId(), (short) (-ConfigSystem.configObject.client.steeringIncrement.value.shortValue()*(aircraft.rudderAngle > 0 ? 2 : 1)), ConfigSystem.configObject.client.controlSurfaceCooldown.value.byteValue()));
			}
		}
		if(ControlsJoystick.AIRCRAFT_TRIM_YAW_R.isPressed()){
			MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 10));
		}
		if(ControlsJoystick.AIRCRAFT_TRIM_YAW_L.isPressed()){
			MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 2));
		}
		
		//Check is mouse yoke is enabled.  If so do controls by mouse rather than buttons.
		if(ConfigSystem.configObject.client.mouseYoke.value){
			if(CameraSystem.lockedView && WrapperGUI.isGUIActive(null)){
				long mousePosition = WrapperInput.getTrackedMousePosition();
				MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), (short) (mousePosition >> Integer.SIZE)));
				MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), (short) (mousePosition & Integer.MAX_VALUE)));
			}
		}else{
			//Check pitch.
			if(ControlsJoystick.AIRCRAFT_PITCH.config.joystickName != null){
				MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), ControlsJoystick.AIRCRAFT_PITCH.getAxisState((short) 250)));
			}else{
				if(ControlsKeyboard.AIRCRAFT_PITCH_U.isPressed()){
					MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), true, ConfigSystem.configObject.client.controlSurfaceCooldown.value.shortValue()));
				}
				if(ControlsKeyboard.AIRCRAFT_PITCH_D.isPressed()){
					MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), false, ConfigSystem.configObject.client.controlSurfaceCooldown.value.shortValue()));
				}
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_PITCH_U.isPressed()){
				MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 9));
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_PITCH_D.isPressed()){
				MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 1));
			}
			
			//Check roll.
			if(ControlsJoystick.AIRCRAFT_ROLL.config.joystickName != null){
				MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), ControlsJoystick.AIRCRAFT_ROLL.getAxisState((short) 250)));
			}else{
				if(ControlsKeyboard.AIRCRAFT_ROLL_R.isPressed()){
					MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), true, ConfigSystem.configObject.client.controlSurfaceCooldown.value.shortValue()));
				}
				if(ControlsKeyboard.AIRCRAFT_ROLL_L.isPressed()){
					MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), false, ConfigSystem.configObject.client.controlSurfaceCooldown.value.shortValue()));
				}
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_ROLL_R.isPressed()){
				MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 8));
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_ROLL_L.isPressed()){
				MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 0));
			}
		}
	}
	
	private static void controlGroundVehicle(EntityVehicleF_Ground powered, boolean isPlayerController){
		controlCamera(ControlsKeyboardDynamic.CAR_CHANGEHUD, ControlsKeyboard.CAR_ZOOM_I, ControlsKeyboard.CAR_ZOOM_O, ControlsJoystick.CAR_CHANGEVIEW);
		rotateCamera(ControlsJoystick.CAR_LOOK_R, ControlsJoystick.CAR_LOOK_L, ControlsJoystick.CAR_LOOK_U, ControlsJoystick.CAR_LOOK_D, ControlsJoystick.CAR_LOOK_A);
		controlRadio(powered, ControlsKeyboard.CAR_RADIO);
		controlGun(powered, ControlsKeyboard.CAR_GUN);
		if(!isPlayerController){
			return;
		}
		controlBrake(ControlsKeyboardDynamic.CAR_PARK, ControlsJoystick.CAR_BRAKE_ANALOG, ControlsJoystick.CAR_PARK, powered.getEntityId());
		
		//Open or close the panel.
		if(ControlsKeyboard.CAR_PANEL.isPressed()){
			if(WrapperGUI.isGUIActive(null)){
				FMLCommonHandler.instance().showGuiScreen(new GUIPanelGround(powered));
			}else if(WrapperGUI.isGUIActive(GUIPanelGround.class)){
				WrapperGUI.closeGUI();
			}
		}
		
		//Change gas to on or off.
		if(ControlsJoystick.CAR_GAS.config.joystickName != null){
			MTS.MTSNet.sendToServer(new ThrottlePacket(powered.getEntityId(), (byte) ControlsJoystick.CAR_GAS.getAxisState((short) 100)));
		}else{
			if(ControlsKeyboardDynamic.CAR_SLOW.isPressed()){
				MTS.MTSNet.sendToServer(new ThrottlePacket(powered.getEntityId(), (byte) 50));
			}else if(ControlsKeyboard.CAR_GAS.isPressed()){
				MTS.MTSNet.sendToServer(new ThrottlePacket(powered.getEntityId(), (byte) 100));
			}else{
				MTS.MTSNet.sendToServer(new ThrottlePacket(powered.getEntityId(), (byte) 0));
			}
		}
		
		//Check steering, turn signals, and lights.
		//Check is mouse yoke is enabled.  If so do controls by mouse rather than buttons.
		if(ConfigSystem.configObject.client.mouseYoke.value){
			if(CameraSystem.lockedView && WrapperGUI.isGUIActive(null)){
				long mousePosition = WrapperInput.getTrackedMousePosition();
				MTS.MTSNet.sendToServer(new SteeringPacket(powered.getEntityId(), (short) (mousePosition >> Integer.SIZE)));
			}
		}else{
			if(ControlsJoystick.CAR_TURN.config.joystickName != null){
				MTS.MTSNet.sendToServer(new SteeringPacket(powered.getEntityId(), ControlsJoystick.CAR_TURN.getAxisState((short) 450)));
			}else{
				//Depending on what we are pressing, send out packets.
				//If we are turning in the opposite direction of our current angle, send out a packet with twice the value.
				boolean turningRight = ControlsKeyboard.CAR_TURN_R.isPressed();
				boolean turningLeft = ControlsKeyboard.CAR_TURN_L.isPressed();
				if(turningRight && !turningLeft){
					MTS.MTSNet.sendToServer(new SteeringPacket(powered.getEntityId(), (short) (ConfigSystem.configObject.client.steeringIncrement.value.shortValue()*(powered.steeringAngle < 0 ? 2 : 1)), ConfigSystem.configObject.client.controlSurfaceCooldown.value.byteValue()));
				}else if(turningLeft && !turningRight){
					MTS.MTSNet.sendToServer(new SteeringPacket(powered.getEntityId(), (short) (-ConfigSystem.configObject.client.steeringIncrement.value.shortValue()*(powered.steeringAngle > 0 ? 2 : 1)), ConfigSystem.configObject.client.controlSurfaceCooldown.value.byteValue()));
				}
			}
		}
		
		//Check if we are shifting.
		if(ControlsKeyboard.CAR_SHIFT_U.isPressed()){
			MTS.MTSNet.sendToServer(new ShiftPacket(powered.getEntityId(), true));
		}
		if(ControlsKeyboard.CAR_SHIFT_D.isPressed()){
			MTS.MTSNet.sendToServer(new ShiftPacket(powered.getEntityId(), false));
		}
		
		//Check if horn button is pressed.
		if(ControlsKeyboard.CAR_HORN.isPressed()){
			MTS.MTSNet.sendToServer(new HornPacket(powered.getEntityId(), true));
		}else{
			MTS.MTSNet.sendToServer(new HornPacket(powered.getEntityId(), false));
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
			}else if(linkedJoystick.config.joystickName != null && ConfigSystem.configObject.client.keyboardOverride.value){
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
		CAR_RADIO(false, false),
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
					pollValue = (float) ((pollValue - config.axisMinTravel)/(config.axisMinTravel - config.axisMinTravel));
					
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
		AIRCRAFT_CHANGEHUD(ControlsKeyboard.AIRCRAFT_CAMLOCK, ControlsKeyboard.AIRCRAFT_MOD),
		AIRCRAFT_PARK(ControlsKeyboard.AIRCRAFT_BRAKE, ControlsKeyboard.AIRCRAFT_MOD),
		
		CAR_CHANGEHUD(ControlsKeyboard.CAR_CAMLOCK, ControlsKeyboard.CAR_MOD),
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
