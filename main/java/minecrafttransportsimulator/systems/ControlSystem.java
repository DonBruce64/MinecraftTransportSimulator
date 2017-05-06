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
import minecrafttransportsimulator.guis.GUIPanelPlane;
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
		
		keyboardMap.put(controls.MOD.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.MOD.keyboardName, Keyboard.KEY_RSHIFT).getInt());
		keyboardMap.put(controls.CAM.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.CAM.keyboardName, Keyboard.KEY_RCONTROL).getInt());
		keyboardMap.put(controls.PITCH.keyboardIncrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.PITCH.keyboardIncrementName, Keyboard.KEY_S).getInt());
		keyboardMap.put(controls.PITCH.keyboardDecrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.PITCH.keyboardDecrementName, Keyboard.KEY_W).getInt());
		keyboardMap.put(controls.ROLL.keyboardIncrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.ROLL.keyboardIncrementName, Keyboard.KEY_D).getInt());
		keyboardMap.put(controls.ROLL.keyboardDecrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.ROLL.keyboardDecrementName, Keyboard.KEY_A).getInt());
		keyboardMap.put(controls.YAW.keyboardIncrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.YAW.keyboardIncrementName, Keyboard.KEY_L).getInt());
		keyboardMap.put(controls.YAW.keyboardDecrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.YAW.keyboardDecrementName, Keyboard.KEY_J).getInt());
		keyboardMap.put(controls.THROTTLE.keyboardIncrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.THROTTLE.keyboardIncrementName, Keyboard.KEY_I).getInt());
		keyboardMap.put(controls.THROTTLE.keyboardDecrementName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.THROTTLE.keyboardDecrementName, Keyboard.KEY_K).getInt());
		keyboardMap.put(controls.FLAPS_U.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.FLAPS_U.keyboardName, Keyboard.KEY_Y).getInt());
		keyboardMap.put(controls.FLAPS_D.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.FLAPS_D.keyboardName, Keyboard.KEY_H).getInt());
		keyboardMap.put(controls.BRAKE.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.BRAKE.keyboardName, Keyboard.KEY_B).getInt());
		keyboardMap.put(controls.PANEL.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.PANEL.keyboardName, Keyboard.KEY_U).getInt());
		keyboardMap.put(controls.ZOOM_I.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.ZOOM_I.keyboardName, Keyboard.KEY_PRIOR).getInt());
		keyboardMap.put(controls.ZOOM_O.keyboardName, ConfigSystem.config.get(KEYBOARD_CONFIG, controls.ZOOM_O.keyboardName, Keyboard.KEY_NEXT).getInt());
		
		joystickName = ConfigSystem.config.get(JOYSTICK_CONFIG, "JoystickName", "").getString();
		for(Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()){
			if(controller.getName() != null){
				if(controller.getName().equals(joystickName)){
					joystick = controller;
					break;
				}
			}
		}
		
		joystickMap.put(controls.MOD.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.MOD.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.CAM.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.CAM.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.PITCH.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.PITCH.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.ROLL.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.ROLL.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.YAW.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.YAW.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.THROTTLE.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.THROTTLE.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.FLAPS_U.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.FLAPS_U.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.FLAPS_D.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.FLAPS_D.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.BRAKE.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.BRAKE.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.PANEL.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.PANEL.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.ZOOM_I.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.ZOOM_I.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.ZOOM_O.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.ZOOM_O.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.CHANGEVIEW.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.CHANGEVIEW.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.LOOK_L.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.LOOK_L.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.LOOK_R.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.LOOK_R.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.LOOK_U.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.LOOK_U.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.LOOK_D.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.LOOK_D.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.LOOK_ALL.joystickName, ConfigSystem.config.get(JOYSTICK_CONFIG, controls.LOOK_ALL.joystickName, NULL_COMPONENT).getInt());
		
		controls.PITCH.joystickMaxTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, controls.PITCH.joystickName+"maxtravel", 1D).getDouble();
		controls.PITCH.joystickMinTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, controls.PITCH.joystickName+"mintravel", -1D).getDouble();
		controls.ROLL.joystickMaxTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, controls.ROLL.joystickName+"maxtravel", 1D).getDouble();
		controls.ROLL.joystickMinTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, controls.ROLL.joystickName+"mintravel", -1D).getDouble();
		controls.YAW.joystickMaxTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, controls.YAW.joystickName+"maxtravel", 1D).getDouble();
		controls.YAW.joystickMinTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, controls.YAW.joystickName+"mintravel", -1D).getDouble();
		controls.THROTTLE.joystickMaxTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, controls.THROTTLE.joystickName+"maxtravel", 1D).getDouble();
		controls.THROTTLE.joystickMinTravel = ConfigSystem.config.get(JOYSTICK_CONFIG, controls.THROTTLE.joystickName+"mintravel", -1D).getDouble();
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
	
	public static void setAxisBounds(String axisName, double minBound, double maxBound){
		for(ControlSystem.controls control : ControlSystem.controls.values()){
			if(control.joystickName.equals(axisName)){
				control.joystickMinTravel = minBound;
				control.joystickMaxTravel = maxBound;
				ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put(control.joystickName+"maxtravel", new Property(control.joystickName+"maxtravel", String.valueOf(control.joystickMaxTravel), Property.Type.DOUBLE));
				ConfigSystem.config.getCategory(JOYSTICK_CONFIG).put(control.joystickName+"mintravel", new Property(control.joystickName+"mintravel", String.valueOf(control.joystickMinTravel), Property.Type.DOUBLE));
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
	
	private static boolean isControlPressed(ControlSystem.controls control){
		if(!joystickMap.get(control.joystickName).equals(NULL_COMPONENT) && joystick != null){
			return joystick.getComponents()[joystickMap.get(control.joystickName)].getPollData() > 0;
		}
		if(keyboardMap.containsKey(control.keyboardName)){
			return Keyboard.isKeyDown(keyboardMap.get(control.keyboardName));
		}
		return false;
	}
	
	private static short getAxisState(ControlSystem.controls control, boolean controlSurface){
		float pollValue = joystick.getComponents()[joystickMap.get(control.joystickName)].getPollData();
		if(Math.abs(pollValue) > joystick.getComponents()[joystickMap.get(control.joystickName)].getDeadZone() && Math.abs(pollValue) > ConfigSystem.getDoubleConfig("JoystickDeadZone")){
			if(pollValue < 0){
				if(controlSurface){
					return (short) (-350*Math.pow(2, ConfigSystem.getIntegerConfig("JoystickForceFactor")*pollValue/control.joystickMinTravel - ConfigSystem.getIntegerConfig("JoystickForceFactor")));
				}else{
					return (short) (-100*pollValue/control.joystickMinTravel);
				}
			}else{
				if(controlSurface){
					return (short) (350*Math.pow(2, ConfigSystem.getIntegerConfig("JoystickForceFactor")*pollValue/control.joystickMaxTravel - ConfigSystem.getIntegerConfig("JoystickForceFactor")));
				}else{
					return (short) (100*pollValue/control.joystickMaxTravel);
				}
			}
		}else{
			return 0;
		}
	}
	
	public static void controlCamera(){
		if(isControlPressed(controls.CAM)){
			if(!camLockKeyPressed){
				if(!isControlPressed(controls.MOD)){
					camLockKeyPressed=true;
					CameraSystem.changeCameraLock();
				}
			}
		}else{
			camLockKeyPressed=false;
		}
		
		if(isControlPressed(controls.ZOOM_I)){
			if(!zoomInKeyPressed){
				zoomInKeyPressed=true;
				CameraSystem.changeCameraZoom(false);
			}
		}else{
			zoomInKeyPressed=false;
		}
		if(isControlPressed(controls.ZOOM_O)){
			if(!zoomOutKeyPressed){
				zoomOutKeyPressed=true;
				CameraSystem.changeCameraZoom(true);
			}
		}else{
			zoomOutKeyPressed=false;
		}
		if(isControlPressed(controls.CHANGEVIEW)){
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
		if(isControlPressed(controls.LOOK_R)){
			Minecraft.getMinecraft().thePlayer.rotationYaw+=2;
		}
		if(isControlPressed(controls.LOOK_L)){
			Minecraft.getMinecraft().thePlayer.rotationYaw-=2;
		}
		if(isControlPressed(controls.LOOK_D)){
			Minecraft.getMinecraft().thePlayer.rotationPitch+=2;
		}
		if(isControlPressed(controls.LOOK_U)){
			Minecraft.getMinecraft().thePlayer.rotationPitch-=2;
		}
		if(isControlPressed(controls.LOOK_ALL)){
			float pollData = joystick.getComponents()[joystickMap.get(controls.LOOK_ALL.joystickName)].getPollData();
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
	
	public static void controlPlane(EntityPlane plane, EntityPlayer controller){
		if(joystick!=null){
			if(!joystick.poll()){
				joystick = null;
			}
		}
		
		checkHUD();
		checkBrakes(plane);
		checkThrottle(plane);
		if(plane.hasFlaps){checkFlaps(plane);}
		checkPanel(plane, controller);
		
		if(ConfigSystem.getBooleanConfig("MouseYoke")){
			if(CameraSystem.lockedView){
				int dx = Mouse.getDX();
				int dy = Mouse.getDY();
				if(Math.abs(dx) < 100){
					mousePosX = (short) Math.max(Math.min(mousePosX + dx/5F, 350), -350);
				}
				if(Math.abs(dy) < 100){
					mousePosY = (short) Math.max(Math.min(mousePosY - dy, 350), -350);
				}
				MTS.MFSNet.sendToServer(new AileronPacket(plane.getEntityId(), mousePosX));
				MTS.MFSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), mousePosY));
			}
		}else{
			if(joystickMap.get(controls.ROLL.joystickName) != 999 && joystick != null){
				MTS.MFSNet.sendToServer(new AileronPacket(plane.getEntityId(), getAxisState(controls.ROLL, true)));
			}else if(Keyboard.isKeyDown(keyboardMap.get(controls.ROLL.keyboardIncrementName))){
				if(isControlPressed(controls.MOD)){
					MTS.MFSNet.sendToServer(new TrimPacket(plane.getEntityId(), (byte) 8));
				}else{
					MTS.MFSNet.sendToServer(new AileronPacket(plane.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}else if(Keyboard.isKeyDown(keyboardMap.get(controls.ROLL.keyboardDecrementName))){
				if(isControlPressed(controls.MOD)){
					MTS.MFSNet.sendToServer(new TrimPacket(plane.getEntityId(), (byte) 0));
				}else{
					MTS.MFSNet.sendToServer(new AileronPacket(plane.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}
			
			if(joystickMap.get(controls.PITCH.joystickName) != 999 && joystick != null){
				MTS.MFSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), getAxisState(controls.PITCH, true)));
			}else if(Keyboard.isKeyDown(keyboardMap.get(controls.PITCH.keyboardIncrementName))){
				if(isControlPressed(controls.MOD)){
					MTS.MFSNet.sendToServer(new TrimPacket(plane.getEntityId(), (byte) 9));
				}else{
					MTS.MFSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}else if(Keyboard.isKeyDown(keyboardMap.get(controls.PITCH.keyboardDecrementName))){
				if(isControlPressed(controls.MOD)){
					MTS.MFSNet.sendToServer(new TrimPacket(plane.getEntityId(), (byte) 1));
				}else{
					MTS.MFSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}
		}
		
		if(joystickMap.get(controls.YAW.joystickName) != 999 && joystick != null){
			MTS.MFSNet.sendToServer(new RudderPacket(plane.getEntityId(), getAxisState(controls.YAW, true)));
		}else if(Keyboard.isKeyDown(keyboardMap.get(controls.YAW.keyboardIncrementName))){
			if(isControlPressed(controls.MOD)){
				MTS.MFSNet.sendToServer(new TrimPacket(plane.getEntityId(), (byte) 10));
			}else{
				MTS.MFSNet.sendToServer(new RudderPacket(plane.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
			}
		}else if(Keyboard.isKeyDown(keyboardMap.get(controls.YAW.keyboardDecrementName))){
			if(isControlPressed(controls.MOD)){
				MTS.MFSNet.sendToServer(new TrimPacket(plane.getEntityId(), (byte) 2));
			}else{
				MTS.MFSNet.sendToServer(new RudderPacket(plane.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
			}
		}
	}
	
	private static void checkHUD(){
		//TODO make this better for cars.
		if(isControlPressed(controls.CAM)){
			if(!camLockKeyPressed){
				if(isControlPressed(controls.MOD)){
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
		if(joystickMap.get(controls.THROTTLE.joystickName) != 999 && joystick != null){
			MTS.MFSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), (byte) (50 + getAxisState(controls.THROTTLE, false)/2)));
		}else if(Keyboard.isKeyDown(keyboardMap.get(controls.THROTTLE.keyboardIncrementName))){
			MTS.MFSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), Byte.MAX_VALUE));
		}else if(Keyboard.isKeyDown(keyboardMap.get(controls.THROTTLE.keyboardDecrementName))){
			MTS.MFSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), Byte.MIN_VALUE));
		}
	}
	
	private static void checkBrakes(EntityMultipartVehicle vehicle){
		if(isControlPressed(controls.BRAKE)){
			if(!brakeKeyPressed){
				brakeKeyPressed = true;
				if(isControlPressed(controls.MOD)){
					MTS.MFSNet.sendToServer(new BrakePacket(vehicle.getEntityId(), (byte) 12));
				}else{
					MTS.MFSNet.sendToServer(new BrakePacket(vehicle.getEntityId(), (byte) 11));
				}
			}
		}else if(brakeKeyPressed){
			brakeKeyPressed = false;
			MTS.MFSNet.sendToServer(new BrakePacket(vehicle.getEntityId(), (byte) 2));
		}
	}
	
	private static void checkFlaps(EntityPlane plane){
		if(isControlPressed(controls.FLAPS_U)){
			if(!flapKeyPressed){
				MTS.MFSNet.sendToServer(new FlapPacket(plane.getEntityId(), (byte) -50));
				flapKeyPressed = true;
			}
		}else if(isControlPressed(controls.FLAPS_D)){
			if(!flapKeyPressed){
				MTS.MFSNet.sendToServer(new FlapPacket(plane.getEntityId(), (byte) 50));
				flapKeyPressed = true;
			}
		}else{
			flapKeyPressed = false;
		}
	}

	private static void checkPanel(EntityPlane plane, EntityPlayer controller){
		if(isControlPressed(controls.PANEL)){
			if(!panelKeyPressed){
				panelKeyPressed = true;
				if(Minecraft.getMinecraft().currentScreen == null){
					FMLCommonHandler.instance().showGuiScreen(new GUIPanelPlane(plane));
				}else if(Minecraft.getMinecraft().currentScreen.getClass().equals(GUIPanelPlane.class)){
					Minecraft.getMinecraft().displayGuiScreen((GuiScreen)null);
					Minecraft.getMinecraft().setIngameFocus();
				}
			}
		}else{
			panelKeyPressed = false;
		}
	}
	
	public enum controls{
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
		
		private controls(String name, boolean joystickOnly){
			this(name, joystickOnly ? "" : name, joystickOnly ? "" : name);
		}
		private controls(String joystick, String keyboardIncrement, String keyboardDecrement){
			this.joystickName = joystick;
			this.keyboardName = keyboardIncrement;
			this.keyboardIncrementName = keyboardIncrement;
			this.keyboardDecrementName = keyboardDecrement;
		}
	}
}
