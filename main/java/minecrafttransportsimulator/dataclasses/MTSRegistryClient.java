package minecrafttransportsimulator.dataclasses;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityFuelPump;
import minecrafttransportsimulator.blocks.decor.TileEntityDecor6AxisOriented;
import minecrafttransportsimulator.items.core.ItemMultipart;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartD_Moving;
import minecrafttransportsimulator.rendering.RenderMultipart;
import minecrafttransportsimulator.rendering.blockrenders.RenderDecor;
import minecrafttransportsimulator.rendering.blockrenders.RenderFuelPump;
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
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFuelPump.class, new RenderFuelPump());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityDecor6AxisOriented.class, new RenderDecor());
		
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
		registerCoreItemRender(MTSRegistry.itemBlockPole);
		registerCoreItemRender(MTSRegistry.itemBlockPoleBase);
		registerCoreItemRender(MTSRegistry.itemBlockTrafficCone);
		registerCoreItemRender(MTSRegistry.itemBlockCrashBarrier);
		registerCoreItemRender(MTSRegistry.itemBlockTrafficSignal);
		registerCoreItemRender(MTSRegistry.itemBlockStreetLight);
				
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
		String itemModID = item.multipartName.substring(0, item.multipartName.indexOf(':'));
		String itemName = item.multipartName.substring(item.multipartName.indexOf(':') + 1);
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(itemModID + ":vehicles/" + itemName, "inventory"));
	}
	
	private static void registerPartItemRender(AItemPart item){
		String itemModID = item.partName.substring(0, item.partName.indexOf(':'));
		String itemName = item.partName.substring(item.partName.indexOf(':') + 1);
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(itemModID + ":parts/" + itemName, "inventory"));
	}
	
	private static final IRenderFactory<EntityMultipartD_Moving> MTSRenderFactory = new IRenderFactory<EntityMultipartD_Moving>(){
		@Override
		public Render<? super EntityMultipartD_Moving> createRenderFor(RenderManager manager){
			return new RenderMultipart(manager);
		}
	};
}
