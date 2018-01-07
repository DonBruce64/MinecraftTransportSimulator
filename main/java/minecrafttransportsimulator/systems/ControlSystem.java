package minecrafttransportsimulator.systems;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import minecrafttransportsimulator.ClientProxy;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
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
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**Class that handles all control operations.
 * Keybinding lists are initiated during the {@link ClientProxy} init method.
 * 
 * @author don_bruce
 */
public final class ControlSystem{
	//Internal checkers for keys.
	private static boolean brakeKeyPressed;
	private static boolean flapKeyPressed;
	private static boolean panelKeyPressed;
	private static boolean camLockKeyPressed;
	private static boolean changeViewKeyPressed;
	private static boolean zoomInKeyPressed;
	private static boolean zoomOutKeyPressed;
	
	//Constants for actions.
	private static final int NULL_COMPONENT = 999;
	private static final String KEYBOARD_CONFIG = "keyboard";
	private static final String JOYSTICK_CONFIG = "joystick";

	//Configurations and data arrays
	public static KeyBinding configKey;
	private static String joystickName;
	private static Controller joystick;
	private static short mousePosX = 0;
	private static short mousePosY = 0;
	private static Map<String, Integer> keyboardMap = new HashMap<String, Integer>();
	private static Map<String, Integer> joystickMap = new HashMap<String, Integer>();
	
	public static void init(){
		configKey = new KeyBinding("key.config", Keyboard.KEY_P, "key.categories." + MTS.MODID);
		ClientRegistry.registerKeyBinding(configKey);
		
		keyboardMap.put(Controls.MOD.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.MOD.keyboardName, Keyboard.KEY_RSHIFT).getInt());
		keyboardMap.put(Controls.CAM.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.CAM.keyboardName, Keyboard.KEY_RCONTROL).getInt());
		keyboardMap.put(Controls.PITCH.keyboardIncrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.PITCH.keyboardIncrementName, Keyboard.KEY_S).getInt());
		keyboardMap.put(Controls.PITCH.keyboardDecrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.PITCH.keyboardDecrementName, Keyboard.KEY_W).getInt());
		keyboardMap.put(Controls.ROLL.keyboardIncrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.ROLL.keyboardIncrementName, Keyboard.KEY_D).getInt());
		keyboardMap.put(Controls.ROLL.keyboardDecrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.ROLL.keyboardDecrementName, Keyboard.KEY_A).getInt());
		keyboardMap.put(Controls.YAW.keyboardIncrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.YAW.keyboardIncrementName, Keyboard.KEY_L).getInt());
		keyboardMap.put(Controls.YAW.keyboardDecrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.YAW.keyboardDecrementName, Keyboard.KEY_J).getInt());
		keyboardMap.put(Controls.THROTTLE.keyboardIncrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.THROTTLE.keyboardIncrementName, Keyboard.KEY_I).getInt());
		keyboardMap.put(Controls.THROTTLE.keyboardDecrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.THROTTLE.keyboardDecrementName, Keyboard.KEY_K).getInt());
		keyboardMap.put(Controls.FLAPS_U.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.FLAPS_U.keyboardName, Keyboard.KEY_Y).getInt());
		keyboardMap.put(Controls.FLAPS_D.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.FLAPS_D.keyboardName, Keyboard.KEY_H).getInt());
		keyboardMap.put(Controls.BRAKE.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.BRAKE.keyboardName, Keyboard.KEY_B).getInt());
		keyboardMap.put(Controls.PANEL.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.PANEL.keyboardName, Keyboard.KEY_U).getInt());
		keyboardMap.put(Controls.ZOOM_I.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.ZOOM_I.keyboardName, Keyboard.KEY_PRIOR).getInt());
		keyboardMap.put(Controls.ZOOM_O.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, Controls.ZOOM_O.keyboardName, Keyboard.KEY_NEXT).getInt());
		
		joystickName = ConfigSystem.config.get(JOYSTICK_CONFIG, "JoystickName", "").getString();
		for(Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()){
			if(controller.getName() != null){
				if(controller.getName().equals(joystickName)){
					joystick = controller;
					break;
				}
			}
		}
		
		joystickMap.put(Controls.MOD.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.MOD.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.CAM.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.CAM.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.PITCH.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.PITCH.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.ROLL.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.ROLL.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.YAW.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.YAW.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.THROTTLE.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.THROTTLE.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.FLAPS_U.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.FLAPS_U.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.FLAPS_D.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.FLAPS_D.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.BRAKE.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.BRAKE.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.PANEL.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.PANEL.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.ZOOM_I.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.ZOOM_I.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.ZOOM_O.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.ZOOM_O.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.CHANGEVIEW.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.CHANGEVIEW.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.LOOK_L.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.LOOK_L.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.LOOK_R.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.LOOK_R.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.LOOK_U.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.LOOK_U.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.LOOK_D.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.LOOK_D.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(Controls.LOOK_ALL.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.LOOK_ALL.joystickName, NULL_COMPONENT).getInt());
		
		Controls.PITCH.joystickMaxTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.PITCH.joystickName+"maxtravel", 1D).getDouble();
		Controls.PITCH.joystickMinTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.PITCH.joystickName+"mintravel", -1D).getDouble();
		Controls.PITCH.joystickInverted = ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.PITCH.joystickName+"inverted", false).getBoolean();
		Controls.ROLL.joystickMaxTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.ROLL.joystickName+"maxtravel", 1D).getDouble();
		Controls.ROLL.joystickMinTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.ROLL.joystickName+"mintravel", -1D).getDouble();
		Controls.ROLL.joystickInverted = ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.ROLL.joystickName+"inverted", false).getBoolean();
		Controls.YAW.joystickMaxTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.YAW.joystickName+"maxtravel", 1D).getDouble();
		Controls.YAW.joystickMinTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.YAW.joystickName+"mintravel", -1D).getDouble();
		Controls.YAW.joystickInverted = ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.YAW.joystickName+"inverted", false).getBoolean();
		Controls.THROTTLE.joystickMaxTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.THROTTLE.joystickName+"maxtravel", 1D).getDouble();
		Controls.THROTTLE.joystickMinTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.THROTTLE.joystickName+"mintravel", -1D).getDouble();
		Controls.THROTTLE.joystickInverted = ConfigSystem.config.get(JOYSTICK_CONFIG, Controls.THROTTLE.joystickName+"inverted", false).getBoolean();
	}	
	
	public static String getKeyboardKeyname(String keyname){
		return Keyboard.getKeyName(keyboardMap.get(keyname));
	}
	
	public static String getJoystickControlName(int componentId){
		for(Entry<String, Integer> entry : joystickMap.entrySet()){
			if(entry.getValue().equals(componentId)){
				return entry.getKey();
			}
		}
		return "";
	}
	
	public static void setKeyboardKey(String keyname, int bytecode){
		keyboardMap.put(keyname, bytecode);
		ConfigSystem.config.getCategory(KEYBOARD_CONFIG).put(keyname, new Property(keyname, String.valueOf(bytecode), Property.Type.INTEGER));
		ConfigSystem.config.save();
	}

	public static void setJoystickControl(String keyname, int componentId){
		String currentMapping = "";
		for(Entry<String, Integer> entry : joystickMap.entrySet()){
			if(entry.getValue().equals(componentId)){
				currentMapping = entry.getKey();
			}
		}
		if(joystickMap.containsKey(currentMapping)){
			joystickMap.put(currentMapping, NULL_COMPONENT);
			ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put(currentMapping, new Property(currentMapping, String.valueOf(NULL_COMPONENT), Property.Type.INTEGER));
		}			
		joystickMap.put(keyname, componentId);
		ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put(keyname, new Property(keyname, String.valueOf(componentId), Property.Type.INTEGER));
		ConfigSystem.config.save();
	}
	
	public static void setAxisBounds(String axisName, double minBound, double maxBound, boolean inverted){
		for(ControlSystem.Controls control : ControlSystem.Controls.values()){
			if(control.joystickName.equals(axisName)){
				control.joystickMinTravel = minBound;
				control.joystickMaxTravel = maxBound;
				control.joystickInverted = inverted;
				ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put(control.joystickName+"maxtravel", new Property(control.joystickName+"maxtravel", String.valueOf(control.joystickMaxTravel), Property.Type.DOUBLE));
				ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put(control.joystickName+"mintravel", new Property(control.joystickName+"mintravel", String.valueOf(control.joystickMinTravel), Property.Type.DOUBLE));
				ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put(control.joystickName+"inverted", new Property(control.joystickName+"inverted", String.valueOf(control.joystickInverted), Property.Type.BOOLEAN));
			}
		}
	}
	
	public static void setJoystick(Controller controller){
		if(controller.getName() != null){
			if(!controller.getName().equals(joystickName)){
				ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put("JoystickName", new Property("JoystickName", controller.getName(), Property.Type.STRING));
				for(String joystickControl : joystickMap.keySet()){
					setJoystickControl(joystickControl, NULL_COMPONENT);
				}
				joystickName = controller.getName();
			}
		}
		joystick = controller;
	}
	
	public static Controller getJoystick(){
		return joystick;
	}
	
	public static int getNullComponent(){
		return NULL_COMPONENT;
	}
	
	private static boolean isControlPressed(ControlSystem.Controls control){
		if(!joystickMap.get(control.joystickName).equals(NULL_COMPONENT) && joystick != null){
			if(joystick.getComponents()[joystickMap.get(control.joystickName)].getPollData() > 0){
				return true;
			}else if(ConfigSystem.getBooleanConfig("KeyboardOverride")){
				return false;
			}
		}
		if(keyboardMap.containsKey(control.keyboardName)){
			return Keyboard.isKeyDown(keyboardMap.get(control.keyboardName));
		}else{
			return false;			
		}
	}
	
	private static short getAxisState(ControlSystem.Controls control, boolean controlSurface){
		float pollValue = joystick.getComponents()[joystickMap.get(control.joystickName)].getPollData();
		if(!controlSurface || (Math.abs(pollValue) > joystick.getComponents()[joystickMap.get(control.joystickName)].getDeadZone() && Math.abs(pollValue) > ConfigSystem.getDoubleConfig("JoystickDeadZone"))){
			//Normalize from -1 to 1 based on the min-max travel settings.
			pollValue = (float) (((pollValue - control.joystickMinTravel)/(control.joystickMaxTravel - control.joystickMinTravel) - 0.5)*2F);
			//If axis is inverted, invert poll.
			if(control.joystickInverted){
				pollValue *= -1;
			}
			if(controlSurface){
				return (short) (Math.signum(pollValue)*350*Math.pow(2, ConfigSystem.getIntegerConfig("JoystickForceFactor")*Math.abs(pollValue) - ConfigSystem.getIntegerConfig("JoystickForceFactor")));
			}else{
				return (short) (100*(pollValue + 1F)/2F);
			}
		}else{
			return 0;
		}
	}
	
	public static void controlCamera(){
		if(isControlPressed(Controls.CAM)){
			if(!camLockKeyPressed){
				if(!isControlPressed(Controls.MOD)){
					camLockKeyPressed=true;
					CameraSystem.changeCameraLock();
				}
			}
		}else{
			camLockKeyPressed=false;
		}
		
		if(isControlPressed(Controls.ZOOM_I)){
			if(!zoomInKeyPressed){
				zoomInKeyPressed=true;
				CameraSystem.changeCameraZoom(false);
			}
		}else{
			zoomInKeyPressed=false;
		}
		if(isControlPressed(Controls.ZOOM_O)){
			if(!zoomOutKeyPressed){
				zoomOutKeyPressed=true;
				CameraSystem.changeCameraZoom(true);
			}
		}else{
			zoomOutKeyPressed=false;
		}
		if(isControlPressed(Controls.CHANGEVIEW)){
			if(!changeViewKeyPressed){
				changeViewKeyPressed = true;
				if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 2){
					Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
				}else{
					++Minecraft.getMinecraft().gameSettings.thirdPersonView;
				}
			}
		}else{
			changeViewKeyPressed = false;
		}
		if(isControlPressed(Controls.LOOK_R)){
			Minecraft.getMinecraft().thePlayer.rotationYaw+=2;
		}
		if(isControlPressed(Controls.LOOK_L)){
			Minecraft.getMinecraft().thePlayer.rotationYaw-=2;
		}
		if(isControlPressed(Controls.LOOK_D)){
			Minecraft.getMinecraft().thePlayer.rotationPitch+=2;
		}
		if(isControlPressed(Controls.LOOK_U)){
			Minecraft.getMinecraft().thePlayer.rotationPitch-=2;
		}
		if(isControlPressed(Controls.LOOK_ALL)){
			float pollData = joystick.getComponents()[joystickMap.get(Controls.LOOK_ALL.joystickName)].getPollData();
			if(pollData >= 0.125F && pollData <= 0.375F){
				Minecraft.getMinecraft().thePlayer.rotationPitch+=2;
			}
			if(pollData >= 0.375F && pollData <= 0.625F){
				Minecraft.getMinecraft().thePlayer.rotationYaw+=2;
			}
			if(pollData >= 0.625F && pollData <= 0.875F){
				Minecraft.getMinecraft().thePlayer.rotationPitch-=2;
			}
			if(pollData >= 0.875F || pollData <= 0.125F){
				Minecraft.getMinecraft().thePlayer.rotationYaw-=2;
			}
		}
	}
	
	public static void controlVehicle(EntityMultipartVehicle vehicle, EntityPlayer controller){
		if(joystick!=null){
			if(!joystick.poll()){
				joystick = null;
			}
		}
		
		checkHUD();
		checkBrakes(vehicle);
		checkThrottle(vehicle);
		if(vehicle instanceof EntityPlane){
			checkPlaneControls((EntityPlane) vehicle);
		}
		checkPanel(vehicle, controller);
		
		
	}
	
	private static void checkHUD(){
		if(isControlPressed(Controls.CAM)){
			if(!camLockKeyPressed){
				if(isControlPressed(Controls.MOD)){
					camLockKeyPressed=true;
					if(CameraSystem.hudMode == 3){
						CameraSystem.hudMode = 0;
					}else{
						++CameraSystem.hudMode;
					} 
				}
			}
		}
	}
	
	private static void checkThrottle(EntityMultipartVehicle vehicle){
		if(joystickMap.get(Controls.THROTTLE.joystickName) != 999 && joystick != null){
			MTS.MTSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), (byte) getAxisState(Controls.THROTTLE, false)));
		}else if(Keyboard.isKeyDown(keyboardMap.get(Controls.THROTTLE.keyboardIncrementName))){
			MTS.MTSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), Byte.MAX_VALUE));
		}else if(Keyboard.isKeyDown(keyboardMap.get(Controls.THROTTLE.keyboardDecrementName))){
			MTS.MTSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), Byte.MIN_VALUE));
		}
	}
	
	private static void checkBrakes(EntityMultipartVehicle vehicle){
		if(isControlPressed(Controls.BRAKE)){
			if(!brakeKeyPressed){
				brakeKeyPressed = true;
				if(isControlPressed(Controls.MOD)){
					MTS.MTSNet.sendToServer(new BrakePacket(vehicle.getEntityId(), (byte) 12));
				}else{
					MTS.MTSNet.sendToServer(new BrakePacket(vehicle.getEntityId(), (byte) 11));
				}
			}
		}else if(brakeKeyPressed){
			brakeKeyPressed = false;
			MTS.MTSNet.sendToServer(new BrakePacket(vehicle.getEntityId(), (byte) 2));
		}
	}
	
	private static void checkPanel(EntityMultipartVehicle vehicle, EntityPlayer controller){
		if(isControlPressed(Controls.PANEL)){
			if(!panelKeyPressed){
				panelKeyPressed = true;
				if(Minecraft.getMinecraft().currentScreen == null){
					FMLCommonHandler.instance().showGuiScreen(vehicle.getPanel());
				}else if(Minecraft.getMinecraft().currentScreen.getClass().equals(GUIPanelAircraft.class)){
					Minecraft.getMinecraft().displayGuiScreen((GuiScreen)null);
					Minecraft.getMinecraft().setIngameFocus();
				}
			}
		}else{
			panelKeyPressed = false;
		}
	}
	
	private static void checkPlaneControls(EntityPlane plane){
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
				MTS.MTSNet.sendToServer(new AileronPacket(plane.getEntityId(), mousePosX));
				MTS.MTSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), mousePosY));
			}
		}else{
			if(joystickMap.get(Controls.ROLL.joystickName) != 999 && joystick != null){
				MTS.MTSNet.sendToServer(new AileronPacket(plane.getEntityId(), getAxisState(Controls.ROLL, true)));
			}else if(Keyboard.isKeyDown(keyboardMap.get(Controls.ROLL.keyboardIncrementName))){
				if(isControlPressed(Controls.MOD)){
					MTS.MTSNet.sendToServer(new TrimPacket(plane.getEntityId(), (byte) 8));
				}else{
					MTS.MTSNet.sendToServer(new AileronPacket(plane.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}else if(Keyboard.isKeyDown(keyboardMap.get(Controls.ROLL.keyboardDecrementName))){
				if(isControlPressed(Controls.MOD)){
					MTS.MTSNet.sendToServer(new TrimPacket(plane.getEntityId(), (byte) 0));
				}else{
					MTS.MTSNet.sendToServer(new AileronPacket(plane.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}
			
			if(joystickMap.get(Controls.PITCH.joystickName) != 999 && joystick != null){
				MTS.MTSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), getAxisState(Controls.PITCH, true)));
			}else if(Keyboard.isKeyDown(keyboardMap.get(Controls.PITCH.keyboardIncrementName))){
				if(isControlPressed(Controls.MOD)){
					MTS.MTSNet.sendToServer(new TrimPacket(plane.getEntityId(), (byte) 9));
				}else{
					MTS.MTSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}else if(Keyboard.isKeyDown(keyboardMap.get(Controls.PITCH.keyboardDecrementName))){
				if(isControlPressed(Controls.MOD)){
					MTS.MTSNet.sendToServer(new TrimPacket(plane.getEntityId(), (byte) 1));
				}else{
					MTS.MTSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}
		}
		
		if(joystickMap.get(Controls.YAW.joystickName) != 999 && joystick != null){
			MTS.MTSNet.sendToServer(new RudderPacket(plane.getEntityId(), getAxisState(Controls.YAW, true)));
		}else if(Keyboard.isKeyDown(keyboardMap.get(Controls.YAW.keyboardIncrementName))){
			if(isControlPressed(Controls.MOD)){
				MTS.MTSNet.sendToServer(new TrimPacket(plane.getEntityId(), (byte) 10));
			}else{
				MTS.MTSNet.sendToServer(new RudderPacket(plane.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
			}
		}else if(Keyboard.isKeyDown(keyboardMap.get(Controls.YAW.keyboardDecrementName))){
			if(isControlPressed(Controls.MOD)){
				MTS.MTSNet.sendToServer(new TrimPacket(plane.getEntityId(), (byte) 2));
			}else{
				MTS.MTSNet.sendToServer(new RudderPacket(plane.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
			}
		}
		
		if(plane.pack.plane.hasFlaps){
			if(isControlPressed(Controls.FLAPS_U)){
				if(!flapKeyPressed){
					MTS.MTSNet.sendToServer(new FlapPacket(plane.getEntityId(), (byte) -50));
					flapKeyPressed = true;
				}
			}else if(isControlPressed(Controls.FLAPS_D)){
				if(!flapKeyPressed){
					MTS.MTSNet.sendToServer(new FlapPacket(plane.getEntityId(), (byte) 50));
					flapKeyPressed = true;
				}
			}else{
				flapKeyPressed = false;
			}
		}
	}
	
	public enum Controls{
		MOD(I18n.format("input.key.mod"), false),
		CAM(I18n.format("input.key.camlock"), false),
		PITCH(I18n.format("input.joystick.pitch"), I18n.format("input.key.pitchu"), I18n.format("input.key.pitchd")),
		ROLL(I18n.format("input.joystick.roll"), I18n.format("input.key.rollr"), I18n.format("input.key.rolll")),
		YAW(I18n.format("input.joystick.yaw"), I18n.format("input.key.yawr"), I18n.format("input.key.yawl")),
		THROTTLE(I18n.format("input.joystick.throttle"), I18n.format("input.key.throttleu"), I18n.format("input.key.throttled")),
		FLAPS_U(I18n.format("input.key.flapsu"), false),
		FLAPS_D(I18n.format("input.key.flapsd"), false),
		BRAKE(I18n.format("input.key.brake"), false),
		PANEL(I18n.format("input.key.panel"), false),
		ZOOM_I(I18n.format("input.key.zoomi"), false),
		ZOOM_O(I18n.format("input.key.zoomo"), false),
		CHANGEVIEW(I18n.format("input.joystick.changeview"), true),
		LOOK_L(I18n.format("input.joystick.lookl"), true),
		LOOK_R(I18n.format("input.joystick.lookr"), true),
		LOOK_U(I18n.format("input.joystick.looku"), true),
		LOOK_D(I18n.format("input.joystick.lookd"), true),
		LOOK_ALL(I18n.format("input.joystick.looka"), true)
		;

		public final String keyboardName;
		public final String keyboardIncrementName;
		public final String keyboardDecrementName;
		public final String joystickName;
		public double joystickMaxTravel = 1;
		public double joystickMinTravel = -1;
		public boolean joystickInverted = false;
		
		private Controls(String name, boolean joystickOnly){
			this(name, joystickOnly ? "" : name, joystickOnly ? "" : name);
		}
		private Controls(String joystick, String keyboardIncrement, String keyboardDecrement){
			this.joystickName = joystick;
			this.keyboardName = keyboardIncrement;
			this.keyboardIncrementName = keyboardIncrement;
			this.keyboardDecrementName = keyboardDecrement;
		}
	}
}
