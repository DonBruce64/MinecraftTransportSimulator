package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.parts.EntitySkid;
import minecraftflightsimulator.models.ModelSkid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderSkid extends Render{
	private static final ModelSkid model = new ModelSkid();
	private static final ResourceLocation skidTexture = new ResourceLocation("mfs", "textures/parts/skid.png");
	
    public RenderSkid(){
        super();
    }

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float pitch){		
		EntitySkid skid=(EntitySkid) entity;
		if(skid.parent != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x, y + 0.25, z);
			GL11.glRotatef(180, 1, 0, 0);
			GL11.glRotatef(skid.parent.rotationYaw, 0, 1, 0);
			GL11.glRotatef(skid.parent.rotationPitch, 1, 0, 0);
			GL11.glRotatef(skid.parent.rotationRoll, 0, 0, 1);
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	        Minecraft.getMinecraft().renderEngine.bindTexture(skidTexture);
	        model.render();
			GL11.glPopMatrix();
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity propellor){
		return null;
	}
}