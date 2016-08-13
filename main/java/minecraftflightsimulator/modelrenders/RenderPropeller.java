package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.entities.parts.EntityPropeller;
import minecraftflightsimulator.models.ModelPropeller;
import minecraftflightsimulator.utilities.RenderHelper;
import minecraftflightsimulator.utilities.RenderHelper.RenderChild;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderPropeller extends RenderChild{
	private static final ModelPropeller model = new ModelPropeller();
	private static final ResourceLocation tierOneTexture = new ResourceLocation("minecraft", "textures/blocks/planks_oak.png");
	private static final ResourceLocation tierTwoTexture = new ResourceLocation("minecraft", "textures/blocks/iron_block.png");
	private static final ResourceLocation tierThreeTexture = new ResourceLocation("minecraft", "textures/blocks/obsidian.png");

	@Override
	public void renderChildModel(EntityChild child, double x, double y, double z){		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(180-child.parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(-child.parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(-child.parent.rotationRoll, 0, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		if(child.propertyCode%10==1){
			RenderHelper.bindTexture(tierTwoTexture);
		}else if(child.propertyCode%10==2){
			RenderHelper.bindTexture(tierThreeTexture);
		}else{
			RenderHelper.bindTexture(tierOneTexture);
		}
		
		model.renderPropellor(child.propertyCode%100/10, 70+5*(child.propertyCode/1000), -((EntityPropeller) child).angularPosition);
		GL11.glPopMatrix();
	}
}