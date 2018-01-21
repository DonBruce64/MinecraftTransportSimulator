package minecrafttransportsimulator.dataclasses;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityFuelPump;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.rendering.RenderMultipart;
import minecrafttransportsimulator.rendering.blockrenders.RenderFuelPump;
import minecrafttransportsimulator.rendering.blockrenders.RenderPropellerBench;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Sub-class for client-only registration.  Used for models, item texture, entity rendering and the like.
 * Registrations are fired on events, so should be good for the next few MC versions.
 * 
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
public final class MTSRegistryClient{
	private static final MTSRegistryClient instance = new MTSRegistryClient();
	/**Map of parsed models keyed by name.*/
	public static final Map<String, Map<String, Float[][]>> modelMap = new HashMap<String, Map<String, Float[][]>>();

	public static void loadCustomOBJModels(){
		modelMap.put("fuelpump", OBJParserSystem.parseOBJModel(new ResourceLocation(MTS.MODID, "objmodels/fuelpump.obj")));
		modelMap.put("enginecarsmall", OBJParserSystem.parseOBJModel(new ResourceLocation(MTS.MODID, "objmodels/enginecarsmall.obj")));
		for(String name : PackParserSystem.getRegisteredNames()){
			modelMap.put(name, OBJParserSystem.parseOBJModel(new ResourceLocation(MTS.MODID, "objmodels/" + PackParserSystem.getPack(name).rendering.modelName)));
		}
	}
	
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event){
		MTSExternalResourcePack.init();
		
		//Load the OBJ models.
		loadCustomOBJModels();
		
		//Register the TESRs.
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPropellerBench.class, new RenderPropellerBench());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFuelPump.class, new RenderFuelPump());
		
		//Register the Entity rendering classes.
		RenderingRegistry.registerEntityRenderingHandler(EntityMultipartMoving.class, instance.new MTSRenderingFactory(RenderMultipart.class));
		
		//Register the item models.
		registerItemRender(MTSRegistry.wheelSmall);
		registerItemRender(MTSRegistry.wheelLarge);
		registerItemRender(MTSRegistry.skid);
		registerItemRender(MTSRegistry.pontoon);
		registerItemRender(MTSRegistry.engineCarSmall);
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
				ModelLoader.setCustomModelResourceLocation(MTSRegistry.multipartItemMap.get(name), 0, new ModelResourceLocation(MTS.MODID + ":" + PackParserSystem.getDefinitionForPack(name).uniqueName, "inventory"));
				jsonWriter.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private static void registerItemRender(Item item){
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(MTS.MODID + ":" + item.getRegistryName().getResourcePath(), "inventory"));
	}

	private static void registerItemRenderSeries(Item item, int numberMetas){
		for(byte i=0; i<numberMetas; ++i){
			//If items are called the same thing no matter the metadata, then they should simply gain a numerical suffix.
			//Otherwise take their unlocalized name and use it for the JSON name.
			//Example: Seats would fall into the first category, but flight instruments would fall into the second.
			ModelResourceLocation model;
			if(item.getUnlocalizedName(new ItemStack(item, 1, i)).equals(item.getUnlocalizedName())){
				model = new ModelResourceLocation(MTS.MODID + ":" + item.getUnlocalizedName(new ItemStack(item, 1, i)).substring(5) + Integer.valueOf(i), "inventory");
			}else{
				model = new ModelResourceLocation(MTS.MODID + ":" + item.getUnlocalizedName(new ItemStack(item, 1, i)).substring(5), "inventory");
			}
			
			ModelLoader.setCustomModelResourceLocation(item, i, model);
		}
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
}
