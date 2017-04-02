package minecrafttransportsimulator.rendering.partrenders;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.entities.core.EntityChild;
import minecrafttransportsimulator.entities.parts.EntityChest;
import minecrafttransportsimulator.rendering.partmodels.ModelPlaneChest;
import minecrafttransportsimulator.systems.GL11DrawSystem;
import minecrafttransportsimulator.systems.RenderSystem.RenderChild;
import net.minecraft.util.ResourceLocation;

public class RenderPlaneChest extends RenderChild{
	private static final ModelPlaneChest model = new ModelPlaneChest();
	private static final ResourceLocation chestTexture = new ResourceLocation("minecraft", "textures/entity/chest/normal.png");

	@Override
	public void render(EntityChild child, double x, double y, double z, float partialTicks){
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(-child.parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(180 + child.parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(-child.parent.rotationRoll, 0, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11DrawSystem.bindTexture(chestTexture);
		model.renderAll(-((EntityChest) child).lidAngle);
		GL11.glPopMatrix();
	}
}