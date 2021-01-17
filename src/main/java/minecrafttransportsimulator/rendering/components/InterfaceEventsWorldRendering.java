package minecrafttransportsimulator.rendering.components;

import java.util.List;

import minecrafttransportsimulator.mcinterface.BuilderEntity;
import minecrafttransportsimulator.mcinterface.BuilderTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.Vec3d;
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
        for(Entity entity : Minecraft.getMinecraft().world.loadedEntityList){
            if(entity instanceof BuilderEntity){
            	Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(entity).doRender(entity, 0, 0, 0, 0, event.getPartialTicks());
            }
        }
        Entity renderViewEntity = Minecraft.getMinecraft().getRenderViewEntity();
		double playerX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * event.getPartialTicks();
		double playerY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * event.getPartialTicks();
		double playerZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * event.getPartialTicks();
        List<TileEntity> teList = Minecraft.getMinecraft().world.loadedTileEntityList; 
		for(int i=0; i<teList.size(); ++i){
			TileEntity tile = teList.get(i);
			if(tile instanceof BuilderTileEntity){
        		Vec3d delta = new Vec3d(tile.getPos()).add(-playerX, -playerY, -playerZ);
        		//Prevent crashing on corrupted TEs.
        		if(TileEntityRendererDispatcher.instance.getRenderer(tile) != null){
        			TileEntityRendererDispatcher.instance.getRenderer(tile).render(tile, delta.x, delta.y, delta.z, event.getPartialTicks(), 0, 0);
        		}
        	}
        }
    }
}
