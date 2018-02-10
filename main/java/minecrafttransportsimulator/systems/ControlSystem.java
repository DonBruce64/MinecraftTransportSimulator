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
import minecrafttransportsimulator.entities.main.EntityCar;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.guis.GUIPanelAircraft;
import minecrafttransportsimulator.packets.control.AileronPacket;
import minecrafttransportsimulator.packets.control.BrakePacket;
import minecrafttransportsimulator.packets.control.ElevatorPacket;
import minecrafttransportsimulator.packets.control.FlapPacket;
import minecrafttransportsimulator.packets.control.RudderPacket;
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
		for(Controller joystick : ControllerEnvironment.getDefaultEnvironment().getControllers()){
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
	
	public static boolean isControlMasterPressed(){
		return configKey.isPressed();
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
		//for(Controller joystick : joystickMap.values()){
			//joystick.poll();
		//}
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
	
	private static boolean isKeyboardButtonPressed(ControlsKeyboard control){
		//Check to see if there is a working joystick assigned to this control.
		if(joystickMap.containsKey(control.linkedJoystickControl.joystickAssigned)){
			//Need to check to see if this axis is currently bound.
			if(control.linkedJoystickControl.joystickButton != NULL_COMPONENT){
				//Joystick control is bound and presumably functional.  If we are overriding the keyboard we must return this value.
				//Check to make sure this isn't mapped to an axis first.  If so, return if the value is over 25.
				if(control.linkedJoystickControl.isAxis){
					return getJoystickAxisState(control.linkedJoystickControl, true) > 25;
				}
				boolean pressed = getTrueJoystickButtonState(control.linkedJoystickControl, isJoystickButtonPressed(control.linkedJoystickControl));
				if(ConfigSystem.getBooleanConfig("KeyboardOverride") && !pressed){
					return false;
				}else if(pressed){
					return true;
				}
			}
		}
		return getTrueKeyboardButtonState(control, Keyboard.isKeyDown(control.button));
	}
	
	private static boolean isJoystickButtonPressed(ControlsJoystick control){
		return getJoystickMultistateValue(control) > 0;
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
					return (short) (control.joystickInverted ? (-350*pollValue) : (350*pollValue));
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
	
	private static void controlCamera(ControlsKeyboard lock, ControlsKeyboard zoomIn, ControlsKeyboard zoomOut, ControlsKeyboard mod, ControlsJoystick changeView){
		if(isKeyboardButtonPressed(lock)){
			if(isKeyboardButtonPressed(mod)){
				if(CameraSystem.hudMode == 3){
					CameraSystem.hudMode = 0;
				}else{
					++CameraSystem.hudMode;
				}
			}else{
				CameraSystem.changeCameraLock();
			}
		}
		if(isKeyboardButtonPressed(zoomIn)){
			CameraSystem.changeCameraZoom(false);
		}
		if(isKeyboardButtonPressed(zoomOut)){
			CameraSystem.changeCameraZoom(true);
		}
		
		if(isJoystickButtonPressed(changeView)){
			if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 2){
				Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
			}else{
				++Minecraft.getMinecraft().gameSettings.thirdPersonView;
			}
		}
	}
	
	private static void rotateCamera(ControlsJoystick lookR, ControlsJoystick lookL, ControlsJoystick lookU, ControlsJoystick lookD, ControlsJoystick lookA){
		if(isJoystickButtonPressed(lookR)){
			Minecraft.getMinecraft().thePlayer.rotationYaw+=3;
		}
		if(isJoystickButtonPressed(lookL)){
			Minecraft.getMinecraft().thePlayer.rotationYaw-=3;
		}
		if(isJoystickButtonPressed(lookU)){
			Minecraft.getMinecraft().thePlayer.rotationPitch-=3;
		}
		if(isJoystickButtonPressed(lookD)){
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
	
	private static void controlBrake(ControlsKeyboard brake, ControlsKeyboard mod, ControlsJoystick pBrake, int entityID){
		if(isJoystickButtonPressed(pBrake)){
			MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 12));
			return;
		}		
		
		if(isKeyboardButtonPressed(brake)){
			if(isKeyboardButtonPressed(mod)){
				MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 12));	
			}else{
				MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 11));
			}
		}else{
			MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 2));
		}
	}
	
	private static void controlAircraft(EntityMultipartVehicle aircraft, boolean isPlayerController){
		controlCamera(ControlsKeyboard.AIRCRAFT_CAMLOCK, ControlsKeyboard.AIRCRAFT_ZOOM_I, ControlsKeyboard.AIRCRAFT_ZOOM_O, ControlsKeyboard.AIRCRAFT_MOD, ControlsJoystick.AIRCRAFT_CHANGEVIEW);
		rotateCamera(ControlsJoystick.AIRCRAFT_LOOK_R, ControlsJoystick.AIRCRAFT_LOOK_L, ControlsJoystick.AIRCRAFT_LOOK_U, ControlsJoystick.AIRCRAFT_LOOK_D, ControlsJoystick.AIRCRAFT_LOOK_A);
		if(!isPlayerController){
			return;
		}
		controlBrake(ControlsKeyboard.AIRCRAFT_BRAKE, ControlsKeyboard.AIRCRAFT_MOD, ControlsJoystick.AIRCRAFT_PARK, aircraft.getEntityId());
		
		//Open or close the panel.
		if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_PANEL)){
			if(Minecraft.getMinecraft().currentScreen == null){
				FMLCommonHandler.instance().showGuiScreen(new GUIPanelAircraft(aircraft));
			}else if(Minecraft.getMinecraft().currentScreen.getClass().equals(GUIPanelAircraft.class)){
				Minecraft.getMinecraft().displayGuiScreen((GuiScreen)null);
				Minecraft.getMinecraft().setIngameFocus();
			}
		}
		
		//Increment or decrement throttle.
		if(joystickMap.containsKey(ControlsJoystick.AIRCRAFT_THROTTLE.joystickAssigned) && ControlsJoystick.AIRCRAFT_THROTTLE.joystickButton != NULL_COMPONENT){
			MTS.MTSNet.sendToServer(new ThrottlePacket(aircraft.getEntityId(), (byte) getJoystickAxisState(ControlsJoystick.AIRCRAFT_THROTTLE, true)));
		}else{
			if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_THROTTLE_U)){
				MTS.MTSNet.sendToServer(new ThrottlePacket(aircraft.getEntityId(), Byte.MAX_VALUE));
			}
			if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_THROTTLE_D)){
				MTS.MTSNet.sendToServer(new ThrottlePacket(aircraft.getEntityId(), Byte.MIN_VALUE));
			}
		}
		
		//Check flaps.
		if(aircraft.pack.general.type.equals("plane") && aircraft.pack.plane.hasFlaps){
			if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_FLAPS_U)){
				MTS.MTSNet.sendToServer(new FlapPacket(aircraft.getEntityId(), (byte) -50));
			}
			if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_FLAPS_D)){
				MTS.MTSNet.sendToServer(new FlapPacket(aircraft.getEntityId(), (byte) 50));
			}
		}
		
		//Check yaw.
		if(joystickMap.containsKey(ControlsJoystick.AIRCRAFT_YAW.joystickAssigned) && ControlsJoystick.AIRCRAFT_YAW.joystickButton != NULL_COMPONENT){
			MTS.MTSNet.sendToServer(new RudderPacket(aircraft.getEntityId(), getJoystickAxisState(ControlsJoystick.AIRCRAFT_YAW, false)));
		}else{
			if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_YAW_R)){
				if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_MOD)){
					MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 10));
				}else{
					MTS.MTSNet.sendToServer(new RudderPacket(aircraft.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}
			if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_YAW_L)){
				if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_MOD)){
					MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 2));
				}else{
					MTS.MTSNet.sendToServer(new RudderPacket(aircraft.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}
		}
		
		//Check is mouse yoke is enabled.  If so do controls by mouse rather than buttons.
		if(ConfigSystem.getBooleanConfig("MouseYoke")){
			if(CameraSystem.lockedView && Minecraft.getMinecraft().currentScreen == null){
				int dx = Mouse.getDX();
				int dy = Mouse.getDY();
				if(Math.abs(dx) < 100){
					mousePosX = (short) Math.max(Math.min(mousePosX + dx/5F, 350), -350);
				}
				if(Math.abs(dy) < 100){
					mousePosY = (short) Math.max(Math.min(mousePosY - dy, 350), -350);
				}
				MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), mousePosX));
				MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), mousePosY));
			}
		}else{
			//Check pitch.
			if(joystickMap.containsKey(ControlsJoystick.AIRCRAFT_PITCH.joystickAssigned) && ControlsJoystick.AIRCRAFT_PITCH.joystickButton != NULL_COMPONENT){
				MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), getJoystickAxisState(ControlsJoystick.AIRCRAFT_PITCH, false)));
			}else{
				if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_PITCH_U)){
					if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_MOD)){
						MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 9));
					}else{
						MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
					}
				}
				if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_PITCH_D)){
					if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_MOD)){
						MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 1));
					}else{
						MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
					}
				}
			}
			
			//Check roll.
			if(joystickMap.containsKey(ControlsJoystick.AIRCRAFT_ROLL.joystickAssigned) && ControlsJoystick.AIRCRAFT_ROLL.joystickButton != NULL_COMPONENT){
				MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), getJoystickAxisState(ControlsJoystick.AIRCRAFT_ROLL, false)));
			}else{
				if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_ROLL_R)){
					if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_MOD)){
						MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 8));
					}else{
						MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
					}
				}
				if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_ROLL_L)){
					if(isKeyboardButtonPressed(ControlsKeyboard.AIRCRAFT_MOD)){
						MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 0));
					}else{
						MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
					}
				}
			}
		}
	}
	
	private static void controlCar(EntityMultipartVehicle car, boolean isPlayerController){
		controlCamera(ControlsKeyboard.CAR_CAMLOCK, ControlsKeyboard.CAR_ZOOM_I, ControlsKeyboard.CAR_ZOOM_O, ControlsKeyboard.CAR_MOD, ControlsJoystick.CAR_CHANGEVIEW);
		rotateCamera(ControlsJoystick.CAR_LOOK_R, ControlsJoystick.CAR_LOOK_L, ControlsJoystick.CAR_LOOK_U, ControlsJoystick.CAR_LOOK_D, ControlsJoystick.CAR_LOOK_A);
		if(!isPlayerController){
			return;
		}
		controlBrake(ControlsKeyboard.CAR_BRAKE, ControlsKeyboard.CAR_MOD, ControlsJoystick.CAR_PARK, car.getEntityId());
		
		//Change gas to on or off.
		if(joystickMap.containsKey(ControlsJoystick.CAR_GAS.joystickAssigned) && ControlsJoystick.CAR_GAS.joystickButton != NULL_COMPONENT){
			MTS.MTSNet.sendToServer(new ThrottlePacket(car.getEntityId(), (byte) getJoystickAxisState(ControlsJoystick.CAR_GAS, true)));
		}else{
			if(isKeyboardButtonPressed(ControlsKeyboard.CAR_GAS)){
				MTS.MTSNet.sendToServer(new ThrottlePacket(car.getEntityId(), (byte) 100));
			}else{
				MTS.MTSNet.sendToServer(new ThrottlePacket(car.getEntityId(), (byte) 0));
			}
		}
		
		//Check steering.
		if(joystickMap.containsKey(ControlsJoystick.CAR_TURN.joystickAssigned) && ControlsJoystick.CAR_TURN.joystickButton != NULL_COMPONENT){
			MTS.MTSNet.sendToServer(new RudderPacket(car.getEntityId(), (byte) getJoystickAxisState(ControlsJoystick.CAR_TURN, true)));
		}else{
			boolean turningRight = isKeyboardButtonPressed(ControlsKeyboard.CAR_TURN_R);
			boolean turningLeft = isKeyboardButtonPressed(ControlsKeyboard.CAR_TURN_L);
			if(turningRight && !turningLeft){
				MTS.MTSNet.sendToServer(new RudderPacket(car.getEntityId(), (short) 350));
			}else if(turningLeft && !turningRight){
				MTS.MTSNet.sendToServer(new RudderPacket(car.getEntityId(), (short) -350));
			}else if(!turningRight && !turningLeft){
				MTS.MTSNet.sendToServer(new RudderPacket(car.getEntityId(), (short) 0));
			}
		}
	}
		
	public enum ControlsKeyboard{
		AIRCRAFT_MOD(Keyboard.KEY_RSHIFT, ControlsJoystick.AIRCRAFT_MOD, false),
		AIRCRAFT_CAMLOCK(Keyboard.KEY_RCONTROL, ControlsJoystick.AIRCRAFT_CAMLOCK, true),
		AIRCRAFT_PITCH_U(Keyboard.KEY_S, ControlsJoystick.AIRCRAFT_PITCH, false),
		AIRCRAFT_PITCH_D(Keyboard.KEY_W, ControlsJoystick.AIRCRAFT_PITCH, false),
		AIRCRAFT_ROLL_R(Keyboard.KEY_D, ControlsJoystick.AIRCRAFT_ROLL, false),
		AIRCRAFT_ROLL_L(Keyboard.KEY_A, ControlsJoystick.AIRCRAFT_ROLL, false),
		AIRCRAFT_YAW_R(Keyboard.KEY_L, ControlsJoystick.AIRCRAFT_YAW, false),
		AIRCRAFT_YAW_L(Keyboard.KEY_J, ControlsJoystick.AIRCRAFT_YAW, false),
		AIRCRAFT_THROTTLE_U(Keyboard.KEY_I, ControlsJoystick.AIRCRAFT_THROTTLE, false),
		AIRCRAFT_THROTTLE_D(Keyboard.KEY_K, ControlsJoystick.AIRCRAFT_THROTTLE, false),
		AIRCRAFT_FLAPS_U(Keyboard.KEY_Y, ControlsJoystick.AIRCRAFT_FLAPS_U, true),
		AIRCRAFT_FLAPS_D(Keyboard.KEY_H, ControlsJoystick.AIRCRAFT_FLAPS_D, true),
		AIRCRAFT_BRAKE(Keyboard.KEY_B, ControlsJoystick.AIRCRAFT_BRAKE, false),
		AIRCRAFT_PANEL(Keyboard.KEY_U, ControlsJoystick.AIRCRAFT_PANEL, true),
		AIRCRAFT_ZOOM_I(Keyboard.KEY_PRIOR, ControlsJoystick.AIRCRAFT_ZOOM_I, true),
		AIRCRAFT_ZOOM_O(Keyboard.KEY_NEXT, ControlsJoystick.AIRCRAFT_ZOOM_O, true),
		
		CAR_MOD(Keyboard.KEY_RSHIFT, ControlsJoystick.CAR_MOD, true),
		CAR_CAMLOCK(Keyboard.KEY_RCONTROL, ControlsJoystick.CAR_CAMLOCK, true),
		CAR_TURN_R(Keyboard.KEY_D, ControlsJoystick.CAR_TURN, false),
		CAR_TURN_L(Keyboard.KEY_A, ControlsJoystick.CAR_TURN, false),
		CAR_GAS(Keyboard.KEY_W, ControlsJoystick.CAR_GAS, false),
		CAR_BRAKE(Keyboard.KEY_S, ControlsJoystick.AIRCRAFT_BRAKE, false),
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
	}
	
	public enum ControlsJoystick{
		AIRCRAFT_MOD(false, true),
		AIRCRAFT_CAMLOCK(false, true),
		AIRCRAFT_PITCH(true, false),
		AIRCRAFT_ROLL(true, false),
		AIRCRAFT_YAW(true, false),
		AIRCRAFT_THROTTLE(true, false),
		AIRCRAFT_FLAPS_U(false, true),
		AIRCRAFT_FLAPS_D(false, true),
		AIRCRAFT_BRAKE(false, false),
		AIRCRAFT_PANEL(false, true),
		AIRCRAFT_ZOOM_I(false, true),
		AIRCRAFT_ZOOM_O(false, true),
		AIRCRAFT_PARK(false, true),
		AIRCRAFT_CHANGEVIEW(false, true),
		AIRCRAFT_LOOK_L(false, false),
		AIRCRAFT_LOOK_R(false, false),
		AIRCRAFT_LOOK_U(false, false),
		AIRCRAFT_LOOK_D(false, false),
		AIRCRAFT_LOOK_A(false, false),
		
		CAR_MOD(false, true),
		CAR_CAMLOCK(false, true),
		CAR_TURN(true, false),
		CAR_GAS(true, false),
		CAR_BRAKE(true, false),
		CAR_HORN(false, false),
		CAR_START(false, false),
		CAR_LIGHTS(false, true),
		CAR_ZOOM_I(false, true),
		CAR_ZOOM_O(false, true),
		CAR_PARK(false, true),
		CAR_CHANGEVIEW(false, true),
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
	}
}
