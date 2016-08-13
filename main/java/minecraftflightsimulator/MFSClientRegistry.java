package minecraftflightsimulator;

import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.entities.core.EntityCore;
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
		
		RenderHelper.registerParentRender(EntityMC172.class, RenderMC172.class);
		RenderHelper.registerParentRender(EntityTrimotor.class, RenderTrimotor.class);
		RenderHelper.registerParentRender(EntityVulcanair.class, RenderVulcanair.class);
		RenderHelper.registerParentRender(EntityPZLP11.class, RenderPZLP11.class);
		
		RenderHelper.registerChildRender(EntitySeat.class, RenderSeat.class);
		RenderHelper.registerChildRender(EntityPlaneChest.class, RenderPlaneChest.class);
		RenderHelper.registerChildRender(EntityWheelSmall.class, RenderWheel.class);
		RenderHelper.registerChildRender(EntityWheelLarge.class, RenderWheel.class);
		RenderHelper.registerChildRender(EntitySkid.class, RenderSkid.class);
		RenderHelper.registerChildRender(EntityPontoon.class, RenderPontoon.class);
		RenderHelper.registerChildRender(EntityPontoonDummy.class, null);
		RenderHelper.registerChildRender(EntityPropeller.class, RenderPropeller.class);
		RenderHelper.registerChildRender(EntityEngineSmall.class, RenderEngine.class);
		RenderHelper.registerChildRender(EntityEngineLarge.class, RenderEngine.class);
		RenderHelper.registerChildRender(EntityCore.class, null);
	}
}
