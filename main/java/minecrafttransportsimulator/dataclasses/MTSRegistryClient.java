package minecrafttransportsimulator.dataclasses;

import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.blocks.TileEntitySurveyFlag;
import minecrafttransportsimulator.blocks.TileEntityTrack;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityCore;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.entities.parts.*;
import minecrafttransportsimulator.rendering.blockrenders.RenderPropellerBench;
import minecrafttransportsimulator.rendering.blockrenders.RenderSurveyFlag;
import minecrafttransportsimulator.rendering.blockrenders.RenderTrack;
import minecrafttransportsimulator.rendering.partrenders.*;
import minecrafttransportsimulator.systems.RenderSystem.RenderChild;
import minecrafttransportsimulator.systems.RenderSystem.RenderNull;
import minecrafttransportsimulator.systems.RenderSystem.RenderParent;
import minecrafttransportsimulator.systems.RenderSystem.RenderTileBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Loader;

import java.util.HashMap;
import java.util.Map;

public class MTSRegistryClient{
	private static final MTSRegistryClient instance = new MTSRegistryClient();
	public static Map <Class<? extends EntityMultipartChild>, RenderChild> childRenderMap = new HashMap<Class<? extends EntityMultipartChild>, RenderChild>();

	public static void preInit(){
		if(!(Loader.MC_VERSION.equals("1.7.10") || Loader.MC_VERSION.equals("1.8"))){
			initTileEntityRenderers();
			initEntityRenders();
		}
	}
	
	public static void init(){
		if(Loader.MC_VERSION.equals("1.7.10") || Loader.MC_VERSION.equals("1.8")){
			initTileEntityRenderers();
			initEntityRenders();
		}
	}
	
	private static void initTileEntityRenderers(){
		registerTileEntityRender(TileEntityPropellerBench.class, RenderPropellerBench.class);
		registerTileEntityRender(TileEntitySurveyFlag.class, RenderSurveyFlag.class);
		registerTileEntityRender(TileEntityTrack.class, RenderTrack.class);
	}
	
	private static void initEntityRenders(){
		//MinecraftForgeClient.registerItemRenderer(this.item, new ItemRender());
		registerParentRender(EntityPlane.class, RenderPlane.class);
		
		registerChildRender(EntitySeat.class, RenderSeat.class);
		registerChildRender(EntityChest.class, RenderPlaneChest.class);
		registerChildRender(EntityWheel.EntityWheelSmall.class, RenderWheel.class);
		registerChildRender(EntityWheel.EntityWheelLarge.class, RenderWheel.class);
		registerChildRender(EntitySkid.class, RenderSkid.class);
		registerChildRender(EntityPontoon.class, RenderPontoon.class);
		registerChildRender(EntityPontoon.EntityPontoonDummy.class, null);
		registerChildRender(EntityPropeller.class, RenderPropeller.class);
		registerChildRender(EntityEngineAircraftSmall.class, RenderEngine.class);
		registerChildRender(EntityEngineAircraftLarge.class, RenderEngine.class);
		registerChildRender(EntityCore.class, null);
	}
	
    
    /**
     * Helper method to register a parent rendering class.
     * @param entityClass
     * @param renderClass
     */
    public static void registerParentRender(Class<? extends EntityMultipartParent> entityClass, Class<? extends RenderParent> renderClass){
		try{
			RenderingRegistry.registerEntityRenderingHandler(entityClass, renderClass.getConstructor(RenderManager.class).newInstance((Object) null));
		}catch(Exception e){
			System.err.println("ERROR: Could not register Parent renderer.  Entity will not be visible!");
		}
	}
    
    /**
     * Registers a child part with child rendering system.  All child parts registered
     * in here will be rendered directly after their parents, ensuring correct placement.
     * renderClass may be null to prevent rendering altogether.
     * @param entityClass
     * @param renderClass
     */
    public static void registerChildRender(Class<? extends EntityMultipartChild> entityClass, Class<? extends RenderChild> renderClass){
    	try{
    		RenderingRegistry.registerEntityRenderingHandler(entityClass, RenderNull.class.getConstructor(RenderManager.class).newInstance((Object) null));
    		if(renderClass != null){
    			childRenderMap.put(entityClass, renderClass.newInstance());
    		}
		}catch(Exception e){
			System.err.println("ERROR: Could not register Child renderer.  Model will not be visible!");
		}	
    }
    
    public static void registerTileEntityRender(Class<? extends TileEntity> tileEntityClass, Class<? extends RenderTileBase> renderClass){
		try{
			ClientRegistry.bindTileEntitySpecialRenderer(tileEntityClass, renderClass.newInstance());
		}catch(Exception e){
			System.err.println("ERROR: Could not register TileEntity renderer.  TileEntity will not be visible!");
		}
	}
}
