package minecrafttransportsimulator.rendering.components;

import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;

/**Helper class used to handle rendering operations across multiple passes.  Tracks
 * rendering times and returns true or false to tell systems to render.  This is done
 * to allow us to use multiple render passes without double-rendering, and to detect
 * the Funny Business that shaders do.
*
* @author don_bruce
*/
public class RenderTickData{
	private final World world;
	private long[] lastTickPass = new long[]{0L, 0L, 0L};
	private boolean doneRenderingShaders;
	private static boolean shadersDetected;
	
	public RenderTickData(World world){
		this.world = world;
	}
	
	public boolean shouldRender(){
		//Get render pass.  Render data uses 2 for pass -1 as it uses arrays and arrays can't have a -1 index.
		int renderPass = MinecraftForgeClient.getRenderPass();
		if(renderPass == -1){
			renderPass = 2;
		}
		
		//We always render on pass 0 and 1, but we only render on pass 2 if we haven't rendered on pass 0 or 1.
		//If we are rendering on pass 0 or 1 a second time (before pass 2), it means shaders are present.
		//Set bit to detect these buggers and keep vehicles from rendering funny or disappearing.
		if(renderPass != 2 && lastTickPass[renderPass] > lastTickPass[2] && lastTickPass[2] > 0){
			shadersDetected = true;
		}
		
		//If we are rendering the object, update times.
		//This may not be the case if shaders are present and we haven't rendered the shader component.
		//Shaders do a pre-render to get their shadow, so the first render pass is actually invalid.
		if(!shadersDetected || doneRenderingShaders){
			//Rendering the actual model now.
			lastTickPass[renderPass] = world.getTotalWorldTime();
		}else if(shadersDetected && !doneRenderingShaders){
			//Rendering shader components.  If we're on pass 1, then shaders should be done rendering this cycle.
			if(renderPass == 1){
				doneRenderingShaders = true;
			}
		}
		
		if(renderPass == 2){
			//If we already rendered in pass 0, don't render now.
			//Note that shaders may do operations in pass 0 for lighting, but won't render the actual model.
			//In this case, the lastTickPass won't have been updated, so we do render here.
			//We also need to reset the shader render state variable to ensure we are ready for the next cycle.
			if(shadersDetected){
				doneRenderingShaders = false;
			}
			
			return lastTickPass[0] != lastTickPass[2];
		}else{
			return true;
		}
	}
}