package minecrafttransportsimulator.dataclasses;

import java.lang.reflect.Field;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityDecor;
import minecrafttransportsimulator.blocks.core.TileEntityFuelPump;
import minecrafttransportsimulator.blocks.pole.TileEntityPoleSign;
import minecrafttransportsimulator.blocks.pole.TileEntityPoleWallConnector;
import minecrafttransportsimulator.items.core.ItemDecor;
import minecrafttransportsimulator.items.core.ItemInstrument;
import minecrafttransportsimulator.items.core.ItemVehicle;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.rendering.RenderVehicle;
import minecrafttransportsimulator.rendering.blockrenders.RenderDecor;
import minecrafttransportsimulator.rendering.blockrenders.RenderFuelPump;
import minecrafttransportsimulator.rendering.blockrenders.RenderPoleLighted;
import minecrafttransportsimulator.rendering.blockrenders.RenderPoleSign;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.Item;
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
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPoleWallConnector.class, new RenderPoleLighted());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPoleSign.class, new RenderPoleSign());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityDecor.class, new RenderDecor());
		
		//Register the vehicle rendering class.
		RenderingRegistry.registerEntityRenderingHandler(EntityVehicleE_Powered.class, MTSRenderFactory);
		
		//Register the item models.
		//First register the core items.
		for(Field field : MTSRegistry.class.getFields()){
			if(field.getType().equals(Item.class)){
				try{
					registerCoreItemRender((Item) field.get(null));
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		//Now register items for the packs.
		for(ItemVehicle vehicleItem : MTSRegistry.vehicleItemMap.values()){
			registerPackItemRender(vehicleItem, vehicleItem.vehicleName, "vehicles");
		}
		for(AItemPart partItem : MTSRegistry.partItemMap.values()){
			registerPackItemRender(partItem, partItem.partName, "parts");
		}
		for(ItemInstrument instrumentItem : MTSRegistry.instrumentItemMap.values()){
			registerPackItemRender(instrumentItem, instrumentItem.instrumentName, "instruments");
		}
		for(ItemDecor decorItem : MTSRegistry.decorItemMap.values()){
			registerPackItemRender(decorItem, decorItem.decorName, "decors");
		}
	}
	
	private static void registerCoreItemRender(Item item){
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(MTS.MODID + ":" + item.getRegistryName().getResourcePath(), "inventory"));
	}
	
	private static void registerPackItemRender(Item item, String wholeName, String renderFolder){
		String itemModID = wholeName.substring(0, wholeName.indexOf(':'));
		String itemName = wholeName.substring(wholeName.indexOf(':') + 1);
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(itemModID + ":" + renderFolder + "/" + itemName, "inventory"));
	}
	
	private static final IRenderFactory<EntityVehicleE_Powered> MTSRenderFactory = new IRenderFactory<EntityVehicleE_Powered>(){
		@Override
		public Render<? super EntityVehicleE_Powered> createRenderFor(RenderManager manager){
			return new RenderVehicle(manager);
		}
	};
}
