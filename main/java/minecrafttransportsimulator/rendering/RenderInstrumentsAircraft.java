package minecrafttransportsimulator.rendering;

import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;
import minecrafttransportsimulator.sounds.StallSound;
import net.minecraft.client.Minecraft;

public final class RenderInstrumentsAircraft extends RenderInstruments{
	private static StallSound stallBuzzer;

	private static void drawGaugeBase(boolean lightsOn){
		if(lightsOn){
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
	    	renderSquareUV(60, 60, 0, 0.75F, 1F, 0.25F, 0F);
	    	Minecraft.getMinecraft().entityRenderer.disableLightmap();
		}else{
			renderSquareUV(60, 60, 0, 0.75F, 1F, 0.25F, 0F);
		}
    }
	
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
