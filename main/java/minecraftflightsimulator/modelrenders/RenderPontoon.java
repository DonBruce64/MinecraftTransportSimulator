package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.models.ModelPontoon;
import minecraftflightsimulator.utilities.RenderHelper;
import minecraftflightsimulator.utilities.RenderHelper.RenderChild;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderPontoon extends RenderChild{
	private static final ModelPontoon model = new ModelPontoon();
	private static final ResourceLocation pontoonTexture = new ResourceLocation("minecraft", "textures/blocks/iron_block.png");

	@Override
	public void renderChildModel(EntityChild child, double x, double y, double z){		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(child.parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(child.parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(-child.parent.rotationRoll, 0, 0, 1);
		GL11.glTranslatef(0, -0.4F, -0.2F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.bindTexture(pontoonTexture);
        model.render();
		GL11.glPopMatrix();
	}
}