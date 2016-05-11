package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntityEngineLarge;
import minecraftflightsimulator.models.ModelEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderEngine extends Render{
	private static final ModelEngine model = new ModelEngine();
	private static final ResourceLocation engineTexture = new ResourceLocation("minecraft", "textures/blocks/obsidian.png");
	
    public RenderEngine(){
        super();
    }

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw,float pitch){		
		EntityEngine engine=(EntityEngine) entity;
		if(engine.parent != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glRotatef(-engine.parent.rotationYaw, 0, 1, 0);
			GL11.glRotatef(engine.parent.rotationPitch, 1, 0, 0);
			GL11.glRotatef(engine.parent.rotationRoll, 0, 0, 1);
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	        Minecraft.getMinecraft().renderEngine.bindTexture(engineTexture);
			if(engine instanceof EntityEngineLarge){
				model.renderLargeEngine();
			}else{
				model.renderSmallEngine();
			}
			GL11.glPopMatrix();
	        if(engine.engineRPM > 1000 && engine.fueled){
	        	engine.worldObj.spawnParticle("smoke", engine.posX+0.5*MathHelper.sin(engine.parent.rotationYaw * 0.017453292F), engine.posY-MathHelper.sin(engine.parent.rotationPitch * 0.017453292F), engine.posZ-0.5*MathHelper.cos(engine.parent.rotationYaw * 0.017453292F), 0.25*MathHelper.sin(engine.parent.rotationYaw * 0.017453292F), -0.25, -0.25*MathHelper.cos(engine.parent.rotationYaw * 0.017453292F));
	        }
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity propellor){
		return null;
	}
}