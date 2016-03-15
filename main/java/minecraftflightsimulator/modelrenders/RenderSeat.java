package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.EntitySeat;
import minecraftflightsimulator.models.ModelSeat;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderSeat extends Render{
	private static final ModelSeat model = new ModelSeat();
	
    public RenderSeat(){
        super();
    }

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw,float pitch){		
		EntitySeat seat=(EntitySeat) entity;
		if(seat.parent != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glRotatef(-seat.parent.rotationYaw, 0, 1, 0);
			GL11.glRotatef(seat.parent.rotationPitch, 1, 0, 0);
			GL11.glRotatef(seat.parent.rotationRoll, 0, 0, 1);
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			this.bindTexture(new ResourceLocation("minecraft", "textures/blocks/" + Blocks.wooden_slab.getIcon(0, seat.propertyCode & 7).getIconName()  + ".png"));
			model.renderFrame();
			this.bindTexture(new ResourceLocation("minecraft", "textures/blocks/" + Blocks.wool.getIcon(0, seat.propertyCode >> 3).getIconName()  + ".png"));
			model.renderCushion();
			GL11.glPopMatrix();
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity seat) {
		return new ResourceLocation("minecraft", "textures/blocks/" + Blocks.wool.getIcon(0, ((EntitySeat) seat).propertyCode >> 2).getIconName()  + ".png");
	}
}