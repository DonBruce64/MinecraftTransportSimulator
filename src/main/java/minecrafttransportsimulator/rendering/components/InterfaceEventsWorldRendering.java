package minecrafttransportsimulator.rendering.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.BuilderEntity;
import minecrafttransportsimulator.mcinterface.BuilderTileEntity;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityPlayerGun;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartSeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for handling events pertaining to world rendering.  This handles the various calls for player/entity
 * rendering, as well as block rendering, if required.  Note that this does NOT affect modifications to the rendered
 * world, such as cameras or overlays.  These methods are only for what is rendered and how we render it. 
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceEventsWorldRendering{
	private static final Map<EntityPlayer, ItemStack> tempHeldItemStorage = new HashMap<EntityPlayer, ItemStack>();
	
	 /**
     * Pre-post methods for adjusting player angles while seated.
     * This adjusts the player's model to move with the seat/vehicle.
     */
    @SubscribeEvent
    public static void on(RenderPlayerEvent.Pre event){
    	EntityPlayer renderedPlayer = event.getEntityPlayer();
    	
    	//If we are holding a gun, disable the third-person item icon.
    	//We can't use the setHeldItem hand as it plays the equip sound, so we use slots instead.
    	EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(renderedPlayer.getUniqueID().toString());
    	if(entity != null && entity.gun != null){
    		tempHeldItemStorage.put(renderedPlayer, renderedPlayer.getHeldItemMainhand());
    		renderedPlayer.inventory.mainInventory.set(renderedPlayer.inventory.currentItem, ItemStack.EMPTY);
    	}
    	
    	//If we are riding an entity, adjust seating.
    	if(renderedPlayer.getRidingEntity() instanceof BuilderEntity){
        	AEntityBase ridingEntity = ((BuilderEntity) renderedPlayer.getRidingEntity()).entity;
        	GL11.glPushMatrix();
        	if(ridingEntity != null){
        		//Get total angles for the entity the player is riding.
        		Point3d entityAngles = ridingEntity.angles.copy();
        		Point3d ridingAngles = new Point3d(0, 0, 0);
	            if(ridingEntity instanceof EntityVehicleF_Physics){
	            	for(WrapperEntity rider : ridingEntity.locationRiderMap.values()){
						if(Minecraft.getMinecraft().player.equals(rider.entity)){
							PartSeat seat = (PartSeat) ((EntityVehicleF_Physics) ridingEntity).getPartAtLocation(ridingEntity.locationRiderMap.inverse().get(rider));
							ridingAngles = seat.placementRotation.copy().add(seat.getPositionRotation(event.getPartialRenderTick()));
		            		if(seat.parentPart != null){
		            			ridingAngles.add(seat.parentPart.placementRotation).add(seat.parentPart.getPositionRotation(event.getPartialRenderTick()));
			            	}
						}
	            	}
	            }
	            
        		//Set the player yaw offset to 0.  This is needed as we are rotating the player manually.
	            renderedPlayer.renderYawOffset = 0;
	            
	            //Set the player's head yaw to the delta between their yaw and their angled yaw.
	            renderedPlayer.rotationYawHead = (float) (renderedPlayer.rotationYaw + entityAngles.y + ridingAngles.y);
	            
	            //Now add the rotations.
	            //We have to do this via OpenGL, as changing the player's pitch doesn't make them tilt in the seat, and roll doesn't exist for them.
	            //In this case, the player's eyes are their center point for rotation, but these aren't the same as 
	            //their actual position.  Means we have to do funky math.
	            //We also need to check if we are the client player or another player, as other players require a
	            //different pre-render offset to be performed to get them into the right place. 
	            if(!renderedPlayer.equals(Minecraft.getMinecraft().player)){
	            	EntityPlayerSP masterPlayer = Minecraft.getMinecraft().player;
	            	double playerDistanceX = renderedPlayer.lastTickPosX + - masterPlayer.lastTickPosX + (renderedPlayer.posX - renderedPlayer.lastTickPosX -(masterPlayer.posX - masterPlayer.lastTickPosX))*event.getPartialRenderTick();
	            	double playerDistanceY = renderedPlayer.lastTickPosY + - masterPlayer.lastTickPosY + (renderedPlayer.posY - renderedPlayer.lastTickPosY -(masterPlayer.posY - masterPlayer.lastTickPosY))*event.getPartialRenderTick();
	            	double playerDistanceZ = renderedPlayer.lastTickPosZ + - masterPlayer.lastTickPosZ + (renderedPlayer.posZ - renderedPlayer.lastTickPosZ -(masterPlayer.posZ - masterPlayer.lastTickPosZ))*event.getPartialRenderTick();
	                GL11.glTranslated(playerDistanceX, playerDistanceY, playerDistanceZ);
	                
	                GL11.glTranslated(0, renderedPlayer.getEyeHeight(), 0);
	                GL11.glRotated(entityAngles.y, 0, 1, 0);
	                GL11.glRotated(entityAngles.x, 1, 0, 0);
	                GL11.glRotated(entityAngles.z, 0, 0, 1);
	                if(!ridingAngles.isZero()){
		                GL11.glRotated(ridingAngles.y, 0, 1, 0);
		                GL11.glRotated(ridingAngles.x, 1, 0, 0);
		                GL11.glRotated(ridingAngles.z, 0, 0, 1);
	                }
	                GL11.glTranslated(0, -renderedPlayer.getEyeHeight(), 0);
	                
	                GL11.glTranslated(-playerDistanceX, -playerDistanceY, -playerDistanceZ);
	            }else{
	            	GL11.glTranslated(0, renderedPlayer.getEyeHeight(), 0);
	            	GL11.glRotated(entityAngles.y, 0, 1, 0);
	                GL11.glRotated(entityAngles.x, 1, 0, 0);
	                GL11.glRotated(entityAngles.z, 0, 0, 1);
	                if(!ridingAngles.isZero()){
		                GL11.glRotated(ridingAngles.y, 0, 1, 0);
		                GL11.glRotated(ridingAngles.x, 1, 0, 0);
		                GL11.glRotated(ridingAngles.z, 0, 0, 1);
	                }
	            	GL11.glTranslated(0, -renderedPlayer.getEyeHeight(), 0);
	            }
        	}
        }
    }

    @SubscribeEvent
    public static void on(RenderPlayerEvent.Post event){
    	EntityPlayer renderedPlayer = event.getEntityPlayer();
    	if(tempHeldItemStorage.containsKey(renderedPlayer)){
    		renderedPlayer.inventory.mainInventory.set(renderedPlayer.inventory.currentItem, tempHeldItemStorage.get(renderedPlayer));
    		tempHeldItemStorage.remove(renderedPlayer);
    	}
    	if(renderedPlayer.getRidingEntity() instanceof BuilderEntity){
    		GL11.glPopMatrix();
        }
    }
    
    /**
	 *  Hand render events.  We use these to disable rendering of the item in the player's hand
	 *  if they are holding a gun.  Not sure why there's two events, but we cancel them both!
	 */
    @SubscribeEvent
    public static void on(RenderHandEvent event){
    	EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getMinecraft().player.getUniqueID().toString());
    	if(entity != null && entity.gun != null){
    		event.setCanceled(true);
    	}
    }
    
    @SubscribeEvent
    public static void on(RenderSpecificHandEvent event){
    	EntityPlayerGun entity = EntityPlayerGun.playerClientGuns.get(Minecraft.getMinecraft().player.getUniqueID().toString());
    	if(entity != null && entity.gun != null){
    		event.setCanceled(true);
    	}
    }
	
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
