package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.models.ModelSkid;
import minecraftflightsimulator.utilities.RenderHelper.RenderChild;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderSkid extends RenderChild{
	private static final ModelSkid model = new ModelSkid();
	private static final ResourceLocation skidTexture = new ResourceLocation("mfs", "textures/parts/skid.png");

	@Override
	public void renderChildModel(EntityChild child, double x, double y, double z) {
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(child.parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(child.parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(-child.parent.rotationRoll, 0, 0, 1);
		GL11.glTranslatef(0, -0.25F, 0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().renderEngine.bindTexture(skidTexture);
        model.render();
		GL11.glPopMatrix();
	}
}