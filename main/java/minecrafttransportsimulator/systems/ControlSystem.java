package minecrafttransportsimulator.systems;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import minecrafttransportsimulator.ClientProxy;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.guis.GUIPanelAircraft;
import minecrafttransportsimulator.packets.control.AileronPacket;
import minecrafttransportsimulator.packets.control.BrakePacket;
import minecrafttransportsimulator.packets.control.ElevatorPacket;
import minecrafttransportsimulator.packets.control.FlapPacket;
import minecrafttransportsimulator.packets.control.HornPacket;
import minecrafttransportsimulator.packets.control.LightPacket;
import minecrafttransportsimulator.packets.control.ReverseThrustPacket;
import minecrafttransportsimulator.packets.control.RudderPacket;
import minecrafttransportsimulator.packets.control.ShiftPacket;
import minecrafttransportsimulator.packets.control.SirenPacket;
import minecrafttransportsimulator.packets.control.SteeringPacket;
import minecrafttransportsimulator.packets.control.ThrottlePacket;
import minecrafttransportsimulator.packets.control.TrailerPacket;
import minecrafttransportsimulator.packets.control.TrimPacket;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal.PacketEngineTypes;
import minecrafttransportsimulator.packets.parts.PacketPartGunSignal;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered.LightTypes;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Ground;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartGun;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**Class that handles all control operations.
 * Keybinding lists are initiated during the {@link ClientProxy} init method.
 * 
 * @author don_bruce
 */
@SideOnly(Side.CLIENT)
public final class ControlSystem{	
	private static boolean joystickEnabled = false;
	private static final int NULL_COMPONENT = 999;
	private static final String KEYBOARD_CONFIG = "controls_keyboard";
	private static final String JOYSTICK_CONFIG = "controls_joystick";
	private static final Map<String, Controller> joystickMap = new HashMap<String, Controller>();
	
	private static KeyBinding configKey;
	private static short mousePosX = 0;
	private static short mousePosY = 0;
	private static final Map<ControlsKeyboard, Long> pressedKeyboardButtons = new HashMap<ControlsKeyboard, Long>();
	private static final Map<ControlsJoystick, Long> pressedJoystickButtons = new HashMap<ControlsJoystick, Long>();
	
	
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
	
	public static void controlVehicle(EntityVehicleE_Powered vehicle, boolean isPlayerController){
		if(vehicle instanceof EntityVehicleF_Air){
			controlAircraft((EntityVehicleF_Air) vehicle, isPlayerController);
		}else if(vehicle instanceof EntityVehicleF_Ground){
			controlGroundVehicle((EntityVehicleF_Ground) vehicle, isPlayerController);
		}
	}
	
	private static <ControlEnum> boolean getTrueButtonState(Map<ControlEnum, Long> buttonMap, ControlEnum button, boolean pressed){
		//If this control is used in a momentary fashion make sure to only
		//fire the event on the tick that the button was last pressed on!
		if(pressed){
			Long time = Minecraft.getMinecraft().world.getTotalWorldTime();
			if(!buttonMap.containsKey(button)){
				buttonMap.put(button, time);
			}
			return buttonMap.get(button).longValue() == time;
		}else{
			buttonMap.remove(button);
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
	private static short getJoystickAxisState(ControlsJoystick control, short pollBounds){
		if(joystickMap.containsKey(control.joystickAssigned)){
			joystickMap.get(control.joystickAssigned).poll();
			float pollValue = joystickMap.get(control.joystickAssigned).getComponents()[control.joystickButton].getPollData();
			if(Math.abs(pollValue) > ConfigSystem.getDoubleConfig("JoystickDeadZone") || pollBounds == 0){
				//Clamp the poll value to the defined axis bounds set during config to prevent over and under-runs.
				pollValue = (float) Math.max(control.joystickMinTravel, pollValue);
				pollValue = (float) Math.min(control.joystickMaxTravel, pollValue);				
				
				//If we don't need to normalize the axis, return it as-is.  Otherwise do a normalization from 0-1.
				if(pollBounds != 0){
					return (short) (control.joystickInverted ? (-pollBounds*pollValue) : (pollBounds*pollValue));
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
		
		float pollData = getJoystickMultistateValue(lookA);
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
		if(joystickMap.containsKey(analogBrake.joystickAssigned) && analogBrake.joystickButton != NULL_COMPONENT){
			if(pBrake.isPressed()){
				MTS.MTSNet.sendToServer(new BrakePacket(entityID, (byte) 12));
			}else if(getJoystickAxisState(analogBrake, (short) 0) > 25){
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
			if(seat.isController){
				for(APart part : vehicle.getVehicleParts()){
					if(part instanceof APartGun && part.parentPart == null){
						MTS.MTSNet.sendToServer(new PacketPartGunSignal((APartGun) part, Minecraft.getMinecraft().player.getEntityId(), gun.isPressed()));
					}
				}
			}else{
				for(APart part : seat.childParts){
					if(part instanceof APartGun){
						MTS.MTSNet.sendToServer(new PacketPartGunSignal((APartGun) part, Minecraft.getMinecraft().player.getEntityId(), gun.isPressed()));
					}
				}
			}
		}
	}
	
	private static void controlAircraft(EntityVehicleF_Air aircraft, boolean isPlayerController){
		controlCamera(ControlsKeyboardDynamic.AIRCRAFT_CHANGEHUD, ControlsKeyboard.AIRCRAFT_ZOOM_I, ControlsKeyboard.AIRCRAFT_ZOOM_O, ControlsJoystick.AIRCRAFT_CHANGEHUD, ControlsJoystick.AIRCRAFT_CHANGEVIEW);
		rotateCamera(ControlsJoystick.AIRCRAFT_LOOK_R, ControlsJoystick.AIRCRAFT_LOOK_L, ControlsJoystick.AIRCRAFT_LOOK_U, ControlsJoystick.AIRCRAFT_LOOK_D, ControlsJoystick.AIRCRAFT_LOOK_A);
		controlGun(aircraft, ControlsKeyboard.AIRCRAFT_GUN);
		if(!isPlayerController){
			return;
		}
		controlBrake(ControlsKeyboardDynamic.AIRCRAFT_PARK, ControlsJoystick.AIRCRAFT_BRAKE_ANALOG, ControlsJoystick.AIRCRAFT_PARK, aircraft.getEntityId());
		
		
		//Open or close the panel.
		if(ControlsKeyboard.AIRCRAFT_PANEL.isPressed()){
			if(Minecraft.getMinecraft().currentScreen == null){
				FMLCommonHandler.instance().showGuiScreen(new GUIPanelAircraft(aircraft));
			}else if(Minecraft.getMinecraft().currentScreen instanceof GUIPanelAircraft){
				Minecraft.getMinecraft().displayGuiScreen((GuiScreen)null);
				Minecraft.getMinecraft().setIngameFocus();
			}
		}
		
		//Check for thrust reverse button.
		if(ControlsJoystick.AIRCRAFT_REVERSE.isPressed()){
			MTS.proxy.playSound(aircraft.getPositionVector(), MTS.MODID + ":panel_buzzer", 1.0F, 1.0F);
			MTS.MTSNet.sendToServer(new ReverseThrustPacket(aircraft.getEntityId(), !aircraft.reverseThrust));
		}
		
		//Increment or decrement throttle.
		if(joystickMap.containsKey(ControlsJoystick.AIRCRAFT_THROTTLE.joystickAssigned) && ControlsJoystick.AIRCRAFT_THROTTLE.joystickButton != NULL_COMPONENT){
			MTS.MTSNet.sendToServer(new ThrottlePacket(aircraft.getEntityId(), (byte) getJoystickAxisState(ControlsJoystick.AIRCRAFT_THROTTLE, (short) 0)));
		}else{
			if(ControlsKeyboard.AIRCRAFT_THROTTLE_U.isPressed()){
				MTS.MTSNet.sendToServer(new ThrottlePacket(aircraft.getEntityId(), Byte.MAX_VALUE));
			}
			if(ControlsKeyboard.AIRCRAFT_THROTTLE_D.isPressed()){
				MTS.MTSNet.sendToServer(new ThrottlePacket(aircraft.getEntityId(), Byte.MIN_VALUE));
			}
		}		
		
		//Check flaps.
		if(aircraft.pack.plane != null && aircraft.pack.plane.hasFlaps){
			if(ControlsKeyboard.AIRCRAFT_FLAPS_U.isPressed()){
				MTS.MTSNet.sendToServer(new FlapPacket(aircraft.getEntityId(), (byte) -50));
			}
			if(ControlsKeyboard.AIRCRAFT_FLAPS_D.isPressed()){
				MTS.MTSNet.sendToServer(new FlapPacket(aircraft.getEntityId(), (byte) 50));
			}
		}
		
		//Check yaw.
		if(joystickMap.containsKey(ControlsJoystick.AIRCRAFT_YAW.joystickAssigned) && ControlsJoystick.AIRCRAFT_YAW.joystickButton != NULL_COMPONENT){
			MTS.MTSNet.sendToServer(new RudderPacket(aircraft.getEntityId(), getJoystickAxisState(ControlsJoystick.AIRCRAFT_YAW, (short) 250)));
		}else{
			if(ControlsKeyboard.AIRCRAFT_YAW_R.isPressed()){
				MTS.MTSNet.sendToServer(new RudderPacket(aircraft.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
			}
			if(ControlsKeyboard.AIRCRAFT_YAW_L.isPressed()){
				MTS.MTSNet.sendToServer(new RudderPacket(aircraft.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
			}
		}
		if(ControlsJoystick.AIRCRAFT_TRIM_YAW_R.isPressed()){
			MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 10));
		}
		if(ControlsJoystick.AIRCRAFT_TRIM_YAW_L.isPressed()){
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
				MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), getJoystickAxisState(ControlsJoystick.AIRCRAFT_PITCH, (short) 250)));
			}else{
				if(ControlsKeyboard.AIRCRAFT_PITCH_U.isPressed()){
					MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
				if(ControlsKeyboard.AIRCRAFT_PITCH_D.isPressed()){
					MTS.MTSNet.sendToServer(new ElevatorPacket(aircraft.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_PITCH_U.isPressed()){
				MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 9));
			}
			if(ControlsJoystick.AIRCRAFT_TRIM_PITCH_D.isPressed()){
				MTS.MTSNet.sendToServer(new TrimPacket(aircraft.getEntityId(), (byte) 1));
			}
			
			//Check roll.
			if(joystickMap.containsKey(ControlsJoystick.AIRCRAFT_ROLL.joystickAssigned) && ControlsJoystick.AIRCRAFT_ROLL.joystickButton != NULL_COMPONENT){
				MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), getJoystickAxisState(ControlsJoystick.AIRCRAFT_ROLL, (short) 250)));
			}else{
				if(ControlsKeyboard.AIRCRAFT_ROLL_R.isPressed()){
					MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
				}
				if(ControlsKeyboard.AIRCRAFT_ROLL_L.isPressed()){
					MTS.MTSNet.sendToServer(new AileronPacket(aircraft.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
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
		controlCamera(ControlsKeyboardDynamic.CAR_CHANGEHUD, ControlsKeyboard.CAR_ZOOM_I, ControlsKeyboard.CAR_ZOOM_O, ControlsJoystick.CAR_CHANGEHUD, ControlsJoystick.CAR_CHANGEVIEW);
		rotateCamera(ControlsJoystick.CAR_LOOK_R, ControlsJoystick.CAR_LOOK_L, ControlsJoystick.CAR_LOOK_U, ControlsJoystick.CAR_LOOK_D, ControlsJoystick.CAR_LOOK_A);
		if(!isPlayerController){
			return;
		}
		controlBrake(ControlsKeyboardDynamic.CAR_PARK, ControlsJoystick.CAR_BRAKE_ANALOG, ControlsJoystick.CAR_PARK, powered.getEntityId());
		controlGun(powered, ControlsKeyboard.CAR_GUN);
		
		//Change gas to on or off.
		if(joystickMap.containsKey(ControlsJoystick.CAR_GAS.joystickAssigned) && ControlsJoystick.CAR_GAS.joystickButton != NULL_COMPONENT){
			MTS.MTSNet.sendToServer(new ThrottlePacket(powered.getEntityId(), (byte) getJoystickAxisState(ControlsJoystick.CAR_GAS, (short) 0)));
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
		if(ControlsKeyboardDynamic.CAR_SIREN.isPressed()){
			MTS.MTSNet.sendToServer(new SirenPacket(powered.getEntityId()));
		}else if(ControlsKeyboard.CAR_LIGHTS_SPECIAL.isPressed()){
			MTS.MTSNet.sendToServer(new LightPacket(powered.getEntityId(), LightTypes.EMERGENCYLIGHT));
		}
			
			
		if(ControlsKeyboardDynamic.CAR_TURNSIGNAL_R.isPressed()){
			MTS.MTSNet.sendToServer(new LightPacket(powered.getEntityId(), LightTypes.RIGHTTURNLIGHT));
		}else if(ControlsKeyboardDynamic.CAR_TURNSIGNAL_L.isPressed()){
			MTS.MTSNet.sendToServer(new LightPacket(powered.getEntityId(), LightTypes.LEFTTURNLIGHT));
		}else{
			if(ControlsKeyboard.CAR_LIGHTS.isPressed()){
				MTS.MTSNet.sendToServer(new LightPacket(powered.getEntityId(), LightTypes.HEADLIGHT));
				MTS.MTSNet.sendToServer(new LightPacket(powered.getEntityId(), LightTypes.RUNNINGLIGHT));
			}
			//Check is mouse yoke is enabled.  If so do controls by mouse rather than buttons.
			if(ConfigSystem.getBooleanConfig("MouseYoke")){
				if(CameraSystem.lockedView && Minecraft.getMinecraft().currentScreen == null){
					int dx = Mouse.getDX();
					if(Math.abs(dx) < 100){
						mousePosX = (short) Math.max(Math.min(mousePosX + dx*10, 450), -450);
					}
					MTS.MTSNet.sendToServer(new SteeringPacket(powered.getEntityId(), mousePosX));
				}
			}else{
				if(joystickMap.containsKey(ControlsJoystick.CAR_TURN.joystickAssigned) && ControlsJoystick.CAR_TURN.joystickButton != NULL_COMPONENT){
					MTS.MTSNet.sendToServer(new SteeringPacket(powered.getEntityId(), getJoystickAxisState(ControlsJoystick.CAR_TURN, (short) 450)));
				}else{
					boolean turningRight = ControlsKeyboard.CAR_TURN_R.isPressed();
					boolean turningLeft = ControlsKeyboard.CAR_TURN_L.isPressed();
					if(turningRight && !turningLeft){
						MTS.MTSNet.sendToServer(new SteeringPacket(powered.getEntityId(), true, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
					}else if(turningLeft && !turningRight){
						MTS.MTSNet.sendToServer(new SteeringPacket(powered.getEntityId(), false, (short) ConfigSystem.getIntegerConfig("ControlSurfaceCooldown")));
					}
				}
			}
		}
		
		//Check starter.
		if(powered.getEngineByNumber((byte) 0) != null){
			if(ControlsKeyboardDynamic.CAR_STOP.isPressed() || ControlsJoystick.CAR_STOP.isPressed()){
				MTS.MTSNet.sendToServer(new PacketPartEngineSignal(powered.getEngineByNumber((byte) 0), PacketEngineTypes.MAGNETO_OFF));
			}else if(ControlsKeyboard.CAR_START.isPressed()){
				MTS.MTSNet.sendToServer(new PacketPartEngineSignal(powered.getEngineByNumber((byte) 0), PacketEngineTypes.MAGNETO_ON));
				MTS.MTSNet.sendToServer(new PacketPartEngineSignal(powered.getEngineByNumber((byte) 0), PacketEngineTypes.ES_ON));
			}else{
				MTS.MTSNet.sendToServer(new PacketPartEngineSignal(powered.getEngineByNumber((byte) 0), PacketEngineTypes.ES_OFF));
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
		
		//Check if we pressed the trailer button.
		if(ControlsKeyboard.CAR_TRAILER.isPressed()){
			MTS.MTSNet.sendToServer(new TrailerPacket(powered.getEntityId()));
		}
	}
		
	public enum ControlsKeyboard{
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
		AIRCRAFT_GUN(Keyboard.KEY_SPACE, ControlsJoystick.AIRCRAFT_GUN, false),
		AIRCRAFT_ZOOM_I(Keyboard.KEY_PRIOR, ControlsJoystick.AIRCRAFT_ZOOM_I, true),
		AIRCRAFT_ZOOM_O(Keyboard.KEY_NEXT, ControlsJoystick.AIRCRAFT_ZOOM_O, true),
		AIRCRAFT_MOD(Keyboard.KEY_RSHIFT, ControlsJoystick.AIRCRAFT_MOD, false),
		
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
		CAR_LIGHTS_SPECIAL(Keyboard.KEY_V, ControlsJoystick.CAR_LIGHTS_SPECIAL, true),
		CAR_TRAILER(Keyboard.KEY_G, ControlsJoystick.CAR_TRAILER, true),
		CAR_GUN(Keyboard.KEY_SPACE, ControlsJoystick.CAR_GUN, false),
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
			return this.isMomentary ? getTrueButtonState(pressedKeyboardButtons, this, Keyboard.isKeyDown(this.button)) : Keyboard.isKeyDown(this.button);
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
		AIRCRAFT_BRAKE_ANALOG(true, false),
		AIRCRAFT_PANEL(false, true),
		AIRCRAFT_PARK(false, true),
		AIRCRAFT_GUN(false, false),
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
		AIRCRAFT_REVERSE(false, true),
		
		
		CAR_MOD(false, false),
		CAR_CAMLOCK(false, true),
		CAR_TURN(true, false),
		CAR_GAS(true, false),
		CAR_BRAKE(false, false),
		CAR_BRAKE_ANALOG(true, false),
		CAR_SHIFT_U(false, true),
		CAR_SHIFT_D(false, true),
		CAR_HORN(false, false),
		CAR_START(false, false),
		CAR_STOP(false, false),
		CAR_LIGHTS(false, true),
		CAR_LIGHTS_SPECIAL(false, true),
		CAR_PARK(false, true),
		CAR_TRAILER(false, true),
		CAR_GUN(false, false),
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
			return this.isMomentary ? getTrueButtonState(pressedJoystickButtons, this, getJoystickMultistateValue(this) > 0) : getJoystickMultistateValue(this) > 0;
		}
	}
	
	public enum ControlsKeyboardDynamic{
		AIRCRAFT_CHANGEHUD(ControlsKeyboard.AIRCRAFT_CAMLOCK, ControlsKeyboard.AIRCRAFT_MOD),
		AIRCRAFT_PARK(ControlsKeyboard.AIRCRAFT_BRAKE, ControlsKeyboard.AIRCRAFT_MOD),
		
		CAR_CHANGEHUD(ControlsKeyboard.CAR_CAMLOCK, ControlsKeyboard.CAR_MOD),
		CAR_PARK(ControlsKeyboard.CAR_BRAKE, ControlsKeyboard.CAR_MOD),
		CAR_STOP(ControlsKeyboard.CAR_START, ControlsKeyboard.CAR_MOD),
		CAR_SLOW(ControlsKeyboard.CAR_GAS, ControlsKeyboard.CAR_MOD),
		CAR_SIREN(ControlsKeyboard.CAR_LIGHTS_SPECIAL, ControlsKeyboard.CAR_MOD),
		CAR_TURNSIGNAL_R(ControlsKeyboard.CAR_TURN_R, ControlsKeyboard.CAR_LIGHTS),
		CAR_TURNSIGNAL_L(ControlsKeyboard.CAR_TURN_L, ControlsKeyboard.CAR_LIGHTS);
		
		
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
