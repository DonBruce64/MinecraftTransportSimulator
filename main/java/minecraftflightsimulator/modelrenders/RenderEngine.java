package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntityEngine.EngineTypes;
import minecraftflightsimulator.models.ModelEngineLarge;
import minecraftflightsimulator.models.ModelEngineSmall;
import minecraftflightsimulator.utilities.RenderHelper;
import minecraftflightsimulator.utilities.RenderHelper.RenderChild;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderEngine extends RenderChild{
	private static final ModelEngineSmall modelSmall = new ModelEngineSmall();
	private static final ModelEngineLarge modelLarge = new ModelEngineLarge();
	private static final ResourceLocation smallTexture = new ResourceLocation("mfs", "textures/parts/enginesmall.png");
	private static final ResourceLocation largeTexture = new ResourceLocation("mfs", "textures/parts/enginelarge.png");

	@Override
	public void renderChildModel(EntityChild child, double x, double y, double z){		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(-child.parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(child.parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(child.parent.rotationRoll, 0, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		if(((EntityEngine) child).type.equals(EngineTypes.PLANE_LARGE)){
			GL11.glRotatef(180, 1, 0, 0);
			GL11.glTranslatef(0, -0.4F, -0.4F);
			RenderHelper.bindTexture(largeTexture);
			modelLarge.render();
		}else{
			RenderHelper.bindTexture(smallTexture);
			GL11.glRotatef(180, 1, 0, 0);
			GL11.glTranslatef(0, -0.4F, -0.0F);
			modelSmall.render();
		}
		GL11.glPopMatrix();
	}
}