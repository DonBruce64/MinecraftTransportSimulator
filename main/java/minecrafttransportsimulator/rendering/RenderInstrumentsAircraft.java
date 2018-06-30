package minecrafttransportsimulator.rendering;

import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;
import minecrafttransportsimulator.sounds.StallSound;
import net.minecraft.client.Minecraft;

public final class RenderInstrumentsAircraft extends RenderInstruments{
	private static StallSound stallBuzzer;
	//TODO make this part of jets when we get them.
	private static void drawLiftReserveIndicator(EntityMultipartF_Plane plane, boolean lightsOn){
        //This indicator has a special function of sounding a buzzer if the track angle is too high (stall).
        if(plane.trackAngle<=-17){
        	if(stallBuzzer == null){
        		stallBuzzer = new StallSound();
        		Minecraft.getMinecraft().getSoundHandler().playSound(stallBuzzer);
        	}
        	stallBuzzer.setOn(plane);
        }else if(stallBuzzer != null){
        	stallBuzzer.setOff(plane);
        }
	}
}
