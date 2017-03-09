package minecraftflightsimulator.rendering.renders.parts;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.parts.EntityChest;
import minecraftflightsimulator.rendering.models.parts.ModelPlaneChest;
import minecraftflightsimulator.systems.GL11DrawSystem;
import minecraftflightsimulator.systems.RenderSystem.RenderChild;
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