package minecrafttransportsimulator.dataclasses;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
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
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.rendering.RenderMultipart;
import minecrafttransportsimulator.rendering.blockrenders.RenderPropellerBench;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class MTSRegistryClient{
	private static final MTSRegistryClient instance = new MTSRegistryClient();
	/**Map of parsed models keyed by name.*/
	public static final Map<String, Map<String, Float[][]>> modelMap = new HashMap<String, Map<String, Float[][]>>();

	public MTSRegistryClient(){
		initCustomResourceLocation();
	}
	
	public static void preInit(){
		loadCustomOBJModels();
		initTileEntityRenderers();
		initEntityRenders();
	}
	
	public static void init(){
		initItemRenders();
	}
	
	private static void initCustomResourceLocation(){
		String[] fieldNames = new String[]{"defaultResourcePacks", "field_110449_ao"}; 
		List<IResourcePack> resourcePacks = ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), fieldNames);
		resourcePacks.add(new MTSExternalResourcePack());
	}
	
	public static void loadCustomOBJModels(){
		for(String name : PackParserSystem.getRegisteredNames()){
			if(!modelMap.containsKey(name)){
				modelMap.put(name, OBJParserSystem.parseOBJModel(PackParserSystem.getPack(name).rendering.modelName));
			}
		}
	}
	
	private static void initTileEntityRenderers(){
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPropellerBench.class, new RenderPropellerBench());
	}
	
	private static void initEntityRenders(){
		RenderingRegistry.registerEntityRenderingHandler(EntityMultipartMoving.class, instance.new MTSRenderingFactory(RenderMultipart.class));
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
		registerItemRenderSeries(MTSRegistry.instrument, MTSInstruments.Instruments.values().length);
		registerItemRender(MTSRegistry.pointerShort);
		registerItemRender(MTSRegistry.pointerLong);
		registerItemRender(MTSRegistry.wrench);
		registerItemRender(MTSRegistry.key);
		registerItemRender(MTSRegistry.manual);
		registerItemRender(Item.getItemFromBlock(MTSRegistry.propellerBench));
				
		//Now register items for the pack data.
		try{
			//We manually create the JSON files, so get rid of what's in the directory first.
			File jsonDir = new File(MTS.assetDir + File.separator + "models" + File.separator + "item");
			for(File file : jsonDir.listFiles()){
				if(file.getName().endsWith(".json")){
					file.delete();
				}
			}
			//Now create the files and register the item renders.
			for(String name : MTSRegistry.multipartItemMap.keySet()){
				String uniqueItemName = PackParserSystem.getDefinitionForPack(name).uniqueName;
				FileWriter jsonWriter = new FileWriter(new File(jsonDir.getAbsolutePath() + File.separator + uniqueItemName + ".json"));
				jsonWriter.write("{\"parent\":\"mts:item/basic\",\"textures\":{\"layer0\": \"" + MTS.MODID + ":items/" + uniqueItemName + "\"}}");
				registerMultipartItemRender(name);
				jsonWriter.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//START OF ITEM REGISTRY HELPER CODE
	private static void registerItemRender(Item item){
	    Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, new ModelResourceLocation(MTS.MODID + ":" + item.getUnlocalizedName().substring(5).replace("block", ""), "inventory"));
	}

	private static void registerItemRenderSeries(Item item, int numberMetas){
		ModelResourceLocation[] models = new ModelResourceLocation[numberMetas];
		for(byte i=0; i<numberMetas; ++i){
			if(item.getUnlocalizedName(new ItemStack(item, 1, i)).equals(item.getUnlocalizedName())){
				models[i] = new ModelResourceLocation(MTS.MODID + ":" + item.getUnlocalizedName(new ItemStack(item, 1, i)).substring(5) + Integer.valueOf(i), "inventory");
			}else{
				models[i] = new ModelResourceLocation(MTS.MODID + ":" + item.getUnlocalizedName(new ItemStack(item, 1, i)).substring(5), "inventory");
			}
			
			Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, i, models[i]);
		}
		ModelBakery.registerItemVariants(item, models);
	}
	
	private static void registerMultipartItemRender(String name){
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(MTSRegistry.multipartItemMap.get(name), 0, new ModelResourceLocation(MTS.MODID + ":" + PackParserSystem.getDefinitionForPack(name).uniqueName, "inventory"));
	}
	
	private class MTSRenderingFactory implements IRenderFactory{
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
			return FileUtils.openInputStream(new File(MTS.assetDir + File.separatorChar + location.getResourcePath()));
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
