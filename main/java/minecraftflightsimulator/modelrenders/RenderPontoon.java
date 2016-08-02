package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.parts.EntityPontoon;
import minecraftflightsimulator.models.ModelPontoon;
import minecraftflightsimulator.utilities.RenderHelper.RenderEntityBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderPontoon extends RenderEntityBase{
	private static final ModelPontoon model = new ModelPontoon();
	private static final ResourceLocation pontoonTexture = new ResourceLocation("minecraft", "textures/blocks/iron_block.png");
	
    public RenderPontoon(RenderManager manager){
        super(manager);
    }

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float pitch){		
		EntityPontoon pontoon=(EntityPontoon) entity;
		if(pontoon.parent != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glRotatef(180, 1, 0, 0);
			GL11.glRotatef(pontoon.parent.rotationYaw, 0, 1, 0);
			GL11.glRotatef(pontoon.parent.rotationPitch, 1, 0, 0);
			GL11.glRotatef(-pontoon.parent.rotationRoll, 0, 0, 1);
			GL11.glTranslatef(0, -0.6F, -0.2F);
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	        Minecraft.getMinecraft().renderEngine.bindTexture(pontoonTexture);
	        model.render();
			GL11.glPopMatrix();
		}
	}
}