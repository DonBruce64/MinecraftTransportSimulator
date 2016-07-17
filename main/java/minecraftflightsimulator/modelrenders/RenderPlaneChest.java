package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.parts.EntityPlaneChest;
import minecraftflightsimulator.models.ModelPlaneChest;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderPlaneChest extends Render{
	private static final ModelPlaneChest model = new ModelPlaneChest();
	private static final ResourceLocation chestTexture = new ResourceLocation("minecraft", "textures/entity/chest/normal.png");
	
    public RenderPlaneChest(){
        super();
    }

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw,float pitch){		
		EntityPlaneChest chest=(EntityPlaneChest) entity;
		if(chest.parent != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glRotatef(-chest.parent.rotationYaw, 0, 1, 0);
			GL11.glRotatef(180 + chest.parent.rotationPitch, 1, 0, 0);
			GL11.glRotatef(-chest.parent.rotationRoll, 0, 0, 1);
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			this.bindTexture(chestTexture);
			model.renderAll(-chest.lidAngle);
			GL11.glPopMatrix();
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity seat) {
		return null;
	}
}