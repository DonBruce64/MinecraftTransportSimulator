package minecraftflightsimulator.other;

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
	
	public static int modKey;
	public static int camLockKey;
	public static int pitchUpKey;
	public static int pitchDownKey;
	public static int rollLeftKey;
	public static int rollRightKey;
	public static int throttleUpKey;
	public static int throttleDownKey;
	public static int yawLeftKey;
	public static int yawRightKey;
	public static int flapsUpKey;
	public static int flapsDownKey;
	public static int brakeKey;
	public static int starterKey;
	public static int zoomInKey;
	public static int zoomOutKey;
	
	public static void controlCamera(){
		if(Keyboard.isKeyDown(camLockKey)){
			if(!camLockKeyPressed){
				if(!Keyboard.isKeyDown(modKey)){
					camLockKeyPressed=true;
					MFS.proxy.changeCameraLock();
				}
			}
		}else{
			camLockKeyPressed=false;
		}
		
		if(Keyboard.isKeyDown(zoomInKey)){
			if(!zoomInKeyPressed){
				zoomInKeyPressed=true;
				MFS.proxy.changeCameraZoom(-1);
			}
		}else{
			zoomInKeyPressed=false;
		}
		if(Keyboard.isKeyDown(zoomOutKey)){
			if(!zoomOutKeyPressed){
				zoomOutKeyPressed=true;
				MFS.proxy.changeCameraZoom(1);
			}
		}else{
			zoomOutKeyPressed=false;
		}	
	}
	
	public static void controlPlane(EntityPlane plane){
		checkBrakes(plane);
		checkStarter(plane);
		checkThrottle(plane);
		
		if(Keyboard.isKeyDown(rollLeftKey)){	
			MFS.MFSNet.sendToServer(new AileronPacket(plane.getEntityId(), (byte) -plane.aileronIncrement));
		}else if(Keyboard.isKeyDown(rollRightKey)){
			MFS.MFSNet.sendToServer(new AileronPacket(plane.getEntityId(), plane.aileronIncrement));
		}
		
		if(Keyboard.isKeyDown(pitchDownKey)){	
			MFS.MFSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), (byte) -plane.elevatorIncrement));
		}else if(Keyboard.isKeyDown(pitchUpKey)){
			MFS.MFSNet.sendToServer(new ElevatorPacket(plane.getEntityId(), plane.elevatorIncrement));
		}
		
		if(Keyboard.isKeyDown(yawLeftKey)){
			MFS.MFSNet.sendToServer(new RudderPacket(plane.getEntityId(), (byte) -plane.rudderIncrement));
		}else if(Keyboard.isKeyDown(yawRightKey)){
			MFS.MFSNet.sendToServer(new RudderPacket(plane.getEntityId(), plane.rudderIncrement));
		}
		
		if(plane.hasFlaps){
			if(Keyboard.isKeyDown(flapsUpKey)){
				if(!flapKeyPressed){
					MFS.MFSNet.sendToServer(new FlapPacket(plane.getEntityId(), (byte) -50));
					flapKeyPressed = true;
				}
			}else if(Keyboard.isKeyDown(flapsDownKey)){
				if(!flapKeyPressed){
					MFS.MFSNet.sendToServer(new FlapPacket(plane.getEntityId(), (byte) 50));
					flapKeyPressed = true;
				}
			}else{
				flapKeyPressed = false;
			}
		}
		
		if(Keyboard.isKeyDown(camLockKey)){
			if(!camLockKeyPressed){
				if(Keyboard.isKeyDown(modKey)){
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
		if(Keyboard.isKeyDown(brakeKey)){
			if(!brakeKeyPressed){
				brakeKeyPressed = true;
				if(Keyboard.isKeyDown(modKey)){
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
		if(Keyboard.isKeyDown(starterKey)){
			if(Keyboard.isKeyDown(modKey)){
				MFS.MFSNet.sendToServer(new EnginePacket(vehicle.getEntityId(), (byte) 0));
			}else{
				MFS.MFSNet.sendToServer(new EnginePacket(vehicle.getEntityId(), (byte) 1));
			}
		}
	}
	
	private static void checkThrottle(EntityParent vehicle){
		if(Keyboard.isKeyDown(throttleUpKey)){
			MFS.MFSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), (byte) 1));
		}else if(Keyboard.isKeyDown(throttleDownKey)){
			MFS.MFSNet.sendToServer(new ThrottlePacket(vehicle.getEntityId(), (byte) 0));
		}
	}
}
