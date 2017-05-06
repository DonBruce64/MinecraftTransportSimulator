package minecrafttransportsimulator.dataclasses;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.blocks.TileEntitySurveyFlag;
import minecrafttransportsimulator.blocks.TileEntityTrack;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.parts.EntityChest;
import minecrafttransportsimulator.entities.parts.EntityEngine;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftLarge;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftSmall;
import minecrafttransportsimulator.entities.parts.EntityPontoon;
import minecrafttransportsimulator.entities.parts.EntityPropeller;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntitySkid;
import minecrafttransportsimulator.entities.parts.EntityWheel;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;

public class MTSRegistryClient{
	private static final MTSRegistryClient instance = new MTSRegistryClient();
	/**Maps children to render classes.*/
	public static final Map <Class<? extends EntityMultipartChild>, Class<? extends RenderChild>> childRenderMap = new HashMap<Class<? extends EntityMultipartChild>, Class<? extends RenderChild>>();

	public static void preInit(){
		initTileEntityRenderers();
		initEntityRenders();
	}
	
	public static void init(){
		initItemRenders();
	}
	
	private static void initTileEntityRenderers(){
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPropellerBench.class, new RenderPropellerBench());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySurveyFlag.class, new RenderSurveyFlag());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTrack.class, new RenderTrack());
	}
	
	private static void initEntityRenders(){
		//MinecraftForgeClient.registerItemRenderer(this.item, new ItemRender());
		//TODO Should init a master multipart render here.
		
		childRenderMap.put(EntitySeat.class, RenderSeat.class);
		childRenderMap.put(EntityChest.class, RenderPlaneChest.class);
		childRenderMap.put(EntityWheel.EntityWheelSmall.class, RenderWheel.class);
		childRenderMap.put(EntityWheel.EntityWheelLarge.class, RenderWheel.class);
		childRenderMap.put(EntitySkid.class, RenderSkid.class);
		childRenderMap.put(EntityPontoon.class, RenderPontoon.class);
		childRenderMap.put(EntityPontoon.EntityPontoonDummy.class, null);
		childRenderMap.put(EntityPropeller.class, RenderPropeller.class);
		childRenderMap.put(EntityEngineAircraftSmall.class, RenderEngine.class);
		childRenderMap.put(EntityEngineAircraftLarge.class, RenderEngine.class);
	}


	private static void initItemRenders(){
		//TODO no earthly idea what to put here....
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
