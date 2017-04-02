package minecrafttransportsimulator.rendering.partrenders;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityChild;
import minecrafttransportsimulator.entities.parts.EntityEngine;
import minecrafttransportsimulator.entities.parts.EntityEngine.EngineTypes;
import minecrafttransportsimulator.rendering.partmodels.ModelEngineLarge;
import minecrafttransportsimulator.rendering.partmodels.ModelEngineSmall;
import minecrafttransportsimulator.systems.GL11DrawSystem;
import minecrafttransportsimulator.systems.RenderSystem.RenderChild;
import net.minecraft.util.ResourceLocation;

public class RenderEngine extends RenderChild{
	private static final ModelEngineSmall modelSmall = new ModelEngineSmall();
	private static final ModelEngineLarge modelLarge = new ModelEngineLarge();
	private static final ResourceLocation smallTexture = new ResourceLocation(MTS.MODID, "textures/parts/enginesmall.png");
	private static final ResourceLocation largeTexture = new ResourceLocation(MTS.MODID, "textures/parts/enginelarge.png");

	@Override
	public void render(EntityChild child, double x, double y, double z, float partialTicks){		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(child.parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(child.parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(-child.parent.rotationRoll, 0, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(0, -((EntityEngine) child).type.size/2, 0);
		if(((EntityEngine) child).type.equals(EngineTypes.PLANE_LARGE)){
			GL11DrawSystem.bindTexture(largeTexture);
			modelLarge.render();
		}else{
			GL11DrawSystem.bindTexture(smallTexture);
			modelSmall.render();
		}
		GL11.glPopMatrix();
	}
}