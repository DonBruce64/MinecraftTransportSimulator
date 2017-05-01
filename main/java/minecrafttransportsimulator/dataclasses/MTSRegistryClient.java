package minecrafttransportsimulator.dataclasses;

import minecrafttransportsimulator.MTS;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Loader;

import java.util.HashMap;
import java.util.Map;

public class MTSRegistryClient{
	private static final MTSRegistryClient instance = new MTSRegistryClient();
	public static Map <Class<? extends EntityMultipartChild>, RenderChild> childRenderMap = new HashMap<Class<? extends EntityMultipartChild>, RenderChild>();

	public static void preInit(){
		initTileEntityRenderers();
		initEntityRenders();
	}
	
	public static void init(){
		initItemRenders();
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


	private static void initItemRenders(){
		registerItemSeries(MTSRegistry.planeMC172, 6);
		registerItemSeries(MTSRegistry.planePZLP11, 1);
		registerItemSeries(MTSRegistry.planeVulcanair, 7);
		registerItemSeries(MTSRegistry.planeTrimotor, 15);
		registerItemSeries(MTSRegistry.planeVans, 15);
		registerItemSeries(MTSRegistry.seat, 102);
		registerItemSeries(MTSRegistry.flightInstrument, InstrumentHelper.AircraftGauges.values().length);
		registerItemSeries(MTSRegistry.engine, EntityEngine.EngineTypes.values().length);

		ModelResourceLocation[] propellerNames = new ModelResourceLocation[Short.MAX_VALUE];
		for(int i=0; i<propellerNames.length; ++i){
			propellerNames[i] = registerItemRenderWithAltName(MTSRegistry.propeller, i, i%10 < 3 ? String.valueOf(i%10) : "0");
		}
		ModelBakery.registerItemVariants(MTSRegistry.propeller, propellerNames);
		String[] modelNames = new String[propellerNames.length]; for(int i=0; i<modelNames.length; ++i){modelNames[i]=propellerNames[i].getResourceDomain() + ":"+propellerNames[i].getResourcePath();};ModelBakery.addVariantName(MTSRegistry.propeller, modelNames);

		registerItemRender(MTSRegistry.wheelSmall);
		registerItemRender(MTSRegistry.wheelLarge);
		registerItemRender(MTSRegistry.skid);
		registerItemRender(MTSRegistry.pontoon);
		registerItemRender(MTSRegistry.pointerShort);
		registerItemRender(MTSRegistry.pointerLong);
		registerItemRender(MTSRegistry.flightInstrumentBase);
		registerItemRender(Item.getItemFromBlock(MTSRegistry.blockPropellerBench));
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

	private static void registerItemSeries(Item item, int metaNumber){
		ModelResourceLocation[] names = new ModelResourceLocation[metaNumber];
		for(int i=0; i<metaNumber; ++i){
			names[i] = registerItemRender(item, i);
		}
		ModelBakery.registerItemVariants(item, names);
		String[] modelNames = new String[names.length]; for(int i=0; i<modelNames.length; ++i){modelNames[i]=names[i].getResourceDomain() + ":"+names[i].getResourcePath();};ModelBakery.addVariantName(item, modelNames);

	}

	private static void registerItemSeriesWithAltName(Item item, int metaNumber, String altName){
		ModelResourceLocation[] names = new ModelResourceLocation[metaNumber];
		for(int i=0; i<metaNumber; ++i){
			names[i] = registerItemRenderWithAltName(item, i, altName);
		}
		ModelBakery.registerItemVariants(item, names);
		String[] modelNames = new String[names.length]; for(int i=0; i<modelNames.length; ++i){modelNames[i]=names[i].getResourceDomain() + ":"+names[i].getResourcePath();};ModelBakery.addVariantName(item, modelNames);
	}

	private static void registerItemRender(Item item){
	    Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, new ModelResourceLocation(MFS.MODID + ":" + item.getUnlocalizedName().substring(5), "inventory"));
	}

	private static ModelResourceLocation registerItemRender(Item item, int itemMeta){
		ModelResourceLocation location = new ModelResourceLocation(MTS.MODID + ":" + item.getUnlocalizedName().substring(5) + String.valueOf(itemMeta), "inventory");
	    Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, itemMeta, location);
	    return location;
	}

	private static ModelResourceLocation registerItemRenderWithAltName(Item item, int itemMeta, String altName){
		ModelResourceLocation location = new ModelResourceLocation(MTS.MODID + ":" + item.getUnlocalizedName().substring(5) + altName, "inventory");
	    Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, itemMeta, location);
	    return location;
	}

	private class MTSRenderingFactory implements IRenderFactory {
		private final Class<? extends Render> entityRender;
		public MTSRenderingFactory(Class<? extends Render>  entityRender){
			this.entityRender = entityRender;
		}

		@Override
		public Render createRenderFor(RenderManager manager){
			try{
				return entityRender.getConstructor(RenderManager.class).newInstance(manager);
			}catch(Exception e){
				e.printStackTrace();
				return null;
			}
		}
	};
}
