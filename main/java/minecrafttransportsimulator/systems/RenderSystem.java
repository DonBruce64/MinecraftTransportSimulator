package minecrafttransportsimulator.systems;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSRegistryClient;
import minecrafttransportsimulator.entities.core.EntityChild;
import minecrafttransportsimulator.entities.core.EntityParent;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/**This class contains a set of classes used for the custom render system.
 * All parents and children should extend the appropriate classes for rendering.
 * 
 * @author don_bruce
 */
public final class RenderSystem{	
	
    /**Abstract class for parent rendering.
     * Renders the parent model, and all child models that have been registered by
     * {@link registerChildRender}.  Ensures all parts are rendered in the exact
     * location they should be in as all rendering is done in the same operation.
     * 
     * @author don_bruce
     */
    public static abstract class RenderParent extends Render{
    	private boolean playerRiding;
    	private MTSVector childOffset;
    	private static EntityPlayer player;
    	
    	public RenderParent(RenderManager manager){
            super();
            shadowSize = 0;
        }
    	
    	@Override
    	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks){
    		this.render((EntityParent) entity, x, y, z, partialTicks);
    	}
    	
    	/**
    	 * Entities don't render above 255 well in later versions due to the
    	 * new chunk visibility system.  This code is for the default system
    	 * only, and will be called directly from Minecraft's rendering
    	 * system or {@link ClientEventSystem#on(RenderWorldLastEvent)}.
    	 * The latter method only calls if this method hasn't been called first
    	 * by Minecraft's system.
		 **/
    	private void render(EntityParent parent, double x, double y, double z, float partialTicks){
    		if(!parent.rendered && parent.posY >= 255){return;}
    		parent.rendered = true;
    		player = Minecraft.getMinecraft().thePlayer;
    		GL11.glPushMatrix();
    		playerRiding = false;
    		if(player.ridingEntity instanceof EntitySeat){
    			if(parent.equals(((EntitySeat) player.ridingEntity).parent)){
    				playerRiding = true;
    			}
    		}
    		//x, y, and z aren't correct here due to the delayed update system.
    		//Have to do this or put up with shaking while in the plane.
    		if(playerRiding){
    			GL11.glTranslated(parent.posX - player.posX, parent.posY - player.posY, parent.posZ - player.posZ);
    		}else{
    			GL11.glTranslated(x, y, z);
    		}
            for(EntityChild child : parent.getChildren()){
            	if(MTSRegistryClient.childRenderMap.get(child.getClass()) != null){
            		childOffset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, parent.rotationPitch, parent.rotationYaw, parent.rotationRoll);
            		MTSRegistryClient.childRenderMap.get(child.getClass()).render(child, childOffset.xCoord, childOffset.yCoord, childOffset.zCoord, partialTicks);
        		}
            }
            this.renderParentModel(parent, partialTicks);
            GL11.glPopMatrix();
    	}
    	
    	protected abstract void renderParentModel(EntityParent parent, float partialTicks);
    	
    	@Override
    	protected ResourceLocation getEntityTexture(Entity propellor){
    		return null;
    	}
    }
    
    /**Abstract class for child rendering.
     * Register with {@link registerChildRender} to activate rendering in {@link RenderParent}
     * 
     * @author don_bruce
     */
    public static abstract class RenderChild{
    	public RenderChild(){}
    	public abstract void render(EntityChild child, double x, double y, double z, float partialTicks);
    }
    
    public static abstract class RenderTileBase extends TileEntitySpecialRenderer{
    	
    	@Override
    	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float scale){
    		this.doRender(tile, x, y, z);
    	}
    	
    	protected abstract void doRender(TileEntity tile, double x, double y, double z);
    }
    
    public static class RenderNull extends Render{
    	public RenderNull(RenderManager manager){
            super();
    	}
    	@Override
    	public void doRender(Entity entity, double x, double y, double z, float yaw,float pitch){}
    	@Override
    	protected ResourceLocation getEntityTexture(Entity entity){return null;}
    }
}
