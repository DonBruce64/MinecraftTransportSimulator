package minecraftflightsimulator;

import java.util.HashMap;
import java.util.Map;

import minecraftflightsimulator.blocks.TileEntityPropellerBench;
import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.core.EntityCore;
import minecraftflightsimulator.entities.core.EntityParent;
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
import minecraftflightsimulator.utilities.RenderHelper.RenderChild;
import minecraftflightsimulator.utilities.RenderHelper.RenderNull;
import minecraftflightsimulator.utilities.RenderHelper.RenderParent;
import minecraftflightsimulator.utilities.RenderHelper.RenderTileBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.tileentity.TileEntity;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class MFSClientRegistry{
	private static final MFSClientRegistry instance = new MFSClientRegistry();
	public static Map <Class<? extends EntityChild>, RenderChild> childRenderMap = new HashMap<Class<? extends EntityChild>, RenderChild>();

	public static void preInit(){
		/*INS189
		initTileEntityRenderers();
		initEntityRenders();
		INS189*/
	}
	
	public static void init(){
		initTileEntityRenderers();
		initEntityRenders();//INS189
		/*INS180
		initItemRenders();
		INS180*/
	}
	
	private static void initTileEntityRenderers(){
		registerTileEntityRender(TileEntityPropellerBench.class, RenderPropellerBench.class);
	}
	
	private static void initEntityRenders(){
		//MinecraftForgeClient.registerItemRenderer(this.item, new ItemRender());
		
		registerParentRender(EntityMC172.class, RenderMC172.class);
		registerParentRender(EntityTrimotor.class, RenderTrimotor.class);
		registerParentRender(EntityVulcanair.class, RenderVulcanair.class);
		registerParentRender(EntityPZLP11.class, RenderPZLP11.class);
		
		registerChildRender(EntitySeat.class, RenderSeat.class);
		registerChildRender(EntityPlaneChest.class, RenderPlaneChest.class);
		registerChildRender(EntityWheelSmall.class, RenderWheel.class);
		registerChildRender(EntityWheelLarge.class, RenderWheel.class);
		registerChildRender(EntitySkid.class, RenderSkid.class);
		registerChildRender(EntityPontoon.class, RenderPontoon.class);
		registerChildRender(EntityPontoonDummy.class, null);
		registerChildRender(EntityPropeller.class, RenderPropeller.class);
		registerChildRender(EntityEngineSmall.class, RenderEngine.class);
		registerChildRender(EntityEngineLarge.class, RenderEngine.class);
		registerChildRender(EntityCore.class, null);
	}
	
	/*INS180	
  private static void initItemRenders(){
		registerItemSeries(MFSRegistry.planeMC172, 6);
		registerItemSeries(MFSRegistry.planePZLP11, 1);
		registerItemSeries(MFSRegistry.planeVulcanair, 7);
		registerItemSeries(MFSRegistry.planeTrimotor, 15);
		registerItemSeries(MFSRegistry.seat, 96);
		registerItemSeries(MFSRegistry.flightInstrument, 15);
		registerItemSeriesWithAltName(MFSRegistry.engineSmall, Short.MAX_VALUE, "");
		registerItemSeriesWithAltName(MFSRegistry.engineLarge, Short.MAX_VALUE, "");
		
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
	}
	INS180*/
	
    
    /**
     * Helper method to register a parent rendering class.
     * @param entityClass
     * @param renderClass
     */
    public static void registerParentRender(Class<? extends EntityParent> entityClass, Class<? extends RenderParent> renderClass){
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
    public static void registerChildRender(Class<? extends EntityChild> entityClass, Class<? extends RenderChild> renderClass){
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
