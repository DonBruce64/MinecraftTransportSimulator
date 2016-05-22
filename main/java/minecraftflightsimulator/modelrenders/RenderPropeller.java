package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.parts.EntityPropeller;
import minecraftflightsimulator.models.ModelPropeller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderPropeller extends Render{
	private static final ModelPropeller model = new ModelPropeller();
	private static final ResourceLocation tierOneTexture = new ResourceLocation("minecraft", "textures/blocks/planks_oak.png");
	private static final ResourceLocation tierTwoTexture = new ResourceLocation("minecraft", "textures/blocks/iron_block.png");
	private static final ResourceLocation tierThreeTexture = new ResourceLocation("minecraft", "textures/blocks/obsidian.png");
	
    public RenderPropeller(){
        super();
    }

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw,float pitch){		
		EntityPropeller propeller=(EntityPropeller) entity;
		if(propeller.parent != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glRotatef(180-propeller.parent.rotationYaw, 0, 1, 0);
			GL11.glRotatef(-propeller.parent.rotationPitch, 1, 0, 0);
			GL11.glRotatef(-propeller.parent.rotationRoll, 0, 0, 1);
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			if(propeller.propertyCode%10==1){
				Minecraft.getMinecraft().renderEngine.bindTexture(tierTwoTexture);
			}else if(propeller.propertyCode%10==2){
				Minecraft.getMinecraft().renderEngine.bindTexture(tierThreeTexture);
			}else{
				Minecraft.getMinecraft().renderEngine.bindTexture(tierOneTexture);
			}
			
			model.renderPropellor(propeller.propertyCode%100/10, 70+5*(propeller.propertyCode/1000), -propeller.angularPosition);
			GL11.glPopMatrix();
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity propellor){
		return null;
	}
}