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
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class MFSClientRegistry{
	public static final MFSClientRegistry instance = new MFSClientRegistry();
	
	public void preInit(){
		//versions 1.8.9+
		//initEntityRenders();
	}
	
	public void init(){
		//versions 1.7.10 and versions 1.8;
		initEntityRenders();
	}
	
	private void initEntityRenders(){
		//MinecraftForgeClient.registerItemRenderer(this.item, new ItemRender());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPropellerBench.class, new RenderPropellerBench());
		
		RenderingRegistry.registerEntityRenderingHandler(EntityMC172.class, new RenderMC172(null));
		RenderingRegistry.registerEntityRenderingHandler(EntityTrimotor.class, new RenderTrimotor(null));
		RenderingRegistry.registerEntityRenderingHandler(EntityVulcanair.class, new RenderVulcanair(null));
		RenderingRegistry.registerEntityRenderingHandler(EntityPZLP11.class, new RenderPZLP11(null));
		
		RenderingRegistry.registerEntityRenderingHandler(EntitySeat.class, new RenderSeat(null));
		RenderingRegistry.registerEntityRenderingHandler(EntityPlaneChest.class, new RenderPlaneChest(null));
		RenderingRegistry.registerEntityRenderingHandler(EntityWheelSmall.class, new RenderWheel(null));
		RenderingRegistry.registerEntityRenderingHandler(EntityWheelLarge.class, new RenderWheel(null));
		RenderingRegistry.registerEntityRenderingHandler(EntitySkid.class, new RenderSkid(null));
		RenderingRegistry.registerEntityRenderingHandler(EntityPontoon.class, new RenderPontoon(null));
		RenderingRegistry.registerEntityRenderingHandler(EntityPontoonDummy.class, new RenderNull(null));
		RenderingRegistry.registerEntityRenderingHandler(EntityPropeller.class, new RenderPropeller(null));
		RenderingRegistry.registerEntityRenderingHandler(EntityEngineSmall.class, new RenderEngine(null));
		RenderingRegistry.registerEntityRenderingHandler(EntityEngineLarge.class, new RenderEngine(null));
		RenderingRegistry.registerEntityRenderingHandler(EntityBase.class, new RenderNull(null));
	}
}
