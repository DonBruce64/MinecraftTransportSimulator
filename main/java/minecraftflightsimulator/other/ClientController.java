package minecraftflightsimulator.other;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import minecraftflightsimulator.ClientProxy;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.EntityParent;
import minecraftflightsimulator.entities.EntityPlane;
import minecraftflightsimulator.packets.control.AileronPacket;
import minecraftflightsimulator.packets.control.BrakePacket;
import minecraftflightsimulator.packets.control.ElevatorPacket;
import minecraftflightsimulator.packets.control.EnginePacket;
import minecraftflightsimulator.packets.control.FlapPacket;
import minecraftflightsimulator.packets.control.RudderPacket;
import minecraftflightsimulator.packets.control.ThrottlePacket;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.config.Property;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Class that handles all control operations.  Keybinding lists are initiated during PreInit.
 * @author don_bruce
 *
 */
@SideOnly(Side.CLIENT)
public class ClientController{
	//Internal checkers for keys.
	private static boolean brakeKeyPressed;
	private static boolean flapKeyPressed;
	private static boolean camLockKeyPressed;
	private static boolean changeViewKeyPressed;
	private static boolean zoomInKeyPressed;
	private static boolean zoomOutKeyPressed;
	
	//Constants for actions.
	private static final int NULL_COMPONENT = 999;
	private static final String KEYBOARD_CONFIG = "keyboard";
	private static final String JOYSTICK_CONFIG = "joystick";
	private static final String CLIENTSIDE_CONFIG = "clientconfig";

	//Data arrays
	public static boolean seaLevelOffset;
	private static boolean throttleKills;
	private static byte joystickForceFactor;
	private static short controlSurfaceCooldown;
	private static double joystickDeadZone;
	private static Controller joystick;
	private static Map<String, Integer> keyboardMap = new HashMap<String, Integer>();
	private static Map<String, Integer> joystickMap = new HashMap<String, Integer>();
	
	public static void init(){
		throttleKills = MFS.config.get(CLIENTSIDE_CONFIG, "ThrottleKills", false, "Whether or not the throttle can be put to zero and kill the engine.  \nOnly valid for joysticks.").getBoolean();
		seaLevelOffset = MFS.config.get(CLIENTSIDE_CONFIG, "SeaLevelOffset", false, "Does altimiter read zero at Y=64 instead of Y=0?").getBoolean();
		joystickForceFactor = (byte) MFS.config.get(CLIENTSIDE_CONFIG, "JoystickForceFactor", 15, "Factor by which joystick inputs are multiplied.  \nThis controls how quickly the control surfaces change.", 1, Byte.MAX_VALUE).getInt();
		controlSurfaceCooldown = (short) MFS.config.get(CLIENTSIDE_CONFIG, "ControlSurfaceCooldown", 4, "How long (in ticks) it takes before control surfaces try to return to their natural angle.  \nThis is not used when using a joystick.", 0, Short.MAX_VALUE).getInt();
		joystickDeadZone = MFS.config.get(CLIENTSIDE_CONFIG, "DeadZone", 0.03D, "Dead zone for joystick axis.  \nMFS will always use the greater of this value and the value provided by the computer.").getDouble();
		MFS.config.save();
		
		keyboardMap.put(controls.MOD.keyboardName, MFS.config.get(KEYBOARD_CONFIG, controls.MOD.keyboardName, Keyboard.KEY_RSHIFT).getInt());
		keyboardMap.put(controls.CAM.keyboardName, MFS.config.get(KEYBOARD_CONFIG, controls.CAM.keyboardName, Keyboard.KEY_RCONTROL).getInt());
		keyboardMap.put(controls.PITCH.keyboardIncrementName, MFS.config.get(KEYBOARD_CONFIG, controls.PITCH.keyboardIncrementName, Keyboard.KEY_S).getInt());
		keyboardMap.put(controls.PITCH.keyboardDecrementName, MFS.config.get(KEYBOARD_CONFIG, controls.PITCH.keyboardDecrementName, Keyboard.KEY_W).getInt());
		keyboardMap.put(controls.ROLL.keyboardIncrementName, MFS.config.get(KEYBOARD_CONFIG, controls.ROLL.keyboardIncrementName, Keyboard.KEY_D).getInt());
		keyboardMap.put(controls.ROLL.keyboardDecrementName, MFS.config.get(KEYBOARD_CONFIG, controls.ROLL.keyboardDecrementName, Keyboard.KEY_A).getInt());
		keyboardMap.put(controls.YAW.keyboardIncrementName, MFS.config.get(KEYBOARD_CONFIG, controls.YAW.keyboardIncrementName, Keyboard.KEY_L).getInt());
		keyboardMap.put(controls.YAW.keyboardDecrementName, MFS.config.get(KEYBOARD_CONFIG, controls.YAW.keyboardDecrementName, Keyboard.KEY_J).getInt());
		keyboardMap.put(controls.THROTTLE.keyboardIncrementName, MFS.config.get(KEYBOARD_CONFIG, controls.THROTTLE.keyboardIncrementName, Keyboard.KEY_I).getInt());
		keyboardMap.put(controls.THROTTLE.keyboardDecrementName, MFS.config.get(KEYBOARD_CONFIG, controls.THROTTLE.keyboardDecrementName, Keyboard.KEY_K).getInt());
		keyboardMap.put(controls.FLAPS_U.keyboardName, MFS.config.get(KEYBOARD_CONFIG, controls.FLAPS_U.keyboardName, Keyboard.KEY_Y).getInt());
		keyboardMap.put(controls.FLAPS_D.keyboardName, MFS.config.get(KEYBOARD_CONFIG, controls.FLAPS_D.keyboardName, Keyboard.KEY_H).getInt());
		keyboardMap.put(controls.BRAKE.keyboardName, MFS.config.get(KEYBOARD_CONFIG, controls.BRAKE.keyboardName, Keyboard.KEY_B).getInt());
		keyboardMap.put(controls.STARTER.keyboardName, MFS.config.get(KEYBOARD_CONFIG, controls.STARTER.keyboardName, Keyboard.KEY_M).getInt());
		keyboardMap.put(controls.ZOOM_I.keyboardName, MFS.config.get(KEYBOARD_CONFIG, controls.ZOOM_I.keyboardName, Keyboard.KEY_PRIOR).getInt());
		keyboardMap.put(controls.ZOOM_O.keyboardName, MFS.config.get(KEYBOARD_CONFIG, controls.ZOOM_O.keyboardName, Keyboard.KEY_NEXT).getInt());
		
		String joystickName = MFS.config.get(CLIENTSIDE_CONFIG, "JoystickName", "", "Name of the joystick controller.  \nChanged to match selection in control GUI.").getString();
		for(Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()){
			if(controller.getName().equals(joystickName)){
				joystick = controller;
				break;
			}
		}
		
		joystickMap.put(controls.MOD.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.MOD.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.CAM.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.CAM.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.PITCH.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.PITCH.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.ROLL.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.ROLL.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.YAW.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.YAW.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.THROTTLE.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.THROTTLE.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.FLAPS_U.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.FLAPS_U.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.FLAPS_D.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.FLAPS_D.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.BRAKE.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.BRAKE.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.STARTER.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.STARTER.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.ZOOM_I.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.ZOOM_I.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.ZOOM_O.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.ZOOM_O.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.CHANGEVIEW.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.CHANGEVIEW.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.LOOK_L.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.LOOK_L.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.LOOK_R.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.LOOK_R.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.LOOK_U.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.LOOK_U.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.LOOK_D.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.LOOK_D.joystickName, NULL_COMPONENT).getInt());
		joystickMap.put(controls.LOOK_ALL.joystickName, MFS.config.get(JOYSTICK_CONFIG, controls.LOOK_ALL.joystickName, NULL_COMPONENT).getInt());
		
		controls.PITCH.joystickMaxTravel = MFS.config.get(JOYSTICK_CONFIG, controls.PITCH.joystickName+"maxtravel", 1D).getDouble();
		controls.PITCH.joystickMinTravel = MFS.config.get(JOYSTICK_CONFIG, controls.PITCH.joystickName+"mintravel", -1D).getDouble();
		controls.ROLL.joystickMaxTravel = MFS.config.get(JOYSTICK_CONFIG, controls.ROLL.joystickName+"maxtravel", 1D).getDouble();
		controls.ROLL.joystickMinTravel = MFS.config.get(JOYSTICK_CONFIG, controls.ROLL.joystickName+"mintravel", -1D).getDouble();
		controls.YAW.joystickMaxTravel = MFS.config.get(JOYSTICK_CONFIG, controls.YAW.joystickName+"maxtravel", 1D).getDouble();
		controls.YAW.joystickMinTravel = MFS.config.get(JOYSTICK_CONFIG, controls.YAW.joystickName+"mintravel", -1D).getDouble();
		controls.THROTTLE.joystickMaxTravel = MFS.config.get(JOYSTICK_CONFIG, controls.THROTTLE.joystickName+"maxtravel", 1D).getDouble();
		controls.THROTTLE.joystickMinTravel = MFS.config.get(JOYSTICK_CONFIG, controls.THROTTLE.joystickName+"mintravel", -1D).getDouble();
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
		MFS.config.getCategory(KEYBOARD_CONFIG).put(keyname, new Property(keyname, String.valueOf(bytecode), Property.Type.INTEGER));
		MFS.config.save();
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
			MFS.config.getCategory(JOYSTICK_CONFIG).put(currentMapping, new Property(currentMapping, String.valueOf(NULL_COMPONENT), Property.Type.INTEGER));
		}			
		joystickMap.put(keyname, componentId);
		MFS.config.getCategory(JOYSTICK_CONFIG).put(keyname, new Property(keyname, String.valueOf(componentId), Property.Type.INTEGER));
		MFS.config.save();
	}
	
	public static void setAxisBounds(String axisName, double minBound, double maxBound){
		for(ClientController.controls control : ClientController.controls.values()){
			if(control.joystickName.equals(axisName)){
				control.joystickMinTravel = minBound;
				control.joystickMaxTravel = maxBound;
				MFS.config.getCategory(JOYSTICK_CONFIG).put(control.joystickName+"maxtravel", new Property(control.joystickName+"maxtravel", String.valueOf(control.joystickMaxTravel), Property.Type.DOUBLE));
				MFS.config.getCategory(JOYSTICK_CONFIG).put(control.joystickName+"mintravel", new Property(control.joystickName+"mintravel", String.valueOf(control.joystickMinTravel), Property.Type.DOUBLE));
			}
		}
	}
	
	public static void setJoystick(Controller controller){
		joystick = controller;
		if(!joystick.getName().equals(MFS.config.get(CLIENTSIDE_CONFIG, "JoystickName", "").getString())){
			MFS.config.getCategory(CLIENTSIDE_CONFIG).put("JoystickName", new Property("JoystickName", joystick.getName(), Property.Type.STRING));
			for(String joystickControl : joystickMap.keySet()){
				setJoystickControl(joystickControl, NULL_COMPONENT);
			}
		}
	}
	
	public static Controller getJoystick(){
		return joystick;
	}
	
	public static int getNullComponent(){
		return NULL_COMPONENT;
	}
	
	private static boolean isControlPressed(ClientController.controls control){
		if(!joystickMap.get(control.joystickName).equals(NULL_COMPONENT) && joystick != null){
			return joystick.getComponents()[joystickMap.get(control.joystickName)].getPollData() > 0;
		}
		if(keyboardMap.containsKey(control.keyboardName)){
			return Keyboard.isKeyDown(keyboardMap.get(control.keyboardName));
		}
		return false;
	}
	
	private static short getAxisState(ClientController.controls control, boolean controlSurface){
		float pollValue = joystick.getComponents()[joystickMap.get(control.joystickName)].getPollData();
		if(Math.abs(pollValue) > joystick.getComponents()[joystickMap.get(control.joystickName)].getDeadZone() && Math.abs(pollValue) > joystickDeadZone){
			if(pollValue < 0){
				if(controlSurface){
					return (short) (-350*Math.pow(2, joystickForceFactor*pollValue/control.joystickMinTravel - joystickForceFactor));
				}else{
					return (short) (-100*pollValue/control.joystickMinTravel);
				}
			}else{
				if(controlSurface){
					return (short) (350*Math.pow(2, joystickForceFactor*pollValue/control.joystickMaxTravel - joystickForceFactor));
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
					MFS.proxy.changeCameraLock();
				}
			}
		}else{
			camLockKeyPressed=false;
		}
		
		if(isControlPressed(controls.ZOOM_I)){
			if(!zoomInKeyPressed){
				zoomInKeyPressed=true;
				MFS.proxy.changeCameraZoom(-1);
			}
		}else{
			zoomInKeyPressed=false;
		}
		if(isControlPressed(controls.ZOOM_O)){
			if(!zoomOutKeyPressed){
				zoomOutKeyPressed=true;
				MFS.proxy.changeCameraZoom(1);
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
	
	public static void controlPlane(EntityPlane plane){
		if(joystick!=null){
			if(!joystick.poll()){
				joystick = null;
			}
		}
		checkHUD();
		checkBrakes(plane);
		checkStarter(plane);
		checkThrottle(plane);
		if(plane.hasFlaps){checkFlaps(plane);}
		
		if(joystickMap.get(controls.ROLL.joystickName) != 999 && joystick != null){
			MFS.MFSNet.sendToServer(new AileronPacket(plane.getEntityId(), getAxisState(controls.ROLL, true)));
		}else if(Keyboard.isKeyDown(keyboardMap.get(controls.ROLL.keyboardIncrementName))){
			MFS.MFSNet.sendToServer(new AileronPacket(plane.getEntityId(), true, controlSurfaceCooldown));
		}else if(Keyboard.isKeyDown(keyboardMap.get(controls.ROLL.keyboardDecrementName))){
			MFS.MFSNet.sendToServer(new AileronPacket(plane.getEntityId(), false, controlSurfaceCooldown));
		}
		
		if(joystickMap.get(controls.PITCH.joystickName) != 999 && joystick != null){
			MFS.MFSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), getAxisState(controls.PITCH, true)));
		}else if(Keyboard.isKeyDown(keyboardMap.get(controls.PITCH.keyboardIncrementName))){
			MFS.MFSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), true, controlSurfaceCooldown));
		}else if(Keyboard.isKeyDown(keyboardMap.get(controls.PITCH.keyboardDecrementName))){
			MFS.MFSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), false, controlSurfaceCooldown));
		}
		
		if(joystickMap.get(controls.YAW.joystickName) != 999 && joystick != null){
			MFS.MFSNet.sendToServer(new RudderPacket(plane.getEntityId(), getAxisState(controls.YAW, true)));
		}else if(Keyboard.isKeyDown(keyboardMap.get(controls.YAW.keyboardIncrementName))){
			MFS.MFSNet.sendToServer(new RudderPacket(plane.getEntityId(), true, controlSurfaceCooldown));
		}else if(Keyboard.isKeyDown(keyboardMap.get(controls.YAW.keyboardDecrementName))){
			MFS.MFSNet.sendToServer(new RudderPacket(plane.getEntityId(), false, controlSurfaceCooldown));
		}
	}
	
	private static void checkHUD(){
		if(isControlPressed(controls.CAM)){
			if(!camLockKeyPressed){
				if(isControlPressed(controls.MOD)){
					camLockKeyPressed=true;
					if(ClientProxy.hudMode == 3){
						ClientProxy.hudMode = 0;
					}else{
						++ClientProxy.hudMode;
					} 
				}
			}
		}
	}
	
	private static void checkBrakes(EntityParent vehicle){
		if(isControlPressed(controls.BRAKE)){
			if(!brakeKeyPressed){
				brakeKeyPressed = true;
				if(isControlPressed(controls.MOD)){
					MFS.MFSNet.sendToServer(new BrakePacket(vehicle.getEntityId(), (byte) 12));
				}else{
					MFS.MFSNet.sendToServer(new BrakePacket(vehicle.getEntityId(), (byte) 11));
				}
			}
		}else if(brakeKeyPressed){
			brakeKeyPressed = false;
			MFS.MFSNet.sendToServer(new BrakePacket(vehicle.getEntityId(), (byte) 2));
		}
	}
	
	private static void checkStarter(EntityParent vehicle){
		if(isControlPressed(controls.STARTER)){
			if(isControlPressed(controls.MOD)){
				MFS.MFSNet.sendToServer(new EnginePacket(vehicle.getEntityId(), (byte) 0));
			}else{
				MFS.MFSNet.sendToServer(new EnginePacket(vehicle.getEntityId(), (byte) 1));
			}
		}
	}
	
	private static void checkThrottle(EntityParent vehicle){
		if(joystickMap.get(controls.THROTTLE.joystickName) != 999 && joystick != null){
			MFS.MFSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), (byte) (Math.max(50 + getAxisState(controls.THROTTLE, false)/2, throttleKills ? 0 : 15))));
		}else if(Keyboard.isKeyDown(keyboardMap.get(controls.THROTTLE.keyboardIncrementName))){
			MFS.MFSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), Byte.MAX_VALUE));
		}else if(Keyboard.isKeyDown(keyboardMap.get(controls.THROTTLE.keyboardDecrementName))){
			MFS.MFSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), Byte.MIN_VALUE));
		}
	}
	
	private static void checkFlaps(EntityPlane plane){
		if(isControlPressed(controls.FLAPS_U)){
			if(!flapKeyPressed){
				MFS.MFSNet.sendToServer(new FlapPacket(plane.getEntityId(), (byte) -50));
				flapKeyPressed = true;
			}
		}else if(isControlPressed(controls.FLAPS_D)){
			if(!flapKeyPressed){
				MFS.MFSNet.sendToServer(new FlapPacket(plane.getEntityId(), (byte) 50));
				flapKeyPressed = true;
			}
		}else{
			flapKeyPressed = false;
		}
	}
	
	public enum controls{
		MOD("Mod", "ModKey"),
		CAM("CamLock", "CamLockKey"),
		PITCH("Pitch", "PitchUpKey", "PitchDownKey"),
		ROLL("Roll", "RollRightKey", "RollLeftKey"),
		YAW("Yaw", "YawRightKey", "YawLeftKey"),
		THROTTLE("Throttle", "ThrottleUpKey", "ThrottleDownKey"),
		FLAPS_U("FlapsUp", "FlapsUpKey"),
		FLAPS_D("FlapsDown", "FlapsDownKey"),
		BRAKE("Brake", "BrakeKey"),
		STARTER("Starter", "StarterKey"),
		ZOOM_I("ZoomIn", "ZoomInKey"),
		ZOOM_O("ZoomOut", "ZoomOutKey"),
		CHANGEVIEW("ChangeView", ""),
		LOOK_L("LookLeft", ""),
		LOOK_R("LookRight", ""),
		LOOK_U("LookUp", ""),
		LOOK_D("LookDown", ""),
		LOOK_ALL("LookDirectional", "")
		;
		
		public final String keyboardName;
		public final String keyboardIncrementName;
		public final String keyboardDecrementName;
		public final String joystickName;
		public double joystickMaxTravel = 1;
		public double joystickMinTravel = -1;
		
		private controls(String joystick, String keyboard){
			this(joystick, keyboard, keyboard);
		}
		private controls(String joystick, String keyboardIncrement, String keyboardDecrement){
			this.joystickName = joystick;
			this.keyboardName = keyboardIncrement;
			this.keyboardIncrementName = keyboardIncrement;
			this.keyboardDecrementName = keyboardDecrement;
		}
	}
}
