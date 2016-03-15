package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.EntityWheel;
import minecraftflightsimulator.entities.EntityWheelLarge;
import minecraftflightsimulator.models.ModelWheel;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderWheel extends Render{
	private static final ModelWheel model = new ModelWheel();
	
    public RenderWheel(){
        super();
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
			if(wheel instanceof EntityWheelLarge){
				this.bindTexture(model.innerTexture);
				model.renderLargeInnerWheel(wheel.angularPosition);
				this.bindTexture(model.outerTexture);
				model.renderLargeOuterWheel(wheel.angularPosition);
			}else{
				this.bindTexture(model.innerTexture);
				model.renderSmallInnerWheel(wheel.angularPosition);
				this.bindTexture(model.outerTexture);
				model.renderSmallOuterWheel(wheel.angularPosition);
			}
			GL11.glPopMatrix();
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
		return model.outerTexture;
	}
}