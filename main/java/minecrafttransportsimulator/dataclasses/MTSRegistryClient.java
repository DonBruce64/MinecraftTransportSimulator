package minecrafttransportsimulator.dataclasses;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Sets;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.blocks.TileEntitySurveyFlag;
import minecrafttransportsimulator.blocks.TileEntityTrack;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.parts.EntityChest;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftLarge;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftSmall;
import minecrafttransportsimulator.entities.parts.EntityPontoon;
import minecrafttransportsimulator.entities.parts.EntityPropeller;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntitySkid;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.rendering.AircraftInstruments;
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
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class MTSRegistryClient{
	private static final MTSRegistryClient instance = new MTSRegistryClient();
	/**Maps children to render classes.*/
	public static final Map <Class<? extends EntityMultipartChild>, Class<? extends RenderChild>> childRenderMap = new HashMap<Class<? extends EntityMultipartChild>, Class<? extends RenderChild>>();

	public static void preInit(){
		initCustomResourceLocation();
		initTileEntityRenderers();
		initEntityRenders();
	}
	
	public static void init(){
		initItemRenders();
	}
	
	private static void initCustomResourceLocation(){
		String[] fieldNames = new String[]{"defaultResourcePacks", "field_110449_ao"}; 
		List<IResourcePack> resourcePacks = ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), fieldNames);
		resourcePacks.add(instance.new ExteralResourcePack());
	}
	
	private static void initTileEntityRenderers(){
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPropellerBench.class, new RenderPropellerBench());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySurveyFlag.class, new RenderSurveyFlag());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTrack.class, new RenderTrack());
	}
	
	private static void initEntityRenders(){		
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
		registerItemRender(MTSRegistry.wheelSmall);
		registerItemRender(MTSRegistry.wheelLarge);
		registerItemRender(MTSRegistry.skid);
		registerItemRender(MTSRegistry.pontoon);
		registerItemRender(MTSRegistry.engineAircraftSmall);
		registerItemRender(MTSRegistry.engineAircraftLarge);
		registerItemRenderSeries(MTSRegistry.propeller, 3);
		registerItemRenderSeries(MTSRegistry.seat, 102);
		registerItemRenderSeries(MTSRegistry.flightInstrument, AircraftInstruments.AircraftGauges.values().length);
		registerItemRender(MTSRegistry.pointerShort);
		registerItemRender(MTSRegistry.pointerLong);
		registerItemRender(MTSRegistry.wrench);
		registerItemRender(MTSRegistry.flightManual);
		registerItemRender(Item.getItemFromBlock(MTSRegistry.propellerBench));
		
		registerItemRender(MTSRegistry.track);
		registerItemRender(MTSRegistry.bogie);
		registerItemRender(Item.getItemFromBlock(MTSRegistry.blockTrack));
		registerItemRender(Item.getItemFromBlock(MTSRegistry.surveyFlag));
	}
	
	//START OF ITEM REGISTRY HELPER CODE
	private static void registerItemRender(Item item){
	    Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, new ModelResourceLocation(MTS.MODID + ":" + item.getUnlocalizedName().substring(5), "inventory"));
	}

	private static void registerItemRenderSeries(Item item, int numberMetas){
		ModelResourceLocation[] models = new ModelResourceLocation[numberMetas];
		for(byte i=0; i<numberMetas; ++i){
			models[i] = new ModelResourceLocation(MTS.MODID + ":" + item.getUnlocalizedName().substring(5) + String.valueOf(i), "inventory");
			Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, i, models[i]);
		}
		ModelBakery.registerItemVariants(item, models);
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
	
	private class ExteralResourcePack implements IResourcePack{
		@Override
		public InputStream getInputStream(ResourceLocation location) throws IOException{
			return FileUtils.openInputStream(new File(MTS.assetDir + File.pathSeparator + location.getResourcePath()));
		}

		@Override
		public boolean resourceExists(ResourceLocation location){
			return new File(MTS.assetDir, location.getResourcePath()).exists();
		}

		@Override
		public Set<String> getResourceDomains() {
			return Sets.newHashSet(MTS.MODID);
		}

		@Override
		public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException{
			return null;
		}

		@Override
		public BufferedImage getPackImage() throws IOException{
			return null;
		}

		@Override
		public String getPackName(){
			return MTS.MODID;
		}
	}
}
