package minecraftflightsimulator;

import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.entities.core.EntityBase;
import minecraftflightsimulator.entities.parts.EntityEngineLarge;
import minecraftflightsimulator.entities.parts.EntityEngineSmall;
import minecraftflightsimulator.entities.parts.EntityPlaneChest;
import minecraftflightsimulator.entities.parts.EntityPontoon;
import minecraftflightsimulator.entities.parts.EntityPontoonDummy;
import minecraftflightsimulator.entities.parts.EntityPropeller;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.entities.parts.EntitySkid;
import minecraftflightsimulator.entities.parts.EntityWheelLarge;
import minecraftflightsimulator.entities.parts.EntityWheelSmall;
import minecraftflightsimulator.modelrenders.RenderEngine;
import minecraftflightsimulator.modelrenders.RenderNull;
import minecraftflightsimulator.modelrenders.RenderPlaneChest;
import minecraftflightsimulator.modelrenders.RenderPontoon;
import minecraftflightsimulator.modelrenders.RenderPropeller;
import minecraftflightsimulator.modelrenders.RenderPropellerBench;
import minecraftflightsimulator.modelrenders.RenderSeat;
import minecraftflightsimulator.modelrenders.RenderSkid;
import minecraftflightsimulator.modelrenders.RenderWheel;
import minecraftflightsimulator.planes.MC172.EntityMC172;
import minecraftflightsimulator.planes.MC172.RenderMC172;
import minecraftflightsimulator.planes.PZLP11.EntityPZLP11;
import minecraftflightsimulator.planes.PZLP11.RenderPZLP11;
import minecraftflightsimulator.planes.Trimotor.EntityTrimotor;
import minecraftflightsimulator.planes.Trimotor.RenderTrimotor;
import minecraftflightsimulator.planes.Vulcanair.EntityVulcanair;
import minecraftflightsimulator.planes.Vulcanair.RenderVulcanair;
import minecraftflightsimulator.utilities.RenderHelper;

/**Client registry class.  Add any render that needs registering here,
 * and make sure to reference {@link RenderHelper} when calling registry methods.  
 * This allows the class to work with any MCVersion.
 * 
 * @author don_bruce
 */
public class MFSClientRegistry{
	public static final MFSClientRegistry instance = new MFSClientRegistry();
	
	public void init(){
		initEntityRenders();
		initTileEntityRenderers();
	}
	
	private void initTileEntityRenderers(){
		RenderHelper.registerTileEntityRender(TileEntityPropellerBench.class, RenderPropellerBench.class);
	}
	
	private void initEntityRenders(){
		//MinecraftForgeClient.registerItemRenderer(this.item, new ItemRender());
		
		RenderHelper.registerEntityRender(EntityMC172.class, RenderMC172.class);
		RenderHelper.registerEntityRender(EntityTrimotor.class, RenderTrimotor.class);
		RenderHelper.registerEntityRender(EntityVulcanair.class, RenderVulcanair.class);
		RenderHelper.registerEntityRender(EntityPZLP11.class, RenderPZLP11.class);
		
		RenderHelper.registerEntityRender(EntitySeat.class, RenderSeat.class);
		RenderHelper.registerEntityRender(EntityPlaneChest.class, RenderPlaneChest.class);
		RenderHelper.registerEntityRender(EntityWheelSmall.class, RenderWheel.class);
		RenderHelper.registerEntityRender(EntityWheelLarge.class, RenderWheel.class);
		RenderHelper.registerEntityRender(EntitySkid.class, RenderSkid.class);
		RenderHelper.registerEntityRender(EntityPontoon.class, RenderPontoon.class);
		RenderHelper.registerEntityRender(EntityPontoonDummy.class, RenderNull.class);
		RenderHelper.registerEntityRender(EntityPropeller.class, RenderPropeller.class);
		RenderHelper.registerEntityRender(EntityEngineSmall.class, RenderEngine.class);
		RenderHelper.registerEntityRender(EntityEngineLarge.class, RenderEngine.class);
		RenderHelper.registerEntityRender(EntityBase.class, RenderNull.class);
	}
}
