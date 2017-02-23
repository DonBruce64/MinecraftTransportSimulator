package minecraftflightsimulator.rendering.modelrenders;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.rendering.partmodels.ModelSkid;
import minecraftflightsimulator.systems.GL11DrawSystem;
import minecraftflightsimulator.systems.RenderSystem.RenderChild;
import net.minecraft.util.ResourceLocation;

public class RenderSkid extends RenderChild{
	private static final ModelSkid model = new ModelSkid();
	private static final ResourceLocation skidTexture = new ResourceLocation("mfs", "textures/parts/skid.png");

	@Override
	public void render(EntityChild child, double x, double y, double z, float partialTicks){
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(child.parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(child.parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(-child.parent.rotationRoll, 0, 0, 1);
		GL11.glTranslatef(0, -0.25F, 0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11DrawSystem.bindTexture(skidTexture);
        model.render();
		GL11.glPopMatrix();
	}
}