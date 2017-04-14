package minecrafttransportsimulator.dataclasses;

import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.Loader;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.blocks.TileEntitySurveyFlag;
import minecrafttransportsimulator.blocks.TileEntityTrack;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityCore;
import minecrafttransportsimulator.entities.parts.EntityChest;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftLarge;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftSmall;
import minecrafttransportsimulator.entities.parts.EntityPontoon;
import minecrafttransportsimulator.entities.parts.EntityPropeller;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntitySkid;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.planes.Comanche.EntityComanche;
import minecrafttransportsimulator.planes.Comanche.RenderComanche;
import minecrafttransportsimulator.planes.MC172.EntityMC172;
import minecrafttransportsimulator.planes.MC172.RenderMC172;
import minecrafttransportsimulator.planes.PZLP11.EntityPZLP11;
import minecrafttransportsimulator.planes.PZLP11.RenderPZLP11;
import minecrafttransportsimulator.planes.Trimotor.EntityTrimotor;
import minecrafttransportsimulator.planes.Trimotor.RenderTrimotor;
import minecrafttransportsimulator.planes.Vulcanair.EntityVulcanair;
import minecrafttransportsimulator.planes.Vulcanair.RenderVulcanair;
import minecrafttransportsimulator.rendering.blockrenders.RenderPropellerBench;
import minecrafttransportsimulator.rendering.blockrenders.RenderSurveyFlag;
import minecrafttransportsimulator.rendering.blockrenders.RenderTrack;
import minecrafttransportsimulator.rendering.partrenders.RenderEngine;
import minecrafttransportsimulator.rendering.partrenders.RenderPlaneChest;
import minecrafttransportsimulator.rendering.partrenders.RenderPontoon;
import minecrafttransportsimulator.rendering.partrenders.RenderPropeller;
import minecrafttransportsimulator.rendering.partrenders.RenderSeat;
import minecrafttransportsimulator.rendering.partrenders.RenderSkid;
import minecrafttransportsimulator.rendering.partrenders.RenderWheel;
import minecrafttransportsimulator.systems.RenderSystem.RenderChild;
import minecrafttransportsimulator.systems.RenderSystem.RenderNull;
import minecrafttransportsimulator.systems.RenderSystem.RenderParent;
import minecrafttransportsimulator.systems.RenderSystem.RenderTileBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.tileentity.TileEntity;

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
		initItemRenders();
	}
	
	private static void initTileEntityRenderers(){
		registerTileEntityRender(TileEntityPropellerBench.class, RenderPropellerBench.class);
		registerTileEntityRender(TileEntitySurveyFlag.class, RenderSurveyFlag.class);
		registerTileEntityRender(TileEntityTrack.class, RenderTrack.class);
	}
	
	private static void initEntityRenders(){
		//MinecraftForgeClient.registerItemRenderer(this.item, new ItemRender());
		
		registerParentRender(EntityMC172.class, RenderMC172.class);
		registerParentRender(EntityTrimotor.class, RenderTrimotor.class);
		registerParentRender(EntityVulcanair.class, RenderVulcanair.class);
		registerParentRender(EntityPZLP11.class, RenderPZLP11.class);
		registerParentRender(EntityComanche.class, RenderComanche.class);
		
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
	
	
	//TODO make sure this works with 1.8 and up.
		
  private static void initItemRenders(){
	  	/*INS180
		registerItemSeries(MFSRegistry.planeMC172, 6);
		registerItemSeries(MFSRegistry.planePZLP11, 1);
		registerItemSeries(MFSRegistry.planeVulcanair, 7);
		registerItemSeries(MFSRegistry.planeTrimotor, 15);
		registerItemSeries(MFSRegistry.planeVans, 15);
		registerItemSeries(MFSRegistry.seat, 102);
		registerItemSeries(MFSRegistry.flightInstrument, InstrumentHelper.AircraftGauges.values().length);
		registerItemSeries(MFSRegistry.engine, EntityEngine.EngineTypes.values().length);
		
		ModelResourceLocation[] propellerNames = new ModelResourceLocation[Short.MAX_VALUE];
		for(int i=0; i<propellerNames.length; ++i){
			propellerNames[i] = registerItemRenderWithAltName(MFSRegistry.propeller, i, i%10 < 3 ? String.valueOf(i%10) : "0");
		}
		INS180*/
		/*INS189
		ModelBakery.registerItemVariants(MFSRegistry.propeller, propellerNames);
		INS189*/
		/*INS180
		String[] modelNames = new String[propellerNames.length]; for(int i=0; i<modelNames.length; ++i){modelNames[i]=propellerNames[i].getResourceDomain() + ":"+propellerNames[i].getResourcePath();};ModelBakery.addVariantName(MFSRegistry.propeller, modelNames);
		
		registerItemRender(MFSRegistry.wheelSmall);
		registerItemRender(MFSRegistry.wheelLarge);
		registerItemRender(MFSRegistry.skid);
		registerItemRender(MFSRegistry.pontoon);
		registerItemRender(MFSRegistry.pointerShort);
		registerItemRender(MFSRegistry.pointerLong);
		registerItemRender(MFSRegistry.flightInstrumentBase);
		registerItemRender(Item.getItemFromBlock(MFSRegistry.blockPropellerBench));
		INS180*/
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
	
	/*INS180
	private static void registerItemSeries(Item item, int metaNumber){
		ModelResourceLocation[] names = new ModelResourceLocation[metaNumber];
		for(int i=0; i<metaNumber; ++i){
			names[i] = registerItemRender(item, i);
		}
		INS180*/
		/*INS189
		ModelBakery.registerItemVariants(item, names);
		INS189*/
		/*INS180
		String[] modelNames = new String[names.length]; for(int i=0; i<modelNames.length; ++i){modelNames[i]=names[i].getResourceDomain() + ":"+names[i].getResourcePath();};ModelBakery.addVariantName(item, modelNames);
		
	}
	
	private static void registerItemSeriesWithAltName(Item item, int metaNumber, String altName){
		ModelResourceLocation[] names = new ModelResourceLocation[metaNumber];
		for(int i=0; i<metaNumber; ++i){
			names[i] = registerItemRenderWithAltName(item, i, altName);
		}
		INS180*/
		/*INS189
		ModelBakery.registerItemVariants(item, names);
		INS189*/
		/*INS180
		String[] modelNames = new String[names.length]; for(int i=0; i<modelNames.length; ++i){modelNames[i]=names[i].getResourceDomain() + ":"+names[i].getResourcePath();};ModelBakery.addVariantName(item, modelNames);
	}
	
	private static void registerItemRender(Item item){
	    Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, new ModelResourceLocation(MFS.MODID + ":" + item.getUnlocalizedName().substring(5), "inventory"));
	}
	
	private static ModelResourceLocation registerItemRender(Item item, int itemMeta){
		ModelResourceLocation location = new ModelResourceLocation(MFS.MODID + ":" + item.getUnlocalizedName().substring(5) + String.valueOf(itemMeta), "inventory");
	    Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, itemMeta, location);
	    return location;
	}
	
	private static ModelResourceLocation registerItemRenderWithAltName(Item item, int itemMeta, String altName){
		ModelResourceLocation location = new ModelResourceLocation(MFS.MODID + ":" + item.getUnlocalizedName().substring(5) + altName, "inventory");
	    Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, itemMeta, location);
	    return location;
	}
	INS180*/
    
    /*INS189
	private class MFSRenderingFactory implements IRenderFactory{
		private final Class<? extends Render> entityRender;
		public MFSRenderingFactory(Class<? extends Render>  entityRender){
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
	INS189*/
}
