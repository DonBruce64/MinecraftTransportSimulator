package minecraftflightsimulator.rendering.renders.parts;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.parts.EntityWheel;
import minecraftflightsimulator.rendering.models.parts.ModelWheel;
import minecraftflightsimulator.systems.GL11DrawSystem;
import minecraftflightsimulator.systems.RenderSystem.RenderChild;
import net.minecraft.util.ResourceLocation;

public class RenderWheel extends RenderChild{
	private static final ModelWheel model = new ModelWheel();
	private static final ResourceLocation innerTexture = new ResourceLocation("minecraft", "textures/blocks/wool_colored_white.png");
	private static final ResourceLocation outerTexture = new ResourceLocation("minecraft", "textures/blocks/wool_colored_black.png");

	@Override
	public void render(EntityChild child, double x, double y, double z, float partialTicks){
		EntityWheel wheel = (EntityWheel) child;
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		if(wheel.isFlat){
			GL11.glTranslated(0, -wheel.height/2F, 0);
		}
		GL11.glRotatef(-wheel.parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(wheel.parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(wheel.parent.rotationRoll, 0, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		if(wheel instanceof EntityWheel.EntityWheelSmall){
			GL11DrawSystem.bindTexture(innerTexture);
			model.renderSmallInnerWheel(wheel.angularPosition + wheel.angularVelocity*partialTicks);
			if(!wheel.isFlat){
				GL11DrawSystem.bindTexture(outerTexture);
				model.renderSmallOuterWheel(wheel.angularPosition + wheel.angularVelocity*partialTicks);
			}
		}else{
			GL11DrawSystem.bindTexture(innerTexture);
			model.renderLargeInnerWheel(wheel.angularPosition + wheel.angularVelocity*partialTicks);
			if(!wheel.isFlat){
				GL11DrawSystem.bindTexture(outerTexture);
				model.renderLargeOuterWheel(wheel.angularPosition + wheel.angularVelocity*partialTicks);
			}
		}
		GL11.glPopMatrix();
	}
}