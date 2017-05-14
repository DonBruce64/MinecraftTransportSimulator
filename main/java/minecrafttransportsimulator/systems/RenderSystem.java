package minecrafttransportsimulator.systems;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSRegistryClient;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/**This class contains a set of classes used for the custom render system.
 * All parents and children should extend the appropriate classes for rendering.
 * 
 * @author don_bruce
 */
//TODO see about elminating these classes or something.  Not a fan of classes in classes where there's ONLY classes
public final class RenderSystem{	
	
    /**Abstract class for parent rendering.
     * Renders the parent model, and all child models that have been registered by
     * {@link registerChildRender}.  Ensures all parts are rendered in the exact
     * location they should be in as all rendering is done in the same operation.
     * Entities don't render above 255 well due to the new chunk visibility system.
     * This code is present to be called manually from
     * {@link ClientEventSystem#on(RenderWorldLastEvent)}.
     *
     * @author don_bruce
     */
    public static abstract class RenderParent extends Render{
    	private MTSVector childOffset;
    	private static EntityPlayer player;
    	
    	public RenderParent(RenderManager manager){
            super(manager);
            shadowSize = 0;
        }
    	
    	@Override
    	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks){
    		EntityMultipartParent parent = (EntityMultipartParent) entity;
    		player = Minecraft.getMinecraft().thePlayer;
    		GL11.glPushMatrix();
    		boolean playerRiding = false;
    		if(player.getRidingEntity() instanceof EntitySeat){
    			if(parent.equals(((EntitySeat) player.getRidingEntity()).parent)){
    				playerRiding = true;
    			}
    		}
    		//x, y, and z aren't correct here due to the delayed update system.
    		//Have to do this or put up with shaking while in the plane.
    		//TODO look into how PartialTicks affects this.  May not need an if statement.
    		if(playerRiding){
    			GL11.glTranslated(parent.posX - player.posX, parent.posY - player.posY, parent.posZ - player.posZ);
    		}else{
    			GL11.glTranslated(x, y, z);
    		}
            for(EntityMultipartChild child : parent.getChildren()){
            	if(MTSRegistryClient.childRenderMap.get(child.getClass()) != null){
            		childOffset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, parent.rotationPitch, parent.rotationYaw, parent.rotationRoll);
					MTSRegistryClient.childRenderMap.get(child.getClass()).render(child, childOffset.xCoord, childOffset.yCoord, childOffset.zCoord, partialTicks);
				}
            }
            this.renderParentModel(parent, partialTicks);
            GL11.glPopMatrix();
    	}
    	
    	protected abstract void renderParentModel(EntityMultipartParent parent, float partialTicks);
    }
}
