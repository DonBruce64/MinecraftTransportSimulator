package minecrafttransportsimulator.mcinterface;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

/**Simple static class that's only purpose is to get the totalWorldTime on
 * the client and return a true/false based on a modulo operation.  Used
 * frequently for rendering when we need something to flash or operate
 * on a set cycle.  Not called clock because we aren't keeping time, we
 * are only keeping tempo. 
 * 
 * @author don_bruce
 */
public class MTSMetronome{
	private static World clientWorld;
	
	public static boolean isOn(int timePeriod, int ticksOn){
		if(clientWorld == null){
			clientWorld = Minecraft.getMinecraft().world;
		}
		return clientWorld.getTotalWorldTime()%timePeriod > ticksOn;
	}
	
	public static boolean isOnHalf(int timePeriod){
		return isOn(timePeriod, timePeriod/2);
	}
}
