package minecrafttransportsimulator.rendering.partrenders;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import minecrafttransportsimulator.entities.parts.EntityPropeller;
import minecrafttransportsimulator.rendering.partmodels.ModelPropeller;
import minecrafttransportsimulator.systems.GL11DrawSystem;
import minecrafttransportsimulator.systems.RenderSystem.RenderChild;
import net.minecraft.util.ResourceLocation;

public class RenderPropeller extends RenderChild{
	private static final ModelPropeller model = new ModelPropeller();
	private static final ResourceLocation tierOneTexture = new ResourceLocation("minecraft", "textures/blocks/planks_oak.png");
	private static final ResourceLocation tierTwoTexture = new ResourceLocation("minecraft", "textures/blocks/iron_block.png");
	private static final ResourceLocation tierThreeTexture = new ResourceLocation("minecraft", "textures/blocks/obsidian.png");

	@Override
	public void render(EntityMultipartChild child, double x, double y, double z, float partialTicks){
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(180-child.parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(-child.parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(-child.parent.rotationRoll, 0, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		if(child.propertyCode==1){
			GL11DrawSystem.bindTexture(tierTwoTexture);
		}else if(child.propertyCode==2){
			GL11DrawSystem.bindTexture(tierThreeTexture);
		}else{
			GL11DrawSystem.bindTexture(tierOneTexture);
		}
		
		model.renderPropeller(((EntityPropeller) child).numberBlades, ((EntityPropeller) child).diameter, -((EntityPropeller) child).angularPosition - ((EntityPropeller) child).angularVelocity*partialTicks);
		GL11.glPopMatrix();
	}
}