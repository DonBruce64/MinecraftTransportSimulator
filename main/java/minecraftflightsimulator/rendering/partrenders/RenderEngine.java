package minecraftflightsimulator.rendering.partrenders;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntityEngine.EngineTypes;
import minecraftflightsimulator.rendering.partmodels.ModelEngineLarge;
import minecraftflightsimulator.rendering.partmodels.ModelEngineSmall;
import minecraftflightsimulator.systems.GL11DrawSystem;
import minecraftflightsimulator.systems.RenderSystem.RenderChild;
import net.minecraft.util.ResourceLocation;

public class RenderEngine extends RenderChild{
	private static final ModelEngineSmall modelSmall = new ModelEngineSmall();
	private static final ModelEngineLarge modelLarge = new ModelEngineLarge();
	private static final ResourceLocation smallTexture = new ResourceLocation("mfs", "textures/parts/enginesmall.png");
	private static final ResourceLocation largeTexture = new ResourceLocation("mfs", "textures/parts/enginelarge.png");

	@Override
	public void render(EntityChild child, double x, double y, double z, float partialTicks){		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(-child.parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(child.parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(child.parent.rotationRoll, 0, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		if(((EntityEngine) child).type.equals(EngineTypes.PLANE_LARGE)){
			GL11.glRotatef(180, 1, 0, 0);
			GL11.glTranslatef(0, -0.4F, -0.4F);
			GL11DrawSystem.bindTexture(largeTexture);
			modelLarge.render();
		}else{
			GL11DrawSystem.bindTexture(smallTexture);
			GL11.glRotatef(180, 1, 0, 0);
			GL11.glTranslatef(0, -0.4F, -0.0F);
			modelSmall.render();
		}
		GL11.glPopMatrix();
	}
}