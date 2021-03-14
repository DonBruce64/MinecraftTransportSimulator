package minecrafttransportsimulator.rendering.components;

import java.util.List;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.mcinterface.BuilderEntity;
import minecrafttransportsimulator.mcinterface.BuilderTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for handling events pertaining to world rendering.  This handles the various calls for doing
 * rendering in the world.  It does not handle rendering of specific entities/players.  That's part of its
 * own interface. 
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceEventsWorldRendering{
	
	/**
	 *  World last event.  This occurs at the end of rendering in a special pass of -1.
	 *  We normally don't do anything here.  The exception is if an entity or Tile Entity
	 *  didn't get rendered.  In this case, we manually render it.  The rendering pipelines
	 *  of those methods are set up to handle this and will tread a -1 pass as a combined 0/1 pass.
	 */
    @SubscribeEvent
    public static void on(RenderWorldLastEvent event){
    	float partialTicks = event.getPartialTicks();
    	
    	//Enable lighting as pass -1 has that disabled.
    	RenderHelper.enableStandardItemLighting();
    	InterfaceRender.setLightingState(true);
    	
    	//Render pass 0 and 1 here manually.
    	for(int pass=0; pass<2; ++pass){
    		if(pass == 1){
    			InterfaceRender.setBlend(true);
    			GlStateManager.depthMask(false);
    		}
    	
	    	for(Entity entity : Minecraft.getMinecraft().world.loadedEntityList){
	            if(entity instanceof BuilderEntity){
	            	BuilderEntity builder = (BuilderEntity) entity;
	            	if(builder.entity != null && builder.entity instanceof AEntityC_Definable){
						if(builder.renderData.shouldRender()){
							AEntityC_Definable<?> internalEntity = ((AEntityC_Definable<?>) builder.entity);
							internalEntity.getRenderer().render(internalEntity, pass == 1, partialTicks);
						}
					}
	            }
	        }
	        
	        List<TileEntity> teList = Minecraft.getMinecraft().world.loadedTileEntityList; 
			for(int i=0; i<teList.size(); ++i){
				TileEntity tile = teList.get(i);
				if(tile instanceof BuilderTileEntity){
					BuilderTileEntity<?> builder = (BuilderTileEntity<?>) tile;
					if(builder.tileEntity != null && builder.renderData.shouldRender()){
						if(!builder.getWorld().isAirBlock(builder.getPos())){
							builder.tileEntity.getRenderer().render(builder.tileEntity, pass == 1, partialTicks);
						}
					}
				}
	        }
			
			if(pass == 1){
    			InterfaceRender.setBlend(false);
    			GlStateManager.depthMask(true);
    		}
    	}
		
		//Turn lighting back off.
		RenderHelper.disableStandardItemLighting();
		InterfaceRender.setLightingState(false);
    }
}
