package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntityEngineLarge;
import minecraftflightsimulator.models.ModelEngineLarge;
import minecraftflightsimulator.models.ModelEngineSmall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFlameFX;
import net.minecraft.client.particle.EntitySmokeFX;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderEngine extends Render{
	private static final ModelEngineSmall modelSmall = new ModelEngineSmall();
	private static final ModelEngineLarge modelLarge = new ModelEngineLarge();
	private static final ResourceLocation smallTexture = new ResourceLocation("mfs", "textures/parts/enginesmall.png");
	private static final ResourceLocation largeTexture = new ResourceLocation("mfs", "textures/parts/enginelarge.png");
	
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
			if(engine instanceof EntityEngineLarge){
				GL11.glRotatef(180, 1, 0, 0);
				GL11.glTranslatef(0, -0.4F, -0.4F);
				Minecraft.getMinecraft().renderEngine.bindTexture(largeTexture);
				modelLarge.render();
			}else{
				Minecraft.getMinecraft().renderEngine.bindTexture(smallTexture);
				GL11.glRotatef(180, 1, 0, 0);
				GL11.glTranslatef(0, -0.4F, -0.0F);
				modelSmall.render();
			}
			GL11.glPopMatrix();
			if(engine.engineTemp > 93.3333){
				if(Minecraft.getMinecraft().effectRenderer != null){
					Minecraft.getMinecraft().effectRenderer.addEffect(new EntitySmokeFX(engine.worldObj, engine.posX, engine.posY, engine.posZ, 0, 0.5, 0));
					if(engine.engineTemp > 107.222){
						Minecraft.getMinecraft().effectRenderer.addEffect(new EntitySmokeFX(engine.worldObj, engine.posX, engine.posY, engine.posZ, 0, 0.5, 0, 2.5F));
					}
					if(engine.engineTemp > 121.111){
						Minecraft.getMinecraft().effectRenderer.addEffect(new EntityFlameFX(engine.worldObj, engine.posX, engine.posY, engine.posZ, 0, 0.5, 0));
					}
				}
			}
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity propellor){
		return null;
	}
}