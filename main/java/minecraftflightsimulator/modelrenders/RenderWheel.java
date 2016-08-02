package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.parts.EntityWheel;
import minecraftflightsimulator.entities.parts.EntityWheelSmall;
import minecraftflightsimulator.models.ModelWheel;
import minecraftflightsimulator.utilities.RenderHelper.RenderEntityBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderWheel extends RenderEntityBase{
	private static final ModelWheel model = new ModelWheel();
	private static final ResourceLocation innerTexture = new ResourceLocation("minecraft", "textures/blocks/wool_colored_white.png");
	private static final ResourceLocation outerTexture = new ResourceLocation("minecraft", "textures/blocks/wool_colored_black.png");
	
    public RenderWheel(RenderManager manager){
        super(manager);
    }

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw,float pitch){		
		EntityWheel wheel = (EntityWheel) entity;
		if(wheel.parent != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glRotatef(-wheel.parent.rotationYaw, 0, 1, 0);
			GL11.glRotatef(wheel.parent.rotationPitch, 1, 0, 0);
			GL11.glRotatef(wheel.parent.rotationRoll, 0, 0, 1);
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			if(wheel instanceof EntityWheelSmall){
				this.bindTexture(innerTexture);
				model.renderSmallInnerWheel(wheel.angularPosition);
				this.bindTexture(outerTexture);
				model.renderSmallOuterWheel(wheel.angularPosition);
			}else{
				this.bindTexture(innerTexture);
				model.renderLargeInnerWheel(wheel.angularPosition);
				this.bindTexture(outerTexture);
				model.renderLargeOuterWheel(wheel.angularPosition);
			}
			GL11.glPopMatrix();
		}
	}
}