package minecrafttransportsimulator.systems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import minecrafttransportsimulator.ClientProxy;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle.LightTypes;
import minecrafttransportsimulator.entities.main.EntityCar;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.guis.GUIPanelAircraft;
import minecrafttransportsimulator.packets.control.AileronPacket;
import minecrafttransportsimulator.packets.control.BrakePacket;
import minecrafttransportsimulator.packets.control.ElevatorPacket;
import minecrafttransportsimulator.packets.control.EnginePacket;
import minecrafttransportsimulator.packets.control.FlapPacket;
import minecrafttransportsimulator.packets.control.HornPacket;
import minecrafttransportsimulator.packets.control.LightPacket;
import minecrafttransportsimulator.packets.control.RudderPacket;
import minecrafttransportsimulator.packets.control.ShiftPacket;
import minecrafttransportsimulator.packets.control.SteeringPacket;
import minecrafttransportsimulator.packets.control.ThrottlePacket;
import minecrafttransportsimulator.packets.control.TrimPacket;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**Class that handles all control operations.
 * Keybinding lists are initiated during the {@link ClientProxy} init method.
 * 
 * @author don_bruce
 */
public final class ControlSystem{	
	private static boolean joystickEnabled = false;
	private static final int NULL_COMPONENT = 999;
	private static final String KEYBOARD_CONFIG = "controls_keyboard";
	private static final String JOYSTICK_CONFIG = "controls_joystick";
	private static final Map<String, Controller> joystickMap = new HashMap<String, Controller>();
	
	private static KeyBinding configKey;
	private static short mousePosX = 0;
	private static short mousePosY = 0;
	private static final List<ControlsKeyboard> pressedKeyboardButtons = new ArrayList<ControlsKeyboard>();
	private static final List<ControlsJoystick> pressedJoystickButtons = new ArrayList<ControlsJoystick>();
	
	
	public static void init(){
		configKey = new KeyBinding("key.config", Keyboard.KEY_P, "key.categories." + MTS.MODID);
		ClientRegistry.registerKeyBinding(configKey);
		
		//Populate the joystick device map.
		//Joystick will be enabled if at least one controller is found.  If none are found, we likely have an error.
		for(Controller joystick : ControllerEnvironment.getDefaultEnvironment().getControllers()){
			joystickEnabled = true;
			if(joystick.getType() != null && joystick.getName() != null){
				if(!joystick.getType().equals(Controller.Type.MOUSE) && !joystick.getType().equals(Controller.Type.KEYBOARD)){
					if(joystick.getComponents().length != 0){
						joystickMap.put(joystick.getName(), joystick);
					}
				}
			}
		}
		
		//Recall keybindings from config file.
		for(ControlsKeyboard control : ControlsKeyboard.values()){
			control.button = ConfigSystem.config.get(KEYBOARD_CONFIG, control.buttonName, control.defaultButton).getInt();
		}
		for(ControlsJoystick control : ControlsJoystick.values()){
			control.joystickAssigned = ConfigSystem.config.get(JOYSTICK_CONFIG, control.buttonName + "_joystick", "").getString();
			control.joystickButton = ConfigSystem.config.get(JOYSTICK_CONFIG, control.buttonName + "_button", NULL_COMPONENT).getInt();
			if(control.isAxis){
				control.joystickMinTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, control.buttonName + "_mintravel", -1D).getDouble();
				control.joystickMaxTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, control.buttonName + "_maxtravel", 1D).getDouble();
				control.joystickInverted = ConfigSystem.config.get(JOYSTICK_CONFIG, control.buttonName + "_inverted", false).getBoolean();
			}
		}
		ConfigSystem.config.save();
	}
	
	public static boolean isMasterControlButttonPressed(){
		return configKey.isPressed();
	}
	
	public static boolean isJoystickSupportEnabled(){
		return joystickEnabled;
	}
	
	public static void setKeyboardKey(ControlsKeyboard control, int keyNumber){
		control.button = keyNumber;
		ConfigSystem.config.getCategory(KEYBOARD_CONFIG).put(control.buttonName, new Property(control.buttonName, String.valueOf(keyNumber), Property.Type.INTEGER));
		ConfigSystem.config.save();
	}
	
	public static void setControlJoystick(ControlsJoystick control, String joystickName, int componentId){
		for(ControlsJoystick cont : ControlsJoystick.values()){
			if(cont.joystickAssigned.equals(joystickName) && cont.joystickButton == componentId){
				cont.joystickAssigned = "";
				cont.joystickButton = NULL_COMPONENT;
			}
		}
		control.joystickAssigned = joystickName;
		control.joystickButton = componentId;
		ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put(control.buttonName + "_joystick", new Property(control.buttonName + "_joystick", joystickName, Property.Type.STRING));
		ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put(control.buttonName + "_button", new Property(control.buttonName + "_button", String.valueOf(componentId), Property.Type.INTEGER));
		ConfigSystem.config.save();
	}
	
	public static void setAxisJoystick(ControlsJoystick control, String joystickName, int componentId, double minBound, double maxBound, boolean inverted){
		setControlJoystick(control, joystickName, componentId);
		control.joystickMinTravel = minBound;
		control.joystickMaxTravel = maxBound;
		control.joystickInverted = inverted;
		ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put(control.buttonName + "_mintravel", new Property(control.buttonName + "_mintravel", String.valueOf(control.joystickMinTravel), Property.Type.DOUBLE));
		ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put(control.buttonName + "_maxtravel", new Property(control.buttonName + "_maxtravel", String.valueOf(control.joystickMaxTravel), Property.Type.DOUBLE));
		ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put(control.buttonName + "_inverted", new Property(control.buttonName + "_inverted", String.valueOf(control.joystickInverted), Property.Type.BOOLEAN));
		ConfigSystem.config.save();
	}
	
	public static void clearControlJoystick(ControlsJoystick control){
		setControlJoystick(control, "", NULL_COMPONENT);
	}
	
	public static void controlVehicle(EntityMultipartVehicle vehicle, boolean isPlayerController){
		if(vehicle instanceof EntityPlane){
			controlAircraft(vehicle, isPlayerController);
		}else if(vehicle instanceof EntityCar){
			controlCar(vehicle, isPlayerController);
		}
	}
	
	private static boolean getTrueKeyboardButtonState(ControlsKeyboard control, boolean pressed){
		//If this control is used in a momentary fashion make sure to only fire the event once!
		if(pressed){
			if(control.isMomentary){
				if(pressedKeyboardButtons.contains(control)){
					return false;
				}else{
					pressedKeyboardButtons.add(control);
					return true;
				}
			}else{
				return true;
			}
		}else{
			pressedKeyboardButtons.remove(control);
			return false;
		}
	}
	
	private static boolean getTrueJoystickButtonState(ControlsJoystick control, boolean pressed){
		if(pressed){
			if(control.isMomentary){
				if(pressedJoystickButtons.contains(control)){
					return false;
				}else{
					pressedJoystickButtons.add(control);
					return true;
				}
			}else{
				return true;
			}
		}else{
			pressedJoystickButtons.remove(control);
			return false;
		}
	}
	
	private static float getJoystickMultistateValue(ControlsJoystick control){
		//Check to make sure this control is operational before testing.  It could have been removed from a prior game.
		if(joystickMap.containsKey(control.joystickAssigned)){
			joystickMap.get(control.joystickAssigned).poll();
			return joystickMap.get(control.joystickAssigned).getComponents()[control.joystickButton].getPollData();
		}else{
			return 0;
		}
	}
	
	//Return type is short to allow for easier packet transmission.
	private static short getJoystickAxisState(ControlsJoystick control, boolean normalized){
		if(joystickMap.containsKey(control.joystickAssigned)){
			joystickMap.get(control.joystickAssigned).poll();
			float pollValue = joystickMap.get(control.joystickAssigned).getComponents()[control.joystickButton].getPollData();
			if(Math.abs(pollValue) > ConfigSystem.getDoubleConfig("JoystickDeadZone")){
				//Clamp the poll value to the defined axis bounds set during config to prevent over and under-runs.
				pollValue = (float) Math.max(control.joystickMinTravel, pollValue);
				pollValue = (float) Math.min(control.joystickMaxTravel, pollValue);				
				
				//If we don't need to normalize the axis, return it as-is.  Otherwise do a normalization from 0-1.
				if(!normalized){
					return (short) (control.joystickInverted ? (-250*pollValue) : (250*pollValue));
				}else{
					//Divide the poll value plus the min bounds by the span to get it in the range of 0-1.
					pollValue = (float) ((pollValue - control.joystickMinTravel)/(control.joystickMaxTravel - control.joystickMinTravel));
					
					//If axis is inverted, invert poll.
					if(control.joystickInverted){
						pollValue = 1 - pollValue;
					}
					
					//Now return this value in a range from 0-100.
					return (short) (pollValue*100);
				}
			}
			
		}
		return 0;
	}
	
	private static void controlCamera(ControlsKeyboardDynamic dynamic, ControlsKeyboard zoomIn, ControlsKeyboard zoomOut, ControlsJoystick hudMode, ControlsJoystick changeView){
		if(dynamic.isPressed() || hudMode.isPressed()){
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
			Minecraft.getMinecraft().thePlayer.rotationYaw+=3;
		}
		if(lookL.isPressed()){
			Minecraft.getMinecraft().thePlayer.rotationYaw-=3;
		}
		if(lookU.isPressed()){
			Minecraft.getMinecraft().thePlayer.rotationPitch-=3;
		}
		if(lookD.isPressed()){
			Minecraft.getMinecraft().thePlayer.rotationPitch+=3;
		}
		
		float pollData = getJoystickMultistateValue(lookA);
		if(pollData != 0){
			if(pollData >= 0.125F && pollData <= 0.375F){
				Minecraft.getMinecraft().thePlayer.rotationPitch+=3;
			}
			if(pollData >= 0.375F && pollData <= 0.625F){
				Minecraft.getMinecraft().thePlayer.rotationYaw+=3;
			}
			if(pollData >= 0.625F && pollData <= 0.875F){
				Minecraft.getMinecraft().thePlayer.rotationPitch-=3;
			}
			if(pollData >= 0.875F || pollData <= 0.125F){
				Minecraft.getMinecraft().thePlayer.rotationYaw-=3;
			}
		}
	}
	
	private static void controlBrake(ControlsKeyboardDynamic dynamic, ControlsJoystick pBrake, int entityID){
		if(dynamic.isPressed() || pBrake.isPressed()){
			MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 12));
		}else if(dynamic.mainControl.isPressed()){
			MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 11));
		}else{
			MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 2));
		}
	}
	
	private static void controlAircraft(EntityMultipartVehicle aircraft, boolean isPlayerController){
		controlCamera(ControlsKeyboardDynamic.AIRCRAFT_CHANGEHUD, ControlsKeyboard.AIRCRAFT_ZOOM_I, ControlsKeyboard.AIRCRAFT_ZOOM_O, ControlsJoystick.AIRCRAFT_CHANGEHUD, ControlsJoystick.AIRCRAFT_CHANGEVIEW);
		rotateCamera(ControlsJoystick.AIRCRAFT_LOOK_R, ControlsJoystick.AIRCRAFT_LOOK_L, ControlsJoystick.AIRCRAFT_LOOK_U, ControlsJoystick.AIRCRAFT_LOOK_D, ControlsJoystick.AIRCRAFT_LOOK_A);
		if(!isPlayerController){
			return;
		}
		controlBrake(ControlsKeyboardDynamic.AIRCRAFT_PARK, ControlsJoystick.AIRCRAFT_PARK, aircraft.getEntityId());
		
		//Open or close the panel.
		if(ControlsKeyboard.AIRCRAFT_PANEL.isPressed()){
			if(Minecraft.getMinecraft().currentScreen == null){
				FMLCommonHandler.instance().showGuiScreen(new GUIPanelAircraft(aircraft));
			}else if(Minecraft.getMinecraft().currentScreen instanceof GUIPanelAircraft){
				Minecraft.getMinecraft().displayGuiScreen((GuiScreen)null);
				Minecraft.getMinecraft().setIngameFocus();
			}
		}
		
		//Increment or decrement throttle.
		if(joystickMap.containsKey(ControlsJoystick.AIRCRAFT_THROTTLE.joystickAssigned) && ControlsJoystick.AIRCRAFT_THROTTLE.joystickButton != NULL_COMPONENT){
			MTS.MTSNet.sendToServer(new ThrottlePacket(aircraft.getEntityId(), (byte) getJoystickAxisState(ControlsJoystick.AIRCRAFT_THROTTLE, true)));
		}else{
			if(ControlsKeyboard.AIRCRAFT_THROTTLE_U.isPressed()){
				MTS.MTSNet.sendToServer(new ThrottlePacket(aircraft.getEntityId(), Byte.MAX_VALUE));
			}
			if(ControlsKeyboard.AIRCRAFT_THROTTLE_D.isPressed()){
				MTS.MTSNet.sendToServer(new ThrottlePacket(aircraft.getEntityId(), Byte.MIN_VALUE));
			}
		}
		
		//Check flaps.
		if(aircraft.pack.general.type.equals("plane") && aircraft.pack.plane.hasFlaps){
			if(ControlsKeyboard.AIRCRAFT_FLAPS_U.isPressed()){
				MTS.MTSNet.sendToServer(new FlapPacket(aircraft.getEntityId(), (byte) -50));
			}
			if(ControlsKeyboard.AIRCRAFT_FLAPS_D.isPressed()){
				MTS.MTSNet.sendToServer(new FlapPacket(aircraft.getEntityId(), (byte) 50));
			}
		}
		
		//Check yaw.
		if(joystickMap.containsKey(ControlsJoystick.AIRCRAFT_YAW.joystickAssigned) && ControlsJoystick.AIRCRAFT_YAW.joystickButton != NULL_COMPONENT){
			MTS.MTSNet.sendToServer(new RudderPacket(aircraft.getEntityId(), getJoystickAxisState(ControlsJoystick.AIRCRAFT_YAW, false)));
		}else{
			if(ControlsKeyboard.AIRCRAFT_YAW_R.isPressed() && !ControlsKeyboardDynamic.AIRCRAFT_TRIM_YAW_R.isPressed()){
				MTS.MTSNet.sendToServer(new RudderPacket(aircraft.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
			}
			if(ControlsKeyboard.AIRCRAFT_YAW_L.isPressed() && !ControlsKeyboardDynamic.AIRCRAFT_TRIM_YAW_L.isPressed()){
				MTS.MTSNet.sendToServer(new RudderPacket(aircraft.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
			}
		}
		if(ControlsKeyboardDynamic.AIRCRAFT_TRIM_YAW_R.isPressed() || ControlsJoystick.AIRCRAFT_TRIM_YAW_R.isPressed()){
			MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 10));
		}
		if(ControlsKeyboardDynamic.AIRCRAFT_TRIM_YAW_L.isPressed() || ControlsJoystick.AIRCRAFT_TRIM_YAW_L.isPressed()){
			MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 2));
		}
		
		//Check is mouse yoke is enabled.  If so do controls by mouse rather than buttons.
		if(ConfigSystem.getBooleanConfig("MouseYoke")){
			if(CameraSystem.lockedView && Minecraft.getMinecraft().currentScreen == null){
				int dx = Mouse.getDX();
				int dy = Mouse.getDY();
				if(Math.abs(dx) < 100){
					mousePosX = (short) Math.max(Math.min(mousePosX + dx/5F, 250), -250);
				}
				if(Math.abs(dy) < 100){
					mousePosY = (short) Math.max(Math.min(mousePosY - dy, 250), -250);
				}
				MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), mousePosX));
				MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), mousePosY));
			}
		}else{
			//Check pitch.
			if(joystickMap.containsKey(ControlsJoystick.AIRCRAFT_PITCH.joystickAssigned) && ControlsJoystick.AIRCRAFT_PITCH.joystickButton != NULL_COMPONENT){
				MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), getJoystickAxisState(ControlsJoystick.AIRCRAFT_PITCH, false)));
			}else{
				if(ControlsKeyboard.AIRCRAFT_PITCH_U.isPressed() && !ControlsKeyboardDynamic.AIRCRAFT_TRIM_PITCH_U.isPressed()){
					MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
				if(ControlsKeyboard.AIRCRAFT_PITCH_D.isPressed() && !ControlsKeyboardDynamic.AIRCRAFT_TRIM_PITCH_D.isPressed()){
					MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}
			if(ControlsKeyboardDynamic.AIRCRAFT_TRIM_PITCH_U.isPressed() || ControlsJoystick.AIRCRAFT_TRIM_PITCH_U.isPressed()){
				MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 9));
			}
			if(ControlsKeyboardDynamic.AIRCRAFT_TRIM_PITCH_D.isPressed() || ControlsJoystick.AIRCRAFT_TRIM_PITCH_D.isPressed()){
				MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 1));
			}
			
			//Check roll.
			if(joystickMap.containsKey(ControlsJoystick.AIRCRAFT_ROLL.joystickAssigned) && ControlsJoystick.AIRCRAFT_ROLL.joystickButton != NULL_COMPONENT){
				MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), getJoystickAxisState(ControlsJoystick.AIRCRAFT_ROLL, false)));
			}else{
				if(ControlsKeyboard.AIRCRAFT_ROLL_R.isPressed() && !ControlsKeyboardDynamic.AIRCRAFT_TRIM_ROLL_R.isPressed()){
					MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
				if(ControlsKeyboard.AIRCRAFT_ROLL_L.isPressed() && !ControlsKeyboardDynamic.AIRCRAFT_TRIM_ROLL_L.isPressed()){
					MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}
			if(ControlsKeyboardDynamic.AIRCRAFT_TRIM_ROLL_R.isPressed() || ControlsJoystick.AIRCRAFT_TRIM_ROLL_R.isPressed()){
				MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 8));
			}
			if(ControlsKeyboardDynamic.AIRCRAFT_TRIM_ROLL_L.isPressed() || ControlsJoystick.AIRCRAFT_TRIM_ROLL_L.isPressed()){
				MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 0));
			}
		}
	}
	
	private static void controlCar(EntityMultipartVehicle car, boolean isPlayerController){
		controlCamera(ControlsKeyboardDynamic.CAR_CHANGEHUD, ControlsKeyboard.CAR_ZOOM_I, ControlsKeyboard.CAR_ZOOM_O, ControlsJoystick.CAR_CHANGEHUD, ControlsJoystick.CAR_CHANGEVIEW);
		rotateCamera(ControlsJoystick.CAR_LOOK_R, ControlsJoystick.CAR_LOOK_L, ControlsJoystick.CAR_LOOK_U, ControlsJoystick.CAR_LOOK_D, ControlsJoystick.CAR_LOOK_A);
		if(!isPlayerController){
			return;
		}
		controlBrake(ControlsKeyboardDynamic.CAR_PARK, ControlsJoystick.CAR_PARK, car.getEntityId());
		
		//Change gas to on or off.
		if(joystickMap.containsKey(ControlsJoystick.CAR_GAS.joystickAssigned) && ControlsJoystick.CAR_GAS.joystickButton != NULL_COMPONENT){
			MTS.MTSNet.sendToServer(new ThrottlePacket(car.getEntityId(), (byte) getJoystickAxisState(ControlsJoystick.CAR_GAS, true)));
		}else{
			if(ControlsKeyboard.CAR_GAS.isPressed()){
				MTS.MTSNet.sendToServer(new ThrottlePacket(car.getEntityId(), (byte) 100));
			}else{
				MTS.MTSNet.sendToServer(new ThrottlePacket(car.getEntityId(), (byte) 0));
			}
		}
		
		//Check steering, turn signals, and regular lights.
		boolean lightButtonPressed = ControlsKeyboard.CAR_LIGHTS.isPressed();
		if(lightButtonPressed && ControlsKeyboard.CAR_TURN_L.isPressed()){
			MTS.MTSNet.sendToServer(new LightPacket(car.getEntityId(), LightTypes.LEFTTURNLIGHT));
		}else if(lightButtonPressed && ControlsKeyboard.CAR_TURN_R.isPressed()){
			MTS.MTSNet.sendToServer(new LightPacket(car.getEntityId(), LightTypes.RIGHTTURNLIGHT));
		}else{
			if(lightButtonPressed){
				MTS.MTSNet.sendToServer(new LightPacket(car.getEntityId(), LightTypes.HEADLIGHT));
				MTS.MTSNet.sendToServer(new LightPacket(car.getEntityId(), LightTypes.RUNNINGLIGHT));
			}
			if(joystickMap.containsKey(ControlsJoystick.CAR_TURN.joystickAssigned) && ControlsJoystick.CAR_TURN.joystickButton != NULL_COMPONENT){
				MTS.MTSNet.sendToServer(new SteeringPacket(car.getEntityId(), getJoystickAxisState(ControlsJoystick.CAR_TURN, false)));
			}else{
				boolean turningRight = ControlsKeyboard.CAR_TURN_R.isPressed();
				boolean turningLeft = ControlsKeyboard.CAR_TURN_L.isPressed();
				if(turningRight && !turningLeft){
					MTS.MTSNet.sendToServer(new SteeringPacket(car.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}else if(turningLeft && !turningRight){
					MTS.MTSNet.sendToServer(new SteeringPacket(car.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}
		}
		
		//Check starter.
		if(ControlsKeyboard.CAR_START.isPressed()){
			if(car.getEngineByNumber((byte) 1) != null){
				if(ControlsKeyboard.CAR_MOD.isPressed()){
					MTS.MTSNet.sendToServer(new EnginePacket(car.getEntityId(), car.getEngineByNumber((byte) 1).getEntityId(), (byte) 0));
				}else{
					MTS.MTSNet.sendToServer(new EnginePacket(car.getEntityId(), car.getEngineByNumber((byte) 1).getEntityId(), (byte) 1));
					MTS.MTSNet.sendToServer(new EnginePacket(car.getEntityId(), car.getEngineByNumber((byte) 1).getEntityId(), (byte) 3));
				}
			}
		}else{
			if(car.getEngineByNumber((byte) 1) != null){
				MTS.MTSNet.sendToServer(new EnginePacket(car.getEntityId(), car.getEngineByNumber((byte) 1).getEntityId(), (byte) 2));
			}
		}
		
		//Check if we are shifting.
		if(ControlsKeyboard.CAR_SHIFT_U.isPressed()){
			MTS.MTSNet.sendToServer(new ShiftPacket(car.getEntityId(), true));
		}
		if(ControlsKeyboard.CAR_SHIFT_D.isPressed()){
			MTS.MTSNet.sendToServer(new ShiftPacket(car.getEntityId(), false));
		}
		
		//Check if horn button is pressed.
		if(ControlsKeyboard.CAR_HORN.isPressed()){
			MTS.MTSNet.sendToServer(new HornPacket(car.getEntityId(), true));
		}else{
			MTS.MTSNet.sendToServer(new HornPacket(car.getEntityId(), false));
		}
	}
		
	public enum ControlsKeyboard{
		AIRCRAFT_MOD(Keyboard.KEY_RSHIFT, ControlsJoystick.AIRCRAFT_MOD, false),
		AIRCRAFT_CAMLOCK(Keyboard.KEY_RCONTROL, ControlsJoystick.AIRCRAFT_CAMLOCK, true),
		AIRCRAFT_YAW_R(Keyboard.KEY_L, ControlsJoystick.AIRCRAFT_YAW, false),
		AIRCRAFT_YAW_L(Keyboard.KEY_J, ControlsJoystick.AIRCRAFT_YAW, false),
		AIRCRAFT_PITCH_U(Keyboard.KEY_S, ControlsJoystick.AIRCRAFT_PITCH, false),
		AIRCRAFT_PITCH_D(Keyboard.KEY_W, ControlsJoystick.AIRCRAFT_PITCH, false),
		AIRCRAFT_ROLL_R(Keyboard.KEY_D, ControlsJoystick.AIRCRAFT_ROLL, false),
		AIRCRAFT_ROLL_L(Keyboard.KEY_A, ControlsJoystick.AIRCRAFT_ROLL, false),
		AIRCRAFT_THROTTLE_U(Keyboard.KEY_I, ControlsJoystick.AIRCRAFT_THROTTLE, false),
		AIRCRAFT_THROTTLE_D(Keyboard.KEY_K, ControlsJoystick.AIRCRAFT_THROTTLE, false),
		AIRCRAFT_FLAPS_U(Keyboard.KEY_Y, ControlsJoystick.AIRCRAFT_FLAPS_U, true),
		AIRCRAFT_FLAPS_D(Keyboard.KEY_H, ControlsJoystick.AIRCRAFT_FLAPS_D, true),
		AIRCRAFT_BRAKE(Keyboard.KEY_B, ControlsJoystick.AIRCRAFT_BRAKE, false),
		AIRCRAFT_PANEL(Keyboard.KEY_U, ControlsJoystick.AIRCRAFT_PANEL, true),
		AIRCRAFT_ZOOM_I(Keyboard.KEY_PRIOR, ControlsJoystick.AIRCRAFT_ZOOM_I, true),
		AIRCRAFT_ZOOM_O(Keyboard.KEY_NEXT, ControlsJoystick.AIRCRAFT_ZOOM_O, true),
		
		CAR_MOD(Keyboard.KEY_RSHIFT, ControlsJoystick.CAR_MOD, false),
		CAR_CAMLOCK(Keyboard.KEY_RCONTROL, ControlsJoystick.CAR_CAMLOCK, true),
		CAR_TURN_R(Keyboard.KEY_D, ControlsJoystick.CAR_TURN, false),
		CAR_TURN_L(Keyboard.KEY_A, ControlsJoystick.CAR_TURN, false),
		CAR_GAS(Keyboard.KEY_W, ControlsJoystick.CAR_GAS, false),
		CAR_BRAKE(Keyboard.KEY_S, ControlsJoystick.CAR_BRAKE, false),
		CAR_SHIFT_U(Keyboard.KEY_R, ControlsJoystick.CAR_SHIFT_U, true),
		CAR_SHIFT_D(Keyboard.KEY_F, ControlsJoystick.CAR_SHIFT_D, true),
		CAR_HORN(Keyboard.KEY_C, ControlsJoystick.CAR_HORN, false),
		CAR_START(Keyboard.KEY_Z, ControlsJoystick.CAR_START, false),
		CAR_LIGHTS(Keyboard.KEY_X, ControlsJoystick.CAR_LIGHTS, true),
		CAR_ZOOM_I(Keyboard.KEY_PRIOR, ControlsJoystick.CAR_ZOOM_I, true),
		CAR_ZOOM_O(Keyboard.KEY_NEXT, ControlsJoystick.CAR_ZOOM_O, true);
		

		public final String buttonName;
		private final boolean isMomentary;
		private final int defaultButton;
		private final ControlsJoystick linkedJoystickControl;
		private int button;
		
		private ControlsKeyboard(int defaultButton, ControlsJoystick linkedJoystickControl, boolean momentary){
			this.buttonName="input." + this.name().toLowerCase().substring(0, this.name().indexOf('_')) + "." + this.name().toLowerCase().substring(this.name().indexOf('_') + 1);
			this.defaultButton=defaultButton;
			this.linkedJoystickControl=linkedJoystickControl;
			this.isMomentary=momentary;
		}
		
		public String getCurrentButton(){
			return Keyboard.getKeyName(this.button);
		}
		
		public boolean isPressed(){
			//Check to see if there is a working joystick assigned to this control.
			if(joystickMap.containsKey(this.linkedJoystickControl.joystickAssigned)){
				//Need to check to see if this axis is currently bound.
				if(this.linkedJoystickControl.joystickButton != NULL_COMPONENT){
					//Joystick control is bound and presumably functional.  If we are overriding the keyboard we must return this value.
					//Check to make sure this isn't mapped to an axis first.  If so, don't check for joystick values.
					boolean pressed = false;
					if(!this.linkedJoystickControl.isAxis){
						pressed = this.linkedJoystickControl.isPressed();
					}
					if(pressed || ConfigSystem.getBooleanConfig("KeyboardOverride")){
						return pressed;
					}
				}
			}
			return getTrueKeyboardButtonState(this, Keyboard.isKeyDown(this.button));
		}
	}
	
	public enum ControlsJoystick{
		AIRCRAFT_MOD(false, false),
		AIRCRAFT_CAMLOCK(false, true),
		AIRCRAFT_YAW(true, false),
		AIRCRAFT_PITCH(true, false),
		AIRCRAFT_ROLL(true, false),
		AIRCRAFT_THROTTLE(true, false),
		AIRCRAFT_FLAPS_U(false, true),
		AIRCRAFT_FLAPS_D(false, true),
		AIRCRAFT_BRAKE(false, false),
		AIRCRAFT_PANEL(false, true),
		AIRCRAFT_PARK(false, true),
		AIRCRAFT_ZOOM_I(false, true),
		AIRCRAFT_ZOOM_O(false, true),
		AIRCRAFT_CHANGEVIEW(false, true),
		AIRCRAFT_CHANGEHUD(false, true),
		AIRCRAFT_LOOK_L(false, false),
		AIRCRAFT_LOOK_R(false, false),
		AIRCRAFT_LOOK_U(false, false),
		AIRCRAFT_LOOK_D(false, false),
		AIRCRAFT_LOOK_A(false, false),
		AIRCRAFT_TRIM_YAW_R(false, true),
		AIRCRAFT_TRIM_YAW_L(false, true),
		AIRCRAFT_TRIM_PITCH_U(false, true),
		AIRCRAFT_TRIM_PITCH_D(false, true),
		AIRCRAFT_TRIM_ROLL_R(false, true),
		AIRCRAFT_TRIM_ROLL_L(false, true),
		
		
		CAR_MOD(false, false),
		CAR_CAMLOCK(false, true),
		CAR_TURN(true, false),
		CAR_GAS(true, false),
		CAR_BRAKE(false, false),
		CAR_SHIFT_U(false, true),
		CAR_SHIFT_D(false, true),
		CAR_HORN(false, false),
		CAR_START(false, false),
		CAR_LIGHTS(false, true),
		CAR_PARK(false, true),
		CAR_ZOOM_I(false, true),
		CAR_ZOOM_O(false, true),
		CAR_CHANGEVIEW(false, true),
		CAR_CHANGEHUD(false, true),
		CAR_LOOK_L(false, false),
		CAR_LOOK_R(false, false),
		CAR_LOOK_U(false, false),
		CAR_LOOK_D(false, false),
		CAR_LOOK_A(false, false);
		
		
		public final boolean isAxis;
		public final String buttonName;
		private final boolean isMomentary;
		private boolean joystickInverted = false;
		private int joystickButton;
		private double joystickMaxTravel;
		private double joystickMinTravel;
		private String joystickAssigned;
		
		private ControlsJoystick(boolean isAxis, boolean isMomentary){
			this.buttonName="input." + this.name().toLowerCase().substring(0, this.name().indexOf('_')) + "." + this.name().toLowerCase().substring(this.name().indexOf('_') + 1);
			this.isAxis=isAxis;
			this.isMomentary=isMomentary;
		}
		
		public String getCurrentJoystick(){
			return this.joystickAssigned;
		}
		
		public int getCurrentButton(){
			return this.joystickButton;
		}
		
		public  boolean isPressed(){
			return getTrueJoystickButtonState(this, getJoystickMultistateValue(this) > 0);
		}
	}
	
	public enum ControlsKeyboardDynamic{
		AIRCRAFT_CHANGEHUD(ControlsKeyboard.AIRCRAFT_CAMLOCK, ControlsKeyboard.AIRCRAFT_MOD),
		AIRCRAFT_PARK(ControlsKeyboard.AIRCRAFT_BRAKE, ControlsKeyboard.AIRCRAFT_MOD),
		AIRCRAFT_TRIM_YAW_R(ControlsKeyboard.AIRCRAFT_YAW_R, ControlsKeyboard.AIRCRAFT_MOD),
		AIRCRAFT_TRIM_YAW_L(ControlsKeyboard.AIRCRAFT_YAW_L, ControlsKeyboard.AIRCRAFT_MOD),
		AIRCRAFT_TRIM_PITCH_U(ControlsKeyboard.AIRCRAFT_PITCH_U, ControlsKeyboard.AIRCRAFT_MOD),
		AIRCRAFT_TRIM_PITCH_D(ControlsKeyboard.AIRCRAFT_PITCH_D, ControlsKeyboard.AIRCRAFT_MOD),
		AIRCRAFT_TRIM_ROLL_R(ControlsKeyboard.AIRCRAFT_ROLL_R, ControlsKeyboard.AIRCRAFT_MOD),
		AIRCRAFT_TRIM_ROLL_L(ControlsKeyboard.AIRCRAFT_ROLL_L, ControlsKeyboard.AIRCRAFT_MOD),
		
		CAR_CHANGEHUD(ControlsKeyboard.CAR_CAMLOCK, ControlsKeyboard.CAR_MOD),
		CAR_PARK(ControlsKeyboard.CAR_BRAKE, ControlsKeyboard.CAR_MOD),
		CAR_STOP(ControlsKeyboard.CAR_START, ControlsKeyboard.CAR_MOD);		
		
		
		public final String buttonName;
		public final ControlsKeyboard mainControl;
		public final ControlsKeyboard modControl;
		
		private ControlsKeyboardDynamic(ControlsKeyboard mainControl, ControlsKeyboard modControl){
			this.buttonName="input." + this.name().toLowerCase().substring(0, this.name().indexOf('_')) + "." + this.name().toLowerCase().substring(this.name().indexOf('_') + 1);
			this.mainControl=mainControl;
			this.modControl=modControl;
		}
		
		public boolean isPressed(){
			return this.modControl.isPressed() ? this.mainControl.isPressed() : false;
		}
	}
}
