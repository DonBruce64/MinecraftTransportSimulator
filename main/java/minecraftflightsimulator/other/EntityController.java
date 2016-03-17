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
import net.minecraftforge.common.config.Property;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EntityController{
	private static boolean brakeKeyPressed;
	private static boolean flapKeyPressed;
	private static boolean camLockKeyPressed;
	private static boolean zoomInKeyPressed;
	private static boolean zoomOutKeyPressed;
	private static int joystickControlId;
	private static int joystickMultiplier = 50;
	
	private static Controller joystick;
	private static Map<String, Integer> keyboardMap = new HashMap<String, Integer>();
	private static Map<String, Integer> joystickMap = new HashMap<String, Integer>();
	
	public static void initKeys(){
		keyboardMap.put("ModKey", MFS.config.get("keyboard", "ModKey", Keyboard.KEY_RSHIFT).getInt());
		keyboardMap.put("CamLockKey", MFS.config.get("keyboard", "CamLockKey", Keyboard.KEY_RCONTROL).getInt());
		keyboardMap.put("PitchUpKey", MFS.config.get("keyboard", "PitchUpKey", Keyboard.KEY_S).getInt());
		keyboardMap.put("PitchDownKey", MFS.config.get("keyboard", "PitchDownKey", Keyboard.KEY_W).getInt());
		keyboardMap.put("RollLeftKey", MFS.config.get("keyboard", "RollLeftKey", Keyboard.KEY_A).getInt());
		keyboardMap.put("RollRightKey", MFS.config.get("keyboard", "RollRightKey", Keyboard.KEY_D).getInt());
		keyboardMap.put("YawLeftKey", MFS.config.get("keyboard", "YawLeftKey", Keyboard.KEY_J).getInt());
		keyboardMap.put("YawRightKey", MFS.config.get("keyboard", "YawRightKey", Keyboard.KEY_L).getInt());
		keyboardMap.put("ThrottleUpKey", MFS.config.get("keyboard", "ThrottleUpKey", Keyboard.KEY_I).getInt());
		keyboardMap.put("ThrottleDownKey", MFS.config.get("keyboard", "ThrottleDownKey", Keyboard.KEY_K).getInt());
		keyboardMap.put("FlapsUpKey", MFS.config.get("keyboard", "FlapsUpKey", Keyboard.KEY_Y).getInt());
		keyboardMap.put("FlapsDownKey", MFS.config.get("keyboard", "FlapsDownKey", Keyboard.KEY_H).getInt());
		keyboardMap.put("BrakeKey", MFS.config.get("keyboard", "BrakeKey", Keyboard.KEY_B).getInt());
		keyboardMap.put("StarterKey", MFS.config.get("keyboard", "StarterKey", Keyboard.KEY_M).getInt());
		keyboardMap.put("ZoomInKey", MFS.config.get("keyboard", "ZoomInKey", Keyboard.KEY_PRIOR).getInt());
		keyboardMap.put("ZoomOutKey", MFS.config.get("keyboard", "ZoomOutKey", Keyboard.KEY_NEXT).getInt());
		
		String joystickName = MFS.config.get("joystick", "Name", "").getString();
		for(Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()){
			if(controller.getName().equals(joystickName)){
				joystick = controller;
				break;
			}
		}
		
		joystickMap.put("Mod", MFS.config.get("joystick", "Mod", 999).getInt());
		joystickMap.put("CamLock", MFS.config.get("joystick", "CamLock", 999).getInt());
		joystickMap.put("Pitch", MFS.config.get("joystick", "Pitch", 999).getInt());
		joystickMap.put("Roll", MFS.config.get("joystick", "Roll", 999).getInt());
		joystickMap.put("Yaw", MFS.config.get("joystick", "Yaw", 999).getInt());
		joystickMap.put("Throttle", MFS.config.get("joystick", "Throttle", 999).getInt());
		joystickMap.put("FlapsUp", MFS.config.get("joystick", "FlapsUp", 999).getInt());
		joystickMap.put("FlapsDown", MFS.config.get("joystick", "FlapsDown", 999).getInt());
		joystickMap.put("Brake", MFS.config.get("joystick", "Brake", 999).getInt());
		joystickMap.put("Starter", MFS.config.get("joystick", "Starter", 999).getInt());
		joystickMap.put("ZoomIn", MFS.config.get("joystick", "ZoomIn", 999).getInt());
		joystickMap.put("ZoomOut", MFS.config.get("joystick", "ZoomOut", 999).getInt());		
	}
	
	public static String getKeyboardKeyname(String keyname){
		if(keyboardMap.containsKey(keyname)){
			return Keyboard.getKeyName(keyboardMap.get(keyname));
		}else{
			return "";
		}
	}
	
	public static String getJoystickControlName(int componentId){
		for(Entry<String, Integer> entry : joystickMap.entrySet()){
			if(entry.getValue().equals(componentId)){
				return entry.getKey();
			}
		}
		return "";
	}

	private static int getJoystickControlIdByPartialString(String name){
		if(joystick != null){
			for(String joystickKey : joystickMap.keySet()){
				if(name.contains(joystickKey)){
					return joystickMap.get(joystickKey);
				}
			}
		}
		return 999;
	}
	
	public static void setKeyboardKey(String keyname, int bytecode){
		if(keyboardMap.containsKey(keyname)){
			keyboardMap.put(keyname, bytecode);
			MFS.config.getCategory("keyboard").put(keyname, new Property(keyname, String.valueOf(bytecode), Property.Type.INTEGER));
			MFS.config.save();
		}
	}
	
	public static void setJoystickControlNumber(String keyname, int componentNumber){
		if(joystickMap.containsKey(keyname)){
			joystickMap.put(keyname, componentNumber);
			MFS.config.getCategory("joystick").put(keyname, new Property(keyname, String.valueOf(componentNumber), Property.Type.INTEGER));
			MFS.config.save();
		}
	}
	
	public static void setJoystick(Controller controller){
		if(!joystick.getName().equals(MFS.config.get("joystick", "Name", "").getString())){
			joystick = controller;
			MFS.config.getCategory("joystick").put("Name", new Property("Name", joystick.getName(), Property.Type.STRING));
			for(String joystickControl : joystickMap.keySet()){
				setJoystickControlNumber(joystickControl, 999);
			}
		}
	}
	
	public static Controller getJoystick(){
		return joystick;
	}
	
	private static boolean isControlPressed(String name){
		joystickControlId = getJoystickControlIdByPartialString(name);
		if(joystickControlId != 999){
			return joystick.getComponents()[joystickControlId].getPollData() > 0;
		}else{
			return Keyboard.isKeyDown(keyboardMap.get(name));
		}
	}
	
	public static void controlCamera(){
		if(isControlPressed("CamLockKey")){
			if(!camLockKeyPressed){
				if(!isControlPressed("ModKey")){
					camLockKeyPressed=true;
					MFS.proxy.changeCameraLock();
				}
			}
		}else{
			camLockKeyPressed=false;
		}
		
		if(isControlPressed("ZoomInKey")){
			if(!zoomInKeyPressed){
				zoomInKeyPressed=true;
				MFS.proxy.changeCameraZoom(-1);
			}
		}else{
			zoomInKeyPressed=false;
		}
		if(isControlPressed("ZoomOutKey")){
			if(!zoomOutKeyPressed){
				zoomOutKeyPressed=true;
				MFS.proxy.changeCameraZoom(1);
			}
		}else{
			zoomOutKeyPressed=false;
		}	
	}
	
	public static void controlPlane(EntityPlane plane){
		joystickMultiplier = 50;
		joystick.poll();
		checkBrakes(plane);
		checkStarter(plane);
		checkThrottle(plane);
		
		
		joystickControlId = getJoystickControlIdByPartialString("Roll");
		if(joystickControlId != 999){
			MFS.MFSNet.sendToServer(new AileronPacket(plane.getEntityId(), (byte) (joystickMultiplier*joystick.getComponents()[joystickControlId].getPollData())));
		}else if(Keyboard.isKeyDown(keyboardMap.get("RollLeftKey"))){	
			MFS.MFSNet.sendToServer(new AileronPacket(plane.getEntityId(), (byte) -111));
		}else if(Keyboard.isKeyDown(keyboardMap.get("RollRightKey"))){
			MFS.MFSNet.sendToServer(new AileronPacket(plane.getEntityId(), (byte) 111));
		}
		
		joystickControlId = getJoystickControlIdByPartialString("Pitch");
		if(joystickControlId != 999){
			MFS.MFSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), (byte) (joystickMultiplier*joystick.getComponents()[joystickControlId].getPollData())));
		}else if(Keyboard.isKeyDown(keyboardMap.get("PitchDownKey"))){	
			MFS.MFSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), (byte) -111));
		}else if(Keyboard.isKeyDown(keyboardMap.get("PitchUpKey"))){
			MFS.MFSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), (byte) 111));
		}
		
		joystickControlId = getJoystickControlIdByPartialString("Yaw");
		if(joystickControlId != 999){
			if(joystick.getComponents()[joystickControlId].getDeadZone() < Math.abs(joystick.getComponents()[joystickControlId].getPollData())){
				MFS.MFSNet.sendToServer(new RudderPacket(plane.getEntityId(), (byte) (joystickMultiplier*joystick.getComponents()[joystickControlId].getPollData())));
			}else{
				MFS.MFSNet.sendToServer(new RudderPacket(plane.getEntityId(), (byte) 0));
			}
		}else if(Keyboard.isKeyDown(keyboardMap.get("YawLeftKey"))){
			MFS.MFSNet.sendToServer(new RudderPacket(plane.getEntityId(), (byte) -111));
		}else if(Keyboard.isKeyDown(keyboardMap.get("YawRightKey"))){
			MFS.MFSNet.sendToServer(new RudderPacket(plane.getEntityId(), (byte) 111));
		}
		
		if(plane.hasFlaps){
			if(isControlPressed("FlapsUpKey")){
				if(!flapKeyPressed){
					MFS.MFSNet.sendToServer(new FlapPacket(plane.getEntityId(), (byte) -50));
					flapKeyPressed = true;
				}
			}else if(isControlPressed("FlapsDownKey")){
				if(!flapKeyPressed){
					MFS.MFSNet.sendToServer(new FlapPacket(plane.getEntityId(), (byte) 50));
					flapKeyPressed = true;
				}
			}else{
				flapKeyPressed = false;
			}
		}
		
		if(isControlPressed("CamLockKey")){
			if(!camLockKeyPressed){
				if(isControlPressed("ModKey")){
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
		if(isControlPressed("BrakeKey")){
			if(!brakeKeyPressed){
				brakeKeyPressed = true;
				if(isControlPressed("ModKey")){
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
		if(isControlPressed("StarterKey")){
			if(isControlPressed("ModKey")){
				MFS.MFSNet.sendToServer(new EnginePacket(vehicle.getEntityId(), (byte) 0));
			}else{
				MFS.MFSNet.sendToServer(new EnginePacket(vehicle.getEntityId(), (byte) 1));
			}
		}
	}
	
	private static void checkThrottle(EntityParent vehicle){
		joystickControlId = getJoystickControlIdByPartialString("Throttle");
		if(joystickControlId != 999){
			MFS.MFSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), (byte) (joystickMultiplier + joystickMultiplier*joystick.getComponents()[joystickControlId].getPollData())));
		}else if(Keyboard.isKeyDown(keyboardMap.get("ThrottleUpKey"))){
			MFS.MFSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), (byte) 111));
		}else if(Keyboard.isKeyDown(keyboardMap.get("ThrottleDownKey"))){
			MFS.MFSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), (byte) -111));
		}
	}
}
