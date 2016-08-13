package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.parts.EntityWheel;
import minecraftflightsimulator.entities.parts.EntityWheelSmall;
import minecraftflightsimulator.models.ModelWheel;
import minecraftflightsimulator.utilities.RenderHelper;
import minecraftflightsimulator.utilities.RenderHelper.RenderChild;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderWheel extends RenderChild{
	private static final ModelWheel model = new ModelWheel();
	private static final ResourceLocation innerTexture = new ResourceLocation("minecraft", "textures/blocks/wool_colored_white.png");
	private static final ResourceLocation outerTexture = new ResourceLocation("minecraft", "textures/blocks/wool_colored_black.png");

	@Override
	public void renderChildModel(EntityChild child, double x, double y, double z) {
		EntityWheel wheel = (EntityWheel) child;
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(-wheel.parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(wheel.parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(wheel.parent.rotationRoll, 0, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		if(wheel instanceof EntityWheelSmall){
			RenderHelper.bindTexture(innerTexture);
			model.renderSmallInnerWheel(wheel.angularPosition);
			RenderHelper.bindTexture(outerTexture);
			model.renderSmallOuterWheel(wheel.angularPosition);
		}else{
			RenderHelper.bindTexture(innerTexture);
			model.renderLargeInnerWheel(wheel.angularPosition);
			RenderHelper.bindTexture(outerTexture);
			model.renderLargeOuterWheel(wheel.angularPosition);
		}
		GL11.glPopMatrix();
	}
}