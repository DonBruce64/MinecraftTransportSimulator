package minecrafttransportsimulator.dataclasses;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityFuelPump;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.items.core.ItemMultipart;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.rendering.RenderMultipart;
import minecrafttransportsimulator.rendering.blockrenders.RenderFuelPump;
import minecrafttransportsimulator.rendering.blockrenders.RenderPropellerBench;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
	
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event){
		//Register the TESRs for blocks.
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPropellerBench.class, new RenderPropellerBench());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFuelPump.class, new RenderFuelPump());
		
		//Register the multipart rendering class.
		RenderingRegistry.registerEntityRenderingHandler(EntityMultipartD_Moving.class, MTSRenderFactory);
		
		//Register the item models.
		//First register the core items.
		registerCoreItemRender(MTSRegistry.manual);
		registerCoreItemRender(MTSRegistry.wrench);
		registerCoreItemRender(MTSRegistry.key);
		registerCoreItemRender(MTSRegistry.itemBlockPropellerBench);
		registerCoreItemRender(MTSRegistry.itemBlockFuelPump);
		registerCoreItemRender(MTSRegistry.pointerShort);
		registerCoreItemRender(MTSRegistry.pointerLong);
		registerCoreItemRenderSeries(MTSRegistry.instrument, MTSInstruments.Instruments.values().length);
				
		//Now register items for the packs.
		for(ItemMultipart multipartItem : MTSRegistry.multipartItemMap.values()){
			registerMultipartItemRender(multipartItem);
		}
		for(AItemPart partItem : MTSRegistry.partItemMap.values()){
			registerPartItemRender(partItem);
		}
	}
	
	private static void registerCoreItemRender(Item item){
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(MTS.MODID + ":" + item.getRegistryName().getResourcePath(), "inventory"));
	}

	private static void registerCoreItemRenderSeries(Item item, int numberMetas){
		for(byte i=0; i<numberMetas; ++i){
			ModelResourceLocation model = new ModelResourceLocation(MTS.MODID + ":" + item.getUnlocalizedName(new ItemStack(item, 1, i)).substring(5), "inventory");
			ModelLoader.setCustomModelResourceLocation(item, i, model);
		}
	}
	
	private static void registerMultipartItemRender(ItemMultipart item){
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.multipartName, "inventory"));
	}
	
	private static void registerPartItemRender(AItemPart item){
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.partName, "inventory"));
	}
	
	private static final IRenderFactory<EntityMultipartD_Moving> MTSRenderFactory = new IRenderFactory<EntityMultipartD_Moving>(){
		@Override
		public Render<? super EntityMultipartD_Moving> createRenderFor(RenderManager manager){
			return new RenderMultipart(manager);
		}
	};
}
