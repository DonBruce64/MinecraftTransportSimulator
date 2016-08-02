package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.parts.EntitySkid;
import minecraftflightsimulator.models.ModelSkid;
import minecraftflightsimulator.utilities.RenderHelper.RenderEntityBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderSkid extends RenderEntityBase{
	private static final ModelSkid model = new ModelSkid();
	private static final ResourceLocation skidTexture = new ResourceLocation("mfs", "textures/parts/skid.png");
	
    public RenderSkid(RenderManager manager){
        super(manager);
    }

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float pitch){		
		EntitySkid skid=(EntitySkid) entity;
		if(skid.parent != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glRotatef(180, 1, 0, 0);
			GL11.glRotatef(skid.parent.rotationYaw, 0, 1, 0);
			GL11.glRotatef(skid.parent.rotationPitch, 1, 0, 0);
			GL11.glRotatef(-skid.parent.rotationRoll, 0, 0, 1);
			GL11.glTranslatef(0, -0.25F, 0);
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	        Minecraft.getMinecraft().renderEngine.bindTexture(skidTexture);
	        model.render();
			GL11.glPopMatrix();
		}
	}
}